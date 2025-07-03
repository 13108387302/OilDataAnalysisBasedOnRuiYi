package com.ruoyi.common.utils.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.file.FileNameLengthLimitExceededException;
import com.ruoyi.common.exception.file.FileSizeLimitExceededException;
import com.ruoyi.common.exception.file.InvalidExtensionException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.common.utils.uuid.Seq;
import com.ruoyi.common.utils.spring.SpringUtils;

/**
 * 文件上传工具类
 * 
 * @author ruoyi
 */
public class FileUploadUtils
{
    /**
     * 默认大小 50M
     */
    public static final long DEFAULT_MAX_SIZE = 50 * 1024 * 1024L;

    /**
     * 默认的文件名最大长度 100
     */
    public static final int DEFAULT_FILE_NAME_LENGTH = 100;

    /**
     * 默认上传的地址
     */
    private static String defaultBaseDir = SpringUtils.getBean(RuoYiConfig.class).getProfile();

    public static void setDefaultBaseDir(String defaultBaseDir)
    {
        FileUploadUtils.defaultBaseDir = defaultBaseDir;
    }

    public static String getDefaultBaseDir()
    {
        return defaultBaseDir;
    }

    /**
     * 以默认配置进行文件上传
     *
     * @param file 上传的文件
     * @return 文件名称
     * @throws Exception
     */
    public static final String upload(MultipartFile file) throws IOException
    {
        try
        {
            return upload(getDefaultBaseDir(), file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * 根据文件路径上传
     *
     * @param baseDir 相对应用的基目录
     * @param file 上传的文件
     * @return 文件名称
     * @throws IOException
     */
    public static final String upload(String baseDir, MultipartFile file) throws IOException
    {
        try
        {
            return upload(baseDir, file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * 文件上传
     *
     * @param baseDir 相对应用的基目录
     * @param file 上传的文件
     * @param allowedExtension 上传文件类型
     * @return 返回上传成功的文件名
     * @throws FileSizeLimitExceededException 如果超出最大大小
     * @throws FileNameLengthLimitExceededException 文件名太长
     * @throws IOException 比如读写文件出错时
     * @throws InvalidExtensionException 文件校验异常
     */
    public static final String upload(String baseDir, MultipartFile file, String[] allowedExtension)
            throws FileSizeLimitExceededException, IOException, FileNameLengthLimitExceededException,
            InvalidExtensionException
    {
        return upload(baseDir, file, allowedExtension, false);
    }
    
    /**
     * 文件上传
     *
     * @param baseDir 相对应用的基目录
     * @param file 上传的文件
     * @param useCustomNaming 系统自定义文件名
     * @param allowedExtension 上传文件类型
     * @return 返回上传成功的文件名
     * @throws FileSizeLimitExceededException 如果超出最大大小
     * @throws FileNameLengthLimitExceededException 文件名太长
     * @throws IOException 比如读写文件出错时
     * @throws InvalidExtensionException 文件校验异常
     */
    public static final String upload(String baseDir, MultipartFile file, String[] allowedExtension, boolean useCustomNaming)
            throws FileSizeLimitExceededException, IOException, FileNameLengthLimitExceededException,
            InvalidExtensionException
    {
        int fileNameLength = Objects.requireNonNull(file.getOriginalFilename()).length();
        if (fileNameLength > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH)
        {
            throw new FileNameLengthLimitExceededException(FileUploadUtils.DEFAULT_FILE_NAME_LENGTH);
        }

        assertAllowed(file, allowedExtension);

        String fileName = useCustomNaming ? uuidFilename(file) : extractFilename(file);

        String absPath = getAbsoluteFile(baseDir, fileName).getAbsolutePath();

        // 使用更安全的文件保存方式，避免transferTo可能导致的文件损坏
        saveFileSecurely(file, absPath);

        return getPathFileName(baseDir, fileName);
    }

    /**
     * 安全地保存文件，避免transferTo可能导致的文件损坏
     */
    private static void saveFileSecurely(MultipartFile file, String absPath) throws IOException {
        saveFileSecurely(file, absPath, null);
    }

    /**
     * 安全地保存文件（支持事务）
     */
    private static void saveFileSecurely(MultipartFile file, String absPath, FileUploadTransaction transaction) throws IOException {
        File targetFile = new File(absPath);
        File tempFile = new File(absPath + ".tmp");

        // 确保父目录存在
        File parentDir = targetFile.getParentFile();
        boolean dirCreated = false;
        if (!parentDir.exists()) {
            dirCreated = parentDir.mkdirs();
            if (transaction != null && dirCreated) {
                transaction.addCreatedDirectory(parentDir.getAbsolutePath());
            }
        }

        // 将临时文件和目标文件添加到事务中
        if (transaction != null) {
            transaction.addTempFile(tempFile);
            transaction.addTargetFile(targetFile);
        }

        // 获取原始文件大小用于验证
        long originalSize = file.getSize();
        long writtenBytes = 0;

        // 调试信息
        System.out.println("🔍 [DEBUG] 开始保存文件: " + targetFile.getAbsolutePath());
        System.out.println("🔍 [DEBUG] 原始文件大小: " + originalSize + " 字节");
        System.out.println("🔍 [DEBUG] 临时文件路径: " + tempFile.getAbsolutePath());

        // 使用临时文件进行写入，成功后再重命名
        try (InputStream inputStream = file.getInputStream();
             BufferedInputStream bis = new BufferedInputStream(inputStream, 65536);
             FileOutputStream fos = new FileOutputStream(tempFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 65536)) {

            // 使用更大的缓冲区提高性能
            byte[] buffer = new byte[65536];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                writtenBytes += bytesRead;
            }

            // 确保所有数据都写入磁盘
            bos.flush();
            fos.getFD().sync();

            System.out.println("🔍 [DEBUG] 文件写入完成，写入字节数: " + writtenBytes);

        } catch (IOException e) {
            // 如果保存失败，删除临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw new IOException("文件写入失败: " + e.getMessage(), e);
        }

        // 验证文件大小
        System.out.println("🔍 [DEBUG] 临时文件大小: " + tempFile.length() + " 字节");
        if (tempFile.length() != originalSize) {
            System.out.println("❌ [DEBUG] 文件大小验证失败！期望: " + originalSize + "，实际: " + tempFile.length());
            tempFile.delete();
            throw new IOException(String.format("文件大小验证失败，期望: %d 字节，实际: %d 字节",
                originalSize, tempFile.length()));
        }
        System.out.println("✅ [DEBUG] 文件大小验证通过");

        // 验证写入的字节数
        if (writtenBytes != originalSize) {
            tempFile.delete();
            throw new IOException(String.format("写入字节数验证失败，期望: %d 字节，实际: %d 字节",
                originalSize, writtenBytes));
        }

        // 原子性地重命名临时文件为目标文件
        try {
            if (targetFile.exists()) {
                targetFile.delete();
            }

            if (!tempFile.renameTo(targetFile)) {
                // 如果重命名失败，尝试复制
                java.nio.file.Files.move(tempFile.toPath(), targetFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (Exception e) {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw new IOException("文件重命名失败: " + e.getMessage(), e);
        }

        // 最终验证目标文件
        System.out.println("🔍 [DEBUG] 最终文件验证 - 存在: " + targetFile.exists() + ", 大小: " + targetFile.length());
        if (!targetFile.exists() || targetFile.length() != originalSize) {
            System.out.println("❌ [DEBUG] 最终文件验证失败！");
            throw new IOException("文件保存后验证失败");
        }
        System.out.println("✅ [DEBUG] 文件保存成功: " + targetFile.getAbsolutePath());
    }

    /**
     * 编码文件名(日期格式目录 + 原文件名 + 序列值 + 后缀)
     */
    public static final String extractFilename(MultipartFile file)
    {
        return StringUtils.format("{}_{}.{}", FilenameUtils.getBaseName(file.getOriginalFilename()), Seq.getId(Seq.uploadSeqType), getExtension(file));
    }

    /**
     * 编编码文件名(日期格式目录 + UUID + 后缀)
     */
    public static final String uuidFilename(MultipartFile file)
    {
        return StringUtils.format("{}.{}", IdUtils.fastSimpleUUID(), getExtension(file));
    }

    public static final File getAbsoluteFile(String uploadDir, String fileName) throws IOException
    {
        File desc = new File(uploadDir + File.separator + fileName);

        if (!desc.exists())
        {
            if (!desc.getParentFile().exists())
            {
                desc.getParentFile().mkdirs();
            }
        }
        return desc;
    }

    public static final String getPathFileName(String uploadDir, String fileName) throws IOException
    {
        int dirLastIndex = SpringUtils.getBean(RuoYiConfig.class).getProfile().length() + 1;
        String currentDir = StringUtils.substring(uploadDir, dirLastIndex);
        return Constants.RESOURCE_PREFIX + "/" + currentDir + "/" + fileName;
    }

    /**
     * 文件大小校验
     *
     * @param file 上传的文件
     * @return
     * @throws FileSizeLimitExceededException 如果超出最大大小
     * @throws InvalidExtensionException
     */
    public static final void assertAllowed(MultipartFile file, String[] allowedExtension)
            throws FileSizeLimitExceededException, InvalidExtensionException
    {
        long size = file.getSize();
        if (size > DEFAULT_MAX_SIZE)
        {
            throw new FileSizeLimitExceededException(DEFAULT_MAX_SIZE / 1024 / 1024);
        }

        String fileName = file.getOriginalFilename();
        String extension = getExtension(file);
        if (allowedExtension != null && !isAllowedExtension(extension, allowedExtension))
        {
            if (allowedExtension == MimeTypeUtils.IMAGE_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidImageExtensionException(allowedExtension, extension,
                        fileName);
            }
            else if (allowedExtension == MimeTypeUtils.FLASH_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidFlashExtensionException(allowedExtension, extension,
                        fileName);
            }
            else if (allowedExtension == MimeTypeUtils.MEDIA_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidMediaExtensionException(allowedExtension, extension,
                        fileName);
            }
            else if (allowedExtension == MimeTypeUtils.VIDEO_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidVideoExtensionException(allowedExtension, extension,
                        fileName);
            }
            else
            {
                throw new InvalidExtensionException(allowedExtension, extension, fileName);
            }
        }
    }

    /**
     * 判断MIME类型是否是允许的MIME类型
     *
     * @param extension
     * @param allowedExtension
     * @return
     */
    public static final boolean isAllowedExtension(String extension, String[] allowedExtension)
    {
        for (String str : allowedExtension)
        {
            if (str.equalsIgnoreCase(extension))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文件名的后缀
     *
     * @param file 表单文件
     * @return 后缀名
     */
    public static final String getExtension(MultipartFile file)
    {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (StringUtils.isEmpty(extension))
        {
            extension = MimeTypeUtils.getExtension(Objects.requireNonNull(file.getContentType()));
        }
        return extension;
    }

    /**
     * 计算文件的MD5哈希值
     *
     * @param file 文件
     * @return MD5哈希值
     * @throws IOException
     */
    public static String calculateFileMD5(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis, 65536)) {

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[65536];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }

            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5算法不可用", e);
        }
    }

    /**
     * 计算MultipartFile的MD5哈希值
     *
     * @param file MultipartFile
     * @return MD5哈希值
     * @throws IOException
     */
    public static String calculateFileMD5(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             BufferedInputStream bis = new BufferedInputStream(is, 65536)) {

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[65536];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }

            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5算法不可用", e);
        }
    }

    /**
     * 验证上传文件的完整性
     *
     * @param originalFile 原始文件
     * @param savedFile 保存的文件
     * @return 是否完整
     */
    public static boolean verifyFileIntegrity(MultipartFile originalFile, File savedFile) {
        try {
            System.out.println("🔍 [DEBUG] 开始验证文件完整性");
            System.out.println("🔍 [DEBUG] 原始文件大小: " + originalFile.getSize());
            System.out.println("🔍 [DEBUG] 保存文件大小: " + savedFile.length());

            // 验证文件大小
            if (originalFile.getSize() != savedFile.length()) {
                System.out.println("❌ [DEBUG] 文件大小不匹配！");
                return false;
            }

            // 验证MD5哈希
            System.out.println("🔍 [DEBUG] 开始计算MD5哈希值");
            String originalMD5 = calculateFileMD5(originalFile);
            String savedMD5 = calculateFileMD5(savedFile);

            System.out.println("🔍 [DEBUG] 原始文件MD5: " + originalMD5);
            System.out.println("🔍 [DEBUG] 保存文件MD5: " + savedMD5);

            boolean result = originalMD5.equals(savedMD5);
            System.out.println("🔍 [DEBUG] MD5验证结果: " + (result ? "通过" : "失败"));

            return result;

        } catch (IOException e) {
            System.out.println("❌ [DEBUG] 完整性验证异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 安全上传文件（带完整性验证）
     *
     * @param baseDir 基础目录
     * @param file 上传文件
     * @param allowedExtension 允许的扩展名
     * @param useCustomNaming 是否使用自定义命名
     * @return 文件路径
     * @throws FileSizeLimitExceededException
     * @throws IOException
     * @throws FileNameLengthLimitExceededException
     * @throws InvalidExtensionException
     */
    public static final String uploadWithIntegrityCheck(String baseDir, MultipartFile file,
            String[] allowedExtension, boolean useCustomNaming)
            throws FileSizeLimitExceededException, IOException, FileNameLengthLimitExceededException,
            InvalidExtensionException {

        // 执行正常的上传流程
        String filePath = upload(baseDir, file, allowedExtension, useCustomNaming);

        // 验证上传后的文件完整性
        String absolutePath = getAbsoluteFile(baseDir, filePath).getAbsolutePath();
        File savedFile = new File(absolutePath);

        if (!verifyFileIntegrity(file, savedFile)) {
            // 如果验证失败，删除损坏的文件
            savedFile.delete();
            throw new IOException("文件上传后完整性验证失败，文件可能已损坏");
        }

        return filePath;
    }

    /**
     * 事务性文件上传
     *
     * @param baseDir 基础目录
     * @param file 上传文件
     * @param allowedExtension 允许的扩展名
     * @param useCustomNaming 是否使用自定义命名
     * @return 文件路径
     * @throws FileSizeLimitExceededException
     * @throws IOException
     * @throws FileNameLengthLimitExceededException
     * @throws InvalidExtensionException
     */
    public static final String uploadWithTransaction(String baseDir, MultipartFile file,
            String[] allowedExtension, boolean useCustomNaming)
            throws FileSizeLimitExceededException, IOException, FileNameLengthLimitExceededException,
            InvalidExtensionException {

        FileUploadTransaction transaction = FileUploadTransaction.begin();
        String fileName = useCustomNaming ? uuidFilename(file) : extractFilename(file);
        String absPath = getAbsoluteFile(baseDir, fileName).getAbsolutePath();

        // 开始监控
        FileUploadMonitor.UploadSession session = FileUploadMonitor.startUpload(file, absPath);

        try {
            int fileNameLength = Objects.requireNonNull(file.getOriginalFilename()).length();
            if (fileNameLength > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH) {
                throw new FileNameLengthLimitExceededException(FileUploadUtils.DEFAULT_FILE_NAME_LENGTH);
            }

            assertAllowed(file, allowedExtension);

            // 使用支持事务的文件保存方法
            saveFileSecurely(file, absPath, transaction);

            // 验证文件完整性
            File savedFile = new File(absPath);
            if (!verifyFileIntegrity(file, savedFile)) {
                throw new IOException("文件上传后完整性验证失败");
            }

            // 提交事务
            transaction.commit();

            // 记录成功
            FileUploadMonitor.recordSuccess(session, savedFile);

            return getPathFileName(baseDir, fileName);

        } catch (Exception e) {
            // 回滚事务
            transaction.rollback();
            // 记录失败
            FileUploadMonitor.recordFailure(session, e);
            throw e;
        }
    }
}
