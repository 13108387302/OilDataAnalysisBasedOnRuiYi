<template>
  <div class="app-container">
    <el-card v-if="task" v-loading="loading">
      <div slot="header" class="clearfix">
        <span>任务结果: {{ task.taskName }} (ID: {{ task.id }})</span>
      </div>

      <!-- 任务基本信息 -->
      <el-descriptions title="任务详情" :column="2" border>
        <el-descriptions-item label="算法类型">{{ task.algorithm }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">
          <el-tag :type="statusTagType(task.status)">{{ task.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ task.createTime }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ task.updateTime }}</el-descriptions-item>
      </el-descriptions>

      <!-- 失败任务的错误信息和调试图表 -->
      <div v-if="task.status === 'FAILED'">
        <el-divider content-position="left">错误详情</el-divider>
        <el-alert :title="errorMessageForDisplay" type="error" :closable="false" show-icon></el-alert>
        <el-row v-if="debugPlotUrl" style="margin-top: 20px;">
            <el-col :span="12">
                <el-card>
                    <div slot="header">调试信息：数据散点图</div>
                     <el-image :src="debugPlotUrl" :preview-src-list="[debugPlotUrl]" style="width: 100%; height: auto;" fit="contain">
                        <div slot="error" class="image-slot">
                            <i class="el-icon-picture-outline"></i> 调试图表加载失败
                        </div>
                    </el-image>
                </el-card>
            </el-col>
        </el-row>
      </div>

      <!-- 统计结果 -->
      <el-divider></el-divider>
      <div v-if="statisticalResults">
        <h3>统计指标</h3>
        <el-table :data="statisticalResults" border style="width: 100%">
          <el-table-column prop="metric" label="指标"></el-table-column>
          <el-table-column prop="value" label="值"></el-table-column>
        </el-table>
      </div>
      <el-alert v-else-if="!loading" title="没有可用的统计结果" type="info" show-icon></el-alert>

      <!-- 可视化结果 -->
      <el-divider></el-divider>
      <div v-if="visualizationPlots && visualizationPlots.length > 0">
        <h3>可视化图表</h3>
        <el-row :gutter="20">
          <el-col v-for="plot in visualizationPlots" :key="plot.name" :span="12" style="margin-bottom: 20px;">
            <el-card>
              <div slot="header">{{ plot.name }}</div>
              <el-image :src="plot.url" :preview-src-list="[plot.url]" style="width: 100%; height: auto;" fit="contain">
                 <div slot="error" class="image-slot">
                    <i class="el-icon-picture-outline"></i> 图片加载失败
                  </div>
              </el-image>
            </el-card>
          </el-col>
        </el-row>
      </div>
       <el-alert v-else-if="!loading" title="没有可用的可视化图表" type="info" show-icon></el-alert>
       <el-button @click="$router.back()" style="margin-top: 20px;">返回</el-button>

    </el-card>
     <el-alert v-else-if="!loading" title="未能加载任务数据" type="error" center show-icon></el-alert>
  </div>
</template>

<script>
import { getTask } from '@/api/petrol/task';

export default {
  name: "PetrolTaskResult",
  data() {
    return {
      loading: true,
      task: null,
      statisticalResults: null,
      visualizationPlots: [],
      debugPlotUrl: null,
      errorMessageForDisplay: ''
    };
  },
  created() {
    const taskId = this.$route.params && this.$route.params.id;
    if (taskId) {
      this.fetchTaskData(taskId);
    } else {
      this.$message.error("未提供任务ID");
      this.loading = false;
    }
  },
  methods: {
    fetchTaskData(taskId) {
      this.loading = true;
      getTask(taskId).then(response => {
        this.task = response.data;
        if (this.task.status === 'FAILED' && this.task.errorMessage) {
          this.parseErrorMessageForDebugPlot();
        } else {
          this.errorMessageForDisplay = this.task.errorMessage;
        }
        if (this.task.resultsJson) {
          try {
            const results = JSON.parse(this.task.resultsJson);
            // 解析统计数据
            if (results.statistics) {
              this.statisticalResults = Object.entries(results.statistics).map(([key, value]) => ({
                metric: key,
                value: typeof value === 'number' ? value.toFixed(4) : value
              }));
            }
            // 解析可视化图表
            if (results.visualizations) {
               const baseUrl = process.env.VUE_APP_BASE_API;
               this.visualizationPlots = Object.entries(results.visualizations).map(([key, value]) => ({
                 name: key,
                 url: baseUrl + value
               }));
            }
          } catch (e) {
            this.$message.error("解析结果JSON失败: " + e.message);
          }
        }
      }).catch(err => {
        this.$message.error("获取任务数据失败: " + err.message);
      }).finally(() => {
        this.loading = false;
      });
    },
    parseErrorMessageForDebugPlot() {
      const errorMessage = this.task.errorMessage;
      if (!errorMessage) {
          this.errorMessageForDisplay = '';
          return;
      }

      // Default to showing the full error message
      this.errorMessageForDisplay = errorMessage;

      // Try to extract the debug plot file name
      const match = errorMessage.match(/'(debug_scatter_.*\.png)'/);
      if (!match || !match[1]) {
          return; // No debug plot mentioned, we're done.
      }

      const debugPlotFileName = match[1];
      const outputDirPath = this.task.outputDirPath;

      if (outputDirPath && outputDirPath.startsWith('/profile/')) {
           const baseUrl = process.env.VUE_APP_BASE_API;
           
           // Correctly construct the full, web-accessible URL
           // The outputDirPath already starts with /profile, which is needed for the backend's static resource mapping.
           this.debugPlotUrl = baseUrl + outputDirPath + '/' + debugPlotFileName;
           
           // For a cleaner UI, show only the part of the error message before the plot info.
           const plotInfoIndex = errorMessage.indexOf("A debug scatter plot was generated");
           if (plotInfoIndex !== -1) {
               this.errorMessageForDisplay = errorMessage.substring(0, plotInfoIndex).trim();
           }
      }
    },
    statusTagType(status) {
        if (status === 'COMPLETED') return 'success';
        if (status === 'FAILED') return 'danger';
        if (status === 'PROCESSING') return 'primary';
        return 'info';
    }
  }
};
</script>

<style scoped>
.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}
.clearfix:after {
  clear: both
}
.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 200px;
  background: #f5f7fa;
  color: #909399;
  font-size: 30px;
}
</style> 