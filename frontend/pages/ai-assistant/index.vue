<template>
  <view class="redirect-page">
    <text class="redirect-text">正在进入 AI 校园助手...</text>
  </view>
</template>

<script>
export default {
  onLoad() {
    uni.redirectTo({ url: '/pages/ai-chat/index' })
  }
}
</script>

<style scoped>
.redirect-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.18), transparent 28%),
    linear-gradient(180deg, #faf7f2 0%, #f3eff8 100%);
}

.redirect-text {
  color: var(--theme-muted);
  font-size: 28rpx;
}
</style>
