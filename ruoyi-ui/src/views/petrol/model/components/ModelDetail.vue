<template>
  <el-dialog 
    title="模型详情" 
    :visible.sync="dialogVisible" 
    width="80%" 
    append-to-body
    :before-close="handleClose"
  >
    <div v-loading="loading">
      <div v-if="model">
        <!-- 基本信息 -->
        <el-card class="model-info-card" shadow="never">
          <div slot="header" class="clearfix">
            <span class="card-title">📊 基本信息</span>
            <el-tag 
              :type="model.status === 'ACTIVE' ? 'success' : 'danger'" 
              style="float: right;"
            >
              {{ model.status === 'ACTIVE' ? '可用' : '不可用' }}
            </el-tag>
          </div>
          
          <el-descriptions :column="2" border>
            <el-descriptions-item label="模型名称">{{ model.modelName }}</el-descriptions-item>
            <el-descriptions-item label="模型类型">{{ getModelTypeName(model.modelType) }}</el-descriptions-item>
            <el-descriptions-item label="算法类型">{{ model.algorithm }}</el-descriptions-item>
            <el-descriptions-item label="输出目标">{{ model.outputTarget }}</el-descriptions-item>
            <el-descriptions-item label="文件大小">{{ formatFileSize(model.fileSize) }}</el-descriptions-item>
            <el-descriptions-item label="模型来源">
              <el-tag :type="model.sourceTaskId ? 'success' : 'primary'" size="small">
                {{ model.sourceTaskId ? '分析任务生成' : '用户上传' }}
              </el-tag>
              <span v-if="model.sourceTaskId" style="margin-left: 10px; color: #909399;">
                (任务ID: {{ model.sourceTaskId }})
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ model.createTime }}</el-descriptions-item>
            <el-descriptions-item label="创建者">{{ model.createdBy }}</el-descriptions-item>
            <el-descriptions-item label="模型描述" :span="2">{{ model.description || '暂无描述' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 输入特征 -->
        <el-card class="features-card" shadow="never" style="margin-top: 20px;" v-if="inputFeatures.length > 0">
          <div slot="header" class="clearfix">
            <span class="card-title">🔧 输入特征 ({{ inputFeatures.length }}个)</span>
          </div>
          
          <div class="features-container">
            <el-tag 
              v-for="(feature, index) in inputFeatures" 
              :key="index"
              :type="getFeatureTagType(index)"
              style="margin-right: 10px; margin-bottom: 10px;"
              size="medium"
            >
              {{ feature }}
            </el-tag>
          </div>
        </el-card>

        <!-- 训练指标 -->
        <el-card class="metrics-card" shadow="never" style="margin-top: 20px;" v-if="trainingMetrics">
          <div slot="header" class="clearfix">
            <span class="card-title">📈 训练指标</span>
            <el-tag 
              :type="getPerformanceTagType()" 
              style="float: right;"
            >
              {{ getPerformanceLevel() }}
            </el-tag>
          </div>
          
          <el-row :gutter="20" v-if="parsedMetrics">
            <el-col :span="6" v-for="metric in displayMetrics" :key="metric.key">
              <div class="metric-item">
                <div class="metric-value">{{ metric.value }}</div>
                <div class="metric-label">{{ metric.label }}</div>
                <div class="metric-desc">{{ metric.description }}</div>
              </div>
            </el-col>
          </el-row>
          
          <!-- 原始指标数据 -->
          <el-collapse style="margin-top: 20px;">
            <el-collapse-item title="查看原始指标数据" name="raw-metrics">
              <pre class="metrics-raw">{{ formatMetrics(model.trainingMetrics) }}</pre>
            </el-collapse-item>
          </el-collapse>
        </el-card>

        <!-- 模型参数 -->
        <el-card class="params-card" shadow="never" style="margin-top: 20px;" v-if="modelParams">
          <div slot="header" class="clearfix">
            <span class="card-title">⚙️ 模型参数</span>
          </div>
          
          <el-table :data="parametersList" border size="small">
            <el-table-column prop="parameter" label="参数名称" width="200" />
            <el-table-column prop="value" label="参数值" width="150">
              <template slot-scope="scope">
                <el-tag size="small" :type="getParameterTagType(scope.row.type)">
                  {{ scope.row.value }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="参数描述" show-overflow-tooltip />
          </el-table>
          
          <!-- 原始参数数据 -->
          <el-collapse style="margin-top: 20px;">
            <el-collapse-item title="查看原始参数数据" name="raw-params">
              <pre class="params-raw">{{ formatParams(model.modelParams) }}</pre>
            </el-collapse-item>
          </el-collapse>
        </el-card>

        <!-- 操作按钮 -->
        <el-card class="actions-card" shadow="never" style="margin-top: 20px;">
          <div slot="header" class="clearfix">
            <span class="card-title">🚀 快速操作</span>
          </div>
          
          <el-row :gutter="20">
            <el-col :span="6">
              <el-button type="primary" icon="el-icon-magic-stick" @click="handlePredict" :disabled="model.status !== 'ACTIVE'">
                使用此模型预测
              </el-button>
            </el-col>
            <el-col :span="6">
              <el-button type="success" icon="el-icon-view" @click="handleViewSource" v-if="model.sourceTaskId">
                查看源任务
              </el-button>
            </el-col>
            <el-col :span="6">
              <el-button type="info" icon="el-icon-download" @click="handleDownload">
                下载模型
              </el-button>
            </el-col>
            <el-col :span="6">
              <el-button type="warning" icon="el-icon-refresh" @click="handleValidate">
                验证模型
              </el-button>
            </el-col>
          </el-row>
        </el-card>
      </div>
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">关 闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getModel, validateModel } from '@/api/petrol/model'

export default {
  name: 'ModelDetail',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    modelId: {
      type: [Number, String],
      default: null
    }
  },
  data() {
    return {
      dialogVisible: false,
      loading: false,
      model: null
    }
  },
  computed: {
    inputFeatures() {
      if (!this.model || !this.model.inputFeatures) return []
      try {
        return JSON.parse(this.model.inputFeatures)
      } catch (e) {
        return this.model.inputFeatures.split(',').map(f => f.trim())
      }
    },
    
    trainingMetrics() {
      if (!this.model || !this.model.trainingMetrics) return null
      try {
        return JSON.parse(this.model.trainingMetrics)
      } catch (e) {
        return null
      }
    },
    
    parsedMetrics() {
      return this.trainingMetrics
    },
    
    displayMetrics() {
      if (!this.parsedMetrics) return []
      
      const metrics = []
      const data = this.parsedMetrics
      
      // 常见的回归指标
      if (data.mse !== undefined) {
        metrics.push({
          key: 'mse',
          label: 'MSE',
          value: this.formatNumber(data.mse),
          description: '均方误差'
        })
      }
      if (data.rmse !== undefined) {
        metrics.push({
          key: 'rmse',
          label: 'RMSE',
          value: this.formatNumber(data.rmse),
          description: '均方根误差'
        })
      }
      if (data.mae !== undefined) {
        metrics.push({
          key: 'mae',
          label: 'MAE',
          value: this.formatNumber(data.mae),
          description: '平均绝对误差'
        })
      }
      if (data.r2_score !== undefined) {
        metrics.push({
          key: 'r2',
          label: 'R²',
          value: this.formatNumber(data.r2_score),
          description: '决定系数'
        })
      }
      
      // 常见的分类指标
      if (data.accuracy !== undefined) {
        metrics.push({
          key: 'accuracy',
          label: '准确率',
          value: this.formatPercentage(data.accuracy),
          description: '分类准确率'
        })
      }
      if (data.precision !== undefined) {
        metrics.push({
          key: 'precision',
          label: '精确率',
          value: this.formatPercentage(data.precision),
          description: '精确率'
        })
      }
      if (data.recall !== undefined) {
        metrics.push({
          key: 'recall',
          label: '召回率',
          value: this.formatPercentage(data.recall),
          description: '召回率'
        })
      }
      if (data.f1_score !== undefined) {
        metrics.push({
          key: 'f1',
          label: 'F1分数',
          value: this.formatNumber(data.f1_score),
          description: 'F1分数'
        })
      }
      
      return metrics.slice(0, 4) // 最多显示4个主要指标
    },
    
    modelParams() {
      if (!this.model || !this.model.modelParams) return null
      try {
        return JSON.parse(this.model.modelParams)
      } catch (e) {
        return null
      }
    },
    
    parametersList() {
      if (!this.modelParams) return []
      
      return Object.entries(this.modelParams).map(([key, value]) => ({
        parameter: this.getParameterDisplayName(key),
        value: this.formatParameterValue(value),
        type: this.getParameterType(value),
        description: this.getParameterDescription(key)
      }))
    }
  },
  watch: {
    visible(val) {
      this.dialogVisible = val
      if (val && this.modelId) {
        this.loadModelDetail()
      }
    },
    dialogVisible(val) {
      this.$emit('update:visible', val)
    },
    modelId(val) {
      if (val && this.dialogVisible) {
        this.loadModelDetail()
      }
    }
  },
  methods: {
    async loadModelDetail() {
      if (!this.modelId) return
      
      this.loading = true
      try {
        const response = await getModel(this.modelId)
        this.model = response.data
      } catch (error) {
        console.error('加载模型详情失败:', error)
        this.$message.error('加载模型详情失败')
      } finally {
        this.loading = false
      }
    },
    
    getModelTypeName(type) {
      const typeMap = {
        'regression': '回归模型',
        'classification': '分类模型',
        'clustering': '聚类模型',
        'time_series': '时间序列模型'
      }
      return typeMap[type] || type
    },
    
    getFeatureTagType(index) {
      const types = ['primary', 'success', 'info', 'warning']
      return types[index % types.length]
    },
    
    getPerformanceTagType() {
      if (!this.parsedMetrics) return 'info'
      
      // 根据R²分数判断性能
      if (this.parsedMetrics.r2_score !== undefined) {
        const r2 = this.parsedMetrics.r2_score
        if (r2 >= 0.9) return 'success'
        if (r2 >= 0.7) return 'warning'
        return 'danger'
      }
      
      // 根据准确率判断性能
      if (this.parsedMetrics.accuracy !== undefined) {
        const acc = this.parsedMetrics.accuracy
        if (acc >= 0.9) return 'success'
        if (acc >= 0.7) return 'warning'
        return 'danger'
      }
      
      return 'info'
    },
    
    getPerformanceLevel() {
      if (!this.parsedMetrics) return '未知'
      
      // 根据主要指标判断性能等级
      let score = 0
      if (this.parsedMetrics.r2_score !== undefined) {
        score = this.parsedMetrics.r2_score
      } else if (this.parsedMetrics.accuracy !== undefined) {
        score = this.parsedMetrics.accuracy
      }
      
      if (score >= 0.9) return '优秀'
      if (score >= 0.8) return '良好'
      if (score >= 0.7) return '一般'
      if (score >= 0.6) return '较差'
      return '很差'
    },
    
    getParameterDisplayName(key) {
      const nameMap = {
        'n_estimators': '树的数量',
        'max_depth': '最大深度',
        'learning_rate': '学习率',
        'random_state': '随机种子',
        'C': '正则化参数',
        'gamma': 'Gamma参数',
        'kernel': '核函数',
        'alpha': 'Alpha参数',
        'fit_intercept': '拟合截距',
        'normalize': '标准化'
      }
      return nameMap[key] || key
    },
    
    getParameterDescription(key) {
      const descMap = {
        'n_estimators': '集成学习中树的数量',
        'max_depth': '决策树的最大深度',
        'learning_rate': '梯度下降的学习率',
        'random_state': '随机数生成器的种子',
        'C': 'SVM的正则化参数',
        'gamma': 'RBF核的参数',
        'kernel': 'SVM使用的核函数类型',
        'alpha': '正则化强度参数',
        'fit_intercept': '是否拟合截距项',
        'normalize': '是否对特征进行标准化'
      }
      return descMap[key] || '模型参数'
    },
    
    getParameterType(value) {
      if (typeof value === 'number') return 'number'
      if (typeof value === 'boolean') return 'boolean'
      return 'string'
    },
    
    getParameterTagType(type) {
      const typeMap = {
        'number': 'primary',
        'boolean': 'success',
        'string': 'info'
      }
      return typeMap[type] || 'info'
    },
    
    formatParameterValue(value) {
      if (typeof value === 'number') {
        return Number.isInteger(value) ? value.toString() : value.toFixed(4)
      }
      if (typeof value === 'boolean') {
        return value ? '是' : '否'
      }
      return String(value)
    },
    
    formatNumber(num) {
      if (num === null || num === undefined) return '-'
      if (Number.isInteger(num)) return num.toString()
      return Number(num).toFixed(4)
    },
    
    formatPercentage(rate) {
      if (rate === null || rate === undefined) return '-'
      return (rate * 100).toFixed(1) + '%'
    },
    
    formatFileSize(bytes) {
      if (!bytes) return '-'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    
    formatMetrics(metrics) {
      if (!metrics) return '暂无训练指标'
      try {
        return JSON.stringify(JSON.parse(metrics), null, 2)
      } catch (e) {
        return metrics
      }
    },
    
    formatParams(params) {
      if (!params) return '暂无模型参数'
      try {
        return JSON.stringify(JSON.parse(params), null, 2)
      } catch (e) {
        return params
      }
    },
    
    handlePredict() {
      this.$router.push({
        path: '/petrol/prediction',
        query: {
          modelId: this.model.id,
          predictionName: `${this.model.modelName}_预测_${new Date().getTime()}`,
          tab: 'regression'
        }
      })
      this.handleClose()
    },
    
    handleViewSource() {
      if (this.model.sourceTaskId) {
        this.$router.push({
          path: '/petrol/analysis',
          query: {
            taskId: this.model.sourceTaskId
          }
        })
        this.handleClose()
      }
    },
    
    handleDownload() {
      this.$message.info('模型下载功能开发中...')
    },
    
    async handleValidate() {
      try {
        this.loading = true
        await validateModel(this.model.modelPath)
        this.$message.success('模型验证通过')
      } catch (error) {
        this.$message.error('模型验证失败：' + error.message)
      } finally {
        this.loading = false
      }
    },
    
    handleClose() {
      this.dialogVisible = false
      this.model = null
    }
  }
}
</script>

<style scoped>
.card-title {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
}

.model-info-card {
  border: 1px solid #e4e7ed;
}

.features-container {
  min-height: 60px;
  padding: 10px 0;
}

.metric-item {
  text-align: center;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.metric-label {
  font-size: 14px;
  font-weight: bold;
  color: #606266;
  margin-bottom: 4px;
}

.metric-desc {
  font-size: 12px;
  color: #909399;
}

.metrics-raw, .params-raw {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
  max-height: 300px;
  overflow-y: auto;
}

.actions-card .el-button {
  width: 100%;
}

.dialog-footer {
  text-align: right;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .metric-item {
    margin-bottom: 15px;
  }

  .metric-value {
    font-size: 20px;
  }
}
</style>
