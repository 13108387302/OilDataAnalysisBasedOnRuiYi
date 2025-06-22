<template>
  <div class="generic-results-visualization">
    <!-- 算法信息卡片 -->
    <el-card class="algorithm-info-card" shadow="never" style="margin-bottom: 20px;">
      <div slot="header">
        <span>🔧 {{ getAlgorithmDisplayName() }} - 算法信息</span>
      </div>
      
      <el-descriptions :column="2" border>
        <el-descriptions-item label="算法类型">
          <el-tag type="primary">{{ algorithmType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="任务状态">
          <el-tag :type="getStatusTagType()">{{ taskInfo.status || '未知' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="任务名称">
          {{ taskInfo.taskName || '未命名任务' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ taskInfo.createTime || '未知' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 结果统计卡片 -->
    <el-card v-if="hasStatistics" class="statistics-card" shadow="never" style="margin-bottom: 20px;">
      <div slot="header">
        <span>📊 结果统计</span>
      </div>
      
      <el-table :data="statisticsTableData" border stripe style="width: 100%;">
        <el-table-column prop="metric" label="指标名称" width="200">
          <template slot-scope="scope">
            <strong>{{ scope.row.metric }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="value" label="数值">
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

    <!-- 模型参数卡片 -->
    <el-card v-if="hasModelParams" class="params-card" shadow="never" style="margin-bottom: 20px;">
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

    <!-- 数据可视化 -->
    <el-row :gutter="20" v-if="hasVisualizableData">
      <!-- 数据分布图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📈 数据分布</span>
          </div>
          <div ref="distributionChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 数据趋势图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 数据趋势</span>
          </div>
          <div ref="trendChart" class="chart"></div>
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

    <!-- 原始结果数据 -->
    <el-card class="raw-data-card" shadow="never">
      <div slot="header">
        <span>📄 原始结果数据</span>
        <el-button 
          type="text" 
          icon="el-icon-copy-document" 
          @click="copyRawData"
          style="float: right;">
          复制数据
        </el-button>
      </div>
      
      <el-input
        type="textarea"
        :rows="15"
        :value="formattedRawResults"
        readonly
        placeholder="暂无原始结果数据">
      </el-input>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "GenericResultsVisualization",
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
      distributionChart: null,
      trendChart: null
    };
  },
  computed: {
    hasStatistics() {
      const stats = this.results.statistics || this.results.metrics || {};
      return Object.keys(stats).length > 0;
    },

    hasModelParams() {
      const params = this.results.model_params || {};
      return Object.keys(params).length > 0;
    },

    hasVisualizableData() {
      // 检查是否有可可视化的数据
      const predictions = this.results.predictions || this.results.y_pred || [];
      const actualValues = this.results.actual_values || this.results.y_true || [];
      const inputSample = this.results.input_sample || [];
      
      return predictions.length > 0 || actualValues.length > 0 || inputSample.length > 0;
    },

    hasInputSample() {
      const sample = this.results.input_sample || [];
      return Array.isArray(sample) && sample.length > 0;
    },

    statisticsTableData() {
      const stats = this.results.statistics || this.results.metrics || {};
      return Object.entries(stats).map(([key, value]) => ({
        metric: this.getMetricDisplayName(key),
        value: this.formatStatisticValue(value),
        description: this.getMetricDescription(key)
      }));
    },
    
    modelParameters() {
      const params = this.results.model_params || {};
      return Object.entries(params).map(([key, value]) => ({
        parameter: this.getParameterDisplayName(key),
        value: this.formatParameterValue(value),
        description: this.getParameterDescription(key)
      }));
    },

    inputSampleData() {
      const sample = this.results.input_sample || [];
      return Array.isArray(sample) ? sample.slice(0, 10) : [];
    },

    inputSampleColumns() {
      if (this.inputSampleData.length === 0) return [];
      return Object.keys(this.inputSampleData[0]);
    },

    formattedRawResults() {
      return this.results ? JSON.stringify(this.results, null, 2) : '';
    }
  },
  mounted() {
    console.log('🎨 通用可视化组件挂载');
    console.log('📊 接收到的结果数据:', this.results);
    console.log('📋 接收到的任务信息:', this.taskInfo);

    this.$nextTick(() => {
      if (this.hasVisualizableData) {
        this.initializeCharts();
      }
    });
  },
  beforeDestroy() {
    // 销毁图表实例
    [this.distributionChart, this.trendChart].forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    initializeCharts() {
      console.log('🎨 开始初始化通用可视化图表');
      
      setTimeout(() => {
        this.renderDistributionChart();
        this.renderTrendChart();
        console.log('✅ 通用可视化图表初始化完成');
      }, 100);
    },
    
    getAlgorithmDisplayName() {
      const names = {
        'fractal_dimension': '分形维数分析',
        'automatic_regression': '自动回归选择',
        'feature_engineering': '特征工程',
        'data_preprocessing': '数据预处理',
        'statistical_analysis': '统计分析'
      };
      return names[this.algorithmType] || this.algorithmType;
    },

    getStatusTagType() {
      const status = this.taskInfo.status;
      const types = {
        'COMPLETED': 'success',
        'RUNNING': 'warning',
        'QUEUED': 'info',
        'FAILED': 'danger'
      };
      return types[status] || 'info';
    },
    
    getMetricDisplayName(key) {
      const names = {
        'fractal_dimension': '分形维数',
        'correlation_coefficient': '相关系数',
        'sample_count': '样本数量',
        'min_value': '最小值',
        'max_value': '最大值',
        'mean_value': '平均值',
        'std_dev': '标准差',
        'depth_range': '深度范围'
      };
      return names[key] || key;
    },

    getMetricDescription(key) {
      const descriptions = {
        'fractal_dimension': '数据的分形维数，反映复杂度',
        'correlation_coefficient': '数据间的相关性强度',
        'sample_count': '参与分析的样本总数',
        'min_value': '数据集中的最小值',
        'max_value': '数据集中的最大值',
        'mean_value': '数据集的平均值',
        'std_dev': '数据的标准差，反映离散程度',
        'depth_range': '分析的深度范围'
      };
      return descriptions[key] || '算法计算结果';
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
    
    formatStatisticValue(value) {
      if (value === null || value === undefined) {
        return 'N/A';
      }
      if (typeof value === 'number') {
        return Number(value).toFixed(4);
      }
      return String(value);
    },
    
    formatParameterValue(value) {
      if (value === null || value === undefined) {
        return 'N/A';
      }
      if (typeof value === 'number') {
        return Number(value).toFixed(4);
      }
      if (Array.isArray(value)) {
        return value.join(', ');
      }
      return String(value);
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

    copyRawData() {
      const textArea = document.createElement('textarea');
      textArea.value = this.formattedRawResults;
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
      this.$message.success('数据已复制到剪贴板');
    },

    /** 渲染数据分布图 */
    renderDistributionChart() {
      const chartDom = this.$refs.distributionChart;
      if (!chartDom) return;

      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.distributionChart) {
        this.distributionChart.dispose();
      }

      this.distributionChart = echarts.init(chartDom);

      // 尝试从多个数据源获取数据
      let data = [];
      const predictions = this.results.predictions || this.results.y_pred || [];
      const actualValues = this.results.actual_values || this.results.y_true || [];
      const featureValues = this.results.feature_values || [];

      if (predictions.length > 0) {
        data = predictions.slice(0, 100);
      } else if (actualValues.length > 0) {
        data = actualValues.slice(0, 100);
      } else if (featureValues.length > 0) {
        data = featureValues.slice(0, 100);
      }

      if (data.length === 0) {
        this.distributionChart.setOption({
          title: {
            text: '暂无分布数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 计算直方图数据
      const min = Math.min(...data);
      const max = Math.max(...data);
      const binCount = 20;
      const binWidth = (max - min) / binCount;
      const bins = Array(binCount).fill(0);
      const binLabels = [];

      for (let i = 0; i < binCount; i++) {
        binLabels.push((min + i * binWidth).toFixed(2));
      }

      data.forEach(value => {
        const binIndex = Math.min(Math.floor((value - min) / binWidth), binCount - 1);
        bins[binIndex]++;
      });

      const option = {
        title: {
          text: '数据分布直方图',
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
          name: '数值区间'
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

      this.distributionChart.setOption(option);
    },

    /** 渲染数据趋势图 */
    renderTrendChart() {
      const chartDom = this.$refs.trendChart;
      if (!chartDom) return;

      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.trendChart) {
        this.trendChart.dispose();
      }

      this.trendChart = echarts.init(chartDom);

      // 尝试从多个数据源获取数据
      let data = [];
      let dataName = '数据';

      const predictions = this.results.predictions || this.results.y_pred || [];
      const actualValues = this.results.actual_values || this.results.y_true || [];
      const featureValues = this.results.feature_values || [];

      if (predictions.length > 0) {
        data = predictions.slice(0, 100);
        dataName = '预测值';
      } else if (actualValues.length > 0) {
        data = actualValues.slice(0, 100);
        dataName = '实际值';
      } else if (featureValues.length > 0) {
        data = featureValues.slice(0, 100);
        dataName = '特征值';
      }

      if (data.length === 0) {
        this.trendChart.setOption({
          title: {
            text: '暂无趋势数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      const indices = Array.from({length: data.length}, (_, i) => i);

      const option = {
        title: {
          text: '数据趋势图',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            return `索引: ${params[0].dataIndex}<br/>${dataName}: ${params[0].data.toFixed(4)}`;
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
          data: indices,
          name: '样本索引'
        },
        yAxis: {
          type: 'value',
          name: dataName
        },
        series: [
          {
            name: dataName,
            type: 'line',
            data: data,
            itemStyle: { color: '#91cc75' },
            symbol: 'circle',
            symbolSize: 3,
            smooth: true
          }
        ]
      };

      this.trendChart.setOption(option);
    }
  }
};
</script>

<style scoped>
.generic-results-visualization {
  padding: 0;
}

.algorithm-info-card,
.statistics-card,
.params-card,
.sample-data-card,
.raw-data-card {
  margin-bottom: 20px;
}

.chart {
  width: 100%;
  height: 350px;
}

.chart-card {
  margin-bottom: 20px;
}

.el-descriptions {
  margin-top: 10px;
}

.el-table {
  margin-top: 10px;
}
</style>
