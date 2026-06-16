<template>
  <view class="edit-page">
    <view class="page-glow page-glow-left"></view>
    <view class="page-glow page-glow-right"></view>

    <view class="topbar">
      <view class="back-btn" @click="goBack">‹</view>
      <text class="topbar-title">编辑资料</text>
      <view class="back-placeholder"></view>
    </view>

    <view class="profile-card">
      <view class="avatar-row" @click="handleChooseAvatar">
        <text class="label">头像</text>
        <view class="avatar-right">
          <image :src="form.avatarUrl || '/static/logo.png'" class="avatar-preview" mode="aspectFill" />
          <text class="arrow">›</text>
        </view>
      </view>

      <view class="form-item">
        <text class="label">昵称</text>
        <input class="input" v-model="form.nickname" placeholder="请输入昵称" placeholder-class="input-placeholder" />
      </view>

      <view class="form-item">
        <text class="label">学院</text>
        <input class="input" v-model="form.school" placeholder="例如：计算机学院" placeholder-class="input-placeholder" />
      </view>

      <view class="form-item">
        <text class="label">个性签名</text>
        <input class="input" v-model="form.bio" placeholder="介绍一下自己吧" placeholder-class="input-placeholder" />
      </view>
    </view>

    <button class="save-btn" @click="handleSave" :loading="loading">保存修改</button>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import { commonApi } from '@/api/common'

const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  avatarUrl: '',
  nickname: '',
  school: '',
  bio: ''
})

onMounted(() => {
  const info = userStore.userInfo
  if (info) {
    form.avatarUrl = info.avatarUrl
    form.nickname = info.nickname
    form.school = info.school
    form.bio = info.bio
  }
})

const handleChooseAvatar = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: async (res) => {
      const tempFilePath = res.tempFilePaths[0]
      form.avatarUrl = tempFilePath
      try {
        uni.showLoading({ title: '上传中...' })
        const uploadRes = await commonApi.uploadImage(tempFilePath)
        if (uploadRes.code === 200) {
          form.avatarUrl = uploadRes.data.url
        }
      } catch (error) {
        uni.showToast({ title: '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

const goBack = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.switchTab({ url: '/pages/index/index' })
  }
}

const handleSave = async () => {
  if (!form.nickname) return uni.showToast({ title: '昵称不能为空', icon: 'none' })

  loading.value = true
  try {
    const payload = {
      avatarUrl: (form.avatarUrl || '').trim(),
      nickname: (form.nickname || '').trim(),
      school: (form.school || '').trim(),
      bio: (form.bio || '').trim()
    }
    const res = await userApi.updateProfile(payload)
    if (res.code === 200) {
      uni.showToast({ title: '保存成功' })
      setTimeout(() => uni.navigateBack(), 800)
    }
  } catch (error) {
    uni.showToast({ title: error.message || '保存失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.edit-page {
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
.profile-card,
.save-btn {
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

.profile-card {
  background: rgba(255, 255, 255, 0.84);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow-soft);
  border-radius: 30rpx;
  padding: 0 30rpx;
}

.avatar-row,
.form-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx solid rgba(140, 128, 216, 0.1);
}

.avatar-row {
  padding: 28rpx 0;
}

.form-item {
  padding: 24rpx 0;
}

.form-item:last-child {
  border-bottom: none;
}

.label {
  width: 170rpx;
  font-size: 28rpx;
  color: var(--theme-ink);
}

.avatar-right {
  display: flex;
  align-items: center;
}

.avatar-preview {
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  margin-right: 16rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.96);
  box-shadow: 0 8rpx 18rpx rgba(121, 110, 176, 0.18);
}

.arrow {
  font-size: 34rpx;
  color: var(--theme-muted);
}

.input {
  flex: 1;
  height: 72rpx;
  text-align: right;
  font-size: 28rpx;
  color: var(--theme-ink);
}

.input-placeholder {
  color: #a09aaf;
}

.save-btn {
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

.save-btn::after {
  border: none;
}
</style>
