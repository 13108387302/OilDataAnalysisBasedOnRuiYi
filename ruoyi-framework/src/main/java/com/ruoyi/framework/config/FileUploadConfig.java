package com.ruoyi.framework.config;

import java.io.File;
import javax.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * 文件上传配置
 * 
 * @author ruoyi
 */
@Configuration
public class FileUploadConfig {

    @Value("${ruoyi.profile:./data}")
    private String profile;

    /**
     * 文件上传配置
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // 设置文件大小限制
        factory.setMaxFileSize(DataSize.ofMegabytes(100)); // 100MB
        factory.setMaxRequestSize(DataSize.ofMegabytes(200)); // 200MB
        
        // 设置内存阈值，超过此大小的文件将写入临时文件
        factory.setFileSizeThreshold(DataSize.ofMegabytes(1)); // 1MB
        
        // 设置临时文件存储位置
        String tempDir = profile + File.separator + "temp";
        File tempDirFile = new File(tempDir);
        if (!tempDirFile.exists()) {
            tempDirFile.mkdirs();
        }
        factory.setLocation(tempDir);
        
        return factory.createMultipartConfig();
    }
}
