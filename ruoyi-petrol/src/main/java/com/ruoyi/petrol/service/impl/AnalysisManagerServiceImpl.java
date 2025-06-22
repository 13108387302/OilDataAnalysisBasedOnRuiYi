package com.ruoyi.petrol.service.impl;

import com.ruoyi.petrol.domain.AnalysisTask;
import com.ruoyi.petrol.service.IAnalysisManagerService;
import com.ruoyi.petrol.service.IAnalysisTaskService;
import com.ruoyi.petrol.service.strategy.AnalysisStrategy;
import com.ruoyi.petrol.service.strategy.AnalysisStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.apache.commons.io.FileUtils;
import com.ruoyi.common.config.RuoYiConfig;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

@Service
public class AnalysisManagerServiceImpl implements IAnalysisManagerService {
    private static final Logger log = LoggerFactory.getLogger(AnalysisManagerServiceImpl.class);

    private final AnalysisStrategyFactory strategyFactory;
    private final IAnalysisTaskService taskService;
    private final RuoYiConfig ruoYiConfig;

    @Autowired
    public AnalysisManagerServiceImpl(AnalysisStrategyFactory strategyFactory, IAnalysisTaskService taskService, RuoYiConfig ruoYiConfig) {
        this.strategyFactory = strategyFactory;
        this.taskService = taskService;
        this.ruoYiConfig = ruoYiConfig;
    }
    
    /**
     * 构建用于存放分析结果的目录的Web相对路径。
     * 规则: data/petrol/results/[算法名称]/[时间戳]/
     * @param algorithmName 算法名称
     * @return 配置文件根目录下的相对路径 (e.g., data/petrol/results/regression_lightgbm_train/1678886400000/)
     */
    private String buildResultsRelativePath(String algorithmName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        // 注意：这里不再包含 /profile 前缀，并且路径的根是 petrol，因为 profile 根目录已经是 data
        return Paths.get(
            "petrol", "results", algorithmName, timestamp
        ).toString().replace("\\", "/");
    }

    private String getAbsoluteOutputPath(String relativePath) throws IOException {
        if (relativePath == null) {
            return null;
        }
        // 直接与配置的根目录拼接
        return Paths.get(ruoYiConfig.getProfile(), relativePath).toFile().getCanonicalPath();
    }

    @Override
    @Async("threadPoolTaskExecutor")
    public void processTask(AnalysisTask task) {
        // --- 1. 强制设定和创建结果输出目录 ---
        // 先生成相对路径
        String resultsRelativePath = buildResultsRelativePath(task.getAlgorithm());
        // Web访问路径需要加上 /profile/ 前缀
        String resultsWebPath = "/profile/" + resultsRelativePath;
        task.setOutputDirPath(resultsWebPath);
        log.info("[任务ID: {}] 已设置结果输出目录 (Web): {}", task.getId(), resultsWebPath);

        try {
            // 使用相对路径获取绝对物理路径
            String absolutePath = getAbsoluteOutputPath(resultsRelativePath);
            if (absolutePath != null) {
                File outputDir = new File(absolutePath);
                // 确保目录存在
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                    log.info("[任务ID: {}] 已成功创建结果目录 (物理): {}", task.getId(), absolutePath);
                }
            }
        } catch (IOException e) {
            log.error("[任务ID: {}] 创建结果目录失败: [{}]. 错误: {}", task.getId(), resultsWebPath, e.getMessage(), e);
            task.setStatus("FAILED");
            task.setErrorMessage("无法创建结果输出目录: " + e.getMessage());
            taskService.updateAnalysisTask(task);
            return; // 关键步骤失败，提前退出
        }
        
        // --- 2. 清理旧目录 (如果需要) ---
        // 注意：由于我们现在每次都创建带时间戳的唯一目录，理论上不需要清理了。
        // 但保留此逻辑可以作为一种安全保障，以防万一。
        // if (task.getOutputDirPath() != null && !task.getOutputDirPath().isEmpty()) {
        // ... (原有的清理逻辑可以被注释或移除)
        // }
        
        task.setStatus("RUNNING");
        task.setUpdateTime(new Date());
        taskService.updateAnalysisTask(task);

        try {
            String algorithm = task.getAlgorithm();

            if (algorithm == null || algorithm.trim().isEmpty()) {
                throw new IllegalArgumentException("算法名称 (algorithm) 不能为空");
            }

            String strategyName = task.getAlgorithm();
            
            AnalysisStrategy strategy = strategyFactory.getStrategy(strategyName);
            if (strategy == null) {
                throw new Exception("不支持的分析策略: " + strategyName);
            }
            
            log.info("[任务ID: {}] 已匹配到算法策略: [{}], 准备执行。", task.getId(), strategyName);
            
            String resultJson = strategy.execute(task);

            task.setResultsJson(resultJson);
            task.setStatus("COMPLETED");
            task.setErrorMessage(null);

        } catch (Exception e) {
            log.error("任务处理失败 -> [FAILED], 任务ID: {}. 错误: {}", task.getId(), e.getMessage(), e);
            task.setStatus("FAILED");
            
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String errorMessage = rootCause.getMessage();
            // 关键修复：截断错误信息，防止超出数据库字段长度限制
            if (errorMessage != null && errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 500);
            }
            task.setErrorMessage(errorMessage);

        } finally {
            task.setUpdateTime(new Date());
            taskService.updateAnalysisTask(task);
        }
    }
} 