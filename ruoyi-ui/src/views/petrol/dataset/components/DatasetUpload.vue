<template>
  <el-dialog
    title="上传数据集"
    :visible.sync="dialogVisible"
    width="600px"
    :before-close="handleClose"
    append-to-body
  >
    <el-form ref="uploadForm" :model="uploadForm" :rules="uploadRules" label-width="100px">
      <el-form-item label="数据集名称" prop="datasetName">
        <el-input 
          v-model="uploadForm.datasetName" 
          placeholder="请输入数据集名称"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>
      
      <el-form-item label="数据集描述" prop="description">
        <el-input 
          v-model="uploadForm.description" 
          type="textarea" 
          :rows="3"
          placeholder="请输入数据集描述"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
      
      <el-form-item label="数据集类别" prop="category">
        <el-select v-model="uploadForm.category" placeholder="请选择数据集类别" style="width: 100%">
          <el-option label="测井" value="测井" />
          <el-option label="地震" value="地震" />
          <el-option label="生产" value="生产" />
          <el-option label="地质" value="地质" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="数据文件" prop="file">
        <el-upload
          ref="upload"
          :action="uploadUrl"
          :headers="uploadHeaders"
          :data="uploadData"
          :file-list="fileList"
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :before-upload="beforeUpload"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          accept=".csv,.xlsx,.xls"
          drag
          :limit="1"
        >
          <i class="el-icon-upload"></i>
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
          <div class="el-upload__tip" slot="tip">
            支持 CSV、XLSX、XLS 格式，文件大小不超过100MB
          </div>
        </el-upload>
      </el-form-item>
      
      <!-- 文件验证结果 -->
      <el-form-item v-if="validationResult" label="文件验证">
        <el-alert
          :title="validationResult.message"
          :type="validationResult.valid ? 'success' : 'error'"
          :closable="false"
          show-icon
        />
      </el-form-item>
      
      <!-- 上传进度 -->
      <el-form-item v-if="uploading" label="上传进度">
        <el-progress
          :percentage="Math.round(uploadProgress)"
          :status="uploadStatus === 'success' ? 'success' : (uploadStatus === 'exception' ? 'exception' : null)"
          :stroke-width="8"
        />
      </el-form-item>
    </el-form>
    
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">取 消</el-button>
      <el-button 
        type="primary" 
        @click="handleUpload"
        :loading="uploading"
        :disabled="!canUpload"
      >
        {{ uploading ? '上传中...' : '确认上传' }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { uploadDataset, validateDatasetFile } from '@/api/petrol/dataset'
import { getToken } from '@/utils/auth'

export default {
  name: 'DatasetUpload',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      dialogVisible: false,
      uploading: false,
      uploadProgress: 0,
      uploadStatus: '',
      validationResult: null,
      fileList: [],
      uploadForm: {
        datasetName: '',
        description: '',
        category: '测井',
        file: null
      },
      uploadRules: {
        datasetName: [
          { required: true, message: '请输入数据集名称', trigger: 'blur' },
          { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
        ],
        category: [
          { required: true, message: '请选择数据集类别', trigger: 'change' }
        ],
        file: [
          { required: true, message: '请选择要上传的文件', trigger: 'change' }
        ]
      },
      uploadUrl: process.env.VUE_APP_BASE_API + '/petrol/dataset/upload',
      uploadHeaders: {
        Authorization: 'Bearer ' + getToken()
      }
    }
  },
  computed: {
    uploadData() {
      return {
        datasetName: this.uploadForm.datasetName,
        description: this.uploadForm.description,
        category: this.uploadForm.category
      }
    },
    canUpload() {
      return this.uploadForm.datasetName && 
             this.uploadForm.category && 
             this.fileList.length > 0 && 
             this.validationResult && 
             this.validationResult.valid &&
             !this.uploading
    }
  },
  watch: {
    visible(val) {
      this.dialogVisible = val
      if (val) {
        this.resetForm()
      }
    },
    dialogVisible(val) {
      this.$emit('update:visible', val)
    }
  },
  methods: {
    resetForm() {
      this.uploadForm = {
        datasetName: '',
        description: '',
        category: '测井',
        file: null
      }
      this.fileList = []
      this.validationResult = null
      this.uploading = false
      this.uploadProgress = 0
      this.uploadStatus = ''
      
      if (this.$refs.uploadForm) {
        this.$refs.uploadForm.clearValidate()
      }
      if (this.$refs.upload) {
        this.$refs.upload.clearFiles()
      }
    },
    
    handleFileChange(file, fileList) {
      this.fileList = fileList
      this.uploadForm.file = file.raw
      
      // 验证文件
      if (file.raw) {
        this.validateFile(file.raw)
      }
      
      // 触发表单验证
      this.$refs.uploadForm.validateField('file')
    },
    
    handleFileRemove(file, fileList) {
      this.fileList = fileList
      this.uploadForm.file = null
      this.validationResult = null
    },
    
    async validateFile(file) {
      try {
        const formData = new FormData()
        formData.append('file', file)
        
        const response = await validateDatasetFile(formData)
        this.validationResult = response.data
        
        if (this.validationResult.valid) {
          this.$message.success('文件验证通过')
        } else {
          this.$message.error(this.validationResult.message)
        }
      } catch (error) {
        console.error('文件验证失败:', error)
        this.validationResult = {
          valid: false,
          message: '文件验证失败，请重试'
        }
      }
    },
    
    beforeUpload(file) {
      // 这里可以添加额外的上传前检查
      return true
    },
    
    handleUpload() {
      this.$refs.uploadForm.validate(async (valid) => {
        if (!valid) {
          return false
        }
        
        if (!this.canUpload) {
          this.$message.error('请完善表单信息并确保文件验证通过')
          return
        }
        
        try {
          this.uploading = true
          this.uploadProgress = 0
          this.uploadStatus = ''
          
          const formData = new FormData()
          formData.append('file', this.uploadForm.file)
          formData.append('datasetName', this.uploadForm.datasetName)
          formData.append('description', this.uploadForm.description)
          formData.append('category', this.uploadForm.category)
          
          // 模拟上传进度
          const progressInterval = setInterval(() => {
            if (this.uploadProgress < 90) {
              this.uploadProgress += Math.random() * 10
            }
          }, 200)
          
          const response = await uploadDataset(formData)
          
          clearInterval(progressInterval)
          this.uploadProgress = 100
          this.uploadStatus = 'success'
          
          this.$message.success('数据集上传成功')
          this.$emit('success', response.data)
          
          setTimeout(() => {
            this.handleClose()
          }, 1000)
          
        } catch (error) {
          this.uploading = false
          this.uploadProgress = 0
          this.uploadStatus = 'exception'
          
          console.error('上传失败:', error)
          this.$message.error(error.response?.data?.msg || '上传失败，请重试')
        }
      })
    },
    
    handleUploadSuccess(response, file) {
      this.uploading = false
      this.uploadProgress = 100
      this.uploadStatus = 'success'
      
      if (response.code === 200) {
        this.$message.success('数据集上传成功')
        this.$emit('success', response.data)
        this.handleClose()
      } else {
        this.$message.error(response.msg || '上传失败')
      }
    },
    
    handleUploadError(error, file) {
      this.uploading = false
      this.uploadProgress = 0
      this.uploadStatus = 'exception'
      
      console.error('上传错误:', error)
      this.$message.error('上传失败，请重试')
    },
    
    handleClose() {
      this.dialogVisible = false
      this.resetForm()
    }
  }
}
</script>

<style scoped>
.el-upload__tip {
  color: #606266;
  font-size: 12px;
  margin-top: 7px;
}

.dialog-footer {
  text-align: right;
}

.el-progress {
  margin-top: 10px;
}
</style>
