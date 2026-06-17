import request from '@/utils/request'

const AI_TIMEOUT = 180000

export const createSession = (data = {}) =>
  request.post('/ai-chat/sessions', data, { timeout: AI_TIMEOUT })

export const getSessions = () =>
  request.get('/ai-chat/sessions', {}, { timeout: AI_TIMEOUT })

export const getSessionDetail = (sessionId) =>
  request.get(`/ai-chat/sessions/${sessionId}`, {}, { timeout: AI_TIMEOUT })

export const deleteSession = (sessionId) =>
  request.del(`/ai-chat/sessions/${sessionId}`, {}, { timeout: AI_TIMEOUT })

export const sendMessage = (sessionId, data) =>
  request.post(`/ai-chat/sessions/${sessionId}/messages`, data, { timeout: AI_TIMEOUT })

export const ask = (data) =>
  request.post('/ai-chat/ask', data, { timeout: AI_TIMEOUT })

export const retrieveDebug = (query) =>
  request.get('/ai-chat/retrieve', { query }, { timeout: AI_TIMEOUT })
