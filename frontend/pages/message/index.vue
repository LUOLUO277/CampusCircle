<template>
  <view class="offline-page">
    <view class="card">
      <text class="eyebrow">Message Module</text>
      <text class="title">消息会话模块已下线</text>
      <text class="desc">当前版本不再提供私聊与消息会话能力。为了保持产品边界清晰，这里统一跳转到信息订阅中心。</text>
      <button class="primary-btn" @click="goInfoCenter">前往信息订阅中心</button>
    </view>
  </view>
</template>

<script setup>
const goInfoCenter = () => {
  uni.switchTab({ url: '/pages/info-center/index' })
}
</script>

<style scoped>
.offline-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.18), transparent 26%),
    linear-gradient(180deg, #faf7f2 0%, #f4efe9 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.card {
  width: 100%;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(247, 242, 255, 0.96) 100%);
  border: 1rpx solid rgba(140, 128, 216, 0.14);
  border-radius: 34rpx;
  padding: 52rpx 40rpx;
  box-shadow: var(--theme-shadow);
}

.eyebrow,
.title,
.desc {
  display: block;
}

.eyebrow {
  font-size: 22rpx;
  letter-spacing: 2rpx;
  text-transform: uppercase;
  color: var(--theme-primary-deep);
  font-weight: 700;
}

.title {
  margin-top: 18rpx;
  font-size: 40rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.desc {
  margin-top: 20rpx;
  font-size: 26rpx;
  line-height: 1.75;
  color: #6a647d;
}

.primary-btn {
  margin-top: 40rpx;
  background: var(--theme-gradient-strong);
  color: #fff;
  border-radius: 999rpx;
  height: 84rpx;
  line-height: 84rpx;
  font-size: 28rpx;
  font-weight: 700;
  box-shadow: 0 12rpx 28rpx rgba(121, 110, 176, 0.22);
}
</style>
