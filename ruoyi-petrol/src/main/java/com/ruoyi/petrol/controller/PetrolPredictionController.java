package com.ruoyi.petrol.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.petrol.domain.PetrolPrediction;
import com.ruoyi.petrol.service.IPetrolPredictionService;

/**
 * 石油预测Controller
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@RestController
@RequestMapping("/petrol/prediction")
public class PetrolPredictionController extends BaseController
{
    @Autowired
    private IPetrolPredictionService petrolPredictionService;

    /**
     * 查询石油预测列表
     */
    @PreAuthorize("@ss.hasPermi('petrol:prediction:list')")
    @GetMapping("/list")
    public TableDataInfo list(PetrolPrediction petrolPrediction)
    {
        startPage();
        List<PetrolPrediction> list = petrolPredictionService.selectPetrolPredictionList(petrolPrediction);
        return getDataTable(list);
    }

    /**
     * 导出石油预测列表
     */
    @PreAuthorize("@ss.hasPermi('petrol:prediction:export')")
    @Log(title = "石油预测", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PetrolPrediction petrolPrediction)
    {
        List<PetrolPrediction> list = petrolPredictionService.selectPetrolPredictionList(petrolPrediction);
        ExcelUtil<PetrolPrediction> util = new ExcelUtil<PetrolPrediction>(PetrolPrediction.class);
        util.exportExcel(response, list, "石油预测数据");
    }

    /**
     * 获取石油预测详细信息
     */
    @PreAuthorize("@ss.hasPermi('petrol:prediction:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(petrolPredictionService.selectPetrolPredictionById(id));
    }

    /**
     * 新增石油预测
     */
    @PreAuthorize("@ss.hasPermi('petrol:prediction:add')")
    @Log(title = "石油预测", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PetrolPrediction petrolPrediction)
    {
        return toAjax(petrolPredictionService.insertPetrolPrediction(petrolPrediction));
    }

    /**
     * 修改石油预测
     */
    @PreAuthorize("@ss.hasPermi('petrol:prediction:edit')")
    @Log(title = "石油预测", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PetrolPrediction petrolPrediction)
    {
        return toAjax(petrolPredictionService.updatePetrolPrediction(petrolPrediction));
    }

    /**
     * 删除石油预测
     */
    @PreAuthorize("@ss.hasPermi('petrol:prediction:remove')")
    @Log(title = "石油预测", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(petrolPredictionService.deletePetrolPredictionByIds(ids));
    }

    /**
     * 根据模型ID查询预测记录
     */
    @GetMapping("/model/{modelId}")
    public AjaxResult getPredictionsByModelId(@PathVariable("modelId") Long modelId)
    {
        List<PetrolPrediction> predictions = petrolPredictionService.selectPredictionsByModelId(modelId);
        return success(predictions);
    }

    /**
     * 提交预测任务（简化版，支持数据集模式）
     */
    @PreAuthorize("@ss.hasPermi('petrol:prediction:add')")
    @Log(title = "提交预测任务", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public AjaxResult submitPrediction(@RequestBody Map<String, Object> params)
    {
        try {
            // 参数验证
            String predictionName = (String) params.get("predictionName");
            Long modelId = params.get("modelId") != null ? Long.valueOf(params.get("modelId").toString()) : null;
            Long datasetId = params.get("datasetId") != null ? Long.valueOf(params.get("datasetId").toString()) : null;

            if (predictionName == null || predictionName.trim().isEmpty()) {
                return error("预测任务名称不能为空");
            }

            if (modelId == null) {
                return error("请选择预测模型");
            }

            if (datasetId == null) {
                return error("请选择数据集");
            }

            // 构建预测参数JSON
            Map<String, Object> predictionParamsMap = new HashMap<>();
            predictionParamsMap.put("datasetId", datasetId);
            predictionParamsMap.put("selectedFeatures", params.get("selectedFeatures"));
            predictionParamsMap.put("targetColumn", params.get("targetColumn"));
            predictionParamsMap.put("sampleCount", params.get("sampleCount"));
            predictionParamsMap.put("rowSelectionStrategy", params.get("rowSelectionStrategy"));
            predictionParamsMap.put("customRows", params.get("customRows"));

            String predictionParamsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(predictionParamsMap);

            // 创建预测任务
            PetrolPrediction prediction = new PetrolPrediction();
            prediction.setPredictionName(predictionName);
            prediction.setModelId(modelId);
            prediction.setPredictionParams(predictionParamsJson);
            prediction.setStatus("pending");
            prediction.setPredictionCount(params.get("sampleCount") != null ? Integer.valueOf(params.get("sampleCount").toString()) : 100);

            int result = petrolPredictionService.insertPetrolPrediction(prediction);

            if (result > 0) {
                // 异步执行预测任务
                petrolPredictionService.processPredictionTask(prediction);
                return success(prediction);
            } else {
                return error("预测任务提交失败");
            }

        } catch (Exception e) {
            logger.error("提交预测任务失败", e);
            return error("提交预测任务失败: " + e.getMessage());
        }
    }

    /**
     * 执行预测（支持文件上传）
     */
    @PreAuthorize("@ss.hasPermi('petrol:prediction:execute')")
    @Log(title = "执行预测", businessType = BusinessType.OTHER)
    @PostMapping("/execute")
    public AjaxResult executePrediction(@RequestParam("predictionName") String predictionName,
                                      @RequestParam(value = "modelId", required = false) Long modelId,
                                      @RequestParam(value = "dataFile", required = false) MultipartFile dataFile,
                                      @RequestParam(value = "modelFile", required = false) MultipartFile modelFile,
                                      @RequestParam(value = "modelName", required = false) String modelName,
                                      @RequestParam(value = "algorithm", required = false) String algorithm,
                                      @RequestParam(value = "predictionParams", required = false) String predictionParams,
                                      @RequestParam(value = "description", required = false) String description)
    {
        try {
            // 参数验证
            if (predictionName == null || predictionName.trim().isEmpty()) {
                return error("预测任务名称不能为空");
            }

            if (dataFile == null || dataFile.isEmpty()) {
                return error("请上传数据文件");
            }

            if (modelId == null && (modelFile == null || modelFile.isEmpty()) &&
                (algorithm == null || algorithm.trim().isEmpty())) {
                return error("请选择模型、上传模型文件或指定算法");
            }

            Map<String, Object> result = petrolPredictionService.executePredictionWithFiles(
                predictionName, modelId, dataFile, modelFile, modelName, algorithm, predictionParams, description);

            if ((Boolean) result.get("success")) {
                AjaxResult ajaxResult = success("预测执行成功");
                ajaxResult.put("data", result.get("data"));
                ajaxResult.put("predictionId", result.get("predictionId"));
                return ajaxResult;
            } else {
                String errorMessage = result.get("message") != null ?
                    result.get("message").toString() : "预测执行失败";
                return error(errorMessage);
            }
        } catch (RuntimeException e) {
            logger.error("预测执行失败: {}", e.getMessage(), e);
            return error("预测执行失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("预测执行异常", e);
            return error("系统异常，请稍后重试");
        }
    }

    /**
     * 执行预测（JSON方式）
     */
    @PreAuthorize("@ss.hasPermi('petrol:prediction:execute')")
    @Log(title = "执行预测", businessType = BusinessType.OTHER)
    @PostMapping("/execute/json")
    public AjaxResult executePredictionJson(@RequestBody Map<String, Object> request)
    {
        try {
            // 参数验证
            if (request == null || request.isEmpty()) {
                return error("请求参数不能为空");
            }

            if (!request.containsKey("predictionName") || request.get("predictionName") == null) {
                return error("预测任务名称不能为空");
            }

            if (!request.containsKey("inputData") || request.get("inputData") == null) {
                return error("输入数据不能为空");
            }

            Long modelId = null;
            if (request.containsKey("modelId") && request.get("modelId") != null) {
                try {
                    modelId = Long.valueOf(request.get("modelId").toString());
                } catch (NumberFormatException e) {
                    return error("模型ID格式不正确");
                }
            }

            String predictionName = request.get("predictionName").toString().trim();
            if (predictionName.isEmpty()) {
                return error("预测任务名称不能为空");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> inputData = (Map<String, Object>) request.get("inputData");

            Map<String, Object> result = petrolPredictionService.executePrediction(modelId, inputData, predictionName);

            if ((Boolean) result.get("success")) {
                AjaxResult ajaxResult = success("预测执行成功");
                ajaxResult.put("data", result.get("data"));
                ajaxResult.put("predictionId", result.get("predictionId"));
                return ajaxResult;
            } else {
                String errorMessage = result.get("message") != null ?
                    result.get("message").toString() : "预测执行失败";
                return error(errorMessage);
            }
        } catch (ClassCastException e) {
            logger.error("请求参数格式错误: {}", e.getMessage(), e);
            return error("请求参数格式错误");
        } catch (RuntimeException e) {
            logger.error("预测执行失败: {}", e.getMessage(), e);
            return error("预测执行失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("预测执行异常", e);
            return error("系统异常，请稍后重试");
        }
    }

    /**
     * 批量预测（文件上传）
     */
    @PreAuthorize("@ss.hasPermi('petrol:prediction:batch')")
    @Log(title = "批量预测", businessType = BusinessType.OTHER)
    @PostMapping("/batch")
    public AjaxResult executeBatchPrediction(@RequestParam("file") MultipartFile file,
                                           @RequestParam("modelId") Long modelId,
                                           @RequestParam("predictionName") String predictionName)
    {
        try {
            if (file.isEmpty()) {
                return error("请选择要上传的数据文件");
            }

            Map<String, Object> result = petrolPredictionService.executeBatchPrediction(modelId, file, predictionName);
            
            if ((Boolean) result.get("success")) {
                return success(result);
            } else {
                return error(result.get("message").toString());
            }
        } catch (Exception e) {
            logger.error("批量预测执行失败", e);
            return error("批量预测执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取预测状态
     */
    @GetMapping("/status/{predictionId}")
    public AjaxResult getPredictionStatus(@PathVariable("predictionId") Long predictionId)
    {
        Map<String, Object> result = petrolPredictionService.getPredictionStatus(predictionId);
        
        if ((Boolean) result.get("success")) {
            return success(result);
        } else {
            return error(result.get("message").toString());
        }
    }

    /**
     * 获取预测结果
     */
    @GetMapping("/result/{predictionId}")
    public AjaxResult getPredictionResult(@PathVariable("predictionId") Long predictionId)
    {
        Map<String, Object> result = petrolPredictionService.getPredictionResult(predictionId);

        if ((Boolean) result.get("success")) {
            return success(result);
        } else {
            return error(result.get("message").toString());
        }
    }

    /**
     * 获取预测统计信息
     */
    @GetMapping("/stats")
    public AjaxResult getStats()
    {
        try {
            Map<String, Object> stats = petrolPredictionService.getStats();
            return success(stats);
        } catch (Exception e) {
            logger.error("获取统计信息失败", e);
            return error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 创建增强的预测任务
     */
    @PostMapping("/enhanced")
    public AjaxResult createEnhancedPrediction(@RequestBody Map<String, Object> predictionData)
    {
        try {
            // 参数验证
            if (predictionData == null || predictionData.isEmpty()) {
                return error("预测参数不能为空");
            }

            String predictionName = (String) predictionData.get("predictionName");
            if (predictionName == null || predictionName.trim().isEmpty()) {
                return error("预测任务名称不能为空");
            }

            logger.info("开始创建增强预测任务: {}", predictionName);
            Map<String, Object> result = petrolPredictionService.createEnhancedPrediction(predictionData);

            if ((Boolean) result.get("success")) {
                // 预测任务创建成功
                logger.info("预测任务创建成功: {}, ID: {}", predictionName, result.get("predictionId"));
                AjaxResult ajaxResult = success("预测任务创建成功");
                ajaxResult.put("data", result.get("data"));
                ajaxResult.put("predictionId", result.get("predictionId"));
                return ajaxResult;
            } else {
                // 预测任务创建失败，返回详细错误信息
                String errorMessage = result.get("message") != null ?
                    result.get("message").toString() : "预测任务创建失败";
                String errorType = result.get("errorType") != null ?
                    result.get("errorType").toString() : null;
                Boolean retryable = result.get("retryable") != null ?
                    (Boolean) result.get("retryable") : false;

                logger.error("预测任务创建失败: {}, 错误: {}", predictionName, errorMessage);

                AjaxResult errorResult = error(errorMessage);
                if (errorType != null) {
                    errorResult.put("errorType", errorType);
                }
                errorResult.put("retryable", retryable);

                return errorResult;
            }
        } catch (RuntimeException e) {
            logger.error("预测任务创建失败: {}", e.getMessage(), e);
            return error("预测任务创建失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("预测任务创建异常", e);
            return error("系统异常，请稍后重试");
        }
    }
}
