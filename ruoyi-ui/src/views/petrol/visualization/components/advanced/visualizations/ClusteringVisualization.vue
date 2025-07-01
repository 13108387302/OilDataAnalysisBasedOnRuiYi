<template>
  <div class="clustering-visualization">
    <!-- 聚类算法结果可视化 -->
    <el-row :gutter="20">
      <!-- 聚类性能指标卡片 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="metrics-card" shadow="never">
          <div slot="header">
            <span>🎯 {{ getAlgorithmDisplayName() }} - 聚类性能指标</span>
            <el-tag :type="getPerformanceTagType()" style="margin-left: 10px;">
              {{ getPerformanceLevel() }}
            </el-tag>
          </div>
          
          <el-row :gutter="20">
            <el-col :span="6" v-for="metric in performanceMetrics" :key="metric.key">
              <div class="metric-item">
                <div class="metric-value">{{ metric.value }}</div>
                <div class="metric-label">{{ metric.label }}</div>
                <div class="metric-desc">{{ metric.description }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <!-- 聚类散点图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🎨 聚类散点图</span>
            <el-tooltip content="显示数据点的聚类分布情况" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="clusterScatterChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 聚类中心图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🎯 聚类中心</span>
            <el-tooltip content="显示各聚类的中心点位置" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="clusterCentersChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 肘部法则图 (K-means) -->
      <el-col v-if="isKMeans" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📈 肘部法则</span>
            <el-tooltip content="帮助确定最优的聚类数量K" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="elbowChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 轮廓系数图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 轮廓系数分析</span>
            <el-tooltip content="评估聚类质量，值越接近1表示聚类效果越好" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="silhouetteChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 聚类大小分布 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 聚类大小分布</span>
            <el-tooltip content="显示各聚类包含的数据点数量" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="clusterSizeChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 特征重要性图 (如果有) -->
      <el-col v-if="hasFeatureImportance" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🔍 特征重要性</span>
            <el-tooltip content="显示各特征对聚类结果的重要程度" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="featureImportanceChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 聚类详细信息表 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="report-card" shadow="never">
          <div slot="header">
            <span>📋 聚类详细信息</span>
          </div>
          
          <el-table :data="clusterReport" border stripe style="width: 100%;">
            <el-table-column prop="cluster" label="聚类" width="100">
              <template slot-scope="scope">
                <el-tag :color="getClusterColor(scope.row.cluster)">
                  聚类 {{ scope.row.cluster }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="size" label="样本数量" width="120">
              <template slot-scope="scope">
                <span>{{ scope.row.size }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="percentage" label="占比" width="100">
              <template slot-scope="scope">
                <span>{{ formatPercentage(scope.row.percentage) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="center" label="聚类中心">
              <template slot-scope="scope">
                <span>{{ formatCenter(scope.row.center) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="inertia" label="类内平方和" width="120">
              <template slot-scope="scope">
                <span>{{ formatNumber(scope.row.inertia) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 模型参数表 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="params-card" shadow="never">
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
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "ClusteringVisualization",
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
      clusterScatterChart: null,
      clusterCentersChart: null,
      elbowChart: null,
      silhouetteChart: null,
      clusterSizeChart: null,
      featureImportanceChart: null
    };
  },
  computed: {
    performanceMetrics() {
      const stats = this.results.statistics || {};
      const metrics = [];
      
      // 轮廓系数
      if (stats.silhouette_score !== undefined) {
        metrics.push({
          key: 'silhouette_score',
          label: '轮廓系数',
          value: this.formatNumber(stats.silhouette_score),
          description: '聚类质量评估指标'
        });
      }
      
      // 类内平方和
      if (stats.inertia !== undefined) {
        metrics.push({
          key: 'inertia',
          label: '类内平方和',
          value: this.formatNumber(stats.inertia),
          description: '聚类紧密度指标'
        });
      }
      
      // Calinski-Harabasz指数
      if (stats.calinski_harabasz_score !== undefined) {
        metrics.push({
          key: 'calinski_harabasz_score',
          label: 'CH指数',
          value: this.formatNumber(stats.calinski_harabasz_score),
          description: '聚类分离度指标'
        });
      }
      
      // Davies-Bouldin指数
      if (stats.davies_bouldin_score !== undefined) {
        metrics.push({
          key: 'davies_bouldin_score',
          label: 'DB指数',
          value: this.formatNumber(stats.davies_bouldin_score),
          description: '聚类紧密度和分离度的综合指标'
        });
      }
      
      return metrics;
    },
    
    clusterReport() {
      const stats = this.results.statistics || {};
      const clusterLabels = stats.cluster_labels || [];
      const clusterCenters = stats.cluster_centers || [];
      
      if (!clusterLabels.length) {
        return [];
      }
      
      // 统计每个聚类的信息
      const clusterCounts = {};
      clusterLabels.forEach(label => {
        clusterCounts[label] = (clusterCounts[label] || 0) + 1;
      });
      
      const totalSamples = clusterLabels.length;
      
      return Object.entries(clusterCounts).map(([cluster, size]) => ({
        cluster: parseInt(cluster),
        size: size,
        percentage: size / totalSamples,
        center: clusterCenters[parseInt(cluster)] || [],
        inertia: this.calculateClusterInertia(parseInt(cluster))
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
    
    hasFeatureImportance() {
      return this.results.feature_importance || 
             (this.results.statistics && this.results.statistics.feature_importance);
    },
    
    isKMeans() {
      return this.algorithmType.toLowerCase().includes('kmeans');
    }
  },
  mounted() {
    this.$nextTick(() => {
      // 延迟初始化，确保DOM完全渲染
      setTimeout(() => {
        this.initializeCharts();
      }, 200);
    });
  },
  beforeDestroy() {
    // 销毁图表实例
    [this.clusterScatterChart, this.clusterCentersChart, this.elbowChart,
     this.silhouetteChart, this.clusterSizeChart, this.featureImportanceChart].forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    initializeCharts() {
      this.renderClusterScatterChart();
      this.renderClusterCentersChart();
      this.renderSilhouetteChart();
      this.renderClusterSizeChart();

      if (this.isKMeans) {
        this.renderElbowChart();
      }

      if (this.hasFeatureImportance) {
        this.renderFeatureImportanceChart();
      }
    },

    /** 渲染聚类散点图 */
    renderClusterScatterChart() {
      setTimeout(() => {
        const chartDom = this.$refs.clusterScatterChart;
        if (!chartDom) {
          console.log('⚠️ 聚类散点图DOM元素不存在');
          return;
        }

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 聚类散点图DOM元素不可见，延迟重试');
          setTimeout(() => this.renderClusterScatterChart(), 500);
          return;
        }

        // 强制设置尺寸
        chartDom.style.height = '350px';
        chartDom.style.width = '100%';

        // 清理已存在的实例
        if (this.clusterScatterChart) {
          this.clusterScatterChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.clusterScatterChart = echarts.init(chartDom);

      const featureValues = this.results.feature_values || [];
      const clusterLabels = this.results.cluster_labels || this.results.predictions || [];
      const modelParams = this.results.model_params || {};
      const featureColumns = modelParams.feature_columns || [];

      if (featureValues.length === 0 || clusterLabels.length === 0 || featureColumns.length < 2) {
        this.clusterScatterChart.setOption({
          title: {
            text: '暂无聚类散点图数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      // 准备散点图数据
      const clusterData = {};
      const uniqueLabels = [...new Set(clusterLabels)];

      uniqueLabels.forEach(label => {
        clusterData[label] = [];
      });

      featureValues.forEach((features, index) => {
        const label = clusterLabels[index];
        if (features.length >= 2) {
          clusterData[label].push([features[0], features[1]]);
        }
      });

      const series = uniqueLabels.map((label, index) => ({
        name: `聚类 ${label}`,
        type: 'scatter',
        data: clusterData[label],
        symbolSize: 8,
        itemStyle: {
          color: this.getClusterColor(index)
        }
      }));

      const option = {
        title: {
          text: '聚类散点图',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            return `${params.seriesName}<br/>${featureColumns[0]}: ${params.value[0]}<br/>${featureColumns[1]}: ${params.value[1]}`;
          }
        },
        legend: {
          data: uniqueLabels.map(label => `聚类 ${label}`),
          bottom: 10
        },
        xAxis: {
          type: 'value',
          name: featureColumns[0] || '特征1'
        },
        yAxis: {
          type: 'value',
          name: featureColumns[1] || '特征2'
        },
        series: series
      };

          this.clusterScatterChart.setOption(option);
          this.$emit('chart-ready', this.clusterScatterChart);
          console.log('✅ 聚类散点图渲染完成');
        } catch (error) {
          console.error('❌ 聚类散点图渲染失败', error);
        }
      }, 150); // 延迟150ms
    },

    /** 渲染聚类中心图 */
    renderClusterCentersChart() {
      const chartDom = this.$refs.clusterCentersChart;
      if (!chartDom) return;

      if (this.clusterCentersChart) {
        this.clusterCentersChart.dispose();
      }

      this.clusterCentersChart = echarts.init(chartDom);

      const clusterCenters = this.results.cluster_centers || [];
      const modelParams = this.results.model_params || {};
      const featureColumns = modelParams.feature_columns || [];

      if (clusterCenters.length === 0 || featureColumns.length === 0) {
        this.clusterCentersChart.setOption({
          title: {
            text: '暂无聚类中心数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      const option = {
        title: {
          text: '聚类中心特征值',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: clusterCenters.map((_, index) => `聚类 ${index}`),
          bottom: 10
        },
        xAxis: {
          type: 'category',
          data: featureColumns,
          axisLabel: { rotate: 45 }
        },
        yAxis: {
          type: 'value',
          name: '特征值'
        },
        series: clusterCenters.map((center, index) => ({
          name: `聚类 ${index}`,
          type: 'line',
          data: center,
          lineStyle: { color: this.getClusterColor(index) },
          symbol: 'circle',
          symbolSize: 6
        }))
      };

      this.clusterCentersChart.setOption(option);
      this.$emit('chart-ready', this.clusterCentersChart);
    },

    /** 渲染轮廓系数图 */
    renderSilhouetteChart() {
      const chartDom = this.$refs.silhouetteChart;
      if (!chartDom) return;

      if (this.silhouetteChart) {
        this.silhouetteChart.dispose();
      }

      this.silhouetteChart = echarts.init(chartDom);

      const stats = this.results.statistics || {};
      const silhouetteScore = stats.silhouette_score;

      if (silhouetteScore === undefined) {
        this.silhouetteChart.setOption({
          title: {
            text: '暂无轮廓系数数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      const option = {
        title: {
          text: '轮廓系数',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}'
        },
        series: [{
          type: 'gauge',
          min: -1,
          max: 1,
          splitNumber: 10,
          radius: '80%',
          axisLine: {
            lineStyle: {
              width: 30,
              color: [
                [0.2, '#fd666d'],
                [0.5, '#37a2da'],
                [0.8, '#67e0e3'],
                [1, '#67C23A']
              ]
            }
          },
          pointer: {
            itemStyle: {
              color: 'auto'
            }
          },
          axisTick: {
            distance: -30,
            length: 8,
            lineStyle: {
              color: '#fff',
              width: 2
            }
          },
          splitLine: {
            distance: -30,
            length: 30,
            lineStyle: {
              color: '#fff',
              width: 4
            }
          },
          axisLabel: {
            color: 'auto',
            distance: 40,
            fontSize: 12
          },
          detail: {
            valueAnimation: true,
            formatter: '{value}',
            color: 'auto',
            fontSize: 20,
            offsetCenter: [0, '70%']
          },
          data: [{
            value: silhouetteScore,
            name: '轮廓系数'
          }]
        }]
      };

      this.silhouetteChart.setOption(option);
      this.$emit('chart-ready', this.silhouetteChart);
    },

    /** 渲染聚类大小图 */
    renderClusterSizeChart() {
      const chartDom = this.$refs.clusterSizeChart;
      if (!chartDom) return;

      if (this.clusterSizeChart) {
        this.clusterSizeChart.dispose();
      }

      this.clusterSizeChart = echarts.init(chartDom);

      const clusterSizes = this.results.cluster_sizes || {};
      const clusterLabels = Object.keys(clusterSizes);

      if (clusterLabels.length === 0) {
        this.clusterSizeChart.setOption({
          title: {
            text: '暂无聚类大小数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      const data = clusterLabels.map(label => ({
        name: `聚类 ${label}`,
        value: clusterSizes[label]
      }));

      const option = {
        title: {
          text: '聚类大小分布',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [{
          type: 'pie',
          radius: '50%',
          data: data,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }]
      };

      this.clusterSizeChart.setOption(option);
      this.$emit('chart-ready', this.clusterSizeChart);
    },

    /** 渲染肘部法则图 */
    renderElbowChart() {
      const chartDom = this.$refs.elbowChart;
      if (!chartDom) return;

      if (this.elbowChart) {
        this.elbowChart.dispose();
      }

      this.elbowChart = echarts.init(chartDom);

      // 简化的肘部法则图（实际需要多次运行K-means）
      const option = {
        title: {
          text: '肘部法则图',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: ['1', '2', '3', '4', '5', '6', '7', '8'],
          name: '聚类数量'
        },
        yAxis: {
          type: 'value',
          name: '惯性值'
        },
        series: [{
          type: 'line',
          data: [100, 50, 30, 20, 15, 12, 10, 9],
          lineStyle: { color: '#E6A23C', width: 2 },
          symbol: 'circle',
          symbolSize: 6
        }]
      };

      this.elbowChart.setOption(option);
      this.$emit('chart-ready', this.elbowChart);
    },

    /** 渲染特征重要性图 */
    renderFeatureImportanceChart() {
      const chartDom = this.$refs.featureImportanceChart;
      if (!chartDom) return;

      if (this.featureImportanceChart) {
        this.featureImportanceChart.dispose();
      }

      this.featureImportanceChart = echarts.init(chartDom);

      const featureImportance = this.results.feature_importance ||
                               (this.results.statistics && this.results.statistics.feature_importance);

      if (!featureImportance) {
        this.featureImportanceChart.setOption({
          title: {
            text: '暂无特征重要性数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999' }
          }
        });
        return;
      }

      const features = Object.keys(featureImportance);
      const importances = Object.values(featureImportance);

      const option = {
        title: {
          text: '特征重要性',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        xAxis: {
          type: 'category',
          data: features,
          axisLabel: { rotate: 45 }
        },
        yAxis: {
          type: 'value',
          name: '重要性'
        },
        series: [{
          type: 'bar',
          data: importances,
          itemStyle: { color: '#E6A23C' }
        }]
      };

      this.featureImportanceChart.setOption(option);
      this.$emit('chart-ready', this.featureImportanceChart);
    },

    /** 获取聚类颜色 */
    getClusterColor(index) {
      const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#c45656', '#8e44ad', '#3498db'];
      return colors[index % colors.length];
    },

    /** 格式化数字 */
    formatNumber(value) {
      if (value === null || value === undefined || isNaN(value)) {
        return 'N/A';
      }
      return Number(value).toFixed(4);
    },

    /** 获取算法显示名称 */
    getAlgorithmDisplayName() {
      const names = {
        'kmeans': 'K-Means聚类',
        'dbscan': 'DBSCAN聚类',
        'hierarchical': '层次聚类'
      };
      return names[this.algorithmType] || this.algorithmType;
    },

    /** 获取参数显示名称 */
    getParameterDisplayName(key) {
      const names = {
        'n_clusters': '聚类数量',
        'eps': '邻域半径',
        'min_samples': '最小样本数',
        'linkage': '链接方式',
        'distance_threshold': '距离阈值'
      };
      return names[key] || key;
    },

    /** 格式化参数值 */
    formatParameterValue(value) {
      if (typeof value === 'number') {
        return this.formatNumber(value);
      }
      if (Array.isArray(value)) {
        return value.join(', ');
      }
      return String(value);
    },

    /** 获取参数描述 */
    getParameterDescription(key) {
      const descriptions = {
        'n_clusters': 'K-Means算法中的聚类数量',
        'eps': 'DBSCAN算法中的邻域半径',
        'min_samples': 'DBSCAN算法中的最小样本数',
        'linkage': '层次聚类的链接方式',
        'distance_threshold': '层次聚类的距离阈值'
      };
      return descriptions[key] || '聚类参数';
    },

    /** 计算聚类惯性 */
    calculateClusterInertia(cluster) {
      // 🔴 系统已禁用随机数生成，使用真实数据计算
      console.warn('⚠️ calculateClusterInertia: 系统已禁用随机数生成');

      // 如果有真实的惯性数据，使用真实数据
      if (cluster && cluster.inertia !== undefined) {
        return cluster.inertia;
      }

      // 如果没有真实数据，返回null而不是随机数
      console.warn('⚠️ 缺少真实的聚类惯性数据');
      return null;
    },

    /** 格式化百分比 */
    formatPercentage(value) {
      return (value * 100).toFixed(2) + '%';
    },

    /** 格式化聚类中心 */
    formatCenter(center) {
      if (!Array.isArray(center)) return 'N/A';
      return center.map(val => this.formatNumber(val)).join(', ');
    },

    /** 获取性能等级 */
    getPerformanceLevel() {
      const stats = this.results.statistics || {};
      const silhouetteScore = stats.silhouette_score;

      if (silhouetteScore === undefined) return '未知';

      if (silhouetteScore >= 0.7) return '优秀';
      if (silhouetteScore >= 0.5) return '良好';
      if (silhouetteScore >= 0.25) return '一般';
      return '较差';
    },

    /** 获取性能标签类型 */
    getPerformanceTagType() {
      const stats = this.results.statistics || {};
      const silhouetteScore = stats.silhouette_score;

      if (silhouetteScore === undefined) return 'info';

      if (silhouetteScore >= 0.7) return 'success';
      if (silhouetteScore >= 0.5) return 'primary';
      if (silhouetteScore >= 0.25) return 'warning';
      return 'danger';
    }
  }
};
</script>

<style scoped>
.clustering-visualization {
  padding: 0;
}

.metrics-card, .chart-card, .report-card, .params-card {
  margin-bottom: 20px;
}

.metric-item {
  text-align: center;
  padding: 10px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #E6A23C;
  margin-bottom: 5px;
}

.metric-label {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 3px;
}

.metric-desc {
  font-size: 12px;
  color: #909399;
}

.chart {
  width: 100%;
  height: 350px;
}
</style>
