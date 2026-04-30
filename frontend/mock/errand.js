// mock/errand.js
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))

// 模拟数据库
let errands = [
  {
    id: 1,
    publisherId: 1001, // 假设 1001 是当前登录用户 (小明)
    publisher: { id: 1001, nickname: "我 (小明)", avatar: "" },
    content: "帮我带一杯瑞幸咖啡，生椰拿铁，少冰无糖",
    pickupAddr: "北门瑞幸",
    deliveryAddr: "图书馆 404",
    bounty: 3.00,
    status: 0, // 待接单
    hiddenInfo: "手机尾号 1234",
    createTime: Date.now() - 1000000,
    runnerId: null
  },
  {
    id: 2,
    publisherId: 999, // 其他人
    publisher: { id: 999, nickname: "学习狂魔", avatar: "" },
    content: "急需一个笔记本电脑充电器，联想方口的，借用两小时",
    pickupAddr: "C楼 302",
    deliveryAddr: "C楼 302",
    bounty: 10.00,
    status: 0,
    hiddenInfo: "到了直接敲门",
    createTime: Date.now() - 2000000,
    runnerId: null
  },
  // 2. 对应 mock/user.js -> getMyErrands -> publishedList (我发布的)
    {
      id: 201,
      publisherId: 1001, // 1001是当前用户
      publisher: { id: 1001, nickname: "小明", avatar: "" },
      content: "帮拿快递",
      pickupAddr: "南门",
      deliveryAddr: "3栋",
      bounty: 2.00,
      currency: 2, // 现金
      status: 0, // 待接单
      hiddenInfo: "取件码 8888",
      deadline: "2024-12-31 18:00:00",
      version: 1,
      createTime: Date.now() - 200000
    },
    {
      id: 202,
      publisherId: 1001, // 1001是当前用户
      publisher: { id: 1001, nickname: "小明", avatar: "" },
      content: "求借充电宝",
      pickupAddr: "C楼",
      deliveryAddr: "C楼",
      bounty: 50,
      currency: 1, // 积分
      status: 2, // 已完成
      hiddenInfo: "到了直接敲门",
      deadline: "2024-10-01 12:00:00",
      version: 1,
      createTime: Date.now() - 86400000
    },
  
    // 3. 对应 mock/user.js -> getMyErrands -> acceptedList (我接受的)
    {
      id: 301,
      publisherId: 1002, // 别人发的
      publisher: { id: 1002, nickname: "学习狂魔", avatar: "" },
      content: "代买咖啡",
      pickupAddr: "瑞幸",
      deliveryAddr: "图书馆",
      bounty: 3.00,
      currency: 2,
      status: 1, // 进行中
      hiddenInfo: "手机号 13800000000", // 因为我接了单，所以我能看到
      deadline: "2024-12-31 14:00:00",
      version: 1,
      createTime: Date.now() - 50000,
      runnerId: 1001 // 接单人是我(1001)
    }

]

export default {
  // 5.2 获取列表
  async getList(params) {
    await delay(500)
    // 简单过滤：Mock暂时只返回所有待接单的，或者按需过滤
    // 真实后端会根据 status=0 筛选
    const list = errands.map(item => {
      // 列表页通常不返回 hiddenInfo
      const { hiddenInfo, ...rest } = item
      return rest
    })
    return { code: 200, data: { list, total: list.length } }
  },

  // 5.6 获取详情
  async getDetail(id) {
    await delay(400)
    const item = errands.find(e => e.id == id)
    if (!item) throw { code: 404, message: '订单不存在' }
    
    // 模拟权限控制：
    // 只有 "我是发布者" 或者 "我是接单人" 才能看到 hiddenInfo
    const currentUserId = 1001 // 假设当前用户是 1001
    const canSeeHidden = item.publisherId === currentUserId || item.runnerId === currentUserId
    
    if (canSeeHidden) {
      return { code: 200, data: item }
    } else {
      const { hiddenInfo, ...rest } = item
      return { code: 200, data: { ...rest, hiddenInfo: '*** 接单后可见 ***' } }
    }
  },

  // 5.1 发布 (更新逻辑)
    async create(data) {
      await delay(800)
      
      // 逻辑检查：如果是积分支付，检查余额
      if (data.currency === 1) { // 1-积分
        if (userInfo.points < data.bounty) {
          throw { code: 400, message: `积分不足，当前仅剩 ${userInfo.points} 积分` }
        }
        // 扣除积分
        userInfo.points -= parseInt(data.bounty)
      }
  
      const newErrand = {
        id: Date.now(),
        publisherId: 1001,
        publisher: { id: 1001, nickname: "小明", avatar: "" },
        ...data,
        status: 0, // 0-待接单
        runnerId: null,
        version: 1, // 初始版本号
        createTime: Date.now()
      }
      errands.unshift(newErrand)
      return { code: 200, message: '发布成功', data: newErrand }
    },

  // 5.3 接单
  async accept(id) {
    await delay(600)
    const item = errands.find(e => e.id == id)
    if (item.status !== 0) throw { code: 400, message: '手慢了，已被抢单' }
    
    item.status = 1 // 进行中
    item.runnerId = 1001 // 假设我自己接的单
    return { code: 200, message: '抢单成功！请尽快配送' }
  },

  // 5.4 确认完成
  async complete(id) {
    await delay(500)
    const item = errands.find(e => e.id == id)
    item.status = 2 // 已完成
    return { code: 200, message: '订单已完成，赏金已到账' }
  },

  // 5.5 取消
  async cancel(id) {
    await delay(500)
    const item = errands.find(e => e.id == id)
    item.status = 3 // 已取消
    return { code: 200, message: '订单已取消' }
  }
}