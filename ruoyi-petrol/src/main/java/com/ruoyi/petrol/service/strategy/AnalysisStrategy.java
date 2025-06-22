package com.ruoyi.petrol.service.strategy;

import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.petrol.domain.AnalysisTask;

/**
 * 通用分析策略接口
 *
 * @author ruoyi
 */
public interface AnalysisStrategy {

    /**
     * 执行分析策略
     * @param task 分析任务
     * @return 返回结果的JSON字符串
     * @throws Exception 执行过程中的异常
     */
    String execute(AnalysisTask task) throws Exception;

    /**
     * 获取此策略的唯一名称 (例如: "regression_linear_regression_train")
     * 此名称将用作Spring Bean的名称，并用于在工厂中查找策略。
     * @return 策略的唯一名称
     */
    String getStrategyName();
} 