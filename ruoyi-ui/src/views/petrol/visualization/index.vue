<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <el-page-header content="数据可视化分析平台">
      <template slot="content">
        <span class="text-large font-600 mr-3">数据可视化分析平台</span>
      </template>
    </el-page-header>



    <!-- 数据集选择区域 -->
    <el-card shadow="never" style="margin-top: 20px;">
      <div slot="header" class="clearfix">
        <span>数据集选择</span>
        <div style="float: right;">
          <el-button style="padding: 3px 0" type="text" @click="refreshDatasets">
            <i class="el-icon-refresh"></i> 刷新
          </el-button>
        </div>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="8">
          <el-select 
            v-model="selectedDatasetId" 
            placeholder="请选择数据集" 
            style="width: 100%;"
            @change="handleDatasetChange"
            :loading="datasetsLoading">
            <el-option
              v-for="dataset in datasets"
              :key="dataset.id"
              :label="dataset.fileName"
              :value="dataset.id">
              <span style="float: left">{{ dataset.fileName }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">
                {{ dataset.rowCount }}行 × {{ dataset.columnCount }}列
              </span>
            </el-option>
          </el-select>
        </el-col>
        <el-col :span="16" v-if="selectedDataset">
          <el-descriptions :column="4" size="small">
            <el-descriptions-item label="文件名">{{ selectedDataset.fileName }}</el-descriptions-item>
            <el-descriptions-item label="行数">{{ selectedDataset.rowCount }}</el-descriptions-item>
            <el-descriptions-item label="列数">{{ selectedDataset.columnCount }}</el-descriptions-item>
            <el-descriptions-item label="上传时间">{{ selectedDataset.createTime }}</el-descriptions-item>
          </el-descriptions>
        </el-col>
      </el-row>
    </el-card>

    <!-- 可视化功能区域 -->
    <el-row :gutter="20" style="margin-top: 20px;" v-if="selectedDataset">
      <!-- 左侧功能面板 -->
      <el-col :span="6">
        <el-card shadow="always">
          <div slot="header" class="clearfix">
            <span>可视化功能</span>
          </div>
          
          <!-- 功能选择 -->
          <div style="margin-bottom: 15px;">
            <el-radio-group v-model="activeFunction" @change="handleFunctionChange">
              <el-radio-button label="overview">数据概览</el-radio-button>
              <el-radio-button label="curves">测井曲线</el-radio-button>
              <el-radio-button label="statistics">统计分析</el-radio-button>
              <el-radio-button label="correlation">相关性分析</el-radio-button>
            </el-radio-group>
          </div>

          <!-- 功能说明 -->
          <el-alert
            :title="functionTitles[activeFunction]"
            type="info"
            :closable="false"
            show-icon>
            <template slot="description">
              {{ getFunctionDescription(activeFunction) }}
            </template>
          </el-alert>

          <!-- 刷新按钮 -->
          <el-button
            type="primary"
            @click="refreshVisualization"
            :loading="loading"
            style="width: 100%; margin-top: 15px;"
            :disabled="!selectedDatasetId">
            <i class="el-icon-refresh"></i> 刷新可视化
          </el-button>
        </el-card>
      </el-col>

      <!-- 右侧可视化结果 -->
      <el-col :span="18">
        <el-card shadow="always" style="min-height: 600px;">
          <div slot="header" class="clearfix">
            <span>{{ functionTitles[activeFunction] || '可视化结果' }}</span>
            <div style="float: right;">
              <el-button 
                size="small" 
                type="text" 
                @click="downloadChart" 
                v-if="chartData && activeFunction !== 'overview'"
                :disabled="loading">
                <i class="el-icon-download"></i> 下载图表
              </el-button>
              <el-button 
                size="small" 
                type="text" 
                @click="fullscreen" 
                v-if="chartData"
                :disabled="loading">
                <i class="el-icon-full-screen"></i> 全屏
              </el-button>
            </div>
          </div>
          
          <!-- 加载状态 -->
          <div v-if="loading" class="loading-container">
            <i class="el-icon-loading" style="font-size: 30px;"></i>
            <p style="margin-top: 10px;">正在生成可视化...</p>
          </div>
          
          <!-- 错误状态 -->
          <div v-else-if="error" class="error-container">
            <i class="el-icon-warning" style="font-size: 30px; color: #f56c6c;"></i>
            <p style="margin-top: 10px; color: #f56c6c;">{{ error }}</p>
            <el-button type="primary" @click="generateVisualization" style="margin-top: 10px;">
              重试
            </el-button>
          </div>
          
          <!-- 可视化结果 - 根据选择的功能显示对应组件 -->
          <div v-else-if="selectedDatasetId">
            <!-- 数据概览 -->
            <div v-if="activeFunction === 'overview'">
              <DataPreview
                :source-id="selectedDatasetId"
                :source-type="selectedDataset ? (selectedDataset.sourceType || 'dataset') : 'dataset'">
              </DataPreview>
            </div>

            <!-- 石油曲线图 -->
            <div v-else-if="activeFunction === 'curves'">
              <PetroleumCurves
                :source-id="selectedDatasetId"
                :source-type="selectedDataset ? (selectedDataset.sourceType || 'dataset') : 'dataset'">
              </PetroleumCurves>
            </div>

            <!-- 相关性分析 -->
            <div v-else-if="activeFunction === 'correlation'">
              <CorrelationAnalysis
                :source-id="selectedDatasetId"
                :source-type="selectedDataset ? (selectedDataset.sourceType || 'dataset') : 'dataset'">
              </CorrelationAnalysis>
            </div>

            <!-- 统计分析 -->
            <div v-else-if="activeFunction === 'statistics'">
              <StatisticsAnalysis
                :source-id="selectedDatasetId"
                :source-type="selectedDataset ? (selectedDataset.sourceType || 'dataset') : 'dataset'">
              </StatisticsAnalysis>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else class="empty-container">
            <i class="el-icon-picture" style="font-size: 30px;"></i>
            <p style="margin-top: 10px;">请选择数据集开始可视化分析</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 空状态 - 未选择数据集 -->
    <el-empty 
      v-else
      description="请先选择一个数据集开始可视化分析"
      :image-size="200">
    </el-empty>
  </div>
</template>

<script>
import { generateVisualization } from "@/api/petrol/visualization";
import { listAvailableDatasets, getDatasetColumns } from "@/api/petrol/dataset";



// 导入简单组件（替换旧的显示组件）
import StatisticsAnalysis from './components/simple/StatisticsAnalysis.vue';
import PetroleumCurves from './components/simple/PetroleumCurves.vue';
import CorrelationAnalysis from './components/simple/CorrelationAnalysis.vue';
import DataPreview from './components/simple/DataPreview.vue';

export default {
  name: "PetrolVisualization",
  components: {
    // 简单组件
    StatisticsAnalysis,
    PetroleumCurves,
    CorrelationAnalysis,
    DataPreview
  },
  data() {
    return {
      // 数据集相关
      datasets: [],
      datasetsLoading: false,
      selectedDatasetId: null,
      selectedDataset: null,
      datasetColumns: [],
      numericColumns: [],

      // 功能相关
      activeFunction: 'overview',
      functionTitles: {
        overview: '数据概览',
        curves: '测井曲线',
        statistics: '统计分析',
        correlation: '相关性分析'
      },

      // 配置和数据
      chartConfig: {},
      chartData: null,
      chartHeight: '500px',
      
      // 状态
      loading: false,
      error: null,

      // 图表实例
      chartInstance: null
    };
  },
  computed: {
    canGenerate() {
      return this.selectedDatasetId && !this.loading;
    }
  },
  created() {
    this.loadDatasets();
  },
  beforeDestroy() {
    if (this.chartInstance) {
      this.chartInstance.dispose();
    }
  },
  methods: {
    /** 加载数据集列表 */
    async loadDatasets() {
      this.datasetsLoading = true;

      try {
        const response = await listAvailableDatasets();
        const datasets = response.data || [];
        this.datasets = datasets.map(dataset => ({
          id: dataset.id,
          fileName: dataset.datasetName,
          rowCount: dataset.rowCount || 0,
          columnCount: dataset.columnCount || 0,
          createTime: dataset.uploadTime,
          datasetCategory: dataset.datasetCategory
        }));
        this.datasetsLoading = false;
      } catch (error) {
        this.datasetsLoading = false;
        this.$message.error("加载数据集失败: " + error.message);
      }
    },
    
    /** 刷新数据集 */
    refreshDatasets() {
      this.loadDatasets();
    },
    
    /** 数据集改变 */
    handleDatasetChange(datasetId) {
      const dataset = this.datasets.find(d => d.id === datasetId);
      if (dataset) {
        this.selectedDataset = dataset;
        this.loadDatasetColumns(datasetId);
        this.resetVisualization();
        // 自动激活可视化
        this.$nextTick(() => {
          this.chartData = { generated: true, timestamp: Date.now() };
          this.$message.success(`已选择数据集: ${dataset.fileName}，可视化已激活`);
        });
      }
    },
    
    /** 加载数据集列信息 */
    async loadDatasetColumns(datasetId) {
      try {
        const response = await getDatasetColumns(datasetId);
        const data = response.data;
        this.datasetColumns = data.columns || [];
        this.numericColumns = data.numericColumns || data.columns.filter(col => {
          const stats = data.stats && data.stats[col];
          return stats && (stats.type === 'numeric' || stats.type === 'integer' || stats.type === 'float');
        }) || [];
      } catch (error) {
        this.$message.error("获取数据集列信息失败: " + error.message);
        this.datasetColumns = [];
        this.numericColumns = [];
      }
    },
    

    
    /** 配置改变 */
    handleConfigChange(config) {
      this.chartConfig = { ...config };
    },
    
    /** 刷新可视化 */
    refreshVisualization() {
      if (!this.selectedDatasetId) {
        this.$modal.msgError("请先选择数据集");
        return;
      }

      // 触发组件刷新
      this.loading = true;
      this.error = null;

      // 重置组件
      this.chartData = null;
      this.$nextTick(() => {
        this.chartData = { generated: true, timestamp: Date.now() };
        this.loading = false;
        this.$message.success('可视化已刷新');
      });
    },

    /** 获取真实数据 */
    getRealData() {
      // 返回空对象，让各个组件自己处理数据获取
      return {};
    },

    /** 生成数据预览数据 */
    generateOverviewData() {
      if (!this.selectedDataset || !this.selectedDataset.data) {
        return {
          preview: [],
          statistics: {},
          missingInfo: {}
        };
      }

      const data = this.selectedDataset.data;
      const preview = data.slice(0, 10); // 前10行作为预览

      // 计算统计信息
      const statistics = {};
      const missingInfo = {};

      if (data.length > 0) {
        const columns = Object.keys(data[0]);

        columns.forEach(column => {
          const values = data.map(row => row[column]).filter(val => val != null && val !== '');
          const numericValues = values.filter(val => !isNaN(parseFloat(val))).map(val => parseFloat(val));

          if (numericValues.length > 0) {
            const sorted = numericValues.sort((a, b) => a - b);
            const mean = numericValues.reduce((sum, val) => sum + val, 0) / numericValues.length;
            const variance = numericValues.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / numericValues.length;
            const std = Math.sqrt(variance);

            statistics[column] = {
              count: numericValues.length,
              mean: parseFloat(mean.toFixed(3)),
              std: parseFloat(std.toFixed(3)),
              min: sorted[0],
              max: sorted[sorted.length - 1],
              q25: sorted[Math.floor(sorted.length * 0.25)],
              q50: sorted[Math.floor(sorted.length * 0.5)],
              q75: sorted[Math.floor(sorted.length * 0.75)]
            };
          }

          const missingCount = data.length - values.length;
          missingInfo[column] = {
            missingCount: missingCount,
            missingRatio: parseFloat((missingCount / data.length).toFixed(3))
          };
        });
      }

      return {
        preview,
        statistics,
        missingInfo
      };
    },

    /** 生成统计分析数据 */
    generateStatisticsData() {
      return {
        chartType: 'statistics',
        message: '统计分析数据',
        data: this.selectedDataset ? this.selectedDataset.data : []
      };
    },

    /** 生成石油曲线数据 */
    generateCurvesData() {
      return {
        chartType: 'curves',
        message: '石油曲线图数据',
        data: this.selectedDataset ? this.selectedDataset.data : []
      };
    },

    /** 生成相关性分析数据 */
    generateCorrelationData() {
      return {
        chartType: 'correlation',
        message: '相关性分析数据',
        data: this.selectedDataset ? this.selectedDataset.data : []
      };
    },

    /** 生成数据 */
    generateData() {
      switch (this.activeFunction) {
        case 'overview':
          return this.generateOverviewData();
        case 'curves':
          return this.generateCurvesData();
        case 'statistics':
          return this.generateStatisticsData();
        case 'correlation':
          return this.generateCorrelationData();
        default:
          return {};
      }
    },


    
    /** 重置可视化 */
    resetVisualization() {
      this.chartData = null;
      this.error = null;
      this.chartConfig = {};
      if (this.chartInstance) {
        this.chartInstance.dispose();
        this.chartInstance = null;
      }
    },
    
    /** 图表准备就绪 */
    handleChartReady(chartInstance) {
      this.chartInstance = chartInstance;
    },
    
    /** 下载图表 */
    downloadChart() {
      if (!this.chartInstance) {
        this.$modal.msgError("图表未准备就绪");
        return;
      }
      
      const url = this.chartInstance.getDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: '#fff'
      });
      
      const link = document.createElement('a');
      link.href = url;
      link.download = `${this.selectedDataset?.datasetName || 'dataset'}_${this.activeFunction}_chart.png`;
      link.click();
    },

    /** 功能切换处理 */
    handleFunctionChange(value) {
      this.activeFunction = value;
      // 可以在这里添加切换功能时的逻辑
    },

    /** 获取功能描述 */
    getFunctionDescription(func) {
      const descriptions = {
        overview: '显示数据集的基本信息、数据预览和质量统计',
        curves: '展示石油测井曲线图，包括深度、孔隙度、渗透率等参数',
        statistics: '提供详细的统计分析，包括分布图、箱线图等',
        correlation: '分析各参数之间的相关性，生成相关性矩阵和热力图'
      };
      return descriptions[func] || '选择功能查看详细说明';
    },

    /** 全屏显示 */
    fullscreen() {
      // 实现全屏功能
      this.$modal.msgInfo("全屏功能开发中...");
    },


  }
};
</script>

<style scoped>
.visualization-menu {
  border: none;
}

.visualization-menu .el-menu-item {
  height: 45px;
  line-height: 45px;
}

.loading-container,
.error-container,
.empty-container {
  text-align: center;
  padding: 100px 0;
  color: #909399;
}

.text-large {
  font-size: 18px;
}

.font-600 {
  font-weight: 600;
}

.mr-3 {
  margin-right: 12px;
}
</style>
