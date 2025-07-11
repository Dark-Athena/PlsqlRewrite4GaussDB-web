package com.plsqlrewriter.webapp.controller;

import com.plsqlrewriter.webapp.model.Project;
import com.plsqlrewriter.webapp.model.request.BatchDeleteRequest;
import com.plsqlrewriter.webapp.repository.ProjectRepository;
import com.plsqlrewriter.webapp.service.ProjectService;
import com.plsqlrewriter.webapp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createProject(
            @RequestParam("name") String name,
            HttpSession session,
            @RequestParam(value = "params", required = false) String params,
            @RequestParam("file") MultipartFile file,
            @RequestParam("inputCharset") String inputCharset,
            @RequestParam("outputCharset") String outputCharset,
            @RequestParam(value = "concurrency", required = false, defaultValue = "1") int concurrency,
            HttpServletRequest request) {
        try {
            User user = (User) session.getAttribute("user");
            logger.info("[createProject] user from session: {}", user);
            if (user == null) {
                // 打印 sessionId 和 cookie 信息
                String sessionId = request.getSession(false) != null ? request.getSession(false).getId() : null;
                logger.warn("[createProject] user is null, sessionId: {}", sessionId);
                if (request.getCookies() != null) {
                    for (var cookie : request.getCookies()) {
                        logger.warn("[createProject] cookie: {}={}", cookie.getName(), cookie.getValue());
                    }
                } else {
                    logger.warn("[createProject] no cookies found in request");
                }
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未登录或登录已失效，请重新登录");
            }
            // 校验项目名称唯一性
            if (projectRepository.findAll().stream().anyMatch(p -> name.equals(p.getName()))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("项目名称已存在，请更换名称");
            }
            Project project = projectService.createAndRunProject(name, user.getUsername(), null, params, file, inputCharset, outputCharset, concurrency);
            if(user.getUserGroup() != null) {
                project.setUserGroup(user.getUserGroup());
                project.setGroup(user.getUserGroup().getName());
                projectRepository.save(project);
            }

            int queuePos = projectService.getQueuePosition(project.getId());
            Map<String, Object> resp = new HashMap<>();
            resp.put("id", project.getId());
            resp.put("status", project.getStatus());
            resp.put("queuePosition", queuePos);

            if (queuePos > 0) {
                resp.put("message", "服务器资源忙，正在排队中，前方还有" + (queuePos - 1) + "个项目");
            }
            
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectDetails(@PathVariable String id) {
        Project project = projectService.getProject(id);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(project);
    }

    // 项目列表
    @GetMapping("/list")
    public Map<String, Object> listProjects(
            HttpSession session,
            HttpServletRequest request,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "owner", required = false) String owner,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortOrder", required = false) String sortOrder
    ) {
        User user = (User) session.getAttribute("user");
        String sessionId = session.getId();
        System.out.println("[ProjectController /api/project/list] sessionId=" + sessionId + ", user=" + user);
        return projectService.listProjects(user, page, pageSize, name, status, owner, sortField, sortOrder);
    }

    // 项目状态
    @GetMapping("/status/{id}")
    public ResponseEntity<?> getStatus(@PathVariable String id) {
        Project p = projectService.getProject(id);
        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("项目不存在");
        }
        Map<String, Object> res = new HashMap<>();
        res.put("status", p.getStatus());
        res.put("updateTime", p.getUpdateTime());
        res.put("errorMsg", p.getErrorMsg());
        res.put("queuePosition", projectService.getQueuePosition(id));
        return ResponseEntity.ok(res);
    }

    // 查看日志
    @GetMapping(value = "/log/{id}", produces = "application/json")
    public ResponseEntity<?> getLog(
        @PathVariable String id,
        @RequestParam(value = "fromByte", required = false) Long fromByte,
        @RequestParam(value = "download", required = false) String download
    ) {
        Project project = projectService.getProject(id);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("content", "", "totalBytes", 0));
        }
        Path logPath = Paths.get(project.getLogFilePath());
        if (!Files.exists(logPath)) {
            if ("1".equals(download)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("日志文件不存在");
            }
            return ResponseEntity.ok(Map.of("content", "Log file not created yet.", "totalBytes", 0));
        }
        if ("1".equals(download)) {
            try {
                Resource resource = new UrlResource(logPath.toUri());
                String filename = logPath.getFileName().toString();
                String headerValue = String.format("attachment; filename=\"%s\"", filename);
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                    .body(resource);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("下载日志失败: " + e.getMessage());
            }
        }
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(logPath.toFile(), "r")) {
            long totalBytes = raf.length();
            long start = fromByte != null ? fromByte : Math.max(0, totalBytes - 4096); // 默认最多4KB
            if (start > totalBytes) start = totalBytes;
            raf.seek(start);
            byte[] bytes = new byte[(int)(totalBytes - start)];
            int readLen = raf.read(bytes); // 可能为0
            String content = (readLen > 0) ? new String(bytes, 0, readLen, StandardCharsets.UTF_8) : "";
            return ResponseEntity.ok(Map.of("content", content, "totalBytes", totalBytes));
        } catch (Exception e) {
            logger.error("读取日志文件异常", e);
            return ResponseEntity.ok(Map.of("content", "日志正在写入中或被占用，请稍后重试。\n" + e.getMessage(), "totalBytes", 0));
        }
    }
    
    // 终止项目
    @PostMapping("/terminate/{id}")
    public ResponseEntity<?> terminateProject(@PathVariable String id) {
        projectService.terminateProject(id);
        return ResponseEntity.ok().build();
    }

    // 删除项目
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable String id) {
        boolean success = projectService.deleteProject(id);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除项目失败");
        }
    }

    // 批量删除项目
    @DeleteMapping("/batch")
    public ResponseEntity<?> deleteProjects(@RequestBody BatchDeleteRequest request) {
        try {
            projectService.deleteProjects(request.getIds());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("批量删除项目失败: " + e.getMessage());
        }
    }

    // 下载转换结果
    @GetMapping("/download/{id}")
    public ResponseEntity<?> download(@PathVariable String id) {
        Project project = projectService.getProject(id);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("项目不存在");
        }

        Path path = Paths.get(project.getOutputFilePath());
        if (!Files.exists(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("结果文件不存在");
        }

        try {
            Resource resource = new UrlResource(path.toUri());
            String contentType;
            if ("zip".equals(project.getOutputType())) {
                contentType = "application/zip";
            } else {
                contentType = Files.probeContentType(path);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            }

            String filename = path.getFileName().toString();
            String headerValue = String.format("attachment; filename=\"%s\"", filename);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("下载失败: " + e.getMessage());
        }
    }

    // 下载原始输入文件
    @GetMapping("/downloadInput/{id}")
    public ResponseEntity<?> downloadInput(@PathVariable String id) {
        Project project = projectService.getProject(id);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("项目不存在");
        }
        Path path = Paths.get(project.getInputFilePath());
        if (!Files.exists(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("原始文件不存在");
        }
        try {
            Resource resource = new UrlResource(path.toUri());
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            String filename = path.getFileName().toString();
            String headerValue = String.format("attachment; filename=\"%s\"", filename);
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("下载失败: " + e.getMessage());
        }
    }

    @GetMapping("/maxThreads")
    public int getMaxThreads() {
        return projectService.getMaxThreads();
    }

    // 转换明细接口（支持分页、筛选、内容查找）
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getProjectDetail(
        @PathVariable String id,
        @RequestParam(value = "fileName", required = false) String fileName,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "content", required = false) String content,
        @RequestParam(value = "page", required = false, defaultValue = "1") int page,
        @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize
    ) {
        Project project = projectService.getProject(id);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("total", 0, "list", java.util.List.of()));
        }
        java.util.List<Project.FileDetail> details = project.getFileDetails();
        // 文件名筛选
        if (fileName != null && !fileName.isEmpty()) {
            details = details.stream().filter(f -> f.getFileName() != null && f.getFileName().contains(fileName)).collect(java.util.stream.Collectors.toList());
        }
        // 状态筛选
        if (status != null && !status.isEmpty()) {
            details = details.stream().filter(f -> status.equals(f.getStatus())).collect(java.util.stream.Collectors.toList());
        }
        // 内容查找
        if (content != null && !content.isEmpty()) {
            String keyword = content;
            java.util.List<Project.FileDetail> filtered = new java.util.ArrayList<>();
            for (Project.FileDetail f : details) {
                try {
                    String input = f.getInputPath() != null ? java.nio.file.Files.readString(java.nio.file.Paths.get(f.getInputPath())) : "";
                    String output = f.getOutputPath() != null ? java.nio.file.Files.readString(java.nio.file.Paths.get(f.getOutputPath())) : "";
                    boolean matched = (input != null && input.contains(keyword)) || (output != null && output.contains(keyword));
                    System.out.println("[内容查找] 文件: " + f.getFileName() + " 命中: " + matched);
                    if (matched) {
                        filtered.add(f);
                    }
                } catch (Exception e) {
                    System.err.println("[内容查找] 读取文件失败: " + f.getFileName() + ", 错误: " + e.getMessage());
                    // 不加入filtered
                }
            }
            details = filtered;
        }
        int total = details.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(from + pageSize, total);
        java.util.List<Project.FileDetail> pageList = from < to ? details.subList(from, to) : java.util.List.of();
        return ResponseEntity.ok(java.util.Map.of("total", total, "list", pageList));
    }

    // 单文件内容接口
    @GetMapping("/fileContent/{id}")
    public ResponseEntity<?> getFileContent(@PathVariable String id,
                                            @RequestParam("file") String file,
                                            @RequestParam("type") String type) {
        Project project = projectService.getProject(id);
        if (project == null || !StringUtils.hasText(file) || !StringUtils.hasText(type)) {
            return ResponseEntity.badRequest().body("参数错误");
        }
        String reqFile = file.replace('\\', '/').replace('/', '/');
        if (project.getFileDetails() != null) {
            System.out.println("[fileContent] reqFile=" + reqFile);
            for (Project.FileDetail f : project.getFileDetails()) {
                System.out.println("[fileContent] fileDetail.fileName=" + f.getFileName().replace('\\','/'));
            }
        }
        Project.FileDetail detail = project.getFileDetails() == null ? null : project.getFileDetails().stream().filter(f -> f.getFileName().replace('\\','/').equals(reqFile)).findFirst().orElse(null);
        if (detail == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("文件不存在");
        String path = "input".equals(type) ? detail.getInputPath() : detail.getOutputPath();
        if (!Files.exists(Paths.get(path))) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("文件不存在");
        try {
            String content = Files.readString(Paths.get(path), StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("读取文件失败: " + e.getMessage());
        }
    }
}
