package com.ruoyi.petrol.service;

import java.util.List;
import com.ruoyi.petrol.domain.PetrolModel;
import org.springframework.web.multipart.MultipartFile;

/**
 * 石油模型Service接口
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface IPetrolModelService 
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
     * 批量删除石油模型
     * 
     * @param ids 需要删除的石油模型主键集合
     * @return 结果
     */
    public int deletePetrolModelByIds(Long[] ids);

    /**
     * 删除石油模型信息
     * 
     * @param id 石油模型主键
     * @return 结果
     */
    public int deletePetrolModelById(Long id);

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

    /**
     * 上传模型文件
     * 
     * @param file 模型文件
     * @param petrolModel 模型信息
     * @return 结果
     */
    public int uploadModel(MultipartFile file, PetrolModel petrolModel);

    /**
     * 验证模型文件
     * 
     * @param modelPath 模型文件路径
     * @return 验证结果
     */
    public boolean validateModel(String modelPath);
}
