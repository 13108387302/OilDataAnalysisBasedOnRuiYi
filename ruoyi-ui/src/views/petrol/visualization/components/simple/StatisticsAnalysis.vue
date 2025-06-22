<template>
  <div class="statistics-analysis">
    <!-- 列选择 -->
    <el-card class="selection-card" shadow="never">
      <div slot="header">
        <span>🎯 选择分析列</span>
        <el-button 
          type="text" 
          @click="selectAllColumns"
          :disabled="loading">
          {{ selectedColumns.length === numericColumns.length ? '取消全选' : '全选数值列' }}
        </el-button>
      </div>
      
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="2" animated />
      </div>
      
      <div v-else-if="numericColumns.length === 0" class="empty-container">
        <el-empty description="暂无数值列可分析" :image-size="100" />
      </div>
      
      <div v-else class="column-selection">
        <el-checkbox-group v-model="selectedColumns" @change="loadStatistics">
          <el-checkbox 
            v-for="column in numericColumns" 
            :key="column" 
            :label="column"
            class="column-checkbox">
            {{ column }}
          </el-checkbox>
        </el-checkbox-group>
      </div>
    </el-card>

    <!-- 统计结果 -->
    <el-card v-if="selectedColumns.length > 0" class="statistics-card" shadow="never">
      <div slot="header">
        <span>📊 统计分析结果</span>
        <el-button
          type="text"
          icon="el-icon-refresh"
          @click="loadStatistics"
          :loading="statisticsLoading">
          刷新数据
        </el-button>
      </div>
      
      <div v-if="statisticsLoading" class="loading-container">
        <el-skeleton :rows="4" animated />
      </div>
      
      <div v-else-if="Object.keys(statistics).length === 0" class="empty-container">
        <el-empty description="暂无统计数据" :image-size="100" />
      </div>
      
      <div v-else class="statistics-content">
        <!-- 统计表格 -->
        <el-table 
          :data="statisticsTableData" 
          border 
          stripe 
          size="small"
          style="width: 100%; margin-bottom: 20px;">
          <el-table-column prop="column" label="列名" width="150" fixed="left">
            <template slot-scope="scope">
              <el-tag type="success" size="mini">{{ scope.row.column }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="count" label="计数" width="80" align="center">
            <template slot-scope="scope">
              <span class="number-value">{{ formatNumber(scope.row.count) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="mean" label="均值" width="100" align="center">
            <template slot-scope="scope">
              <span class="number-value">{{ formatNumber(scope.row.mean) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="std" label="标准差" width="100" align="center">
            <template slot-scope="scope">
              <span class="number-value">{{ formatNumber(scope.row.std) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="min" label="最小值" width="100" align="center">
            <template slot-scope="scope">
              <span class="number-value">{{ formatNumber(scope.row.min) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="q25" label="25%分位" width="100" align="center">
            <template slot-scope="scope">
              <span class="number-value">{{ formatNumber(scope.row.q25) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="median" label="中位数" width="100" align="center">
            <template slot-scope="scope">
              <span class="number-value">{{ formatNumber(scope.row.median) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="q75" label="75%分位" width="100" align="center">
            <template slot-scope="scope">
              <span class="number-value">{{ formatNumber(scope.row.q75) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="max" label="最大值" width="100" align="center">
            <template slot-scope="scope">
              <span class="number-value">{{ formatNumber(scope.row.max) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="skewness" label="偏度" width="100" align="center">
            <template slot-scope="scope">
              <span class="number-value">{{ formatNumber(scope.row.skewness) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="kurtosis" label="峰度" width="100" align="center">
            <template slot-scope="scope">
              <span class="number-value">{{ formatNumber(scope.row.kurtosis) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 统计图表 -->
        <div class="charts-container">
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="chart-container">
                <h4>箱线图分析</h4>
                <div ref="boxplotChart" class="chart"></div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="chart-container">
                <h4>数据趋势分析</h4>
                <div ref="trendChart" class="chart"></div>
              </div>
            </el-col>
          </el-row>

          <!-- 散点图矩阵 -->
          <el-row :gutter="20" style="margin-top: 20px;" v-if="selectedColumns.length >= 2">
            <el-col :span="24">
              <div class="chart-container">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                  <h4>散点图矩阵</h4>
                  <el-select
                    v-model="scatterNormalization"
                    size="small"
                    style="width: 150px;">
                    <el-option label="无标准化" value="none"></el-option>
                    <el-option label="Z-Score标准化" value="zscore"></el-option>
                    <el-option label="Min-Max标准化" value="minmax"></el-option>
                  </el-select>
                </div>
                <div ref="scatterMatrixChart" class="chart" style="height: 600px;"></div>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-card>

    <!-- 空状态 -->
    <el-empty 
      v-if="selectedColumns.length === 0 && !loading"
      description="请选择要分析的数值列"
      :image-size="200">
    </el-empty>
  </div>
</template>

<script>
import { getDataSourceColumns, getDataSourceStatistics, readDataSourceData } from "@/api/petrol/visualization";
import * as echarts from 'echarts';

export default {
  name: "StatisticsAnalysis",
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
      statisticsLoading: false,
      numericColumns: [],
      selectedColumns: [],
      statistics: {},
      boxplotChart: null,
      trendChart: null,
      scatterMatrixChart: null,
      rawData: [],
      scatterNormalization: 'zscore' // 散点图标准化方法
    };
  },
  computed: {
    statisticsTableData() {
      return this.selectedColumns.map(column => {
        const stats = this.statistics[column] || {};
        // 如果没有从后端获取到统计数据，则从原始数据计算
        const computedStats = this.rawData.length > 0 ? this.computeStatistics(column) : {};
        const finalStats = Object.keys(stats).length > 0 ? stats : computedStats;

        return {
          column,
          count: finalStats.count || 0,
          mean: finalStats.mean || 0,
          std: finalStats.std || 0,
          min: finalStats.min || 0,
          q25: finalStats['25%'] || finalStats.q25 || 0,
          median: finalStats['50%'] || finalStats.median || 0,
          q75: finalStats['75%'] || finalStats.q75 || 0,
          max: finalStats.max || 0,
          skewness: finalStats.skewness || 0,
          kurtosis: finalStats.kurtosis || 0
        };
      });
    }
  },
  created() {
    this.loadColumns();
  },
  beforeDestroy() {
    if (this.boxplotChart) {
      this.boxplotChart.dispose();
    }
    if (this.trendChart) {
      this.trendChart.dispose();
    }
    if (this.scatterMatrixChart) {
      this.scatterMatrixChart.dispose();
    }
  },
  watch: {
    // 监听选择列变化
    selectedColumns: {
      handler() {
        if (this.selectedColumns.length > 0) {
          console.log('🔄 选择列变化，自动刷新统计分析');
          this.loadStatistics();
        }
      },
      deep: true
    },
    // 监听散点图标准化方法变化
    scatterNormalization() {
      if (this.rawData.length > 0 && this.selectedColumns.length >= 2) {
        console.log('🔄 散点图标准化方法变化，重新渲染散点图矩阵');
        this.$nextTick(() => {
          this.renderScatterMatrixChart();
        });
      }
    }
  },
  methods: {
    /** 加载列信息 */
    async loadColumns() {
      this.loading = true;
      try {
        const response = await getDataSourceColumns(this.sourceId, this.sourceType);
        const data = response.data || {};
        this.numericColumns = data.numericColumns || [];
        
        // 默认选择前5个数值列
        if (this.numericColumns.length > 0) {
          this.selectedColumns = this.numericColumns.slice(0, Math.min(5, this.numericColumns.length));
          this.$nextTick(() => {
            this.loadStatistics();
          });
        }
      } catch (error) {
        this.$message.error("加载列信息失败: " + error.message);
      } finally {
        this.loading = false;
      }
    },

    /** 加载统计信息 */
    async loadStatistics() {
      if (this.selectedColumns.length === 0) {
        this.statistics = {};
        this.rawData = [];
        return;
      }

      this.statisticsLoading = true;
      try {
        console.log('🔍 开始加载统计信息', {
          sourceId: this.sourceId,
          sourceType: this.sourceType,
          selectedColumns: this.selectedColumns
        });

        // 加载统计信息
        const statsParams = {
          columns: this.selectedColumns
        };
        const statsResponse = await getDataSourceStatistics(this.sourceId, this.sourceType, statsParams);
        this.statistics = statsResponse.data || {};

        console.log('📊 统计信息响应', {
          response: statsResponse,
          statistics: this.statistics
        });

        // 加载原始数据用于图表绘制
        const dataParams = {
          columns: this.selectedColumns,
          maxRows: 500 // 限制数据量以提高性能
        };
        const dataResponse = await readDataSourceData(this.sourceId, this.sourceType, dataParams);
        this.rawData = dataResponse.data || [];

        console.log('📋 原始数据响应', {
          response: dataResponse,
          dataLength: this.rawData.length,
          sampleData: this.rawData.slice(0, 3)
        });

        // 等待DOM渲染完成后渲染图表
        this.$nextTick(() => {
          this.waitForDOMAndRender();
        });
      } catch (error) {
        console.error('❌ 加载统计信息失败', error);
        this.$message.error("加载统计信息失败: " + error.message);
        this.statistics = {};
        this.rawData = [];
      } finally {
        this.statisticsLoading = false;
      }
    },

    /** 全选/取消全选 */
    selectAllColumns() {
      if (this.selectedColumns.length === this.numericColumns.length) {
        this.selectedColumns = [];
      } else {
        this.selectedColumns = [...this.numericColumns];
      }
      this.loadStatistics();
    },



    /** 渲染图表 */
    renderCharts() {
      console.log('🎨 renderCharts 开始', {
        selectedColumns: this.selectedColumns,
        rawDataLength: this.rawData.length,
        statisticsKeys: Object.keys(this.statistics)
      });

      try {
        this.renderBoxplotChart();
        this.renderTrendChart();
        // 渲染散点图矩阵
        if (this.selectedColumns.length >= 2) {
          this.renderScatterMatrixChart();
        }
        console.log('✅ 所有图表渲染完成');
      } catch (error) {
        console.error('❌ 图表渲染失败', error);
      }
    },



    /** 渲染箱线图 */
    renderBoxplotChart() {
      setTimeout(() => {
        const chartDom = this.$refs.boxplotChart;
        if (!chartDom) return;

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 箱线图DOM元素不可见，延迟重试');
          setTimeout(() => this.renderBoxplotChart(), 500);
          return;
        }

        chartDom.style.height = '300px';
        chartDom.style.width = '100%';

        if (this.boxplotChart) {
          this.boxplotChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.boxplotChart = echarts.init(chartDom);

          if (this.rawData.length === 0 || this.selectedColumns.length === 0) {
            const emptyOption = {
              title: {
                text: '暂无数据',
                left: 'center',
                top: 'center',
                textStyle: { fontSize: 16, color: '#999' }
              }
            };
            this.boxplotChart.setOption(emptyOption);
            return;
          }

          // 计算每列的箱线图数据
          const boxplotData = this.selectedColumns.map(column => {
            const values = this.rawData.map(row => parseFloat(row[column])).filter(v => !isNaN(v));
            if (values.length === 0) return [0, 0, 0, 0, 0];

            const sorted = [...values].sort((a, b) => a - b);
            const q1 = this.percentile(sorted, 0.25);
            const median = this.percentile(sorted, 0.5);
            const q3 = this.percentile(sorted, 0.75);
            const iqr = q3 - q1;

            // 计算异常值边界
            const lowerBound = q1 - 1.5 * iqr;
            const upperBound = q3 + 1.5 * iqr;

            // 找到在边界内的最小值和最大值
            const min = sorted.find(v => v >= lowerBound) || sorted[0];
            const max = [...sorted].reverse().find(v => v <= upperBound) || sorted[sorted.length - 1];

            return [min, q1, median, q3, max];
          });

          const option = {
            title: {
              text: '箱线图分析',
              left: 'center',
              textStyle: { fontSize: 14 }
            },
            tooltip: {
              trigger: 'item',
              formatter: function(params) {
                if (params.seriesType === 'boxplot') {
                  const data = params.data;
                  return `${params.name}<br/>
                          最小值: ${data[0].toFixed(3)}<br/>
                          Q1: ${data[1].toFixed(3)}<br/>
                          中位数: ${data[2].toFixed(3)}<br/>
                          Q3: ${data[3].toFixed(3)}<br/>
                          最大值: ${data[4].toFixed(3)}`;
                }
                return '';
              }
            },
            grid: {
              left: '10%',
              right: '10%',
              bottom: '15%',
              top: '15%'
            },
            xAxis: {
              type: 'category',
              data: this.selectedColumns,
              axisLabel: {
                rotate: 45
              }
            },
            yAxis: {
              type: 'value',
              name: '数值'
            },
            series: [{
              name: 'boxplot',
              type: 'boxplot',
              data: boxplotData,
              itemStyle: {
                color: '#91cc75'
              }
            }]
          };

          this.boxplotChart.setOption(option);
          console.log('✅ 箱线图渲染完成');

        } catch (error) {
          console.error('❌ 箱线图渲染失败', error);
        }
      }, 150); // 延迟150ms
    },

    /** 渲染趋势图 */
    renderTrendChart() {
      setTimeout(() => {
        const chartDom = this.$refs.trendChart;
        if (!chartDom) return;

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 趋势图DOM元素不可见，延迟重试');
          setTimeout(() => this.renderTrendChart(), 500);
          return;
        }

        chartDom.style.height = '300px';
        chartDom.style.width = '100%';

        if (this.trendChart) {
          this.trendChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.trendChart = echarts.init(chartDom);

          if (this.rawData.length === 0 || this.selectedColumns.length === 0) {
            const emptyOption = {
              title: {
                text: '暂无数据',
                left: 'center',
                top: 'center',
                textStyle: { fontSize: 16, color: '#999' }
              }
            };
            this.trendChart.setOption(emptyOption);
            return;
          }

          // 使用数据索引作为X轴
          const xAxisData = this.rawData.map((_, index) => index + 1);

          const series = this.selectedColumns.slice(0, 3).map((column, index) => {
            const colors = ['#5470c6', '#91cc75', '#fac858'];
            return {
              name: column,
              type: 'line',
              data: this.rawData.map(row => parseFloat(row[column]) || 0),
              smooth: true,
              symbol: 'none',
              lineStyle: {
                width: 2,
                color: colors[index % colors.length]
              }
            };
          });

          const option = {
            title: {
              text: '数据趋势分析',
              left: 'center',
              textStyle: { fontSize: 14 }
            },
            tooltip: {
              trigger: 'axis',
              axisPointer: { type: 'cross' }
            },
            legend: {
              data: this.selectedColumns.slice(0, 3),
              bottom: 0
            },
            grid: {
              left: '10%',
              right: '10%',
              bottom: '15%',
              top: '15%'
            },
            xAxis: {
              type: 'category',
              data: xAxisData,
              name: '数据点',
              nameLocation: 'middle',
              nameGap: 30
            },
            yAxis: {
              type: 'value',
              name: '数值'
            },
            series: series
          };

          this.trendChart.setOption(option);
          console.log('✅ 趋势图渲染完成');

        } catch (error) {
          console.error('❌ 趋势图渲染失败', error);
        }
      }, 200); // 延迟200ms
    },



    /** 渲染散点图矩阵 */
    renderScatterMatrixChart() {
      setTimeout(() => {
        const chartDom = this.$refs.scatterMatrixChart;
        if (!chartDom || this.rawData.length === 0 || this.selectedColumns.length < 2) {
          console.log('⚠️ 散点图矩阵DOM元素不存在或数据不足');
          return;
        }

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 散点图矩阵DOM元素不可见，延迟重试');
          setTimeout(() => this.renderScatterMatrixChart(), 500);
          return;
        }

        // 设置固定高度确保可见
        chartDom.style.height = '600px';
        chartDom.style.width = '100%';

        if (this.scatterMatrixChart) {
          this.scatterMatrixChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        this.scatterMatrixChart = echarts.init(chartDom);

      // 计算标准化数据
      const normalizedData = {};
      this.selectedColumns.forEach(column => {
        normalizedData[column] = this.normalizeScatterData(this.rawData, column);
      });

      const n = this.selectedColumns.length;
      const gridSize = 80 / n; // 网格大小百分比
      const grids = [];
      const xAxes = [];
      const yAxes = [];
      const series = [];

      // 获取标准化方法标签
      const normalizationLabels = {
        'none': '无标准化',
        'zscore': 'Z-Score标准化',
        'minmax': 'Min-Max标准化'
      };

      // 创建网格和坐标轴
      for (let i = 0; i < n; i++) {
        for (let j = 0; j < n; j++) {
          const gridIndex = i * n + j;

          grids.push({
            left: `${10 + j * gridSize}%`,
            top: `${10 + i * gridSize}%`,
            width: `${gridSize - 2}%`,
            height: `${gridSize - 2}%`
          });

          xAxes.push({
            type: 'value',
            gridIndex: gridIndex,
            show: i === n - 1, // 只在最后一行显示x轴
            name: i === n - 1 ? this.selectedColumns[j] : '',
            nameLocation: 'middle',
            nameGap: 20,
            nameTextStyle: { fontSize: 10 }
          });

          yAxes.push({
            type: 'value',
            gridIndex: gridIndex,
            show: j === 0, // 只在第一列显示y轴
            name: j === 0 ? this.selectedColumns[i] : '',
            nameLocation: 'middle',
            nameGap: 30,
            nameTextStyle: { fontSize: 10 }
          });

          if (i === j) {
            // 对角线显示直方图（使用标准化数据）
            const values = normalizedData[this.selectedColumns[i]].filter(v => !isNaN(v));
            const hist = this.calculateHistogram(values, 15);

            series.push({
              type: 'bar',
              xAxisIndex: gridIndex,
              yAxisIndex: gridIndex,
              data: hist.counts,
              barWidth: '80%',
              itemStyle: {
                color: '#5470c6'
              }
            });

            // 更新x轴为分类轴
            xAxes[gridIndex].type = 'category';
            xAxes[gridIndex].data = hist.labels;
          } else {
            // 非对角线显示散点图（使用标准化数据）
            const xNormalizedData = normalizedData[this.selectedColumns[j]];
            const yNormalizedData = normalizedData[this.selectedColumns[i]];
            const scatterData = xNormalizedData.map((x, idx) => [x, yNormalizedData[idx]]).filter(([x, y]) => !isNaN(x) && !isNaN(y));

            series.push({
              type: 'scatter',
              xAxisIndex: gridIndex,
              yAxisIndex: gridIndex,
              data: scatterData,
              symbolSize: 3,
              itemStyle: {
                color: '#91cc75',
                opacity: 0.7
              }
            });
          }
        }
      }

      const option = {
        title: {
          text: `散点图矩阵 - ${normalizationLabels[this.scatterNormalization]}`,
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item',
          formatter: (params) => {
            if (params.seriesType === 'scatter') {
              const gridIndex = params.seriesIndex;
              const i = Math.floor(gridIndex / n);
              const j = gridIndex % n;
              if (i !== j) {
                const dataIndex = params.dataIndex;
                const xOriginal = parseFloat(this.rawData[dataIndex][this.selectedColumns[j]]);
                const yOriginal = parseFloat(this.rawData[dataIndex][this.selectedColumns[i]]);
                return `${this.selectedColumns[j]}: ${xOriginal.toFixed(3)} (标准化: ${params.value[0].toFixed(3)})<br/>
                        ${this.selectedColumns[i]}: ${yOriginal.toFixed(3)} (标准化: ${params.value[1].toFixed(3)})`;
              }
            }
            return '';
          }
        },
        grid: grids,
        xAxis: xAxes,
        yAxis: yAxes,
        series: series
      };

      this.scatterMatrixChart.setOption(option);
      console.log('✅ 散点图矩阵渲染完成');
      }, 350); // 延迟350ms确保DOM渲染完成
    },

    /** 计算统计量 */
    computeStatistics(column) {
      const values = this.rawData
        .map(row => parseFloat(row[column]))
        .filter(v => !isNaN(v) && v !== null && v !== undefined);

      if (values.length === 0) return {};

      // 排序
      const sorted = [...values].sort((a, b) => a - b);
      const n = values.length;

      // 基本统计量
      const sum = values.reduce((a, b) => a + b, 0);
      const mean = sum / n;
      const variance = values.reduce((acc, val) => acc + Math.pow(val - mean, 2), 0) / n;
      const std = Math.sqrt(variance);

      // 分位数
      const q25 = this.percentile(sorted, 0.25);
      const median = this.percentile(sorted, 0.5);
      const q75 = this.percentile(sorted, 0.75);

      // 偏度和峰度
      const skewness = this.calculateSkewness(values, mean, std);
      const kurtosis = this.calculateKurtosis(values, mean, std);

      return {
        count: n,
        mean: mean,
        std: std,
        min: sorted[0],
        q25: q25,
        median: median,
        q75: q75,
        max: sorted[n - 1],
        skewness: skewness,
        kurtosis: kurtosis
      };
    },

    /** 计算分位数 */
    percentile(sortedArray, p) {
      const index = p * (sortedArray.length - 1);
      const lower = Math.floor(index);
      const upper = Math.ceil(index);
      const weight = index % 1;

      if (upper >= sortedArray.length) return sortedArray[sortedArray.length - 1];
      return sortedArray[lower] * (1 - weight) + sortedArray[upper] * weight;
    },

    /** 计算偏度 */
    calculateSkewness(values, mean, std) {
      if (std === 0) return 0;
      const n = values.length;
      const sum = values.reduce((acc, val) => acc + Math.pow((val - mean) / std, 3), 0);
      return (n / ((n - 1) * (n - 2))) * sum;
    },

    /** 计算峰度 */
    calculateKurtosis(values, mean, std) {
      if (std === 0) return 0;
      const n = values.length;
      const sum = values.reduce((acc, val) => acc + Math.pow((val - mean) / std, 4), 0);
      return ((n * (n + 1)) / ((n - 1) * (n - 2) * (n - 3))) * sum - (3 * (n - 1) * (n - 1)) / ((n - 2) * (n - 3));
    },

    /** 计算相关性矩阵 */
    calculateCorrelationMatrix() {
      const n = this.selectedColumns.length;
      const matrix = Array(n).fill().map(() => Array(n).fill(0));

      for (let i = 0; i < n; i++) {
        for (let j = 0; j < n; j++) {
          if (i === j) {
            matrix[i][j] = 1;
          } else {
            const xValues = this.rawData.map(row => parseFloat(row[this.selectedColumns[i]])).filter(v => !isNaN(v));
            const yValues = this.rawData.map(row => parseFloat(row[this.selectedColumns[j]])).filter(v => !isNaN(v));
            matrix[i][j] = this.calculateCorrelation(xValues, yValues);
          }
        }
      }

      return matrix;
    },

    /** 计算两个变量的相关系数 */
    calculateCorrelation(x, y) {
      if (x.length !== y.length || x.length === 0) return 0;

      const n = x.length;
      const sumX = x.reduce((a, b) => a + b, 0);
      const sumY = y.reduce((a, b) => a + b, 0);
      const sumXY = x.reduce((acc, xi, i) => acc + xi * y[i], 0);
      const sumX2 = x.reduce((acc, xi) => acc + xi * xi, 0);
      const sumY2 = y.reduce((acc, yi) => acc + yi * yi, 0);

      const numerator = n * sumXY - sumX * sumY;
      const denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));

      return denominator === 0 ? 0 : numerator / denominator;
    },

    /** 数据标准化处理 */
    normalizeScatterData(data, column) {
      const values = data.map(row => parseFloat(row[column])).filter(v => !isNaN(v));
      if (values.length === 0) return data.map(() => 0);

      let normalizedValues;

      switch (this.scatterNormalization) {
        case 'zscore':
          // Z-Score标准化: (x - mean) / std
          const mean = values.reduce((a, b) => a + b, 0) / values.length;
          const variance = values.reduce((acc, val) => acc + Math.pow(val - mean, 2), 0) / values.length;
          const std = Math.sqrt(variance);
          if (std === 0) {
            normalizedValues = values.map(() => 0);
          } else {
            normalizedValues = values.map(v => (v - mean) / std);
          }
          break;

        case 'minmax':
          // Min-Max标准化: (x - min) / (max - min)
          const min = Math.min(...values);
          const max = Math.max(...values);
          if (max === min) {
            normalizedValues = values.map(() => 0);
          } else {
            normalizedValues = values.map(v => (v - min) / (max - min));
          }
          break;

        case 'none':
        default:
          // 不进行标准化
          normalizedValues = values;
          break;
      }

      // 将标准化后的值映射回原始数据结构
      let valueIndex = 0;
      return data.map(row => {
        const originalValue = parseFloat(row[column]);
        if (isNaN(originalValue)) {
          return 0;
        } else {
          return normalizedValues[valueIndex++];
        }
      });
    },

    /** 计算直方图 */
    calculateHistogram(values, binCount) {
      if (values.length === 0) return { counts: [], labels: [] };

      const min = Math.min(...values);
      const max = Math.max(...values);
      const binWidth = (max - min) / binCount;
      const counts = Array(binCount).fill(0);
      const labels = [];

      // 计算每个区间的频次
      values.forEach(value => {
        const binIndex = Math.min(Math.floor((value - min) / binWidth), binCount - 1);
        counts[binIndex]++;
      });

      // 生成区间标签
      for (let i = 0; i < binCount; i++) {
        const start = min + i * binWidth;
        const end = min + (i + 1) * binWidth;
        labels.push(`${start.toFixed(1)}-${end.toFixed(1)}`);
      }

      return { counts, labels };
    },

    /** 等待DOM渲染完成后渲染图表 */
    waitForDOMAndRender() {
      const maxRetries = 10;
      let retryCount = 0;

      const checkAndRender = () => {
        retryCount++;

        // 检查图表容器是否存在且可见
        const boxplotContainer = this.$refs.boxplotChart;
        const trendContainer = this.$refs.trendChart;
        const scatterContainer = this.$refs.scatterMatrixChart;

        let allVisible = true;

        // 检查箱线图容器
        if (boxplotContainer) {
          const rect = boxplotContainer.getBoundingClientRect();
          if (rect.width === 0 || rect.height === 0) {
            allVisible = false;
          }
        }

        // 检查趋势图容器
        if (trendContainer) {
          const rect = trendContainer.getBoundingClientRect();
          if (rect.width === 0 || rect.height === 0) {
            allVisible = false;
          }
        }

        if (allVisible) {
          console.log('📊 统计分析DOM已准备就绪，开始渲染图表');
          this.renderCharts();
          this.$message.success('图表重新渲染完成');
          return;
        }

        if (retryCount < maxRetries) {
          console.log(`⏳ 等待统计分析DOM渲染... (${retryCount}/${maxRetries})`);
          setTimeout(checkAndRender, 300);
        } else {
          console.log('⚠️ 统计分析DOM等待超时，强制渲染图表');
          this.renderCharts();
          this.$message.success('图表渲染完成');
        }
      };

      checkAndRender();
    },

    /** 格式化数字 */
    formatNumber(value) {
      if (value === null || value === undefined || isNaN(value)) {
        return '-';
      }
      return Number(value).toFixed(4);
    }
  }
};
</script>

<style scoped>
.statistics-analysis {
  padding: 0;
}

.selection-card, .statistics-card {
  margin-bottom: 20px;
}

.loading-container, .empty-container {
  padding: 40px 0;
  text-align: center;
}

.column-selection {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.column-checkbox {
  margin: 0;
  min-width: 120px;
}

.statistics-content {
  padding: 0;
}

.number-value {
  color: #67c23a;
  font-family: 'Courier New', monospace;
  font-weight: 500;
}

.charts-container {
  margin-top: 20px;
}

.chart-container {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 16px;
}

.chart-container h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 14px;
  text-align: center;
}

.chart {
  width: 100%;
  height: 300px;
}

.chart-large {
  width: 100%;
  height: 500px;
}
</style>
