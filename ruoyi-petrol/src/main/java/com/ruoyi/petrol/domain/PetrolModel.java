package com.ruoyi.petrol.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 石油模型对象 petrol_model
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public class PetrolModel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模型ID */
    private Long id;

    /** 模型名称 */
    @Excel(name = "模型名称")
    private String modelName;

    /** 模型类型 */
    @Excel(name = "模型类型")
    private String modelType;

    /** 算法类型 */
    @Excel(name = "算法类型")
    private String algorithm;

    /** 模型文件路径 */
    @Excel(name = "模型文件路径")
    private String modelPath;

    /** 模型描述 */
    @Excel(name = "模型描述")
    private String description;

    /** 输入特征 */
    @Excel(name = "输入特征")
    private String inputFeatures;

    /** 输出目标 */
    @Excel(name = "输出目标")
    private String outputTarget;

    /** 模型参数 */
    private String modelParams;

    /** 训练指标 */
    private String trainingMetrics;

    /** 来源任务ID */
    @Excel(name = "来源任务ID")
    private Long sourceTaskId;

    /** 模型状态 */
    @Excel(name = "模型状态")
    private String status;

    /** 是否可用 */
    @Excel(name = "是否可用")
    private String isAvailable;

    /** 文件大小 */
    @Excel(name = "文件大小")
    private Long fileSize;

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
    public void setModelName(String modelName) 
    {
        this.modelName = modelName;
    }

    public String getModelName() 
    {
        return modelName;
    }
    public void setModelType(String modelType) 
    {
        this.modelType = modelType;
    }

    public String getModelType() 
    {
        return modelType;
    }
    public void setAlgorithm(String algorithm) 
    {
        this.algorithm = algorithm;
    }

    public String getAlgorithm() 
    {
        return algorithm;
    }
    public void setModelPath(String modelPath) 
    {
        this.modelPath = modelPath;
    }

    public String getModelPath() 
    {
        return modelPath;
    }
    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }
    public void setInputFeatures(String inputFeatures) 
    {
        this.inputFeatures = inputFeatures;
    }

    public String getInputFeatures() 
    {
        return inputFeatures;
    }
    public void setOutputTarget(String outputTarget) 
    {
        this.outputTarget = outputTarget;
    }

    public String getOutputTarget() 
    {
        return outputTarget;
    }
    public void setModelParams(String modelParams) 
    {
        this.modelParams = modelParams;
    }

    public String getModelParams() 
    {
        return modelParams;
    }
    public void setTrainingMetrics(String trainingMetrics) 
    {
        this.trainingMetrics = trainingMetrics;
    }

    public String getTrainingMetrics() 
    {
        return trainingMetrics;
    }
    public void setSourceTaskId(Long sourceTaskId) 
    {
        this.sourceTaskId = sourceTaskId;
    }

    public Long getSourceTaskId() 
    {
        return sourceTaskId;
    }
    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }
    public void setIsAvailable(String isAvailable) 
    {
        this.isAvailable = isAvailable;
    }

    public String getIsAvailable() 
    {
        return isAvailable;
    }
    public void setFileSize(Long fileSize) 
    {
        this.fileSize = fileSize;
    }

    public Long getFileSize() 
    {
        return fileSize;
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
            .append("modelName", getModelName())
            .append("modelType", getModelType())
            .append("algorithm", getAlgorithm())
            .append("modelPath", getModelPath())
            .append("description", getDescription())
            .append("inputFeatures", getInputFeatures())
            .append("outputTarget", getOutputTarget())
            .append("modelParams", getModelParams())
            .append("trainingMetrics", getTrainingMetrics())
            .append("sourceTaskId", getSourceTaskId())
            .append("status", getStatus())
            .append("isAvailable", getIsAvailable())
            .append("fileSize", getFileSize())
            .append("createdBy", getCreatedBy())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
