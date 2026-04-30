// mock/user.js
// 模拟简单的内存数据库
import { userInfo as currentUser } from './user'

export const userInfo = {
  id: 1001,
  username: "student2024",
  nickname: "小明",
  avatarUrl: "",
  school: "计算机学院",
  points: 120, // 初始积分
  bio: "好好学习",
  checkedIn: false
}
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))

export default {
  // 模拟登录
  async login(data) {
    await delay(500)
    if (data.username === 'student2024' && data.password === '123456') {
      return {
        code: 200,
        data: { token: "MockToken123", user: userInfo }
      }
    }
    throw { code: 400, message: '用户名或密码错误' }
  },

  // 模拟获取信息
  async getUserInfo() {
    await delay(300)
    return { code: 200, data: userInfo }
  },

  // 模拟我的帖子
  async getMyPosts() {
    await delay(300)
    return {
      code: 200,
      data: {
        list: [
          { id: 1, content: "这是我发布的第一条帖子，校园里的猫真可爱 #猫咪", createTime: Date.now(), stats: { views: 120 } },
          { id: 2, content: "期末考试求复习资料", createTime: Date.now(), stats: { views: 45 } }
        ]
      }
    }
  },

  // 3.6 获取签到状态
  async getCheckInStatus() {
    await delay(200)
    return {
      code: 200,
      data: { checkedIn: userInfo.checkedIn, checkInDays: 5 }
    }
  },

  // 3.7 签到动作
  async checkIn() {
    await delay(500)
    if (userInfo.checkedIn) {
      throw { code: 400, message: '今天已经签过到了' }
    }
    // 更新数据
    userInfo.checkedIn = true
    userInfo.points += 10
    
    return {
      code: 200,
      message: '签到成功 +10积分',
      data: { points: 10, totalPoints: userInfo.points }
    }
  },
  // 3.2 更新个人资料
    async updateProfile(params) {
      await delay(800)
      // 更新内存中的用户信息
      if (params.nickname) userInfo.nickname = params.nickname
      if (params.bio) userInfo.bio = params.bio
      if (params.avatarUrl) userInfo.avatarUrl = params.avatarUrl
      if (params.school) userInfo.school = params.school
      
      return {
        code: 200,
        message: '保存成功',
        data: userInfo // 返回最新的用户信息
      }
    },
    
    // 补充: 获取我的跑腿
    async getMyErrands(params) {
      await delay(500)
      const type = params.type || 'published'
      return {
        code: 200,
        data: {
          list: [
            {
              id: type === 'published' ? 201 : 301,
              content: type === 'published' ? "求带一份黄焖鸡米饭到C楼" : "帮同学取了快递",
              status: type === 'published' ? 0 : 2, // 0待接单, 2已完成
              bounty: 5.00,
              createTime: Date.now()
            }
          ]
        }
      }
    },
	
	async  updatePoints({ delta }) {
	  await delay(300);
	
	  if (userInfo.points + delta < 0) {
	    throw { code: 400, message: '积分不足' };
	  }
	
	  userInfo.points += delta;
	  return {
	    code: 200,
	    message: '积分已更新',
	    data: { points: userInfo.points }
	  };
	},
	// 3.8 变更积分 (新增)
	  async changePoints({ amount }) {
	    await delay(300)
	    if (userInfo.points + amount < 0) {
	      throw { code: 400, message: '积分不足' }
	    }
	    userInfo.points += amount
	    return { code: 200, data: { currentPoints: userInfo.points } }
	  },
	// 获取我的跑腿 (根据之前定义的 3.2 补充，这里完善逻辑)
	  async getMyErrands(params) {
	    await delay(500)
	    const type = params.type || 'published'
	    
	    // 模拟两组数据
	    const publishedList = [
	      { id: 201, content: "帮拿快递", pickupAddr:"南门", deliveryAddr:"3栋", status: 0, bounty: 2.00, currency: 2, createTime: Date.now() },
	      { id: 202, content: "求借充电宝", pickupAddr:"C楼", deliveryAddr:"C楼", status: 2, bounty: 50, currency: 1, createTime: Date.now() - 86400000 }
	    ]
	    const acceptedList = [
	      { id: 301, content: "代买咖啡", pickupAddr:"瑞幸", deliveryAddr:"图书馆", status: 1, bounty: 3.00, currency: 2, createTime: Date.now() }
	    ]
	
	    return {
	      code: 200,
	      data: {
	        list: type === 'published' ? publishedList : acceptedList,
	        total: 10
	      }
	    }
	  },
	
	  // 3.9 获取关注列表
	  async getFollowList() {
	    await delay(400)
	    return {
	      code: 200,
	      data: {
	        list: [
	          { id: 1002, nickname: "学习小能手", avatarUrl: "", bio: "天天向上", isFollowing: true },
	          { id: 1003, nickname: "运动达人", avatarUrl: "", bio: "约球找我", isFollowing: true }
	        ]
	      }
	    }
	  },
	
	  // 3.10 获取他人主页
	  async getUserProfile(id) {
	    await delay(500)
	    return {
	      code: 200,
	      data: {
	        id: parseInt(id),
	        nickname: id == 1002 ? "学习小能手" : "路人甲",
	        avatarUrl: "",
	        school: "理学院",
	        bio: "这是模拟的他人主页签名",
	        stats: { likes: 345, following: 12, followers: 89 },
	        isFollowing: id == 1002 // 模拟：只关注了1002
	      }
	    }
	  },
	
	  // 3.11 关注/取关
	  async toggleFollow(id) {
	    await delay(300)
	    return { code: 200, message: '操作成功' }
	  },
	  // 3.4 获取我的帖子 (确保符合你提供的 U05 格式)
	    async getMyPosts(page) {
	      await delay(400)
	      return {
	        code: 200,
	        data: {
	          list: [
	            {
	              id: 1,
	              content: "今天天气真好，去图书馆打卡！#学习",
	              images: ["https://picsum.photos/200/200?1"], // 模拟一张图
	              createTime: "2023-10-01T12:00:00Z",
	              stats: { views: 100, likes: 10, comments: 2 }
	            },
	            {
	              id: 2,
	              content: "食堂的红烧肉卖完了，难受...",
	              images: [],
	              createTime: "2023-10-02T12:00:00Z",
	              stats: { views: 50, likes: 2, comments: 0 }
	            }
	          ],
	          total: 2,
	          hasMore: false
	        }
	      }
	    },
	  
	    // 3.12 获取我的收藏 (带作者信息)
	    async getMyCollections(page) {
	      await delay(500)
	      return {
	        code: 200,
	        message: "success",
	        data: {
	          list: [
	            {
	              id: 88, // 帖子的ID
	              content: "期末复习重点整理，拿走不谢！",
	              images: ["https://picsum.photos/200/200?2"],
	              author: { 
	                id: 1005,
	                nickname: "学伯",
	                avatar: "" 
	              },
	              stats: { views: 1000, likes: 500, comments: 20 },
	              createTime: "2023-09-01T12:00:00Z",
	              collectedAt: "2023-10-05T09:00:00Z"
	            }
	          ],
	          total: 1,
	          hasMore: false
	        }
	      }
	    },
	  
	    // 3.9 获取粉丝列表
	    async getFollowers(page) {
	      await delay(400)
	      return {
	        code: 200,
	        data: {
	          list: [
	            { id: 1008, nickname: "小学妹", avatarUrl: "", bio: "很高兴认识你", isFollowing: false }, // false代表我没回粉
	            { id: 1009, nickname: "隔壁班长", avatarUrl: "", bio: "互关呀", isFollowing: true } // true代表互关
	          ],
	          total: 2
	        }
	      }
	    }

}