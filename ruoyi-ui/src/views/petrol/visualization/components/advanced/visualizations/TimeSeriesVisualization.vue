<template>
  <div class="time-series-visualization">
    <!-- 时间序列算法结果可视化 -->
    <el-row :gutter="20">
      <!-- 模型性能指标卡片 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="metrics-card" shadow="never">
          <div slot="header">
            <span>🕒 {{ getAlgorithmDisplayName() }} - 时间序列预测性能</span>
            <el-tag :type="getPerformanceTagType()" style="margin-left: 10px;">
              {{ getPerformanceLevel() }}
            </el-tag>
          </div>
          
          <el-row :gutter="20">
            <el-col :span="6" v-for="metric in performanceMetrics" :key="metric.key">
              <div class="metric-item">
                <div class="metric-value">{{ metric.value }}</div>
                <div class="metric-label">{{ metric.label }}</div>
                <div class="metric-desc">{{ metric.description }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <!-- 时间序列预测图 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📈 时间序列预测结果</span>
            <el-tooltip content="显示历史数据、预测值和实际值的对比" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="timeSeriesChart" class="large-chart"></div>
        </el-card>
      </el-col>

      <!-- 预测误差分析 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 预测误差分析</span>
            <el-tooltip content="显示预测误差的分布和趋势" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="errorAnalysisChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 残差自相关图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🔄 残差自相关分析</span>
            <el-tooltip content="检验模型残差的自相关性" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="autocorrelationChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 训练损失曲线 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📚 训练损失曲线</span>
            <el-tooltip content="显示模型训练过程中的损失变化" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="trainingLossChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 特征重要性 (如果有) -->
      <el-col v-if="hasFeatureImportance" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🔍 特征重要性</span>
            <el-tooltip content="显示各时间步特征的重要程度" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="featureImportanceChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 预测区间图 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 预测置信区间</span>
            <el-tooltip content="显示预测值的置信区间" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="confidenceIntervalChart" class="large-chart"></div>
        </el-card>
      </el-col>

      <!-- 模型架构信息 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="architecture-card" shadow="never">
          <div slot="header">
            <span>🏗️ 模型架构信息</span>
          </div>
          
          <el-descriptions :column="2" border>
            <el-descriptions-item label="模型类型">{{ getModelType() }}</el-descriptions-item>
            <el-descriptions-item label="序列长度">{{ getSequenceLength() }}</el-descriptions-item>
            <el-descriptions-item label="隐藏层数">{{ getHiddenLayers() }}</el-descriptions-item>
            <el-descriptions-item label="隐藏单元数">{{ getHiddenUnits() }}</el-descriptions-item>
            <el-descriptions-item label="训练轮数">{{ getEpochs() }}</el-descriptions-item>
            <el-descriptions-item label="批次大小">{{ getBatchSize() }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 模型参数表 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="params-card" shadow="never">
          <div slot="header">
            <span>⚙️ 模型参数</span>
          </div>
          
          <el-table :data="modelParameters" border stripe style="width: 100%;">
            <el-table-column prop="parameter" label="参数名称" width="200">
              <template slot-scope="scope">
                <strong>{{ scope.row.parameter }}</strong>
              </template>
            </el-table-column>
            <el-table-column prop="value" label="参数值">
              <template slot-scope="scope">
                <code>{{ scope.row.value }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明">
              <template slot-scope="scope">
                <span>{{ scope.row.description }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "TimeSeriesVisualization",
  props: {
    results: {
      type: Object,
      required: true
    },
    taskInfo: {
      type: Object,
      required: true
    },
    algorithmType: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      // 图表实例
      timeSeriesChart: null,
      errorAnalysisChart: null,
      autocorrelationChart: null,
      trainingLossChart: null,
      featureImportanceChart: null,
      confidenceIntervalChart: null
    };
  },
  computed: {
    performanceMetrics() {
      const stats = this.results.statistics || {};
      const metrics = [];
      
      // RMSE
      if (stats.rmse !== undefined) {
        metrics.push({
          key: 'rmse',
          label: 'RMSE',
          value: this.formatNumber(stats.rmse),
          description: '均方根误差'
        });
      }
      
      // MAE
      if (stats.mae !== undefined) {
        metrics.push({
          key: 'mae',
          label: 'MAE',
          value: this.formatNumber(stats.mae),
          description: '平均绝对误差'
        });
      }
      
      // MAPE
      if (stats.mape !== undefined) {
        metrics.push({
          key: 'mape',
          label: 'MAPE',
          value: this.formatPercentage(stats.mape),
          description: '平均绝对百分比误差'
        });
      }
      
      // R²
      if (stats.r2_score !== undefined) {
        metrics.push({
          key: 'r2_score',
          label: 'R²',
          value: this.formatNumber(stats.r2_score),
          description: '决定系数'
        });
      }
      
      return metrics;
    },
    
    modelParameters() {
      const params = this.results.model_params || {};
      return Object.entries(params).map(([key, value]) => ({
        parameter: this.getParameterDisplayName(key),
        value: this.formatParameterValue(value),
        description: this.getParameterDescription(key)
      }));
    },
    
    hasFeatureImportance() {
      return this.results.feature_importance || 
             (this.results.statistics && this.results.statistics.feature_importance);
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initializeCharts();
    });
  },
  beforeDestroy() {
    // 销毁图表实例
    [this.timeSeriesChart, this.errorAnalysisChart, this.autocorrelationChart,
     this.trainingLossChart, this.featureImportanceChart, this.confidenceIntervalChart].forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    initializeCharts() {
      this.renderTimeSeriesChart();
      this.renderErrorAnalysisChart();
      this.renderAutocorrelationChart();
      this.renderTrainingLossChart();
      this.renderConfidenceIntervalChart();
      
      if (this.hasFeatureImportance) {
        this.renderFeatureImportanceChart();
      }
    },
    
    getAlgorithmDisplayName() {
      const names = {
        'bilstm_regression': 'BiLSTM时间序列回归',
        'tcn_regression': 'TCN时间序列回归',
        'lstm_regression': 'LSTM时间序列回归',
        'gru_regression': 'GRU时间序列回归'
      };
      return names[this.algorithmType] || this.algorithmType;
    },
    
    getPerformanceLevel() {
      const r2 = this.results.statistics?.r2_score;
      if (r2 === undefined) return '未知';
      
      if (r2 >= 0.9) return '优秀';
      if (r2 >= 0.8) return '良好';
      if (r2 >= 0.6) return '一般';
      return '较差';
    },
    
    getPerformanceTagType() {
      const r2 = this.results.statistics?.r2_score;
      if (r2 === undefined) return 'info';
      
      if (r2 >= 0.9) return 'success';
      if (r2 >= 0.8) return 'primary';
      if (r2 >= 0.6) return 'warning';
      return 'danger';
    },
    
    formatNumber(value) {
      if (value === null || value === undefined || isNaN(value)) {
        return 'N/A';
      }
      return Number(value).toFixed(4);
    },
    
    formatPercentage(value) {
      if (value === null || value === undefined || isNaN(value)) {
        return 'N/A';
      }
      return (Number(value) * 100).toFixed(2) + '%';
    },
    
    getModelType() {
      if (this.algorithmType.includes('bilstm')) return 'BiLSTM (双向长短期记忆网络)';
      if (this.algorithmType.includes('tcn')) return 'TCN (时间卷积网络)';
      if (this.algorithmType.includes('lstm')) return 'LSTM (长短期记忆网络)';
      if (this.algorithmType.includes('gru')) return 'GRU (门控循环单元)';
      return '未知';
    },
    
    getSequenceLength() {
      return this.results.model_params?.sequence_length || 'N/A';
    },
    
    getHiddenLayers() {
      return this.results.model_params?.hidden_layers || 'N/A';
    },
    
    getHiddenUnits() {
      return this.results.model_params?.hidden_units || 'N/A';
    },
    
    getEpochs() {
      return this.results.model_params?.epochs || 'N/A';
    },
    
    getBatchSize() {
      return this.results.model_params?.batch_size || 'N/A';
    },
    
    getParameterDisplayName(key) {
      const names = {
        'sequence_length': '序列长度',
        'hidden_units': '隐藏单元数',
        'hidden_layers': '隐藏层数',
        'dropout_rate': 'Dropout率',
        'learning_rate': '学习率',
        'epochs': '训练轮数',
        'batch_size': '批次大小',
        'optimizer': '优化器',
        'loss_function': '损失函数'
      };
      return names[key] || key;
    },
    
    formatParameterValue(value) {
      if (typeof value === 'number') {
        return this.formatNumber(value);
      }
      return String(value);
    },
    
    getParameterDescription(key) {
      const descriptions = {
        'sequence_length': '输入序列的时间步长度',
        'hidden_units': '每个隐藏层的神经元数量',
        'hidden_layers': '隐藏层的数量',
        'dropout_rate': '防止过拟合的Dropout比率',
        'learning_rate': '模型训练的学习率',
        'epochs': '模型训练的总轮数',
        'batch_size': '每个批次的样本数量',
        'optimizer': '模型优化算法',
        'loss_function': '模型损失函数'
      };
      return descriptions[key] || '模型参数';
    }
  }
};
</script>

<style scoped>
.time-series-visualization {
  padding: 0;
}

.metrics-card, .chart-card, .architecture-card, .params-card {
  margin-bottom: 20px;
}

.metric-item {
  text-align: center;
  padding: 10px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #722ED1;
  margin-bottom: 5px;
}

.metric-label {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 3px;
}

.metric-desc {
  font-size: 12px;
  color: #909399;
}

.chart {
  width: 100%;
  height: 350px;
}

.large-chart {
  width: 100%;
  height: 450px;
}
</style>
