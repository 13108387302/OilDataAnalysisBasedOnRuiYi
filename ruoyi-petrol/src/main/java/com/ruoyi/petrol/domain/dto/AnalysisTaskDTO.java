package com.ruoyi.petrol.domain.dto;

import lombok.Data;

/**
 * 用于接收前端提交任务的表单数据
 */
@Data
public class AnalysisTaskDTO {
    private String taskName;
    private String taskType;
    private String algorithm;
    private String inputParamsJson;
} 