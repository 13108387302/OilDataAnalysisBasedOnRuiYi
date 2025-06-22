<template>
  <div class="correlation-analysis">
    <!-- 参数配置 -->
    <el-card class="config-card" shadow="never">
      <div slot="header">
        <span>⚙️ 相关性分析配置</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="config-item">
            <label>分析方法:</label>
            <el-select 
              v-model="method" 
              size="small"
              style="width: 100%">
              <el-option label="皮尔逊相关系数" value="pearson"></el-option>
              <el-option label="斯皮尔曼相关系数" value="spearman"></el-option>
              <el-option label="肯德尔相关系数" value="kendall"></el-option>
            </el-select>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="config-item">
            <label>显示阈值:</label>
            <el-slider
              v-model="threshold"
              :min="0"
              :max="1"
              :step="0.1"
              :format-tooltip="formatThreshold"
              style="margin-top: 8px;">
            </el-slider>
          </div>
        </el-col>
      </el-row>
      
      <div class="column-selection">
        <label>选择分析列:</label>
        <div class="column-checkboxes">
          <el-checkbox-group v-model="selectedColumns">
            <el-checkbox 
              v-for="column in numericColumns" 
              :key="column"
              :label="column"
              class="column-checkbox">
              {{ column }}
            </el-checkbox>
          </el-checkbox-group>
        </div>
        <div class="selection-actions">
          <el-button type="text" size="mini" @click="selectAllColumns">
            {{ selectedColumns.length === numericColumns.length ? '取消全选' : '全选' }}
          </el-button>
          <el-button type="text" size="mini" @click="selectHighCorrelation">
            选择高相关
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 相关性结果 -->
    <el-card class="result-card" shadow="never">
      <div slot="header">
        <span>📊 相关性分析结果</span>
        <div class="result-controls">
          <el-button-group size="mini">
            <el-button 
              :type="viewMode === 'heatmap' ? 'primary' : ''"
              @click="viewMode = 'heatmap'">
              热力图
            </el-button>
            <el-button 
              :type="viewMode === 'network' ? 'primary' : ''"
              @click="viewMode = 'network'">
              网络图
            </el-button>
            <el-button 
              :type="viewMode === 'table' ? 'primary' : ''"
              @click="viewMode = 'table'">
              数据表
            </el-button>
          </el-button-group>
          <el-button 
            type="text" 
            icon="el-icon-download"
            @click="exportResult">
            导出
          </el-button>
        </div>
      </div>
      
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="6" animated />
      </div>
      
      <div v-else class="result-content">
        <!-- 热力图 -->
        <div v-if="viewMode === 'heatmap'" class="chart-container">
          <div ref="heatmapChart" class="chart"></div>
        </div>
        
        <!-- 网络图 -->
        <div v-if="viewMode === 'network'" class="chart-container">
          <div ref="networkChart" class="chart"></div>
        </div>
        
        <!-- 数据表 -->
        <div v-if="viewMode === 'table'" class="table-container">
          <el-table 
            :data="correlationTableData" 
            border 
            stripe 
            size="small"
            max-height="500"
            style="width: 100%;">
            <el-table-column prop="variable1" label="变量1" width="120" fixed="left">
              <template slot-scope="scope">
                <el-tag size="mini">{{ scope.row.variable1 }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="variable2" label="变量2" width="120">
              <template slot-scope="scope">
                <el-tag size="mini">{{ scope.row.variable2 }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="correlation" label="相关系数" width="120" align="center">
              <template slot-scope="scope">
                <span :class="getCorrelationClass(scope.row.correlation)">
                  {{ scope.row.correlation.toFixed(4) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="strength" label="相关强度" width="100" align="center">
              <template slot-scope="scope">
                <el-tag :type="getStrengthType(scope.row.correlation)" size="mini">
                  {{ getStrengthLabel(scope.row.correlation) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="200">
              <template slot-scope="scope">
                {{ getCorrelationDescription(scope.row.correlation) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <!-- 统计信息 -->
        <div class="statistics-info">
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-item">
                <span class="label">分析变量:</span>
                <span class="value">{{ selectedColumns.length }}</span>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <span class="label">相关对数:</span>
                <span class="value">{{ correlationPairs }}</span>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <span class="label">强相关(>0.7):</span>
                <span class="value">{{ strongCorrelations }}</span>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <span class="label">分析方法:</span>
                <span class="value">{{ methodLabels[method] }}</span>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-card>


  </div>
</template>

<script>
import { getDataSourceColumns, readDataSourceData } from "@/api/petrol/visualization";
import * as echarts from 'echarts';

export default {
  name: "CorrelationAnalysis",
  props: {
    sourceId: {
      type: String,
      required: true
    },
    sourceType: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      loading: false,
      numericColumns: [],
      selectedColumns: [],
      method: 'pearson',
      threshold: 0.5,
      viewMode: 'heatmap',
      correlationGenerated: true, // 强制显示结果
      correlationMatrix: {},
      correlationData: [],
      heatmapChart: null,
      networkChart: null,
      // 防重复请求标志
      isLoadingData: false,
      isInitialized: false,

      methodLabels: {
        pearson: '皮尔逊',
        spearman: '斯皮尔曼',
        kendall: '肯德尔'
      }
    };
  },
  computed: {
    correlationTableData() {
      const data = [];
      for (let i = 0; i < this.selectedColumns.length; i++) {
        for (let j = i + 1; j < this.selectedColumns.length; j++) {
          const var1 = this.selectedColumns[i];
          const var2 = this.selectedColumns[j];
          const correlation = this.correlationMatrix[var1]?.[var2] || 0;
          
          if (Math.abs(correlation) >= this.threshold) {
            data.push({
              variable1: var1,
              variable2: var2,
              correlation: correlation
            });
          }
        }
      }
      return data.sort((a, b) => Math.abs(b.correlation) - Math.abs(a.correlation));
    },
    correlationPairs() {
      return (this.selectedColumns.length * (this.selectedColumns.length - 1)) / 2;
    },
    strongCorrelations() {
      return this.correlationTableData.filter(item => Math.abs(item.correlation) > 0.7).length;
    }
  },
  created() {
    console.log('🔗 CorrelationAnalysis组件创建', {
      sourceId: this.sourceId,
      sourceType: this.sourceType
    });
    this.loadColumns();
  },
  beforeDestroy() {
    if (this.heatmapChart) {
      this.heatmapChart.dispose();
    }
    if (this.networkChart) {
      this.networkChart.dispose();
    }
  },
  watch: {
    // 监听选择列变化
    selectedColumns: {
      handler() {
        if (this.selectedColumns.length >= 2 && this.isInitialized && !this.isLoadingData) {
          console.log('🔄 选择列变化，自动刷新相关性分析');
          this.loadCorrelationData();
        }
      },
      deep: true
    },
    // 监听分析方法变化
    method() {
      if (this.selectedColumns.length >= 2 && this.isInitialized && !this.isLoadingData) {
        console.log('🔄 分析方法变化，重新计算相关性');
        this.loadCorrelationData();
      }
    },
    // 监听显示阈值变化
    threshold() {
      if (this.correlationMatrix && Object.keys(this.correlationMatrix).length > 0 && this.isInitialized) {
        console.log('🔄 显示阈值变化，重新渲染图表');
        this.$nextTick(() => {
          this.renderCharts();
        });
      }
    },
    // 监听视图模式变化
    viewMode() {
      if (this.isInitialized) {
        this.$nextTick(() => {
          this.renderCharts();
        });
      }
    }
  },
  methods: {
    /** 加载列信息 */
    async loadColumns() {
      if (this.isLoadingData) {
        console.log('🚫 正在加载数据，跳过重复请求');
        return;
      }

      this.loading = true;
      this.isLoadingData = true;

      try {
        console.log('📋 开始加载列信息', {
          sourceId: this.sourceId,
          sourceType: this.sourceType
        });

        const response = await getDataSourceColumns(this.sourceId, this.sourceType);
        const data = response.data || {};
        this.numericColumns = data.numericColumns || [];

        console.log('✅ 列信息加载完成', {
          numericColumns: this.numericColumns.length,
          columns: this.numericColumns
        });

        // 默认选择前6个数值列
        if (this.numericColumns.length >= 2) {
          this.selectedColumns = this.numericColumns.slice(0, Math.min(6, this.numericColumns.length));

          // 标记初始化完成
          this.isInitialized = true;

          // 仿照统计分析，自动加载数据并生成分析
          this.$nextTick(() => {
            this.loadCorrelationData();
          });
        }
      } catch (error) {
        console.error('❌ 加载列信息失败', error);
        this.$message.error("加载列信息失败: " + error.message);
      } finally {
        this.loading = false;
        this.isLoadingData = false;
      }
    },

    /** 加载相关性数据（仿照统计分析的loadStatistics方法） */
    async loadCorrelationData() {
      if (this.selectedColumns.length < 2) {
        this.correlationMatrix = {};
        return;
      }

      if (this.isLoadingData) {
        console.log('🚫 正在加载相关性数据，跳过重复请求');
        return;
      }

      this.loading = true;
      this.isLoadingData = true;

      try {
        console.log('🔍 开始加载相关性分析数据', {
          sourceId: this.sourceId,
          sourceType: this.sourceType,
          selectedColumns: this.selectedColumns
        });

        // 读取数据
        const params = {
          columns: this.selectedColumns,
          maxRows: 1000 // 限制数据量以提高性能
        };

        const response = await readDataSourceData(this.sourceId, this.sourceType, params);
        const data = response.data || [];

        console.log('📋 相关性分析数据响应', {
          response: response,
          dataLength: data.length,
          sampleData: data.slice(0, 3)
        });

        if (data.length === 0) {
          this.$message.warning("没有读取到数据");
          return;
        }

        // 计算相关性矩阵
        this.calculateCorrelationMatrix(data);

        // 等待DOM渲染完成后渲染图表
        this.$nextTick(() => {
          this.waitForDOMAndRender();
        });

      } catch (error) {
        console.error('❌ 加载相关性分析数据失败', error);
        this.$message.error("加载相关性分析数据失败: " + error.message);
        this.correlationMatrix = {};
      } finally {
        this.loading = false;
        this.isLoadingData = false;
      }
    },

    /** 生成相关性分析（重新加载数据） */
    generateCorrelation() {
      if (this.isLoadingData) {
        console.log('🚫 正在加载数据，请稍候');
        this.$message.warning('数据正在处理，请稍候');
        return;
      }
      this.loadCorrelationData();
    },

    /** 计算相关性矩阵 */
    calculateCorrelationMatrix(data) {
      this.correlationMatrix = {};
      
      // 简化的皮尔逊相关系数计算
      for (let i = 0; i < this.selectedColumns.length; i++) {
        const col1 = this.selectedColumns[i];
        this.correlationMatrix[col1] = {};
        
        for (let j = 0; j < this.selectedColumns.length; j++) {
          const col2 = this.selectedColumns[j];
          
          if (i === j) {
            this.correlationMatrix[col1][col2] = 1.0;
          } else {
            const correlation = this.calculatePearsonCorrelation(data, col1, col2);
            this.correlationMatrix[col1][col2] = correlation;
          }
        }
      }
    },

    /** 计算皮尔逊相关系数 */
    calculatePearsonCorrelation(data, col1, col2) {
      const values1 = data.map(row => parseFloat(row[col1])).filter(v => !isNaN(v));
      const values2 = data.map(row => parseFloat(row[col2])).filter(v => !isNaN(v));
      
      if (values1.length !== values2.length || values1.length === 0) {
        return 0;
      }

      const n = values1.length;
      const sum1 = values1.reduce((a, b) => a + b, 0);
      const sum2 = values2.reduce((a, b) => a + b, 0);
      const sum1Sq = values1.reduce((a, b) => a + b * b, 0);
      const sum2Sq = values2.reduce((a, b) => a + b * b, 0);
      const pSum = values1.reduce((acc, val, i) => acc + val * values2[i], 0);

      const num = pSum - (sum1 * sum2 / n);
      const den = Math.sqrt((sum1Sq - sum1 * sum1 / n) * (sum2Sq - sum2 * sum2 / n));

      return den === 0 ? 0 : num / den;
    },

    /** 渲染图表 */
    renderCharts() {
      if (this.viewMode === 'heatmap') {
        this.renderHeatmap();
      } else if (this.viewMode === 'network') {
        this.renderNetwork();
      }
    },

    /** 渲染热力图 */
    renderHeatmap() {
      setTimeout(() => {
        const chartDom = this.$refs.heatmapChart;
        if (!chartDom) {
          console.log('⚠️ 热力图DOM元素不存在');
          return;
        }

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 热力图DOM元素不可见，延迟重试');
          setTimeout(() => this.renderHeatmap(), 500);
          return;
        }

        // 设置固定高度确保可见
        chartDom.style.height = '500px';
        chartDom.style.width = '100%';

        if (this.heatmapChart) {
          this.heatmapChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        this.heatmapChart = echarts.init(chartDom);
      
      const data = [];
      for (let i = 0; i < this.selectedColumns.length; i++) {
        for (let j = 0; j < this.selectedColumns.length; j++) {
          const correlation = this.correlationMatrix[this.selectedColumns[i]]?.[this.selectedColumns[j]] || 0;
          data.push([j, i, correlation.toFixed(4)]);
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
            return `${params.name}: ${params.value[2]}`;
          }
        },
        grid: {
          height: '70%',
          top: '10%'
        },
        xAxis: {
          type: 'category',
          data: this.selectedColumns,
          splitArea: {
            show: true
          }
        },
        yAxis: {
          type: 'category',
          data: this.selectedColumns,
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
          bottom: '5%',
          inRange: {
            color: ['#313695', '#4575b4', '#74add1', '#abd9e9', '#e0f3f8', '#ffffbf', '#fee090', '#fdae61', '#f46d43', '#d73027', '#a50026']
          }
        },
        series: [{
          name: '相关性',
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

      this.heatmapChart.setOption(option);
      console.log('✅ 热力图渲染完成');
      }, 100); // 延迟100ms确保DOM渲染完成
    },

    /** 渲染网络图 */
    renderNetwork() {
      setTimeout(() => {
        const chartDom = this.$refs.networkChart;
        if (!chartDom) {
          console.log('⚠️ 网络图DOM元素不存在');
          return;
        }

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 网络图DOM元素不可见，延迟重试');
          setTimeout(() => this.renderNetwork(), 500);
          return;
        }

        // 设置固定高度确保可见
        chartDom.style.height = '500px';
        chartDom.style.width = '100%';

        if (this.networkChart) {
          this.networkChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        this.networkChart = echarts.init(chartDom);
      
      const nodes = this.selectedColumns.map(col => ({
        name: col,
        symbolSize: 30,
        category: 0
      }));

      const links = [];
      for (let i = 0; i < this.selectedColumns.length; i++) {
        for (let j = i + 1; j < this.selectedColumns.length; j++) {
          const correlation = this.correlationMatrix[this.selectedColumns[i]]?.[this.selectedColumns[j]] || 0;
          if (Math.abs(correlation) >= this.threshold) {
            links.push({
              source: this.selectedColumns[i],
              target: this.selectedColumns[j],
              value: Math.abs(correlation),
              lineStyle: {
                width: Math.abs(correlation) * 5,
                color: correlation > 0 ? '#67c23a' : '#f56c6c'
              }
            });
          }
        }
      }

      const option = {
        title: {
          text: '相关性网络图',
          left: 'center'
        },
        tooltip: {
          formatter: function(params) {
            if (params.dataType === 'edge') {
              return `${params.data.source} - ${params.data.target}: ${params.data.value.toFixed(4)}`;
            }
            return params.name;
          }
        },
        series: [{
          type: 'graph',
          layout: 'force',
          data: nodes,
          links: links,
          categories: [{
            name: '变量'
          }],
          roam: true,
          force: {
            repulsion: 100,
            edgeLength: 150
          },
          label: {
            show: true,
            position: 'inside'
          },
          emphasis: {
            focus: 'adjacency'
          }
        }]
      };

      this.networkChart.setOption(option);
      console.log('✅ 网络图渲染完成');
      }, 150); // 延迟150ms确保DOM渲染完成
    },

    /** 全选/取消全选 */
    selectAllColumns() {
      if (this.selectedColumns.length === this.numericColumns.length) {
        this.selectedColumns = [];
      } else {
        this.selectedColumns = [...this.numericColumns];
      }
    },

    /** 强制重新渲染图表 */
    forceRenderCharts() {
      console.log('🔧 强制重新渲染相关性图表');
      if (this.correlationData.length === 0) {
        this.$message.warning('没有相关性数据，请先生成分析');
        return;
      }

      // 销毁现有图表
      [this.heatmapChart, this.networkChart].forEach(chart => {
        if (chart) {
          chart.dispose();
        }
      });

      // 重置图表实例
      this.heatmapChart = null;
      this.networkChart = null;

      // 延迟重新渲染
      setTimeout(() => {
        this.renderCharts();
        this.$message.success('相关性图表重新渲染完成');
      }, 300);
    },

    /** 选择高相关变量 */
    selectHighCorrelation() {
      // 这里可以实现智能选择逻辑
      this.selectedColumns = this.numericColumns.slice(0, 8);
    },

    /** 格式化阈值显示 */
    formatThreshold(val) {
      if (val === null || val === undefined || isNaN(val)) {
        return '0.0';
      }
      return `${Number(val).toFixed(1)}`;
    },

    /** 获取相关性样式类 */
    getCorrelationClass(correlation) {
      const abs = Math.abs(correlation);
      if (abs >= 0.7) return 'strong-correlation';
      if (abs >= 0.3) return 'moderate-correlation';
      return 'weak-correlation';
    },

    /** 获取强度类型 */
    getStrengthType(correlation) {
      const abs = Math.abs(correlation);
      if (abs >= 0.7) return 'danger';
      if (abs >= 0.3) return 'warning';
      return 'info';
    },

    /** 获取强度标签 */
    getStrengthLabel(correlation) {
      const abs = Math.abs(correlation);
      if (abs >= 0.7) return '强相关';
      if (abs >= 0.3) return '中等相关';
      return '弱相关';
    },

    /** 获取相关性描述 */
    getCorrelationDescription(correlation) {
      const abs = Math.abs(correlation);
      const direction = correlation > 0 ? '正相关' : '负相关';
      
      if (abs >= 0.7) {
        return `${direction}，变量间存在强线性关系`;
      } else if (abs >= 0.3) {
        return `${direction}，变量间存在中等程度线性关系`;
      } else {
        return `${direction}，变量间线性关系较弱`;
      }
    },

    /** 导出结果 */
    exportResult() {
      if (this.viewMode === 'heatmap' && this.heatmapChart) {
        const url = this.heatmapChart.getDataURL({
          type: 'png',
          backgroundColor: '#fff'
        });
        this.downloadImage(url, 'correlation_heatmap.png');
      } else if (this.viewMode === 'network' && this.networkChart) {
        const url = this.networkChart.getDataURL({
          type: 'png',
          backgroundColor: '#fff'
        });
        this.downloadImage(url, 'correlation_network.png');
      } else if (this.viewMode === 'table') {
        this.exportTable();
      }
    },

    /** 下载图片 */
    downloadImage(url, filename) {
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      link.click();
      this.$message.success("导出成功");
    },

    /** 等待DOM渲染完成后渲染图表 */
    waitForDOMAndRender() {
      const maxRetries = 10;
      let retryCount = 0;

      const checkAndRender = () => {
        retryCount++;

        // 检查图表容器是否存在且可见
        const heatmapContainer = this.$refs.heatmapChart;
        const networkContainer = this.$refs.networkChart;

        if (heatmapContainer || networkContainer) {
          const container = heatmapContainer || networkContainer;
          const rect = container.getBoundingClientRect();
          if (rect.width > 0 && rect.height > 0) {
            console.log('🔗 相关性分析DOM已准备就绪，开始渲染图表');
            this.renderCharts();
            return;
          }
        }

        if (retryCount < maxRetries) {
          console.log(`⏳ 等待相关性分析DOM渲染... (${retryCount}/${maxRetries})`);
          setTimeout(checkAndRender, 300);
        } else {
          console.log('⚠️ 相关性分析DOM等待超时，强制渲染图表');
          this.renderCharts();
        }
      };

      checkAndRender();
    },

    /** 导出表格 */
    exportTable() {
      const csv = this.correlationTableData.map(row =>
        `${row.variable1},${row.variable2},${row.correlation.toFixed(4)}`
      ).join('\n');

      const blob = new Blob([`变量1,变量2,相关系数\n${csv}`], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      this.downloadImage(url, 'correlation_data.csv');
    }
  }
};
</script>

<style scoped>
.correlation-analysis {
  padding: 0;
}

.config-card, .result-card {
  margin-bottom: 20px;
}

.config-item {
  margin-bottom: 16px;
}

.config-item label {
  display: block;
  margin-bottom: 4px;
  color: #606266;
  font-size: 14px;
}

.column-selection {
  margin-top: 16px;
}

.column-selection label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-size: 14px;
}

.column-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 8px;
}

.column-checkbox {
  margin: 0;
  min-width: 120px;
}

.selection-actions {
  display: flex;
  gap: 8px;
}

.result-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.loading-container {
  padding: 40px 0;
  text-align: center;
}

.chart-container {
  margin-bottom: 20px;
}

.chart {
  width: 100%;
  height: 500px;
}

.table-container {
  margin-bottom: 20px;
}

.statistics-info {
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.stat-item {
  text-align: center;
}

.stat-item .label {
  display: block;
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}

.stat-item .value {
  color: #303133;
  font-weight: 500;
  font-size: 16px;
}

.strong-correlation {
  color: #f56c6c;
  font-weight: bold;
}

.moderate-correlation {
  color: #e6a23c;
  font-weight: 500;
}

.weak-correlation {
  color: #909399;
}
</style>
