package com.ruoyi.petrol.service.impl;

import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.petrol.domain.AnalysisTask;
import com.ruoyi.petrol.domain.dto.AnalysisTaskUpdateDTO;
import com.ruoyi.petrol.domain.dto.FileAnalysisResult;
import com.ruoyi.petrol.mapper.AnalysisTaskMapper;
import com.ruoyi.petrol.service.IAnalysisTaskService;
import com.ruoyi.petrol.service.IAnalysisManagerService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;
import com.ruoyi.common.config.RuoYiConfig;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.petrol.domain.dto.ColumnStats;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.nio.file.Paths;

/**
 * 分析任务Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-06-18
 */
@Service
public class AnalysisTaskServiceImpl implements IAnalysisTaskService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTaskServiceImpl.class);

    @Autowired
    private AnalysisTaskMapper analysisTaskMapper;
    
    @Autowired
    private RuoYiConfig ruoYiConfig;

    private final IAnalysisManagerService analysisManagerService;

    // 使用 @Lazy 注解解决循环依赖问题
    @Autowired
    public AnalysisTaskServiceImpl(@Lazy IAnalysisManagerService analysisManagerService) {
        this.analysisManagerService = analysisManagerService;
    }

    /**
     * 查询分析任务
     * 
     * @param id 分析任务主键
     * @return 分析任务
     */
    @Override
    public AnalysisTask selectAnalysisTaskById(Long id) {
        return analysisTaskMapper.selectAnalysisTaskById(id);
    }

    /**
     * 查询分析任务列表
     * 
     * @param analysisTask 分析任务
     * @return 分析任务
     */
    @Override
    public List<AnalysisTask> selectAnalysisTaskList(AnalysisTask analysisTask) {
        return analysisTaskMapper.selectAnalysisTaskList(analysisTask);
    }

    /**
     * 新增分析任务
     * 
     * @param analysisTask 分析任务
     * @return 结果
     */
    @Override
    public int insertAnalysisTask(AnalysisTask analysisTask) {
        analysisTask.setCreateTime(DateUtils.getNowDate());
        return analysisTaskMapper.insertAnalysisTask(analysisTask);
    }

    /**
     * 修改分析任务
     * 
     * @param analysisTask 分析任务
     * @return 结果
     */
    @Override
    public int updateAnalysisTask(AnalysisTask analysisTask) {
        analysisTask.setUpdateTime(DateUtils.getNowDate());
        return analysisTaskMapper.updateAnalysisTask(analysisTask);
    }

    /**
     * 修改分析任务，并触发重新处理
     *
     * @param taskUpdateDTO 前端传递的更新数据
     * @return 结果
     */
    @Override
    @Transactional
    public int updateTaskParams(AnalysisTaskUpdateDTO taskUpdateDTO)
    {
        // 1. 从数据库加载原始任务实体
        AnalysisTask existingTask = analysisTaskMapper.selectAnalysisTaskById(taskUpdateDTO.getId());
        if (existingTask == null) {
            throw new RuntimeException("任务不存在, ID: " + taskUpdateDTO.getId());
        }

        // 2. 清理旧的输出目录，防止孤儿文件
        if (existingTask.getOutputDirPath() != null && !existingTask.getOutputDirPath().isEmpty()) {
            try {
                String absolutePath = getAbsoluteOutputPath(existingTask.getOutputDirPath());
                if (absolutePath != null) {
                    File oldDirectory = new File(absolutePath);
                    if (oldDirectory.exists()) {
                        FileUtils.deleteDirectory(oldDirectory);
                        log.info("成功清理任务ID {} 的旧结果文件夹: {}", taskUpdateDTO.getId(), absolutePath);
                    }
                }
            } catch (Exception e) {
                log.warn("清理任务ID {} 的旧结果文件夹时发生异常: {}", taskUpdateDTO.getId(), existingTask.getOutputDirPath(), e);
                // 不抛出异常，允许继续执行
            }
        }

        // 3. 检查算法是否改变，如果改变需要重新处理文件路径
        String oldAlgorithm = existingTask.getAlgorithm();
        String newAlgorithm = taskUpdateDTO.getAlgorithm();
        boolean algorithmChanged = !oldAlgorithm.equals(newAlgorithm);

        // 4. 使用 DTO 中的数据更新任务实体
        existingTask.setTaskName(taskUpdateDTO.getTaskName());
        existingTask.setAlgorithm(taskUpdateDTO.getAlgorithm());
        existingTask.setRemark(taskUpdateDTO.getRemark());

        // 关键：序列化新的参数到 JSON 字符串
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            existingTask.setInputParamsJson(objectMapper.writeValueAsString(taskUpdateDTO.getParams()));
        } catch (JsonProcessingException e) {
            log.error("序列化任务参数失败", e);
            throw new RuntimeException("序列化任务参数失败", e);
        }

        // 5. 如果算法改变了，需要重新处理文件路径
        if (algorithmChanged) {
            try {
                // 获取原始文件的绝对路径
                String oldInputPath = getAbsoluteInputPath(existingTask.getInputFilePath());
                File originalFile = new File(oldInputPath);

                if (originalFile.exists()) {
                    // 创建新的目录结构
                    String taskTimestampDir = String.valueOf(System.currentTimeMillis());
                    String inputFileSubDir = "petrol" + File.separator + "uploads" + File.separator + newAlgorithm + File.separator + taskTimestampDir;
                    String outputDirSubDir = "petrol" + File.separator + "results" + File.separator + newAlgorithm + File.separator + taskTimestampDir;

                    // 创建新的输入目录
                    String absoluteInputPath = ruoYiConfig.getProfile() + File.separator + inputFileSubDir;
                    File newInputDir = new File(absoluteInputPath);
                    if (!newInputDir.exists()) {
                        newInputDir.mkdirs();
                    }

                    // 复制文件到新位置
                    String originalFileName = originalFile.getName();
                    File newInputFile = new File(newInputDir, originalFileName);
                    Files.copy(originalFile.toPath(), newInputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // 更新文件路径
                    String newInputFilePath = "/profile/" + inputFileSubDir.replace(File.separator, "/") + "/" + originalFileName;
                    String newOutputDirPath = "/profile/" + outputDirSubDir.replace(File.separator, "/");

                    existingTask.setInputFilePath(newInputFilePath);
                    existingTask.setOutputDirPath(newOutputDirPath);

                    log.info("算法改变，已将文件从 {} 复制到 {}", oldInputPath, newInputFile.getAbsolutePath());
                } else {
                    log.warn("原始文件不存在: {}, 保持原有路径", oldInputPath);
                    // 清空输出路径，让processTask重新生成
                    existingTask.setOutputDirPath(null);
                }
            } catch (Exception e) {
                log.error("处理文件路径时发生错误", e);
                // 如果文件处理失败，至少清空输出路径
                existingTask.setOutputDirPath(null);
            }
        } else {
            // 算法未改变，只清空输出路径和结果
            existingTask.setOutputDirPath(null);
        }

        // 6. 清空旧的结果，让processTask重新生成
        existingTask.setResultsJson(null);
        existingTask.setErrorMessage(null);

        // 5. 将更新后的任务状态重置为 PENDING，并保存到数据库
        existingTask.setStatus("PENDING");
        existingTask.setUpdateTime(DateUtils.getNowDate());
        analysisTaskMapper.updateAnalysisTask(existingTask);

        // 6. 异步调用分析管理器，使用更新后的任务实体重新处理
        analysisManagerService.processTask(existingTask);

        return 1;
    }

    private String getAbsoluteOutputPath(String profilePath) throws IOException {
        if (profilePath == null || !profilePath.startsWith("/profile/")) {
            return null;
        }
        // 移除/profile/前缀
        String relativePath = profilePath.substring("/profile/".length());

        // 使用 Paths.get() 来健壮地拼接路径，避免斜杠问题
        return java.nio.file.Paths.get(ruoYiConfig.getProfile(), relativePath).toFile().getCanonicalPath();
    }

    private String getAbsoluteInputPath(String profilePath) throws IOException {
        if (profilePath == null || !profilePath.startsWith("/profile/")) {
            return null;
        }
        // 移除/profile/前缀
        String relativePath = profilePath.substring("/profile/".length());

        // 使用 Paths.get() 来健壮地拼接路径，避免斜杠问题
        return java.nio.file.Paths.get(ruoYiConfig.getProfile(), relativePath).toFile().getCanonicalPath();
    }



    /**
     * 批量删除分析任务
     * 
     * @param ids 需要删除的分析任务主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteAnalysisTaskByIds(Long[] ids) {
        for (Long id : ids) {
            // 先从数据库查询到完整的任务信息
            AnalysisTask task = analysisTaskMapper.selectAnalysisTaskById(id);
            if (task == null) {
                continue;
            }

            // 1. 删除输出结果文件夹
            if (task.getOutputDirPath() != null && !task.getOutputDirPath().isEmpty()) {
                try {
                    String absolutePath = getAbsoluteOutputPath(task.getOutputDirPath());
                    if (absolutePath != null) {
                        File directory = new File(absolutePath);
                        if (directory.exists()) {
                            FileUtils.deleteDirectory(directory);
                            log.info("成功删除任务ID {} 的结果文件夹: {}", id, absolutePath);
                        }
                    }
                } catch (Exception e) {
                    log.error("删除任务ID {} 的结果文件夹时发生异常: {}", id, task.getOutputDirPath(), e);
                }
            }
            
            // 2. 删除原始输入文件所在的文件夹
            if (task.getInputFilePath() != null && !task.getInputFilePath().isEmpty()) {
                try {
                    String absolutePath = getAbsoluteOutputPath(task.getInputFilePath());
                    if (absolutePath != null) {
                        File inputFile = new File(absolutePath);
                        // 我们删除的是包含输入文件的整个目录
                        if (inputFile.exists() && inputFile.getParentFile().isDirectory()) {
                            FileUtils.deleteDirectory(inputFile.getParentFile());
                             log.info("成功删除任务ID {} 的原始输入文件夹: {}", id, inputFile.getParent());
                        }
                    }
                } catch (Exception e) {
                    log.error("删除任务ID {} 的原始输入文件夹时发生异常: {}", id, task.getInputFilePath(), e);
                }
            }
        }
        // 统一删除数据库记录
        return analysisTaskMapper.deleteAnalysisTaskByIds(ids);
    }

    /**
     * 删除分析任务信息
     *
     * @param id 分析任务主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteAnalysisTaskById(Long id)
    {
        AnalysisTask task = analysisTaskMapper.selectAnalysisTaskById(id);
        if (task != null) {
            // 1. 删除输出结果文件夹
            if (task.getOutputDirPath() != null && !task.getOutputDirPath().isEmpty()) {
                try {
                    String absolutePath = getAbsoluteOutputPath(task.getOutputDirPath());
                    if (absolutePath != null) {
                        File directory = new File(absolutePath);
                        if (directory.exists()) {
                            FileUtils.deleteDirectory(directory);
                            log.info("成功删除任务ID {} 的结果文件夹: {}", id, absolutePath);
                        }
                    }
                } catch (Exception e) {
                    log.error("删除任务ID {} 的结果文件夹时发生异常: {}", id, task.getOutputDirPath(), e);
                }
            }
            
            // 2. 删除原始输入文件所在的文件夹
            if (task.getInputFilePath() != null && !task.getInputFilePath().isEmpty()) {
                try {
                    String absolutePath = getAbsoluteOutputPath(task.getInputFilePath());
                    if (absolutePath != null) {
                        File inputFile = new File(absolutePath);
                        if (inputFile.exists() && inputFile.getParentFile().isDirectory()) {
                            FileUtils.deleteDirectory(inputFile.getParentFile());
                             log.info("成功删除任务ID {} 的原始输入文件夹: {}", id, inputFile.getParent());
                        }
                    }
                } catch (Exception e) {
                    log.error("删除任务ID {} 的原始输入文件夹时发生异常: {}", id, task.getInputFilePath(), e);
                }
            }
        }
        return analysisTaskMapper.deleteAnalysisTaskById(id);
    }

    @Override
    public AnalysisTask submitAnalysisTask(AnalysisTask analysisTask) {
        analysisTask.setCreateTime(DateUtils.getNowDate());
        analysisTask.setCreateBy(SecurityUtils.getUsername());
        analysisTask.setStatus("PENDING");
        analysisTaskMapper.insertAnalysisTask(analysisTask);

        // 调用任务管理器开始异步处理
        analysisManagerService.processTask(analysisTask);
        
        return analysisTask;
    }

    @Override
    public FileAnalysisResult analyzeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        List<String> headers = new ArrayList<>();
        Map<String, ColumnStats> stats = new HashMap<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalStateException("文件中没有找到工作表");
            }

            // 1. 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalStateException("文件中没有找到表头行");
            }
            headerRow.forEach(cell -> {
                String header = cell.getStringCellValue();
                headers.add(header);
                stats.put(header, new ColumnStats());
            });

            // 2. 遍历数据行以计算统计信息
            DataFormatter dataFormatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell == null) continue;

                    try {
                        // 尝试将单元格内容转换为double
                        double value = cell.getNumericCellValue();
                        String header = headers.get(j);
                        ColumnStats columnStats = stats.get(header);
                        
                        // 更新最小值
                        if (columnStats.getMin() == null || value < columnStats.getMin()) {
                            columnStats.setMin(value);
                        }
                        // 更新最大值
                        if (columnStats.getMax() == null || value > columnStats.getMax()) {
                            columnStats.setMax(value);
                        }
                    } catch (IllegalStateException | NumberFormatException e) {
                        // 如果单元格不是数值类型，则忽略
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            log.error("解析上传文件时发生IO异常", e);
            throw new RuntimeException("文件解析失败", e);
        }

        return new FileAnalysisResult(headers, stats);
    }
} 