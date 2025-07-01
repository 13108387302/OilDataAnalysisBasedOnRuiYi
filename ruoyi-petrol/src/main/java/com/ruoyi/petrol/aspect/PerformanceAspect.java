package com.ruoyi.petrol.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 性能监控切面
 * 用于监控方法执行时间和性能指标
 * 
 * @author ruoyi
 */
@Aspect
@Component
public class PerformanceAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceAspect.class);
    
    /**
     * 性能监控注解
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PerformanceMonitor {
        /**
         * 操作描述
         */
        String value() default "";
        
        /**
         * 慢查询阈值（毫秒）
         */
        long threshold() default 1000;
        
        /**
         * 是否记录参数
         */
        boolean logArgs() default false;
        
        /**
         * 是否记录返回值
         */
        boolean logResult() default false;
    }
    
    /**
     * 切点：所有Controller方法
     */
    @Pointcut("execution(* com.ruoyi.petrol.controller..*(..))")
    public void controllerMethods() {}
    
    /**
     * 切点：所有Service方法
     */
    @Pointcut("execution(* com.ruoyi.petrol.service..*(..))")
    public void serviceMethods() {}
    
    /**
     * 切点：带有PerformanceMonitor注解的方法
     */
    @Pointcut("@annotation(performanceMonitor)")
    public void annotatedMethods(PerformanceMonitor performanceMonitor) {}
    
    /**
     * 监控Controller方法性能
     */
    @Around("controllerMethods()")
    public Object monitorController(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorExecution(joinPoint, "Controller", 2000, false, false);
    }
    
    /**
     * 监控Service方法性能
     */
    @Around("serviceMethods()")
    public Object monitorService(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorExecution(joinPoint, "Service", 1000, false, false);
    }
    
    /**
     * 监控带注解的方法
     */
    @Around("annotatedMethods(performanceMonitor)")
    public Object monitorAnnotated(ProceedingJoinPoint joinPoint, PerformanceMonitor performanceMonitor) throws Throwable {
        return monitorExecution(
            joinPoint, 
            performanceMonitor.value(), 
            performanceMonitor.threshold(),
            performanceMonitor.logArgs(),
            performanceMonitor.logResult()
        );
    }
    
    /**
     * 执行性能监控
     */
    private Object monitorExecution(ProceedingJoinPoint joinPoint, String operation, 
                                  long threshold, boolean logArgs, boolean logResult) throws Throwable {
        
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 记录方法调用
        if (logArgs) {
            logger.info("开始执行 {} - {}, 参数: {}", operation, fullMethodName, joinPoint.getArgs());
        } else {
            logger.debug("开始执行 {} - {}", operation, fullMethodName);
        }
        
        Object result = null;
        Exception exception = null;
        
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // 记录执行结果
            if (exception != null) {
                logger.error("执行失败 {} - {}, 耗时: {}ms, 异常: {}", 
                    operation, fullMethodName, executionTime, exception.getMessage());
            } else if (executionTime > threshold) {
                logger.warn("慢操作 {} - {}, 耗时: {}ms (阈值: {}ms)", 
                    operation, fullMethodName, executionTime, threshold);
                if (logResult) {
                    logger.warn("返回值: {}", result);
                }
            } else {
                logger.debug("执行完成 {} - {}, 耗时: {}ms", operation, fullMethodName, executionTime);
                if (logResult) {
                    logger.debug("返回值: {}", result);
                }
            }
            
            // 记录性能指标到监控系统（可选）
            recordMetrics(operation, fullMethodName, executionTime, exception == null);
        }
    }
    
    /**
     * 记录性能指标
     */
    private void recordMetrics(String operation, String method, long executionTime, boolean success) {
        // 这里可以集成监控系统，如Micrometer、Prometheus等
        // 例如：
        // meterRegistry.timer("method.execution.time", "operation", operation, "method", method)
        //     .record(executionTime, TimeUnit.MILLISECONDS);
        // 
        // meterRegistry.counter("method.execution.count", "operation", operation, "method", method, "success", String.valueOf(success))
        //     .increment();
        
        // 当前只记录到日志
        if (executionTime > 5000) { // 超过5秒的操作
            logger.warn("超长操作记录 - 操作: {}, 方法: {}, 耗时: {}ms, 成功: {}", 
                operation, method, executionTime, success);
        }
    }
}
