package com.ruoyi.petrol.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 石油预测对象 petrol_prediction
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public class PetrolPrediction extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 预测ID */
    private Long id;

    /** 预测名称 */
    @Excel(name = "预测名称")
    private String predictionName;

    /** 使用的模型ID */
    @Excel(name = "使用的模型ID")
    private Long modelId;

    /** 数据集ID */
    @Excel(name = "数据集ID")
    private Long datasetId;

    /** 预测类型 */
    @Excel(name = "预测类型")
    private String predictionType;

    /** 模型名称 */
    @Excel(name = "模型名称")
    private String modelName;

    /** 输入数据路径 */
    @Excel(name = "输入数据路径")
    private String inputDataPath;

    /** 输出数据路径 */
    @Excel(name = "输出数据路径")
    private String outputDataPath;

    /** 输入数据 */
    private String inputData;

    /** 预测结果 */
    private String predictionResult;

    /** 预测状态 */
    @Excel(name = "预测状态")
    private String status;

    /** 输入文件路径 */
    @Excel(name = "输入文件路径")
    private String inputFilePath;

    /** 结果文件路径 */
    @Excel(name = "结果文件路径")
    private String resultFilePath;

    /** 预测参数 */
    private String predictionParams;

    /** 错误信息 */
    private String errorMessage;

    /** 执行时间(秒) */
    @Excel(name = "执行时间")
    private Long executionTime;

    /** 预测数量 */
    @Excel(name = "预测数量")
    private Integer predictionCount;

    /** 置信度分数 */
    @Excel(name = "置信度分数")
    private java.math.BigDecimal confidenceScore;

    /** 创建者 */
    @Excel(name = "创建者")
    private String createdBy;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setPredictionName(String predictionName) 
    {
        this.predictionName = predictionName;
    }

    public String getPredictionName() 
    {
        return predictionName;
    }
    public void setModelId(Long modelId) 
    {
        this.modelId = modelId;
    }

    public Long getModelId()
    {
        return modelId;
    }

    public void setDatasetId(Long datasetId)
    {
        this.datasetId = datasetId;
    }

    public Long getDatasetId()
    {
        return datasetId;
    }

    public void setPredictionType(String predictionType)
    {
        this.predictionType = predictionType;
    }

    public String getPredictionType()
    {
        return predictionType;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setInputDataPath(String inputDataPath)
    {
        this.inputDataPath = inputDataPath;
    }

    public String getInputDataPath()
    {
        return inputDataPath;
    }

    public void setOutputDataPath(String outputDataPath)
    {
        this.outputDataPath = outputDataPath;
    }

    public String getOutputDataPath()
    {
        return outputDataPath;
    }

    public void setInputData(String inputData)
    {
        this.inputData = inputData;
    }

    public String getInputData() 
    {
        return inputData;
    }
    public void setPredictionResult(String predictionResult) 
    {
        this.predictionResult = predictionResult;
    }

    public String getPredictionResult() 
    {
        return predictionResult;
    }
    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }
    public void setInputFilePath(String inputFilePath) 
    {
        this.inputFilePath = inputFilePath;
    }

    public String getInputFilePath() 
    {
        return inputFilePath;
    }
    public void setResultFilePath(String resultFilePath) 
    {
        this.resultFilePath = resultFilePath;
    }

    public String getResultFilePath() 
    {
        return resultFilePath;
    }
    public void setPredictionParams(String predictionParams) 
    {
        this.predictionParams = predictionParams;
    }

    public String getPredictionParams() 
    {
        return predictionParams;
    }
    public void setErrorMessage(String errorMessage) 
    {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() 
    {
        return errorMessage;
    }
    public void setExecutionTime(Long executionTime) 
    {
        this.executionTime = executionTime;
    }

    public Long getExecutionTime() 
    {
        return executionTime;
    }
    public void setPredictionCount(Integer predictionCount) 
    {
        this.predictionCount = predictionCount;
    }

    public Integer getPredictionCount()
    {
        return predictionCount;
    }

    public void setConfidenceScore(java.math.BigDecimal confidenceScore)
    {
        this.confidenceScore = confidenceScore;
    }

    public java.math.BigDecimal getConfidenceScore()
    {
        return confidenceScore;
    }

    public void setCreatedBy(String createdBy)
    {
        this.createdBy = createdBy;
    }

    public String getCreatedBy() 
    {
        return createdBy;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("predictionName", getPredictionName())
            .append("modelId", getModelId())
            .append("modelName", getModelName())
            .append("inputData", getInputData())
            .append("predictionResult", getPredictionResult())
            .append("status", getStatus())
            .append("inputFilePath", getInputFilePath())
            .append("resultFilePath", getResultFilePath())
            .append("predictionParams", getPredictionParams())
            .append("errorMessage", getErrorMessage())
            .append("executionTime", getExecutionTime())
            .append("predictionCount", getPredictionCount())
            .append("createdBy", getCreatedBy())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
