package com.ruoyi.petrol.service;

import com.ruoyi.petrol.domain.AnalysisTask;

import java.util.Map;

/**
 * Python脚本执行器服务接口
 */
public interface PythonExecutorService {

    /**
     * 执行一个Python脚本模块
     *
     * @param scriptModuleName Python模块的名称 (例如, main_processor)
     * @param pythonAlgorithmName Python算法脚本的名称 (例如, regression.linear)
     * @param task 包含所有执行所需参数的分析任务对象
     * @return 脚本的标准输出
     * @throws Exception 如果脚本执行失败
     */
    String executeScript(String scriptModuleName, String pythonAlgorithmName, AnalysisTask task) throws Exception;
}