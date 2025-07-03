package com.ruoyi.common.utils.file;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传监控器
 * 记录文件上传的详细信息和性能指标
 * 
 * @author ruoyi
 */
public class FileUploadMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(FileUploadMonitor.class);
    private static final Logger uploadLog = LoggerFactory.getLogger("FILE_UPLOAD");
    
    // 统计信息
    private static final AtomicLong totalUploads = new AtomicLong(0);
    private static final AtomicLong successfulUploads = new AtomicLong(0);
    private static final AtomicLong failedUploads = new AtomicLong(0);
    private static final AtomicLong totalBytes = new AtomicLong(0);
    
    /**
     * 记录上传开始
     */
    public static UploadSession startUpload(MultipartFile file, String targetPath) {
        totalUploads.incrementAndGet();
        
        UploadSession session = new UploadSession();
        session.fileName = file.getOriginalFilename();
        session.fileSize = file.getSize();
        session.contentType = file.getContentType();
        session.targetPath = targetPath;
        session.startTime = System.currentTimeMillis();
        session.sessionId = generateSessionId();
        
        uploadLog.info("UPLOAD_START|{}|{}|{}|{}|{}", 
            session.sessionId,
            session.fileName,
            session.fileSize,
            session.contentType,
            session.targetPath);
        
        log.debug("开始上传文件: {} ({}字节) -> {}", 
            session.fileName, session.fileSize, session.targetPath);
        
        return session;
    }
    
    /**
     * 记录上传成功
     */
    public static void recordSuccess(UploadSession session, File savedFile) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - session.startTime;
        
        successfulUploads.incrementAndGet();
        totalBytes.addAndGet(session.fileSize);
        
        // 计算上传速度 (KB/s)
        double speedKBps = session.fileSize > 0 && duration > 0 ? 
            (session.fileSize / 1024.0) / (duration / 1000.0) : 0;
        
        uploadLog.info("UPLOAD_SUCCESS|{}|{}|{}|{}|{:.2f}|{}",
            session.sessionId,
            session.fileName,
            session.fileSize,
            duration,
            String.format("%.2f", speedKBps),
            savedFile.getAbsolutePath());

        log.info("文件上传成功: {} ({}字节, {}ms, {:.2f}KB/s)",
            session.fileName, session.fileSize, duration, String.format("%.2f", speedKBps));
    }
    
    /**
     * 记录上传失败
     */
    public static void recordFailure(UploadSession session, Exception error) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - session.startTime;
        
        failedUploads.incrementAndGet();
        
        uploadLog.error("UPLOAD_FAILURE|{}|{}|{}|{}|{}", 
            session.sessionId,
            session.fileName,
            session.fileSize,
            duration,
            error.getMessage());
        
        log.error("文件上传失败: {} ({}字节, {}ms) - {}", 
            session.fileName, session.fileSize, duration, error.getMessage(), error);
    }
    
    /**
     * 记录分片上传开始
     */
    public static ChunkUploadSession startChunkUpload(String fileName, String fileHash, 
            int totalChunks, long totalSize) {
        
        ChunkUploadSession session = new ChunkUploadSession();
        session.fileName = fileName;
        session.fileHash = fileHash;
        session.totalChunks = totalChunks;
        session.totalSize = totalSize;
        session.startTime = System.currentTimeMillis();
        session.sessionId = generateSessionId();
        
        uploadLog.info("CHUNK_UPLOAD_START|{}|{}|{}|{}|{}", 
            session.sessionId,
            session.fileName,
            session.fileHash,
            session.totalChunks,
            session.totalSize);
        
        log.info("开始分片上传: {} ({}个分片, {}字节)", 
            session.fileName, session.totalChunks, session.totalSize);
        
        return session;
    }
    
    /**
     * 记录分片上传成功
     */
    public static void recordChunkSuccess(ChunkUploadSession session, int chunkIndex) {
        session.uploadedChunks++;
        
        uploadLog.debug("CHUNK_SUCCESS|{}|{}|{}/{}", 
            session.sessionId,
            session.fileName,
            chunkIndex,
            session.totalChunks);
        
        log.debug("分片上传成功: {} 分片{}/{}", 
            session.fileName, chunkIndex + 1, session.totalChunks);
    }
    
    /**
     * 记录分片合并成功
     */
    public static void recordMergeSuccess(ChunkUploadSession session, File mergedFile) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - session.startTime;
        
        successfulUploads.incrementAndGet();
        totalBytes.addAndGet(session.totalSize);
        
        double speedKBps = session.totalSize > 0 && duration > 0 ? 
            (session.totalSize / 1024.0) / (duration / 1000.0) : 0;
        
        uploadLog.info("CHUNK_MERGE_SUCCESS|{}|{}|{}|{}|{:.2f}|{}", 
            session.sessionId,
            session.fileName,
            session.totalSize,
            duration,
            speedKBps,
            mergedFile.getAbsolutePath());
        
        log.info("分片合并成功: {} ({}字节, {}ms, {:.2f}KB/s)", 
            session.fileName, session.totalSize, duration, speedKBps);
    }
    
    /**
     * 获取上传统计信息
     */
    public static String getStatistics() {
        return String.format("上传统计 - 总计: %d, 成功: %d, 失败: %d, 总字节数: %d", 
            totalUploads.get(), successfulUploads.get(), failedUploads.get(), totalBytes.get());
    }
    
    /**
     * 生成会话ID
     */
    private static String generateSessionId() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + 
               "_" + Thread.currentThread().getId() + 
               "_" + System.nanoTime() % 10000;
    }
    
    /**
     * 上传会话信息
     */
    public static class UploadSession {
        public String sessionId;
        public String fileName;
        public long fileSize;
        public String contentType;
        public String targetPath;
        public long startTime;
    }
    
    /**
     * 分片上传会话信息
     */
    public static class ChunkUploadSession {
        public String sessionId;
        public String fileName;
        public String fileHash;
        public int totalChunks;
        public long totalSize;
        public int uploadedChunks = 0;
        public long startTime;
    }
}
