package com.ruoyi.petrol.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 数据集管理对象 petrol_dataset
 * 
 * @author ruoyi
 * @date 2025-06-29
 */
public class PetrolDataset extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 数据集ID */
    private Long id;

    /** 数据集名称 */
    @Excel(name = "数据集名称")
    private String datasetName;

    /** 数据集描述 */
    @Excel(name = "数据集描述")
    private String datasetDescription;

    /** 原始文件名 */
    @Excel(name = "原始文件名")
    private String fileName;

    /** 文件存储路径 */
    @Excel(name = "文件存储路径")
    private String filePath;

    /** 文件大小（字节） */
    @Excel(name = "文件大小", readConverterExp = "字=节")
    private Long fileSize;

    /** 文件类型（CSV/XLSX） */
    @Excel(name = "文件类型", readConverterExp = "C=SV/XLSX")
    private String fileType;

    /** 总行数 */
    @Excel(name = "总行数")
    private Long totalRows;

    /** 总列数 */
    @Excel(name = "总列数")
    private Long totalColumns;

    /** 列信息（名称、类型、统计） */
    @Excel(name = "列信息")
    private String columnInfo;

    // /** 缺失值数量 */
    // @Excel(name = "缺失值数量")
    // private Long missingValuesCount;

    // /** 重复行数量 */
    // @Excel(name = "重复行数量")
    // private Long duplicateRowsCount;

    /** 数据质量评分 */
    @Excel(name = "数据质量评分")
    private BigDecimal dataQualityScore;

    /** 数据集类别（测井、地震、生产等） */
    @Excel(name = "数据集类别", readConverterExp = "测=井、地震、生产等")
    private String datasetCategory;

    // /** 井数量 */
    // @Excel(name = "井数量")
    // private Long wellCount;

    // /** 深度范围 */
    // @Excel(name = "深度范围")
    // private String depthRange;

    /** 状态（ACTIVE/INACTIVE/PROCESSING） */
    @Excel(name = "状态", readConverterExp = "A=CTIVE/INACTIVE/PROCESSING")
    private String status;

    /** 是否公开（0=否，1=是） */
    @Excel(name = "是否公开", readConverterExp = "0=否,1=是")
    private Integer isPublic;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setDatasetName(String datasetName) 
    {
        this.datasetName = datasetName;
    }

    public String getDatasetName() 
    {
        return datasetName;
    }
    public void setDatasetDescription(String datasetDescription) 
    {
        this.datasetDescription = datasetDescription;
    }

    public String getDatasetDescription() 
    {
        return datasetDescription;
    }
    public void setFileName(String fileName) 
    {
        this.fileName = fileName;
    }

    public String getFileName() 
    {
        return fileName;
    }
    public void setFilePath(String filePath) 
    {
        this.filePath = filePath;
    }

    public String getFilePath() 
    {
        return filePath;
    }
    public void setFileSize(Long fileSize) 
    {
        this.fileSize = fileSize;
    }

    public Long getFileSize() 
    {
        return fileSize;
    }
    public void setFileType(String fileType) 
    {
        this.fileType = fileType;
    }

    public String getFileType() 
    {
        return fileType;
    }
    public void setTotalRows(Long totalRows) 
    {
        this.totalRows = totalRows;
    }

    public Long getTotalRows() 
    {
        return totalRows;
    }
    public void setTotalColumns(Long totalColumns) 
    {
        this.totalColumns = totalColumns;
    }

    public Long getTotalColumns() 
    {
        return totalColumns;
    }
    public void setColumnInfo(String columnInfo) 
    {
        this.columnInfo = columnInfo;
    }

    public String getColumnInfo() 
    {
        return columnInfo;
    }
    // public void setMissingValuesCount(Long missingValuesCount)
    // {
    //     this.missingValuesCount = missingValuesCount;
    // }

    // public Long getMissingValuesCount()
    // {
    //     return missingValuesCount;
    // }
    // public void setDuplicateRowsCount(Long duplicateRowsCount)
    // {
    //     this.duplicateRowsCount = duplicateRowsCount;
    // }

    // public Long getDuplicateRowsCount()
    // {
    //     return duplicateRowsCount;
    // }
    public void setDataQualityScore(BigDecimal dataQualityScore) 
    {
        this.dataQualityScore = dataQualityScore;
    }

    public BigDecimal getDataQualityScore() 
    {
        return dataQualityScore;
    }
    public void setDatasetCategory(String datasetCategory) 
    {
        this.datasetCategory = datasetCategory;
    }

    public String getDatasetCategory() 
    {
        return datasetCategory;
    }
    // public void setWellCount(Long wellCount)
    // {
    //     this.wellCount = wellCount;
    // }

    // public Long getWellCount()
    // {
    //     return wellCount;
    // }
    // public void setDepthRange(String depthRange)
    // {
    //     this.depthRange = depthRange;
    // }

    // public String getDepthRange()
    // {
    //     return depthRange;
    // }
    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }
    public void setIsPublic(Integer isPublic)
    {
        this.isPublic = isPublic;
    }

    public Integer getIsPublic()
    {
        return isPublic;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("datasetName", getDatasetName())
            .append("datasetDescription", getDatasetDescription())
            .append("fileName", getFileName())
            .append("filePath", getFilePath())
            .append("fileSize", getFileSize())
            .append("fileType", getFileType())
            .append("totalRows", getTotalRows())
            .append("totalColumns", getTotalColumns())
            .append("columnInfo", getColumnInfo())
            .append("dataQualityScore", getDataQualityScore())
            .append("datasetCategory", getDatasetCategory())
            .append("status", getStatus())
            .append("isPublic", getIsPublic())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
