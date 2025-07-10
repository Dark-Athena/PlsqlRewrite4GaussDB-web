package com.plsqlrewriter.webapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Disabled
public class ProjectControllerTest {
    static {
        System.setProperty("plsqlweb.maxThreads", "2");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCreateAndListProject() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.sql", "text/plain", "select 1 from dual;".getBytes());
        mockMvc.perform(multipart("/project/create")
                .file(file)
                .param("name", "testProject")
                .param("owner", "user1")
                .param("group", "group1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
        mockMvc.perform(get("/project/list")
                .param("user", "user1")
                .param("group", "group1")
                .param("isAdmin", "false"))
                .andExpect(status().isOk());
    }

    @Test
    public void testZipUploadAndConvert() throws Exception {
        // 构造一个zip包，内含两个sql文件
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream zos = new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(baos)) {
            zos.putArchiveEntry(new org.apache.commons.compress.archivers.zip.ZipArchiveEntry("a.sql"));
            zos.write("select 1 from dual;".getBytes());
            zos.closeArchiveEntry();
            zos.putArchiveEntry(new org.apache.commons.compress.archivers.zip.ZipArchiveEntry("b.sql"));
            zos.write("select 2 from dual;".getBytes());
            zos.closeArchiveEntry();
            zos.finish();
        }
        MockMultipartFile zipFile = new MockMultipartFile("file", "test.zip", "application/zip", baos.toByteArray());
        String resp = mockMvc.perform(multipart("/project/create")
                .file(zipFile)
                .param("name", "zipProject")
                .param("owner", "user2")
                .param("group", "group2"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.json.JSONObject obj = new org.json.JSONObject(resp);
        String id = obj.getString("id");
        // 等待任务完成，轮询状态
        boolean finished = false;
        for (int i = 0; i < 10; i++) {
            String statusResp = mockMvc.perform(get("/project/status/" + id)).andReturn().getResponse().getContentAsString();
            org.json.JSONObject statusObj = new org.json.JSONObject(statusResp);
            String status = statusObj.getString("status");
            if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
                finished = true;
                break;
            }
            Thread.sleep(500);
        }
        org.junit.jupiter.api.Assertions.assertTrue(finished, "任务未在预期时间内完成");
        // 检查日志和结果包，轮询等待日志文件存在
        boolean logExists = false;
        for (int i = 0; i < 15; i++) { // 最多等4.5秒
            int status = mockMvc.perform(get("/project/log/" + id)).andReturn().getResponse().getStatus();
            if (status == 200) { logExists = true; break; }
            Thread.sleep(300);
        }
        org.junit.jupiter.api.Assertions.assertTrue(logExists, "日志文件未及时生成" +
            (logExists ? "" : "\n目录内容: " + java.util.Arrays.toString(new java.io.File("data/" + id).listFiles())));
        mockMvc.perform(get("/project/download/" + id)).andExpect(status().isOk());
    }

    @Test
    public void testQueuePositionAndBusyMessage() throws Exception {
        // 通过接口获取最大并发数
        String maxThreadsStr = mockMvc.perform(get("/project/maxThreads")).andReturn().getResponse().getContentAsString();
        int maxThreads = Integer.parseInt(maxThreadsStr.trim());
        // 提交maxThreads+2个任务
        List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < maxThreads + 2; i++) {
            MockMultipartFile file = new MockMultipartFile("file", "a.sql", "text/plain", "select 1 from dual;".getBytes());
            String resp = mockMvc.perform(multipart("/project/create")
                    .file(file)
                    .param("name", "p" + i)
                    .param("owner", "u1")
                    .param("group", "g1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            org.json.JSONObject obj = new org.json.JSONObject(resp);
            ids.add(obj.getString("id"));
            if (i >= maxThreads) {
                org.junit.jupiter.api.Assertions.assertTrue(obj.has("message"), "应有排队提示");
                org.junit.jupiter.api.Assertions.assertTrue(obj.getInt("queuePosition") > 0, "应有排队序号");
            }
        }
        // 检查状态接口queuePosition
        for (String id : ids) {
            String statusResp = mockMvc.perform(get("/project/status/" + id)).andReturn().getResponse().getContentAsString();
            org.json.JSONObject statusObj = new org.json.JSONObject(statusResp);
            org.junit.jupiter.api.Assertions.assertTrue(statusObj.has("queuePosition"));
        }
    }
} 