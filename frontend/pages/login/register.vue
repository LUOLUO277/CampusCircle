<template>
  <view class="register-container">
	  <view class="nav-back" @click="goBack">
	    <text>←</text> <!-- 这里可以用 "←" 或者 "✕" -->
	  </view>
    <view class="header">
      <text class="title">注册账号</text>
      <text class="sub-title">加入码住校园圈，开启校园生活</text>
    </view>

    <view class="form-box">
      <!-- 用户名 -->
      <view class="input-item">
        <text class="label">用户名</text>
        <input 
          class="input" 
          v-model="form.username" 
          placeholder="请输入用户名" 
        />
      </view>

      <!-- 邮箱 -->
      <view class="input-item">
        <text class="label">校园邮箱</text>
        <input 
          class="input" 
          v-model="form.email" 
          placeholder="test@univ.edu.cn" 
        />
      </view>
      
      <!-- 密码 -->
      <view class="input-item">
        <text class="label">设置密码</text>
        <input 
          class="input" 
          v-model="form.password" 
          password 
          placeholder="请输入密码" 
        />
      </view>

      <!-- 确认密码 -->
      <view class="input-item">
        <text class="label">确认密码</text>
        <input 
          class="input" 
          v-model="form.confirmPassword" 
          password 
          placeholder="请再次输入密码" 
        />
      </view>

      <button class="submit-btn" :loading="loading" @click="handleRegister">
        立即注册
      </button>

      <view class="actions">
        <text class="link" @click="goLogin">已有账号? 去登录</text>
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
  // 1. 基础校验
  if (!form.username || !form.email || !form.password) {
    return uni.showToast({ title: '请补全注册信息', icon: 'none' })
  }
  if (form.password !== form.confirmPassword) {
    return uni.showToast({ title: '两次密码输入不一致', icon: 'none' })
  }

  loading.value = true
  try {
    // 2. 调用真实 API
    const res = await userApi.register(form)
    
    if (res.code === 200) {
      uni.showToast({ title: '注册成功', icon: 'success' })
      // 延迟跳转回登录页
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    }
  } catch (error) {
    // 错误已经在 request.js 中通过 toast 提示了，这里可以不做处理或打日志
    console.error('注册失败', error)
  } finally {
    loading.value = false
  }
}
const goBack = () => {
  const pages = getCurrentPages()
  // 如果页面栈大于1，说明有上一页，直接返回
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    // 如果没有上一页（比如直接跳转过来的），回首页
    uni.switchTab({ url: '/pages/index/index' })
  }
}

const goLogin = () => {
  uni.navigateBack()
}
</script>

<style scoped>
.register-container { padding: 60rpx; min-height: 100vh; background: #fff; }
.header { margin-top: 80rpx; margin-bottom: 60rpx; }
.title { font-size: 48rpx; font-weight: bold; display: block; margin-bottom: 20rpx; color: #333; }
.sub-title { font-size: 28rpx; color: #999; }

.input-item { margin-bottom: 30rpx; border-bottom: 1rpx solid #eee; padding-bottom: 10rpx; }
.label { font-size: 28rpx; color: #333; margin-bottom: 10rpx; display: block; font-weight: bold; }
.input { height: 80rpx; font-size: 32rpx; }

.submit-btn {
  background: #1890FF; /* 注册按钮用蓝色区分一下 */
  color: #fff; border-radius: 50rpx; 
  margin-top: 60rpx; font-size: 32rpx; font-weight: bold;
}
.submit-btn:active { opacity: 0.9; }

.actions { display: flex; justify-content: center; margin-top: 30rpx; font-size: 28rpx; color: #666; }
.link { color: #1890FF; font-weight: bold; }
.nav-back {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 100;
  /* 避开手机状态栏高度，再加一点间距 */
  padding-top: calc(var(--status-bar-height) + 20rpx);
  padding-left: 30rpx;
  padding-right: 30rpx;
  padding-bottom: 20rpx;
}

.nav-back text {
  font-size: 44rpx; /* 图标大小 */
  color: #333;      /* 图标颜色，如果是深色背景改为 #fff */
  font-weight: bold;
}
</style>