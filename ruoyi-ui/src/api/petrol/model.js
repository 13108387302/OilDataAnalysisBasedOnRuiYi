import request from '@/utils/request'

// 查询石油模型列表
export function listModel(query) {
  return request({
    url: '/petrol/model/list',
    method: 'get',
    params: query
  })
}

// 查询石油模型详细
export function getModel(id) {
  return request({
    url: '/petrol/model/' + id,
    method: 'get'
  })
}

// 新增石油模型
export function addModel(data) {
  return request({
    url: '/petrol/model',
    method: 'post',
    data: data
  })
}

// 修改石油模型
export function updateModel(data) {
  return request({
    url: '/petrol/model',
    method: 'put',
    data: data
  })
}

// 删除石油模型
export function delModel(id) {
  return request({
    url: '/petrol/model/' + id,
    method: 'delete'
  })
}

// 批量删除石油模型
export function delModels(ids) {
  return request({
    url: '/petrol/model/' + ids,
    method: 'delete'
  })
}

// 根据任务ID查询模型
export function getModelsByTaskId(taskId) {
  return request({
    url: '/petrol/model/task/' + taskId,
    method: 'get'
  })
}

// 查询可用的模型列表
export function getAvailableModels(modelType) {
  return request({
    url: '/petrol/model/available',
    method: 'get',
    params: { modelType }
  })
}

// 上传模型文件
export function uploadModel(data) {
  return request({
    url: '/petrol/model/upload',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 训练新模型
export function trainModel(data) {
  return request({
    url: '/petrol/model/train',
    method: 'post',
    data: data
  })
}

// 验证模型文件
export function validateModel(modelPath) {
  return request({
    url: '/petrol/model/validate',
    method: 'post',
    data: { modelPath }
  })
}

// 下载模型文件
export function downloadModel(id) {
  return request({
    url: '/petrol/model/download/' + id,
    method: 'get',
    responseType: 'blob'
  })
}
