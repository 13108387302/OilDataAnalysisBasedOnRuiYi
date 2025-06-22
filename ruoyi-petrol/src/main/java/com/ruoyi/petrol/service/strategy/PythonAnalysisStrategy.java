package com.ruoyi.petrol.service.strategy;

import com.ruoyi.petrol.domain.AnalysisTask;
import com.ruoyi.petrol.service.PythonExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 一个通用的分析策略，用于调用 Python 微服务执行任务。
 * 这个策略本身不对应任何一个具体的算法，而是作为所有Python算法的代理。
 */
@Component("pythonAnalysisStrategy") // 给这个 bean 一个固定的名字
public class PythonAnalysisStrategy implements AnalysisStrategy {

    private final PythonExecutorService pythonExecutorService;

    @Autowired
    public PythonAnalysisStrategy(PythonExecutorService pythonExecutorService) {
        this.pythonExecutorService = pythonExecutorService;
    }

    @Override
    public String execute(AnalysisTask task) throws Exception {
        // 直接调用 Python 执行器服务，它会处理与 FastAPI 的所有通信
        // 第一个参数 scriptModuleName 在新架构下已无意义，可以传递 null 或算法名
        // 第二个参数 pythonAlgorithmName 同样无意义
        return pythonExecutorService.executeScript(task.getAlgorithm(), task.getAlgorithm(), task);
    }

    @Override
    public String getStrategyName() {
        // 这个策略是通用的，不返回特定的名称，因此工厂需要特殊处理它。
        return null;
    }
} 