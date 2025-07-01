package com.ruoyi.petrol.service.impl;

import com.ruoyi.petrol.domain.AnalysisTask;
import com.ruoyi.petrol.domain.PetrolModel;
import com.ruoyi.petrol.domain.PetrolDataset;
import com.ruoyi.petrol.service.IAnalysisManagerService;
import com.ruoyi.petrol.service.IAnalysisTaskService;
import com.ruoyi.petrol.service.IPetrolModelService;
import com.ruoyi.petrol.service.IPetrolDatasetService;
import com.ruoyi.petrol.service.strategy.AnalysisStrategy;
import com.ruoyi.petrol.service.strategy.AnalysisStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;
import org.springframework.stereotype.Service;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.DateUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.nio.file.Paths;

@Service
public class AnalysisManagerServiceImpl implements IAnalysisManagerService {
    private static final Logger log = LoggerFactory.getLogger(AnalysisManagerServiceImpl.class);

    private final AnalysisStrategyFactory strategyFactory;
    private final IAnalysisTaskService taskService;
    private final IPetrolModelService modelService;
    private final IPetrolDatasetService datasetService;
    private final RuoYiConfig ruoYiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AnalysisManagerServiceImpl(AnalysisStrategyFactory strategyFactory,
                                    IAnalysisTaskService taskService,
                                    IPetrolModelService modelService,
                                    IPetrolDatasetService datasetService,
                                    RuoYiConfig ruoYiConfig) {
        this.strategyFactory = strategyFactory;
        this.taskService = taskService;
        this.modelService = modelService;
        this.datasetService = datasetService;
        this.ruoYiConfig = ruoYiConfig;
    }
    
    /**
     * 构建用于存放分析结果的目录的Web相对路径。
     * 规则: data/petrol/results/[算法名称]/[时间戳]/
     * @param algorithmName 算法名称
     * @return 配置文件根目录下的相对路径 (e.g., data/petrol/results/regression_lightgbm_train/1678886400000/)
     */
    private String buildResultsRelativePath(String algorithmName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        // 注意：这里不再包含 /profile 前缀，并且路径的根是 petrol，因为 profile 根目录已经是 data
        return Paths.get(
            "petrol", "results", algorithmName, timestamp
        ).toString().replace("\\", "/");
    }

    private String getAbsoluteOutputPath(String relativePath) throws IOException {
        if (relativePath == null) {
            return null;
        }
        // 直接与配置的根目录拼接
        return Paths.get(ruoYiConfig.getProfile(), relativePath).toFile().getCanonicalPath();
    }

    @Override
    @Async("threadPoolTaskExecutor")
    public void processTask(AnalysisTask task) {
        // --- 0. 处理数据集模式 ---
        if (task.getDatasetId() != null) {
            try {
                // 获取数据集信息
                PetrolDataset dataset = datasetService.selectPetrolDatasetById(task.getDatasetId());
                if (dataset == null) {
                    throw new Exception("数据集不存在: " + task.getDatasetId());
                }

                // 设置任务的输入文件路径为数据集文件路径
                task.setInputFilePath(dataset.getFilePath());

                // 如果数据集有列信息，提取列名设置为任务的头信息
                if (dataset.getColumnInfo() != null && !dataset.getColumnInfo().isEmpty()) {
                    try {
                        // 解析数据集的列信息，提取列名
                        List<String> columnNames = extractColumnNamesFromDataset(dataset.getColumnInfo());
                        if (!columnNames.isEmpty()) {
                            task.setInputFileHeadersJson(JSON.toJSONString(columnNames));
                            log.info("[任务ID: {}] 从数据集提取到列名: {}", task.getId(), columnNames);
                        } else {
                            log.warn("[任务ID: {}] 数据集列信息为空，尝试重新解析文件", task.getId());
                            // 如果没有列信息，尝试重新解析文件
                            List<String> headers = parseFileHeaders(dataset.getFilePath());
                            if (!headers.isEmpty()) {
                                task.setInputFileHeadersJson(JSON.toJSONString(headers));
                                log.info("[任务ID: {}] 重新解析文件得到列名: {}", task.getId(), headers);
                            }
                        }
                    } catch (Exception e) {
                        log.error("[任务ID: {}] 解析数据集列信息失败，尝试重新解析文件: {}", task.getId(), e.getMessage());
                        // 如果解析失败，尝试重新解析文件
                        try {
                            List<String> headers = parseFileHeaders(dataset.getFilePath());
                            if (!headers.isEmpty()) {
                                task.setInputFileHeadersJson(JSON.toJSONString(headers));
                                log.info("[任务ID: {}] 重新解析文件得到列名: {}", task.getId(), headers);
                            }
                        } catch (Exception ex) {
                            log.error("[任务ID: {}] 重新解析文件也失败: {}", task.getId(), ex.getMessage());
                        }
                    }
                }

                log.info("[任务ID: {}] 使用数据集模式，数据集ID: {}, 文件路径: {}",
                    task.getId(), task.getDatasetId(), dataset.getFilePath());

            } catch (Exception e) {
                log.error("[任务ID: {}] 处理数据集失败: {}", task.getId(), e.getMessage(), e);
                task.setStatus("FAILED");
                task.setErrorMessage("数据集处理失败: " + e.getMessage());
                taskService.updateAnalysisTask(task);
                return;
            }
        }

        // --- 1. 强制设定和创建结果输出目录 ---
        // 先生成相对路径
        String resultsRelativePath = buildResultsRelativePath(task.getAlgorithm());
        // Web访问路径需要加上 /profile/ 前缀
        String resultsWebPath = "/profile/" + resultsRelativePath;
        task.setOutputDirPath(resultsWebPath);
        log.info("[任务ID: {}] 已设置结果输出目录 (Web): {}", task.getId(), resultsWebPath);

        try {
            // 使用相对路径获取绝对物理路径
            String absolutePath = getAbsoluteOutputPath(resultsRelativePath);
            if (absolutePath != null) {
                File outputDir = new File(absolutePath);
                // 确保目录存在
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                    log.info("[任务ID: {}] 已成功创建结果目录 (物理): {}", task.getId(), absolutePath);
                }
            }
        } catch (IOException e) {
            log.error("[任务ID: {}] 创建结果目录失败: [{}]. 错误: {}", task.getId(), resultsWebPath, e.getMessage(), e);
            task.setStatus("FAILED");
            task.setErrorMessage("无法创建结果输出目录: " + e.getMessage());
            taskService.updateAnalysisTask(task);
            return; // 关键步骤失败，提前退出
        }
        
        // --- 2. 清理旧目录 (如果需要) ---
        // 注意：由于我们现在每次都创建带时间戳的唯一目录，理论上不需要清理了。
        // 但保留此逻辑可以作为一种安全保障，以防万一。
        // if (task.getOutputDirPath() != null && !task.getOutputDirPath().isEmpty()) {
        // ... (原有的清理逻辑可以被注释或移除)
        // }
        
        task.setStatus("RUNNING");
        task.setUpdateTime(new Date());
        taskService.updateAnalysisTask(task);

        try {
            String algorithm = task.getAlgorithm();

            if (algorithm == null || algorithm.trim().isEmpty()) {
                throw new IllegalArgumentException("算法名称 (algorithm) 不能为空");
            }

            String strategyName = task.getAlgorithm();
            
            AnalysisStrategy strategy = strategyFactory.getStrategy(strategyName);
            if (strategy == null) {
                throw new Exception("不支持的分析策略: " + strategyName);
            }
            
            log.info("[任务ID: {}] 已匹配到算法策略: [{}], 准备执行。", task.getId(), strategyName);
            
            String resultJson = strategy.execute(task);

            task.setResultsJson(resultJson);
            task.setStatus("COMPLETED");
            task.setErrorMessage(null);

            // 尝试从结果中提取并保存模型信息
            try {
                saveModelFromResults(task, resultJson);
            } catch (Exception modelSaveException) {
                log.warn("[任务ID: {}] 保存模型信息失败，但任务已完成: {}",
                        task.getId(), modelSaveException.getMessage(), modelSaveException);
            }

        } catch (Exception e) {
            log.error("任务处理失败 -> [FAILED], 任务ID: {}. 错误: {}", task.getId(), e.getMessage(), e);
            task.setStatus("FAILED");
            
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String errorMessage = rootCause.getMessage();
            // 关键修复：截断错误信息，防止超出数据库字段长度限制
            if (errorMessage != null && errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 500);
            }
            task.setErrorMessage(errorMessage);

        } finally {
            task.setUpdateTime(new Date());
            taskService.updateAnalysisTask(task);
        }
    }

    /**
     * 从分析结果中提取模型信息并保存到数据库
     */
    private void saveModelFromResults(AnalysisTask task, String resultJson) {
        if (resultJson == null || resultJson.trim().isEmpty()) {
            log.warn("[任务ID: {}] 结果JSON为空，无法保存模型", task.getId());
            return;
        }

        try {
            log.info("[任务ID: {}] 开始解析结果JSON并保存模型", task.getId());
            JsonNode resultNode = objectMapper.readTree(resultJson);

            // 打印完整的JSON结构用于调试
            log.debug("[任务ID: {}] 完整结果JSON: {}", task.getId(), resultJson);

            JsonNode modelArtifact = resultNode.get("model_artifact");

            // 检查是否有模型文件
            if (modelArtifact == null) {
                log.warn("[任务ID: {}] 结果中没有model_artifact字段，跳过模型保存", task.getId());
                log.debug("[任务ID: {}] 可用字段: {}", task.getId(), resultNode.fieldNames());
                return;
            }

            if (!modelArtifact.isObject() || modelArtifact.size() == 0) {
                log.warn("[任务ID: {}] model_artifact字段为空或不是对象，跳过模型保存", task.getId());
                return;
            }

            // 获取模型文件信息
            String modelFileName = null;
            for (JsonNode fileNode : modelArtifact) {
                if (fileNode.isTextual()) {
                    modelFileName = fileNode.asText();
                    break;
                }
            }

            if (modelFileName == null) {
                log.debug("[任务ID: {}] 未找到有效的模型文件名", task.getId());
                return;
            }

            // 构建模型文件的完整路径
            String modelPath = task.getOutputDirPath() + "/" + modelFileName;

            // 获取模型参数和统计信息
            JsonNode modelParams = resultNode.get("model_params");
            JsonNode statistics = resultNode.get("statistics");

            // 创建模型记录
            PetrolModel model = new PetrolModel();
            model.setModelName(generateModelName(task));
            model.setAlgorithm(task.getAlgorithm());
            model.setModelType(determineModelType(task.getAlgorithm()));
            model.setModelPath(modelPath);
            model.setSourceTaskId(task.getId());
            model.setStatus("ACTIVE");
            model.setCreateTime(new Date());

            // 设置输入特征和输出目标
            if (modelParams != null) {
                JsonNode featureColumns = modelParams.get("feature_columns");
                JsonNode targetColumn = modelParams.get("target_column");

                if (featureColumns != null) {
                    // 保存为JSON格式，而不是字符串
                    try {
                        model.setInputFeatures(featureColumns.toString());
                    } catch (Exception e) {
                        log.warn("[任务ID: {}] 设置输入特征JSON失败，使用默认值: {}", task.getId(), e.getMessage());
                        model.setInputFeatures("[]");
                    }
                } else {
                    model.setInputFeatures("[]");
                }

                if (targetColumn != null) {
                    model.setOutputTarget(targetColumn.asText());
                } else {
                    model.setOutputTarget("");
                }
            } else {
                // 设置默认值
                model.setInputFeatures("[]");
                model.setOutputTarget("");
            }

            // 设置训练指标
            if (statistics != null) {
                try {
                    model.setTrainingMetrics(statistics.toString());
                } catch (Exception e) {
                    log.warn("[任务ID: {}] 设置训练指标JSON失败，使用默认值: {}", task.getId(), e.getMessage());
                    model.setTrainingMetrics("{}");
                }
            } else {
                model.setTrainingMetrics("{}");
            }

            // 设置描述
            model.setDescription(String.format("由分析任务 #%d 自动生成的%s模型",
                                             task.getId(), getAlgorithmDisplayName(task.getAlgorithm())));

            // 设置默认值
            model.setIsAvailable("Y");
            model.setCreatedBy("system");

            // 计算文件大小（如果可能）
            try {
                String absolutePath = getAbsoluteOutputPath(modelPath.replace("/profile/", ""));
                if (absolutePath != null) {
                    File modelFile = new File(absolutePath);
                    if (modelFile.exists()) {
                        model.setFileSize(modelFile.length());
                        log.debug("[任务ID: {}] 模型文件大小: {} bytes", task.getId(), modelFile.length());
                    } else {
                        log.warn("[任务ID: {}] 模型文件不存在: {}", task.getId(), absolutePath);
                    }
                }
            } catch (Exception e) {
                log.debug("[任务ID: {}] 无法获取模型文件大小: {}", task.getId(), e.getMessage());
            }

            // 保存模型记录
            log.info("[任务ID: {}] 准备保存模型记录: {}", task.getId(), model.getModelName());
            log.debug("[任务ID: {}] 模型详细信息: 算法={}, 类型={}, 路径={}, 状态={}, 可用={}",
                    task.getId(), model.getAlgorithm(), model.getModelType(),
                    model.getModelPath(), model.getStatus(), model.getIsAvailable());

            int result = modelService.insertPetrolModel(model);
            if (result > 0) {
                log.info("[任务ID: {}] 成功保存模型记录: {} (ID: {}, 路径: {})",
                        task.getId(), model.getModelName(), model.getId(), model.getModelPath());

                // 验证模型是否可以被查询到
                try {
                    PetrolModel savedModel = modelService.selectPetrolModelById(model.getId());
                    if (savedModel != null && "ACTIVE".equals(savedModel.getStatus()) && "Y".equals(savedModel.getIsAvailable())) {
                        log.info("[任务ID: {}] 模型验证成功，可以在预测模块中使用", task.getId());
                    } else {
                        log.warn("[任务ID: {}] 模型验证失败，状态: {}, 可用: {}",
                                task.getId(),
                                savedModel != null ? savedModel.getStatus() : "null",
                                savedModel != null ? savedModel.getIsAvailable() : "null");
                    }
                } catch (Exception e) {
                    log.warn("[任务ID: {}] 模型验证异常: {}", task.getId(), e.getMessage());
                }
            } else {
                log.error("[任务ID: {}] 保存模型记录失败，返回值: {}", task.getId(), result);
                throw new RuntimeException("保存模型记录失败，返回值: " + result);
            }

        } catch (Exception e) {
            log.error("[任务ID: {}] 解析结果JSON并保存模型失败: {}", task.getId(), e.getMessage(), e);
            throw new RuntimeException("保存模型信息失败", e);
        }
    }

    /**
     * 生成模型名称
     */
    private String generateModelName(AnalysisTask task) {
        String algorithmName = getAlgorithmDisplayName(task.getAlgorithm());
        String timestamp = DateUtils.dateTimeNow("yyyyMMdd_HHmmss");
        return String.format("%s_%s_Task%d", algorithmName, timestamp, task.getId());
    }

    /**
     * 根据算法确定模型类型
     */
    private String determineModelType(String algorithm) {
        if (algorithm == null) return "unknown";

        if (algorithm.contains("regression") || algorithm.contains("regressor")) {
            return "regression";
        } else if (algorithm.contains("classification") || algorithm.contains("classifier")) {
            return "classification";
        } else if (algorithm.contains("clustering") || algorithm.contains("kmeans")) {
            return "clustering";
        }

        return "other";
    }

    /**
     * 获取算法显示名称
     */
    private String getAlgorithmDisplayName(String algorithm) {
        if (algorithm == null) return "未知算法";

        switch (algorithm) {
            case "regression_linear_train": return "线性回归";
            case "regression_polynomial_train": return "多项式回归";
            case "regression_exponential_train": return "指数回归";
            case "regression_logarithmic_train": return "对数回归";
            case "regression_svm_train": return "SVR回归";
            case "regression_random_forest_train": return "随机森林回归";
            case "regression_lightgbm_train": return "LightGBM回归";
            case "regression_xgboost_train": return "XGBoost回归";
            case "regression_bilstm_train": return "BiLSTM回归";
            case "regression_tcn_train": return "TCN回归";
            case "classification_svm_train": return "SVM分类";
            case "classification_knn_train": return "KNN分类";
            case "clustering_kmeans_train": return "K-Means聚类";
            default: return algorithm;
        }
    }

    /**
     * 从数据集的列信息JSON中提取列名
     */
    private List<String> extractColumnNamesFromDataset(String columnInfoJson) {
        List<String> columnNames = new ArrayList<>();
        try {
            // 解析列信息JSON，格式为：[{"name": "col1", ...}, {"name": "col2", ...}]
            List<Map> columns = JSON.parseArray(columnInfoJson, Map.class);
            for (Map column : columns) {
                String name = (String) column.get("name");
                if (name != null && !name.trim().isEmpty()) {
                    columnNames.add(name);
                }
            }
        } catch (Exception e) {
            log.error("解析数据集列信息失败: {}", e.getMessage());
        }
        return columnNames;
    }

    /**
     * 直接解析文件获取列名
     */
    private List<String> parseFileHeaders(String filePath) {
        List<String> headers = new ArrayList<>();

        // 转换存储的相对路径为实际文件系统路径
        String actualFilePath = convertToActualFilePath(filePath);

        try {
            File file = new File(actualFilePath);
            if (!file.exists()) {
                log.error("文件不存在: {}", actualFilePath);
                return headers;
            }

            String extension = getFileExtension(actualFilePath).toLowerCase();

            if ("xlsx".equals(extension) || "xls".equals(extension)) {
                // 解析Excel文件
                try (Workbook workbook = WorkbookFactory.create(file)) {
                    Sheet sheet = workbook.getSheetAt(0);
                    Row headerRow = sheet.getRow(0);
                    if (headerRow != null) {
                        for (Cell cell : headerRow) {
                            if (cell != null) {
                                String cellValue = "";
                                switch (cell.getCellType()) {
                                    case STRING:
                                        cellValue = cell.getStringCellValue();
                                        break;
                                    case NUMERIC:
                                        cellValue = String.valueOf(cell.getNumericCellValue());
                                        break;
                                    default:
                                        cellValue = cell.toString();
                                }
                                headers.add(cellValue);
                            }
                        }
                    }
                }
            } else if ("csv".equals(extension)) {
                // 解析CSV文件
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line = reader.readLine();
                    if (line != null) {
                        String[] values = line.split(",");
                        for (String value : values) {
                            headers.add(value.trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析文件头信息失败: {}", e.getMessage());
        }

        return headers;
    }

    /**
     * 将存储的相对路径转换为实际文件系统路径
     */
    private String convertToActualFilePath(String storedPath) {
        if (storedPath.startsWith("/profile/")) {
            // 去掉/profile前缀，拼接实际路径
            String relativePath = storedPath.substring("/profile/".length());
            return "./data/" + relativePath;
        } else {
            // 兼容其他情况
            return storedPath;
        }
    }

    /**
     * 同步处理分析任务（不使用@Async注解）
     */
    @Override
    public void processTaskSync(AnalysisTask task) {
        // 移除@Async注解，直接调用处理逻辑
        processTaskInternal(task);
    }

    /**
     * 内部任务处理逻辑（提取公共部分）
     */
    private void processTaskInternal(AnalysisTask task) {
        // 这里应该包含原有processTask方法的核心逻辑
        // 为了简化，这里只是一个示例实现
        try {
            log.info("开始处理分析任务: taskId={}", task.getId());

            // 更新任务状态为运行中
            task.setStatus("RUNNING");
            taskService.updateAnalysisTask(task);

            // 调用策略执行任务
            AnalysisStrategy strategy = strategyFactory.getStrategy(task.getAlgorithm());
            String result = strategy.execute(task);

            // 更新任务结果
            task.setStatus("COMPLETED");
            task.setResultsJson(result);
            taskService.updateAnalysisTask(task);

            log.info("分析任务处理完成: taskId={}", task.getId());

        } catch (Exception e) {
            log.error("分析任务处理失败: taskId={}", task.getId(), e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            taskService.updateAnalysisTask(task);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

}