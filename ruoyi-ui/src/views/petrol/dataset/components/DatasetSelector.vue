<template>
  <div class="dataset-selector">
    <el-select
      v-model="selectedDatasetId"
      placeholder="请选择数据集"
      filterable
      clearable
      :loading="loading"
      @change="handleDatasetChange"
      style="width: 100%"
    >
      <el-option-group
        v-for="group in groupedDatasets"
        :key="group.category"
        :label="group.category"
      >
        <el-option
          v-for="dataset in group.datasets"
          :key="dataset.id"
          :label="dataset.datasetName"
          :value="dataset.id"
        >
          <div class="dataset-option">
            <div class="dataset-name">{{ dataset.datasetName }}</div>
            <div class="dataset-info">
              <el-tag size="mini" :type="getCategoryTagType(dataset.datasetCategory)">
                {{ dataset.datasetCategory }}
              </el-tag>
              <span class="dataset-size">{{ dataset.totalRows }}行 × {{ dataset.totalColumns }}列</span>
              <span class="dataset-quality">质量: {{ dataset.dataQualityScore }}%</span>
            </div>
          </div>
        </el-option>
      </el-option-group>
    </el-select>

    <!-- 数据集详情卡片 -->
    <el-card 
      v-if="selectedDataset" 
      class="dataset-detail-card" 
      shadow="never"
      style="margin-top: 15px;"
    >
      <div slot="header" class="clearfix">
        <span class="card-title">{{ selectedDataset.datasetName }}</span>
        <div style="float: right;">
          <el-button size="mini" type="text" @click="showPreview">预览数据</el-button>
          <el-button size="mini" type="text" @click="showColumns">查看列信息</el-button>
        </div>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="detail-item">
            <span class="label">文件名：</span>
            <span class="value">{{ selectedDataset.fileName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">文件类型：</span>
            <el-tag size="small">{{ selectedDataset.fileType }}</el-tag>
          </div>
          <div class="detail-item">
            <span class="label">数据规模：</span>
            <span class="value">{{ selectedDataset.totalRows }}行 × {{ selectedDataset.totalColumns }}列</span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="detail-item">
            <span class="label">文件大小：</span>
            <span class="value">{{ formatFileSize(selectedDataset.fileSize) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">数据质量：</span>
            <el-progress 
              :percentage="selectedDataset.dataQualityScore" 
              :color="getQualityColor(selectedDataset.dataQualityScore)"
              :show-text="false"
              :stroke-width="6"
              style="width: 100px; display: inline-block; margin-right: 10px;"
            ></el-progress>
            <span class="value">{{ selectedDataset.dataQualityScore }}%</span>
          </div>
          <div class="detail-item">
            <span class="label">创建时间：</span>
            <span class="value">{{ formatDate(selectedDataset.createTime) }}</span>
          </div>
        </el-col>
      </el-row>
      
      <div v-if="selectedDataset.datasetDescription" class="detail-item" style="margin-top: 10px;">
        <span class="label">描述：</span>
        <span class="value">{{ selectedDataset.datasetDescription }}</span>
      </div>
      
      <!-- 列信息预览 -->
      <div v-if="datasetColumns.length > 0" class="columns-preview" style="margin-top: 15px;">
        <div class="label" style="margin-bottom: 10px;">列信息：</div>
        <el-tag 
          v-for="column in datasetColumns.slice(0, 10)" 
          :key="column.name"
          size="small" 
          style="margin-right: 8px; margin-bottom: 5px;"
          :type="column.type === 'numeric' ? 'success' : 'info'"
        >
          {{ column.name }}
        </el-tag>
        <span v-if="datasetColumns.length > 10" class="more-columns">
          等{{ datasetColumns.length }}个列
        </span>
      </div>
    </el-card>

    <!-- 数据集预览对话框 -->
    <DatasetPreview 
      :visible.sync="previewVisible" 
      :dataset-id="selectedDatasetId"
    />

    <!-- 列信息对话框 -->
    <el-dialog
      title="列信息"
      :visible.sync="columnsVisible"
      width="800px"
      append-to-body
    >
      <el-table :data="datasetColumns" border size="small" max-height="400">
        <el-table-column prop="name" label="列名" width="150" />
        <el-table-column prop="type" label="数据类型" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.type === 'numeric' ? 'success' : 'info'" size="small">
              {{ scope.row.type === 'numeric' ? '数值' : '文本' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="min" label="最小值" width="100">
          <template slot-scope="scope">
            {{ scope.row.type === 'numeric' ? formatNumber(scope.row.min) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="max" label="最大值" width="100">
          <template slot-scope="scope">
            {{ scope.row.type === 'numeric' ? formatNumber(scope.row.max) : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { listAvailableDatasets, getDataset, getDatasetColumns } from '@/api/petrol/dataset'
import DatasetPreview from './DatasetPreview'

export default {
  name: 'DatasetSelector',
  components: {
    DatasetPreview
  },
  props: {
    value: {
      type: [Number, String],
      default: null
    },
    category: {
      type: String,
      default: null
    },
    showDetail: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      loading: false,
      datasets: [],
      selectedDatasetId: null,
      selectedDataset: null,
      datasetColumns: [],
      previewVisible: false,
      columnsVisible: false
    }
  },
  computed: {
    groupedDatasets() {
      const groups = {}
      this.datasets.forEach(dataset => {
        const category = dataset.datasetCategory || '其他'
        if (!groups[category]) {
          groups[category] = {
            category,
            datasets: []
          }
        }
        groups[category].datasets.push(dataset)
      })
      return Object.values(groups)
    }
  },
  watch: {
    value: {
      immediate: true,
      handler(val) {
        this.selectedDatasetId = val
        if (val) {
          this.loadDatasetDetail(val)
        } else {
          this.selectedDataset = null
          this.datasetColumns = []
        }
      }
    },
    category: {
      immediate: true,
      handler() {
        this.loadDatasets()
      }
    }
  },
  created() {
    this.loadDatasets()
  },
  methods: {
    async loadDatasets() {
      this.loading = true
      try {
        const response = await listAvailableDatasets()
        this.datasets = response.data || []
        
        // 如果指定了类别，过滤数据集
        if (this.category) {
          this.datasets = this.datasets.filter(dataset => 
            dataset.datasetCategory === this.category
          )
        }
      } catch (error) {
        console.error('加载数据集列表失败:', error)
        this.$message.error('加载数据集列表失败')
      } finally {
        this.loading = false
      }
    },
    
    async loadDatasetDetail(datasetId) {
      if (!datasetId) return
      
      try {
        const [datasetRes, columnsRes] = await Promise.all([
          getDataset(datasetId),
          getDatasetColumns(datasetId)
        ])
        
        this.selectedDataset = datasetRes.data
        this.datasetColumns = columnsRes.data || []
      } catch (error) {
        console.error('加载数据集详情失败:', error)
      }
    },
    
    handleDatasetChange(datasetId) {
      this.$emit('input', datasetId)
      this.$emit('change', datasetId, this.selectedDataset)
      
      if (datasetId) {
        this.loadDatasetDetail(datasetId)
      } else {
        this.selectedDataset = null
        this.datasetColumns = []
      }
    },
    
    showPreview() {
      this.previewVisible = true
    },
    
    showColumns() {
      this.columnsVisible = true
    },
    
    getCategoryTagType(category) {
      const typeMap = {
        '测井': 'primary',
        '地震': 'success',
        '生产': 'warning',
        '地质': 'info'
      }
      return typeMap[category] || 'info'
    },
    
    getQualityColor(score) {
      if (score >= 90) return '#67c23a'
      if (score >= 70) return '#e6a23c'
      return '#f56c6c'
    },
    
    formatFileSize(bytes) {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    
    formatDate(dateStr) {
      if (!dateStr) return '-'
      return new Date(dateStr).toLocaleString()
    },
    
    formatNumber(num) {
      if (num === null || num === undefined) return '-'
      if (Number.isInteger(num)) return num.toString()
      return Number(num).toFixed(3)
    }
  }
}
</script>

<style scoped>
.dataset-option {
  padding: 5px 0;
}

.dataset-name {
  font-weight: bold;
  margin-bottom: 3px;
}

.dataset-info {
  font-size: 12px;
  color: #909399;
}

.dataset-size,
.dataset-quality {
  margin-left: 8px;
}

.dataset-detail-card {
  border: 1px solid #e4e7ed;
}

.card-title {
  font-weight: bold;
  font-size: 16px;
}

.detail-item {
  margin-bottom: 8px;
  font-size: 14px;
}

.detail-item .label {
  font-weight: bold;
  color: #606266;
  display: inline-block;
  width: 80px;
}

.detail-item .value {
  color: #303133;
}

.columns-preview .label {
  font-weight: bold;
  color: #606266;
}

.more-columns {
  color: #909399;
  font-size: 12px;
}
</style>
