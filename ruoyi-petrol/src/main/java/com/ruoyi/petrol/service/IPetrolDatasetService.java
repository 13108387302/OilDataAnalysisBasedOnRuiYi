package com.ruoyi.petrol.service;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.petrol.domain.PetrolDataset;

/**
 * 数据集管理Service接口
 * 
 * @author ruoyi
 * @date 2025-06-29
 */
public interface IPetrolDatasetService 
{
    /**
     * 查询数据集管理
     * 
     * @param id 数据集管理主键
     * @return 数据集管理
     */
    public PetrolDataset selectPetrolDatasetById(Long id);

    /**
     * 根据数据集名称查询数据集
     * 
     * @param datasetName 数据集名称
     * @return 数据集管理
     */
    public PetrolDataset selectPetrolDatasetByName(String datasetName);

    /**
     * 查询数据集管理列表
     * 
     * @param petrolDataset 数据集管理
     * @return 数据集管理集合
     */
    public List<PetrolDataset> selectPetrolDatasetList(PetrolDataset petrolDataset);

    /**
     * 查询可用的数据集列表
     * 
     * @return 数据集管理集合
     */
    public List<PetrolDataset> selectAvailableDatasets();

    /**
     * 根据类别查询数据集列表
     * 
     * @param category 数据集类别
     * @return 数据集管理集合
     */
    public List<PetrolDataset> selectDatasetsByCategory(String category);

    /**
     * 新增数据集管理
     * 
     * @param petrolDataset 数据集管理
     * @return 结果
     */
    public int insertPetrolDataset(PetrolDataset petrolDataset);

    /**
     * 修改数据集管理
     * 
     * @param petrolDataset 数据集管理
     * @return 结果
     */
    public int updatePetrolDataset(PetrolDataset petrolDataset);

    /**
     * 批量删除数据集管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deletePetrolDatasetByIds(Long[] ids);

    /**
     * 删除数据集管理信息
     *
     * @param id 数据集管理主键
     * @return 结果
     */
    public int deletePetrolDatasetById(Long id);

    /**
     * 删除所有数据集（用于完整清理）
     *
     * @return 删除的记录数
     */
    public int deleteAllDatasets();

    /**
     * 上传数据集文件并创建数据集
     * 
     * @param file 上传的文件
     * @param datasetName 数据集名称
     * @param description 数据集描述
     * @param category 数据集类别
     * @return 结果
     */
    public Map<String, Object> uploadDataset(MultipartFile file, String datasetName, String description, String category);

    /**
     * 分析数据集文件并生成统计信息
     * 
     * @param filePath 文件路径
     * @return 数据集统计信息
     */
    public Map<String, Object> analyzeDatasetFile(String filePath);

    /**
     * 获取数据集预览数据
     * 
     * @param datasetId 数据集ID
     * @param rows 预览行数
     * @return 预览数据
     */
    public Map<String, Object> getDatasetPreview(Long datasetId, Integer rows);

    /**
     * 检查数据集是否被使用
     *
     * @param datasetId 数据集ID
     * @return 使用情况
     */
    public Map<String, Object> checkDatasetUsage(Long datasetId);

    /**
     * 分片上传文件
     *
     * @param chunk 文件分片
     * @param chunkIndex 分片索引
     * @param chunkHash 分片哈希
     * @param fileHash 文件哈希
     * @param fileName 文件名
     * @param totalChunks 总分片数
     * @return 结果
     */
    public Map<String, Object> uploadChunk(MultipartFile chunk, Integer chunkIndex, String chunkHash,
                                         String fileHash, String fileName, Integer totalChunks);

    /**
     * 检查已上传的分片
     *
     * @param fileHash 文件哈希
     * @param fileName 文件名
     * @param totalChunks 总分片数
     * @return 已上传的分片信息
     */
    public Map<String, Object> checkUploadedChunks(String fileHash, String fileName, Integer totalChunks);

    /**
     * 合并分片并创建数据集
     *
     * @param fileHash 文件哈希
     * @param fileName 文件名
     * @param totalChunks 总分片数
     * @param fileSize 文件大小
     * @param datasetName 数据集名称
     * @param description 数据集描述
     * @param category 数据集类别
     * @return 结果
     */
    public Map<String, Object> mergeChunksAndCreateDataset(String fileHash, String fileName, Integer totalChunks,
                                                          Long fileSize, String datasetName, String description, String category);

    /**
     * 更新数据集统计信息
     * 
     * @param datasetId 数据集ID
     * @return 结果
     */
    public int updateDatasetStats(Long datasetId);

    /**
     * 获取数据集的列信息
     * 
     * @param datasetId 数据集ID
     * @return 列信息列表
     */
    public List<Map<String, Object>> getDatasetColumns(Long datasetId);

    /**
     * 验证数据集文件格式
     * 
     * @param file 文件
     * @return 验证结果
     */
    public Map<String, Object> validateDatasetFile(MultipartFile file);
}
