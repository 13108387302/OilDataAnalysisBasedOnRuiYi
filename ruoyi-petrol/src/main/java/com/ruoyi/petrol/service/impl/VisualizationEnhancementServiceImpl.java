package com.ruoyi.petrol.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.petrol.domain.AnalysisTask;
import com.ruoyi.petrol.service.IAnalysisTaskService;
import com.ruoyi.petrol.service.IVisualizationEnhancementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 可视化增强服务实现类
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@Service
public class VisualizationEnhancementServiceImpl implements IVisualizationEnhancementService {

    private static final Logger logger = LoggerFactory.getLogger(VisualizationEnhancementServiceImpl.class);

    @Autowired
    private IAnalysisTaskService analysisTaskService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> getEnhancedVisualizationData(Long taskId) {
        try {
            AnalysisTask task = analysisTaskService.selectAnalysisTaskById(taskId);
            if (task == null) {
                throw new RuntimeException("任务不存在: " + taskId);
            }

            // 读取任务结果文件
            Map<String, Object> resultData = readTaskResultFile(task);
            
            // 增强数据
            Map<String, Object> enhancedData = new HashMap<>();
            enhancedData.put("taskInfo", buildTaskInfo(task));
            enhancedData.put("algorithmType", task.getAlgorithm());
            enhancedData.put("algorithmCategory", determineAlgorithmCategory(task.getAlgorithm()));
            enhancedData.put("results", resultData);
            enhancedData.put("visualizationConfig", getVisualizationConfig(task.getAlgorithm()));
            enhancedData.put("performanceMetrics", extractPerformanceMetrics(resultData));
            enhancedData.put("modelParameters", extractModelParameters(resultData));
            
            return enhancedData;
        } catch (Exception e) {
            logger.error("获取增强可视化数据失败", e);
            throw new RuntimeException("获取增强可视化数据失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getVisualizationConfig(String algorithmType) {
        Map<String, Object> config = new HashMap<>();
        
        String category = determineAlgorithmCategory(algorithmType);
        config.put("category", category);
        config.put("supportedCharts", getSupportedCharts(category));
        config.put("requiredMetrics", getRequiredMetrics(category));
        config.put("optionalFeatures", getOptionalFeatures(category));
        config.put("colorScheme", getColorScheme(category));
        
        return config;
    }

    @Override
    public Map<String, Object> generateSpecificVisualization(Long taskId, String visualizationType, Map<String, Object> params) {
        try {
            AnalysisTask task = analysisTaskService.selectAnalysisTaskById(taskId);
            Map<String, Object> resultData = readTaskResultFile(task);
            
            Map<String, Object> visualizationData = new HashMap<>();
            
            switch (visualizationType) {
                case "confusion_matrix":
                    visualizationData = generateConfusionMatrixData(resultData);
                    break;
                case "roc_curve":
                    visualizationData = generateROCCurveData(resultData);
                    break;
                case "feature_importance":
                    visualizationData = generateFeatureImportanceData(resultData);
                    break;
                case "learning_curve":
                    visualizationData = generateLearningCurveData(resultData);
                    break;
                case "prediction_distribution":
                    visualizationData = generatePredictionDistributionData(resultData);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的可视化类型: " + visualizationType);
            }
            
            return visualizationData;
        } catch (Exception e) {
            logger.error("生成特定可视化数据失败", e);
            throw new RuntimeException("生成可视化数据失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getModelPerformanceComparison(Long[] taskIds) {
        try {
            List<Map<String, Object>> modelPerformances = new ArrayList<>();
            
            for (Long taskId : taskIds) {
                AnalysisTask task = analysisTaskService.selectAnalysisTaskById(taskId);
                if (task != null) {
                    Map<String, Object> resultData = readTaskResultFile(task);
                    Map<String, Object> performance = new HashMap<>();
                    performance.put("taskId", taskId);
                    performance.put("algorithmType", task.getAlgorithm());
                    performance.put("metrics", extractPerformanceMetrics(resultData));
                    performance.put("executionTime", calculateExecutionTime(task));
                    performance.put("status", task.getStatus());
                    modelPerformances.add(performance);
                }
            }
            
            Map<String, Object> comparison = new HashMap<>();
            comparison.put("models", modelPerformances);
            comparison.put("bestModel", findBestModel(modelPerformances));
            comparison.put("comparisonCharts", generateComparisonCharts(modelPerformances));
            
            return comparison;
        } catch (Exception e) {
            logger.error("获取模型性能对比数据失败", e);
            throw new RuntimeException("获取性能对比数据失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getResultSummary(Long taskId) {
        try {
            AnalysisTask task = analysisTaskService.selectAnalysisTaskById(taskId);
            Map<String, Object> resultData = readTaskResultFile(task);
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("taskInfo", buildTaskInfo(task));
            summary.put("algorithmType", task.getAlgorithm());
            summary.put("performanceLevel", determinePerformanceLevel(resultData));
            summary.put("keyMetrics", extractKeyMetrics(resultData));
            summary.put("dataQuality", assessDataQuality(resultData));
            summary.put("recommendations", generateRecommendations(task.getAlgorithm(), resultData));
            
            return summary;
        } catch (Exception e) {
            logger.error("获取算法结果统计摘要失败", e);
            throw new RuntimeException("获取统计摘要失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> exportVisualizationData(Long taskId, String format) {
        try {
            Map<String, Object> enhancedData = getEnhancedVisualizationData(taskId);
            
            String fileName = "visualization_data_" + taskId + "_" + System.currentTimeMillis();
            String filePath;
            
            switch (format.toLowerCase()) {
                case "json":
                    filePath = exportToJson(enhancedData, fileName);
                    break;
                case "csv":
                    filePath = exportToCsv(enhancedData, fileName);
                    break;
                case "excel":
                    filePath = exportToExcel(enhancedData, fileName);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的导出格式: " + format);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", fileName + "." + format);
            result.put("filePath", filePath);
            result.put("downloadUrl", "/download/" + fileName + "." + format);
            
            return result;
        } catch (Exception e) {
            logger.error("导出可视化数据失败", e);
            throw new RuntimeException("导出数据失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getTrainingProgress(Long taskId) {
        try {
            AnalysisTask task = analysisTaskService.selectAnalysisTaskById(taskId);
            Map<String, Object> resultData = readTaskResultFile(task);
            
            Map<String, Object> progressData = new HashMap<>();
            
            // 从结果中提取训练历史
            Object trainingHistory = resultData.get("training_history");
            if (trainingHistory != null) {
                progressData.put("trainingHistory", trainingHistory);
                progressData.put("hasTrainingData", true);
            } else {
                progressData.put("hasTrainingData", false);
                progressData.put("message", "该算法不包含训练历史数据");
            }
            
            return progressData;
        } catch (Exception e) {
            logger.error("获取训练进度数据失败", e);
            throw new RuntimeException("获取训练进度失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getFeatureImportanceData(Long taskId) {
        try {
            AnalysisTask task = analysisTaskService.selectAnalysisTaskById(taskId);
            Map<String, Object> resultData = readTaskResultFile(task);
            
            Map<String, Object> importanceData = new HashMap<>();
            
            Object featureImportance = resultData.get("feature_importance");
            if (featureImportance != null) {
                importanceData.put("featureImportance", featureImportance);
                importanceData.put("hasFeatureImportance", true);
                importanceData.put("topFeatures", extractTopFeatures(featureImportance));
            } else {
                importanceData.put("hasFeatureImportance", false);
                importanceData.put("message", "该算法不支持特征重要性分析");
            }
            
            return importanceData;
        } catch (Exception e) {
            logger.error("获取特征重要性数据失败", e);
            throw new RuntimeException("获取特征重要性失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getPredictionConfidenceData(Long taskId) {
        try {
            AnalysisTask task = analysisTaskService.selectAnalysisTaskById(taskId);
            Map<String, Object> resultData = readTaskResultFile(task);
            
            Map<String, Object> confidenceData = new HashMap<>();
            
            Object confidences = resultData.get("confidences");
            if (confidences != null) {
                confidenceData.put("confidences", confidences);
                confidenceData.put("hasConfidenceData", true);
                confidenceData.put("averageConfidence", calculateAverageConfidence(confidences));
                confidenceData.put("confidenceDistribution", analyzeConfidenceDistribution(confidences));
            } else {
                confidenceData.put("hasConfidenceData", false);
                confidenceData.put("message", "该算法不提供预测置信度数据");
            }
            
            return confidenceData;
        } catch (Exception e) {
            logger.error("获取预测置信度数据失败", e);
            throw new RuntimeException("获取置信度数据失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getAlgorithmBenchmark(String algorithmType, Integer datasetSize) {
        // 这里可以实现算法性能基准对比
        // 可以从历史数据中统计同类算法的平均性能
        Map<String, Object> benchmark = new HashMap<>();
        benchmark.put("algorithmType", algorithmType);
        benchmark.put("averagePerformance", getAveragePerformance(algorithmType));
        benchmark.put("performanceRange", getPerformanceRange(algorithmType));
        benchmark.put("recommendedParameters", getRecommendedParameters(algorithmType));
        
        return benchmark;
    }

    // 私有辅助方法
    private Map<String, Object> readTaskResultFile(AnalysisTask task) {
        try {
            // 首先尝试从数据库中读取结果JSON
            String resultsJson = task.getResultsJson();
            if (resultsJson != null && !resultsJson.trim().isEmpty()) {
                logger.info("从数据库读取任务结果: {}", task.getId());
                return objectMapper.readValue(resultsJson, new TypeReference<Map<String, Object>>() {});
            }

            // 如果数据库中没有，尝试从文件读取
            String outputDir = task.getOutputDirPath();
            if (outputDir != null) {
                Path resultPath = Paths.get(outputDir, "result.json");

                if (Files.exists(resultPath)) {
                    logger.info("从文件读取任务结果: {}", resultPath);
                    String content = new String(Files.readAllBytes(resultPath));
                    return objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
                }
            }

            // 如果都没有找到，返回空结果
            logger.warn("任务 {} 没有找到结果数据", task.getId());
            return new HashMap<>();

        } catch (Exception e) {
            logger.error("读取任务结果失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private Map<String, Object> buildTaskInfo(AnalysisTask task) {
        Map<String, Object> taskInfo = new HashMap<>();
        taskInfo.put("id", task.getId());
        taskInfo.put("name", task.getTaskName());
        taskInfo.put("algorithm", task.getAlgorithm());
        taskInfo.put("status", task.getStatus());
        taskInfo.put("createTime", task.getCreateTime());
        taskInfo.put("executionTime", calculateExecutionTime(task));
        return taskInfo;
    }

    private String determineAlgorithmCategory(String algorithmType) {
        if (algorithmType.contains("regression") && !algorithmType.contains("predict")) {
            return "regression";
        } else if (algorithmType.contains("classification") || algorithmType.contains("logistic")) {
            return "classification";
        } else if (algorithmType.contains("clustering") || algorithmType.contains("kmeans")) {
            return "clustering";
        } else if (algorithmType.contains("fractal_dimension") || algorithmType.contains("automatic_regression")) {
            return "feature_engineering";
        } else if (algorithmType.contains("bilstm") || algorithmType.contains("tcn") || algorithmType.contains("lstm")) {
            return "time_series";
        } else if (algorithmType.contains("predict")) {
            return "prediction";
        }
        return "unknown";
    }

    // 其他辅助方法的实现...
    private List<String> getSupportedCharts(String category) {
        // 根据算法类别返回支持的图表类型
        return Arrays.asList("scatter", "line", "bar", "heatmap");
    }

    private List<String> getRequiredMetrics(String category) {
        // 根据算法类别返回必需的指标
        return Arrays.asList("accuracy", "precision", "recall");
    }

    private List<String> getOptionalFeatures(String category) {
        // 根据算法类别返回可选功能
        return Arrays.asList("feature_importance", "confusion_matrix");
    }

    private Map<String, String> getColorScheme(String category) {
        // 根据算法类别返回颜色方案
        Map<String, String> colors = new HashMap<>();
        colors.put("primary", "#409EFF");
        colors.put("secondary", "#67C23A");
        return colors;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPerformanceMetrics(Map<String, Object> resultData) {
        Map<String, Object> metrics = new HashMap<>();

        // 尝试从多个可能的字段中提取统计数据
        Object statistics = resultData.get("statistics");
        Object metricsObj = resultData.get("metrics");

        // 优先使用statistics字段
        if (statistics instanceof Map) {
            metrics.putAll((Map<String, Object>) statistics);
        } else if (metricsObj instanceof Map) {
            metrics.putAll((Map<String, Object>) metricsObj);
        }

        // 标准化字段名称，确保前端能正确识别
        Map<String, Object> normalizedMetrics = new HashMap<>();

        // 处理R²决定系数
        Object r2 = metrics.get("r2_score");
        if (r2 == null) r2 = metrics.get("r2");
        if (r2 == null) r2 = metrics.get("R²决定系数");
        if (r2 != null) normalizedMetrics.put("r2_score", r2);

        // 处理均方误差
        Object mse = metrics.get("mean_squared_error");
        if (mse == null) mse = metrics.get("mse");
        if (mse == null) mse = metrics.get("MSE");
        if (mse != null) normalizedMetrics.put("mean_squared_error", mse);

        // 处理平均绝对误差
        Object mae = metrics.get("mean_absolute_error");
        if (mae == null) mae = metrics.get("mae");
        if (mae == null) mae = metrics.get("MAE");
        if (mae != null) normalizedMetrics.put("mean_absolute_error", mae);

        // 处理准确率（分类任务）
        Object accuracy = metrics.get("accuracy");
        if (accuracy != null) normalizedMetrics.put("accuracy", accuracy);

        // 处理精确率
        Object precision = metrics.get("precision");
        if (precision != null) normalizedMetrics.put("precision", precision);

        // 处理召回率
        Object recall = metrics.get("recall");
        if (recall != null) normalizedMetrics.put("recall", recall);

        // 处理F1分数
        Object f1Score = metrics.get("f1_score");
        if (f1Score == null) f1Score = metrics.get("f1");
        if (f1Score != null) normalizedMetrics.put("f1_score", f1Score);

        // 如果没有找到任何指标，保留原始数据
        if (normalizedMetrics.isEmpty() && !metrics.isEmpty()) {
            normalizedMetrics.putAll(metrics);
        }

        logger.debug("提取的性能指标: {}", normalizedMetrics);
        return normalizedMetrics;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractModelParameters(Map<String, Object> resultData) {
        Object modelParams = resultData.get("model_params");
        return modelParams instanceof Map ? (Map<String, Object>) modelParams : new HashMap<>();
    }

    // 其他方法的简化实现...
    private Map<String, Object> generateConfusionMatrixData(Map<String, Object> resultData) {
        return new HashMap<>();
    }

    private Map<String, Object> generateROCCurveData(Map<String, Object> resultData) {
        return new HashMap<>();
    }

    private Map<String, Object> generateFeatureImportanceData(Map<String, Object> resultData) {
        return new HashMap<>();
    }

    private Map<String, Object> generateLearningCurveData(Map<String, Object> resultData) {
        return new HashMap<>();
    }

    private Map<String, Object> generatePredictionDistributionData(Map<String, Object> resultData) {
        return new HashMap<>();
    }

    private Map<String, Object> findBestModel(List<Map<String, Object>> modelPerformances) {
        return new HashMap<>();
    }

    private Map<String, Object> generateComparisonCharts(List<Map<String, Object>> modelPerformances) {
        return new HashMap<>();
    }

    private String determinePerformanceLevel(Map<String, Object> resultData) {
        return "良好";
    }

    private Map<String, Object> extractKeyMetrics(Map<String, Object> resultData) {
        return new HashMap<>();
    }

    private Map<String, Object> assessDataQuality(Map<String, Object> resultData) {
        return new HashMap<>();
    }

    private List<String> generateRecommendations(String algorithmType, Map<String, Object> resultData) {
        return Arrays.asList("建议增加训练数据", "建议调整模型参数");
    }

    private String exportToJson(Map<String, Object> data, String fileName) {
        return "/exports/" + fileName + ".json";
    }

    private String exportToCsv(Map<String, Object> data, String fileName) {
        return "/exports/" + fileName + ".csv";
    }

    private String exportToExcel(Map<String, Object> data, String fileName) {
        return "/exports/" + fileName + ".xlsx";
    }

    private List<Map<String, Object>> extractTopFeatures(Object featureImportance) {
        return new ArrayList<>();
    }

    private Double calculateAverageConfidence(Object confidences) {
        return 0.85;
    }

    private Map<String, Object> analyzeConfidenceDistribution(Object confidences) {
        return new HashMap<>();
    }

    private Map<String, Object> getAveragePerformance(String algorithmType) {
        return new HashMap<>();
    }

    private Map<String, Object> getPerformanceRange(String algorithmType) {
        return new HashMap<>();
    }

    private Map<String, Object> getRecommendedParameters(String algorithmType) {
        return new HashMap<>();
    }

    /**
     * 计算任务执行时间
     */
    private Long calculateExecutionTime(AnalysisTask task) {
        if (task.getCreateTime() != null && task.getUpdateTime() != null) {
            return task.getUpdateTime().getTime() - task.getCreateTime().getTime();
        }
        return null;
    }
}
