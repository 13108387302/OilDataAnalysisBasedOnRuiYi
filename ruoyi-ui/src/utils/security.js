/**
 * 前端安全工具类
 * 提供输入验证、XSS防护、CSRF防护等安全功能
 */

/**
 * XSS防护工具
 */
export const xssProtection = {
  /**
   * HTML编码
   * @param {string} str 需要编码的字符串
   * @returns {string} 编码后的字符串
   */
  encodeHtml(str) {
    if (typeof str !== 'string') return str;
    
    const htmlEntities = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#x27;',
      '/': '&#x2F;'
    };
    
    return str.replace(/[&<>"'/]/g, char => htmlEntities[char]);
  },

  /**
   * HTML解码
   * @param {string} str 需要解码的字符串
   * @returns {string} 解码后的字符串
   */
  decodeHtml(str) {
    if (typeof str !== 'string') return str;
    
    const htmlEntities = {
      '&amp;': '&',
      '&lt;': '<',
      '&gt;': '>',
      '&quot;': '"',
      '&#x27;': "'",
      '&#x2F;': '/'
    };
    
    return str.replace(/&(amp|lt|gt|quot|#x27|#x2F);/g, (match, entity) => {
      return htmlEntities[`&${entity};`] || match;
    });
  },

  /**
   * 移除HTML标签
   * @param {string} str 包含HTML的字符串
   * @returns {string} 移除HTML标签后的字符串
   */
  stripHtml(str) {
    if (typeof str !== 'string') return str;
    return str.replace(/<[^>]*>/g, '');
  },

  /**
   * 检测是否包含潜在的XSS攻击代码
   * @param {string} str 需要检测的字符串
   * @returns {boolean} 是否包含XSS攻击代码
   */
  containsXss(str) {
    if (typeof str !== 'string') return false;
    
    const xssPatterns = [
      /<script[^>]*>.*?<\/script>/gi,
      /javascript:/gi,
      /vbscript:/gi,
      /on\w+\s*=/gi,
      /<iframe[^>]*>.*?<\/iframe>/gi,
      /<object[^>]*>.*?<\/object>/gi,
      /<embed[^>]*>/gi,
      /<link[^>]*>/gi,
      /<meta[^>]*>/gi
    ];
    
    return xssPatterns.some(pattern => pattern.test(str));
  }
};

/**
 * 输入验证工具
 */
export const inputValidation = {
  /**
   * 验证文件名
   * @param {string} filename 文件名
   * @returns {object} 验证结果
   */
  validateFilename(filename) {
    const result = { valid: true, message: '' };
    
    if (!filename || typeof filename !== 'string') {
      result.valid = false;
      result.message = '文件名不能为空';
      return result;
    }
    
    // 长度检查
    if (filename.length > 255) {
      result.valid = false;
      result.message = '文件名长度不能超过255个字符';
      return result;
    }
    
    // 非法字符检查
    const illegalChars = /[<>:"/\\|?*\x00-\x1f]/;
    if (illegalChars.test(filename)) {
      result.valid = false;
      result.message = '文件名包含非法字符';
      return result;
    }
    
    // 路径遍历检查
    if (filename.includes('..')) {
      result.valid = false;
      result.message = '文件名不能包含路径遍历字符';
      return result;
    }
    
    // 保留名称检查（Windows）
    const reservedNames = /^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\.|$)/i;
    if (reservedNames.test(filename)) {
      result.valid = false;
      result.message = '文件名不能使用系统保留名称';
      return result;
    }
    
    return result;
  },

  /**
   * 验证文件类型
   * @param {File} file 文件对象
   * @param {Array} allowedTypes 允许的文件类型
   * @returns {object} 验证结果
   */
  validateFileType(file, allowedTypes = []) {
    const result = { valid: true, message: '' };
    
    if (!file) {
      result.valid = false;
      result.message = '请选择文件';
      return result;
    }
    
    if (allowedTypes.length > 0) {
      const fileExtension = file.name.toLowerCase().split('.').pop();
      const isAllowed = allowedTypes.some(type => {
        if (type.startsWith('.')) {
          return type.toLowerCase() === `.${fileExtension}`;
        }
        return type.toLowerCase() === fileExtension;
      });
      
      if (!isAllowed) {
        result.valid = false;
        result.message = `只允许上传以下类型的文件: ${allowedTypes.join(', ')}`;
        return result;
      }
    }
    
    return result;
  },

  /**
   * 验证文件大小
   * @param {File} file 文件对象
   * @param {number} maxSize 最大文件大小（字节）
   * @returns {object} 验证结果
   */
  validateFileSize(file, maxSize = 100 * 1024 * 1024) { // 默认100MB
    const result = { valid: true, message: '' };
    
    if (!file) {
      result.valid = false;
      result.message = '请选择文件';
      return result;
    }
    
    if (file.size > maxSize) {
      const maxSizeMB = Math.round(maxSize / (1024 * 1024));
      result.valid = false;
      result.message = `文件大小不能超过 ${maxSizeMB}MB`;
      return result;
    }
    
    if (file.size === 0) {
      result.valid = false;
      result.message = '文件不能为空';
      return result;
    }
    
    return result;
  },

  /**
   * 验证数据集名称
   * @param {string} name 数据集名称
   * @returns {object} 验证结果
   */
  validateDatasetName(name) {
    const result = { valid: true, message: '' };
    
    if (!name || typeof name !== 'string') {
      result.valid = false;
      result.message = '数据集名称不能为空';
      return result;
    }
    
    // 长度检查
    if (name.length < 2 || name.length > 100) {
      result.valid = false;
      result.message = '数据集名称长度应在2-100个字符之间';
      return result;
    }
    
    // XSS检查
    if (xssProtection.containsXss(name)) {
      result.valid = false;
      result.message = '数据集名称包含非法字符';
      return result;
    }
    
    // 字符检查：只允许中文、英文、数字、下划线、连字符、空格
    const validPattern = /^[a-zA-Z0-9_\-\u4e00-\u9fa5\s]+$/;
    if (!validPattern.test(name)) {
      result.valid = false;
      result.message = '数据集名称只能包含中文、英文、数字、下划线、连字符和空格';
      return result;
    }
    
    return result;
  },

  /**
   * 验证邮箱地址
   * @param {string} email 邮箱地址
   * @returns {object} 验证结果
   */
  validateEmail(email) {
    const result = { valid: true, message: '' };
    
    if (!email || typeof email !== 'string') {
      result.valid = false;
      result.message = '邮箱地址不能为空';
      return result;
    }
    
    const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    if (!emailPattern.test(email)) {
      result.valid = false;
      result.message = '请输入有效的邮箱地址';
      return result;
    }
    
    return result;
  },

  /**
   * 验证数值参数
   * @param {number} value 数值
   * @param {number} min 最小值
   * @param {number} max 最大值
   * @returns {object} 验证结果
   */
  validateNumericParameter(value, min = -Infinity, max = Infinity) {
    const result = { valid: true, message: '' };
    
    if (value === null || value === undefined || value === '') {
      result.valid = false;
      result.message = '参数值不能为空';
      return result;
    }
    
    const numValue = Number(value);
    if (isNaN(numValue)) {
      result.valid = false;
      result.message = '参数值必须是有效数字';
      return result;
    }
    
    if (numValue < min || numValue > max) {
      result.valid = false;
      result.message = `参数值应在 ${min} 到 ${max} 之间`;
      return result;
    }
    
    return result;
  }
};

/**
 * CSRF防护工具
 */
export const csrfProtection = {
  /**
   * 生成CSRF令牌
   * @returns {string} CSRF令牌
   */
  generateToken() {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return Array.from(array, byte => byte.toString(16).padStart(2, '0')).join('');
  },

  /**
   * 获取存储的CSRF令牌
   * @returns {string} CSRF令牌
   */
  getToken() {
    return sessionStorage.getItem('csrf_token') || this.generateToken();
  },

  /**
   * 设置CSRF令牌
   * @param {string} token CSRF令牌
   */
  setToken(token) {
    sessionStorage.setItem('csrf_token', token);
  },

  /**
   * 清除CSRF令牌
   */
  clearToken() {
    sessionStorage.removeItem('csrf_token');
  }
};

/**
 * 安全的本地存储工具
 */
export const secureStorage = {
  /**
   * 安全地设置localStorage项
   * @param {string} key 键名
   * @param {any} value 值
   */
  setItem(key, value) {
    try {
      const sanitizedKey = xssProtection.encodeHtml(key);
      const sanitizedValue = typeof value === 'string' 
        ? xssProtection.encodeHtml(value) 
        : JSON.stringify(value);
      localStorage.setItem(sanitizedKey, sanitizedValue);
    } catch (error) {
      console.error('Failed to set localStorage item:', error);
    }
  },

  /**
   * 安全地获取localStorage项
   * @param {string} key 键名
   * @returns {any} 值
   */
  getItem(key) {
    try {
      const sanitizedKey = xssProtection.encodeHtml(key);
      const value = localStorage.getItem(sanitizedKey);
      if (value === null) return null;
      
      try {
        return JSON.parse(value);
      } catch {
        return xssProtection.decodeHtml(value);
      }
    } catch (error) {
      console.error('Failed to get localStorage item:', error);
      return null;
    }
  },

  /**
   * 移除localStorage项
   * @param {string} key 键名
   */
  removeItem(key) {
    try {
      const sanitizedKey = xssProtection.encodeHtml(key);
      localStorage.removeItem(sanitizedKey);
    } catch (error) {
      console.error('Failed to remove localStorage item:', error);
    }
  }
};

/**
 * 内容安全策略工具
 */
export const contentSecurityPolicy = {
  /**
   * 检查是否违反CSP策略
   * @param {string} content 内容
   * @returns {boolean} 是否违反CSP
   */
  violatesPolicy(content) {
    if (typeof content !== 'string') return false;
    
    // 检查内联脚本
    if (content.includes('<script') && !content.includes('nonce=')) {
      return true;
    }
    
    // 检查内联样式
    if (content.includes('style=') && !content.includes('nonce=')) {
      return true;
    }
    
    // 检查事件处理器
    const eventHandlers = /on\w+\s*=/gi;
    if (eventHandlers.test(content)) {
      return true;
    }
    
    return false;
  }
};

export default {
  xssProtection,
  inputValidation,
  csrfProtection,
  secureStorage,
  contentSecurityPolicy
};
