<template>
  <view class="user-home-container">
    <view class="header-bg"></view>

    <view class="profile-card">
      <view class="top-row">
        <view class="avatar-wrapper">
          <image
            v-if="userInfo?.avatarUrl"
            :src="userInfo.avatarUrl"
            mode="aspectFill"
            class="avatar-img"
          />
          <view v-else class="avatar-placeholder">👤</view>
        </view>

        <view class="btn-group">
          <button
            class="action-btn follow-style"
            :class="{ 'is-followed': userInfo?.isFollowing }"
            @click="handleFollow"
          >
            {{ userInfo?.isFollowing ? '已关注' : '+ 关注' }}
          </button>
        </view>
      </view>

      <view v-if="userInfo" class="info-block">
        <view class="name-row">
          <text class="name">{{ userInfo.nickname }}</text>
        </view>
        <view v-if="userInfo.school" class="school-row">
          <text class="school-tag">{{ userInfo.school }}</text>
        </view>
        <view class="bio">{{ userInfo.bio || '这个人很懒，什么都没有留下。' }}</view>
      </view>

      <view v-if="userInfo" class="stats-row">
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

    <view class="module-status">
      <text class="status-title">当前版本说明</text>
      <text class="status-desc">私聊与消息会话模块已下线，当前仅保留用户资料、关注关系与公开内容浏览。</text>
    </view>

    <view class="posts-section">
      <view class="section-header">
        <text class="section-title">Ta 的动态</text>
        <text v-if="userPosts.length" class="post-count">({{ userPosts.length }})</text>
      </view>

      <view v-if="loadingPosts" class="loading-box">
        <text>加载动态中...</text>
      </view>

      <view v-else-if="!userPosts || userPosts.length === 0" class="empty-tip">
        <text>暂无公开动态</text>
      </view>

      <view v-else class="post-list">
        <view
          v-for="post in userPosts"
          :key="post.id"
          class="post-item"
          @click="goToPostDetail(post.id)"
        >
          <view class="post-text">{{ post.content }}</view>

          <view v-if="post.images && post.images.length > 0" class="post-media">
            <image :src="post.images[0]" mode="aspectFill" class="media-img" />
            <view v-if="post.images.length > 1" class="media-count">+{{ post.images.length }}</view>
          </view>

          <view class="post-footer">
            <text class="post-time">{{ formatDate(post.createTime) }}</text>
            <view class="post-stats">
              <text class="stat-icon">浏览 {{ post.stats?.views || 0 }}</text>
              <text class="stat-icon">点赞 {{ post.stats?.likes || 0 }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

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
  if (!options.id) return
  userId.value = options.id
  loadUserProfile()
  loadUserPosts()
})

const loadUserProfile = async () => {
  try {
    const res = await userApi.getUserProfile(userId.value)
    if (res.code === 200) {
      userInfo.value = res.data
    }
  } catch (error) {
    console.error('加载用户资料失败', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

const loadUserPosts = async () => {
  loadingPosts.value = true
  try {
    const res = await userApi.getUserPosts(userId.value)
    if (res.code === 200) {
      if (Array.isArray(res.data)) {
        userPosts.value = res.data
      } else if (Array.isArray(res.data?.list)) {
        userPosts.value = res.data.list
      } else {
        userPosts.value = []
      }
    }
  } catch (error) {
    console.error('加载动态失败', error)
  } finally {
    loadingPosts.value = false
  }
}

const handleFollow = async () => {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }

  const newState = !userInfo.value?.isFollowing
  try {
    await userApi.toggleFollow(userId.value, newState)
    userInfo.value.isFollowing = newState
    uni.showToast({ title: newState ? '已关注' : '已取消', icon: 'none' })
  } catch (error) {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

const goToPostDetail = (id) => {
  uni.navigateTo({ url: `/pages/post/detail?id=${id}` })
}

const formatDate = (ts) => {
  if (!ts) return ''
  return `${ts}`.split('T')[0]
}
</script>

<style scoped>
.user-home-container {
  min-height: 100vh;
  background: #f5f5f5;
}

.header-bg {
  height: 260rpx;
  background: linear-gradient(120deg, #84fab0 0%, #8fd3f4 100%);
}

.profile-card {
  position: relative;
  margin: -100rpx 30rpx 30rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.top-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  height: 80rpx;
  position: relative;
  margin-bottom: 20rpx;
}

.avatar-wrapper {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 150rpx;
  height: 150rpx;
  border-radius: 50%;
  border: 6rpx solid #fff;
  background: #fff;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 10;
}

.avatar-img {
  width: 100%;
  height: 100%;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: #eee;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 60rpx;
}

.btn-group {
  display: flex;
  gap: 20rpx;
}

.action-btn {
  margin: 0;
  height: 64rpx;
  line-height: 60rpx;
  border-radius: 32rpx;
  font-size: 26rpx;
  padding: 0 36rpx;
  box-sizing: border-box;
}

.follow-style {
  background: #52c41a;
  color: #fff;
  border: 2rpx solid #52c41a;
}

.follow-style.is-followed {
  background: #f5f5f5;
  color: #999;
  border-color: #ddd;
}

.info-block {
  margin-top: 20rpx;
  margin-bottom: 30rpx;
}

.name {
  font-size: 40rpx;
  font-weight: bold;
  color: #333;
}

.school-row {
  margin-top: 10rpx;
}

.school-tag {
  font-size: 22rpx;
  color: #1890ff;
  background: #e6f7ff;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  border: 1rpx solid #91d5ff;
}

.bio {
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #666;
  line-height: 1.4;
}

.stats-row {
  display: flex;
  border-top: 1rpx solid #eee;
  padding-top: 20rpx;
}

.stat {
  flex: 1;
  text-align: center;
}

.stat .num {
  display: block;
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.stat .label {
  font-size: 24rpx;
  color: #999;
}

.module-status {
  margin: 0 30rpx 30rpx;
  padding: 28rpx 30rpx;
  background: #fffbe6;
  border: 1rpx solid #ffe58f;
  border-radius: 20rpx;
}

.status-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #ad6800;
}

.status-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #8c6d1f;
  line-height: 1.6;
}

.posts-section {
  margin: 0 30rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 30rpx;
}

.section-header {
  margin-bottom: 20rpx;
  display: flex;
  align-items: baseline;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-right: 10rpx;
}

.post-count {
  color: #999;
  font-size: 24rpx;
}

.post-item {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.post-item:last-child {
  border-bottom: none;
}

.post-text {
  font-size: 28rpx;
  color: #333;
  line-height: 1.5;
  margin-bottom: 16rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-media {
  position: relative;
  width: 200rpx;
  height: 160rpx;
  margin-bottom: 16rpx;
  border-radius: 8rpx;
  overflow: hidden;
}

.media-img {
  width: 100%;
  height: 100%;
  background: #f0f0f0;
}

.media-count {
  position: absolute;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 20rpx;
  padding: 2rpx 8rpx;
  border-top-left-radius: 8rpx;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 22rpx;
  color: #ccc;
}

.post-stats .stat-icon {
  margin-left: 20rpx;
}

.loading-box,
.empty-tip {
  text-align: center;
  padding: 60rpx 0;
  color: #999;
  font-size: 26rpx;
}
</style>
