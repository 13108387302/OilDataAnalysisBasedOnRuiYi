<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="数据集名称" prop="datasetName">
        <el-input
          v-model="queryParams.datasetName"
          placeholder="请输入数据集名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="数据集类别" prop="datasetCategory">
        <el-select v-model="queryParams.datasetCategory" placeholder="请选择数据集类别" clearable>
          <el-option label="测井" value="测井" />
          <el-option label="地震" value="地震" />
          <el-option label="生产" value="生产" />
          <el-option label="地质" value="地质" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="活跃" value="ACTIVE" />
          <el-option label="非活跃" value="INACTIVE" />
          <el-option label="处理中" value="PROCESSING" />
        </el-select>
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
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['petrol:dataset:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-upload"
          size="mini"
          @click="handleUpload"
          v-hasPermi="['petrol:dataset:add']"
        >上传数据集</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['petrol:dataset:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['petrol:dataset:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="datasetList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="数据集ID" align="center" prop="id" width="80" />
      <el-table-column label="数据集名称" align="center" prop="datasetName" :show-overflow-tooltip="true" />
      <el-table-column label="文件名" align="center" prop="fileName" :show-overflow-tooltip="true" />
      <el-table-column label="类别" align="center" prop="datasetCategory" width="80">
        <template slot-scope="scope">
          <el-tag :type="getCategoryTagType(scope.row.datasetCategory)">{{ scope.row.datasetCategory }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件类型" align="center" prop="fileType" width="80" />
      <el-table-column label="数据规模" align="center" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.totalRows }}行 × {{ scope.row.totalColumns }}列</span>
        </template>
      </el-table-column>
      <el-table-column label="文件大小" align="center" width="100">
        <template slot-scope="scope">
          <span>{{ formatFileSize(scope.row.fileSize) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="质量评分" align="center" width="100">
        <template slot-scope="scope">
          <el-progress 
            :percentage="scope.row.dataQualityScore" 
            :color="getQualityColor(scope.row.dataQualityScore)"
            :show-text="false"
            :stroke-width="8"
          ></el-progress>
          <div style="font-size: 12px; margin-top: 2px;">{{ scope.row.dataQualityScore }}%</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="getStatusTagType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handlePreview(scope.row)"
          >预览</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['petrol:dataset:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['petrol:dataset:remove']"
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

    <!-- 添加或修改数据集管理对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="数据集名称" prop="datasetName">
          <el-input v-model="form.datasetName" placeholder="请输入数据集名称" />
        </el-form-item>
        <el-form-item label="数据集描述" prop="datasetDescription">
          <el-input v-model="form.datasetDescription" type="textarea" placeholder="请输入数据集描述" />
        </el-form-item>
        <el-form-item label="数据集类别" prop="datasetCategory">
          <el-select v-model="form.datasetCategory" placeholder="请选择数据集类别">
            <el-option label="测井" value="测井" />
            <el-option label="地震" value="地震" />
            <el-option label="生产" value="生产" />
            <el-option label="地质" value="地质" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否公开" prop="isPublic">
          <el-radio-group v-model="form.isPublic">
            <el-radio label="Y">是</el-radio>
            <el-radio label="N">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 数据集上传对话框 -->
    <DatasetUpload 
      :visible.sync="uploadVisible" 
      @success="handleUploadSuccess"
    />

    <!-- 数据集预览对话框 -->
    <DatasetPreview 
      :visible.sync="previewVisible" 
      :dataset-id="currentDatasetId"
    />
  </div>
</template>

<script>
import { listDataset, getDataset, delDataset, addDataset, updateDataset } from "@/api/petrol/dataset";
import DatasetUpload from './components/DatasetUpload';
import DatasetPreview from './components/DatasetPreview';
import VirtualTable from '@/components/VirtualTable';

export default {
  name: "Dataset",
  components: {
    DatasetUpload,
    DatasetPreview,
    VirtualTable
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
      // 数据集管理表格数据
      datasetList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 上传对话框
      uploadVisible: false,
      // 预览对话框
      previewVisible: false,
      // 当前数据集ID
      currentDatasetId: null,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        datasetName: null,
        datasetCategory: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        datasetName: [
          { required: true, message: "数据集名称不能为空", trigger: "blur" }
        ],
        datasetCategory: [
          { required: true, message: "数据集类别不能为空", trigger: "change" }
        ]
      },
      // 虚拟表格列配置
      tableColumns: [
        { prop: 'id', label: '数据集ID', width: '80px' },
        { prop: 'datasetName', label: '数据集名称', minWidth: '150px' },
        { prop: 'fileName', label: '文件名', minWidth: '150px' },
        { prop: 'datasetCategory', label: '类别', width: '100px' },
        { prop: 'fileType', label: '文件类型', width: '80px' },
        { prop: 'dataSize', label: '数据规模', width: '120px' },
        { prop: 'fileSize', label: '文件大小', width: '100px' },
        { prop: 'dataQualityScore', label: '质量评分', width: '120px' },
        { prop: 'status', label: '状态', width: '80px' },
        { prop: 'createTime', label: '创建时间', width: '180px' },
        { prop: 'actions', label: '操作', width: '200px' }
      ]
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询数据集管理列表 */
    getList() {
      this.loading = true;
      listDataset(this.queryParams).then(response => {
        this.datasetList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
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
        datasetName: null,
        datasetDescription: null,
        datasetCategory: null,
        isPublic: "N",
        remark: null
      };
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
      this.title = "添加数据集管理";
    },
    /** 上传按钮操作 */
    handleUpload() {
      this.uploadVisible = true;
    },
    /** 预览按钮操作 */
    handlePreview(row) {
      this.currentDatasetId = row.id;
      this.previewVisible = true;
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getDataset(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改数据集管理";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateDataset(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addDataset(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除数据集管理编号为"' + ids + '"的数据项？').then(function() {
        return delDataset(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('petrol/dataset/export', {
        ...this.queryParams
      }, `dataset_${new Date().getTime()}.xlsx`)
    },
    /** 上传成功回调 */
    handleUploadSuccess() {
      this.uploadVisible = false;
      this.getList();
    },
    /** 格式化文件大小 */
    formatFileSize(bytes) {
      if (bytes === 0) return '0 B';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },
    /** 获取类别标签类型 */
    getCategoryTagType(category) {
      const typeMap = {
        '测井': 'primary',
        '地震': 'success',
        '生产': 'warning',
        '地质': 'info'
      };
      return typeMap[category] || 'info';
    },
    /** 获取状态标签类型 */
    getStatusTagType(status) {
      const typeMap = {
        'ACTIVE': 'success',
        'INACTIVE': 'info',
        'PROCESSING': 'warning'
      };
      return typeMap[status] || 'info';
    },
    /** 获取状态文本 */
    getStatusText(status) {
      const textMap = {
        'ACTIVE': '活跃',
        'INACTIVE': '非活跃',
        'PROCESSING': '处理中'
      };
      return textMap[status] || status;
    },
    /** 获取质量评分颜色 */
    getQualityColor(score) {
      if (score >= 90) return '#67c23a';
      if (score >= 70) return '#e6a23c';
      return '#f56c6c';
    }
  }
};
</script>
