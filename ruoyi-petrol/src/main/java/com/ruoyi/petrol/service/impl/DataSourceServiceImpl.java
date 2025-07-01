package com.ruoyi.petrol.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.ruoyi.petrol.domain.AnalysisTask;
import com.ruoyi.petrol.domain.PetrolDataset;
import com.ruoyi.petrol.service.IAnalysisTaskService;
import com.ruoyi.petrol.service.IDataSourceService;
import com.ruoyi.petrol.service.IPetrolDatasetService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

/**
 * 统一数据源服务实现（只支持分析任务）
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@Service
public class DataSourceServiceImpl implements IDataSourceService {
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceServiceImpl.class);
    
    @Autowired
    private IAnalysisTaskService analysisTaskService;

    @Autowired
    private IPetrolDatasetService petrolDatasetService;

    @Value("${ruoyi.profile}")
    private String uploadPath;
    
    /**
     * 获取所有可用的数据源（包含分析任务和数据集）
     */
    @Override
    public List<Map<String, Object>> getAllDataSources() {
        List<Map<String, Object>> allSources = new ArrayList<>();

        try {
            // 1. 添加分析任务中的数据
            AnalysisTask taskQuery = new AnalysisTask();
            List<AnalysisTask> tasks = analysisTaskService.selectAnalysisTaskList(taskQuery);
            log.info("🔍 获取数据源列表 - 查询到的分析任务数量: {}", tasks.size());

            for (AnalysisTask task : tasks) {
                log.info("🔍 处理分析任务: ID={}, 名称={}, 文件路径={}",
                    task.getId(), task.getTaskName(), task.getInputFilePath());
                if (task.getInputFilePath() != null && !task.getInputFilePath().isEmpty()) {
                    Map<String, Object> source = new HashMap<>();
                    source.put("id", "task_" + task.getId());
                    source.put("name", task.getTaskName());
                    source.put("description", getTaskDescription(task));
                    source.put("fileName", extractFileName(task.getInputFilePath()));
                    source.put("algorithm", task.getAlgorithm());
                    source.put("status", task.getStatus());
                    source.put("createTime", task.getCreateTime());
                    source.put("sourceType", "task");
                    source.put("source", "分析任务");

                    // 添加任务特有的信息
                    source.put("hasResults", task.getResultsJson() != null && !task.getResultsJson().isEmpty());
                    source.put("outputDirPath", task.getOutputDirPath());

                    // 尝试从任务的头信息中获取列数
                    if (task.getInputFileHeadersJson() != null) {
                        try {
                            List<String> headers = JSON.parseArray(task.getInputFileHeadersJson(), String.class);
                            source.put("columnCount", headers.size());
                            source.put("rowCount", estimateRowCount(task.getInputFilePath()));
                        } catch (Exception e) {
                            log.warn("解析任务头信息失败: {}", task.getId());
                        }
                    }

                    allSources.add(source);
                    log.info("✅ 添加数据源: {}", source.get("name"));
                }
            }

            // 2. 添加数据集
            List<PetrolDataset> datasets = petrolDatasetService.selectAvailableDatasets();
            log.info("🔍 获取数据源列表 - 查询到的数据集数量: {}", datasets.size());

            for (PetrolDataset dataset : datasets) {
                log.info("🔍 处理数据集: ID={}, 名称={}, 文件路径={}",
                    dataset.getId(), dataset.getDatasetName(), dataset.getFilePath());
                if (dataset.getFilePath() != null && !dataset.getFilePath().isEmpty()) {
                    Map<String, Object> source = new HashMap<>();
                    source.put("id", dataset.getId().toString());
                    source.put("name", dataset.getDatasetName());
                    source.put("description", dataset.getDatasetDescription() != null ? dataset.getDatasetDescription() : "数据集: " + dataset.getDatasetCategory());
                    source.put("fileName", dataset.getFileName());
                    source.put("category", dataset.getDatasetCategory());
                    source.put("fileType", dataset.getFileType());
                    source.put("fileSize", dataset.getFileSize());
                    source.put("createTime", dataset.getCreateTime());
                    source.put("sourceType", "dataset");
                    source.put("source", "数据集");

                    // 添加数据集特有的信息
                    source.put("hasResults", false); // 数据集本身没有分析结果
                    source.put("columnCount", dataset.getTotalColumns());
                    source.put("rowCount", dataset.getTotalRows());
                    source.put("qualityScore", dataset.getDataQualityScore());
                    source.put("status", dataset.getStatus());

                    allSources.add(source);
                    log.info("✅ 添加数据集: {}", source.get("name"));
                }
            }

            // 按创建时间倒序排列
            allSources.sort((a, b) -> {
                Date timeA = (Date) a.get("createTime");
                Date timeB = (Date) b.get("createTime");
                return timeB.compareTo(timeA);
            });

            log.info("🎯 最终数据源列表大小: {}", allSources.size());

        } catch (Exception e) {
            log.error("❌ 获取数据源列表失败", e);
        }

        return allSources;
    }
    
    /**
     * 根据数据源ID和类型获取数据源信息（支持分析任务和数据集）
     */
    @Override
    public Map<String, Object> getDataSourceInfo(String sourceId, String sourceType) {
        if ("task".equals(sourceType)) {
            Long taskId = Long.parseLong(sourceId.replace("task_", ""));
            AnalysisTask task = analysisTaskService.selectAnalysisTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("分析任务不存在: " + taskId);
            }

            Map<String, Object> info = new HashMap<>();
            info.put("id", task.getId());
            info.put("name", task.getTaskName());
            info.put("description", getTaskDescription(task));
            info.put("algorithm", task.getAlgorithm());
            info.put("status", task.getStatus());
            info.put("filePath", task.getInputFilePath());
            info.put("sourceType", "task");
            info.put("hasResults", task.getResultsJson() != null && !task.getResultsJson().isEmpty());

            // 尝试获取行列数
            if (task.getInputFileHeadersJson() != null) {
                try {
                    List<String> headers = JSON.parseArray(task.getInputFileHeadersJson(), String.class);
                    info.put("columnCount", headers.size());
                    info.put("rowCount", estimateRowCount(task.getInputFilePath()));
                } catch (Exception e) {
                    log.warn("解析任务头信息失败: {}", task.getId());
                }
            }

            return info;
        } else if ("dataset".equals(sourceType)) {
            Long datasetId = Long.parseLong(sourceId);
            PetrolDataset dataset = petrolDatasetService.selectPetrolDatasetById(datasetId);
            if (dataset == null) {
                throw new IllegalArgumentException("数据集不存在: " + datasetId);
            }

            Map<String, Object> info = new HashMap<>();
            info.put("id", dataset.getId());
            info.put("name", dataset.getDatasetName());
            info.put("description", dataset.getDatasetDescription());
            info.put("fileName", dataset.getFileName());
            info.put("fileType", dataset.getFileType());
            info.put("fileSize", dataset.getFileSize());
            info.put("filePath", dataset.getFilePath());
            info.put("sourceType", "dataset");
            info.put("columnCount", dataset.getTotalColumns());
            info.put("rowCount", dataset.getTotalRows());
            info.put("category", dataset.getDatasetCategory());
            info.put("status", dataset.getStatus());
            info.put("hasResults", false); // 数据集本身没有分析结果

            return info;
        }

        throw new IllegalArgumentException("不支持的数据源类型: " + sourceType);
    }
    
    /**
     * 获取数据源的列信息（支持分析任务和数据集）
     */
    @Override
    public Map<String, Object> getDataSourceColumns(String sourceId, String sourceType) {
        if ("task".equals(sourceType)) {
            Long taskId = Long.parseLong(sourceId.replace("task_", ""));
            AnalysisTask task = analysisTaskService.selectAnalysisTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("分析任务不存在: " + taskId);
            }

            Map<String, Object> result = new HashMap<>();
            List<String> allColumns = new ArrayList<>();
            List<String> numericColumns = new ArrayList<>();

            if (task.getInputFileHeadersJson() != null) {
                allColumns = JSON.parseArray(task.getInputFileHeadersJson(), String.class);
            }

            // 如果没有头信息，尝试从实际数据中获取
            if (allColumns.isEmpty()) {
                try {
                    List<Map<String, Object>> sampleData = readDataSourceData(sourceId, sourceType, null, 5);
                    if (!sampleData.isEmpty()) {
                        allColumns = new ArrayList<>(sampleData.get(0).keySet());
                    }
                } catch (Exception e) {
                    log.warn("无法从数据中获取列信息: {}", e.getMessage());
                }
            }

            // 检测数值列
            if (!allColumns.isEmpty()) {
                try {
                    List<Map<String, Object>> sampleData = readDataSourceData(sourceId, sourceType, allColumns, 10);
                    numericColumns = detectNumericColumns(sampleData, allColumns);
                } catch (Exception e) {
                    log.warn("检测数值列失败: {}", e.getMessage());
                    // 如果检测失败，假设常见的石油数据列为数值列
                    for (String column : allColumns) {
                        String upperColumn = column.toUpperCase();
                        if (upperColumn.matches(".*(?:DEPTH|GR|RT|RHOB|NPHI|DT|PE|CALI|SP|RXO|PORO|PERM|SW|TOC).*")) {
                            numericColumns.add(column);
                        }
                    }
                }
            }

            result.put("columns", allColumns);
            result.put("numericColumns", numericColumns);
            result.put("totalColumns", allColumns.size());
            result.put("numericColumnsCount", numericColumns.size());

            log.info("数据源 {} 列信息: 总列数={}, 数值列数={}, 数值列={}",
                sourceId, allColumns.size(), numericColumns.size(), numericColumns);

            return result;
        } else if ("dataset".equals(sourceType)) {
            Long datasetId = Long.parseLong(sourceId);
            PetrolDataset dataset = petrolDatasetService.selectPetrolDatasetById(datasetId);
            if (dataset == null) {
                throw new IllegalArgumentException("数据集不存在: " + datasetId);
            }

            Map<String, Object> result = new HashMap<>();
            List<String> allColumns = new ArrayList<>();
            List<String> numericColumns = new ArrayList<>();

            // 从数据集的列信息中获取列名
            List<Map<String, Object>> columnDetails = petrolDatasetService.getDatasetColumns(datasetId);
            for (Map<String, Object> columnInfo : columnDetails) {
                String columnName = (String) columnInfo.get("name");
                String columnType = (String) columnInfo.get("type");
                allColumns.add(columnName);

                // 根据列类型判断是否为数值列
                if ("numeric".equals(columnType) || "integer".equals(columnType) ||
                    "float".equals(columnType) || "double".equals(columnType)) {
                    numericColumns.add(columnName);
                }
            }

            result.put("columns", allColumns);
            result.put("numericColumns", numericColumns);
            result.put("totalColumns", allColumns.size());
            result.put("numericColumnsCount", numericColumns.size());

            log.info("数据集 {} 列信息: 总列数={}, 数值列数={}, 数值列={}",
                datasetId, allColumns.size(), numericColumns.size(), numericColumns);

            return result;
        }

        throw new IllegalArgumentException("不支持的数据源类型: " + sourceType);
    }

    /**
     * 检测数值列
     */
    private List<String> detectNumericColumns(List<Map<String, Object>> sampleData, List<String> allColumns) {
        List<String> numericColumns = new ArrayList<>();

        if (sampleData.isEmpty()) {
            return numericColumns;
        }

        for (String column : allColumns) {
            int numericCount = 0;
            int totalCount = 0;

            for (Map<String, Object> row : sampleData) {
                Object value = row.get(column);
                if (value != null) {
                    totalCount++;
                    if (value instanceof Number) {
                        numericCount++;
                    } else {
                        try {
                            Double.parseDouble(value.toString());
                            numericCount++;
                        } catch (NumberFormatException e) {
                            // 不是数值
                        }
                    }
                }
            }

            // 如果超过80%的值是数值，则认为是数值列
            if (totalCount > 0 && (double) numericCount / totalCount > 0.8) {
                numericColumns.add(column);
            }
        }

        return numericColumns;
    }
    
    /**
     * 读取数据源数据（只支持分析任务）
     */
    @Override
    public List<Map<String, Object>> readDataSourceData(String sourceId, String sourceType, 
                                                        List<String> columns, Integer maxRows) {
        String filePath = getDataSourceFilePath(sourceId, sourceType);
        if (filePath == null) {
            return new ArrayList<>();
        }
        
        return readExcelData(filePath, columns, maxRows != null ? maxRows : 100);
    }
    
    /**
     * 获取数据源统计信息（支持分析任务和数据集）
     */
    @Override
    public Map<String, Object> getDataSourceStatistics(String sourceId, String sourceType,
                                                       List<String> columns) {
        if ("task".equals(sourceType)) {
            Long taskId = Long.parseLong(sourceId.replace("task_", ""));
            AnalysisTask task = analysisTaskService.selectAnalysisTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("分析任务不存在: " + taskId);
            }

            // 读取数据并计算统计信息
            List<Map<String, Object>> data = readDataSourceData(sourceId, sourceType, columns, 1000);
            Map<String, Object> stats = calculateStatistics(data, columns);

            // 添加任务基本信息
            stats.put("algorithm", task.getAlgorithm());
            stats.put("status", task.getStatus());
            stats.put("dataRows", data.size());

            // 如果有任务结果，也包含进来
            if (task.getResultsJson() != null) {
                try {
                    Map<String, Object> results = JSON.parseObject(task.getResultsJson(), new TypeReference<Map<String, Object>>(){});
                    stats.put("taskResults", results);
                } catch (Exception e) {
                    log.warn("解析任务结果失败: {}", task.getId());
                }
            }

            return stats;
        } else if ("dataset".equals(sourceType)) {
            Long datasetId = Long.parseLong(sourceId);
            PetrolDataset dataset = petrolDatasetService.selectPetrolDatasetById(datasetId);
            if (dataset == null) {
                throw new IllegalArgumentException("数据集不存在: " + datasetId);
            }

            // 读取数据并计算统计信息
            List<Map<String, Object>> data = readDataSourceData(sourceId, sourceType, columns, 1000);
            Map<String, Object> stats = calculateStatistics(data, columns);

            // 添加数据集基本信息
            stats.put("datasetName", dataset.getDatasetName());
            stats.put("category", dataset.getDatasetCategory());
            stats.put("fileType", dataset.getFileType());
            stats.put("totalRows", dataset.getTotalRows());
            stats.put("totalColumns", dataset.getTotalColumns());
            stats.put("dataRows", data.size());
            stats.put("qualityScore", dataset.getDataQualityScore());

            return stats;
        }

        throw new IllegalArgumentException("不支持的数据源类型: " + sourceType);
    }

    /**
     * 计算数据的统计信息
     */
    private Map<String, Object> calculateStatistics(List<Map<String, Object>> data, List<String> columns) {
        Map<String, Object> statistics = new HashMap<>();

        if (data.isEmpty()) {
            return statistics;
        }

        // 获取所有数值列
        Set<String> numericColumns = new HashSet<>();
        if (columns != null && !columns.isEmpty()) {
            numericColumns.addAll(columns);
        } else {
            // 自动检测数值列
            Map<String, Object> firstRow = data.get(0);
            for (String key : firstRow.keySet()) {
                Object value = firstRow.get(key);
                if (value instanceof Number || isNumericString(value)) {
                    numericColumns.add(key);
                }
            }
        }

        // 为每个数值列计算统计信息
        for (String column : numericColumns) {
            List<Double> values = new ArrayList<>();

            // 提取数值
            for (Map<String, Object> row : data) {
                Object value = row.get(column);
                if (value != null) {
                    try {
                        double numValue;
                        if (value instanceof Number) {
                            numValue = ((Number) value).doubleValue();
                        } else {
                            numValue = Double.parseDouble(value.toString());
                        }
                        if (!Double.isNaN(numValue) && Double.isFinite(numValue)) {
                            values.add(numValue);
                        }
                    } catch (NumberFormatException e) {
                        // 忽略非数值
                    }
                }
            }

            if (!values.isEmpty()) {
                Map<String, Object> columnStats = calculateColumnStatistics(values);
                statistics.put(column, columnStats);
            }
        }

        return statistics;
    }

    /**
     * 计算单列的统计信息
     */
    private Map<String, Object> calculateColumnStatistics(List<Double> values) {
        Map<String, Object> stats = new HashMap<>();

        if (values.isEmpty()) {
            return stats;
        }

        // 排序
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        int n = values.size();

        // 基本统计量
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double mean = sum / n;
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / n;
        double std = Math.sqrt(variance);

        // 分位数
        double q25 = percentile(sorted, 0.25);
        double median = percentile(sorted, 0.5);
        double q75 = percentile(sorted, 0.75);

        // 偏度和峰度
        double skewness = calculateSkewness(values, mean, std);
        double kurtosis = calculateKurtosis(values, mean, std);

        stats.put("count", n);
        stats.put("mean", Math.round(mean * 10000.0) / 10000.0);
        stats.put("std", Math.round(std * 10000.0) / 10000.0);
        stats.put("min", sorted.get(0));
        stats.put("25%", Math.round(q25 * 10000.0) / 10000.0);
        stats.put("50%", Math.round(median * 10000.0) / 10000.0);
        stats.put("75%", Math.round(q75 * 10000.0) / 10000.0);
        stats.put("max", sorted.get(n - 1));
        stats.put("skewness", Math.round(skewness * 10000.0) / 10000.0);
        stats.put("kurtosis", Math.round(kurtosis * 10000.0) / 10000.0);

        return stats;
    }

    /**
     * 计算分位数
     */
    private double percentile(List<Double> sortedValues, double p) {
        int n = sortedValues.size();
        double index = p * (n - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        double weight = index - lower;

        if (upper >= n) {
            return sortedValues.get(n - 1);
        }

        return sortedValues.get(lower) * (1 - weight) + sortedValues.get(upper) * weight;
    }

    /**
     * 计算偏度
     */
    private double calculateSkewness(List<Double> values, double mean, double std) {
        if (std == 0 || values.size() < 3) return 0;

        int n = values.size();
        double sum = values.stream().mapToDouble(v -> Math.pow((v - mean) / std, 3)).sum();
        return (n / ((double)(n - 1) * (n - 2))) * sum;
    }

    /**
     * 计算峰度
     */
    private double calculateKurtosis(List<Double> values, double mean, double std) {
        if (std == 0 || values.size() < 4) return 0;

        int n = values.size();
        double sum = values.stream().mapToDouble(v -> Math.pow((v - mean) / std, 4)).sum();
        return ((n * (n + 1)) / ((double)(n - 1) * (n - 2) * (n - 3))) * sum -
               (3 * (n - 1) * (n - 1)) / ((double)(n - 2) * (n - 3));
    }

    /**
     * 判断是否为数值字符串
     */
    private boolean isNumericString(Object value) {
        if (value == null) return false;
        try {
            Double.parseDouble(value.toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // 辅助方法
    private String getDataSourceFilePath(String sourceId, String sourceType) {
        if ("task".equals(sourceType)) {
            Long taskId = Long.parseLong(sourceId.replace("task_", ""));
            AnalysisTask task = analysisTaskService.selectAnalysisTaskById(taskId);
            return task != null ? task.getInputFilePath() : null;
        } else if ("dataset".equals(sourceType)) {
            Long datasetId = Long.parseLong(sourceId);
            PetrolDataset dataset = petrolDatasetService.selectPetrolDatasetById(datasetId);
            return dataset != null ? dataset.getFilePath() : null;
        }
        return null;
    }
    
    private String getTaskDescription(AnalysisTask task) {
        return "分析任务: " + task.getAlgorithm() + " - " + task.getStatus();
    }
    
    private String extractFileName(String filePath) {
        if (filePath == null) return "";
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }
    
    /**
     * 🟡 待优化 - 估算文件行数
     *
     * 当前实现：
     * - 返回固定值1000，这是一个简化的实现
     * - 不是模拟数据，而是功能占位符
     *
     * TODO 优化建议：
     * - 实际读取文件并计算行数
     * - 对于大文件，可以采样估算
     * - 缓存结果以提高性能
     * - 支持不同文件格式的行数计算
     *
     * @param filePath 文件路径
     * @return 估算的行数
     */
    private int estimateRowCount(String filePath) {
        // TODO: 实现真实的文件行数计算逻辑
        // 当前返回固定值作为占位符，实际项目中应该优化
        log.debug("TODO: 需要实现真实的文件行数估算逻辑，当前返回固定值");
        return 1000;
    }

    private List<Map<String, Object>> readExcelData(String filePath, List<String> columns, int maxRows) {
        List<Map<String, Object>> data = new ArrayList<>();

        try {
            // 处理文件路径 - 转换为绝对路径
            String absolutePath = convertToAbsolutePath(filePath);
            log.info("🔍 读取Excel文件 - 原始路径: {}, 绝对路径: {}", filePath, absolutePath);

            // 检查文件是否存在
            File file = new File(absolutePath);
            if (!file.exists()) {
                log.error("❌ 文件不存在: {}", absolutePath);
                throw new RuntimeException("文件不存在: " + absolutePath);
            }

            if (!file.canRead()) {
                log.error("❌ 文件无法读取: {}", absolutePath);
                throw new RuntimeException("文件无法读取: " + absolutePath);
            }

            log.info("✅ 文件存在且可读: {}", absolutePath);

            // 使用安全的Excel读取方法
            return readExcelDataSafely(file, columns, maxRows);

        } catch (Exception e) {
            log.error("读取Excel文件失败: {}", filePath, e);
            throw new RuntimeException("读取Excel文件失败: " + filePath, e);
        }
    }

    /**
     * 安全地读取Excel数据，避免POI的保存问题
     */
    private List<Map<String, Object>> readExcelDataSafely(File file, List<String> columns, int maxRows) {
        List<Map<String, Object>> data = new ArrayList<>();

        try {
            // 使用简单的方式读取Excel，避免复杂的资源管理
            Workbook workbook = new XSSFWorkbook(file);
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                log.error("Excel文件没有标题行: {}", file.getAbsolutePath());
                throw new RuntimeException("Excel文件格式错误，缺少标题行: " + file.getAbsolutePath());
            }

                // 获取列名
                List<String> headers = new ArrayList<>();
                for (Cell cell : headerRow) {
                    String cellValue = "";
                    if (cell != null) {
                        switch (cell.getCellType()) {
                            case STRING:
                                cellValue = cell.getStringCellValue();
                                break;
                            case NUMERIC:
                                cellValue = String.valueOf(cell.getNumericCellValue());
                                break;
                            default:
                                cellValue = cell.toString();
                        }
                    }
                    headers.add(cellValue);
                }

                log.info("Excel文件列名: {}", headers);

                // 读取数据行
                int rowCount = 0;
                for (int i = 1; i <= sheet.getLastRowNum() && rowCount < maxRows; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    Map<String, Object> rowData = new HashMap<>();
                    boolean hasValidData = false;

                    for (int j = 0; j < headers.size(); j++) {
                        Cell cell = row.getCell(j);
                        String header = headers.get(j);

                        // 如果指定了列，只读取这些列
                        if (columns != null && !columns.isEmpty() && !columns.contains(header)) {
                            continue;
                        }

                        Object value = null;
                        if (cell != null) {
                            switch (cell.getCellType()) {
                                case NUMERIC:
                                    value = cell.getNumericCellValue();
                                    hasValidData = true;
                                    break;
                                case STRING:
                                    String stringValue = cell.getStringCellValue();
                                    // 尝试转换为数字
                                    try {
                                        value = Double.parseDouble(stringValue);
                                        hasValidData = true;
                                    } catch (NumberFormatException e) {
                                        value = stringValue;
                                        if (!stringValue.isEmpty()) {
                                            hasValidData = true;
                                        }
                                    }
                                    break;
                                case BOOLEAN:
                                    value = cell.getBooleanCellValue();
                                    hasValidData = true;
                                    break;
                                case FORMULA:
                                    try {
                                        value = cell.getNumericCellValue();
                                        hasValidData = true;
                                    } catch (Exception e) {
                                        value = cell.getStringCellValue();
                                        hasValidData = true;
                                    }
                                    break;
                                default:
                                    value = "";
                            }
                        } else {
                            value = null;
                        }
                        rowData.put(header, value);
                    }

                    if (hasValidData) {
                        data.add(rowData);
                        rowCount++;
                    }
                }

            log.info("成功读取Excel数据: {} 行", data.size());

            // 不调用workbook.close()，让GC自动处理，避免POI的保存问题
            log.debug("跳过workbook.close()以避免POI保存问题");

        } catch (Exception e) {
            log.error("读取Excel文件失败: {}", file.getAbsolutePath(), e);
            throw new RuntimeException("读取Excel文件失败: " + file.getAbsolutePath(), e);
        }

        return data;
    }

    /**
     * 转换为绝对路径
     */
    private String convertToAbsolutePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        log.debug("🔍 转换文件路径 - 原始路径: {}, uploadPath: {}", filePath, uploadPath);

        // 如果已经是绝对路径，直接返回
        if (new File(filePath).isAbsolute()) {
            log.debug("✅ 绝对路径，直接返回: {}", filePath);
            return filePath;
        }

        // 处理以 /profile/ 开头的路径（旧格式）
        if (filePath.startsWith("/profile/")) {
            String relativePath = filePath.substring("/profile".length());
            String fullPath = uploadPath + relativePath;
            log.debug("🔄 转换 /profile/ 路径: {} -> {}", filePath, fullPath);
            return fullPath;
        }

        // 处理以 profile/ 开头的路径（无前导斜杠）
        if (filePath.startsWith("profile/")) {
            String relativePath = filePath.substring("profile".length());
            String fullPath = uploadPath + relativePath;
            log.debug("🔄 转换 profile/ 路径: {} -> {}", filePath, fullPath);
            return fullPath;
        }

        // 处理以 /data/ 开头的路径
        if (filePath.startsWith("/data/")) {
            log.debug("✅ /data/ 路径，直接返回: {}", filePath);
            return filePath;
        }

        // 处理以 data/ 开头的路径
        if (filePath.startsWith("data/")) {
            String fullPath = "./" + filePath;
            log.debug("🔄 转换 data/ 路径: {} -> {}", filePath, fullPath);
            return fullPath;
        }

        // 处理相对路径（不以上述前缀开头）
        String fullPath = uploadPath + "/" + filePath;
        log.debug("🔄 拼接相对路径: {} -> {}", filePath, fullPath);
        return fullPath;
    }


}
