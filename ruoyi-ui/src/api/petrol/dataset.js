import request from '@/utils/request'

// 查询数据集列表
export function listDatasets(query) {
  return request({
    url: '/petrol/dataset/list',
    method: 'get',
    params: query
  })
}

// 查询数据集详细信息
export function getDataset(datasetId) {
  return request({
    url: '/petrol/dataset/' + datasetId,
    method: 'get'
  })
}

// 获取数据集信息（用于可视化）
export function getDatasetInfo(datasetId) {
  return request({
    url: '/petrol/dataset/' + datasetId + '/info',
    method: 'get'
  })
}

// 上传数据集
export function uploadDataset(data) {
  return request({
    url: '/petrol/dataset/upload',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除数据集
export function deleteDataset(datasetId) {
  return request({
    url: '/petrol/dataset/' + datasetId,
    method: 'delete'
  })
}

// 预览数据集
export function previewDataset(datasetId) {
  return request({
    url: '/petrol/dataset/' + datasetId + '/preview',
    method: 'get'
  })
}

// 下载数据集
export function downloadDataset(datasetId) {
  return request({
    url: '/petrol/dataset/' + datasetId + '/download',
    method: 'get',
    responseType: 'blob'
  })
}

// 生成可视化
export function generateVisualization(params) {
  return request({
    url: '/petrol/dataset/visualize',
    method: 'post',
    data: params
  })
}
