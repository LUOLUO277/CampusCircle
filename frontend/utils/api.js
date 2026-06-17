const DEFAULT_BACKEND_ORIGIN = 'http://localhost:8080'
const DEV_BACKEND_ORIGIN_KEY = 'devBackendOrigin'

const getH5DeploymentOrigin = () => {
  // #ifdef H5
  if (typeof window !== 'undefined' && window.location && process.env.NODE_ENV !== 'development') {
    const { origin, pathname } = window.location
    const firstSegment = pathname.split('/').filter(Boolean)[0]
    return firstSegment ? `${origin}/${firstSegment}` : origin
  }
  // #endif
  return ''
}

const normalizeOrigin = (origin) => {
  if (!origin || typeof origin !== 'string') {
    return DEFAULT_BACKEND_ORIGIN
  }
  return origin.replace(/\/+$/, '')
}

export const getBackendOrigin = () => {
  const customOrigin = uni.getStorageSync(DEV_BACKEND_ORIGIN_KEY)
  if (customOrigin) {
    return normalizeOrigin(customOrigin)
  }
  const deployedOrigin = getH5DeploymentOrigin()
  if (deployedOrigin) {
    return normalizeOrigin(deployedOrigin)
  }
  return DEFAULT_BACKEND_ORIGIN
}

export const getApiBaseUrl = () => `${getBackendOrigin()}/api`

export const toAbsoluteUrl = (url, fallback = '/static/logo.png') => {
  if (!url) {
    return fallback
  }
  if (/^https?:\/\//i.test(url)) {
    return url
  }
  return `${getBackendOrigin()}${url.startsWith('/') ? url : `/${url}`}`
}

export const setDevBackendOrigin = (origin) => {
  const normalized = normalizeOrigin(origin)
  uni.setStorageSync(DEV_BACKEND_ORIGIN_KEY, normalized)
  return normalized
}

export const clearDevBackendOrigin = () => {
  uni.removeStorageSync(DEV_BACKEND_ORIGIN_KEY)
}
