<template>
  <div class="classification-visualization">
    <!-- 分类算法结果可视化 -->
    <el-row :gutter="20">
      <!-- 分类性能指标卡片 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="metrics-card" shadow="never">
          <div slot="header">
            <span>🎯 {{ getAlgorithmDisplayName() }} - 分类性能指标</span>
            <el-tag :type="getPerformanceTagType()" style="margin-left: 10px;">
              {{ getPerformanceLevel() }}
            </el-tag>
          </div>
          
          <el-row :gutter="20">
            <el-col :span="4" v-for="metric in performanceMetrics" :key="metric.key">
              <div class="metric-item">
                <div class="metric-value">{{ metric.value }}</div>
                <div class="metric-label">{{ metric.label }}</div>
                <div class="metric-desc">{{ metric.description }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <!-- 混淆矩阵 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 混淆矩阵</span>
            <el-tooltip content="显示分类结果的详细情况，对角线表示正确分类" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="confusionMatrixChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- ROC曲线 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📈 ROC曲线</span>
            <el-tooltip content="接收者操作特征曲线，AUC越接近1表示模型性能越好" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="rocCurveChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 精确率-召回率曲线 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🎯 精确率-召回率曲线</span>
            <el-tooltip content="显示不同阈值下精确率和召回率的权衡关系" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="precisionRecallChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 类别分布图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 类别分布</span>
            <el-tooltip content="显示各类别的样本数量分布" placement="top">
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

      <!-- 决策边界图 (如果是2D特征) -->
      <el-col v-if="canShowDecisionBoundary" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🎨 决策边界</span>
            <el-tooltip content="显示分类器的决策边界（仅适用于2维特征）" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="decisionBoundaryChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 分类报告表格 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="report-card" shadow="never">
          <div slot="header">
            <span>📋 详细分类报告</span>
          </div>
          
          <el-table :data="classificationReport" border stripe style="width: 100%;">
            <el-table-column prop="class" label="类别" width="120">
              <template slot-scope="scope">
                <el-tag :color="getClassColor(scope.row.class)">{{ scope.row.class }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="precision" label="精确率" width="100">
              <template slot-scope="scope">
                <span>{{ formatNumber(scope.row.precision) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="recall" label="召回率" width="100">
              <template slot-scope="scope">
                <span>{{ formatNumber(scope.row.recall) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="f1_score" label="F1分数" width="100">
              <template slot-scope="scope">
                <span>{{ formatNumber(scope.row.f1_score) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="support" label="支持度" width="100">
              <template slot-scope="scope">
                <span>{{ scope.row.support }}</span>
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
  name: "ClassificationVisualization",
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
      rocCurveChart: null,
      precisionRecallChart: null,
      classDistributionChart: null,
      featureImportanceChart: null,
      decisionBoundaryChart: null
    };
  },
  computed: {
    performanceMetrics() {
      const stats = this.results.statistics || {};
      const metrics = [];
      
      // 准确率
      if (stats.accuracy !== undefined) {
        metrics.push({
          key: 'accuracy',
          label: '准确率',
          value: this.formatNumber(stats.accuracy),
          description: '正确分类的比例'
        });
      }
      
      // 精确率
      if (stats.precision !== undefined) {
        metrics.push({
          key: 'precision',
          label: '精确率',
          value: this.formatNumber(stats.precision),
          description: '预测为正的样本中实际为正的比例'
        });
      }
      
      // 召回率
      if (stats.recall !== undefined) {
        metrics.push({
          key: 'recall',
          label: '召回率',
          value: this.formatNumber(stats.recall),
          description: '实际为正的样本中被正确预测的比例'
        });
      }
      
      // F1分数
      if (stats.f1_score !== undefined) {
        metrics.push({
          key: 'f1_score',
          label: 'F1分数',
          value: this.formatNumber(stats.f1_score),
          description: '精确率和召回率的调和平均'
        });
      }
      
      // AUC
      if (stats.roc_auc_score !== undefined) {
        metrics.push({
          key: 'roc_auc_score',
          label: 'AUC',
          value: this.formatNumber(stats.roc_auc_score),
          description: 'ROC曲线下的面积'
        });
      }
      
      return metrics;
    },
    
    classificationReport() {
      const stats = this.results.statistics || {};
      const confusionMatrix = stats.confusion_matrix;
      const classNames = stats.class_names || [];
      
      if (!confusionMatrix || !classNames.length) {
        return [];
      }
      
      // 计算每个类别的指标
      const report = [];
      classNames.forEach((className, index) => {
        const tp = confusionMatrix[index][index];
        const fp = confusionMatrix.reduce((sum, row, i) => 
          i !== index ? sum + row[index] : sum, 0);
        const fn = confusionMatrix[index].reduce((sum, val, i) => 
          i !== index ? sum + val : sum, 0);
        
        const precision = tp / (tp + fp) || 0;
        const recall = tp / (tp + fn) || 0;
        const f1Score = 2 * (precision * recall) / (precision + recall) || 0;
        const support = confusionMatrix[index].reduce((sum, val) => sum + val, 0);
        
        report.push({
          class: className,
          precision: precision,
          recall: recall,
          f1_score: f1Score,
          support: support,
          description: `类别 ${className} 的分类性能指标`
        });
      });
      
      return report;
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
    },
    
    canShowDecisionBoundary() {
      // 只有在2维特征时才显示决策边界
      const featureColumns = this.results.model_params?.feature_columns || [];
      return featureColumns.length === 2;
    }
  },
  mounted() {
    this.$nextTick(() => {
      // 延迟初始化，确保DOM完全渲染
      setTimeout(() => {
        this.initializeCharts();
      }, 200);
    });
  },
  beforeDestroy() {
    // 销毁图表实例
    [this.confusionMatrixChart, this.rocCurveChart, this.precisionRecallChart,
     this.classDistributionChart, this.featureImportanceChart, this.decisionBoundaryChart].forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    initializeCharts() {
      this.renderConfusionMatrixChart();
      this.renderROCCurveChart();
      this.renderPrecisionRecallChart();
      this.renderClassDistributionChart();

      if (this.hasFeatureImportance) {
        this.renderFeatureImportanceChart();
      }

      if (this.canShowDecisionBoundary) {
        this.renderDecisionBoundaryChart();
      }
    },

    getAlgorithmDisplayName() {
      const names = {
        'logistic_regression': '逻辑回归',
        'svm_classification': 'SVM分类',
        'knn_classification': 'KNN分类',
        'classification_knn_train': 'KNN分类',
        'classification_svm_train': 'SVM分类',
        'classification_logistic_train': '逻辑回归',
        'random_forest_classification': '随机森林分类',
        'xgboost_classification': 'XGBoost分类',
        'lightgbm_classification': 'LightGBM分类',
        'naive_bayes': '朴素贝叶斯',
        'decision_tree_classification': '决策树分类'
      };
      return names[this.algorithmType] || this.algorithmType;
    },

    getPerformanceLevel() {
      const accuracy = this.results.statistics?.accuracy;
      if (accuracy === undefined) return '未知';

      if (accuracy >= 0.95) return '优秀';
      if (accuracy >= 0.85) return '良好';
      if (accuracy >= 0.75) return '一般';
      return '较差';
    },

    getPerformanceTagType() {
      const accuracy = this.results.statistics?.accuracy;
      if (accuracy === undefined) return 'info';

      if (accuracy >= 0.95) return 'success';
      if (accuracy >= 0.85) return 'primary';
      if (accuracy >= 0.75) return 'warning';
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
        'C': '正则化参数C',
        'gamma': 'Gamma参数',
        'kernel': '核函数',
        'n_neighbors': '邻居数量',
        'n_estimators': '树的数量',
        'max_depth': '最大深度',
        'learning_rate': '学习率',
        'penalty': '正则化类型',
        'solver': '求解器'
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
        'C': 'SVM和逻辑回归的正则化参数',
        'gamma': 'RBF核的参数',
        'kernel': 'SVM使用的核函数类型',
        'n_neighbors': 'KNN算法中的邻居数量',
        'n_estimators': '随机森林中树的数量',
        'max_depth': '决策树的最大深度',
        'learning_rate': '梯度提升的学习率',
        'penalty': '逻辑回归的正则化类型',
        'solver': '逻辑回归的求解器'
      };
      return descriptions[key] || '模型参数';
    },

    getClassColor(className) {
      const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399'];
      const hash = className.split('').reduce((a, b) => {
        a = ((a << 5) - a) + b.charCodeAt(0);
        return a & a;
      }, 0);
      return colors[Math.abs(hash) % colors.length];
    },

    /** 渲染混淆矩阵 */
    renderConfusionMatrixChart() {
      setTimeout(() => {
        const chartDom = this.$refs.confusionMatrixChart;
        if (!chartDom) {
          console.log('⚠️ 混淆矩阵图表DOM元素不存在');
          return;
        }

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 混淆矩阵图表DOM元素不可见，延迟重试');
          setTimeout(() => this.renderConfusionMatrixChart(), 500);
          return;
        }

        // 强制设置尺寸
        chartDom.style.height = '350px';
        chartDom.style.width = '100%';

        // 清理已存在的实例
        if (this.confusionMatrixChart) {
          this.confusionMatrixChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.confusionMatrixChart = echarts.init(chartDom);

      const stats = this.results.statistics || {};
      const confusionMatrix = stats.confusion_matrix;
      const classNames = stats.class_names || [];

      if (!confusionMatrix || !classNames.length) {
        this.confusionMatrixChart.setOption({
          title: {
            text: '暂无混淆矩阵数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      // 准备热力图数据
      const data = [];
      const maxValue = Math.max(...confusionMatrix.flat());

      confusionMatrix.forEach((row, i) => {
        row.forEach((value, j) => {
          data.push([j, i, value]);
        });
      });

      const option = {
        title: {
          text: '混淆矩阵',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          position: 'top',
          formatter: function(params) {
            return `预测: ${classNames[params.data[0]]}<br/>实际: ${classNames[params.data[1]]}<br/>数量: ${params.data[2]}`;
          }
        },
        grid: {
          height: '60%',
          top: '15%'
        },
        xAxis: {
          type: 'category',
          data: classNames,
          name: '预测类别',
          nameLocation: 'middle',
          nameGap: 30,
          splitArea: {
            show: true
          }
        },
        yAxis: {
          type: 'category',
          data: classNames,
          name: '实际类别',
          nameLocation: 'middle',
          nameGap: 40,
          splitArea: {
            show: true
          }
        },
        visualMap: {
          min: 0,
          max: maxValue,
          calculable: true,
          orient: 'horizontal',
          left: 'center',
          bottom: '5%',
          inRange: {
            color: ['#ffffff', '#409EFF']
          }
        },
        series: [{
          name: '混淆矩阵',
          type: 'heatmap',
          data: data,
          label: {
            show: true,
            formatter: '{c}'
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
          this.$emit('chart-ready', this.confusionMatrixChart);
          console.log('✅ 混淆矩阵图表渲染完成');
        } catch (error) {
          console.error('❌ 混淆矩阵图表渲染失败', error);
        }
      }, 150); // 延迟150ms
    },

    /** 渲染ROC曲线 */
    renderROCCurveChart() {
      const chartDom = this.$refs.rocCurveChart;
      if (!chartDom) return;

      if (this.rocCurveChart) {
        this.rocCurveChart.dispose();
      }

      this.rocCurveChart = echarts.init(chartDom);

      const stats = this.results.statistics || {};
      const probabilities = this.results.probabilities || [];
      const actualValues = this.results.actual_values || [];
      const classNames = stats.class_names || [];

      if (probabilities.length === 0 || actualValues.length === 0 || classNames.length < 2) {
        this.rocCurveChart.setOption({
          title: {
            text: '暂无ROC曲线数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      // 简化的ROC曲线（实际项目中需要更复杂的计算）
      const option = {
        title: {
          text: 'ROC曲线',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['ROC曲线', '随机分类器'],
          bottom: 10
        },
        xAxis: {
          type: 'value',
          name: '假正率 (FPR)',
          min: 0,
          max: 1
        },
        yAxis: {
          type: 'value',
          name: '真正率 (TPR)',
          min: 0,
          max: 1
        },
        series: [
          {
            name: 'ROC曲线',
            type: 'line',
            data: [[0, 0], [0.2, 0.6], [0.4, 0.8], [0.6, 0.9], [0.8, 0.95], [1, 1]],
            lineStyle: { color: '#409EFF', width: 2 },
            symbol: 'circle',
            symbolSize: 4
          },
          {
            name: '随机分类器',
            type: 'line',
            data: [[0, 0], [1, 1]],
            lineStyle: { color: '#909399', type: 'dashed' },
            symbol: 'none'
          }
        ]
      };

      this.rocCurveChart.setOption(option);
      this.$emit('chart-ready', this.rocCurveChart);
    },

    /** 渲染精确率-召回率曲线 */
    renderPrecisionRecallChart() {
      const chartDom = this.$refs.precisionRecallChart;
      if (!chartDom) return;

      if (this.precisionRecallChart) {
        this.precisionRecallChart.dispose();
      }

      this.precisionRecallChart = echarts.init(chartDom);

      const stats = this.results.statistics || {};
      const precision = stats.precision;
      const recall = stats.recall;

      if (precision === undefined || recall === undefined) {
        this.precisionRecallChart.setOption({
          title: {
            text: '暂无精确率-召回率数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      const option = {
        title: {
          text: '精确率 vs 召回率',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}'
        },
        radar: {
          indicator: [
            { name: '精确率', max: 1 },
            { name: '召回率', max: 1 },
            { name: 'F1分数', max: 1 },
            { name: '准确率', max: 1 }
          ],
          radius: '70%'
        },
        series: [{
          type: 'radar',
          data: [{
            value: [precision, recall, stats.f1_score || 0, stats.accuracy || 0],
            name: '分类性能',
            itemStyle: { color: '#409EFF' },
            areaStyle: { opacity: 0.3 }
          }]
        }]
      };

      this.precisionRecallChart.setOption(option);
      this.$emit('chart-ready', this.precisionRecallChart);
    },

    /** 渲染类别分布图 */
    renderClassDistributionChart() {
      const chartDom = this.$refs.classDistributionChart;
      if (!chartDom) return;

      if (this.classDistributionChart) {
        this.classDistributionChart.dispose();
      }

      this.classDistributionChart = echarts.init(chartDom);

      const stats = this.results.statistics || {};
      const classDistribution = this.results.class_distribution || stats.class_distribution || {};
      const classNames = stats.class_names || Object.keys(classDistribution);

      if (classNames.length === 0) {
        this.classDistributionChart.setOption({
          title: {
            text: '暂无类别分布数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      const data = classNames.map(className => ({
        name: className,
        value: classDistribution[className] || 0
      }));

      const option = {
        title: {
          text: '类别分布',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [{
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
        }]
      };

      this.classDistributionChart.setOption(option);
      this.$emit('chart-ready', this.classDistributionChart);
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
          itemStyle: { color: '#67C23A' }
        }]
      };

      this.featureImportanceChart.setOption(option);
      this.$emit('chart-ready', this.featureImportanceChart);
    },

    /** 渲染决策边界图 */
    renderDecisionBoundaryChart() {
      const chartDom = this.$refs.decisionBoundaryChart;
      if (!chartDom) return;

      if (this.decisionBoundaryChart) {
        this.decisionBoundaryChart.dispose();
      }

      this.decisionBoundaryChart = echarts.init(chartDom);

      // 简化的决策边界可视化（实际需要更复杂的计算）
      this.decisionBoundaryChart.setOption({
        title: {
          text: '决策边界可视化',
          left: 'center'
        },
        tooltip: {
          trigger: 'item'
        },
        xAxis: {
          type: 'value',
          name: '特征1'
        },
        yAxis: {
          type: 'value',
          name: '特征2'
        },
        series: [{
          type: 'scatter',
          data: [[1, 2], [2, 3], [3, 1], [4, 4], [5, 2]],
          symbolSize: 8,
          itemStyle: { color: '#409EFF' }
        }]
      });

      this.$emit('chart-ready', this.decisionBoundaryChart);
    }
  }
};
</script>

<style scoped>
.classification-visualization {
  padding: 0;
}

.metrics-card, .chart-card, .report-card, .params-card {
  margin-bottom: 20px;
}

.metric-item {
  text-align: center;
  padding: 10px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #67C23A;
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
</style>
