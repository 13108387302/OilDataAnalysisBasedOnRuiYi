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

@Service("feature_engineering_fractal_dimension_train")
public class FeatureEngineeringFractalDimensionStrategyImpl implements AnalysisStrategy {

    private static final Logger logger = LoggerFactory.getLogger(FeatureEngineeringFractalDimensionStrategyImpl.class);

    @Autowired
    private PythonExecutorService pythonExecutorService;

    private static final String STRATEGY_NAME = "feature_engineering_fractal_dimension_train";
    private static final String PYTHON_SCRIPT_PATH = "feature_engineering.fractal_dimension";

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
            // This algorithm does not have tunable parameters from the user side in this implementation
            // But we keep the structure for consistency
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> params = new HashMap<>();

            if (task.getInputParamsJson() != null && !task.getInputParamsJson().isEmpty()) {
                params.putAll(objectMapper.readValue(task.getInputParamsJson(), new TypeReference<Map<String, Object>>() {}));
            }

            task.setInputParamsJson(objectMapper.writeValueAsString(params));

            return pythonExecutorService.executeScript("train_processor", getPythonScriptPath(), task);
        } catch (Exception e) {
            logger.error("执行分形维数计算策略失败", e);
            throw new Exception("执行分形维数计算策略失败", e);
        }
    }
} 