/**
 * 参数辅助工具函数
 * 提供参数描述、推荐值等辅助功能
 */

// 参数描述信息
const PARAMETER_DESCRIPTIONS = {
  // 通用参数
  n_estimators: {
    label: '估计器数量',
    description: '集成算法中基学习器的数量',
    type: 'number',
    min: 1,
    max: 1000,
    default: 100,
    tips: '更多的估计器通常能提高性能，但会增加计算时间'
  },
  
  max_depth: {
    label: '最大深度',
    description: '树的最大深度',
    type: 'number',
    min: 1,
    max: 50,
    default: null,
    tips: '限制树的深度可以防止过拟合'
  },
  
  min_samples_split: {
    label: '最小分割样本数',
    description: '分割内部节点所需的最小样本数',
    type: 'number',
    min: 2,
    max: 100,
    default: 2,
    tips: '增加此值可以减少过拟合'
  },
  
  min_samples_leaf: {
    label: '叶节点最小样本数',
    description: '叶节点所需的最小样本数',
    type: 'number',
    min: 1,
    max: 50,
    default: 1,
    tips: '增加此值可以使模型更加平滑'
  },
  
  max_features: {
    label: '最大特征数',
    description: '寻找最佳分割时考虑的特征数量',
    type: 'select',
    options: [
      { value: 'auto', label: '自动' },
      { value: 'sqrt', label: '平方根' },
      { value: 'log2', label: '对数' }
    ],
    default: 'auto',
    tips: '减少特征数可以增加随机性，减少过拟合'
  },
  
  C: {
    label: '正则化参数',
    description: 'SVM的正则化参数',
    type: 'number',
    min: 0.001,
    max: 1000,
    default: 1.0,
    tips: '较小的C值会产生更平滑的决策边界'
  },
  
  kernel: {
    label: '核函数',
    description: 'SVM使用的核函数类型',
    type: 'select',
    options: [
      { value: 'linear', label: '线性' },
      { value: 'poly', label: '多项式' },
      { value: 'rbf', label: 'RBF' },
      { value: 'sigmoid', label: 'Sigmoid' }
    ],
    default: 'rbf',
    tips: 'RBF核适用于大多数情况'
  },
  
  gamma: {
    label: 'Gamma参数',
    description: 'RBF、多项式和sigmoid核的核系数',
    type: 'select',
    options: [
      { value: 'scale', label: '自动缩放' },
      { value: 'auto', label: '自动' }
    ],
    default: 'scale',
    tips: '控制单个训练样本的影响范围'
  },
  
  fit_intercept: {
    label: '拟合截距',
    description: '是否计算截距项',
    type: 'boolean',
    default: true,
    tips: '大多数情况下应该保持启用'
  },
  
  normalize: {
    label: '标准化',
    description: '是否在拟合前标准化特征',
    type: 'boolean',
    default: false,
    tips: '当特征量级差异很大时建议启用'
  },
  
  random_state: {
    label: '随机种子',
    description: '随机数生成器的种子',
    type: 'number',
    min: 0,
    max: 9999,
    default: 42,
    tips: '设置固定值可以确保结果可重现'
  }
}

/**
 * 获取参数描述信息
 * @param {string} paramKey - 参数键名
 * @returns {Object} 参数描述对象
 */
export function getParameterDescription(paramKey) {
  return PARAMETER_DESCRIPTIONS[paramKey] || {
    label: paramKey,
    description: '参数描述',
    type: 'text',
    tips: '请参考算法文档'
  }
}

/**
 * 获取推荐参数配置
 * @param {string} algorithm - 算法名称
 * @param {string} dataSize - 数据规模 ('small', 'medium', 'large')
 * @returns {Object} 推荐参数配置
 */
export function getRecommendedParameters(algorithm, dataSize = 'medium') {
  const recommendations = {
    linear_regression: {
      small: { fit_intercept: true, normalize: false },
      medium: { fit_intercept: true, normalize: false, copy_X: true },
      large: { fit_intercept: true, normalize: true, copy_X: true }
    },
    
    random_forest: {
      small: { n_estimators: 50, max_depth: 5 },
      medium: { n_estimators: 100, max_depth: null, min_samples_split: 2 },
      large: { n_estimators: 200, max_depth: 20, min_samples_split: 5, min_samples_leaf: 2 }
    },
    
    svm: {
      small: { C: 1.0, kernel: 'rbf', gamma: 'scale' },
      medium: { C: 1.0, kernel: 'rbf', gamma: 'scale' },
      large: { C: 0.1, kernel: 'rbf', gamma: 'scale' }
    }
  }
  
  return recommendations[algorithm] && recommendations[algorithm][dataSize] || {}
}

/**
 * 获取参数的建议值范围
 * @param {string} paramKey - 参数键名
 * @returns {Object} 建议值范围
 */
export function getParameterRange(paramKey) {
  const description = getParameterDescription(paramKey)
  return {
    min: description.min,
    max: description.max,
    default: description.default,
    type: description.type
  }
}

/**
 * 验证参数值是否在合理范围内
 * @param {string} paramKey - 参数键名
 * @param {any} value - 参数值
 * @returns {Object} 验证结果
 */
export function validateParameterValue(paramKey, value) {
  const description = getParameterDescription(paramKey)
  const result = {
    valid: true,
    message: '',
    suggestion: ''
  }
  
  if (description.type === 'number') {
    const numValue = Number(value)
    if (isNaN(numValue)) {
      result.valid = false
      result.message = '必须是数字'
      return result
    }
    
    if (description.min !== undefined && numValue < description.min) {
      result.valid = false
      result.message = `值不能小于 ${description.min}`
      result.suggestion = `建议值: ${description.default || description.min}`
    }
    
    if (description.max !== undefined && numValue > description.max) {
      result.valid = false
      result.message = `值不能大于 ${description.max}`
      result.suggestion = `建议值: ${description.default || description.max}`
    }
  }
  
  return result
}

/**
 * 格式化参数值用于显示
 * @param {string} paramKey - 参数键名
 * @param {any} value - 参数值
 * @returns {string} 格式化后的值
 */
export function formatParameterValue(paramKey, value) {
  const description = getParameterDescription(paramKey)
  
  if (description.type === 'boolean') {
    return value ? '是' : '否'
  }
  
  if (description.type === 'number') {
    return Number(value).toString()
  }
  
  if (description.type === 'select' && description.options) {
    const option = description.options.find(opt => opt.value === value)
    return option ? option.label : value
  }
  
  return String(value)
}

/**
 * 获取参数的默认值
 * @param {string} paramKey - 参数键名
 * @returns {any} 默认值
 */
export function getParameterDefault(paramKey) {
  const description = getParameterDescription(paramKey)
  return description.default
}

/**
 * 检查参数是否为必需参数
 * @param {string} paramKey - 参数键名
 * @returns {boolean} 是否必需
 */
export function isRequiredParameter(paramKey) {
  // 定义必需参数列表
  const requiredParams = ['n_estimators', 'C', 'kernel']
  return requiredParams.includes(paramKey)
}
