<template>
  <div class="petroleum-curves">
    <!-- 参数配置 -->
    <el-card class="config-card" shadow="never">
      <div slot="header">
        <span>⚙️ 曲线配置</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="config-item">
            <label>深度列:</label>
            <el-select 
              v-model="depthColumn" 
              placeholder="选择深度列"
              size="small"
              style="width: 100%">
              <el-option 
                v-for="column in numericColumns" 
                :key="column"
                :label="column" 
                :value="column">
              </el-option>
            </el-select>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="config-item">
            <label>布局模式:</label>
            <el-select 
              v-model="layoutMode" 
              size="small"
              style="width: 100%">
              <el-option label="并列显示" value="parallel"></el-option>
              <el-option label="叠加显示" value="overlay"></el-option>
            </el-select>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="config-item">
            <label>深度范围:</label>
            <el-input
              v-model="depthRange"
              placeholder="自动计算，可手动修改"
              size="small">
              <template slot="prepend">
                <el-button
                  size="mini"
                  type="text"
                  @click="autoCalculateDepthRange"
                  title="重新计算深度范围">
                  <i class="el-icon-refresh"></i>
                </el-button>
              </template>
              <template slot="append">
                <el-button
                  size="mini"
                  type="primary"
                  @click="applyDepthFilter"
                  title="应用深度范围过滤">
                  应用
                </el-button>
                <el-button
                  size="mini"
                  type="warning"
                  @click="testDepthFilter"
                  title="测试深度范围过滤">
                  测试
                </el-button>
              </template>
            </el-input>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 15px;">
        <el-col :span="8">
          <div class="config-item">
            <label>数据标准化:</label>
            <el-select
              v-model="normalizationMethod"
              size="small"
              style="width: 100%">
              <el-option label="无标准化" value="none"></el-option>
              <el-option label="Z-Score标准化" value="zscore"></el-option>
              <el-option label="Min-Max标准化" value="minmax"></el-option>
              <el-option label="单位向量标准化" value="unit"></el-option>
            </el-select>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="config-item">
            <label>曲线间距:</label>
            <el-slider
              v-model="curveSpacing"
              :min="0"
              :max="5"
              :step="0.5"
              :format-tooltip="formatSpacing"
              style="margin-top: 8px;">
            </el-slider>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="config-item">
            <label>显示网格:</label>
            <el-switch
              v-model="showGrid"
              active-text="开"
              inactive-text="关"
              size="small"
              style="margin-top: 8px;">
            </el-switch>
          </div>
        </el-col>
      </el-row>
      
      <div class="feature-selection">
        <label>特征列选择:</label>
        <div class="feature-checkboxes">
          <el-checkbox-group v-model="selectedFeatures">
            <el-checkbox 
              v-for="column in availableFeatures" 
              :key="column"
              :label="column"
              class="feature-checkbox">
              {{ column }}
            </el-checkbox>
          </el-checkbox-group>
        </div>
        <div class="selection-actions">
          <el-button type="text" size="mini" @click="selectAllFeatures">
            {{ selectedFeatures.length === availableFeatures.length ? '取消全选' : '全选' }}
          </el-button>
          <el-button type="text" size="mini" @click="selectCommonFeatures">
            选择常用
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 曲线图表 -->
    <el-card class="chart-card" shadow="never">
      <div slot="header">
        <span>📈 石油特征曲线图</span>
        <div class="chart-controls">
          <el-button-group size="mini">
            <el-button 
              :type="chartType === 'line' ? 'primary' : ''"
              @click="chartType = 'line'; updateChart()">
              线图
            </el-button>
            <el-button 
              :type="chartType === 'scatter' ? 'primary' : ''"
              @click="chartType = 'scatter'; updateChart()">
              散点图
            </el-button>
          </el-button-group>
          <el-button
            type="text"
            icon="el-icon-refresh"
            @click="refreshChart"
            :loading="loading">
            刷新
          </el-button>
          <el-button
            type="text"
            icon="el-icon-download"
            @click="exportChart">
            导出
          </el-button>
        </div>
      </div>
      
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="8" animated />
        <p style="text-align: center; color: #909399; margin-top: 10px;">
          正在加载石油曲线数据...
        </p>
      </div>
      
      <div v-else class="chart-container">
        <div ref="curvesChart" class="chart"></div>
        
        <div class="chart-info">
          <div class="info-item">
            <span class="label">深度列:</span>
            <span class="value">{{ depthColumn }}</span>
          </div>
          <div class="info-item">
            <span class="label">特征数:</span>
            <span class="value">{{ selectedFeatures.length }}</span>
          </div>
          <div class="info-item">
            <span class="label">显示数据:</span>
            <span class="value">{{ dataPoints }}</span>
          </div>
          <div class="info-item">
            <span class="label">原始数据:</span>
            <span class="value">{{ originalChartData.length }}</span>
          </div>
          <div class="info-item">
            <span class="label">深度范围:</span>
            <span class="value">{{ actualDepthRange }}</span>
          </div>
          <div class="info-item" v-if="isFirstLoad">
            <span class="label" style="color: #409eff;">提示:</span>
            <span class="value" style="color: #409eff; font-size: 12px;">如图表显示异常，请点击刷新按钮</span>
          </div>
        </div>
      </div>
    </el-card>


  </div>
</template>

<script>
import { getDataSourceColumns, readDataSourceData } from "@/api/petrol/visualization";
import * as echarts from 'echarts';

export default {
  name: "PetroleumCurves",
  props: {
    sourceId: {
      type: [String, Number],
      required: true
    },
    sourceType: {
      type: String,
      required: true,
      default: 'dataset',
      validator: value => ['task', 'dataset'].includes(value)
    }
  },
  data() {
    return {
      loading: false,
      numericColumns: [],
      depthColumn: '',
      selectedFeatures: [],
      layoutMode: 'parallel',
      chartType: 'line',
      depthRange: '',
      normalizationMethod: 'zscore', // 默认使用Z-Score标准化
      curveSpacing: 2.0, // 曲线间距
      showGrid: true, // 显示网格
      chartGenerated: false, // 初始不显示图表，等数据加载完成后再显示
      originalChartData: [], // 保存原始数据，用于深度范围过滤
      chartData: [],
      dataPoints: 0,
      actualDepthRange: '',
      curvesChart: null,
      // 防重复请求标志
      isLoadingData: false,
      isInitialized: false,
      isFirstLoad: true, // 标记是否为第一次加载
      
      // 常用石油特征列映射
      commonFeatures: {
        'DEPTH': '深度',
        'GR': '自然伽马',
        'GAMMA': '自然伽马',
        'RT': '电阻率',
        'RESISTIVITY': '电阻率',
        'RHOB': '体积密度',
        'DENSITY': '体积密度',
        'NPHI': '中子孔隙度',
        'NEUTRON': '中子孔隙度',
        'DT': '声波时差',
        'SP': '自然电位',
        'POROSITY': '孔隙度',
        'PERMEABILITY': '渗透率'
      }
    };
  },
  computed: {
    availableFeatures() {
      return this.numericColumns.filter(col => col !== this.depthColumn);
    },
    canGenerate() {
      return this.depthColumn && this.selectedFeatures.length > 0;
    }
  },
  created() {
    console.log('🎨 PetroleumCurves组件创建', {
      sourceId: this.sourceId,
      sourceType: this.sourceType
    });
    this.loadColumns();
  },
  mounted() {
    console.log('🎨 PetroleumCurves组件挂载完成');
    // 第一次加载时，延迟一段时间后自动刷新
    if (this.isFirstLoad) {
      setTimeout(() => {
        console.log('🔄 第一次加载，执行自动刷新');
        this.handleFirstLoadRefresh();
      }, 1500); // 延迟1.5秒确保DOM完全渲染和数据加载完成
    }
  },
  beforeDestroy() {
    if (this.curvesChart) {
      this.curvesChart.dispose();
    }
  },
  watch: {
    // 监听深度列变化
    depthColumn() {
      if (this.depthColumn && this.selectedFeatures.length > 0 && this.isInitialized && !this.isLoadingData) {
        console.log('🔄 深度列变化，自动刷新石油曲线图');
        this.loadCurvesData();
      }
    },
    // 监听特征列选择变化
    selectedFeatures: {
      handler() {
        if (this.depthColumn && this.selectedFeatures.length > 0 && this.isInitialized && !this.isLoadingData) {
          console.log('🔄 特征列选择变化，自动刷新石油曲线图');
          this.loadCurvesData();
        }
      },
      deep: true
    },
    // 监听布局模式变化
    layoutMode() {
      if (this.chartData.length > 0) {
        console.log('🔄 布局模式变化，重新渲染图表');
        this.$nextTick(() => {
          this.renderChart();
        });
      }
    },
    // 监听图表类型变化
    chartType() {
      if (this.chartData.length > 0) {
        console.log('🔄 图表类型变化，重新渲染图表');
        this.$nextTick(() => {
          this.renderChart();
        });
      }
    },
    // 监听深度范围变化
    depthRange: {
      handler(newVal, oldVal) {
        console.log('🔄 深度范围变化', {
          oldVal,
          newVal,
          hasOriginalData: !!(this.originalChartData && this.originalChartData.length > 0)
        });

        if (this.originalChartData && this.originalChartData.length > 0) {
          console.log('🔄 开始重新过滤数据');
          // 只过滤数据，不重新加载（避免无限循环）
          this.filterDataByDepthRange();
          this.calculateStatistics();
          // 使用延迟渲染确保图表更新
          this.$nextTick(() => {
            setTimeout(() => {
              this.renderChart();
            }, 100);
          });
        }
      },
      immediate: false
    },
    // 监听标准化方法变化
    normalizationMethod() {
      if (this.chartData.length > 0) {
        console.log('🔄 标准化方法变化，重新渲染图表');
        this.$nextTick(() => {
          this.renderChart();
        });
      }
    },
    // 监听曲线间距变化
    curveSpacing() {
      if (this.chartData.length > 0) {
        console.log('🔄 曲线间距变化，重新渲染图表');
        this.$nextTick(() => {
          this.renderChart();
        });
      }
    },
    // 监听网格显示变化
    showGrid() {
      if (this.chartData.length > 0) {
        console.log('🔄 网格显示变化，重新渲染图表');
        this.$nextTick(() => {
          this.renderChart();
        });
      }
    }
  },
  methods: {
    /** 加载列信息 */
    async loadColumns() {
      if (this.isLoadingData) {
        console.log('🚫 正在加载数据，跳过重复请求');
        return;
      }

      this.loading = true;
      this.isLoadingData = true;

      try {
        // 确保sourceId是字符串类型，sourceType有默认值
        const sourceId = String(this.sourceId);
        const sourceType = this.sourceType || 'dataset';

        console.log('📋 开始加载列信息', {
          sourceId,
          sourceType
        });

        const response = await getDataSourceColumns(sourceId, sourceType);
        const data = response.data || {};
        this.numericColumns = data.numericColumns || [];

        console.log('✅ 列信息加载完成', {
          numericColumns: this.numericColumns.length,
          columns: this.numericColumns
        });

        // 自动选择深度列和特征列
        this.autoSelectDepthColumn();
        this.selectCommonFeatures();

        // 标记初始化完成
        this.isInitialized = true;

        // 仿照统计分析，自动加载数据并生成图表
        if (this.selectedFeatures.length > 0) {
          this.$nextTick(() => {
            this.loadCurvesData();
          });
        }
      } catch (error) {
        console.error('❌ 加载列信息失败', error);
        this.$message.error("加载列信息失败: " + error.message);
      } finally {
        this.loading = false;
        this.isLoadingData = false;
      }
    },

    /** 加载曲线数据（仿照统计分析的loadStatistics方法） */
    async loadCurvesData() {
      if (!this.canGenerate) {
        this.chartData = [];
        return;
      }

      if (this.isLoadingData) {
        console.log('🚫 正在加载曲线数据，跳过重复请求');
        return;
      }

      this.loading = true;
      this.isLoadingData = true;

      try {
        // 确保sourceId是字符串类型，sourceType有默认值
        const sourceId = String(this.sourceId);
        const sourceType = this.sourceType || 'dataset';

        console.log('🔍 开始加载石油曲线数据', {
          sourceId,
          sourceType,
          depthColumn: this.depthColumn,
          selectedFeatures: this.selectedFeatures
        });

        // 读取数据
        const columns = [this.depthColumn, ...this.selectedFeatures];
        const params = {
          columns: columns,
          maxRows: 1000 // 限制数据量
        };

        const response = await readDataSourceData(sourceId, sourceType, params);
        this.originalChartData = response.data || [];
        this.chartData = [...this.originalChartData]; // 复制原始数据

        console.log('📋 石油曲线数据响应', {
          response: response,
          dataLength: this.originalChartData.length,
          sampleData: this.originalChartData.slice(0, 3)
        });

        if (this.originalChartData.length === 0) {
          this.$message.warning("没有读取到数据");
          return;
        }

        // 自动计算并设置深度范围
        this.autoCalculateDepthRange();

        // 过滤深度范围和计算统计信息
        this.filterDataByDepthRange();
        this.calculateStatistics();

        // 设置图表生成状态
        this.chartGenerated = true;

        // 等待DOM渲染完成后渲染图表
        this.$nextTick(() => {
          this.waitForDOMAndRender();
        });

      } catch (error) {
        console.error('❌ 加载石油曲线数据失败', error);
        this.$message.error("加载石油曲线数据失败: " + error.message);
        this.chartData = [];
      } finally {
        this.loading = false;
        this.isLoadingData = false;
      }
    },

    /** 自动选择深度列 */
    autoSelectDepthColumn() {
      const depthKeywords = ['depth', '深度', 'dept', 'md', 'tvd'];
      for (const keyword of depthKeywords) {
        const found = this.numericColumns.find(col => 
          col.toLowerCase().includes(keyword.toLowerCase())
        );
        if (found) {
          this.depthColumn = found;
          break;
        }
      }
      
      // 如果没找到，选择第一个数值列
      if (!this.depthColumn && this.numericColumns.length > 0) {
        this.depthColumn = this.numericColumns[0];
      }
    },

    /** 选择常用特征 */
    selectCommonFeatures() {
      const autoSelected = [];

      // 优先选择常见的石油测井特征
      const priorityFeatures = ['GR', 'RT', 'RHOB', 'NPHI', 'DT', 'SP'];

      for (const feature of priorityFeatures) {
        if (this.availableFeatures.includes(feature) && autoSelected.length < 5) {
          autoSelected.push(feature);
        }
      }

      // 如果还没有选够，继续选择其他特征
      if (autoSelected.length < 3) {
        for (const feature of this.availableFeatures) {
          if (!autoSelected.includes(feature) && autoSelected.length < 5) {
            autoSelected.push(feature);
          }
        }
      }

      this.selectedFeatures = autoSelected;
    },

    /** 获取特征显示名称 */
    getFeatureDisplayName(feature) {
      return this.commonFeatures[feature] || feature;
    },

    /** 自动计算深度范围 */
    autoCalculateDepthRange() {
      try {
        if (!this.originalChartData || this.originalChartData.length === 0 || !this.depthColumn) {
          console.log('🎯 跳过深度范围计算：缺少必要数据', {
            hasOriginalData: !!(this.originalChartData && this.originalChartData.length > 0),
            depthColumn: this.depthColumn
          });
          return;
        }

        // 提取深度列的所有有效数值（使用原始数据）
        const depthValues = this.originalChartData
          .map(row => {
            if (!row || typeof row !== 'object') return NaN;
            const value = row[this.depthColumn];
            return parseFloat(value);
          })
          .filter(depth => !isNaN(depth) && depth !== null && depth !== undefined && isFinite(depth));

        if (depthValues.length === 0) {
          console.warn('深度列没有有效数值', {
            depthColumn: this.depthColumn,
            sampleData: this.originalChartData.slice(0, 3)
          });
          return;
        }

        // 计算最小值和最大值
        const minDepth = Math.min(...depthValues);
        const maxDepth = Math.max(...depthValues);

        // 验证计算结果
        if (!isFinite(minDepth) || !isFinite(maxDepth) || minDepth >= maxDepth) {
          console.warn('深度范围计算结果无效', { minDepth, maxDepth });
          return;
        }

        // 设置深度范围，保留2位小数
        this.depthRange = `${minDepth.toFixed(2)}-${maxDepth.toFixed(2)}`;

        console.log('🎯 自动计算深度范围成功', {
          depthColumn: this.depthColumn,
          minDepth: minDepth,
          maxDepth: maxDepth,
          depthRange: this.depthRange,
          totalDataPoints: this.originalChartData.length,
          validDepthPoints: depthValues.length
        });
      } catch (error) {
        console.error('❌ 自动计算深度范围失败', error);
      }
    },

    /** 格式化间距提示 */
    formatSpacing(value) {
      return `${value}倍`;
    },

    /** 数据标准化处理 */
    normalizeData(data, column) {
      const values = data.map(row => parseFloat(row[column])).filter(v => !isNaN(v));
      if (values.length === 0) return data.map(() => 0);

      let normalizedValues;

      switch (this.normalizationMethod) {
        case 'zscore':
          // Z-Score标准化: (x - mean) / std
          const mean = values.reduce((a, b) => a + b, 0) / values.length;
          const variance = values.reduce((acc, val) => acc + Math.pow(val - mean, 2), 0) / values.length;
          const std = Math.sqrt(variance);
          if (std === 0) {
            normalizedValues = values.map(() => 0);
          } else {
            normalizedValues = values.map(v => (v - mean) / std);
          }
          break;

        case 'minmax':
          // Min-Max标准化: (x - min) / (max - min)
          const min = Math.min(...values);
          const max = Math.max(...values);
          if (max === min) {
            normalizedValues = values.map(() => 0);
          } else {
            normalizedValues = values.map(v => (v - min) / (max - min));
          }
          break;

        case 'unit':
          // 单位向量标准化: x / ||x||
          const norm = Math.sqrt(values.reduce((acc, val) => acc + val * val, 0));
          if (norm === 0) {
            normalizedValues = values.map(() => 0);
          } else {
            normalizedValues = values.map(v => v / norm);
          }
          break;

        case 'none':
        default:
          // 不进行标准化
          normalizedValues = values;
          break;
      }

      // 将标准化后的值映射回原始数据结构
      let valueIndex = 0;
      return data.map(row => {
        const originalValue = parseFloat(row[column]);
        if (isNaN(originalValue)) {
          return 0;
        } else {
          return normalizedValues[valueIndex++];
        }
      });
    },

    /** 全选/取消全选特征 */
    selectAllFeatures() {
      if (this.selectedFeatures.length === this.availableFeatures.length) {
        this.selectedFeatures = [];
      } else {
        this.selectedFeatures = [...this.availableFeatures];
      }
    },

    /** 生成曲线图（重新加载数据） */
    generateCurves() {
      if (this.isLoadingData) {
        console.log('🚫 正在加载数据，请稍候');
        this.$message.warning('数据正在处理，请稍候');
        return;
      }
      this.loadCurvesData();
    },

    /** 手动应用深度范围过滤 */
    applyDepthFilter() {
      if (this.isLoadingData) {
        this.$message.warning('数据正在处理，请稍候');
        return;
      }

      console.log('🔧 手动应用深度范围过滤', {
        depthRange: this.depthRange,
        originalDataLength: this.originalChartData?.length || 0,
        currentDataLength: this.chartData?.length || 0
      });

      if (!this.originalChartData || this.originalChartData.length === 0) {
        this.$message.warning('没有原始数据，请先加载数据');
        return;
      }

      this.filterDataByDepthRange();
      this.calculateStatistics();

      // 强制重新渲染图表
      this.forceRenderChart();

      this.$message.success(`深度范围过滤完成，显示 ${this.chartData.length} 条数据`);
    },

    /** 强制重新渲染图表 */
    forceRenderChart() {
      console.log('🔄 强制重新渲染图表');

      // 销毁现有图表
      if (this.curvesChart) {
        this.curvesChart.dispose();
        this.curvesChart = null;
      }

      // 等待DOM更新后重新渲染
      this.$nextTick(() => {
        setTimeout(() => {
          this.renderChart();
        }, 100);
      });
    },

    /** 测试深度范围过滤 */
    testDepthFilter() {
      console.log('🧪 测试深度范围过滤功能', {
        originalData: this.originalChartData?.length || 0,
        currentData: this.chartData?.length || 0,
        depthRange: this.depthRange,
        depthColumn: this.depthColumn
      });

      if (this.originalChartData && this.originalChartData.length > 0) {
        // 测试设置一个小范围
        const depths = this.originalChartData.map(row => parseFloat(row[this.depthColumn])).filter(d => !isNaN(d));
        const minDepth = Math.min(...depths);
        const maxDepth = Math.max(...depths);
        const midDepth = (minDepth + maxDepth) / 2;
        const testRange = `${minDepth.toFixed(2)}-${midDepth.toFixed(2)}`;

        console.log('🔬 设置测试深度范围', {
          originalRange: `${minDepth.toFixed(2)}-${maxDepth.toFixed(2)}`,
          testRange: testRange
        });

        this.depthRange = testRange;
      }
    },

    /** 根据深度范围过滤数据 */
    filterDataByDepthRange() {
      try {
        console.log('🔍 开始深度范围过滤', {
          depthRange: this.depthRange,
          depthColumn: this.depthColumn,
          originalDataLength: this.originalChartData?.length || 0
        });

        // 如果没有原始数据，直接返回
        if (!this.originalChartData || this.originalChartData.length === 0) {
          console.warn('没有原始数据，无法过滤');
          this.chartData = [];
          return;
        }

        // 如果没有深度列，使用原始数据
        if (!this.depthColumn) {
          console.warn('没有深度列，使用原始数据');
          this.chartData = [...this.originalChartData];
          return;
        }

        // 如果没有深度范围，使用原始数据
        if (!this.depthRange || this.depthRange.trim() === '') {
          console.log('没有深度范围限制，使用原始数据');
          this.chartData = [...this.originalChartData];
          return;
        }

        const range = this.depthRange.split('-');
        if (range.length === 2) {
          const minDepth = parseFloat(range[0].trim());
          const maxDepth = parseFloat(range[1].trim());

          console.log('解析深度范围', { minDepth, maxDepth });

          if (!isNaN(minDepth) && !isNaN(maxDepth) && isFinite(minDepth) && isFinite(maxDepth) && minDepth <= maxDepth) {
            // 基于原始数据进行过滤
            const beforeFilter = this.originalChartData.length;
            this.chartData = this.originalChartData.filter(row => {
              if (!row || typeof row !== 'object') return false;
              const depth = parseFloat(row[this.depthColumn]);
              const isValid = !isNaN(depth) && isFinite(depth) && depth >= minDepth && depth <= maxDepth;
              return isValid;
            });

            console.log('✅ 深度范围过滤完成', {
              depthRange: this.depthRange,
              minDepth: minDepth,
              maxDepth: maxDepth,
              depthColumn: this.depthColumn,
              beforeFilter: beforeFilter,
              afterFilter: this.chartData.length,
              filteredOut: beforeFilter - this.chartData.length
            });

            // 如果过滤后没有数据，给出警告
            if (this.chartData.length === 0) {
              console.warn('⚠️ 过滤后没有数据，请检查深度范围设置');
              // 避免在初始化时显示警告消息
              if (this.chartGenerated) {
                this.$message.warning(`深度范围 ${this.depthRange} 内没有数据`);
              }
            }
          } else {
            // 如果范围无效，使用原始数据
            console.warn('深度范围格式无效，使用原始数据', {
              depthRange: this.depthRange,
              minDepth,
              maxDepth
            });
            this.chartData = [...this.originalChartData];
          }
        } else {
          // 如果格式无效，使用原始数据
          console.warn('深度范围格式错误，使用原始数据', {
            depthRange: this.depthRange,
            splitResult: range
          });
          this.chartData = [...this.originalChartData];
        }
      } catch (error) {
        console.error('❌ 深度范围过滤失败', error);
        // 发生错误时使用原始数据
        this.chartData = this.originalChartData ? [...this.originalChartData] : [];
      }
    },

    /** 计算统计信息 */
    calculateStatistics() {
      this.dataPoints = this.chartData.length;
      
      if (this.chartData.length > 0) {
        const depths = this.chartData.map(row => parseFloat(row[this.depthColumn])).filter(d => !isNaN(d));
        if (depths.length > 0) {
          const minDepth = Math.min(...depths);
          const maxDepth = Math.max(...depths);
          this.actualDepthRange = `${minDepth.toFixed(2)} - ${maxDepth.toFixed(2)}`;
        }
      }
    },

    /** 渲染图表 */
    renderChart() {
      try {
        const chartContainer = this.$refs.curvesChart;
        if (!chartContainer) {
          console.warn('图表容器不存在，跳过渲染');
          return;
        }

        if (!this.chartData || this.chartData.length === 0) {
          console.warn('没有图表数据，跳过渲染');
          return;
        }

        // 确保容器有正确的尺寸
        if (!chartContainer.style.height) {
          chartContainer.style.height = '600px';
        }
        if (!chartContainer.style.width) {
          chartContainer.style.width = '100%';
        }

        // 检查容器尺寸
        const rect = chartContainer.getBoundingClientRect();
        console.log('📊 图表容器尺寸', {
          width: rect.width,
          height: rect.height
        });

        if (this.curvesChart) {
          this.curvesChart.dispose();
        }

        // 清理可能存在的实例
        const existingInstance = echarts.getInstanceByDom(chartContainer);
        if (existingInstance) {
          existingInstance.dispose();
        }

        this.curvesChart = echarts.init(chartContainer);

        if (this.layoutMode === 'parallel') {
          this.renderParallelChart();
        } else {
          this.renderOverlayChart();
        }

        // 确保图表正确调整大小
        setTimeout(() => {
          if (this.curvesChart) {
            this.curvesChart.resize();
          }
        }, 100);

      } catch (error) {
        console.error('❌ 图表渲染失败', error);
      }
    },

    /** 渲染并列图表 */
    renderParallelChart() {
      // 计算标准化数据
      const normalizedData = {};
      this.selectedFeatures.forEach(feature => {
        normalizedData[feature] = this.normalizeData(this.chartData, feature);
      });

      const option = {
        title: {
          text: `石油特征曲线图 (并列显示) - ${this.getNormalizationLabel()}`,
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross' },
          formatter: (params) => {
            if (params.length === 0) return '';
            const dataIndex = params[0].dataIndex;
            let tooltip = `深度: ${parseFloat(this.chartData[dataIndex][this.depthColumn]).toFixed(2)}<br/>`;

            params.forEach(param => {
              const feature = this.selectedFeatures[param.seriesIndex];
              const originalValue = parseFloat(this.chartData[dataIndex][feature]);
              const normalizedValue = param.value[0];
              tooltip += `${this.getFeatureDisplayName(feature)}: ${originalValue.toFixed(3)} (标准化: ${normalizedValue.toFixed(3)})<br/>`;
            });

            return tooltip;
          }
        },
        legend: {
          data: this.selectedFeatures.map(f => this.getFeatureDisplayName(f)),
          bottom: 0
        },
        grid: this.selectedFeatures.map((feature, index) => ({
          left: `${index * (90 / this.selectedFeatures.length) + 5}%`,
          right: `${(this.selectedFeatures.length - index - 1) * (90 / this.selectedFeatures.length) + 5}%`,
          top: '20%',
          bottom: '20%',
          show: this.showGrid,
          borderColor: '#ddd'
        })),
        xAxis: this.selectedFeatures.map((feature, index) => ({
          gridIndex: index,
          type: 'value',
          name: this.getFeatureDisplayName(feature),
          nameLocation: 'middle',
          nameGap: 30,
          nameTextStyle: {
            fontSize: 12,
            fontWeight: 'bold'
          },
          splitLine: { show: this.showGrid },
          axisLine: { show: true },
          axisTick: { show: true }
        })),
        yAxis: this.selectedFeatures.map((feature, index) => {
          // 计算当前数据的深度范围
          let minDepth = 0;
          let maxDepth = 1000;

          try {
            if (this.chartData && this.chartData.length > 0 && this.depthColumn) {
              const depths = this.chartData
                .map(row => row ? parseFloat(row[this.depthColumn]) : NaN)
                .filter(d => !isNaN(d) && isFinite(d));

              if (depths.length > 0) {
                minDepth = Math.min(...depths);
                maxDepth = Math.max(...depths);

                // 确保范围有效
                if (!isFinite(minDepth) || !isFinite(maxDepth) || minDepth >= maxDepth) {
                  minDepth = 0;
                  maxDepth = 1000;
                }
              }
            }
          } catch (error) {
            console.warn('计算Y轴范围失败，使用默认值', error);
          }

          return {
            gridIndex: index,
            type: 'value',
            name: index === 0 ? `深度 (${this.depthColumn})` : '',
            nameLocation: 'middle',
            nameGap: 50,
            nameTextStyle: {
              fontSize: 12,
              fontWeight: 'bold'
            },
            inverse: true,
            min: minDepth,
            max: maxDepth,
            splitLine: { show: this.showGrid },
            axisLine: { show: true },
            axisTick: { show: true }
          };
        }),
        series: this.selectedFeatures.map((feature, index) => {
          const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452'];
          return {
            name: this.getFeatureDisplayName(feature),
            type: this.chartType,
            xAxisIndex: index,
            yAxisIndex: index,
            data: this.chartData.map((row, dataIndex) => [
              normalizedData[feature][dataIndex] + index * this.curveSpacing, // 添加间距偏移
              parseFloat(row[this.depthColumn]) || 0
            ]),
            symbolSize: this.chartType === 'scatter' ? 3 : 0,
            lineStyle: {
              width: 2,
              color: colors[index % colors.length]
            },
            itemStyle: {
              color: colors[index % colors.length]
            }
          };
        })
      };

      this.curvesChart.setOption(option);
    },

    /** 渲染叠加图表 */
    renderOverlayChart() {
      // 计算标准化数据
      const normalizedData = {};
      this.selectedFeatures.forEach(feature => {
        normalizedData[feature] = this.normalizeData(this.chartData, feature);
      });

      const option = {
        title: {
          text: `石油特征曲线图 (叠加显示) - ${this.getNormalizationLabel()}`,
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross' },
          formatter: (params) => {
            if (params.length === 0) return '';
            const dataIndex = params[0].dataIndex;
            let tooltip = `深度: ${parseFloat(this.chartData[dataIndex][this.depthColumn]).toFixed(2)}<br/>`;

            params.forEach(param => {
              const feature = this.selectedFeatures[param.seriesIndex];
              const originalValue = parseFloat(this.chartData[dataIndex][feature]);
              const normalizedValue = param.value[0];
              tooltip += `${this.getFeatureDisplayName(feature)}: ${originalValue.toFixed(3)} (标准化: ${normalizedValue.toFixed(3)})<br/>`;
            });

            return tooltip;
          }
        },
        legend: {
          data: this.selectedFeatures.map(f => this.getFeatureDisplayName(f)),
          bottom: 0
        },
        grid: {
          left: '10%',
          right: '10%',
          top: '20%',
          bottom: '20%',
          show: this.showGrid,
          borderColor: '#ddd'
        },
        xAxis: {
          type: 'value',
          name: '标准化特征值',
          nameLocation: 'middle',
          nameGap: 30,
          nameTextStyle: {
            fontSize: 12,
            fontWeight: 'bold'
          },
          splitLine: { show: this.showGrid },
          axisLine: { show: true },
          axisTick: { show: true }
        },
        yAxis: {
          type: 'value',
          name: `深度 (${this.depthColumn})`,
          nameLocation: 'middle',
          nameGap: 50,
          nameTextStyle: {
            fontSize: 12,
            fontWeight: 'bold'
          },
          inverse: true,
          min: (() => {
            try {
              if (this.chartData && this.chartData.length > 0 && this.depthColumn) {
                const depths = this.chartData
                  .map(row => row ? parseFloat(row[this.depthColumn]) : NaN)
                  .filter(d => !isNaN(d) && isFinite(d));
                if (depths.length > 0) {
                  const min = Math.min(...depths);
                  return isFinite(min) ? min : 0;
                }
              }
            } catch (error) {
              console.warn('计算Y轴最小值失败', error);
            }
            return 0;
          })(),
          max: (() => {
            try {
              if (this.chartData && this.chartData.length > 0 && this.depthColumn) {
                const depths = this.chartData
                  .map(row => row ? parseFloat(row[this.depthColumn]) : NaN)
                  .filter(d => !isNaN(d) && isFinite(d));
                if (depths.length > 0) {
                  const max = Math.max(...depths);
                  return isFinite(max) ? max : 1000;
                }
              }
            } catch (error) {
              console.warn('计算Y轴最大值失败', error);
            }
            return 1000;
          })(),
          splitLine: { show: this.showGrid },
          axisLine: { show: true },
          axisTick: { show: true }
        },
        series: this.selectedFeatures.map((feature, index) => {
          const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452'];
          return {
            name: this.getFeatureDisplayName(feature),
            type: this.chartType,
            data: this.chartData.map((row, dataIndex) => [
              normalizedData[feature][dataIndex] + index * this.curveSpacing, // 添加间距偏移
              parseFloat(row[this.depthColumn]) || 0
            ]),
            symbolSize: this.chartType === 'scatter' ? 3 : 0,
            lineStyle: {
              width: 2,
              color: colors[index % colors.length]
            },
            itemStyle: {
              color: colors[index % colors.length]
            }
          };
        })
      };

      this.curvesChart.setOption(option);
    },

    /** 获取标准化方法标签 */
    getNormalizationLabel() {
      const labels = {
        'none': '无标准化',
        'zscore': 'Z-Score标准化',
        'minmax': 'Min-Max标准化',
        'unit': '单位向量标准化'
      };
      return labels[this.normalizationMethod] || '无标准化';
    },

    /** 更新图表 */
    updateChart() {
      if (this.chartGenerated) {
        this.renderChart();
      }
    },

    /** 刷新图表 */
    refreshChart() {
      console.log('🔄 手动刷新石油曲线图表');

      // 重置状态
      this.chartGenerated = false;
      this.chartData = [];
      this.originalChartData = [];

      // 销毁现有图表
      if (this.curvesChart) {
        this.curvesChart.dispose();
        this.curvesChart = null;
      }

      // 重新加载数据
      if (this.canGenerate) {
        this.loadCurvesData();
      } else {
        this.$message.warning('请先选择深度列和特征列');
      }
    },

    /** 等待DOM渲染完成后渲染图表 */
    waitForDOMAndRender() {
      const maxRetries = 20; // 增加重试次数
      let retryCount = 0;

      const checkAndRender = () => {
        retryCount++;

        console.log(`🔄 等待DOM渲染 - 第${retryCount}次尝试`, {
          hasChartData: this.chartData && this.chartData.length > 0,
          hasDepthColumn: !!this.depthColumn,
          hasSelectedFeatures: this.selectedFeatures.length > 0,
          chartGenerated: this.chartGenerated
        });

        // 检查数据是否准备就绪
        if (!this.chartData || this.chartData.length === 0) {
          console.log('⏳ 图表数据未准备就绪，继续等待...');
          if (retryCount < maxRetries) {
            setTimeout(checkAndRender, 200);
          } else {
            console.warn('❌ 等待图表数据超时');
          }
          return;
        }

        // 检查图表容器是否存在且可见
        const chartContainer = this.$refs.curvesChart;
        if (chartContainer) {
          // 确保容器有固定的尺寸
          if (!chartContainer.style.height) {
            chartContainer.style.height = '600px';
          }
          if (!chartContainer.style.width) {
            chartContainer.style.width = '100%';
          }

          const rect = chartContainer.getBoundingClientRect();
          if (rect.width > 100 && rect.height > 100) { // 确保有合理的尺寸
            console.log('🎨 石油曲线DOM已准备就绪，开始渲染图表', {
              width: rect.width,
              height: rect.height
            });

            // 延迟一点时间确保DOM完全稳定
            setTimeout(() => {
              this.renderChart();
            }, 100);
            return;
          }
        }

        if (retryCount < maxRetries) {
          console.log(`⏳ 等待石油曲线DOM渲染... (${retryCount}/${maxRetries})`);
          setTimeout(checkAndRender, 200); // 减少等待间隔
        } else {
          console.log('⚠️ 石油曲线DOM等待超时，强制渲染图表');
          // 强制设置容器尺寸
          if (chartContainer) {
            chartContainer.style.height = '600px';
            chartContainer.style.width = '100%';
          }
          setTimeout(() => {
            this.renderChart();
          }, 200);
        }
      };

      checkAndRender();
    },

    /** 处理第一次加载刷新 */
    handleFirstLoadRefresh() {
      console.log('🔄 处理第一次加载刷新', {
        isFirstLoad: this.isFirstLoad,
        hasData: this.chartData && this.chartData.length > 0,
        hasDepthColumn: !!this.depthColumn,
        hasSelectedFeatures: this.selectedFeatures.length > 0,
        chartGenerated: this.chartGenerated
      });

      if (this.isFirstLoad) {
        this.isFirstLoad = false; // 标记已经不是第一次加载了

        // 如果已经有数据但图表显示异常，强制重新渲染
        if (this.chartData && this.chartData.length > 0 && this.depthColumn && this.selectedFeatures.length > 0) {
          console.log('🔄 数据已存在，强制重新渲染图表');
          this.forceRenderChart();
        } else if (this.depthColumn && this.selectedFeatures.length > 0) {
          // 如果配置已完成但没有数据，重新加载数据
          console.log('🔄 配置已完成，重新加载数据');
          this.loadCurvesData();
        } else {
          // 如果配置还没完成，等待一下再重试
          console.log('🔄 配置未完成，延迟重试');
          setTimeout(() => {
            if (this.depthColumn && this.selectedFeatures.length > 0) {
              this.loadCurvesData();
            }
          }, 500);
        }
      }
    },

    /** 导出图表 */
    exportChart() {
      if (this.curvesChart) {
        const url = this.curvesChart.getDataURL({
          type: 'png',
          backgroundColor: '#fff'
        });

        const link = document.createElement('a');
        link.href = url;
        link.download = `petroleum_curves_${Date.now()}.png`;
        link.click();

        this.$message.success("图表导出成功");
      }
    }
  }
};
</script>

<style scoped>
.petroleum-curves {
  padding: 0;
}

.config-card, .chart-card {
  margin-bottom: 20px;
}

.config-item {
  margin-bottom: 16px;
}

.config-item label {
  display: block;
  margin-bottom: 4px;
  color: #606266;
  font-size: 14px;
}

.feature-selection {
  margin-top: 16px;
}

.feature-selection label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-size: 14px;
}

.feature-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 8px;
}

.feature-checkbox {
  margin: 0;
  min-width: 120px;
}

.selection-actions {
  display: flex;
  gap: 8px;
}

.chart-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.loading-container {
  padding: 40px 0;
  text-align: center;
}

.chart-container {
  position: relative;
}

.chart {
  width: 100%;
  height: 600px;
  min-height: 600px; /* 确保最小高度 */
  min-width: 300px;  /* 确保最小宽度 */
}

.chart-info {
  display: flex;
  justify-content: space-around;
  margin-top: 16px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.info-item {
  text-align: center;
}

.info-item .label {
  display: block;
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}

.info-item .value {
  color: #303133;
  font-weight: 500;
}
</style>
