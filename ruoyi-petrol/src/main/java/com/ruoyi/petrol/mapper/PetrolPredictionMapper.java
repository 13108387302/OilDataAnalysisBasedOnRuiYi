package com.ruoyi.petrol.mapper;

import java.util.List;
import com.ruoyi.petrol.domain.PetrolPrediction;

/**
 * 石油预测Mapper接口
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface PetrolPredictionMapper 
{
    /**
     * 查询石油预测
     * 
     * @param id 石油预测主键
     * @return 石油预测
     */
    public PetrolPrediction selectPetrolPredictionById(Long id);

    /**
     * 查询石油预测列表
     * 
     * @param petrolPrediction 石油预测
     * @return 石油预测集合
     */
    public List<PetrolPrediction> selectPetrolPredictionList(PetrolPrediction petrolPrediction);

    /**
     * 新增石油预测
     * 
     * @param petrolPrediction 石油预测
     * @return 结果
     */
    public int insertPetrolPrediction(PetrolPrediction petrolPrediction);

    /**
     * 修改石油预测
     * 
     * @param petrolPrediction 石油预测
     * @return 结果
     */
    public int updatePetrolPrediction(PetrolPrediction petrolPrediction);

    /**
     * 删除石油预测
     * 
     * @param id 石油预测主键
     * @return 结果
     */
    public int deletePetrolPredictionById(Long id);

    /**
     * 批量删除石油预测
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deletePetrolPredictionByIds(Long[] ids);

    /**
     * 根据模型ID查询预测记录
     * 
     * @param modelId 模型ID
     * @return 预测记录列表
     */
    public List<PetrolPrediction> selectPredictionsByModelId(Long modelId);
}
