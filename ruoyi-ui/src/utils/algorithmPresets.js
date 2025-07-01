/**
 * 算法参数预设配置
 * 为不同算法提供简化的参数选择预设
 */

// 预设类型定义
export const PRESET_TYPES = {
  FAST: 'fast',
  BALANCED: 'balanced', 
  ACCURATE: 'accurate',
  SMALL_DATA: 'small_data',
  LARGE_DATA: 'large_data',
  CUSTOM: 'custom'
}

// 算法参数预设配置
export const ALGORITHM_PRESETS = {
  // 回归算法预设
  'regression_linear_train': [
    {
      key: 'default',
      label: '默认配置',
      type: 'primary',
      level: 'simple',
      description: '线性回归默认配置，适合大多数场景',
      params: {}
    }
  ],

  'regression_polynomial_train': [
    {
      key: 'low_degree',
      label: '低次多项式',
      type: 'success',
      level: 'simple',
      description: '2次多项式，避免过拟合',
      params: { degree: 2 }
    },
    {
      key: 'medium_degree',
      label: '中等多项式',
      type: 'primary',
      level: 'simple',
      description: '3-4次多项式，平衡拟合效果',
      params: { degree: 3 }
    },
    {
      key: 'high_degree',
      label: '高次多项式',
      type: 'warning',
      level: 'simple',
      description: '5次以上多项式，拟合能力强但易过拟合',
      params: { degree: 5 }
    }
  ],

  'regression_svm_train': [
    {
      key: 'linear_fast',
      label: '线性快速',
      type: 'success',
      level: 'simple',
      description: '线性核函数，训练速度快',
      params: { kernel: 'linear', C: 1, gamma: 'scale' }
    },
    {
      key: 'rbf_balanced',
      label: 'RBF平衡',
      type: 'primary',
      level: 'simple',
      description: 'RBF核函数，适合非线性问题',
      params: { kernel: 'rbf', C: 1, gamma: 'scale' }
    },
    {
      key: 'rbf_complex',
      label: 'RBF复杂',
      type: 'warning',
      level: 'simple',
      description: '高复杂度RBF，适合复杂非线性关系',
      params: { kernel: 'rbf', C: 10, gamma: 0.1 }
    }
  ],

  'regression_random_forest_train': [
    {
      key: 'fast',
      label: '快速训练',
      type: 'success',
      level: 'simple',
      description: '少量树，快速训练，适合数据探索',
      params: { 
        n_estimators: 50, 
        max_depth: 5, 
        min_samples_split: 10,
        random_state: 42
      }
    },
    {
      key: 'balanced',
      label: '平衡配置',
      type: 'primary',
      level: 'simple',
      description: '平衡速度和精度，推荐配置',
      params: { 
        n_estimators: 100, 
        max_depth: 10, 
        min_samples_split: 5,
        random_state: 42
      }
    },
    {
      key: 'accurate',
      label: '高精度',
      type: 'warning',
      level: 'simple',
      description: '大量树，追求最高精度，训练时间较长',
      params: { 
        n_estimators: 200, 
        max_depth: 15, 
        min_samples_split: 2,
        random_state: 42
      }
    }
  ],

  'regression_lightgbm_train': [
    {
      key: 'fast',
      label: '快速训练',
      type: 'success',
      level: 'simple',
      description: '快速训练配置，适合大数据集',
      params: { 
        n_estimators: 50, 
        learning_rate: 0.1, 
        max_depth: 5,
        num_leaves: 31
      }
    },
    {
      key: 'balanced',
      label: '平衡配置',
      type: 'primary',
      level: 'simple',
      description: '平衡速度和精度',
      params: { 
        n_estimators: 100, 
        learning_rate: 0.1, 
        max_depth: 7,
        num_leaves: 50
      }
    },
    {
      key: 'accurate',
      label: '高精度',
      type: 'warning',
      level: 'simple',
      description: '高精度配置，训练时间较长',
      params: { 
        n_estimators: 200, 
        learning_rate: 0.05, 
        max_depth: 10,
        num_leaves: 100
      }
    }
  ],

  'regression_xgboost_train': [
    {
      key: 'fast',
      label: '快速训练',
      type: 'success',
      level: 'simple',
      description: '快速训练配置',
      params: { 
        n_estimators: 50, 
        learning_rate: 0.1, 
        max_depth: 5,
        subsample: 0.8,
        colsample_bytree: 0.8
      }
    },
    {
      key: 'balanced',
      label: '平衡配置',
      type: 'primary',
      level: 'simple',
      description: '推荐的平衡配置',
      params: { 
        n_estimators: 100, 
        learning_rate: 0.1, 
        max_depth: 6,
        subsample: 0.9,
        colsample_bytree: 0.9
      }
    },
    {
      key: 'accurate',
      label: '高精度',
      type: 'warning',
      level: 'simple',
      description: '高精度配置，训练时间较长',
      params: { 
        n_estimators: 200, 
        learning_rate: 0.05, 
        max_depth: 8,
        subsample: 1.0,
        colsample_bytree: 1.0
      }
    }
  ],

  // 分类算法预设
  'classification_logistic_train': [
    {
      key: 'default',
      label: '默认配置',
      type: 'primary',
      level: 'simple',
      description: '逻辑回归默认配置',
      params: { C: 1.0, max_iter: 1000 }
    },
    {
      key: 'regularized',
      label: '正则化',
      type: 'success',
      level: 'simple',
      description: '强正则化，防止过拟合',
      params: { C: 0.1, max_iter: 1000 }
    }
  ],

  'classification_svm_train': [
    {
      key: 'linear_fast',
      label: '线性快速',
      type: 'success',
      level: 'simple',
      description: '线性SVM，适合高维数据',
      params: { kernel: 'linear', C: 1, gamma: 'scale' }
    },
    {
      key: 'rbf_balanced',
      label: 'RBF平衡',
      type: 'primary',
      level: 'simple',
      description: 'RBF核，适合非线性分类',
      params: { kernel: 'rbf', C: 1, gamma: 'scale' }
    }
  ],

  'classification_knn_train': [
    {
      key: 'small_k',
      label: '小K值',
      type: 'warning',
      level: 'simple',
      description: 'K=3，对噪声敏感但边界精细',
      params: { n_neighbors: 3, weights: 'uniform' }
    },
    {
      key: 'medium_k',
      label: '中等K值',
      type: 'primary',
      level: 'simple',
      description: 'K=5，平衡配置',
      params: { n_neighbors: 5, weights: 'distance' }
    },
    {
      key: 'large_k',
      label: '大K值',
      type: 'success',
      level: 'simple',
      description: 'K=10，更平滑的决策边界',
      params: { n_neighbors: 10, weights: 'distance' }
    }
  ],

  // 聚类算法预设
  'clustering_kmeans_train': [
    {
      key: 'few_clusters',
      label: '少量聚类',
      type: 'success',
      level: 'simple',
      description: '2-3个聚类中心',
      params: { n_clusters: 3, init: 'k-means++', max_iter: 300 }
    },
    {
      key: 'medium_clusters',
      label: '中等聚类',
      type: 'primary',
      level: 'simple',
      description: '5-6个聚类中心',
      params: { n_clusters: 5, init: 'k-means++', max_iter: 300 }
    },
    {
      key: 'many_clusters',
      label: '多聚类',
      type: 'warning',
      level: 'simple',
      description: '8-10个聚类中心',
      params: { n_clusters: 8, init: 'k-means++', max_iter: 300 }
    }
  ],

  // 深度学习算法预设
  'regression_bilstm_train': [
    {
      key: 'fast',
      label: '快速训练',
      type: 'success',
      level: 'simple',
      description: '小模型，快速训练',
      params: { 
        sequence_length: 10, 
        hidden_size: 64, 
        num_layers: 1,
        epochs: 20,
        batch_size: 32,
        learning_rate: 0.001,
        dropout: 0.1
      }
    },
    {
      key: 'balanced',
      label: '平衡配置',
      type: 'primary',
      level: 'simple',
      description: '推荐的LSTM配置',
      params: { 
        sequence_length: 15, 
        hidden_size: 128, 
        num_layers: 2,
        epochs: 50,
        batch_size: 32,
        learning_rate: 0.001,
        dropout: 0.2
      }
    },
    {
      key: 'accurate',
      label: '高精度',
      type: 'warning',
      level: 'simple',
      description: '大模型，高精度但训练时间长',
      params: { 
        sequence_length: 20, 
        hidden_size: 256, 
        num_layers: 3,
        epochs: 100,
        batch_size: 16,
        learning_rate: 0.0005,
        dropout: 0.3
      }
    }
  ]
}

/**
 * 获取算法的预设配置
 * @param {string} algorithm - 算法名称
 * @param {string} level - 配置级别 (simple/standard/advanced)
 * @returns {Array} 预设配置列表
 */
export function getAlgorithmPresets(algorithm, level = 'simple') {
  const presets = ALGORITHM_PRESETS[algorithm] || []
  return presets.filter(preset => preset.level === level || preset.level === 'all')
}

/**
 * 根据数据特征推荐预设
 * @param {string} algorithm - 算法名称
 * @param {Object} dataInfo - 数据信息 {sampleCount, featureCount, hasNulls}
 * @returns {string} 推荐的预设key
 */
export function recommendPreset(algorithm, dataInfo = {}) {
  const { sampleCount = 1000, featureCount = 10 } = dataInfo
  
  // 基于数据大小的推荐逻辑
  if (sampleCount < 1000) {
    return 'fast'
  } else if (sampleCount < 10000) {
    return 'balanced'
  } else {
    return 'accurate'
  }
}

/**
 * 获取预设的详细信息
 * @param {string} algorithm - 算法名称
 * @param {string} presetKey - 预设键名
 * @returns {Object|null} 预设详细信息
 */
export function getPresetDetails(algorithm, presetKey) {
  const presets = ALGORITHM_PRESETS[algorithm] || []
  return presets.find(preset => preset.key === presetKey) || null
}
