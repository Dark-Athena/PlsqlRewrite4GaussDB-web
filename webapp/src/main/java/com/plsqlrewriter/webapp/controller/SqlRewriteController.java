package com.plsqlrewriter.webapp.controller;

import com.plsqlrewriter.core.PlSqlRewriter;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/sql")
public class SqlRewriteController {
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * 重写SQL。接收纯文本SQL，返回纯文本结果。
     * @param sql 待转换的SQL语句
     * @param timeout 超时时间（秒），默认20秒
     * @return 转换后的SQL或错误信息
     */
    @PostMapping(value = "/rewrite", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> rewriteSql(@RequestBody String sql, 
                                           @RequestParam(value = "timeout", defaultValue = "20") int timeout) {
        try {
            MDC.put("file", "api-online");
            
            // 使用Future来实现超时控制
            Future<String> future = executorService.submit(() -> {
                try {
                    return PlSqlRewriter.rewriteSql(sql, "UTF-8", "UTF-8", "api-online");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            try {
                String result = future.get(timeout, TimeUnit.SECONDS);
                return ResponseEntity.ok(result);
            } catch (TimeoutException e) {
                future.cancel(true);
                return ResponseEntity.status(408).body("转换超时，请尝试增加超时时间或简化SQL语句");
            } catch (ExecutionException e) {
                return ResponseEntity.status(500).body(e.getCause().toString());
            }
            
        } catch (Exception e) {
            // 返回详细的异常信息，便于前端调试
            return ResponseEntity.status(500).body(e.toString());
        } finally {
            MDC.remove("file");
        }
    }
} 