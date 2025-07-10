package com.plsqlrewriter.webapp.controller;

import com.plsqlrewriter.webapp.model.ParamTemplate;
import com.plsqlrewriter.webapp.service.ParamTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/api/template")
public class ParamTemplateController {
    @Autowired
    private ParamTemplateService service;

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> create(@RequestBody ParamTemplate t) {
        return ResponseEntity.ok(service.create(t));
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> list(@RequestParam String owner, @RequestParam String group, @RequestParam boolean isAdmin) {
        return ResponseEntity.ok(service.list(owner, group, isAdmin));
    }

    @PostMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> delete(@PathVariable String id) {
        return ResponseEntity.ok(service.delete(id));
    }

    @PostMapping(value = "/setDefault/{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> setDefault(@PathVariable String id) {
        return ResponseEntity.ok(service.setDefault(id));
    }

    @GetMapping(value = "/default", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> getDefault() {
        ParamTemplate t = service.getDefault();
        if (t == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("无默认模板");
        return ResponseEntity.ok(t);
    }
} 