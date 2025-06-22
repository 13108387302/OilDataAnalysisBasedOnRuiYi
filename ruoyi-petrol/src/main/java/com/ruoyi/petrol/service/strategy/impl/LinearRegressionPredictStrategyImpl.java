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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service("predict_linear_regression_predict")
public class LinearRegressionPredictStrategyImpl implements AnalysisStrategy {

    private static final Logger logger = LoggerFactory.getLogger(LinearRegressionPredictStrategyImpl.class);

    @Autowired
    private PythonExecutorService pythonExecutorService;

    private static final String STRATEGY_NAME = "predict_linear_regression_predict";

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    public String getPythonScriptPath() {
        return "regression.linear";
    }

    @Override
    public String execute(AnalysisTask task) throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> params = new HashMap<>();

            if (task.getInputParamsJson() != null && !task.getInputParamsJson().isEmpty()) {
                params.putAll(objectMapper.readValue(task.getInputParamsJson(), new TypeReference<Map<String, Object>>() {}));
            }

            // --- Backward Compatibility & Parameter Unification ---
            if (!params.containsKey("feature_columns") && params.containsKey("x_column")) {
                params.put("feature_columns", Collections.singletonList(params.get("x_column")));
                params.remove("x_column");
            }
            
            if (!params.containsKey("model_path")) {
                logger.warn("任务 {} 的 model_path 参数未提供。", task.getId());
                // Depending on requirements, you might want to throw an exception here
                // throw new IllegalArgumentException("Parameter 'model_path' is required for prediction.");
            }

            task.setInputParamsJson(objectMapper.writeValueAsString(params));

            return pythonExecutorService.executeScript("predict_processor", getPythonScriptPath(), task);
        } catch (Exception e) {
            logger.error("执行Linear Regression预测策略失败", e);
            throw new Exception("执行Linear Regression预测策略失败", e);
        }
    }
}