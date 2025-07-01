import request from '@/utils/request'

// 查询数据集管理列表
export function listDataset(query) {
  return request({
    url: '/petrol/dataset/list',
    method: 'get',
    params: query
  })
}

// 查询可用的数据集列表
export function listAvailableDatasets() {
  return request({
    url: '/petrol/dataset/available',
    method: 'get'
  })
}

// 根据类别查询数据集列表
export function listDatasetsByCategory(category) {
  return request({
    url: `/petrol/dataset/category/${category}`,
    method: 'get'
  })
}

// 查询数据集管理详细
export function getDataset(id) {
  return request({
    url: '/petrol/dataset/' + id,
    method: 'get'
  })
}

// 获取数据集预览数据
export function getDatasetPreview(id, rows = 10) {
  return request({
    url: `/petrol/dataset/${id}/preview`,
    method: 'get',
    params: { rows }
  })
}

// 获取数据集列信息
export function getDatasetColumns(id) {
  return request({
    url: `/petrol/dataset/${id}/columns`,
    method: 'get'
  })
}

// 检查数据集使用情况
export function checkDatasetUsage(id) {
  return request({
    url: `/petrol/dataset/${id}/usage`,
    method: 'get'
  })
}

// 新增数据集管理
export function addDataset(data) {
  return request({
    url: '/petrol/dataset',
    method: 'post',
    data: data
  })
}

// 修改数据集管理
export function updateDataset(data) {
  return request({
    url: '/petrol/dataset',
    method: 'put',
    data: data
  })
}

// 更新数据集统计信息
export function updateDatasetStats(id) {
  return request({
    url: `/petrol/dataset/${id}/stats`,
    method: 'put'
  })
}

// 删除数据集管理
export function delDataset(id) {
  return request({
    url: '/petrol/dataset/' + id,
    method: 'delete'
  })
}

// 上传数据集文件
export function uploadDataset(formData) {
  return request({
    url: '/petrol/dataset/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 验证数据集文件
export function validateDatasetFile(formData) {
  return request({
    url: '/petrol/dataset/validate',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
