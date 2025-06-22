<template>
  <div class="advanced-results-visualization">
    <!-- 算法类型检测和可视化路由 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="error" class="error-container">
      <el-alert :title="error" type="error" show-icon :closable="false" />
    </div>

    <div v-else class="visualization-content">
      <!-- 回归算法可视化 -->
      <RegressionVisualization
        v-if="algorithmCategory === 'regression'"
        :results="results"
        :task-info="taskInfo"
        :algorithm-type="algorithmType"
        @chart-ready="onChartReady"
      />

      <!-- 分类算法可视化 -->
      <ClassificationVisualization
        v-else-if="algorithmCategory === 'classification'"
        :results="results"
        :task-info="taskInfo"
        :algorithm-type="algorithmType"
        @chart-ready="onChartReady"
      />

      <!-- 聚类算法可视化 -->
      <ClusteringVisualization
        v-else-if="algorithmCategory === 'clustering'"
        :results="results"
        :task-info="taskInfo"
        :algorithm-type="algorithmType"
        @chart-ready="onChartReady"
      />

      <!-- 特征工程可视化 -->
      <FeatureEngineeringVisualization
        v-else-if="algorithmCategory === 'feature_engineering'"
        :results="results"
        :task-info="taskInfo"
        :algorithm-type="algorithmType"
        @chart-ready="onChartReady"
      />

      <!-- 时间序列可视化 -->
      <TimeSeriesVisualization
        v-else-if="algorithmCategory === 'time_series'"
        :results="results"
        :task-info="taskInfo"
        :algorithm-type="algorithmType"
        @chart-ready="onChartReady"
      />

      <!-- 预测任务可视化 -->
      <PredictionVisualization
        v-else-if="algorithmCategory === 'prediction'"
        :results="results"
        :task-info="taskInfo"
        :algorithm-type="algorithmType"
        @chart-ready="onChartReady"
      />

      <!-- 未知算法类型 -->
      <div v-else class="unknown-algorithm">
        <el-alert
          title="未知的算法类型"
          :description="`算法类型 '${algorithmType}' 暂不支持高级可视化`"
          type="warning"
          show-icon
          :closable="false">
        </el-alert>
        
        <!-- 显示原始结果 -->
        <el-card class="raw-results-card" shadow="never">
          <div slot="header">
            <span>📊 原始分析结果</span>
          </div>
          <el-input
            type="textarea"
            :rows="15"
            :value="formattedResults"
            readonly
            placeholder="暂无结果数据">
          </el-input>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script>
// 导入各种算法类型的可视化组件
import RegressionVisualization from './visualizations/RegressionVisualization.vue';
import ClassificationVisualization from './visualizations/ClassificationVisualization.vue';
import ClusteringVisualization from './visualizations/ClusteringVisualization.vue';
import FeatureEngineeringVisualization from './visualizations/FeatureEngineeringVisualization.vue';
import TimeSeriesVisualization from './visualizations/TimeSeriesVisualization.vue';
import PredictionVisualization from './visualizations/PredictionVisualization.vue';

export default {
  name: "AdvancedResultsVisualization",
  components: {
    RegressionVisualization,
    ClassificationVisualization,
    ClusteringVisualization,
    FeatureEngineeringVisualization,
    TimeSeriesVisualization,
    PredictionVisualization
  },
  props: {
    sourceId: {
      type: String,
      required: true
    },
    sourceType: {
      type: String,
      required: true
    },
    results: {
      type: Object,
      default: () => ({})
    },
    taskInfo: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      loading: false,
      error: null,
      chartInstances: [] // 存储所有图表实例
    };
  },
  computed: {
    algorithmType() {
      const algorithm = this.taskInfo.algorithm || '';
      console.log('🔍 算法类型:', algorithm);
      return algorithm;
    },

    algorithmCategory() {
      const algorithm = this.algorithmType.toLowerCase();
      console.log('🔍 算法类型 (小写):', algorithm);

      // 回归算法
      if ((algorithm.includes('regression') ||
           algorithm.includes('linear') ||
           algorithm.includes('polynomial') ||
           algorithm.includes('exponential') ||
           algorithm.includes('logarithmic') ||
           algorithm.includes('xgboost_regression') ||
           algorithm.includes('lightgbm_regression') ||
           algorithm.includes('random_forest_regression') ||
           algorithm.includes('svm_regression')) &&
          !algorithm.includes('predict')) {
        console.log('✅ 识别为回归算法');
        return 'regression';
      }

      // 分类算法
      if (algorithm.includes('classification') ||
          algorithm.includes('logistic') ||
          algorithm.includes('svm') ||
          algorithm.includes('knn') ||
          algorithm.includes('naive_bayes') ||
          algorithm.includes('decision_tree')) {
        console.log('✅ 识别为分类算法');
        return 'classification';
      }

      // 聚类算法
      if (algorithm.includes('clustering') ||
          algorithm.includes('kmeans') ||
          algorithm.includes('dbscan') ||
          algorithm.includes('hierarchical')) {
        console.log('✅ 识别为聚类算法');
        return 'clustering';
      }

      // 特征工程
      if (algorithm.includes('feature_engineering') ||
          algorithm.includes('fractal_dimension') ||
          algorithm.includes('automatic_regression')) {
        console.log('✅ 识别为特征工程算法');
        return 'feature_engineering';
      }

      // 时间序列
      if (algorithm.includes('bilstm') ||
          algorithm.includes('tcn') ||
          algorithm.includes('lstm')) {
        console.log('✅ 识别为时间序列算法');
        return 'time_series';
      }

      // 预测任务
      if (algorithm.includes('predict')) {
        console.log('✅ 识别为预测任务');
        return 'prediction';
      }

      console.log('⚠️ 未知算法类型');
      return 'unknown';
    },

    formattedResults() {
      return this.results ? JSON.stringify(this.results, null, 2) : '';
    }
  },

  watch: {
    results: {
      handler(newResults) {
        console.log('📊 高级可视化组件接收到结果数据:', newResults);
      },
      immediate: true,
      deep: true
    },
    taskInfo: {
      handler(newTaskInfo) {
        console.log('📋 高级可视化组件接收到任务信息:', newTaskInfo);
      },
      immediate: true,
      deep: true
    }
  },
  beforeDestroy() {
    // 销毁所有图表实例
    this.chartInstances.forEach(chart => {
      if (chart && typeof chart.dispose === 'function') {
        chart.dispose();
      }
    });
  },
  methods: {
    onChartReady(chartInstance) {
      if (chartInstance) {
        this.chartInstances.push(chartInstance);
      }
    }
  }
};
</script>

<style scoped>
.advanced-results-visualization {
  padding: 0;
}

.loading-container,
.error-container {
  padding: 40px 0;
  text-align: center;
}

.visualization-content {
  min-height: 400px;
}

.unknown-algorithm {
  padding: 20px 0;
}

.raw-results-card {
  margin-top: 20px;
}
</style>
