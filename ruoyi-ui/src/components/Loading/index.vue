<template>
  <div class="loading-container" v-if="visible">
    <div class="loading-backdrop" v-if="backdrop"></div>
    <div class="loading-content" :class="{ 'loading-center': center }">
      <div class="loading-spinner" :class="spinnerClass">
        <div class="spinner-item" v-for="i in 12" :key="i"></div>
      </div>
      <div class="loading-text" v-if="text">{{ text }}</div>
      <div class="loading-progress" v-if="showProgress">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progress + '%' }"></div>
        </div>
        <div class="progress-text">{{ progress }}%</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Loading',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    text: {
      type: String,
      default: ''
    },
    backdrop: {
      type: Boolean,
      default: true
    },
    center: {
      type: Boolean,
      default: true
    },
    size: {
      type: String,
      default: 'medium',
      validator: value => ['small', 'medium', 'large'].includes(value)
    },
    progress: {
      type: Number,
      default: 0,
      validator: value => value >= 0 && value <= 100
    },
    showProgress: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    spinnerClass() {
      return `spinner-${this.size}`;
    }
  }
};
</script>

<style scoped>
.loading-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.loading-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.8);
  z-index: 1000;
}

.loading-content {
  position: relative;
  z-index: 1001;
}

.loading-center {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.loading-spinner {
  display: inline-block;
  position: relative;
}

.spinner-small {
  width: 20px;
  height: 20px;
}

.spinner-medium {
  width: 40px;
  height: 40px;
}

.spinner-large {
  width: 60px;
  height: 60px;
}

.spinner-item {
  position: absolute;
  width: 2px;
  height: 25%;
  background-color: #409eff;
  border-radius: 1px;
  animation: spinner-fade 1.2s linear infinite;
}

.spinner-small .spinner-item {
  width: 1px;
}

.spinner-large .spinner-item {
  width: 3px;
}

.spinner-item:nth-child(1) {
  transform: rotate(0deg) translate(0, -150%);
  animation-delay: 0s;
}

.spinner-item:nth-child(2) {
  transform: rotate(30deg) translate(0, -150%);
  animation-delay: -0.1s;
}

.spinner-item:nth-child(3) {
  transform: rotate(60deg) translate(0, -150%);
  animation-delay: -0.2s;
}

.spinner-item:nth-child(4) {
  transform: rotate(90deg) translate(0, -150%);
  animation-delay: -0.3s;
}

.spinner-item:nth-child(5) {
  transform: rotate(120deg) translate(0, -150%);
  animation-delay: -0.4s;
}

.spinner-item:nth-child(6) {
  transform: rotate(150deg) translate(0, -150%);
  animation-delay: -0.5s;
}

.spinner-item:nth-child(7) {
  transform: rotate(180deg) translate(0, -150%);
  animation-delay: -0.6s;
}

.spinner-item:nth-child(8) {
  transform: rotate(210deg) translate(0, -150%);
  animation-delay: -0.7s;
}

.spinner-item:nth-child(9) {
  transform: rotate(240deg) translate(0, -150%);
  animation-delay: -0.8s;
}

.spinner-item:nth-child(10) {
  transform: rotate(270deg) translate(0, -150%);
  animation-delay: -0.9s;
}

.spinner-item:nth-child(11) {
  transform: rotate(300deg) translate(0, -150%);
  animation-delay: -1s;
}

.spinner-item:nth-child(12) {
  transform: rotate(330deg) translate(0, -150%);
  animation-delay: -1.1s;
}

@keyframes spinner-fade {
  0%, 39%, 100% {
    opacity: 0;
  }
  40% {
    opacity: 1;
  }
}

.loading-text {
  margin-top: 16px;
  color: #606266;
  font-size: 14px;
}

.loading-progress {
  margin-top: 16px;
  width: 200px;
}

.progress-bar {
  width: 100%;
  height: 4px;
  background-color: #f0f0f0;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: #409eff;
  border-radius: 2px;
  transition: width 0.3s ease;
}

.progress-text {
  margin-top: 8px;
  color: #606266;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .loading-progress {
    width: 150px;
  }
  
  .loading-text {
    font-size: 12px;
  }
}
</style>
