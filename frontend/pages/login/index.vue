<template>
  <view class="auth-page">
    <view class="page-glow page-glow-left"></view>
    <view class="page-glow page-glow-right"></view>

    <view class="topbar">
      <view class="back-btn" @click="goBack">‹</view>
      <text class="topbar-title">登录</text>
      <view class="back-placeholder"></view>
    </view>

    <view class="hero-card">
      <text class="hero-badge">Welcome Back</text>
      <text class="hero-title">欢迎回到码住校园</text>
      <text class="hero-subtitle">登录后即可查看动态、发布内容并管理个人主页</text>
    </view>

    <view class="form-card">
      <view class="input-item">
        <text class="label">账号</text>
        <input
          class="input"
          v-model="form.username"
          placeholder="请输入学号或手机号"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="input-item">
        <text class="label">密码</text>
        <input
          class="input"
          v-model="form.password"
          password
          placeholder="请输入密码"
          placeholder-class="input-placeholder"
        />
      </view>

      <button class="submit-btn" :loading="loading" @click="handleLogin">立即登录</button>

      <view class="footer-actions">
        <text class="link" @click="toggleMode">没有账号？去注册</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!form.username || !form.password) {
    return uni.showToast({ title: '请输入账号密码', icon: 'none' })
  }

  loading.value = true
  try {
    const res = await userApi.login({
      username: form.username,
      password: form.password
    })

    if (res.code === 200) {
      if (userStore.setLoginState) {
        userStore.setLoginState(res.data)
      } else {
        uni.setStorageSync('token', res.data.token)
        uni.setStorageSync('userInfo', res.data.user)
        userStore.token = res.data.token
        userStore.userInfo = res.data.user
      }

      uni.showToast({ title: '登录成功' })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/index/index' })
      }, 800)
    }
  } catch (error) {
    uni.showToast({
      title: error.message || '登录失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.switchTab({ url: '/pages/index/index' })
  }
}

const toggleMode = () => {
  uni.navigateTo({ url: '/pages/login/register' })
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 28rpx 24rpx 60rpx;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.24), transparent 28%),
    linear-gradient(180deg, #faf7f2 0%, #f5f1eb 100%);
}

.page-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(24rpx);
  opacity: 0.42;
  pointer-events: none;
}

.page-glow-left {
  width: 260rpx;
  height: 260rpx;
  top: 120rpx;
  left: -80rpx;
  background: rgba(185, 160, 213, 0.34);
}

.page-glow-right {
  width: 220rpx;
  height: 220rpx;
  top: 340rpx;
  right: -70rpx;
  background: rgba(140, 128, 216, 0.24);
}

.topbar,
.hero-card,
.form-card {
  position: relative;
  z-index: 2;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.back-btn,
.back-placeholder {
  width: 72rpx;
  height: 72rpx;
}

.back-btn {
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(140, 128, 216, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--theme-ink);
  font-size: 44rpx;
}

.topbar-title {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.hero-card {
  padding: 38rpx 32rpx;
  border-radius: 34rpx;
  background: var(--theme-gradient);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow);
}

.hero-badge,
.hero-title,
.hero-subtitle {
  display: block;
}

.hero-badge {
  font-size: 22rpx;
  text-transform: uppercase;
  letter-spacing: 2rpx;
  color: var(--theme-primary-deep);
  font-weight: 700;
}

.hero-title {
  margin-top: 14rpx;
  font-size: 48rpx;
  line-height: 1.22;
  font-weight: 800;
  color: var(--theme-ink);
}

.hero-subtitle {
  margin-top: 14rpx;
  font-size: 25rpx;
  line-height: 1.65;
  color: #6d6582;
}

.form-card {
  margin-top: 24rpx;
  background: rgba(255, 255, 255, 0.84);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow-soft);
  border-radius: 30rpx;
  padding: 20rpx 30rpx 34rpx;
}

.input-item {
  padding: 24rpx 0;
  border-bottom: 1rpx solid rgba(140, 128, 216, 0.1);
}

.input-item:last-of-type {
  border-bottom: none;
}

.label {
  display: block;
  font-size: 26rpx;
  color: var(--theme-muted);
  margin-bottom: 10rpx;
}

.input {
  height: 72rpx;
  font-size: 30rpx;
  color: var(--theme-ink);
}

.input-placeholder {
  color: #a09aaf;
}

.submit-btn {
  margin-top: 38rpx;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 999rpx;
  background: var(--theme-ink);
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
  border: none;
}

.submit-btn::after {
  border: none;
}

.footer-actions {
  margin-top: 24rpx;
  text-align: center;
}

.link {
  color: var(--theme-primary-deep);
  font-size: 26rpx;
}
</style>
