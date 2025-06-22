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

@Service("regression_random_forest_train")
public class RandomForestRegressionTrainStrategyImpl implements AnalysisStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RandomForestRegressionTrainStrategyImpl.class);

    @Autowired
    private PythonExecutorService pythonExecutorService;

    private static final String STRATEGY_NAME = "regression_random_forest_train";

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    public String getPythonScriptPath() {
        return "regression.random_forest";
    }

    @Override
    public String execute(AnalysisTask task) throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> params = new HashMap<>();

            if (task.getInputParamsJson() != null && !task.getInputParamsJson().isEmpty()) {
                params.putAll(objectMapper.readValue(task.getInputParamsJson(), new TypeReference<Map<String, Object>>() {}));
            }

            // Add algorithm-specific and default parameters
            params.putIfAbsent("n_estimators", 100);
            // 'max_depth' is optional, so we don't set a default if it's null, Python handles it.
            params.putIfAbsent("test_size", 0.2);
            params.putIfAbsent("random_state", 42);

            task.setInputParamsJson(objectMapper.writeValueAsString(params));

            return pythonExecutorService.executeScript("train_processor", getPythonScriptPath(), task);
        } catch (Exception e) {
            logger.error("执行Random Forest Regression训练策略失败", e);
            throw new Exception("执行Random Forest Regression训练策略失败", e);
        }
    }
} 