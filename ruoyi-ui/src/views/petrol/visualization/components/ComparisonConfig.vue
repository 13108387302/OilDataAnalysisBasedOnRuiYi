<template>
  <div class="config-container">
    <el-form size="small" label-width="80px">
      <el-form-item label="对比类型">
        <el-radio-group v-model="config.comparisonType" @change="handleComparisonTypeChange">
          <el-radio-button label="datasets">数据集对比</el-radio-button>
          <el-radio-button label="columns">列对比</el-radio-button>
          <el-radio-button label="algorithms">算法结果对比</el-radio-button>
        </el-radio-group>
      </el-form-item>
      
      <!-- 数据集对比配置 -->
      <div v-if="config.comparisonType === 'datasets'">
        <el-form-item label="对比数据集">
          <el-select 
            v-model="config.compareDatasets" 
            multiple 
            placeholder="选择要对比的数据集" 
            style="width: 100%;"
            @change="emitChange">
            <!-- 这里需要从父组件传入可用的数据集列表 -->
            <el-option label="当前数据集" value="current"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="对比指标">
          <el-select 
            v-model="config.compareMetrics" 
            multiple 
            placeholder="选择对比指标" 
            style="width: 100%;"
            @change="emitChange">
            <el-option label="行数" value="rowCount"></el-option>
            <el-option label="列数" value="columnCount"></el-option>
            <el-option label="缺失值比例" value="missingRatio"></el-option>
            <el-option label="数据类型分布" value="dataTypes"></el-option>
          </el-select>
        </el-form-item>
      </div>
      
      <!-- 列对比配置 -->
      <div v-if="config.comparisonType === 'columns'">
        <el-form-item label="对比列">
          <el-select 
            v-model="config.compareColumns" 
            multiple 
            placeholder="选择要对比的列" 
            style="width: 100%;"
            @change="emitChange">
            <el-option
              v-for="column in numericColumns"
              :key="column"
              :label="column"
              :value="column">
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="对比方式">
          <el-checkbox-group v-model="config.comparisonMethods" @change="emitChange">
            <el-checkbox label="distribution">分布对比</el-checkbox>
            <el-checkbox label="statistics">统计对比</el-checkbox>
            <el-checkbox label="correlation">相关性对比</el-checkbox>
            <el-checkbox label="trend">趋势对比</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </div>
      
      <!-- 算法结果对比配置 -->
      <div v-if="config.comparisonType === 'algorithms'">
        <el-form-item label="算法任务">
          <el-select 
            v-model="config.algorithmTasks" 
            multiple 
            placeholder="选择算法任务" 
            style="width: 100%;"
            @change="emitChange">
            <!-- 这里需要从API获取算法任务列表 -->
            <el-option label="线性回归任务1" value="task1"></el-option>
            <el-option label="多项式回归任务2" value="task2"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="对比指标">
          <el-checkbox-group v-model="config.algorithmMetrics" @change="emitChange">
            <el-checkbox label="accuracy">准确度</el-checkbox>
            <el-checkbox label="rmse">均方根误差</el-checkbox>
            <el-checkbox label="mae">平均绝对误差</el-checkbox>
            <el-checkbox label="r2">决定系数</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </div>
      
      <el-form-item label="图表类型">
        <el-select v-model="config.chartType" @change="emitChange" style="width: 100%;">
          <el-option label="柱状图" value="bar"></el-option>
          <el-option label="雷达图" value="radar"></el-option>
          <el-option label="折线图" value="line"></el-option>
          <el-option label="散点图" value="scatter"></el-option>
        </el-select>
      </el-form-item>
      
      <el-form-item label="显示选项">
        <el-checkbox-group v-model="config.displayOptions" @change="emitChange">
          <el-checkbox label="showValues">显示数值</el-checkbox>
          <el-checkbox label="showPercentage">显示百分比</el-checkbox>
          <el-checkbox label="showDifference">显示差异</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: "ComparisonConfig",
  props: {
    value: {
      type: Object,
      default: () => ({})
    },
    columns: {
      type: Array,
      default: () => []
    },
    numericColumns: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      config: {
        comparisonType: 'columns',
        compareDatasets: [],
        compareMetrics: ['rowCount', 'columnCount'],
        compareColumns: [],
        comparisonMethods: ['distribution', 'statistics'],
        algorithmTasks: [],
        algorithmMetrics: ['accuracy', 'rmse'],
        chartType: 'bar',
        displayOptions: ['showValues']
      }
    };
  },
  watch: {
    value: {
      handler(newVal) {
        this.config = { ...this.config, ...newVal };
      },
      immediate: true
    },
    numericColumns: {
      handler(newVal) {
        if (newVal.length > 0 && this.config.compareColumns.length === 0) {
          this.config.compareColumns = newVal.slice(0, 3);
          this.emitChange();
        }
      },
      immediate: true
    }
  },
  methods: {
    handleComparisonTypeChange() {
      // 重置相关配置
      this.config.compareDatasets = [];
      this.config.compareColumns = [];
      this.config.algorithmTasks = [];
      this.emitChange();
    },
    
    emitChange() {
      this.$emit('input', this.config);
      this.$emit('config-change', this.config);
    }
  }
};
</script>

<style scoped>
.config-container {
  padding: 10px 0;
}
</style>
