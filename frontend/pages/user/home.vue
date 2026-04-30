<template>
  <view class="user-home-container">
    <!-- 头部背景 -->
    <view class="header-bg"></view>
    
    <!-- 个人资料卡片 -->
    <view class="profile-card">
      <!-- 顶部行：包含头像(绝对定位) 和 按钮组(右对齐) -->
      <view class="top-row">
        <!-- 头像 -->
        <view class="avatar-wrapper">
          <image 
            v-if="userInfo?.avatarUrl" 
            :src="userInfo.avatarUrl" 
            mode="aspectFill" 
            class="avatar-img"
          />
          <view v-else class="avatar-placeholder">👤</view>
        </view>
        
        <!-- 按钮组 -->
        <view class="btn-group">
          <button class="action-btn chat-style" @click="handleChat">私聊</button>
          <button 
            class="action-btn follow-style" 
            :class="{ 'is-followed': userInfo?.isFollowing }"
            @click="handleFollow"
          >
            {{ userInfo?.isFollowing ? '已关注' : '+ 关注' }}
          </button>
        </view>
      </view>
      
      <!-- 用户信息 -->
      <view class="info-block" v-if="userInfo">
        <view class="name-row">
          <text class="name">{{ userInfo.nickname }}</text>
        </view>
        <view class="school-row" v-if="userInfo.school">
          <text class="school-tag">{{ userInfo.school }}</text>
        </view>
        <view class="bio">{{ userInfo.bio || '这个人很懒，什么都没留下...' }}</view>
      </view>
      
      <!-- 统计数据 -->
      <view class="stats-row" v-if="userInfo">
        <view class="stat">
          <text class="num">{{ userInfo.stats?.likes || 0 }}</text>
          <text class="label">获赞</text>
        </view>
        <view class="stat">
          <text class="num">{{ userInfo.stats?.following || 0 }}</text>
          <text class="label">关注</text>
        </view>
        <view class="stat">
          <text class="num">{{ userInfo.stats?.followers || 0 }}</text>
          <text class="label">粉丝</text>
        </view>
      </view>
    </view>
    
    <!-- Ta 的动态 -->
    <view class="posts-section">
      <view class="section-header">
        <text class="section-title">Ta 的动态</text>
        <text class="post-count" v-if="userPosts.length">({{ userPosts.length }})</text>
      </view>
      
      <!-- 加载中 -->
      <view v-if="loadingPosts" class="loading-box">
        <text>加载动态中...</text>
      </view>
      
      <!-- 空状态 -->
      <view v-else-if="!userPosts || userPosts.length === 0" class="empty-tip">
        <text>暂无公开动态</text>
      </view>
      
      <!-- 帖子列表 -->
      <view v-else class="post-list">
        <view 
          v-for="post in userPosts" 
          :key="post.id" 
          class="post-item"
          @click="goToPostDetail(post.id)"
        >
          <!-- 纯文本内容 -->
          <view class="post-text">{{ post.content }}</view>
          
          <!-- 图片展示 (如果有) -->
          <view class="post-media" v-if="post.images && post.images.length > 0">
            <image :src="post.images[0]" mode="aspectFill" class="media-img" />
            <view v-if="post.images.length > 1" class="media-count">+{{post.images.length}}</view>
          </view>
          
          <!-- 底部信息 -->
          <view class="post-footer">
            <text class="post-time">{{ formatDate(post.createTime) }}</text>
            <view class="post-stats">
              <text class="stat-icon">👁 {{ post.stats?.views || 0 }}</text>
              <text class="stat-icon">❤️ {{ post.stats?.likes || 0 }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 底部占位 -->
    <view style="height: 40rpx;"></view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userInfo = ref(null)
const userPosts = ref([])
const userId = ref('')
const loadingPosts = ref(false)
const userStore = useUserStore()

onLoad((options) => {
  if (options.id) {
    userId.value = options.id
    console.log('当前查看用户ID:', userId.value)
    loadUserProfile()
    loadUserPosts()
  }
})

// 加载个人资料
const loadUserProfile = async () => {
  try {
    const res = await userApi.getUserProfile(userId.value)
    if (res.code === 200) {
      userInfo.value = res.data
    }
  } catch (e) {
    console.error('用户资料加载失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

// 加载动态
const loadUserPosts = async () => {
  loadingPosts.value = true
  try {
    const res = await userApi.getUserPosts(userId.value)
    console.log('用户动态API返回:', res) // 调试日志
    
    if (res.code === 200) {
      // 兼容后端可能直接返回数组，或者返回 { list: [] } 的情况
      if (Array.isArray(res.data)) {
        userPosts.value = res.data
      } else if (res.data && Array.isArray(res.data.list)) {
        userPosts.value = res.data.list
      } else {
        userPosts.value = []
      }
    }
  } catch (e) {
    console.error('加载动态失败', e)
  } finally {
    loadingPosts.value = false
  }
}

const handleChat = () => {
  if (!userStore.isLoggedIn) return uni.navigateTo({ url: '/pages/login/index' })
  if (String(userStore.userInfo?.id) === String(userId.value)) {
    return uni.showToast({ title: '不能和自己私聊', icon: 'none' })
  }
  uni.navigateTo({ url: `/pages/chat/detail?userId=${userId.value}` })
}

const handleFollow = async () => {
  if (!userStore.isLoggedIn) return uni.navigateTo({ url: '/pages/login/index' })
  
  const newState = !userInfo.value.isFollowing
  try {
    await userApi.toggleFollow(userId.value, newState)
    userInfo.value.isFollowing = newState
    uni.showToast({ title: newState ? '已关注' : '已取消', icon: 'none' })
  } catch (e) {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

const goToPostDetail = (id) => {
  uni.navigateTo({ url: `/pages/post/detail?id=${id}` })
}

const formatDate = (ts) => {
  if (!ts) return ''
  return ts.split('T')[0]
}
</script>

<style scoped>
.user-home-container { min-height: 100vh; background: #F5F5F5; }

/* 头部背景图 */
.header-bg {
  height: 260rpx;
  background: linear-gradient(120deg, #84fab0 0%, #8fd3f4 100%);
}

/* 资料卡片 */
.profile-card {
  position: relative;
  margin: -100rpx 30rpx 30rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.06);
}

/* --- 1. 顶部布局修正 --- */
.top-row {
  display: flex;
  justify-content: flex-end; /* 按钮靠右 */
  align-items: center; /* 垂直居中 */
  height: 80rpx; /* 给一个固定高度，给头像留出位置 */
  position: relative;
  margin-bottom: 20rpx;
}

/* --- 2. 头像绝对定位 --- */
.avatar-wrapper {
  position: absolute;
  left: 0;
  bottom: 0; /* 对齐 top-row 的底部 */
  width: 150rpx;
  height: 150rpx;
  border-radius: 50%;
  border: 6rpx solid #fff;
  background: #fff;
  box-shadow: 0 2rpx 10rpx rgba(0,0,0,0.1);
  overflow: hidden;
  z-index: 10;
}
.avatar-img { width: 100%; height: 100%; }
.avatar-placeholder { 
  width: 100%; height: 100%; 
  background: #eee; 
  display: flex; justify-content: center; align-items: center; 
  font-size: 60rpx; 
}

/* --- 3. 按钮组美化 --- */
.btn-group {
  display: flex;
  gap: 20rpx;
}

.action-btn {
  margin: 0;
  height: 64rpx;
  line-height: 60rpx; /* 减去边框 */
  border-radius: 32rpx;
  font-size: 26rpx;
  padding: 0 36rpx;
  box-sizing: border-box;
}

.chat-style {
  background: #fff;
  color: #52C41A;
  border: 2rpx solid #52C41A;
}

.follow-style {
  background: #52C41A;
  color: #fff;
  border: 2rpx solid #52C41A;
}
.follow-style.is-followed {
  background: #f5f5f5;
  color: #999;
  border-color: #ddd;
}

/* 信息区域 */
.info-block { margin-top: 20rpx; margin-bottom: 30rpx; }
.name { font-size: 40rpx; font-weight: bold; color: #333; }
.school-row { margin-top: 10rpx; }
.school-tag { 
  font-size: 22rpx; color: #1890FF; background: #e6f7ff; 
  padding: 4rpx 12rpx; border-radius: 6rpx; border: 1rpx solid #91d5ff;
}
.bio { margin-top: 16rpx; font-size: 28rpx; color: #666; line-height: 1.4; }

/* 统计数据 */
.stats-row { display: flex; border-top: 1rpx solid #eee; padding-top: 20rpx; }
.stat { flex: 1; text-align: center; }
.stat .num { display: block; font-size: 32rpx; font-weight: bold; color: #333; }
.stat .label { font-size: 24rpx; color: #999; }

/* 动态区域 */
.posts-section { margin: 0 30rpx; background: #fff; border-radius: 24rpx; padding: 30rpx; }
.section-header { margin-bottom: 20rpx; display: flex; align-items: baseline; }
.section-title { font-size: 32rpx; font-weight: bold; color: #333; margin-right: 10rpx; }
.post-count { color: #999; font-size: 24rpx; }

/* 列表样式 */
.post-item { padding: 20rpx 0; border-bottom: 1rpx solid #f0f0f0; }
.post-item:last-child { border-bottom: none; }
.post-text { font-size: 28rpx; color: #333; line-height: 1.5; margin-bottom: 16rpx; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.post-media { position: relative; width: 200rpx; height: 160rpx; margin-bottom: 16rpx; border-radius: 8rpx; overflow: hidden; }
.media-img { width: 100%; height: 100%; background: #f0f0f0; }
.media-count { position: absolute; right: 0; bottom: 0; background: rgba(0,0,0,0.5); color: #fff; font-size: 20rpx; padding: 2rpx 8rpx; border-top-left-radius: 8rpx; }

.post-footer { display: flex; justify-content: space-between; align-items: center; font-size: 22rpx; color: #ccc; }
.post-stats .stat-icon { margin-left: 20rpx; }

.loading-box, .empty-tip { text-align: center; padding: 60rpx 0; color: #999; font-size: 26rpx; }
</style>