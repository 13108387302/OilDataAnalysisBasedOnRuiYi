package com.ruoyi.petrol.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.List;
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
import com.ruoyi.petrol.domain.PetrolModel;
import com.ruoyi.petrol.service.IPetrolModelService;

/**
 * 石油模型Controller
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@RestController
@RequestMapping("/petrol/model")
public class PetrolModelController extends BaseController
{
    @Autowired
    private IPetrolModelService petrolModelService;

    /**
     * 查询石油模型列表
     */
    @PreAuthorize("@ss.hasPermi('petrol:model:list')")
    @GetMapping("/list")
    public TableDataInfo list(PetrolModel petrolModel)
    {
        startPage();
        List<PetrolModel> list = petrolModelService.selectPetrolModelList(petrolModel);
        return getDataTable(list);
    }

    /**
     * 导出石油模型列表
     */
    @PreAuthorize("@ss.hasPermi('petrol:model:export')")
    @Log(title = "石油模型", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PetrolModel petrolModel)
    {
        List<PetrolModel> list = petrolModelService.selectPetrolModelList(petrolModel);
        ExcelUtil<PetrolModel> util = new ExcelUtil<PetrolModel>(PetrolModel.class);
        util.exportExcel(response, list, "石油模型数据");
    }

    /**
     * 获取石油模型详细信息
     */
    @PreAuthorize("@ss.hasPermi('petrol:model:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(petrolModelService.selectPetrolModelById(id));
    }

    /**
     * 新增石油模型
     */
    @PreAuthorize("@ss.hasPermi('petrol:model:add')")
    @Log(title = "石油模型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PetrolModel petrolModel)
    {
        return toAjax(petrolModelService.insertPetrolModel(petrolModel));
    }

    /**
     * 修改石油模型
     */
    @PreAuthorize("@ss.hasPermi('petrol:model:edit')")
    @Log(title = "石油模型", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PetrolModel petrolModel)
    {
        return toAjax(petrolModelService.updatePetrolModel(petrolModel));
    }

    /**
     * 删除单个石油模型
     */
    @PreAuthorize("@ss.hasPermi('petrol:model:remove')")
    @Log(title = "石油模型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        return toAjax(petrolModelService.deletePetrolModelById(id));
    }

    /**
     * 批量删除石油模型
     */
    @PreAuthorize("@ss.hasPermi('petrol:model:remove')")
    @Log(title = "石油模型", businessType = BusinessType.DELETE)
	@DeleteMapping("/batch/{ids}")
    public AjaxResult removeBatch(@PathVariable Long[] ids)
    {
        return toAjax(petrolModelService.deletePetrolModelByIds(ids));
    }

    /**
     * 根据任务ID查询模型
     */
    @GetMapping("/task/{taskId}")
    public AjaxResult getModelsByTaskId(@PathVariable("taskId") Long taskId)
    {
        List<PetrolModel> models = petrolModelService.selectModelsByTaskId(taskId);
        return success(models);
    }

    /**
     * 查询可用的模型列表
     */
    @GetMapping("/available")
    public AjaxResult getAvailableModels(@RequestParam(value = "modelType", required = false) String modelType)
    {
        List<PetrolModel> models = petrolModelService.selectAvailableModels(modelType);
        return success(models);
    }

    /**
     * 上传模型文件
     */
    @PreAuthorize("@ss.hasPermi('petrol:model:upload')")
    @Log(title = "上传模型", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult uploadModel(@RequestParam("file") MultipartFile file,
                                  @RequestParam("modelName") String modelName,
                                  @RequestParam("modelType") String modelType,
                                  @RequestParam("algorithm") String algorithm,
                                  @RequestParam(value = "description", required = false) String description,
                                  @RequestParam(value = "inputFeatures", required = false) String inputFeatures,
                                  @RequestParam(value = "outputTarget", required = false) String outputTarget)
    {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return error("请选择要上传的模型文件");
            }

            // 检查文件类型
            String fileName = file.getOriginalFilename();
            if (!fileName.toLowerCase().endsWith(".pkl") &&
                !fileName.toLowerCase().endsWith(".joblib") &&
                !fileName.toLowerCase().endsWith(".model") &&
                !fileName.toLowerCase().endsWith(".pth")) {
                return error("只支持 .pkl、.joblib、.model、.pth 格式的模型文件");
            }

            // 创建模型对象
            PetrolModel petrolModel = new PetrolModel();
            petrolModel.setModelName(modelName);
            petrolModel.setModelType(modelType);
            petrolModel.setAlgorithm(algorithm);
            petrolModel.setDescription(description);
            petrolModel.setInputFeatures(inputFeatures);
            petrolModel.setOutputTarget(outputTarget);

            // 上传模型
            int result = petrolModelService.uploadModel(file, petrolModel);
            
            if (result > 0) {
                return success(petrolModel);
            } else {
                return error("模型上传失败");
            }
        } catch (Exception e) {
            logger.error("模型上传失败", e);
            return error("模型上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载模型文件
     */
    @PreAuthorize("@ss.hasPermi('petrol:model:query')")
    @GetMapping("/download/{id}")
    public void downloadModel(@PathVariable Long id, HttpServletResponse response)
    {
        try {
            logger.info("开始下载模型文件，模型ID: {}", id);

            // 获取模型信息
            PetrolModel model = petrolModelService.selectPetrolModelById(id);
            if (model == null) {
                logger.warn("模型不存在，ID: {}", id);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 获取模型文件路径
            String modelPath = model.getModelPath();
            logger.info("模型路径: {}", modelPath);

            if (modelPath == null || modelPath.isEmpty()) {
                logger.warn("模型路径为空，模型ID: {}", id);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 转换为绝对路径
            String absolutePath = getAbsoluteModelPath(modelPath);
            logger.info("转换后的绝对路径: {}", absolutePath);

            File modelFile = new File(absolutePath);

            if (!modelFile.exists()) {
                logger.warn("模型文件不存在: {}", absolutePath);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (!modelFile.isFile()) {
                logger.warn("路径不是文件: {}", modelFile.getAbsolutePath());
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 设置响应头
            String fileName = modelFile.getName();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            response.setContentLengthLong(modelFile.length());

            logger.info("开始传输文件: {}, 大小: {} bytes", fileName, modelFile.length());

            // 输出文件
            try (FileInputStream fis = new FileInputStream(modelFile);
                 OutputStream os = response.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                os.flush();

                logger.info("成功下载模型文件: {} (ID: {}), 传输字节数: {}", fileName, id, totalBytes);
            }

        } catch (Exception e) {
            logger.error("下载模型文件失败，模型ID: " + id, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取模型文件的绝对路径
     */
    private String getAbsoluteModelPath(String modelPath) {
        if (modelPath == null || modelPath.isEmpty()) {
            return null;
        }

        // 统一路径分隔符
        modelPath = modelPath.replace("\\", "/");

        // 如果是相对路径，转换为绝对路径
        if (modelPath.startsWith("/profile/")) {
            // 移除 /profile/ 前缀，使用data目录
            String relativePath = modelPath.substring("/profile/".length());
            String basePath = System.getProperty("user.dir").replace("\\", "/");
            return basePath + "/data/" + relativePath;
        }

        // 如果已经是绝对路径，直接返回
        return modelPath;
    }



    /**
     * 验证模型文件
     */
    @PostMapping("/validate")
    public AjaxResult validateModel(@RequestParam("modelPath") String modelPath)
    {
        boolean isValid = petrolModelService.validateModel(modelPath);
        if (isValid) {
            return success("模型文件验证通过");
        } else {
            return error("模型文件验证失败");
        }
    }

    /**
     * 训练新模型
     */
    @PreAuthorize("@ss.hasPermi('petrol:model:add')")
    @Log(title = "训练模型", businessType = BusinessType.INSERT)
    @PostMapping("/train")
    public AjaxResult trainModel(@RequestBody java.util.Map<String, Object> params)
    {
        try {
            // 参数验证
            String modelName = (String) params.get("modelName");
            Long datasetId = params.get("datasetId") != null ? Long.valueOf(params.get("datasetId").toString()) : null;
            String algorithm = (String) params.get("algorithm");
            String description = (String) params.get("description");

            if (modelName == null || modelName.trim().isEmpty()) {
                return error("模型名称不能为空");
            }

            if (datasetId == null) {
                return error("请选择训练数据集");
            }

            if (algorithm == null || algorithm.trim().isEmpty()) {
                return error("请选择算法类型");
            }

            // 构建训练参数
            java.util.Map<String, Object> trainParams = new java.util.HashMap<>();
            trainParams.put("datasetId", datasetId);
            trainParams.put("selectedFeatures", params.get("selectedFeatures"));
            trainParams.put("targetColumn", params.get("targetColumn"));
            trainParams.put("algorithm", algorithm);

            String trainParamsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(trainParams);

            // 创建模型记录
            PetrolModel model = new PetrolModel();
            model.setModelName(modelName);
            model.setAlgorithm(algorithm);
            model.setModelType("regression"); // 默认回归类型
            model.setDescription(description);
            model.setModelParams(trainParamsJson); // 使用modelParams字段存储训练参数
            model.setInputFeatures(params.get("selectedFeatures") != null ? params.get("selectedFeatures").toString() : null);
            model.setOutputTarget((String) params.get("targetColumn"));
            model.setStatus("training");

            int result = petrolModelService.insertPetrolModel(model);

            if (result > 0) {
                return success(model);
            } else {
                return error("模型训练任务创建失败");
            }

        } catch (Exception e) {
            logger.error("创建模型训练任务失败", e);
            return error("创建模型训练任务失败: " + e.getMessage());
        }
    }
}
