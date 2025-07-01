<template>
  <div class="target-selector">
    <el-card>
      <div slot="header">
        <span>目标选择 (预测变量 Y)</span>
        <el-button 
          style="float: right; padding: 3px 0" 
          type="text" 
          @click="showHelp = !showHelp">
          {{ showHelp ? '隐藏' : '帮助' }}
        </el-button>
      </div>

      <!-- 帮助信息 -->
      <el-collapse-transition>
        <el-alert
          v-show="showHelp"
          title="目标变量选择指南"
          type="info"
          :closable="false"
          style="margin-bottom: 20px;">
          <div slot="description">
            <p><strong>目标变量（Target）</strong>是您希望预测的变量，选择合适的目标变量决定了预测任务的类型：</p>
            <ul>
              <li><strong>回归任务</strong>：预测连续数值，如油产量、孔隙度、渗透率等</li>
              <li><strong>分类任务</strong>：预测离散类别，如岩性类型、油气显示等</li>
              <li><strong>数据要求</strong>：目标变量应该有足够的变化和代表性</li>
              <li><strong>质量检查</strong>：避免选择缺失值过多的列作为目标</li>
            </ul>
          </div>
        </el-alert>
      </el-collapse-transition>

      <!-- 无数据状态 -->
      <div v-if="!columns || columns.length === 0" style="text-align: center; padding: 40px;">
        <i class="el-icon-warning" style="font-size: 48px; color: #E6A23C;"></i>
        <p style="color: #909399; margin-top: 10px;">请先上传数据文件并完成数据预览</p>
      </div>

      <!-- 目标选择界面 -->
      <div v-else>
        <!-- 任务类型选择 -->
        <div class="task-type-section">
          <h4>预测任务类型</h4>
          <el-radio-group v-model="taskType" @change="onTaskTypeChange">
            <el-radio-button label="regression">
              <i class="el-icon-trend-charts"></i> 回归预测
            </el-radio-button>
            <el-radio-button label="classification">
              <i class="el-icon-pie-chart"></i> 分类预测
            </el-radio-button>
            <el-radio-button label="auto">
              <i class="el-icon-magic-stick"></i> 自动检测
            </el-radio-button>
          </el-radio-group>
          <p class="task-type-desc">{{ getTaskTypeDescription() }}</p>
        </div>

        <!-- 目标列选择 -->
        <div class="target-selection-section">
          <!-- 选择说明 -->
          <div class="selection-instruction" style="margin-bottom: 15px; padding: 10px; background-color: #fef7e0; border-left: 4px solid #E6A23C; border-radius: 4px;">
            <div style="display: flex; align-items: center;">
              <i class="el-icon-warning" style="color: #E6A23C; margin-right: 8px; font-size: 16px;"></i>
              <span style="font-weight: bold; color: #E6A23C;">单选目标列 (预测变量 Y)</span>
            </div>
            <p style="margin: 5px 0 0 24px; color: #606266; font-size: 13px;">
              请选择<strong>一个列</strong>作为预测目标，这是您希望模型学习和预测的数值
            </p>
          </div>

          <h4>选择目标变量</h4>
          
          <!-- 搜索和过滤 -->
          <el-row :gutter="10" style="margin-bottom: 15px;">
            <el-col :span="12">
              <el-input
                v-model="searchText"
                placeholder="搜索列名..."
                size="small"
                prefix-icon="el-icon-search"
                clearable />
            </el-col>
            <el-col :span="12">
              <el-button-group size="small">
                <el-button @click="showRecommended">推荐目标</el-button>
                <el-button @click="showAllColumns">显示全部</el-button>
              </el-button-group>
            </el-col>
          </el-row>

          <!-- 目标列列表 -->
          <el-radio-group v-model="selectedTarget" @change="onTargetChange" class="target-list">
            <div class="target-grid">
              <div
                v-for="column in filteredTargetColumns"
                :key="column.name"
                class="target-item"
                :class="{ 
                  'recommended': isRecommended(column),
                  'selected': selectedTarget === column.name 
                }">
                <el-radio :label="column.name" class="target-radio">
                  <div class="target-info">
                    <div class="target-header">
                      <i class="el-icon-aim" style="color: #E6A23C; margin-right: 5px;"></i>
                      <span class="target-name">{{ column.name }}</span>
                      <el-tag 
                        :type="getColumnTypeColor(column.type)" 
                        size="mini"
                        style="margin-left: 8px;">
                        {{ getColumnTypeName(column.type) }}
                      </el-tag>
                      <el-tag 
                        v-if="isRecommended(column)"
                        type="success" 
                        size="mini"
                        style="margin-left: 5px;">
                        推荐
                      </el-tag>
                      <el-tag 
                        :type="getTaskTypeTagColor(column)"
                        size="mini"
                        style="margin-left: 5px;">
                        {{ getDetectedTaskType(column) }}
                      </el-tag>
                    </div>
                    
                    <div class="target-stats">
                      <span class="stat-item">
                        <i class="el-icon-check"></i> {{ column.nonNullCount }} 有效值
                      </span>
                      <span class="stat-item" v-if="column.nullCount > 0">
                        <i class="el-icon-close" style="color: #F56C6C;"></i> {{ column.nullCount }} 缺失值
                      </span>
                      <span class="stat-item">
                        <i class="el-icon-data-line"></i> {{ column.uniqueCount }} 唯一值
                      </span>
                    </div>
                    
                    <div class="target-sample" v-if="column.sampleValues && column.sampleValues.length > 0">
                      <span class="sample-label">示例值：</span>
                      <span class="sample-values">{{ column.sampleValues.slice(0, 4).join(', ') }}</span>
                    </div>
                    
                    <!-- 数据分布信息 -->
                    <div class="target-distribution">
                      <span class="distribution-label">数据分布：</span>
                      <span class="distribution-info">{{ getDistributionInfo(column) }}</span>
                    </div>
                  </div>
                </el-radio>
              </div>
            </div>
          </el-radio-group>
        </div>

        <!-- 目标变量分析 -->
        <div v-if="selectedTarget" class="target-analysis">
          <el-divider>目标变量分析</el-divider>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-statistic 
                title="预测任务类型" 
                :value="getDetectedTaskType(selectedTargetColumn)" />
            </el-col>
            <el-col :span="8">
              <el-statistic 
                title="数据完整性" 
                :value="getDataCompleteness(selectedTargetColumn)" 
                suffix="%" />
            </el-col>
            <el-col :span="8">
              <el-statistic 
                title="数据变异性" 
                :value="getDataVariability(selectedTargetColumn)" />
            </el-col>
          </el-row>
          
          <!-- 质量评估 -->
          <div style="margin-top: 15px;">
            <h5>质量评估</h5>
            <div class="quality-indicators">
              <el-tag 
                :type="getQualityIndicator('completeness').type" 
                size="small"
                style="margin-right: 8px;">
                {{ getQualityIndicator('completeness').label }}
              </el-tag>
              <el-tag 
                :type="getQualityIndicator('variability').type" 
                size="small"
                style="margin-right: 8px;">
                {{ getQualityIndicator('variability').label }}
              </el-tag>
              <el-tag 
                :type="getQualityIndicator('distribution').type" 
                size="small">
                {{ getQualityIndicator('distribution').label }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 验证提示 -->
        <div v-if="validationMessage" class="validation-message">
          <el-alert
            :title="validationMessage.title"
            :description="validationMessage.description"
            :type="validationMessage.type"
            show-icon
            :closable="false" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'TargetSelector',
  props: {
    columns: {
      type: Array,
      default: () => []
    },
    value: {
      type: String,
      default: ''
    },
    excludeColumns: {
      type: Array,
      default: () => []
    },
    compact: {
      type: Boolean,
      default: false
    },
    preferredTaskType: {
      type: String,
      default: 'auto'
    }
  },
  data() {
    return {
      selectedTarget: '',
      taskType: 'regression', // 默认为回归任务
      showHelp: false,
      searchText: '',
      showOnlyRecommended: false,
      recommendedTargets: [],
      _isInitializing: true
    }
  },
  computed: {
    availableTargetColumns() {
      return this.columns.filter(col => 
        !this.excludeColumns.includes(col.name)
      )
    },
    
    filteredTargetColumns() {
      let filtered = this.availableTargetColumns
      
      // 搜索过滤
      if (this.searchText) {
        filtered = filtered.filter(col =>
          col.name.toLowerCase().includes(this.searchText.toLowerCase())
        )
      }
      
      // 推荐过滤
      if (this.showOnlyRecommended) {
        filtered = filtered.filter(col => 
          this.recommendedTargets.includes(col.name)
        )
      }
      
      // 根据任务类型过滤
      if (this.taskType === 'regression') {
        filtered = filtered.filter(col => 
          ['int64', 'float64', 'int32', 'float32', 'number'].includes(col.type)
        )
      } else if (this.taskType === 'classification') {
        filtered = filtered.filter(col => 
          col.uniqueCount < 50 // 分类任务的类别数不应太多
        )
      }
      
      return filtered
    },
    
    selectedTargetColumn() {
      return this.columns.find(col => col.name === this.selectedTarget)
    },
    
    validationMessage() {
      if (!this.selectedTarget) {
        return {
          title: '请选择目标变量',
          description: '需要选择一个目标变量来进行预测',
          type: 'warning'
        }
      }
      
      const column = this.selectedTargetColumn
      if (!column) return null
      
      // 检查缺失值比例
      const missingRatio = column.nullCount / (column.nullCount + column.nonNullCount)
      if (missingRatio > 0.5) {
        return {
          title: '目标变量质量较差',
          description: `选择的目标变量缺失值比例过高 (${(missingRatio * 100).toFixed(1)}%)，可能影响模型性能`,
          type: 'error'
        }
      }
      
      if (missingRatio > 0.2) {
        return {
          title: '注意数据质量',
          description: `目标变量有 ${(missingRatio * 100).toFixed(1)}% 的缺失值，建议进行数据清洗`,
          type: 'warning'
        }
      }
      
      // 检查变异性
      if (column.uniqueCount === 1) {
        return {
          title: '目标变量无变化',
          description: '选择的目标变量只有一个唯一值，无法进行有效预测',
          type: 'error'
        }
      }
      
      return null
    }
  },
  watch: {
    value: {
      handler(newValue) {
        this.selectedTarget = newValue || ''
        // 只在初始化时触发change事件，避免无限循环
        if (this._isInitializing !== false) {
          this._isInitializing = false
          this.$nextTick(() => {
            this.onTargetChange()
          })
        }
      },
      immediate: true
    },
    
    preferredTaskType: {
      handler(newValue) {
        this.taskType = newValue || 'auto'
      },
      immediate: true
    },
    
    columns: {
      handler() {
        this.generateRecommendations()
      },
      immediate: true
    }
  },
  methods: {
    /** 任务类型变化处理 */
    onTaskTypeChange() {
      console.log('任务类型变化:', this.taskType)
      this.$emit('task-type-change', this.taskType)

      // 任务类型变化时也需要重新发送目标变化事件
      if (this.selectedTarget) {
        this.onTargetChange()
      }
    },
    
    /** 目标变化处理 */
    onTargetChange() {
      const detectedType = this.selectedTargetColumn ?
        this.getDetectedTaskType(this.selectedTargetColumn) : this.taskType

      this.$emit('input', this.selectedTarget)
      this.$emit('change', {
        target: this.selectedTarget,
        taskType: this.taskType === 'auto' ? detectedType : this.taskType,
        detectedType: detectedType,
        isValid: this.selectedTarget && (!this.validationMessage || this.validationMessage.type !== 'error')
      })
    },

    /** 显示推荐目标 */
    showRecommended() {
      this.showOnlyRecommended = true
    },

    /** 显示全部列 */
    showAllColumns() {
      this.showOnlyRecommended = false
    },

    /** 生成推荐 */
    generateRecommendations() {
      if (!this.columns || this.columns.length === 0) return

      const recommendations = []

      // 推荐包含特定关键词的列
      const targetKeywords = ['目标', '预测', '结果', '产量', '含量', '类型', '等级']
      const keywordCols = this.availableTargetColumns.filter(col =>
        targetKeywords.some(keyword => col.name.includes(keyword))
      )
      recommendations.push(...keywordCols.map(col => col.name))

      // 推荐高质量的数值列（用于回归）
      const goodNumericCols = this.availableTargetColumns.filter(col =>
        ['int64', 'float64', 'int32', 'float32', 'number'].includes(col.type) &&
        col.nullCount / (col.nullCount + col.nonNullCount) < 0.1 && // 缺失值少于10%
        col.uniqueCount > 10 && // 有足够的变化
        col.uniqueCount < col.nonNullCount * 0.8 // 不是ID列
      )
      recommendations.push(...goodNumericCols.slice(0, 3).map(col => col.name))

      // 推荐合适的分类列
      const goodCategoricalCols = this.availableTargetColumns.filter(col =>
        col.uniqueCount > 1 && col.uniqueCount < 20 && // 2-20个类别
        col.nullCount / (col.nullCount + col.nonNullCount) < 0.1
      )
      recommendations.push(...goodCategoricalCols.slice(0, 2).map(col => col.name))

      this.recommendedTargets = [...new Set(recommendations)]
    },

    /** 是否为推荐目标 */
    isRecommended(column) {
      return this.recommendedTargets.includes(column.name)
    },

    /** 获取任务类型描述 */
    getTaskTypeDescription() {
      const descriptions = {
        'regression': '预测连续数值，如产量、孔隙度、渗透率等',
        'classification': '预测离散类别，如岩性类型、油气显示等',
        'auto': '根据选择的目标变量自动检测任务类型'
      }
      return descriptions[this.taskType] || ''
    },

    /** 检测任务类型 */
    getDetectedTaskType(column) {
      if (!column) return '未知'

      // 数值类型且唯一值较多 -> 回归
      if (['int64', 'float64', 'int32', 'float32', 'number'].includes(column.type)) {
        if (column.uniqueCount > 20) {
          return '回归'
        } else if (column.uniqueCount <= 10) {
          return '分类'
        } else {
          return '回归/分类'
        }
      }

      // 文本类型或唯一值较少 -> 分类
      if (column.uniqueCount <= 50) {
        return '分类'
      }

      return '其他'
    },

    /** 获取任务类型标签颜色 */
    getTaskTypeTagColor(column) {
      const taskType = this.getDetectedTaskType(column)
      const colorMap = {
        '回归': 'primary',
        '分类': 'success',
        '回归/分类': 'warning',
        '其他': 'info',
        '未知': 'info'
      }
      return colorMap[taskType] || 'info'
    },

    /** 获取分布信息 */
    getDistributionInfo(column) {
      if (!column) return ''

      const ratio = column.uniqueCount / column.nonNullCount
      if (ratio > 0.8) return '高度分散'
      if (ratio > 0.5) return '中等分散'
      if (ratio > 0.1) return '适度集中'
      return '高度集中'
    },

    /** 获取数据完整性 */
    getDataCompleteness(column) {
      if (!column) return 0
      return ((column.nonNullCount / (column.nonNullCount + column.nullCount)) * 100).toFixed(1)
    },

    /** 获取数据变异性 */
    getDataVariability(column) {
      if (!column) return '未知'

      const ratio = column.uniqueCount / column.nonNullCount
      if (ratio > 0.8) return '高'
      if (ratio > 0.3) return '中'
      return '低'
    },

    /** 获取质量指标 */
    getQualityIndicator(type) {
      const column = this.selectedTargetColumn
      if (!column) return { type: 'info', label: '未知' }

      if (type === 'completeness') {
        const completeness = parseFloat(this.getDataCompleteness(column))
        if (completeness >= 95) return { type: 'success', label: '完整性优秀' }
        if (completeness >= 80) return { type: 'warning', label: '完整性良好' }
        return { type: 'danger', label: '完整性较差' }
      }

      if (type === 'variability') {
        const variability = this.getDataVariability(column)
        if (variability === '高') return { type: 'success', label: '变异性高' }
        if (variability === '中') return { type: 'warning', label: '变异性中等' }
        return { type: 'danger', label: '变异性低' }
      }

      if (type === 'distribution') {
        const distribution = this.getDistributionInfo(column)
        if (distribution.includes('适度') || distribution.includes('中等')) {
          return { type: 'success', label: '分布良好' }
        }
        return { type: 'warning', label: '分布需注意' }
      }

      return { type: 'info', label: '未知' }
    },

    /** 获取列类型颜色 */
    getColumnTypeColor(type) {
      const colorMap = {
        'int64': 'primary', 'float64': 'primary', 'int32': 'primary', 'float32': 'primary', 'number': 'primary',
        'object': 'success', 'string': 'success', 'category': 'warning',
        'datetime64': 'info', 'datetime': 'info', 'date': 'info'
      }
      return colorMap[type] || 'info'
    },

    /** 获取列类型名称 */
    getColumnTypeName(type) {
      const nameMap = {
        'int64': '整数', 'float64': '浮点数', 'int32': '整数', 'float32': '浮点数', 'number': '数值',
        'object': '文本', 'string': '字符串', 'category': '分类',
        'datetime64': '日期时间', 'datetime': '日期时间', 'date': '日期'
      }
      return nameMap[type] || type
    }
  }
}
</script>

<style scoped>
.target-selector {
  width: 100%;
}

.task-type-section {
  margin-bottom: 25px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 6px;
}

.task-type-desc {
  margin-top: 10px;
  color: #606266;
  font-size: 14px;
}

.target-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 15px;
  margin-top: 10px;
}

.target-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 15px;
  transition: all 0.3s;
}

.target-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.target-item.recommended {
  border-color: #67c23a;
  background-color: #f0f9ff;
}

.target-item.selected {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.target-radio {
  width: 100%;
}

.target-info {
  margin-left: 20px;
}

.target-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.target-name {
  font-weight: 500;
  color: #303133;
}

.target-stats {
  display: flex;
  gap: 15px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 3px;
}

.target-sample, .target-distribution {
  font-size: 12px;
  color: #606266;
  margin-bottom: 5px;
}

.sample-label, .distribution-label {
  color: #909399;
}

.sample-values {
  font-family: monospace;
}

.target-analysis {
  margin-top: 20px;
  padding: 15px;
  background-color: #fafafa;
  border-radius: 6px;
}

.quality-indicators {
  margin-top: 10px;
}

.validation-message {
  margin-top: 15px;
}

h4, h5 {
  margin: 15px 0 10px 0;
  color: #303133;
}
</style>
