import request from '@/utils/request'

const AI_TIMEOUT = 180000

export const queryAiNotices = (question) =>
  request.post('/ai/notices/query', { question }, { timeout: AI_TIMEOUT })

export const classifyNotice = (noticeId, force = false) =>
  request.post(`/ai/notices/${noticeId}/classify`, { force }, { timeout: AI_TIMEOUT })

export const classifyNoticeBatch = (params = {}) =>
  request.post(
    `/ai/notices/classify/batch?limit=${params.limit || 20}&onlyUncategorized=${params.onlyUncategorized !== false}`,
    {},
    { timeout: AI_TIMEOUT }
  )

export const searchAiPosts = (question) =>
  request.post('/ai/posts/search', { question }, { timeout: AI_TIMEOUT })
