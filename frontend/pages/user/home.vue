<template>
  <view class="user-home-container">
    <view class="page-glow page-glow-left"></view>
    <view class="page-glow page-glow-right"></view>

    <view class="custom-nav">
      <view class="nav-btn" @click="goBack">‹</view>
      <text class="nav-title">用户主页</text>
      <view class="nav-placeholder"></view>
    </view>

    <view class="profile-hero">
      <view class="hero-top">
        <view class="avatar-wrapper">
          <image
            v-if="userInfo?.avatarUrl"
            :src="userInfo.avatarUrl"
            mode="aspectFill"
            class="avatar-img"
          />
          <view v-else class="avatar-placeholder">◌</view>
        </view>

        <button
          class="action-btn follow-style"
          :class="{ 'is-followed': userInfo?.isFollowing }"
          @click="handleFollow"
        >
          {{ userInfo?.isFollowing ? '已关注' : '+ 关注' }}
        </button>
      </view>

      <view v-if="userInfo" class="info-block">
        <text class="name">{{ userInfo.nickname }}</text>
        <text v-if="userInfo.school" class="school-tag">{{ userInfo.school }}</text>
        <text class="bio">{{ userInfo.bio || '这个用户还没有留下更多自我介绍。' }}</text>
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
      <text class="status-title">公开主页</text>
      <text class="status-desc">你可以在这里查看对方公开资料、关注关系和最新动态。</text>
    </view>

    <view class="posts-section">
      <view class="section-header">
        <view>
          <text class="section-title">Ta 的动态</text>
          <text class="section-subtitle">最近公开发布的内容会显示在这里</text>
        </view>
        <text v-if="userPosts.length" class="post-count">{{ userPosts.length }}</text>
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
          <text class="post-text">{{ post.content }}</text>

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

    <view class="page-bottom-space"></view>
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

const goBack = () => {
  uni.navigateBack()
}

const formatDate = (ts) => {
  if (!ts) return ''
  return `${ts}`.split('T')[0]
}
</script>

<style scoped>
.user-home-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 24rpx 24rpx 56rpx;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.24), transparent 28%),
    linear-gradient(180deg, #faf7f2 0%, #f5f1eb 100%);
}

.page-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(24rpx);
  opacity: 0.4;
  pointer-events: none;
}

.page-glow-left {
  width: 240rpx;
  height: 240rpx;
  top: 120rpx;
  left: -100rpx;
  background: rgba(185, 160, 213, 0.34);
}

.page-glow-right {
  width: 210rpx;
  height: 210rpx;
  top: 340rpx;
  right: -80rpx;
  background: rgba(140, 128, 216, 0.24);
}

.custom-nav,
.profile-hero,
.module-status,
.posts-section {
  position: relative;
  z-index: 2;
}

.custom-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.nav-btn,
.nav-placeholder {
  width: 72rpx;
  height: 72rpx;
}

.nav-btn {
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.8);
  border: 1rpx solid rgba(140, 128, 216, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--theme-ink);
  font-size: 36rpx;
}

.nav-title {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.profile-hero {
  padding: 34rpx;
  border-radius: 34rpx;
  background: var(--theme-gradient);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow);
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.avatar-wrapper {
  width: 150rpx;
  height: 150rpx;
  border-radius: 50%;
  border: 6rpx solid rgba(255, 255, 255, 0.96);
  background: #fff;
  box-shadow: 0 10rpx 24rpx rgba(121, 110, 176, 0.18);
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: var(--theme-gradient-strong);
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  font-size: 56rpx;
}

.action-btn {
  margin: 0;
  min-width: 168rpx;
  height: 68rpx;
  line-height: 64rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  padding: 0 32rpx;
  box-sizing: border-box;
  border: none;
}

.follow-style {
  background: var(--theme-ink);
  color: #fff;
}

.follow-style.is-followed {
  background: rgba(255, 255, 255, 0.72);
  color: var(--theme-muted);
}

.info-block {
  margin-top: 26rpx;
}

.name,
.school-tag,
.bio,
.status-title,
.status-desc,
.section-title,
.section-subtitle {
  display: block;
}

.name {
  font-size: 42rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.school-tag {
  margin-top: 12rpx;
  display: inline-block;
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.84);
  color: var(--theme-primary-deep);
  font-size: 23rpx;
}

.bio {
  margin-top: 16rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #6d6582;
}

.stats-row {
  display: flex;
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid rgba(140, 128, 216, 0.12);
}

.stat {
  flex: 1;
  text-align: center;
}

.stat .num {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.stat .label {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: var(--theme-muted);
}

.module-status {
  margin-top: 22rpx;
  padding: 28rpx 30rpx;
  background: rgba(255, 255, 255, 0.82);
  border: 1rpx solid rgba(140, 128, 216, 0.1);
  border-radius: 28rpx;
  box-shadow: var(--theme-shadow-soft);
}

.status-title {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--theme-primary-deep);
}

.status-desc {
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #716a85;
}

.posts-section {
  margin-top: 22rpx;
  background: rgba(255, 255, 255, 0.84);
  border: 1rpx solid rgba(140, 128, 216, 0.1);
  border-radius: 30rpx;
  padding: 30rpx;
  box-shadow: var(--theme-shadow-soft);
}

.section-header {
  margin-bottom: 18rpx;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.section-subtitle {
  margin-top: 8rpx;
  font-size: 23rpx;
  color: var(--theme-muted);
}

.post-count {
  min-width: 58rpx;
  height: 58rpx;
  border-radius: 50%;
  background: rgba(140, 128, 216, 0.1);
  color: var(--theme-primary-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 700;
}

.post-item {
  padding: 24rpx 0;
  border-bottom: 1rpx solid rgba(140, 128, 216, 0.08);
}

.post-item:last-child {
  border-bottom: none;
}

.post-text {
  font-size: 28rpx;
  color: var(--theme-ink);
  line-height: 1.65;
  margin-bottom: 16rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-media {
  position: relative;
  width: 220rpx;
  height: 168rpx;
  margin-bottom: 16rpx;
  border-radius: 18rpx;
  overflow: hidden;
}

.media-img {
  width: 100%;
  height: 100%;
  background: #f0eef8;
}

.media-count {
  position: absolute;
  right: 12rpx;
  bottom: 12rpx;
  min-width: 52rpx;
  height: 36rpx;
  padding: 0 10rpx;
  border-radius: 999rpx;
  background: rgba(52, 48, 48, 0.68);
  color: #fff;
  font-size: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
  font-size: 22rpx;
  color: var(--theme-muted);
}

.post-stats {
  display: flex;
  gap: 18rpx;
  flex-wrap: wrap;
}

.loading-box,
.empty-tip {
  text-align: center;
  padding: 60rpx 0;
  color: var(--theme-muted);
  font-size: 26rpx;
}

.page-bottom-space {
  height: 24rpx;
}
</style>
