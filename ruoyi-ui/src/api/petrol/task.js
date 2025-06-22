import request from '@/utils/request'

// 新增分析任务 (包含文件上传)
export function addTask(data) {
  return request({
    url: '/petrol/task/submit',
    method: 'post',
    data: data,
    headers: {
      // FormData 会被自动设置正确的 Content-Type，这里无需手动指定
    }
  })
}

// 查询分析任务列表
export function listTask(query) {
  return request({
    url: '/petrol/task/list',
    method: 'get',
    params: query
  })
}

// 查询分析任务详细
export function getTask(taskId) {
  return request({
    url: '/petrol/task/' + taskId,
    method: 'get'
  })
}

// 修改分析任务 (包含重新处理)
export function updateTask(data) {
  return request({
    url: '/petrol/task',
    method: 'put',
    data: data
  })
}

// 删除分析任务
export function delTask(taskId) {
  return request({
    url: '/petrol/task/' + taskId,
    method: 'delete'
  })
}

// 上传文件并分析，返回文件头
export function analyzeFile(file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: '/petrol/task/analyzeFile',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}
