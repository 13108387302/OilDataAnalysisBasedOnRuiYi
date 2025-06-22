import request from '@/utils/request'

// 获取所有数据源（数据集+分析任务）
export function getAllDataSources() {
  return request({
    url: '/petrol/visualization/data-sources',
    method: 'get'
  })
}



// 获取数据源信息
export function getDataSourceInfo(sourceId, sourceType) {
  return request({
    url: `/petrol/visualization/data-source/${sourceId}/${sourceType}`,
    method: 'get'
  })
}

// 获取数据源列信息
export function getDataSourceColumns(sourceId, sourceType) {
  return request({
    url: `/petrol/visualization/columns/${sourceId}/${sourceType}`,
    method: 'get'
  })
}

// 读取数据源数据
export function readDataSourceData(sourceId, sourceType, params = {}) {
  return request({
    url: `/petrol/visualization/data/${sourceId}/${sourceType}`,
    method: 'post',
    data: params,
    headers: {
      'repeatSubmit': false // 跳过重复提交检查
    }
  })
}

// 获取数据源统计信息
export function getDataSourceStatistics(sourceId, sourceType, params = {}) {
  return request({
    url: `/petrol/visualization/statistics/${sourceId}/${sourceType}`,
    method: 'post',
    data: params,
    headers: {
      'repeatSubmit': false // 跳过重复提交检查
    }
  })
}

// 生成可视化
export function generateVisualization(data) {
  return request({
    url: '/petrol/visualization/generate',
    method: 'post',
    data: data
  })
}

// 导出可视化结果
export function exportVisualization(data) {
  return request({
    url: '/petrol/visualization/export',
    method: 'post',
    data: data,
    responseType: 'blob'
  })
}



// ==================== 可视化增强API ====================

// 获取任务的增强可视化数据
export function getTaskVisualizationData(taskId) {
  return request({
    url: `/petrol/visualization/enhancement/task/${taskId}`,
    method: 'get',
    headers: {
      'repeatSubmit': false
    }
  })
}

// 获取算法类型的可视化配置
export function getVisualizationConfig(algorithmType) {
  return request({
    url: `/petrol/visualization/enhancement/config/${algorithmType}`,
    method: 'get',
    headers: {
      'repeatSubmit': false
    }
  })
}

// 生成特定类型的可视化数据
export function generateVisualizationData(taskId, visualizationType, params = {}) {
  return request({
    url: `/petrol/visualization/enhancement/generate/${taskId}/${visualizationType}`,
    method: 'post',
    data: params,
    headers: {
      'repeatSubmit': false
    }
  })
}

// 获取模型性能对比数据
export function getModelPerformanceComparison(taskIds) {
  return request({
    url: `/petrol/visualization/enhancement/compare/performance`,
    method: 'post',
    data: taskIds,
    headers: {
      'repeatSubmit': false
    }
  })
}

// 获取算法结果的统计摘要
export function getResultSummary(taskId) {
  return request({
    url: `/petrol/visualization/enhancement/summary/${taskId}`,
    method: 'get',
    headers: {
      'repeatSubmit': false
    }
  })
}

// 导出可视化数据
export function exportVisualizationData(taskId, format = 'json') {
  return request({
    url: `/petrol/visualization/enhancement/export/${taskId}`,
    method: 'get',
    params: { format },
    headers: {
      'repeatSubmit': false
    }
  })
}

// 获取实时训练进度数据
export function getTrainingProgress(taskId) {
  return request({
    url: `/petrol/visualization/enhancement/training/progress/${taskId}`,
    method: 'get',
    headers: {
      'repeatSubmit': false
    }
  })
}

// 获取特征重要性分析数据
export function getFeatureImportance(taskId) {
  return request({
    url: `/petrol/visualization/enhancement/feature/importance/${taskId}`,
    method: 'get',
    headers: {
      'repeatSubmit': false
    }
  })
}

// 获取预测置信度数据
export function getPredictionConfidence(taskId) {
  return request({
    url: `/petrol/visualization/enhancement/prediction/confidence/${taskId}`,
    method: 'get',
    headers: {
      'repeatSubmit': false
    }
  })
}

// 获取算法性能基准对比
export function getAlgorithmBenchmark(algorithmType, datasetSize) {
  return request({
    url: `/petrol/visualization/enhancement/benchmark/${algorithmType}`,
    method: 'get',
    params: { datasetSize },
    headers: {
      'repeatSubmit': false
    }
  })
}
