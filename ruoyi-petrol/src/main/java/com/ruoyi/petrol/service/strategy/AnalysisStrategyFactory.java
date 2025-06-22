package com.ruoyi.petrol.service.strategy;

import com.ruoyi.common.enums.AlgorithmType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

@Component
public class AnalysisStrategyFactory implements InitializingBean {

    private final List<AnalysisStrategy> strategies;
    private final Map<String, AnalysisStrategy> strategyMap = new ConcurrentHashMap<>();

    @Autowired
    private PythonAnalysisStrategy pythonAnalysisStrategy;

    @Autowired
    public AnalysisStrategyFactory(List<AnalysisStrategy> strategies) {
        this.strategies = strategies;
    }

    @Override
    public void afterPropertiesSet() {
        if (strategies != null) {
            for (AnalysisStrategy strategy : strategies) {
                if (strategy.getStrategyName() != null) {
                    strategyMap.put(strategy.getStrategyName(), strategy);
                }
            }
        }
    }

    public AnalysisStrategy getStrategy(String strategyName) {
        AnalysisStrategy strategy = strategyMap.get(strategyName);
        if (strategy != null) {
            return strategy;
        }
        
        return pythonAnalysisStrategy;
    }
} 