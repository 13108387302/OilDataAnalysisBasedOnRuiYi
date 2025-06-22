package com.ruoyi.petrol.service.strategy.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.petrol.domain.AnalysisTask;
import com.ruoyi.petrol.service.PythonExecutorService;
import com.ruoyi.petrol.service.strategy.AnalysisStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("predict_bilstm_predict")
public class BilstmRegressionPredictStrategyImpl implements AnalysisStrategy {

    private static final Logger logger = LoggerFactory.getLogger(BilstmRegressionPredictStrategyImpl.class);

    @Autowired
    private PythonExecutorService pythonExecutorService;

    private static final String STRATEGY_NAME = "predict_bilstm_predict";
    private static final String PYTHON_SCRIPT_PATH = "regression.bilstm";

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    public String getPythonScriptPath() {
        return PYTHON_SCRIPT_PATH;
    }

    @Override
    public String execute(AnalysisTask task) throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> params = new HashMap<>();

            if (task.getInputParamsJson() != null && !task.getInputParamsJson().isEmpty()) {
                Map<String, Object> existingParams = objectMapper.readValue(task.getInputParamsJson(), new TypeReference<Map<String, Object>>() {});
                params.putAll(existingParams);
            }

            List<String> featureColumns = objectMapper.readValue(task.getInputFileHeadersJson(), new TypeReference<List<String>>() {});
            params.put("feature_columns", featureColumns);

            if (!params.containsKey("model_path")) {
                logger.warn("任务 {} 的 model_path 参数未提供。", task.getId());
            }

            task.setInputParamsJson(objectMapper.writeValueAsString(params));

            return pythonExecutorService.executeScript("predict_processor", getPythonScriptPath(), task);
        } catch (Exception e) {
            logger.error("执行BiLSTM回归预测策略失败", e);
            throw new Exception("执行BiLSTM回归预测策略失败", e);
        }
    }
} 