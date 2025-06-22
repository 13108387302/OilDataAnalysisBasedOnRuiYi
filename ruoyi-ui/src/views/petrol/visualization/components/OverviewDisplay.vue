<template>
  <div class="overview-display">
    <!-- 数据预览 -->
    <el-card shadow="never" style="margin-bottom: 20px;">
      <div slot="header" class="clearfix">
        <span>数据预览</span>
        <span style="float: right; color: #666; font-size: 12px;">
          显示前 {{ config.previewRows || 10 }} 行数据
        </span>
      </div>
      
      <el-table 
        :data="data.preview || []" 
        style="width: 100%"
        size="small"
        stripe
        border>
        <el-table-column
          v-for="column in displayColumns"
          :key="column"
          :prop="column"
          :label="column"
          :width="120"
          show-overflow-tooltip>
          <template slot-scope="scope">
            <span v-if="isNumeric(scope.row[column])">
              {{ formatNumber(scope.row[column]) }}
            </span>
            <span v-else>{{ scope.row[column] }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 统计信息 -->
    <el-row :gutter="20" v-if="data.statistics">
      <!-- 基础统计 -->
      <el-col :span="12" v-if="config.statisticsTypes && config.statisticsTypes.includes('basic')">
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span>基础统计信息</span>
          </div>
          
          <el-table 
            :data="basicStatisticsData" 
            style="width: 100%"
            size="small">
            <el-table-column prop="column" label="列名" width="120"></el-table-column>
            <el-table-column prop="count" label="计数" width="80"></el-table-column>
            <el-table-column prop="mean" label="均值" width="100">
              <template slot-scope="scope">
                {{ scope.row.mean ? formatNumber(scope.row.mean) : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="std" label="标准差" width="100">
              <template slot-scope="scope">
                {{ scope.row.std ? formatNumber(scope.row.std) : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="min" label="最小值" width="100">
              <template slot-scope="scope">
                {{ scope.row.min ? formatNumber(scope.row.min) : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="max" label="最大值" width="100">
              <template slot-scope="scope">
                {{ scope.row.max ? formatNumber(scope.row.max) : '-' }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 分布信息 -->
      <el-col :span="12" v-if="config.statisticsTypes && config.statisticsTypes.includes('distribution')">
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span>分布信息</span>
          </div>
          
          <el-table 
            :data="distributionData" 
            style="width: 100%"
            size="small">
            <el-table-column prop="column" label="列名" width="120"></el-table-column>
            <el-table-column prop="q25" label="25%分位数" width="100">
              <template slot-scope="scope">
                {{ scope.row.q25 ? formatNumber(scope.row.q25) : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="q50" label="中位数" width="100">
              <template slot-scope="scope">
                {{ scope.row.q50 ? formatNumber(scope.row.q50) : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="q75" label="75%分位数" width="100">
              <template slot-scope="scope">
                {{ scope.row.q75 ? formatNumber(scope.row.q75) : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="skewness" label="偏度" width="80">
              <template slot-scope="scope">
                {{ scope.row.skewness ? formatNumber(scope.row.skewness) : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="kurtosis" label="峰度" width="80">
              <template slot-scope="scope">
                {{ scope.row.kurtosis ? formatNumber(scope.row.kurtosis) : '-' }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 缺失值信息 -->
    <el-card 
      shadow="never" 
      style="margin-top: 20px;" 
      v-if="config.statisticsTypes && config.statisticsTypes.includes('missing') && data.missingInfo">
      <div slot="header" class="clearfix">
        <span>缺失值分析</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-table 
            :data="missingData" 
            style="width: 100%"
            size="small">
            <el-table-column prop="column" label="列名" width="150"></el-table-column>
            <el-table-column prop="missingCount" label="缺失数量" width="100"></el-table-column>
            <el-table-column prop="missingRatio" label="缺失比例" width="100">
              <template slot-scope="scope">
                {{ (scope.row.missingRatio * 100).toFixed(2) }}%
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template slot-scope="scope">
                <el-tag 
                  :type="scope.row.missingRatio > 0.1 ? 'danger' : scope.row.missingRatio > 0.05 ? 'warning' : 'success'"
                  size="mini">
                  {{ scope.row.missingRatio > 0.1 ? '严重' : scope.row.missingRatio > 0.05 ? '中等' : '良好' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-col>
        
        <el-col :span="12">
          <div id="missing-chart" style="width: 100%; height: 300px;"></div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: "OverviewDisplay",
  props: {
    data: {
      type: Object,
      default: () => ({})
    },
    config: {
      type: Object,
      default: () => ({})
    },
    height: {
      type: String,
      default: '500px'
    }
  },
  data() {
    return {
      missingChart: null
    };
  },
  computed: {
    displayColumns() {
      if (this.config.selectedColumns && this.config.selectedColumns.length > 0) {
        return this.config.selectedColumns;
      }
      if (this.data.preview && this.data.preview.length > 0) {
        return Object.keys(this.data.preview[0]).slice(0, 8); // 最多显示8列
      }
      return [];
    },
    
    basicStatisticsData() {
      if (!this.data.statistics) return [];
      return Object.keys(this.data.statistics).map(column => ({
        column,
        ...this.data.statistics[column]
      }));
    },
    
    distributionData() {
      if (!this.data.statistics) return [];
      return Object.keys(this.data.statistics).map(column => ({
        column,
        ...this.data.statistics[column]
      }));
    },
    
    missingData() {
      if (!this.data.missingInfo) return [];
      return Object.keys(this.data.missingInfo).map(column => ({
        column,
        ...this.data.missingInfo[column]
      }));
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.renderMissingChart();
    });
  },
  watch: {
    data: {
      handler() {
        this.$nextTick(() => {
          this.renderMissingChart();
        });
      },
      deep: true
    }
  },
  beforeDestroy() {
    if (this.missingChart) {
      this.missingChart.dispose();
    }
  },
  methods: {
    isNumeric(value) {
      return !isNaN(parseFloat(value)) && isFinite(value);
    },
    
    formatNumber(value) {
      if (value === null || value === undefined) return '-';
      return Number(value).toFixed(4);
    },
    
    renderMissingChart() {
      if (!this.data.missingInfo || this.missingData.length === 0) return;
      
      const container = document.getElementById('missing-chart');
      if (!container) return;
      
      if (this.missingChart) {
        this.missingChart.dispose();
      }
      
      this.missingChart = echarts.init(container);
      
      const option = {
        title: {
          text: '缺失值分布',
          left: 'center',
          textStyle: {
            fontSize: 14
          }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: function(params) {
            const data = params[0];
            return `${data.name}<br/>缺失比例: ${(data.value * 100).toFixed(2)}%`;
          }
        },
        xAxis: {
          type: 'category',
          data: this.missingData.map(item => item.column),
          axisLabel: {
            rotate: 45
          }
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: '{value}%'
          }
        },
        series: [{
          name: '缺失比例',
          type: 'bar',
          data: this.missingData.map(item => (item.missingRatio * 100).toFixed(2)),
          itemStyle: {
            color: function(params) {
              const value = params.value;
              if (value > 10) return '#f56c6c';
              if (value > 5) return '#e6a23c';
              return '#67c23a';
            }
          }
        }]
      };
      
      this.missingChart.setOption(option);
      
      // 触发图表准备就绪事件
      this.$emit('chart-ready', this.missingChart);
    }
  }
};
</script>

<style scoped>
.overview-display {
  padding: 0;
}
</style>
