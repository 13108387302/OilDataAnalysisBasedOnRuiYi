<template>
  <div class="comparison-display">
    <div id="comparison-chart" :style="{ width: '100%', height: height }"></div>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "ComparisonDisplay",
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
      const container = document.getElementById('comparison-chart');
      if (!container) return;
      
      this.chart = echarts.init(container);
      this.updateChart();
      this.$emit('chart-ready', this.chart);
    },
    
    updateChart() {
      if (!this.chart) return;
      
      const option = {
        title: {
          text: '对比分析图表',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['数据集A', '数据集B', '数据集C'],
          top: 30
        },
        xAxis: {
          type: 'category',
          data: ['指标1', '指标2', '指标3', '指标4', '指标5']
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '数据集A',
            type: 'bar',
            data: [120, 200, 150, 80, 70]
          },
          {
            name: '数据集B',
            type: 'bar',
            data: [100, 180, 120, 90, 85]
          },
          {
            name: '数据集C',
            type: 'bar',
            data: [140, 160, 180, 75, 95]
          }
        ]
      };
      
      this.chart.setOption(option);
    }
  }
};
</script>

<style scoped>
.comparison-display {
  width: 100%;
  height: 100%;
}
</style>
