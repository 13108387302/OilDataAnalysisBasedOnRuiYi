package com.ruoyi.petrol.mapper;

import java.util.List;
import com.ruoyi.petrol.domain.PetrolModel;

/**
 * 石油模型Mapper接口
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface PetrolModelMapper 
{
    /**
     * 查询石油模型
     * 
     * @param id 石油模型主键
     * @return 石油模型
     */
    public PetrolModel selectPetrolModelById(Long id);

    /**
     * 查询石油模型列表
     * 
     * @param petrolModel 石油模型
     * @return 石油模型集合
     */
    public List<PetrolModel> selectPetrolModelList(PetrolModel petrolModel);

    /**
     * 新增石油模型
     * 
     * @param petrolModel 石油模型
     * @return 结果
     */
    public int insertPetrolModel(PetrolModel petrolModel);

    /**
     * 修改石油模型
     * 
     * @param petrolModel 石油模型
     * @return 结果
     */
    public int updatePetrolModel(PetrolModel petrolModel);

    /**
     * 删除石油模型
     * 
     * @param id 石油模型主键
     * @return 结果
     */
    public int deletePetrolModelById(Long id);

    /**
     * 批量删除石油模型
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deletePetrolModelByIds(Long[] ids);

    /**
     * 根据任务ID查询模型
     * 
     * @param taskId 任务ID
     * @return 模型列表
     */
    public List<PetrolModel> selectModelsByTaskId(Long taskId);

    /**
     * 查询可用的模型列表
     * 
     * @param modelType 模型类型
     * @return 模型列表
     */
    public List<PetrolModel> selectAvailableModels(String modelType);
}
