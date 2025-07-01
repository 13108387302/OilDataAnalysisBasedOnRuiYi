/**
 * 回归预测工具函数
 */

/**
 * 验证模型与特征的兼容性
 * @param {Object} model - 预训练模型信息
 * @param {Array} selectedFeatures - 用户选择的特征列表
 * @param {Array} dataColumns - 数据文件的列信息
 * @returns {Object} 验证结果
 */
export function validateModelFeatureCompatibility(model, selectedFeatures, dataColumns) {
  const result = {
    isCompatible: false,
    errors: [],
    warnings: [],
    suggestions: []
  }

  // 检查模型是否存在
  if (!model || !model.expectedFeatures) {
    // 如果模型没有期望特征定义，认为是通用模型，兼容所有特征
    result.isCompatible = true
    result.warnings.push('模型未定义期望特征，将使用您选择的特征进行预测')
    return result
  }

  const expectedFeatures = model.expectedFeatures
  const expectedFeatureCount = expectedFeatures.length
  const selectedFeatureCount = selectedFeatures.length

  // 1. 灵活的特征数量检查
  if (selectedFeatureCount === 0) {
    result.errors.push('请至少选择一个特征')
    return result
  }

  // 允许特征数量不完全匹配，但给出建议
  if (selectedFeatureCount !== expectedFeatureCount) {
    result.warnings.push(
      `特征数量建议：模型期望 ${expectedFeatureCount} 个特征，您选择了 ${selectedFeatureCount} 个特征`
    )
    result.suggestions.push(`建议选择 ${expectedFeatureCount} 个特征以获得最佳效果`)
  }

  // 2. 灵活的特征名称匹配
  const matchedFeatures = []
  const fuzzyMatchedFeatures = []
  const missingFeatures = []

  expectedFeatures.forEach(expectedFeature => {
    const expectedName = expectedFeature.name

    // 精确匹配
    if (selectedFeatures.includes(expectedName)) {
      matchedFeatures.push(expectedName)
      return
    }

    // 模糊匹配（大小写不敏感）
    const fuzzyMatch = selectedFeatures.find(sf =>
      sf.toLowerCase() === expectedName.toLowerCase()
    )
    if (fuzzyMatch) {
      fuzzyMatchedFeatures.push(`${fuzzyMatch} → ${expectedName}`)
      return
    }

    // 部分匹配（包含关系）
    const partialMatch = selectedFeatures.find(sf =>
      sf.toLowerCase().includes(expectedName.toLowerCase()) ||
      expectedName.toLowerCase().includes(sf.toLowerCase())
    )
    if (partialMatch) {
      fuzzyMatchedFeatures.push(`${partialMatch} ≈ ${expectedName}`)
      return
    }

    missingFeatures.push(expectedName)
  })

  // 计算匹配度
  const totalExpected = expectedFeatures.length
  const totalMatched = matchedFeatures.length + fuzzyMatchedFeatures.length
  const matchRatio = totalMatched / totalExpected

  if (fuzzyMatchedFeatures.length > 0) {
    result.warnings.push(`模糊匹配的特征：${fuzzyMatchedFeatures.join(', ')}`)
  }

  if (missingFeatures.length > 0) {
    result.warnings.push(`未匹配的期望特征：${missingFeatures.join(', ')}`)
    result.suggestions.push(`如果有相似特征，可以尝试使用`)
  }

  // 如果匹配度超过50%，认为兼容
  if (matchRatio >= 0.5) {
    result.isCompatible = true
    if (matchRatio < 1.0) {
      result.warnings.push(`特征匹配度：${(matchRatio * 100).toFixed(0)}%`)
    }
  } else {
    result.errors.push(`特征匹配度过低：${(matchRatio * 100).toFixed(0)}%，建议至少匹配50%的期望特征`)
  }

  // 3. 宽松的特征类型检查
  const typeWarnings = []
  expectedFeatures.forEach(expectedFeature => {
    const dataColumn = dataColumns.find(col => col.name === expectedFeature.name)
    if (dataColumn) {
      const expectedType = expectedFeature.type
      const actualType = dataColumn.type

      // 只对明显不兼容的类型组合给出警告
      const isIncompatible = (
        (expectedType === 'string' && actualType === 'numeric') ||
        (expectedType === 'numeric' && actualType === 'string')
      )

      if (isIncompatible) {
        typeWarnings.push(
          `特征 "${expectedFeature.name}" 类型可能不匹配：期望 ${expectedType}，实际 ${actualType}`
        )
      }
    }
  })

  if (typeWarnings.length > 0) {
    result.warnings.push(...typeWarnings)
    result.suggestions.push('类型不匹配可能影响预测效果，但仍可尝试预测')
  }

  // 4. 检查特征值范围（如果模型定义了范围）
  const rangeWarnings = []
  expectedFeatures.forEach(expectedFeature => {
    if (expectedFeature.range) {
      const dataColumn = dataColumns.find(col => col.name === expectedFeature.name)
      if (dataColumn && dataColumn.statistics) {
        const { min, max } = dataColumn.statistics
        const { min: expectedMin, max: expectedMax } = expectedFeature.range
        
        if (min < expectedMin || max > expectedMax) {
          rangeWarnings.push(
            `特征 "${expectedFeature.name}" 值范围超出模型训练范围：` +
            `数据范围 [${min.toFixed(2)}, ${max.toFixed(2)}]，` +
            `模型范围 [${expectedMin.toFixed(2)}, ${expectedMax.toFixed(2)}]`
          )
        }
      }
    }
  })

  if (rangeWarnings.length > 0) {
    result.warnings.push(...rangeWarnings)
    result.suggestions.push('数据范围超出模型训练范围可能影响预测准确性')
  }

  // 设置兼容性状态 - 只要没有严重错误就认为兼容
  if (!result.isCompatible) {
    result.isCompatible = result.errors.length === 0
  }

  return result
}

/**
 * 获取与选择特征兼容的模型列表
 * @param {Array} availableModels - 可用模型列表
 * @param {Array} selectedFeatures - 用户选择的特征
 * @param {Array} dataColumns - 数据列信息
 * @returns {Array} 兼容的模型列表
 */
export function getCompatibleModels(availableModels, selectedFeatures, dataColumns) {
  return availableModels.map(model => {
    const compatibility = validateModelFeatureCompatibility(model, selectedFeatures, dataColumns)
    return {
      ...model,
      compatibility,
      isCompatible: compatibility.isCompatible
    }
  }).sort((a, b) => {
    // 兼容的模型排在前面
    if (a.isCompatible && !b.isCompatible) return -1
    if (!a.isCompatible && b.isCompatible) return 1
    return 0
  })
}

/**
 * 生成预测行数序列
 * @param {number} totalRows - 数据总行数
 * @param {number} sampleCount - 要预测的样本数量
 * @param {string} strategy - 采样策略：'sequential', 'custom'
 * @param {Array} customIndices - 自定义行索引（当strategy为'custom'时使用）
 * @returns {Array} 行索引数组
 */
export function generatePredictionIndices(totalRows, sampleCount, strategy = 'sequential', customIndices = []) {
  switch (strategy) {
    case 'sequential':
      // 顺序选择前N行
      return Array.from({ length: Math.min(sampleCount, totalRows) }, (_, i) => i)

    case 'custom':
      // 使用自定义索引
      return customIndices.filter(index => index >= 0 && index < totalRows)

    default:
      console.warn(`未知的采样策略: ${strategy}，使用默认的顺序策略`)
      return Array.from({ length: Math.min(sampleCount, totalRows) }, (_, i) => i)
  }
}

/**
 * 构建预测请求数据
 * @param {Object} params - 预测参数
 * @returns {Object} 预测请求数据
 */
export function buildRegressionPredictionRequest(params) {
  const {
    dataFile,
    selectedFeatures,
    targetColumn,
    modelSelection,
    predictionIndices,
    predictionParams
  } = params

  // 🔧 修复：提取文件信息而不是传递File对象
  let dataFileInfo = null
  if (dataFile) {
    dataFileInfo = {
      name: dataFile.name,
      size: dataFile.size,
      type: dataFile.type,
      lastModified: dataFile.lastModified,
      // 🔧 关键修复：标记这是真实数据文件
      _isRealDataFile: true,
      _uploadTimestamp: Date.now()
    }

    // 🔧 新增：如果有服务器文件路径，添加到文件信息中
    if (params.dataFilePath) {
      dataFileInfo.serverPath = params.dataFilePath
      console.log('🎯 添加服务器文件路径:', params.dataFilePath)
    }

    console.log('🔧 构建数据文件信息:', dataFileInfo)
  }

  const requestData = {
    // 🔧 修复：传递文件信息而不是File对象
    dataFile: dataFileInfo,
    features: selectedFeatures,
    target: targetColumn,
    taskType: 'regression',
    modelSelection,
    predictionIndices,
    predictionName: predictionParams.taskName, // 添加任务名称字段
    parameters: {
      ...predictionParams,
      outputFormat: 'detailed', // 详细输出格式
      includeInputFeatures: true, // 包含输入特征
      // 🔧 修复：不要覆盖用户的置信度配置
      includeConfidence: predictionParams.includeConfidence !== undefined ? predictionParams.includeConfidence : true,
      precision: predictionParams.outputPrecision || 4,
      // 🔧 修复：在parameters中也添加数据文件信息
      dataFile: dataFileInfo
    }
  }

  console.log('🔧 构建的预测请求数据:', {
    hasDataFile: !!dataFileInfo,
    dataFileName: dataFileInfo?.name,
    featuresCount: selectedFeatures?.length,
    predictionIndicesCount: predictionIndices?.length
  })

  return requestData
}

/**
 * 格式化回归预测结果
 * @param {Object} rawResult - 原始预测结果
 * @param {Array} selectedFeatures - 选择的特征
 * @param {string} targetColumn - 目标列名
 * @returns {Object} 格式化后的结果
 */
export function formatRegressionResult(rawResult, selectedFeatures, targetColumn) {
  console.log('🔄 开始格式化回归结果:', rawResult)

  // 灵活提取预测数据
  let predictions = null
  let input_data = null
  let confidences = null
  let statistics = null
  let prediction_indices = null

  if (rawResult.prediction_result) {
    // 标准格式：有 prediction_result 字段
    const predResult = rawResult.prediction_result
    predictions = predResult.predictions
    input_data = predResult.input_data
    confidences = predResult.confidences
    statistics = predResult.statistics
    prediction_indices = predResult.prediction_indices
    console.log('✅ 使用标准格式 prediction_result')
  } else if (rawResult.predictions) {
    // 简化格式：直接有 predictions 字段
    predictions = rawResult.predictions
    input_data = rawResult.input_data
    confidences = rawResult.confidences
    statistics = rawResult.statistics
    prediction_indices = rawResult.prediction_indices
    console.log('✅ 使用简化格式 predictions')
  } else if (Array.isArray(rawResult)) {
    // 最简格式：直接是预测数组
    predictions = rawResult
    console.log('✅ 使用数组格式')
  } else {
    // 尝试解构原始格式
    ({
      predictions,
      input_data,
      confidences,
      statistics,
      prediction_indices
    } = rawResult)
    console.log('✅ 使用解构格式')
  }

  if (!predictions || !Array.isArray(predictions)) {
    throw new Error('无法从响应中提取有效的预测数组')
  }

  console.log('📊 提取的数据:', {
    predictionsCount: predictions.length,
    hasInputData: !!input_data,
    hasConfidences: !!confidences,
    hasStatistics: !!statistics,
    hasPredictionIndices: !!prediction_indices
  })

  // 构建结果表格数据
  const resultTable = predictions.map((prediction, index) => {
    const row = {
      '行索引': prediction_indices ? prediction_indices[index] : index,
      [`预测_${targetColumn}`]: typeof prediction === 'number' ? prediction.toFixed(4) : prediction,
      '置信度': confidences[index] ? (confidences[index] * 100).toFixed(2) + '%' : 'N/A'
    }

    // 添加输入特征
    if (input_data && input_data[index]) {
      selectedFeatures.forEach(feature => {
        if (input_data[index][feature] !== undefined) {
          row[feature] = input_data[index][feature]
        }
      })
    }

    return row
  })

  // 构建统计摘要
  const summary = {
    totalPredictions: predictions.length,
    targetColumn,
    selectedFeatures: selectedFeatures.length,
    statistics: {
      mean: statistics.mean_prediction || 0,
      min: statistics.min_prediction || 0,
      max: statistics.max_prediction || 0,
      std: statistics.std_prediction || 0,
      median: statistics.median_prediction || 0
    },
    confidence: {
      mean: statistics.mean_confidence || 0,
      min: statistics.min_confidence || 0,
      max: statistics.max_confidence || 0
    }
  }

  return {
    resultTable,
    summary,
    rawResult
  }
}

/**
 * 导出预测结果为CSV格式
 * @param {Array} resultTable - 结果表格数据
 * @param {string} filename - 文件名
 */
export function exportResultToCSV(resultTable, filename = 'regression_prediction_result.csv') {
  if (!resultTable || resultTable.length === 0) {
    throw new Error('没有可导出的数据')
  }

  // 获取列名
  const headers = Object.keys(resultTable[0])
  
  // 构建CSV内容
  const csvContent = [
    headers.join(','), // 表头
    ...resultTable.map(row => 
      headers.map(header => {
        const value = row[header]
        // 处理包含逗号的值
        return typeof value === 'string' && value.includes(',') ? `"${value}"` : value
      }).join(',')
    )
  ].join('\n')

  // 创建下载链接
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  
  link.setAttribute('href', url)
  link.setAttribute('download', filename)
  link.style.visibility = 'hidden'
  
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

/**
 * 验证预测参数
 * @param {Object} params - 预测参数
 * @returns {Object} 验证结果
 */
export function validatePredictionParams(params) {
  const errors = []
  const warnings = []

  // 检查必需参数
  if (!params.dataFile) {
    errors.push('请上传数据文件')
  }

  if (!params.selectedFeatures || params.selectedFeatures.length === 0) {
    errors.push('请选择至少一个输入特征')
  }

  if (!params.targetColumn) {
    errors.push('请选择预测目标列')
  }

  if (!params.modelSelection || !params.modelSelection.isValid) {
    errors.push('请选择有效的预测模型')
  }

  // 检查任务名称
  if (!params.taskName || params.taskName.trim() === '') {
    errors.push('预测任务名称不能为空')
  } else if (params.taskName.trim().length < 2) {
    errors.push('预测任务名称至少需要2个字符')
  }

  // 检查预测数量
  if (params.sampleCount && params.sampleCount > 10000) {
    warnings.push('预测样本数量较大，可能需要较长时间')
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings
  }
}
