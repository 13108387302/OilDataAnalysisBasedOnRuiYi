<template>
  <div class="config-container">
    <el-form size="small" label-width="80px">
      <el-form-item label="分析列">
        <el-select 
          v-model="config.selectedColumns" 
          multiple 
          placeholder="选择要分析的列" 
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
      
      <el-form-item label="统计类型">
        <el-checkbox-group v-model="config.statisticsTypes" @change="emitChange">
          <el-checkbox label="descriptive">描述性统计</el-checkbox>
          <el-checkbox label="distribution">分布统计</el-checkbox>
          <el-checkbox label="outliers">异常值检测</el-checkbox>
          <el-checkbox label="correlation">相关性</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      
      <el-form-item label="显示格式">
        <el-radio-group v-model="config.displayFormat" @change="emitChange">
          <el-radio-button label="table">表格</el-radio-button>
          <el-radio-button label="chart">图表</el-radio-button>
          <el-radio-button label="both">表格+图表</el-radio-button>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="精度">
        <el-input-number 
          v-model="config.precision" 
          :min="2" 
          :max="8" 
          style="width: 100%;"
          @change="emitChange">
        </el-input-number>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: "StatisticsConfig",
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
        selectedColumns: [],
        statisticsTypes: ['descriptive', 'distribution'],
        displayFormat: 'both',
        precision: 4
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
        if (newVal.length > 0 && this.config.selectedColumns.length === 0) {
          this.config.selectedColumns = newVal.slice(0, 5);
          this.emitChange();
        }
      },
      immediate: true
    }
  },
  methods: {
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
