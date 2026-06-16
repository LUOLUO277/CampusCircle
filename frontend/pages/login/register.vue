<template>
  <view class="auth-page">
    <view class="page-glow page-glow-left"></view>
    <view class="page-glow page-glow-right"></view>

    <view class="topbar">
      <view class="back-btn" @click="goBack">‹</view>
      <text class="topbar-title">注册</text>
      <view class="back-placeholder"></view>
    </view>

    <view class="hero-card">
      <text class="hero-badge">New Account</text>
      <text class="hero-title">创建你的校园账号</text>
      <text class="hero-subtitle">完善基础信息后即可加入校园内容社区</text>
    </view>

    <view class="form-card">
      <view class="input-item">
        <text class="label">用户名</text>
        <input
          class="input"
          v-model="form.username"
          placeholder="请输入用户名"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="input-item">
        <text class="label">校园邮箱</text>
        <input
          class="input"
          v-model="form.email"
          placeholder="test@univ.edu.cn"
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

      <view class="input-item">
        <text class="label">确认密码</text>
        <input
          class="input"
          v-model="form.confirmPassword"
          password
          placeholder="请再次输入密码"
          placeholder-class="input-placeholder"
        />
      </view>

      <button class="submit-btn" :loading="loading" @click="handleRegister">立即注册</button>

      <view class="footer-actions">
        <text class="link" @click="goLogin">已有账号？去登录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { userApi } from '@/api/user'

const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const handleRegister = async () => {
  if (!form.username || !form.email || !form.password) {
    return uni.showToast({ title: '请补全注册信息', icon: 'none' })
  }
  if (form.password !== form.confirmPassword) {
    return uni.showToast({ title: '两次密码输入不一致', icon: 'none' })
  }

  loading.value = true
  try {
    const res = await userApi.register(form)
    if (res.code === 200) {
      uni.showToast({ title: '注册成功', icon: 'success' })
      setTimeout(() => {
        uni.navigateBack()
      }, 1000)
    }
  } catch (error) {
    console.error('注册失败', error)
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

const goLogin = () => {
  uni.navigateBack()
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
  top: 100rpx;
  left: -90rpx;
  background: rgba(185, 160, 213, 0.34);
}

.page-glow-right {
  width: 220rpx;
  height: 220rpx;
  top: 380rpx;
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
  padding: 22rpx 0;
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
  margin-top: 34rpx;
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
