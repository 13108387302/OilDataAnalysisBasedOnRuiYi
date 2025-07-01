package com.ruoyi.petrol.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分析任务实体类 analysis_task
 * 
 * @author ruoyi
 * @date 2025-06-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pt_analysis_task")
public class AnalysisTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务名称 */
    private String taskName;

    /** 算法名称 */
    private String algorithm;

    /** 任务状态 (QUEUED, RUNNING, COMPLETED, FAILED) */
    private String status;

    /** 任务进度 */
    private Integer progress;

    /** 输入文件路径 */
    private String inputFilePath;

    /** 输入参数（JSON格式） */
    private String inputParamsJson;

    /** 输出目录路径 */
    private String outputDirPath;

    /** 结果 (JSON格式) */
    private String resultsJson;

    /** 错误信息 */
    private String errorMessage;

    /** 原始文件头 (JSON Array) */
    private String inputFileHeadersJson;

    /** 关联数据集ID */
    private Long datasetId;

    /** 数据集名称 */
    private String datasetName;

    /** 部门ID */
    private Long deptId;

    // 手动添加getter/setter方法以确保编译通过
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getInputFilePath() { return inputFilePath; }
    public void setInputFilePath(String inputFilePath) { this.inputFilePath = inputFilePath; }

    public String getInputParamsJson() { return inputParamsJson; }
    public void setInputParamsJson(String inputParamsJson) { this.inputParamsJson = inputParamsJson; }

    public String getOutputDirPath() { return outputDirPath; }
    public void setOutputDirPath(String outputDirPath) { this.outputDirPath = outputDirPath; }

    public String getResultsJson() { return resultsJson; }
    public void setResultsJson(String resultsJson) { this.resultsJson = resultsJson; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getInputFileHeadersJson() { return inputFileHeadersJson; }
    public void setInputFileHeadersJson(String inputFileHeadersJson) { this.inputFileHeadersJson = inputFileHeadersJson; }

    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }

    public String getDatasetName() { return datasetName; }
    public void setDatasetName(String datasetName) { this.datasetName = datasetName; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
}