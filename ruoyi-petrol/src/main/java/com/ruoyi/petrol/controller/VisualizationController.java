package com.ruoyi.petrol.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.petrol.service.IDataSourceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据可视化Controller - 重构版
 * 基于统一数据源架构
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@RestController
@RequestMapping("/petrol/visualization")
public class VisualizationController extends BaseController
{
    @Autowired
    private IDataSourceService dataSourceService;

    /**
     * 获取所有可用的数据源
     */
    @PreAuthorize("@ss.hasPermi('petrol:visualization:view')")
    @GetMapping("/data-sources")
    public AjaxResult getAllDataSources()
    {
        try {
            List<Map<String, Object>> result = dataSourceService.getAllDataSources();
            return success(result);
        } catch (Exception e) {
            logger.error("获取数据源列表失败", e);
            return error("获取数据源列表失败: " + e.getMessage());
        }
    }



    /**
     * 获取数据源信息
     */
    @PreAuthorize("@ss.hasPermi('petrol:visualization:view')")
    @GetMapping("/data-source/{sourceId}/{sourceType}")
    public AjaxResult getDataSourceInfo(@PathVariable("sourceId") String sourceId,
                                       @PathVariable("sourceType") String sourceType)
    {
        try {
            Map<String, Object> result = dataSourceService.getDataSourceInfo(sourceId, sourceType);
            return success(result);
        } catch (Exception e) {
            logger.error("获取数据源信息失败", e);
            return error("获取数据源信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据源列信息
     */
    @PreAuthorize("@ss.hasPermi('petrol:visualization:view')")
    @GetMapping("/columns/{sourceId}/{sourceType}")
    public AjaxResult getDataSourceColumns(@PathVariable("sourceId") String sourceId,
                                          @PathVariable("sourceType") String sourceType)
    {
        try {
            Map<String, Object> result = dataSourceService.getDataSourceColumns(sourceId, sourceType);
            return success(result);
        } catch (Exception e) {
            logger.error("获取数据源列信息失败", e);
            return error("获取数据源列信息失败: " + e.getMessage());
        }
    }

    /**
     * 读取数据源数据
     */
    @PreAuthorize("@ss.hasPermi('petrol:visualization:view')")
    @PostMapping("/data/{sourceId}/{sourceType}")
    public AjaxResult readDataSourceData(@PathVariable("sourceId") String sourceId,
                                        @PathVariable("sourceType") String sourceType,
                                        @RequestBody(required = false) Map<String, Object> params)
    {
        try {
            List<String> columns = null;
            Integer maxRows = 100;

            if (params != null) {
                if (params.containsKey("columns")) {
                    columns = (List<String>) params.get("columns");
                }
                if (params.containsKey("maxRows")) {
                    maxRows = (Integer) params.get("maxRows");
                }
            }

            List<Map<String, Object>> result = dataSourceService.readDataSourceData(
                sourceId, sourceType, columns, maxRows);
            return success(result);
        } catch (Exception e) {
            logger.error("读取数据源数据失败", e);
            return error("读取数据源数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据源统计信息
     */
    @PreAuthorize("@ss.hasPermi('petrol:visualization:view')")
    @PostMapping("/statistics/{sourceId}/{sourceType}")
    public AjaxResult getDataSourceStatistics(@PathVariable("sourceId") String sourceId,
                                             @PathVariable("sourceType") String sourceType,
                                             @RequestBody(required = false) Map<String, Object> params)
    {
        try {
            List<String> columns = null;
            if (params != null && params.containsKey("columns")) {
                columns = (List<String>) params.get("columns");
            }

            Map<String, Object> result = dataSourceService.getDataSourceStatistics(
                sourceId, sourceType, columns);
            return success(result);
        } catch (Exception e) {
            logger.error("获取数据源统计信息失败", e);
            return error("获取数据源统计信息失败: " + e.getMessage());
        }
    }



}
