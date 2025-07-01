<template>
  <div class="app-container">
    <!-- 任务表单 -->
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>{{ form.id ? '修改分析任务' : '创建新的分析任务' }}</span>
      </div>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务类型" prop="taskType">
          <el-select v-model="form.taskType" placeholder="请选择任务类型" @change="handleTaskTypeChange" style="width: 100%;">
            <el-option v-for="item in taskTypes" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="算法选择" prop="algorithm">
          <el-select v-model="form.algorithm" placeholder="请先选择任务类型" @change="handleAlgorithmChange" style="width: 100%;">
            <el-option v-for="item in algorithmOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>

        <!-- 数据集选择 -->
        <el-form-item v-if="!form.id" label="选择数据集" prop="datasetId">
          <el-row :gutter="10">
            <el-col :span="16">
              <el-select
                v-model="form.datasetId"
                placeholder="请选择数据集"
                filterable
                style="width: 100%;"
                @change="handleDatasetChange">
                <el-option
                  v-for="dataset in availableDatasets"
                  :key="dataset.id"
                  :label="dataset.datasetName"
                  :value="dataset.id">
                  <div style="display: flex; justify-content: space-between; align-items: center;">
                    <span>{{ dataset.datasetName }}</span>
                    <div style="font-size: 12px; color: #8492a6;">
                      <el-tag size="mini" :type="getDatasetCategoryType(dataset.datasetCategory)">
                        {{ dataset.datasetCategory }}
                      </el-tag>
                      <span style="margin-left: 5px;">{{ dataset.rowCount }}行</span>
                    </div>
                  </div>
                  <div style="font-size: 11px; color: #909399; margin-top: 2px;">
                    {{ dataset.datasetDescription || '暂无描述' }}
                  </div>
                </el-option>
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-button type="primary" icon="el-icon-view" @click="previewSelectedDataset" :disabled="!form.datasetId">
                预览数据集
              </el-button>
              <el-button type="info" icon="el-icon-refresh" @click="loadAvailableDatasets" size="small" style="margin-left: 5px;">
                刷新
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>

        <!-- 参数模板选择 -->
        <el-form-item label="参数模板" v-if="hasParameterTemplates">
          <el-row :gutter="10">
            <el-col :span="16">
              <el-select v-model="selectedTemplate" placeholder="选择参数模板" style="width: 100%;" @change="handleTemplateChange">
                <el-option
                  v-for="template in availableTemplates"
                  :key="template.key"
                  :label="template.label"
                  :value="template.key">
                  <span style="float: left">{{ template.label }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">{{ template.description }}</span>
                </el-option>
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-button type="primary" icon="el-icon-magic-stick" @click="applyRecommendedParameters" :disabled="!canRecommend">
                使用推荐参数
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>

        <!-- 参数预览卡片 -->
        <el-card v-if="showParameterPreview" class="parameter-preview-card" shadow="never">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-view"></i> 参数配置预览</span>
            <el-button style="float: right; padding: 3px 0" type="text" @click="showParameterPreview = false">收起</el-button>
          </div>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item v-for="(value, key) in form.parameters" :key="key" :label="getParameterDisplayName(key)">
              <el-tag size="mini" :type="getParameterValueType(key, value)">{{ formatParameterValue(value) }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 增强的动态参数表单 (高级模式) -->
        <div v-if="parameterMode === 'advanced'" v-for="param in currentAlgorithmParams" :key="param.key" class="enhanced-param-item">
            <el-form-item :label="param.label" :prop="'parameters.' + param.key" :rules="getFormRules(param)">
              <!-- 参数说明提示 -->
              <div class="param-description" v-if="getParameterInfo(param.key).description">
                <el-tooltip :content="getParameterInfo(param.key).tips" placement="top">
                  <i class="el-icon-question param-help-icon"></i>
                </el-tooltip>
                <span class="param-desc-text">{{ getParameterInfo(param.key).description }}</span>
              </div>

              <!-- 列选择器 -->
              <el-select v-if="param.type === 'select_column'"
                         v-model="form.parameters[param.key]"
                         :placeholder="'请选择' + param.label"
                         filterable
                         style="width: 100%;"
                         @change="onParameterChange(param.key, $event)">
                <el-option v-for="col in availableColumns" :key="col" :label="col" :value="col">
                  <span style="float: left">{{ col }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px" v-if="columnStats[col]">
                    {{ getColumnStatsText(col) }}
                  </span>
                </el-option>
              </el-select>

              <!-- 多列选择器 -->
              <el-select v-else-if="param.type === 'multi_select_column'"
                         v-model="form.parameters[param.key]"
                         :placeholder="'请选择' + param.label"
                         filterable
                         multiple
                         collapse-tags
                         style="width: 100%;">
                <el-option v-for="col in availableColumns" :key="col" :label="col" :value="col">
                  <span style="float: left">{{ col }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px" v-if="columnStats[col]">
                    {{ getColumnStatsText(col) }}
                  </span>
                </el-option>
              </el-select>

              <!-- 选项选择器 -->
              <el-select v-else-if="param.type === 'select_option'"
                         v-model="form.parameters[param.key]"
                         :placeholder="'请选择' + param.label"
                         style="width: 100%;">
                <el-option v-for="opt in getParameterOptions(param)" :key="opt.value || opt" :label="opt.label || opt" :value="opt.value || opt">
                  <span style="float: left">{{ opt.label || opt }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px" v-if="opt.description">
                    {{ opt.description }}
                  </span>
                </el-option>
              </el-select>

              <!-- 数字输入器 -->
              <div v-else-if="param.type === 'number'" class="number-input-container">
                <el-input-number v-model="form.parameters[param.key]"
                                 :min="param.min"
                                 :max="param.max"
                                 :step="param.step || 1"
                                 :placeholder="param.placeholder || ('请输入' + param.label)"
                                 controls-position="right"
                                 style="width: 100%;">
                </el-input-number>
                <div class="param-range-hint" v-if="getParameterInfo(param.key).ranges">
                  推荐值: {{ getParameterInfo(param.key).ranges.recommended.join(', ') }}
                </div>
              </div>

              <!-- 文本输入器 -->
              <el-input v-else
                        v-model="form.parameters[param.key]"
                        :placeholder="'请输入' + param.label" />

              <!-- 参数验证状态 -->
              <div class="param-validation" v-if="getParameterValidation(param.key)">
                <div class="validation-message">
                  <i :class="getParameterValidation(param.key).icon" :style="{color: getParameterValidation(param.key).color}"></i>
                  <span :style="{color: getParameterValidation(param.key).color}">{{ getParameterValidation(param.key).message }}</span>
                </div>
                <!-- 显示建议 -->
                <div v-if="getParameterValidation(param.key).suggestions && getParameterValidation(param.key).suggestions.length > 0"
                     class="validation-suggestions">
                  <el-tooltip placement="top" :content="getParameterValidation(param.key).suggestions.join('; ')">
                    <el-button type="text" size="mini" icon="el-icon-info">查看建议</el-button>
                  </el-tooltip>
                </div>
              </div>
            </el-form-item>
        </div>

        <!-- 参数验证汇总 -->
        <el-form-item v-if="validationSummary.errorCount > 0 || validationSummary.warningCount > 0">
          <el-alert
            :title="`参数验证: ${validationSummary.errorCount} 个错误, ${validationSummary.warningCount} 个警告`"
            :type="validationSummary.errorCount > 0 ? 'error' : 'warning'"
            :closable="false"
            show-icon>
            <div slot="description">
              <div v-if="validationSummary.errors.length > 0">
                <strong>错误:</strong>
                <ul style="margin: 5px 0; padding-left: 20px;">
                  <li v-for="error in validationSummary.errors" :key="error">{{ error }}</li>
                </ul>
              </div>
              <div v-if="validationSummary.warnings.length > 0">
                <strong>警告:</strong>
                <ul style="margin: 5px 0; padding-left: 20px;">
                  <li v-for="warning in validationSummary.warnings" :key="warning">{{ warning }}</li>
                </ul>
              </div>
            </div>
          </el-alert>
        </el-form-item>

        <!-- 参数配置模式选择 -->
        <el-form-item>
          <div class="parameter-mode-selector">
            <el-radio-group v-model="parameterMode" @change="handleParameterModeChange" size="small">
              <el-radio-button label="simple">
                <i class="el-icon-magic-stick"></i> 简化配置
              </el-radio-button>
              <el-radio-button label="advanced">
                <i class="el-icon-s-operation"></i> 高级配置
              </el-radio-button>
            </el-radio-group>
            <span class="mode-description">
              {{ parameterMode === 'simple' ? '使用预设参数，快速开始' : '自定义所有参数，精细调优' }}
            </span>
          </div>
        </el-form-item>

        <!-- 简化参数选择器 -->
        <div v-if="parameterMode === 'simple'" class="simplified-parameter-section">
          <SimplifiedParameterSelector
            v-model="form.parameters"
            :algorithm="form.algorithm"
            :task-type="form.taskType"
            :algorithm-params="currentAlgorithmParams"
            :available-columns="availableColumns"
            :data-info="getDataInfo()"
            @change="handleSimplifiedParameterChange"
            @apply="handleParameterApply">
          </SimplifiedParameterSelector>
        </div>

        <!-- 高级参数配置 -->
        <div v-else class="advanced-parameter-section">
          <!-- 参数配置操作按钮 -->
          <el-form-item>
            <el-row :gutter="10">
              <el-col :span="6">
                <el-button type="info" icon="el-icon-view" @click="toggleParameterPreview" size="small">
                  {{ showParameterPreview ? '隐藏预览' : '预览配置' }}
                </el-button>
              </el-col>
              <el-col :span="6">
                <el-button type="primary" icon="el-icon-check" @click="validateParametersInternal" size="small" :loading="isValidating">
                  验证参数
                </el-button>
              </el-col>
              <el-col :span="6">
                <el-button type="warning" icon="el-icon-refresh-left" @click="resetParameters" size="small">
                  重置参数
                </el-button>
              </el-col>
              <el-col :span="6">
                <el-button type="success" icon="el-icon-download" @click="saveParameterTemplate" size="small">
                  保存模板
                </el-button>
              </el-col>
            </el-row>
          </el-form-item>
        </div>
        
        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">{{ form.id ? '确认修改' : '立即创建' }}</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 任务统计 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.total }}</div>
            <div class="stat-label">总任务数</div>
          </div>
          <i class="el-icon-s-data stat-icon"></i>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card running">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.running }}</div>
            <div class="stat-label">运行中</div>
          </div>
          <i class="el-icon-loading stat-icon"></i>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card completed">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.completed }}</div>
            <div class="stat-label">已完成</div>
          </div>
          <i class="el-icon-circle-check stat-icon"></i>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card failed">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.failed }}</div>
            <div class="stat-label">失败</div>
          </div>
          <i class="el-icon-circle-close stat-icon"></i>
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史任务列表 -->
    <el-card class="box-card" style="margin-top: 20px;">
       <div slot="header" class="clearfix">
          <span>历史任务列表</span>
          <div style="float: right;">
            <el-tooltip :content="autoRefreshEnabled ? '点击关闭自动刷新' : '点击开启自动刷新'" placement="top">
              <el-button
                size="mini"
                :type="autoRefreshEnabled ? 'success' : 'info'"
                :icon="autoRefreshEnabled ? 'el-icon-video-play' : 'el-icon-video-pause'"
                @click="toggleAutoRefresh"
                circle>
              </el-button>
            </el-tooltip>
            <el-button size="mini" type="primary" icon="el-icon-refresh" @click="getList" circle></el-button>
            <span style="margin-left: 10px; font-size: 12px; color: #909399;">
              {{ autoRefreshEnabled ? '自动刷新: 开启' : '自动刷新: 关闭' }}
            </span>
          </div>
        </div>
      <el-table v-loading="loading" :data="taskList" :row-key="row => row.id">
        <el-table-column label="任务ID" align="center" prop="id" />
        <el-table-column label="任务名称" align="center" prop="taskName" />
        <el-table-column label="算法类型" align="center">
          <template slot-scope="scope">
            <el-tag :type="getAlgorithmTagType(scope.row.algorithm)">
              {{ getAlgorithmDisplayName(scope.row.algorithm) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="120">
          <template slot-scope="scope">
            <div style="display: flex; align-items: center; justify-content: center;">
              <i v-if="scope.row.status === 'RUNNING'" class="el-icon-loading" style="margin-right: 5px; color: #E6A23C;"></i>
              <i v-else-if="scope.row.status === 'PENDING' || scope.row.status === 'QUEUED'" class="el-icon-time" style="margin-right: 5px; color: #909399;"></i>
              <el-tag :type="getStatusTagType(scope.row.status)" size="small">
                {{ getStatusDisplayName(scope.row.status) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" width="160">
          <template slot-scope="scope">
            <span>{{ formatDateTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
       <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="300">
         <template slot-scope="scope">
           <el-button
             size="mini"
             type="text"
             icon="el-icon-view"
             @click="handleView(scope.row)"
             :loading="scope.row.status === 'RUNNING'">
             {{ scope.row.status === 'RUNNING' ? '实时查看' : '查看结果' }}
           </el-button>
           <el-button
             size="mini"
             type="text"
             icon="el-icon-edit"
             @click="handleUpdate(scope.row)"
             v-hasPermi="['petrol:task:edit']"
             :disabled="scope.row.status === 'RUNNING' || scope.row.status === 'PENDING'">
             修改参数
           </el-button>
           <el-button
             size="mini"
             type="text"
             icon="el-icon-delete"
             @click="handleDelete(scope.row)"
             v-hasPermi="['petrol:task:remove']"
             :disabled="scope.row.status === 'RUNNING'">
             删除
           </el-button>
         </template>
       </el-table-column>
     </el-table>
     <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    </el-card>

    <!-- 结果展示对话框 -->
    <el-dialog :title="resultsDialog.title" :visible.sync="resultsDialog.visible" width="80%" append-to-body @close="handleDialogClose">
      <div v-if="resultsDialog.data || (resultsDialog.task && resultsDialog.task.inputParamsJson)">
        <!-- 任务基本信息 -->
        <el-card class="box-card" v-if="resultsDialog.task">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-info"></i> 任务信息</span>
          </div>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="任务ID">{{ resultsDialog.task.id }}</el-descriptions-item>
            <el-descriptions-item label="任务名称">{{ resultsDialog.task.taskName }}</el-descriptions-item>
            <el-descriptions-item label="算法类型">
              <el-tag :type="getAlgorithmTagType(resultsDialog.task.algorithm)">
                {{ getAlgorithmDisplayName(resultsDialog.task.algorithm) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="任务状态">
              <el-tag :type="getStatusTagType(resultsDialog.task.status)">
                {{ getStatusDisplayName(resultsDialog.task.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ resultsDialog.task.createTime }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ resultsDialog.task.updateTime }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

         <!-- 参数展示 -->
        <el-card class="box-card" v-if="resultsDialog.task && resultsDialog.task.inputParamsJson">
            <div slot="header" class="clearfix">
                <span><i class="el-icon-setting"></i> 运行参数</span>
            </div>
            <el-descriptions :column="2" border>
                <el-descriptions-item v-for="(value, key) in parsedTaskParams" :key="key">
                    <template slot="label">{{ getParameterDisplayName(key) }}</template>
                    {{ value }}
                </el-descriptions-item>
            </el-descriptions>
        </el-card>

        <!-- 原始JSON展示 -->
        <div v-if="resultsDialog.data && resultsDialog.data.raw" style="margin-top: 20px;">
          <p>任务结果为非标准JSON，显示原始文本：</p>
          <pre style="background-color: #f5f5f5; padding: 10px; border-radius: 4px;">{{ resultsDialog.data.raw }}</pre>
        </div>
        <!-- 格式化展示 -->
        <div v-else-if="resultsDialog.data" style="margin-top: 20px;">
            <!-- 核心指标 - 使用统计卡片样式 -->
            <el-card class="box-card" v-if="resultsDialog.data.metrics && Object.keys(resultsDialog.data.metrics).length > 0">
              <div slot="header" class="clearfix">
                <span><i class="el-icon-s-data"></i> 模型评估指标</span>
              </div>
              <el-row :gutter="20">
                <el-col :span="8" v-for="(value, key) in resultsDialog.data.metrics" :key="key">
                  <div class="metric-card">
                    <div class="metric-label">{{ getMetricDisplayName(key) }}</div>
                    <div class="metric-value" :class="getMetricValueClass(key, value)">
                      {{ formatMetricValue(key, value) }}
                    </div>
                    <div class="metric-description">{{ getMetricDescription(key) }}</div>
                  </div>
                </el-col>
              </el-row>
            </el-card>
            <el-card class="box-card" style="margin-top: 20px;" v-if="resultsDialog.data.visualizations && Object.keys(resultsDialog.data.visualizations).length > 0">
              <div slot="header" class="clearfix">
                <span><i class="el-icon-picture-outline"></i> 可视化图表</span>
              </div>
              <div v-for="(path, key) in resultsDialog.data.visualizations" :key="key" style="margin-bottom: 15px; text-align: center;">
                <p style="font-weight: bold; margin-bottom: 10px;">{{ key }}</p>
                <el-image :src="getImageUrl(path)" :preview-src-list="[getImageUrl(path)]" fit="contain" style="max-width: 100%; height: auto;">
                  <div slot="error" class="image-slot"><i class="el-icon-picture-outline"></i></div>
                  <div slot="placeholder" class="image-slot">加载中<span class="dot">...</span></div>
                </el-image>
              </div>
            </el-card>

            <!-- 下载区域 -->
            <el-card class="box-card" style="margin-top: 20px;" v-if="(resultsDialog.data.model_artifact && Object.keys(resultsDialog.data.model_artifact).length > 0) || (resultsDialog.data.excel_report && Object.keys(resultsDialog.data.excel_report).length > 0)">
              <div slot="header" class="clearfix">
                <span><i class="el-icon-download"></i> 文件下载</span>
              </div>
              <div class="download-links">
                <div v-for="(path, key) in resultsDialog.data.model_artifact" :key="'model_'+key">
                   <el-button type="primary" icon="el-icon-download" @click="handleDownload(path)">下载模型 ({{ key }})</el-button>
                </div>
                <div v-for="(path, key) in resultsDialog.data.excel_report" :key="'excel_'+key" style="margin-top: 10px;">
                   <el-button type="success" icon="el-icon-document" @click="handleDownload(path)">下载Excel报告 ({{ key }})</el-button>
                </div>
              </div>
            </el-card>
        </div>
      </div>
      <!-- 错误信息展示 -->
      <div v-else-if="resultsDialog.isError">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-warning-outline"></i> 任务执行失败</span>
          </div>
          <pre style="background-color: #fef0f0; color: #f56c6c; padding: 15px; border-radius: 4px; white-space: pre-wrap; word-wrap: break-word;">{{ resultsDialog.errorMessage }}</pre>
        </el-card>
      </div>
      <!-- 加载中提示 -->
      <div v-else>
        <div style="text-align: center; padding: 40px;">
          <i class="el-icon-loading" style="font-size: 50px;"></i>
          <p>正在努力加载数据，请稍候...</p>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="resultsDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 增强版数据集预览对话框 -->
    <el-dialog
      title="数据集预览"
      :visible.sync="datasetPreviewVisible"
      width="90%"
      append-to-body
      :close-on-click-modal="false">
      <div v-if="selectedDataset" v-loading="previewLoading">
        <!-- 标签页切换 -->
        <el-tabs v-model="previewActiveTab" type="border-card">
          <!-- 基本信息标签页 -->
          <el-tab-pane label="基本信息" name="info">
            <el-card class="box-card">
              <el-descriptions :column="3" border>
                <el-descriptions-item label="数据集名称">{{ selectedDataset.datasetName }}</el-descriptions-item>
                <el-descriptions-item label="数据集类别">
                  <el-tag :type="getDatasetCategoryType(selectedDataset.datasetCategory)">
                    {{ selectedDataset.datasetCategory }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="数据行数">{{ formatNumber(selectedDataset.rowCount) }}</el-descriptions-item>
                <el-descriptions-item label="数据列数">{{ selectedDataset.columnCount }}</el-descriptions-item>
                <el-descriptions-item label="文件大小">{{ formatFileSize(selectedDataset.fileSize) }}</el-descriptions-item>
                <el-descriptions-item label="数据质量">
                  <el-progress
                    :percentage="selectedDataset.dataQualityScore"
                    :color="getQualityColor(selectedDataset.dataQualityScore)"
                    :show-text="false"
                    :stroke-width="6"
                    style="width: 100px; display: inline-block;"
                  ></el-progress>
                  <span style="margin-left: 10px;">{{ selectedDataset.dataQualityScore }}%</span>
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ selectedDataset.createTime }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ selectedDataset.updateTime }}</el-descriptions-item>
                <el-descriptions-item label="描述" :span="3">
                  {{ selectedDataset.datasetDescription || '暂无描述' }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-tab-pane>

          <!-- 数据预览标签页 -->
          <el-tab-pane label="数据预览" name="data">
            <div class="data-preview-controls" style="margin-bottom: 15px;">
              <el-row :gutter="10">
                <el-col :span="8">
                  <el-input-number
                    v-model="previewRows"
                    :min="5"
                    :max="100"
                    :step="5"
                    size="small"
                    @change="loadPreviewData">
                  </el-input-number>
                  <span style="margin-left: 10px;">行数据</span>
                </el-col>
                <el-col :span="8">
                  <el-button type="primary" size="small" icon="el-icon-refresh" @click="loadPreviewData">
                    刷新数据
                  </el-button>
                </el-col>
                <el-col :span="8" style="text-align: right;">
                  <span style="color: #666; font-size: 12px;">
                    显示前 {{ Math.min(previewRows, selectedDataset.rowCount) }} 行数据
                  </span>
                </el-col>
              </el-row>
            </div>

            <!-- 数据表格 -->
            <el-table
              :data="previewData"
              border
              stripe
              size="small"
              max-height="400"
              style="width: 100%"
              v-if="previewData.length > 0">
              <el-table-column
                label="行号"
                type="index"
                width="60"
                align="center"
                :index="(index) => index + 1">
              </el-table-column>
              <el-table-column
                v-for="column in availableColumns"
                :key="column"
                :prop="column"
                :label="column"
                :width="getColumnDisplayWidth(column)"
                show-overflow-tooltip>
                <template slot-scope="scope">
                  <span :class="getCellDisplayClass(scope.row[column], column)">
                    {{ formatCellDisplayValue(scope.row[column]) }}
                  </span>
                </template>
              </el-table-column>
            </el-table>

            <!-- 无数据提示 -->
            <el-empty
              v-if="!previewLoading && previewData.length === 0"
              description="暂无预览数据"
              :image-size="100">
            </el-empty>
          </el-tab-pane>

          <!-- 列信息标签页 -->
          <el-tab-pane label="列信息" name="columns">
            <div class="column-filter" style="margin-bottom: 15px;">
              <el-input
                v-model="columnSearchText"
                placeholder="搜索列名"
                size="small"
                style="width: 200px;"
                prefix-icon="el-icon-search"
                clearable>
              </el-input>
            </div>

            <el-table :data="getFilteredColumnTableData()" size="small" max-height="400">
              <el-table-column label="列名" prop="name" width="150" fixed="left">
                <template slot-scope="scope">
                  <el-tag size="mini" effect="plain">{{ scope.row.name }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="数据类型" prop="type" width="100">
                <template slot-scope="scope">
                  <el-tag size="mini" :type="getColumnTypeTag(scope.row.type)">
                    {{ getColumnTypeDisplay(scope.row.type) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="非空值" prop="nonNull" width="80" align="center"></el-table-column>
              <el-table-column label="唯一值" prop="unique" width="80" align="center"></el-table-column>
              <el-table-column label="缺失率" width="100" align="center">
                <template slot-scope="scope">
                  <span :class="getMissingRateClass(scope.row.missingRate)">
                    {{ formatPercentage(scope.row.missingRate) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="统计信息" prop="stats" min-width="250">
                <template slot-scope="scope">
                  <div v-if="scope.row.stats" class="stats-display">
                    {{ scope.row.stats }}
                  </div>
                  <span v-else style="color: #909399;">-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 数据质量标签页 -->
          <el-tab-pane label="数据质量" name="quality">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-card class="quality-card">
                  <div slot="header">
                    <span><i class="el-icon-warning"></i> 数据质量概览</span>
                  </div>
                  <div class="quality-metrics">
                    <div class="metric-item">
                      <span class="metric-label">总体质量评分：</span>
                      <el-progress
                        :percentage="selectedDataset.dataQualityScore"
                        :color="getQualityColor(selectedDataset.dataQualityScore)"
                        :stroke-width="8">
                      </el-progress>
                    </div>
                    <div class="metric-item">
                      <span class="metric-label">完整性：</span>
                      <span class="metric-value">{{ getDataCompleteness() }}%</span>
                    </div>
                    <div class="metric-item">
                      <span class="metric-label">一致性：</span>
                      <span class="metric-value">{{ getDataConsistency() }}%</span>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card class="quality-card">
                  <div slot="header">
                    <span><i class="el-icon-data-analysis"></i> 列质量分析</span>
                  </div>
                  <div class="column-quality-list">
                    <div v-for="column in getQualityAnalysis()" :key="column.name" class="quality-item">
                      <div class="quality-item-header">
                        <span class="column-name">{{ column.name }}</span>
                        <el-tag :type="column.qualityLevel" size="mini">{{ column.qualityText }}</el-tag>
                      </div>
                      <div class="quality-item-details">
                        <span v-for="issue in column.issues" :key="issue" class="quality-issue">
                          {{ issue }}
                        </span>
                      </div>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>
        </el-tabs>
      </div>

      <span slot="footer" class="dialog-footer">
        <el-button @click="datasetPreviewVisible = false">关 闭</el-button>
        <el-button type="primary" @click="confirmDatasetSelection">
          <i class="el-icon-check"></i> 确认使用此数据集
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 参数配置增强样式 */
.enhanced-param-item {
  margin-bottom: 20px;
}

.param-description {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.param-help-icon {
  margin-right: 6px;
  color: #409EFF;
  cursor: help;
}

.param-desc-text {
  flex: 1;
}

.param-range-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.param-validation {
  margin-top: 6px;
  font-size: 12px;
}

.validation-message {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}

.validation-message i {
  margin-right: 6px;
}

.validation-suggestions {
  margin-left: 20px;
}

.validation-suggestions .el-button--text {
  padding: 0;
  font-size: 11px;
  color: #909399;
}

.number-input-container {
  position: relative;
}

.parameter-preview-card {
  margin-bottom: 20px;
  border: 1px dashed #DCDFE6;
}

.parameter-preview-card .el-card__header {
  background-color: #F5F7FA;
  border-bottom: 1px solid #EBEEF5;
}

/* 任务状态样式优化 */
.task-status-running {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.7; }
  100% { opacity: 1; }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .enhanced-param-item {
    margin-bottom: 15px;
  }

  .param-description {
    flex-direction: column;
    align-items: flex-start;
  }

  .param-help-icon {
    margin-bottom: 4px;
  }
}

/* 模板选择器样式 */
.el-select-dropdown__item {
  height: auto;
  line-height: 1.4;
  padding: 12px 20px;
}

.el-select-dropdown__item span:last-child {
  margin-top: 4px;
  display: block;
  font-size: 12px;
}

/* 参数预览样式 */
.el-descriptions__label {
  font-weight: 600;
}

.el-tag {
  margin-right: 8px;
}
</style>

<script>
import { addTask, listTask, getTask, updateTask, delTask } from "@/api/petrol/task";
import { listAvailableDatasets, getDatasetColumns, getDatasetPreview } from "@/api/petrol/dataset";
import { TEMPLATE_TYPES, PARAMETER_TEMPLATES, getParameterTemplate, getSupportedTemplateTypes, hasPresetTemplates } from "@/utils/parameterTemplates";
import { getParameterDescription, getRecommendedParameters } from "@/utils/parameterHelpers";
import { getParameterRecommendation, getParameterTemplate as getApiParameterTemplate, validateParameters as apiValidateParameters } from "@/api/petrol/parameter";
import { validateParameter, validateAllParameters, getParameterSuggestions, PARAMETER_RANGES } from "@/utils/parameterValidator";
import { debounce, showError, createSafeApiCall, createLoadingManager, debouncedParameterRecommendation } from "@/utils/errorHandler";
import SimplifiedParameterSelector from "@/components/SimplifiedParameterSelector";

const newForm = () => ({
  id: undefined,
  taskName: '',
  taskType: undefined,
  algorithm: undefined,
  parameters: {},
  datasetId: null,
  datasetName: '',
  remark: ''
});

export default {
  name: "PetrolTask",
  components: {
    SimplifiedParameterSelector
  },
   data() {
     return {
       loading: true,
       submitLoading: false,
       total: 0,
       taskList: [],
       queryParams: { pageNum: 1, pageSize: 10 },
       availableColumns: [],
       columnStats: {}, // 保存列统计信息
       availableDatasets: [], // 可用数据集列表
       selectedDataset: null, // 当前选中的数据集
       datasetPreviewVisible: false, // 数据集预览对话框
       datasetLoading: false, // 数据集加载状态
       datasetChangeTimer: null, // 数据集选择防抖定时器
       // 数据集预览相关
       previewActiveTab: 'info', // 预览对话框当前标签页
       previewLoading: false, // 预览数据加载状态
       previewData: [], // 预览数据
       previewRows: 20, // 预览行数
       columnSearchText: '', // 列搜索文本
       form: newForm(),
       pollingTimer: null,
       pollingTaskId: null,
       listPollingTimer: null,
       autoRefreshEnabled: true,
       lastRefreshTime: null,
       // 参数模板相关
       selectedTemplate: 'custom',
       showParameterPreview: false,
       parameterHistory: [], // 参数配置历史
       savedTemplates: [], // 用户保存的模板
       // 参数配置模式
       parameterMode: 'simple', // simple | advanced
       // 参数验证相关
       parameterValidations: {}, // 参数验证结果
       validationSummary: { valid: true, errorCount: 0, warningCount: 0 },
       isValidating: false,
       // 加载状态管理
       loadingManager: null,
       // 防抖函数
       debouncedValidateParameters: null,
       debouncedRecommendParameters: null,
       resultsDialog: {
         visible: false,
         title: '',
         data: null,
         task: null,
         isError: false,
         errorMessage: ''
       },
       taskTypes: [
         { value: 'regression', label: '回归分析' },
         { value: 'classification', label: '分类预测' },
         { value: 'clustering', label: '聚类分析' },
         { value: 'feature_engineering', label: '特征工程' }
       ],
       algorithms: {
         regression: [
            { value: "regression_linear_train", label: "线性回归", params: [ { key: 'feature_columns', label: '特征列(X)', type: 'select_column', rules: [{ required: true, message: "请选择一个特征列", trigger: "change" }] }, { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "Y轴列名不能为空", trigger: "change" }] } ] },
            { value: "regression_polynomial_train", label: "多项式回归", params: [ { key: 'feature_columns', label: '特征列(X)', type: 'select_column', rules: [{ required: true, message: "请选择一个特征列", trigger: "change" }] }, { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "Y轴列名不能为空", trigger: "change" }] }, { key: 'degree', label: '多项式次数', type: 'number', defaultValue: 2, min: 2, max: 10, rules: [{ required: true, message: "多项式次数不能为空", trigger: "blur" }] } ] },
            { value: "regression_exponential_train", label: "指数回归", params: [ { key: 'feature_columns', label: '特征列(X)', type: 'select_column', rules: [{ required: true, message: "请选择一个特征列", trigger: "change" }] }, { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "Y轴列名不能为空", trigger: "change" }] } ] },
            { value: "regression_logarithmic_train", label: "对数回归", params: [ { key: 'feature_columns', label: '特征列(X)', type: 'select_column', rules: [{ required: true, message: "请选择一个特征列", trigger: "change" }] }, { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "Y轴列名不能为空", trigger: "change" }] } ] },
            { value: "regression_svm_train", label: "支持向量机回归(SVR)", params: [
                { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] },
                { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "目标列不能为空", trigger: "change" }] },
                { key: 'kernel', label: '核函数', type: 'select_option', options: ['linear', 'poly', 'rbf', 'sigmoid'], defaultValue: 'rbf' },
                { key: 'C', label: '正则化参数(C)', type: 'select_option', options: [0.1, 1, 10, 100], defaultValue: 1 },
                { key: 'gamma', label: 'Gamma参数', type: 'select_option', options: ['scale', 'auto', 0.001, 0.01, 0.1, 1], defaultValue: 'scale' }
            ] },
            { value: "regression_random_forest_train", label: "随机森林回归", params: [
                { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] },
                { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "目标列不能为空", trigger: "change" }] },
                { key: 'n_estimators', label: '树的数量', type: 'select_option', options: [50, 100, 200, 300, 500], defaultValue: 100 },
                { key: 'max_depth', label: '最大深度', type: 'select_option', options: [3, 5, 10, 15, 20, 'None'], defaultValue: 'None' },
                { key: 'min_samples_split', label: '最小分割样本数', type: 'select_option', options: [2, 5, 10, 20], defaultValue: 2 },
                { key: 'random_state', label: '随机种子', type: 'select_option', options: [42, 123, 456, 789], defaultValue: 42 }
            ] },
            { value: "regression_lightgbm_train", label: "LightGBM回归", params: [
                { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] },
                { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "目标列不能为空", trigger: "change" }] },
                { key: 'n_estimators', label: '树的数量', type: 'select_option', options: [50, 100, 200, 300, 500], defaultValue: 100 },
                { key: 'learning_rate', label: '学习率', type: 'select_option', options: [0.01, 0.05, 0.1, 0.2, 0.3], defaultValue: 0.1 },
                { key: 'max_depth', label: '最大深度', type: 'select_option', options: [3, 5, 7, 10, 15], defaultValue: 7 },
                { key: 'num_leaves', label: '叶子节点数', type: 'select_option', options: [31, 50, 100, 200, 300], defaultValue: 31 }
            ] },
            { value: "regression_xgboost_train", label: "XGBoost回归", params: [
                { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] },
                { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "目标列不能为空", trigger: "change" }] },
                { key: 'n_estimators', label: '树的数量', type: 'select_option', options: [50, 100, 200, 300, 500], defaultValue: 100 },
                { key: 'learning_rate', label: '学习率', type: 'select_option', options: [0.01, 0.05, 0.1, 0.2, 0.3], defaultValue: 0.1 },
                { key: 'max_depth', label: '最大深度', type: 'select_option', options: [3, 5, 6, 8, 10], defaultValue: 6 },
                { key: 'subsample', label: '子样本比例', type: 'select_option', options: [0.6, 0.7, 0.8, 0.9, 1.0], defaultValue: 1.0 },
                { key: 'colsample_bytree', label: '特征采样比例', type: 'select_option', options: [0.6, 0.7, 0.8, 0.9, 1.0], defaultValue: 1.0 }
            ] },
            { value: "regression_bilstm_train", label: "BiLSTM时序回归 (PyTorch)", params: [
                { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "请选择一个或多个特征列", trigger: "change" }] },
                { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "请选择目标列", trigger: "change" }] },
                { key: 'sequence_length', label: '序列长度', type: 'select_option', options: [5, 10, 15, 20, 30, 50], defaultValue: 10 },
                { key: 'hidden_size', label: '隐藏层大小', type: 'select_option', options: [32, 64, 128, 256, 512], defaultValue: 128 },
                { key: 'num_layers', label: 'LSTM层数', type: 'select_option', options: [1, 2, 3, 4], defaultValue: 2 },
                { key: 'epochs', label: '训练轮数', type: 'select_option', options: [20, 50, 100, 200, 300], defaultValue: 50 },
                { key: 'batch_size', label: '批处理大小', type: 'select_option', options: [16, 32, 64, 128], defaultValue: 32 },
                { key: 'learning_rate', label: '学习率', type: 'select_option', options: [0.0001, 0.0005, 0.001, 0.005, 0.01], defaultValue: 0.001 },
                { key: 'dropout', label: 'Dropout率', type: 'select_option', options: [0.0, 0.1, 0.2, 0.3, 0.4, 0.5], defaultValue: 0.2 }
            ] },
            { value: "regression_tcn_train", label: "TCN时序回归 (PyTorch)", params: [
                { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "请选择一个或多个特征列", trigger: "change" }] },
                { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "请选择目标列", trigger: "change" }] },
                { key: 'sequence_length', label: '序列长度', type: 'select_option', options: [30, 60, 90, 120, 150], defaultValue: 60 },
                { key: 'epochs', label: '训练轮数', type: 'select_option', options: [50, 100, 200, 300, 500], defaultValue: 100 },
                { key: 'batch_size', label: '批处理大小', type: 'select_option', options: [16, 32, 64, 128], defaultValue: 32 },
                { key: 'learning_rate', label: '学习率', type: 'select_option', options: [0.0001, 0.0005, 0.001, 0.005, 0.01], defaultValue: 0.001 },
                { key: 'num_channels', label: '通道数', type: 'select_option', options: [32, 64, 128, 256], defaultValue: 64 },
                { key: 'kernel_size', label: '卷积核大小', type: 'select_option', options: [2, 3, 4, 5, 7], defaultValue: 3 },
                { key: 'dropout', label: 'Dropout率', type: 'select_option', options: [0.0, 0.1, 0.2, 0.3, 0.4, 0.5], defaultValue: 0.2 }
            ] }
         ],
         classification: [
            { value: "classification_svm_train", label: "支持向量机分类(SVC)", params: [
                { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] },
                { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "目标列不能为空", trigger: "change" }] },
                { key: 'kernel', label: '核函数', type: 'select_option', options: ['linear', 'poly', 'rbf', 'sigmoid'], defaultValue: 'rbf' },
                { key: 'C', label: '正则化参数(C)', type: 'select_option', options: [0.1, 1, 10, 100], defaultValue: 1 },
                { key: 'gamma', label: 'Gamma参数', type: 'select_option', options: ['scale', 'auto', 0.001, 0.01, 0.1, 1], defaultValue: 'scale' }
            ] },
            { value: "classification_knn_train", label: "K-近邻分类", params: [
                { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] },
                { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "目标列不能为空", trigger: "change" }] },
                { key: 'n_neighbors', label: '邻居数(K)', type: 'select_option', options: [3, 5, 7, 9, 11, 15], defaultValue: 5 },
                { key: 'weights', label: '权重函数', type: 'select_option', options: ['uniform', 'distance'], defaultValue: 'uniform' },
                { key: 'algorithm', label: '算法', type: 'select_option', options: ['auto', 'ball_tree', 'kd_tree', 'brute'], defaultValue: 'auto' }
            ] }
         ],
         clustering: [
            { value: "clustering_kmeans_train", label: "K-Means聚类", params: [
                { key: 'feature_columns', label: '特征列', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] },
                { key: 'n_clusters', label: '聚类数量(K)', type: 'select_option', options: [2, 3, 4, 5, 6, 7, 8, 10], defaultValue: 3 },
                { key: 'init', label: '初始化方法', type: 'select_option', options: ['k-means++', 'random'], defaultValue: 'k-means++' },
                { key: 'max_iter', label: '最大迭代次数', type: 'select_option', options: [100, 200, 300, 500, 1000], defaultValue: 300 },
                { key: 'random_state', label: '随机种子', type: 'select_option', options: [42, 123, 456, 789], defaultValue: 42 }
            ] },
         ],
         feature_engineering: [
           { value: "feature_engineering_fractal_dimension_train", label: "分形维数计算", params: [ { key: 'column_name', label: '计算列', type: 'select_column', rules: [{ required: true, message: "请选择要计算的列", trigger: "change" }] }, { key: 'depth_column', label: '深度列', type: 'select_column', rules: [{ required: true, message: "请选择深度列", trigger: "change" }] }, { key: 'min_depth', label: '最小深度', type: 'number', step: 0.1, defaultValue: 0, placeholder: '留空或填0自动使用数据最小值' }, { key: 'max_depth', label: '最大深度', type: 'number', step: 0.1, defaultValue: 0, placeholder: '留空或填0自动使用数据最大值' } ] },
           { value: "feature_engineering_automatic_regression_train", label: "自动最优回归分析", params: [ { key: 'feature_columns', label: '特征列(X)', type: 'select_column', rules: [{ required: true, message: "请选择一个特征列", trigger: "change" }] }, { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "目标列不能为空", trigger: "change" }] } ] },
         ]
       },
       rules: { taskName: [{ required: true, message: "任务名称不能为空", trigger: "blur" }], taskType: [{ required: true, message: "请选择任务类型", trigger: "change" }], algorithm: [{ required: true, message: "请选择一个算法", trigger: "change" }], file: [{ required: false, validator: (rule, value, callback) => { if (!this.form.id && !this.form.file) { callback(new Error("请上传数据文件")); } else { callback(); } }, trigger: "change" }] }
     };
   },
   computed: {
     algorithmOptions() {
       if (!this.form.taskType) return [];
       return this.algorithms[this.form.taskType] || [];
     },
     currentAlgorithmParams() {
       if (!this.form.algorithm || !this.form.taskType) return [];
       const selectedAlgo = this.algorithms[this.form.taskType]?.find(opt => opt.value === this.form.algorithm);
       return selectedAlgo ? selectedAlgo.params : [];
     },
     // 参数模板相关计算属性
     hasParameterTemplates() {
       return this.form.algorithm && hasPresetTemplates(this.form.algorithm);
     },
     availableTemplates() {
       if (!this.form.algorithm) return [];
       return getSupportedTemplateTypes(this.form.algorithm);
     },
     canRecommend() {
       return this.availableColumns.length > 0 && this.form.algorithm;
     },
     parsedTaskParams() {
        if (this.resultsDialog.task && this.resultsDialog.task.inputParamsJson) {
            try {
                const params = JSON.parse(this.resultsDialog.task.inputParamsJson);
                // 美化数组显示
                Object.keys(params).forEach(key => {
                    if (Array.isArray(params[key])) {
                        params[key] = params[key].join(', ');
                    }
                });
                return params;
            } catch (e) {
                console.error("解析任务参数JSON失败:", e);
                return { "参数解析失败": "无法读取有效的JSON格式" };
            }
        }
        return {};
     },
     /** 任务统计 */
     taskStats() {
       const stats = {
         total: this.taskList.length,
         running: 0,
         completed: 0,
         failed: 0,
         pending: 0
       };

       this.taskList.forEach(task => {
         switch(task.status) {
           case 'RUNNING':
             stats.running++;
             break;
           case 'COMPLETED':
             stats.completed++;
             break;
           case 'FAILED':
             stats.failed++;
             break;
           case 'PENDING':
           case 'QUEUED':
             stats.pending++;
             break;
         }
       });

       return stats;
     }
   },
   created() {
     this.getList();
     this.loadAvailableDatasets();
     this.startListPolling();

     // 初始化加载管理器
     this.loadingManager = createLoadingManager(this);

     // 初始化防抖函数
     this.debouncedValidateParameters = debounce(this.validateParametersInternal, 300);
     this.debouncedRecommendParameters = debounce(this.applyRecommendedParametersInternal, 500);

     // 检查是否有来自模型管理页面的任务ID参数
     if (this.$route.query.taskId) {
       this.highlightTask(this.$route.query.taskId);
     }
   },
   methods: {
     /** 查询任务列表 */
     getList() {
       this.loading = true;
       listTask(this.queryParams).then(response => {
         this.taskList = response.rows;
         this.total = response.total;
         this.lastRefreshTime = new Date();
         this.loading = false;
       }).catch(error => {
         console.error("获取任务列表失败:", error);
         this.loading = false;
       });
     },
     /** 开始列表轮询 */
     startListPolling() {
       if (this.autoRefreshEnabled && !this.listPollingTimer) {
         this.listPollingTimer = setInterval(() => {
           // 只有当存在运行中的任务时才自动刷新
           const hasRunningTasks = this.taskList.some(task =>
             task.status === 'RUNNING' || task.status === 'PENDING' || task.status === 'QUEUED'
           );
           if (hasRunningTasks) {
             this.getList();
           }
         }, 5000); // 每5秒检查一次
       }
     },
     /** 停止列表轮询 */
     stopListPolling() {
       if (this.listPollingTimer) {
         clearInterval(this.listPollingTimer);
         this.listPollingTimer = null;
       }
     },
     /** 切换自动刷新 */
     toggleAutoRefresh() {
       this.autoRefreshEnabled = !this.autoRefreshEnabled;
       if (this.autoRefreshEnabled) {
         this.startListPolling();
         this.$message.success('自动刷新已开启');
       } else {
         this.stopListPolling();
         this.$message.info('自动刷新已关闭');
       }
     },
     /** 重置按钮操作 */
     resetForm() {
       // 彻底替换为新的表单对象，清除包括id在内的所有旧数据
       this.form = newForm();
       this.availableColumns = [];
       this.columnStats = {};
       this.$nextTick(() => {
         this.$refs.form.resetFields();
         // resetFields可能不会清除动态添加的参数校验，我们手动清除一下
         // 同时保留对主表单的校验状态清除
         this.$refs.form.clearValidate();
       });
     },
     /** 提交按钮 */
     submitForm() {
       // 首先进行参数验证
       this.validateParametersInternal();

       // 检查参数验证结果
       if (!this.validationSummary.valid) {
         this.$message.error(`参数验证失败：发现 ${this.validationSummary.errorCount} 个错误，请修正后重试`);
         return;
       }

       this.$refs["form"].validate(valid => {
         if (valid) {
           this.submitLoading = true;
           // 在所有情况下，我们都需要将 params 对象转换为 JSON 字符串，因为后端的一个字段需要它
           const paramsString = JSON.stringify(this.form.parameters);

           if (this.form.id != null) {
             // 更新逻辑: 发送包含 params 的完整 JSON 对象
             const updateData = {
               id: this.form.id,
               taskName: this.form.taskName,
               algorithm: this.form.algorithm,
               remark: this.form.remark,
               params: this.form.parameters // 发送原始 object
             };
             updateTask(updateData).then(response => {
               this.getList();
               this.$modal.msgSuccess("修改并开始重新处理");
               this.resetForm();
             }).finally(() => {
               this.submitLoading = false;
             });
           } else {
             // 新增逻辑: 发送 FormData
             const formData = new FormData();
             formData.append("taskName", this.form.taskName);
             formData.append("taskType", this.form.taskType);
             formData.append("algorithm", this.form.algorithm);
             formData.append("remark", this.form.remark);

             // 使用数据集
             if (!this.form.datasetId) {
               this.$modal.msgError("请选择数据集");
               this.submitLoading = false;
               return;
             }
             formData.append("datasetId", this.form.datasetId);
             formData.append("datasetName", this.form.datasetName);
             // 后端 @RequestPart("params") 需要一个字符串
             formData.append("params", paramsString);

             addTask(formData).then(response => {
               this.getList();
               this.$modal.msgSuccess("任务提交成功，正在处理中...");
               this.resetForm();

               // 如果返回了任务ID，可以自动查看结果
               if (response.data && response.data.id) {
                 setTimeout(() => {
                   const newTask = this.taskList.find(task => task.id === response.data.id);
                   if (newTask) {
                     this.handleView(newTask);
                   }
                 }, 1000);
               }
             }).finally(() => {
               this.submitLoading = false;
             });
           }
         }
       });
     },
     /** 删除按钮操作 */
     handleDelete(row) {
       const taskIds = row.id || this.ids;
       const taskName = row.taskName || '选中的任务';

       const message = `确认删除分析任务 "${taskName}" (ID: ${taskIds})？\n\n⚠️ 注意：\n• 任务相关的所有文件将被删除\n• 由该任务生成的模型也将被删除\n• 删除后无法恢复`;

       this.$confirm(message, '删除确认', {
         type: 'warning',
         confirmButtonText: '确认删除',
         cancelButtonText: '取消',
         dangerouslyUseHTMLString: false
       }).then(() => {
         return delTask(taskIds);
       }).then(() => {
         this.getList();
         this.$modal.msgSuccess("任务及相关数据删除成功");
       }).catch((error) => {
         if (error !== 'cancel') {
           console.error('删除任务失败:', error);
           this.$modal.msgError("删除失败: " + (error.response?.data?.msg || error.message || '未知错误'));
         }
       });
     },
     handleTaskTypeChange() {
       this.form.algorithm = undefined;
       this.form.parameters = {};
     },
     handleAlgorithmChange() {
        this.form.parameters = {};
        this.selectedTemplate = 'custom';
        this.currentAlgorithmParams.forEach(param => {
          if (param.defaultValue !== undefined) {
            this.$set(this.form.parameters, param.key, param.defaultValue);
          } else {
            // 为没有默认值的参数初始化空值，避免验证错误
            if (param.type === 'multi_select_column') {
              this.$set(this.form.parameters, param.key, []);
            } else {
              this.$set(this.form.parameters, param.key, '');
            }
          }
        });
     },

     /** 加载可用数据集列表 */
     async loadAvailableDatasets() {
       try {
         console.log('🔍 开始加载数据集列表...');
         const response = await listAvailableDatasets();
         console.log('📊 API原始响应:', response);
         console.log('📊 响应状态码:', response.code);
         console.log('📊 响应消息:', response.msg);
         console.log('📊 响应数据:', response.data);

         // 确保初始化为数组
         this.availableDatasets = response.data || [];
         console.log('✅ 处理后的数据集列表:', this.availableDatasets);
         console.log('📈 数据集数量:', this.availableDatasets.length);

         if (this.availableDatasets.length === 0) {
           console.warn('⚠️ 没有可用的数据集');
           this.$message.warning('暂无可用数据集，请先上传数据集');

           // 尝试获取所有数据集来调试
           console.log('🔍 尝试获取所有数据集进行调试...');
           try {
             const allDatasetsResponse = await this.$http.get('/petrol/dataset/list');
             console.log('📊 所有数据集响应:', allDatasetsResponse.data);
           } catch (debugError) {
             console.error('获取所有数据集失败:', debugError);
           }
         } else {
           console.log('🎯 数据集详情:', this.availableDatasets.map(d => ({
             id: d.id,
             name: d.datasetName,
             status: d.status,
             category: d.datasetCategory,
             createTime: d.createTime
           })));
           this.$message.success(`成功加载 ${this.availableDatasets.length} 个数据集`);
         }
       } catch (error) {
         console.error('❌ 加载数据集列表失败:', error);
         console.error('❌ 错误详情:', {
           message: error.message,
           response: error.response,
           status: error.response?.status,
           data: error.response?.data
         });
         this.$message.error('加载数据集列表失败: ' + (error.message || '未知错误'));
         // 确保在失败时也初始化为空数组
         this.availableDatasets = [];
       }
     },

     /** 检查数据集列表状态 */
     checkDatasetListStatus() {
       console.log('检查数据集列表状态:');
       console.log('- availableDatasets:', this.availableDatasets);
       console.log('- 类型:', typeof this.availableDatasets);
       console.log('- 是否为数组:', Array.isArray(this.availableDatasets));
       console.log('- 长度:', this.availableDatasets ? this.availableDatasets.length : 'undefined');
     },

     /** 数据集选择变化处理 */
     async handleDatasetChangeInternal(datasetId) {
       if (!datasetId) {
         this.selectedDataset = null;
         this.availableColumns = [];
         this.columnStats = {};
         if (this.form) {
           this.form.parameters = {};
         }
         return;
       }

       try {
         // 显示加载状态
         this.datasetLoading = true;

         // 调试信息
         console.log('选择的数据集ID:', datasetId);
         this.checkDatasetListStatus();

         // 获取数据集信息
         const dataset = this.availableDatasets && this.availableDatasets.find(d => d.id === datasetId);
         if (dataset) {
           this.selectedDataset = dataset;
           this.form.datasetName = dataset.datasetName;
           console.log('找到数据集:', dataset);
         } else {
           console.warn('未找到数据集，ID:', datasetId);
         }

         // 获取数据集列信息
         console.log('正在获取数据集列信息，ID:', datasetId);
         const response = await getDatasetColumns(datasetId);

         if (response.data && response.data.columns) {
           this.availableColumns = response.data.columns;
           this.columnStats = response.data.stats || {};

           // 清空当前参数，让用户重新选择
           if (this.form) {
             this.form.parameters = {};
           }

           this.$message.success('数据集加载成功');
         }
       } catch (error) {
         console.error('加载数据集信息失败:', error);
         let errorMessage = '未知错误';
         if (error.response && error.response.data && error.response.data.msg) {
           errorMessage = error.response.data.msg;
         } else if (error.message) {
           errorMessage = error.message;
         }
         this.$message.error('加载数据集信息失败: ' + errorMessage);
       } finally {
         this.datasetLoading = false;
       }
     },

     /** 数据集选择变化处理（防抖包装） */
     handleDatasetChange(datasetId) {
       console.log('handleDatasetChange 被调用，ID:', datasetId);
       console.log('this 上下文检查:', {
         hasThis: !!this,
         hasForm: !!this.form,
         hasDatasetLoading: 'datasetLoading' in this
       });

       // 使用防抖调用内部方法
       if (this.datasetChangeTimer) {
         clearTimeout(this.datasetChangeTimer);
       }
       this.datasetChangeTimer = setTimeout(() => {
         this.handleDatasetChangeInternal(datasetId);
       }, 300);
     },

     /** 预览选中的数据集 */
     previewSelectedDataset() {
       if (!this.form.datasetId) {
         this.$message.warning('请先选择数据集');
         return;
       }
       this.previewActiveTab = 'info';
       this.datasetPreviewVisible = true;
       // 自动加载预览数据
       this.$nextTick(() => {
         this.loadPreviewData();
       });
     },

     /** 加载预览数据 */
     async loadPreviewData() {
       if (!this.form.datasetId) return;

       this.previewLoading = true;
       try {
         const response = await getDatasetPreview(this.form.datasetId, this.previewRows);
         if (response.data && response.data.success) {
           this.processPreviewData(response.data.data);
         } else {
           this.$message.error('加载预览数据失败');
           this.previewData = [];
         }
       } catch (error) {
         console.error('加载预览数据失败:', error);
         this.$message.error('加载预览数据失败');
         this.previewData = [];
       } finally {
         this.previewLoading = false;
       }
     },

     /** 处理预览数据 */
     processPreviewData(rawData) {
       if (!rawData || rawData.length === 0) {
         this.previewData = [];
         return;
       }

       // 第一行作为列名，其余行作为数据
       const headers = rawData[0] || [];
       const dataRows = rawData.slice(1);

       this.previewData = dataRows.map(row => {
         const rowObj = {};
         headers.forEach((header, index) => {
           rowObj[header] = row[index];
         });
         return rowObj;
       });
     },

     /** 确认选择数据集 */
     confirmDatasetSelection() {
       this.datasetPreviewVisible = false;
       this.$message.success(`已选择数据集：${this.selectedDataset.datasetName}`);
     },
     /** 获取数据集类别标签类型 */
     getDatasetCategoryType(category) {
       const typeMap = {
         '测井': 'primary',
         '地震': 'success',
         '生产': 'warning',
         '地质': 'info'
       };
       return typeMap[category] || '';
     },
     /** 格式化文件大小 */
     formatFileSize(bytes) {
       if (!bytes) return '0 B';
       const k = 1024;
       const sizes = ['B', 'KB', 'MB', 'GB'];
       const i = Math.floor(Math.log(bytes) / Math.log(k));
       return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
     },
     /** 获取数据质量颜色 */
     getQualityColor(score) {
       if (score >= 90) return '#67c23a';
       if (score >= 70) return '#409eff';
       if (score >= 50) return '#e6a23c';
       return '#f56c6c';
     },
     /** 获取列信息表格数据 */
     getColumnTableData() {
       return this.availableColumns.map(col => {
         const stats = this.columnStats[col];
         return {
           name: col,
           type: stats?.type || 'unknown',
           nonNull: stats?.non_null_count || '-',
           unique: stats?.unique_count || '-',
           stats: this.formatColumnStats(stats)
         };
       });
     },
     /** 格式化列统计信息 */
     formatColumnStats(stats) {
       if (!stats) return '';

       const parts = [];
       if (stats.mean !== undefined) {
         parts.push(`均值: ${Number(stats.mean).toFixed(2)}`);
       }
       if (stats.std !== undefined) {
         parts.push(`标准差: ${Number(stats.std).toFixed(2)}`);
       }
       if (stats.min !== undefined && stats.max !== undefined) {
         parts.push(`范围: ${stats.min} ~ ${stats.max}`);
       }

       return parts.join(', ');
     },
     /** 获取列类型标签 */
     getColumnTypeTag(type) {
       const typeMap = {
         'numeric': 'success',
         'integer': 'primary',
         'float': 'primary',
         'string': 'info',
         'object': 'info',
         'datetime': 'warning',
         'boolean': 'danger'
       };
       return typeMap[type] || '';
     },
     // 参数模板相关方法
     handleTemplateChange(templateType) {
       if (templateType === 'custom') {
         return;
       }

       const template = getParameterTemplate(this.form.algorithm, templateType);
       if (template) {
         // 保存当前参数到历史
         this.saveParameterHistory();

         // 应用模板参数
         Object.keys(template).forEach(key => {
           this.$set(this.form.parameters, key, template[key]);
         });

         this.$message.success(`已应用${TEMPLATE_TYPES[templateType]?.label}模板`);
       }
     },
     applyRecommendedParameters() {
       if (!this.canRecommend) {
         this.$message.warning('请先上传数据文件并选择算法');
         return;
       }

       // 使用防抖函数
       this.debouncedRecommendParameters();
     },

     // 内部参数推荐方法
     async applyRecommendedParametersInternal() {
       const loadingKey = 'parameter-recommendation';

       try {
         // 显示加载状态
         this.loadingManager.start(loadingKey, '正在分析数据集特征并生成参数推荐...');

         // 保存当前参数到历史
         this.saveParameterHistory();

         // 获取数据集信息
         const datasetInfo = {
           sampleCount: this.getEstimatedSampleCount(),
           featureCount: this.availableColumns.length,
           targetType: 'numeric'
         };

         // 创建安全的API调用
         const safeApiCall = createSafeApiCall(getParameterRecommendation, {
           showError: false, // 我们自己处理错误
           maxRetries: 2,
           timeout: 10000
         });

         let recommended;

         try {
           // 尝试调用后端API
           const response = await safeApiCall(this.form.algorithm, datasetInfo);
           recommended = response.data;

           this.$message.success('已应用智能推荐参数（基于服务器分析）');
         } catch (apiError) {
           console.warn('后端参数推荐失败，使用本地推荐:', apiError);

           // 降级到前端推荐
           recommended = getRecommendedParameters(this.form.algorithm, datasetInfo);

           this.$message({
             type: 'success',
             message: '已应用本地推荐参数',
             duration: 3000
           });
         }

         // 应用推荐参数
         if (recommended && Object.keys(recommended).length > 0) {
           Object.keys(recommended).forEach(key => {
             this.$set(this.form.parameters, key, recommended[key]);
           });

           // 触发参数验证
           this.debouncedValidateParameters();
         } else {
           this.$message.warning('未找到适合的参数推荐');
         }

       } catch (error) {
         console.error('参数推荐失败:', error);
         showError(error, {
           showSuggestions: true
         });
       } finally {
         // 隐藏加载状态
         this.loadingManager.stop(loadingKey);
       }
     },
     getEstimatedSampleCount() {
       // 基于列统计信息估算样本数量
       const stats = Object.values(this.columnStats);
       if (stats.length > 0) {
         // 这里可以根据实际需要改进估算逻辑
         return 1000; // 默认估算值
       }
       return 1000;
     },
     saveParameterHistory() {
       if (Object.keys(this.form.parameters).length > 0) {
         this.parameterHistory.push({
           timestamp: new Date(),
           parameters: { ...this.form.parameters }
         });

         // 只保留最近10次历史
         if (this.parameterHistory.length > 10) {
           this.parameterHistory.shift();
         }
       }
     },

     // 参数配置管理方法
     resetParameters() {
       this.$confirm('确认重置所有参数配置？', '重置确认', {
         type: 'warning'
       }).then(() => {
         this.saveParameterHistory();
         this.form.parameters = {};
         this.selectedTemplate = 'custom';

         // 重新应用默认值
         this.currentAlgorithmParams.forEach(param => {
           if (param.defaultValue !== undefined) {
             this.$set(this.form.parameters, param.key, param.defaultValue);
           } else {
             // 为没有默认值的参数初始化空值
             if (param.type === 'multi_select_column') {
               this.$set(this.form.parameters, param.key, []);
             } else {
               this.$set(this.form.parameters, param.key, '');
             }
           }
         });

         this.$message.success('参数已重置');
       }).catch(() => {});
     },
     saveParameterTemplate() {
       if (Object.keys(this.form.parameters).length === 0) {
         this.$message.warning('请先配置参数');
         return;
       }

       this.$prompt('请输入模板名称', '保存参数模板', {
         confirmButtonText: '保存',
         cancelButtonText: '取消',
         inputPattern: /^.{1,20}$/,
         inputErrorMessage: '模板名称长度为1-20个字符'
       }).then(({ value }) => {
         const template = {
           name: value,
           algorithm: this.form.algorithm,
           parameters: { ...this.form.parameters },
           createTime: new Date()
         };

         this.savedTemplates.push(template);

         // 这里可以调用API保存到后端
         // savePetrolParameterTemplate(template)

         this.$message.success('参数模板保存成功');
       }).catch(() => {});
     },
     /** 修改按钮操作 */
     handleUpdate(row) {
       // 在加载新数据前，先彻底清空旧表单，防止状态污染
       this.resetForm();
       
       getTask(row.id).then(response => {
         const taskData = response.data;
         
         // 从算法名称中推断 taskType
         const algorithmName = taskData.algorithm || '';
         const taskType = algorithmName.split('_')[0];

         // 逐个字段安全赋值
         this.form.id = taskData.id;
         this.form.taskName = taskData.taskName;
         this.form.remark = taskData.remark;
         this.form.taskType = taskType;

         // 使用 $nextTick 等待 DOM 更新（例如算法下拉框根据taskType刷新）
         this.$nextTick(() => {
           this.form.algorithm = taskData.algorithm;

           // 再次使用 $nextTick 确保算法参数的DOM已经生成
           this.$nextTick(() => {
             // 解析并回填参数
             if (taskData.inputParamsJson) {
               try {
                 this.form.parameters = JSON.parse(taskData.inputParamsJson);
               } catch (e) {
                 console.error("解析任务参数失败:", e);
                 this.form.parameters = {}; // 解析失败则置空
               }
             }
           });
         });
         
         // 回填文件列信息以供参数选择
         if(taskData.inputFileHeadersJson) {
            try {
              this.availableColumns = JSON.parse(taskData.inputFileHeadersJson);
            } catch(e) {
              console.error("解析文件头失败:", e);
              this.availableColumns = [];
            }
         }
       });
     },
     handleView(row) {
        // 清除可能存在的旧轮询
        if (this.pollingTimer) {
            clearInterval(this.pollingTimer);
            this.pollingTimer = null;
        }

        const taskStatus = row.status;
        this.pollingTaskId = row.id;

        if (taskStatus === 'QUEUED' || taskStatus === 'RUNNING' || taskStatus === 'PENDING') {
            this.resultsDialog.title = `任务 #${row.id} - ${row.taskName} 正在处理中...`;
            this.resultsDialog.data = null; 
            this.resultsDialog.isError = false;
            this.resultsDialog.task = row;
            this.resultsDialog.visible = true;
            
            this.pollingTimer = setInterval(() => {
                getTask(this.pollingTaskId).then(response => {
                    const latestTask = response.data;
                    if (latestTask.status !== 'QUEUED' && latestTask.status !== 'RUNNING' && latestTask.status !== 'PENDING') {
                        clearInterval(this.pollingTimer);
                        this.pollingTimer = null;
                        this.pollingTaskId = null;
                        // 任务结束，调用自身以显示最终结果
                        this.handleView(latestTask); 
                    }
                }).catch(error => {
                    console.error("轮询任务状态失败:", error);
                    clearInterval(this.pollingTimer); // 出错时也停止轮询
                    this.pollingTimer = null;
                    this.$modal.msgError("获取任务状态失败，请稍后重试。");
                    this.resultsDialog.visible = false;
                });
            }, 3000); // 每3秒轮询一次

        } else if (taskStatus === 'COMPLETED' && row.resultsJson) {
            try {
                const resultsData = JSON.parse(row.resultsJson);
                this.resultsDialog.data = resultsData;
                this.resultsDialog.title = `任务 #${row.id} - ${row.taskName} 的分析结果`;
                this.resultsDialog.task = row;
                this.resultsDialog.visible = true;
            } catch (e) {
                this.resultsDialog.data = { raw: row.resultsJson };
                this.resultsDialog.title = `任务 #${row.id} - ${row.taskName} 的原始结果`;
                this.resultsDialog.task = row;
                this.resultsDialog.visible = true;
            }
        } else if (taskStatus === 'FAILED' && row.errorMessage) {
            this.resultsDialog.title = `任务 #${row.id} - ${row.taskName} 执行失败`;
            this.resultsDialog.data = null;
            this.resultsDialog.isError = true;
            this.resultsDialog.errorMessage = row.errorMessage;
            this.resultsDialog.task = row;
            this.resultsDialog.visible = true;
        } else {
            this.$modal.msgInfo("该任务当前没有可供查看的结果。");
        }
     },
     handleDialogClose() {
        // 关闭对话框时，清除轮询定时器
        if (this.pollingTimer) {
            clearInterval(this.pollingTimer);
            this.pollingTimer = null;
            this.pollingTaskId = null;
        }
     },
     getImageUrl(relativePath) {
        if (!relativePath) return '';
        if (relativePath.startsWith('/profile/')) {
            return process.env.VUE_APP_BASE_API + relativePath;
        }
        if (relativePath.startsWith('data/')) {
            const webPath = '/profile/' + relativePath.substring('data/'.length);
            return process.env.VUE_APP_BASE_API + webPath;
        }
        if (relativePath.includes(':')) {
            console.warn("路径无法在Web中显示:", relativePath);
            return '';
        }
        return process.env.VUE_APP_BASE_API + relativePath;
     },
     handleDownload(relativePath) {
        const url = this.getImageUrl(relativePath);
        window.open(url, '_blank');
     },

     /** 高亮显示指定任务 */
     highlightTask(taskId) {
       this.$nextTick(() => {
         // 查找对应的任务并高亮显示
         const task = this.taskList.find(t => t.id == taskId);
         if (task) {
           // 滚动到对应任务
           const tableRows = document.querySelectorAll('.el-table__body-wrapper tbody tr');
           const taskIndex = this.taskList.findIndex(t => t.id == taskId);
           if (taskIndex >= 0 && tableRows[taskIndex]) {
             tableRows[taskIndex].scrollIntoView({ behavior: 'smooth', block: 'center' });
             tableRows[taskIndex].style.backgroundColor = '#e6f7ff';
             setTimeout(() => {
               tableRows[taskIndex].style.backgroundColor = '';
             }, 3000);
           }
           // 显示提示信息
           this.$message.success(`已定位到任务 #${taskId}: ${task.taskName}`);
         } else {
           this.$message.warning(`未找到任务 #${taskId}`);
         }
       });
     },

     /** 获取表单验证规则 */
     getFormRules(param) {
       if (!param.rules) return [];

       // 修改验证规则，只在blur时触发，避免在change时立即验证
       return param.rules.map(rule => ({
         ...rule,
         trigger: 'blur' // 改为失去焦点时验证，而不是change时验证
       }));
     },

     onParameterChange(key, value) {
       this.form.parameters[key] = value;

       // 如果是分形维数算法且选择了深度列，自动填充深度范围
       if (this.form.algorithm === 'feature_engineering_fractal_dimension_train' && key === 'depth_column' && value) {
         const columnStat = this.columnStats[value];
         if (columnStat && columnStat.min !== null && columnStat.max !== null) {
           // 自动填充最小深度和最大深度
           this.$set(this.form.parameters, 'min_depth', columnStat.min);
           this.$set(this.form.parameters, 'max_depth', columnStat.max);
           this.$modal.msgSuccess(`已自动设置深度范围：${columnStat.min} - ${columnStat.max}`);
         }
       }

       // 参数改变时重置模板选择为自定义
       this.selectedTemplate = 'custom';

       // 触发参数验证（防抖）
       this.debouncedValidateParameters();
     },

     // 内部参数验证方法
     validateParametersInternal() {
       if (!this.form.parameters || Object.keys(this.form.parameters).length === 0) {
         this.parameterValidations = {};
         this.validationSummary = { valid: true, errorCount: 0, warningCount: 0 };
         return;
       }

       this.isValidating = true;

       try {
         const { results, summary } = validateAllParameters(this.form.parameters);
         this.parameterValidations = results;
         this.validationSummary = summary;

         // 如果有错误，显示汇总信息
         if (summary.errorCount > 0) {
           this.$message({
             type: 'error',
             message: `发现 ${summary.errorCount} 个参数错误，请检查后重试`,
             duration: 3000
           });
         } else if (summary.warningCount > 0) {
           this.$message({
             type: 'warning',
             message: `发现 ${summary.warningCount} 个参数警告，建议优化`,
             duration: 2000
           });
         }
       } catch (error) {
         console.error('参数验证失败:', error);
         showError(error);
       } finally {
         this.isValidating = false;
       }
     },
     // 参数界面增强方法
     getParameterInfo(paramKey) {
       return getParameterDescription(paramKey);
     },
     getParameterOptions(param) {
       const paramInfo = this.getParameterInfo(param.key);
       if (paramInfo.options) {
         return paramInfo.options;
       }
       return param.options || [];
     },
     getColumnStatsText(columnName) {
       const stats = this.columnStats[columnName];
       if (stats && stats.min !== null && stats.max !== null) {
         return `${stats.min.toFixed(2)} ~ ${stats.max.toFixed(2)}`;
       }
       return '';
     },
     getParameterValidation(paramKey) {
       const validation = this.parameterValidations[paramKey];
       if (!validation) {
         return null;
       }

       const iconMap = {
         'success': 'el-icon-success',
         'warning': 'el-icon-warning',
         'error': 'el-icon-close',
         'info': 'el-icon-info'
       };

       const colorMap = {
         'success': '#67C23A',
         'warning': '#E6A23C',
         'error': '#F56C6C',
         'info': '#909399'
       };

       return {
         icon: iconMap[validation.level] || 'el-icon-info',
         color: colorMap[validation.level] || '#909399',
         message: validation.message,
         suggestions: validation.suggestions || []
       };
     },
     toggleParameterPreview() {
       this.showParameterPreview = !this.showParameterPreview;
     },
     formatParameterValue(value) {
       if (Array.isArray(value)) {
         return value.join(', ');
       }
       return String(value);
     },
     getParameterValueType(key, value) {
       const paramInfo = this.getParameterInfo(key);
       if (paramInfo.ranges && typeof value === 'number') {
         const { recommended } = paramInfo.ranges;
         if (recommended && recommended.includes(value)) {
           return 'success';
         }
         return 'warning';
       }
       return 'info';
     },
     /** 获取算法显示名称 */
     getAlgorithmDisplayName(algorithm) {
       if (!algorithm) return '未知算法';

       // 创建算法名称映射
       const algorithmMap = {};
       Object.values(this.algorithms).forEach(categoryAlgos => {
         categoryAlgos.forEach(algo => {
           algorithmMap[algo.value] = algo.label;
         });
       });

       return algorithmMap[algorithm] || algorithm;
     },
     /** 获取算法标签类型 */
     getAlgorithmTagType(algorithm) {
       if (!algorithm) return '';
       if (algorithm.includes('regression')) return 'primary';
       if (algorithm.includes('classification')) return 'success';
       if (algorithm.includes('clustering')) return 'warning';
       if (algorithm.includes('feature_engineering')) return 'info';
       return '';
     },
     /** 获取状态显示名称 */
     getStatusDisplayName(status) {
       const statusMap = {
         'PENDING': '等待中',
         'RUNNING': '运行中',
         'COMPLETED': '已完成',
         'FAILED': '失败',
         'QUEUED': '队列中'
       };
       return statusMap[status] || status;
     },
     /** 获取状态标签类型 */
     getStatusTagType(status) {
       const statusTypeMap = {
         'PENDING': 'info',
         'RUNNING': 'warning',
         'COMPLETED': 'success',
         'FAILED': 'danger',
         'QUEUED': 'info'
       };
       return statusTypeMap[status] || '';
     },
     /** 获取参数显示名称 */
     getParameterDisplayName(paramKey) {
       const paramMap = {
         'feature_columns': '特征列',
         'target_column': '目标列',
         'degree': '多项式次数',
         'kernel': '核函数',
         'n_estimators': '树的数量',
         'n_neighbors': '邻居数(K)',
         'n_clusters': '聚类数量(K)',
         'sequence_length': '序列长度',
         'hidden_size': '隐藏层大小',
         'num_layers': 'LSTM层数',
         'epochs': '训练轮数',
         'batch_size': '批处理大小',
         'learning_rate': '学习率',
         'dropout': 'Dropout率',
         'num_channels': '通道数',
         'kernel_size': '卷积核大小',
         'column_name': '计算列',
         'depth_column': '深度列',
         'min_depth': '最小深度',
         'max_depth': '最大深度'
       };
       return paramMap[paramKey] || paramKey;
     },
     /** 获取指标显示名称 */
     getMetricDisplayName(metricKey) {
       const metricMap = {
         'r2_score': 'R² 决定系数',
         'mse': '均方误差 (MSE)',
         'rmse': '均方根误差 (RMSE)',
         'mae': '平均绝对误差 (MAE)',
         'accuracy': '准确率',
         'precision': '精确率',
         'recall': '召回率',
         'f1_score': 'F1分数',
         'silhouette_score': '轮廓系数',
         'inertia': '簇内平方和',
         'fractal_dimension': '分形维数'
       };
       return metricMap[metricKey] || metricKey;
     },
     /** 获取指标描述 */
     getMetricDescription(metricKey) {
       const descMap = {
         'r2_score': '越接近1越好',
         'mse': '越小越好',
         'rmse': '越小越好',
         'mae': '越小越好',
         'accuracy': '越高越好',
         'precision': '越高越好',
         'recall': '越高越好',
         'f1_score': '越高越好',
         'silhouette_score': '越接近1越好',
         'inertia': '越小越好',
         'fractal_dimension': '分形复杂度'
       };
       return descMap[metricKey] || '';
     },
     /** 格式化指标值 */
     formatMetricValue(metricKey, value) {
       if (typeof value === 'number') {
         if (metricKey === 'accuracy' || metricKey === 'precision' || metricKey === 'recall' || metricKey === 'f1_score') {
           return (value * 100).toFixed(2) + '%';
         }
         return value.toFixed(4);
       }
       return value;
     },
     /** 获取指标值的CSS类 */
     getMetricValueClass(metricKey, value) {
       if (typeof value !== 'number') return '';

       if (metricKey === 'r2_score') {
         if (value >= 0.8) return 'metric-excellent';
         if (value >= 0.6) return 'metric-good';
         if (value >= 0.3) return 'metric-fair';
         return 'metric-poor';
       }

       if (metricKey === 'accuracy' || metricKey === 'precision' || metricKey === 'recall' || metricKey === 'f1_score') {
         if (value >= 0.9) return 'metric-excellent';
         if (value >= 0.8) return 'metric-good';
         if (value >= 0.7) return 'metric-fair';
         return 'metric-poor';
       }

       return '';
     },
     /** 格式化日期时间 */
     formatDateTime(dateTimeStr) {
       if (!dateTimeStr) return '-';
       try {
         const date = new Date(dateTimeStr);
         return date.toLocaleString('zh-CN', {
           year: 'numeric',
           month: '2-digit',
           day: '2-digit',
           hour: '2-digit',
           minute: '2-digit'
         });
       } catch (e) {
         return dateTimeStr;
       }
     },

     // 简化参数选择器相关方法
     handleParameterModeChange(mode) {
       console.log('参数配置模式切换:', mode);
       if (mode === 'simple') {
         // 切换到简化模式时，保存当前高级配置
         this.saveParameterHistory();
       }
     },

     handleSimplifiedParameterChange(parameters) {
       console.log('简化参数变化:', parameters);
       this.form.parameters = { ...parameters };
     },

     handleParameterApply(parameters) {
       console.log('应用参数配置:', parameters);
       this.form.parameters = { ...parameters };
       this.$message.success('参数配置已应用');
     },

     getDataInfo() {
       // 返回数据集信息，用于参数推荐
       return {
         sampleCount: this.selectedDataset?.rowCount || 1000,
         featureCount: this.availableColumns?.length || 10,
         hasNulls: false // 可以根据实际数据质量信息设置
       };
     },

     /** 获取过滤后的列表格数据 */
     getFilteredColumnTableData() {
       const data = this.getColumnTableData();
       if (!this.columnSearchText) return data;

       return data.filter(item =>
         item.name.toLowerCase().includes(this.columnSearchText.toLowerCase())
       );
     },

     /** 获取列显示宽度 */
     getColumnDisplayWidth(column) {
       const baseWidth = 120;
       const maxWidth = 200;
       const minWidth = 80;

       // 根据列名长度和数据类型调整宽度
       const nameLength = column.length;
       const stats = this.columnStats[column];
       const isNumeric = stats && (stats.type === 'numeric' || stats.type === 'float64');

       let width = baseWidth + nameLength * 3;
       if (isNumeric) width += 20; // 数值列稍宽一些

       return Math.max(minWidth, Math.min(maxWidth, width));
     },

     /** 获取单元格显示类名 */
     getCellDisplayClass(value, column) {
       if (value === null || value === undefined || value === '') {
         return 'cell-null';
       }

       const stats = this.columnStats[column];
       if (stats && stats.type === 'numeric') {
         return 'cell-numeric';
       }

       return 'cell-text';
     },

     /** 格式化单元格显示值 */
     formatCellDisplayValue(value) {
       if (value === null || value === undefined || value === '') {
         return 'NULL';
       }

       if (typeof value === 'number') {
         return Number.isInteger(value) ? value : value.toFixed(4);
       }

       return String(value);
     },

     /** 获取列类型显示文本 */
     getColumnTypeDisplay(type) {
       const typeMap = {
         'numeric': '数值',
         'float64': '浮点',
         'int64': '整数',
         'object': '文本',
         'string': '字符串',
         'datetime': '日期',
         'boolean': '布尔'
       };
       return typeMap[type] || type;
     },

     /** 获取缺失率样式类 */
     getMissingRateClass(rate) {
       if (rate === undefined || rate === null) return '';
       if (rate === 0) return 'missing-rate-good';
       if (rate < 0.05) return 'missing-rate-ok';
       if (rate < 0.2) return 'missing-rate-warning';
       return 'missing-rate-danger';
     },

     /** 格式化百分比 */
     formatPercentage(value) {
       if (value === undefined || value === null) return '-';
       return (value * 100).toFixed(1) + '%';
     },

     /** 格式化数字 */
     formatNumber(value) {
       if (value === undefined || value === null) return '-';
       return value.toLocaleString();
     },

     /** 获取数据完整性 */
     getDataCompleteness() {
       if (!this.availableColumns.length) return 100;

       let totalCells = 0;
       let nonNullCells = 0;

       this.availableColumns.forEach(col => {
         const stats = this.columnStats[col];
         if (stats) {
           totalCells += (stats.non_null_count || 0) + (stats.null_count || 0);
           nonNullCells += stats.non_null_count || 0;
         }
       });

       return totalCells > 0 ? Math.round((nonNullCells / totalCells) * 100) : 100;
     },

     /** 获取数据一致性 */
     getDataConsistency() {
       // 简单的一致性评估：基于数据类型的一致性
       if (!this.availableColumns.length) return 100;

       let consistentColumns = 0;
       this.availableColumns.forEach(col => {
         const stats = this.columnStats[col];
         if (stats && stats.type && stats.type !== 'unknown') {
           consistentColumns++;
         }
       });

       return Math.round((consistentColumns / this.availableColumns.length) * 100);
     },

     /** 获取质量分析 */
     getQualityAnalysis() {
       return this.availableColumns.map(col => {
         const stats = this.columnStats[col];
         const issues = [];
         let qualityLevel = 'success';
         let qualityText = '良好';

         if (!stats) {
           issues.push('缺少统计信息');
           qualityLevel = 'danger';
           qualityText = '异常';
         } else {
           const nullRate = stats.null_count / (stats.non_null_count + stats.null_count);

           if (nullRate > 0.2) {
             issues.push(`缺失值过多 (${this.formatPercentage(nullRate)})`);
             qualityLevel = 'danger';
             qualityText = '差';
           } else if (nullRate > 0.05) {
             issues.push(`存在缺失值 (${this.formatPercentage(nullRate)})`);
             qualityLevel = 'warning';
             qualityText = '一般';
           }

           if (stats.unique_count === 1) {
             issues.push('所有值相同');
             qualityLevel = 'warning';
             qualityText = '一般';
           }

           if (issues.length === 0) {
             issues.push('数据质量良好');
           }
         }

         return {
           name: col,
           issues,
           qualityLevel,
           qualityText
         };
       }).slice(0, 10); // 只显示前10列
     }
   },
   mounted() {
     // 确保数据集列表已加载
     if (!this.availableDatasets || this.availableDatasets.length === 0) {
       console.log('mounted: 重新加载数据集列表');
       this.loadAvailableDatasets();
     }
   },
   beforeDestroy() {
    // 组件销毁前清除所有定时器
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
    }
    if (this.listPollingTimer) {
      clearInterval(this.listPollingTimer);
    }
    if (this.datasetChangeTimer) {
      clearTimeout(this.datasetChangeTimer);
    }
  }
};
</script>

<style scoped>
.box-card {
  margin-bottom: 20px;
}
.image-slot {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
    background: #f5f7fa;
    color: #909399;
    font-size: 30px;
}

/* 指标卡片样式 */
.metric-card {
  text-align: center;
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
  margin-bottom: 15px;
  transition: all 0.3s ease;
}

.metric-card:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.metric-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

.metric-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
  transition: color 0.3s ease;
}

.metric-description {
  font-size: 12px;
  color: #909399;
}

/* 指标值颜色 */
.metric-excellent {
  color: #67c23a;
}

.metric-good {
  color: #409eff;
}

.metric-fair {
  color: #e6a23c;
}

.metric-poor {
  color: #f56c6c;
}

/* 统计卡片样式 */
.stat-card {
  position: relative;
  padding: 20px;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.stat-card.running {
  background: linear-gradient(135deg, #ffeaa7 0%, #fab1a0 100%);
}

.stat-card.completed {
  background: linear-gradient(135deg, #a8e6cf 0%, #81c784 100%);
}

.stat-card.failed {
  background: linear-gradient(135deg, #ffb3ba 0%, #ff8a80 100%);
}

.stat-content {
  position: relative;
  z-index: 2;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #34495e;
  font-weight: 500;
}

.stat-icon {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 40px;
  color: rgba(255, 255, 255, 0.3);
  z-index: 1;
}

/* 参数配置模式选择器样式 */
.parameter-mode-selector {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.mode-description {
  color: #666;
  font-size: 13px;
}

.simplified-parameter-section {
  margin-top: 15px;
}

.advanced-parameter-section {
  margin-top: 15px;
}

/* 数据集预览样式 */
.data-preview-controls {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.cell-null {
  color: #f56c6c;
  font-style: italic;
}

.cell-numeric {
  color: #409eff;
  font-family: 'Courier New', monospace;
}

.cell-text {
  color: #333;
}

.missing-rate-good {
  color: #67c23a;
  font-weight: bold;
}

.missing-rate-ok {
  color: #e6a23c;
}

.missing-rate-warning {
  color: #f56c6c;
}

.missing-rate-danger {
  color: #f56c6c;
  font-weight: bold;
}

.quality-card {
  height: 100%;
}

.quality-metrics {
  padding: 10px 0;
}

.metric-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.metric-label {
  width: 120px;
  font-weight: 500;
  color: #666;
}

.metric-value {
  font-weight: bold;
  color: #409eff;
}

.column-quality-list {
  max-height: 300px;
  overflow-y: auto;
}

.quality-item {
  padding: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.quality-item:last-child {
  border-bottom: none;
}

.quality-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.column-name {
  font-weight: 500;
  color: #333;
}

.quality-item-details {
  font-size: 12px;
  color: #666;
}

.quality-issue {
  display: inline-block;
  margin-right: 10px;
}

.stats-display {
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}
</style>
