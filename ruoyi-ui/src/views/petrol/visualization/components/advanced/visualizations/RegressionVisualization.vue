<template>
  <div class="regression-visualization">
    <!-- 回归算法结果可视化 -->
    <el-row :gutter="20">
      <!-- 模型性能指标卡片 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="metrics-card" shadow="never">
          <div slot="header">
            <span>📊 {{ getAlgorithmDisplayName() }} - 模型性能指标</span>
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

      <!-- 拟合效果图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📈 拟合效果图</span>
            <el-tooltip content="显示模型预测值与实际值的拟合情况" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="fittingChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 残差分析图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📉 残差分析图</span>
            <el-tooltip content="分析预测误差的分布情况，检验模型假设" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="residualChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 预测vs实际散点图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🎯 预测vs实际值</span>
            <el-tooltip content="理想情况下所有点应该在对角线上" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="predictionScatterChart" class="chart"></div>
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

      <!-- 学习曲线 (如果有训练历史) -->
      <el-col v-if="hasTrainingHistory" :span="24" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📚 学习曲线</span>
            <el-tooltip content="显示模型训练过程中损失函数的变化" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="learningCurveChart" class="chart"></div>
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
import { testReadDataSourceData } from '@/api/petrol/visualization';

export default {
  name: "RegressionVisualization",
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
      fittingChart: null,
      residualChart: null,
      predictionScatterChart: null,
      featureImportanceChart: null,
      learningCurveChart: null,
      // 真实数据
      rawData: [],
      loading: false
    };
  },
  computed: {
    performanceMetrics() {
      const stats = this.results.statistics || {};
      const metrics = [];

      // R²决定系数 - 支持多种字段名
      const r2Score = stats.r2_score || stats.r2 || stats['R²决定系数'];
      if (r2Score !== undefined) {
        metrics.push({
          key: 'r2_score',
          label: 'R² 决定系数',
          value: this.formatNumber(r2Score),
          description: '模型解释方差的比例'
        });
      }

      // 均方误差 - 支持多种字段名
      const mse = stats.mean_squared_error || stats.mse || stats.MSE;
      if (mse !== undefined) {
        metrics.push({
          key: 'mse',
          label: 'MSE',
          value: this.formatNumber(mse),
          description: '均方误差'
        });
      }

      // 均方根误差
      const rmse = stats.rmse || stats.RMSE || (mse ? Math.sqrt(mse) : undefined);
      if (rmse !== undefined) {
        metrics.push({
          key: 'rmse',
          label: 'RMSE',
          value: this.formatNumber(rmse),
          description: '均方根误差'
        });
      }

      // 平均绝对误差 - 支持多种字段名
      const mae = stats.mean_absolute_error || stats.mae || stats.MAE;
      if (mae !== undefined) {
        metrics.push({
          key: 'mae',
          label: 'MAE',
          value: this.formatNumber(mae),
          description: '平均绝对误差'
        });
      }

      // 如果没有真实指标，显示提示信息
      if (metrics.length === 0) {
        console.log('⚠️ 没有找到性能指标数据，可用字段:', Object.keys(stats));
      }

      return metrics;
    },
    
    modelParameters() {
      const params = this.results.model_params || {};
      const paramEntries = Object.entries(params);

      // 如果没有真实参数，显示提示信息
      if (paramEntries.length === 0) {
        console.log('⚠️ 没有找到模型参数数据');
      }

      return paramEntries.map(([key, value]) => ({
        parameter: this.getParameterDisplayName(key),
        value: this.formatParameterValue(value),
        description: this.getParameterDescription(key)
      }));
    },
    
    hasFeatureImportance() {
      const importance = this.results.feature_importance ||
                        (this.results.statistics && this.results.statistics.feature_importance);

      // 检查是否有有效的特征重要性数据
      if (!importance) return false;

      // 如果是对象，检查是否有键值对
      if (typeof importance === 'object' && !Array.isArray(importance)) {
        return Object.keys(importance).length > 0;
      }

      // 如果是数组，检查是否有元素
      if (Array.isArray(importance)) {
        return importance.length > 0;
      }

      return false;
    },
    
    hasTrainingHistory() {
      return this.results.training_history || 
             (this.results.statistics && this.results.statistics.training_history);
    }
  },
  mounted() {
    console.log('🎨 回归可视化组件挂载');
    console.log('📊 接收到的结果数据:', this.results);
    console.log('📋 接收到的任务信息:', this.taskInfo);
    console.log('🔧 算法类型:', this.algorithmType);

    this.$nextTick(() => {
      this.initializeCharts();
    });
  },

  watch: {
    results: {
      handler(newResults) {
        console.log('📊 回归可视化组件 - 结果数据更新:', newResults);
        this.$nextTick(() => {
          this.initializeCharts();
        });
      },
      deep: true
    }
  },
  beforeDestroy() {
    // 销毁图表实例
    [this.fittingChart, this.residualChart, this.predictionScatterChart,
     this.featureImportanceChart, this.learningCurveChart].forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    initializeCharts() {
      console.log('🎨 开始初始化回归可视化图表');

      // 首先尝试加载真实数据
      this.loadRealData().then(() => {
        // 延迟渲染确保DOM完全加载
        setTimeout(() => {
          this.renderFittingChart();
          this.renderResidualChart();
          this.renderPredictionScatterChart();

          if (this.hasFeatureImportance) {
            this.renderFeatureImportanceChart();
          }

          if (this.hasTrainingHistory) {
            this.renderLearningCurveChart();
          }

          console.log('✅ 回归可视化图表初始化完成');
        }, 100);
      });
    },

    /** 加载真实数据 */
    async loadRealData() {
      try {
        // 如果已经有预测数据，就不需要再加载
        const predictions = this.results.predictions || this.results.y_pred || [];
        const actualValues = this.results.actual_values || this.results.y_true || [];

        if (predictions.length > 0 && actualValues.length > 0) {
          console.log('✅ 已有预测数据，无需重新加载');
          return;
        }

        // 尝试从数据源加载原始数据
        if (this.sourceId && this.sourceType) {
          console.log('🔍 尝试从数据源加载真实数据');
          this.loading = true;

          const params = {
            maxRows: 500 // 限制数据量
          };

          const response = await testReadDataSourceData(this.sourceId, this.sourceType, params);
          this.rawData = response.data || [];

          console.log('📋 加载的原始数据:', {
            dataLength: this.rawData.length,
            sampleData: this.rawData.slice(0, 3)
          });
        }
      } catch (error) {
        console.error('❌ 加载真实数据失败:', error);
      } finally {
        this.loading = false;
      }
    },
    
    getAlgorithmDisplayName() {
      const names = {
        'linear_regression': '线性回归',
        'polynomial_regression': '多项式回归',
        'exponential_regression': '指数回归',
        'logarithmic_regression': '对数回归',
        'svm_regression': 'SVM回归',
        'random_forest_regression': '随机森林回归',
        'xgboost_regression': 'XGBoost回归',
        'lightgbm_regression': 'LightGBM回归',
        'bilstm_regression': 'BiLSTM回归',
        'tcn_regression': 'TCN回归',
        'automatic_regression': '自动回归选择'
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
    
    getParameterDisplayName(key) {
      const names = {
        'degree': '多项式次数',
        'alpha': '正则化参数',
        'C': '正则化参数C',
        'gamma': 'Gamma参数',
        'kernel': '核函数',
        'n_estimators': '树的数量',
        'max_depth': '最大深度',
        'learning_rate': '学习率',
        'random_state': '随机种子'
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
        'degree': '多项式回归的次数',
        'alpha': '岭回归的正则化强度',
        'C': 'SVM的正则化参数',
        'gamma': 'RBF核的参数',
        'kernel': 'SVM使用的核函数类型',
        'n_estimators': '随机森林中树的数量',
        'max_depth': '决策树的最大深度',
        'learning_rate': '梯度提升的学习率',
        'random_state': '随机数生成器的种子'
      };
      return descriptions[key] || '模型参数';
    },

    /** 渲染拟合效果图 */
    renderFittingChart() {
      const chartDom = this.$refs.fittingChart;
      if (!chartDom) {
        console.log('⚠️ 拟合图DOM元素不存在');
        return;
      }

      // 设置图表容器尺寸
      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.fittingChart) {
        this.fittingChart.dispose();
      }

      this.fittingChart = echarts.init(chartDom);
      console.log('✅ 拟合图表实例创建成功');

      // 从结果中获取数据 - 支持多种数据结构
      const predictions = this.results.predictions ||
                         this.results.y_pred ||
                         (this.results.statistics && this.results.statistics.predictions) || [];

      const actualValues = this.results.actual_values ||
                          this.results.y_true ||
                          (this.results.statistics && this.results.statistics.actual_values) || [];

      const featureValues = this.results.feature_values ||
                           this.results.X_test ||
                           (this.results.statistics && this.results.statistics.feature_values) || [];

      console.log('📊 拟合图数据:', {
        predictions: predictions.length,
        actualValues: actualValues.length,
        featureValues: featureValues.length
      });

      // 如果没有真实数据，显示无数据提示
      if (predictions.length === 0 || actualValues.length === 0) {
        console.log('⚠️ 没有找到拟合数据');
        this.fittingChart.setOption({
          title: {
            text: '暂无拟合数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 准备散点数据和拟合线数据
      const scatterData = actualValues.map((actual, index) => [
        featureValues[index] || index,
        actual
      ]);

      const lineData = predictions.map((pred, index) => [
        featureValues[index] || index,
        pred
      ]).sort((a, b) => a[0] - b[0]); // 按x值排序

      const option = {
        title: {
          text: '模型拟合效果',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            let result = `特征值: ${params[0].data[0].toFixed(4)}<br/>`;
            params.forEach(param => {
              result += `${param.seriesName}: ${param.data[1].toFixed(4)}<br/>`;
            });
            return result;
          }
        },
        legend: {
          data: ['实际值', '预测值'],
          bottom: 10
        },
        xAxis: {
          type: 'value',
          name: '特征值',
          nameLocation: 'middle',
          nameGap: 30
        },
        yAxis: {
          type: 'value',
          name: '目标值',
          nameLocation: 'middle',
          nameGap: 40
        },
        series: [
          {
            name: '实际值',
            type: 'scatter',
            data: scatterData,
            symbolSize: 6,
            itemStyle: {
              color: '#5470c6',
              opacity: 0.7
            }
          },
          {
            name: '预测值',
            type: 'line',
            data: lineData,
            smooth: true,
            lineStyle: {
              color: '#ee6666',
              width: 2
            },
            symbol: 'none'
          }
        ]
      };

      this.fittingChart.setOption(option);
      this.$emit('chart-ready', this.fittingChart);
    },

    /** 渲染残差分析图 */
    renderResidualChart() {
      const chartDom = this.$refs.residualChart;
      if (!chartDom) {
        console.log('⚠️ 残差图DOM元素不存在');
        return;
      }

      // 设置图表容器尺寸
      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.residualChart) {
        this.residualChart.dispose();
      }

      this.residualChart = echarts.init(chartDom);
      console.log('✅ 残差图表实例创建成功');

      // 从结果中获取数据 - 支持多种数据结构
      const predictions = this.results.predictions ||
                         this.results.y_pred ||
                         (this.results.statistics && this.results.statistics.predictions) || [];

      const actualValues = this.results.actual_values ||
                          this.results.y_true ||
                          (this.results.statistics && this.results.statistics.actual_values) || [];

      console.log('📊 残差图数据:', {
        predictions: predictions.length,
        actualValues: actualValues.length
      });

      // 如果没有真实数据，显示无数据提示
      if (predictions.length === 0 || actualValues.length === 0) {
        console.log('⚠️ 没有找到残差数据');
        this.residualChart.setOption({
          title: {
            text: '暂无残差数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 计算残差
      const residuals = actualValues.map((actual, index) =>
        actual - (predictions[index] || 0)
      );

      const residualData = residuals.map((residual, index) => [
        predictions[index] || 0,
        residual
      ]);

      const option = {
        title: {
          text: '残差分析',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            return `预测值: ${params.data[0].toFixed(4)}<br/>残差: ${params.data[1].toFixed(4)}`;
          }
        },
        xAxis: {
          type: 'value',
          name: '预测值',
          nameLocation: 'middle',
          nameGap: 30
        },
        yAxis: {
          type: 'value',
          name: '残差',
          nameLocation: 'middle',
          nameGap: 40
        },
        series: [
          {
            type: 'scatter',
            data: residualData,
            symbolSize: 6,
            itemStyle: {
              color: '#91cc75',
              opacity: 0.7
            }
          },
          {
            type: 'line',
            data: [[Math.min(...predictions), 0], [Math.max(...predictions), 0]],
            lineStyle: {
              color: '#ff6b6b',
              type: 'dashed',
              width: 1
            },
            symbol: 'none',
            silent: true
          }
        ]
      };

      this.residualChart.setOption(option);
      this.$emit('chart-ready', this.residualChart);
    },

    /** 渲染预测vs实际值散点图 */
    renderPredictionScatterChart() {
      const chartDom = this.$refs.predictionScatterChart;
      if (!chartDom) {
        console.log('⚠️ 预测散点图DOM元素不存在');
        return;
      }

      // 设置图表容器尺寸
      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.predictionScatterChart) {
        this.predictionScatterChart.dispose();
      }

      this.predictionScatterChart = echarts.init(chartDom);
      console.log('✅ 预测散点图表实例创建成功');

      // 从结果中获取数据 - 支持多种数据结构
      const predictions = this.results.predictions ||
                         this.results.y_pred ||
                         (this.results.statistics && this.results.statistics.predictions) || [];

      const actualValues = this.results.actual_values ||
                          this.results.y_true ||
                          (this.results.statistics && this.results.statistics.actual_values) || [];

      console.log('📊 预测散点图数据:', {
        predictions: predictions.length,
        actualValues: actualValues.length
      });

      // 如果没有真实数据，显示无数据提示
      if (predictions.length === 0 || actualValues.length === 0) {
        console.log('⚠️ 没有找到预测数据');
        this.predictionScatterChart.setOption({
          title: {
            text: '暂无预测数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      const scatterData = actualValues.map((actual, index) => [
        actual,
        predictions[index] || 0
      ]);

      const minVal = Math.min(...actualValues, ...predictions);
      const maxVal = Math.max(...actualValues, ...predictions);

      const option = {
        title: {
          text: '预测值 vs 实际值',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            return `实际值: ${params.data[0].toFixed(4)}<br/>预测值: ${params.data[1].toFixed(4)}`;
          }
        },
        xAxis: {
          type: 'value',
          name: '实际值',
          nameLocation: 'middle',
          nameGap: 30,
          min: minVal,
          max: maxVal
        },
        yAxis: {
          type: 'value',
          name: '预测值',
          nameLocation: 'middle',
          nameGap: 40,
          min: minVal,
          max: maxVal
        },
        series: [
          {
            type: 'scatter',
            data: scatterData,
            symbolSize: 6,
            itemStyle: {
              color: '#fac858',
              opacity: 0.7
            }
          },
          {
            type: 'line',
            data: [[minVal, minVal], [maxVal, maxVal]],
            lineStyle: {
              color: '#ff6b6b',
              type: 'dashed',
              width: 2
            },
            symbol: 'none',
            silent: true
          }
        ]
      };

      this.predictionScatterChart.setOption(option);
      this.$emit('chart-ready', this.predictionScatterChart);
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
                               this.results.statistics?.feature_importance;

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
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        xAxis: {
          type: 'category',
          data: features,
          axisLabel: {
            rotate: 45
          }
        },
        yAxis: {
          type: 'value',
          name: '重要性',
          nameLocation: 'middle',
          nameGap: 40
        },
        series: [
          {
            type: 'bar',
            data: importances,
            itemStyle: {
              color: '#73c0de'
            }
          }
        ]
      };

      this.featureImportanceChart.setOption(option);
      this.$emit('chart-ready', this.featureImportanceChart);
    },

    /** 渲染学习曲线 */
    renderLearningCurveChart() {
      const chartDom = this.$refs.learningCurveChart;
      if (!chartDom) return;

      if (this.learningCurveChart) {
        this.learningCurveChart.dispose();
      }

      this.learningCurveChart = echarts.init(chartDom);

      const trainingHistory = this.results.training_history ||
                             this.results.statistics?.training_history;

      if (!trainingHistory) {
        this.learningCurveChart.setOption({
          title: {
            text: '暂无训练历史数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      const epochs = trainingHistory.epochs || [];
      const trainLoss = trainingHistory.train_loss || [];
      const valLoss = trainingHistory.val_loss || [];

      const option = {
        title: {
          text: '学习曲线',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['训练损失', '验证损失'],
          bottom: 10
        },
        xAxis: {
          type: 'category',
          data: epochs,
          name: 'Epoch',
          nameLocation: 'middle',
          nameGap: 30
        },
        yAxis: {
          type: 'value',
          name: '损失值',
          nameLocation: 'middle',
          nameGap: 40
        },
        series: [
          {
            name: '训练损失',
            type: 'line',
            data: trainLoss,
            smooth: true,
            lineStyle: {
              color: '#5470c6'
            }
          },
          {
            name: '验证损失',
            type: 'line',
            data: valLoss,
            smooth: true,
            lineStyle: {
              color: '#ee6666'
            }
          }
        ]
      };

      this.learningCurveChart.setOption(option);
      this.$emit('chart-ready', this.learningCurveChart);
    }
  }
};
</script>

<style scoped>
.regression-visualization {
  padding: 0;
}

.metrics-card, .chart-card, .params-card {
  margin-bottom: 20px;
}

.metric-item {
  text-align: center;
  padding: 10px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
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
