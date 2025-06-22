<template>
  <div class="config-container">
    <el-form size="small" label-width="80px">
      <el-form-item label="目标列">
        <el-select 
          v-model="config.targetColumn" 
          placeholder="选择目标列" 
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
      
      <el-form-item label="分布类型">
        <el-checkbox-group v-model="config.distributionTypes" @change="emitChange">
          <el-checkbox label="histogram">直方图</el-checkbox>
          <el-checkbox label="density">密度图</el-checkbox>
          <el-checkbox label="boxplot">箱线图</el-checkbox>
          <el-checkbox label="violin">小提琴图</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      
      <el-form-item label="分组数" v-if="config.distributionTypes.includes('histogram')">
        <el-input-number 
          v-model="config.bins" 
          :min="5" 
          :max="100" 
          style="width: 100%;"
          @change="emitChange">
        </el-input-number>
      </el-form-item>
      
      <el-form-item label="分组列">
        <el-select 
          v-model="config.groupColumn" 
          placeholder="选择分组列（可选）" 
          clearable
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
        <el-checkbox-group v-model="config.showStatistics" @change="emitChange">
          <el-checkbox label="mean">均值线</el-checkbox>
          <el-checkbox label="median">中位数线</el-checkbox>
          <el-checkbox label="std">标准差</el-checkbox>
          <el-checkbox label="quartiles">四分位数</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      
      <el-form-item label="正态性检验">
        <el-checkbox v-model="config.normalityTest" @change="emitChange">
          显示正态性检验结果
        </el-checkbox>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: "DistributionConfig",
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
        targetColumn: '',
        distributionTypes: ['histogram', 'density'],
        bins: 30,
        groupColumn: '',
        showStatistics: ['mean', 'median'],
        normalityTest: false
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
        if (newVal.length > 0 && !this.config.targetColumn) {
          this.config.targetColumn = newVal[0];
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
