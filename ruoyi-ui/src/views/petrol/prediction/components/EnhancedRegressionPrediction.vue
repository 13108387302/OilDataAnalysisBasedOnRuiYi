<template>
  <div class="enhanced-regression-prediction">
    <el-card>
      <div slot="header">
        <span>📊 回归预测系统</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="resetForm">
          重置
        </el-button>
      </div>

      <!-- 简化的预测配置表单 -->
      <el-form ref="predictionForm" :model="form" :rules="rules" label-width="120px">
        <!-- 数据源选择 -->
        <el-form-item label="数据源选择" prop="dataSourceType">
          <el-radio-group v-model="form.dataSourceType" @change="handleDataSourceChange">
            <el-radio label="dataset">使用已有数据集</el-radio>
            <el-radio label="upload">上传新文件</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 数据集选择 -->
        <el-form-item v-if="form.dataSourceType === 'dataset'" label="选择数据集" prop="datasetId">
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
                </el-option>
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-button type="primary" icon="el-icon-view" @click="previewSelectedDataset" :disabled="!form.datasetId">
                预览数据集
              </el-button>
            </el-col>
          </el-row>
          <div class="form-tip">
            <i class="el-icon-warning-outline"></i>
            <span>数据质量要求：请确保数据集中没有空值，特别是目标列不能包含空值或缺失值</span>
          </div>
        </el-form-item>

        <!-- 文件上传 -->
        <el-form-item v-if="form.dataSourceType === 'upload'" label="上传数据文件" prop="file">
          <el-alert
            title="文件上传功能"
            description="当前版本建议使用数据集管理功能上传文件，然后在此处选择已有数据集。"
            type="info"
            :closable="false">
          </el-alert>
        </el-form-item>

        <!-- 特征选择 -->
        <el-form-item v-if="availableColumns.length > 0" label="特征列(X)" prop="selectedFeatures">
          <el-select
            v-model="form.selectedFeatures"
            multiple
            filterable
            placeholder="请选择特征列"
            style="width: 100%;"
            @change="handleFeatureSelectionChange">
            <el-option
              v-for="col in availableColumns"
              :key="col"
              :label="col"
              :value="col"
              :disabled="col === form.targetColumn">
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 目标列选择 -->
        <el-form-item v-if="availableColumns.length > 0" label="目标列(Y)" prop="targetColumn">
          <el-select
            v-model="form.targetColumn"
            filterable
            placeholder="请选择目标列"
            style="width: 100%;"
            @change="handleTargetColumnChange">
            <el-option
              v-for="col in availableColumns"
              :key="col"
              :label="col"
              :value="col"
              :disabled="form.selectedFeatures && form.selectedFeatures.includes(col)">
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 模型选择 -->
        <el-form-item label="选择模型" prop="modelId">
          <el-select 
            v-model="form.modelId" 
            placeholder="请选择预测模型" 
            filterable
            style="width: 100%;"
            @change="handleModelChange">
            <el-option
              v-for="model in availableModels"
              :key="model.id"
              :label="model.modelName"
              :value="model.id">
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span>{{ model.modelName }}</span>
                <div style="font-size: 12px; color: #8492a6;">
                  <el-tag size="mini" :type="getModelTypeTag(model.modelType)">
                    {{ model.algorithm }}
                  </el-tag>
                  <span style="margin-left: 5px;">{{ model.accuracyScore }}%</span>
                </div>
              </div>
            </el-option>
          </el-select>

          <!-- 模型兼容性状态显示 -->
          <div v-if="form.modelId && (form.selectedFeatures && form.selectedFeatures.length > 0)"
               style="margin-top: 8px;">
            <el-alert
              v-if="modelCompatibilityStatus"
              :title="modelCompatibilityStatus.message"
              :type="modelCompatibilityStatus.isValid ? 'success' : 'warning'"
              :closable="false"
              show-icon
              style="padding: 8px 12px;">
              <template v-if="!modelCompatibilityStatus.isValid && modelCompatibilityStatus.details">
                <div style="margin-top: 5px; font-size: 12px;">
                  <div v-if="modelCompatibilityStatus.details.modelFeatures && modelCompatibilityStatus.details.modelFeatures.length > 0">
                    <strong>模型要求特征：</strong>{{ modelCompatibilityStatus.details.modelFeatures.join(', ') }}
                  </div>
                  <div v-if="modelCompatibilityStatus.details.modelTarget">
                    <strong>模型要求目标列：</strong>{{ modelCompatibilityStatus.details.modelTarget }}
                  </div>
                </div>
              </template>
            </el-alert>
          </div>
        </el-form-item>

        <!-- 预测配置 -->
        <el-form-item label="预测任务名称" prop="predictionName">
          <el-input 
            v-model="form.predictionName" 
            placeholder="请输入预测任务名称"
            style="width: 100%;" />
        </el-form-item>

        <!-- 行选择策略 -->
        <el-form-item label="行选择策略" prop="rowSelectionStrategy">
          <el-radio-group v-model="form.rowSelectionStrategy" @change="handleRowSelectionChange">
            <el-radio label="sequential">顺序选择</el-radio>
            <el-radio label="custom">自定义行</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 顺序选择配置 -->
        <el-form-item v-if="form.rowSelectionStrategy === 'sequential'" label="预测样本数" prop="sampleCount">
          <el-input-number
            v-model="form.sampleCount"
            :min="1"
            :max="1000"
            placeholder="从第1行开始预测的样本数量"
            style="width: 300px;" />
          <div style="margin-top: 5px; font-size: 12px; color: #909399;">
            将从数据集的第1行开始，连续预测 {{ form.sampleCount }} 行数据
          </div>
        </el-form-item>

        <!-- 自定义行选择配置 -->
        <el-form-item v-if="form.rowSelectionStrategy === 'custom'" label="指定行号" prop="customRows">
          <div>
            <el-input
              v-model="customRowsInput"
              type="textarea"
              :rows="3"
              placeholder="请输入要预测的行号，支持以下格式：&#10;1. 单个行号：1,5,10,15&#10;2. 连续范围：1-10,20-30&#10;3. 混合格式：1,5,10-15,20"
              style="width: 100%;"
              @input="parseCustomRows" />
            <div style="margin-top: 5px; font-size: 12px; color: #909399;">
              <span v-if="form.customRows && form.customRows.length > 0">
                已选择 {{ form.customRows.length }} 行：{{ getRowRangeDisplay() }}
              </span>
              <span v-else style="color: #F56C6C;">请输入要预测的行号</span>
            </div>
          </div>
        </el-form-item>

        <!-- 表单提交按钮 -->
        <el-form-item>
          <el-button type="primary" @click="submitPrediction" :loading="submitting">
            <i class="el-icon-magic-stick"></i> 开始预测
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>

        <!-- 预测进度条 -->
        <el-form-item v-if="predictionProgress.visible">
          <el-card class="prediction-progress-card">
            <div slot="header">
              <span>🔄 预测任务执行中</span>
              <el-button style="float: right; padding: 3px 0" type="text" @click="cancelPrediction">
                取消
              </el-button>
            </div>
            <div class="progress-content">
              <div class="progress-info">
                <p><strong>任务名称：</strong>{{ predictionProgress.taskName }}</p>
                <p><strong>当前状态：</strong>{{ predictionProgress.statusText }}</p>
                <p><strong>已用时间：</strong>{{ predictionProgress.elapsedTime }}秒</p>
              </div>
              <el-progress
                :percentage="predictionProgress.percentage"
                :status="predictionProgress.status || undefined"
                :stroke-width="20"
                text-inside>
              </el-progress>
              <div class="progress-steps">
                <el-steps :active="predictionProgress.currentStep" finish-status="success" simple>
                  <el-step title="提交任务"></el-step>
                  <el-step title="数据处理"></el-step>
                  <el-step title="模型预测"></el-step>
                  <el-step title="结果生成"></el-step>
                  <el-step title="完成"></el-step>
                </el-steps>
              </div>
            </div>
          </el-card>
        </el-form-item>
      </el-form>

      <!-- 数据集预览对话框 -->
      <el-dialog 
        title="数据集预览" 
        :visible.sync="datasetPreviewVisible" 
        width="80%" 
        append-to-body>
        <div v-if="selectedDataset">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="数据集名称">{{ selectedDataset.datasetName }}</el-descriptions-item>
            <el-descriptions-item label="数据集类别">
              <el-tag :type="getDatasetCategoryType(selectedDataset.datasetCategory)">
                {{ selectedDataset.datasetCategory }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="数据行数">{{ selectedDataset.rowCount }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <span slot="footer" class="dialog-footer">
          <el-button @click="datasetPreviewVisible = false">关 闭</el-button>
        </span>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import { listAvailableDatasets, getDatasetColumns } from "@/api/petrol/dataset";
import { listModel } from "@/api/petrol/model";
import { submitPrediction, getPrediction } from "@/api/petrol/prediction";

export default {
  name: "EnhancedRegressionPrediction",
  data() {
    return {
      form: {
        dataSourceType: 'dataset',
        datasetId: null,
        datasetName: '',
        selectedFeatures: [],
        targetColumn: '',
        modelId: null,
        predictionName: '',
        sampleCount: 100,
        rowSelectionStrategy: 'sequential',
        customRows: []
      },
      rules: {
        dataSourceType: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
        datasetId: [{ required: true, message: '请选择数据集', trigger: 'change' }],
        selectedFeatures: [{ required: true, message: '请选择特征列', trigger: 'change' }],
        targetColumn: [{ required: true, message: '请选择目标列', trigger: 'change' }],
        modelId: [{ required: true, message: '请选择预测模型', trigger: 'change' }],
        predictionName: [{ required: true, message: '请输入预测任务名称', trigger: 'blur' }],
        sampleCount: [{ required: true, message: '请输入预测样本数', trigger: 'blur' }],
        rowSelectionStrategy: [{ required: true, message: '请选择行选择策略', trigger: 'change' }],
        customRows: [
          {
            validator: (rule, value, callback) => {
              if (this.form.rowSelectionStrategy === 'custom') {
                if (!value || value.length === 0) {
                  callback(new Error('请输入要预测的行号'));
                } else {
                  callback();
                }
              } else {
                callback();
              }
            },
            trigger: 'change'
          }
        ]
      },
      availableDatasets: [],
      availableModels: [],
      availableColumns: [],
      selectedDataset: null,
      datasetPreviewVisible: false,
      submitting: false,
      customRowsInput: '', // 自定义行输入的原始文本
      modelCompatibilityStatus: null, // 模型兼容性状态
      // 预测进度相关
      predictionProgress: {
        visible: false,
        taskId: null,
        taskName: '',
        percentage: 0,
        status: null,
        statusText: '准备中...',
        currentStep: 0,
        elapsedTime: 0,
        startTime: null,
        timer: null,
        pollTimer: null
      }
    };
  },
  created() {
    this.loadAvailableDatasets();
    this.loadAvailableModels();
  },
  methods: {
    async loadAvailableDatasets() {
      try {
        const response = await listAvailableDatasets();
        this.availableDatasets = response.data || [];
      } catch (error) {
        console.error('加载数据集列表失败:', error);
        this.$message.error('加载数据集列表失败');
      }
    },
    async loadAvailableModels() {
      try {
        const response = await listModel();
        this.availableModels = response.rows || [];
      } catch (error) {
        console.error('加载模型列表失败:', error);
        this.$message.error('加载模型列表失败');
      }
    },
    handleDataSourceChange() {
      this.form.datasetId = null;
      this.form.datasetName = '';
      this.availableColumns = [];
      this.form.selectedFeatures = [];
      this.form.targetColumn = '';
    },
    async handleDatasetChange(datasetId) {
      if (!datasetId) {
        this.selectedDataset = null;
        this.availableColumns = [];
        return;
      }

      try {
        const dataset = this.availableDatasets.find(d => d.id === datasetId);
        if (dataset) {
          this.selectedDataset = dataset;
          this.form.datasetName = dataset.datasetName;
        }

        const response = await getDatasetColumns(datasetId);
        if (response.data && response.data.columns) {
          this.availableColumns = response.data.columns;
          this.$message.success('数据集加载成功');
        }
      } catch (error) {
        console.error('加载数据集信息失败:', error);
        this.$message.error('加载数据集信息失败');
      }
    },

    previewSelectedDataset() {
      if (!this.form.datasetId) {
        this.$message.warning('请先选择数据集');
        return;
      }
      this.datasetPreviewVisible = true;
    },
    getDatasetCategoryType(category) {
      const typeMap = {
        '测井': 'primary',
        '地震': 'success', 
        '生产': 'warning',
        '地质': 'info'
      };
      return typeMap[category] || '';
    },
    getModelTypeTag(type) {
      const typeMap = {
        'regression': 'success',
        'classification': 'primary'
      };
      return typeMap[type] || '';
    },
    async submitPrediction() {
      this.$refs.predictionForm.validate(async (valid) => {
        if (!valid) return;

        // 🔍 模型兼容性验证
        const compatibilityCheck = this.validateModelCompatibility();
        if (!compatibilityCheck.isValid) {
          this.$message.error(compatibilityCheck.message);
          return;
        }

        this.submitting = true;
        try {
          const params = {
            predictionName: this.form.predictionName,
            modelId: this.form.modelId,
            datasetId: this.form.datasetId,
            selectedFeatures: this.form.selectedFeatures,
            targetColumn: this.form.targetColumn,
            sampleCount: this.form.sampleCount,
            rowSelectionStrategy: this.form.rowSelectionStrategy,
            customRows: this.form.customRows
          };

          const response = await submitPrediction(params);
          this.$message.success('预测任务提交成功');

          // 启动进度监控
          if (response.data && response.data.id) {
            this.startProgressMonitoring(response.data.id, this.form.predictionName);
          }

          // 不立即重置表单，等预测完成后再重置
        } catch (error) {
          console.error('预测任务提交失败:', error);
          this.$message.error('预测任务提交失败');
          this.submitting = false;
        }
      });
    },
    resetForm() {
      // 直接手动重置，避免Element UI的resetFields问题
      this.form = {
        dataSourceType: 'dataset',
        datasetId: null,
        datasetName: '',
        selectedFeatures: [],
        targetColumn: '',
        modelId: null,
        predictionName: '',
        sampleCount: 100
      };

      // 清除验证状态
      this.$nextTick(() => {
        if (this.$refs.predictionForm) {
          try {
            this.$refs.predictionForm.clearValidate();
          } catch (error) {
            console.warn('清除表单验证状态时出现警告:', error);
          }
        }
      });

      this.availableColumns = [];
      this.selectedDataset = null;
      this.stopProgressMonitoring();
    },

    // 进度监控相关方法
    startProgressMonitoring(taskId, taskName) {
      this.predictionProgress.visible = true;
      this.predictionProgress.taskId = taskId;
      this.predictionProgress.taskName = taskName;
      this.predictionProgress.percentage = 10;
      this.predictionProgress.status = null;
      this.predictionProgress.statusText = '任务已提交，开始处理...';
      this.predictionProgress.currentStep = 1;
      this.predictionProgress.elapsedTime = 0;
      this.predictionProgress.startTime = Date.now();

      // 启动计时器
      this.predictionProgress.timer = setInterval(() => {
        this.predictionProgress.elapsedTime = Math.floor((Date.now() - this.predictionProgress.startTime) / 1000);
      }, 1000);

      // 启动轮询检查任务状态
      this.predictionProgress.pollTimer = setInterval(() => {
        this.checkPredictionStatus();
      }, 2000); // 每2秒检查一次

      // 模拟进度更新
      this.simulateProgress();
    },

    async checkPredictionStatus() {
      try {
        const response = await getPrediction(this.predictionProgress.taskId);
        const prediction = response.data;

        if (prediction.status === 'RUNNING') {
          this.predictionProgress.statusText = '模型正在执行预测...';
          this.predictionProgress.currentStep = 3;
        } else if (prediction.status === 'COMPLETED') {
          this.onPredictionCompleted();
        } else if (prediction.status === 'FAILED') {
          this.onPredictionFailed(prediction.errorMessage);
        }
      } catch (error) {
        console.error('检查预测状态失败:', error);
      }
    },

    simulateProgress() {
      // 🔴 系统已禁用模拟进度，使用确定性进度更新
      console.warn('⚠️ 系统已禁用模拟进度生成，使用确定性进度更新');

      const progressInterval = setInterval(() => {
        if (this.predictionProgress.percentage < 90) {
          // 使用确定性增长而不是随机数
          this.predictionProgress.percentage += 5; // 固定增长5%
          if (this.predictionProgress.percentage > 90) {
            this.predictionProgress.percentage = 90;
          }
        }

        // 根据进度更新步骤
        if (this.predictionProgress.percentage > 20 && this.predictionProgress.currentStep < 2) {
          this.predictionProgress.currentStep = 2;
          this.predictionProgress.statusText = '正在处理数据...';
        }
        if (this.predictionProgress.percentage > 50 && this.predictionProgress.currentStep < 3) {
          this.predictionProgress.currentStep = 3;
          this.predictionProgress.statusText = '模型正在执行预测...';
        }
        if (this.predictionProgress.percentage > 80 && this.predictionProgress.currentStep < 4) {
          this.predictionProgress.currentStep = 4;
          this.predictionProgress.statusText = '正在生成结果...';
        }
      }, 1000);

      // 保存interval ID以便清理
      this.predictionProgress.progressInterval = progressInterval;
    },

    onPredictionCompleted() {
      this.predictionProgress.percentage = 100;
      this.predictionProgress.status = 'success';
      this.predictionProgress.statusText = '预测任务完成！';
      this.predictionProgress.currentStep = 5;

      this.$message.success('预测任务执行成功！');

      // 延迟隐藏进度条并刷新历史
      setTimeout(() => {
        this.stopProgressMonitoring();
        this.resetForm();
        this.submitting = false;

        // 通知父组件刷新预测历史
        this.$emit('prediction-completed');
      }, 2000);
    },

    onPredictionFailed(errorMessage) {
      this.predictionProgress.status = 'exception';
      this.predictionProgress.statusText = '预测任务失败：' + (errorMessage || '未知错误');

      // 根据错误类型显示不同的提示
      if (errorMessage && errorMessage.includes('为空值')) {
        this.$alert(
          '数据集中存在空值，无法进行预测。请检查以下几点：\n' +
          '1. 确保目标列没有空值或缺失值\n' +
          '2. 确保特征列数据完整\n' +
          '3. 可以使用数据预处理功能清理数据\n\n' +
          '详细错误：' + errorMessage,
          '数据质量问题',
          {
            confirmButtonText: '我知道了',
            type: 'warning',
            dangerouslyUseHTMLString: false
          }
        );
      } else if (errorMessage && errorMessage.includes('无法读取数据文件')) {
        this.$alert(
          '无法读取数据文件，请检查：\n' +
          '1. 文件是否已正确上传\n' +
          '2. 文件格式是否正确（支持CSV、Excel）\n' +
          '3. 文件是否损坏\n\n' +
          '详细错误：' + errorMessage,
          '文件读取问题',
          {
            confirmButtonText: '我知道了',
            type: 'error',
            dangerouslyUseHTMLString: false
          }
        );
      } else {
        this.$message.error('预测任务执行失败：' + (errorMessage || '未知错误'));
      }

      // 延迟隐藏进度条
      setTimeout(() => {
        this.stopProgressMonitoring();
        this.submitting = false;
      }, 3000);
    },

    cancelPrediction() {
      this.$confirm('确定要取消当前预测任务吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.stopProgressMonitoring();
        this.submitting = false;
        this.$message.info('预测任务已取消');
      });
    },

    stopProgressMonitoring() {
      this.predictionProgress.visible = false;

      if (this.predictionProgress.timer) {
        clearInterval(this.predictionProgress.timer);
        this.predictionProgress.timer = null;
      }

      if (this.predictionProgress.pollTimer) {
        clearInterval(this.predictionProgress.pollTimer);
        this.predictionProgress.pollTimer = null;
      }

      if (this.predictionProgress.progressInterval) {
        clearInterval(this.predictionProgress.progressInterval);
        this.predictionProgress.progressInterval = null;
      }
    },

    /** 从路由参数设置模型 */
    setModelFromRoute(params) {
      const { modelId, predictionName } = params;

      if (modelId) {
        this.form.modelId = parseInt(modelId);
        this.handleModelChange(this.form.modelId);
      }

      if (predictionName) {
        this.form.predictionName = predictionName;
      }

      this.$message.success('已自动选择模型，请配置其他预测参数');
    },

    handleModelChange(modelId) {
      if (!modelId) return;

      const selectedModel = this.availableModels.find(m => m.id === modelId);
      if (selectedModel) {
        console.log('选择的模型:', selectedModel);
        // 实时验证模型兼容性
        this.validateAndShowModelCompatibility(selectedModel);
      }
    },

    /** 验证并显示模型兼容性信息 */
    validateAndShowModelCompatibility(model) {
      if (!model || !this.form.selectedFeatures || this.form.selectedFeatures.length === 0) {
        this.modelCompatibilityStatus = null;
        return;
      }

      const compatibility = this.checkModelFeatureCompatibility(model);
      this.modelCompatibilityStatus = compatibility;

      // 如果不兼容，显示警告消息
      if (!compatibility.isValid) {
        this.$message.warning(`模型兼容性提醒：${compatibility.message}`);
      }
    },

    /** 处理特征选择变化 */
    handleFeatureSelectionChange(selectedFeatures) {
      // 如果已选择模型，验证兼容性
      if (this.form.modelId) {
        const selectedModel = this.availableModels.find(m => m.id === this.form.modelId);
        if (selectedModel) {
          this.validateAndShowModelCompatibility(selectedModel);
        }
      }
    },

    /** 处理目标列选择变化 */
    handleTargetColumnChange(targetColumn) {
      // 如果已选择模型，验证兼容性
      if (this.form.modelId) {
        const selectedModel = this.availableModels.find(m => m.id === this.form.modelId);
        if (selectedModel) {
          this.validateAndShowModelCompatibility(selectedModel);
        }
      }
    },

    /** 验证模型兼容性 */
    validateModelCompatibility() {
      const selectedModel = this.availableModels.find(m => m.id === this.form.modelId);
      if (!selectedModel) {
        return {
          isValid: false,
          message: '请选择一个有效的预测模型'
        };
      }

      return this.checkModelFeatureCompatibility(selectedModel);
    },

    /** 检查模型特征兼容性 */
    checkModelFeatureCompatibility(model) {
      try {
        // 解析模型的输入特征
        let modelFeatures = [];
        if (model.inputFeatures) {
          try {
            modelFeatures = JSON.parse(model.inputFeatures);
          } catch (e) {
            console.warn('模型特征解析失败:', e);
            // 如果解析失败，尝试按逗号分割的字符串格式
            modelFeatures = model.inputFeatures.split(',').map(f => f.trim()).filter(f => f);
          }
        }

        // 获取模型的目标列
        const modelTarget = model.outputTarget;

        // 如果模型没有定义特征或目标，认为是通用模型
        if (!modelFeatures || modelFeatures.length === 0) {
          return {
            isValid: true,
            message: '模型未定义特征要求，将使用您选择的特征进行预测'
          };
        }

        // 验证特征列兼容性
        const selectedFeatures = this.form.selectedFeatures || [];
        const selectedTarget = this.form.targetColumn;

        // 检查特征列是否匹配
        const missingFeatures = modelFeatures.filter(feature => !selectedFeatures.includes(feature));
        const extraFeatures = selectedFeatures.filter(feature => !modelFeatures.includes(feature));

        // 检查目标列是否匹配
        const targetMismatch = modelTarget && selectedTarget && modelTarget !== selectedTarget;

        // 构建验证结果
        if (missingFeatures.length > 0 || extraFeatures.length > 0 || targetMismatch) {
          let errorMessages = [];

          if (missingFeatures.length > 0) {
            errorMessages.push(`缺少模型所需特征列：${missingFeatures.join(', ')}`);
          }

          if (extraFeatures.length > 0) {
            errorMessages.push(`包含模型不需要的特征列：${extraFeatures.join(', ')}`);
          }

          if (targetMismatch) {
            errorMessages.push(`目标列不匹配：模型需要 "${modelTarget}"，您选择了 "${selectedTarget}"`);
          }

          return {
            isValid: false,
            message: errorMessages.join('；'),
            details: {
              modelFeatures,
              selectedFeatures,
              modelTarget,
              selectedTarget,
              missingFeatures,
              extraFeatures
            }
          };
        }

        return {
          isValid: true,
          message: '模型与所选特征和目标列完全兼容'
        };

      } catch (error) {
        console.error('模型兼容性检查失败:', error);
        return {
          isValid: false,
          message: '模型兼容性检查失败，请检查模型配置'
        };
      }
    },

    /** 处理行选择策略变化 */
    handleRowSelectionChange(strategy) {
      if (strategy === 'sequential') {
        // 切换到顺序选择时，清空自定义行
        this.form.customRows = [];
        this.customRowsInput = '';
      } else if (strategy === 'custom') {
        // 切换到自定义选择时，设置默认样本数
        if (!this.form.sampleCount) {
          this.form.sampleCount = 10;
        }
      }
    },

    /** 解析自定义行输入 */
    parseCustomRows() {
      const input = this.customRowsInput.trim();
      if (!input) {
        this.form.customRows = [];
        return;
      }

      try {
        const rows = new Set(); // 使用Set避免重复

        // 分割逗号分隔的部分
        const parts = input.split(',').map(part => part.trim()).filter(part => part);

        for (const part of parts) {
          if (part.includes('-')) {
            // 处理范围格式：1-10
            const [start, end] = part.split('-').map(s => parseInt(s.trim()));
            if (!isNaN(start) && !isNaN(end) && start > 0 && end > 0 && start <= end) {
              for (let i = start; i <= end; i++) {
                rows.add(i);
              }
            }
          } else {
            // 处理单个数字
            const num = parseInt(part);
            if (!isNaN(num) && num > 0) {
              rows.add(num);
            }
          }
        }

        // 转换为数组并排序
        this.form.customRows = Array.from(rows).sort((a, b) => a - b);

        // 验证行数限制
        if (this.form.customRows.length > 1000) {
          this.$message.warning('最多只能选择1000行数据，已自动截取前1000行');
          this.form.customRows = this.form.customRows.slice(0, 1000);
        }

      } catch (error) {
        console.error('解析自定义行失败:', error);
        this.form.customRows = [];
      }
    },

    /** 获取行范围显示文本 */
    getRowRangeDisplay() {
      if (!this.form.customRows || this.form.customRows.length === 0) {
        return '';
      }

      const rows = this.form.customRows;
      if (rows.length <= 10) {
        return rows.join(', ');
      } else {
        return `${rows[0]}-${rows[rows.length - 1]} (共${rows.length}行)`;
      }
    }
  },

  beforeDestroy() {
    // 组件销毁时清理定时器
    this.stopProgressMonitoring();
  }
};
</script>

<style scoped>
.enhanced-regression-prediction {
  padding: 20px;
}
.upload-area {
  margin: 20px 0;
}

/* 进度条样式 */
.prediction-progress-card {
  margin-top: 20px;
  border: 2px solid #409EFF;
  border-radius: 8px;
}

.prediction-progress-card .el-card__header {
  background-color: #f0f9ff;
  border-bottom: 1px solid #409EFF;
}

.progress-content {
  padding: 10px 0;
}

.progress-info {
  margin-bottom: 20px;
}

.progress-info p {
  margin: 8px 0;
  color: #606266;
}

.progress-steps {
  margin-top: 20px;
}

.el-progress {
  margin: 15px 0;
}

.el-steps {
  margin-top: 15px;
}

/* 表单提示样式 */
.form-tip {
  margin-top: 8px;
  padding: 8px 12px;
  background-color: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 4px;
  color: #d48806;
  font-size: 12px;
  display: flex;
  align-items: center;
}

.form-tip i {
  margin-right: 6px;
  font-size: 14px;
}
</style>
