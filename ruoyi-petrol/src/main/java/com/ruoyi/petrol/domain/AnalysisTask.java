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

    /** 用户ID */
    private Long userId;

    /** 部门ID */
    private Long deptId;
} 