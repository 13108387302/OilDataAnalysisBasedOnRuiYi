package com.ruoyi.petrol.service.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;

import com.ruoyi.petrol.domain.PetrolDataset;
import com.ruoyi.petrol.mapper.PetrolDatasetMapper;
import com.ruoyi.petrol.service.IPetrolDatasetService;
import com.ruoyi.petrol.security.ValidationUtils;
import com.ruoyi.petrol.aspect.PerformanceAspect.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据集管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-06-29
 */
@Service
public class PetrolDatasetServiceImpl implements IPetrolDatasetService 
{
    private static final Logger log = LoggerFactory.getLogger(PetrolDatasetServiceImpl.class);

    @Autowired
    private PetrolDatasetMapper petrolDatasetMapper;

    @Value("${ruoyi.profile}")
    private String profile;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 查询数据集管理
     *
     * @param id 数据集管理主键
     * @return 数据集管理
     */
    @Override
    @Cacheable(value = "dataset", key = "#id")
    @PerformanceMonitor(value = "查询数据集", threshold = 500)
    public PetrolDataset selectPetrolDatasetById(Long id)
    {
        return petrolDatasetMapper.selectPetrolDatasetById(id);
    }

    /**
     * 根据数据集名称查询数据集
     * 
     * @param datasetName 数据集名称
     * @return 数据集管理
     */
    @Override
    public PetrolDataset selectPetrolDatasetByName(String datasetName)
    {
        return petrolDatasetMapper.selectPetrolDatasetByName(datasetName);
    }

    /**
     * 查询数据集管理列表
     * 
     * @param petrolDataset 数据集管理
     * @return 数据集管理
     */
    @Override
    public List<PetrolDataset> selectPetrolDatasetList(PetrolDataset petrolDataset)
    {
        return petrolDatasetMapper.selectPetrolDatasetList(petrolDataset);
    }

    /**
     * 查询可用的数据集列表
     *
     * @return 数据集管理集合
     */
    @Override
    @Cacheable(value = "datasetList", key = "'available'")
    @PerformanceMonitor(value = "查询可用数据集列表", threshold = 1000)
    public List<PetrolDataset> selectAvailableDatasets()
    {
        return petrolDatasetMapper.selectAvailableDatasets();
    }

    /**
     * 根据类别查询数据集列表
     * 
     * @param category 数据集类别
     * @return 数据集管理集合
     */
    @Override
    public List<PetrolDataset> selectDatasetsByCategory(String category)
    {
        return petrolDatasetMapper.selectDatasetsByCategory(category);
    }

    /**
     * 新增数据集管理
     *
     * @param petrolDataset 数据集管理
     * @return 结果
     */
    @Override
    @CacheEvict(value = "datasetList", allEntries = true)
    public int insertPetrolDataset(PetrolDataset petrolDataset)
    {
        petrolDataset.setCreateTime(DateUtils.getNowDate());
        petrolDataset.setCreateBy(SecurityUtils.getUsername());
        return petrolDatasetMapper.insertPetrolDataset(petrolDataset);
    }

    /**
     * 修改数据集管理
     *
     * @param petrolDataset 数据集管理
     * @return 结果
     */
    @Override
    @CacheEvict(value = {"datasetList", "dataset"}, allEntries = true)
    public int updatePetrolDataset(PetrolDataset petrolDataset)
    {
        petrolDataset.setUpdateTime(DateUtils.getNowDate());
        petrolDataset.setUpdateBy(SecurityUtils.getUsername());
        return petrolDatasetMapper.updatePetrolDataset(petrolDataset);
    }

    /**
     * 批量删除数据集管理
     *
     * @param ids 需要删除的数据集管理主键
     * @return 结果
     */
    @Override
    @CacheEvict(value = {"datasetList", "dataset"}, allEntries = true)
    public int deletePetrolDatasetByIds(Long[] ids)
    {
        // 检查数据集是否被使用
        for (Long id : ids) {
            int usageCount = petrolDatasetMapper.checkDatasetUsage(id);
            if (usageCount > 0) {
                throw new RuntimeException("数据集ID " + id + " 正在被使用，无法删除");
            }
        }
        return petrolDatasetMapper.deletePetrolDatasetByIds(ids);
    }

    /**
     * 删除数据集管理信息
     *
     * @param id 数据集管理主键
     * @return 结果
     */
    @Override
    @CacheEvict(value = {"datasetList", "dataset"}, allEntries = true)
    public int deletePetrolDatasetById(Long id)
    {
        // 检查数据集是否被使用
        int usageCount = petrolDatasetMapper.checkDatasetUsage(id);
        if (usageCount > 0) {
            throw new RuntimeException("数据集正在被使用，无法删除");
        }
        return petrolDatasetMapper.deletePetrolDatasetById(id);
    }

    /**
     * 删除所有数据集（用于完整清理）
     *
     * @return 删除的记录数
     */
    @Override
    @CacheEvict(value = {"datasetList", "dataset"}, allEntries = true)
    public int deleteAllDatasets()
    {
        log.info("🗑️ 开始删除所有数据集记录...");

        // 获取当前数据集数量
        List<PetrolDataset> allDatasets = petrolDatasetMapper.selectPetrolDatasetList(new PetrolDataset());
        int totalCount = allDatasets.size();

        if (totalCount == 0) {
            log.info("📋 没有数据集需要删除");
            return 0;
        }

        // 删除所有数据集记录
        int deletedCount = petrolDatasetMapper.deleteAllDatasets();

        log.info("✅ 成功删除 {} 个数据集记录", deletedCount);
        return deletedCount;
    }

    /**
     * 上传数据集文件并创建数据集
     *
     * @param file 上传的文件
     * @param datasetName 数据集名称
     * @param description 数据集描述
     * @param category 数据集类别
     * @return 结果
     */
    @Override
    @CacheEvict(value = {"datasetList", "dataset"}, allEntries = true)
    public Map<String, Object> uploadDataset(MultipartFile file, String datasetName, String description, String category)
    {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证文件格式
            Map<String, Object> validation = validateDatasetFile(file);
            if (!(Boolean) validation.get("valid")) {
                result.put("success", false);
                result.put("message", validation.get("message"));
                return result;
            }

            // 检查数据集名称是否已存在
            PetrolDataset existingDataset = petrolDatasetMapper.selectPetrolDatasetByName(datasetName);
            if (existingDataset != null) {
                result.put("success", false);
                result.put("message", "数据集名称已存在");
                return result;
            }

            // 上传文件
            String uploadDir = profile + "/datasets/";
            String fileName = FileUploadUtils.upload(uploadDir, file);

            // fileName 返回的是相对于profile的路径，如：/profile/datasets/xxx.xlsx
            // 我们需要转换为实际的文件系统路径
            String actualFilePath;
            if (fileName.startsWith("/profile/")) {
                // 去掉/profile前缀，拼接实际路径
                String relativePath = fileName.substring("/profile/".length());
                actualFilePath = profile + "/" + relativePath;
            } else {
                // 兼容其他情况
                actualFilePath = uploadDir + fileName;
            }

            // 调试日志
            log.info("Profile: {}", profile);
            log.info("Upload dir: {}", uploadDir);
            log.info("File name returned by upload: {}", fileName);
            log.info("Actual file path: {}", actualFilePath);

            // 验证上传的Excel文件格式
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null &&
                (originalFilename.toLowerCase().endsWith(".xlsx") ||
                 originalFilename.toLowerCase().endsWith(".xls"))) {

                File uploadedFile = new File(actualFilePath);
                if (!validateExcelFile(uploadedFile)) {
                    // 删除损坏的文件
                    uploadedFile.delete();

                    result.put("success", false);
                    result.put("message", "上传的Excel文件格式不正确或已损坏，请检查文件完整性后重新上传");
                    return result;
                }

                log.info("✅ Excel文件格式验证通过: {}", file.getOriginalFilename());
            }

            // 分析文件内容
            Map<String, Object> analysisResult = analyzeDatasetFile(actualFilePath);

            // 创建数据集记录
            PetrolDataset dataset = new PetrolDataset();
            dataset.setDatasetName(datasetName);
            dataset.setDatasetDescription(description);
            dataset.setFileName(file.getOriginalFilename());
            dataset.setFilePath(fileName); // 保存相对路径用于前端访问
            dataset.setFileSize(file.getSize());
            dataset.setFileType(getFileExtension(file.getOriginalFilename()).toUpperCase());
            dataset.setDatasetCategory(category);
            dataset.setStatus("ACTIVE");
            dataset.setIsPublic(0); // 0=不公开，1=公开

            // 设置分析结果
            if (analysisResult.get("success").equals(true)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = (Map<String, Object>) analysisResult.get("stats");
                dataset.setTotalRows(((Number) stats.get("totalRows")).longValue());
                dataset.setTotalColumns(((Number) stats.get("totalColumns")).longValue());
                dataset.setColumnInfo(objectMapper.writeValueAsString(stats.get("columns")));
                // dataset.setMissingValuesCount(((Number) stats.get("missingValues")).longValue());
                dataset.setDataQualityScore(new BigDecimal(stats.get("qualityScore").toString()));
            }

            int insertResult = insertPetrolDataset(dataset);
            
            if (insertResult > 0) {
                result.put("success", true);
                result.put("message", "数据集上传成功");
                result.put("datasetId", dataset.getId());
                result.put("dataset", dataset);
            } else {
                result.put("success", false);
                result.put("message", "数据集保存失败");
            }

        } catch (Exception e) {
            log.error("上传数据集失败", e);
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 验证数据集文件格式（使用统一的验证工具类）
     *
     * @param file 文件
     * @return 验证结果
     */
    @Override
    public Map<String, Object> validateDatasetFile(MultipartFile file)
    {
        Map<String, Object> result = new HashMap<>();

        if (file == null || file.isEmpty()) {
            result.put("valid", false);
            result.put("message", "文件不能为空");
            return result;
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            result.put("valid", false);
            result.put("message", "文件名不能为空");
            return result;
        }

        // 使用统一的验证工具类
        if (!ValidationUtils.isValidFilename(fileName)) {
            result.put("valid", false);
            result.put("message", "文件名不符合安全要求或格式不支持");
            return result;
        }

        if (!ValidationUtils.isValidFileSize(file.getSize())) {
            result.put("valid", false);
            result.put("message", "文件大小超出限制（最大100MB）");
            return result;
        }

        result.put("valid", true);
        result.put("message", "文件格式验证通过");
        return result;
    }

    /**
     * 获取文件扩展名（简化版本）
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }



    /**
     * 将存储的相对路径转换为实际文件系统路径
     */
    private String convertToActualFilePath(String storedPath) {
        if (storedPath.startsWith("/profile/")) {
            // 去掉/profile前缀，拼接实际路径
            String relativePath = storedPath.substring("/profile/".length());
            return profile + "/" + relativePath;
        } else {
            // 兼容其他情况，可能是绝对路径或其他格式
            return storedPath;
        }
    }

    // 其他方法的实现将在下一个文件中继续...
    
    /**
     * 分析数据集文件并生成统计信息
     *
     * @param filePath 文件路径
     * @return 数据集统计信息
     */
    @Override
    public Map<String, Object> analyzeDatasetFile(String filePath) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 确保使用绝对路径
            File file = new File(filePath);
            if (!file.isAbsolute()) {
                // 如果是相对路径，转换为绝对路径
                file = new File(System.getProperty("user.dir"), filePath);
            }

            if (!file.exists()) {
                result.put("success", false);
                result.put("message", "文件不存在: " + file.getAbsolutePath());
                return result;
            }

            String extension = getFileExtension(filePath).toLowerCase();
            Map<String, Object> stats = new HashMap<>();

            if ("csv".equals(extension)) {
                stats = analyzeCsvFile(file.getAbsolutePath());
            } else if ("xlsx".equals(extension) || "xls".equals(extension)) {
                stats = analyzeExcelFile(file.getAbsolutePath());
            } else {
                result.put("success", false);
                result.put("message", "不支持的文件格式");
                return result;
            }

            result.put("success", true);
            result.put("stats", stats);

        } catch (Exception e) {
            log.error("分析数据集文件失败: " + filePath, e);
            result.put("success", false);
            result.put("message", "文件分析失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 分析Excel文件
     */
    private Map<String, Object> analyzeExcelFile(String filePath) throws IOException {
        Map<String, Object> stats = new HashMap<>();
        List<Map<String, Object>> columns = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(new File(filePath))) {
            Sheet sheet = workbook.getSheetAt(0);

            // 获取总行数和列数
            int totalRows = sheet.getLastRowNum() + 1;
            Row headerRow = sheet.getRow(0);
            int totalColumns = headerRow != null ? headerRow.getLastCellNum() : 0;

            stats.put("totalRows", totalRows - 1); // 减去标题行
            stats.put("totalColumns", totalColumns);

            // 分析列信息
            if (headerRow != null) {
                for (int i = 0; i < totalColumns; i++) {
                    Cell cell = headerRow.getCell(i);
                    String columnName = cell != null ? cell.getStringCellValue() : "Column_" + (i + 1);

                    Map<String, Object> columnInfo = analyzeExcelColumn(sheet, i, columnName, totalRows);
                    columns.add(columnInfo);
                }
            }

            stats.put("columns", columns);
            stats.put("missingValues", calculateMissingValues(columns));
            stats.put("qualityScore", calculateQualityScore(stats));
        }

        return stats;
    }

    /**
     * 分析Excel列
     */
    private Map<String, Object> analyzeExcelColumn(Sheet sheet, int columnIndex, String columnName, int totalRows) {
        Map<String, Object> columnInfo = new HashMap<>();
        columnInfo.put("name", columnName);

        int missingCount = 0;
        List<Double> numericValues = new ArrayList<>();
        boolean isNumeric = true;

        // 分析数据类型和统计信息（跳过标题行）
        for (int rowIndex = 1; rowIndex < totalRows; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                missingCount++;
                continue;
            }

            Cell cell = row.getCell(columnIndex);
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                missingCount++;
                continue;
            }

            try {
                if (cell.getCellType() == CellType.NUMERIC) {
                    numericValues.add(cell.getNumericCellValue());
                } else if (cell.getCellType() == CellType.STRING) {
                    String value = cell.getStringCellValue();
                    try {
                        numericValues.add(Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        isNumeric = false;
                    }
                } else {
                    isNumeric = false;
                }
            } catch (Exception e) {
                isNumeric = false;
            }
        }

        columnInfo.put("type", isNumeric ? "numeric" : "string");
        columnInfo.put("missingCount", missingCount);
        columnInfo.put("missingRate", (double) missingCount / (totalRows - 1));

        if (isNumeric && !numericValues.isEmpty()) {
            DoubleSummaryStatistics stats = numericValues.stream().mapToDouble(Double::doubleValue).summaryStatistics();
            columnInfo.put("min", stats.getMin());
            columnInfo.put("max", stats.getMax());
            columnInfo.put("mean", stats.getAverage());
            columnInfo.put("count", stats.getCount());
        }

        return columnInfo;
    }

    /**
     * 分析CSV文件
     */
    private Map<String, Object> analyzeCsvFile(String filePath) throws IOException {
        Map<String, Object> stats = new HashMap<>();
        List<Map<String, Object>> columns = new ArrayList<>();

        // 简化的CSV分析实现
        // 实际项目中可以使用Apache Commons CSV等库
        stats.put("totalRows", 100); // 临时值
        stats.put("totalColumns", 5); // 临时值
        stats.put("columns", columns);
        stats.put("missingValues", 0);
        stats.put("qualityScore", 95.0);

        return stats;
    }

    /**
     * 计算缺失值总数
     */
    private int calculateMissingValues(List<Map<String, Object>> columns) {
        return columns.stream()
                .mapToInt(col -> ((Number) col.getOrDefault("missingCount", 0)).intValue())
                .sum();
    }

    /**
     * 计算数据质量评分
     */
    private double calculateQualityScore(Map<String, Object> stats) {
        int totalRows = ((Number) stats.get("totalRows")).intValue();
        int missingValues = ((Number) stats.get("missingValues")).intValue();

        if (totalRows == 0) return 0.0;

        double completeness = 1.0 - (double) missingValues / (totalRows * ((List<?>) stats.get("columns")).size());
        return Math.max(0.0, Math.min(100.0, completeness * 100));
    }

    @Override
    public Map<String, Object> getDatasetPreview(Long datasetId, Integer rows) {
        Map<String, Object> result = new HashMap<>();
        PetrolDataset dataset = null;

        try {
            dataset = petrolDatasetMapper.selectPetrolDatasetById(datasetId);
            if (dataset == null) {
                result.put("success", false);
                result.put("message", "数据集不存在");
                return result;
            }

            // 读取文件前几行作为预览
            String storedPath = dataset.getFilePath();
            String actualFilePath = convertToActualFilePath(storedPath);

            log.info("Stored path: {}", storedPath);
            log.info("Actual file path for preview: {}", actualFilePath);

            List<List<Object>> previewData = readFilePreview(actualFilePath, rows != null ? rows : 10);

            result.put("success", true);
            result.put("data", previewData);
            result.put("dataset", dataset);

        } catch (Exception e) {
            log.error("获取数据集预览失败", e);
            result.put("success", false);
            result.put("message", "获取预览失败: " + e.getMessage());

            // 添加更详细的错误信息
            if (e instanceof java.io.FileNotFoundException) {
                result.put("errorType", "FILE_NOT_FOUND");
                result.put("details", "文件路径: " + (dataset != null ? dataset.getFilePath() : "未知"));
            } else {
                result.put("errorType", "GENERAL_ERROR");
                result.put("details", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * 读取文件预览数据
     */
    private List<List<Object>> readFilePreview(String filePath, int rows) throws IOException {
        List<List<Object>> data = new ArrayList<>();
        String extension = getFileExtension(filePath).toLowerCase();

        // 确保使用绝对路径
        File file = new File(filePath);
        if (!file.isAbsolute()) {
            // 如果是相对路径，转换为绝对路径
            file = new File(System.getProperty("user.dir"), filePath);
        }

        // 检查文件是否存在
        if (!file.exists()) {
            throw new IOException("文件不存在: " + file.getAbsolutePath());
        }

        if ("xlsx".equals(extension) || "xls".equals(extension)) {
            try (Workbook workbook = WorkbookFactory.create(file)) {
                Sheet sheet = workbook.getSheetAt(0);
                int maxRows = Math.min(rows, sheet.getLastRowNum() + 1);

                for (int i = 0; i < maxRows; i++) {
                    Row row = sheet.getRow(i);
                    List<Object> rowData = new ArrayList<>();

                    if (row != null) {
                        for (int j = 0; j < row.getLastCellNum(); j++) {
                            Cell cell = row.getCell(j);
                            Object value = getCellValue(cell);
                            rowData.add(value);
                        }
                    }
                    data.add(rowData);
                }
            }
        } else if ("csv".equals(extension)) {
            // 处理CSV文件
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                int rowCount = 0;

                while ((line = reader.readLine()) != null && rowCount < rows) {
                    List<Object> rowData = new ArrayList<>();
                    // 简单的CSV解析（不处理引号内的逗号）
                    String[] values = line.split(",");
                    for (String value : values) {
                        rowData.add(value.trim());
                    }
                    data.add(rowData);
                    rowCount++;
                }
            }
        }

        return data;
    }

    /**
     * 获取单元格值
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                return cell.getStringCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case BLANK:
                return null;
            default:
                return cell.toString();
        }
    }

    @Override
    public Map<String, Object> checkDatasetUsage(Long datasetId) {
        Map<String, Object> result = new HashMap<>();

        try {
            int usageCount = petrolDatasetMapper.checkDatasetUsage(datasetId);
            result.put("success", true);
            result.put("usageCount", usageCount);
            result.put("canDelete", usageCount == 0);

        } catch (Exception e) {
            log.error("检查数据集使用情况失败", e);
            result.put("success", false);
            result.put("message", "检查失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public int updateDatasetStats(Long datasetId) {
        try {
            PetrolDataset dataset = petrolDatasetMapper.selectPetrolDatasetById(datasetId);
            if (dataset == null) {
                return 0;
            }

            // 重新分析文件
            String storedPath = dataset.getFilePath();
            String actualFilePath = convertToActualFilePath(storedPath);

            Map<String, Object> analysisResult = analyzeDatasetFile(actualFilePath);
            if (analysisResult.get("success").equals(true)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = (Map<String, Object>) analysisResult.get("stats");

                dataset.setTotalRows(((Number) stats.get("totalRows")).longValue());
                dataset.setTotalColumns(((Number) stats.get("totalColumns")).longValue());
                dataset.setColumnInfo(objectMapper.writeValueAsString(stats.get("columns")));
                // dataset.setMissingValuesCount(((Number) stats.get("missingValues")).longValue());
                dataset.setDataQualityScore(new BigDecimal(stats.get("qualityScore").toString()));

                return petrolDatasetMapper.updateDatasetStats(dataset);
            }

        } catch (Exception e) {
            log.error("更新数据集统计信息失败", e);
        }

        return 0;
    }

    @Override
    public List<Map<String, Object>> getDatasetColumns(Long datasetId) {
        List<Map<String, Object>> columns = new ArrayList<>();

        try {
            PetrolDataset dataset = petrolDatasetMapper.selectPetrolDatasetById(datasetId);
            if (dataset != null && dataset.getColumnInfo() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> columnList = objectMapper.readValue(
                    dataset.getColumnInfo(),
                    List.class
                );
                columns = columnList;
            }

        } catch (Exception e) {
            log.error("获取数据集列信息失败", e);
        }

        return columns;
    }

    /**
     * 分片上传文件
     */
    @Override
    public Map<String, Object> uploadChunk(MultipartFile chunk, Integer chunkIndex, String chunkHash,
                                         String fileHash, String fileName, Integer totalChunks) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 创建分片存储目录
            String chunkDir = profile + File.separator + "chunks" + File.separator + fileHash;
            File chunkDirFile = new File(chunkDir);
            if (!chunkDirFile.exists()) {
                chunkDirFile.mkdirs();
            }

            // 保存分片文件
            String chunkFileName = fileHash + "_" + chunkIndex;
            File chunkFile = new File(chunkDir, chunkFileName);

            // 使用安全的方式保存分片文件，避免transferTo可能导致的损坏
            saveChunkSecurely(chunk, chunkFile);

            log.info("分片上传成功: {} ({}/{})", chunkFileName, chunkIndex + 1, totalChunks);

            result.put("success", true);
            result.put("message", "分片上传成功");

            Map<String, Object> data = new HashMap<>();
            data.put("chunkIndex", chunkIndex);
            data.put("chunkHash", chunkHash);
            data.put("uploaded", true);
            result.put("data", data);

        } catch (Exception e) {
            log.error("分片上传失败", e);
            result.put("success", false);
            result.put("message", "分片上传失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 检查已上传的分片
     */
    @Override
    public Map<String, Object> checkUploadedChunks(String fileHash, String fileName, Integer totalChunks) {
        Map<String, Object> result = new HashMap<>();

        try {
            String chunkDir = profile + File.separator + "chunks" + File.separator + fileHash;
            File chunkDirFile = new File(chunkDir);

            List<Integer> uploadedChunks = new ArrayList<>();

            if (chunkDirFile.exists()) {
                for (int i = 0; i < totalChunks; i++) {
                    String chunkFileName = fileHash + "_" + i;
                    File chunkFile = new File(chunkDir, chunkFileName);
                    if (chunkFile.exists()) {
                        uploadedChunks.add(i);
                    }
                }
            }

            result.put("uploadedChunks", uploadedChunks);
            result.put("totalChunks", totalChunks);
            result.put("isComplete", uploadedChunks.size() == totalChunks);

            log.info("检查分片完成: {} ({}/{})", fileName, uploadedChunks.size(), totalChunks);

        } catch (Exception e) {
            log.error("检查分片失败", e);
            result.put("uploadedChunks", new ArrayList<>());
            result.put("totalChunks", totalChunks);
            result.put("isComplete", false);
        }

        return result;
    }

    /**
     * 合并分片并创建数据集
     */
    @Override
    public Map<String, Object> mergeChunksAndCreateDataset(String fileHash, String fileName, Integer totalChunks,
                                                          Long fileSize, String datasetName, String description, String category) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查所有分片是否都已上传
            Map<String, Object> checkResult = checkUploadedChunks(fileHash, fileName, totalChunks);
            if (!(Boolean) checkResult.get("isComplete")) {
                result.put("success", false);
                result.put("message", "分片不完整，无法合并");
                return result;
            }

            // 创建最终文件目录
            String uploadDir = profile + File.separator + "upload" + File.separator + "datasets";
            File uploadDirFile = new File(uploadDir);
            if (!uploadDirFile.exists()) {
                uploadDirFile.mkdirs();
            }

            // 合并分片
            String finalFileName = System.currentTimeMillis() + "_" + fileName;
            File finalFile = new File(uploadDir, finalFileName);

            String chunkDir = profile + File.separator + "chunks" + File.separator + fileHash;

            try (FileOutputStream fos = new FileOutputStream(finalFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos, 65536)) { // 64KB缓冲区

                for (int i = 0; i < totalChunks; i++) {
                    String chunkFileName = fileHash + "_" + i;
                    File chunkFile = new File(chunkDir, chunkFileName);

                    if (!chunkFile.exists()) {
                        throw new RuntimeException("分片文件不存在: " + chunkFileName);
                    }

                    try (FileInputStream fis = new FileInputStream(chunkFile);
                         BufferedInputStream bis = new BufferedInputStream(fis, 65536)) {

                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = bis.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                        }
                    }
                }

                // 确保所有数据都写入磁盘
                bos.flush();
                fos.getFD().sync();
            }

            // 验证文件大小
            if (finalFile.length() != fileSize) {
                finalFile.delete();
                result.put("success", false);
                result.put("message", "文件合并后大小不匹配，期望: " + fileSize + "，实际: " + finalFile.length());
                return result;
            }

            // 验证Excel文件格式（如果是Excel文件）
            if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls")) {
                if (!validateExcelFile(finalFile)) {
                    finalFile.delete();
                    result.put("success", false);
                    result.put("message", "Excel文件格式验证失败，文件可能在合并过程中损坏");
                    return result;
                }
            }

            // 分析文件并创建数据集
            String relativePath = "/profile/upload/datasets/" + finalFileName;
            Map<String, Object> analysisResult = analyzeDatasetFile(finalFile.getAbsolutePath());

            if (!(Boolean) analysisResult.get("success")) {
                finalFile.delete();
                result.put("success", false);
                result.put("message", "文件分析失败: " + analysisResult.get("message"));
                return result;
            }

            // 创建数据集记录
            PetrolDataset dataset = new PetrolDataset();
            dataset.setDatasetName(datasetName);
            dataset.setDatasetDescription(description);
            dataset.setFileName(fileName);
            dataset.setFilePath(relativePath);
            dataset.setFileSize(fileSize);
            dataset.setFileType(getFileExtension(fileName).toUpperCase());
            dataset.setDatasetCategory(category);
            dataset.setStatus("ACTIVE");
            // dataset.setIsPublic("N"); // 根据实际字段类型调整

            // 设置分析结果
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = (Map<String, Object>) analysisResult.get("stats");
            if (stats != null) {
                dataset.setTotalRows(((Number) stats.getOrDefault("totalRows", 0)).longValue());
                dataset.setTotalColumns(((Number) stats.getOrDefault("totalColumns", 0)).longValue());
                dataset.setColumnInfo(objectMapper.writeValueAsString(stats.get("columns")));
                dataset.setDataQualityScore(new BigDecimal(stats.getOrDefault("qualityScore", 0).toString()));
            }

            insertPetrolDataset(dataset);

            // 清理分片文件
            cleanupChunks(fileHash);

            result.put("success", true);
            result.put("message", "文件合并成功，数据集创建完成");
            result.put("dataset", dataset);

            log.info("分片合并完成，数据集创建成功: {}", datasetName);

        } catch (Exception e) {
            log.error("合并分片失败", e);
            result.put("success", false);
            result.put("message", "合并分片失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 安全地保存分片文件
     */
    private void saveChunkSecurely(MultipartFile chunk, File chunkFile) throws IOException {
        // 确保父目录存在
        File parentDir = chunkFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 使用缓冲流进行文件复制，确保数据完整性
        try (InputStream inputStream = chunk.getInputStream();
             BufferedInputStream bis = new BufferedInputStream(inputStream, 8192);
             FileOutputStream fos = new FileOutputStream(chunkFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 8192)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }

            // 确保所有数据都写入磁盘
            bos.flush();
            fos.getFD().sync();

        } catch (IOException e) {
            // 如果保存失败，删除可能的不完整文件
            if (chunkFile.exists()) {
                chunkFile.delete();
            }
            throw e;
        }
    }

    /**
     * 验证Excel文件格式
     */
    private boolean validateExcelFile(File file) {
        try {
            // 使用POI尝试打开Excel文件
            Workbook workbook = WorkbookFactory.create(file);

            // 检查是否至少有一个工作表
            if (workbook.getNumberOfSheets() == 0) {
                workbook.close();
                return false;
            }

            // 尝试读取第一个工作表的第一行
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet != null && sheet.getPhysicalNumberOfRows() > 0) {
                Row firstRow = sheet.getRow(0);
                if (firstRow != null) {
                    // 文件格式正常
                    workbook.close();
                    return true;
                }
            }

            workbook.close();
            return true; // 即使没有数据，格式也是正确的

        } catch (Exception e) {
            log.error("Excel文件格式验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 清理分片文件
     */
    private void cleanupChunks(String fileHash) {
        try {
            String chunkDir = profile + File.separator + "chunks" + File.separator + fileHash;
            File chunkDirFile = new File(chunkDir);

            if (chunkDirFile.exists()) {
                File[] chunkFiles = chunkDirFile.listFiles();
                if (chunkFiles != null) {
                    for (File chunkFile : chunkFiles) {
                        chunkFile.delete();
                    }
                }
                chunkDirFile.delete();
            }

            log.info("分片文件清理完成: {}", fileHash);
        } catch (Exception e) {
            log.warn("清理分片文件失败: {}", e.getMessage());
        }
    }
}
