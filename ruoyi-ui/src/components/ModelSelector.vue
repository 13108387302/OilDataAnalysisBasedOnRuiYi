<template>
  <div class="model-selector">
    <el-card>
      <div slot="header">
        <span>模型选择</span>
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
          title="模型选择指南"
          type="info"
          :closable="false"
          style="margin-bottom: 20px;">
          <div slot="description">
            <p><strong>选择合适的机器学习算法</strong>对预测性能至关重要：</p>
            <ul>
              <li><strong>回归算法</strong>：用于预测连续数值，如线性回归、随机森林回归、XGBoost等</li>
              <li><strong>分类算法</strong>：用于预测离散类别，如逻辑回归、SVM、神经网络等</li>
              <li><strong>数据规模</strong>：小数据集适合简单模型，大数据集可以使用复杂模型</li>
              <li><strong>解释性</strong>：线性模型易解释，树模型和神经网络较复杂但性能更好</li>
            </ul>
          </div>
        </el-alert>
      </el-collapse-transition>

      <!-- 模型选择模式 -->
      <div class="selection-mode">
        <h4>选择模式</h4>
        <el-radio-group v-model="selectionMode" @change="onSelectionModeChange">
          <el-radio-button label="existing">使用已有模型</el-radio-button>
          <el-radio-button label="upload">上传模型文件</el-radio-button>
        </el-radio-group>
        <div class="mode-description" style="margin-top: 10px;">
          <p v-if="selectionMode === 'existing'" class="description-text">
            <i class="el-icon-folder-opened"></i>
            从系统中选择已保存的训练好的模型进行预测
          </p>
          <p v-if="selectionMode === 'upload'" class="description-text">
            <i class="el-icon-upload"></i>
            上传您自己训练好的模型文件（支持 .pkl、.joblib、.model、.h5 格式）
          </p>
        </div>
      </div>

      <!-- 已有模型选择 -->
      <div v-if="selectionMode === 'existing'" class="existing-models">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
          <h4 style="margin: 0;">已有模型</h4>
          <el-button
            size="small"
            icon="el-icon-refresh"
            @click="loadModels"
            :loading="loading">
            刷新
          </el-button>
        </div>
        <div v-if="loading" style="text-align: center; padding: 20px;">
          <el-icon class="is-loading"><i class="el-icon-loading"></i></el-icon>
          <p>加载模型列表...</p>
        </div>
        <div v-else-if="availableModels.length === 0" style="text-align: center; padding: 20px;">
          <i class="el-icon-box" style="font-size: 48px; color: #C0C4CC;"></i>
          <p style="color: #909399; margin-top: 10px;">暂无可用模型</p>
          <div style="margin-top: 15px;">
            <el-button type="primary" @click="selectionMode = 'upload'">上传模型文件</el-button>
            <el-button @click="loadModels" :loading="loading">重新加载</el-button>
          </div>
        </div>
        <div v-else>
          <!-- 调试信息 -->
          <div v-if="isDevelopmentMode" style="background: #f5f5f5; padding: 10px; margin-bottom: 15px; font-size: 12px;">
            <strong>调试信息:</strong><br>
            总模型数: {{ availableModels.length }}<br>
            过滤后模型数: {{ filteredModels.length }}<br>
            当前选择: {{ selectedModel }}<br>
            任务类型: {{ taskType }}
          </div>

          <el-radio-group v-model="selectedModel" @change="onModelChange" class="model-list">
            <div class="model-grid">
              <div
                v-for="model in filteredModels"
                :key="model.id"
                class="model-item"
                :class="{ 'selected': selectedModel === model.id }">
                <el-radio :label="model.id" class="model-radio">
                  <div class="model-info">
                    <div class="model-header">
                      <span class="model-name">{{ model.modelName }}</span>
                      <el-tag 
                        :type="getModelTypeColor(model.modelType)" 
                        size="mini"
                        style="margin-left: 8px;">
                        {{ getModelTypeName(model.modelType) }}
                      </el-tag>
                      <el-tag
                        :type="model.sourceTaskId ? 'success' : 'primary'"
                        size="mini"
                        style="margin-left: 5px;">
                        {{ model.sourceTaskId ? '训练生成' : '用户上传' }}
                      </el-tag>
                      <!-- 兼容性警告标签 -->
                      <el-tag
                        v-if="!isModelCompatible(model)"
                        type="warning"
                        size="mini"
                        style="margin-left: 4px;">
                        类型不匹配
                      </el-tag>
                    </div>
                    <div class="model-details">
                      <p><strong>算法：</strong>{{ model.algorithm }}</p>
                      <p><strong>创建时间：</strong>{{ formatDate(model.createTime) }}</p>
                      <p v-if="model.description"><strong>描述：</strong>{{ model.description }}</p>
                      <!-- 兼容性警告信息 -->
                      <div v-if="!isModelCompatible(model)" class="compatibility-warning">
                        <el-alert
                          :title="getCompatibilityWarning(model).message"
                          type="warning"
                          size="small"
                          :closable="false"
                          show-icon
                          style="margin-top: 8px;">
                        </el-alert>
                      </div>
                    </div>
                  </div>
                </el-radio>
              </div>
            </div>
          </el-radio-group>
        </div>
      </div>



      <!-- 模型上传 -->
      <div v-if="selectionMode === 'upload'" class="model-upload">
        <h4>上传模型文件</h4>
        <el-upload
          ref="modelUpload"
          action="#"
          :before-upload="beforeModelUpload"
          :on-change="handleModelFileChange"
          :auto-upload="false"
          accept=".pkl,.joblib,.model,.h5"
          :limit="1"
          drag>
          <i class="el-icon-upload"></i>
          <div class="el-upload__text">将模型文件拖到此处，或<em>点击上传</em></div>
          <div class="el-upload__tip" slot="tip">
            支持 pkl、joblib、model、h5 格式，最大500MB
          </div>
        </el-upload>
        
        <el-form v-if="uploadedModelFile" :model="uploadModelForm" label-width="100px" style="margin-top: 20px;">
          <el-form-item label="模型名称" required>
            <el-input
              v-model="uploadModelForm.modelName"
              placeholder="请输入模型名称"
              @input="onUploadFormChange" />
          </el-form-item>
          <el-form-item label="算法类型" required>
            <el-select
              v-model="uploadModelForm.algorithm"
              placeholder="选择算法类型"
              @change="onUploadFormChange">
              <el-option label="线性回归" value="linear_regression" />
              <el-option label="随机森林" value="random_forest" />
              <el-option label="XGBoost" value="xgboost" />
              <el-option label="神经网络" value="neural_network" />
              <el-option label="Sklearn模型" value="sklearn_model" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="模型描述">
            <el-input
              v-model="uploadModelForm.description"
              type="textarea"
              :rows="2"
              placeholder="模型的描述信息（可选）"
              @input="onUploadFormChange" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 选择摘要 -->
      <div v-if="hasSelection" class="selection-summary">
        <el-divider>选择摘要</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="选择模式">{{ getSelectionModeLabel() }}</el-descriptions-item>
          <el-descriptions-item label="模型/算法">{{ getSelectedLabel() }}</el-descriptions-item>
          <el-descriptions-item v-if="selectionMode === 'existing'" label="模型类型">
            {{ getSelectedModelType() }}
          </el-descriptions-item>
        </el-descriptions>
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
    </el-card>
  </div>
</template>

<script>
import { listModel } from '@/api/petrol/model'

export default {
  name: 'ModelSelector',
  props: {
    value: {
      type: Object,
      default: () => ({})
    },
    taskType: {
      type: String,
      default: 'auto'
    },
    featureCount: {
      type: Number,
      default: 0
    },
    compact: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      selectionMode: 'existing',
      selectedModel: null,
      uploadedModelFile: null,
      showHelp: false,
      loading: false,
      availableModels: [],

      uploadModelForm: {
        modelName: '',
        algorithm: '',
        description: ''
      }
    }
  },
  computed: {
    filteredModels() {
      const models = Array.isArray(this.availableModels) ? this.availableModels : []
      console.log('开始过滤模型，原始模型数量:', models.length, '任务类型:', this.taskType)

      // 移除任务类型过滤，只检查模型的基本有效性
      const filtered = models.filter(model => {
        if (!model || !model.id) {
          console.warn('过滤掉无效模型:', model)
          return false
        }

        // 检查模型状态
        if (model.status !== 'ACTIVE') {
          console.warn('过滤掉非活跃模型:', model.modelName, '状态:', model.status)
          return false
        }

        // 检查可用性
        if (model.isAvailable !== 'Y') {
          console.warn('过滤掉不可用模型:', model.modelName, '可用性:', model.isAvailable)
          return false
        }

        // 显示所有状态正常的模型，不再根据任务类型过滤
        return true
      })

      console.log('过滤后的模型数量:', filtered.length)
      console.log('过滤后的模型列表:', filtered.map(m => ({
        id: m.id,
        name: m.modelName,
        type: m.modelType,
        status: m.status,
        compatible: this.isModelCompatible(m)
      })))

      return filtered
    },
    
    hasSelection() {
      return (this.selectionMode === 'existing' && this.selectedModel) ||
             (this.selectionMode === 'upload' && this.uploadedModelFile)
    },

    /** 是否为开发模式 */
    isDevelopmentMode() {
      return process.env.NODE_ENV === 'development' || process.env.VUE_APP_ENV === 'development'
    },
    
    validationMessage() {
      if (!this.hasSelection) {
        return {
          title: '请选择模型',
          description: '需要选择一个模型或算法来进行预测',
          type: 'warning'
        }
      }

      if (this.selectionMode === 'existing' && this.selectedModel) {
        const model = this.availableModels.find(m => m.id === this.selectedModel)
        if (!model) {
          return {
            title: '模型不存在',
            description: '选择的模型在系统中不存在',
            type: 'error'
          }
        }
        if (model.status !== 'ACTIVE' || model.isAvailable !== 'Y') {
          return {
            title: '模型不可用',
            description: '选择的模型当前不可用',
            type: 'error'
          }
        }

        // 兼容性警告（不阻止选择，只是提醒）
        if (!this.isModelCompatible(model)) {
          return {
            title: '模型类型提醒',
            description: `所选模型类型(${this.getModelTypeName(model.modelType)})与当前任务类型(${this.getTaskTypeName(this.taskType)})不匹配，但仍可继续使用`,
            type: 'info'
          }
        }
      }

      if (this.selectionMode === 'upload' && this.uploadedModelFile) {
        if (!this.uploadModelForm.modelName) {
          return {
            title: '请填写模型名称',
            description: '上传模型需要提供模型名称',
            type: 'error'
          }
        }
        if (!this.uploadModelForm.algorithm) {
          return {
            title: '请选择算法类型',
            description: '需要指定模型的算法类型',
            type: 'error'
          }
        }
      }

      return null
    }
  },
  watch: {
    taskType: {
      handler(newType) {
        // 简化后的模型选择不需要复杂的推荐逻辑
        console.log('任务类型变化:', newType)
      },
      immediate: true
    }
  },
  created() {
    this.loadModels()
  },
  methods: {
    /** 加载模型列表 */
    async loadModels() {
      this.loading = true
      console.log('开始加载模型列表...')

      try {
        const response = await listModel({
          pageNum: 1,
          pageSize: 100,
          status: 'ACTIVE',
          isAvailable: 'Y'
        })

        console.log('模型列表API响应:', response)

        if (response && response.code === 200) {
          const models = response.rows || response.data || []
          this.availableModels = Array.isArray(models) ? models : []

          console.log('加载到的模型数量:', this.availableModels.length)
          console.log('模型列表详情:', this.availableModels)

          // 验证模型数据完整性
          this.availableModels = this.availableModels.filter(model => {
            if (!model || !model.id) {
              console.warn('发现无效模型数据:', model)
              return false
            }
            return true
          })

          // 按创建时间倒序排列，最新的模型在前面
          this.availableModels.sort((a, b) => {
            const timeA = new Date(a.createTime || 0).getTime()
            const timeB = new Date(b.createTime || 0).getTime()
            return timeB - timeA
          })

          // 统计不同来源的模型
          const taskModels = this.availableModels.filter(m => m.sourceTaskId)
          const uploadModels = this.availableModels.filter(m => !m.sourceTaskId)

          console.log(`模型统计: 总计${this.availableModels.length}个, 任务生成${taskModels.length}个, 手动上传${uploadModels.length}个`)
          console.log('过滤后的模型列表:', this.filteredModels)

          if (this.availableModels.length === 0) {
            this.$message.warning('暂无可用模型，请先执行分析任务生成模型或上传模型文件')
          } else {
            this.$message.success(`成功加载${this.availableModels.length}个可用模型`)

            // 如果只有一个模型，自动选择它
            if (this.availableModels.length === 1 && !this.selectedModel) {
              this.selectedModel = this.availableModels[0].id
              console.log('自动选择唯一模型:', this.availableModels[0])
              this.onModelChange()
            }
          }
        } else {
          throw new Error(response?.msg || response?.message || '服务器响应异常')
        }
      } catch (error) {
        console.error('加载模型列表失败:', error)
        this.$message.error('加载模型列表失败: ' + (error.message || '网络错误'))
        this.availableModels = []
      } finally {
        this.loading = false
      }
    },
    
    /** 选择模式变化 */
    onSelectionModeChange() {
      this.selectedModel = null
      this.selectedAlgorithm = null
      this.uploadedModelFile = null

      if (this.selectionMode === 'existing') {
        this.loadModels()
      }

      this.emitChange()
    },
    
    /** 模型选择变化 */
    onModelChange() {
      this.emitChange()
    },
    

    
    /** 模型文件上传前检查 */
    beforeModelUpload(file) {
      const validTypes = ['pkl', 'joblib', 'model', 'h5']
      const isValidType = validTypes.some(ext => 
        file.name.toLowerCase().endsWith('.' + ext)
      )
      const isLt500M = file.size / 1024 / 1024 < 500

      if (!isValidType) {
        this.$message.error('只能上传 pkl、joblib、model、h5 格式的文件!')
      }
      if (!isLt500M) {
        this.$message.error('文件大小不能超过 500MB!')
      }
      return false // 阻止自动上传
    },
    
    /** 模型文件变化 */
    handleModelFileChange(file) {
      this.uploadedModelFile = file.raw
      this.uploadModelForm.modelName = file.name.split('.')[0]

      // 根据文件扩展名推测算法类型
      const extension = file.name.split('.').pop().toLowerCase()
      if (extension === 'pkl' || extension === 'joblib') {
        this.uploadModelForm.algorithm = 'sklearn_model'
      } else if (extension === 'h5') {
        this.uploadModelForm.algorithm = 'neural_network'
      }

      this.emitChange()
    },

    /** 模型上传表单变化 */
    onUploadFormChange() {
      this.emitChange()
    },


    
    /** 检查模型与任务类型的兼容性 */
    isModelCompatible(model) {
      if (!model || !model.modelType || this.taskType === 'auto') {
        return true // 如果没有类型信息或任务类型为auto，认为兼容
      }

      return model.modelType === this.taskType || model.modelType === 'auto'
    },

    /** 获取模型兼容性提示 */
    getCompatibilityWarning(model) {
      if (!this.isModelCompatible(model)) {
        return {
          show: true,
          message: `此模型类型(${this.getModelTypeName(model.modelType)})与当前任务类型(${this.getTaskTypeName(this.taskType)})可能不匹配`,
          type: 'warning'
        }
      }
      return { show: false }
    },

    /** 获取任务类型名称 */
    getTaskTypeName(taskType) {
      const typeNames = {
        'regression': '回归任务',
        'classification': '分类任务',
        'auto': '自动选择'
      }
      return typeNames[taskType] || taskType
    },

    /** 发出变化事件 */
    emitChange() {
      const selection = {
        mode: this.selectionMode,
        isValid: !this.validationMessage || this.validationMessage.type !== 'error'
      }

      if (this.selectionMode === 'existing' && this.selectedModel) {
        const model = this.availableModels.find(m => m && (m.id == this.selectedModel || m.id === this.selectedModel))
        selection.model = model

        // 添加兼容性信息
        if (model) {
          selection.compatibility = {
            isCompatible: this.isModelCompatible(model),
            warning: this.getCompatibilityWarning(model)
          }
        }
      } else if (this.selectionMode === 'upload' && this.uploadedModelFile) {
        selection.uploadFile = this.uploadedModelFile
        selection.uploadForm = this.uploadModelForm
      }

      this.$emit('input', selection)
      this.$emit('change', selection)
    },
    
    /** 获取选择模式标签 */
    getSelectionModeLabel() {
      const labels = {
        'existing': '使用已有模型',
        'upload': '上传模型文件'
      }
      return labels[this.selectionMode] || ''
    },
    
    /** 获取选择标签 */
    getSelectedLabel() {
      if (this.selectionMode === 'existing' && this.selectedModel) {
        const model = this.availableModels.find(m => m.id === this.selectedModel)
        return model ? model.modelName : ''
      } else if (this.selectionMode === 'upload' && this.uploadedModelFile) {
        return this.uploadModelForm.modelName || this.uploadedModelFile.name
      }
      return ''
    },
    
    /** 获取选择的模型类型 */
    getSelectedModelType() {
      if (this.selectedModel) {
        const model = this.availableModels.find(m => m.id === this.selectedModel)
        return model ? this.getModelTypeName(model.modelType) : ''
      }
      return ''
    },
    
    /** 获取选择的任务类型 */
    getSelectedTaskType() {
      return ''
    },
    
    /** 获取模型类型颜色 */
    getModelTypeColor(type) {
      const colorMap = {
        'regression': 'primary',
        'classification': 'success',
        'clustering': 'warning'
      }
      return colorMap[type] || 'info'
    },
    
    /** 获取模型类型名称 */
    getModelTypeName(type) {
      const nameMap = {
        'regression': '回归',
        'classification': '分类',
        'clustering': '聚类'
      }
      return nameMap[type] || type
    },
    

    
    /** 格式化日期 */
    formatDate(dateString) {
      if (!dateString) return ''
      return new Date(dateString).toLocaleDateString()
    }
  }
}
</script>

<style scoped>
.model-selector {
  width: 100%;
}

.selection-mode {
  margin-bottom: 25px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 6px;
}

.model-grid, .algorithm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 15px;
  margin-top: 15px;
}

.model-item, .algorithm-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 15px;
  transition: all 0.3s;
}

.model-item:hover, .algorithm-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.model-item.selected, .algorithm-item.selected {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.algorithm-item.recommended {
  border-color: #67c23a;
  background-color: #f0f9ff;
}

.model-radio, .algorithm-radio {
  width: 100%;
}

.model-info, .algorithm-info {
  margin-left: 20px;
}

.model-header, .algorithm-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.model-name, .algorithm-name {
  font-weight: 500;
  color: #303133;
}

.model-details, .algorithm-details {
  font-size: 14px;
  color: #606266;
}

.algorithm-desc {
  margin-bottom: 10px;
  line-height: 1.4;
}

.algorithm-features {
  margin-bottom: 8px;
}

.algorithm-metrics {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #909399;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 3px;
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

h4 {
  margin: 15px 0 10px 0;
  color: #303133;
}
</style>
