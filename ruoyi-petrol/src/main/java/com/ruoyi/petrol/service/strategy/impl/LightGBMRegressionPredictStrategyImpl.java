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
import java.util.Map;

@Service("predict_lightgbm_regression_predict")
public class LightGBMRegressionPredictStrategyImpl implements AnalysisStrategy {

    private static final Logger logger = LoggerFactory.getLogger(LightGBMRegressionPredictStrategyImpl.class);

    @Autowired
    private PythonExecutorService pythonExecutorService;

    private static final String STRATEGY_NAME = "predict_lightgbm_regression_predict";

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    public String getPythonScriptPath() {
        return "regression.lightgbm";
    }

    @Override
    public String execute(AnalysisTask task) throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> params = new HashMap<>();

            if (task.getInputParamsJson() != null && !task.getInputParamsJson().isEmpty()) {
                params.putAll(objectMapper.readValue(task.getInputParamsJson(), new TypeReference<Map<String, Object>>() {}));
            }
            
            if (!params.containsKey("model_path")) {
                logger.warn("任务 {} 的 model_path 参数未提供。", task.getId());
            }

            task.setInputParamsJson(objectMapper.writeValueAsString(params));

            return pythonExecutorService.executeScript("predict_processor", getPythonScriptPath(), task);
        } catch (Exception e) {
            logger.error("执行LightGBM Regression预测策略失败", e);
            throw new Exception("执行LightGBM Regression预测策略失败", e);
        }
    }
} 