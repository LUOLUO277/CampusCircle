import request from '@/utils/request'

export const getInfoSources = (params = {}) => request.get('/info-sources', params)
export const searchInfoSources = (keyword) => request.get('/info-sources/search', { keyword })
export const getInfoSubscriptions = () => request.get('/info-subscriptions')
export const subscribeSource = (sourceId) => request.post(`/info-subscriptions/${sourceId}`)
export const unsubscribeSource = (sourceId) => request.delete(`/info-subscriptions/${sourceId}`)
export const updateSubscriptionKeywords = (sourceId, keywords = []) =>
  request.put(`/info-subscriptions/${sourceId}/keywords`, { keywords })

export const getInfoNotices = (params = {}) => request.get('/info-center/notices', params)
export const getInfoNoticeDetail = (id) => request.get(`/info-center/notices/${id}`)
export const completeInfoNotice = (id) => request.post(`/notices/${id}/complete`)
export const uncompleteInfoNotice = (id) => request.del(`/notices/${id}/complete`)
export const getInfoNoticeStatus = (id) => request.get(`/notices/${id}/status`)
export const extractInfoNoticeDeadline = (id) => request.post(`/notices/${id}/deadline/extract`)
export const getInfoNoticeComments = (id) => request.get(`/info-center/notices/${id}/comments`)
export const createInfoNoticeComment = (id, data) => request.post(`/info-center/notices/${id}/comments`, data)
export const deleteInfoComment = (commentId) => request.delete(`/info-center/comments/${commentId}`)
export const likeInfoComment = (commentId) => request.post(`/info-center/comments/${commentId}/like`)
export const getHomeDeadlineReminders = (days = 7) => request.get('/home/deadline-reminders', { days })

export const getScheduleItems = (params = {}) => request.get('/schedule-items', params)
export const getScheduleItemDetail = (id) => request.get(`/schedule-items/${id}`)
export const createScheduleItem = (data) => request.post('/schedule-items', data)
export const updateScheduleItem = (id, data) => request.put(`/schedule-items/${id}`, data)
export const deleteScheduleItem = (id) => request.del(`/schedule-items/${id}`)
export const completeScheduleItem = (id) => request.post(`/schedule-items/${id}/complete`)
export const uncompleteScheduleItem = (id) => request.del(`/schedule-items/${id}/complete`)

export const createInfoSource = (data) => request.post('/admin/info-sources', data)
export const getAdminInfoSourceDetail = (id) => request.get(`/admin/info-sources/${id}`)
export const updateInfoSource = (id, data) => request.put(`/admin/info-sources/${id}`, data)
export const enableInfoSource = (id) => request.post(`/admin/info-sources/${id}/enable`)
export const disableInfoSource = (id) => request.post(`/admin/info-sources/${id}/disable`)
export const triggerSourceFetch = (id) => request.post(`/admin/info-sources/${id}/fetch`)
export const getSourceFetchLogs = (id) => request.get(`/admin/info-sources/${id}/fetch-logs`)

export const createManualNotice = (data) => request.post('/admin/info-center/notices/manual', data)
export const updateManualNotice = (id, data) => request.put(`/admin/info-center/notices/${id}`, data)
export const offlineManualNotice = (id) => request.post(`/admin/info-center/notices/${id}/offline`)

export const getCanvasBinding = () => request.get('/canvas-binding')
export const saveCanvasBinding = (data) => request.put('/canvas-binding', data)
export const syncCanvasBinding = (params = {}) => {
  const source = params.source || 'all'
  const forceRelogin = params.forceRelogin === true
  const debugRaw = params.debugRaw === true
  return request.post(
    `/canvas-binding/sync?source=${encodeURIComponent(source)}&forceRelogin=${forceRelogin}&debugRaw=${debugRaw}`,
    {},
    { timeout: 180000 }
  )
}
export const browserLoginCanvasBinding = () => request.post('/canvas-binding/browser-login', {}, { timeout: 180000 })
export const disconnectCanvasBinding = () => request.delete('/canvas-binding')
