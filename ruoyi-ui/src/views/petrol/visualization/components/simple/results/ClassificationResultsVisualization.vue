<template>
  <div class="classification-results-visualization">
    <!-- 性能指标卡片 -->
    <el-card class="metrics-card" shadow="never" style="margin-bottom: 20px;">
      <div slot="header">
        <span>📊 {{ getAlgorithmDisplayName() }} - 分类性能指标</span>
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
      
      <!-- 如果没有指标，显示提示 -->
      <el-empty v-if="performanceMetrics.length === 0" 
                description="暂无分类性能指标数据" 
                :image-size="100">
      </el-empty>
    </el-card>

    <!-- 可视化图表 -->
    <el-row :gutter="20">
      <!-- 混淆矩阵 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 混淆矩阵</span>
            <el-tooltip content="显示分类结果的详细情况" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="confusionMatrixChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 分类结果分布 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🎯 分类结果分布</span>
            <el-tooltip content="显示各类别的预测分布" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="classDistributionChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 特征重要性图 (如果有) -->
      <el-col v-if="hasFeatureImportance" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🔍 特征重要性</span>
            <el-tooltip content="显示各特征对分类结果的重要程度" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="featureImportanceChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- ROC曲线 (如果有概率数据) -->
      <el-col v-if="hasProbabilities" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📈 ROC曲线</span>
            <el-tooltip content="显示分类器的性能曲线" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="rocCurveChart" class="chart"></div>
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
  name: "ClassificationResultsVisualization",
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
      confusionMatrixChart: null,
      classDistributionChart: null,
      featureImportanceChart: null,
      rocCurveChart: null
    };
  },
  computed: {
    performanceMetrics() {
      const stats = this.results.statistics || this.results.metrics || {};
      const metrics = [];

      // 准确率
      const accuracy = stats.accuracy;
      if (accuracy !== undefined) {
        metrics.push({
          key: 'accuracy',
          label: '准确率',
          value: this.formatPercentage(accuracy),
          description: '正确预测的比例'
        });
      }

      // 精确率
      const precision = stats.precision;
      if (precision !== undefined) {
        metrics.push({
          key: 'precision',
          label: '精确率',
          value: this.formatPercentage(precision),
          description: '预测为正例中实际为正例的比例'
        });
      }

      // 召回率
      const recall = stats.recall;
      if (recall !== undefined) {
        metrics.push({
          key: 'recall',
          label: '召回率',
          value: this.formatPercentage(recall),
          description: '实际正例中被正确预测的比例'
        });
      }

      // F1分数
      const f1Score = stats.f1_score || stats.f1;
      if (f1Score !== undefined) {
        metrics.push({
          key: 'f1_score',
          label: 'F1分数',
          value: this.formatPercentage(f1Score),
          description: '精确率和召回率的调和平均数'
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
      const importance = this.results.feature_importance ||
                        (this.results.statistics && this.results.statistics.feature_importance);
      
      if (!importance) return false;
      
      if (typeof importance === 'object' && !Array.isArray(importance)) {
        return Object.keys(importance).length > 0;
      }
      
      if (Array.isArray(importance)) {
        return importance.length > 0;
      }
      
      return false;
    },

    hasProbabilities() {
      const probabilities = this.results.probabilities || this.results.y_prob || [];
      return probabilities.length > 0;
    }
  },
  mounted() {
    console.log('🎨 分类可视化组件挂载');
    console.log('📊 接收到的结果数据:', this.results);
    console.log('📋 接收到的任务信息:', this.taskInfo);

    this.$nextTick(() => {
      this.initializeCharts();
    });
  },
  beforeDestroy() {
    // 销毁图表实例
    [this.confusionMatrixChart, this.classDistributionChart, this.featureImportanceChart, this.rocCurveChart].forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    initializeCharts() {
      console.log('🎨 开始初始化分类可视化图表');
      
      setTimeout(() => {
        this.renderConfusionMatrixChart();
        this.renderClassDistributionChart();

        if (this.hasFeatureImportance) {
          this.renderFeatureImportanceChart();
        }

        if (this.hasProbabilities) {
          this.renderROCCurveChart();
        }

        console.log('✅ 分类可视化图表初始化完成');
      }, 100);
    },
    
    getAlgorithmDisplayName() {
      const names = {
        'logistic_regression': '逻辑回归',
        'svm_classification': 'SVM分类',
        'random_forest_classification': '随机森林分类',
        'knn_classification': 'KNN分类',
        'decision_tree_classification': '决策树分类',
        'naive_bayes': '朴素贝叶斯',
        'neural_network_classification': '神经网络分类'
      };
      return names[this.algorithmType] || this.algorithmType;
    },
    
    getPerformanceLevel() {
      const accuracy = this.results.statistics?.accuracy || this.results.metrics?.accuracy;
      if (accuracy === undefined) return '未知';
      
      if (accuracy >= 0.95) return '优秀';
      if (accuracy >= 0.85) return '良好';
      if (accuracy >= 0.70) return '一般';
      return '较差';
    },
    
    getPerformanceTagType() {
      const accuracy = this.results.statistics?.accuracy || this.results.metrics?.accuracy;
      if (accuracy === undefined) return 'info';
      
      if (accuracy >= 0.95) return 'success';
      if (accuracy >= 0.85) return 'primary';
      if (accuracy >= 0.70) return 'warning';
      return 'danger';
    },
    
    formatPercentage(value) {
      if (value === null || value === undefined || isNaN(value)) {
        return 'N/A';
      }
      return (Number(value) * 100).toFixed(2) + '%';
    },
    
    getParameterDisplayName(key) {
      const names = {
        'C': '正则化参数C',
        'gamma': 'Gamma参数',
        'kernel': '核函数',
        'n_estimators': '树的数量',
        'max_depth': '最大深度',
        'n_neighbors': '邻居数量',
        'random_state': '随机种子'
      };
      return names[key] || key;
    },
    
    formatParameterValue(value) {
      if (typeof value === 'number') {
        return Number(value).toFixed(4);
      }
      return String(value);
    },
    
    getParameterDescription(key) {
      const descriptions = {
        'C': 'SVM的正则化参数',
        'gamma': 'RBF核的参数',
        'kernel': 'SVM使用的核函数类型',
        'n_estimators': '随机森林中树的数量',
        'max_depth': '决策树的最大深度',
        'n_neighbors': 'KNN算法的邻居数量',
        'random_state': '随机数生成器的种子'
      };
      return descriptions[key] || '模型参数';
    },

    /** 渲染混淆矩阵图 */
    renderConfusionMatrixChart() {
      const chartDom = this.$refs.confusionMatrixChart;
      if (!chartDom) return;

      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.confusionMatrixChart) {
        this.confusionMatrixChart.dispose();
      }

      this.confusionMatrixChart = echarts.init(chartDom);

      // 获取混淆矩阵数据
      const confusionMatrix = this.results.confusion_matrix || [];

      if (confusionMatrix.length === 0) {
        this.confusionMatrixChart.setOption({
          title: {
            text: '暂无混淆矩阵数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 准备热力图数据
      const data = [];
      const classNames = this.results.class_names || [];
      
      for (let i = 0; i < confusionMatrix.length; i++) {
        for (let j = 0; j < confusionMatrix[i].length; j++) {
          data.push([i, j, confusionMatrix[i][j]]);
        }
      }

      const option = {
        title: {
          text: '混淆矩阵',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          position: 'top',
          formatter: function(params) {
            return `实际: ${classNames[params.data[0]] || params.data[0]}<br/>预测: ${classNames[params.data[1]] || params.data[1]}<br/>数量: ${params.data[2]}`;
          }
        },
        grid: {
          height: '50%',
          top: '10%'
        },
        xAxis: {
          type: 'category',
          data: classNames.length > 0 ? classNames : Array.from({length: confusionMatrix.length}, (_, i) => `类别${i}`),
          splitArea: {
            show: true
          },
          name: '预测类别'
        },
        yAxis: {
          type: 'category',
          data: classNames.length > 0 ? classNames : Array.from({length: confusionMatrix.length}, (_, i) => `类别${i}`),
          splitArea: {
            show: true
          },
          name: '实际类别'
        },
        visualMap: {
          min: 0,
          max: Math.max(...data.map(item => item[2])),
          calculable: true,
          orient: 'horizontal',
          left: 'center',
          bottom: '15%'
        },
        series: [{
          name: '混淆矩阵',
          type: 'heatmap',
          data: data,
          label: {
            show: true
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }]
      };

      this.confusionMatrixChart.setOption(option);
    },

    /** 渲染分类结果分布图 */
    renderClassDistributionChart() {
      const chartDom = this.$refs.classDistributionChart;
      if (!chartDom) return;

      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.classDistributionChart) {
        this.classDistributionChart.dispose();
      }

      this.classDistributionChart = echarts.init(chartDom);

      // 获取预测结果数据
      const predictions = this.results.predictions || this.results.y_pred || [];
      const classNames = this.results.class_names || [];

      if (predictions.length === 0) {
        this.classDistributionChart.setOption({
          title: {
            text: '暂无分类结果数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 统计各类别的数量
      const classCounts = {};
      predictions.forEach(pred => {
        classCounts[pred] = (classCounts[pred] || 0) + 1;
      });

      const data = Object.entries(classCounts).map(([className, count]) => ({
        name: classNames[className] || `类别${className}`,
        value: count
      }));

      const option = {
        title: {
          text: '分类结果分布',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [
          {
            name: '分类结果',
            type: 'pie',
            radius: '50%',
            data: data,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      };

      this.classDistributionChart.setOption(option);
    }
  }
};
</script>

<style scoped>
.classification-results-visualization {
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
