/**
 * 错误处理工具
 * 提供统一的错误处理、用户友好的错误信息、防抖机制等功能
 */

// 错误类型定义
export const ERROR_TYPES = {
  NETWORK_ERROR: 'NETWORK_ERROR',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  PERMISSION_ERROR: 'PERMISSION_ERROR',
  SERVER_ERROR: 'SERVER_ERROR',
  TIMEOUT_ERROR: 'TIMEOUT_ERROR',
  UNKNOWN_ERROR: 'UNKNOWN_ERROR'
};

// 错误信息映射
export const ERROR_MESSAGES = {
  // 网络错误
  [ERROR_TYPES.NETWORK_ERROR]: {
    title: '网络连接失败',
    message: '请检查网络连接后重试',
    suggestions: [
      '检查网络连接是否正常',
      '确认服务器地址是否正确',
      '稍后重试或联系管理员'
    ],
    icon: 'el-icon-wifi-off',
    type: 'error'
  },

  // 参数验证错误
  [ERROR_TYPES.VALIDATION_ERROR]: {
    title: '参数配置错误',
    message: '请检查参数配置后重试',
    suggestions: [
      '检查必填参数是否已设置',
      '确认参数值在有效范围内',
      '查看参数说明和建议值'
    ],
    icon: 'el-icon-warning',
    type: 'warning'
  },

  // 权限错误
  [ERROR_TYPES.PERMISSION_ERROR]: {
    title: '权限不足',
    message: '您没有执行此操作的权限',
    suggestions: [
      '联系管理员获取相应权限',
      '确认登录状态是否正常',
      '检查用户角色配置'
    ],
    icon: 'el-icon-lock',
    type: 'error'
  },

  // 服务器错误
  [ERROR_TYPES.SERVER_ERROR]: {
    title: '服务器错误',
    message: '服务器处理请求时发生错误',
    suggestions: [
      '稍后重试',
      '检查请求参数是否正确',
      '联系技术支持'
    ],
    icon: 'el-icon-warning-outline',
    type: 'error'
  },

  // 超时错误
  [ERROR_TYPES.TIMEOUT_ERROR]: {
    title: '请求超时',
    message: '服务器响应超时，请重试',
    suggestions: [
      '检查网络连接速度',
      '稍后重试',
      '尝试减少数据量'
    ],
    icon: 'el-icon-time',
    type: 'warning'
  },

  // 未知错误
  [ERROR_TYPES.UNKNOWN_ERROR]: {
    title: '未知错误',
    message: '发生了未知错误',
    suggestions: [
      '刷新页面重试',
      '检查浏览器控制台错误信息',
      '联系技术支持'
    ],
    icon: 'el-icon-question',
    type: 'error'
  }
};

// 特定错误码映射
export const SPECIFIC_ERROR_CODES = {
  400: ERROR_TYPES.VALIDATION_ERROR,
  401: ERROR_TYPES.PERMISSION_ERROR,
  403: ERROR_TYPES.PERMISSION_ERROR,
  404: {
    title: '资源不存在',
    message: '请求的资源不存在',
    suggestions: ['检查请求地址是否正确', '确认资源是否已被删除'],
    icon: 'el-icon-document-delete',
    type: 'error'
  },
  500: ERROR_TYPES.SERVER_ERROR,
  502: ERROR_TYPES.SERVER_ERROR,
  503: ERROR_TYPES.SERVER_ERROR,
  504: ERROR_TYPES.TIMEOUT_ERROR
};

/**
 * 防抖函数
 * @param {Function} func - 要防抖的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @param {boolean} immediate - 是否立即执行
 * @returns {Function} 防抖后的函数
 */
export function debounce(func, delay = 300, immediate = false) {
  let timeoutId;
  let lastCallTime;
  
  return function debounced(...args) {
    const now = Date.now();
    const callNow = immediate && !timeoutId;
    
    // 清除之前的定时器
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
    
    // 设置新的定时器
    timeoutId = setTimeout(() => {
      timeoutId = null;
      if (!immediate) {
        func.apply(this, args);
      }
    }, delay);
    
    // 立即执行
    if (callNow) {
      func.apply(this, args);
    }
    
    lastCallTime = now;
  };
}

/**
 * 节流函数
 * @param {Function} func - 要节流的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @returns {Function} 节流后的函数
 */
export function throttle(func, delay = 300) {
  let timeoutId;
  let lastExecTime = 0;
  
  return function throttled(...args) {
    const now = Date.now();
    
    if (now - lastExecTime > delay) {
      func.apply(this, args);
      lastExecTime = now;
    } else {
      clearTimeout(timeoutId);
      timeoutId = setTimeout(() => {
        func.apply(this, args);
        lastExecTime = Date.now();
      }, delay - (now - lastExecTime));
    }
  };
}

/**
 * 解析错误信息
 * @param {Error|Object} error - 错误对象
 * @returns {Object} 解析后的错误信息
 */
export function parseError(error) {
  // 网络错误
  if (!error.response) {
    return {
      type: ERROR_TYPES.NETWORK_ERROR,
      ...ERROR_MESSAGES[ERROR_TYPES.NETWORK_ERROR],
      originalError: error
    };
  }

  const { status, data } = error.response;
  
  // 特定状态码错误
  if (SPECIFIC_ERROR_CODES[status]) {
    const errorConfig = SPECIFIC_ERROR_CODES[status];
    if (typeof errorConfig === 'string') {
      return {
        type: errorConfig,
        ...ERROR_MESSAGES[errorConfig],
        originalError: error
      };
    } else {
      return {
        type: `HTTP_${status}`,
        ...errorConfig,
        originalError: error
      };
    }
  }

  // 服务器返回的错误信息
  if (data && data.msg) {
    return {
      type: ERROR_TYPES.SERVER_ERROR,
      title: '操作失败',
      message: data.msg,
      suggestions: ['请检查输入参数', '稍后重试'],
      icon: 'el-icon-warning',
      type: 'error',
      originalError: error
    };
  }

  // 默认错误
  return {
    type: ERROR_TYPES.UNKNOWN_ERROR,
    ...ERROR_MESSAGES[ERROR_TYPES.UNKNOWN_ERROR],
    originalError: error
  };
}

/**
 * 显示错误信息
 * @param {Error|Object} error - 错误对象
 * @param {Object} options - 显示选项
 */
export function showError(error, options = {}) {
  const errorInfo = parseError(error);
  const {
    showSuggestions = true,
    duration = 0,
    showClose = true
  } = options;

  // 构建错误消息
  let message = `<div style="line-height: 1.6;">
    <div style="font-weight: bold; margin-bottom: 8px;">
      <i class="${errorInfo.icon}" style="margin-right: 6px;"></i>
      ${errorInfo.title}
    </div>
    <div style="margin-bottom: 8px;">${errorInfo.message}</div>`;

  if (showSuggestions && errorInfo.suggestions && errorInfo.suggestions.length > 0) {
    message += `<div style="margin-top: 12px;">
      <div style="font-weight: bold; margin-bottom: 6px;">解决建议：</div>
      <ul style="margin: 0; padding-left: 20px;">`;
    
    errorInfo.suggestions.forEach(suggestion => {
      message += `<li style="margin-bottom: 4px;">${suggestion}</li>`;
    });
    
    message += `</ul></div>`;
  }

  message += `</div>`;

  // 使用Element UI的消息组件
  if (window.Vue && window.Vue.prototype.$message) {
    window.Vue.prototype.$message({
      type: errorInfo.type,
      message,
      dangerouslyUseHTMLString: true,
      duration,
      showClose
    });
  } else {
    console.error('Error:', errorInfo);
  }

  return errorInfo;
}

/**
 * 重试机制
 * @param {Function} func - 要重试的函数
 * @param {number} maxRetries - 最大重试次数
 * @param {number} delay - 重试延迟（毫秒）
 * @returns {Promise} 重试结果
 */
export async function retry(func, maxRetries = 3, delay = 1000) {
  let lastError;
  
  for (let i = 0; i <= maxRetries; i++) {
    try {
      return await func();
    } catch (error) {
      lastError = error;
      
      if (i === maxRetries) {
        throw error;
      }
      
      // 等待后重试
      await new Promise(resolve => setTimeout(resolve, delay * Math.pow(2, i)));
    }
  }
  
  throw lastError;
}

/**
 * 创建带有错误处理的API调用函数
 * @param {Function} apiFunc - API函数
 * @param {Object} options - 选项
 * @returns {Function} 包装后的函数
 */
export function createSafeApiCall(apiFunc, options = {}) {
  const {
    showError: shouldShowError = true,
    maxRetries = 0,
    retryDelay = 1000,
    timeout = 30000
  } = options;

  return async function safeApiCall(...args) {
    try {
      // 添加超时控制
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Request timeout')), timeout);
      });

      const apiPromise = maxRetries > 0 
        ? retry(() => apiFunc(...args), maxRetries, retryDelay)
        : apiFunc(...args);

      return await Promise.race([apiPromise, timeoutPromise]);
      
    } catch (error) {
      if (shouldShowError) {
        showError(error);
      }
      throw error;
    }
  };
}

/**
 * 参数推荐API的防抖包装
 */
export const debouncedParameterRecommendation = debounce(
  async function(apiFunc, algorithm, datasetInfo) {
    try {
      const response = await apiFunc(algorithm, datasetInfo);
      return response;
    } catch (error) {
      showError(error, {
        showSuggestions: true
      });
      throw error;
    }
  },
  500 // 500ms防抖延迟
);

/**
 * 创建加载状态管理器
 * @param {Object} vue - Vue实例
 * @returns {Object} 加载状态管理器
 */
export function createLoadingManager(vue) {
  const loadingStates = new Map();
  
  return {
    start(key, message = '加载中...') {
      if (loadingStates.has(key)) {
        return;
      }
      
      const loading = vue.$loading({
        lock: true,
        text: message,
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      });
      
      loadingStates.set(key, loading);
      return loading;
    },
    
    stop(key) {
      const loading = loadingStates.get(key);
      if (loading) {
        loading.close();
        loadingStates.delete(key);
      }
    },
    
    stopAll() {
      loadingStates.forEach(loading => loading.close());
      loadingStates.clear();
    }
  };
}
