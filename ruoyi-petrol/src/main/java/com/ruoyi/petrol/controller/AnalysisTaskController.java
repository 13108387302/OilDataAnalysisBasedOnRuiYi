package com.ruoyi.petrol.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.petrol.domain.AnalysisTask;
import com.ruoyi.petrol.domain.dto.AnalysisTaskDTO;
import com.ruoyi.petrol.domain.dto.AnalysisTaskUpdateDTO;
import com.ruoyi.petrol.service.IAnalysisManagerService;
import com.ruoyi.petrol.service.IAnalysisTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.petrol.domain.dto.FileAnalysisResult;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/petrol/task")
public class AnalysisTaskController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTaskController.class);

    @Autowired
    private IAnalysisTaskService analysisTaskService;

    @Autowired
    private IAnalysisManagerService analysisManagerService;
    
    @Value("${ruoyi.profile}")
    private String uploadPath;

    /**
     * 新增分析任务 (包含文件上传)
     */
    @PreAuthorize("@ss.hasPermi('petrol:task:add')")
    @Log(title = "分析任务", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public AjaxResult submitTask(@RequestPart("file") MultipartFile file,
                                 @RequestParam("taskName") String taskName,
                                 @RequestParam("algorithm") String algorithm,
                                 @RequestParam("params") String params,
                                 @RequestParam(value = "remark", required = false) String remark) throws Exception {
        if (file == null || file.isEmpty()) {
            return AjaxResult.error("上传文件不能为空，请选择文件后重试");
        }
        if (taskName == null || taskName.trim().isEmpty()) {
            return AjaxResult.error("任务名称不能为空");
        }
        if (algorithm == null || algorithm.trim().isEmpty()) {
            return AjaxResult.error("算法类型不能为空");
        }

        // 手动构建 AnalysisTask 对象
        AnalysisTask analysisTask = new AnalysisTask();
        analysisTask.setTaskName(taskName);
        analysisTask.setAlgorithm(algorithm);
        analysisTask.setInputParamsJson(params);
        analysisTask.setRemark(remark);
        
        // --- 任务处理逻辑保持不变 ---
        // Create a unique timestamped directory name for this task
        String taskTimestampDir = String.valueOf(System.currentTimeMillis());
        
        // 1. Define and create the input directory for the uploaded file
        String inputFileSubDir = "petrol" + File.separator + "uploads" + File.separator + algorithm + File.separator + taskTimestampDir;
        String absoluteInputPath = uploadPath + File.separator + inputFileSubDir;
        String uploadedFilePathUrl = FileUploadUtils.upload(absoluteInputPath, file);

        // 2. Define the output directory for results
        String outputDirSubDir = "petrol" + File.separator + "results" + File.separator + algorithm + File.separator + taskTimestampDir;
        File outputDir = new File(uploadPath + File.separator + outputDirSubDir);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // 3. 解析文件以获取头信息
        FileAnalysisResult analysisResult = analysisTaskService.analyzeFile(file);
        analysisTask.setInputFileHeadersJson(JSON.toJSONString(analysisResult.getHeaders()));

        // 4. 设置任务属性
        analysisTask.setInputFilePath(uploadedFilePathUrl.replace('\\', '/'));
        analysisTask.setOutputDirPath("/profile/" + outputDirSubDir.replace(File.separator, "/"));
        analysisTask.setStatus("QUEUED"); // 设置初始状态为排队中
        analysisTask.setUserId(getUserId());
        analysisTask.setDeptId(getDeptId());

        // 5. 将任务存入数据库
        analysisTaskService.insertAnalysisTask(analysisTask);

        // 6. 异步执行任务
        analysisManagerService.processTask(analysisTask);

        AjaxResult ajax = AjaxResult.success("任务提交成功，正在后台处理...");
        ajax.put("taskId", analysisTask.getId());
        return ajax;
    }

    /**
     * 上传并分析文件，返回文件头和统计信息 (服务于前端UI交互)
     */
    @PostMapping("/analyzeFile")
    public AjaxResult analyzeUploadedFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return AjaxResult.error("上传的文件不能为空");
        }
        try {
            FileAnalysisResult result = analysisTaskService.analyzeFile(file);
            return AjaxResult.success(result);
        } catch (Exception e) {
            logger.error("解析文件失败", e);
            return AjaxResult.error("解析文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取分析任务详细信息
     */
    @PreAuthorize("@ss.hasPermi('petrol:task:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(analysisTaskService.selectAnalysisTaskById(id));
    }

    /**
     * 查询当前用户的分析任务列表
     */
    @PreAuthorize("@ss.hasPermi('petrol:task:list')")
    @GetMapping("/list")
    public TableDataInfo list(AnalysisTask analysisTask)
    {
        startPage();
        analysisTask.setUserId(getUserId());
        List<AnalysisTask> list = analysisTaskService.selectAnalysisTaskList(analysisTask);
        return getDataTable(list);
    }
    
    /**
     * 删除分析任务
     */
    @PreAuthorize("@ss.hasPermi('petrol:task:remove')")
    @Log(title = "分析任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(analysisTaskService.deleteAnalysisTaskByIds(ids));
    }

    /**
     * 修改分析任务 (包含重新处理)
     */
    @PreAuthorize("@ss.hasPermi('petrol:task:edit')")
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AnalysisTaskUpdateDTO taskUpdateDTO)
    {
        // 调用我们重构后的服务层方法
        return toAjax(analysisTaskService.updateTaskParams(taskUpdateDTO));
    }
} 