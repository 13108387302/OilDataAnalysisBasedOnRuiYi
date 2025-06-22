package com.ruoyi.petrol.domain.dto;

import java.util.List;

/**
 * 数据集信息DTO（用于可视化）
 */
public class DatasetInfo {
    
    private String fileName;
    private Integer rowCount;
    private Integer columnCount;
    private List<String> columns;
    private List<String> numericColumns;

    public DatasetInfo() {
    }

    public DatasetInfo(String fileName, Integer rowCount, Integer columnCount, List<String> columns, List<String> numericColumns) {
        this.fileName = fileName;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.columns = columns;
        this.numericColumns = numericColumns;
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

    public List<String> getNumericColumns() {
        return numericColumns;
    }

    public void setNumericColumns(List<String> numericColumns) {
        this.numericColumns = numericColumns;
    }
}
