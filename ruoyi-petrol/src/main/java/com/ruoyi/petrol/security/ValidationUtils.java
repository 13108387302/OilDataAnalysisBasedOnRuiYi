package com.ruoyi.petrol.security;

import org.springframework.util.StringUtils;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;

/**
 * 输入验证工具类
 * 提供各种输入数据的安全验证方法
 * 
 * @author ruoyi
 */
public class ValidationUtils {
    
    // 文件名安全字符正则表达式
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._\\-\\u4e00-\\u9fa5]+$");
    
    // SQL注入检测关键词
    private static final List<String> SQL_INJECTION_KEYWORDS = Arrays.asList(
        "select", "insert", "update", "delete", "drop", "create", "alter", 
        "exec", "execute", "union", "script", "javascript", "vbscript",
        "onload", "onerror", "onclick", "alert", "confirm", "prompt"
    );
    
    // XSS攻击检测模式
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)<script[^>]*>.*?</script>|javascript:|vbscript:|onload=|onerror=|onclick=|alert\\(|confirm\\(|prompt\\(",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // 路径遍历攻击检测
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("\\.\\.[\\\\/]");
    
    // 允许的文件扩展名
    private static final List<String> ALLOWED_FILE_EXTENSIONS = Arrays.asList(
        ".csv", ".xlsx", ".xls", ".txt", ".json", ".pkl", ".joblib", ".model"
    );
    
    // 最大文件大小 (100MB)
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024L;
    
    /**
     * 验证文件名是否安全
     * 
     * @param filename 文件名
     * @return 是否安全
     */
    public static boolean isValidFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return false;
        }
        
        // 检查文件名长度
        if (filename.length() > 255) {
            return false;
        }
        
        // 检查是否包含路径遍历字符
        if (PATH_TRAVERSAL_PATTERN.matcher(filename).find()) {
            return false;
        }
        
        // 检查文件名是否符合安全模式
        if (!SAFE_FILENAME_PATTERN.matcher(filename).matches()) {
            return false;
        }
        
        // 检查文件扩展名
        String extension = getFileExtension(filename);
        return ALLOWED_FILE_EXTENSIONS.contains(extension.toLowerCase());
    }
    
    /**
     * 验证文件大小是否在允许范围内
     * 
     * @param fileSize 文件大小（字节）
     * @return 是否在允许范围内
     */
    public static boolean isValidFileSize(long fileSize) {
        return fileSize > 0 && fileSize <= MAX_FILE_SIZE;
    }
    
    /**
     * 检测SQL注入攻击
     * 
     * @param input 输入字符串
     * @return 是否包含SQL注入
     */
    public static boolean containsSqlInjection(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        return SQL_INJECTION_KEYWORDS.stream()
                .anyMatch(keyword -> lowerInput.contains(keyword));
    }
    
    /**
     * 检测XSS攻击
     * 
     * @param input 输入字符串
     * @return 是否包含XSS攻击代码
     */
    public static boolean containsXss(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        
        return XSS_PATTERN.matcher(input).find();
    }
    
    /**
     * 检测路径遍历攻击
     * 
     * @param path 路径字符串
     * @return 是否包含路径遍历
     */
    public static boolean containsPathTraversal(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        
        return PATH_TRAVERSAL_PATTERN.matcher(path).find();
    }
    
    /**
     * 验证数据集名称
     * 
     * @param datasetName 数据集名称
     * @return 是否有效
     */
    public static boolean isValidDatasetName(String datasetName) {
        if (!StringUtils.hasText(datasetName)) {
            return false;
        }
        
        // 长度检查
        if (datasetName.length() < 2 || datasetName.length() > 100) {
            return false;
        }
        
        // 安全检查
        if (containsSqlInjection(datasetName) || containsXss(datasetName)) {
            return false;
        }
        
        // 字符检查：允许中文、英文、数字、下划线、连字符
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9_\\-\\u4e00-\\u9fa5\\s]+$");
        return pattern.matcher(datasetName).matches();
    }
    
    /**
     * 验证模型名称
     * 
     * @param modelName 模型名称
     * @return 是否有效
     */
    public static boolean isValidModelName(String modelName) {
        return isValidDatasetName(modelName); // 使用相同的验证规则
    }
    
    /**
     * 验证任务名称
     * 
     * @param taskName 任务名称
     * @return 是否有效
     */
    public static boolean isValidTaskName(String taskName) {
        return isValidDatasetName(taskName); // 使用相同的验证规则
    }
    
    /**
     * 验证数值参数
     * 
     * @param value 数值
     * @param min 最小值
     * @param max 最大值
     * @return 是否在有效范围内
     */
    public static boolean isValidNumericParameter(double value, double min, double max) {
        return value >= min && value <= max && !Double.isNaN(value) && !Double.isInfinite(value);
    }
    
    /**
     * 验证整数参数
     * 
     * @param value 整数值
     * @param min 最小值
     * @param max 最大值
     * @return 是否在有效范围内
     */
    public static boolean isValidIntegerParameter(int value, int min, int max) {
        return value >= min && value <= max;
    }
    
    /**
     * 清理用户输入，移除潜在的危险字符
     * 
     * @param input 原始输入
     * @return 清理后的字符串
     */
    public static String sanitizeInput(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        
        // 移除HTML标签
        String cleaned = input.replaceAll("<[^>]+>", "");
        
        // 移除JavaScript相关内容
        cleaned = cleaned.replaceAll("(?i)javascript:", "");
        cleaned = cleaned.replaceAll("(?i)vbscript:", "");
        cleaned = cleaned.replaceAll("(?i)on\\w+\\s*=", "");
        
        // 移除SQL关键词（如果在非预期位置出现）
        for (String keyword : SQL_INJECTION_KEYWORDS) {
            cleaned = cleaned.replaceAll("(?i)\\b" + keyword + "\\b", "");
        }
        
        return cleaned.trim();
    }
    
    /**
     * 获取文件扩展名
     * 
     * @param filename 文件名
     * @return 文件扩展名（包含点号）
     */
    private static String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        
        return filename.substring(lastDotIndex);
    }
    
    /**
     * 验证邮箱格式
     * 
     * @param email 邮箱地址
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        
        Pattern emailPattern = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        );
        
        return emailPattern.matcher(email).matches();
    }
    
    /**
     * 验证URL格式
     * 
     * @param url URL地址
     * @return 是否有效
     */
    public static boolean isValidUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        
        Pattern urlPattern = Pattern.compile(
            "^https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$"
        );
        
        return urlPattern.matcher(url).matches();
    }
    
    /**
     * 验证IP地址格式
     * 
     * @param ip IP地址
     * @return 是否有效
     */
    public static boolean isValidIpAddress(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        
        Pattern ipPattern = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        );
        
        return ipPattern.matcher(ip).matches();
    }
}
