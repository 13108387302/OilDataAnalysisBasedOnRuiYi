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

@Service("regression_linear_regression_train")
public class LinearRegressionTrainStrategyImpl implements AnalysisStrategy {

    private static final Logger logger = LoggerFactory.getLogger(LinearRegressionTrainStrategyImpl.class);

    @Autowired
    private PythonExecutorService pythonExecutorService;

    private static final String STRATEGY_NAME = "regression_linear_regression_train";

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
            // If new 'feature_columns' isn't present, check for old 'x_column'
            if (!params.containsKey("feature_columns") && params.containsKey("x_column")) {
                params.put("feature_columns", Collections.singletonList(params.get("x_column")));
                params.remove("x_column"); // remove old key
            }

            // If new 'target_column' isn't present, check for old 'y_column'
            if (!params.containsKey("target_column") && params.containsKey("y_column")) {
                params.put("target_column", params.get("y_column"));
                params.remove("y_column"); // remove old key
            }
            
            // Add default parameters if they are absent
            params.putIfAbsent("test_size", 0.2);
            params.putIfAbsent("random_state", 42);

            task.setInputParamsJson(objectMapper.writeValueAsString(params));

            // Pass the specific algorithm script path to the executor
            return pythonExecutorService.executeScript("train_processor", getPythonScriptPath(), task);
        } catch (Exception e) {
            logger.error("执行Linear Regression训练策略失败", e);
            throw new Exception("执行Linear Regression训练策略失败", e);
        }
    }
} 