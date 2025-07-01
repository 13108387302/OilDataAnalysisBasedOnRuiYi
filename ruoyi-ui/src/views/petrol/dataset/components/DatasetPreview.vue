<template>
  <el-dialog
    title="数据集预览"
    :visible.sync="dialogVisible"
    width="90%"
    :before-close="handleClose"
    append-to-body
  >
    <div v-loading="loading">
      <!-- 数据集基本信息 -->
      <el-card class="dataset-info" shadow="never" v-if="dataset">
        <div slot="header" class="clearfix">
          <span class="card-title">数据集信息</span>
        </div>
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="info-item">
              <span class="label">数据集名称：</span>
              <span class="value">{{ dataset.datasetName }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">文件名：</span>
              <span class="value">{{ dataset.fileName }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">文件类型：</span>
              <el-tag size="small">{{ dataset.fileType }}</el-tag>
            </div>
          </el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top: 10px;">
          <el-col :span="8">
            <div class="info-item">
              <span class="label">数据规模：</span>
              <span class="value">{{ dataset.totalRows }}行 × {{ dataset.totalColumns }}列</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">文件大小：</span>
              <span class="value">{{ formatFileSize(dataset.fileSize) }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <span class="label">质量评分：</span>
              <el-progress 
                :percentage="dataset.dataQualityScore" 
                :color="getQualityColor(dataset.dataQualityScore)"
                :show-text="false"
                :stroke-width="6"
                style="width: 100px; display: inline-block; margin-right: 10px;"
              ></el-progress>
              <span class="value">{{ dataset.dataQualityScore }}%</span>
            </div>
          </el-col>
        </el-row>
        <el-row style="margin-top: 10px;" v-if="dataset.datasetDescription">
          <el-col :span="24">
            <div class="info-item">
              <span class="label">描述：</span>
              <span class="value">{{ dataset.datasetDescription }}</span>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 预览控制 -->
      <el-card class="preview-controls" shadow="never" style="margin-top: 15px;">
        <div slot="header" class="clearfix">
          <span class="card-title">数据预览</span>
          <div style="float: right;">
            <el-input-number 
              v-model="previewRows" 
              :min="5" 
              :max="100" 
              :step="5"
              size="small"
              style="width: 120px; margin-right: 10px;"
            />
            <el-button size="small" type="primary" @click="loadPreview">刷新预览</el-button>
          </div>
        </div>

        <!-- 数据表格 -->
        <el-table 
          :data="previewData" 
          border 
          stripe
          size="small"
          max-height="400"
          style="width: 100%"
          v-if="previewData.length > 0"
        >
          <el-table-column 
            label="行号" 
            type="index" 
            width="60" 
            align="center"
            :index="indexMethod"
          />
          <el-table-column
            v-for="(column, index) in tableColumns"
            :key="index"
            :prop="'col_' + index"
            :label="column"
            :width="getColumnWidth(column)"
            show-overflow-tooltip
          >
            <template slot-scope="scope">
              <span :class="getCellClass(scope.row['col_' + index])">
                {{ formatCellValue(scope.row['col_' + index]) }}
              </span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 无数据提示 -->
        <el-empty 
          v-if="!loading && previewData.length === 0" 
          description="暂无预览数据"
          :image-size="100"
        />
      </el-card>

      <!-- 列信息统计 -->
      <el-card class="column-stats" shadow="never" style="margin-top: 15px;" v-if="columnStats.length > 0">
        <div slot="header" class="clearfix">
          <span class="card-title">列统计信息</span>
        </div>
        <el-table :data="columnStats" border size="small">
          <el-table-column prop="name" label="列名" width="150" />
          <el-table-column prop="type" label="数据类型" width="100">
            <template slot-scope="scope">
              <el-tag :type="scope.row.type === 'numeric' ? 'success' : 'info'" size="small">
                {{ scope.row.type === 'numeric' ? '数值' : '文本' }}
              </el-tag>
            </template>
          </el-table-column>
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
          <el-table-column prop="mean" label="平均值" width="100">
            <template slot-scope="scope">
              {{ scope.row.type === 'numeric' ? formatNumber(scope.row.mean) : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="missingCount" label="缺失值" width="100">
            <template slot-scope="scope">
              <span :class="scope.row.missingCount > 0 ? 'text-warning' : 'text-success'">
                {{ scope.row.missingCount || 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="missingRate" label="缺失率" width="100">
            <template slot-scope="scope">
              <span :class="scope.row.missingRate > 0.1 ? 'text-warning' : 'text-success'">
                {{ formatPercentage(scope.row.missingRate) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
        </el-table>
      </el-card>
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">关 闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getDataset, getDatasetPreview, getDatasetColumns } from '@/api/petrol/dataset'

export default {
  name: 'DatasetPreview',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    datasetId: {
      type: [Number, String],
      default: null
    }
  },
  data() {
    return {
      dialogVisible: false,
      loading: false,
      dataset: null,
      previewData: [],
      columnStats: [],
      tableColumns: [],
      previewRows: 20
    }
  },
  watch: {
    visible(val) {
      this.dialogVisible = val
      if (val && this.datasetId) {
        this.loadData()
      }
    },
    dialogVisible(val) {
      this.$emit('update:visible', val)
    },
    datasetId(val) {
      if (val && this.dialogVisible) {
        this.loadData()
      }
    }
  },
  methods: {
    async loadData() {
      if (!this.datasetId) return
      
      this.loading = true
      try {
        // 并行加载数据集信息、预览数据和列统计
        const [datasetRes, previewRes, columnsRes] = await Promise.all([
          getDataset(this.datasetId),
          getDatasetPreview(this.datasetId, this.previewRows),
          getDatasetColumns(this.datasetId)
        ])
        
        this.dataset = datasetRes.data
        this.columnStats = columnsRes.data || []
        
        if (previewRes.data && previewRes.data.success) {
          this.processPreviewData(previewRes.data.data)
        }
        
      } catch (error) {
        console.error('加载数据失败:', error)
        this.$message.error('加载数据失败')
      } finally {
        this.loading = false
      }
    },
    
    async loadPreview() {
      if (!this.datasetId) return
      
      this.loading = true
      try {
        const response = await getDatasetPreview(this.datasetId, this.previewRows)
        if (response.data && response.data.success) {
          this.processPreviewData(response.data.data)
        }
      } catch (error) {
        console.error('加载预览数据失败:', error)
        this.$message.error('加载预览数据失败')
      } finally {
        this.loading = false
      }
    },
    
    processPreviewData(rawData) {
      if (!rawData || rawData.length === 0) {
        this.previewData = []
        this.tableColumns = []
        return
      }
      
      // 第一行作为列名
      this.tableColumns = rawData[0] || []
      
      // 其余行作为数据
      this.previewData = rawData.slice(1).map(row => {
        const rowObj = {}
        row.forEach((cell, index) => {
          rowObj['col_' + index] = cell
        })
        return rowObj
      })
    },
    
    indexMethod(index) {
      return index + 2 // 从第2行开始（第1行是标题）
    },
    
    getColumnWidth(columnName) {
      // 根据列名长度和内容类型动态设置宽度
      const baseWidth = Math.max(columnName.length * 12, 80)
      return Math.min(baseWidth, 200)
    },
    
    getCellClass(value) {
      if (value === null || value === undefined || value === '') {
        return 'cell-missing'
      }
      if (typeof value === 'number') {
        return 'cell-numeric'
      }
      return 'cell-text'
    },
    
    formatCellValue(value) {
      if (value === null || value === undefined) {
        return 'NULL'
      }
      if (value === '') {
        return '(空)'
      }
      if (typeof value === 'number') {
        return this.formatNumber(value)
      }
      return String(value)
    },
    
    formatNumber(num) {
      if (num === null || num === undefined) return '-'
      if (Number.isInteger(num)) return num.toString()
      return Number(num).toFixed(3)
    },
    
    formatPercentage(rate) {
      if (rate === null || rate === undefined) return '-'
      return (rate * 100).toFixed(1) + '%'
    },
    
    formatFileSize(bytes) {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    
    getQualityColor(score) {
      if (score >= 90) return '#67c23a'
      if (score >= 70) return '#e6a23c'
      return '#f56c6c'
    },
    
    handleClose() {
      this.dialogVisible = false
      this.dataset = null
      this.previewData = []
      this.columnStats = []
      this.tableColumns = []
    }
  }
}
</script>

<style scoped>
.dataset-info {
  margin-bottom: 15px;
}

.card-title {
  font-weight: bold;
  font-size: 16px;
}

.info-item {
  margin-bottom: 8px;
}

.info-item .label {
  font-weight: bold;
  color: #606266;
}

.info-item .value {
  color: #303133;
}

.cell-missing {
  color: #f56c6c;
  font-style: italic;
}

.cell-numeric {
  color: #409eff;
  font-family: 'Courier New', monospace;
}

.cell-text {
  color: #303133;
}

.text-warning {
  color: #e6a23c;
}

.text-success {
  color: #67c23a;
}

.dialog-footer {
  text-align: right;
}
</style>
