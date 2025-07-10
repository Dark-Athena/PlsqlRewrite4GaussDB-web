package com.plsqlrewriter.webapp.model;

import java.time.LocalDateTime;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ParamTemplate {
    private String id;
    private String name;
    private String owner;
    private String group;
    private Map<String, String> params;
    @JsonProperty("isDefault")
    private boolean isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // getter/setter 省略，可用lombok后续补全
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    public Map<String, String> getParams() { return params; }
    public void setParams(Map<String, String> params) { this.params = params; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
} 