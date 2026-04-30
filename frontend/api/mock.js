import mock from '@/mock/common'
// 如果有 request 封装，引入它；这里演示上传通常是特殊的 uni.uploadFile
const USE_MOCK = true

export const commonApi = {
  uploadImage(filePath) {
    if (USE_MOCK) return mock.uploadFile()
    
    // 真实上传逻辑
    return new Promise((resolve, reject) => {
      uni.uploadFile({
        url: 'https://api.campuscircle.com/v1/common/upload',
        filePath: filePath,
        name: 'file',
        header: {
          Authorization: 'Bearer ' + uni.getStorageSync('token')
        },
        success: (uploadFileRes) => {
          const res = JSON.parse(uploadFileRes.data)
          if (res.code === 200) resolve(res)
          else reject(res)
        },
        fail: (err) => reject(err)
      })
    })
  }
}