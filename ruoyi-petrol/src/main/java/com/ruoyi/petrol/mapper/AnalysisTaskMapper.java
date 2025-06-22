package com.ruoyi.petrol.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.petrol.domain.AnalysisTask;
import java.util.List;

/**
 * 分析任务Mapper接口
 * 
 * @author ruoyi
 * @date 2025-06-18
 */
public interface AnalysisTaskMapper extends BaseMapper<AnalysisTask>
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
     * 批量删除分析任务
     *
     * @param ids 需要删除的任务ID集合
     * @return 结果
     */
    public int deleteAnalysisTaskByIds(Long[] ids);

    /**
     * 删除分析任务
     *
     * @param id 需要删除的任务ID
     * @return 结果
     */
    public int deleteAnalysisTaskById(Long id);
} 