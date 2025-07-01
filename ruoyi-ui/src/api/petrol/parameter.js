import request from '@/utils/request'

// 获取参数推荐
export function getParameterRecommendation(algorithm, datasetInfo) {
  return request({
    url: '/petrol/parameter/recommend',
    method: 'post',
    data: {
      algorithm,
      datasetInfo
    }
  })
}

// 获取参数模板
export function getParameterTemplate(algorithm, templateType) {
  return request({
    url: '/petrol/parameter/template',
    method: 'get',
    params: {
      algorithm,
      templateType
    }
  })
}

// 验证参数配置
export function validateParameters(algorithm, parameters) {
  return request({
    url: '/petrol/parameter/validate',
    method: 'post',
    data: {
      algorithm,
      parameters
    }
  })
}

// 获取算法支持的模板类型
export function getSupportedTemplates(algorithm) {
  return request({
    url: '/petrol/parameter/templates',
    method: 'get',
    params: {
      algorithm
    }
  })
}
