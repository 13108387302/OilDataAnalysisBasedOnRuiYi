/**
 * 参数验证工具函数
 * 提供参数验证、建议等功能
 */

import { getParameterDescription, validateParameterValue } from './parameterHelpers'

// 参数范围定义
export const PARAMETER_RANGES = {
  n_estimators: { min: 1, max: 1000, default: 100 },
  max_depth: { min: 1, max: 50, default: null },
  min_samples_split: { min: 2, max: 100, default: 2 },
  min_samples_leaf: { min: 1, max: 50, default: 1 },
  C: { min: 0.001, max: 1000, default: 1.0 },
  gamma: { values: ['scale', 'auto'], default: 'scale' },
  kernel: { values: ['linear', 'poly', 'rbf', 'sigmoid'], default: 'rbf' },
  random_state: { min: 0, max: 9999, default: 42 }
}

/**
 * 验证单个参数
 * @param {string} paramKey - 参数键名
 * @param {any} value - 参数值
 * @param {string} algorithm - 算法名称
 * @returns {Object} 验证结果
 */
export function validateParameter(paramKey, value, algorithm = '') {
  const result = {
    valid: true,
    level: 'success', // success, warning, error
    message: '',
    suggestion: ''
  }
  
  // 使用参数辅助函数进行基础验证
  const basicValidation = validateParameterValue(paramKey, value)
  if (!basicValidation.valid) {
    result.valid = false
    result.level = 'error'
    result.message = basicValidation.message
    result.suggestion = basicValidation.suggestion
    return result
  }
  
  // 特定参数的高级验证
  switch (paramKey) {
    case 'n_estimators':
      if (value < 10) {
        result.level = 'warning'
        result.message = '估计器数量较少，可能影响模型性能'
        result.suggestion = '建议使用至少50个估计器'
      } else if (value > 500) {
        result.level = 'warning'
        result.message = '估计器数量较多，训练时间可能很长'
        result.suggestion = '对于大多数任务，100-200个估计器已足够'
      }
      break
      
    case 'max_depth':
      if (value && value > 20) {
        result.level = 'warning'
        result.message = '树深度过大可能导致过拟合'
        result.suggestion = '建议将深度限制在10-15之间'
      }
      break
      
    case 'C':
      if (value < 0.01) {
        result.level = 'warning'
        result.message = 'C值过小可能导致欠拟合'
        result.suggestion = '建议使用0.1-10之间的值'
      } else if (value > 100) {
        result.level = 'warning'
        result.message = 'C值过大可能导致过拟合'
        result.suggestion = '建议使用0.1-10之间的值'
      }
      break
  }
  
  return result
}

/**
 * 验证所有参数
 * @param {Object} parameters - 参数对象
 * @param {string} algorithm - 算法名称
 * @returns {Object} 验证结果汇总
 */
export function validateAllParameters(parameters, algorithm = '') {
  const results = {}
  let errorCount = 0
  let warningCount = 0
  
  Object.keys(parameters).forEach(paramKey => {
    const value = parameters[paramKey]
    const validation = validateParameter(paramKey, value, algorithm)
    
    results[paramKey] = validation
    
    if (validation.level === 'error') {
      errorCount++
    } else if (validation.level === 'warning') {
      warningCount++
    }
  })
  
  return {
    results,
    summary: {
      valid: errorCount === 0,
      errorCount,
      warningCount,
      totalParams: Object.keys(parameters).length
    }
  }
}

/**
 * 获取参数建议
 * @param {string} paramKey - 参数键名
 * @param {any} currentValue - 当前值
 * @param {string} algorithm - 算法名称
 * @returns {Array} 建议列表
 */
export function getParameterSuggestions(paramKey, currentValue, algorithm = '') {
  const suggestions = []
  const range = PARAMETER_RANGES[paramKey]
  
  if (!range) {
    return suggestions
  }
  
  // 基于参数类型提供建议
  switch (paramKey) {
    case 'n_estimators':
      suggestions.push(
        { value: 50, label: '快速训练 (50)', description: '适合快速实验' },
        { value: 100, label: '标准配置 (100)', description: '平衡性能和速度' },
        { value: 200, label: '高性能 (200)', description: '更好的性能，较慢的训练' }
      )
      break
      
    case 'max_depth':
      suggestions.push(
        { value: null, label: '无限制', description: '允许树完全生长' },
        { value: 5, label: '浅层 (5)', description: '防止过拟合' },
        { value: 10, label: '中等 (10)', description: '平衡复杂度' },
        { value: 15, label: '深层 (15)', description: '捕获复杂模式' }
      )
      break
      
    case 'C':
      suggestions.push(
        { value: 0.1, label: '低正则化 (0.1)', description: '更平滑的边界' },
        { value: 1.0, label: '标准 (1.0)', description: '默认配置' },
        { value: 10.0, label: '高正则化 (10.0)', description: '更复杂的边界' }
      )
      break
      
    case 'kernel':
      suggestions.push(
        { value: 'linear', label: '线性核', description: '适合线性可分数据' },
        { value: 'rbf', label: 'RBF核', description: '适合大多数情况' },
        { value: 'poly', label: '多项式核', description: '适合多项式关系' }
      )
      break
  }
  
  return suggestions
}

/**
 * 检查参数组合的兼容性
 * @param {Object} parameters - 参数对象
 * @param {string} algorithm - 算法名称
 * @returns {Object} 兼容性检查结果
 */
export function checkParameterCompatibility(parameters, algorithm) {
  const issues = []
  
  // SVM特定的兼容性检查
  if (algorithm === 'svm') {
    if (parameters.kernel === 'linear' && parameters.gamma) {
      issues.push({
        level: 'warning',
        message: '线性核不使用gamma参数',
        suggestion: '移除gamma参数或选择其他核函数'
      })
    }
    
    if (parameters.kernel === 'poly' && !parameters.degree) {
      issues.push({
        level: 'warning',
        message: '多项式核建议设置degree参数',
        suggestion: '添加degree参数，通常设置为2-4'
      })
    }
  }
  
  // 随机森林特定的兼容性检查
  if (algorithm === 'random_forest') {
    if (parameters.min_samples_split && parameters.min_samples_leaf) {
      if (parameters.min_samples_split <= parameters.min_samples_leaf) {
        issues.push({
          level: 'error',
          message: 'min_samples_split必须大于min_samples_leaf',
          suggestion: '调整参数值以满足约束条件'
        })
      }
    }
  }
  
  return {
    compatible: issues.filter(issue => issue.level === 'error').length === 0,
    issues
  }
}

/**
 * 生成参数优化建议
 * @param {Object} parameters - 当前参数
 * @param {string} algorithm - 算法名称
 * @param {Object} context - 上下文信息（数据大小、任务类型等）
 * @returns {Array} 优化建议列表
 */
export function generateOptimizationSuggestions(parameters, algorithm, context = {}) {
  const suggestions = []
  
  // 基于数据大小的建议
  if (context.dataSize === 'large') {
    if (algorithm === 'random_forest' && parameters.n_estimators > 200) {
      suggestions.push({
        type: 'performance',
        message: '对于大数据集，建议减少估计器数量以提高训练速度',
        action: 'reduce_n_estimators',
        suggestedValue: 100
      })
    }
  }
  
  // 基于任务类型的建议
  if (context.taskType === 'classification') {
    if (algorithm === 'svm' && !parameters.probability) {
      suggestions.push({
        type: 'feature',
        message: '分类任务建议启用概率估计',
        action: 'enable_probability',
        suggestedValue: true
      })
    }
  }
  
  return suggestions
}
