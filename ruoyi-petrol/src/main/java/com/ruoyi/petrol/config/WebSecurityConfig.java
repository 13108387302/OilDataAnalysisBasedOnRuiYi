package com.ruoyi.petrol.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Arrays;

/**
 * Web安全配置类
 * 配置系统的Web安全策略和防护措施
 *
 * @author ruoyi
 */
@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {
    
    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // 使用强度为12的BCrypt
    }
    
    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的源（生产环境应该配置具体的域名）
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 允许携带凭证
        configuration.setAllowCredentials(true);
        
        // 预检请求的缓存时间
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 安全头拦截器
        registry.addInterceptor(new SecurityHeaderInterceptor())
                .addPathPatterns("/**");
        
        // 请求限流拦截器
        registry.addInterceptor(new RateLimitInterceptor())
                .addPathPatterns("/api/**");
        
        // 输入验证拦截器
        registry.addInterceptor(new InputValidationInterceptor())
                .addPathPatterns("/api/**");
    }
    
    /**
     * 安全头拦截器
     * 添加安全相关的HTTP头
     */
    public static class SecurityHeaderInterceptor implements HandlerInterceptor {
        
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            // 防止点击劫持
            response.setHeader("X-Frame-Options", "DENY");
            
            // 防止MIME类型嗅探
            response.setHeader("X-Content-Type-Options", "nosniff");
            
            // XSS保护
            response.setHeader("X-XSS-Protection", "1; mode=block");
            
            // 强制HTTPS（生产环境启用）
            // response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            
            // 内容安全策略
            response.setHeader("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: blob:; " +
                "font-src 'self'; " +
                "connect-src 'self'; " +
                "frame-ancestors 'none';"
            );
            
            // 引用策略
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // 权限策略
            response.setHeader("Permissions-Policy", 
                "camera=(), microphone=(), geolocation=(), payment=()"
            );
            
            return true;
        }
    }
    
    /**
     * 请求限流拦截器
     * 防止API滥用和DDoS攻击
     */
    public static class RateLimitInterceptor implements HandlerInterceptor {
        
        // 简单的内存限流实现（生产环境建议使用Redis）
        private final java.util.Map<String, RateLimitInfo> rateLimitMap = 
            new java.util.concurrent.ConcurrentHashMap<>();
        
        // 每分钟最大请求数
        private static final int MAX_REQUESTS_PER_MINUTE = 100;
        
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String clientIp = getClientIpAddress(request);
            String key = clientIp + ":" + request.getRequestURI();
            
            RateLimitInfo rateLimitInfo = rateLimitMap.computeIfAbsent(key, k -> new RateLimitInfo());
            
            long currentTime = System.currentTimeMillis();
            
            // 重置计数器（每分钟）
            if (currentTime - rateLimitInfo.getLastResetTime() > 60000) {
                rateLimitInfo.setRequestCount(0);
                rateLimitInfo.setLastResetTime(currentTime);
            }
            
            // 检查是否超过限制
            if (rateLimitInfo.getRequestCount() >= MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(429); // Too Many Requests
                response.setHeader("Retry-After", "60");
                return false;
            }
            
            // 增加请求计数
            rateLimitInfo.incrementRequestCount();
            
            return true;
        }
        
        /**
         * 获取客户端真实IP地址
         */
        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        }
    }
    
    /**
     * 输入验证拦截器
     * 对所有输入进行基础安全检查
     */
    public static class InputValidationInterceptor implements HandlerInterceptor {
        
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            // 检查请求参数
            for (String paramName : request.getParameterMap().keySet()) {
                String[] paramValues = request.getParameterValues(paramName);
                for (String paramValue : paramValues) {
                    if (containsMaliciousContent(paramValue)) {
                        response.setStatus(400); // Bad Request
                        return false;
                    }
                }
            }
            
            // 检查请求头
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                if (containsMaliciousContent(headerValue)) {
                    response.setStatus(400); // Bad Request
                    return false;
                }
            }
            
            return true;
        }
        
        /**
         * 检查是否包含恶意内容
         */
        private boolean containsMaliciousContent(String input) {
            if (input == null || input.isEmpty()) {
                return false;
            }
            
            // 检查常见的攻击模式
            String lowerInput = input.toLowerCase();
            
            // SQL注入检查
            if (lowerInput.contains("union") && lowerInput.contains("select") ||
                lowerInput.contains("drop") && lowerInput.contains("table") ||
                lowerInput.contains("insert") && lowerInput.contains("into") ||
                lowerInput.contains("delete") && lowerInput.contains("from")) {
                return true;
            }
            
            // XSS检查
            if (lowerInput.contains("<script") ||
                lowerInput.contains("javascript:") ||
                lowerInput.contains("vbscript:") ||
                lowerInput.contains("onload=") ||
                lowerInput.contains("onerror=")) {
                return true;
            }
            
            // 路径遍历检查
            if (input.contains("../") || input.contains("..\\")) {
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * 限流信息类
     */
    public static class RateLimitInfo {
        private int requestCount = 0;
        private long lastResetTime = System.currentTimeMillis();
        
        public int getRequestCount() {
            return requestCount;
        }
        
        public void setRequestCount(int requestCount) {
            this.requestCount = requestCount;
        }
        
        public void incrementRequestCount() {
            this.requestCount++;
        }
        
        public long getLastResetTime() {
            return lastResetTime;
        }
        
        public void setLastResetTime(long lastResetTime) {
            this.lastResetTime = lastResetTime;
        }
    }
}
