package com.ruoyi.petrol.service;

import com.ruoyi.petrol.domain.AnalysisTask;

/**
 * 分析管理器服务接口
 *
 * @author ruoyi
 */
public interface IAnalysisManagerService {

    /**
     * 异步处理分析任务
     * @param task 包含所有任务信息的实体
     */
    void processTask(AnalysisTask task);
} 