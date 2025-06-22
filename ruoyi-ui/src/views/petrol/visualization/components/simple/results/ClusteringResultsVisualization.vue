<template>
  <div class="clustering-results-visualization">
    <!-- 聚类性能指标卡片 -->
    <el-card class="metrics-card" shadow="never" style="margin-bottom: 20px;">
      <div slot="header">
        <span>📊 {{ getAlgorithmDisplayName() }} - 聚类性能指标</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="6" v-for="metric in clusteringMetrics" :key="metric.key">
          <div class="metric-item">
            <div class="metric-value">{{ metric.value }}</div>
            <div class="metric-label">{{ metric.label }}</div>
            <div class="metric-desc">{{ metric.description }}</div>
          </div>
        </el-col>
      </el-row>
      
      <!-- 如果没有指标，显示提示 -->
      <el-empty v-if="clusteringMetrics.length === 0" 
                description="暂无聚类性能指标数据" 
                :image-size="100">
      </el-empty>
    </el-card>

    <!-- 可视化图表 -->
    <el-row :gutter="20">
      <!-- 聚类结果散点图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🎯 聚类结果</span>
            <el-tooltip content="显示数据点的聚类分布" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="clusterScatterChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 聚类分布饼图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 聚类分布</span>
            <el-tooltip content="显示各聚类的样本数量分布" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="clusterDistributionChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 聚类中心图 (如果有) -->
      <el-col v-if="hasClusterCenters" :span="24" style="margin-bottom: 20px;">
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
    </el-row>

    <!-- 算法参数表 -->
    <el-card class="params-card" shadow="never" style="margin-bottom: 20px;">
      <div slot="header">
        <span>⚙️ 算法参数</span>
      </div>
      
      <el-table :data="algorithmParameters" border stripe style="width: 100%;">
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
      
      <!-- 如果没有参数，显示提示 -->
      <el-empty v-if="algorithmParameters.length === 0" 
                description="暂无算法参数数据" 
                :image-size="100">
      </el-empty>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "ClusteringResultsVisualization",
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
      clusterDistributionChart: null,
      clusterCentersChart: null
    };
  },
  computed: {
    clusteringMetrics() {
      const stats = this.results.statistics || this.results.metrics || {};
      const metrics = [];

      // 轮廓系数
      const silhouetteScore = stats.silhouette_score;
      if (silhouetteScore !== undefined) {
        metrics.push({
          key: 'silhouette_score',
          label: '轮廓系数',
          value: this.formatNumber(silhouetteScore),
          description: '聚类效果评估，范围[-1,1]'
        });
      }

      // 惯性
      const inertia = stats.inertia;
      if (inertia !== undefined) {
        metrics.push({
          key: 'inertia',
          label: '惯性',
          value: this.formatNumber(inertia),
          description: '样本到聚类中心距离平方和'
        });
      }

      // 聚类数量
      const nClusters = stats.n_clusters || this.getClusterCount();
      if (nClusters !== undefined) {
        metrics.push({
          key: 'n_clusters',
          label: '聚类数量',
          value: nClusters,
          description: '算法识别的聚类数量'
        });
      }

      // 样本数量
      const sampleCount = stats.sample_count || this.getSampleCount();
      if (sampleCount !== undefined) {
        metrics.push({
          key: 'sample_count',
          label: '样本数量',
          value: sampleCount,
          description: '参与聚类的样本总数'
        });
      }

      return metrics;
    },
    
    algorithmParameters() {
      const params = this.results.model_params || {};
      return Object.entries(params).map(([key, value]) => ({
        parameter: this.getParameterDisplayName(key),
        value: this.formatParameterValue(value),
        description: this.getParameterDescription(key)
      }));
    },

    hasClusterCenters() {
      const centers = this.results.cluster_centers || [];
      return Array.isArray(centers) && centers.length > 0;
    }
  },
  mounted() {
    console.log('🎨 聚类可视化组件挂载');
    console.log('📊 接收到的结果数据:', this.results);
    console.log('📋 接收到的任务信息:', this.taskInfo);

    this.$nextTick(() => {
      this.initializeCharts();
    });
  },
  beforeDestroy() {
    // 销毁图表实例
    [this.clusterScatterChart, this.clusterDistributionChart, this.clusterCentersChart].forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    initializeCharts() {
      console.log('🎨 开始初始化聚类可视化图表');
      
      setTimeout(() => {
        this.renderClusterScatterChart();
        this.renderClusterDistributionChart();

        if (this.hasClusterCenters) {
          this.renderClusterCentersChart();
        }

        console.log('✅ 聚类可视化图表初始化完成');
      }, 100);
    },
    
    getAlgorithmDisplayName() {
      const names = {
        'kmeans': 'K-Means聚类',
        'dbscan': 'DBSCAN聚类',
        'hierarchical_clustering': '层次聚类',
        'gaussian_mixture': '高斯混合模型',
        'spectral_clustering': '谱聚类'
      };
      return names[this.algorithmType] || this.algorithmType;
    },

    getClusterCount() {
      const labels = this.results.cluster_labels || [];
      if (labels.length === 0) return undefined;
      return new Set(labels).size;
    },

    getSampleCount() {
      const labels = this.results.cluster_labels || [];
      return labels.length;
    },
    
    formatNumber(value) {
      if (value === null || value === undefined || isNaN(value)) {
        return 'N/A';
      }
      return Number(value).toFixed(4);
    },
    
    getParameterDisplayName(key) {
      const names = {
        'n_clusters': '聚类数量',
        'eps': '邻域半径',
        'min_samples': '最小样本数',
        'max_iter': '最大迭代次数',
        'random_state': '随机种子',
        'init': '初始化方法'
      };
      return names[key] || key;
    },
    
    formatParameterValue(value) {
      if (typeof value === 'number') {
        return this.formatNumber(value);
      }
      return String(value);
    },
    
    getParameterDescription(key) {
      const descriptions = {
        'n_clusters': 'K-Means算法的聚类数量',
        'eps': 'DBSCAN算法的邻域半径',
        'min_samples': 'DBSCAN算法的最小样本数',
        'max_iter': '算法的最大迭代次数',
        'random_state': '随机数生成器的种子',
        'init': '聚类中心的初始化方法'
      };
      return descriptions[key] || '算法参数';
    },

    /** 渲染聚类结果散点图 */
    renderClusterScatterChart() {
      const chartDom = this.$refs.clusterScatterChart;
      if (!chartDom) return;

      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.clusterScatterChart) {
        this.clusterScatterChart.dispose();
      }

      this.clusterScatterChart = echarts.init(chartDom);

      // 获取聚类标签和特征数据
      const clusterLabels = this.results.cluster_labels || [];
      const featureValues = this.results.feature_values || this.results.X_test || [];

      if (clusterLabels.length === 0 || featureValues.length === 0) {
        this.clusterScatterChart.setOption({
          title: {
            text: '暂无聚类结果数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 准备散点数据，按聚类分组
      const clusterData = {};
      const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4'];
      
      clusterLabels.forEach((label, index) => {
        if (!clusterData[label]) {
          clusterData[label] = [];
        }
        // 使用前两个特征作为x,y坐标
        const x = Array.isArray(featureValues[index]) ? featureValues[index][0] : featureValues[index];
        const y = Array.isArray(featureValues[index]) ? featureValues[index][1] || featureValues[index][0] : index;
        clusterData[label].push([x, y]);
      });

      const series = Object.entries(clusterData).map(([label, data], index) => ({
        name: `聚类 ${label}`,
        type: 'scatter',
        data: data.slice(0, 200), // 限制显示数量
        itemStyle: { 
          color: colors[index % colors.length],
          opacity: 0.7 
        },
        symbolSize: 6
      }));

      const option = {
        title: {
          text: '聚类结果散点图',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            return `${params.seriesName}<br/>X: ${params.data[0].toFixed(3)}<br/>Y: ${params.data[1].toFixed(3)}`;
          }
        },
        legend: {
          data: Object.keys(clusterData).map(label => `聚类 ${label}`),
          top: 30
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '20%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          name: '特征1'
        },
        yAxis: {
          type: 'value',
          name: '特征2'
        },
        series: series
      };

      this.clusterScatterChart.setOption(option);
    },

    /** 渲染聚类分布饼图 */
    renderClusterDistributionChart() {
      const chartDom = this.$refs.clusterDistributionChart;
      if (!chartDom) return;

      chartDom.style.width = '100%';
      chartDom.style.height = '350px';

      if (this.clusterDistributionChart) {
        this.clusterDistributionChart.dispose();
      }

      this.clusterDistributionChart = echarts.init(chartDom);

      // 获取聚类标签
      const clusterLabels = this.results.cluster_labels || [];

      if (clusterLabels.length === 0) {
        this.clusterDistributionChart.setOption({
          title: {
            text: '暂无聚类分布数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      // 统计各聚类的数量
      const clusterCounts = {};
      clusterLabels.forEach(label => {
        clusterCounts[label] = (clusterCounts[label] || 0) + 1;
      });

      const data = Object.entries(clusterCounts).map(([label, count]) => ({
        name: `聚类 ${label}`,
        value: count
      }));

      const option = {
        title: {
          text: '聚类分布',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [
          {
            name: '聚类分布',
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
          }
        ]
      };

      this.clusterDistributionChart.setOption(option);
    }
  }
};
</script>

<style scoped>
.clustering-results-visualization {
  padding: 0;
}

.metrics-card .metric-item {
  text-align: center;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.metric-label {
  font-size: 14px;
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

.chart-card {
  margin-bottom: 20px;
}

.el-table {
  margin-top: 10px;
}
</style>
