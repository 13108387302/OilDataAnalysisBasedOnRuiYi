package com.ruoyi.petrol.domain.dto;

import java.util.List;
import java.util.Map;

public class FileAnalysisResult {

    private List<String> headers;
    private Map<String, ColumnStats> stats;

    public FileAnalysisResult(List<String> headers, Map<String, ColumnStats> stats) {
        this.headers = headers;
        this.stats = stats;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public Map<String, ColumnStats> getStats() {
        return stats;
    }

    public void setStats(Map<String, ColumnStats> stats) {
        this.stats = stats;
    }
} 