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

      // 显示需要真实数据的提示
      const option = {
        title: {
          text: '数据分布图',
          left: 'center',
          top: '40%',
          textStyle: {
            fontSize: 18,
            color: '#666'
          }
        },
        graphic: {
          elements: [{
            type: 'text',
            left: 'center',
            top: '55%',
            style: {
              text: '请提供真实数据源以显示分布图',
              fontSize: 14,
              fill: '#999'
            }
          }]
        }
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
