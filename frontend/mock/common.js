// mock/common.js
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))

export default {
  // 模拟文件上传
  async uploadFile() {
    await delay(1000) // 模拟网络延迟
    return {
      code: 200,
      message: '上传成功',
      data: {
        // 返回一个随机图片作为模拟结果
        // 这里的图片地址是互联网上的随机图服务，用于测试
        url: `https://picsum.photos/200/200?random=${Date.now()}` 
      }
    }
  }
}