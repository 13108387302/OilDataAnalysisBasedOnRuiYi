<template>
  <div class="feature-engineering-visualization">
    <!-- 特征工程结果可视化 -->
    <el-row :gutter="20">
      <!-- 算法信息卡片 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="info-card" shadow="never">
          <div slot="header">
            <span>🔧 {{ getAlgorithmDisplayName() }} - 特征工程分析</span>
            <el-tag :type="getAlgorithmTagType()" style="margin-left: 10px;">
              {{ getAlgorithmCategory() }}
            </el-tag>
          </div>
          
          <el-descriptions :column="3" border>
            <el-descriptions-item label="算法类型">{{ getAlgorithmDisplayName() }}</el-descriptions-item>
            <el-descriptions-item label="处理特征">{{ getProcessedFeatures() }}</el-descriptions-item>
            <el-descriptions-item label="输出结果">{{ getOutputDescription() }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 分形维数分析 (如果是分形维数算法) -->
      <el-col v-if="isFractalDimension" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 分形维数分析</span>
            <el-tooltip content="显示不同深度范围的分形维数变化" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="fractalDimensionChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 井测曲线图 (如果是分形维数算法) -->
      <el-col v-if="isFractalDimension" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📈 井测曲线</span>
            <el-tooltip content="显示原始数据的井测曲线" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="wellLogChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 自动回归模型对比 (如果是自动回归算法) -->
      <el-col v-if="isAutomaticRegression" :span="24" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🏆 模型性能对比</span>
            <el-tooltip content="显示不同回归模型的性能对比" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="modelComparisonChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 最佳模型拟合效果 (如果是自动回归算法) -->
      <el-col v-if="isAutomaticRegression" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📈 最佳模型拟合效果</span>
            <el-tooltip content="显示自动选择的最佳模型的拟合效果" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="bestModelFitChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 模型选择过程 (如果是自动回归算法) -->
      <el-col v-if="isAutomaticRegression" :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🔍 模型选择过程</span>
            <el-tooltip content="显示各个候选模型的评估结果" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="modelSelectionChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 特征变换前后对比 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>🔄 特征变换对比</span>
            <el-tooltip content="显示特征工程前后的数据分布变化" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="featureTransformChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 统计指标图 -->
      <el-col :span="12" style="margin-bottom: 20px;">
        <el-card class="chart-card" shadow="never">
          <div slot="header">
            <span>📊 统计指标</span>
            <el-tooltip content="显示特征工程的关键统计指标" placement="top">
              <i class="el-icon-question" style="margin-left: 5px; color: #909399;"></i>
            </el-tooltip>
          </div>
          <div ref="statisticsChart" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 详细结果表格 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="results-card" shadow="never">
          <div slot="header">
            <span>📋 详细分析结果</span>
          </div>
          
          <!-- 分形维数结果表 -->
          <div v-if="isFractalDimension">
            <h4>分形维数分析结果</h4>
            <el-table :data="fractalResults" border stripe style="width: 100%; margin-bottom: 20px;">
              <el-table-column prop="depth_range" label="深度范围" width="150">
                <template slot-scope="scope">
                  <span>{{ scope.row.depth_range }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="fractal_dimension" label="分形维数" width="120">
                <template slot-scope="scope">
                  <span>{{ formatNumber(scope.row.fractal_dimension) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="correlation" label="相关系数" width="120">
                <template slot-scope="scope">
                  <span>{{ formatNumber(scope.row.correlation) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="说明">
                <template slot-scope="scope">
                  <span>{{ scope.row.description }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 自动回归结果表 -->
          <div v-if="isAutomaticRegression">
            <h4>模型对比结果</h4>
            <el-table :data="regressionResults" border stripe style="width: 100%; margin-bottom: 20px;">
              <el-table-column prop="model_name" label="模型名称" width="200">
                <template slot-scope="scope">
                  <strong>{{ scope.row.model_name }}</strong>
                  <el-tag v-if="scope.row.is_best" type="success" size="mini" style="margin-left: 5px;">最佳</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="r2_score" label="R²分数" width="100">
                <template slot-scope="scope">
                  <span>{{ formatNumber(scope.row.r2_score) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="mse" label="MSE" width="120">
                <template slot-scope="scope">
                  <span>{{ formatNumber(scope.row.mse) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="mae" label="MAE" width="120">
                <template slot-scope="scope">
                  <span>{{ formatNumber(scope.row.mae) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="模型描述">
                <template slot-scope="scope">
                  <span>{{ scope.row.description }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 通用统计结果表 -->
          <div v-if="generalResults.length > 0">
            <h4>统计分析结果</h4>
            <el-table :data="generalResults" border stripe style="width: 100%;">
              <el-table-column prop="metric" label="指标名称" width="200">
                <template slot-scope="scope">
                  <strong>{{ scope.row.metric }}</strong>
                </template>
              </el-table-column>
              <el-table-column prop="value" label="数值">
                <template slot-scope="scope">
                  <span>{{ formatValue(scope.row.value) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="说明">
                <template slot-scope="scope">
                  <span>{{ scope.row.description }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>

      <!-- 模型参数表 -->
      <el-col :span="24" style="margin-bottom: 20px;">
        <el-card class="params-card" shadow="never">
          <div slot="header">
            <span>⚙️ 算法参数</span>
          </div>
          
          <el-table :data="modelParameters" border stripe style="width: 100%;">
            <el-table-column prop="parameter" label="参数名称" width="200">
              <template slot-scope="scope">
                <strong>{{ scope.row.parameter }}</strong>
              </template>
            </el-table-column>
            <el-table-column prop="value" label="参数值">
              <template slot-scope="scope">
                <code>{{ scope.row.value }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明">
              <template slot-scope="scope">
                <span>{{ scope.row.description }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "FeatureEngineeringVisualization",
  props: {
    results: {
      type: Object,
      required: true
    },
    taskInfo: {
      type: Object,
      required: true
    },
    algorithmType: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      // 图表实例
      fractalDimensionChart: null,
      wellLogChart: null,
      modelComparisonChart: null,
      bestModelFitChart: null,
      modelSelectionChart: null,
      featureTransformChart: null,
      statisticsChart: null
    };
  },
  computed: {
    isFractalDimension() {
      return this.algorithmType.toLowerCase().includes('fractal_dimension');
    },
    
    isAutomaticRegression() {
      return this.algorithmType.toLowerCase().includes('automatic_regression');
    },
    
    fractalResults() {
      if (!this.isFractalDimension) return [];
      
      const stats = this.results.statistics || {};
      const fractalDimension = stats.fractal_dimension;
      const correlation = stats.correlation_coefficient;
      const depthRange = stats.depth_range || 'N/A';
      
      return [{
        depth_range: depthRange,
        fractal_dimension: fractalDimension,
        correlation: correlation,
        description: '基于盒计数法计算的分形维数'
      }];
    },
    
    regressionResults() {
      if (!this.isAutomaticRegression) return [];
      
      const stats = this.results.statistics || {};
      const allModels = stats.all_models_tried || [];
      const bestModel = stats.best_model_selected;
      
      return allModels.map(model => ({
        model_name: model.name || 'Unknown',
        r2_score: model.r2_score,
        mse: model.mse,
        mae: model.mae,
        is_best: model.name === bestModel,
        description: this.getModelDescription(model.name)
      }));
    },
    
    generalResults() {
      const stats = this.results.statistics || {};
      const results = [];
      
      Object.entries(stats).forEach(([key, value]) => {
        if (!['all_models_tried', 'best_model_selected'].includes(key)) {
          results.push({
            metric: this.getMetricDisplayName(key),
            value: value,
            description: this.getMetricDescription(key)
          });
        }
      });
      
      return results;
    },
    
    modelParameters() {
      const params = this.results.model_params || {};
      return Object.entries(params).map(([key, value]) => ({
        parameter: this.getParameterDisplayName(key),
        value: this.formatParameterValue(value),
        description: this.getParameterDescription(key)
      }));
    }
  },
  mounted() {
    this.$nextTick(() => {
      // 延迟初始化，确保DOM完全渲染
      setTimeout(() => {
        this.initializeCharts();
      }, 200);
    });
  },
  beforeDestroy() {
    // 销毁图表实例
    [this.fractalDimensionChart, this.wellLogChart, this.modelComparisonChart,
     this.bestModelFitChart, this.modelSelectionChart, this.featureTransformChart,
     this.statisticsChart].forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    initializeCharts() {
      console.log('🚀 开始初始化图表', {
        algorithmType: this.algorithmType,
        isFractalDimension: this.isFractalDimension,
        isAutomaticRegression: this.isAutomaticRegression,
        results: this.results
      });

      if (this.isFractalDimension) {
        console.log('📊 渲染分形维数相关图表');
        this.renderFractalDimensionChart();
        this.renderWellLogChart();
      }

      if (this.isAutomaticRegression) {
        console.log('📊 渲染自动回归相关图表');
        this.renderModelComparisonChart();
        this.renderBestModelFitChart();
        this.renderModelSelectionChart();
      }

      console.log('📊 渲染通用图表');
      this.renderFeatureTransformChart();
      this.renderStatisticsChart();
    },

    /** 渲染分形维数图表 */
    renderFractalDimensionChart() {
      if (!this.validateData()) {
        console.warn('⚠️ 数据验证失败，跳过分形维数图表渲染');
        return;
      }

      this.safeRenderChart(
        'fractalDimensionChart',
        'fractalDimensionChart',
        (chartDom) => {
          this.fractalDimensionChart = echarts.init(chartDom);

          const stats = this.results.statistics || {};
          const fractalDimension = stats.fractal_dimension || this.results.fractal_dimension;
          const correlation = stats.correlation_coefficient || this.results.correlation_coefficient;

          console.log('🔍 分形维数数据', { fractalDimension, correlation, stats });

          if (fractalDimension === undefined) {
            this.fractalDimensionChart.setOption({
              title: {
                text: '暂无分形维数数据',
                left: 'center',
                top: 'middle',
                textStyle: { color: '#999', fontSize: 16 }
              }
            });
            return;
          }

      const option = {
        title: {
          text: '分形维数分析结果',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}'
        },
        series: [{
          type: 'gauge',
          min: 1,
          max: 3,
          splitNumber: 10,
          radius: '80%',
          axisLine: {
            lineStyle: {
              width: 30,
              color: [
                [0.3, '#67e0e3'],
                [0.7, '#37a2da'],
                [1, '#fd666d']
              ]
            }
          },
          pointer: {
            itemStyle: {
              color: 'auto'
            }
          },
          axisTick: {
            distance: -30,
            length: 8,
            lineStyle: {
              color: '#fff',
              width: 2
            }
          },
          splitLine: {
            distance: -30,
            length: 30,
            lineStyle: {
              color: '#fff',
              width: 4
            }
          },
          axisLabel: {
            color: 'auto',
            distance: 40,
            fontSize: 12
          },
          detail: {
            valueAnimation: true,
            formatter: '{value}',
            color: 'auto',
            fontSize: 20,
            offsetCenter: [0, '70%']
          },
          data: [{
            value: fractalDimension,
            name: '分形维数'
          }]
        }]
      };

          this.fractalDimensionChart.setOption(option);
        },
        '分形维数'
      ).catch(error => {
        console.error('❌ 分形维数图表渲染失败', error);
      });
    },

    /** 渲染井测曲线图 */
    renderWellLogChart() {
      setTimeout(() => {
        const chartDom = this.$refs.wellLogChart;
        if (!chartDom) {
          console.log('⚠️ 井测曲线图DOM元素不存在');
          return;
        }

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 井测曲线图DOM元素不可见，延迟重试');
          setTimeout(() => this.renderWellLogChart(), 500);
          return;
        }

        // 强制设置尺寸
        chartDom.style.height = '350px';
        chartDom.style.width = '100%';

        // 清理已存在的实例
        if (this.wellLogChart) {
          this.wellLogChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.wellLogChart = echarts.init(chartDom);

          const depthValues = this.results.depth_values || [];
          const featureValues = this.results.feature_values || [];

          console.log('🔍 井测数据', { depthValuesLength: depthValues.length, featureValuesLength: featureValues.length });

          if (depthValues.length === 0 || featureValues.length === 0) {
            this.wellLogChart.setOption({
              title: {
                text: '暂无井测数据',
                left: 'center',
                top: 'middle',
                textStyle: { color: '#999', fontSize: 16 }
              }
            });
            return;
          }

      const data = depthValues.map((depth, index) => [featureValues[index], depth]);

      const option = {
        title: {
          text: '井测曲线',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            const point = params[0];
            return `深度: ${point.value[1]}<br/>数值: ${point.value[0]}`;
          }
        },
        xAxis: {
          type: 'value',
          name: '数值'
        },
        yAxis: {
          type: 'value',
          name: '深度',
          inverse: true
        },
        series: [{
          type: 'line',
          data: data,
          smooth: true,
          lineStyle: {
            color: '#5470c6',
            width: 2
          },
          symbol: 'none'
        }]
      };

          this.wellLogChart.setOption(option);
          console.log('✅ 井测曲线图渲染完成');
        } catch (error) {
          console.error('❌ 井测曲线图渲染失败', error);
        }
      }, 200); // 延迟200ms
    },

    /** 渲染模型比较图表 */
    renderModelComparisonChart() {
      // 实现模型比较图表
      console.log('渲染模型比较图表');
    },

    /** 渲染最佳模型拟合图表 */
    renderBestModelFitChart() {
      // 实现最佳模型拟合图表
      console.log('渲染最佳模型拟合图表');
    },

    /** 渲染模型选择图表 */
    renderModelSelectionChart() {
      // 实现模型选择图表
      console.log('渲染模型选择图表');
    },

    /** 渲染特征变换图表 */
    renderFeatureTransformChart() {
      // 实现特征变换图表
      console.log('渲染特征变换图表');
    },

    /** 渲染统计图表 */
    renderStatisticsChart() {
      setTimeout(() => {
        const chartDom = this.$refs.statisticsChart;
        if (!chartDom) {
          console.log('⚠️ 统计图表DOM元素不存在');
          return;
        }

        // 检查元素是否可见
        const rect = chartDom.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
          console.log('⚠️ 统计图表DOM元素不可见，延迟重试');
          setTimeout(() => this.renderStatisticsChart(), 500);
          return;
        }

        // 强制设置尺寸
        chartDom.style.height = '350px';
        chartDom.style.width = '100%';

        // 清理已存在的实例
        if (this.statisticsChart) {
          this.statisticsChart.dispose();
        }

        const existingInstance = echarts.getInstanceByDom(chartDom);
        if (existingInstance) {
          existingInstance.dispose();
        }

        try {
          this.statisticsChart = echarts.init(chartDom);

          const stats = this.results.statistics || {};
          const data = Object.entries(stats)
            .filter(([key, value]) => typeof value === 'number')
            .map(([key, value]) => ({
              name: this.getMetricDisplayName(key),
              value: value
            }));

          console.log('🔍 统计数据', { stats, dataLength: data.length });

          if (data.length === 0) {
            this.statisticsChart.setOption({
              title: {
                text: '暂无统计数据',
                left: 'center',
                top: 'middle',
                textStyle: { color: '#999', fontSize: 16 }
              }
            });
            return;
          }

      const option = {
        title: {
          text: '统计指标',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}'
        },
        xAxis: {
          type: 'category',
          data: data.map(item => item.name),
          axisLabel: {
            rotate: 45
          }
        },
        yAxis: {
          type: 'value'
        },
        series: [{
          type: 'bar',
          data: data.map(item => item.value),
          itemStyle: {
            color: '#5470c6'
          }
        }]
      };

          this.statisticsChart.setOption(option);
          console.log('✅ 统计图表渲染完成');
        } catch (error) {
          console.error('❌ 统计图表渲染失败', error);
        }
      }, 250); // 延迟250ms
    },

    /** 获取指标显示名称 */
    getMetricDisplayName(key) {
      const names = {
        'fractal_dimension': '分形维数',
        'correlation_coefficient': '相关系数',
        'r2_score': 'R²决定系数',
        'mean_squared_error': '均方误差',
        'mean_absolute_error': '平均绝对误差',
        'rmse': '均方根误差'
      };
      return names[key] || key;
    },

    /** 获取指标描述 */
    getMetricDescription(key) {
      const descriptions = {
        'fractal_dimension': '描述数据的复杂程度和自相似性',
        'correlation_coefficient': '描述变量间的线性相关程度',
        'r2_score': '模型解释方差的比例',
        'mean_squared_error': '预测值与真实值差值的平方的平均值',
        'mean_absolute_error': '预测值与真实值差值绝对值的平均值',
        'rmse': '均方误差的平方根'
      };
      return descriptions[key] || '暂无描述';
    },

    /** 获取参数显示名称 */
    getParameterDisplayName(key) {
      const names = {
        'depth_column': '深度列',
        'column_name': '分析列',
        'min_depth': '最小深度',
        'max_depth': '最大深度',
        'feature_columns': '特征列',
        'target_column': '目标列'
      };
      return names[key] || key;
    },

    /** 获取参数描述 */
    getParameterDescription(key) {
      const descriptions = {
        'depth_column': '用作深度参考的数据列',
        'column_name': '进行分形维数分析的数据列',
        'min_depth': '分析的最小深度值',
        'max_depth': '分析的最大深度值',
        'feature_columns': '用于分析的特征列',
        'target_column': '目标预测列'
      };
      return descriptions[key] || '暂无描述';
    },

    /** 格式化参数值 */
    formatParameterValue(value) {
      if (typeof value === 'number') {
        return value.toFixed(4);
      }
      if (Array.isArray(value)) {
        return value.join(', ');
      }
      return String(value);
    },

    /** 格式化数字 */
    formatNumber(value) {
      if (typeof value === 'number') {
        return value.toFixed(4);
      }
      return value;
    },

    /** 格式化值 */
    formatValue(value) {
      if (typeof value === 'number') {
        return value.toFixed(4);
      }
      if (Array.isArray(value)) {
        return value.join(', ');
      }
      return String(value);
    },

    /** 获取模型描述 */
    getModelDescription(modelName) {
      const descriptions = {
        'linear': '线性回归模型',
        'polynomial': '多项式回归模型',
        'exponential': '指数回归模型',
        'logarithmic': '对数回归模型'
      };
      return descriptions[modelName] || '未知模型';
    },

    /** 获取算法显示名称 */
    getAlgorithmDisplayName() {
      const names = {
        'fractal_dimension': '分形维数分析',
        'automatic_regression': '自动回归选择'
      };
      return names[this.algorithmType] || this.algorithmType;
    },

    /** 获取算法标签类型 */
    getAlgorithmTagType() {
      return 'primary';
    },

    /** 获取算法类别 */
    getAlgorithmCategory() {
      return '特征工程';
    },

    /** 获取处理特征 */
    getProcessedFeatures() {
      const params = this.results.model_params || {};
      const featureColumns = params.feature_columns || [];
      return featureColumns.length > 0 ? featureColumns.join(', ') : '未指定';
    },

    /** 获取输出描述 */
    getOutputDescription() {
      if (this.isFractalDimension) {
        return '分形维数值和相关系数';
      }
      if (this.isAutomaticRegression) {
        return '最佳回归模型和性能指标';
      }
      return '特征工程结果';
    },

    /** 验证数据完整性 */
    validateData() {
      console.log('🔍 验证数据完整性', this.results);

      if (!this.results) {
        console.warn('⚠️ 结果数据为空');
        return false;
      }

      if (this.isFractalDimension) {
        const stats = this.results.statistics || {};
        const fractalDimension = stats.fractal_dimension || this.results.fractal_dimension;
        const correlation = stats.correlation_coefficient || this.results.correlation_coefficient;

        console.log('🔍 分形维数数据验证', {
          fractalDimension,
          correlation,
          stats
        });

        return fractalDimension !== undefined;
      }

      return true;
    },

    /** 安全渲染图表的通用方法 */
    safeRenderChart(chartRef, chartInstance, renderFunction, chartName) {
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          try {
            const chartDom = this.$refs[chartRef];
            if (!chartDom) {
              console.warn(`⚠️ ${chartName}图表DOM元素不存在`);
              reject(new Error(`DOM元素不存在: ${chartRef}`));
              return;
            }

            // 检查元素是否可见
            const rect = chartDom.getBoundingClientRect();
            if (rect.width === 0 || rect.height === 0) {
              console.warn(`⚠️ ${chartName}图表DOM元素不可见，延迟重试`);
              setTimeout(() => {
                this.safeRenderChart(chartRef, chartInstance, renderFunction, chartName)
                  .then(resolve)
                  .catch(reject);
              }, 500);
              return;
            }

            // 强制设置尺寸
            chartDom.style.height = '350px';
            chartDom.style.width = '100%';

            // 清理已存在的实例
            if (this[chartInstance]) {
              this[chartInstance].dispose();
            }

            const existingInstance = echarts.getInstanceByDom(chartDom);
            if (existingInstance) {
              existingInstance.dispose();
            }

            // 执行渲染函数
            renderFunction.call(this, chartDom);
            console.log(`✅ ${chartName}图表渲染完成`);
            resolve();
          } catch (error) {
            console.error(`❌ ${chartName}图表渲染失败`, error);
            reject(error);
          }
        }, 150);
      });
    }
  }
};
</script>

<style scoped>
.feature-engineering-visualization {
  padding: 0;
}

.info-card, .chart-card, .results-card, .params-card {
  margin-bottom: 20px;
}

.chart {
  width: 100%;
  height: 350px;
}

h4 {
  margin: 15px 0 10px 0;
  color: #303133;
  font-weight: 500;
}
</style>
