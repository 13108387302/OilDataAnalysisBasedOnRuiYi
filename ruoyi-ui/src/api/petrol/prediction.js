import request from '@/utils/request'

// 查询石油预测列表
export function listPrediction(query) {
  return request({
    url: '/petrol/prediction/list',
    method: 'get',
    params: query
  })
}

// 查询石油预测详细
export function getPrediction(id) {
  return request({
    url: '/petrol/prediction/' + id,
    method: 'get'
  })
}

// 新增石油预测
export function addPrediction(data) {
  return request({
    url: '/petrol/prediction',
    method: 'post',
    data: data
  })
}

// 修改石油预测
export function updatePrediction(data) {
  return request({
    url: '/petrol/prediction',
    method: 'put',
    data: data
  })
}

// 删除石油预测
export function delPrediction(id) {
  return request({
    url: '/petrol/prediction/' + id,
    method: 'delete'
  })
}

// 根据模型ID查询预测记录
export function getPredictionsByModelId(modelId) {
  return request({
    url: '/petrol/prediction/model/' + modelId,
    method: 'get'
  })
}

// 执行预测
export function executePrediction(data) {
  return request({
    url: '/petrol/prediction/execute',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 批量预测
export function executeBatchPrediction(data) {
  return request({
    url: '/petrol/prediction/batch',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取预测状态
export function getPredictionStatus(predictionId) {
  return request({
    url: '/petrol/prediction/status/' + predictionId,
    method: 'get'
  })
}

// 获取预测结果
export function getPredictionResult(predictionId) {
  return request({
    url: '/petrol/prediction/result/' + predictionId,
    method: 'get'
  })
}

// 获取统计数据
export function getStats() {
  return request({
    url: '/petrol/prediction/stats',
    method: 'get'
  })
}

// 创建增强的预测任务
export function createEnhancedPrediction(data, options = {}) {
  const config = {
    url: '/petrol/prediction/enhanced',
    method: 'post',
    data: data
  }

  // 支持取消信号
  if (options.signal) {
    config.signal = options.signal
  }

  return request(config)
}

// 提交预测任务（简化版）
export function submitPrediction(data) {
  return request({
    url: '/petrol/prediction/submit',
    method: 'post',
    data: data
  })
}
