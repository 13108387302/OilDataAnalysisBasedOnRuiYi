package com.ruoyi.petrol.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.petrol.domain.AnalysisTask;
import com.ruoyi.petrol.service.PythonExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class PythonExecutorServiceImpl implements PythonExecutorService {
    private static final Logger log = LoggerFactory.getLogger(PythonExecutorServiceImpl.class);

    @Autowired
    private RuoYiConfig ruoYiConfig;

    @Autowired
    private RestTemplate restTemplate; // 注入 RestTemplate

    @Value("${python.api.url}") // 在 application.yml 中配置 Python API 地址
    private String pythonApiUrl;

    /**
     * 将 /profile/ 开头的web相对路径，转换为绝对路径。
     */
    private String getCanonicalPath(String profilePath) throws IOException {
        if (profilePath == null || !profilePath.startsWith("/profile/")) {
            throw new IOException("无法解析路径，因为它不是一个规范的Profile路径 (必须以 /profile/ 开头): " + profilePath);
        }
        String profileRoot = ruoYiConfig.getProfile();
        String relativePath = profilePath.substring("/profile/".length());
        File finalFile = new File(profileRoot, relativePath);
        return finalFile.getCanonicalPath();
    }

    @Override
    public String executeScript(String scriptModuleName, String pythonAlgorithmName, AnalysisTask task) throws Exception {
        // 新的实现：通过 HTTP 调用 FastAPI 服务
        String endpoint = pythonApiUrl + "/api/v1/submit";
        log.info("准备调用 Python API: {}，任务ID: {}", endpoint, task.getId());

        // 1. 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. 解析原始的 JSON 参数字符串为 Map
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> inputParams = new HashMap<>();
        if (task.getInputParamsJson() != null && !task.getInputParamsJson().isEmpty()) {
            inputParams = objectMapper.readValue(task.getInputParamsJson(), Map.class);
        }

        // 3. 构建请求体 (对应 FastAPI 的 TaskSubmitRequest 模型)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", task.getId());
        requestBody.put("task_name", task.getTaskName());
        requestBody.put("task_type", task.getAlgorithm()); // 使用算法名称作为任务类型
        requestBody.put("algorithm", task.getAlgorithm()); // 使用 task 的 algorithm 字段
        requestBody.put("input_params", inputParams);

        // 恢复对getCanonicalPath的调用，以确保相对路径被正确转换为绝对路径
        requestBody.put("input_file_path", getCanonicalPath(task.getInputFilePath()));
        requestBody.put("output_dir_path", getCanonicalPath(task.getOutputDirPath()));

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 4. 发起 POST 请求
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                String responseBody = response.getBody();
                log.info("收到 Python API 响应，任务ID: {}，响应体: {}", task.getId(), responseBody);

                // 检查响应中是否包含错误信息
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonNode = mapper.readTree(responseBody);

                    // 检查是否有error字段、status为failed或success为false
                    boolean hasError = jsonNode.has("error") ||
                                     (jsonNode.has("status") && "failed".equals(jsonNode.get("status").asText())) ||
                                     (jsonNode.has("success") && !jsonNode.get("success").asBoolean());

                    if (hasError) {
                        String errorMessage = jsonNode.has("message") ?
                            jsonNode.get("message").asText() :
                            jsonNode.has("error") ? jsonNode.get("error").asText() : "Python API返回错误";

                        int errorCode = jsonNode.has("error_code") ?
                            jsonNode.get("error_code").asInt() : 500;

                        log.error("Python API 返回错误，任务ID: {}，错误码: {}，错误信息: {}",
                                task.getId(), errorCode, errorMessage);

                        // 根据错误码抛出不同的异常
                        String exceptionMessage = String.format("[错误码:%d] %s", errorCode, errorMessage);
                        throw new Exception(exceptionMessage);
                    }

                    log.info("Python API 执行成功，任务ID: {}", task.getId());
                    return responseBody;

                } catch (Exception jsonException) {
                    // 如果JSON解析失败，记录警告但仍返回原始响应
                    log.warn("解析Python API响应JSON失败，任务ID: {}，将返回原始响应: {}",
                            task.getId(), jsonException.getMessage());
                    return responseBody;
                }
            } else {
                String errorBody = response.hasBody() ? response.getBody() : "无响应体";
                log.error("调用 Python API 失败，任务ID: {}. 状态码: {}, 响应: {}", task.getId(), response.getStatusCode(), errorBody);
                // 返回一个合法的 JSON 对象来表示错误
                String safeErrorBody = errorBody != null ? errorBody.replace("\"", "'") : "无错误详情";
                return "{\"error\": \"调用 Python API 失败\", \"status_code\": " + response.getStatusCodeValue() + ", \"details\": \"" + safeErrorBody + "\"}";
            }
        } catch (Exception e) {
            log.error("调用 Python API 时发生网络或连接错误，任务ID: {}. 地址: {}", task.getId(), endpoint, e);
            // 返回一个合法的 JSON 对象来表示错误
            return "{\"error\": \"无法连接到 Python API 服务\", \"message\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    @Override
    public Map<String, Object> predict(Map<String, Object> request) throws Exception {
        String endpoint = pythonApiUrl + "/api/v1/predict";
        log.info("准备调用 Python 预测 API: {}", endpoint);

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);

        try {
            // 发起 POST 请求
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                Map<String, Object> responseBody = response.getBody();
                log.info("成功调用 Python 预测 API，响应: {}", responseBody);
                return responseBody;
            } else {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "调用 Python 预测 API 失败，状态码: " + response.getStatusCode());
                return errorResult;
            }
        } catch (Exception e) {
            log.error("调用 Python 预测 API 时发生错误", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "无法连接到 Python 预测 API 服务: " + e.getMessage());
            return errorResult;
        }
    }

    @Override
    public Map<String, Object> batchPredict(Map<String, Object> request, MultipartFile file) throws Exception {
        String endpoint = pythonApiUrl + "/api/v1/batch-predict";
        log.info("准备调用 Python 批量预测 API: {}", endpoint);

        // 构建多部分请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // 添加文件
        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        body.add("file", fileResource);

        // 添加其他参数
        ObjectMapper objectMapper = new ObjectMapper();
        for (Map.Entry<String, Object> entry : request.entrySet()) {
            if (entry.getValue() != null) {
                body.add(entry.getKey(), entry.getValue().toString());
            }
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            // 发起 POST 请求
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                Map<String, Object> responseBody = response.getBody();
                log.info("成功调用 Python 批量预测 API，响应: {}", responseBody);
                return responseBody;
            } else {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "调用 Python 批量预测 API 失败，状态码: " + response.getStatusCode());
                return errorResult;
            }
        } catch (Exception e) {
            log.error("调用 Python 批量预测 API 时发生错误", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "无法连接到 Python 批量预测 API 服务: " + e.getMessage());
            return errorResult;
        }
    }
}