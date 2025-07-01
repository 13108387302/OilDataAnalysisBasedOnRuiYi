package com.ruoyi.petrol.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class ColumnStats {
    private Double min;
    private Double max;
    private Double mean;
    private Double std;
    private Integer count;
    private Integer nullCount;
    private Integer uniqueCount;
    private List<String> sampleValues;
    private String dtype;

    public ColumnStats() {
        this.count = 0;
        this.nullCount = 0;
        this.uniqueCount = 0;
        this.sampleValues = new ArrayList<>();
        this.dtype = "object";
    }

    public ColumnStats(Double min, Double max) {
        this();
        this.min = min;
        this.max = max;
    }

    // Getters and Setters
    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Double getMean() {
        return mean;
    }

    public void setMean(Double mean) {
        this.mean = mean;
    }

    public Double getStd() {
        return std;
    }

    public void setStd(Double std) {
        this.std = std;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getNullCount() {
        return nullCount;
    }

    public void setNullCount(Integer nullCount) {
        this.nullCount = nullCount;
    }

    public Integer getUniqueCount() {
        return uniqueCount;
    }

    public void setUniqueCount(Integer uniqueCount) {
        this.uniqueCount = uniqueCount;
    }

    public List<String> getSampleValues() {
        return sampleValues;
    }

    public void setSampleValues(List<String> sampleValues) {
        this.sampleValues = sampleValues;
    }

    public String getDtype() {
        return dtype;
    }

    public void setDtype(String dtype) {
        this.dtype = dtype;
    }
}