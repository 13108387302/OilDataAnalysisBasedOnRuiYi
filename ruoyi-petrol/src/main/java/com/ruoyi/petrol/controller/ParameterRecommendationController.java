package com.ruoyi.petrol.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.petrol.service.IParameterRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 参数推荐控制器
 * 
 * @author ruoyi
 * @date 2025-06-24
 */
@RestController
@RequestMapping("/petrol/parameter")
public class ParameterRecommendationController extends BaseController {
    
    @Autowired
    private IParameterRecommendationService parameterRecommendationService;
    
    /**
     * 获取参数推荐
     */
    @PreAuthorize("@ss.hasPermi('petrol:analysis:add')")
    @PostMapping("/recommend")
    public AjaxResult recommendParameters(@RequestBody Map<String, Object> request) {
        try {
            String algorithm = (String) request.get("algorithm");
            Map<String, Object> datasetInfo = (Map<String, Object>) request.get("datasetInfo");
            
            if (algorithm == null || algorithm.isEmpty()) {
                return AjaxResult.error("算法名称不能为空");
            }
            
            if (datasetInfo == null) {
                datasetInfo = new HashMap<>();
                datasetInfo.put("sampleCount", 1000);
                datasetInfo.put("featureCount", 10);
                datasetInfo.put("targetType", "numeric");
            }
            
            Map<String, Object> recommendations = parameterRecommendationService.recommendParameters(algorithm, datasetInfo);
            
            return AjaxResult.success("参数推荐成功", recommendations);
            
        } catch (Exception e) {
            logger.error("参数推荐失败", e);
            return AjaxResult.error("参数推荐失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取参数模板
     */
    @PreAuthorize("@ss.hasPermi('petrol:analysis:add')")
    @GetMapping("/template")
    public AjaxResult getParameterTemplate(@RequestParam String algorithm, @RequestParam String templateType) {
        try {
            if (algorithm == null || algorithm.isEmpty()) {
                return AjaxResult.error("算法名称不能为空");
            }
            
            if (templateType == null || templateType.isEmpty()) {
                templateType = "balanced";
            }
            
            Map<String, Object> template = parameterRecommendationService.getParameterTemplate(algorithm, templateType);
            
            return AjaxResult.success("获取参数模板成功", template);
            
        } catch (Exception e) {
            logger.error("获取参数模板失败", e);
            return AjaxResult.error("获取参数模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证参数配置
     */
    @PreAuthorize("@ss.hasPermi('petrol:analysis:add')")
    @PostMapping("/validate")
    public AjaxResult validateParameters(@RequestBody Map<String, Object> request) {
        try {
            String algorithm = (String) request.get("algorithm");
            Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");
            
            if (algorithm == null || algorithm.isEmpty()) {
                return AjaxResult.error("算法名称不能为空");
            }
            
            if (parameters == null) {
                return AjaxResult.error("参数配置不能为空");
            }
            
            Map<String, Object> validation = parameterRecommendationService.validateParameters(algorithm, parameters);
            
            return AjaxResult.success("参数验证完成", validation);
            
        } catch (Exception e) {
            logger.error("参数验证失败", e);
            return AjaxResult.error("参数验证失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取算法支持的参数模板类型
     */
    @PreAuthorize("@ss.hasPermi('petrol:analysis:query')")
    @GetMapping("/templates")
    public AjaxResult getSupportedTemplates(@RequestParam String algorithm) {
        try {
            // 返回支持的模板类型
            Map<String, Object> templates = new HashMap<>();

            Map<String, Object> fastTemplate = new HashMap<>();
            fastTemplate.put("label", "快速训练");
            fastTemplate.put("description", "适合快速验证和原型开发");
            templates.put("fast", fastTemplate);

            Map<String, Object> balancedTemplate = new HashMap<>();
            balancedTemplate.put("label", "平衡性能");
            balancedTemplate.put("description", "在训练时间和模型精度之间取得平衡");
            templates.put("balanced", balancedTemplate);

            Map<String, Object> highAccuracyTemplate = new HashMap<>();
            highAccuracyTemplate.put("label", "高精度");
            highAccuracyTemplate.put("description", "追求最佳模型性能，训练时间较长");
            templates.put("high_accuracy", highAccuracyTemplate);

            Map<String, Object> customTemplate = new HashMap<>();
            customTemplate.put("label", "自定义");
            customTemplate.put("description", "手动配置所有参数");
            templates.put("custom", customTemplate);

            return AjaxResult.success("获取模板类型成功", templates);
            
        } catch (Exception e) {
            logger.error("获取模板类型失败", e);
            return AjaxResult.error("获取模板类型失败: " + e.getMessage());
        }
    }
}
