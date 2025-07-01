<template>
  <div class="data-preview">
    <!-- 数据源信息 -->
    <el-row :gutter="20" class="info-row">
      <el-col :span="6">
        <el-card class="info-card">
          <div class="info-item">
            <i class="el-icon-document"></i>
            <div>
              <h4>数据行数</h4>
              <p>{{ dataInfo.rowCount || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="info-card">
          <div class="info-item">
            <i class="el-icon-menu"></i>
            <div>
              <h4>数据列数</h4>
              <p>{{ dataInfo.columnCount || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="info-card">
          <div class="info-item">
            <i class="el-icon-folder"></i>
            <div>
              <h4>文件名称</h4>
              <p>{{ dataInfo.fileName || '未知' }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="info-card">
          <div class="info-item">
            <i class="el-icon-cpu"></i>
            <div>
              <h4>数据源类型</h4>
              <p>
                <el-tag :type="dataInfo.sourceType === 'dataset' ? 'success' : 'primary'" size="mini">
                  {{ dataInfo.sourceType === 'dataset' ? '数据集' : '分析任务' }}
                </el-tag>
              </p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 任务详细信息 - 仅对分析任务显示 -->
    <el-card v-if="dataInfo.sourceType === 'task'" class="task-info-card" shadow="never">
      <div slot="header">
        <span>🔧 任务详细信息</span>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="任务名称">{{ dataInfo.name || 'N/A' }}</el-descriptions-item>
        <el-descriptions-item label="算法类型">
          <el-tag type="info">{{ getAlgorithmName(dataInfo.algorithm) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="任务状态">
          <el-tag :type="getStatusType(dataInfo.status)">{{ getStatusName(dataInfo.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="包含结果">
          <el-tag :type="dataInfo.hasResults ? 'success' : 'warning'">
            {{ dataInfo.hasResults ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ dataInfo.createTime || 'N/A' }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ dataInfo.description || 'N/A' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 列信息 -->
    <el-card class="columns-card" shadow="never">
      <div slot="header">
        <span>📋 数据列信息</span>
        <el-button 
          type="text" 
          icon="el-icon-refresh" 
          @click="loadColumns"
          :loading="columnsLoading">
          刷新
        </el-button>
      </div>
      
      <div v-if="columnsLoading" class="loading-container">
        <el-skeleton :rows="2" animated />
      </div>
      
      <div v-else-if="columns.length === 0" class="empty-container">
        <el-empty description="暂无列信息" :image-size="100" />
      </div>
      
      <div v-else class="columns-container">
        <div class="column-section">
          <h4>所有列 ({{ columns.length }})</h4>
          <div class="column-tags">
            <el-tag 
              v-for="column in columns" 
              :key="column" 
              class="column-tag">
              {{ column }}
            </el-tag>
          </div>
        </div>
        
        <div v-if="numericColumns.length > 0" class="column-section">
          <h4>数值列 ({{ numericColumns.length }})</h4>
          <div class="column-tags">
            <el-tag 
              v-for="column in numericColumns" 
              :key="column" 
              type="success"
              class="column-tag">
              {{ column }}
            </el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 数据预览 -->
    <el-card class="preview-card" shadow="never">
      <div slot="header">
        <span>👀 数据预览</span>
        <div class="header-controls">
          <el-input-number 
            v-model="maxRows" 
            :min="10" 
            :max="1000" 
            :step="10"
            size="mini"
            @change="loadPreviewData">
          </el-input-number>
          <span class="control-label">显示行数</span>
          <el-button 
            type="text" 
            icon="el-icon-refresh" 
            @click="loadPreviewData"
            :loading="previewLoading">
            刷新
          </el-button>
        </div>
      </div>
      
      <div v-if="previewLoading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>
      
      <div v-else-if="previewData.length === 0" class="empty-container">
        <el-empty description="暂无数据" :image-size="100" />
      </div>
      
      <div v-else class="preview-table-container">
        <el-table 
          :data="previewData" 
          border 
          stripe 
          size="mini"
          max-height="400"
          style="width: 100%">
          <el-table-column 
            type="index" 
            label="#" 
            width="50"
            align="center">
          </el-table-column>
          <el-table-column 
            v-for="column in displayColumns" 
            :key="column"
            :prop="column"
            :label="column"
            min-width="120"
            show-overflow-tooltip>
            <template slot-scope="scope">
              <span :class="getValueClass(scope.row[column])">
                {{ formatValue(scope.row[column]) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
        
        <div class="table-footer">
          <span class="data-count">
            显示 {{ previewData.length }} 行数据
          </span>
          <el-button 
            v-if="displayColumns.length < columns.length"
            type="text" 
            @click="showAllColumns = !showAllColumns">
            {{ showAllColumns ? '显示部分列' : '显示全部列' }}
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getDataSourceInfo, getDataSourceColumns, readDataSourceData } from "@/api/petrol/visualization";

export default {
  name: "DataPreview",
  props: {
    sourceId: {
      type: [String, Number],
      required: true
    },
    sourceType: {
      type: String,
      required: true,
      default: 'dataset',
      validator: value => ['task', 'dataset'].includes(value)
    }
  },
  data() {
    return {
      dataInfo: {},
      columns: [],
      numericColumns: [],
      previewData: [],
      maxRows: 50,
      showAllColumns: false,
      columnsLoading: false,
      previewLoading: false
    };
  },
  computed: {
    displayColumns() {
      if (this.showAllColumns || this.columns.length <= 10) {
        return this.columns;
      }
      return this.columns.slice(0, 10);
    }
  },
  created() {
    this.loadDataInfo();
    this.loadColumns();
    this.loadPreviewData();
  },
  methods: {
    /** 加载数据源信息 */
    async loadDataInfo() {
      try {
        // 确保sourceId是字符串类型，sourceType有默认值
        const sourceId = String(this.sourceId);
        const sourceType = this.sourceType || 'dataset';

        console.log('加载数据源信息:', { sourceId, sourceType });
        const response = await getDataSourceInfo(sourceId, sourceType);
        this.dataInfo = response.data || {};
      } catch (error) {
        console.error("加载数据源信息失败:", error);
      }
    },

    /** 加载列信息 */
    async loadColumns() {
      this.columnsLoading = true;
      try {
        // 确保sourceId是字符串类型，sourceType有默认值
        const sourceId = String(this.sourceId);
        const sourceType = this.sourceType || 'dataset';

        const response = await getDataSourceColumns(sourceId, sourceType);
        const data = response.data || {};
        this.columns = data.columns || [];
        this.numericColumns = data.numericColumns || [];
      } catch (error) {
        this.$message.error("加载列信息失败: " + error.message);
        this.columns = [];
        this.numericColumns = [];
      } finally {
        this.columnsLoading = false;
      }
    },

    /** 加载预览数据 */
    async loadPreviewData() {
      this.previewLoading = true;
      try {
        // 确保sourceId是字符串类型，sourceType有默认值
        const sourceId = String(this.sourceId);
        const sourceType = this.sourceType || 'dataset';

        const params = {
          maxRows: this.maxRows
        };
        const response = await readDataSourceData(sourceId, sourceType, params);
        this.previewData = response.data || [];
      } catch (error) {
        this.$message.error("加载预览数据失败: " + error.message);
        this.previewData = [];
      } finally {
        this.previewLoading = false;
      }
    },

    /** 格式化值显示 */
    formatValue(value) {
      if (value === null || value === undefined) {
        return 'NULL';
      }
      if (typeof value === 'number') {
        return Number.isInteger(value) ? value : value.toFixed(4);
      }
      return String(value);
    },

    /** 获取值的样式类 */
    getValueClass(value) {
      if (value === null || value === undefined) {
        return 'null-value';
      }
      if (typeof value === 'number') {
        return 'number-value';
      }
      return 'text-value';
    },

    /** 获取算法显示名称 */
    getAlgorithmName(algorithm) {
      const names = {
        'regression_linear_train': '线性回归训练',
        'regression_exponential_train': '指数回归训练',
        'regression_random_forest_train': '随机森林回归训练',
        'cluster_kmeans': 'K均值聚类',
        'cluster_dbscan': 'DBSCAN聚类',
        'fractal_dimension': '分形维数分析'
      };
      return names[algorithm] || algorithm;
    },

    /** 获取状态类型 */
    getStatusType(status) {
      const types = {
        'COMPLETED': 'success',
        'RUNNING': 'warning',
        'QUEUED': 'info',
        'FAILED': 'danger'
      };
      return types[status] || 'info';
    },

    /** 获取状态名称 */
    getStatusName(status) {
      const names = {
        'COMPLETED': '已完成',
        'RUNNING': '运行中',
        'QUEUED': '排队中',
        'FAILED': '失败'
      };
      return names[status] || status;
    }
  }
};
</script>

<style scoped>
.data-preview {
  padding: 0;
}

.info-row {
  margin-bottom: 20px;
}

.info-card {
  height: 100%;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-item i {
  font-size: 24px;
  color: #409eff;
}

.info-item h4 {
  margin: 0 0 4px 0;
  color: #303133;
  font-size: 14px;
}

.info-item p {
  margin: 0;
  color: #606266;
  font-size: 16px;
  font-weight: 500;
}

.task-info-card, .columns-card, .preview-card {
  margin-bottom: 20px;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-label {
  font-size: 12px;
  color: #909399;
}

.loading-container, .empty-container {
  padding: 40px 0;
  text-align: center;
}

.columns-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.column-section h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 14px;
}

.column-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.column-tag {
  margin: 0;
}

.preview-table-container {
  position: relative;
}

.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.data-count {
  font-size: 12px;
  color: #909399;
}

.null-value {
  color: #f56c6c;
  font-style: italic;
}

.number-value {
  color: #67c23a;
  font-family: 'Courier New', monospace;
}

.text-value {
  color: #303133;
}
</style>
