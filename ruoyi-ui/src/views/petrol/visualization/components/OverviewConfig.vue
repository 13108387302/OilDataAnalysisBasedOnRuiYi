<template>
  <div class="config-container">
    <el-form size="small" label-width="80px">
      <el-form-item label="显示行数">
        <el-input-number 
          v-model="config.previewRows" 
          :min="5" 
          :max="100" 
          style="width: 100%;"
          @change="emitChange">
        </el-input-number>
      </el-form-item>
      
      <el-form-item label="显示列">
        <el-select 
          v-model="config.selectedColumns" 
          multiple 
          placeholder="选择要显示的列" 
          style="width: 100%;"
          @change="emitChange">
          <el-option
            v-for="column in columns"
            :key="column"
            :label="column"
            :value="column">
          </el-option>
        </el-select>
      </el-form-item>
      
      <el-form-item label="统计信息">
        <el-checkbox-group v-model="config.statisticsTypes" @change="emitChange">
          <el-checkbox label="basic">基础统计</el-checkbox>
          <el-checkbox label="distribution">分布信息</el-checkbox>
          <el-checkbox label="missing">缺失值</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: "OverviewConfig",
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
        previewRows: 10,
        selectedColumns: [],
        statisticsTypes: ['basic', 'missing']
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
    columns: {
      handler(newVal) {
        if (newVal.length > 0 && this.config.selectedColumns.length === 0) {
          this.config.selectedColumns = newVal.slice(0, 5); // 默认选择前5列
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
