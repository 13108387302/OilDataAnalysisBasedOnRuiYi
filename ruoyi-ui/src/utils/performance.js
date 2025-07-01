/**
 * 前端性能监控工具
 * 用于监控页面加载性能、组件渲染性能和用户交互性能
 */

class PerformanceMonitor {
  constructor() {
    this.metrics = {};
    this.observers = [];
    this.init();
  }

  init() {
    // 监控页面加载性能
    this.observePageLoad();
    // 监控长任务
    this.observeLongTasks();
    // 监控内存使用
    this.observeMemory();
  }

  /**
   * 监控页面加载性能
   */
  observePageLoad() {
    if (typeof window !== 'undefined' && 'performance' in window) {
      window.addEventListener('load', () => {
        setTimeout(() => {
          const perfData = performance.getEntriesByType('navigation')[0];
          if (perfData) {
            this.metrics.pageLoad = {
              // DNS查询时间
              dnsTime: perfData.domainLookupEnd - perfData.domainLookupStart,
              // TCP连接时间
              tcpTime: perfData.connectEnd - perfData.connectStart,
              // 请求响应时间
              requestTime: perfData.responseEnd - perfData.requestStart,
              // DOM解析时间
              domParseTime: perfData.domContentLoadedEventEnd - perfData.domContentLoadedEventStart,
              // 页面完全加载时间
              loadTime: perfData.loadEventEnd - perfData.loadEventStart,
              // 首次内容绘制
              fcp: this.getFCP(),
              // 最大内容绘制
              lcp: this.getLCP()
            };
            this.reportMetrics('pageLoad', this.metrics.pageLoad);
          }
        }, 0);
      });
    }
  }

  /**
   * 获取首次内容绘制时间
   */
  getFCP() {
    const fcpEntry = performance.getEntriesByName('first-contentful-paint')[0];
    return fcpEntry ? fcpEntry.startTime : 0;
  }

  /**
   * 获取最大内容绘制时间
   */
  getLCP() {
    return new Promise((resolve) => {
      if ('PerformanceObserver' in window) {
        const observer = new PerformanceObserver((list) => {
          const entries = list.getEntries();
          const lastEntry = entries[entries.length - 1];
          resolve(lastEntry.startTime);
        });
        observer.observe({ entryTypes: ['largest-contentful-paint'] });
        this.observers.push(observer);
      } else {
        resolve(0);
      }
    });
  }

  /**
   * 监控长任务
   */
  observeLongTasks() {
    if ('PerformanceObserver' in window) {
      const observer = new PerformanceObserver((list) => {
        const entries = list.getEntries();
        entries.forEach(entry => {
          if (entry.duration > 50) { // 超过50ms的任务
            console.warn('Long task detected:', {
              duration: entry.duration,
              startTime: entry.startTime,
              name: entry.name
            });
          }
        });
      });
      observer.observe({ entryTypes: ['longtask'] });
      this.observers.push(observer);
    }
  }

  /**
   * 监控内存使用
   */
  observeMemory() {
    if ('memory' in performance) {
      setInterval(() => {
        const memory = performance.memory;
        this.metrics.memory = {
          usedJSHeapSize: memory.usedJSHeapSize,
          totalJSHeapSize: memory.totalJSHeapSize,
          jsHeapSizeLimit: memory.jsHeapSizeLimit,
          usage: (memory.usedJSHeapSize / memory.jsHeapSizeLimit * 100).toFixed(2)
        };
        
        // 内存使用超过80%时警告
        if (this.metrics.memory.usage > 80) {
          console.warn('High memory usage detected:', this.metrics.memory);
        }
      }, 30000); // 每30秒检查一次
    }
  }

  /**
   * 测量组件渲染时间
   */
  measureComponent(componentName, fn) {
    const startTime = performance.now();
    const result = fn();
    const endTime = performance.now();
    const duration = endTime - startTime;
    
    if (!this.metrics.components) {
      this.metrics.components = {};
    }
    
    this.metrics.components[componentName] = {
      duration,
      timestamp: Date.now()
    };
    
    if (duration > 16) { // 超过一帧的时间
      console.warn(`Component ${componentName} render time: ${duration.toFixed(2)}ms`);
    }
    
    return result;
  }

  /**
   * 测量API请求性能
   */
  measureAPI(apiName, promise) {
    const startTime = performance.now();
    
    return promise.then(result => {
      const endTime = performance.now();
      const duration = endTime - startTime;
      
      if (!this.metrics.api) {
        this.metrics.api = {};
      }
      
      this.metrics.api[apiName] = {
        duration,
        timestamp: Date.now(),
        status: 'success'
      };
      
      if (duration > 3000) { // 超过3秒的请求
        console.warn(`Slow API request ${apiName}: ${duration.toFixed(2)}ms`);
      }
      
      return result;
    }).catch(error => {
      const endTime = performance.now();
      const duration = endTime - startTime;
      
      if (!this.metrics.api) {
        this.metrics.api = {};
      }
      
      this.metrics.api[apiName] = {
        duration,
        timestamp: Date.now(),
        status: 'error',
        error: error.message
      };
      
      throw error;
    });
  }

  /**
   * 获取性能报告
   */
  getReport() {
    return {
      timestamp: Date.now(),
      userAgent: navigator.userAgent,
      url: window.location.href,
      metrics: this.metrics
    };
  }

  /**
   * 上报性能数据
   */
  reportMetrics(type, data) {
    // 这里可以发送到性能监控服务
    if (process.env.NODE_ENV === 'development') {
      console.log(`Performance ${type}:`, data);
    }
    
    // 可以集成第三方监控服务
    // 例如：发送到后端API或第三方服务
  }

  /**
   * 清理观察者
   */
  disconnect() {
    this.observers.forEach(observer => {
      observer.disconnect();
    });
    this.observers = [];
  }
}

// 创建全局实例
const performanceMonitor = new PerformanceMonitor();

// Vue插件形式
export const PerformancePlugin = {
  install(Vue) {
    // 添加全局方法
    Vue.prototype.$performance = performanceMonitor;
    
    // 添加全局混入，监控组件性能
    Vue.mixin({
      beforeCreate() {
        this._renderStart = performance.now();
      },
      mounted() {
        const renderTime = performance.now() - this._renderStart;
        if (renderTime > 16 && this.$options.name) {
          console.warn(`Component ${this.$options.name} mount time: ${renderTime.toFixed(2)}ms`);
        }
      }
    });
  }
};

// 工具函数
export const performanceUtils = {
  // 防抖函数
  debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
      const later = () => {
        clearTimeout(timeout);
        func(...args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  },

  // 节流函数
  throttle(func, limit) {
    let inThrottle;
    return function(...args) {
      if (!inThrottle) {
        func.apply(this, args);
        inThrottle = true;
        setTimeout(() => inThrottle = false, limit);
      }
    };
  },

  // 懒加载图片
  lazyLoadImages() {
    if ('IntersectionObserver' in window) {
      const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            const img = entry.target;
            img.src = img.dataset.src;
            img.classList.remove('lazy');
            observer.unobserve(img);
          }
        });
      });

      document.querySelectorAll('img[data-src]').forEach(img => {
        imageObserver.observe(img);
      });
    }
  }
};

export default performanceMonitor;
