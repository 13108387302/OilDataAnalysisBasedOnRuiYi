<template>
  <div class="feature-engineering-results-visualization">
    <!-- 特征工程结果卡片 -->
    <el-card class="metrics-card" shadow="never" style="margin-bottom: 20px;">
      <div slot="header">
        <span>🔧 {{ getAlgorithmDisplayName() }} - 特征工程结果</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="6" v-for="metric in featureMetrics" :key="metric.key">
          <div class="metric-item">
            <div class="metric-value">{{ metric.value }}</div>
            <div class="metric-label">{{ metric.label }}</div>
            <div class="metric-desc">{{ metric.description }}</div>
          </div>
        </el-col>
      </el-row>
      
      <!-- 如果没有指标，显示提示 -->
      <el-empty v-if="featureMetrics.length === 0" 
                description="暂无特征工程指标数据" 
                :image-size="100">
      </el-empty>
    </el-card>

    <!-- 可视化图表 -->
    <el-row :gutter="20">
      <!-- 特征分布图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 特征分布</span>
            <el-tooltip content="显示特征值的分布情况" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="featureDistributionChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 深度-特征关系图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📈 深度-特征关系</span>
            <el-tooltip content="显示特征值随深度的变化" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="depthFeatureChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 分形维数分析图 (如果是分形维数算法) -->
      <el-col v-if="isFractalDimension" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🔍 分形维数分析</span>
            <el-tooltip content="显示分形维数的计算结果" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="fractalChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 相关性分析图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🎯 相关性分析</span>
            <el-tooltip content="显示特征间的相关性" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="correlationChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 输入样本数据 -->
    <el-card v-if="hasInputSample" class="sample-data-card" shadow="never" style="margin-bottom: 20px;">
      <div slot="header">
        <span>📋 输入样本数据 (前10条)</span>
      </div>
      
      <el-table :data="inputSampleData" border stripe style="width: 100%;" max-height="300">
        <el-table-column 
          v-for="column in inputSampleColumns" 
          :key="column" 
          :prop="column" 
          :label="column"
          :width="120">
          <template slot-scope="scope">
            <span>{{ formatSampleValue(scope.row[column]) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 算法参数表 -->
    <el-card class="params-card" shadow="never" style="margin-bottom: 20px;">
      <div slot="header">
        <span>⚙️ 算法参数</span>
      </div>
      
      <el-table :data="algorithmParameters" border stripe style="width: 100%;">
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
      <el-empty v-if="algorithmParameters.length === 0" 
                description="暂无算法参数数据" 
                :image-size="100">
      </el-empty>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "FeatureEngineeringResultsVisualization",
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
      featureDistributionChart: null,
      depthFeatureChart: null,
      fractalChart: null,
      correlationChart: null
    };
  },
  computed: {
    featureMetrics() {
      // 优先使用标准化的statistics字段
      const stats = this.results.statistics || this.results.metrics || {};
      const metrics = [];

      console.log('🔍 特征工程指标数据源:', {
        statistics: this.results.statistics,
        metrics: this.results.metrics,
        algorithmType: this.results.algorithm_type,
        allResults: this.results
      });

      // 定义指标配置
      const metricConfigs = [
        {
          keys: ['fractal_dimension'],
          label: '分形维数',
          description: '数据的分形特征维度，反映数据复杂性',
          format: 'number'
        },
        {
          keys: ['correlation_coefficient'],
          label: '相关系数',
          description: '特征与深度的相关性强度',
          format: 'number'
        },
        {
          keys: ['sample_count'],
          label: '样本数量',
          description: '分析的数据点数量',
          format: 'integer'
        },
        {
          keys: ['mean_value'],
          label: '均值',
          description: '特征的平均值',
          format: 'number'
        },
        {
          keys: ['std_dev'],
          label: '标准差',
          description: '特征的标准偏差，反映数据离散程度',
          format: 'number'
        },
        {
          keys: ['min_value'],
          label: '最小值',
          description: '特征的最小值',
          format: 'number'
        },
        {
          keys: ['max_value'],
          label: '最大值',
          description: '特征的最大值',
          format: 'number'
        },
        {
          keys: ['depth_range'],
          label: '深度范围',
          description: '分析的深度区间',
          format: 'string'
        }
      ];

      // 处理每个指标
      metricConfigs.forEach(config => {
        let value = this.getValueByKeys(stats, config.keys);

        // 如果statistics中没有，尝试从results根级别获取
        if (value === undefined) {
          value = this.getValueByKeys(this.results, config.keys);
        }

        if (value !== undefined && value !== null) {
          metrics.push({
            key: config.keys[0],
            label: config.label,
            value: config.format === 'integer' ? value :
                   config.format === 'string' ? value : this.formatNumber(value),
            description: config.description
          });
        }
      });

      console.log('📊 解析到的特征工程指标:', metrics);
      return metrics;
    },
    
    algorithmParameters() {
      const params = this.results.model_params || {};
      return Object.entries(params).map(([key, value]) => ({
        parameter: this.getParameterDisplayName(key),
        value: this.formatParameterValue(value),
        description: this.getParameterDescription(key)
      }));
    },

    hasInputSample() {
      const sample = this.results.input_sample || [];
      return Array.isArray(sample) && sample.length > 0;
    },

    inputSampleData() {
      const sample = this.results.input_sample || [];
      return Array.isArray(sample) ? sample.slice(0, 10) : [];
    },

    inputSampleColumns() {
      if (this.inputSampleData.length === 0) return [];
      return Object.keys(this.inputSampleData[0]);
    },

    isFractalDimension() {
      return this.algorithmType.toLowerCase().includes('fractal_dimension');
    }
  },
  mounted() {
    console.log('🎨 特征工程可视化组件挂载');
    console.log('📊 接收到的结果数据:', this.results);
    console.log('📋 接收到的任务信息:', this.taskInfo);

    this.$nextTick(() => {
      this.waitForDOMAndRenderCharts();
    });
  },
  beforeDestroy() {
    // 销毁图表实例
    [this.featureDistributionChart, this.depthFeatureChart, this.fractalChart, this.correlationChart].forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    /** 等待DOM渲染完成后渲染图表 */
    waitForDOMAndRenderCharts() {
      console.log('🎨 等待DOM渲染完成...');

      // 检查DOM元素是否存在
      const checkDOM = () => {
        const featureDistributionDom = this.$refs.featureDistributionChart;
        const depthFeatureDom = this.$refs.depthFeatureChart;
        const correlationDom = this.$refs.correlationChart;

        if (featureDistributionDom && depthFeatureDom && correlationDom) {
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
      console.log('🎨 开始渲染特征工程可视化图表');

      try {
        this.renderFeatureDistributionChart();
        this.renderDepthFeatureChart();

        if (this.isFractalDimension) {
          this.renderFractalChart();
        }

        this.renderCorrelationChart();
        console.log('✅ 特征工程可视化图表渲染完成');
      } catch (error) {
        console.error('❌ 图表渲染失败', error);
      }
    },
    
    getAlgorithmDisplayName() {
      const names = {
        'fractal_dimension': '分形维数分析',
        'automatic_regression': '自动回归选择',
        'feature_selection': '特征选择',
        'feature_extraction': '特征提取',
        'dimensionality_reduction': '降维分析'
      };
      return names[this.algorithmType] || this.algorithmType;
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
        'depth_column': '深度列',
        'column_name': '分析列',
        'min_depth': '最小深度',
        'max_depth': '最大深度',
        'feature_columns': '特征列',
        'target_column': '目标列'
      };
      return names[key] || key;
    },
    
    formatParameterValue(value) {
      if (value === null || value === undefined) {
        return 'N/A';
      }
      if (typeof value === 'number') {
        return this.formatNumber(value);
      }
      if (Array.isArray(value)) {
        return value.join(', ');
      }
      return String(value);
    },
    
    getParameterDescription(key) {
      const descriptions = {
        'depth_column': '用作深度参考的列名',
        'column_name': '进行分析的主要列名',
        'min_depth': '分析的最小深度值',
        'max_depth': '分析的最大深度值',
        'feature_columns': '用作特征的列名列表',
        'target_column': '目标预测列名'
      };
      return descriptions[key] || '算法参数';
    },

    formatSampleValue(value) {
      if (value === null || value === undefined) {
        return 'N/A';
      }
      if (typeof value === 'number') {
        return Number(value).toFixed(3);
      }
      return String(value);
    },

    /** 渲染特征分布图 */
    renderFeatureDistributionChart() {
      setTimeout(() => {
        const chartDom = this.$refs.featureDistributionChart;
        if (!chartDom) return;

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          setTimeout(() => this.renderFeatureDistributionChart(), 500);
          return;
        }

        chartDom.style.width = '100%';
        chartDom.style.height = '350px';

        if (this.featureDistributionChart) {
          this.featureDistributionChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.featureDistributionChart = echarts.init(chartDom);

      // 获取特征值数据 - 使用标准化方法
      const featureValues = this.getValueByKeys(this.results, ['feature_values', 'depth_values']) || [];

      if (featureValues.length === 0) {
        this.featureDistributionChart.setOption({
          title: {
            text: '暂无特征分布数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 计算直方图数据
      const data = featureValues.slice(0, 200); // 限制数据量
      const min = Math.min(...data);
      const max = Math.max(...data);
      const binCount = 20;
      const binWidth = (max - min) / binCount;
      const bins = Array(binCount).fill(0);
      const binLabels = [];

      for (let i = 0; i < binCount; i++) {
        binLabels.push((min + i * binWidth).toFixed(3));
      }

      data.forEach(value => {
        const binIndex = Math.min(Math.floor((value - min) / binWidth), binCount - 1);
        bins[binIndex]++;
      });

      const option = {
        title: {
          text: '特征值分布',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            return `区间: ${params[0].name}<br/>频次: ${params[0].data}`;
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
          data: binLabels,
          name: '特征值区间'
        },
        yAxis: {
          type: 'value',
          name: '频次'
        },
        series: [
          {
            name: '频次',
            type: 'bar',
            data: bins,
            itemStyle: { color: '#5470c6' }
          }
        ]
      };

          this.featureDistributionChart.setOption(option);

        } catch (error) {
          console.error('❌ 特征分布图渲染失败', error);
        }
      }, 150);
    },

    /** 渲染深度-特征关系图 */
    renderDepthFeatureChart() {
      const chartDom = this.$refs.depthFeatureChart;
      if (!chartDom) return;

      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.depthFeatureChart) {
        this.depthFeatureChart.dispose();
      }

      this.depthFeatureChart = echarts.init(chartDom);

      // 获取深度和特征值数据 - 使用标准化方法
      const depthValues = this.getValueByKeys(this.results, ['depth_values']) || [];
      const featureValues = this.getValueByKeys(this.results, ['feature_values']) || [];

      if (depthValues.length === 0 || featureValues.length === 0) {
        this.depthFeatureChart.setOption({
          title: {
            text: '暂无深度-特征关系数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 准备散点数据
      const scatterData = depthValues.map((depth, index) => [depth, featureValues[index]]).slice(0, 200);

      const option = {
        title: {
          text: '深度-特征关系',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            return `深度: ${params.data[0].toFixed(3)}<br/>特征值: ${params.data[1].toFixed(3)}`;
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
          type: 'value',
          name: '深度'
        },
        yAxis: {
          type: 'value',
          name: '特征值'
        },
        series: [
          {
            name: '深度-特征',
            type: 'scatter',
            data: scatterData,
            itemStyle: { color: '#91cc75', opacity: 0.7 },
            symbolSize: 4
          }
        ]
      };

      this.depthFeatureChart.setOption(option);
    },

    /** 渲染分形维数分析图 */
    renderFractalChart() {
      const chartDom = this.$refs.fractalChart;
      if (!chartDom) return;

      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.fractalChart) {
        this.fractalChart.dispose();
      }

      this.fractalChart = echarts.init(chartDom);

      // 获取分形维数相关数据 - 使用标准化方法
      const fractalDimension = this.getValueByKeys(this.results, ['fractal_dimension']) ||
                               this.getValueByKeys(this.results.statistics || {}, ['fractal_dimension']);
      const correlationCoeff = this.getValueByKeys(this.results, ['correlation_coefficient']) ||
                               this.getValueByKeys(this.results.statistics || {}, ['correlation_coefficient']);

      if (fractalDimension === undefined) {
        this.fractalChart.setOption({
          title: {
            text: '暂无分形维数数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 创建分形维数可视化
      const option = {
        title: {
          text: '分形维数分析',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item'
        },
        series: [
          {
            name: '分形维数',
            type: 'gauge',
            min: 1,
            max: 3,
            splitNumber: 10,
            radius: '80%',
            axisLine: {
              lineStyle: {
                width: 10,
                color: [
                  [0.3, '#67e0e3'],
                  [0.7, '#37a2da'],
                  [1, '#fd666d']
                ]
              }
            },
            pointer: {
              itemStyle: {
                color: 'auto'
              }
            },
            axisTick: {
              distance: -30,
              length: 8,
              lineStyle: {
                color: '#fff',
                width: 2
              }
            },
            splitLine: {
              distance: -30,
              length: 30,
              lineStyle: {
                color: '#fff',
                width: 4
              }
            },
            axisLabel: {
              color: 'auto',
              distance: 40,
              fontSize: 12
            },
            detail: {
              valueAnimation: true,
              formatter: '{value}',
              color: 'auto',
              fontSize: 20
            },
            data: [
              {
                value: fractalDimension,
                name: '分形维数'
              }
            ]
          }
        ]
      };

      this.fractalChart.setOption(option);
    },

    /** 渲染相关性分析图 */
    renderCorrelationChart() {
      const chartDom = this.$refs.correlationChart;
      if (!chartDom) return;

      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.correlationChart) {
        this.correlationChart.dispose();
      }

      this.correlationChart = echarts.init(chartDom);

      // 获取相关性数据
      const correlationCoeff = this.results.correlation_coefficient || this.results.statistics?.correlation_coefficient;

      if (correlationCoeff === undefined) {
        this.correlationChart.setOption({
          title: {
            text: '暂无相关性数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 创建相关性可视化
      const option = {
        title: {
          text: '相关性分析',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item'
        },
        series: [
          {
            name: '相关系数',
            type: 'gauge',
            min: -1,
            max: 1,
            splitNumber: 10,
            radius: '80%',
            axisLine: {
              lineStyle: {
                width: 10,
                color: [
                  [0.2, '#fd666d'],
                  [0.4, '#fac858'],
                  [0.6, '#91cc75'],
                  [0.8, '#37a2da'],
                  [1, '#67e0e3']
                ]
              }
            },
            pointer: {
              itemStyle: {
                color: 'auto'
              }
            },
            axisTick: {
              distance: -30,
              length: 8,
              lineStyle: {
                color: '#fff',
                width: 2
              }
            },
            splitLine: {
              distance: -30,
              length: 30,
              lineStyle: {
                color: '#fff',
                width: 4
              }
            },
            axisLabel: {
              color: 'auto',
              distance: 40,
              fontSize: 12
            },
            detail: {
              valueAnimation: true,
              formatter: '{value}',
              color: 'auto',
              fontSize: 20
            },
            data: [
              {
                value: correlationCoeff,
                name: '相关系数'
              }
            ]
          }
        ]
      };

      this.correlationChart.setOption(option);
    }
  }
};
</script>

<style scoped>
.feature-engineering-results-visualization {
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
