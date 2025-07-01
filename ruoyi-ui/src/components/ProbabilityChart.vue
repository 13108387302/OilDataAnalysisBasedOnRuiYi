<template>
  <div class="probability-chart" :class="{ 'compact-mode': compact }">
    <div v-if="!compact" class="chart-header">
      <h4>{{ title }}</h4>
      <el-tag :type="getConfidenceType(maxProbability)" size="small">
        置信度: {{ (maxProbability * 100).toFixed(2) }}%
      </el-tag>
    </div>

    <!-- Compact模式：只显示最高概率的类别 -->
    <div v-if="compact" class="compact-display">
      <span class="compact-class">{{ getTopClassName() }}</span>
      <span class="compact-probability">{{ getTopProbability() }}%</span>
    </div>

    <!-- 完整模式：显示所有概率条 -->
    <div v-if="!compact" class="probability-bars">
      <div
        v-for="(item, index) in sortedProbabilities"
        :key="index"
        class="probability-item">
        <div class="probability-label">
          <span class="class-name">{{ item.className }}</span>
          <span class="probability-value">{{ (item.probability * 100).toFixed(2) }}%</span>
        </div>
        <div class="probability-bar-container">
          <div
            class="probability-bar"
            :style="{
              width: (item.probability * 100) + '%',
              backgroundColor: getBarColor(index, item.probability === maxProbability)
            }">
          </div>
        </div>
      </div>
    </div>
    
    <!-- 详细概率信息 -->
    <div v-if="showDetails" class="probability-details">
      <el-divider content-position="left">详细概率分布</el-divider>
      <el-row :gutter="10">
        <el-col :span="8" v-for="(item, index) in sortedProbabilities" :key="index">
          <el-card shadow="never" class="probability-card">
            <div class="probability-card-content">
              <div class="class-icon" :style="{ backgroundColor: getBarColor(index, false) }">
                {{ item.className.charAt(0) }}
              </div>
              <div class="class-info">
                <div class="class-name">{{ item.className }}</div>
                <div class="class-probability">{{ (item.probability * 100).toFixed(2) }}%</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProbabilityChart',
  props: {
    probabilities: {
      type: Object,
      required: true,
      default: () => ({})
    },
    title: {
      type: String,
      default: '类别概率分布'
    },
    showDetails: {
      type: Boolean,
      default: false
    },
    compact: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    sortedProbabilities() {
      return Object.entries(this.probabilities)
        .map(([className, probability]) => ({
          className,
          probability: Number(probability)
        }))
        .sort((a, b) => b.probability - a.probability)
    },
    
    maxProbability() {
      return Math.max(...Object.values(this.probabilities))
    }
  },
  methods: {
    getBarColor(index, isMax) {
      const colors = [
        '#409EFF', // 蓝色
        '#67C23A', // 绿色
        '#E6A23C', // 橙色
        '#F56C6C', // 红色
        '#909399', // 灰色
        '#9C27B0', // 紫色
        '#FF9800', // 深橙色
        '#4CAF50'  // 深绿色
      ]
      
      if (isMax) {
        return '#409EFF' // 最高概率使用主色
      }
      
      return colors[index % colors.length]
    },
    
    getConfidenceType(probability) {
      if (probability >= 0.8) return 'success'
      if (probability >= 0.6) return 'warning'
      return 'danger'
    },

    getTopClassName() {
      return this.sortedProbabilities.length > 0 ? this.sortedProbabilities[0].className : ''
    },

    getTopProbability() {
      return this.sortedProbabilities.length > 0 ?
        (this.sortedProbabilities[0].probability * 100).toFixed(1) : '0.0'
    }
  }
}
</script>

<style scoped>
.probability-chart {
  width: 100%;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-header h4 {
  margin: 0;
  color: #303133;
}

.probability-bars {
  margin-bottom: 20px;
}

.probability-item {
  margin-bottom: 12px;
}

.probability-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.class-name {
  font-weight: 500;
  color: #303133;
}

.probability-value {
  font-size: 12px;
  color: #606266;
  font-weight: bold;
}

.probability-bar-container {
  width: 100%;
  height: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
  overflow: hidden;
}

.probability-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.probability-details {
  margin-top: 20px;
}

.probability-card {
  margin-bottom: 8px;
  border: 1px solid #ebeef5;
}

.probability-card-content {
  display: flex;
  align-items: center;
  padding: 8px;
}

.class-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 14px;
  margin-right: 12px;
}

.class-info {
  flex: 1;
}

.class-info .class-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
}

.class-info .class-probability {
  font-size: 12px;
  color: #606266;
  font-weight: bold;
}

/* Compact模式样式 */
.compact-mode {
  padding: 8px;
}

.compact-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.compact-class {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.compact-probability {
  font-size: 12px;
  color: #409EFF;
  font-weight: bold;
}
</style>
