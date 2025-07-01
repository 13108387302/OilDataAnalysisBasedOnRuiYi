package com.ruoyi.petrol.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 预测参数解析工具类
 * 用于解析和验证预测参数JSON
 * 
 * @author ruoyi
 * @date 2025-06-24
 */
public class PredictionParameterParser {
    
    private static final Logger logger = LoggerFactory.getLogger(PredictionParameterParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 解析预测参数JSON字符串
     * 
     * @param parametersJson JSON字符串
     * @return 解析后的参数Map
     */
    public static Map<String, Object> parseParameters(String parametersJson) {
        if (parametersJson == null || parametersJson.trim().isEmpty()) {
            return getDefaultParameters();
        }
        
        try {
            Map<String, Object> parameters = objectMapper.readValue(parametersJson, 
                new TypeReference<Map<String, Object>>() {});
            
            // 验证和补充默认值
            return validateAndFillDefaults(parameters);
            
        } catch (Exception e) {
            logger.warn("解析预测参数失败，使用默认参数: {}", e.getMessage());
            return getDefaultParameters();
        }
    }
    
    /**
     * 获取默认预测参数
     * 
     * @return 默认参数Map
     */
    public static Map<String, Object> getDefaultParameters() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("batch_size", 32);
        defaults.put("confidence_threshold", 0.8);
        defaults.put("max_predictions", 1000);
        defaults.put("feature_scaling", true);
        defaults.put("include_probabilities", true);
        defaults.put("output_format", "detailed");
        defaults.put("prediction_mode", "batch");
        defaults.put("enable_uncertainty", false);
        defaults.put("random_seed", 42);
        defaults.put("timeout_seconds", 300);
        defaults.put("save_intermediate", false);
        return defaults;
    }
    
    /**
     * 验证参数并填充默认值
     * 
     * @param parameters 输入参数
     * @return 验证后的参数
     */
    private static Map<String, Object> validateAndFillDefaults(Map<String, Object> parameters) {
        Map<String, Object> result = new HashMap<>(getDefaultParameters());
        
        // 合并用户提供的参数
        if (parameters != null) {
            parameters.forEach((key, value) -> {
                if (isValidParameter(key, value)) {
                    result.put(key, value);
                } else {
                    logger.warn("无效的参数 {}={}, 使用默认值", key, value);
                }
            });
        }
        
        return result;
    }
    
    /**
     * 验证单个参数
     * 
     * @param key 参数名
     * @param value 参数值
     * @return 是否有效
     */
    private static boolean isValidParameter(String key, Object value) {
        if (value == null) {
            return false;
        }
        
        switch (key) {
            case "batch_size":
                return isValidInteger(value, 1, 1024);
            case "confidence_threshold":
                return isValidDouble(value, 0.0, 1.0);
            case "max_predictions":
                return isValidInteger(value, 1, 100000);
            case "feature_scaling":
            case "include_probabilities":
            case "enable_uncertainty":
            case "save_intermediate":
                return value instanceof Boolean;
            case "output_format":
                return value instanceof String && 
                       ("simple".equals(value) || "detailed".equals(value) || "full".equals(value));
            case "prediction_mode":
                return value instanceof String && 
                       ("single".equals(value) || "batch".equals(value) || "streaming".equals(value));
            case "random_seed":
                return isValidInteger(value, 0, 999999);
            case "timeout_seconds":
                return isValidInteger(value, 10, 3600);
            default:
                // 允许其他参数通过，可能是算法特定参数
                return true;
        }
    }
    
    /**
     * 验证整数参数
     */
    private static boolean isValidInteger(Object value, int min, int max) {
        try {
            int intValue;
            if (value instanceof Integer) {
                intValue = (Integer) value;
            } else if (value instanceof Number) {
                intValue = ((Number) value).intValue();
            } else {
                intValue = Integer.parseInt(value.toString());
            }
            return intValue >= min && intValue <= max;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 验证浮点数参数
     */
    private static boolean isValidDouble(Object value, double min, double max) {
        try {
            double doubleValue;
            if (value instanceof Double) {
                doubleValue = (Double) value;
            } else if (value instanceof Number) {
                doubleValue = ((Number) value).doubleValue();
            } else {
                doubleValue = Double.parseDouble(value.toString());
            }
            return doubleValue >= min && doubleValue <= max;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 将参数Map转换为JSON字符串
     * 
     * @param parameters 参数Map
     * @return JSON字符串
     */
    public static String parametersToJson(Map<String, Object> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (Exception e) {
            logger.error("参数转换为JSON失败", e);
            return "{}";
        }
    }
    
    /**
     * 获取算法特定的默认参数
     * 
     * @param algorithmType 算法类型
     * @return 算法特定参数
     */
    public static Map<String, Object> getAlgorithmSpecificDefaults(String algorithmType) {
        Map<String, Object> specific = new HashMap<>();
        
        if (algorithmType == null) {
            return specific;
        }
        
        switch (algorithmType.toLowerCase()) {
            case "regression":
                specific.put("prediction_interval", false);
                specific.put("interval_confidence", 0.95);
                break;
            case "classification":
                specific.put("class_threshold", 0.5);
                specific.put("top_k_classes", 3);
                break;
            case "clustering":
                specific.put("assign_clusters", true);
                specific.put("distance_metric", "euclidean");
                break;
        }
        
        return specific;
    }
    
    /**
     * 合并通用参数和算法特定参数
     * 
     * @param generalParams 通用参数JSON
     * @param algorithmType 算法类型
     * @return 合并后的参数
     */
    public static Map<String, Object> mergeParameters(String generalParams, String algorithmType) {
        Map<String, Object> result = parseParameters(generalParams);
        Map<String, Object> algorithmSpecific = getAlgorithmSpecificDefaults(algorithmType);
        
        // 算法特定参数不会覆盖用户设置的通用参数
        algorithmSpecific.forEach((key, value) -> {
            if (!result.containsKey(key)) {
                result.put(key, value);
            }
        });
        
        return result;
    }
}
