<template>
  <div class="distribution-display">
    <div id="distribution-chart" :style="{ width: '100%', height: height }"></div>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "DistributionDisplay",
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
      const container = document.getElementById('distribution-chart');
      if (!container) return;
      
      this.chart = echarts.init(container);
      this.updateChart();
      this.$emit('chart-ready', this.chart);
    },
    
    updateChart() {
      if (!this.chart) return;
      
      // 生成模拟分布数据
      const bins = this.config.bins || 30;
      const data = [];
      const categories = [];
      
      // 生成正态分布数据
      for (let i = 0; i < bins; i++) {
        const x = i * 10;
        const y = Math.exp(-Math.pow(x - 150, 2) / (2 * Math.pow(50, 2))) * 100;
        data.push(y + Math.random() * 10);
        categories.push(x.toString());
      }
      
      const option = {
        title: {
          text: '数据分布图',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        xAxis: {
          type: 'category',
          data: categories,
          name: '数值范围'
        },
        yAxis: {
          type: 'value',
          name: '频次'
        },
        series: [{
          name: '频次分布',
          type: 'bar',
          data: data,
          itemStyle: {
            color: '#5470c6'
          }
        }]
      };
      
      this.chart.setOption(option);
    }
  }
};
</script>

<style scoped>
.distribution-display {
  width: 100%;
  height: 100%;
}
</style>
