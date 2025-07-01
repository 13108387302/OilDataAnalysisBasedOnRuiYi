package com.ruoyi.petrol.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.petrol.domain.PetrolPrediction;
import org.springframework.web.multipart.MultipartFile;

/**
 * 石油预测Service接口
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface IPetrolPredictionService 
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
     * 批量删除石油预测
     * 
     * @param ids 需要删除的石油预测主键集合
     * @return 结果
     */
    public int deletePetrolPredictionByIds(Long[] ids);

    /**
     * 删除石油预测信息
     * 
     * @param id 石油预测主键
     * @return 结果
     */
    public int deletePetrolPredictionById(Long id);

    /**
     * 根据模型ID查询预测记录
     * 
     * @param modelId 模型ID
     * @return 预测记录列表
     */
    public List<PetrolPrediction> selectPredictionsByModelId(Long modelId);

    /**
     * 执行预测
     * 
     * @param modelId 模型ID
     * @param inputData 输入数据
     * @param predictionName 预测名称
     * @return 预测结果
     */
    public Map<String, Object> executePrediction(Long modelId, Map<String, Object> inputData, String predictionName);

    /**
     * 批量预测（文件上传）
     * 
     * @param modelId 模型ID
     * @param file 输入文件
     * @param predictionName 预测名称
     * @return 预测结果
     */
    public Map<String, Object> executeBatchPrediction(Long modelId, MultipartFile file, String predictionName);

    /**
     * 获取预测状态
     * 
     * @param predictionId 预测ID
     * @return 预测状态
     */
    public Map<String, Object> getPredictionStatus(Long predictionId);

    /**
     * 获取预测结果
     *
     * @param predictionId 预测ID
     * @return 预测结果
     */
    public Map<String, Object> getPredictionResult(Long predictionId);

    /**
     * 执行预测（支持文件上传）
     *
     * @param predictionName 预测名称
     * @param modelId 模型ID（可选）
     * @param dataFile 数据文件（可选）
     * @param modelFile 模型文件（可选）
     * @param modelName 模型名称（可选）
     * @param algorithm 算法类型（可选）
     * @param predictionParams 预测参数（可选）
     * @param description 描述（可选）
     * @return 预测结果
     */
    public Map<String, Object> executePredictionWithFiles(String predictionName, Long modelId,
                                                         MultipartFile dataFile, MultipartFile modelFile,
                                                         String modelName, String algorithm,
                                                         String predictionParams, String description);

    /**
     * 获取统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getStats();

    /**
     * 创建增强的预测任务（支持特征选择和目标选择）
     *
     * @param predictionData 预测数据，包含特征列表、目标列、模型选择等信息
     * @return 创建结果
     */
    public Map<String, Object> createEnhancedPrediction(Map<String, Object> predictionData);

    /**
     * 异步处理预测任务
     *
     * @param prediction 预测任务
     */
    public void processPredictionTask(PetrolPrediction prediction);
}
