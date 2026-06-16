import request from '@/utils/request'

export const getCourses = async (params = {}) => {
  return await request.get('/courses', params)
}

export const createCourse = async (data) => {
  return await request.post('/courses', data)
}

export const searchCourses = async (keyword = '') => {
  return await request.get('/courses/search', { keyword })
}

export const getHotCourses = async () => {
  return await request.get('/courses/hot')
}

export const getMyCourses = async () => {
  return await request.get('/courses/mine')
}

export const getCourseDetail = async (courseId) => {
  return await request.get(`/courses/${courseId}`)
}

export const joinCourse = async (courseId) => {
  return await request.post(`/courses/${courseId}/join`)
}

export const quitCourse = async (courseId) => {
  return await request.del(`/courses/${courseId}/join`)
}

export const getCourseQuestions = async (courseId, params = {}) => {
  return await request.get(`/courses/${courseId}/questions`, params)
}

export const createCourseQuestion = async (courseId, data) => {
  return await request.post(`/courses/${courseId}/questions`, data)
}

export const getCourseQuestionDetail = async (questionId) => {
  return await request.get(`/course-questions/${questionId}`)
}

export const createCourseQuestionReply = async (questionId, data) => {
  return await request.post(`/course-questions/${questionId}/replies`, data)
}

export const resolveCourseQuestion = async (questionId) => {
  return await request.patch(`/course-questions/${questionId}/resolved`)
}

export const getCourseExperiences = async (courseId, params = {}) => {
  return await request.get(`/courses/${courseId}/experiences`, params)
}

export const createCourseExperience = async (courseId, data) => {
  return await request.post(`/courses/${courseId}/experiences`, data)
}

export const getCourseExperienceDetail = async (experienceId) => {
  return await request.get(`/course-experiences/${experienceId}`)
}

export const getCourseReviews = async (courseId) => {
  return await request.get(`/courses/${courseId}/reviews`)
}

export const createCourseReview = async (courseId, data) => {
  return await request.post(`/courses/${courseId}/reviews`, data)
}

export const getCourseReviewSummary = async (courseId) => {
  return await request.get(`/courses/${courseId}/review-summary`)
}
