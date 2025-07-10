package com.plsqlrewriter.webapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
public class SqlRewriteControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testRewriteSql() throws Exception {
        String sql = "select * from dual;";
        mockMvc.perform(post("/api/sql/rewrite")
                .contentType(MediaType.TEXT_PLAIN)
                .content(sql))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.notNullValue()));
    }
} 