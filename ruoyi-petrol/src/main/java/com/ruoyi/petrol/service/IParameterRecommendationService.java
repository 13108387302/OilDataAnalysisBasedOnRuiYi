package com.ruoyi.petrol.service;

import java.util.Map;

/**
 * 参数推荐服务接口
 * 
 * @author ruoyi
 * @date 2025-06-24
 */
public interface IParameterRecommendationService {
    
    /**
     * 基于数据集特征推荐算法参数
     * 
     * @param algorithm 算法名称
     * @param datasetInfo 数据集信息
     * @return 推荐的参数配置
     */
    Map<String, Object> recommendParameters(String algorithm, Map<String, Object> datasetInfo);
    
    /**
     * 获取参数模板
     * 
     * @param algorithm 算法名称
     * @param templateType 模板类型 (fast, balanced, high_accuracy)
     * @return 参数模板
     */
    Map<String, Object> getParameterTemplate(String algorithm, String templateType);
    
    /**
     * 验证参数配置的有效性
     * 
     * @param algorithm 算法名称
     * @param parameters 参数配置
     * @return 验证结果
     */
    Map<String, Object> validateParameters(String algorithm, Map<String, Object> parameters);
}
