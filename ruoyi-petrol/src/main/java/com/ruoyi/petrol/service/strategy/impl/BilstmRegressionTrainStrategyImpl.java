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

@Service("regression_bilstm_train")
public class BilstmRegressionTrainStrategyImpl implements AnalysisStrategy {

    private static final Logger logger = LoggerFactory.getLogger(BilstmRegressionTrainStrategyImpl.class);

    @Autowired
    private PythonExecutorService pythonExecutorService;

    private static final String STRATEGY_NAME = "regression_bilstm_train";
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

            // 核心修正：直接使用前端传递的参数，不再覆盖
            if (task.getInputParamsJson() != null && !task.getInputParamsJson().isEmpty()) {
                params = objectMapper.readValue(task.getInputParamsJson(), new TypeReference<Map<String, Object>>() {});
            }

            // 验证关键参数是否存在
            if (!params.containsKey("target_column") || params.get("target_column") == null || params.get("target_column").toString().isEmpty()) {
                throw new IllegalArgumentException("关键参数 'target_column' 缺失或为空。");
            }
            if (!params.containsKey("feature_columns") || !(params.get("feature_columns") instanceof List) || ((List<?>)params.get("feature_columns")).isEmpty()) {
                throw new IllegalArgumentException("关键参数 'feature_columns' 缺失或不是一个有效的列表。");
            }

            // 更新任务中的参数（可选，但保持一致性）
            task.setInputParamsJson(objectMapper.writeValueAsString(params));

            return pythonExecutorService.executeScript("train_processor", getPythonScriptPath(), task);
        } catch (Exception e) {
            logger.error("执行BiLSTM回归训练策略失败: " + e.getMessage(), e);
            // 将原始异常信息传递出去，方便排查
            throw new Exception("执行BiLSTM回归训练策略失败: " + e.getMessage(), e);
        }
    }
} 