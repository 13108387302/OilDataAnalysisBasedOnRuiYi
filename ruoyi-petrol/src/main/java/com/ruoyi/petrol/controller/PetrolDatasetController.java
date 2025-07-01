package com.ruoyi.petrol.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.petrol.domain.PetrolDataset;
import com.ruoyi.petrol.service.IPetrolDatasetService;


/**
 * 数据集管理Controller
 * 
 * @author ruoyi
 * @date 2025-06-29
 */
@RestController
@RequestMapping("/petrol/dataset")
public class PetrolDatasetController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(PetrolDatasetController.class);

    @Autowired
    private IPetrolDatasetService petrolDatasetService;



    @Autowired
    private CacheManager cacheManager;

    /**
     * 清除数据集缓存
     */
    @PostMapping("/clearCache")
    public AjaxResult clearCache()
    {
        try {
            // 清理Spring Cache Manager缓存
            if (cacheManager.getCache("datasetList") != null) {
                cacheManager.getCache("datasetList").clear();
            }
            if (cacheManager.getCache("dataset") != null) {
                cacheManager.getCache("dataset").clear();
            }

            // 自定义缓存服务已移除，使用Spring Cache Manager统一管理

            log.info("✅ 数据集缓存已清除");
            return success("缓存清除成功");
        } catch (Exception e) {
            log.error("❌ 清除缓存失败", e);
            return error("清除缓存失败: " + e.getMessage());
        }
    }

    /**
     * 完整数据清理（清理数据库+缓存+文件）
     */
    @PreAuthorize("@ss.hasPermi('petrol:dataset:remove')")
    @DeleteMapping("/completeCleanup")
    public AjaxResult completeCleanup()
    {
        try {
            log.info("🔄 开始执行完整数据清理...");

            // 1. 清理缓存
            clearCache();

            // 2. 清理数据库中的数据集记录
            int deletedCount = petrolDatasetService.deleteAllDatasets();

            log.info("✅ 完整数据清理完成，删除了 {} 个数据集记录", deletedCount);
            return success("完整数据清理成功，删除了 " + deletedCount + " 个数据集记录。请手动清理文件系统中的数据文件。");
        } catch (Exception e) {
            log.error("❌ 完整数据清理失败", e);
            return error("完整数据清理失败: " + e.getMessage());
        }
    }

    /**
     * 查询数据集管理列表
     */
    @PreAuthorize("@ss.hasPermi('petrol:dataset:list')")
    @GetMapping("/list")
    public TableDataInfo list(PetrolDataset petrolDataset)
    {
        startPage();
        List<PetrolDataset> list = petrolDatasetService.selectPetrolDatasetList(petrolDataset);
        return getDataTable(list);
    }

    /**
     * 查询可用的数据集列表
     */
    @PreAuthorize("@ss.hasPermi('petrol:dataset:list')")
    @GetMapping("/available")
    public AjaxResult getAvailableDatasets()
    {
        log.info("🔍 开始查询可用数据集列表");

        try {
            List<PetrolDataset> list = petrolDatasetService.selectAvailableDatasets();
            log.info("📊 查询结果 - 数据集数量: {}", list.size());

            if (!list.isEmpty()) {
                log.info("📈 数据集详情:");
                for (PetrolDataset dataset : list) {
                    log.info("  - ID: {}, 名称: {}, 状态: {}, 类别: {}, 创建时间: {}",
                        dataset.getId(),
                        dataset.getDatasetName(),
                        dataset.getStatus(),
                        dataset.getDatasetCategory(),
                        dataset.getCreateTime());
                }
                log.info("数据集ID列表: {}", list.stream().map(PetrolDataset::getId).collect(Collectors.toList()));
            } else {
                log.warn("⚠️ 没有找到可用的数据集，检查数据库中的数据集状态");

                // 调试：查询所有数据集
                try {
                    List<PetrolDataset> allDatasets = petrolDatasetService.selectPetrolDatasetList(new PetrolDataset());
                    log.info("🔍 数据库中所有数据集数量: {}", allDatasets.size());
                    for (PetrolDataset dataset : allDatasets) {
                        log.info("  - 所有数据集 - ID: {}, 名称: {}, 状态: {}",
                            dataset.getId(), dataset.getDatasetName(), dataset.getStatus());
                    }
                } catch (Exception debugError) {
                    log.error("查询所有数据集失败", debugError);
                }
            }

            return success(list);
        } catch (Exception e) {
            log.error("❌ 查询数据集列表失败", e);
            return error("查询数据集列表失败: " + e.getMessage());
        }
    }



    /**
     * 根据类别查询数据集列表
     */
    @GetMapping("/category/{category}")
    public AjaxResult getDatasetsByCategory(@PathVariable String category)
    {
        List<PetrolDataset> list = petrolDatasetService.selectDatasetsByCategory(category);
        return success(list);
    }

    /**
     * 导出数据集管理列表
     */
    @PreAuthorize("@ss.hasPermi('petrol:dataset:export')")
    @Log(title = "数据集管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PetrolDataset petrolDataset)
    {
        List<PetrolDataset> list = petrolDatasetService.selectPetrolDatasetList(petrolDataset);
        ExcelUtil<PetrolDataset> util = new ExcelUtil<PetrolDataset>(PetrolDataset.class);
        util.exportExcel(response, list, "数据集管理数据");
    }

    /**
     * 获取数据集管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('petrol:dataset:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(petrolDatasetService.selectPetrolDatasetById(id));
    }

    /**
     * 获取数据集预览数据
     */
    @GetMapping("/{id}/preview")
    public AjaxResult getPreview(@PathVariable("id") Long id, 
                                @RequestParam(defaultValue = "10") Integer rows)
    {
        Map<String, Object> result = petrolDatasetService.getDatasetPreview(id, rows);
        if ((Boolean) result.get("success")) {
            return success(result);
        } else {
            return error((String) result.get("message"));
        }
    }

    /**
     * 获取数据集列信息
     */
    @GetMapping("/{id}/columns")
    public AjaxResult getColumns(@PathVariable("id") Long id)
    {
        try {
            log.info("获取数据集列信息，ID: {}", id);
            PetrolDataset dataset = petrolDatasetService.selectPetrolDatasetById(id);
            if (dataset == null) {
                log.warn("数据集不存在，ID: {}", id);
                return error("数据集不存在");
            }
            log.info("找到数据集: {}", dataset.getDatasetName());

            List<Map<String, Object>> columns = petrolDatasetService.getDatasetColumns(id);

            Map<String, Object> result = new HashMap<>();
            result.put("columns", columns.stream().map(col -> col.get("name")).collect(Collectors.toList()));
            result.put("columnDetails", columns);
            result.put("stats", new HashMap<>()); // 暂时返回空的stats
            result.put("numericColumns", columns.stream()
                .filter(col -> "numeric".equals(col.get("type")) || "integer".equals(col.get("type")) || "float".equals(col.get("type")))
                .map(col -> col.get("name"))
                .collect(Collectors.toList()));

            return success(result);
        } catch (Exception e) {
            log.error("获取数据集列信息失败", e);
            return error("获取列信息失败: " + e.getMessage());
        }
    }

    /**
     * 检查数据集使用情况
     */
    @GetMapping("/{id}/usage")
    public AjaxResult checkUsage(@PathVariable("id") Long id)
    {
        Map<String, Object> result = petrolDatasetService.checkDatasetUsage(id);
        return success(result);
    }

    /**
     * 上传数据集文件
     */
    @PreAuthorize("@ss.hasPermi('petrol:dataset:add')")
    @Log(title = "数据集管理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                           @RequestParam("datasetName") String datasetName,
                           @RequestParam(value = "description", required = false) String description,
                           @RequestParam(value = "category", defaultValue = "测井") String category)
    {
        Map<String, Object> result = petrolDatasetService.uploadDataset(file, datasetName, description, category);
        if ((Boolean) result.get("success")) {
            return AjaxResult.success((String) result.get("message"), result.get("dataset"));
        } else {
            return error((String) result.get("message"));
        }
    }

    /**
     * 验证数据集文件
     */
    @PostMapping("/validate")
    public AjaxResult validateFile(@RequestParam("file") MultipartFile file)
    {
        Map<String, Object> result = petrolDatasetService.validateDatasetFile(file);
        return success(result);
    }

    /**
     * 分片上传文件
     */
    @PostMapping("/upload-chunk")
    public AjaxResult uploadChunk(@RequestParam("chunk") MultipartFile chunk,
                                @RequestParam("chunkIndex") Integer chunkIndex,
                                @RequestParam("chunkHash") String chunkHash,
                                @RequestParam("fileHash") String fileHash,
                                @RequestParam("fileName") String fileName,
                                @RequestParam("totalChunks") Integer totalChunks)
    {
        try {
            Map<String, Object> result = petrolDatasetService.uploadChunk(
                chunk, chunkIndex, chunkHash, fileHash, fileName, totalChunks
            );

            if ((Boolean) result.get("success")) {
                return AjaxResult.success((String) result.get("message"), result.get("data"));
            } else {
                return error((String) result.get("message"));
            }
        } catch (Exception e) {
            logger.error("分片上传失败", e);
            return error("分片上传失败: " + e.getMessage());
        }
    }

    /**
     * 检查已上传的分片
     */
    @PostMapping("/check-chunks")
    public AjaxResult checkChunks(@RequestParam("fileHash") String fileHash,
                                @RequestParam("fileName") String fileName,
                                @RequestParam("totalChunks") Integer totalChunks)
    {
        try {
            Map<String, Object> result = petrolDatasetService.checkUploadedChunks(fileHash, fileName, totalChunks);
            return AjaxResult.success("检查完成", result);
        } catch (Exception e) {
            logger.error("检查分片失败", e);
            return error("检查分片失败: " + e.getMessage());
        }
    }

    /**
     * 合并分片文件
     */
    @PostMapping("/merge-chunks")
    public AjaxResult mergeChunks(@RequestParam("fileHash") String fileHash,
                                @RequestParam("fileName") String fileName,
                                @RequestParam("totalChunks") Integer totalChunks,
                                @RequestParam("fileSize") Long fileSize,
                                @RequestParam("datasetName") String datasetName,
                                @RequestParam(value = "description", required = false) String description,
                                @RequestParam(value = "category", defaultValue = "测井") String category)
    {
        try {
            Map<String, Object> result = petrolDatasetService.mergeChunksAndCreateDataset(
                fileHash, fileName, totalChunks, fileSize, datasetName, description, category
            );

            if ((Boolean) result.get("success")) {
                return AjaxResult.success((String) result.get("message"), result.get("dataset"));
            } else {
                return error((String) result.get("message"));
            }
        } catch (Exception e) {
            logger.error("合并分片失败", e);
            return error("合并分片失败: " + e.getMessage());
        }
    }

    /**
     * 新增数据集管理
     */
    @PreAuthorize("@ss.hasPermi('petrol:dataset:add')")
    @Log(title = "数据集管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PetrolDataset petrolDataset)
    {
        return toAjax(petrolDatasetService.insertPetrolDataset(petrolDataset));
    }

    /**
     * 修改数据集管理
     */
    @PreAuthorize("@ss.hasPermi('petrol:dataset:edit')")
    @Log(title = "数据集管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PetrolDataset petrolDataset)
    {
        return toAjax(petrolDatasetService.updatePetrolDataset(petrolDataset));
    }

    /**
     * 更新数据集统计信息
     */
    @PreAuthorize("@ss.hasPermi('petrol:dataset:edit')")
    @Log(title = "数据集管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/stats")
    public AjaxResult updateStats(@PathVariable("id") Long id)
    {
        int result = petrolDatasetService.updateDatasetStats(id);
        return toAjax(result);
    }

    /**
     * 删除数据集管理
     */
    @PreAuthorize("@ss.hasPermi('petrol:dataset:remove')")
    @Log(title = "数据集管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        try {
            return toAjax(petrolDatasetService.deletePetrolDatasetByIds(ids));
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }
}
