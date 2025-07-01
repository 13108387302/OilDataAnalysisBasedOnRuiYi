<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="模型名称" prop="modelName">
        <el-input
          v-model="queryParams.modelName"
          placeholder="请输入模型名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="模型类型" prop="modelType">
        <el-select v-model="queryParams.modelType" placeholder="请选择模型类型" clearable>
          <el-option label="回归" value="regression" />
          <el-option label="分类" value="classification" />
          <el-option label="聚类" value="clustering" />
        </el-select>
      </el-form-item>
      <el-form-item label="算法类型" prop="algorithm">
        <el-input
          v-model="queryParams.algorithm"
          placeholder="请输入算法类型"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-cpu"
          size="mini"
          @click="handleTrainModel"
          v-hasPermi="['petrol:model:add']"
        >训练新模型</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['petrol:model:add']"
        >上传模型</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['petrol:model:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['petrol:model:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="modelList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="模型ID" align="center" prop="id" />
      <el-table-column label="模型名称" align="center" prop="modelName" />
      <el-table-column label="模型类型" align="center" prop="modelType">
        <template slot-scope="scope">
          <el-tag :type="getModelTypeColor(scope.row.modelType)">
            {{ getModelTypeName(scope.row.modelType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="算法类型" align="center" prop="algorithm" />
      <el-table-column label="来源" align="center" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.sourceTaskId ? 'success' : 'primary'" size="mini">
            {{ scope.row.sourceTaskId ? '分析生成' : '用户上传' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="输出目标" align="center" prop="outputTarget" />
      <el-table-column label="文件大小" align="center" prop="fileSize">
        <template slot-scope="scope">
          {{ formatFileSize(scope.row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'danger'">
            {{ scope.row.status === 'ACTIVE' ? '可用' : '不可用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
            v-hasPermi="['petrol:model:query']"
          >详情</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-magic-stick"
            @click="handlePredict(scope.row)"
            v-hasPermi="['petrol:prediction:execute']"
          >预测</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['petrol:model:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['petrol:model:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 上传模型对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="模型类型" prop="modelType">
          <el-select v-model="form.modelType" placeholder="请选择模型类型" style="width: 100%">
            <el-option label="回归" value="regression" />
            <el-option label="分类" value="classification" />
            <el-option label="聚类" value="clustering" />
          </el-select>
        </el-form-item>
        <el-form-item label="算法类型" prop="algorithm">
          <el-input v-model="form.algorithm" placeholder="请输入算法类型，如：lightgbm, xgboost等" />
        </el-form-item>
        <el-form-item label="输出目标" prop="outputTarget">
          <el-input v-model="form.outputTarget" placeholder="请输入输出目标变量名" />
        </el-form-item>
        <el-form-item label="输入特征" prop="inputFeatures">
          <el-input 
            v-model="form.inputFeatures" 
            type="textarea" 
            :rows="3"
            placeholder="请输入输入特征，JSON格式，如：[&quot;GR&quot;, &quot;RT&quot;, &quot;RHOB&quot;]" 
          />
        </el-form-item>
        <el-form-item label="模型描述" prop="description">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入模型描述" 
          />
        </el-form-item>
        <el-form-item label="模型文件" prop="file" v-if="!form.id">
          <el-upload
            class="upload-demo"
            drag
            :before-upload="beforeUpload"
            :on-change="handleFileChange"
            :auto-upload="false"
            accept=".pkl,.joblib,.model"
            :limit="1"
          >
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">将模型文件拖到此处，或<em>点击上传</em></div>
            <div class="el-upload__tip" slot="tip">只能上传pkl/joblib/model文件，且不超过100MB</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" :loading="uploading">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 模型详情组件 -->
    <model-detail
      :visible.sync="detailOpen"
      :model-id="currentModelId"
    />

    <!-- 训练新模型对话框 -->
    <el-dialog title="训练新模型" :visible.sync="trainOpen" width="60%" append-to-body>
      <el-form ref="trainForm" :model="trainForm" :rules="trainRules" label-width="120px">
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="trainForm.modelName" placeholder="请输入模型名称" />
        </el-form-item>

        <el-form-item label="数据源选择" prop="dataSourceType">
          <el-radio-group v-model="trainForm.dataSourceType" @change="handleTrainDataSourceChange">
            <el-radio label="dataset">使用已有数据集</el-radio>
            <el-radio label="upload">上传新文件</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 数据集选择 -->
        <el-form-item v-if="trainForm.dataSourceType === 'dataset'" label="选择数据集" prop="datasetId">
          <el-select
            v-model="trainForm.datasetId"
            placeholder="请选择数据集"
            filterable
            style="width: 100%;"
            @change="handleTrainDatasetChange">
            <el-option
              v-for="dataset in availableDatasets"
              :key="dataset.id"
              :label="dataset.datasetName"
              :value="dataset.id">
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span>{{ dataset.datasetName }}</span>
                <div style="font-size: 12px; color: #8492a6;">
                  <el-tag size="mini" :type="getDatasetCategoryType(dataset.datasetCategory)">
                    {{ dataset.datasetCategory }}
                  </el-tag>
                  <span style="margin-left: 5px;">{{ dataset.rowCount }}行</span>
                </div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 文件上传 -->
        <el-form-item v-if="trainForm.dataSourceType === 'upload'" label="上传训练数据" prop="file">
          <el-upload
            ref="trainDataUpload"
            :auto-upload="false"
            accept=".csv,.xlsx,.xls"
            :limit="1"
            drag>
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">将训练数据文件拖到此处，或<em>点击上传</em></div>
            <div class="el-upload__tip" slot="tip">支持 CSV、Excel 格式</div>
          </el-upload>
        </el-form-item>

        <!-- 特征选择 -->
        <el-form-item v-if="trainAvailableColumns.length > 0" label="特征列(X)" prop="selectedFeatures">
          <el-select
            v-model="trainForm.selectedFeatures"
            multiple
            filterable
            placeholder="请选择特征列"
            style="width: 100%;">
            <el-option
              v-for="col in trainAvailableColumns"
              :key="col"
              :label="col"
              :value="col"
              :disabled="col === trainForm.targetColumn">
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 目标列选择 -->
        <el-form-item v-if="trainAvailableColumns.length > 0" label="目标列(Y)" prop="targetColumn">
          <el-select
            v-model="trainForm.targetColumn"
            filterable
            placeholder="请选择目标列"
            style="width: 100%;">
            <el-option
              v-for="col in trainAvailableColumns"
              :key="col"
              :label="col"
              :value="col"
              :disabled="trainForm.selectedFeatures && trainForm.selectedFeatures.includes(col)">
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="算法类型" prop="algorithm">
          <el-select v-model="trainForm.algorithm" placeholder="请选择算法类型" style="width: 100%;">
            <el-option label="线性回归" value="linear_regression" />
            <el-option label="随机森林" value="random_forest" />
            <el-option label="XGBoost" value="xgboost" />
            <el-option label="LightGBM" value="lightgbm" />
          </el-select>
        </el-form-item>

        <el-form-item label="模型描述" prop="description">
          <el-input
            v-model="trainForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模型描述" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitTrainForm" :loading="training">开始训练</el-button>
        <el-button @click="cancelTrain">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listModel, getModel, delModel, updateModel, uploadModel, trainModel } from "@/api/petrol/model";
import { listAvailableDatasets, getDatasetColumns } from "@/api/petrol/dataset";
import ModelDetail from './components/ModelDetail.vue';

export default {
  name: "Model",
  components: {
    ModelDetail
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 石油模型表格数据
      modelList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否显示详情弹出层
      detailOpen: false,
      // 当前模型ID
      currentModelId: null,
      // 上传状态
      uploading: false,
      // 上传的文件
      uploadFile: null,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        modelName: null,
        modelType: null,
        algorithm: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        modelName: [
          { required: true, message: "模型名称不能为空", trigger: "blur" }
        ],
        modelType: [
          { required: true, message: "请选择模型类型", trigger: "change" }
        ],
        algorithm: [
          { required: true, message: "算法类型不能为空", trigger: "blur" }
        ]
      },
      // 训练模型相关
      trainOpen: false,
      training: false,
      availableDatasets: [],
      trainAvailableColumns: [],
      trainForm: {
        modelName: '',
        dataSourceType: 'dataset',
        datasetId: null,
        selectedFeatures: [],
        targetColumn: '',
        algorithm: '',
        description: ''
      },
      trainRules: {
        modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
        dataSourceType: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
        datasetId: [{ required: true, message: '请选择数据集', trigger: 'change' }],
        selectedFeatures: [{ required: true, message: '请选择特征列', trigger: 'change' }],
        targetColumn: [{ required: true, message: '请选择目标列', trigger: 'change' }],
        algorithm: [{ required: true, message: '请选择算法类型', trigger: 'change' }]
      }
    };
  },
  created() {
    this.getList();
    this.loadAvailableDatasets();
  },
  methods: {
    /** 查询石油模型列表 */
    getList() {
      this.loading = true;
      listModel(this.queryParams).then(response => {
        this.modelList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 获取模型类型名称 */
    getModelTypeName(type) {
      const typeMap = {
        'regression': '回归',
        'classification': '分类',
        'clustering': '聚类'
      };
      return typeMap[type] || type;
    },
    /** 获取模型类型颜色 */
    getModelTypeColor(type) {
      const colorMap = {
        'regression': 'success',
        'classification': 'warning',
        'clustering': 'info'
      };
      return colorMap[type] || '';
    },
    /** 格式化文件大小 */
    formatFileSize(size) {
      if (!size) return 'N/A';
      if (size < 1024) return size + ' B';
      if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB';
      return (size / (1024 * 1024)).toFixed(2) + ' MB';
    },

    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        modelName: null,
        modelType: null,
        algorithm: null,
        description: null,
        inputFeatures: null,
        outputTarget: null
      };
      this.uploadFile = null;
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "上传模型";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getModel(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改模型";
      });
    },
    /** 查看详情 */
    handleView(row) {
      this.currentModelId = row.id;
      this.detailOpen = true;
    },
    /** 预测 */
    handlePredict(row) {
      this.$router.push({
        path: '/petrol/prediction',
        query: {
          modelId: row.id,
          predictionName: `${row.modelName}_预测_${new Date().getTime()}`,
          tab: 'regression'
        }
      });
    },
    /** 文件上传前检查 */
    beforeUpload(file) {
      const isValidType = file.name.toLowerCase().endsWith('.pkl') || 
                         file.name.toLowerCase().endsWith('.joblib') ||
                         file.name.toLowerCase().endsWith('.model');
      const isLt100M = file.size / 1024 / 1024 < 100;

      if (!isValidType) {
        this.$message.error('只能上传 pkl、joblib、model 格式的模型文件!');
      }
      if (!isLt100M) {
        this.$message.error('上传文件大小不能超过 100MB!');
      }
      return false; // 阻止自动上传
    },
    /** 文件选择变化 */
    handleFileChange(file) {
      this.uploadFile = file.raw;
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.uploading = true;
          
          if (this.form.id != null) {
            // 修改模型
            updateModel(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
              this.uploading = false;
            }).catch(() => {
              this.uploading = false;
            });
          } else {
            // 上传新模型
            if (!this.uploadFile) {
              this.$message.error('请选择要上传的模型文件');
              this.uploading = false;
              return;
            }

            const formData = new FormData();
            formData.append('file', this.uploadFile);
            formData.append('modelName', this.form.modelName);
            formData.append('modelType', this.form.modelType);
            formData.append('algorithm', this.form.algorithm);
            formData.append('description', this.form.description || '');
            formData.append('inputFeatures', this.form.inputFeatures || '');
            formData.append('outputTarget', this.form.outputTarget || '');

            uploadModel(formData).then(response => {
              this.$modal.msgSuccess("上传成功");
              this.open = false;
              this.getList();
              this.uploading = false;
            }).catch(() => {
              this.uploading = false;
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除石油模型编号为"' + ids + '"的数据项？').then(function() {
        return delModel(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 训练新模型 */
    handleTrainModel() {
      this.trainOpen = true;
      this.resetTrainForm();
    },
    /** 加载可用数据集列表 */
    async loadAvailableDatasets() {
      try {
        const response = await listAvailableDatasets();
        this.availableDatasets = response.data || [];
      } catch (error) {
        console.error('加载数据集列表失败:', error);
        this.$message.error('加载数据集列表失败');
      }
    },
    /** 训练数据源类型变化处理 */
    handleTrainDataSourceChange() {
      this.trainForm.datasetId = null;
      this.trainAvailableColumns = [];
      this.trainForm.selectedFeatures = [];
      this.trainForm.targetColumn = '';
    },
    /** 训练数据集选择变化处理 */
    async handleTrainDatasetChange(datasetId) {
      if (!datasetId) {
        this.trainAvailableColumns = [];
        return;
      }

      try {
        const response = await getDatasetColumns(datasetId);
        if (response.data && response.data.columns) {
          this.trainAvailableColumns = response.data.columns;
          this.$message.success('数据集加载成功');
        }
      } catch (error) {
        console.error('加载数据集信息失败:', error);
        this.$message.error('加载数据集信息失败');
      }
    },
    /** 提交训练表单 */
    submitTrainForm() {
      this.$refs.trainForm.validate(async (valid) => {
        if (!valid) return;

        this.training = true;
        try {
          const params = {
            modelName: this.trainForm.modelName,
            datasetId: this.trainForm.datasetId,
            selectedFeatures: this.trainForm.selectedFeatures,
            targetColumn: this.trainForm.targetColumn,
            algorithm: this.trainForm.algorithm,
            description: this.trainForm.description
          };

          await trainModel(params);
          this.$message.success('模型训练任务提交成功');
          this.trainOpen = false;
          this.getList();
        } catch (error) {
          console.error('模型训练失败:', error);
          this.$message.error('模型训练失败');
        } finally {
          this.training = false;
        }
      });
    },
    /** 取消训练 */
    cancelTrain() {
      this.trainOpen = false;
      this.resetTrainForm();
    },
    /** 重置训练表单 */
    resetTrainForm() {
      this.trainForm = {
        modelName: '',
        dataSourceType: 'dataset',
        datasetId: null,
        selectedFeatures: [],
        targetColumn: '',
        algorithm: '',
        description: ''
      };
      this.trainAvailableColumns = [];
      if (this.$refs.trainForm) {
        this.$refs.trainForm.resetFields();
      }
    },
    /** 获取数据集类别标签类型 */
    getDatasetCategoryType(category) {
      const typeMap = {
        '测井': 'primary',
        '地震': 'success',
        '生产': 'warning',
        '地质': 'info'
      };
      return typeMap[category] || '';
    }
  }
};
</script>

<style scoped>
.upload-demo {
  width: 100%;
}
</style>
