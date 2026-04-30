import request from '@/utils/request'
import mock from '@/mock/errand'

const USE_MOCK = false

const mockRequest = async (fn, params) => {
  try { return await fn(params) } catch (e) { throw e }
}

export const errandApi = {
  // 获取列表
  getList(params) {
    if (USE_MOCK) return mockRequest(mock.getList, params)
    return request.get('/errands', params)
  },
  
  // 获取详情
  getDetail(id) {
    if (USE_MOCK) return mockRequest(mock.getDetail, id)
    return request.get(`/errands/${id}`)
  },
  
  // 发布
  create(data) {
    if (USE_MOCK) return mockRequest(mock.create, data)
    return request.post('/errands', data)
  },
  
  // 接单
  accept(id) {
    if (USE_MOCK) return mockRequest(mock.accept, id)
    return request.post(`/errands/${id}/accept`)
  },
  
  // 完成
  complete(id) {
    if (USE_MOCK) return mockRequest(mock.complete, id)
    return request.post(`/errands/${id}/complete`)
  },
  
  // 取消
  cancel(id) {
    if (USE_MOCK) return mockRequest(mock.cancel, id)
    return request.post(`/errands/${id}/cancel`)
  }
}