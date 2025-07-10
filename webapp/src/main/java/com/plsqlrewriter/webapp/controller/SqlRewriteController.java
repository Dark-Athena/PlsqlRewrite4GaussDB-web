package com.plsqlrewriter.webapp.controller;

import com.plsqlrewriter.core.PlSqlRewriter;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/api/sql")
public class SqlRewriteController {
    /**
     * 重写SQL。接收纯文本SQL，返回纯文本结果。
     * @param sql 待转换的SQL语句
     * @return 转换后的SQL或错误信息
     */
    @PostMapping(value = "/rewrite", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> rewriteSql(@RequestBody String sql) {
        try {
            MDC.put("file", "api-online");
            // 直接调用，不再使用反射
            String result = PlSqlRewriter.rewriteSql(sql, "UTF-8", "UTF-8", "api-online");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // 返回详细的异常信息，便于前端调试
            return ResponseEntity.status(500).body(e.toString());
        } finally {
            MDC.remove("file");
        }
    }
} 