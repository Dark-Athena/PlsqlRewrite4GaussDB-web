package com.plsqlrewriter.webapp.service;

import com.plsqlrewriter.webapp.model.Project;
import com.plsqlrewriter.webapp.model.UserGroup;
import com.plsqlrewriter.webapp.repository.ProjectRepository;
import com.plsqlrewriter.webapp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.classic.PatternLayout;
import org.slf4j.MDC;
import java.nio.charset.StandardCharsets;
import com.plsqlrewriter.webapp.model.Project.FileDetail;

@Service
public class ProjectService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    @Autowired
    private ProjectRepository projectRepository;

    private final Queue<String> projectQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final Map<String, Runnable> taskMap = new ConcurrentHashMap<>();
    private final int maxThreads;
    private final ExecutorService executor;
    private final AtomicInteger runningCount = new AtomicInteger(0);

    public ProjectService() {
        String maxThreadsProp = System.getProperty("plsqlweb.maxThreads");
        int sysMax = maxThreadsProp != null ? Integer.parseInt(maxThreadsProp) : (Runtime.getRuntime().availableProcessors() - 1);
        this.maxThreads = Math.max(1, sysMax);
        this.executor = Executors.newFixedThreadPool(this.maxThreads);
    }
    
    public Project createAndRunProject(String name, String owner, String group, String params, MultipartFile file, String inputCharset, String outputCharset, int concurrency) throws IOException {
        String id = UUID.randomUUID().toString();
        logger.info("Creating project id={}, name={}", id, name);
        Path uploadDir = Paths.get("data", id, "input");
        Files.createDirectories(uploadDir);
        Path inputFile = uploadDir.resolve(file.getOriginalFilename());
        file.transferTo(inputFile);

        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setOwner(owner);
        project.setGroup(group);
        project.setInputFilePath(inputFile.toString());

        String originalFilename = file.getOriginalFilename();
        String outputFilename;
        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".zip")) {
            String baseName = originalFilename.substring(0, originalFilename.length() - 4);
            outputFilename = String.format("%s_output_%d.zip", baseName, System.currentTimeMillis());
            project.setOutputType("zip");
        } else {
            String baseName = originalFilename.contains(".") ? originalFilename.substring(0, originalFilename.lastIndexOf('.')) : originalFilename;
            String extension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf('.')) : "";
            outputFilename = String.format("%s_output_%d%s", baseName, System.currentTimeMillis(), extension);
            project.setOutputType("single");
        }
        project.setOutputFilePath("data/" + id + "/" + outputFilename);
        project.setLogFilePath("data/" + id + "/log.txt");
        project.setStatus(Project.Status.QUEUED);
        project.setCreateTime(java.time.LocalDateTime.now());
        project.setUpdateTime(java.time.LocalDateTime.now());
        projectRepository.save(project);

        Runnable task = () -> {
            Project currentProject = projectRepository.findById(project.getId()).orElse(null);
            if(currentProject == null) {
                logger.warn("Project {} not found in DB before task starts.", project.getId());
                return;
            }
            // 修复：确保 userGroup 和 group 字段不丢失
            if (project.getUserGroup() != null) {
                currentProject.setUserGroup(project.getUserGroup());
                currentProject.setGroup(project.getGroup());
            }
            logger.info("Task starting for project id={}", currentProject.getId());
            boolean success = false;
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("ROOT");
            String logFilePath = currentProject.getLogFilePath();
            FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
            fileAppender.setContext(loggerContext);
            fileAppender.setName("project-log-" + currentProject.getId());
            fileAppender.setFile(logFilePath);
            LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
            encoder.setContext(loggerContext);
            PatternLayout layout = new PatternLayout();
            layout.setContext(loggerContext);
            layout.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
            layout.start();
            encoder.setLayout(layout);
            encoder.setCharset(StandardCharsets.UTF_8);
            fileAppender.setEncoder(encoder);
            fileAppender.start();
            rootLogger.addAppender(fileAppender);
            try (BufferedWriter logWriter = Files.newBufferedWriter(Paths.get(currentProject.getLogFilePath()), StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                logWriter.write("任务开始\n");
                File inputFileAsFile = inputFile.toFile();
                File outputDir = Paths.get("data/" + id + "/output").toFile();
                outputDir.mkdirs();
                if ("zip".equals(currentProject.getOutputType())) {
                    // 1. 直接解压到 input 目录
                    com.plsqlrewriter.webapp.util.ZipUtil.unzip(inputFileAsFile, uploadDir.toFile());
                    // 2. 遍历 input 目录下所有 .sql 文件
                    List<Path> sqlFiles;
                    try (Stream<Path> walk = Files.walk(uploadDir)) {
                        sqlFiles = walk.filter(p -> p.toString().toLowerCase().endsWith(".sql")).collect(Collectors.toList());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    int threadNum = Math.max(1, Math.min(concurrency, sqlFiles.size()));
                    ExecutorService pool = Executors.newFixedThreadPool(threadNum);
                    List<Future<?>> futures = new ArrayList<>();
                    List<FileDetail> fileDetails = Collections.synchronizedList(new ArrayList<>());
                    for (Path sqlPath : sqlFiles) {
                        futures.add(pool.submit(() -> {
                            FileDetail detail = new FileDetail();
                            detail.setFileName(uploadDir.relativize(sqlPath).toString());
                            detail.setInputPath(sqlPath.toString());
                            Path outPath = outputDir.toPath().resolve(detail.getFileName());
                            detail.setOutputPath(outPath.toString());
                            try {
                                outPath.getParent().toFile().mkdirs();
                                String sql = new String(Files.readAllBytes(sqlPath), inputCharset);
                                MDC.put("file", detail.getFileName());
                                try {
                                    String result = com.plsqlrewriter.core.PlSqlRewriter.rewriteSql(sql, inputCharset, outputCharset, detail.getFileName());
                                    Files.write(outPath, result.getBytes(outputCharset));
                                    detail.setStatus("success");
                                } finally {
                                    MDC.remove("file");
                                }
                                synchronized (logWriter) {
                                    logWriter.write("转换成功: " + detail.getFileName() + "\n");
                                }
                            } catch (Exception ex) {
                                detail.setStatus("failed");
                                detail.setError(ex.getMessage());
                                synchronized (logWriter) {
                                    try { logWriter.write("转换失败: " + sqlPath + " 错误: " + ex.getMessage() + "\n"); ex.printStackTrace(new PrintWriter(logWriter));} catch(Exception ignore){}
                                }
                            }
                            fileDetails.add(detail);
                        }));
                    }
                    for (Future<?> f : futures) {
                        try { f.get(); } catch (Exception ignore) {}
                    }
                    pool.shutdown();
                    currentProject.setFileDetails(fileDetails);
                    projectRepository.save(currentProject);
                    com.plsqlrewriter.webapp.util.ZipUtil.zipDir(outputDir, new File(currentProject.getOutputFilePath()));
                } else { // "single"
                    String sql = new String(Files.readAllBytes(inputFile), inputCharset);
                    MDC.put("file", file.getOriginalFilename());
                    try {
                        String result = com.plsqlrewriter.core.PlSqlRewriter.rewriteSql(sql, inputCharset, outputCharset, file.getOriginalFilename());
                        Files.write(Paths.get(currentProject.getOutputFilePath()), result.getBytes(outputCharset));
                    } finally {
                        MDC.remove("file");
                    }
                    logWriter.write("转换成功: " + file.getOriginalFilename() + "\n");

                    // 新增：为单文件项目生成 fileDetails
                    List<FileDetail> fileDetails = new ArrayList<>();
                    FileDetail detail = new FileDetail();
                    detail.setFileName(file.getOriginalFilename());
                    detail.setInputPath(inputFile.toString());
                    detail.setOutputPath(currentProject.getOutputFilePath());
                    detail.setStatus("success");
                    detail.setError(null);
                    fileDetails.add(detail);
                    currentProject.setFileDetails(fileDetails);
                    projectRepository.save(currentProject);
                }
                logWriter.write("任务完成\n");
                logWriter.flush();
                success = true;
            } catch (Exception ex) {
                try (BufferedWriter logWriter = Files.newBufferedWriter(Paths.get(currentProject.getLogFilePath()), StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    logWriter.write("任务失败: " + ex.getMessage() + "\n");
                    ex.printStackTrace(new PrintWriter(logWriter));
                    logWriter.flush();
                } catch(Exception ignore){}
                currentProject.setStatus(Project.Status.FAILED);
                currentProject.setErrorMsg(ex.getMessage());
            } finally {
                rootLogger.detachAppender(fileAppender);
            }
            if (success) {
                currentProject.setStatus(Project.Status.SUCCESS);
            }
            currentProject.setUpdateTime(java.time.LocalDateTime.now());
            currentProject.setFinishTime(java.time.LocalDateTime.now());
            projectRepository.save(currentProject);
            logger.info("Task finished for project id={}, status={}", currentProject.getId(), currentProject.getStatus());
        };
        this.enqueueTask(project, task);
        return project;
    }

    private void enqueueTask(Project project, Runnable task) {
        logger.info("Queuing project: {}", project);
        taskMap.put(project.getId(), task);
        projectQueue.offer(project.getId());
        scheduleNext();
    }
    
    private void scheduleNext() {
        logger.info("scheduleNext called. Running: {}, Queued: {}, MaxThreads: {}", runningCount.get(), projectQueue.size(), maxThreads);
        if (runningCount.get() >= maxThreads) {
            logger.info("Scheduler busy. runningCount >= maxThreads. Exiting.");
            return;
        }
        String nextId = projectQueue.poll();
        if (nextId == null) {
            logger.info("Queue is empty. Exiting scheduler.");
            return;
        }
        
        Project project = projectRepository.findById(nextId).orElse(null);
        Runnable task = taskMap.get(nextId);
        if (project == null || task == null) {
            logger.warn("Project or task for id={} not found. Might have been deleted.", nextId);
            return;
        }

        logger.info("Scheduler is starting project id={}", nextId);
        project.setStatus(Project.Status.RUNNING);
        project.setUpdateTime(java.time.LocalDateTime.now());
        projectRepository.save(project); // Save RUNNING status
        
        runningCount.incrementAndGet();
        
        Future<?> future = executor.submit(() -> {
            try {
                task.run();
            } finally {
                logger.info("Finally block for project id={}. Decrementing runningCount.", project.getId());
                runningCount.decrementAndGet();
                runningTasks.remove(project.getId());
                taskMap.remove(project.getId());
                scheduleNext(); // Schedule the next one
            }
        });
        runningTasks.put(project.getId(), future);
    }

    public Map<String, Object> listProjects(User currentUser, int page, int pageSize, String name, String status, String owner, String sortField, String sortOrder) {
        Map<String, Object> resultMap = new HashMap<>();
        if (currentUser == null) {
            resultMap.put("total", 0);
            resultMap.put("list", Collections.emptyList());
            return resultMap;
        }
        List<Project> all;
        if (currentUser.getRoles().contains("ADMIN")) {
            all = projectRepository.findAll();
        } else {
            UserGroup group = currentUser.getUserGroup();
            if (group != null) {
                all = projectRepository.findByUserGroup(group);
            } else {
                all = Collections.emptyList();
            }
        }
        // 过滤
        Stream<Project> stream = all.stream();
        if (name != null && !name.isEmpty()) {
            stream = stream.filter(p -> p.getName() != null && p.getName().contains(name));
        }
        if (status != null && !status.isEmpty()) {
            stream = stream.filter(p -> p.getStatus() != null && p.getStatus().name().equalsIgnoreCase(status));
        }
        if (owner != null && !owner.isEmpty()) {
            stream = stream.filter(p -> p.getOwner() != null && p.getOwner().contains(owner));
        }
        List<Project> filtered = stream.collect(Collectors.toList());
        // 排序
        if (sortField != null && !sortField.isEmpty()) {
            Comparator<Project> comparator = null;
            switch (sortField) {
                case "name":
                    comparator = Comparator.comparing(Project::getName, Comparator.nullsLast(String::compareToIgnoreCase));
                    break;
                case "status":
                    comparator = Comparator.comparing(p -> p.getStatus() != null ? p.getStatus().name() : "");
                    break;
                case "owner":
                    comparator = Comparator.comparing(Project::getOwner, Comparator.nullsLast(String::compareToIgnoreCase));
                    break;
                case "createTime":
                    comparator = Comparator.comparing(Project::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "finishTime":
                    comparator = Comparator.comparing(Project::getFinishTime, Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                default:
                    break;
            }
            if (comparator != null) {
                if ("desc".equalsIgnoreCase(sortOrder)) {
                    comparator = comparator.reversed();
                }
                filtered.sort(comparator);
            }
        } else {
            // 默认按创建时间倒序
            filtered.sort(Comparator.comparing(Project::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        }
        // 分页
        int total = filtered.size();
        int fromIndex = Math.max(0, Math.min((page - 1) * pageSize, total));
        int toIndex = Math.max(0, Math.min(fromIndex + pageSize, total));
        List<Project> pageList = filtered.subList(fromIndex, toIndex);
        // 恢复运行/排队状态
        for (Project p : pageList) {
            if (taskMap.containsKey(p.getId())) {
                p.setStatus(runningTasks.containsKey(p.getId()) ? Project.Status.RUNNING : Project.Status.QUEUED);
            }
        }
        resultMap.put("total", total);
        resultMap.put("list", pageList);
        return resultMap;
    }

    public Project getProject(String id) {
        return projectRepository.findById(id).orElse(null);
    }

    public void terminateProject(String id) {
        Future<?> future = runningTasks.get(id);
        if (future != null) {
            future.cancel(true); // Attempt to interrupt the thread
            runningTasks.remove(id);
            taskMap.remove(id);
            logger.info("Terminated running project id={}", id);
        } else {
            logger.info("Project id={} not running, cannot terminate.", id);
        }
    }

    public boolean deleteProject(String id) {
        terminateProject(id);
        projectQueue.remove(id);

        if (getProject(id) != null) {
            projectRepository.deleteById(id);
            try {
                Path projectDir = Paths.get("data", id);
                if (Files.exists(projectDir)) {
                    Files.walk(projectDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                }
                return true;
            } catch (IOException e) {
                logger.error("Failed to delete project directory for id={}", id, e);
                return false;
            }
        }
        return false;
    }

    public void deleteProjects(List<String> ids) {
        for (String id : ids) {
            deleteProject(id);
        }
    }

    public int getQueuePosition(String id) {
        if (!projectQueue.contains(id)) {
            return 0; // Not in queue (running, finished, or invalid)
        }
        if (runningTasks.containsKey(id)) return 0;
        int pos = 1;
        for (String pid : projectQueue) {
            if (pid.equals(id)) return pos;
            pos++;
        }
        return -1;
    }

    public int getMaxThreads() { return maxThreads; }
    public int getRunningCount() { return runningCount.get(); }
    public int getQueueSize() { return projectQueue.size(); }
} 