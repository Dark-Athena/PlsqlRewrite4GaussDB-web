package com.plsqlrewriter.webapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.plsqlrewriter.webapp.model.UserGroup;
import jakarta.persistence.Lob;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Data
@Entity
public class Project {
    public enum Status {
        QUEUED, RUNNING, SUCCESS, FAILED, TERMINATED
    }

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String name;
    private String owner;
    @Column(name = "`group`")
    private String group;
    
    @Convert(converter = StringMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> params;

    private String inputFilePath;
    private String outputFilePath;
    private String logFilePath;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime finishTime;

    @Column(columnDefinition = "TEXT")
    private String errorMsg;
    
    private String outputType; // "zip" or "single"
    private String inputCharset; // 输入文件字符编码
    private String outputCharset; // 输出文件字符编码

    @ManyToOne
    @JoinColumn(name = "user_group_id")
    private UserGroup userGroup;

    // 转换明细
    public static class FileDetail {
        private String fileName;
        private String status; // success/failed
        private String error;
        private String inputPath;
        private String outputPath;
        // getter/setter
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getInputPath() { return inputPath; }
        public void setInputPath(String inputPath) { this.inputPath = inputPath; }
        public String getOutputPath() { return outputPath; }
        public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
    }
    @Lob
    private String fileDetailsJson;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private transient List<FileDetail> fileDetails;
    public List<FileDetail> getFileDetails() {
        if (fileDetails != null) return fileDetails;
        if (fileDetailsJson != null && !fileDetailsJson.isEmpty()) {
            try {
                fileDetails = objectMapper.readValue(fileDetailsJson, new TypeReference<List<FileDetail>>(){});
            } catch (Exception ignored) {}
        }
        return fileDetails;
    }
    public void setFileDetails(List<FileDetail> fileDetails) {
        this.fileDetails = fileDetails;
        try {
            this.fileDetailsJson = (fileDetails == null) ? null : objectMapper.writeValueAsString(fileDetails);
        } catch (Exception ignored) {}
    }

    // All getters and setters are now handled by @Data
} 