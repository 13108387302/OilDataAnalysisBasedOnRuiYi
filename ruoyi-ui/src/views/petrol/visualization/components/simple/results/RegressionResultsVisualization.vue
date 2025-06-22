<template>
  <div class="regression-results-visualization">
    <!-- 性能指标卡片 -->
    <el-card class="metrics-card" shadow="never" style="margin-bottom: 20px;">
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
      
      <!-- 如果没有指标，显示提示 -->
      <el-empty v-if="performanceMetrics.length === 0" 
                description="暂无性能指标数据" 
                :image-size="100">
      </el-empty>
    </el-card>

    <!-- 可视化图表 -->
    <el-row :gutter="20">
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

      <!-- 残差分析图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📉 残差分析图</span>
            <el-tooltip content="分析预测误差的分布情况" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="residualChart" class="chart"></div>
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
  name: "RegressionResultsVisualization",
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
      featureImportanceChart: null
    };
  },
  computed: {
    performanceMetrics() {
      // 优先使用标准化的statistics字段
      const stats = this.results.statistics || this.results.metrics || {};
      const metrics = [];

      console.log('🔍 回归指标数据源:', {
        statistics: this.results.statistics,
        metrics: this.results.metrics,
        algorithmType: this.results.algorithm_type,
        allResults: this.results
      });

      // 定义指标配置
      const metricConfigs = [
        {
          keys: ['r2_score', 'r2', 'R²决定系数'],
          label: 'R² 决定系数',
          description: '模型解释方差的比例，越接近1越好',
          format: 'number'
        },
        {
          keys: ['mean_squared_error', 'mse', 'MSE'],
          label: 'MSE',
          description: '均方误差，越小越好',
          format: 'number'
        },
        {
          keys: ['rmse', 'RMSE', 'root_mean_squared_error'],
          label: 'RMSE',
          description: '均方根误差，与目标变量同单位',
          format: 'number',
          fallback: (stats) => {
            const mse = this.getValueByKeys(stats, ['mean_squared_error', 'mse', 'MSE']);
            return mse !== undefined ? Math.sqrt(mse) : undefined;
          }
        },
        {
          keys: ['mean_absolute_error', 'mae', 'MAE'],
          label: 'MAE',
          description: '平均绝对误差，越小越好',
          format: 'number'
        }
      ];

      // 处理每个指标
      metricConfigs.forEach(config => {
        let value = this.getValueByKeys(stats, config.keys);

        // 如果没有找到值且有fallback函数，尝试计算
        if (value === undefined && config.fallback) {
          value = config.fallback(stats);
        }

        if (value !== undefined && value !== null && !isNaN(value)) {
          metrics.push({
            key: config.keys[0],
            label: config.label,
            value: this.formatNumber(value),
            description: config.description
          });
        }
      });

      console.log('📊 解析到的回归指标:', metrics);
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
    }
  },
  mounted() {
    console.log('🎨 回归可视化组件挂载');
    console.log('📊 接收到的结果数据:', this.results);
    console.log('📋 接收到的任务信息:', this.taskInfo);

    this.$nextTick(() => {
      this.waitForDOMAndRenderCharts();
    });
  },
  beforeDestroy() {
    // 销毁图表实例
    [this.fittingChart, this.residualChart, this.predictionScatterChart, this.featureImportanceChart].forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    /** 等待DOM渲染完成后渲染图表 */
    waitForDOMAndRenderCharts() {
      console.log('🎨 等待DOM渲染完成...');

      // 检查DOM元素是否存在
      const checkDOM = () => {
        const fittingChartDom = this.$refs.fittingChart;
        const scatterChartDom = this.$refs.predictionScatterChart;
        const residualChartDom = this.$refs.residualChart;

        if (fittingChartDom && scatterChartDom && residualChartDom) {
          console.log('✅ DOM元素已准备就绪，开始渲染图表');
          this.renderCharts();
        } else {
          console.log('⏳ DOM元素未准备就绪，继续等待...');
          setTimeout(checkDOM, 100);
        }
      };

      setTimeout(checkDOM, 50);
    },

    /** 渲染所有图表 */
    renderCharts() {
      console.log('🎨 开始渲染回归可视化图表');

      try {
        this.renderFittingChart();
        this.renderPredictionScatterChart();
        this.renderResidualChart();

        if (this.hasFeatureImportance) {
          this.renderFeatureImportanceChart();
        }

        console.log('✅ 回归可视化图表渲染完成');
      } catch (error) {
        console.error('❌ 图表渲染失败', error);
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

    /** 根据多个可能的键名获取值 */
    getValueByKeys(obj, keys) {
      for (const key of keys) {
        if (obj && obj[key] !== undefined && obj[key] !== null) {
          return obj[key];
        }
      }
      return undefined;
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

    /** 获取目标变量名称 */
    getTargetVariableName() {
      // 从任务信息或结果中获取目标变量名
      const targetColumn = this.taskInfo.targetColumn ||
                          this.results.target_column ||
                          this.results.target_variable ||
                          '目标变量';
      return targetColumn;
    },

    /** 渲染拟合效果图 */
    renderFittingChart() {
      setTimeout(() => {
        const chartDom = this.$refs.fittingChart;
        if (!chartDom) {
          console.log('⚠️ 拟合效果图DOM元素不存在');
          return;
        }

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 拟合效果图DOM元素不可见，延迟重试');
          setTimeout(() => this.renderFittingChart(), 500);
          return;
        }

        chartDom.style.width = '100%';
        chartDom.style.height = '350px';

        if (this.fittingChart) {
          this.fittingChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.fittingChart = echarts.init(chartDom);

      // 从结果中获取数据 - 使用标准化方法
      const predictions = this.getValueByKeys(this.results, ['predictions', 'y_pred']) || [];
      const actualValues = this.getValueByKeys(this.results, ['actual_values', 'y_true', 'y_test']) || [];

      if (predictions.length === 0 || actualValues.length === 0) {
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

      // 准备数据
      const indices = Array.from({length: Math.min(predictions.length, actualValues.length)}, (_, i) => i);

      const option = {
        title: {
          text: '模型拟合效果',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            let result = `样本 ${params[0].dataIndex}<br/>`;
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
          left: '10%',
          right: '10%',
          bottom: '15%',
          top: '20%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: indices,
          name: '样本索引',
          nameLocation: 'middle',
          nameGap: 30,
          nameTextStyle: {
            fontSize: 12,
            fontWeight: 'bold'
          }
        },
        yAxis: {
          type: 'value',
          name: this.getTargetVariableName(),
          nameLocation: 'middle',
          nameGap: 50,
          nameTextStyle: {
            fontSize: 12,
            fontWeight: 'bold'
          }
        },
        series: [
          {
            name: '实际值',
            type: 'line',
            data: actualValues.slice(0, 100), // 限制显示数量
            itemStyle: { color: '#5470c6' },
            symbol: 'circle',
            symbolSize: 4
          },
          {
            name: '预测值',
            type: 'line',
            data: predictions.slice(0, 100), // 限制显示数量
            itemStyle: { color: '#91cc75' },
            symbol: 'triangle',
            symbolSize: 4
          }
        ]
      };

          this.fittingChart.setOption(option);
          console.log('✅ 拟合效果图渲染完成');

        } catch (error) {
          console.error('❌ 拟合效果图渲染失败', error);
        }
      }, 150); // 延迟150ms
    },

    /** 渲染预测vs实际散点图 */
    renderPredictionScatterChart() {
      setTimeout(() => {
        const chartDom = this.$refs.predictionScatterChart;
        if (!chartDom) {
          console.log('⚠️ 预测散点图DOM元素不存在');
          return;
        }

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 预测散点图DOM元素不可见，延迟重试');
          setTimeout(() => this.renderPredictionScatterChart(), 500);
          return;
        }

        chartDom.style.width = '100%';
        chartDom.style.height = '350px';

        if (this.predictionScatterChart) {
          this.predictionScatterChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.predictionScatterChart = echarts.init(chartDom);

      const predictions = this.getValueByKeys(this.results, ['predictions', 'y_pred']) || [];
      const actualValues = this.getValueByKeys(this.results, ['actual_values', 'y_true', 'y_test']) || [];

      if (predictions.length === 0 || actualValues.length === 0) {
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

      // 准备散点数据
      const scatterData = actualValues.map((actual, index) => [actual, predictions[index]]);

      // 计算对角线范围
      const allValues = [...actualValues, ...predictions];
      const minVal = Math.min(...allValues);
      const maxVal = Math.max(...allValues);

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
        grid: {
          left: '15%',
          right: '10%',
          bottom: '15%',
          top: '20%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          name: `实际值 (${this.getTargetVariableName()})`,
          nameLocation: 'middle',
          nameGap: 30,
          nameTextStyle: {
            fontSize: 12,
            fontWeight: 'bold'
          },
          min: minVal,
          max: maxVal
        },
        yAxis: {
          type: 'value',
          name: `预测值 (${this.getTargetVariableName()})`,
          nameLocation: 'middle',
          nameGap: 50,
          nameTextStyle: {
            fontSize: 12,
            fontWeight: 'bold'
          },
          min: minVal,
          max: maxVal
        },
        series: [
          {
            name: '预测点',
            type: 'scatter',
            data: scatterData,
            itemStyle: { color: '#5470c6', opacity: 0.6 },
            symbolSize: 6
          },
          {
            name: '理想线',
            type: 'line',
            data: [[minVal, minVal], [maxVal, maxVal]],
            itemStyle: { color: '#ff6b6b' },
            lineStyle: { type: 'dashed', width: 2 },
            symbol: 'none'
          }
        ]
      };

          this.predictionScatterChart.setOption(option);
          console.log('✅ 预测散点图渲染完成');

        } catch (error) {
          console.error('❌ 预测散点图渲染失败', error);
        }
      }, 200); // 延迟200ms
    },

    /** 渲染残差分析图 */
    renderResidualChart() {
      setTimeout(() => {
        const chartDom = this.$refs.residualChart;
        if (!chartDom) {
          console.log('⚠️ 残差分析图DOM元素不存在');
          return;
        }

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 残差分析图DOM元素不可见，延迟重试');
          setTimeout(() => this.renderResidualChart(), 500);
          return;
        }

        chartDom.style.width = '100%';
        chartDom.style.height = '350px';

        if (this.residualChart) {
          this.residualChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.residualChart = echarts.init(chartDom);

      const predictions = this.getValueByKeys(this.results, ['predictions', 'y_pred']) || [];
      const actualValues = this.getValueByKeys(this.results, ['actual_values', 'y_true', 'y_test']) || [];

      if (predictions.length === 0 || actualValues.length === 0) {
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
      const residuals = actualValues.map((actual, index) => actual - predictions[index]);
      const indices = Array.from({length: residuals.length}, (_, i) => i);

      const option = {
        title: {
          text: '残差分析',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            return `样本 ${params.dataIndex}<br/>残差: ${params.data.toFixed(4)}`;
          }
        },
        grid: {
          left: '10%',
          right: '10%',
          bottom: '15%',
          top: '20%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: indices,
          name: '样本索引',
          nameLocation: 'middle',
          nameGap: 30,
          nameTextStyle: {
            fontSize: 12,
            fontWeight: 'bold'
          }
        },
        yAxis: {
          type: 'value',
          name: `残差 (${this.getTargetVariableName()})`,
          nameLocation: 'middle',
          nameGap: 50,
          nameTextStyle: {
            fontSize: 12,
            fontWeight: 'bold'
          }
        },
        series: [
          {
            name: '残差',
            type: 'scatter',
            data: residuals,
            itemStyle: { color: '#fac858', opacity: 0.7 },
            symbolSize: 4
          },
          {
            name: '零线',
            type: 'line',
            data: Array(residuals.length).fill(0),
            itemStyle: { color: '#ff6b6b' },
            lineStyle: { type: 'dashed', width: 1 },
            symbol: 'none'
          }
        ]
      };

          this.residualChart.setOption(option);
          console.log('✅ 残差分析图渲染完成');

        } catch (error) {
          console.error('❌ 残差分析图渲染失败', error);
        }
      }, 250); // 延迟250ms
    },

    /** 渲染特征重要性图 */
    renderFeatureImportanceChart() {
      setTimeout(() => {
        const chartDom = this.$refs.featureImportanceChart;
        if (!chartDom) {
          console.log('⚠️ 特征重要性图DOM元素不存在');
          return;
        }

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 特征重要性图DOM元素不可见，延迟重试');
          setTimeout(() => this.renderFeatureImportanceChart(), 500);
          return;
        }

        chartDom.style.width = '100%';
        chartDom.style.height = '350px';

        if (this.featureImportanceChart) {
          this.featureImportanceChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.featureImportanceChart = echarts.init(chartDom);

      const importance = this.results.feature_importance || {};
      const importanceEntries = Object.entries(importance);

      if (importanceEntries.length === 0) {
        this.featureImportanceChart.setOption({
          title: {
            text: '暂无特征重要性数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 排序并准备数据
      const sortedImportance = importanceEntries
        .sort((a, b) => b[1] - a[1])
        .slice(0, 20); // 只显示前20个特征

      const features = sortedImportance.map(item => item[0]);
      const values = sortedImportance.map(item => item[1]);

      const option = {
        title: {
          text: '特征重要性',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            return `${params[0].name}<br/>重要性: ${params[0].data.toFixed(4)}`;
          }
        },
        grid: {
          left: '15%',
          right: '10%',
          bottom: '15%',
          top: '20%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          name: '重要性分数',
          nameLocation: 'middle',
          nameGap: 30,
          nameTextStyle: {
            fontSize: 12,
            fontWeight: 'bold'
          }
        },
        yAxis: {
          type: 'category',
          data: features,
          name: '特征名称',
          nameLocation: 'middle',
          nameGap: 50,
          nameTextStyle: {
            fontSize: 12,
            fontWeight: 'bold'
          }
        },
        series: [
          {
            name: '特征重要性',
            type: 'bar',
            data: values,
            itemStyle: { color: '#73c0de' }
          }
        ]
      };

          this.featureImportanceChart.setOption(option);
          console.log('✅ 特征重要性图渲染完成');

        } catch (error) {
          console.error('❌ 特征重要性图渲染失败', error);
        }
      }, 300); // 延迟300ms
    }
  }
};
</script>

<style scoped>
.regression-results-visualization {
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

.params-card .el-table {
  margin-top: 10px;
}
</style>
