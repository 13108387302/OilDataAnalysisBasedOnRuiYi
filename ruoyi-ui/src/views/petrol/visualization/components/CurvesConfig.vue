<template>
  <div class="config-container">
    <el-form size="small" label-width="80px">
      <el-form-item label="深度列" required>
        <el-select 
          v-model="config.depthColumn" 
          placeholder="选择深度列" 
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
      
      <el-form-item label="特征列" required>
        <el-select 
          v-model="config.featureColumns" 
          multiple 
          placeholder="选择特征列" 
          style="width: 100%;"
          @change="emitChange">
          <el-option
            v-for="column in availableFeatureColumns"
            :key="column"
            :label="column"
            :value="column">
          </el-option>
        </el-select>
      </el-form-item>
      
      <el-form-item label="深度范围">
        <el-row :gutter="10">
          <el-col :span="12">
            <el-input-number 
              v-model="config.depthMin" 
              placeholder="最小深度"
              style="width: 100%;"
              @change="emitChange">
            </el-input-number>
          </el-col>
          <el-col :span="12">
            <el-input-number 
              v-model="config.depthMax" 
              placeholder="最大深度"
              style="width: 100%;"
              @change="emitChange">
            </el-input-number>
          </el-col>
        </el-row>
      </el-form-item>
      
      <el-form-item label="图表类型">
        <el-radio-group v-model="config.chartType" @change="emitChange">
          <el-radio-button label="line">折线图</el-radio-button>
          <el-radio-button label="scatter">散点图</el-radio-button>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="布局模式">
        <el-radio-group v-model="config.layoutMode" @change="emitChange">
          <el-radio-button label="parallel">并列坐标系</el-radio-button>
          <el-radio-button label="overlay">叠加坐标系</el-radio-button>
        </el-radio-group>
        <div style="font-size: 12px; color: #666; margin-top: 5px;">
          并列模式：每个特征独立坐标系，深度为纵坐标<br/>
          叠加模式：所有特征在同一坐标系，深度为横坐标
        </div>
      </el-form-item>

      <el-form-item label="显示选项">
        <el-checkbox-group v-model="config.displayOptions" @change="emitChange">
          <el-checkbox label="grid">显示网格</el-checkbox>
          <el-checkbox label="legend">显示图例</el-checkbox>
          <el-checkbox label="dataZoom">数据缩放</el-checkbox>
          <el-checkbox label="smooth">平滑曲线</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      
      <el-form-item label="颜色主题">
        <el-select v-model="config.colorTheme" @change="emitChange" style="width: 100%;">
          <el-option label="默认" value="default"></el-option>
          <el-option label="石油专业" value="petroleum"></el-option>
          <el-option label="地质" value="geology"></el-option>
          <el-option label="彩虹" value="rainbow"></el-option>
        </el-select>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: "CurvesConfig",
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
        depthColumn: '',
        featureColumns: [],
        depthMin: null,
        depthMax: null,
        chartType: 'line',
        layoutMode: 'parallel',
        displayOptions: ['grid', 'legend', 'dataZoom'],
        colorTheme: 'petroleum'
      }
    };
  },
  computed: {
    availableFeatureColumns() {
      return this.numericColumns.filter(col => col !== this.config.depthColumn);
    }
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
        if (newVal.length > 0) {
          // 自动设置默认值
          if (!this.config.depthColumn && newVal.includes('depth')) {
            this.config.depthColumn = 'depth';
          } else if (!this.config.depthColumn) {
            this.config.depthColumn = newVal[0];
          }
          
          // 自动选择特征列
          if (this.config.featureColumns.length === 0) {
            const features = newVal.filter(col => col !== this.config.depthColumn).slice(0, 3);
            this.config.featureColumns = features;
          }
          
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
