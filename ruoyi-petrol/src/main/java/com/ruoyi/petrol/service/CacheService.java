package com.ruoyi.petrol.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 缓存管理服务
 * 提供统一的缓存操作接口
 */
@Service
public class CacheService {
    
    @Autowired
    private Cache<String, Object> datasetCache;
    
    @Autowired
    private Cache<String, Object> modelCache;
    
    @Autowired
    private Cache<String, Object> analysisResultCache;
    
    @Autowired
    private Cache<String, Object> userSessionCache;
    
    @Autowired
    private Cache<String, Object> fileMetadataCache;
    
    /**
     * 数据集缓存操作
     */
    public void putDataset(String key, Object value) {
        datasetCache.put(key, value);
    }
    
    public Object getDataset(String key) {
        return datasetCache.getIfPresent(key);
    }
    
    public void evictDataset(String key) {
        datasetCache.invalidate(key);
    }
    
    public void clearDatasetCache() {
        datasetCache.invalidateAll();
    }
    
    /**
     * 模型缓存操作
     */
    public void putModel(String key, Object value) {
        modelCache.put(key, value);
    }
    
    public Object getModel(String key) {
        return modelCache.getIfPresent(key);
    }
    
    public void evictModel(String key) {
        modelCache.invalidate(key);
    }
    
    /**
     * 分析结果缓存操作
     */
    public void putAnalysisResult(String key, Object value) {
        analysisResultCache.put(key, value);
    }
    
    public Object getAnalysisResult(String key) {
        return analysisResultCache.getIfPresent(key);
    }
    
    /**
     * 文件元数据缓存操作
     */
    public void putFileMetadata(String key, Object value) {
        fileMetadataCache.put(key, value);
    }
    
    public Object getFileMetadata(String key) {
        return fileMetadataCache.getIfPresent(key);
    }
    
    /**
     * 用户会话缓存操作
     */
    public void putUserSession(String key, Object value) {
        userSessionCache.put(key, value);
    }
    
    public Object getUserSession(String key) {
        return userSessionCache.getIfPresent(key);
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("Dataset Cache: ").append(datasetCache.stats()).append("\n");
        stats.append("Model Cache: ").append(modelCache.stats()).append("\n");
        stats.append("Analysis Result Cache: ").append(analysisResultCache.stats()).append("\n");
        stats.append("User Session Cache: ").append(userSessionCache.stats()).append("\n");
        stats.append("File Metadata Cache: ").append(fileMetadataCache.stats()).append("\n");
        return stats.toString();
    }
    
    /**
     * 清理所有缓存
     */
    public void clearAllCaches() {
        datasetCache.invalidateAll();
        modelCache.invalidateAll();
        analysisResultCache.invalidateAll();
        userSessionCache.invalidateAll();
        fileMetadataCache.invalidateAll();
    }
}
