package com.ruoyi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 读取项目相关配置
 * 
 * @author ruoyi
 */
@Component
@ConfigurationProperties(prefix = "ruoyi")
public class RuoYiConfig
{
    /** 项目名称 */
    private String name;

    /** 版本 */
    private String version;

    /** 版权年份 */
    private String copyrightYear;

    /** 上传路径 */
    private String profile;

    /** 获取地址开关 */
    private boolean addressEnabled;

    /** 验证码类型 */
    private String captchaType;

    /** 自动去除表前缀 */
    private static String autoRemovePre;

    /** 数据库和表名之间的分隔符 */
    private static String tableSeparator;

    /** Python解释器路径 */
    private String pythonExecutorPath;

    /** Python脚本根目录 */
    private String pythonScriptsPath;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getCopyrightYear()
    {
        return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear)
    {
        this.copyrightYear = copyrightYear;
    }

    public String getProfile()
    {
        return profile;
    }

    public void setProfile(String profile)
    {
        this.profile = profile;
    }

    public boolean isAddressEnabled()
    {
        return addressEnabled;
    }

    public void setAddressEnabled(boolean addressEnabled)
    {
        this.addressEnabled = addressEnabled;
    }

    public String getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(String captchaType) {
        this.captchaType = captchaType;
    }

    /**
     * 获取导入上传路径
     */
    public String getImportPath()
    {
        return getProfile() + "/import";
    }

    /**
     * 获取头像上传路径
     */
    public String getAvatarPath()
    {
        return getProfile() + "/avatar";
    }

    /**
     * 获取下载路径
     */
    public String getDownloadPath()
    {
        return getProfile() + "/download/";
    }

    /**
     * 获取上传路径
     */
    public String getUploadPath()
    {
        return getProfile() + "/upload";
    }

    public static void setTableSeparator(String tableSeparator)
    {
        RuoYiConfig.tableSeparator = tableSeparator;
    }

    public String getPythonExecutorPath() {
        return pythonExecutorPath;
    }

    public void setPythonExecutorPath(String pythonExecutorPath) {
        this.pythonExecutorPath = pythonExecutorPath;
    }

    public String getPythonScriptsPath() {
        return pythonScriptsPath;
    }

    public void setPythonScriptsPath(String pythonScriptsPath) {
        this.pythonScriptsPath = pythonScriptsPath;
    }
}
