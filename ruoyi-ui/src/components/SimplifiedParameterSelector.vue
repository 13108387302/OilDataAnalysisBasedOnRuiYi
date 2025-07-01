<template>
  <div class="simplified-parameter-selector">
    <!-- 算法未选择提示 -->
    <div v-if="!algorithm" class="no-algorithm-tip">
      <el-alert
        title="请先选择算法"
        description="选择算法后将显示相应的参数配置选项"
        type="info"
        :closable="false"
        show-icon>
      </el-alert>
    </div>

    <!-- 参数配置内容 -->
    <div v-else>
      <!-- 参数配置级别选择 -->
      <div class="parameter-level-selector">
        <h4 style="margin-bottom: 15px;">
          <i class="el-icon-setting"></i> 参数配置
        </h4>

        <el-radio-group v-model="selectedLevel" @change="handleLevelChange" class="level-radio-group">
          <el-radio-button label="simple">
            <i class="el-icon-magic-stick"></i> 简单模式
          </el-radio-button>
          <el-radio-button label="standard">
            <i class="el-icon-s-tools"></i> 标准模式
          </el-radio-button>
          <el-radio-button label="advanced">
            <i class="el-icon-s-operation"></i> 高级模式
          </el-radio-button>
        </el-radio-group>

        <div class="level-description">
          {{ getLevelDescription() }}
        </div>
      </div>

    <!-- 参数预设模板 -->
    <div v-if="availablePresets.length > 0" class="parameter-presets">
      <h5 style="margin: 20px 0 10px 0;">
        <i class="el-icon-collection"></i> 推荐配置
      </h5>
      
      <el-row :gutter="15">
        <el-col :span="8" v-for="preset in availablePresets" :key="preset.key">
          <el-card 
            class="preset-card" 
            :class="{ 'selected': selectedPreset === preset.key }"
            @click.native="selectPreset(preset.key)"
            shadow="hover">
            <div class="preset-header">
              <el-tag :type="preset.type" size="mini">{{ preset.label }}</el-tag>
              <el-button 
                v-if="selectedPreset === preset.key"
                type="primary" 
                size="mini" 
                icon="el-icon-check"
                circle
                class="selected-icon">
              </el-button>
            </div>
            <div class="preset-description">{{ preset.description }}</div>
            <div class="preset-params">
              <div v-for="(value, key) in preset.params" :key="key" class="param-item">
                <span class="param-name">{{ getParameterLabel(key) }}:</span>
                <span class="param-value">{{ formatParameterValue(key, value) }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 参数详细配置 -->
    <div v-if="selectedLevel !== 'simple'" class="parameter-details">
      <h5 style="margin: 20px 0 10px 0;">
        <i class="el-icon-edit"></i> 参数调整
      </h5>
      
      <el-row :gutter="20">
        <el-col :span="12" v-for="param in visibleParameters" :key="param.key">
          <el-form-item :label="param.label" class="simplified-form-item">
            <!-- 数值输入 -->
            <el-input-number
              v-if="param.type === 'number'"
              v-model="parameters[param.key]"
              :min="param.min"
              :max="param.max"
              :step="param.step || 1"
              :precision="param.precision || 0"
              size="small"
              style="width: 100%;"
              @change="handleParameterChange(param.key, $event)">
            </el-input-number>
            
            <!-- 选择器 -->
            <el-select
              v-else-if="param.type === 'select_option'"
              v-model="parameters[param.key]"
              size="small"
              style="width: 100%;"
              @change="handleParameterChange(param.key, $event)">
              <el-option
                v-for="option in param.options"
                :key="option.value || option"
                :label="option.label || option"
                :value="option.value || option">
              </el-option>
            </el-select>
            
            <!-- 列选择器 -->
            <el-select
              v-else-if="param.type === 'select_column'"
              v-model="parameters[param.key]"
              size="small"
              style="width: 100%;"
              filterable
              @change="handleParameterChange(param.key, $event)">
              <el-option
                v-for="column in availableColumns"
                :key="column"
                :label="column"
                :value="column">
              </el-option>
            </el-select>
            
            <!-- 多列选择器 -->
            <el-select
              v-else-if="param.type === 'multi_select_column'"
              v-model="parameters[param.key]"
              multiple
              size="small"
              style="width: 100%;"
              filterable
              @change="handleParameterChange(param.key, $event)">
              <el-option
                v-for="column in availableColumns"
                :key="column"
                :label="column"
                :value="column">
              </el-option>
            </el-select>
            
            <!-- 开关 -->
            <el-switch
              v-else-if="param.type === 'boolean'"
              v-model="parameters[param.key]"
              @change="handleParameterChange(param.key, $event)">
            </el-switch>
            
            <!-- 文本输入 -->
            <el-input
              v-else
              v-model="parameters[param.key]"
              size="small"
              @change="handleParameterChange(param.key, $event)">
            </el-input>
            
            <!-- 参数提示 -->
            <div v-if="param.tips" class="param-tips">
              <i class="el-icon-info"></i> {{ param.tips }}
            </div>
          </el-form-item>
        </el-col>
      </el-row>
    </div>

      <!-- 操作按钮 -->
      <div class="parameter-actions">
        <el-button @click="resetToDefaults" size="small">
          <i class="el-icon-refresh"></i> 重置默认
        </el-button>
        <el-button type="primary" @click="applyParameters" size="small">
          <i class="el-icon-check"></i> 应用配置
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { getAlgorithmPresets, recommendPreset } from '@/utils/algorithmPresets'

export default {
  name: 'SimplifiedParameterSelector',
  props: {
    algorithm: {
      type: String,
      required: false,
      default: ''
    },
    taskType: {
      type: String,
      required: false,
      default: ''
    },
    algorithmParams: {
      type: Array,
      default: () => []
    },
    availableColumns: {
      type: Array,
      default: () => []
    },
    value: {
      type: Object,
      default: () => ({})
    },
    dataInfo: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      selectedLevel: 'simple',
      selectedPreset: null,
      parameters: {}
    }
  },
  computed: {
    availablePresets() {
      if (!this.algorithm) return []
      return this.getPresetsForAlgorithm(this.algorithm, this.selectedLevel)
    },
    visibleParameters() {
      if (this.selectedLevel === 'simple') return []
      if (this.selectedLevel === 'standard') {
        return this.algorithmParams.filter(p => !p.advanced)
      }
      return this.algorithmParams
    }
  },
  watch: {
    algorithm: {
      immediate: true,
      handler() {
        this.initializeParameters()
      }
    },
    value: {
      immediate: true,
      deep: true,
      handler(newVal) {
        this.parameters = { ...newVal }
      }
    }
  },
  methods: {
    initializeParameters() {
      // 如果没有算法，直接返回
      if (!this.algorithm) {
        this.parameters = { ...this.value }
        return
      }

      // 初始化参数为默认值
      const defaultParams = {}
      this.algorithmParams.forEach(param => {
        defaultParams[param.key] = param.defaultValue
      })
      this.parameters = { ...defaultParams, ...this.value }

      // 自动选择第一个预设
      if (this.availablePresets.length > 0) {
        this.selectedPreset = this.availablePresets[0].key
        this.selectPreset(this.selectedPreset)
      }
    },
    
    handleLevelChange(level) {
      this.selectedLevel = level
      if (level === 'simple' && this.availablePresets.length > 0) {
        this.selectPreset(this.availablePresets[0].key)
      }
    },
    
    selectPreset(presetKey) {
      this.selectedPreset = presetKey
      const preset = this.availablePresets.find(p => p.key === presetKey)
      if (preset) {
        this.parameters = { ...this.parameters, ...preset.params }
        this.emitChange()
      }
    },
    
    handleParameterChange(key, value) {
      this.parameters[key] = value
      this.selectedPreset = null // 手动修改后取消预设选择
      this.emitChange()
    },
    
    resetToDefaults() {
      this.initializeParameters()
      this.emitChange()
    },
    
    applyParameters() {
      this.$emit('apply', this.parameters)
    },
    
    emitChange() {
      this.$emit('input', this.parameters)
      this.$emit('change', this.parameters)
    },
    
    getLevelDescription() {
      const descriptions = {
        simple: '使用推荐的参数配置，适合快速开始',
        standard: '显示常用参数，适合一般使用场景',
        advanced: '显示所有参数，适合专业用户精细调优'
      }
      return descriptions[this.selectedLevel]
    },
    
    getParameterLabel(key) {
      const param = this.algorithmParams.find(p => p.key === key)
      return param ? param.label : key
    },
    
    formatParameterValue(key, value) {
      if (Array.isArray(value)) {
        return value.length > 3 ? `${value.slice(0, 3).join(', ')}...` : value.join(', ')
      }
      return String(value)
    },
    
    getPresetsForAlgorithm(algorithm, level) {
      if (!algorithm) return []
      return getAlgorithmPresets(algorithm, level)
    },

    getRecommendedPreset() {
      if (!this.algorithm) return null
      return recommendPreset(this.algorithm, this.dataInfo)
    }
  }
}
</script>

<style scoped>
.simplified-parameter-selector {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.level-radio-group {
  margin-bottom: 10px;
}

.level-description {
  color: #666;
  font-size: 13px;
  margin-top: 8px;
}

.preset-card {
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 15px;
}

.preset-card:hover {
  transform: translateY(-2px);
}

.preset-card.selected {
  border-color: #409EFF;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.3);
}

.preset-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.selected-icon {
  width: 20px;
  height: 20px;
}

.preset-description {
  color: #666;
  font-size: 12px;
  margin-bottom: 10px;
}

.preset-params {
  font-size: 11px;
}

.param-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 3px;
}

.param-name {
  color: #999;
}

.param-value {
  color: #333;
  font-weight: 500;
}

.simplified-form-item {
  margin-bottom: 15px;
}

.param-tips {
  color: #999;
  font-size: 11px;
  margin-top: 5px;
}

.parameter-actions {
  text-align: center;
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.no-algorithm-tip {
  padding: 20px;
  text-align: center;
}
</style>
