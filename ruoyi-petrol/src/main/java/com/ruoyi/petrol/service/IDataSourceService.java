package com.ruoyi.petrol.service;

import java.util.List;
import java.util.Map;

/**
 * 统一数据源服务接口
 * 支持数据集和分析任务数据的统一访问
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface IDataSourceService {
    
    /**
     * 获取所有可用的数据源
     * 包括数据集表中的数据和分析任务中的数据
     * 
     * @return 数据源列表
     */
    List<Map<String, Object>> getAllDataSources();
    
    /**
     * 根据数据源ID和类型获取数据源信息
     * 
     * @param sourceId 数据源ID
     * @param sourceType 数据源类型 (dataset/task)
     * @return 数据源信息
     */
    Map<String, Object> getDataSourceInfo(String sourceId, String sourceType);
    
    /**
     * 获取数据源的列信息
     * 
     * @param sourceId 数据源ID
     * @param sourceType 数据源类型
     * @return 列信息
     */
    Map<String, Object> getDataSourceColumns(String sourceId, String sourceType);
    
    /**
     * 读取数据源的实际数据
     * 
     * @param sourceId 数据源ID
     * @param sourceType 数据源类型
     * @param columns 需要读取的列（可选）
     * @param maxRows 最大行数（可选）
     * @return 数据内容
     */
    List<Map<String, Object>> readDataSourceData(String sourceId, String sourceType, 
                                                  List<String> columns, Integer maxRows);
    
    /**
     * 获取数据源的统计信息
     * 
     * @param sourceId 数据源ID
     * @param sourceType 数据源类型
     * @param columns 指定列（可选）
     * @return 统计信息
     */
    Map<String, Object> getDataSourceStatistics(String sourceId, String sourceType, 
                                               List<String> columns);
}
