package com.ruoyi.petrol.mapper;

import java.util.List;
import com.ruoyi.petrol.domain.PetrolDataset;

/**
 * 数据集管理Mapper接口
 * 
 * @author ruoyi
 * @date 2025-06-29
 */
public interface PetrolDatasetMapper 
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
     * 删除数据集管理
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
     * 批量删除数据集管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deletePetrolDatasetByIds(Long[] ids);

    /**
     * 检查数据集是否被使用
     * 
     * @param datasetId 数据集ID
     * @return 使用次数
     */
    public int checkDatasetUsage(Long datasetId);

    /**
     * 更新数据集统计信息
     *
     * @param petrolDataset 数据集管理
     * @return 结果
     */
    public int updateDatasetStats(PetrolDataset petrolDataset);

    /**
     * 查询带统计信息的数据集列表（优化查询）
     *
     * @param petrolDataset 数据集管理
     * @return 数据集管理集合
     */
    public List<PetrolDataset> selectPetrolDatasetListWithStats(PetrolDataset petrolDataset);

    /**
     * 批量查询数据集（避免N+1查询）
     *
     * @param ids 数据集ID数组
     * @return 数据集管理集合
     */
    public List<PetrolDataset> selectPetrolDatasetByIds(Long[] ids);
}
