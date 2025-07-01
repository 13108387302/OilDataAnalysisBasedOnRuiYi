<template>
  <div class="error-boundary">
    <div v-if="hasError" class="error-container">
      <div class="error-icon">
        <i class="el-icon-warning-outline"></i>
      </div>
      <div class="error-content">
        <h3 class="error-title">{{ errorTitle }}</h3>
        <p class="error-message">{{ errorMessage }}</p>
        <div class="error-details" v-if="showDetails">
          <el-collapse>
            <el-collapse-item title="错误详情" name="details">
              <pre class="error-stack">{{ errorStack }}</pre>
            </el-collapse-item>
          </el-collapse>
        </div>
        <div class="error-actions">
          <el-button type="primary" @click="retry" v-if="canRetry">
            <i class="el-icon-refresh"></i> 重试
          </el-button>
          <el-button @click="goBack" v-if="canGoBack">
            <i class="el-icon-back"></i> 返回
          </el-button>
          <el-button @click="reportError" v-if="canReport">
            <i class="el-icon-message"></i> 报告问题
          </el-button>
          <el-button type="text" @click="toggleDetails">
            {{ showDetails ? '隐藏' : '显示' }}详情
          </el-button>
        </div>
      </div>
    </div>
    <slot v-else></slot>
  </div>
</template>

<script>
export default {
  name: 'ErrorBoundary',
  props: {
    // 是否可以重试
    canRetry: {
      type: Boolean,
      default: true
    },
    // 是否可以返回
    canGoBack: {
      type: Boolean,
      default: true
    },
    // 是否可以报告错误
    canReport: {
      type: Boolean,
      default: true
    },
    // 自定义错误标题
    customTitle: {
      type: String,
      default: ''
    },
    // 自定义错误消息
    customMessage: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      hasError: false,
      error: null,
      errorInfo: null,
      showDetails: false
    };
  },
  computed: {
    errorTitle() {
      if (this.customTitle) return this.customTitle;
      
      if (this.error) {
        if (this.error.name === 'ChunkLoadError') {
          return '资源加载失败';
        } else if (this.error.name === 'NetworkError') {
          return '网络连接错误';
        } else if (this.error.name === 'TypeError') {
          return '数据类型错误';
        } else if (this.error.name === 'ReferenceError') {
          return '引用错误';
        }
      }
      
      return '系统错误';
    },
    errorMessage() {
      if (this.customMessage) return this.customMessage;
      
      if (this.error) {
        if (this.error.name === 'ChunkLoadError') {
          return '页面资源加载失败，可能是网络问题或版本更新导致。请尝试刷新页面或清除浏览器缓存。';
        } else if (this.error.name === 'NetworkError') {
          return '网络连接出现问题，请检查网络连接后重试。';
        } else if (this.error.message) {
          return this.error.message;
        }
      }
      
      return '抱歉，系统遇到了一个意外错误。我们已经记录了这个问题，请稍后重试。';
    },
    errorStack() {
      if (this.error && this.error.stack) {
        return this.error.stack;
      }
      return '暂无详细错误信息';
    }
  },
  errorCaptured(err, instance, info) {
    this.hasError = true;
    this.error = err;
    this.errorInfo = info;
    
    // 记录错误到控制台
    console.error('ErrorBoundary captured an error:', err);
    console.error('Error info:', info);
    
    // 发送错误报告到监控系统
    this.logError(err, info);
    
    // 阻止错误继续传播
    return false;
  },
  methods: {
    retry() {
      this.hasError = false;
      this.error = null;
      this.errorInfo = null;
      this.showDetails = false;
      
      // 触发重试事件
      this.$emit('retry');
      
      // 如果没有监听重试事件，则刷新页面
      this.$nextTick(() => {
        if (this.$listeners.retry === undefined) {
          window.location.reload();
        }
      });
    },
    goBack() {
      this.$emit('go-back');
      
      // 如果没有监听返回事件，则使用浏览器返回
      this.$nextTick(() => {
        if (this.$listeners['go-back'] === undefined) {
          if (window.history.length > 1) {
            this.$router.go(-1);
          } else {
            this.$router.push('/');
          }
        }
      });
    },
    reportError() {
      const errorReport = {
        error: this.error ? this.error.toString() : 'Unknown error',
        stack: this.error ? this.error.stack : '',
        info: this.errorInfo,
        url: window.location.href,
        userAgent: navigator.userAgent,
        timestamp: new Date().toISOString()
      };
      
      this.$emit('report-error', errorReport);
      
      // 如果没有监听报告事件，则显示默认提示
      this.$nextTick(() => {
        if (this.$listeners['report-error'] === undefined) {
          this.$message.success('错误报告已发送，感谢您的反馈！');
        }
      });
    },
    toggleDetails() {
      this.showDetails = !this.showDetails;
    },
    logError(error, info) {
      // 发送到错误监控服务
      if (window.errorLogger) {
        window.errorLogger.log({
          error: error.toString(),
          stack: error.stack,
          componentStack: info,
          url: window.location.href,
          userAgent: navigator.userAgent,
          timestamp: Date.now()
        });
      }
      
      // 发送到后端API（可选）
      if (process.env.VUE_APP_ERROR_REPORTING_URL) {
        fetch(process.env.VUE_APP_ERROR_REPORTING_URL, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            error: error.toString(),
            stack: error.stack,
            info: info,
            url: window.location.href,
            userAgent: navigator.userAgent,
            timestamp: new Date().toISOString()
          })
        }).catch(err => {
          console.error('Failed to report error:', err);
        });
      }
    }
  }
};
</script>

<style scoped>
.error-boundary {
  width: 100%;
  height: 100%;
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 40px 20px;
  text-align: center;
}

.error-icon {
  font-size: 64px;
  color: #f56c6c;
  margin-bottom: 24px;
}

.error-content {
  max-width: 600px;
  width: 100%;
}

.error-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.error-message {
  font-size: 16px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 32px;
}

.error-details {
  margin-bottom: 32px;
  text-align: left;
}

.error-stack {
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 16px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #606266;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .error-container {
    padding: 20px 16px;
    min-height: 300px;
  }
  
  .error-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }
  
  .error-title {
    font-size: 20px;
    margin-bottom: 12px;
  }
  
  .error-message {
    font-size: 14px;
    margin-bottom: 24px;
  }
  
  .error-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .error-actions .el-button {
    width: 100%;
    max-width: 200px;
  }
}
</style>
