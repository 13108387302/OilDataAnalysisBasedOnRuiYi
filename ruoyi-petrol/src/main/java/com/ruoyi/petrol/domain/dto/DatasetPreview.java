package com.ruoyi.petrol.domain.dto;

import java.util.List;
import java.util.Map;

/**
 * 数据集预览DTO
 */
public class DatasetPreview {
    
    private String fileName;
    private Integer rowCount;
    private Integer columnCount;
    private List<String> columns;
    private List<Map<String, Object>> data;

    public DatasetPreview() {
    }

    public DatasetPreview(String fileName, Integer rowCount, Integer columnCount, List<String> columns, List<Map<String, Object>> data) {
        this.fileName = fileName;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.columns = columns;
        this.data = data;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Integer getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(Integer columnCount) {
        this.columnCount = columnCount;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }
}
