<template>
  <div class="statistics-display">
    <div id="statistics-chart" :style="{ width: '100%', height: height }"></div>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "StatisticsDisplay",
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
      const container = document.getElementById('statistics-chart');
      if (!container) return;
      
      this.chart = echarts.init(container);
      this.updateChart();
      this.$emit('chart-ready', this.chart);
    },
    
    updateChart() {
      if (!this.chart) return;
      
      const option = {
        title: {
          text: '统计分析图表',
          left: 'center'
        },
        tooltip: {
          trigger: 'item'
        },
        series: [{
          name: '统计数据',
          type: 'pie',
          radius: '50%',
          data: [
            { value: 1048, name: '数据A' },
            { value: 735, name: '数据B' },
            { value: 580, name: '数据C' },
            { value: 484, name: '数据D' },
            { value: 300, name: '数据E' }
          ]
        }]
      };
      
      this.chart.setOption(option);
    }
  }
};
</script>

<style scoped>
.statistics-display {
  width: 100%;
  height: 100%;
}
</style>
