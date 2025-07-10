package com.plsqlrewriter.webapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ParamTemplateControllerTest {
    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testTemplateCrudAndDefault() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("k1", "v1");
        params.put("k2", "v2");
        Map<String, Object> req = new HashMap<>();
        req.put("name", "模板1");
        req.put("owner", "u1");
        req.put("group", "g1");
        req.put("params", params);
        req.put("isDefault", true);
        // 新建模板
        String resp = mockMvc.perform(post("/api/template/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        org.json.JSONObject obj = new org.json.JSONObject(resp);
        String id = obj.getString("id");
        // 查列表
        mockMvc.perform(get("/api/template/list?owner=u1&group=g1&isAdmin=true"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("模板1")));
        // 查默认
        mockMvc.perform(get("/api/template/default"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("模板1")));
        // 取消默认，设为非默认
        req.put("isDefault", false);
        mockMvc.perform(post("/api/template/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk());
        // 设默认
        mockMvc.perform(post("/api/template/setDefault/" + id))
            .andExpect(status().isOk());
        // 删除
        mockMvc.perform(post("/api/template/delete/" + id))
            .andExpect(status().isOk());
    }
} 