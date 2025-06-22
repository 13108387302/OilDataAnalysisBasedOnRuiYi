<template>
  <div class="app-container">
    <el-card v-if="task" class="box-card">
      <div slot="header" class="clearfix">
        <span>任务详情 (ID: {{ taskId }})</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="goBack">返回列表</el-button>
      </div>
      <div v-if="loading" class="loading-container">
        <div class="loader"></div>
        <p>{{ loadingMessage }}</p>
      </div>
      <div v-else-if="error">
        <el-alert
          :title="'任务执行失败 (状态: ' + task.status + ')'"
          type="error"
          :closable="false"
          show-icon>
            <div slot="title" style="font-size: 16px; font-weight: bold;">任务执行失败 (状态: {{ task.status }})</div>
            <pre style="white-space: pre-wrap; word-wrap: break-word; margin-top: 10px;">{{ error }}</pre>
        </el-alert>
      </div>
      <div v-else class="results-container">
        <h3>任务状态: <el-tag type="success">{{ task.status }}</el-tag></h3>
        <el-tabs v-model="activeTab" type="card">
          <el-tab-pane label="分析结果" name="results">
            <div v-for="(value, key) in results" :key="key" class="result-item">
              <h4>{{ getResultTitle(key) }}</h4>
              <div v-if="isImageUrl(value)">
                <el-image 
                  style="width: 400px; height: auto;"
                  :src="getImageUrl(value)" 
                  :preview-src-list="[getImageUrl(value)]">
                </el-image>
              </div>
              <pre v-else>{{ formatJson(value) }}</pre>
            </div>
          </el-tab-pane>
          <el-tab-pane label="原始任务信息" name="raw">
             <pre>{{ formatJson(task) }}</pre>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
     <div v-else-if="!loading">
       <el-alert
          title="未找到任务"
          type="warning"
          :description="'无法加载ID为 ' + taskId + ' 的任务详情，请检查任务ID是否正确或返回列表重试。'"
          show-icon
          :closable="false">
        </el-alert>
    </div>
  </div>
</template>

<script>
import { getTask } from "@/api/petrol/task";

export default {
  name: "PetrolTaskResult",
  data() {
    return {
      taskId: null,
      task: null,
      results: null,
      loading: true,
      loadingMessage: "正在加载任务数据...",
      error: null,
      polling: null,
      activeTab: 'results',
      resultTitles: {
        'metrics': '模型评估指标',
        'model_params': '模型参数',
        'regression_plot': '回归拟合图',
        'report_file': '结果数据报告',
        'model_file': '已保存的模型文件',
        'cluster_plot': '聚类结果图',
        'clustered_data_file': '聚类结果数据',
        'confusion_matrix': '混淆矩阵',
        'roc_curve': 'ROC曲线'
      }
    };
  },
  created() {
    this.taskId = this.$route.params.id;
    this.fetchTaskData();
    this.startPolling();
  },
  beforeDestroy() {
    this.stopPolling();
  },
  methods: {
    fetchTaskData() {
      if (!this.taskId) return;
      getTask(this.taskId)
        .then(response => {
          this.task = response.data;
          if (!this.task) {
              this.loading = false;
              this.stopPolling();
              return;
          }

          if (this.task.status === 'RUNNING' || this.task.status === 'QUEUED') {
            this.loading = true;
            this.loadingMessage = `任务正在执行中... (状态: ${this.task.status}), 页面将自动刷新。`;
          } else if (this.task.status === 'COMPLETED') {
            this.loading = false;
            this.stopPolling();
            try {
              this.results = JSON.parse(this.task.resultsJson);
            } catch(e) {
                this.error = "结果解析失败：后端返回的JSON格式无效。\n原始返回内容:\n" + this.task.resultsJson;
                this.results = null;
            }
          } else if (this.task.status === 'FAILED') {
            this.loading = false;
            this.stopPolling();
            this.error = this.task.errorMessage || "任务执行失败，但未提供具体的错误信息。";
          }
        })
        .catch(err => {
          this.loading = false;
          this.error = "加载任务数据失败，请检查网络连接或联系管理员。";
          this.stopPolling();
        });
    },
    startPolling() {
      // 每5秒轮询一次
      this.polling = setInterval(() => {
        if (this.task && (this.task.status === 'COMPLETED' || this.task.status === 'FAILED')) {
            this.stopPolling();
        } else {
            this.fetchTaskData();
        }
      }, 5000);
    },
    stopPolling() {
      if (this.polling) {
        clearInterval(this.polling);
        this.polling = null;
      }
    },
    isImageUrl(value) {
      if (typeof value !== 'string') return false;
      return value.toLowerCase().match(/\.(jpeg|jpg|gif|png|svg)$/) != null;
    },
    getImageUrl(path) {
        // 假设Python返回的是相对于`profile`目录的相对路径
        // 例如 'petrol/linear_regression/12345/figure.png'
        // 我们需要构造成 '/profile/petrol/linear_regression/12345/figure.png'
        if (path.startsWith(process.env.VUE_APP_BASE_API + '/profile')) {
            return path;
        }
        return process.env.VUE_APP_BASE_API + '/profile/' + path;
    },
    formatJson(data) {
        if (typeof data === 'string') {
            try {
                data = JSON.parse(data);
            } catch (e) {
                return data;
            }
        }
        return JSON.stringify(data, null, 2);
    },
    goBack() {
      this.$router.push({ path: '/petrol/task' });
    },
    getResultTitle(key) {
      return this.resultTitles[key] || key;
    }
  }
};
</script>

<style scoped>
.loading-container {
  text-align: center;
  padding: 50px;
}
.loader {
  border: 8px solid #f3f3f3;
  border-radius: 50%;
  border-top: 8px solid #3498db;
  width: 60px;
  height: 60px;
  -webkit-animation: spin 2s linear infinite; /* Safari */
  animation: spin 2s linear infinite;
  margin: 0 auto 20px auto;
}
.results-container {
  font-family: 'Courier New', Courier, monospace;
}
.result-item {
  margin-bottom: 20px;
}
pre {
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style> 