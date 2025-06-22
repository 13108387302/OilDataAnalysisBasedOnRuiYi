<template>
  <div class="prediction-visualization">
    <!-- 预测任务结果可视化 -->
    <el-row :gutter="20">
      <!-- 预测信息卡片 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="info-card" shadow="never">
          <div slot="header">
            <span>🔮 {{ getAlgorithmDisplayName() }} - 预测结果</span>
            <el-tag :type="getPredictionTagType()" style="margin-left: 10px;">
              {{ getPredictionCategory() }}
            </el-tag>
          </div>
          
          <el-descriptions :column="3" border>
            <el-descriptions-item label="预测类型">{{ getPredictionType() }}</el-descriptions-item>
            <el-descriptions-item label="预测样本数">{{ getPredictionCount() }}</el-descriptions-item>
            <el-descriptions-item label="使用模型">{{ getUsedModel() }}</el-descriptions-item>
            <el-descriptions-item label="输入特征">{{ getInputFeatures() }}</el-descriptions-item>
            <el-descriptions-item label="预测时间">{{ getPredictionTime() }}</el-descriptions-item>
            <el-descriptions-item label="置信度">{{ getConfidenceLevel() }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 预测结果分布图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 预测结果分布</span>
            <el-tooltip content="显示预测值的分布情况" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="predictionDistributionChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 预测置信度图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🎯 预测置信度</span>
            <el-tooltip content="显示每个预测值的置信度" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="confidenceChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 特征重要性图 (如果有) -->
      <el-col v-if="hasFeatureImportance" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🔍 特征重要性</span>
            <el-tooltip content="显示各特征对预测结果的重要程度" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="featureImportanceChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 预测趋势图 (如果是时间序列预测) -->
      <el-col v-if="isTimeSeriesPrediction" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📈 预测趋势</span>
            <el-tooltip content="显示时间序列的预测趋势" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="trendChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 分类预测概率图 (如果是分类预测) -->
      <el-col v-if="isClassificationPrediction" :span="24" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🎯 分类预测概率</span>
            <el-tooltip content="显示各类别的预测概率分布" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="classificationProbChart" class="large-chart"></div>
        </el-card>
      </el-col>

      <!-- 回归预测散点图 (如果是回归预测) -->
      <el-col v-if="isRegressionPrediction" :span="24" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 回归预测结果</span>
            <el-tooltip content="显示回归预测的结果分布" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="regressionPredictionChart" class="large-chart"></div>
        </el-card>
      </el-col>

      <!-- 预测结果详细表格 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="results-card" shadow="never">
          <div slot="header">
            <span>📋 预测结果详情</span>
            <el-button 
              type="primary" 
              size="mini" 
              icon="el-icon-download"
              @click="exportPredictions"
              style="float: right;">
              导出预测结果
            </el-button>
          </div>
          
          <el-table 
            :data="predictionResults" 
            border 
            stripe 
            style="width: 100%;"
            max-height="400">
            <el-table-column type="index" label="序号" width="60"></el-table-column>
            
            <!-- 输入特征列 -->
            <el-table-column 
              v-for="feature in inputFeatures" 
              :key="feature"
              :prop="feature" 
              :label="feature"
              width="120">
              <template slot-scope="scope">
                <span>{{ formatValue(scope.row[feature]) }}</span>
              </template>
            </el-table-column>
            
            <!-- 预测结果列 -->
            <el-table-column prop="prediction" label="预测值" width="120">
              <template slot-scope="scope">
                <strong>{{ formatValue(scope.row.prediction) }}</strong>
              </template>
            </el-table-column>
            
            <!-- 置信度列 (如果有) -->
            <el-table-column v-if="hasConfidence" prop="confidence" label="置信度" width="100">
              <template slot-scope="scope">
                <el-progress 
                  :percentage="scope.row.confidence * 100" 
                  :stroke-width="8"
                  :show-text="false">
                </el-progress>
                <span style="margin-left: 5px;">{{ formatPercentage(scope.row.confidence) }}</span>
              </template>
            </el-table-column>
            
            <!-- 预测概率列 (如果是分类) -->
            <el-table-column v-if="isClassificationPrediction" prop="probabilities" label="预测概率">
              <template slot-scope="scope">
                <div v-if="scope.row.probabilities">
                  <el-tag 
                    v-for="(prob, className) in scope.row.probabilities" 
                    :key="className"
                    size="mini"
                    style="margin-right: 5px;">
                    {{ className }}: {{ formatPercentage(prob) }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 模型信息表 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="model-info-card" shadow="never">
          <div slot="header">
            <span>🤖 使用的模型信息</span>
          </div>
          
          <el-descriptions :column="2" border>
            <el-descriptions-item label="模型类型">{{ getModelType() }}</el-descriptions-item>
            <el-descriptions-item label="模型版本">{{ getModelVersion() }}</el-descriptions-item>
            <el-descriptions-item label="训练时间">{{ getTrainingTime() }}</el-descriptions-item>
            <el-descriptions-item label="模型大小">{{ getModelSize() }}</el-descriptions-item>
            <el-descriptions-item label="训练样本数">{{ getTrainingSamples() }}</el-descriptions-item>
            <el-descriptions-item label="验证准确率">{{ getValidationAccuracy() }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "PredictionVisualization",
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
      predictionDistributionChart: null,
      confidenceChart: null,
      featureImportanceChart: null,
      trendChart: null,
      classificationProbChart: null,
      regressionPredictionChart: null
    };
  },
  computed: {
    predictionResults() {
      const predictions = this.results.predictions || [];
      const inputData = this.results.input_data || [];
      const confidences = this.results.confidences || [];
      const probabilities = this.results.probabilities || [];
      
      return predictions.map((prediction, index) => ({
        ...inputData[index],
        prediction: prediction,
        confidence: confidences[index],
        probabilities: probabilities[index]
      }));
    },
    
    inputFeatures() {
      const modelParams = this.results.model_params || {};
      return modelParams.feature_columns || [];
    },
    
    hasFeatureImportance() {
      return this.results.feature_importance || 
             (this.results.statistics && this.results.statistics.feature_importance);
    },
    
    hasConfidence() {
      return this.results.confidences && this.results.confidences.length > 0;
    },
    
    isTimeSeriesPrediction() {
      return this.algorithmType.toLowerCase().includes('lstm') ||
             this.algorithmType.toLowerCase().includes('tcn') ||
             this.algorithmType.toLowerCase().includes('bilstm');
    },
    
    isClassificationPrediction() {
      return this.algorithmType.toLowerCase().includes('classification') ||
             this.algorithmType.toLowerCase().includes('logistic') ||
             this.algorithmType.toLowerCase().includes('knn_predict') ||
             this.algorithmType.toLowerCase().includes('svm_predict');
    },
    
    isRegressionPrediction() {
      return this.algorithmType.toLowerCase().includes('regression_predict') ||
             this.algorithmType.toLowerCase().includes('predict') && 
             !this.isClassificationPrediction;
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initializeCharts();
    });
  },
  beforeDestroy() {
    // 销毁图表实例
    [this.predictionDistributionChart, this.confidenceChart, this.featureImportanceChart,
     this.trendChart, this.classificationProbChart, this.regressionPredictionChart].forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    initializeCharts() {
      this.renderPredictionDistributionChart();
      
      if (this.hasConfidence) {
        this.renderConfidenceChart();
      }
      
      if (this.hasFeatureImportance) {
        this.renderFeatureImportanceChart();
      }
      
      if (this.isTimeSeriesPrediction) {
        this.renderTrendChart();
      }
      
      if (this.isClassificationPrediction) {
        this.renderClassificationProbChart();
      }
      
      if (this.isRegressionPrediction) {
        this.renderRegressionPredictionChart();
      }
    },
    
    getAlgorithmDisplayName() {
      const names = {
        'linear_regression_predict': '线性回归预测',
        'polynomial_regression_predict': '多项式回归预测',
        'svm_regression_predict': 'SVM回归预测',
        'random_forest_regression_predict': '随机森林回归预测',
        'xgboost_regression_predict': 'XGBoost回归预测',
        'lightgbm_regression_predict': 'LightGBM回归预测',
        'bilstm_regression_predict': 'BiLSTM回归预测',
        'tcn_regression_predict': 'TCN回归预测',
        'knn_predict': 'KNN预测',
        'svm_predict': 'SVM预测'
      };
      return names[this.algorithmType] || this.algorithmType;
    },
    
    getPredictionCategory() {
      if (this.isClassificationPrediction) return '分类预测';
      if (this.isRegressionPrediction) return '回归预测';
      if (this.isTimeSeriesPrediction) return '时间序列预测';
      return '预测任务';
    },
    
    getPredictionTagType() {
      if (this.isClassificationPrediction) return 'success';
      if (this.isRegressionPrediction) return 'primary';
      if (this.isTimeSeriesPrediction) return 'warning';
      return 'info';
    },
    
    getPredictionType() {
      return this.getPredictionCategory();
    },
    
    getPredictionCount() {
      return this.results.predictions ? this.results.predictions.length : 0;
    },
    
    getUsedModel() {
      return this.results.model_info?.model_name || '未知模型';
    },
    
    getInputFeatures() {
      return this.inputFeatures.join(', ') || '未知特征';
    },
    
    getPredictionTime() {
      return this.results.prediction_time || '未知时间';
    },
    
    getConfidenceLevel() {
      if (!this.hasConfidence) return 'N/A';
      const avgConfidence = this.results.confidences.reduce((sum, conf) => sum + conf, 0) / this.results.confidences.length;
      return this.formatPercentage(avgConfidence);
    },
    
    formatValue(value) {
      if (value === null || value === undefined) {
        return 'N/A';
      }
      if (typeof value === 'number') {
        return Number(value).toFixed(4);
      }
      return String(value);
    },
    
    formatPercentage(value) {
      if (value === null || value === undefined || isNaN(value)) {
        return 'N/A';
      }
      return (Number(value) * 100).toFixed(2) + '%';
    },
    
    exportPredictions() {
      // 导出预测结果为CSV
      const csvContent = this.convertToCSV(this.predictionResults);
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const link = document.createElement('a');
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', `prediction_results_${Date.now()}.csv`);
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      this.$message.success('预测结果已导出');
    },
    
    convertToCSV(data) {
      if (!data.length) return '';

      const headers = Object.keys(data[0]);
      const csvRows = [headers.join(',')];

      data.forEach(row => {
        const values = headers.map(header => {
          const value = row[header];
          return typeof value === 'object' ? JSON.stringify(value) : value;
        });
        csvRows.push(values.join(','));
      });

      return csvRows.join('\n');
    },

    /** 渲染预测结果分布图 */
    renderPredictionDistributionChart() {
      const chartDom = this.$refs.predictionDistributionChart;
      if (!chartDom) return;

      if (this.predictionDistributionChart) {
        this.predictionDistributionChart.dispose();
      }

      this.predictionDistributionChart = echarts.init(chartDom);

      const predictions = this.results.predictions || [];

      if (predictions.length === 0) {
        this.predictionDistributionChart.setOption({
          title: {
            text: '暂无预测数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      const option = {
        title: {
          text: '预测结果分布',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: predictions.map((_, index) => `样本${index + 1}`)
        },
        yAxis: {
          type: 'value',
          name: '预测值'
        },
        series: [{
          type: 'bar',
          data: predictions,
          itemStyle: { color: '#409EFF' }
        }]
      };

      this.predictionDistributionChart.setOption(option);
      this.$emit('chart-ready', this.predictionDistributionChart);
    },

    /** 渲染置信度图 */
    renderConfidenceChart() {
      const chartDom = this.$refs.confidenceChart;
      if (!chartDom) return;

      if (this.confidenceChart) {
        this.confidenceChart.dispose();
      }

      this.confidenceChart = echarts.init(chartDom);

      const confidences = this.results.confidences || [];

      if (confidences.length === 0) {
        this.confidenceChart.setOption({
          title: {
            text: '暂无置信度数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      const option = {
        title: {
          text: '预测置信度',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: {c}%'
        },
        xAxis: {
          type: 'category',
          data: confidences.map((_, index) => `样本${index + 1}`)
        },
        yAxis: {
          type: 'value',
          name: '置信度 (%)',
          min: 0,
          max: 100
        },
        series: [{
          type: 'line',
          data: confidences.map(conf => conf * 100),
          lineStyle: { color: '#67C23A', width: 2 },
          symbol: 'circle',
          symbolSize: 4
        }]
      };

      this.confidenceChart.setOption(option);
      this.$emit('chart-ready', this.confidenceChart);
    },

    /** 渲染特征重要性图 */
    renderFeatureImportanceChart() {
      const chartDom = this.$refs.featureImportanceChart;
      if (!chartDom) return;

      if (this.featureImportanceChart) {
        this.featureImportanceChart.dispose();
      }

      this.featureImportanceChart = echarts.init(chartDom);

      const featureImportance = this.results.feature_importance ||
                               (this.results.statistics && this.results.statistics.feature_importance);

      if (!featureImportance) {
        this.featureImportanceChart.setOption({
          title: {
            text: '暂无特征重要性数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      const features = Object.keys(featureImportance);
      const importances = Object.values(featureImportance);

      const option = {
        title: {
          text: '特征重要性',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        xAxis: {
          type: 'category',
          data: features,
          axisLabel: { rotate: 45 }
        },
        yAxis: {
          type: 'value',
          name: '重要性'
        },
        series: [{
          type: 'bar',
          data: importances,
          itemStyle: { color: '#E6A23C' }
        }]
      };

      this.featureImportanceChart.setOption(option);
      this.$emit('chart-ready', this.featureImportanceChart);
    },

    /** 渲染趋势图 */
    renderTrendChart() {
      const chartDom = this.$refs.trendChart;
      if (!chartDom) return;

      if (this.trendChart) {
        this.trendChart.dispose();
      }

      this.trendChart = echarts.init(chartDom);

      const predictions = this.results.predictions || [];

      if (predictions.length === 0) {
        this.trendChart.setOption({
          title: {
            text: '暂无趋势数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      const option = {
        title: {
          text: '预测趋势',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: predictions.map((_, index) => `时间点${index + 1}`)
        },
        yAxis: {
          type: 'value',
          name: '预测值'
        },
        series: [{
          type: 'line',
          data: predictions,
          smooth: true,
          lineStyle: { color: '#409EFF', width: 2 },
          symbol: 'circle',
          symbolSize: 4
        }]
      };

      this.trendChart.setOption(option);
      this.$emit('chart-ready', this.trendChart);
    },

    /** 渲染分类预测概率图 */
    renderClassificationProbChart() {
      const chartDom = this.$refs.classificationProbChart;
      if (!chartDom) return;

      if (this.classificationProbChart) {
        this.classificationProbChart.dispose();
      }

      this.classificationProbChart = echarts.init(chartDom);

      const probabilities = this.results.probabilities || [];

      if (probabilities.length === 0) {
        this.classificationProbChart.setOption({
          title: {
            text: '暂无分类概率数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      // 简化的分类概率可视化
      const option = {
        title: {
          text: '分类预测概率分布',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['类别1', '类别2'],
          bottom: 10
        },
        xAxis: {
          type: 'category',
          data: probabilities.map((_, index) => `样本${index + 1}`)
        },
        yAxis: {
          type: 'value',
          name: '概率',
          min: 0,
          max: 1
        },
        series: [
          {
            name: '类别1',
            type: 'bar',
            data: probabilities.map(prob => Array.isArray(prob) ? prob[0] : 0.5),
            itemStyle: { color: '#409EFF' }
          },
          {
            name: '类别2',
            type: 'bar',
            data: probabilities.map(prob => Array.isArray(prob) ? prob[1] : 0.5),
            itemStyle: { color: '#67C23A' }
          }
        ]
      };

      this.classificationProbChart.setOption(option);
      this.$emit('chart-ready', this.classificationProbChart);
    },

    /** 渲染回归预测图 */
    renderRegressionPredictionChart() {
      const chartDom = this.$refs.regressionPredictionChart;
      if (!chartDom) return;

      if (this.regressionPredictionChart) {
        this.regressionPredictionChart.dispose();
      }

      this.regressionPredictionChart = echarts.init(chartDom);

      const predictions = this.results.predictions || [];

      if (predictions.length === 0) {
        this.regressionPredictionChart.setOption({
          title: {
            text: '暂无回归预测数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      const option = {
        title: {
          text: '回归预测结果',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: predictions.map((_, index) => `样本${index + 1}`)
        },
        yAxis: {
          type: 'value',
          name: '预测值'
        },
        series: [{
          type: 'scatter',
          data: predictions.map((pred, index) => [index, pred]),
          symbolSize: 8,
          itemStyle: { color: '#409EFF' }
        }]
      };

      this.regressionPredictionChart.setOption(option);
      this.$emit('chart-ready', this.regressionPredictionChart);
    },

    /** 获取模型类型 */
    getModelType() {
      return this.results.model_info?.model_type || '未知类型';
    },

    /** 获取模型版本 */
    getModelVersion() {
      return this.results.model_info?.model_version || 'v1.0';
    },

    /** 获取训练时间 */
    getTrainingTime() {
      return this.results.model_info?.training_time || '未知时间';
    },

    /** 获取模型大小 */
    getModelSize() {
      return this.results.model_info?.model_size || '未知大小';
    },

    /** 获取训练样本数 */
    getTrainingSamples() {
      return this.results.model_info?.training_samples || '未知数量';
    },

    /** 获取验证准确率 */
    getValidationAccuracy() {
      return this.results.model_info?.validation_accuracy || 'N/A';
    }
  }
};
</script>

<style scoped>
.prediction-visualization {
  padding: 0;
}

.info-card, .chart-card, .results-card, .model-info-card {
  margin-bottom: 20px;
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
