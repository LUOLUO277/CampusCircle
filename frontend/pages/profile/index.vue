<template>
  <view class="page-container">
    <view class="profile-hero">
      <view v-if="userStore.isLoggedIn" class="checkin-btn-wrapper">
        <button
          class="checkin-btn"
          :class="{ checked: isCheckedIn }"
          :disabled="isCheckedIn"
          @click="handleCheckIn"
        >
          {{ isCheckedIn ? '今日已签到' : '签到 +10' }}
        </button>
      </view>

      <view class="user-card" @click="handleUserCardClick">
        <image
          v-if="userStore.isLoggedIn && userStore.avatar"
          :src="userStore.avatar"
          class="avatar-img"
          mode="aspectFill"
        />
        <view v-else class="avatar-placeholder">◌</view>

        <view class="info">
          <template v-if="userStore.isLoggedIn">
            <text class="username">{{ userStore.nickname }}</text>
            <text class="school">{{ userStore.userInfo.school || '未认证学校' }}</text>
            <text class="bio">{{ userStore.userInfo.bio || '还没有留下个人简介。' }}</text>
          </template>
          <template v-else>
            <text class="username">未登录用户</text>
            <text class="desc">点击这里登录或注册，开始管理你的校园主页。</text>
          </template>
        </view>
      </view>

      <view v-if="userStore.isLoggedIn" class="stats">
        <view class="stat-item">
          <text class="num">{{ userStore.userInfo.points || 0 }}</text>
          <text class="label">我的积分</text>
        </view>
        <view class="stat-item" @click="handleFollowers">
          <text class="num">{{ userStore.userInfo.stats?.followers || 0 }}</text>
          <text class="label">粉丝</text>
        </view>
        <view class="stat-item" @click="goToFollowList">
          <text class="num">{{ userStore.userInfo.stats?.following || 0 }}</text>
          <text class="label">关注</text>
        </view>
      </view>
    </view>

    <view class="menu-panel">
      <view class="menu-list">
        <view class="menu-item" @click="handleMyPosts">
          <text class="menu-text">我的发布</text>
          <text class="arrow">›</text>
        </view>

        <view class="menu-item" @click="handleMyCollections">
          <text class="menu-text">我的收藏</text>
          <text class="arrow">›</text>
        </view>

        <view class="menu-item" @click="handleSettings">
          <text class="menu-text">个人资料设置</text>
          <text class="arrow">›</text>
        </view>

        <view
          v-if="userStore.isLoggedIn"
          class="menu-item"
          @click="handleCanvasBinding"
        >
          <text class="menu-text">Canvas 账号绑定</text>
          <text class="arrow">›</text>
        </view>

        <view
          v-if="userStore.isAdmin"
          class="menu-item admin-entry"
          @click="handleAdmin"
        >
          <text class="menu-text">内容审核后台</text>
          <text class="arrow">›</text>
        </view>

        <view
          v-if="userStore.isAdmin"
          class="menu-item admin-entry secondary"
          @click="handleInfoAdmin"
        >
          <text class="menu-text">信息订阅后台</text>
          <text class="arrow">›</text>
        </view>

        <view
          v-if="userStore.isLoggedIn"
          class="menu-item logout"
          @click="handleLogout"
        >
          <text class="menu-text logout-text">退出登录</text>
        </view>
      </view>
    </view>

    <view v-if="userStore.isLoggedIn && myPosts.length > 0" class="recent-posts">
      <view class="section-head">
        <text class="section-title">最近发布</text>
        <text class="section-meta">{{ myPosts.length }} 条</text>
      </view>
      <view
        v-for="post in myPosts"
        :key="post.id"
        class="mini-post"
        @click="handlePostClick(post.id)"
      >
        <text class="post-content">{{ post.content }}</text>
        <view class="post-meta">
          <text class="post-date">浏览 {{ post.stats?.views || 0 }}</text>
          <text class="post-date likes">点赞 {{ post.stats?.likes || 0 }}</text>
        </view>
      </view>
    </view>

    <TabBar current-tab="profile" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TabBar from '@/components/TabBar.vue'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'

const userStore = useUserStore()
const myPosts = ref([])
const isCheckedIn = ref(false)

onShow(() => {
  uni.hideTabBar()
  if (userStore.isLoggedIn) {
    refreshData()
    return
  }

  myPosts.value = []
  isCheckedIn.value = false
})

const refreshData = async () => {
  try {
    const [userRes, checkInRes, postRes] = await Promise.all([
      userApi.getUserInfo(),
      userApi.getCheckInStatus(),
      userApi.getMyPosts({ page: 1, size: 5 })
    ])

    if (userRes.code === 200) userStore.updateUserInfo(userRes.data)
    if (checkInRes.code === 200) isCheckedIn.value = checkInRes.data.checkedIn
    if (postRes.code === 200) {
      myPosts.value = Array.isArray(postRes.data?.list) ? postRes.data.list : []
    }
  } catch (error) {
    console.error('刷新个人中心数据失败', error)
  }
}

const ensureLogin = () => {
  if (userStore.isLoggedIn) return true
  uni.navigateTo({ url: '/pages/login/index' })
  return false
}

const handleCheckIn = async () => {
  if (isCheckedIn.value) return

  try {
    uni.showLoading({ title: '签到中...' })
    const res = await userApi.checkIn()
    if (res.code === 200) {
      uni.showToast({ title: '签到成功', icon: 'success' })
      isCheckedIn.value = true
      userStore.updateUserInfo({ points: res.data.totalPoints })
    }
  } catch (error) {
    uni.showToast({ title: error.message || '签到失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

const handlePostClick = (postId) => {
  uni.navigateTo({ url: `/pages/post/detail?id=${postId}` })
}

const handleUserCardClick = () => {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.navigateTo({ url: '/pages/profile/edit' })
}

const handleSettings = () => {
  if (!ensureLogin()) return
  uni.navigateTo({ url: '/pages/profile/edit' })
}

const handleCanvasBinding = () => {
  if (!ensureLogin()) return
  uni.navigateTo({ url: '/pages/profile/canvas-binding' })
}

const handleMyPosts = () => {
  if (!ensureLogin()) return
  uni.navigateTo({ url: '/pages/profile/my-post' })
}

const handleMyCollections = () => {
  if (!ensureLogin()) return
  uni.navigateTo({ url: '/pages/profile/my-collection' })
}

const handleFollowers = () => {
  if (!userStore.isLoggedIn) return
  uni.navigateTo({ url: '/pages/profile/follow-list?type=followers' })
}

const goToFollowList = () => {
  if (!userStore.isLoggedIn) return
  uni.navigateTo({ url: '/pages/profile/follow-list?type=following' })
}

const handleAdmin = () => {
  uni.navigateTo({ url: '/pages/admin/report-list' })
}

const handleInfoAdmin = () => {
  uni.navigateTo({ url: '/pages/admin/info-center' })
}

const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (!res.confirm) return
      userStore.logout()
      myPosts.value = []
      isCheckedIn.value = false
      uni.showToast({ title: '已退出登录', icon: 'none' })
    }
  })
}
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.22), transparent 28%),
    linear-gradient(180deg, #faf7f2 0%, #f5f1eb 100%);
  padding: 28rpx 24rpx 160rpx;
}

.profile-hero {
  position: relative;
  padding: 112rpx 34rpx 34rpx;
  border-radius: 34rpx;
  background: var(--theme-gradient);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow);
  overflow: hidden;
}

.user-card {
  display: flex;
  align-items: center;
  margin-bottom: 42rpx;
}

.avatar-img {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  margin-right: 28rpx;
  border: 6rpx solid rgba(255, 255, 255, 0.96);
  box-shadow: 0 10rpx 24rpx rgba(121, 110, 176, 0.18);
}

.avatar-placeholder {
  width: 140rpx;
  height: 140rpx;
  background: var(--theme-gradient-strong);
  border-radius: 50%;
  margin-right: 28rpx;
  font-size: 56rpx;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.username {
  font-size: 40rpx;
  font-weight: 800;
  margin-bottom: 12rpx;
  color: var(--theme-ink);
}

.school {
  font-size: 24rpx;
  color: var(--theme-primary-deep);
  background: rgba(255, 255, 255, 0.82);
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  align-self: flex-start;
  margin-bottom: 10rpx;
}

.bio,
.desc {
  font-size: 26rpx;
  color: #6c6581;
  line-height: 1.6;
}

.stats {
  display: flex;
  justify-content: space-around;
  padding: 24rpx 0 4rpx;
  border-top: 1rpx solid rgba(140, 128, 216, 0.12);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.num {
  font-weight: 800;
  font-size: 38rpx;
  margin-bottom: 8rpx;
  color: var(--theme-ink);
}

.label {
  font-size: 24rpx;
  color: var(--theme-muted);
}

.menu-panel {
  margin-top: 22rpx;
}

.menu-list {
  background: rgba(255, 255, 255, 0.82);
  border: 1rpx solid rgba(140, 128, 216, 0.1);
  border-radius: 30rpx;
  overflow: hidden;
  box-shadow: var(--theme-shadow-soft);
}

.menu-item {
  padding: 34rpx 34rpx;
  border-bottom: 1rpx solid rgba(140, 128, 216, 0.08);
  font-size: 30rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.5);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-text {
  color: var(--theme-ink);
  font-size: 28rpx;
}

.arrow {
  color: var(--theme-muted);
  font-size: 36rpx;
  line-height: 1;
}

.logout {
  justify-content: center;
  background: rgba(206, 111, 139, 0.08);
}

.logout-text {
  color: #b65473;
  font-weight: 700;
}

.recent-posts {
  margin-top: 22rpx;
  background: rgba(255, 255, 255, 0.82);
  border: 1rpx solid rgba(140, 128, 216, 0.1);
  border-radius: 30rpx;
  padding: 30rpx 34rpx;
  box-shadow: var(--theme-shadow-soft);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.section-title {
  font-weight: 800;
  font-size: 30rpx;
  color: var(--theme-ink);
}

.section-meta {
  font-size: 22rpx;
  color: var(--theme-muted);
}

.mini-post {
  padding: 22rpx 0;
  border-bottom: 1rpx solid rgba(140, 128, 216, 0.08);
}

.mini-post:last-child {
  border-bottom: none;
}

.post-content {
  font-size: 28rpx;
  color: var(--theme-ink);
  margin-bottom: 10rpx;
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.post-meta {
  display: flex;
  align-items: center;
}

.post-date {
  font-size: 22rpx;
  color: var(--theme-muted);
}

.likes {
  margin-left: 20rpx;
}

.checkin-btn-wrapper {
  position: absolute;
  top: 28rpx;
  right: 28rpx;
}

.checkin-btn {
  background: var(--theme-ink);
  color: #fff;
  font-size: 24rpx;
  padding: 0 30rpx;
  height: 60rpx;
  line-height: 60rpx;
  border-radius: 999rpx;
  box-shadow: 0 8rpx 18rpx rgba(52, 48, 48, 0.16);
  border: none;
}

.checkin-btn.checked {
  background: rgba(255, 255, 255, 0.72);
  color: var(--theme-muted);
  box-shadow: none;
}

.admin-entry {
  background-color: rgba(206, 111, 139, 0.07);
}

.admin-entry .menu-text {
  color: #b65473;
  font-weight: 700;
}

.admin-entry.secondary {
  background-color: rgba(140, 128, 216, 0.08);
}

.admin-entry.secondary .menu-text {
  color: var(--theme-primary-deep);
}
</style>
