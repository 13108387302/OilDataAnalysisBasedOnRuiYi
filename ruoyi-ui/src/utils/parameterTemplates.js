/**
 * 参数模板工具函数
 * 为分析任务提供预设的参数模板
 */

// 模板类型定义
export const TEMPLATE_TYPES = {
  QUICK: 'quick',
  STANDARD: 'standard',
  ADVANCED: 'advanced',
  CUSTOM: 'custom'
}

// 参数模板定义
export const PARAMETER_TEMPLATES = {
  // 线性回归模板
  linear_regression: {
    [TEMPLATE_TYPES.QUICK]: {
      fit_intercept: true,
      normalize: false
    },
    [TEMPLATE_TYPES.STANDARD]: {
      fit_intercept: true,
      normalize: false,
      copy_X: true
    },
    [TEMPLATE_TYPES.ADVANCED]: {
      fit_intercept: true,
      normalize: false,
      copy_X: true,
      n_jobs: 1
    }
  },
  
  // 随机森林模板
  random_forest: {
    [TEMPLATE_TYPES.QUICK]: {
      n_estimators: 10,
      max_depth: 3
    },
    [TEMPLATE_TYPES.STANDARD]: {
      n_estimators: 100,
      max_depth: null,
      min_samples_split: 2,
      min_samples_leaf: 1
    },
    [TEMPLATE_TYPES.ADVANCED]: {
      n_estimators: 200,
      max_depth: null,
      min_samples_split: 2,
      min_samples_leaf: 1,
      max_features: 'auto',
      bootstrap: true,
      random_state: 42
    }
  },
  
  // SVM模板
  svm: {
    [TEMPLATE_TYPES.QUICK]: {
      C: 1.0,
      kernel: 'rbf'
    },
    [TEMPLATE_TYPES.STANDARD]: {
      C: 1.0,
      kernel: 'rbf',
      gamma: 'scale'
    },
    [TEMPLATE_TYPES.ADVANCED]: {
      C: 1.0,
      kernel: 'rbf',
      gamma: 'scale',
      degree: 3,
      coef0: 0.0
    }
  }
}

/**
 * 获取参数模板
 * @param {string} algorithm - 算法名称
 * @param {string} templateType - 模板类型
 * @returns {Object|null} 参数模板
 */
export function getParameterTemplate(algorithm, templateType) {
  if (!algorithm || !templateType) {
    return null
  }
  
  const algorithmTemplates = PARAMETER_TEMPLATES[algorithm]
  if (!algorithmTemplates) {
    return null
  }
  
  return algorithmTemplates[templateType] || null
}

/**
 * 获取支持的模板类型
 * @param {string} algorithm - 算法名称
 * @returns {Array} 支持的模板类型列表
 */
export function getSupportedTemplateTypes(algorithm) {
  if (!algorithm) {
    return []
  }
  
  const algorithmTemplates = PARAMETER_TEMPLATES[algorithm]
  if (!algorithmTemplates) {
    return []
  }
  
  return Object.keys(algorithmTemplates)
}

/**
 * 检查算法是否有预设模板
 * @param {string} algorithm - 算法名称
 * @returns {boolean} 是否有预设模板
 */
export function hasPresetTemplates(algorithm) {
  return !!(algorithm && PARAMETER_TEMPLATES[algorithm])
}

/**
 * 获取所有可用的算法列表
 * @returns {Array} 算法列表
 */
export function getAvailableAlgorithms() {
  return Object.keys(PARAMETER_TEMPLATES)
}

/**
 * 获取模板类型的显示名称
 * @param {string} templateType - 模板类型
 * @returns {string} 显示名称
 */
export function getTemplateDisplayName(templateType) {
  const displayNames = {
    [TEMPLATE_TYPES.QUICK]: '快速模板',
    [TEMPLATE_TYPES.STANDARD]: '标准模板',
    [TEMPLATE_TYPES.ADVANCED]: '高级模板',
    [TEMPLATE_TYPES.CUSTOM]: '自定义'
  }
  
  return displayNames[templateType] || templateType
}

/**
 * 获取模板类型的描述
 * @param {string} templateType - 模板类型
 * @returns {string} 描述信息
 */
export function getTemplateDescription(templateType) {
  const descriptions = {
    [TEMPLATE_TYPES.QUICK]: '快速开始，使用最基本的参数配置',
    [TEMPLATE_TYPES.STANDARD]: '标准配置，适合大多数使用场景',
    [TEMPLATE_TYPES.ADVANCED]: '高级配置，包含更多可调参数',
    [TEMPLATE_TYPES.CUSTOM]: '自定义配置，完全由用户控制'
  }
  
  return descriptions[templateType] || ''
}
