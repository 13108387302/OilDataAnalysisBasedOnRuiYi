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
        
        <el-form-item v-if="!form.id" label="上传数据文件" prop="file">
          <el-upload ref="upload" action="#" :on-change="handleFileChange" :on-remove="handleFileRemove" :file-list="uploadFileList" :auto-upload="false" :limit="1">
            <el-button size="small" type="primary">选取文件</el-button>
          </el-upload>
        </el-form-item>

        <!-- 动态参数表单 -->
        <div v-for="param in currentAlgorithmParams" :key="param.key">
            <el-form-item :label="param.label" :prop="'parameters.' + param.key" :rules="param.rules">
                <el-select v-if="param.type === 'select_column'" v-model="form.parameters[param.key]" :placeholder="'请选择' + param.label" filterable style="width: 100%;" @change="onParameterChange(param.key, $event)"><el-option v-for="col in availableColumns" :key="col" :label="col" :value="col"></el-option></el-select>
                <el-select v-else-if="param.type === 'multi_select_column'" v-model="form.parameters[param.key]" :placeholder="'请选择' + param.label" filterable multiple collapse-tags style="width: 100%;"><el-option v-for="col in availableColumns" :key="col" :label="col" :value="col"></el-option></el-select>
                <el-select v-else-if="param.type === 'select_option'" v-model="form.parameters[param.key]" :placeholder="'请选择' + param.label" style="width: 100%;"><el-option v-for="opt in param.options" :key="opt" :label="opt" :value="opt"></el-option></el-select>
                <el-input-number v-else-if="param.type === 'number'" v-model="form.parameters[param.key]" :min="param.min" :max="param.max" :step="param.step || 1" :placeholder="param.placeholder || ('请输入' + param.label)" controls-position="right" style="width: 100%;"></el-input-number>
                <el-input v-else v-model="form.parameters[param.key]" :placeholder="'请输入' + param.label" />
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
      <el-table v-loading="loading" :data="taskList">
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
  </div>
</template>

<script>
import { addTask, listTask, getTask, updateTask, delTask, analyzeFile } from "@/api/petrol/task";

const newForm = () => ({
  id: undefined,
  taskName: '',
  taskType: undefined,
  algorithm: undefined,
  parameters: {},
  file: null,
  remark: ''
});

export default {
  name: "PetrolTask",
   data() {
     return {
       loading: true,
       submitLoading: false,
       total: 0,
       taskList: [],
       queryParams: { pageNum: 1, pageSize: 10 },
       uploadFileList: [],
       availableColumns: [],
       columnStats: {}, // 保存列统计信息
       form: newForm(),
       pollingTimer: null,
       pollingTaskId: null,
       listPollingTimer: null,
       autoRefreshEnabled: true,
       lastRefreshTime: null,
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
            { value: "regression_svm_train", label: "支持向量机回归(SVR)", params: [ { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] }, { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "目标列不能为空", trigger: "change" }] }, { key: 'kernel', label: '核函数', type: 'select_option', options: ['linear', 'poly', 'rbf', 'sigmoid'], defaultValue: 'rbf' }, ] },
            { value: "regression_random_forest_train", label: "随机森林回归", params: [ { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] }, { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "目标列不能为空", trigger: "change" }] }, { key: 'n_estimators', label: '树的数量', type: 'number', defaultValue: 100 } ] },
            { value: "regression_lightgbm_train", label: "LightGBM回归", params: [ { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] }, { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "目标列不能为空", trigger: "change" }] } ] },
            { value: "regression_xgboost_train", label: "XGBoost回归", params: [ { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] }, { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "目标列不能为空", trigger: "change" }] } ] },
            { value: "regression_bilstm_train", label: "BiLSTM时序回归 (PyTorch)", params: [
                { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "请选择一个或多个特征列", trigger: "change" }] },
                { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "请选择目标列", trigger: "change" }] },
                { key: 'sequence_length', label: '序列长度', type: 'number', defaultValue: 10, min: 1, rules: [{ required: true, message: "序列长度不能为空", trigger: "blur" }] },
                { key: 'hidden_size', label: '隐藏层大小', type: 'number', defaultValue: 128, min: 16, rules: [{ required: true, message: "隐藏层大小不能为空", trigger: "blur" }] },
                { key: 'num_layers', label: 'LSTM层数', type: 'number', defaultValue: 2, min: 1, rules: [{ required: true, message: "LSTM层数不能为空", trigger: "blur" }] },
                { key: 'epochs', label: '训练轮数', type: 'number', defaultValue: 50, min: 10, rules: [{ required: true, message: "训练轮数不能为空", trigger: "blur" }] },
                { key: 'batch_size', label: '批处理大小', type: 'number', defaultValue: 32, min: 1, rules: [{ required: true, message: "批处理大小不能为空", trigger: "blur" }] },
                { key: 'learning_rate', label: '学习率', type: 'number', defaultValue: 0.001, min: 0.0001, step: 0.0001, rules: [{ required: true, message: "学习率不能为空", trigger: "blur" }] },
                { key: 'dropout', label: 'Dropout率', type: 'number', defaultValue: 0.2, min: 0, max: 0.9, step: 0.05, rules: [{ required: true, message: "Dropout率不能为空", trigger: "blur" }] }
            ] },
            { value: "regression_tcn_train", label: "TCN时序回归 (PyTorch)", params: [
                { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "请选择一个或多个特征列", trigger: "change" }] },
                { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "请选择目标列", trigger: "change" }] },
                { key: 'sequence_length', label: '序列长度', type: 'number', defaultValue: 60, min: 1, rules: [{ required: true, message: "序列长度不能为空", trigger: "blur" }] },
                { key: 'epochs', label: '训练轮数', type: 'number', defaultValue: 100, min: 10, rules: [{ required: true, message: "训练轮数不能为空", trigger: "blur" }] },
                { key: 'batch_size', label: '批处理大小', type: 'number', defaultValue: 32, min: 1, rules: [{ required: true, message: "批处理大小不能为空", trigger: "blur" }] },
                { key: 'learning_rate', label: '学习率', type: 'number', defaultValue: 0.001, min: 0.0001, step: 0.0001, rules: [{ required: true, message: "学习率不能为空", trigger: "blur" }] },
                { key: 'num_channels', label: '通道数', type: 'number', defaultValue: 64, min: 16, rules: [{ required: true, message: "通道数不能为空", trigger: "blur" }] },
                { key: 'kernel_size', label: '卷积核大小', type: 'number', defaultValue: 3, min: 2, rules: [{ required: true, message: "卷积核大小不能为空", trigger: "blur" }] },
                { key: 'dropout', label: 'Dropout率', type: 'number', defaultValue: 0.2, min: 0, max: 0.9, step: 0.05, rules: [{ required: true, message: "Dropout率不能为空", trigger: "blur" }] }
            ] }
         ],
         classification: [
            { value: "classification_svm_train", label: "支持向量机分类(SVC)", params: [ { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] }, { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "目标列不能为空", trigger: "change" }] }, { key: 'kernel', label: '核函数', type: 'select_option', options: ['linear', 'poly', 'rbf', 'sigmoid'], defaultValue: 'rbf' } ] },
            { value: "classification_knn_train", label: "K-近邻分类", params: [ { key: 'feature_columns', label: '特征列(X)', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] }, { key: 'target_column', label: '目标列(Y)', type: 'select_column', rules: [{ required: true, message: "目标列不能为空", trigger: "change" }] }, { key: 'n_neighbors', label: '邻居数(K)', type: 'number', defaultValue: 5, min: 1, rules: [{ required: true, message: "邻居数不能为空", trigger: "blur" }] } ] }
         ],
         clustering: [
            { value: "clustering_kmeans_train", label: "K-Means聚类", params: [ { key: 'feature_columns', label: '特征列', type: 'multi_select_column', rules: [{ required: true, message: "特征列不能为空", trigger: "change" }] }, { key: 'n_clusters', label: '聚类数量(K)', type: 'number', defaultValue: 3, min: 2, rules: [{ required: true, message: "K值不能为空", trigger: "blur" }] } ] },
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
     this.startListPolling();
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
       this.uploadFileList = [];
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
             
             if (this.uploadFileList.length > 0) {
               formData.append("file", this.uploadFileList[0].raw);
             } else {
               this.$modal.msgError("请选择要上传的数据文件");
               this.submitLoading = false;
               return;
             }
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
       this.$modal.confirm('是否确认删除任务编号为"' + taskIds + '"的数据项？').then(function() {
         return delTask(taskIds);
       }).then(() => {
         this.getList();
         this.$modal.msgSuccess("删除成功");
       }).catch(() => {});
     },
     handleTaskTypeChange() {
       this.form.algorithm = undefined;
       this.form.parameters = {};
     },
     handleAlgorithmChange() {
        this.form.parameters = {};
        this.currentAlgorithmParams.forEach(param => {
          if (param.defaultValue !== undefined) {
            this.$set(this.form.parameters, param.key, param.defaultValue);
          }
        });
     },
     handleFileChange(file, fileList) {
       this.form.file = file.raw;
       if (file.raw) {
         analyzeFile(file.raw).then(response => {
           this.availableColumns = response.data.headers;
           this.columnStats = response.data.stats || {}; // 保存列统计信息
         }).catch(err => {
           this.$modal.msgError("文件解析失败: " + err.message);
           this.availableColumns = [];
           this.columnStats = {};
         });
       }
       this.uploadFileList = fileList;
     },
     handleFileRemove() {
       this.form.file = null;
       this.availableColumns = [];
       this.columnStats = {};
       this.form.parameters = {};
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
</style>
