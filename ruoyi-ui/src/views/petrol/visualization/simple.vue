<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>数据可视化平台</h2>
      <p>基于统一数据源的可视化分析工具</p>
    </div>

    <!-- 数据源选择 -->
    <el-card class="data-source-card" shadow="never">
      <div slot="header" class="card-header">
        <span>📊 选择数据源</span>
        <el-button
          type="text"
          icon="el-icon-refresh"
          @click="loadDataSources"
          :loading="loading">
          刷新
        </el-button>

      </div>
      
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="3" animated />
      </div>
      
      <div v-else-if="dataSources.length === 0" class="empty-container">
        <el-empty description="暂无可用数据源">
          <el-button type="primary" @click="$router.push('/petrol/dataset')">
            去上传数据集
          </el-button>
        </el-empty>
      </div>
      
      <div v-else class="data-source-grid">
        <div 
          v-for="source in dataSources" 
          :key="source.id"
          class="data-source-item"
          :class="{ active: selectedSource && selectedSource.id === source.id }"
          @click="selectDataSource(source)">
          
          <div class="source-icon">
            <i :class="source.sourceType === 'dataset' ? 'el-icon-folder' : 'el-icon-cpu'"></i>
          </div>
          
          <div class="source-info">
            <h4>{{ source.name }}</h4>
            <p class="source-desc">{{ source.description || '暂无描述' }}</p>
            <div class="source-meta">
              <el-tag :type="source.sourceType === 'dataset' ? 'success' : 'primary'" size="mini">
                {{ source.source }}
              </el-tag>
              <span class="meta-item">
                <i class="el-icon-document"></i>
                {{ source.rowCount || 0 }} 行
              </span>
              <span class="meta-item">
                <i class="el-icon-menu"></i>
                {{ source.columnCount || 0 }} 列
              </span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 调试信息 -->
    <el-card v-if="!selectedSource && !loading" class="debug-card" shadow="never">
      <div slot="header">
        <span>🐛 调试信息</span>
      </div>
      <p>selectedSource: {{ selectedSource }}</p>
      <p>dataSources.length: {{ dataSources.length }}</p>
      <p>loading: {{ loading }}</p>
      <p>activeTab: {{ activeTab }}</p>
    </el-card>

    <!-- 可视化功能 -->
    <el-card v-if="selectedSource" class="visualization-card" shadow="never">
      <div slot="header" class="card-header">
        <span>🎯 可视化分析</span>
        <span class="selected-source">当前数据源: {{ selectedSource.name }}</span>
      </div>
      
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="📋 数据预览" name="preview">
          <DataPreview
            :source-id="selectedSource.id"
            :source-type="selectedSource.sourceType || 'dataset'" />
        </el-tab-pane>

        <el-tab-pane label="📊 统计分析" name="statistics">
          <StatisticsAnalysis
            :source-id="selectedSource.id"
            :source-type="selectedSource.sourceType || 'dataset'" />
        </el-tab-pane>

        <el-tab-pane label="📈 石油曲线" name="curves">
          <PetroleumCurves
            :source-id="selectedSource.id"
            :source-type="selectedSource.sourceType || 'dataset'" />
        </el-tab-pane>

        <el-tab-pane label="🔗 相关性分析" name="correlation">
          <CorrelationAnalysis
            :source-id="selectedSource.id"
            :source-type="selectedSource.sourceType || 'dataset'" />
        </el-tab-pane>

        <!-- 分析结果可视化 - 仅对分析任务显示 -->
        <el-tab-pane
          v-if="selectedSource.sourceType === 'task'"
          label="🎯 分析结果"
          name="results">
          <div class="task-results-container">
            <!-- 任务基本信息 -->
            <el-card class="task-info-card" shadow="never">
              <div slot="header">
                <span>📋 任务信息</span>
              </div>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="任务ID">{{ selectedSource.id }}</el-descriptions-item>
                <el-descriptions-item label="任务名称">{{ selectedSource.name }}</el-descriptions-item>
                <el-descriptions-item label="算法类型">{{ selectedSource.algorithm }}</el-descriptions-item>
                <el-descriptions-item label="任务状态">
                  <el-tag :type="getStatusType(selectedSource.status)">
                    {{ selectedSource.status }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="有结果数据">
                  <el-tag :type="selectedSource.hasResults ? 'success' : 'info'">
                    {{ selectedSource.hasResults ? '是' : '否' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ selectedSource.createTime }}</el-descriptions-item>
              </el-descriptions>
            </el-card>

            <!-- 结果展示区域 -->
            <div v-if="!selectedSource.hasResults" class="no-results-hint">
              <el-alert
                title="该任务暂无分析结果"
                :description="`任务状态: ${selectedSource.status || '未知'}。只有状态为'COMPLETED'且有结果数据的任务才能进行结果可视化。`"
                type="info"
                show-icon
                :closable="false">
              </el-alert>
            </div>
            <TaskResults
              v-else
              :source-id="selectedSource.id"
              :source-type="selectedSource.sourceType || 'task'" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>



    <!-- 空状态 -->
    <el-empty
      v-if="!selectedSource && !loading"
      description="请选择一个数据源开始可视化分析"
      :image-size="200">
    </el-empty>
  </div>
</template>

<script>
import { getAllDataSources } from "@/api/petrol/visualization";

// 导入简化的组件
import DataPreview from './components/simple/DataPreview.vue';
import StatisticsAnalysis from './components/simple/StatisticsAnalysis.vue';
import PetroleumCurves from './components/simple/PetroleumCurves.vue';
import CorrelationAnalysis from './components/simple/CorrelationAnalysis.vue';
import TaskResults from './components/simple/TaskResults.vue';

export default {
  name: "VisualizationSimple",
  components: {
    DataPreview,
    StatisticsAnalysis,
    PetroleumCurves,
    CorrelationAnalysis,
    TaskResults
  },
  data() {
    return {
      loading: false,
      dataSources: [],
      selectedSource: null,
      activeTab: 'preview'
    };
  },
  created() {
    console.log('🚀 Vue组件已创建，开始加载数据源...');
    console.log('🔍 检查登录状态...');
    console.log('🔍 Token:', this.$store.getters.token);
    console.log('🔍 用户信息:', this.$store.getters.name);
    this.loadDataSources();
  },
  mounted() {
    console.log('🎯 Vue组件已挂载，当前状态:', {
      loading: this.loading,
      dataSources: this.dataSources,
      selectedSource: this.selectedSource
    });
  },
  methods: {
    /** 加载数据源列表 */
    async loadDataSources() {
      this.loading = true;
      try {
        console.log('🔍 开始加载数据源...');
        const response = await getAllDataSources();
        console.log('📥 API响应:', response);

        this.dataSources = response.data || [];
        console.log('📊 解析后的数据源:', this.dataSources);
        console.log('📊 数据源数量:', this.dataSources.length);

        // 详细打印每个数据源的信息
        this.dataSources.forEach((source, index) => {
          console.log(`📊 数据源 ${index + 1}:`, {
            id: source.id,
            name: source.name,
            sourceType: source.sourceType,
            hasResults: source.hasResults,
            status: source.status,
            algorithm: source.algorithm
          });
        });

        // 如果有数据源，自动选中第一个
        if (this.dataSources.length > 0) {
          console.log('📊 第一个数据源详情:', JSON.stringify(this.dataSources[0], null, 2));
          console.log('✅ 自动选择第一个数据源:', this.dataSources[0]);
          this.selectDataSource(this.dataSources[0]);
        } else {
          console.log('⚠️ 没有找到任何数据源');
          this.$message.warning('没有找到可用的数据源，请先创建分析任务或上传数据集');
        }
      } catch (error) {
        console.error('❌ 加载数据源失败:', error);
        console.error('❌ 错误详情:', {
          message: error.message,
          response: error.response,
          status: error.response?.status,
          statusText: error.response?.statusText,
          data: error.response?.data
        });

        let errorMessage = '加载数据源失败';
        if (error.response) {
          if (error.response.status === 401) {
            errorMessage = '用户未登录或登录已过期，请重新登录';
          } else if (error.response.status === 403) {
            errorMessage = '没有访问权限';
          } else if (error.response.status === 404) {
            errorMessage = 'API接口不存在';
          } else if (error.response.status >= 500) {
            errorMessage = '服务器内部错误';
          } else {
            errorMessage = `请求失败: ${error.response.status} ${error.response.statusText}`;
          }
        } else if (error.message.includes('Network Error')) {
          errorMessage = '网络连接失败，请检查后端服务是否启动';
        } else {
          errorMessage = error.message;
        }

        this.$message.error(errorMessage);
        this.dataSources = [];
      } finally {
        this.loading = false;
      }
    },

    /** 选择数据源 */
    selectDataSource(source) {
      console.log('🎯 选择数据源:', source);
      console.log('🎯 数据源详细信息:', {
        id: source.id,
        name: source.name,
        sourceType: source.sourceType,
        hasResults: source.hasResults,
        status: source.status,
        algorithm: source.algorithm
      });

      this.selectedSource = source;

      // 如果是任务且有结果，默认切换到结果标签页
      if (source.sourceType === 'task' && source.hasResults) {
        this.activeTab = 'results';
        console.log('🎯 任务有结果，切换到结果标签页');
      } else {
        this.activeTab = 'preview';
        console.log('🎯 切换到预览标签页');
      }

      console.log('🎯 设置后的selectedSource:', this.selectedSource);
      console.log('🎯 activeTab:', this.activeTab);
      this.$message.success(`已选择数据源: ${source.name}`);
    },

    /** 标签切换处理 */
    handleTabClick(tab) {
      // 可以在这里添加标签切换的逻辑
    },





    /** 获取状态类型 */
    getStatusType(status) {
      const types = {
        'COMPLETED': 'success',
        'RUNNING': 'warning',
        'PENDING': 'info',
        'FAILED': 'danger'
      };
      return types[status] || 'info';
    }
  }
};
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h2 {
  color: #303133;
  margin-bottom: 10px;
}

.page-header p {
  color: #909399;
  font-size: 14px;
}

.data-source-card, .visualization-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.selected-source {
  color: #409eff;
  font-size: 14px;
}

.loading-container, .empty-container {
  padding: 40px 0;
  text-align: center;
}

.data-source-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.data-source-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: flex-start;
}

.data-source-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.data-source-item.active {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.source-icon {
  margin-right: 12px;
  font-size: 24px;
  color: #409eff;
  margin-top: 4px;
}

.source-info {
  flex: 1;
}

.source-info h4 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
}

.source-desc {
  margin: 0 0 12px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
}

.source-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.meta-item {
  color: #909399;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.no-results-hint {
  padding: 20px;
}

.task-results-container {
  padding: 0;
}

.task-info-card {
  margin-bottom: 20px;
}
</style>
