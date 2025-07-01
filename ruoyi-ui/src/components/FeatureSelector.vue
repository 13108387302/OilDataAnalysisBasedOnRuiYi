<template>
  <div class="feature-selector">
    <el-card>
      <div slot="header">
        <span>特征选择 (输入变量 X)</span>
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
          title="特征选择指南"
          type="info"
          :closable="false"
          style="margin-bottom: 20px;">
          <div slot="description">
            <p><strong>特征（Feature）</strong>是用于预测的输入变量，选择合适的特征对模型性能至关重要：</p>
            <ul>
              <li><strong>数值特征</strong>：连续的数值数据，如深度、密度、孔隙度等</li>
              <li><strong>分类特征</strong>：离散的类别数据，如岩性、地层等</li>
              <li><strong>特征工程</strong>：可以组合多个原始特征创建新特征</li>
              <li><strong>相关性</strong>：选择与目标变量相关性高的特征</li>
            </ul>
          </div>
        </el-alert>
      </el-collapse-transition>

      <!-- 无数据状态 -->
      <div v-if="!columns || columns.length === 0" style="text-align: center; padding: 40px;">
        <i class="el-icon-warning" style="font-size: 48px; color: #E6A23C;"></i>
        <p style="color: #909399; margin-top: 10px;">请先上传数据文件并完成数据预览</p>
      </div>

      <!-- 特征选择界面 -->
      <div v-else>
        <!-- 选择说明 -->
        <div class="selection-instruction" style="margin-bottom: 15px; padding: 10px; background-color: #f0f9ff; border-left: 4px solid #409EFF; border-radius: 4px;">
          <div style="display: flex; align-items: center;">
            <i class="el-icon-info" style="color: #409EFF; margin-right: 8px; font-size: 16px;"></i>
            <span style="font-weight: bold; color: #409EFF;">多选特征列 (输入变量 X)</span>
          </div>
          <p style="margin: 5px 0 0 24px; color: #606266; font-size: 13px;">
            请选择<strong>多个列</strong>作为模型的输入特征，这些特征将用于训练模型或进行预测计算
          </p>
        </div>

        <!-- 快速选择工具栏 -->
        <div class="selection-toolbar">
          <el-row :gutter="10">
            <el-col :span="12">
              <el-button-group>
                <el-button size="small" @click="selectAll">全选</el-button>
                <el-button size="small" @click="clearAll">清空</el-button>
                <el-button size="small" @click="selectNumeric">选择数值列</el-button>
                <el-button size="small" @click="selectRecommended">智能推荐</el-button>
              </el-button-group>
            </el-col>
            <el-col :span="12" style="text-align: right;">
              <el-input
                v-model="searchText"
                placeholder="搜索列名..."
                size="small"
                style="width: 200px;"
                prefix-icon="el-icon-search"
                clearable />
            </el-col>
          </el-row>
        </div>

        <!-- 过滤器 -->
        <div class="filter-bar" style="margin: 15px 0;">
          <el-row :gutter="10">
            <el-col :span="8">
              <el-select v-model="typeFilter" placeholder="按数据类型过滤" size="small" clearable>
                <el-option label="数值类型" value="numeric" />
                <el-option label="文本类型" value="text" />
                <el-option label="日期类型" value="date" />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-select v-model="qualityFilter" placeholder="按数据质量过滤" size="small" clearable>
                <el-option label="无缺失值" value="no_missing" />
                <el-option label="有缺失值" value="has_missing" />
                <el-option label="高唯一性" value="high_unique" />
              </el-select>
            </el-col>
            <el-col :span="8">
              <span style="color: #909399; font-size: 12px;">
                已选择 {{ selectedFeatures.length }} / {{ filteredColumns.length }} 列
              </span>
            </el-col>
          </el-row>
        </div>

        <!-- 特征列表 -->
        <div class="feature-list">
          <el-checkbox-group v-model="selectedFeatures" @change="onSelectionChange">
            <div class="feature-grid">
              <div
                v-for="column in filteredColumns"
                :key="column.name"
                class="feature-item"
                :class="{ 'recommended': isRecommended(column) }">
                <el-checkbox :label="column.name" class="feature-checkbox">
                  <div class="feature-info">
                    <div class="feature-header">
                      <i class="el-icon-data-line" style="color: #409EFF; margin-right: 5px;"></i>
                      <span class="feature-name">{{ column.name }}</span>
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
                    </div>
                    <div class="feature-stats">
                      <span class="stat-item">
                        <i class="el-icon-check"></i> {{ column.nonNullCount }}
                      </span>
                      <span class="stat-item" v-if="column.nullCount > 0">
                        <i class="el-icon-close" style="color: #F56C6C;"></i> {{ column.nullCount }}
                      </span>
                      <span class="stat-item">
                        <i class="el-icon-data-line"></i> {{ column.uniqueCount }} 唯一值
                      </span>
                    </div>
                    <div class="feature-sample" v-if="column.sampleValues && column.sampleValues.length > 0">
                      <span class="sample-label">示例：</span>
                      <span class="sample-values">{{ column.sampleValues.slice(0, 3).join(', ') }}</span>
                    </div>
                  </div>
                </el-checkbox>
              </div>
            </div>
          </el-checkbox-group>
        </div>

        <!-- 选择摘要 -->
        <div v-if="selectedFeatures.length > 0" class="selection-summary">
          <el-divider>选择摘要</el-divider>
          <el-row :gutter="20">
            <el-col :span="6">
              <el-statistic title="已选特征" :value="selectedFeatures.length" suffix="列" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="数值特征" :value="selectedNumericCount" suffix="列" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="分类特征" :value="selectedCategoricalCount" suffix="列" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="推荐特征" :value="selectedRecommendedCount" suffix="列" />
            </el-col>
          </el-row>
          
          <!-- 已选特征标签 -->
          <div style="margin-top: 15px;">
            <span style="color: #606266; font-size: 14px; margin-right: 10px;">已选特征：</span>
            <el-tag
              v-for="feature in selectedFeatures"
              :key="feature"
              closable
              size="small"
              style="margin-right: 8px; margin-bottom: 8px;"
              @close="removeFeature(feature)">
              {{ feature }}
            </el-tag>
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
  name: 'FeatureSelector',
  props: {
    columns: {
      type: Array,
      default: () => []
    },
    value: {
      type: Array,
      default: () => []
    },
    excludeColumns: {
      type: Array,
      default: () => []
    },
    compact: {
      type: Boolean,
      default: false
    },
    minFeatures: {
      type: Number,
      default: 1
    },
    maxFeatures: {
      type: Number,
      default: 50
    }
  },
  data() {
    return {
      selectedFeatures: [],
      showHelp: false,
      searchText: '',
      typeFilter: '',
      qualityFilter: '',
      recommendedFeatures: [],
      _isInitializing: true
    }
  },
  computed: {
    filteredColumns() {
      let filtered = this.columns.filter(col => 
        !this.excludeColumns.includes(col.name)
      )
      
      // 搜索过滤
      if (this.searchText) {
        filtered = filtered.filter(col =>
          col.name.toLowerCase().includes(this.searchText.toLowerCase())
        )
      }
      
      // 类型过滤
      if (this.typeFilter) {
        filtered = filtered.filter(col => {
          if (this.typeFilter === 'numeric') {
            return ['int64', 'float64', 'int32', 'float32', 'number'].includes(col.type)
          } else if (this.typeFilter === 'text') {
            return ['object', 'string', 'category'].includes(col.type)
          } else if (this.typeFilter === 'date') {
            return ['datetime64', 'datetime', 'date'].includes(col.type)
          }
          return true
        })
      }
      
      // 质量过滤
      if (this.qualityFilter) {
        filtered = filtered.filter(col => {
          if (this.qualityFilter === 'no_missing') {
            return col.nullCount === 0
          } else if (this.qualityFilter === 'has_missing') {
            return col.nullCount > 0
          } else if (this.qualityFilter === 'high_unique') {
            return col.uniqueCount > 10
          }
          return true
        })
      }
      
      return filtered
    },
    
    selectedNumericCount() {
      return this.selectedFeatures.filter(name => {
        const col = this.columns.find(c => c.name === name)
        return col && ['int64', 'float64', 'int32', 'float32', 'number'].includes(col.type)
      }).length
    },
    
    selectedCategoricalCount() {
      return this.selectedFeatures.filter(name => {
        const col = this.columns.find(c => c.name === name)
        return col && ['object', 'string', 'category'].includes(col.type)
      }).length
    },
    
    selectedRecommendedCount() {
      return this.selectedFeatures.filter(name => 
        this.recommendedFeatures.includes(name)
      ).length
    },
    
    validationMessage() {
      if (this.selectedFeatures.length === 0) {
        return {
          title: '请选择特征',
          description: '至少需要选择一个特征列作为模型输入',
          type: 'warning'
        }
      }
      
      if (this.selectedFeatures.length < this.minFeatures) {
        return {
          title: '特征数量不足',
          description: `至少需要选择 ${this.minFeatures} 个特征`,
          type: 'warning'
        }
      }
      
      if (this.selectedFeatures.length > this.maxFeatures) {
        return {
          title: '特征数量过多',
          description: `最多只能选择 ${this.maxFeatures} 个特征`,
          type: 'error'
        }
      }
      
      if (this.selectedNumericCount === 0) {
        return {
          title: '建议包含数值特征',
          description: '大多数机器学习算法需要至少一个数值特征',
          type: 'info'
        }
      }
      
      return null
    }
  },
  watch: {
    value: {
      handler(newValue) {
        this.selectedFeatures = [...(newValue || [])]
        // 只在初始化时触发change事件，避免无限循环
        if (this._isInitializing !== false) {
          this._isInitializing = false
          this.$nextTick(() => {
            this.onSelectionChange()
          })
        }
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
    /** 选择变化处理 */
    onSelectionChange() {
      this.$emit('input', this.selectedFeatures)
      this.$emit('change', {
        features: this.selectedFeatures,
        numericCount: this.selectedNumericCount,
        categoricalCount: this.selectedCategoricalCount,
        isValid: this.selectedFeatures.length > 0 && (!this.validationMessage || this.validationMessage.type !== 'error')
      })
    },
    
    /** 全选 */
    selectAll() {
      this.selectedFeatures = this.filteredColumns.map(col => col.name)
      this.onSelectionChange()
    },
    
    /** 清空 */
    clearAll() {
      this.selectedFeatures = []
      this.onSelectionChange()
    },
    
    /** 选择数值列 */
    selectNumeric() {
      this.selectedFeatures = this.filteredColumns
        .filter(col => ['int64', 'float64', 'int32', 'float32', 'number'].includes(col.type))
        .map(col => col.name)
      this.onSelectionChange()
    },
    
    /** 智能推荐 */
    selectRecommended() {
      this.selectedFeatures = [...this.recommendedFeatures]
      this.onSelectionChange()
    },
    
    /** 移除特征 */
    removeFeature(feature) {
      const index = this.selectedFeatures.indexOf(feature)
      if (index > -1) {
        this.selectedFeatures.splice(index, 1)
        this.onSelectionChange()
      }
    },
    
    /** 生成推荐 */
    generateRecommendations() {
      if (!this.columns || this.columns.length === 0) return
      
      const recommendations = []
      
      // 优先推荐数值列
      const numericCols = this.columns.filter(col => 
        ['int64', 'float64', 'int32', 'float32', 'number'].includes(col.type) &&
        col.nullCount === 0 && // 无缺失值
        col.uniqueCount > 5 && // 有足够的变化
        !this.excludeColumns.includes(col.name)
      )
      
      // 按唯一值数量排序，选择前几个
      numericCols.sort((a, b) => b.uniqueCount - a.uniqueCount)
      recommendations.push(...numericCols.slice(0, Math.min(5, numericCols.length)).map(col => col.name))
      
      // 推荐一些高质量的分类特征
      const categoricalCols = this.columns.filter(col => 
        ['object', 'string', 'category'].includes(col.type) &&
        col.nullCount === 0 &&
        col.uniqueCount > 1 && col.uniqueCount < 20 && // 合理的分类数量
        !this.excludeColumns.includes(col.name)
      )
      
      recommendations.push(...categoricalCols.slice(0, 2).map(col => col.name))
      
      this.recommendedFeatures = recommendations
    },
    
    /** 是否为推荐特征 */
    isRecommended(column) {
      return this.recommendedFeatures.includes(column.name)
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
.feature-selector {
  width: 100%;
}

.selection-toolbar {
  margin-bottom: 15px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 15px;
  margin-top: 10px;
}

.feature-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px;
  transition: all 0.3s;
}

.feature-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.feature-item.recommended {
  border-color: #67c23a;
  background-color: #f0f9ff;
}

.feature-checkbox {
  width: 100%;
}

.feature-info {
  margin-left: 20px;
}

.feature-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.feature-name {
  font-weight: 500;
  color: #303133;
}

.feature-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #909399;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 2px;
}

.feature-sample {
  font-size: 12px;
  color: #606266;
}

.sample-label {
  color: #909399;
}

.sample-values {
  font-family: monospace;
}

.selection-summary {
  margin-top: 20px;
  padding: 15px;
  background-color: #fafafa;
  border-radius: 6px;
}

.validation-message {
  margin-top: 15px;
}
</style>
