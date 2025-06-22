<template>
  <div class="time-series-results-visualization">
    <!-- 时间序列性能指标卡片 -->
    <el-card class="metrics-card" shadow="never" style="margin-bottom: 20px;">
      <div slot="header">
        <span>📊 {{ getAlgorithmDisplayName() }} - 时间序列性能指标</span>
        <el-tag :type="getPerformanceTagType()" style="margin-left: 10px;">
          {{ getPerformanceLevel() }}
        </el-tag>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="6" v-for="metric in timeSeriesMetrics" :key="metric.key">
          <div class="metric-item">
            <div class="metric-value">{{ metric.value }}</div>
            <div class="metric-label">{{ metric.label }}</div>
            <div class="metric-desc">{{ metric.description }}</div>
          </div>
        </el-col>
      </el-row>
      
      <!-- 如果没有指标，显示提示 -->
      <el-empty v-if="timeSeriesMetrics.length === 0" 
                description="暂无时间序列性能指标数据" 
                :image-size="100">
      </el-empty>
    </el-card>

    <!-- 可视化图表 -->
    <el-row :gutter="20">
      <!-- 时间序列预测图 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📈 时间序列预测</span>
            <el-tooltip content="显示实际值与预测值的时间序列对比" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="timeSeriesChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 预测误差分析 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📉 预测误差分析</span>
            <el-tooltip content="显示预测误差随时间的变化" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="errorAnalysisChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 训练损失曲线 (如果有) -->
      <el-col v-if="hasTrainingHistory" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📚 训练损失曲线</span>
            <el-tooltip content="显示模型训练过程中损失函数的变化" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="trainingHistoryChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 模型参数表 -->
    <el-card class="params-card" shadow="never" style="margin-bottom: 20px;">
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
      
      <!-- 如果没有参数，显示提示 -->
      <el-empty v-if="modelParameters.length === 0" 
                description="暂无模型参数数据" 
                :image-size="100">
      </el-empty>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "TimeSeriesResultsVisualization",
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
      trainingHistoryChart: null
    };
  },
  computed: {
    timeSeriesMetrics() {
      const stats = this.results.statistics || this.results.metrics || {};
      const metrics = [];

      // R²决定系数
      const r2Score = stats.r2_score || stats.r2;
      if (r2Score !== undefined) {
        metrics.push({
          key: 'r2_score',
          label: 'R² 决定系数',
          value: this.formatNumber(r2Score),
          description: '模型解释方差的比例'
        });
      }

      // 均方误差
      const mse = stats.mean_squared_error || stats.mse;
      if (mse !== undefined) {
        metrics.push({
          key: 'mse',
          label: 'MSE',
          value: this.formatNumber(mse),
          description: '均方误差'
        });
      }

      // 平均绝对误差
      const mae = stats.mean_absolute_error || stats.mae;
      if (mae !== undefined) {
        metrics.push({
          key: 'mae',
          label: 'MAE',
          value: this.formatNumber(mae),
          description: '平均绝对误差'
        });
      }

      // 训练轮数
      const epochs = stats.epochs || this.getTrainingEpochs();
      if (epochs !== undefined) {
        metrics.push({
          key: 'epochs',
          label: '训练轮数',
          value: epochs,
          description: '模型训练的轮数'
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

    hasTrainingHistory() {
      const history = this.results.training_history || {};
      return Object.keys(history).length > 0;
    }
  },
  mounted() {
    console.log('🎨 时间序列可视化组件挂载');
    console.log('📊 接收到的结果数据:', this.results);
    console.log('📋 接收到的任务信息:', this.taskInfo);

    this.$nextTick(() => {
      this.initializeCharts();
    });
  },
  beforeDestroy() {
    // 销毁图表实例
    [this.timeSeriesChart, this.errorAnalysisChart, this.trainingHistoryChart].forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    initializeCharts() {
      console.log('🎨 开始初始化时间序列可视化图表');
      
      setTimeout(() => {
        this.renderTimeSeriesChart();
        this.renderErrorAnalysisChart();

        if (this.hasTrainingHistory) {
          this.renderTrainingHistoryChart();
        }

        console.log('✅ 时间序列可视化图表初始化完成');
      }, 100);
    },
    
    getAlgorithmDisplayName() {
      const names = {
        'bilstm': 'BiLSTM时间序列',
        'tcn': 'TCN时间序列',
        'lstm': 'LSTM时间序列',
        'gru': 'GRU时间序列',
        'transformer': 'Transformer时间序列',
        'arima': 'ARIMA时间序列'
      };
      return names[this.algorithmType] || this.algorithmType;
    },

    getTrainingEpochs() {
      const history = this.results.training_history || {};
      const loss = history.loss || [];
      return loss.length;
    },
    
    getPerformanceLevel() {
      const r2 = this.results.statistics?.r2_score || this.results.metrics?.r2_score;
      if (r2 === undefined) return '未知';
      
      if (r2 >= 0.9) return '优秀';
      if (r2 >= 0.8) return '良好';
      if (r2 >= 0.6) return '一般';
      return '较差';
    },
    
    getPerformanceTagType() {
      const r2 = this.results.statistics?.r2_score || this.results.metrics?.r2_score;
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
    
    getParameterDisplayName(key) {
      const names = {
        'sequence_length': '序列长度',
        'hidden_size': '隐藏层大小',
        'num_layers': '层数',
        'dropout': 'Dropout率',
        'learning_rate': '学习率',
        'batch_size': '批次大小',
        'epochs': '训练轮数'
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
        'sequence_length': '输入序列的长度',
        'hidden_size': 'LSTM/GRU隐藏层的大小',
        'num_layers': '网络的层数',
        'dropout': '防止过拟合的Dropout比率',
        'learning_rate': '优化器的学习率',
        'batch_size': '训练时的批次大小',
        'epochs': '训练的总轮数'
      };
      return descriptions[key] || '模型参数';
    },

    /** 渲染时间序列预测图 */
    renderTimeSeriesChart() {
      const chartDom = this.$refs.timeSeriesChart;
      if (!chartDom) return;

      chartDom.style.width = '100%';
      chartDom.style.height = '400px';

      if (this.timeSeriesChart) {
        this.timeSeriesChart.dispose();
      }

      this.timeSeriesChart = echarts.init(chartDom);

      // 获取时间序列数据
      const predictions = this.results.predictions || this.results.y_pred || [];
      const actualValues = this.results.actual_values || this.results.y_true || [];

      if (predictions.length === 0 || actualValues.length === 0) {
        this.timeSeriesChart.setOption({
          title: {
            text: '暂无时间序列数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 准备时间轴数据
      const timeIndices = Array.from({length: Math.min(predictions.length, actualValues.length)}, (_, i) => i);

      const option = {
        title: {
          text: '时间序列预测结果',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            let result = `时间点 ${params[0].dataIndex}<br/>`;
            params.forEach(param => {
              result += `${param.seriesName}: ${param.data.toFixed(4)}<br/>`;
            });
            return result;
          }
        },
        legend: {
          data: ['实际值', '预测值'],
          top: 30
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: timeIndices,
          name: '时间步'
        },
        yAxis: {
          type: 'value',
          name: '数值'
        },
        series: [
          {
            name: '实际值',
            type: 'line',
            data: actualValues.slice(0, 200), // 限制显示数量
            itemStyle: { color: '#5470c6' },
            symbol: 'circle',
            symbolSize: 3,
            smooth: true
          },
          {
            name: '预测值',
            type: 'line',
            data: predictions.slice(0, 200), // 限制显示数量
            itemStyle: { color: '#91cc75' },
            symbol: 'triangle',
            symbolSize: 3,
            smooth: true
          }
        ]
      };

      this.timeSeriesChart.setOption(option);
    },

    /** 渲染预测误差分析图 */
    renderErrorAnalysisChart() {
      const chartDom = this.$refs.errorAnalysisChart;
      if (!chartDom) return;

      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.errorAnalysisChart) {
        this.errorAnalysisChart.dispose();
      }

      this.errorAnalysisChart = echarts.init(chartDom);

      const predictions = this.results.predictions || this.results.y_pred || [];
      const actualValues = this.results.actual_values || this.results.y_true || [];

      if (predictions.length === 0 || actualValues.length === 0) {
        this.errorAnalysisChart.setOption({
          title: {
            text: '暂无误差分析数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 计算误差
      const errors = actualValues.map((actual, index) => Math.abs(actual - predictions[index]));
      const timeIndices = Array.from({length: errors.length}, (_, i) => i);

      const option = {
        title: {
          text: '预测误差分析',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            return `时间点 ${params[0].dataIndex}<br/>绝对误差: ${params[0].data.toFixed(4)}`;
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: timeIndices,
          name: '时间步'
        },
        yAxis: {
          type: 'value',
          name: '绝对误差'
        },
        series: [
          {
            name: '绝对误差',
            type: 'line',
            data: errors.slice(0, 200), // 限制显示数量
            itemStyle: { color: '#fac858' },
            symbol: 'circle',
            symbolSize: 3,
            smooth: true,
            areaStyle: {
              opacity: 0.3
            }
          }
        ]
      };

      this.errorAnalysisChart.setOption(option);
    },

    /** 渲染训练历史图 */
    renderTrainingHistoryChart() {
      const chartDom = this.$refs.trainingHistoryChart;
      if (!chartDom) return;

      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.trainingHistoryChart) {
        this.trainingHistoryChart.dispose();
      }

      this.trainingHistoryChart = echarts.init(chartDom);

      const history = this.results.training_history || {};
      const loss = history.loss || [];
      const valLoss = history.val_loss || [];

      if (loss.length === 0) {
        this.trainingHistoryChart.setOption({
          title: {
            text: '暂无训练历史数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      const epochs = Array.from({length: loss.length}, (_, i) => i + 1);
      const series = [
        {
          name: '训练损失',
          type: 'line',
          data: loss,
          itemStyle: { color: '#5470c6' },
          symbol: 'circle',
          symbolSize: 3,
          smooth: true
        }
      ];

      if (valLoss.length > 0) {
        series.push({
          name: '验证损失',
          type: 'line',
          data: valLoss,
          itemStyle: { color: '#91cc75' },
          symbol: 'triangle',
          symbolSize: 3,
          smooth: true
        });
      }

      const option = {
        title: {
          text: '训练损失曲线',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            let result = `轮次 ${params[0].dataIndex + 1}<br/>`;
            params.forEach(param => {
              result += `${param.seriesName}: ${param.data.toFixed(6)}<br/>`;
            });
            return result;
          }
        },
        legend: {
          data: valLoss.length > 0 ? ['训练损失', '验证损失'] : ['训练损失'],
          top: 30
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: epochs,
          name: '训练轮次'
        },
        yAxis: {
          type: 'value',
          name: '损失值'
        },
        series: series
      };

      this.trainingHistoryChart.setOption(option);
    }
  }
};
</script>

<style scoped>
.time-series-results-visualization {
  padding: 0;
}

.metrics-card .metric-item {
  text-align: center;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.metric-label {
  font-size: 14px;
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

.chart-card {
  margin-bottom: 20px;
}

.el-table {
  margin-top: 10px;
}
</style>
