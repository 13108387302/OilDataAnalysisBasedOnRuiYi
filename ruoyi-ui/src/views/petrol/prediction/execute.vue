<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>🔮 预测执行</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="$router.go(-1)">返回</el-button>
      </div>

      <!-- 步骤条 -->
      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step title="选择模型" description="选择用于预测的模型"></el-step>
        <el-step title="输入参数" description="输入预测所需的参数"></el-step>
        <el-step title="执行预测" description="执行预测并查看结果"></el-step>
      </el-steps>

      <div style="margin-top: 30px;">
        <!-- 步骤1: 选择模型 -->
        <div v-if="currentStep === 0">
          <el-form :model="form" label-width="120px">
            <el-form-item label="预测名称" required>
              <el-input v-model="form.predictionName" placeholder="请输入预测名称" style="width: 300px;" />
            </el-form-item>
            <el-form-item label="选择模型" required>
              <el-select v-model="form.modelId" placeholder="请选择模型" style="width: 300px;" @change="handleModelChange">
                <el-option
                  v-for="model in modelList"
                  :key="model.id"
                  :label="model.modelName"
                  :value="model.id"
                >
                  <span style="float: left">{{ model.modelName }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">{{ model.algorithm }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-form>

          <!-- 模型信息展示 -->
          <div v-if="selectedModel" style="margin-top: 20px;">
            <h3>模型信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="模型名称">{{ selectedModel.modelName }}</el-descriptions-item>
              <el-descriptions-item label="算法类型">{{ selectedModel.algorithm }}</el-descriptions-item>
              <el-descriptions-item label="模型类型">{{ selectedModel.modelType }}</el-descriptions-item>
              <el-descriptions-item label="输出目标">{{ selectedModel.outputTarget }}</el-descriptions-item>
              <el-descriptions-item label="模型描述" :span="2">{{ selectedModel.description }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <div style="margin-top: 30px; text-align: center;">
            <el-button type="primary" @click="nextStep" :disabled="!form.modelId || !form.predictionName">下一步</el-button>
          </div>
        </div>

        <!-- 步骤2: 输入参数 -->
        <div v-if="currentStep === 1">
          <el-tabs v-model="inputMode" @tab-click="handleTabClick">
            <el-tab-pane label="单条预测" name="single">
              <div style="margin-top: 20px;">
                <h3>输入预测参数</h3>
                <el-form :model="singleInput" label-width="120px">
                  <el-form-item 
                    v-for="feature in inputFeatures" 
                    :key="feature"
                    :label="feature"
                    required
                  >
                    <el-input-number 
                      v-model="singleInput[feature]" 
                      :precision="4"
                      style="width: 200px;"
                      placeholder="请输入数值"
                    />
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="批量预测" name="batch">
              <div style="margin-top: 20px;">
                <h3>上传数据文件</h3>
                <el-upload
                  class="upload-demo"
                  drag
                  :action="uploadUrl"
                  :before-upload="beforeUpload"
                  :on-success="handleUploadSuccess"
                  :on-error="handleUploadError"
                  :file-list="fileList"
                  accept=".csv,.xlsx,.xls"
                >
                  <i class="el-icon-upload"></i>
                  <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
                  <div class="el-upload__tip" slot="tip">只能上传csv/excel文件，且不超过10MB</div>
                </el-upload>
                
                <div v-if="uploadedFile" style="margin-top: 20px;">
                  <el-alert
                    title="文件上传成功"
                    type="success"
                    :description="`文件名: ${uploadedFile.name}, 大小: ${(uploadedFile.size/1024).toFixed(2)}KB`"
                    show-icon
                    :closable="false">
                  </el-alert>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>

          <div style="margin-top: 30px; text-align: center;">
            <el-button @click="prevStep">上一步</el-button>
            <el-button type="primary" @click="nextStep" :disabled="!canProceedToExecution">下一步</el-button>
          </div>
        </div>

        <!-- 步骤3: 执行预测 -->
        <div v-if="currentStep === 2">
          <div style="text-align: center; margin-top: 50px;">
            <div v-if="!predictionStarted">
              <h3>确认预测信息</h3>
              <el-descriptions :column="1" border style="margin: 20px 0;">
                <el-descriptions-item label="预测名称">{{ form.predictionName }}</el-descriptions-item>
                <el-descriptions-item label="使用模型">{{ selectedModel.modelName }}</el-descriptions-item>
                <el-descriptions-item label="预测模式">{{ inputMode === 'single' ? '单条预测' : '批量预测' }}</el-descriptions-item>
                <el-descriptions-item v-if="inputMode === 'single'" label="输入参数">
                  {{ JSON.stringify(singleInput) }}
                </el-descriptions-item>
                <el-descriptions-item v-if="inputMode === 'batch'" label="数据文件">
                  {{ uploadedFile ? uploadedFile.name : '未上传' }}
                </el-descriptions-item>
              </el-descriptions>
              
              <div style="margin-top: 30px;">
                <el-button @click="prevStep">上一步</el-button>
                <el-button type="primary" @click="executePrediction" :loading="executing">开始预测</el-button>
              </div>
            </div>

            <div v-else>
              <div v-if="executing">
                <i class="el-icon-loading" style="font-size: 48px; color: #409EFF;"></i>
                <h3 style="margin-top: 20px;">预测执行中...</h3>
                <p>请稍候，正在处理您的预测请求</p>
              </div>

              <div v-else-if="predictionResult">
                <i class="el-icon-success" style="font-size: 48px; color: #67C23A;"></i>
                <h3 style="margin-top: 20px;">预测完成！</h3>
                
                <el-card style="margin-top: 30px; text-align: left;">
                  <div slot="header">
                    <span>预测结果</span>
                  </div>
                  <pre>{{ JSON.stringify(predictionResult, null, 2) }}</pre>
                </el-card>

                <div style="margin-top: 30px;">
                  <el-button type="primary" @click="$router.push('/petrol/prediction')">返回预测列表</el-button>
                  <el-button @click="resetPrediction">重新预测</el-button>
                </div>
              </div>

              <div v-else-if="predictionError">
                <i class="el-icon-error" style="font-size: 48px; color: #F56C6C;"></i>
                <h3 style="margin-top: 20px;">预测失败</h3>
                <el-alert
                  :title="predictionError"
                  type="error"
                  show-icon
                  :closable="false"
                  style="margin-top: 20px;">
                </el-alert>
                
                <div style="margin-top: 30px;">
                  <el-button @click="prevStep">返回修改</el-button>
                  <el-button type="primary" @click="executePrediction" :loading="executing">重试</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getAvailableModels } from "@/api/petrol/model";
import { executePrediction, executeBatchPrediction } from "@/api/petrol/prediction";

export default {
  name: "PredictionExecute",
  data() {
    return {
      currentStep: 0,
      inputMode: 'single',
      modelList: [],
      selectedModel: null,
      inputFeatures: [],
      form: {
        predictionName: '',
        modelId: null
      },
      singleInput: {},
      uploadedFile: null,
      fileList: [],
      uploadUrl: process.env.VUE_APP_BASE_API + '/common/upload',
      predictionStarted: false,
      executing: false,
      predictionResult: null,
      predictionError: null
    };
  },
  computed: {
    canProceedToExecution() {
      if (this.inputMode === 'single') {
        return this.inputFeatures.every(feature => this.singleInput[feature] !== undefined && this.singleInput[feature] !== null);
      } else {
        return this.uploadedFile !== null;
      }
    }
  },
  created() {
    this.getModelList();
    // 从路由参数中获取初始值
    if (this.$route.query.modelId) {
      this.form.modelId = parseInt(this.$route.query.modelId);
    }
    if (this.$route.query.predictionName) {
      this.form.predictionName = this.$route.query.predictionName;
    }
  },
  methods: {
    /** 获取模型列表 */
    getModelList() {
      getAvailableModels().then(response => {
        this.modelList = response.data;
        // 如果有预设的模型ID，自动选择
        if (this.form.modelId) {
          this.handleModelChange(this.form.modelId);
        }
      });
    },
    /** 模型选择变化 */
    handleModelChange(modelId) {
      this.selectedModel = this.modelList.find(model => model.id === modelId);
      if (this.selectedModel && this.selectedModel.inputFeatures) {
        try {
          this.inputFeatures = JSON.parse(this.selectedModel.inputFeatures);
          // 初始化单条输入对象
          this.singleInput = {};
          this.inputFeatures.forEach(feature => {
            this.singleInput[feature] = null;
          });
        } catch (e) {
          console.error('解析输入特征失败:', e);
          this.inputFeatures = [];
        }
      }
    },
    /** 下一步 */
    nextStep() {
      if (this.currentStep < 2) {
        this.currentStep++;
      }
    },
    /** 上一步 */
    prevStep() {
      if (this.currentStep > 0) {
        this.currentStep--;
      }
    },
    /** 标签页切换 */
    handleTabClick(tab) {
      this.inputMode = tab.name;
    },
    /** 文件上传前检查 */
    beforeUpload(file) {
      const isValidType = file.type === 'text/csv' || 
                         file.type === 'application/vnd.ms-excel' ||
                         file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
      const isLt10M = file.size / 1024 / 1024 < 10;

      if (!isValidType) {
        this.$message.error('只能上传 CSV 或 Excel 文件!');
      }
      if (!isLt10M) {
        this.$message.error('上传文件大小不能超过 10MB!');
      }
      return isValidType && isLt10M;
    },
    /** 文件上传成功 */
    handleUploadSuccess(response, file) {
      this.uploadedFile = file;
      this.$message.success('文件上传成功');
    },
    /** 文件上传失败 */
    handleUploadError(err, file) {
      this.$message.error('文件上传失败: ' + err.message);
    },
    /** 执行预测 */
    executePrediction() {
      this.predictionStarted = true;
      this.executing = true;
      this.predictionResult = null;
      this.predictionError = null;

      if (this.inputMode === 'single') {
        // 单条预测
        const request = {
          modelId: this.form.modelId,
          predictionName: this.form.predictionName,
          inputData: this.singleInput
        };

        executePrediction(request).then(response => {
          this.executing = false;
          if (response.code === 200) {
            this.predictionResult = response.data.result;
            this.$message.success('预测执行成功');
          } else {
            this.predictionError = response.msg;
          }
        }).catch(error => {
          this.executing = false;
          this.predictionError = error.message || '预测执行失败';
        });
      } else {
        // 批量预测
        if (!this.uploadedFile) {
          this.executing = false;
          this.predictionError = '请先上传数据文件';
          return;
        }

        const formData = new FormData();
        formData.append('file', this.uploadedFile.raw);
        formData.append('modelId', this.form.modelId);
        formData.append('predictionName', this.form.predictionName);

        executeBatchPrediction(formData).then(response => {
          this.executing = false;
          if (response.code === 200) {
            this.predictionResult = response.data.result;
            this.$message.success('批量预测执行成功');
          } else {
            this.predictionError = response.msg;
          }
        }).catch(error => {
          this.executing = false;
          this.predictionError = error.message || '批量预测执行失败';
        });
      }
    },
    /** 重置预测 */
    resetPrediction() {
      this.currentStep = 0;
      this.predictionStarted = false;
      this.executing = false;
      this.predictionResult = null;
      this.predictionError = null;
      this.uploadedFile = null;
      this.fileList = [];
      this.singleInput = {};
      if (this.inputFeatures.length > 0) {
        this.inputFeatures.forEach(feature => {
          this.singleInput[feature] = null;
        });
      }
    }
  }
};
</script>

<style scoped>
.upload-demo {
  margin-top: 20px;
}
</style>
