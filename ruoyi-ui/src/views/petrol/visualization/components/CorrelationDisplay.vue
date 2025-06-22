<template>
  <div class="correlation-display">
    <div id="correlation-chart" :style="{ width: '100%', height: height }"></div>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "CorrelationDisplay",
  props: {
    data: {
      type: Object,
      default: () => ({})
    },
    config: {
      type: Object,
      default: () => ({})
    },
    height: {
      type: String,
      default: '500px'
    }
  },
  data() {
    return {
      chart: null
    };
  },
  mounted() {
    this.$nextTick(() => {
      this.initChart();
    });
  },
  beforeDestroy() {
    if (this.chart) {
      this.chart.dispose();
    }
  },
  methods: {
    initChart() {
      const container = document.getElementById('correlation-chart');
      if (!container) return;
      
      this.chart = echarts.init(container);
      this.updateChart();
      this.$emit('chart-ready', this.chart);
    },
    
    updateChart() {
      if (!this.chart) return;

      // 检查是否有真实数据
      if (!this.data || !this.data.length) {
        this.showNoDataMessage();
        return;
      }

      // 从配置中获取数值列
      const numericColumns = this.config.numericColumns || [];
      if (numericColumns.length < 2) {
        this.showInsufficientDataMessage();
        return;
      }

      // 计算真实的相关性矩阵
      const correlationMatrix = this.calculateCorrelationMatrix(numericColumns);
      const data = [];

      for (let i = 0; i < numericColumns.length; i++) {
        for (let j = 0; j < numericColumns.length; j++) {
          const value = correlationMatrix[i][j];
          data.push([i, j, parseFloat(value.toFixed(3))]);
        }
      }
      
      const option = {
        title: {
          text: '相关性热力图',
          left: 'center'
        },
        tooltip: {
          position: 'top',
          formatter: function(params) {
            return `${columns[params.data[1]]} vs ${columns[params.data[0]]}<br/>相关系数: ${params.data[2]}`;
          }
        },
        grid: {
          height: '50%',
          top: '10%'
        },
        xAxis: {
          type: 'category',
          data: numericColumns,
          splitArea: {
            show: true
          }
        },
        yAxis: {
          type: 'category',
          data: numericColumns,
          splitArea: {
            show: true
          }
        },
        visualMap: {
          min: -1,
          max: 1,
          calculable: true,
          orient: 'horizontal',
          left: 'center',
          bottom: '15%',
          inRange: {
            color: ['#313695', '#4575b4', '#74add1', '#abd9e9', '#e0f3f8', '#ffffbf', '#fee090', '#fdae61', '#f46d43', '#d73027', '#a50026']
          }
        },
        series: [{
          name: '相关系数',
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
      
      this.chart.setOption(option);
    },

    calculateCorrelationMatrix(columns) {
      const matrix = [];

      for (let i = 0; i < columns.length; i++) {
        matrix[i] = [];
        for (let j = 0; j < columns.length; j++) {
          if (i === j) {
            matrix[i][j] = 1.0; // 自相关为1
          } else {
            // 计算皮尔逊相关系数
            const col1Data = this.data.map(row => row[columns[i]]).filter(val => val != null);
            const col2Data = this.data.map(row => row[columns[j]]).filter(val => val != null);

            if (col1Data.length < 2 || col2Data.length < 2) {
              matrix[i][j] = 0;
              continue;
            }

            const correlation = this.pearsonCorrelation(col1Data, col2Data);
            matrix[i][j] = isNaN(correlation) ? 0 : correlation;
          }
        }
      }

      return matrix;
    },

    pearsonCorrelation(x, y) {
      const n = Math.min(x.length, y.length);
      if (n < 2) return 0;

      const sumX = x.slice(0, n).reduce((a, b) => a + b, 0);
      const sumY = y.slice(0, n).reduce((a, b) => a + b, 0);
      const sumXY = x.slice(0, n).reduce((sum, xi, i) => sum + xi * y[i], 0);
      const sumX2 = x.slice(0, n).reduce((sum, xi) => sum + xi * xi, 0);
      const sumY2 = y.slice(0, n).reduce((sum, yi) => sum + yi * yi, 0);

      const numerator = n * sumXY - sumX * sumY;
      const denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));

      return denominator === 0 ? 0 : numerator / denominator;
    },

    showNoDataMessage() {
      const option = {
        title: {
          text: '暂无数据',
          left: 'center',
          top: 'center',
          textStyle: {
            fontSize: 18,
            color: '#999'
          }
        }
      };
      this.chart.setOption(option);
    },

    showInsufficientDataMessage() {
      const option = {
        title: {
          text: '数据不足',
          subtext: '需要至少2个数值列才能计算相关性',
          left: 'center',
          top: 'center',
          textStyle: {
            fontSize: 18,
            color: '#999'
          }
        }
      };
      this.chart.setOption(option);
    }
  }
};
</script>

<style scoped>
.correlation-display {
  width: 100%;
  height: 100%;
}
</style>
