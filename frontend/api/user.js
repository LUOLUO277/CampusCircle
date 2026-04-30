import request from '@/utils/request'

export const userApi = {
  login(data) {
    return request.post('/auth/login', data)
  },

  register(data) {
    return request.post('/auth/register', data)
  },

  getUserInfo() {
    return request.get('/users/me')
  },

  getUserProfile(id) {
    return request.get(`/users/${id}`)
  },

  getUserPosts(userId, params = { page: 1, size: 10 }) {
    return request.get(`/users/${userId}/posts`, params)
  },

  toggleFollow(userId, isFollow) {
    return isFollow ? request.post(`/users/${userId}/follow`) : request.del(`/users/${userId}/follow`)
  },

  getFollowers(userId) {
    return request.get(`/users/${userId}/followers`)
  },

  getFollowList(userId) {
    return request.get(`/users/${userId}/following`)
  },

  checkIn() {
    return request.post('/users/me/checkin')
  },

  getCheckInStatus() {
    return request.get('/users/me/checkin/status')
  },

  getMyPosts() {
    return request.get('/users/me/posts')
  },

  getMyErrands(type) {
    return request.get('/users/me/errands', { type })
  },

  updateProfile(data) {
    return request.put('/users/me', data)
  },

  getMyCollections(params = { page: 1, size: 10 }) {
    return request.get('/users/me/collects', params)
  }
}

export const adminApi = {
  getReports(status = 0) {
    return request.get('/admin/reports', { status })
  },

  processReport(id, action, note) {
    return request.post(`/admin/reports/${id}/process`, { action, note })
  }
}
