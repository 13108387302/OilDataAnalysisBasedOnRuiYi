package com.ruoyi.petrol.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.petrol.domain.AnalysisTask;
import com.ruoyi.petrol.domain.PetrolDataset;
import com.ruoyi.petrol.domain.dto.AnalysisTaskUpdateDTO;
import com.ruoyi.petrol.service.IAnalysisManagerService;
import com.ruoyi.petrol.service.IAnalysisTaskService;
import com.ruoyi.petrol.service.IPetrolDatasetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.alibaba.fastjson2.JSON;
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

    @Autowired
    private IPetrolDatasetService petrolDatasetService;

    @Value("${ruoyi.profile}")
    private String uploadPath;

    /**
     * 新增分析任务 (使用数据集模式)
     */
    @PreAuthorize("@ss.hasPermi('petrol:analysis:add')")
    @Log(title = "分析任务", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public AjaxResult submitTask(@RequestParam("taskName") String taskName,
                                 @RequestParam("algorithm") String algorithm,
                                 @RequestParam("params") String params,
                                 @RequestParam("datasetId") Long datasetId,
                                 @RequestParam("datasetName") String datasetName,
                                 @RequestParam(value = "remark", required = false) String remark) throws Exception {
        // 验证基本参数
        if (taskName == null || taskName.trim().isEmpty()) {
            return AjaxResult.error("任务名称不能为空");
        }
        if (algorithm == null || algorithm.trim().isEmpty()) {
            return AjaxResult.error("算法类型不能为空");
        }

        // 验证数据源：必须提供数据集ID
        if (datasetId == null) {
            return AjaxResult.error("请选择数据集");
        }

        // 手动构建 AnalysisTask 对象
        AnalysisTask analysisTask = new AnalysisTask();
        analysisTask.setTaskName(taskName);
        analysisTask.setAlgorithm(algorithm);
        analysisTask.setInputParamsJson(params);
        analysisTask.setRemark(remark);

        // 设置数据集信息（如果使用数据集模式）
        if (datasetId != null) {
            analysisTask.setDatasetId(datasetId);
            analysisTask.setDatasetName(datasetName);
        }

        // Create a unique timestamped directory name for this task
        String taskTimestampDir = String.valueOf(System.currentTimeMillis());

        // 数据集模式 - 从数据集服务获取文件信息
        PetrolDataset dataset = petrolDatasetService.selectPetrolDatasetById(datasetId);
        if (dataset == null) {
            return error("数据集不存在");
        }

        // 设置数据集ID，让AnalysisManagerService处理文件路径和列信息
        analysisTask.setDatasetId(datasetId);
        analysisTask.setInputFilePath("dataset:" + datasetId); // 临时标识，后续会被替换

        // 如果数据集有列信息，提取列名并设置
        if (dataset.getColumnInfo() != null && !dataset.getColumnInfo().isEmpty()) {
            try {
                // 解析数据集的列信息，提取列名
                @SuppressWarnings("rawtypes")
                List<Map> columns = JSON.parseArray(dataset.getColumnInfo(), Map.class);
                List<String> columnNames = new ArrayList<>();
                for (Map column : columns) {
                    String name = (String) column.get("name");
                    if (name != null && !name.trim().isEmpty()) {
                        columnNames.add(name);
                    }
                }
                if (!columnNames.isEmpty()) {
                    analysisTask.setInputFileHeadersJson(JSON.toJSONString(columnNames));
                    log.info("从数据集提取到列名: {}", columnNames);
                }
            } catch (Exception e) {
                log.error("解析数据集列信息失败: {}", e.getMessage());
            }
        }

        // 2. Define the output directory for results
        String outputDirSubDir = "petrol" + File.separator + "results" + File.separator + algorithm + File.separator + taskTimestampDir;
        File outputDir = new File(uploadPath + File.separator + outputDirSubDir);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        analysisTask.setOutputDirPath("/profile/" + outputDirSubDir.replace(File.separator, "/"));
        analysisTask.setStatus("QUEUED"); // 设置初始状态为排队中
        analysisTask.setCreateBy(getUsername());
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
     * 获取分析任务详细信息
     */
    @PreAuthorize("@ss.hasPermi('petrol:analysis:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(analysisTaskService.selectAnalysisTaskById(id));
    }

    /**
     * 查询当前用户的分析任务列表
     */
    @PreAuthorize("@ss.hasPermi('petrol:analysis:list')")
    @GetMapping("/list")
    public TableDataInfo list(AnalysisTask analysisTask)
    {
        startPage();
        // 不再使用userId字段，改用createBy字段进行过滤
        List<AnalysisTask> list = analysisTaskService.selectAnalysisTaskList(analysisTask);
        return getDataTable(list);
    }
    
    /**
     * 删除分析任务
     */
    @PreAuthorize("@ss.hasPermi('petrol:analysis:remove')")
    @Log(title = "分析任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(analysisTaskService.deleteAnalysisTaskByIds(ids));
    }

    /**
     * 修改分析任务 (包含重新处理)
     */
    @PreAuthorize("@ss.hasPermi('petrol:analysis:edit')")
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AnalysisTaskUpdateDTO taskUpdateDTO)
    {
        // 调用我们重构后的服务层方法
        return toAjax(analysisTaskService.updateTaskParams(taskUpdateDTO));
    }
} 