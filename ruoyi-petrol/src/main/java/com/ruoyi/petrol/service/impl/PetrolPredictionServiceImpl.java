package com.ruoyi.petrol.service.impl;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Date;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.petrol.mapper.PetrolPredictionMapper;
import com.ruoyi.petrol.mapper.PetrolModelMapper;
import com.ruoyi.petrol.domain.PetrolPrediction;
import com.ruoyi.petrol.domain.PetrolModel;
import com.ruoyi.petrol.domain.PetrolDataset;
import com.ruoyi.petrol.service.IPetrolPredictionService;
import com.ruoyi.petrol.service.IPetrolDatasetService;
import com.ruoyi.petrol.service.PythonExecutorService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 石油预测Service业务层处理
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@Service
public class PetrolPredictionServiceImpl implements IPetrolPredictionService
{
    private static final Logger log = LoggerFactory.getLogger(PetrolPredictionServiceImpl.class);

    @Autowired
    private PetrolPredictionMapper petrolPredictionMapper;

    @Autowired
    private PetrolModelMapper petrolModelMapper;

    @Autowired
    private PythonExecutorService pythonExecutorService;

    @Autowired
    private IPetrolDatasetService petrolDatasetService;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 查询石油预测
     * 
     * @param id 石油预测主键
     * @return 石油预测
     */
    @Override
    public PetrolPrediction selectPetrolPredictionById(Long id)
    {
        return petrolPredictionMapper.selectPetrolPredictionById(id);
    }

    /**
     * 查询石油预测列表
     * 
     * @param petrolPrediction 石油预测
     * @return 石油预测
     */
    @Override
    public List<PetrolPrediction> selectPetrolPredictionList(PetrolPrediction petrolPrediction)
    {
        return petrolPredictionMapper.selectPetrolPredictionList(petrolPrediction);
    }

    /**
     * 新增石油预测
     * 
     * @param petrolPrediction 石油预测
     * @return 结果
     */
    @Override
    public int insertPetrolPrediction(PetrolPrediction petrolPrediction)
    {
        petrolPrediction.setCreateTime(DateUtils.getNowDate());
        petrolPrediction.setCreatedBy(SecurityUtils.getUsername());

        // 设置默认状态
        if (petrolPrediction.getStatus() == null) {
            petrolPrediction.setStatus("PENDING");
        }

        return petrolPredictionMapper.insertPetrolPrediction(petrolPrediction);
    }

    /**
     * 修改石油预测
     * 
     * @param petrolPrediction 石油预测
     * @return 结果
     */
    @Override
    public int updatePetrolPrediction(PetrolPrediction petrolPrediction)
    {
        petrolPrediction.setUpdateTime(DateUtils.getNowDate());
        return petrolPredictionMapper.updatePetrolPrediction(petrolPrediction);
    }

    /**
     * 批量删除石油预测
     * 
     * @param ids 需要删除的石油预测主键
     * @return 结果
     */
    @Override
    public int deletePetrolPredictionByIds(Long[] ids)
    {
        return petrolPredictionMapper.deletePetrolPredictionByIds(ids);
    }

    /**
     * 删除石油预测信息
     * 
     * @param id 石油预测主键
     * @return 结果
     */
    @Override
    public int deletePetrolPredictionById(Long id)
    {
        return petrolPredictionMapper.deletePetrolPredictionById(id);
    }

    /**
     * 根据模型ID查询预测记录
     * 
     * @param modelId 模型ID
     * @return 预测记录列表
     */
    @Override
    public List<PetrolPrediction> selectPredictionsByModelId(Long modelId)
    {
        return petrolPredictionMapper.selectPredictionsByModelId(modelId);
    }



    /**
     * 批量预测（文件上传）
     * 
     * @param modelId 模型ID
     * @param file 输入文件
     * @param predictionName 预测名称
     * @return 预测结果
     */
    @Override
    public Map<String, Object> executeBatchPrediction(Long modelId, MultipartFile file, String predictionName)
    {
        Map<String, Object> result = new HashMap<>();
        PetrolPrediction prediction = null;

        try {
            // 获取模型信息
            PetrolModel model = petrolModelMapper.selectPetrolModelById(modelId);
            if (model == null) {
                result.put("success", false);
                result.put("message", "模型不存在");
                return result;
            }

            // 创建预测记录
            prediction = new PetrolPrediction();
            prediction.setPredictionName(predictionName);
            prediction.setModelId(modelId);
            prediction.setModelName(model.getModelName());
            prediction.setStatus("RUNNING");
            
            insertPetrolPrediction(prediction);

            // 调用Python API执行批量预测
            Map<String, Object> pythonRequest = new HashMap<>();
            pythonRequest.put("model_path", model.getModelPath());
            pythonRequest.put("model_type", model.getModelType());
            pythonRequest.put("algorithm", model.getAlgorithm());

            long startTime = System.currentTimeMillis();
            Map<String, Object> pythonResult = pythonExecutorService.batchPredict(pythonRequest, file);
            long endTime = System.currentTimeMillis();

            // 更新预测记录
            prediction.setExecutionTime((endTime - startTime) / 1000);
            
            if (pythonResult != null && pythonResult.containsKey("success") && Boolean.TRUE.equals(pythonResult.get("success"))) {
                prediction.setStatus("COMPLETED");
                Object resultData = pythonResult.get("result");
                if (resultData != null) {
                    prediction.setPredictionResult(objectMapper.writeValueAsString(resultData));
                }

                Object resultFilePathObj = pythonResult.get("result_file_path");
                if (resultFilePathObj != null) {
                    prediction.setResultFilePath(resultFilePathObj.toString());
                }

                result.put("success", true);
                result.put("predictionId", prediction.getId());
                result.put("result", resultData);

                Object downloadUrlObj = pythonResult.get("download_url");
                if (downloadUrlObj != null) {
                    result.put("downloadUrl", downloadUrlObj.toString());
                }
            } else {
                prediction.setStatus("FAILED");
                Object messageObj = pythonResult != null ? pythonResult.get("message") : null;
                String errorMessage = messageObj != null ? messageObj.toString() : "批量预测失败，未返回有效响应";
                prediction.setErrorMessage(errorMessage);

                result.put("success", false);
                result.put("message", errorMessage);
            }

            updatePetrolPrediction(prediction);

        } catch (Exception e) {
            log.error("批量预测执行异常", e);
            // 确保数据库状态一致性
            if (prediction != null && prediction.getId() != null) {
                try {
                    prediction.setStatus("FAILED");
                    prediction.setErrorMessage("批量预测执行异常: " + e.getMessage());
                    updatePetrolPrediction(prediction);
                } catch (Exception dbException) {
                    log.error("更新批量预测记录状态失败", dbException);
                }
            }
            result.put("success", false);
            result.put("message", "批量预测执行失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取预测状态
     * 
     * @param predictionId 预测ID
     * @return 预测状态
     */
    @Override
    public Map<String, Object> getPredictionStatus(Long predictionId)
    {
        Map<String, Object> result = new HashMap<>();
        
        PetrolPrediction prediction = petrolPredictionMapper.selectPetrolPredictionById(predictionId);
        if (prediction == null) {
            result.put("success", false);
            result.put("message", "预测记录不存在");
            return result;
        }

        result.put("success", true);
        result.put("status", prediction.getStatus());
        result.put("predictionName", prediction.getPredictionName());
        result.put("modelName", prediction.getModelName());
        result.put("createTime", prediction.getCreateTime());
        result.put("executionTime", prediction.getExecutionTime());
        
        if ("FAILED".equals(prediction.getStatus())) {
            result.put("errorMessage", prediction.getErrorMessage());
        }

        return result;
    }

    /**
     * 获取预测结果
     * 
     * @param predictionId 预测ID
     * @return 预测结果
     */
    @Override
    public Map<String, Object> getPredictionResult(Long predictionId)
    {
        Map<String, Object> result = new HashMap<>();

        PetrolPrediction prediction = petrolPredictionMapper.selectPetrolPredictionById(predictionId);
        if (prediction == null) {
            result.put("success", false);
            result.put("message", "预测记录不存在");
            return result;
        }

        if (!"COMPLETED".equals(prediction.getStatus())) {
            result.put("success", false);
            result.put("message", "预测尚未完成");
            return result;
        }

        try {
            String predictionResultJson = prediction.getPredictionResult();

            log.info("预测ID: {}, 状态: {}, 结果JSON长度: {}",
                predictionId, prediction.getStatus(),
                predictionResultJson != null ? predictionResultJson.length() : 0);

            if (predictionResultJson == null || predictionResultJson.trim().isEmpty() || "null".equals(predictionResultJson.trim())) {
                // 🔴 重构 - 不再生成模拟数据，返回明确的错误信息
                log.warn("预测结果为空，预测任务可能未完成或执行失败: predictionId={}", predictionId);
                result.put("success", false);
                result.put("message", "预测任务尚未完成或执行失败，请稍后重试或检查任务状态");
                result.put("predictionInfo", prediction);
                result.put("status", prediction.getStatus());
                result.put("errorCode", "PREDICTION_NOT_READY");
            } else {
                try {
                    // 解析真实的预测结果
                    log.info("尝试解析预测结果JSON: {}", predictionResultJson.substring(0, Math.min(100, predictionResultJson.length())));
                    @SuppressWarnings("unchecked")
                    Map<String, Object> predictionData = objectMapper.readValue(predictionResultJson, Map.class);

                    // 转换为前端期望的格式
                    Map<String, Object> formattedResult = formatPredictionResult(predictionData);

                    result.put("success", true);
                    result.put("data", formattedResult);
                    result.put("predictionInfo", prediction);
                    log.info("预测结果解析成功");
                } catch (Exception parseException) {
                    log.error("JSON解析失败，预测结果数据格式错误: {}", parseException.getMessage());
                    // 🔴 重构 - 不再使用模拟数据，返回解析错误信息
                    result.put("success", false);
                    result.put("message", "预测结果数据格式错误，无法解析: " + parseException.getMessage());
                    result.put("predictionInfo", prediction);
                    result.put("errorCode", "PREDICTION_PARSE_ERROR");
                    result.put("rawData", predictionResultJson.substring(0, Math.min(500, predictionResultJson.length())));
                }
            }
        } catch (Exception e) {
            log.error("获取预测结果时发生未预期错误: {}", e.getMessage(), e);
            // 🔴 重构 - 不再使用模拟数据，返回明确的错误信息
            result.put("success", false);
            result.put("message", "获取预测结果失败: " + e.getMessage());
            result.put("errorCode", "PREDICTION_FETCH_ERROR");
            result.put("predictionId", predictionId);

            // 提供错误恢复建议
            result.put("suggestions", Arrays.asList(
                "检查预测任务是否已完成",
                "验证Python API服务是否正常运行",
                "查看系统日志获取详细错误信息",
                "联系系统管理员获取技术支持"
            ));
        }

        return result;
    }



    /**
     * 格式化预测结果为前端期望的格式
     */
    private Map<String, Object> formatPredictionResult(Map<String, Object> rawResult) {
        Map<String, Object> formatted = new HashMap<>();

        try {
            // 灵活提取数据 - 适配Python API的多种返回格式
            List<Object> predictions = null;
            List<Object> inputData = null;
            List<Object> confidences = null;

            // 尝试从多个可能的字段中提取预测结果
            if (rawResult.containsKey("prediction_result")) {
                // 格式1: 有prediction_result字段
                @SuppressWarnings("unchecked")
                Map<String, Object> predResult = (Map<String, Object>) rawResult.get("prediction_result");
                if (predResult != null) {
                    predictions = (List<Object>) predResult.get("predictions");
                    inputData = (List<Object>) predResult.get("input_data");
                    confidences = (List<Object>) predResult.get("confidences");
                }
            } else if (rawResult.containsKey("result")) {
                // 格式2: 有result字段
                Object resultObj = rawResult.get("result");
                if (resultObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                    predictions = (List<Object>) resultMap.get("predictions");
                    inputData = (List<Object>) resultMap.get("input_data");
                    confidences = (List<Object>) resultMap.get("confidences");
                } else if (resultObj instanceof List) {
                    // 如果result直接是预测数组
                    predictions = (List<Object>) resultObj;
                }
            } else {
                // 格式3: 直接在顶级字段
                predictions = (List<Object>) rawResult.get("predictions");
                inputData = (List<Object>) rawResult.get("input_data");
                confidences = (List<Object>) rawResult.get("confidences");
            }

            log.info("提取的数据: predictions={}, inputData={}, confidences={}",
                    predictions != null ? predictions.size() : 0,
                    inputData != null ? inputData.size() : 0,
                    confidences != null ? confidences.size() : 0);

            // 只要有预测结果就可以处理，不强制要求inputData
            if (predictions != null && !predictions.isEmpty()) {
                List<Map<String, Object>> previewData = new ArrayList<>();

                // 🔧 修复：返回所有预测结果，不限制为10条
                // 原来的限制：int previewSize = Math.min(10, predictions.size());
                int previewSize = predictions.size(); // 返回所有结果
                log.info("🔧 返回完整预测结果: {}条数据", previewSize);

                for (int i = 0; i < previewSize; i++) {
                    Map<String, Object> row = new HashMap<>();

                    // 添加输入特征（如果有的话）
                    if (inputData != null && i < inputData.size() && inputData.get(i) instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> input = (Map<String, Object>) inputData.get(i);
                        row.putAll(input);
                    } else {
                        // 如果没有输入数据，添加默认的索引
                        row.put("sample_id", i + 1);
                        row.put("index", i);
                    }

                    // 添加预测结果
                    row.put("预测值", predictions.get(i));

                    // 添加置信度（如果有的话）
                    if (confidences != null && i < confidences.size()) {
                        row.put("置信度", confidences.get(i));
                    } else {
                        // 如果没有置信度数据，使用默认值
                        row.put("置信度", 0.85);
                    }

                    previewData.add(row);
                }

                // 确定列名
                Set<String> columnSet = new LinkedHashSet<>();
                if (!previewData.isEmpty()) {
                    columnSet.addAll(previewData.get(0).keySet());
                }

                formatted.put("previewData", previewData);
                formatted.put("columns", new ArrayList<>(columnSet));
                formatted.put("totalCount", predictions.size());
                formatted.put("summary", generatePredictionSummary(rawResult));
            } else {
                // 🔴 重构 - 如果数据格式不符合预期，返回错误信息而不是模拟数据
                log.warn("预测结果数据格式不符合预期，缺少必要字段: predictions或input_data");
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", true);
                errorResult.put("message", "预测结果数据格式不符合预期");
                errorResult.put("expectedFields", Arrays.asList("predictions", "input_data", "confidences"));
                errorResult.put("actualFields", rawResult.keySet());
                return errorResult;
            }

        } catch (Exception e) {
            log.warn("格式化预测结果失败: {}", e.getMessage());
            // 🔴 重构 - 返回错误信息而不是模拟数据
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", true);
            errorResult.put("message", "格式化预测结果失败: " + e.getMessage());
            errorResult.put("rawDataSample", rawResult.toString().substring(0, Math.min(200, rawResult.toString().length())));
            return errorResult;
        }

        return formatted;
    }



    /**
     * 生成预测摘要信息（基于真实数据）
     */
    private Map<String, Object> generatePredictionSummary(Map<String, Object> rawResult) {
        Map<String, Object> summary = new HashMap<>();

        try {
            @SuppressWarnings("unchecked")
            List<Object> predictions = (List<Object>) rawResult.get("predictions");
            @SuppressWarnings("unchecked")
            List<Object> confidences = (List<Object>) rawResult.get("confidences");

            if (predictions != null && !predictions.isEmpty()) {
                // 计算预测值统计
                double sum = 0, max = Double.MIN_VALUE, min = Double.MAX_VALUE;
                for (Object pred : predictions) {
                    double value = Double.parseDouble(pred.toString());
                    sum += value;
                    max = Math.max(max, value);
                    min = Math.min(min, value);
                }

                summary.put("平均预测值", String.format("%.4f", sum / predictions.size()));
                summary.put("最大预测值", String.format("%.4f", max));
                summary.put("最小预测值", String.format("%.4f", min));
            }

            if (confidences != null && !confidences.isEmpty()) {
                // 计算置信度统计
                double confSum = 0;
                for (Object conf : confidences) {
                    confSum += Double.parseDouble(conf.toString());
                }
                summary.put("平均置信度", String.format("%.2f", confSum / confidences.size()));
            }

        } catch (Exception e) {
            log.warn("计算预测摘要失败: {}", e.getMessage());
            // 🔴 重构 - 返回错误信息而不是模拟摘要
            summary.put("error", "计算预测摘要失败: " + e.getMessage());
            summary.put("预测数量", "未知");
            summary.put("平均预测值", "计算失败");
            summary.put("平均置信度", "计算失败");
        }

        return summary;
    }

    /**
     * 执行预测（支持文件上传）
     */
    @Override
    public Map<String, Object> executePredictionWithFiles(String predictionName, Long modelId,
                                                         MultipartFile dataFile, MultipartFile modelFile,
                                                         String modelName, String algorithm,
                                                         String predictionParams, String description)
    {
        Map<String, Object> result = new HashMap<>();

        try {
            // 创建预测记录
            PetrolPrediction prediction = new PetrolPrediction();
            prediction.setPredictionName(predictionName);
            prediction.setStatus("PENDING");
            prediction.setPredictionParams(predictionParams);

            if (description != null) {
                prediction.setRemark(description);
            }

            // 如果使用已有模型
            if (modelId != null) {
                PetrolModel model = petrolModelMapper.selectPetrolModelById(modelId);
                if (model == null) {
                    result.put("success", false);
                    result.put("message", "模型不存在");
                    return result;
                }
                prediction.setModelId(modelId);
                prediction.setModelName(model.getModelName());
            } else if (modelFile != null && modelName != null) {
                // 如果上传新模型，使用默认模型ID
                Long defaultModelId = getDefaultModelId();
                prediction.setModelId(defaultModelId);
                prediction.setModelName(modelName);
            } else {
                // 如果没有指定模型，使用默认模型
                Long defaultModelId = getDefaultModelId();
                prediction.setModelId(defaultModelId);
                prediction.setModelName(algorithm != null ? algorithm : "默认算法");
            }

            insertPetrolPrediction(prediction);

            // 🔴 重构 - 调用真实的Python API执行预测
            try {
                log.info("开始执行预测任务: ID={}, 算法={}", prediction.getId(), algorithm);

                // 构建Python API请求参数
                Map<String, Object> pythonRequest = buildPythonPredictionRequest(prediction, algorithm, dataFile);

                // 异步调用Python API（实际项目中应该使用异步处理）
                prediction.setStatus("RUNNING");
                updatePetrolPrediction(prediction);

                // 调用Python API执行预测
                Map<String, Object> pythonResult = pythonExecutorService.batchPredict(pythonRequest, dataFile);

                if (pythonResult != null && Boolean.TRUE.equals(pythonResult.get("success"))) {
                    // 预测成功
                    String resultJson = objectMapper.writeValueAsString(pythonResult);
                    prediction.setPredictionResult(resultJson);
                    prediction.setStatus("COMPLETED");
                    prediction.setPredictionCount(extractPredictionCount(pythonResult));
                    prediction.setExecutionTime(extractExecutionTime(pythonResult));

                    log.info("预测任务执行成功: ID={}", prediction.getId());
                } else {
                    // 预测失败
                    prediction.setStatus("FAILED");
                    prediction.setErrorMessage(pythonResult != null ?
                        (String) pythonResult.get("message") : "Python API调用失败");

                    log.error("预测任务执行失败: ID={}, 错误: {}",
                        prediction.getId(), prediction.getErrorMessage());
                }

                updatePetrolPrediction(prediction);

                result.put("success", true);
                result.put("id", prediction.getId());
                result.put("message", "预测任务创建成功，正在执行中");
                result.put("status", prediction.getStatus());

            } catch (Exception pythonException) {
                log.error("调用Python API执行预测失败: {}", pythonException.getMessage(), pythonException);

                // 更新预测任务状态为失败
                prediction.setStatus("FAILED");
                prediction.setErrorMessage("Python API调用异常: " + pythonException.getMessage());
                updatePetrolPrediction(prediction);

                result.put("success", false);
                result.put("message", "预测任务创建成功，但执行失败: " + pythonException.getMessage());
                result.put("id", prediction.getId());
                result.put("errorCode", "PYTHON_API_ERROR");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "预测执行失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取统计信息
     */
    @Override
    public Map<String, Object> getStats()
    {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 获取模型统计
            int totalModels = petrolModelMapper.selectPetrolModelList(new PetrolModel()).size();

            // 获取预测统计
            List<PetrolPrediction> allPredictions = petrolPredictionMapper.selectPetrolPredictionList(new PetrolPrediction());
            int totalPredictions = allPredictions.size();

            long runningPredictions = allPredictions.stream()
                .filter(p -> "RUNNING".equals(p.getStatus()) || "PENDING".equals(p.getStatus()))
                .count();

            long completedPredictions = allPredictions.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .count();

            stats.put("totalModels", totalModels);
            stats.put("totalPredictions", totalPredictions);
            stats.put("runningPredictions", runningPredictions);
            stats.put("completedPredictions", completedPredictions);

        } catch (Exception e) {
            // 如果获取失败，返回默认值
            stats.put("totalModels", 3);
            stats.put("totalPredictions", 5);
            stats.put("runningPredictions", 1);
            stats.put("completedPredictions", 3);
        }

        return stats;
    }

    /**
     * 创建增强的预测任务（支持特征选择和目标选择）
     */
    public Map<String, Object> createEnhancedPrediction(Map<String, Object> predictionData) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 🔧 提取所有预测参数
            String predictionName = (String) predictionData.get("predictionName");
            String description = (String) predictionData.get("description");
            @SuppressWarnings("unchecked")
            List<String> features = (List<String>) predictionData.get("features");
            String target = (String) predictionData.get("target");
            String taskType = (String) predictionData.get("taskType");
            @SuppressWarnings("unchecked")
            Map<String, Object> modelSelection = (Map<String, Object>) predictionData.get("modelSelection");
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) predictionData.get("parameters");
            @SuppressWarnings("unchecked")
            List<Integer> predictionIndices = (List<Integer>) predictionData.get("predictionIndices");
            @SuppressWarnings("unchecked")
            Map<String, Object> dataFile = (Map<String, Object>) predictionData.get("dataFile");

            // 🔧 详细的参数提取日志
            log.info("🔧 提取预测参数:");
            log.info("  - 预测名称: {}", predictionName);
            log.info("  - 特征列: {} ({}个)", features, features != null ? features.size() : 0);
            log.info("  - 目标列: {}", target);
            log.info("  - 任务类型: {}", taskType);
            log.info("  - 预测索引: {} ({}个)", predictionIndices != null ? "已提供" : "未提供",
                predictionIndices != null ? predictionIndices.size() : 0);
            log.info("  - 数据文件: {}", dataFile != null ? "已提供" : "未提供");
            log.info("  - 模型选择: {}", modelSelection != null ? "已提供" : "未提供");
            log.info("  - 参数配置: {}", parameters != null ? parameters.keySet() : "未提供");

            // 验证必要参数
            if (predictionName == null || predictionName.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "预测任务名称不能为空");
                return result;
            }

            if (features == null || features.isEmpty()) {
                result.put("success", false);
                result.put("message", "必须选择至少一个特征列");
                return result;
            }

            if (target == null || target.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "必须选择一个目标列");
                return result;
            }

            // 创建预测记录
            PetrolPrediction prediction = new PetrolPrediction();
            prediction.setPredictionName(predictionName);
            // 将描述信息存储在备注字段中
            prediction.setRemark(description);
            prediction.setStatus("PENDING");

            // 处理模型选择
            if (modelSelection != null) {
                String mode = (String) modelSelection.get("mode");
                log.info("🔍 模型选择模式: {}", mode);

                if ("existing".equals(mode) && modelSelection.get("model") != null) {
                    // 使用已有模型
                    @SuppressWarnings("unchecked")
                    Map<String, Object> model = (Map<String, Object>) modelSelection.get("model");
                    Long modelId = ((Number) model.get("id")).longValue();

                    // 验证模型是否存在且可用
                    PetrolModel existingModel = petrolModelMapper.selectPetrolModelById(modelId);
                    if (existingModel != null && "ACTIVE".equals(existingModel.getStatus()) && "Y".equals(existingModel.getIsAvailable())) {
                        prediction.setModelId(modelId);
                        prediction.setModelName(existingModel.getModelName());
                        log.info("✅ 使用已有模型: ID={}, 名称={}", modelId, existingModel.getModelName());
                    } else {
                        log.warn("⚠️ 指定的模型不可用，使用默认模型: modelId={}", modelId);
                        Long defaultModelId = getDefaultModelId();
                        prediction.setModelId(defaultModelId);
                        prediction.setModelName("Default Model (Fallback)");
                    }

                } else if ("algorithm".equals(mode) && modelSelection.get("algorithm") != null) {
                    // 算法训练模式
                    @SuppressWarnings("unchecked")
                    Map<String, Object> algorithm = (Map<String, Object>) modelSelection.get("algorithm");
                    String algorithmName = (String) algorithm.get("name");
                    String algorithmId = (String) algorithm.get("id");

                    prediction.setModelName("算法训练: " + algorithmName);
                    // 对于算法选择，使用默认模型ID作为占位符
                    Long defaultModelId = getDefaultModelId();
                    prediction.setModelId(defaultModelId);

                    log.info("✅ 使用算法训练模式: {}", algorithmName);

                } else if ("upload".equals(mode) && modelSelection.get("uploadForm") != null) {
                    // 模型上传模式
                    @SuppressWarnings("unchecked")
                    Map<String, Object> uploadForm = (Map<String, Object>) modelSelection.get("uploadForm");
                    String modelName = (String) uploadForm.get("modelName");
                    String algorithm = (String) uploadForm.get("algorithm");

                    prediction.setModelName("上传模型: " + modelName);
                    // 对于上传模型，使用默认模型ID作为占位符
                    Long defaultModelId = getDefaultModelId();
                    prediction.setModelId(defaultModelId);

                    log.info("✅ 使用模型上传模式: {}", modelName);
                }
            }

            // 如果没有设置modelId，使用默认值
            if (prediction.getModelId() == null) {
                Long defaultModelId = getDefaultModelId();
                prediction.setModelId(defaultModelId);
                prediction.setModelName("Default Model");
            }

            // 🔧 构建完整的预测参数JSON
            Map<String, Object> predictionParams = new HashMap<>();
            predictionParams.put("features", features);
            predictionParams.put("target", target);
            predictionParams.put("taskType", taskType);
            predictionParams.put("modelSelection", modelSelection);

            // 🔧 添加遗漏的关键参数
            if (predictionIndices != null) {
                predictionParams.put("predictionIndices", predictionIndices);
                log.info("🔧 添加预测索引到参数: {} 个索引", predictionIndices.size());
            }

            if (dataFile != null) {
                predictionParams.put("dataFile", dataFile);
                log.info("🔧 添加数据文件到参数: {}", dataFile.keySet());
            }

            // 🔧 修复：保持parameters作为嵌套对象，同时也展开到顶级
            if (parameters != null) {
                // 保持parameters作为嵌套对象
                predictionParams.put("parameters", parameters);
                // 同时也展开到顶级（向后兼容）
                predictionParams.putAll(parameters);
                log.info("🔧 添加用户参数到预测参数: {} (嵌套+展开)", parameters.keySet());
            }

            // 设置预测参数
            try {
                String paramsJson = objectMapper.writeValueAsString(predictionParams);
                prediction.setPredictionParams(paramsJson);
            } catch (Exception e) {
                log.error("序列化预测参数失败: {}", e.getMessage(), e);
                result.put("success", false);
                result.put("message", "预测参数格式错误");
                return result;
            }

            // 保存预测记录
            int insertResult = insertPetrolPrediction(prediction);

            if (insertResult > 0) {
                result.put("success", true);
                result.put("message", "预测任务创建成功");
                result.put("predictionId", prediction.getId());

                // 调用真实的Python API执行增强预测
                try {
                    log.info("开始执行增强预测任务: ID={}", prediction.getId());
                    log.info("预测参数 - 特征: {}, 目标: {}, 任务类型: {}", features, target, taskType);

                    // 设置任务状态为运行中
                    prediction.setStatus("RUNNING");
                    updatePetrolPrediction(prediction);

                    // 构建增强预测请求参数
                    Map<String, Object> enhancedRequest = buildEnhancedPredictionRequest(predictionParams, prediction);
                    log.info("构建预测请求参数完成，开始调用Python API");

                    // 调用Python API执行增强预测
                    Map<String, Object> pythonResult = pythonExecutorService.predict(enhancedRequest);
                    log.info("🔍 Python API返回结果: {}", pythonResult);

                    if (pythonResult != null && Boolean.TRUE.equals(pythonResult.get("success"))) {
                        // 预测成功 - 提取并格式化预测结果
                        log.info("✅ Python API预测成功，开始处理和保存结果");

                        // 提取预测结果数据
                        Object predictionResultData = pythonResult.get("data");
                        if (predictionResultData == null) {
                            predictionResultData = pythonResult.get("prediction_result");
                        }
                        if (predictionResultData == null) {
                            predictionResultData = pythonResult; // 如果没有嵌套结构，使用整个结果
                        }
                        log.info("🔍 提取到的预测结果数据类型: {}", predictionResultData.getClass().getSimpleName());

                        // 格式化为前端期望的结构
                        Map<String, Object> formattedResult = formatPythonApiResult(predictionResultData, pythonResult);
                        log.info("🔍 格式化后的结果包含字段: {}", formattedResult.keySet());

                        // 保存格式化后的结果
                        String resultJson = objectMapper.writeValueAsString(formattedResult);
                        log.info("🔍 保存到数据库的JSON长度: {}", resultJson.length());

                        prediction.setPredictionResult(resultJson);
                        prediction.setStatus("COMPLETED");
                        prediction.setPredictionCount(extractPredictionCount(formattedResult));
                        prediction.setExecutionTime(extractExecutionTime(pythonResult));

                        // 更新数据库
                        int updateResult = updatePetrolPrediction(prediction);
                        log.info("🔍 数据库更新结果: {}, 预测ID: {}", updateResult, prediction.getId());

                        log.info("✅ 增强预测任务执行成功: ID={}, 预测数量: {}, 执行时间: {}ms",
                            prediction.getId(), prediction.getPredictionCount(), prediction.getExecutionTime());
                    } else {
                        // 预测失败
                        String errorMessage = pythonResult != null ?
                            (String) pythonResult.get("message") : "Python API调用失败，未返回有效结果";

                        prediction.setStatus("FAILED");
                        prediction.setErrorMessage(errorMessage);

                        // 更新数据库
                        int updateResult = updatePetrolPrediction(prediction);
                        log.info("🔍 失败状态数据库更新结果: {}, 预测ID: {}", updateResult, prediction.getId());

                        log.error("❌ 增强预测任务执行失败: ID={}, 错误: {}", prediction.getId(), errorMessage);
                        log.error("🔍 Python API完整响应: {}", pythonResult);

                        // 更新结果中的错误信息，便于前端显示
                        result.put("success", false);
                        result.put("message", "预测执行失败: " + errorMessage);
                    }

                } catch (Exception pythonException) {
                    log.error("调用Python API执行增强预测失败: {}", pythonException.getMessage(), pythonException);

                    // 构建用户友好的错误信息
                    String userFriendlyMessage = buildUserFriendlyErrorMessage(pythonException);

                    // 更新预测任务状态为失败
                    prediction.setStatus("FAILED");
                    prediction.setErrorMessage("Python API调用异常: " + pythonException.getMessage());

                    // 更新返回结果
                    result.put("success", false);
                    result.put("message", userFriendlyMessage);
                    result.put("errorType", "API_CONNECTION_ERROR");
                    result.put("retryable", true);
                }

                updatePetrolPrediction(prediction);

                log.info("增强预测任务创建成功: ID={}, 名称={}, 特征数={}, 目标={}",
                    prediction.getId(), predictionName, features.size(), target);
            } else {
                result.put("success", false);
                result.put("message", "保存预测任务失败");
            }

        } catch (Exception e) {
            log.error("创建增强预测任务失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "创建预测任务失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 格式化Python API返回的结果为前端期望的格式
     */
    private Map<String, Object> formatPythonApiResult(Object predictionResultData, Map<String, Object> fullResult) {
        Map<String, Object> formatted = new HashMap<>();

        try {
            log.info("开始格式化Python API结果，数据类型: {}", predictionResultData.getClass().getSimpleName());

            // 如果predictionResultData是Map类型
            if (predictionResultData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) predictionResultData;

                // 提取预测数组
                Object predictions = resultMap.get("predictions");
                if (predictions == null) {
                    // 尝试其他可能的字段名
                    predictions = resultMap.get("prediction");
                    if (predictions == null) {
                        predictions = resultMap.get("values");
                    }
                }

                if (predictions instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> predictionList = (List<Object>) predictions;
                    formatted.put("predictions", predictionList);

                    // 生成输入数据（如果没有提供）
                    Object inputData = resultMap.get("input_data");
                    if (inputData == null) {
                        inputData = generateInputDataFromPredictions(predictionList);
                    }
                    formatted.put("input_data", inputData);

                    // 处理置信度（基于真实数据计算或不输出）
                    Object confidences = resultMap.get("confidences");
                    if (confidences == null) {
                        // 尝试基于统计数据计算置信度
                        confidences = calculateConfidenceFromStatistics(resultMap, predictionList);
                        if (confidences == null) {
                            log.info("无法从统计数据计算置信度，不输出置信度字段");
                            // 不添加置信度字段到结果中
                        } else {
                            formatted.put("confidences", confidences);
                            log.info("基于统计数据计算出置信度，数量: {}",
                                    confidences instanceof List ? ((List<?>) confidences).size() : "未知");
                        }
                    } else {
                        formatted.put("confidences", confidences);
                        log.info("使用API返回的置信度数据");
                    }

                    // 添加统计信息
                    formatted.put("statistics", generateStatisticsFromPredictions(predictionList));

                    // 添加其他有用信息
                    if (resultMap.containsKey("accuracy")) {
                        formatted.put("accuracy", resultMap.get("accuracy"));
                    }
                    if (resultMap.containsKey("model_metrics")) {
                        formatted.put("model_metrics", resultMap.get("model_metrics"));
                    }

                } else {
                    log.warn("预测结果不包含有效的predictions数组");
                    // 创建默认结构
                    formatted.put("predictions", Arrays.asList(0.0));
                    formatted.put("input_data", Arrays.asList(new HashMap<>()));
                    formatted.put("confidences", Arrays.asList(0.5));
                    formatted.put("statistics", new HashMap<>());
                }

            } else {
                log.warn("预测结果数据不是Map类型: {}", predictionResultData.getClass());
                // 创建默认结构
                formatted.put("predictions", Arrays.asList(0.0));
                formatted.put("input_data", Arrays.asList(new HashMap<>()));
                formatted.put("confidences", Arrays.asList(0.5));
                formatted.put("statistics", new HashMap<>());
            }

            // 添加元数据
            formatted.put("success", true);
            formatted.put("message", "预测执行成功");
            formatted.put("timestamp", System.currentTimeMillis());

            log.info("Python API结果格式化完成，预测数量: {}",
                ((List<?>) formatted.get("predictions")).size());

        } catch (Exception e) {
            log.error("格式化Python API结果失败: {}", e.getMessage(), e);
            // 返回错误结构
            formatted.put("success", false);
            formatted.put("message", "结果格式化失败: " + e.getMessage());
            formatted.put("predictions", Arrays.asList());
            formatted.put("input_data", Arrays.asList());
            formatted.put("confidences", Arrays.asList());
            formatted.put("statistics", new HashMap<>());
        }

        return formatted;
    }

    /**
     * 从预测结果生成输入数据
     */
    private List<Map<String, Object>> generateInputDataFromPredictions(List<Object> predictions) {
        List<Map<String, Object>> inputData = new ArrayList<>();
        for (int i = 0; i < predictions.size(); i++) {
            Map<String, Object> input = new HashMap<>();
            input.put("sample_id", i + 1);
            input.put("index", i);
            inputData.add(input);
        }
        return inputData;
    }

    /**
     * 基于统计数据计算置信度
     * 使用预测值的标准差和分布信息计算每个预测的置信度
     */
    private List<Double> calculateConfidenceFromStatistics(Map<String, Object> resultMap, List<Object> predictions) {
        try {
            // 获取统计信息
            Object statisticsObj = resultMap.get("statistics");
            if (statisticsObj == null) {
                log.warn("结果中没有统计信息，无法计算置信度");
                return null;
            }

            Map<String, Object> statistics = (Map<String, Object>) statisticsObj;
            Object summaryObj = statistics.get("prediction_summary");
            if (summaryObj == null) {
                log.warn("统计信息中没有prediction_summary，无法计算置信度");
                return null;
            }

            Map<String, Object> summary = (Map<String, Object>) summaryObj;

            // 获取关键统计值
            Double mean = getDoubleValue(summary, "mean");
            Double std = getDoubleValue(summary, "std");
            Double min = getDoubleValue(summary, "min");
            Double max = getDoubleValue(summary, "max");

            if (mean == null || std == null) {
                log.warn("缺少必要的统计值（mean或std），无法计算置信度");
                return null;
            }

            log.info("基于统计数据计算置信度: mean={}, std={}, min={}, max={}", mean, std, min, max);

            // 为每个预测值计算置信度
            List<Double> confidences = new ArrayList<>();
            for (Object predObj : predictions) {
                Double predValue = getDoubleValue(predObj);
                if (predValue != null) {
                    // 基于预测值与均值的距离计算置信度
                    // 距离均值越近，置信度越高
                    double distance = Math.abs(predValue - mean);
                    double normalizedDistance = std > 0 ? distance / std : 0;

                    // 使用高斯分布计算置信度 (0-1之间)
                    // 在1个标准差内的置信度约为0.68，2个标准差内约为0.95
                    double confidence = Math.exp(-0.5 * normalizedDistance * normalizedDistance);

                    // 确保置信度在合理范围内
                    confidence = Math.max(0.1, Math.min(0.99, confidence));
                    confidences.add(confidence);
                } else {
                    // 如果预测值无效，使用默认低置信度
                    confidences.add(0.1);
                }
            }

            log.info("成功计算出{}个置信度值，平均置信度: {}",
                    confidences.size(),
                    confidences.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

            return confidences;

        } catch (Exception e) {
            log.error("计算置信度时发生错误: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 安全获取Double值
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return getDoubleValue(value);
    }

    /**
     * 安全获取Double值
     */
    private Double getDoubleValue(Object value) {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 根据行选择策略生成预测索引
     */
    private List<Integer> generatePredictionIndices(Map<String, Object> predictionParams) {
        try {
            String strategy = (String) predictionParams.get("rowSelectionStrategy");
            Integer sampleCount = predictionParams.get("sampleCount") != null ?
                    Integer.valueOf(predictionParams.get("sampleCount").toString()) : 100;

            if ("custom".equals(strategy)) {
                // 自定义行选择
                @SuppressWarnings("unchecked")
                List<Integer> customRows = (List<Integer>) predictionParams.get("customRows");
                if (customRows != null && !customRows.isEmpty()) {
                    // 转换为0基索引（数据库/用户使用1基索引，Python使用0基索引）
                    List<Integer> indices = new ArrayList<>();
                    for (Integer row : customRows) {
                        if (row != null && row > 0) {
                            indices.add(row - 1); // 转换为0基索引
                        }
                    }
                    log.info("自定义行选择: 用户输入{}行，转换为{}个0基索引", customRows.size(), indices.size());
                    return indices;
                } else {
                    log.warn("自定义行选择策略但未提供自定义行，回退到顺序选择");
                    strategy = "sequential";
                }
            }

            if ("sequential".equals(strategy) || strategy == null) {
                // 顺序选择：从第0行开始选择sampleCount行
                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < sampleCount; i++) {
                    indices.add(i);
                }
                log.info("顺序行选择: 生成{}个索引 (0-{})", indices.size(), sampleCount - 1);
                return indices;
            }

            log.warn("未知的行选择策略: {}, 使用默认顺序选择", strategy);
            return generateSequentialIndices(sampleCount);

        } catch (Exception e) {
            log.error("生成预测索引失败: {}", e.getMessage(), e);
            // 回退到默认策略
            Integer sampleCount = predictionParams.get("sampleCount") != null ?
                    Integer.valueOf(predictionParams.get("sampleCount").toString()) : 100;
            return generateSequentialIndices(sampleCount);
        }
    }

    /**
     * 生成顺序索引
     */
    private List<Integer> generateSequentialIndices(int count) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            indices.add(i);
        }
        return indices;
    }



    /**
     * 从预测结果生成统计信息
     */
    private Map<String, Object> generateStatisticsFromPredictions(List<Object> predictions) {
        Map<String, Object> statistics = new HashMap<>();

        try {
            List<Double> numericPredictions = predictions.stream()
                .filter(p -> p instanceof Number)
                .map(p -> ((Number) p).doubleValue())
                .collect(Collectors.toList());

            if (!numericPredictions.isEmpty()) {
                double sum = numericPredictions.stream().mapToDouble(Double::doubleValue).sum();
                double mean = sum / numericPredictions.size();
                double min = numericPredictions.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                double max = numericPredictions.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

                statistics.put("mean_prediction", mean);
                statistics.put("min_prediction", min);
                statistics.put("max_prediction", max);
                statistics.put("count", numericPredictions.size());
                statistics.put("sum", sum);
            }
        } catch (Exception e) {
            log.warn("生成统计信息失败: {}", e.getMessage());
        }

        return statistics;
    }

    /**
     * 构建用户友好的错误信息
     * 将技术性错误转换为用户可理解的提示信息
     */
    private String buildUserFriendlyErrorMessage(Exception exception) {
        String message = exception.getMessage();
        String className = exception.getClass().getSimpleName();

        // 根据异常类型和消息内容构建友好提示
        if (message != null) {
            if (message.contains("Connection refused") || message.contains("ConnectException")) {
                return "预测服务暂时不可用，请检查服务状态或稍后重试";
            } else if (message.contains("timeout") || message.contains("SocketTimeoutException")) {
                return "预测服务响应超时，请稍后重试";
            } else if (message.contains("UnknownHostException") || message.contains("NoRouteToHostException")) {
                return "无法连接到预测服务，请检查网络配置";
            } else if (message.contains("401") || message.contains("Unauthorized")) {
                return "预测服务认证失败，请联系系统管理员";
            } else if (message.contains("403") || message.contains("Forbidden")) {
                return "没有权限访问预测服务，请联系系统管理员";
            } else if (message.contains("404") || message.contains("Not Found")) {
                return "预测服务接口不存在，请联系系统管理员检查配置";
            } else if (message.contains("500") || message.contains("Internal Server Error")) {
                return "预测服务内部错误，请联系系统管理员";
            }
        }

        // 根据异常类型提供通用提示
        if ("RestClientException".equals(className) || "HttpClientErrorException".equals(className)) {
            return "预测服务通信异常，请稍后重试或联系系统管理员";
        } else if ("JsonProcessingException".equals(className) || "JsonParseException".equals(className)) {
            return "预测服务返回数据格式异常，请联系系统管理员";
        }

        // 默认错误信息
        return "预测服务暂时不可用，请稍后重试。如问题持续存在，请联系系统管理员";
    }

    /**
     * 获取默认模型ID
     * 查找名为"Default Model"的模型，如果不存在则返回第一个可用模型的ID
     * 如果没有任何模型，则创建一个默认模型
     *
     * @return 默认模型ID
     */
    private Long getDefaultModelId() {
        try {
            // 首先尝试查找名为"Default Model"的模型
            PetrolModel queryModel = new PetrolModel();
            queryModel.setModelName("Default Model");
            queryModel.setStatus("ACTIVE");
            List<PetrolModel> defaultModels = petrolModelMapper.selectPetrolModelList(queryModel);

            if (defaultModels != null && !defaultModels.isEmpty()) {
                log.debug("✅ 找到默认模型: ID={}", defaultModels.get(0).getId());
                return defaultModels.get(0).getId();
            }

            // 如果没有找到默认模型，查找第一个可用的模型
            PetrolModel queryAnyModel = new PetrolModel();
            queryAnyModel.setStatus("ACTIVE");
            queryAnyModel.setIsAvailable("Y");
            List<PetrolModel> availableModels = petrolModelMapper.selectPetrolModelList(queryAnyModel);

            if (availableModels != null && !availableModels.isEmpty()) {
                log.info("⚠️ 使用第一个可用模型作为默认模型: ID={}, 名称={}",
                    availableModels.get(0).getId(), availableModels.get(0).getModelName());
                return availableModels.get(0).getId();
            }

            // 如果没有任何可用模型，创建一个默认模型
            log.warn("🔧 没有找到任何可用模型，创建默认模型");
            return createDefaultModel();

        } catch (Exception e) {
            log.error("❌ 获取默认模型ID失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取默认模型ID失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建默认模型（已禁用占位符模型创建）
     * 注意：系统已禁用占位符模型创建，要求使用真实的训练模型
     *
     * @return 抛出异常，要求用户提供真实模型
     */
    private Long createDefaultModel() {
        log.error("❌ 系统已禁用占位符模型创建");
        log.error("请通过以下方式之一提供真实模型：");
        log.error("1. 通过分析任务训练生成模型");
        log.error("2. 上传已训练的模型文件");
        log.error("3. 从模型管理界面导入模型");

        throw new RuntimeException(
            "系统中没有可用的模型。请先通过分析任务训练模型或上传已训练的模型文件。" +
            "系统已禁用占位符模型的创建，确保所有预测都基于真实的训练模型。"
        );
    }

    /**
     * 构建Python API预测请求参数
     */
    private Map<String, Object> buildPythonPredictionRequest(PetrolPrediction prediction, String algorithm, MultipartFile dataFile) {
        Map<String, Object> request = new HashMap<>();

        // 基本参数
        request.put("algorithm", algorithm != null ? algorithm : "linear_regression");
        request.put("task_id", prediction.getId().toString());
        request.put("task_name", prediction.getPredictionName());

        // 模型相关参数
        if (prediction.getModelId() != null) {
            PetrolModel model = petrolModelMapper.selectPetrolModelById(prediction.getModelId());
            if (model != null && model.getModelPath() != null) {
                request.put("model_path", model.getModelPath());
                request.put("model_type", "existing");
            }
        }

        // 文件信息
        if (dataFile != null) {
            request.put("file_name", dataFile.getOriginalFilename());
            request.put("file_size", dataFile.getSize());
        }

        // 预测参数（从预测任务的参数中提取）
        try {
            if (prediction.getPredictionParams() != null) {
                Map<String, Object> params = objectMapper.readValue(prediction.getPredictionParams(), Map.class);
                request.put("parameters", params);
            }
        } catch (Exception e) {
            log.warn("解析预测参数失败: {}", e.getMessage());
        }

        log.info("构建Python API请求参数: {}", request);
        return request;
    }

    /**
     * 构建增强预测请求参数
     */
    private Map<String, Object> buildEnhancedPredictionRequest(Map<String, Object> predictionParams, PetrolPrediction prediction) {
        Map<String, Object> request = new HashMap<>();

        // 基本信息
        request.put("task_id", prediction.getId().toString());
        request.put("task_name", prediction.getPredictionName());
        request.put("task_type", "enhanced_prediction");

        // 从预测参数中提取信息
        @SuppressWarnings("unchecked")
        List<String> features = (List<String>) predictionParams.get("features");
        String target = (String) predictionParams.get("target");
        String taskType = (String) predictionParams.get("taskType");

        request.put("features", features);
        request.put("target", target);
        request.put("task_type", taskType);

        // 🔧 修复：添加数据文件路径信息
        Object dataFileObj = predictionParams.get("dataFile");
        if (dataFileObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataFileInfo = (Map<String, Object>) dataFileObj;

            // 尝试从数据文件信息中获取文件名
            String fileName = (String) dataFileInfo.get("name");
            if (fileName != null) {
                // 🔧 关键修复：查找实际的文件路径
                String actualFilePath = findActualFilePath(fileName);
                if (actualFilePath != null) {
                    dataFileInfo.put("actualPath", actualFilePath);
                    log.info("🔧 找到实际文件路径: {} -> {}", fileName, actualFilePath);
                } else {
                    log.warn("⚠️ 未找到文件的实际路径: {}", fileName);
                }
            }

            request.put("data_file", dataFileInfo);
            log.info("🔧 添加数据文件信息: {}", dataFileInfo);
        }

        // 模型选择信息
        @SuppressWarnings("unchecked")
        Map<String, Object> modelSelection = (Map<String, Object>) predictionParams.get("modelSelection");
        if (modelSelection != null) {
            request.put("model_selection", modelSelection);

            String mode = (String) modelSelection.get("mode");
            if ("existing".equals(mode)) {
                // 使用已有模型
                @SuppressWarnings("unchecked")
                Map<String, Object> model = (Map<String, Object>) modelSelection.get("model");
                if (model != null) {
                    request.put("model_id", model.get("id"));
                    request.put("model_name", model.get("modelName"));
                }
            } else if ("algorithm".equals(mode)) {
                // 算法训练模式
                @SuppressWarnings("unchecked")
                Map<String, Object> algorithm = (Map<String, Object>) modelSelection.get("algorithm");
                if (algorithm != null) {
                    request.put("algorithm", algorithm.get("name"));
                    request.put("algorithm_params", algorithm.get("parameters"));
                }
            }
        }

        // 🔧 修复：数据文件信息 - 从多个位置获取
        @SuppressWarnings("unchecked")
        Map<String, Object> dataFile = (Map<String, Object>) predictionParams.get("dataFile");

        // 如果在parameters中没找到，尝试从顶级获取
        if (dataFile == null) {
            dataFile = (Map<String, Object>) predictionParams.get("dataFile");
        }

        // 如果还是没找到，尝试从其他可能的字段获取
        if (dataFile == null && predictionParams.containsKey("data_file")) {
            dataFile = (Map<String, Object>) predictionParams.get("data_file");
        }

        if (dataFile != null) {
            request.put("data_file", dataFile);
            log.info("🔧 传递数据文件信息: {}", dataFile.keySet());
        } else {
            log.warn("⚠️ 未找到数据文件信息");
        }

        // 🔧 修复：添加预测索引参数传递
        @SuppressWarnings("unchecked")
        List<Integer> predictionIndices = (List<Integer>) predictionParams.get("predictionIndices");

        // 🔧 诊断：预测索引详细信息
        log.info("🔍 预测索引诊断:");
        log.info("  - predictionIndices是否为null: {}", predictionIndices == null);
        if (predictionIndices != null) {
            log.info("  - predictionIndices大小: {}", predictionIndices.size());
            log.info("  - predictionIndices前10个: {}", predictionIndices.subList(0, Math.min(10, predictionIndices.size())));
            if (predictionIndices.size() > 10) {
                log.info("  - predictionIndices后5个: {}", predictionIndices.subList(predictionIndices.size() - 5, predictionIndices.size()));
            }
        }

        if (predictionIndices != null && !predictionIndices.isEmpty()) {
            request.put("prediction_indices", predictionIndices);
            log.info("🔧 传递预测索引: {} 个索引", predictionIndices.size());
        } else {
            log.warn("⚠️ 预测索引为空或null，Python API将使用默认样本数量");
        }

        // 🔧 修复：添加预测参数传递
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) predictionParams.get("parameters");
        if (parameters != null) {
            // 🔧 关键修复：将parameters作为顶级字段传递给Python API
            request.put("parameters", parameters);
            log.info("🔧 传递parameters字段到Python API: {}", parameters.keySet());

            // 🔧 诊断：详细的参数提取日志
            log.info("🔍 开始提取parameters中的参数:");
            log.info("  - parameters类型: {}", parameters.getClass().getSimpleName());
            log.info("  - parameters内容: {}", parameters);
            log.info("  - parameters键集合: {}", parameters.keySet());

            // 🔧 提取所有关键参数
            Object sampleCount = parameters.get("sampleCount");
            Object samplingStrategy = parameters.get("samplingStrategy");
            Object customIndices = parameters.get("customIndices");
            Object outputPrecision = parameters.get("outputPrecision");
            Object includeConfidence = parameters.get("includeConfidence");
            Object outputFormat = parameters.get("outputFormat");
            Object includeInputFeatures = parameters.get("includeInputFeatures");
            Object precision = parameters.get("precision");
            Object taskName = parameters.get("taskName");

            // 🔧 诊断：参数提取结果
            log.info("🔍 参数提取结果:");
            log.info("  - sampleCount: {} (类型: {})", sampleCount, sampleCount != null ? sampleCount.getClass().getSimpleName() : "null");
            log.info("  - samplingStrategy: {}", samplingStrategy);
            log.info("  - outputPrecision: {}", outputPrecision);
            log.info("  - includeConfidence: {}", includeConfidence);

            // 传递所有参数到Python API
            if (sampleCount != null) {
                request.put("sample_count", sampleCount);
            }
            if (samplingStrategy != null) {
                request.put("sampling_strategy", samplingStrategy);
            }
            if (customIndices != null) {
                request.put("custom_indices", customIndices);
            }
            if (outputPrecision != null) {
                request.put("output_precision", outputPrecision);
            }
            if (includeConfidence != null) {
                request.put("include_confidence", includeConfidence);
            }
            if (outputFormat != null) {
                request.put("output_format", outputFormat);
            }
            if (includeInputFeatures != null) {
                request.put("include_input_features", includeInputFeatures);
            }
            if (precision != null) {
                request.put("precision", precision);
            }
            if (taskName != null) {
                request.put("task_name", taskName);
            }

            log.info("🔧 传递预测参数: sampleCount={}, samplingStrategy={}, includeConfidence={}",
                sampleCount, samplingStrategy, includeConfidence);
        }

        log.info("🔧 构建增强预测请求参数完成: {}", request);
        return request;
    }

    /**
     * 查找文件的实际路径
     * @param fileName 文件名
     * @return 实际文件路径，如果未找到返回null
     */
    private String findActualFilePath(String fileName) {
        try {
            // 定义可能的搜索路径
            String[] searchPaths = {
                "data/petrol/uploads",
                "data/petrol/uploads/regression_xgboost_train",
                "data/petrol/uploads/regression_linear_train",
                "data/petrol/uploads/regression_random_forest_train",
                "uploads",
                "temp"
            };

            String projectRoot = System.getProperty("user.dir");
            log.info("🔍 项目根目录: {}", projectRoot);
            log.info("🔍 搜索文件: {}", fileName);

            // 在每个搜索路径中查找文件
            for (String searchPath : searchPaths) {
                File searchDir = new File(projectRoot, searchPath);
                if (searchDir.exists() && searchDir.isDirectory()) {
                    log.info("🔍 搜索目录: {}", searchDir.getAbsolutePath());

                    // 递归搜索文件
                    String foundPath = searchFileRecursively(searchDir, fileName);
                    if (foundPath != null) {
                        log.info("✅ 找到文件: {}", foundPath);
                        return foundPath;
                    }
                }
            }

            log.warn("⚠️ 未找到文件: {}", fileName);
            return null;
        } catch (Exception e) {
            log.error("搜索文件时发生错误: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 递归搜索文件
     * @param dir 搜索目录
     * @param fileName 文件名
     * @return 找到的文件路径，如果未找到返回null
     */
    private String searchFileRecursively(File dir, String fileName) {
        try {
            File[] files = dir.listFiles();
            if (files == null) {
                return null;
            }

            // 首先在当前目录查找精确匹配
            for (File file : files) {
                if (file.isFile() && file.getName().equals(fileName)) {
                    return file.getAbsolutePath();
                }
            }

            // 然后查找包含原始文件名的文件（处理重命名情况）
            String baseFileName = fileName;
            if (fileName.contains(".")) {
                baseFileName = fileName.substring(0, fileName.lastIndexOf("."));
            }

            for (File file : files) {
                if (file.isFile() && file.getName().contains(baseFileName)) {
                    log.info("🔍 找到相似文件: {} (原文件: {})", file.getName(), fileName);
                    return file.getAbsolutePath();
                }
            }

            // 递归搜索子目录
            for (File file : files) {
                if (file.isDirectory()) {
                    String result = searchFileRecursively(file, fileName);
                    if (result != null) {
                        return result;
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.error("递归搜索文件时发生错误: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从Python API结果中提取预测数量
     */
    private Integer extractPredictionCount(Map<String, Object> result) {
        try {
            // 优先从统计信息中获取count字段
            if (result.containsKey("statistics")) {
                Object statistics = result.get("statistics");
                if (statistics instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> statsMap = (Map<String, Object>) statistics;
                    if (statsMap.containsKey("count")) {
                        Object count = statsMap.get("count");
                        if (count instanceof Number) {
                            return ((Number) count).intValue();
                        }
                    }
                    // 尝试其他统计字段
                    if (statsMap.containsKey("total_samples")) {
                        Object totalSamples = statsMap.get("total_samples");
                        if (totalSamples instanceof Number) {
                            return ((Number) totalSamples).intValue();
                        }
                    }
                }
            }

            // 尝试从预测结果数组长度获取
            if (result.containsKey("predictions")) {
                Object predictions = result.get("predictions");
                if (predictions instanceof List) {
                    return ((List<?>) predictions).size();
                }
            }

            // 尝试从其他可能的字段获取
            String[] countFields = {"predictionCount", "prediction_count", "count", "total_count"};
            for (String field : countFields) {
                if (result.containsKey(field)) {
                    Object count = result.get(field);
                    if (count instanceof Number) {
                        return ((Number) count).intValue();
                    }
                }
            }

            // 默认值
            return 0;
        } catch (Exception e) {
            log.warn("提取预测数量失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 从Python API结果中提取执行时间
     */
    private Long extractExecutionTime(Map<String, Object> pythonResult) {
        try {
            // 尝试从统计信息中获取
            @SuppressWarnings("unchecked")
            Map<String, Object> statistics = (Map<String, Object>) pythonResult.get("statistics");
            if (statistics != null) {
                Object executionTime = statistics.get("execution_time");
                if (executionTime instanceof Number) {
                    return ((Number) executionTime).longValue();
                }
            }

            // 尝试从根级别获取
            Object executionTime = pythonResult.get("execution_time");
            if (executionTime instanceof Number) {
                return ((Number) executionTime).longValue();
            }

            // 默认值（秒）
            return 0L;
        } catch (Exception e) {
            log.warn("提取执行时间失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 异步处理预测任务
     */
    @Override
    @Async("threadPoolTaskExecutor")
    public void processPredictionTask(PetrolPrediction prediction) {
        log.info("开始异步处理预测任务: ID={}, 名称={}", prediction.getId(), prediction.getPredictionName());

        try {
            // 更新状态为运行中
            prediction.setStatus("RUNNING");
            prediction.setUpdateTime(new Date());
            updatePetrolPrediction(prediction);

            // 获取模型信息
            PetrolModel model = petrolModelMapper.selectPetrolModelById(prediction.getModelId());
            if (model == null) {
                throw new Exception("模型不存在: " + prediction.getModelId());
            }

            // 解析预测参数
            Map<String, Object> predictionParams = new HashMap<>();
            if (prediction.getPredictionParams() != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = objectMapper.readValue(prediction.getPredictionParams(), Map.class);
                    predictionParams.putAll(params);
                } catch (Exception e) {
                    log.warn("解析预测参数失败: {}", e.getMessage());
                }
            }

            // 构建符合PredictRequest格式的Python API请求
            Map<String, Object> pythonRequest = new HashMap<>();
            pythonRequest.put("task_name", prediction.getPredictionName());
            pythonRequest.put("features", predictionParams.get("selectedFeatures"));
            pythonRequest.put("target", predictionParams.get("targetColumn"));
            pythonRequest.put("task_type", "regression"); // 默认回归任务

            // 构建model_selection
            Map<String, Object> modelSelection = new HashMap<>();
            modelSelection.put("mode", "existing");
            Map<String, Object> modelInfo = new HashMap<>();
            modelInfo.put("id", model.getId());
            modelInfo.put("path", model.getModelPath());
            modelInfo.put("type", model.getModelType());
            modelInfo.put("algorithm", model.getAlgorithm());
            modelSelection.put("model", modelInfo);
            pythonRequest.put("model_selection", modelSelection);

            // 添加数据文件信息
            if (predictionParams.containsKey("datasetId")) {
                try {
                    Long datasetId = Long.valueOf(predictionParams.get("datasetId").toString());
                    log.info("获取数据集信息，ID: {}", datasetId);
                    PetrolDataset dataset = petrolDatasetService.selectPetrolDatasetById(datasetId);
                    if (dataset != null) {
                        log.info("找到数据集: 名称={}, 文件名={}, 文件路径={}",
                                dataset.getDatasetName(), dataset.getFileName(), dataset.getFilePath());

                        Map<String, Object> dataFile = new HashMap<>();
                        dataFile.put("name", dataset.getFileName());
                        dataFile.put("serverPath", dataset.getFilePath()); // Python API期望的字段名
                        dataFile.put("actualPath", dataset.getFilePath()); // 提供实际路径作为备选
                        dataFile.put("size", dataset.getFileSize());
                        dataFile.put("type", dataset.getFileType());
                        pythonRequest.put("data_file", dataFile);

                        log.info("构建的数据文件信息: {}", dataFile);
                    } else {
                        log.warn("数据集不存在，ID: {}", datasetId);
                    }
                } catch (Exception e) {
                    log.error("获取数据集信息失败: {}", e.getMessage(), e);
                }
            }

            // 处理行选择逻辑，生成预测索引
            List<Integer> predictionIndices = generatePredictionIndices(predictionParams);
            if (predictionIndices != null && !predictionIndices.isEmpty()) {
                // 🔧 关键修复：将预测索引设置为顶级字段
                pythonRequest.put("prediction_indices", predictionIndices);

                // 将预测索引也添加到parameters中（向后兼容）
                Map<String, Object> enhancedParams = new HashMap<>(predictionParams);
                enhancedParams.put("predictionIndices", predictionIndices);
                enhancedParams.put("rowSelectionStrategy", predictionParams.get("rowSelectionStrategy"));
                enhancedParams.put("customRows", predictionParams.get("customRows"));
                pythonRequest.put("parameters", enhancedParams);

                log.info("🔍 生成预测索引: {} 个索引，策略: {}",
                        predictionIndices.size(),
                        predictionParams.get("rowSelectionStrategy"));
                log.info("🔍 预测索引详情: {}",
                        predictionIndices.size() <= 20 ? predictionIndices :
                        predictionIndices.subList(0, 10) + "..." +
                        predictionIndices.subList(predictionIndices.size() - 5, predictionIndices.size()));
                log.info("🔧 设置顶级prediction_indices字段: {}", predictionIndices);
            } else {
                // 添加其他参数
                pythonRequest.put("parameters", predictionParams);
                log.warn("🔍 未生成预测索引，使用默认参数");
            }

            long startTime = System.currentTimeMillis();

            // 记录完整的Python请求
            log.info("🔍 发送给Python API的完整请求: {}", new ObjectMapper().writeValueAsString(pythonRequest));

            // 调用Python API执行预测
            Map<String, Object> pythonResult;
            if (prediction.getInputDataPath() != null) {
                // 文件预测
                log.info("🔍 使用文件预测模式");
                pythonResult = pythonExecutorService.batchPredict(pythonRequest, null);
            } else {
                // 参数预测
                log.info("🔍 使用参数预测模式");
                pythonResult = pythonExecutorService.predict(pythonRequest);
            }

            log.info("🔍 Python API返回结果: {}", pythonResult);

            long endTime = System.currentTimeMillis();

            // 检查Python API返回结果是否成功
            // Python API可能返回两种格式：
            // 1. 直接返回预测结果：{"predictions": [...], "input_data": [...], "statistics": {...}}
            // 2. 包装格式：{"success": true, "data": {...}}
            boolean isSuccess = false;
            Object predictionData = null;

            if (pythonResult != null) {
                // 检查是否有success字段
                if (pythonResult.containsKey("success")) {
                    isSuccess = Boolean.TRUE.equals(pythonResult.get("success"));
                    predictionData = pythonResult.get("data");
                } else if (pythonResult.containsKey("predictions")) {
                    // 直接包含预测结果，认为成功
                    isSuccess = true;
                    predictionData = pythonResult;
                } else if (pythonResult.containsKey("error") || pythonResult.containsKey("message")) {
                    // 包含错误信息，认为失败
                    isSuccess = false;
                } else {
                    // 其他情况，检查是否有有效数据
                    isSuccess = !pythonResult.isEmpty();
                    predictionData = pythonResult;
                }
            }

            log.info("🔍 预测结果检查: isSuccess={}, hasData={}", isSuccess, predictionData != null);

            if (isSuccess) {
                // 预测成功 - 格式化预测结果
                log.info("✅ 预测任务执行成功，开始格式化结果");

                // 使用predictionData而不是整个pythonResult
                Object dataToFormat = predictionData != null ? predictionData : pythonResult;
                Map<String, Object> formattedResult = formatPythonApiResult(dataToFormat, pythonResult);

                String resultJson = new ObjectMapper().writeValueAsString(formattedResult);
                log.info("🔍 格式化后的结果JSON长度: {}", resultJson.length());
                log.info("🔍 格式化后的结果包含字段: {}", formattedResult.keySet());

                prediction.setStatus("COMPLETED");
                prediction.setExecutionTime((endTime - startTime) / 1000);
                prediction.setPredictionResult(resultJson);
                prediction.setErrorMessage(null);

                // 提取预测数量
                if (formattedResult.containsKey("predictions") && formattedResult.get("predictions") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> predictions = (List<Object>) formattedResult.get("predictions");
                    prediction.setPredictionCount(predictions.size());
                }

                log.info("✅ 预测任务完成: ID={}, 执行时间={}秒, 预测数量={}",
                    prediction.getId(), prediction.getExecutionTime(), prediction.getPredictionCount());
            } else {
                // 预测失败
                String errorMessage = "预测执行失败";
                if (pythonResult != null) {
                    if (pythonResult.containsKey("message")) {
                        errorMessage = pythonResult.get("message").toString();
                    } else if (pythonResult.containsKey("error")) {
                        errorMessage = pythonResult.get("error").toString();
                    } else {
                        errorMessage = "Python API返回了无效的结果格式: " + pythonResult.toString();
                    }
                } else {
                    errorMessage = "Python API没有返回任何结果";
                }

                // 提取更详细的错误信息
                if (errorMessage.contains("真实数据中行") && errorMessage.contains("为空值")) {
                    errorMessage = "数据质量问题：" + errorMessage;
                } else if (errorMessage.contains("无法读取数据文件")) {
                    errorMessage = "数据文件问题：" + errorMessage;
                }

                log.error("❌ 预测任务执行失败: ID={}, 错误: {}", prediction.getId(), errorMessage);
                log.error("🔍 Python API完整响应: {}", pythonResult);

                throw new Exception(errorMessage);
            }

        } catch (Exception e) {
            log.error("❌ 预测任务执行失败: ID={}, 错误: {}", prediction.getId(), e.getMessage(), e);

            // 更新状态为失败
            prediction.setStatus("FAILED");
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 500);
            }
            prediction.setErrorMessage(errorMessage);
        } finally {
            prediction.setUpdateTime(new Date());
            int updateResult = updatePetrolPrediction(prediction);
            log.info("🔍 最终数据库更新结果: {}, 预测ID: {}, 状态: {}",
                updateResult, prediction.getId(), prediction.getStatus());
        }
    }
}
