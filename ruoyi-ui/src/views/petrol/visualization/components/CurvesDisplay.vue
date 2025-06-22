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
      const depthColumn = this.config.depthColumn || 'depth';
      const featureColumns = this.config.featureColumns || ['gamma', 'resistivity', 'porosity'];
      const chartType = this.config.chartType || 'line';
      const smooth = this.config.displayOptions && this.config.displayOptions.includes('smooth');

      // 生成深度数据
      const depths = [];
      const minDepth = this.config.depthMin || 1000;
      const maxDepth = this.config.depthMax || 2000;
      const step = (maxDepth - minDepth) / 200;

      for (let i = 0; i <= 200; i++) {
        depths.push(minDepth + i * step);
      }

      // 颜色主题
      const colorThemes = {
        petroleum: ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd', '#8c564b'],
        geology: ['#8B4513', '#228B22', '#4169E1', '#DC143C', '#FF8C00', '#9932CC'],
        rainbow: ['#FF0000', '#FF8000', '#FFFF00', '#80FF00', '#00FF00', '#00FF80'],
        default: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272']
      };

      const colors = colorThemes[this.config.colorTheme] || colorThemes.default;

      // 计算每个坐标系的宽度
      const numFeatures = featureColumns.length;
      const gridWidth = Math.floor(90 / numFeatures); // 总宽度90%，平均分配
      const gridGap = 2; // 坐标系之间的间隔

      // 生成网格配置（每个特征一个独立的坐标系）
      const grids = featureColumns.map((column, index) => ({
        left: `${5 + index * (gridWidth + gridGap)}%`,
        right: `${95 - (index + 1) * gridWidth - index * gridGap}%`,
        top: '15%',
        bottom: '10%',
        containLabel: true
      }));

      // 生成X轴配置（每个特征一个X轴，显示特征值）
      const xAxes = featureColumns.map((column, index) => ({
        type: 'value',
        gridIndex: index,
        name: this.getColumnDisplayName(column),
        nameLocation: 'middle',
        nameGap: 30,
        axisLine: {
          show: true,
          lineStyle: {
            color: colors[index % colors.length]
          }
        },
        axisLabel: {
          color: colors[index % colors.length],
          fontSize: 10
        },
        splitLine: {
          show: this.config.displayOptions && this.config.displayOptions.includes('grid'),
          lineStyle: {
            color: '#e0e0e0',
            type: 'dashed'
          }
        }
      }));

      // 生成Y轴配置（每个坐标系一个Y轴，显示深度，反向显示）
      const yAxes = featureColumns.map((column, index) => ({
        type: 'value',
        gridIndex: index,
        name: index === 0 ? '深度 (m)' : '', // 只在第一个坐标系显示深度标签
        nameLocation: 'middle',
        nameGap: 40,
        inverse: true, // 深度反向显示（从上到下递增）
        min: minDepth,
        max: maxDepth,
        axisLine: {
          show: true,
          lineStyle: {
            color: '#666'
          }
        },
        axisLabel: {
          show: index === 0, // 只在第一个坐标系显示深度刻度
          color: '#666',
          fontSize: 10
        },
        splitLine: {
          show: this.config.displayOptions && this.config.displayOptions.includes('grid'),
          lineStyle: {
            color: '#e0e0e0',
            type: 'dashed'
          }
        }
      }));

      // 生成系列数据
      const series = featureColumns.map((column, index) => {
        const data = depths.map(depth => {
          // 根据不同的特征生成不同的模拟数据
          let value;
          switch (column) {
            case 'gamma':
              value = 50 + 30 * Math.sin((depth - minDepth) / 100) + Math.random() * 20;
              break;
            case 'resistivity':
              value = 20 + 15 * Math.cos((depth - minDepth) / 150) + Math.random() * 10;
              break;
            case 'porosity':
              value = 0.2 + 0.1 * Math.sin((depth - minDepth) / 80) + Math.random() * 0.05;
              break;
            case 'permeability':
              value = 50 + 40 * Math.sin((depth - minDepth) / 120) + Math.random() * 30;
              break;
            case 'density':
              value = 2.2 + 0.3 * Math.sin((depth - minDepth) / 90) + Math.random() * 0.1;
              break;
            case 'neutron':
              value = 0.15 + 0.1 * Math.cos((depth - minDepth) / 110) + Math.random() * 0.05;
              break;
            default:
              value = Math.random() * 100;
          }
          return [parseFloat(value.toFixed(3)), depth]; // [特征值, 深度]
        });

        return {
          name: this.getColumnDisplayName(column),
          type: chartType,
          xAxisIndex: index,
          yAxisIndex: index,
          data: data,
          smooth: smooth,
          symbol: chartType === 'scatter' ? 'circle' : 'none',
          symbolSize: chartType === 'scatter' ? 3 : 1,
          lineStyle: {
            width: 2,
            color: colors[index % colors.length]
          },
          itemStyle: {
            color: colors[index % colors.length]
          }
        };
      });

      // 生成数据缩放配置
      const dataZoom = this.config.displayOptions && this.config.displayOptions.includes('dataZoom') ? [
        {
          type: 'slider',
          show: true,
          yAxisIndex: yAxes.map((_, index) => index),
          start: 0,
          end: 100,
          orient: 'vertical',
          right: 10,
          width: 20
        },
        {
          type: 'inside',
          yAxisIndex: yAxes.map((_, index) => index),
          start: 0,
          end: 100,
          orient: 'vertical'
        }
      ] : [];

      // 生成提示框配置
      const tooltip = {
        trigger: 'axis',
        axisPointer: {
          type: 'line',
          lineStyle: {
            color: '#666',
            type: 'dashed'
          }
        },
        formatter: function(params) {
          if (params.length === 0) return '';
          let result = `深度: ${params[0].value[1]}m<br/>`;
          params.forEach(param => {
            if (param.value && param.value.length >= 2) {
              result += `${param.seriesName}: ${param.value[0]}<br/>`;
            }
          });
          return result;
        }
      };

      return {
        grids,
        xAxes,
        yAxes,
        series,
        dataZoom,
        tooltip
      };
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
      const depthColumn = this.config.depthColumn || 'depth';
      const featureColumns = this.config.featureColumns || ['gamma', 'resistivity', 'porosity'];
      const chartType = this.config.chartType || 'line';
      const smooth = this.config.displayOptions && this.config.displayOptions.includes('smooth');

      // 生成深度数据
      const depths = [];
      const minDepth = this.config.depthMin || 1000;
      const maxDepth = this.config.depthMax || 2000;
      const step = (maxDepth - minDepth) / 200;

      for (let i = 0; i <= 200; i++) {
        depths.push(minDepth + i * step);
      }

      // 颜色主题
      const colorThemes = {
        petroleum: ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd', '#8c564b'],
        geology: ['#8B4513', '#228B22', '#4169E1', '#DC143C', '#FF8C00', '#9932CC'],
        rainbow: ['#FF0000', '#FF8000', '#FFFF00', '#80FF00', '#00FF00', '#00FF80'],
        default: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272']
      };

      const colors = colorThemes[this.config.colorTheme] || colorThemes.default;

      // 单一网格配置
      const grids = [{
        left: '10%',
        right: '10%',
        top: '15%',
        bottom: '10%',
        containLabel: true
      }];

      // 单一X轴配置（深度）
      const xAxes = [{
        type: 'value',
        name: '深度 (m)',
        nameLocation: 'middle',
        nameGap: 30,
        min: minDepth,
        max: maxDepth,
        axisLine: {
          lineStyle: {
            color: '#666'
          }
        },
        splitLine: {
          show: this.config.displayOptions && this.config.displayOptions.includes('grid'),
          lineStyle: {
            color: '#e0e0e0',
            type: 'dashed'
          }
        }
      }];

      // 多Y轴配置（每个特征一个Y轴）
      const yAxes = featureColumns.map((column, index) => ({
        type: 'value',
        name: this.getColumnDisplayName(column),
        nameLocation: 'middle',
        nameGap: 50,
        position: index % 2 === 0 ? 'left' : 'right',
        offset: Math.floor(index / 2) * 80,
        axisLine: {
          show: true,
          lineStyle: {
            color: colors[index % colors.length]
          }
        },
        axisLabel: {
          color: colors[index % colors.length]
        },
        splitLine: {
          show: false
        }
      }));

      // 生成系列数据
      const series = featureColumns.map((column, index) => {
        const data = depths.map(depth => {
          // 根据不同的特征生成不同的模拟数据
          let value;
          switch (column) {
            case 'gamma':
              value = 50 + 30 * Math.sin((depth - minDepth) / 100) + Math.random() * 20;
              break;
            case 'resistivity':
              value = 20 + 15 * Math.cos((depth - minDepth) / 150) + Math.random() * 10;
              break;
            case 'porosity':
              value = 0.2 + 0.1 * Math.sin((depth - minDepth) / 80) + Math.random() * 0.05;
              break;
            case 'permeability':
              value = 50 + 40 * Math.sin((depth - minDepth) / 120) + Math.random() * 30;
              break;
            case 'density':
              value = 2.2 + 0.3 * Math.sin((depth - minDepth) / 90) + Math.random() * 0.1;
              break;
            case 'neutron':
              value = 0.15 + 0.1 * Math.cos((depth - minDepth) / 110) + Math.random() * 0.05;
              break;
            default:
              value = Math.random() * 100;
          }
          return [depth, parseFloat(value.toFixed(3))]; // [深度, 特征值]
        });

        return {
          name: this.getColumnDisplayName(column),
          type: chartType,
          yAxisIndex: index,
          data: data,
          smooth: smooth,
          symbol: chartType === 'scatter' ? 'circle' : 'none',
          symbolSize: chartType === 'scatter' ? 4 : 2,
          lineStyle: {
            width: 2,
            color: colors[index % colors.length]
          },
          itemStyle: {
            color: colors[index % colors.length]
          }
        };
      });

      // 生成数据缩放配置
      const dataZoom = this.config.displayOptions && this.config.displayOptions.includes('dataZoom') ? [
        {
          type: 'slider',
          show: true,
          xAxisIndex: [0],
          start: 0,
          end: 100
        },
        {
          type: 'inside',
          xAxisIndex: [0],
          start: 0,
          end: 100
        }
      ] : [];

      // 生成提示框配置
      const tooltip = {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
          label: {
            backgroundColor: '#6a7985'
          }
        },
        formatter: function(params) {
          let result = `深度: ${params[0].axisValue}m<br/>`;
          params.forEach(param => {
            result += `${param.seriesName}: ${param.value[1]}<br/>`;
          });
          return result;
        }
      };

      return {
        grids,
        xAxes,
        yAxes,
        series,
        dataZoom,
        tooltip
      };
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
