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
      
      <el-form-item label="相关性方法">
        <el-select v-model="config.method" @change="emitChange" style="width: 100%;">
          <el-option label="皮尔逊相关" value="pearson"></el-option>
          <el-option label="斯皮尔曼相关" value="spearman"></el-option>
          <el-option label="肯德尔相关" value="kendall"></el-option>
        </el-select>
      </el-form-item>
      
      <el-form-item label="显示类型">
        <el-radio-group v-model="config.displayType" @change="emitChange">
          <el-radio-button label="heatmap">热力图</el-radio-button>
          <el-radio-button label="network">网络图</el-radio-button>
          <el-radio-button label="matrix">矩阵表</el-radio-button>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="阈值设置">
        <el-slider
          v-model="config.threshold"
          :min="0"
          :max="1"
          :step="0.1"
          show-stops
          show-input
          @change="emitChange">
        </el-slider>
        <div style="font-size: 12px; color: #666; margin-top: 5px;">
          只显示相关系数绝对值大于 {{ config.threshold }} 的关系
        </div>
      </el-form-item>
      
      <el-form-item label="颜色方案">
        <el-select v-model="config.colorScheme" @change="emitChange" style="width: 100%;">
          <el-option label="红蓝渐变" value="redblue"></el-option>
          <el-option label="彩虹色" value="rainbow"></el-option>
          <el-option label="热力色" value="heat"></el-option>
          <el-option label="冷暖色" value="coolwarm"></el-option>
        </el-select>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: "CorrelationConfig",
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
        method: 'pearson',
        displayType: 'heatmap',
        threshold: 0.3,
        colorScheme: 'redblue'
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
          this.config.selectedColumns = newVal;
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
