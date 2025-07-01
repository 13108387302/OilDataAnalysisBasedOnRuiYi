package com.ruoyi.petrol.service;

import com.ruoyi.petrol.domain.AnalysisTask;
import com.ruoyi.petrol.domain.dto.AnalysisTaskUpdateDTO;

import java.util.List;

/**
 * 分析任务Service接口
 * 
 * @author ruoyi
 * @date 2025-06-18
 */
public interface IAnalysisTaskService 
{
    /**
     * 查询分析任务
     * 
     * @param id 分析任务主键
     * @return 分析任务
     */
    public AnalysisTask selectAnalysisTaskById(Long id);

    /**
     * 查询分析任务列表
     * 
     * @param analysisTask 分析任务
     * @return 分析任务集合
     */
    public List<AnalysisTask> selectAnalysisTaskList(AnalysisTask analysisTask);

    /**
     * 新增分析任务
     * 
     * @param analysisTask 分析任务
     * @return 结果
     */
    public int insertAnalysisTask(AnalysisTask analysisTask);

    /**
     * 修改分析任务
     * 
     * @param analysisTask 分析任务
     * @return 结果
     */
    public int updateAnalysisTask(AnalysisTask analysisTask);

    /**
     * 更新任务参数
     * @param analysisTaskUpdateDTO 任务对象，至少包含ID和inputParamsJson
     * @return 结果
     */
    public int updateTaskParams(AnalysisTaskUpdateDTO analysisTaskUpdateDTO);

    /**
     * 批量删除分析任务
     * 
     * @param ids 需要删除的分析任务主键集合
     * @return 结果
     */
    public int deleteAnalysisTaskByIds(Long[] ids);

    /**
     * 删除分析任务信息
     * 
     * @param id 分析任务主键
     * @return 结果
     */
    public int deleteAnalysisTaskById(Long id);

    /**
     * 提交一个新的分析任务，并将其存入数据库
     * @param analysisTask
     * @return 返回插入数据库后的任务对象（包含ID）
     */
    AnalysisTask submitAnalysisTask(AnalysisTask analysisTask);


} 