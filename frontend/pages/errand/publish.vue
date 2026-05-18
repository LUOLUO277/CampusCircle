<template>
  <view class="offline-page">
    <view class="card">
      <text class="title">跑腿发布已关闭</text>
      <text class="desc">当前版本不再提供跑腿创建与接单能力，发布入口已下线，页面仅保留作为后续清理前的占位。</text>
      <button class="primary-btn" @click="goPublish">返回发布页</button>
    </view>
  </view>
</template>

<script setup>
const goPublish = () => {
  uni.navigateTo({ url: '/pages/publish/index' })
}
</script>

<style scoped>
.offline-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.card {
  width: 100%;
  background: #fff;
  border-radius: 28rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 16rpx 40rpx rgba(15, 23, 42, 0.08);
}

.title,
.desc {
  display: block;
}

.title {
  font-size: 38rpx;
  font-weight: 700;
  color: #1f2937;
}

.desc {
  margin-top: 20rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #64748b;
}

.primary-btn {
  margin-top: 36rpx;
  background: #1f5f46;
  color: #fff;
  border-radius: 999rpx;
}

.primary-btn::after {
  border: none;
}
</style>
