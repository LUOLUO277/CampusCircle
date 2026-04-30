<template>
  <view class="page-container">
    <view class="profile-header">
      <!-- 签到按钮 -->
      <view class="checkin-btn-wrapper" v-if="userStore.isLoggedIn">
        <button
          class="checkin-btn"
          :class="{ 'checked': isCheckedIn }"
          @click="handleCheckIn"
          :disabled="isCheckedIn"
        >
          {{ isCheckedIn ? '已签到' : '签到 +10' }}
        </button>
      </view>

      <!-- 用户卡片 -->
      <view class="user-card" @click="handleUserCardClick">
        <image
          v-if="userStore.isLoggedIn && userStore.avatar"
          :src="userStore.avatar"
          class="avatar-img"
          mode="aspectFill"
        />
        <view v-else class="avatar-placeholder">👤</view>

        <view class="info">
          <template v-if="userStore.isLoggedIn">
            <text class="username">{{ userStore.nickname }}</text>
            <text class="school">{{ userStore.userInfo.school || '未认证学校' }}</text>
            <text class="bio">{{ userStore.userInfo.bio || '这个人很懒...' }}</text>
          </template>
          <template v-else>
            <text class="username">未登录用户</text>
            <text class="desc">点击此处登录/注册</text>
          </template>
        </view>
      </view>

      <!-- 积分数据 -->
      <view class="stats" v-if="userStore.isLoggedIn">
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

    <!-- 功能菜单 -->
    <view class="menu-list">
      <view class="menu-item" @click="handleMyPosts">
        <text>📝 我的发布</text>
        <text class="arrow">></text>
      </view>

      <view class="menu-item" @click="handleMyErrands">
        <text>🏃‍♂️ 我的跑腿</text>
        <text class="arrow">></text>
      </view>

      <!-- 跳转到我的收藏 -->
      <view class="menu-item" @click="handleMyCollections">
        <text>⭐ 我的收藏</text>
        <text class="arrow">></text>
      </view>

      <view class="menu-item" @click="handleSettings">
        <text>⚙️ 个人资料设置</text>
        <text class="arrow">></text>
      </view>

      <!-- 管理员入口 -->
      <view
        v-if="userStore.isAdmin"
        class="menu-item admin-entry"
        @click="handleAdmin"
      >
        <text>🛡️ 内容审核后台</text>
        <text class="arrow">></text>
      </view>

      <view
        v-if="userStore.isLoggedIn"
        class="menu-item logout"
        @click="handleLogout"
      >
        <text>退出登录</text>
      </view>
    </view>

    <!-- 最近发布列表 -->
    <view v-if="userStore.isLoggedIn && myPosts.length > 0" class="recent-posts">
      <view class="section-title">最近发布</view>
      <!-- 跳转到帖子详情 -->
      <view
        v-for="post in myPosts"
        :key="post.id"
        class="mini-post"
        @click="handlePostClick(post.id)"
      >
        <text class="post-content">{{ post.content }}</text>
        <view class="post-meta">
          <text class="post-date">浏览 {{ post.stats?.views || 0 }}</text>
          <text class="post-date" style="margin-left: 20rpx">❤️ {{ post.stats?.likes || 0 }}</text>
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

// 状态管理
const userStore = useUserStore()
const myPosts = ref([])
const isCheckedIn = ref(false)

// 页面显示时执行
onShow(() => {
  uni.hideTabBar()
  if (userStore.isLoggedIn) {
    refreshData()
  }
})

/**
 * 刷新用户数据（个人信息、签到状态、发布内容）
 */
const refreshData = async () => {
  try {
    const [userRes, checkInRes, postRes] = await Promise.all([
      userApi.getUserInfo(),
      userApi.getCheckInStatus(),
      userApi.getMyPosts({ page: 1, size: 5 }) // 限制只取前5条显示在主页
    ])

    if (userRes.code === 200) userStore.updateUserInfo(userRes.data)
    if (checkInRes.code === 200) isCheckedIn.value = checkInRes.data.checkedIn
    if (postRes.code === 200) myPosts.value = postRes.data.list
  } catch (e) {
    console.error('刷新数据失败', e)
  }
}

/**
 * 处理签到逻辑
 */
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

/**
 * 跳转到帖子详情页
 * @param {string|number} postId 帖子ID
 */
const handlePostClick = (postId) => {
  uni.navigateTo({ url: `/pages/post/detail?id=${postId}` })
}

/**
 * 处理用户卡片点击（登录/编辑资料）
 */
const handleUserCardClick = () => {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/login/index' })
  } else {
    uni.navigateTo({ url: '/pages/profile/edit' })
  }
}

/**
 * 跳转到个人资料设置页
 */
const handleSettings = () => {
  if (!userStore.isLoggedIn) return
  uni.navigateTo({ url: '/pages/profile/edit' })
}

/**
 * 跳转到我的跑腿页面
 */
const handleMyErrands = () => {
  if (!userStore.isLoggedIn) return uni.navigateTo({ url: '/pages/login/index' })
  uni.navigateTo({ url: '/pages/profile/my-errands' })
}

/**
 * 跳转到我的发布页面
 */
const handleMyPosts = () => {
  if (!userStore.isLoggedIn) return uni.navigateTo({ url: '/pages/login/index' })
  uni.navigateTo({ url: '/pages/profile/my-post' }) // 注意文件名一致性
}

/**
 * 跳转到我的收藏页面
 */
const handleMyCollections = () => {
  if (!userStore.isLoggedIn) return uni.navigateTo({ url: '/pages/login/index' })
  uni.navigateTo({ url: '/pages/profile/my-collection' })
}

/**
 * 跳转到粉丝列表页
 */
const handleFollowers = () => {
  if (!userStore.isLoggedIn) return
  uni.navigateTo({ url: '/pages/profile/follow-list?type=followers' })
}

/**
 * 跳转到关注列表页
 */
const goToFollowList = () => {
  if (!userStore.isLoggedIn) return
  uni.navigateTo({ url: '/pages/profile/follow-list?type=following' })
}

/**
 * 跳转到管理员审核后台
 */
const handleAdmin = () => {
  uni.navigateTo({ url: '/pages/admin/report-list' })
}

/**
 * 处理退出登录逻辑
 */
const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        myPosts.value = []
        isCheckedIn.value = false
        uni.showToast({ title: '已退出', icon: 'none' })
      }
    }
  })
}
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  background: #F5F5F5;
  padding-bottom: 160rpx;
}

.profile-header {
  background: #fff;
  padding: 100rpx 40rpx 40rpx;
  margin-bottom: 20rpx;
  position: relative; /* 为签到按钮定位 */
}

.user-card {
  display: flex;
  align-items: center;
  margin-bottom: 50rpx;
}

.avatar-img {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  margin-right: 30rpx;
  border: 4rpx solid #fff;
  box-shadow: 0 4rpx 10rpx rgba(0, 0, 0, 0.1);
}

.avatar-placeholder {
  width: 140rpx;
  height: 140rpx;
  background: #eee;
  border-radius: 50%;
  margin-right: 30rpx;
  font-size: 60rpx;
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
  font-weight: bold;
  margin-bottom: 10rpx;
  color: #333;
}

.school {
  font-size: 24rpx;
  color: #52C41A;
  background: #F6FFED;
  padding: 2rpx 10rpx;
  border-radius: 8rpx;
  align-self: flex-start;
  margin-bottom: 8rpx;
  border: 1rpx solid #B7EB8F;
}

.bio {
  font-size: 26rpx;
  color: #999;
}

.desc {
  color: #999;
  font-size: 28rpx;
}

.stats {
  display: flex;
  justify-content: space-around;
  padding-top: 20rpx;
  border-top: 1rpx solid #f5f5f5;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.num {
  font-weight: bold;
  font-size: 36rpx;
  margin-bottom: 6rpx;
  color: #333;
}

.label {
  font-size: 24rpx;
  color: #999;
}

.menu-list {
  background: #fff;
  margin-bottom: 20rpx;
}

.menu-item {
  padding: 34rpx 40rpx;
  border-bottom: 1rpx solid #f5f5f5;
  font-size: 30rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.menu-item:active {
  background-color: #fafafa;
}

.arrow {
  color: #ccc;
  font-family: monospace;
}

.logout {
  color: #ff4d4f;
  justify-content: center;
  font-weight: bold;
  margin-top: 20rpx;
}

.recent-posts {
  background: #fff;
  padding: 30rpx 40rpx;
}

.section-title {
  font-weight: bold;
  margin-bottom: 20rpx;
  font-size: 30rpx;
}

.mini-post {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #eee;
}

.post-content {
  font-size: 28rpx;
  color: #333;
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
  color: #999;
}

/* 签到按钮样式 */
.checkin-btn-wrapper {
  position: absolute;
  top: 40rpx;
  right: 40rpx;
}

.checkin-btn {
  background: linear-gradient(135deg, #FF7E5F 0%, #FEB47B 100%);
  color: #fff;
  font-size: 24rpx;
  padding: 0 30rpx;
  height: 60rpx;
  line-height: 60rpx;
  border-radius: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(254, 180, 123, 0.4);
  border: none;
}

.checkin-btn.checked {
  background: #f0f0f0;
  color: #999;
  box-shadow: none;
}

.checkin-btn::after {
  border: none;
}

/* 管理员入口样式 */
.admin-entry {
  background-color: #fff1f0; /* 给管理员入口一个特殊的淡红色背景，突出显示 */
}

.admin-entry text {
  color: #d4380d;
  font-weight: bold;
}
</style>