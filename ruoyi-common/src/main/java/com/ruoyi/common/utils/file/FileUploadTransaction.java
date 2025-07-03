package com.ruoyi.common.utils.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件上传事务管理器
 * 确保文件上传操作的原子性，失败时能够完全回滚
 * 
 * @author ruoyi
 */
public class FileUploadTransaction {
    
    private static final Logger log = LoggerFactory.getLogger(FileUploadTransaction.class);
    
    private final List<File> tempFiles = new ArrayList<>();
    private final List<File> targetFiles = new ArrayList<>();
    private final List<String> createdDirectories = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private boolean committed = false;
    private boolean rolledBack = false;
    
    /**
     * 添加临时文件到事务中
     */
    public void addTempFile(File tempFile) {
        lock.lock();
        try {
            if (committed || rolledBack) {
                throw new IllegalStateException("事务已经提交或回滚");
            }
            tempFiles.add(tempFile);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 添加目标文件到事务中
     */
    public void addTargetFile(File targetFile) {
        lock.lock();
        try {
            if (committed || rolledBack) {
                throw new IllegalStateException("事务已经提交或回滚");
            }
            targetFiles.add(targetFile);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 添加创建的目录到事务中
     */
    public void addCreatedDirectory(String directory) {
        lock.lock();
        try {
            if (committed || rolledBack) {
                throw new IllegalStateException("事务已经提交或回滚");
            }
            createdDirectories.add(directory);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 提交事务
     */
    public void commit() throws IOException {
        lock.lock();
        try {
            if (committed) {
                return;
            }
            if (rolledBack) {
                throw new IllegalStateException("事务已经回滚，无法提交");
            }
            
            // 清理临时文件
            for (File tempFile : tempFiles) {
                if (tempFile.exists()) {
                    if (!tempFile.delete()) {
                        log.warn("无法删除临时文件: {}", tempFile.getAbsolutePath());
                    }
                }
            }
            
            committed = true;
            log.debug("文件上传事务提交成功");
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 回滚事务
     */
    public void rollback() {
        lock.lock();
        try {
            if (rolledBack) {
                return;
            }
            if (committed) {
                log.warn("尝试回滚已提交的事务");
                return;
            }
            
            // 删除所有临时文件
            for (File tempFile : tempFiles) {
                if (tempFile.exists()) {
                    if (!tempFile.delete()) {
                        log.error("无法删除临时文件: {}", tempFile.getAbsolutePath());
                    } else {
                        log.debug("已删除临时文件: {}", tempFile.getAbsolutePath());
                    }
                }
            }
            
            // 删除所有目标文件
            for (File targetFile : targetFiles) {
                if (targetFile.exists()) {
                    if (!targetFile.delete()) {
                        log.error("无法删除目标文件: {}", targetFile.getAbsolutePath());
                    } else {
                        log.debug("已删除目标文件: {}", targetFile.getAbsolutePath());
                    }
                }
            }
            
            // 删除创建的空目录（逆序删除）
            for (int i = createdDirectories.size() - 1; i >= 0; i--) {
                File dir = new File(createdDirectories.get(i));
                if (dir.exists() && dir.isDirectory()) {
                    String[] files = dir.list();
                    if (files == null || files.length == 0) {
                        if (!dir.delete()) {
                            log.warn("无法删除空目录: {}", dir.getAbsolutePath());
                        } else {
                            log.debug("已删除空目录: {}", dir.getAbsolutePath());
                        }
                    }
                }
            }
            
            rolledBack = true;
            log.debug("文件上传事务回滚完成");
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 检查事务状态
     */
    public boolean isCommitted() {
        return committed;
    }
    
    /**
     * 检查事务状态
     */
    public boolean isRolledBack() {
        return rolledBack;
    }
    
    /**
     * 自动关闭资源，确保事务被正确处理
     */
    public void close() {
        if (!committed && !rolledBack) {
            rollback();
        }
    }
    
    /**
     * 创建一个新的文件上传事务
     */
    public static FileUploadTransaction begin() {
        return new FileUploadTransaction();
    }
}
