<template>
  <div class="app-container">
    <!-- 页面头部统计 -->
    <el-row :gutter="20" class="header-stats">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-number">{{ stats.totalModels }}</div>
            <div class="stat-label">可用模型</div>
          </div>
          <i class="el-icon-cpu stat-icon"></i>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-number">{{ stats.totalPredictions }}</div>
            <div class="stat-label">预测任务</div>
          </div>
          <i class="el-icon-magic-stick stat-icon"></i>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card running">
          <div class="stat-content">
            <div class="stat-number">{{ stats.runningPredictions }}</div>
            <div class="stat-label">运行中</div>
          </div>
          <i class="el-icon-loading stat-icon"></i>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card completed">
          <div class="stat-content">
            <div class="stat-number">{{ stats.completedPredictions }}</div>
            <div class="stat-label">已完成</div>
          </div>
          <i class="el-icon-circle-check stat-icon"></i>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主要功能区域 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <!-- 回归预测 -->
      <el-tab-pane label="📊 回归预测" name="regression">
        <enhanced-regression-prediction
          ref="regressionPrediction"
          @refresh="refreshData"
          @switch-tab="switchTab"
          @prediction-completed="onPredictionCompleted" />
      </el-tab-pane>

      <!-- 预测历史 -->
      <el-tab-pane label="📋 预测历史" name="history">
        <prediction-history
          ref="predictionHistory"
          @refresh="refreshData" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import EnhancedRegressionPrediction from './components/EnhancedRegressionPrediction.vue'
import PredictionHistory from './components/PredictionHistory.vue'

export default {
  name: "PredictionIndex",
  components: {
    EnhancedRegressionPrediction,
    PredictionHistory
  },
  data() {
    return {
      activeTab: 'regression',
      stats: {
        totalModels: 0,
        totalPredictions: 0,
        runningPredictions: 0,
        completedPredictions: 0
      }
    }
  },
  created() {
    this.loadStats()
    this.handleRouteParams()
  },
  methods: {
    /** 处理路由参数 */
    handleRouteParams() {
      const { modelId, predictionName, tab } = this.$route.query

      // 如果有指定标签页，切换到对应标签页
      if (tab) {
        this.activeTab = tab
      }

      // 如果有模型ID，传递给回归预测组件
      if (modelId) {
        this.$nextTick(() => {
          if (this.$refs.regressionPrediction && this.$refs.regressionPrediction.setModelFromRoute) {
            this.$refs.regressionPrediction.setModelFromRoute({
              modelId: modelId,
              predictionName: predictionName
            })
          }
        })
      }
    },

    /** 加载统计数据 */
    loadStats() {
      // 尝试调用真实API
      import('@/api/petrol/prediction').then(module => {
        if (module.getStats) {
          module.getStats().then(response => {
            this.stats = response.data || this.stats
          }).catch(() => {
            // API调用失败，显示0值
            this.stats = {
              totalModels: 0,
              totalPredictions: 0,
              runningPredictions: 0,
              completedPredictions: 0
            }
          })
        } else {
          // API方法不存在，显示0值
          this.stats = {
            totalModels: 0,
            totalPredictions: 0,
            runningPredictions: 0,
            completedPredictions: 0
          }
        }
      }).catch(() => {
        // 模块加载失败，显示0值
        this.stats = {
          totalModels: 0,
          totalPredictions: 0,
          runningPredictions: 0,
          completedPredictions: 0
        }
      })
    },

    /** 标签页切换 */
    handleTabClick(tab) {
      this.activeTab = tab.name
    },

    /** 切换标签页 */
    switchTab(tabName) {
      this.activeTab = tabName
    },

    /** 刷新数据 */
    refreshData() {
      this.loadStats()
      // 通知子组件刷新
      this.$nextTick(() => {
        if (this.$refs.regressionPrediction && this.$refs.regressionPrediction.refresh) {
          this.$refs.regressionPrediction.refresh()
        }

        if (this.$refs.predictionHistory && this.$refs.predictionHistory.refresh) {
          this.$refs.predictionHistory.refresh()
        }
      })
    },

    /** 预测完成处理 */
    onPredictionCompleted() {
      console.log('预测任务完成，刷新数据和历史记录');

      // 刷新统计数据
      this.loadStats();

      // 自动切换到预测历史标签页
      this.activeTab = 'history';

      // 刷新预测历史
      this.$nextTick(() => {
        if (this.$refs.predictionHistory && this.$refs.predictionHistory.refresh) {
          this.$refs.predictionHistory.refresh();
        }
      });

      // 显示成功提示
      this.$message({
        message: '预测任务已完成，已自动切换到预测历史查看结果',
        type: 'success',
        duration: 3000
      });
    },


  }
}
</script>

<style scoped>
.header-stats {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.stat-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.stat-icon {
  font-size: 32px;
  color: #409eff;
  margin-top: 10px;
}

.running .stat-number,
.running .stat-icon {
  color: #e6a23c;
}

.completed .stat-number,
.completed .stat-icon {
  color: #67c23a;
}
</style>
