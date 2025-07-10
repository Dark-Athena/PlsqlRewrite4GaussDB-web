package com.plsqlrewriter.webapp.model.request;

import java.util.List;
import lombok.Data;

@Data
public class BatchDeleteRequest {
    private List<String> ids;
} 