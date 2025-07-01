package com.ruoyi.petrol.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一响应包装器
 * 提供标准化的API响应格式
 * 
 * @author ruoyi
 * @param <T> 响应数据类型
 */
@Schema(description = "统一响应格式")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseWrapper<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "响应码", example = "200")
    private Integer code;

    @Schema(description = "响应消息", example = "操作成功")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "响应时间戳")
    private LocalDateTime timestamp;

    @Schema(description = "请求追踪ID")
    private String traceId;

    @Schema(description = "是否成功")
    private Boolean success;
    
    public ResponseWrapper() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ResponseWrapper(Integer code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = code == 200;
    }
    
    /**
     * 成功响应
     */
    public static <T> ResponseWrapper<T> success() {
        return new ResponseWrapper<>(200, "操作成功", null);
    }
    
    public static <T> ResponseWrapper<T> success(T data) {
        return new ResponseWrapper<>(200, "操作成功", data);
    }
    
    public static <T> ResponseWrapper<T> success(String message, T data) {
        return new ResponseWrapper<>(200, message, data);
    }
    
    /**
     * 失败响应
     */
    public static <T> ResponseWrapper<T> error() {
        return new ResponseWrapper<>(500, "操作失败", null);
    }
    
    public static <T> ResponseWrapper<T> error(String message) {
        return new ResponseWrapper<>(500, message, null);
    }
    
    public static <T> ResponseWrapper<T> error(Integer code, String message) {
        return new ResponseWrapper<>(code, message, null);
    }
    
    /**
     * 参数错误响应
     */
    public static <T> ResponseWrapper<T> badRequest(String message) {
        return new ResponseWrapper<>(400, message, null);
    }
    
    /**
     * 未授权响应
     */
    public static <T> ResponseWrapper<T> unauthorized(String message) {
        return new ResponseWrapper<>(401, message, null);
    }
    
    /**
     * 禁止访问响应
     */
    public static <T> ResponseWrapper<T> forbidden(String message) {
        return new ResponseWrapper<>(403, message, null);
    }
    
    /**
     * 资源未找到响应
     */
    public static <T> ResponseWrapper<T> notFound(String message) {
        return new ResponseWrapper<>(404, message, null);
    }
    
    // Getters and Setters
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
        this.success = code == 200;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    @Override
    public String toString() {
        return "ResponseWrapper{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                ", traceId='" + traceId + '\'' +
                ", success=" + success +
                '}';
    }
}
