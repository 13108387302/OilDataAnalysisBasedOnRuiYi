package com.ruoyi.petrol.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.petrol.mapper.PetrolModelMapper;
import com.ruoyi.petrol.domain.PetrolModel;
import com.ruoyi.petrol.service.IPetrolModelService;

/**
 * 石油模型Service业务层处理
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@Service
public class PetrolModelServiceImpl implements IPetrolModelService 
{
    @Autowired
    private PetrolModelMapper petrolModelMapper;

    @Value("${ruoyi.profile}")
    private String uploadPath;

    /**
     * 查询石油模型
     * 
     * @param id 石油模型主键
     * @return 石油模型
     */
    @Override
    public PetrolModel selectPetrolModelById(Long id)
    {
        return petrolModelMapper.selectPetrolModelById(id);
    }

    /**
     * 查询石油模型列表
     * 
     * @param petrolModel 石油模型
     * @return 石油模型
     */
    @Override
    public List<PetrolModel> selectPetrolModelList(PetrolModel petrolModel)
    {
        return petrolModelMapper.selectPetrolModelList(petrolModel);
    }

    /**
     * 新增石油模型
     * 
     * @param petrolModel 石油模型
     * @return 结果
     */
    @Override
    public int insertPetrolModel(PetrolModel petrolModel)
    {
        petrolModel.setCreateTime(DateUtils.getNowDate());

        // 在异步环境中安全获取用户名
        try {
            String username = SecurityUtils.getUsername();
            petrolModel.setCreatedBy(username);
        } catch (Exception e) {
            // 如果无法获取用户信息（如在异步线程中），使用默认值
            if (petrolModel.getCreatedBy() == null || petrolModel.getCreatedBy().isEmpty()) {
                petrolModel.setCreatedBy("system");
            }
        }

        return petrolModelMapper.insertPetrolModel(petrolModel);
    }

    /**
     * 修改石油模型
     * 
     * @param petrolModel 石油模型
     * @return 结果
     */
    @Override
    public int updatePetrolModel(PetrolModel petrolModel)
    {
        petrolModel.setUpdateTime(DateUtils.getNowDate());
        return petrolModelMapper.updatePetrolModel(petrolModel);
    }

    /**
     * 批量删除石油模型
     * 
     * @param ids 需要删除的石油模型主键
     * @return 结果
     */
    @Override
    public int deletePetrolModelByIds(Long[] ids)
    {
        // 删除模型文件
        for (Long id : ids) {
            PetrolModel model = petrolModelMapper.selectPetrolModelById(id);
            if (model != null && model.getModelPath() != null) {
                File modelFile = new File(model.getModelPath());
                if (modelFile.exists()) {
                    modelFile.delete();
                }
            }
        }
        return petrolModelMapper.deletePetrolModelByIds(ids);
    }

    /**
     * 删除石油模型信息
     * 
     * @param id 石油模型主键
     * @return 结果
     */
    @Override
    public int deletePetrolModelById(Long id)
    {
        // 删除模型文件
        PetrolModel model = petrolModelMapper.selectPetrolModelById(id);
        if (model != null && model.getModelPath() != null) {
            File modelFile = new File(model.getModelPath());
            if (modelFile.exists()) {
                modelFile.delete();
            }
        }
        return petrolModelMapper.deletePetrolModelById(id);
    }

    /**
     * 根据任务ID查询模型
     * 
     * @param taskId 任务ID
     * @return 模型列表
     */
    @Override
    public List<PetrolModel> selectModelsByTaskId(Long taskId)
    {
        return petrolModelMapper.selectModelsByTaskId(taskId);
    }

    /**
     * 查询可用的模型列表
     * 
     * @param modelType 模型类型
     * @return 模型列表
     */
    @Override
    public List<PetrolModel> selectAvailableModels(String modelType)
    {
        return petrolModelMapper.selectAvailableModels(modelType);
    }

    /**
     * 上传模型文件
     * 
     * @param file 模型文件
     * @param petrolModel 模型信息
     * @return 结果
     */
    @Override
    public int uploadModel(MultipartFile file, PetrolModel petrolModel)
    {
        try {
            // 创建模型存储目录
            String modelDir = uploadPath + "/models";
            File dir = new File(modelDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 保存文件
            String fileName = FileUploadUtils.upload(modelDir, file);
            String modelPath = modelDir + File.separator + fileName;

            // 设置模型信息
            petrolModel.setModelPath(modelPath);
            petrolModel.setFileSize(file.getSize());
            petrolModel.setStatus("ACTIVE");
            petrolModel.setIsAvailable("Y");

            // 验证模型文件
            if (!validateModel(modelPath)) {
                // 删除无效文件
                new File(modelPath).delete();
                throw new RuntimeException("模型文件验证失败");
            }

            return insertPetrolModel(petrolModel);
        } catch (IOException e) {
            throw new RuntimeException("模型文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 验证模型文件
     * 
     * @param modelPath 模型文件路径
     * @return 验证结果
     */
    @Override
    public boolean validateModel(String modelPath)
    {
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            return false;
        }

        // 检查文件扩展名
        String fileName = modelFile.getName().toLowerCase();
        if (!fileName.endsWith(".pkl") && !fileName.endsWith(".joblib") && !fileName.endsWith(".model")) {
            return false;
        }

        // 检查文件大小
        if (modelFile.length() == 0) {
            return false;
        }

        // TODO: 可以添加更多验证逻辑，如尝试加载模型等

        return true;
    }
}
