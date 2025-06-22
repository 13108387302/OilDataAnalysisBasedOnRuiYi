package com.ruoyi.petrol.service;

import java.util.Map;

/**
 * 可视化增强服务接口
 * 为前端高级可视化提供增强的数据支持
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface IVisualizationEnhancementService {

    /**
     * 获取任务的增强可视化数据
     * 
     * @param taskId 任务ID
     * @return 增强的可视化数据
     */
    Map<String, Object> getEnhancedVisualizationData(Long taskId);

    /**
     * 获取算法类型的可视化配置
     * 
     * @param algorithmType 算法类型
     * @return 可视化配置信息
     */
    Map<String, Object> getVisualizationConfig(String algorithmType);

    /**
     * 生成特定类型的可视化数据
     * 
     * @param taskId 任务ID
     * @param visualizationType 可视化类型
     * @param params 参数
     * @return 生成的可视化数据
     */
    Map<String, Object> generateSpecificVisualization(Long taskId, String visualizationType, Map<String, Object> params);

    /**
     * 获取模型性能对比数据
     * 
     * @param taskIds 任务ID列表
     * @return 模型性能对比数据
     */
    Map<String, Object> getModelPerformanceComparison(Long[] taskIds);

    /**
     * 获取算法结果的统计摘要
     * 
     * @param taskId 任务ID
     * @return 统计摘要数据
     */
    Map<String, Object> getResultSummary(Long taskId);

    /**
     * 导出可视化数据
     * 
     * @param taskId 任务ID
     * @param format 导出格式
     * @return 导出结果
     */
    Map<String, Object> exportVisualizationData(Long taskId, String format);

    /**
     * 获取实时训练进度数据
     * 
     * @param taskId 任务ID
     * @return 训练进度数据
     */
    Map<String, Object> getTrainingProgress(Long taskId);

    /**
     * 获取特征重要性分析数据
     * 
     * @param taskId 任务ID
     * @return 特征重要性数据
     */
    Map<String, Object> getFeatureImportanceData(Long taskId);

    /**
     * 获取预测置信度数据
     * 
     * @param taskId 任务ID
     * @return 预测置信度数据
     */
    Map<String, Object> getPredictionConfidenceData(Long taskId);

    /**
     * 获取算法性能基准对比
     * 
     * @param algorithmType 算法类型
     * @param datasetSize 数据集大小
     * @return 性能基准数据
     */
    Map<String, Object> getAlgorithmBenchmark(String algorithmType, Integer datasetSize);
}
