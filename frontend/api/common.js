import mock from '@/mock/common'  
import request from '../utils/request.js'  
import { getApiBaseUrl } from '@/utils/api'
  
const USE_MOCK = false  
  
export async function uploadBatchImages(files) {  
  return new Promise((resolve, reject) => {  
    const token = uni.getStorageSync('token')  
      
    // 确保files是数组  
    const fileArray = Array.isArray(files) ? files : [files]  
      
    // 使用正确的参数名匹配后端 @RequestParam("files[]")  
    uni.uploadFile({  
      url: getApiBaseUrl() + '/common/upload/batch',  
      filePath: fileArray[0],  // 单文件上传  
      name: 'files[]',  // 匹配后端参数名  
      formData: {  
        scene: 'post_image'  
      },  
      header: {  
        Authorization: `Bearer ${token}`  
      },  
      success: (uploadFileRes) => {  
        try {  
          const res = JSON.parse(uploadFileRes.data)  
          if (res.code === 200) {  
            resolve(res)  
          } else {  
            reject(res)  
          }  
        } catch (e) {  
          reject(new Error('响应格式错误'))  
        }  
      },  
      fail: (err) => {  
        console.error('【上传失败】', err)  
        reject(err)  
      }  
    })  
  })  
}
  
export const commonApi = {  
  uploadImage(filePath) {  
    if (USE_MOCK) return mock.uploadFile()  
      
    return new Promise((resolve, reject) => {  
      const token = uni.getStorageSync('token')  
        
      uni.uploadFile({  
        url: getApiBaseUrl() + '/common/upload',  
        filePath: filePath,  
        name: 'file',  
        header: {  
          Authorization: `Bearer ${token}`  
        },  
        success: (uploadFileRes) => {  
          const res = JSON.parse(uploadFileRes.data)  
          if (res.code === 200) resolve(res)  
          else reject(res)  
        },  
        fail: (err) => reject(err)  
      })  
    })  
  },  
    
  // 添加到commonApi对象中  
  uploadBatchImages  
}
