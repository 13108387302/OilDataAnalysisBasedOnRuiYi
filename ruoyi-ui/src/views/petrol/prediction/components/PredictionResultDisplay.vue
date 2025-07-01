<template>
  <div class="prediction-result-display">
    <!-- 结果摘要 -->
    <div class="result-summary">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-statistic title="预测数量" :value="result.count || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="任务类型" :value="getTaskTypeName()" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="平均置信度" :value="getAverageConfidence()" suffix="%" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="执行时间" :value="result.execution_time || 0" suffix="ms" />
        </el-col>
      </el-row>
    </div>

    <!-- 回归预测结果 -->
    <div v-if="isRegression" class="regression-results">
      <el-divider content-position="left">回归预测结果</el-divider>
      
      <!-- 统计信息 -->
      <el-card shadow="never" class="stats-card">
        <div slot="header">统计信息</div>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-statistic title="平均值" :value="getStatValue('mean_prediction')" :precision="4" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="标准差" :value="getStatValue('std_prediction')" :precision="4" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="范围" :value="getPredictionRange()" />
          </el-col>
        </el-row>
      </el-card>

      <!-- 预测结果表格 -->
      <el-card shadow="never" class="results-table-card">
        <div slot="header">
          预测结果详情
          <el-button type="text" size="small" @click="downloadResults" style="float: right;">
            <i class="el-icon-download"></i> 下载结果
          </el-button>
        </div>
        
        <el-table :data="getPreviewData()" border size="small" max-height="400">
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="prediction" label="预测值" width="120">
            <template slot-scope="scope">
              <span style="font-weight: bold; color: #409EFF;">
                {{ scope.row.prediction }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="confidence" label="置信度" width="100">
            <template slot-scope="scope">
              <span :style="getConfidenceStyle(scope.row.confidence)">
                {{ (scope.row.confidence * 100).toFixed(1) }}%
              </span>
            </template>
          </el-table-column>
          <el-table-column 
            v-for="feature in getFeatureColumns()" 
            :key="feature"
            :prop="feature"
            :label="feature"
            min-width="100">
          </el-table-column>
        </el-table>
        
        <div class="table-footer">
          <span>显示前 {{ Math.min(20, getPredictionsLength()) }} 条记录，共 {{ getPredictionsLength() }} 条</span>
        </div>
      </el-card>
    </div>

    <!-- 分类预测结果 -->
    <div v-if="isClassification" class="classification-results">
      <el-divider content-position="left">分类预测结果</el-divider>
      
      <!-- 类别分布 -->
      <el-card shadow="never" class="stats-card">
        <div slot="header">类别分布</div>
        <el-row :gutter="20">
          <el-col :span="8" v-for="(percentage, className) in getClassDistribution()" :key="className">
            <el-statistic :title="className" :value="percentage" suffix="%" />
          </el-col>
        </el-row>
      </el-card>

      <!-- 预测结果表格 -->
      <el-card shadow="never" class="results-table-card">
        <div slot="header">
          分类预测详情
          <el-button type="text" size="small" @click="downloadResults" style="float: right;">
            <i class="el-icon-download"></i> 下载结果
          </el-button>
        </div>
        
        <el-table :data="getPreviewData()" border size="small" max-height="400">
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="predicted_class" label="预测类别" width="120">
            <template slot-scope="scope">
              <el-tag type="success" size="small">
                {{ scope.row.predicted_class }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="confidence" label="置信度" width="100">
            <template slot-scope="scope">
              <span :style="getConfidenceStyle(scope.row.confidence)">
                {{ (scope.row.confidence * 100).toFixed(1) }}%
              </span>
            </template>
          </el-table-column>
          <el-table-column label="概率分布" min-width="200">
            <template slot-scope="scope">
              <probability-chart 
                v-if="scope.row.probabilities"
                :probabilities="scope.row.probabilities"
                :show-details="false"
                :compact="true" />
            </template>
          </el-table-column>
          <el-table-column 
            v-for="feature in getFeatureColumns()" 
            :key="feature"
            :prop="feature"
            :label="feature"
            min-width="100">
          </el-table-column>
        </el-table>
        
        <div class="table-footer">
          <span>显示前 {{ Math.min(20, getPredictionsLength()) }} 条记录，共 {{ getPredictionsLength() }} 条</span>
        </div>
      </el-card>

      <!-- 概率分布示例 -->
      <el-card shadow="never" class="probability-examples" v-if="getClassificationSamples().length > 0">
        <div slot="header">概率分布示例</div>
        <el-row :gutter="20">
          <el-col :span="12" v-for="(sample, index) in getClassificationSamples()" :key="index">
            <probability-chart
              :probabilities="sample.probabilities"
              :title="`样本 ${sample.index} - ${sample.predicted_class}`"
              :show-details="false" />
          </el-col>
        </el-row>
      </el-card>
    </div>

    <!-- 操作按钮 -->
    <div class="result-actions">
      <el-button type="primary" @click="downloadResults">
        <i class="el-icon-download"></i> 下载完整结果
      </el-button>
      <el-button @click="viewDetailedAnalysis">
        <i class="el-icon-data-analysis"></i> 详细分析
      </el-button>
      <el-button @click="createNewPrediction">
        <i class="el-icon-plus"></i> 创建新预测
      </el-button>
    </div>
  </div>
</template>

<script>
import ProbabilityChart from '@/components/ProbabilityChart'

export default {
  name: 'PredictionResultDisplay',
  components: {
    ProbabilityChart
  },
  props: {
    result: {
      type: Object,
      required: true,
      default: () => ({})
    }
  },
  computed: {
    isRegression() {
      return this.result.task_type === 'regression' || 
             (this.result.predictions && this.result.predictions.length > 0 && 
              typeof this.result.predictions[0] === 'number')
    },
    
    isClassification() {
      return this.result.task_type === 'classification' || 
             (this.result.predictions && this.result.predictions.length > 0 && 
              typeof this.result.predictions[0] === 'object' && 
              this.result.predictions[0].predicted_class)
    }
  },
  methods: {
    getTaskTypeName() {
      if (this.isRegression) return '回归预测'
      if (this.isClassification) return '分类预测'
      return '未知'
    },

    getAverageConfidence() {
      const confidences = this.result.confidences || []
      if (confidences.length === 0) return 0
      const avg = confidences.reduce((sum, conf) => sum + conf, 0) / confidences.length
      return (avg * 100).toFixed(1)
    },

    getStatValue(key) {
      return this.result.statistics && this.result.statistics[key] ? this.result.statistics[key] : 0
    },

    getPredictionRange() {
      const stats = this.result.statistics || {}
      const min = stats.min_prediction || 0
      const max = stats.max_prediction || 0
      return `${min.toFixed(2)} - ${max.toFixed(2)}`
    },

    getClassDistribution() {
      const stats = this.result.statistics || {}
      return stats.class_percentages || {}
    },

    getPreviewData() {
      const predictions = this.result.predictions || []
      const inputData = this.result.input_data || []
      const confidences = this.result.confidences || []
      
      return predictions.slice(0, 20).map((prediction, index) => {
        const row = {
          index: index + 1,
          confidence: confidences[index] || 0
        }
        
        if (this.isRegression) {
          row.prediction = typeof prediction === 'number' ? prediction.toFixed(4) : prediction
        } else if (this.isClassification) {
          row.predicted_class = prediction.predicted_class || prediction
          row.probabilities = prediction.probabilities || {}
        }
        
        // 添加输入特征
        if (inputData[index]) {
          Object.assign(row, inputData[index])
        }
        
        return row
      })
    },

    getFeatureColumns() {
      const inputData = this.result.input_data || []
      if (inputData.length === 0) return []
      return Object.keys(inputData[0]).slice(0, 3) // 只显示前3个特征
    },

    getClassificationSamples() {
      if (!this.isClassification) return []
      
      const predictions = this.result.predictions || []
      return predictions.slice(0, 4).map((prediction, index) => ({
        index: index + 1,
        predicted_class: prediction.predicted_class,
        probabilities: prediction.probabilities || {}
      }))
    },

    getConfidenceStyle(confidence) {
      const value = typeof confidence === 'number' ? confidence : parseFloat(confidence)
      if (value >= 0.8) {
        return { color: '#67C23A', fontWeight: 'bold' }
      } else if (value >= 0.6) {
        return { color: '#E6A23C', fontWeight: 'bold' }
      } else {
        return { color: '#F56C6C', fontWeight: 'bold' }
      }
    },

    downloadResults() {
      // 实现结果下载功能
      this.$message.info('下载功能开发中...')
    },

    viewDetailedAnalysis() {
      // 跳转到详细分析页面
      this.$emit('view-analysis', this.result)
    },

    createNewPrediction() {
      // 创建新的预测任务
      this.$emit('create-new')
    },

    getPredictionsLength() {
      return this.result.predictions ? this.result.predictions.length : 0
    }
  }
}
</script>

<style scoped>
.prediction-result-display {
  width: 100%;
}

.result-summary {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.stats-card, .results-table-card, .probability-examples {
  margin-bottom: 20px;
}

.table-footer {
  text-align: center;
  padding: 10px;
  color: #909399;
  font-size: 14px;
}

.result-actions {
  text-align: center;
  padding: 20px 0;
  border-top: 1px solid #e4e7ed;
  margin-top: 20px;
}

.result-actions .el-button {
  margin: 0 10px;
}
</style>
