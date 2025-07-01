<template>
  <div class="regression-result-display">
    <!-- 结果摘要 -->
    <el-card shadow="never" class="summary-card">
      <div slot="header">
        <span>📈 预测摘要</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="6">
          <el-statistic title="预测样本数" :value="summary.totalPredictions" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="输入特征数" :value="summary.selectedFeatures" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="平均预测值" :value="summary.statistics.mean" :precision="4" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="平均置信度" :value="(summary.confidence.mean * 100).toFixed(1)" suffix="%" />
        </el-col>
      </el-row>
    </el-card>

    <!-- 统计信息 -->
    <el-card shadow="never" class="stats-card">
      <div slot="header">
        <span>📊 统计信息</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="stat-item">
            <h4>预测值分布</h4>
            <el-descriptions :column="1" size="small">
              <el-descriptions-item label="最小值">{{ summary.statistics.min.toFixed(4) }}</el-descriptions-item>
              <el-descriptions-item label="最大值">{{ summary.statistics.max.toFixed(4) }}</el-descriptions-item>
              <el-descriptions-item label="中位数">{{ summary.statistics.median.toFixed(4) }}</el-descriptions-item>
              <el-descriptions-item label="标准差">{{ summary.statistics.std.toFixed(4) }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
        
        <el-col :span="8">
          <div class="stat-item">
            <h4>置信度分布</h4>
            <el-descriptions :column="1" size="small">
              <el-descriptions-item label="最低置信度">{{ (summary.confidence.min * 100).toFixed(1) }}%</el-descriptions-item>
              <el-descriptions-item label="最高置信度">{{ (summary.confidence.max * 100).toFixed(1) }}%</el-descriptions-item>
              <el-descriptions-item label="平均置信度">{{ (summary.confidence.mean * 100).toFixed(1) }}%</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
        
        <el-col :span="8">
          <div class="stat-item">
            <h4>预测目标</h4>
            <el-descriptions :column="1" size="small">
              <el-descriptions-item label="目标变量">{{ summary.targetColumn }}</el-descriptions-item>
              <el-descriptions-item label="预测范围">{{ getPredictionRange() }}</el-descriptions-item>
              <el-descriptions-item label="数据质量">{{ getDataQuality() }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 结果表格 -->
    <el-card shadow="never" class="table-card">
      <div slot="header">
        <span>📋 详细结果</span>
        <div style="float: right;">
          <el-button type="text" size="small" @click="exportToCSV">
            <i class="el-icon-download"></i> 导出CSV
          </el-button>
          <el-button type="text" size="small" @click="exportToExcel">
            <i class="el-icon-s-grid"></i> 导出Excel
          </el-button>
        </div>
      </div>
      
      <!-- 表格工具栏 -->
      <div class="table-toolbar">
        <el-row :gutter="10">
          <el-col :span="8">
            <el-input
              v-model="searchText"
              placeholder="搜索结果..."
              prefix-icon="el-icon-search"
              size="small"
              clearable />
          </el-col>
          <el-col :span="8">
            <el-select v-model="filterColumn" placeholder="筛选列" size="small" clearable>
              <el-option 
                v-for="column in tableColumns" 
                :key="column"
                :label="column"
                :value="column" />
            </el-select>
          </el-col>
          <el-col :span="8">
            <el-button-group size="small">
              <el-button @click="showAllRows = !showAllRows">
                {{ showAllRows ? '显示前20行' : '显示全部' }}
              </el-button>
              <el-button @click="refreshTable">
                <i class="el-icon-refresh"></i>
              </el-button>
            </el-button-group>
          </el-col>
        </el-row>
      </div>

      <!-- 数据表格 -->
      <el-table 
        :data="filteredTableData" 
        border 
        size="small" 
        :max-height="400"
        stripe
        highlight-current-row>
        
        <!-- 行索引列 -->
        <el-table-column prop="行索引" label="行号" width="80" fixed="left">
          <template slot-scope="scope">
            <el-tag size="mini" type="info">{{ scope.row['行索引'] + 1 }}</el-tag>
          </template>
        </el-table-column>
        
        <!-- 预测值列 -->
        <el-table-column :prop="`预测_${targetColumn}`" :label="`预测_${targetColumn}`" width="120" fixed="left">
          <template slot-scope="scope">
            <span class="prediction-value">{{ scope.row[`预测_${targetColumn}`] }}</span>
          </template>
        </el-table-column>
        
        <!-- 置信度列 -->
        <el-table-column prop="置信度" label="置信度" width="100" fixed="left">
          <template slot-scope="scope">
            <el-progress 
              :percentage="parseFloat(scope.row['置信度'])" 
              :stroke-width="8"
              :show-text="false"
              :color="getConfidenceColor(parseFloat(scope.row['置信度']))" />
            <span class="confidence-text">{{ scope.row['置信度'] }}</span>
          </template>
        </el-table-column>
        
        <!-- 输入特征列 -->
        <el-table-column 
          v-for="feature in selectedFeatures" 
          :key="feature"
          :prop="feature"
          :label="feature"
          min-width="100">
          <template slot-scope="scope">
            <span>{{ formatFeatureValue(scope.row[feature]) }}</span>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="table-pagination">
        <el-pagination
          v-if="!showAllRows"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="resultTable.length">
        </el-pagination>
        
        <div v-else class="table-info">
          <span>显示全部 {{ resultTable.length }} 条记录</span>
        </div>
      </div>
    </el-card>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <el-button type="primary" @click="$emit('download')">
        <i class="el-icon-download"></i> 下载完整结果
      </el-button>
      <el-button @click="viewDetailedAnalysis">
        <i class="el-icon-data-analysis"></i> 详细分析
      </el-button>
      <el-button @click="$emit('restart')">
        <i class="el-icon-refresh-left"></i> 重新预测
      </el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'RegressionResultDisplay',
  props: {
    result: {
      type: Object,
      required: true,
      default: () => ({})
    },
    selectedFeatures: {
      type: Array,
      default: () => []
    },
    targetColumn: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      searchText: '',
      filterColumn: '',
      showAllRows: false,
      currentPage: 1,
      pageSize: 20
    }
  },
  computed: {
    resultTable() {
      return this.result.resultTable || []
    },
    
    summary() {
      return this.result.summary || {
        totalPredictions: 0,
        selectedFeatures: 0,
        statistics: { mean: 0, min: 0, max: 0, std: 0, median: 0 },
        confidence: { mean: 0, min: 0, max: 0 }
      }
    },
    
    tableColumns() {
      return this.resultTable.length > 0 ? Object.keys(this.resultTable[0]) : []
    },
    
    filteredTableData() {
      let data = this.resultTable
      
      // 搜索过滤
      if (this.searchText) {
        data = data.filter(row => {
          return Object.values(row).some(value => 
            String(value).toLowerCase().includes(this.searchText.toLowerCase())
          )
        })
      }
      
      // 列过滤
      if (this.filterColumn) {
        data = data.filter(row => row[this.filterColumn] !== undefined)
      }
      
      // 分页处理
      if (!this.showAllRows) {
        const start = (this.currentPage - 1) * this.pageSize
        const end = start + this.pageSize
        data = data.slice(start, end)
      }
      
      return data
    }
  },
  methods: {
    getPredictionRange() {
      const { min, max } = this.summary.statistics
      return `${min.toFixed(2)} - ${max.toFixed(2)}`
    },
    
    getDataQuality() {
      const avgConfidence = this.summary.confidence.mean
      if (avgConfidence >= 0.8) return '优秀'
      if (avgConfidence >= 0.6) return '良好'
      if (avgConfidence >= 0.4) return '一般'
      return '较差'
    },
    
    getConfidenceColor(percentage) {
      if (percentage >= 80) return '#67c23a'
      if (percentage >= 60) return '#e6a23c'
      return '#f56c6c'
    },
    
    formatFeatureValue(value) {
      if (typeof value === 'number') {
        return value.toFixed(3)
      }
      return value
    },
    
    handleSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
    },
    
    handleCurrentChange(val) {
      this.currentPage = val
    },
    
    refreshTable() {
      this.searchText = ''
      this.filterColumn = ''
      this.currentPage = 1
    },
    
    exportToCSV() {
      this.$emit('download')
    },
    
    exportToExcel() {
      // 实现Excel导出功能
      this.$message.info('Excel导出功能开发中...')
    },
    
    viewDetailedAnalysis() {
      // 跳转到详细分析页面
      this.$message.info('详细分析功能开发中...')
    }
  }
}
</script>

<style scoped>
.regression-result-display {
  width: 100%;
}

.summary-card, .stats-card, .table-card {
  margin-bottom: 20px;
}

.stat-item h4 {
  margin-bottom: 10px;
  color: #303133;
}

.table-toolbar {
  margin-bottom: 15px;
}

.prediction-value {
  font-weight: bold;
  color: #409EFF;
  font-size: 14px;
}

.confidence-text {
  font-size: 12px;
  color: #606266;
  margin-left: 5px;
}

.table-pagination, .table-info {
  margin-top: 15px;
  text-align: center;
}

.action-buttons {
  text-align: center;
  padding: 20px 0;
  border-top: 1px solid #e4e7ed;
  margin-top: 20px;
}

.action-buttons .el-button {
  margin: 0 10px;
}
</style>
