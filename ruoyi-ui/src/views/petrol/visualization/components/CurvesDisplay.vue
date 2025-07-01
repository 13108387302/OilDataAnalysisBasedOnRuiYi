<template>
  <div class="curves-display">
    <div id="curves-chart" :style="{ width: '100%', height: height }"></div>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "CurvesDisplay",
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
  watch: {
    data: {
      handler() {
        this.$nextTick(() => {
          this.updateChart();
        });
      },
      deep: true
    },
    config: {
      handler() {
        this.$nextTick(() => {
          this.updateChart();
        });
      },
      deep: true
    }
  },
  beforeDestroy() {
    if (this.chart) {
      this.chart.dispose();
    }
  },
  methods: {
    initChart() {
      const container = document.getElementById('curves-chart');
      if (!container) return;
      
      this.chart = echarts.init(container);
      this.updateChart();
      
      // 响应式调整
      window.addEventListener('resize', this.handleResize);
      
      // 触发图表准备就绪事件
      this.$emit('chart-ready', this.chart);
    },
    
    updateChart() {
      if (!this.chart) return;

      // 检查是否有真实数据
      if (!this.data || !this.data.length) {
        this.showNoDataMessage();
        return;
      }

      // 根据布局模式生成不同的数据
      const layoutMode = this.config.layoutMode || 'parallel';
      const chartData = layoutMode === 'parallel' ?
        this.generateParallelCurvesFromRealData() :
        this.generateOverlayCurvesFromRealData();

      const option = {
        title: {
          text: `石油测井曲线图 (${layoutMode === 'parallel' ? '并列模式' : '叠加模式'})`,
          left: 'center',
          textStyle: {
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        tooltip: chartData.tooltip,
        legend: {
          data: chartData.series.map(s => s.name),
          top: 30,
          type: 'scroll'
        },
        grid: chartData.grids,
        toolbox: {
          feature: {
            saveAsImage: {
              title: '保存为图片'
            },
            dataZoom: {
              title: {
                zoom: '区域缩放',
                back: '区域缩放还原'
              }
            },
            restore: {
              title: '还原'
            }
          }
        },
        xAxis: chartData.xAxes,
        yAxis: chartData.yAxes,
        dataZoom: chartData.dataZoom,
        series: chartData.series
      };

      this.chart.setOption(option, true);
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
        },
        graphic: {
          elements: [{
            type: 'text',
            left: 'center',
            top: 'center',
            style: {
              text: '请先选择数据集或创建分析任务',
              fontSize: 14,
              fill: '#999'
            }
          }]
        }
      };
      this.chart.setOption(option, true);
    },
    
    generateParallelCurvesFromRealData() {
      const depthColumn = this.config.depthColumn || 'DEPTH';
      const featureColumns = this.config.featureColumns || [];
      const chartType = this.config.chartType || 'line';
      const smooth = this.config.displayOptions && this.config.displayOptions.includes('smooth');

      if (!featureColumns.length) {
        return this.getEmptyChartData();
      }

      // 从真实数据中提取深度和特征值
      const depths = this.data.map(row => row[depthColumn]).filter(d => d != null);
      const minDepth = Math.min(...depths);
      const maxDepth = Math.max(...depths);

      // 为每个特征创建独立的网格
      const gridCount = featureColumns.length;
      const gridWidth = 1 / gridCount;
      const grids = [];
      const xAxes = [];
      const yAxes = [];

      for (let i = 0; i < gridCount; i++) {
        grids.push({
          left: `${i * gridWidth * 100 + 5}%`,
          right: `${(gridCount - i - 1) * gridWidth * 100 + 5}%`,
          top: '15%',
          bottom: '15%'
        });

        xAxes.push({
          gridIndex: i,
          type: 'value',
          name: featureColumns[i],
          nameLocation: 'middle',
          nameGap: 30,
          axisLabel: {
            fontSize: 10
          }
        });

        yAxes.push({
          gridIndex: i,
          type: 'value',
          name: i === 0 ? depthColumn : '',
          nameLocation: 'middle',
          nameGap: 40,
          inverse: true,
          min: minDepth,
          max: maxDepth,
          axisLabel: {
            fontSize: 10
          }
        });
      }

      // 生成系列数据
      const series = featureColumns.map((column, index) => {
        const data = this.data.map(row => {
          const depth = row[depthColumn];
          const value = row[column];
          return depth != null && value != null ? [value, depth] : null;
        }).filter(point => point !== null);

        return {
          name: column,
          type: chartType,
          smooth: smooth,
          xAxisIndex: index,
          yAxisIndex: index,
          data: data,
          lineStyle: {
            width: 2
          },
          symbol: chartType === 'scatter' ? 'circle' : 'none',
          symbolSize: 4
        };
      });

      return {
        grids: grids,
        xAxes: xAxes,
        yAxes: yAxes,
        series: series,
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            let result = `${depthColumn}: ${params[0].data[1]}<br/>`;
            params.forEach(param => {
              result += `${param.seriesName}: ${param.data[0]}<br/>`;
            });
            return result;
          }
        },
        dataZoom: [
          {
            type: 'slider',
            yAxisIndex: Array.from({length: gridCount}, (_, i) => i),
            width: 20,
            right: 10,
            start: 0,
            end: 100
          }
        ]
      };
    },

    generateParallelCurvesData() {
      // 🔴 完全禁用模拟数据生成 - 抛出错误
      const errorMessage = '系统已完全禁用模拟数据生成，必须使用真实数据源进行可视化';
      console.error('❌ generateParallelCurvesData:', errorMessage);
      throw new Error(errorMessage);


    },

    generateOverlayCurvesFromRealData() {
      const depthColumn = this.config.depthColumn || 'DEPTH';
      const featureColumns = this.config.featureColumns || [];
      const chartType = this.config.chartType || 'line';
      const smooth = this.config.displayOptions && this.config.displayOptions.includes('smooth');

      if (!featureColumns.length) {
        return this.getEmptyChartData();
      }

      // 从真实数据中提取深度范围
      const depths = this.data.map(row => row[depthColumn]).filter(d => d != null);
      const minDepth = Math.min(...depths);
      const maxDepth = Math.max(...depths);

      // 单一网格配置
      const grids = [{
        left: '10%',
        right: '10%',
        top: '15%',
        bottom: '15%'
      }];

      // 双Y轴配置：左侧深度，右侧特征值
      const yAxes = [
        {
          type: 'value',
          name: depthColumn,
          nameLocation: 'middle',
          nameGap: 40,
          inverse: true,
          min: minDepth,
          max: maxDepth,
          position: 'left'
        },
        {
          type: 'value',
          name: '特征值',
          nameLocation: 'middle',
          nameGap: 40,
          position: 'right'
        }
      ];

      const xAxes = [{
        type: 'value',
        show: false
      }];

      // 生成系列数据
      const series = featureColumns.map((column, index) => {
        const data = this.data.map(row => {
          const depth = row[depthColumn];
          const value = row[column];
          return depth != null && value != null ? [index, depth, value] : null;
        }).filter(point => point !== null);

        return {
          name: column,
          type: chartType,
          smooth: smooth,
          yAxisIndex: 0,
          data: data.map(point => [point[2], point[1]]), // [特征值, 深度]
          lineStyle: {
            width: 2
          },
          symbol: chartType === 'scatter' ? 'circle' : 'none',
          symbolSize: 4
        };
      });

      return {
        grids: grids,
        xAxes: xAxes,
        yAxes: yAxes,
        series: series,
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            let result = `${depthColumn}: ${params[0].data[1]}<br/>`;
            params.forEach(param => {
              result += `${param.seriesName}: ${param.data[0]}<br/>`;
            });
            return result;
          }
        },
        dataZoom: [
          {
            type: 'slider',
            yAxisIndex: 0,
            width: 20,
            right: 10,
            start: 0,
            end: 100
          }
        ]
      };
    },

    getEmptyChartData() {
      return {
        grids: [],
        xAxes: [],
        yAxes: [],
        series: [],
        tooltip: {},
        dataZoom: []
      };
    },

    generateOverlayCurvesData() {
      // 🔴 完全禁用模拟数据生成 - 抛出错误
      const errorMessage = '系统已完全禁用模拟数据生成，必须使用真实数据源进行可视化';
      console.error('❌ generateOverlayCurvesData:', errorMessage);
      throw new Error(errorMessage);
    },
    
    getColumnDisplayName(column) {
      const displayNames = {
        depth: '深度 (m)',
        gamma: '自然伽马 (API)',
        resistivity: '电阻率 (Ω·m)',
        resistivity_deep: '深电阻率 (Ω·m)',
        resistivity_shallow: '浅电阻率 (Ω·m)',
        porosity: '孔隙度 (%)',
        permeability: '渗透率 (mD)',
        density: '密度 (g/cm³)',
        neutron: '中子孔隙度 (%)',
        photoelectric: '光电因子 (Pe)',
        sp: '自然电位 (mV)',
        caliper: '井径 (inch)',
        temperature: '温度 (°C)',
        pressure: '压力 (MPa)',
        lithology: '岩性',
        formation: '地层',
        oil_saturation: '含油饱和度 (%)',
        water_saturation: '含水饱和度 (%)',
        rqd: 'RQD (%)',
        fracture_density: '裂缝密度'
      };
      return displayNames[column] || column;
    },
    
    handleResize() {
      if (this.chart) {
        this.chart.resize();
      }
    }
  }
};
</script>

<style scoped>
.curves-display {
  width: 100%;
  height: 100%;
}
</style>
