<template>
  <div class="task-results">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 错误状态 -->
    <el-alert
      v-else-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false">
    </el-alert>

    <!-- 无结果状态 -->
    <el-empty
      v-else-if="!hasResults"
      description="该任务暂无分析结果"
      :image-size="200">
    </el-empty>

    <!-- 结果展示 -->
    <div v-else class="results-content">
      <!-- 结果标签页 -->
      <el-card class="results-card" shadow="never">
        <div slot="header">
          <span>📊 分析结果</span>
          <el-button
            type="text"
            icon="el-icon-refresh"
            @click="loadTaskResults"
            :loading="loading">
            刷新数据
          </el-button>
        </div>

        <el-tabs v-model="activeResultTab" type="card">
          <!-- 统计指标 -->
          <el-tab-pane
            v-if="statisticalResults.length > 0"
            label="📊 统计指标"
            name="statistics">
            <div class="statistics-section">
              <el-table
                :data="statisticalResults"
                border
                stripe
                style="width: 100%">
                <el-table-column prop="metric" label="指标名称" width="200">
                  <template slot-scope="scope">
                    <strong>{{ getMetricDisplayName(scope.row.metric) }}</strong>
                  </template>
                </el-table-column>
                <el-table-column prop="value" label="数值">
                  <template slot-scope="scope">
                    <span class="metric-value">{{ formatMetricValue(scope.row.value) }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="说明">
                  <template slot-scope="scope">
                    <span class="metric-desc">{{ getMetricDescription(scope.row.metric) }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <!-- 结果可视化 -->
          <el-tab-pane
            v-if="hasResults"
            label="🎯 结果可视化"
            name="result_charts">
            <div class="result-charts-section">
              <!-- 调试工具 -->
              <div style="margin-bottom: 20px; text-align: right;">
                <el-button size="mini" type="info" @click="showDebugInfo = !showDebugInfo">
                  {{ showDebugInfo ? '隐藏调试' : '显示调试' }}
                </el-button>
              </div>

              <!-- 调试信息 -->
              <el-card v-if="showDebugInfo" class="debug-info-card" shadow="never" style="margin-bottom: 20px;">
                <div slot="header">
                  <span>🐛 调试信息</span>
                  <el-tag :type="getAlgorithmCategoryTagType()" style="margin-left: 10px;">
                    {{ getAlgorithmCategoryName() }}
                  </el-tag>
                </div>

                <el-collapse>
                  <el-collapse-item title="📊 结果数据结构" name="results">
                    <pre style="background: #f5f5f5; padding: 10px; border-radius: 4px; font-size: 12px; max-height: 300px; overflow-y: auto;">{{ JSON.stringify(results, null, 2) }}</pre>
                  </el-collapse-item>

                  <el-collapse-item title="📋 任务信息" name="taskInfo">
                    <pre style="background: #f5f5f5; padding: 10px; border-radius: 4px; font-size: 12px; max-height: 300px; overflow-y: auto;">{{ JSON.stringify(taskInfo, null, 2) }}</pre>
                  </el-collapse-item>

                  <el-collapse-item title="🖼️ 可视化图表" name="plots">
                    <div v-if="visualizationPlots.length > 0">
                      <div v-for="(plot, index) in visualizationPlots" :key="index" style="margin-bottom: 10px;">
                        <strong>{{ plot.name }}:</strong>
                        <a :href="plot.url" target="_blank" style="margin-left: 10px;">{{ plot.url }}</a>
                        <el-button size="mini" @click="testImageUrl(plot.url)" style="margin-left: 10px;">测试链接</el-button>
                      </div>
                    </div>
                    <div v-else style="color: #999;">暂无可视化图表</div>
                  </el-collapse-item>
                </el-collapse>
              </el-card>

              <!-- 专用算法可视化 -->
              <div v-if="algorithmCategory === 'regression'">
                <RegressionResultsVisualization
                  :results="results"
                  :task-info="taskInfo"
                  :algorithm-type="taskInfo.algorithm || 'unknown'"
                />
              </div>

              <div v-else-if="algorithmCategory === 'classification'">
                <ClassificationResultsVisualization
                  :results="results"
                  :task-info="taskInfo"
                  :algorithm-type="taskInfo.algorithm || 'unknown'"
                />
              </div>

              <div v-else-if="algorithmCategory === 'clustering'">
                <ClusteringResultsVisualization
                  :results="results"
                  :task-info="taskInfo"
                  :algorithm-type="taskInfo.algorithm || 'unknown'"
                />
              </div>

              <div v-else-if="algorithmCategory === 'feature_engineering'">
                <FeatureEngineeringResultsVisualization
                  :results="results"
                  :task-info="taskInfo"
                  :algorithm-type="taskInfo.algorithm || 'unknown'"
                />
              </div>

              <div v-else-if="algorithmCategory === 'time_series'">
                <TimeSeriesResultsVisualization
                  :results="results"
                  :task-info="taskInfo"
                  :algorithm-type="taskInfo.algorithm || 'unknown'"
                />
              </div>

              <!-- 通用可视化 -->
              <div v-else>
                <GenericResultsVisualization
                  :results="results"
                  :task-info="taskInfo"
                  :algorithm-type="taskInfo.algorithm || 'unknown'"
                />
              </div>

              <!-- 无数据提示 -->
              <el-empty v-if="!hasResults"
                        description="暂无可视化数据"
                        :image-size="200">
              </el-empty>
            </div>
          </el-tab-pane>

          <!-- 原始结果图片 -->
          <el-tab-pane
            v-if="visualizationPlots.length > 0"
            label="🖼️ 原始图表"
            name="original_plots">
            <div class="original-plots-section">
              <el-row :gutter="20">
                <el-col
                  v-for="(plot, index) in visualizationPlots"
                  :key="index"
                  :span="12"
                  style="margin-bottom: 20px;">
                  <div class="plot-container">
                    <h5>{{ getPlotDisplayName(plot.name) }}</h5>
                    <el-image
                      :src="plot.url"
                      :preview-src-list="[plot.url]"
                      fit="contain"
                      style="width: 100%; max-height: 400px;">
                      <div slot="error" class="image-slot">
                        <i class="el-icon-picture-outline"></i>
                        <p>图片加载失败</p>
                      </div>
                    </el-image>
                  </div>
                </el-col>
              </el-row>
            </div>
          </el-tab-pane>

          <!-- 原始结果数据 -->
          <el-tab-pane label="📋 原始数据" name="raw">
            <div class="raw-results-section">
              <el-input
                type="textarea"
                :rows="20"
                :value="formattedRawResults"
                readonly
                placeholder="暂无原始结果数据">
              </el-input>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script>
import { getDataSourceInfo, readDataSourceData } from "@/api/petrol/visualization";
import { getTask } from "@/api/petrol/task";
import * as echarts from 'echarts';

// 导入各种算法类型的可视化组件
import RegressionResultsVisualization from '@/views/petrol/visualization/components/simple/results/RegressionResultsVisualization.vue';
import ClassificationResultsVisualization from '@/views/petrol/visualization/components/simple/results/ClassificationResultsVisualization.vue';
import ClusteringResultsVisualization from '@/views/petrol/visualization/components/simple/results/ClusteringResultsVisualization.vue';
import FeatureEngineeringResultsVisualization from '@/views/petrol/visualization/components/simple/results/FeatureEngineeringResultsVisualization.vue';
import TimeSeriesResultsVisualization from '@/views/petrol/visualization/components/simple/results/TimeSeriesResultsVisualization.vue';
import GenericResultsVisualization from '@/views/petrol/visualization/components/simple/results/GenericResultsVisualization.vue';

export default {
  name: "TaskResults",
  components: {
    RegressionResultsVisualization,
    ClassificationResultsVisualization,
    ClusteringResultsVisualization,
    FeatureEngineeringResultsVisualization,
    TimeSeriesResultsVisualization,
    GenericResultsVisualization
  },
  beforeCreate() {
    // 将echarts挂载到Vue实例上
    this.$echarts = echarts;
  },
  props: {
    sourceId: {
      type: [String, Number],
      required: true
    },
    sourceType: {
      type: String,
      required: true,
      default: 'task',
      validator: value => ['task', 'dataset'].includes(value)
    }
  },
  data() {
    return {
      loading: false,
      chartsLoading: false,
      error: null,
      taskInfo: {},
      results: {},
      statisticalResults: [],
      visualizationPlots: [],
      debugPlotUrl: null,
      activeResultTab: 'statistics',
      showDebugInfo: false,
      // 分析数据
      inputData: [],
      predictions: [],
      actualValues: [],
      featureImportanceData: [],
      // 图表实例
      predictionChart: null,
      residualChart: null,
      featureImportanceChart: null,
      distributionChart: null
    };
  },
  computed: {
    hasResults() {
      return this.statisticalResults.length > 0 ||
             this.visualizationPlots.length > 0 ||
             (this.results && Object.keys(this.results).length > 0);
    },
    formattedRawResults() {
      return this.results ? JSON.stringify(this.results, null, 2) : '';
    },
    isRegressionTask() {
      return this.taskInfo.algorithm && this.taskInfo.algorithm.includes('regression');
    },
    isClusteringTask() {
      return this.taskInfo.algorithm && this.taskInfo.algorithm.includes('cluster');
    },

    // 图表数据相关计算属性
    hasPredictionData() {
      const predictions = this.results.predictions || this.results.y_pred || [];
      const actualValues = this.results.actual_values || this.results.y_test || [];
      return predictions.length > 0 && actualValues.length > 0;
    },

    hasFeatureImportance() {
      const importance = this.results.feature_importance;
      return importance && Object.keys(importance).length > 0;
    },

    hasDistributionData() {
      const actualValues = this.results.actual_values || this.results.y_test || [];
      return actualValues.length > 0;
    },

    // 算法分类识别
    algorithmCategory() {
      const algorithm = (this.taskInfo.algorithm || '').toLowerCase();
      const algorithmType = this.results.algorithm_type;

      console.log('🔍 算法类型识别:', { algorithm, algorithmType, results: this.results });

      // 优先使用后端返回的算法类型
      if (algorithmType) {
        console.log('✅ 使用后端算法类型:', algorithmType);
        return algorithmType;
      }

      // 回归算法 - 精确匹配
      const regressionKeywords = [
        'xgboost', 'lightgbm', 'bilstm', 'tcn',
        'linear_regression', 'polynomial_regression', 'exponential_regression',
        'svm_regression', 'random_forest_regression'
      ];

      if (regressionKeywords.some(keyword => algorithm.includes(keyword)) ||
          (algorithm.includes('regression') && !algorithm.includes('predict'))) {
        console.log('✅ 识别为回归算法');
        return 'regression';
      }

      // 分类算法 - 精确匹配
      const classificationKeywords = [
        'logistic_regression', 'svm_classification', 'knn_classification',
        'decision_tree_classification', 'random_forest_classification',
        'naive_bayes', 'neural_network_classification'
      ];

      if (classificationKeywords.some(keyword => algorithm.includes(keyword)) ||
          algorithm.includes('classification')) {
        console.log('✅ 识别为分类算法');
        return 'classification';
      }

      // 聚类算法 - 精确匹配
      const clusteringKeywords = [
        'kmeans', 'dbscan', 'hierarchical_clustering',
        'gaussian_mixture', 'spectral_clustering'
      ];

      if (clusteringKeywords.some(keyword => algorithm.includes(keyword)) ||
          algorithm.includes('clustering')) {
        console.log('✅ 识别为聚类算法');
        return 'clustering';
      }

      // 特征工程 - 精确匹配
      const featureEngineeringKeywords = [
        'fractal_dimension', 'feature_engineering', 'automatic_regression',
        'feature_selection', 'dimensionality_reduction'
      ];

      if (featureEngineeringKeywords.some(keyword => algorithm.includes(keyword))) {
        console.log('✅ 识别为特征工程算法');
        return 'feature_engineering';
      }

      // 时间序列 - 精确匹配 (纯时间序列分析，非回归)
      const timeSeriesKeywords = [
        'arima', 'prophet', 'seasonal_decompose'
      ];

      if (timeSeriesKeywords.some(keyword => algorithm.includes(keyword)) ||
          (algorithm.includes('lstm') && !algorithm.includes('bilstm'))) {
        console.log('✅ 识别为时间序列算法');
        return 'time_series';
      }

      // 预测任务
      if (algorithm.includes('predict')) {
        console.log('✅ 识别为预测任务');
        return 'prediction';
      }

      console.log('⚠️ 未知算法类型，使用通用显示');
      return 'generic';
    },

    // 新增的计算属性
    hasStatistics() {
      const stats = this.results.statistics || this.results.metrics || {};
      return Object.keys(stats).length > 0;
    },

    statisticsTableData() {
      const stats = this.results.statistics || this.results.metrics || {};
      return Object.entries(stats).map(([key, value]) => ({
        metric: key,
        value: value,
        description: this.getMetricDescription(key)
      }));
    }
  },
  created() {
    this.loadTaskInfo();
    this.loadTaskResults();
  },

  beforeDestroy() {
    // 销毁图表实例
    [this.predictionChart, this.residualChart, this.featureImportanceChart, this.distributionChart].forEach(chart => {
      if (chart) chart.dispose();
    });
  },

  watch: {
    // 监听sourceId变化，重新加载数据
    sourceId: {
      handler(newVal, oldVal) {
        if (newVal && newVal !== oldVal) {
          console.log('📊 TaskResults sourceId变化:', { oldVal, newVal });
          this.loadTaskInfo();
          this.loadTaskResults();
        }
      },
      immediate: false
    },

    // 监听sourceType变化
    sourceType: {
      handler(newVal, oldVal) {
        if (newVal && newVal !== oldVal) {
          console.log('📊 TaskResults sourceType变化:', { oldVal, newVal });
          this.loadTaskInfo();
          this.loadTaskResults();
        }
      },
      immediate: false
    }
  },



  methods: {
    /** 加载任务基本信息 */
    async loadTaskInfo() {
      try {
        // 确保sourceId是字符串类型，sourceType有默认值
        const sourceId = String(this.sourceId);
        const sourceType = this.sourceType || 'task';

        const response = await getDataSourceInfo(sourceId, sourceType);
        this.taskInfo = response.data || {};
      } catch (error) {
        console.error("加载任务信息失败:", error);
      }
    },

    /** 加载任务结果 */
    async loadTaskResults() {
      this.loading = true;
      this.error = null;

      try {
        // 确保sourceId是字符串类型，sourceType有默认值
        const sourceId = String(this.sourceId);
        const sourceType = this.sourceType || 'task';

        console.log('📊 开始加载任务结果', {
          sourceId,
          sourceType
        });

        const taskId = sourceId.replace('task_', '');
        console.log('📊 解析任务ID:', taskId);

        const response = await getTask(taskId);
        console.log('📊 任务API响应:', response);

        const task = response.data;

        if (!task) {
          this.error = "任务不存在";
          console.error('❌ 任务不存在');
          return;
        }

        console.log('📊 任务详情:', {
          id: task.id,
          taskName: task.taskName,
          algorithm: task.algorithm,
          status: task.status,
          hasResultsJson: !!task.resultsJson,
          resultsJsonLength: task.resultsJson ? task.resultsJson.length : 0
        });

        // 更新任务信息
        this.taskInfo = {
          ...this.taskInfo,
          id: task.id,
          taskName: task.taskName,
          algorithm: task.algorithm,
          status: task.status,
          createTime: task.createTime,
          updateTime: task.updateTime
        };

        if (task.status !== 'COMPLETED') {
          this.error = `任务状态为 ${task.status}，暂无结果数据`;
          console.warn('⚠️ 任务未完成:', task.status);
          return;
        }

        if (!task.resultsJson) {
          this.error = "任务已完成但无结果数据";
          console.warn('⚠️ 任务无结果数据');
          return;
        }

        // 解析结果JSON
        try {
          this.results = JSON.parse(task.resultsJson);
          console.log('📊 解析结果数据成功:', this.results);
          this.parseResults();
          this.parseDebugInfo(task);
        } catch (e) {
          this.error = "结果数据解析失败: " + e.message;
          console.error('❌ 结果数据解析失败:', e);
        }

      } catch (error) {
        this.error = "加载任务结果失败: " + error.message;
        console.error('❌ 加载任务结果失败:', error);
      } finally {
        this.loading = false;
      }
    },

    /** 解析结果数据 */
    parseResults() {
      console.log('🔍 开始解析结果数据:', this.results);

      // 解析统计数据
      if (this.results.statistics) {
        this.statisticalResults = Object.entries(this.results.statistics).map(([key, value]) => ({
          metric: key,
          value: value,
          description: this.getMetricDescription(key)
        }));
        console.log('📊 解析统计数据:', this.statisticalResults);
      }

      // 解析可视化图表
      if (this.results.visualizations) {
        const baseUrl = process.env.VUE_APP_BASE_API || '';
        this.visualizationPlots = Object.entries(this.results.visualizations).map(([key, value]) => {
          // 处理不同的URL格式
          let imageUrl = value;
          if (typeof value === 'string') {
            if (value.startsWith('http')) {
              imageUrl = value; // 完整URL
            } else if (value.startsWith('/')) {
              imageUrl = baseUrl + value; // 绝对路径
            } else {
              imageUrl = baseUrl + '/' + value; // 相对路径
            }
          }

          console.log(`📊 解析图片: ${key} -> ${imageUrl}`);
          return {
            name: key,
            url: imageUrl
          };
        });

        console.log(`📊 解析到 ${this.visualizationPlots.length} 个可视化图表`);
      }

      // 解析预测数据（用于回归任务）
      if (this.results.predictions) {
        this.predictions = this.results.predictions;
      }

      if (this.results.actual_values) {
        this.actualValues = this.results.actual_values;
      }

      // 解析特征重要性数据
      if (this.results.feature_importance) {
        this.featureImportanceData = Object.entries(this.results.feature_importance).map(([feature, importance]) => ({
          feature,
          importance
        }));
      }

      // 设置默认标签页
      if (this.statisticalResults.length > 0) {
        this.activeResultTab = 'statistics';
      } else if (this.hasPredictionData || this.hasFeatureImportance || this.hasDistributionData) {
        this.activeResultTab = 'result_charts';
      } else if (this.visualizationPlots.length > 0) {
        this.activeResultTab = 'original_plots';
      } else {
        this.activeResultTab = 'raw';
      }

      // 加载输入数据
      this.loadInputData();

      // 如果有图表数据，自动渲染图表
      if (this.hasPredictionData || this.hasFeatureImportance || this.hasDistributionData) {
        this.$nextTick(() => {
          this.renderSimpleCharts();
        });
      }
    },

    /** 解析调试信息 */
    parseDebugInfo(task) {
      console.log('🔍 解析调试信息:', task);

      // 检查错误信息中的调试图片
      if (task.errorMessage && task.errorMessage.includes("debug scatter plot")) {
        const match = task.errorMessage.match(/debug scatter plot was generated at: (.+\.png)/);
        if (match) {
          const debugPlotFileName = match[1];
          const outputDirPath = task.outputDirPath;

          if (outputDirPath) {
            const baseUrl = process.env.VUE_APP_BASE_API || '';
            let debugUrl;

            if (outputDirPath.startsWith('/profile/')) {
              debugUrl = baseUrl + outputDirPath + '/' + debugPlotFileName;
            } else {
              debugUrl = baseUrl + '/profile/' + outputDirPath + '/' + debugPlotFileName;
            }

            this.debugPlotUrl = debugUrl;
            console.log('🖼️ 调试图片URL:', this.debugPlotUrl);
          }
        }
      }

      // 检查输出目录中的其他图片
      if (task.outputDirPath) {
        const baseUrl = process.env.VUE_APP_BASE_API || '';
        const outputPath = task.outputDirPath.startsWith('/profile/')
          ? task.outputDirPath
          : '/profile/' + task.outputDirPath;

        // 不再自动添加常见图片文件，只使用结果中明确指定的可视化图表
      }
    },

    /** 获取算法显示名称 */
    getAlgorithmName(algorithm) {
      const names = {
        'regression_linear_train': '线性回归训练',
        'regression_exponential_train': '指数回归训练',
        'regression_random_forest_train': '随机森林回归训练',
        'cluster_kmeans': 'K均值聚类',
        'cluster_dbscan': 'DBSCAN聚类',
        'fractal_dimension': '分形维数分析'
      };
      return names[algorithm] || algorithm;
    },

    /** 获取状态类型 */
    getStatusType(status) {
      const types = {
        'COMPLETED': 'success',
        'RUNNING': 'warning',
        'QUEUED': 'info',
        'FAILED': 'danger'
      };
      return types[status] || 'info';
    },

    /** 获取状态名称 */
    getStatusName(status) {
      const names = {
        'COMPLETED': '已完成',
        'RUNNING': '运行中',
        'QUEUED': '排队中',
        'FAILED': '失败'
      };
      return names[status] || status;
    },

    /** 获取指标显示名称 */
    getMetricDisplayName(metric) {
      const names = {
        'mse': '均方误差 (MSE)',
        'rmse': '均方根误差 (RMSE)',
        'mae': '平均绝对误差 (MAE)',
        'r2': '决定系数 (R²)',
        'accuracy': '准确率',
        'precision': '精确率',
        'recall': '召回率',
        'f1_score': 'F1分数',
        'silhouette_score': '轮廓系数',
        'inertia': '惯性',
        'fractal_dimension': '分形维数'
      };
      return names[metric] || metric;
    },

    /** 获取指标描述 */
    getMetricDescription(metric) {
      const descriptions = {
        'mse': '预测值与真实值差值的平方的平均值',
        'rmse': '均方误差的平方根，与目标变量同单位',
        'mae': '预测值与真实值差值绝对值的平均值',
        'r2': '模型解释的方差比例，越接近1越好',
        'accuracy': '正确预测的样本比例',
        'precision': '预测为正例中实际为正例的比例',
        'recall': '实际正例中被正确预测的比例',
        'f1_score': '精确率和召回率的调和平均数',
        'silhouette_score': '聚类效果评估，范围[-1,1]，越大越好',
        'inertia': '样本到其聚类中心距离平方和',
        'fractal_dimension': '数据的分形维数特征'
      };
      return descriptions[metric] || '暂无描述';
    },

    /** 获取算法分类显示名称 */
    getAlgorithmCategoryName() {
      const categoryNames = {
        'regression': '回归算法',
        'classification': '分类算法',
        'clustering': '聚类算法',
        'feature_engineering': '特征工程',
        'time_series': '时间序列',
        'prediction': '预测任务',
        'generic': '通用算法'
      };
      return categoryNames[this.algorithmCategory] || '未知类型';
    },

    /** 获取算法分类标签类型 */
    getAlgorithmCategoryTagType() {
      const tagTypes = {
        'regression': 'success',
        'classification': 'primary',
        'clustering': 'warning',
        'feature_engineering': 'info',
        'time_series': 'danger',
        'prediction': 'success',
        'generic': ''
      };
      return tagTypes[this.algorithmCategory] || '';
    },

    /** 测试图片URL是否可访问 */
    testImageUrl(url) {
      console.log('🔍 测试图片URL:', url);

      // 尝试在新窗口打开
      const newWindow = window.open(url, '_blank');

      // 创建一个img元素测试加载
      const img = new Image();
      img.onload = () => {
        console.log('✅ 图片加载成功:', url);
        this.$message.success('图片可以正常访问');
      };
      img.onerror = () => {
        console.error('❌ 图片加载失败:', url);
        this.$message.error('图片无法访问，请检查路径');
      };
      img.src = url;
    },

    /** 渲染简单图表 */
    renderSimpleCharts() {
      console.log('🎨 开始渲染简单图表');

      setTimeout(() => {
        this.renderSimpleChart1();
        this.renderSimpleChart2();
      }, 200);
    },

    /** 渲染简单图表1 - 拟合效果图 */
    renderSimpleChart1() {
      const chartDom = this.$refs.simpleChart1;
      if (!chartDom) return;

      const chart = echarts.init(chartDom);

      const predictions = this.results.predictions || this.results.y_pred || [];
      const actualValues = this.results.actual_values || this.results.y_true || [];

      if (predictions.length === 0 || actualValues.length === 0) {
        chart.setOption({
          title: {
            text: '暂无拟合数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      const indices = Array.from({length: Math.min(predictions.length, actualValues.length, 100)}, (_, i) => i);

      const option = {
        title: {
          text: '模型拟合效果',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['实际值', '预测值'],
          top: 30
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '20%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: indices,
          name: '样本索引'
        },
        yAxis: {
          type: 'value',
          name: '数值'
        },
        series: [
          {
            name: '实际值',
            type: 'line',
            data: actualValues.slice(0, 100),
            itemStyle: { color: '#5470c6' },
            symbol: 'circle',
            symbolSize: 4
          },
          {
            name: '预测值',
            type: 'line',
            data: predictions.slice(0, 100),
            itemStyle: { color: '#91cc75' },
            symbol: 'triangle',
            symbolSize: 4
          }
        ]
      };

      chart.setOption(option);
    },

    /** 渲染简单图表2 - 预测vs实际散点图 */
    renderSimpleChart2() {
      const chartDom = this.$refs.simpleChart2;
      if (!chartDom) return;

      const chart = echarts.init(chartDom);

      const predictions = this.results.predictions || this.results.y_pred || [];
      const actualValues = this.results.actual_values || this.results.y_true || [];

      if (predictions.length === 0 || actualValues.length === 0) {
        chart.setOption({
          title: {
            text: '暂无预测数据',
            left: 'center',
            top: 'middle',
            textStyle: { color: '#999', fontSize: 16 }
          }
        });
        return;
      }

      const scatterData = actualValues.map((actual, index) => [actual, predictions[index]]).slice(0, 200);
      const allValues = [...actualValues, ...predictions];
      const minVal = Math.min(...allValues);
      const maxVal = Math.max(...allValues);

      const option = {
        title: {
          text: '预测值 vs 实际值',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            return `实际值: ${params.data[0].toFixed(4)}<br/>预测值: ${params.data[1].toFixed(4)}`;
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          name: '实际值',
          min: minVal,
          max: maxVal
        },
        yAxis: {
          type: 'value',
          name: '预测值',
          min: minVal,
          max: maxVal
        },
        series: [
          {
            name: '预测点',
            type: 'scatter',
            data: scatterData,
            itemStyle: { color: '#5470c6', opacity: 0.6 },
            symbolSize: 6
          },
          {
            name: '理想线',
            type: 'line',
            data: [[minVal, minVal], [maxVal, maxVal]],
            itemStyle: { color: '#ff6b6b' },
            lineStyle: { type: 'dashed', width: 2 },
            symbol: 'none'
          }
        ]
      };

      chart.setOption(option);
    },

    /** 格式化指标值 */
    formatMetricValue(value) {
      if (typeof value === 'number') {
        return value.toFixed(6);
      }
      return String(value);
    },

    /** 加载输入数据 */
    async loadInputData() {
      try {
        // 确保sourceId是字符串类型，sourceType有默认值
        const sourceId = String(this.sourceId);
        const sourceType = this.sourceType || 'task';

        const dataParams = { maxRows: 1000 };
        const response = await readDataSourceData(sourceId, sourceType, dataParams);
        this.inputData = response.data || [];
        console.log('📊 输入数据加载完成:', this.inputData.length, '条记录');
      } catch (error) {
        console.error("加载输入数据失败:", error);
        // 即使加载失败也不影响图表渲染
        this.inputData = [];
      }
    },









    /** 获取图表显示名称 */
    getPlotDisplayName(name) {
      const names = {
        'prediction_plot': '预测结果图',
        'residual_plot': '残差图',
        'feature_importance': '特征重要性',
        'cluster_plot': '聚类结果图',
        'elbow_plot': '肘部法则图',
        'fractal_plot': '分形分析图'
      };
      return names[name] || name;
    },

    /** 等待DOM渲染完成后渲染图表 */
    waitForDOMAndRenderCharts() {
      const maxRetries = 10;
      let retryCount = 0;

      const checkAndRender = () => {
        retryCount++;

        // 检查图表容器是否存在且可见
        const containers = [
          this.$refs.predictionChart,
          this.$refs.residualChart,
          this.$refs.featureImportanceChart,
          this.$refs.distributionChart
        ].filter(ref => ref); // 过滤掉不存在的容器

        let allVisible = containers.length > 0;

        containers.forEach(container => {
          if (container) {
            const rect = container.getBoundingClientRect();
            if (rect.width === 0 || rect.height === 0) {
              allVisible = false;
            }
          }
        });

        if (allVisible) {
          console.log('📊 结果图表DOM已准备就绪，开始渲染图表');
          this.renderResultCharts();
          return;
        }

        if (retryCount < maxRetries) {
          console.log(`⏳ 等待结果图表DOM渲染... (${retryCount}/${maxRetries})`);
          setTimeout(checkAndRender, 300);
        } else {
          console.log('⚠️ 结果图表DOM等待超时，强制渲染图表');
          this.renderResultCharts();
        }
      };

      checkAndRender();
    },

    /** 渲染结果图表 */
    renderResultCharts() {
      console.log('🎨 开始渲染结果图表', {
        hasPredictionData: this.hasPredictionData,
        hasFeatureImportance: this.hasFeatureImportance,
        hasDistributionData: this.hasDistributionData
      });

      this.chartsLoading = true;

      try {
        if (this.hasPredictionData) {
          this.renderPredictionChart();
          this.renderResidualChart();
        }

        if (this.hasFeatureImportance) {
          this.renderFeatureImportanceChart();
        }

        if (this.hasDistributionData) {
          this.renderDistributionChart();
        }

        console.log('✅ 所有结果图表渲染完成');
      } catch (error) {
        console.error('❌ 结果图表渲染失败', error);
      } finally {
        this.chartsLoading = false;
      }
    },

    /** 渲染预测vs实际值散点图 */
    renderPredictionChart() {
      setTimeout(() => {
        const chartDom = this.$refs.predictionChart;
        if (!chartDom) return;

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 预测图表DOM元素不可见，延迟重试');
          setTimeout(() => this.renderPredictionChart(), 500);
          return;
        }

        chartDom.style.height = '300px';
        chartDom.style.width = '100%';

        if (this.predictionChart) {
          this.predictionChart.dispose();
        }

        const existingInstance = this.$echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.predictionChart = this.$echarts.init(chartDom);

          const predictions = this.results.predictions || this.results.y_pred || [];
          const actualValues = this.results.actual_values || this.results.y_test || [];

          if (predictions.length === 0 || actualValues.length === 0) {
            const emptyOption = {
              title: {
                text: '暂无预测数据',
                left: 'center',
                top: 'center',
                textStyle: { fontSize: 16, color: '#999' }
              }
            };
            this.predictionChart.setOption(emptyOption);
            return;
          }

          // 准备散点数据
          const scatterData = predictions.map((pred, index) => [actualValues[index], pred]);

          // 计算对角线数据（理想预测线）
          const minVal = Math.min(...actualValues, ...predictions);
          const maxVal = Math.max(...actualValues, ...predictions);
          const diagonalData = [[minVal, minVal], [maxVal, maxVal]];

          const option = {
            title: {
              text: '预测值 vs 实际值',
              left: 'center',
              textStyle: { fontSize: 14 }
            },
            tooltip: {
              trigger: 'item',
              formatter: function(params) {
                if (params.seriesName === '理想预测线') {
                  return '理想预测线';
                }
                return `实际值: ${params.data[0].toFixed(4)}<br/>预测值: ${params.data[1].toFixed(4)}`;
              }
            },
            xAxis: {
              type: 'value',
              name: '实际值',
              nameLocation: 'middle',
              nameGap: 30
            },
            yAxis: {
              type: 'value',
              name: '预测值',
              nameLocation: 'middle',
              nameGap: 40
            },
            series: [
              {
                name: '预测结果',
                type: 'scatter',
                data: scatterData,
                itemStyle: {
                  color: '#409EFF',
                  opacity: 0.6
                },
                symbolSize: 6
              },
              {
                name: '理想预测线',
                type: 'line',
                data: diagonalData,
                lineStyle: {
                  color: '#E6A23C',
                  type: 'dashed',
                  width: 2
                },
                symbol: 'none',
                showSymbol: false
              }
            ],
            grid: {
              left: '10%',
              right: '10%',
              bottom: '15%',
              top: '15%'
            }
          };

          this.predictionChart.setOption(option);
          console.log('✅ 预测散点图渲染完成');

        } catch (error) {
          console.error('❌ 预测图表渲染失败', error);
        }
      }, 150);
    },

    /** 渲染残差分析图 */
    renderResidualChart() {
      setTimeout(() => {
        const chartDom = this.$refs.residualChart;
        if (!chartDom) return;

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 残差图表DOM元素不可见，延迟重试');
          setTimeout(() => this.renderResidualChart(), 500);
          return;
        }

        chartDom.style.height = '300px';
        chartDom.style.width = '100%';

        if (this.residualChart) {
          this.residualChart.dispose();
        }

        const existingInstance = this.$echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.residualChart = this.$echarts.init(chartDom);

          const predictions = this.results.predictions || this.results.y_pred || [];
          const actualValues = this.results.actual_values || this.results.y_test || [];

          if (predictions.length === 0 || actualValues.length === 0) {
            const emptyOption = {
              title: {
                text: '暂无残差数据',
                left: 'center',
                top: 'center',
                textStyle: { fontSize: 16, color: '#999' }
              }
            };
            this.residualChart.setOption(emptyOption);
            return;
          }

          // 计算残差
          const residuals = predictions.map((pred, index) => pred - actualValues[index]);
          const residualData = predictions.map((pred, index) => [pred, residuals[index]]);

          const option = {
            title: {
              text: '残差分析',
              left: 'center',
              textStyle: { fontSize: 14 }
            },
            tooltip: {
              trigger: 'item',
              formatter: function(params) {
                return `预测值: ${params.data[0].toFixed(4)}<br/>残差: ${params.data[1].toFixed(4)}`;
              }
            },
            xAxis: {
              type: 'value',
              name: '预测值',
              nameLocation: 'middle',
              nameGap: 30
            },
            yAxis: {
              type: 'value',
              name: '残差',
              nameLocation: 'middle',
              nameGap: 40
            },
            series: [
              {
                name: '残差',
                type: 'scatter',
                data: residualData,
                itemStyle: {
                  color: '#67C23A',
                  opacity: 0.6
                },
                symbolSize: 6
              },
              {
                name: '零线',
                type: 'line',
                data: [[Math.min(...predictions), 0], [Math.max(...predictions), 0]],
                lineStyle: {
                  color: '#F56C6C',
                  type: 'dashed',
                  width: 2
                },
                symbol: 'none',
                showSymbol: false
              }
            ],
            grid: {
              left: '10%',
              right: '10%',
              bottom: '15%',
              top: '15%'
            }
          };

          this.residualChart.setOption(option);
          console.log('✅ 残差分析图渲染完成');

        } catch (error) {
          console.error('❌ 残差图表渲染失败', error);
        }
      }, 200);
    },

    /** 渲染特征重要性图 */
    renderFeatureImportanceChart() {
      setTimeout(() => {
        const chartDom = this.$refs.featureImportanceChart;
        if (!chartDom) return;

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 特征重要性图表DOM元素不可见，延迟重试');
          setTimeout(() => this.renderFeatureImportanceChart(), 500);
          return;
        }

        chartDom.style.height = '300px';
        chartDom.style.width = '100%';

        if (this.featureImportanceChart) {
          this.featureImportanceChart.dispose();
        }

        const existingInstance = this.$echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.featureImportanceChart = this.$echarts.init(chartDom);

          const importance = this.results.feature_importance;
          if (!importance || Object.keys(importance).length === 0) {
            const emptyOption = {
              title: {
                text: '暂无特征重要性数据',
                left: 'center',
                top: 'center',
                textStyle: { fontSize: 16, color: '#999' }
              }
            };
            this.featureImportanceChart.setOption(emptyOption);
            return;
          }

          // 准备数据
          const features = Object.keys(importance);
          const values = Object.values(importance);

          const option = {
            title: {
              text: '特征重要性',
              left: 'center',
              textStyle: { fontSize: 14 }
            },
            tooltip: {
              trigger: 'axis',
              axisPointer: {
                type: 'shadow'
              }
            },
            xAxis: {
              type: 'value',
              name: '重要性'
            },
            yAxis: {
              type: 'category',
              data: features,
              axisLabel: {
                interval: 0,
                rotate: 0
              }
            },
            series: [
              {
                name: '重要性',
                type: 'bar',
                data: values,
                itemStyle: {
                  color: '#909399'
                }
              }
            ],
            grid: {
              left: '20%',
              right: '10%',
              bottom: '10%',
              top: '15%'
            }
          };

          this.featureImportanceChart.setOption(option);
          console.log('✅ 特征重要性图渲染完成');

        } catch (error) {
          console.error('❌ 特征重要性图表渲染失败', error);
        }
      }, 250);
    },

    /** 渲染数据分布图 */
    renderDistributionChart() {
      setTimeout(() => {
        const chartDom = this.$refs.distributionChart;
        if (!chartDom) return;

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 分布图表DOM元素不可见，延迟重试');
          setTimeout(() => this.renderDistributionChart(), 500);
          return;
        }

        chartDom.style.height = '300px';
        chartDom.style.width = '100%';

        if (this.distributionChart) {
          this.distributionChart.dispose();
        }

        const existingInstance = this.$echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.distributionChart = this.$echarts.init(chartDom);

          const actualValues = this.results.actual_values || this.results.y_test || [];
          if (actualValues.length === 0) {
            const emptyOption = {
              title: {
                text: '暂无分布数据',
                left: 'center',
                top: 'center',
                textStyle: { fontSize: 16, color: '#999' }
              }
            };
            this.distributionChart.setOption(emptyOption);
            return;
          }

          // 计算直方图数据
          const bins = 20;
          const min = Math.min(...actualValues);
          const max = Math.max(...actualValues);
          const binWidth = (max - min) / bins;

          const histogram = new Array(bins).fill(0);
          const binLabels = [];

          for (let i = 0; i < bins; i++) {
            const binStart = min + i * binWidth;
            const binEnd = min + (i + 1) * binWidth;
            binLabels.push(`${binStart.toFixed(2)}-${binEnd.toFixed(2)}`);
          }

          actualValues.forEach(value => {
            const binIndex = Math.min(Math.floor((value - min) / binWidth), bins - 1);
            histogram[binIndex]++;
          });

          const option = {
            title: {
              text: '目标变量分布',
              left: 'center',
              textStyle: { fontSize: 14 }
            },
            tooltip: {
              trigger: 'axis',
              axisPointer: {
                type: 'shadow'
              }
            },
            xAxis: {
              type: 'category',
              data: binLabels,
              axisLabel: {
                rotate: 45,
                interval: Math.floor(bins / 10)
              }
            },
            yAxis: {
              type: 'value',
              name: '频次'
            },
            series: [
              {
                name: '频次',
                type: 'bar',
                data: histogram,
                itemStyle: {
                  color: '#E6A23C'
                }
              }
            ],
            grid: {
              left: '10%',
              right: '10%',
              bottom: '20%',
              top: '15%'
            }
          };

          this.distributionChart.setOption(option);
          console.log('✅ 数据分布图渲染完成');

        } catch (error) {
          console.error('❌ 分布图表渲染失败', error);
        }
      }, 300);
    }

  }
};
</script>

<style scoped>
.task-results {
  padding: 0;
}

.loading-container {
  padding: 40px 0;
  text-align: center;
}

.task-info-card, .results-card {
  margin-bottom: 20px;
}

.results-content {
  min-height: 400px;
}

.statistics-section {
  padding: 20px 0;
}

.metric-value {
  font-family: 'Courier New', monospace;
  font-weight: bold;
  color: #409eff;
}

.metric-desc {
  color: #909399;
  font-size: 12px;
}

.visualizations-section {
  padding: 20px 0;
}

.plot-container {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}

.plot-container h4 {
  margin: 0 0 16px 0;
  color: #303133;
}

.raw-results-section {
  padding: 20px 0;
}

.debug-section {
  padding: 20px 0;
  text-align: center;
}

.image-slot {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 200px;
  background: #f5f7fa;
  color: #909399;
}

.image-slot i {
  font-size: 30px;
  margin-bottom: 10px;
}

.chart {
  width: 100%;
  height: 350px;
}

.plot-container h5 {
  margin: 0 0 12px 0;
  color: #409eff;
  font-size: 13px;
  font-weight: 600;
}

.visualizations-section h4 {
  margin: 0 0 20px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

/* 高级可视化样式 */
.advanced-visualizations-section {
  padding: 20px 0;
}

.advanced-visualization-container {
  width: 100%;
}

.original-plots-section {
  padding: 20px 0;
}

/* 结果图表样式 */
.result-charts-section {
  padding: 20px 0;
}

.charts-container {
  width: 100%;
}

.chart-container {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 16px;
  margin-bottom: 20px;
}

.chart-container h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 14px;
  text-align: center;
}

.chart {
  width: 100%;
  height: 300px;
}
</style>
