<template>
  <div class="user-guide">
    <!-- 引导遮罩 -->
    <div v-if="visible" class="guide-overlay" @click="handleOverlayClick">
      <!-- 高亮区域 -->
      <div 
        class="guide-highlight" 
        :style="highlightStyle"
        v-if="currentStep && currentStep.element">
      </div>
      
      <!-- 引导卡片 -->
      <div 
        class="guide-card" 
        :style="cardStyle"
        v-if="currentStep">
        <div class="guide-header">
          <h3 class="guide-title">{{ currentStep.title }}</h3>
          <el-button 
            type="text" 
            icon="el-icon-close" 
            @click="close"
            class="guide-close">
          </el-button>
        </div>
        
        <div class="guide-content">
          <p class="guide-description">{{ currentStep.description }}</p>
          <div v-if="currentStep.image" class="guide-image">
            <img :src="currentStep.image" :alt="currentStep.title" />
          </div>
        </div>
        
        <div class="guide-footer">
          <div class="guide-progress">
            <span class="progress-text">
              {{ currentStepIndex + 1 }} / {{ steps.length }}
            </span>
            <div class="progress-bar">
              <div 
                class="progress-fill" 
                :style="{ width: progressPercentage + '%' }">
              </div>
            </div>
          </div>
          
          <div class="guide-actions">
            <el-button 
              v-if="currentStepIndex > 0"
              @click="prevStep"
              size="small">
              上一步
            </el-button>
            <el-button 
              v-if="currentStepIndex < steps.length - 1"
              type="primary"
              @click="nextStep"
              size="small">
              下一步
            </el-button>
            <el-button 
              v-else
              type="primary"
              @click="finish"
              size="small">
              完成
            </el-button>
            <el-button 
              type="text"
              @click="skip"
              size="small">
              跳过
            </el-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 引导触发按钮 -->
    <el-button 
      v-if="showTrigger && !visible"
      class="guide-trigger"
      type="primary"
      icon="el-icon-question"
      circle
      @click="start">
    </el-button>
  </div>
</template>

<script>
export default {
  name: 'UserGuide',
  props: {
    // 引导步骤
    steps: {
      type: Array,
      required: true,
      default: () => []
    },
    // 是否显示触发按钮
    showTrigger: {
      type: Boolean,
      default: true
    },
    // 是否允许点击遮罩关闭
    maskClosable: {
      type: Boolean,
      default: false
    },
    // 引导唯一标识（用于记录用户是否已看过）
    guideId: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      visible: false,
      currentStepIndex: 0,
      highlightRect: null
    };
  },
  computed: {
    currentStep() {
      return this.steps[this.currentStepIndex];
    },
    progressPercentage() {
      return ((this.currentStepIndex + 1) / this.steps.length) * 100;
    },
    highlightStyle() {
      if (!this.highlightRect) return {};
      
      return {
        left: this.highlightRect.left + 'px',
        top: this.highlightRect.top + 'px',
        width: this.highlightRect.width + 'px',
        height: this.highlightRect.height + 'px'
      };
    },
    cardStyle() {
      if (!this.highlightRect) return {};
      
      const cardWidth = 320;
      const cardHeight = 200;
      const padding = 20;
      
      let left = this.highlightRect.left + this.highlightRect.width + padding;
      let top = this.highlightRect.top;
      
      // 如果右侧空间不够，放到左侧
      if (left + cardWidth > window.innerWidth) {
        left = this.highlightRect.left - cardWidth - padding;
      }
      
      // 如果左侧空间也不够，放到下方
      if (left < 0) {
        left = this.highlightRect.left;
        top = this.highlightRect.top + this.highlightRect.height + padding;
      }
      
      // 如果下方空间不够，放到上方
      if (top + cardHeight > window.innerHeight) {
        top = this.highlightRect.top - cardHeight - padding;
      }
      
      // 确保不超出屏幕边界
      left = Math.max(padding, Math.min(left, window.innerWidth - cardWidth - padding));
      top = Math.max(padding, Math.min(top, window.innerHeight - cardHeight - padding));
      
      return {
        left: left + 'px',
        top: top + 'px'
      };
    }
  },
  mounted() {
    // 检查用户是否已经看过这个引导
    if (!this.hasSeenGuide()) {
      // 延迟显示引导，让页面先渲染完成
      setTimeout(() => {
        this.start();
      }, 1000);
    }
  },
  methods: {
    start() {
      this.visible = true;
      this.currentStepIndex = 0;
      this.updateHighlight();
      this.$emit('start');
    },
    nextStep() {
      if (this.currentStepIndex < this.steps.length - 1) {
        this.currentStepIndex++;
        this.updateHighlight();
        this.$emit('step-change', this.currentStepIndex);
      }
    },
    prevStep() {
      if (this.currentStepIndex > 0) {
        this.currentStepIndex--;
        this.updateHighlight();
        this.$emit('step-change', this.currentStepIndex);
      }
    },
    finish() {
      this.close();
      this.markAsSeenGuide();
      this.$emit('finish');
    },
    skip() {
      this.close();
      this.markAsSeenGuide();
      this.$emit('skip');
    },
    close() {
      this.visible = false;
      this.highlightRect = null;
      this.$emit('close');
    },
    handleOverlayClick() {
      if (this.maskClosable) {
        this.close();
      }
    },
    updateHighlight() {
      this.$nextTick(() => {
        if (this.currentStep && this.currentStep.element) {
          const element = document.querySelector(this.currentStep.element);
          if (element) {
            const rect = element.getBoundingClientRect();
            this.highlightRect = {
              left: rect.left - 4,
              top: rect.top - 4,
              width: rect.width + 8,
              height: rect.height + 8
            };
            
            // 滚动到目标元素
            element.scrollIntoView({
              behavior: 'smooth',
              block: 'center'
            });
          }
        }
      });
    },
    hasSeenGuide() {
      return localStorage.getItem(`guide_seen_${this.guideId}`) === 'true';
    },
    markAsSeenGuide() {
      localStorage.setItem(`guide_seen_${this.guideId}`, 'true');
    }
  }
};
</script>

<style scoped>
.user-guide {
  position: relative;
}

.guide-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  z-index: 9999;
}

.guide-highlight {
  position: absolute;
  border: 2px solid #409eff;
  border-radius: 4px;
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
  pointer-events: none;
  animation: highlight-pulse 2s infinite;
}

@keyframes highlight-pulse {
  0%, 100% {
    box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.3);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(64, 158, 255, 0.1);
  }
}

.guide-card {
  position: absolute;
  width: 320px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  animation: card-appear 0.3s ease-out;
}

@keyframes card-appear {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.guide-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.guide-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.guide-close {
  padding: 0;
  color: #909399;
}

.guide-content {
  padding: 20px;
}

.guide-description {
  margin: 0 0 16px 0;
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
}

.guide-image {
  text-align: center;
}

.guide-image img {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
}

.guide-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.guide-progress {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.progress-text {
  font-size: 12px;
  color: #909399;
  margin-right: 12px;
  min-width: 40px;
}

.progress-bar {
  flex: 1;
  height: 4px;
  background: #e4e7ed;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #409eff;
  transition: width 0.3s ease;
}

.guide-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.guide-trigger {
  position: fixed;
  bottom: 80px;
  right: 20px;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

@media (max-width: 768px) {
  .guide-card {
    width: calc(100vw - 40px);
    max-width: 320px;
  }
  
  .guide-trigger {
    bottom: 60px;
    right: 16px;
  }
}
</style>
