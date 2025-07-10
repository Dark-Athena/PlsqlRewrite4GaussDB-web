package com.plsqlrewriter.webapp.service;

import com.plsqlrewriter.webapp.model.ParamTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.io.*;

@Service
public class ParamTemplateService {
    private final Map<String, ParamTemplate> templateMap = new ConcurrentHashMap<>();
    private final String persistFile = "data/param_templates.json";

    public ParamTemplateService() {
        loadFromFile();
    }

    public synchronized ParamTemplate create(ParamTemplate t) {
        t.setId(UUID.randomUUID().toString());
        t.setCreateTime(java.time.LocalDateTime.now());
        t.setUpdateTime(java.time.LocalDateTime.now());
        boolean wantDefault = t.isDefault();
        t.setDefault(false);
        templateMap.put(t.getId(), t);
        if (wantDefault) {
            setDefault(t.getId());
        } else {
            saveToFile();
        }
        return templateMap.get(t.getId());
    }

    public synchronized boolean delete(String id) {
        boolean ok = templateMap.remove(id) != null;
        saveToFile();
        return ok;
    }

    public List<ParamTemplate> list(String owner, String group, boolean isAdmin) {
        return templateMap.values().stream().filter(t ->
            isAdmin || (group != null && group.equals(t.getGroup()))
        ).collect(Collectors.toList());
    }

    public synchronized boolean setDefault(String id) {
        boolean found = false;
        for (ParamTemplate t : templateMap.values()) {
            if (t.getId().equals(id)) {
                t.setDefault(true);
                found = true;
            } else {
                t.setDefault(false);
            }
        }
        if (found) saveToFile();
        return found;
    }

    public ParamTemplate getDefault() {
        return templateMap.values().stream().filter(ParamTemplate::isDefault).findFirst().orElse(null);
    }

    private void saveToFile() {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(persistFile), "UTF-8")) {
            w.write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(templateMap.values()));
        } catch (Exception ignore) {}
    }
    private void loadFromFile() {
        File f = new File(persistFile);
        if (!f.exists()) return;
        try (Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8")) {
            ParamTemplate[] arr = new com.fasterxml.jackson.databind.ObjectMapper().readValue(r, ParamTemplate[].class);
            for (ParamTemplate t : arr) templateMap.put(t.getId(), t);
        } catch (Exception ignore) {}
    }
} 