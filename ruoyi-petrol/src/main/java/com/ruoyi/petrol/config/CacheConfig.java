package com.ruoyi.petrol.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 使用Caffeine作为本地缓存，提升系统性能
 * 
 * @author ruoyi
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * 默认缓存管理器
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(defaultCaffeineBuilder());
        return cacheManager;
    }
    
    /**
     * 数据集缓存
     * 用于缓存数据集列表和基本信息
     */
    @Bean("datasetCache")
    public Cache<String, Object> datasetCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
    
    /**
     * 模型缓存
     * 用于缓存模型列表和元数据
     */
    @Bean("modelCache")
    public Cache<String, Object> modelCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .expireAfterAccess(20, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
    
    /**
     * 分析结果缓存
     * 用于缓存分析任务的结果
     */
    @Bean("analysisResultCache")
    public Cache<String, Object> analysisResultCache() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
    
    /**
     * 用户会话缓存
     * 用于缓存用户相关的临时数据
     */
    @Bean("userSessionCache")
    public Cache<String, Object> userSessionCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(4, TimeUnit.HOURS)
                .expireAfterAccess(1, TimeUnit.HOURS)
                .recordStats()
                .build();
    }
    
    /**
     * 文件元数据缓存
     * 用于缓存文件的列信息等元数据
     */
    @Bean("fileMetadataCache")
    public Cache<String, Object> fileMetadataCache() {
        return Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(6, TimeUnit.HOURS)
                .expireAfterAccess(2, TimeUnit.HOURS)
                .recordStats()
                .build();
    }
    
    /**
     * 预测结果缓存
     * 用于缓存预测任务的结果
     */
    @Bean("predictionCache")
    public Cache<String, Object> predictionCache() {
        return Caffeine.newBuilder()
                .maximumSize(300)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .expireAfterAccess(20, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * 可视化缓存
     * 用于缓存图表和可视化数据
     */
    @Bean("visualizationCache")
    public Cache<String, Object> visualizationCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * 统计数据缓存
     * 用于缓存各种统计信息
     */
    @Bean("statisticsCache")
    public Cache<String, Object> statisticsCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * 系统配置缓存
     * 用于缓存系统配置和字典数据
     */
    @Bean("configCache")
    public Cache<String, Object> configCache() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .expireAfterAccess(6, TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    /**
     * 默认Caffeine构建器
     */
    private Caffeine<Object, Object> defaultCaffeineBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .recordStats();
    }
}
