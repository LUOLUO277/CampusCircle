<template>
  <view class="publish-page">
    <view class="page-glow page-glow-left"></view>
    <view class="page-glow page-glow-right"></view>

    <view class="topbar">
      <view class="topbar-btn" @click="goBack">×</view>
      <text class="topbar-title">内容发布站</text>
      <view class="topbar-placeholder"></view>
    </view>

    <view class="hero-card">
      <text class="hero-eyebrow">Create Something New</text>
      <text class="hero-title">把校园见闻整理成一条值得被看见的内容</text>
      <text class="hero-subtitle">选择创作类型并完善内容信息，发布后会同步到校园内容广场。</text>
    </view>

    <view class="option-panel">
      <view class="option-card post" @click="handlePublishPost">
        <view class="option-icon">✦</view>
        <view class="option-copy">
          <text class="opt-title">发布帖子</text>
          <text class="opt-desc">分享日常、二手、经验和活动记录，让更多同学看到你的内容。</text>
        </view>
        <text class="opt-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  methods: {
    goBack() {
      uni.navigateBack()
    },
    handlePublishPost() {
      uni.navigateTo({
        url: '/pages/publish/Post'
      })
    }
  }
}
</script>

<style scoped>
.publish-page {
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
  top: 80rpx;
  left: -90rpx;
  background: rgba(185, 160, 213, 0.36);
}

.page-glow-right {
  width: 200rpx;
  height: 200rpx;
  top: 220rpx;
  right: -60rpx;
  background: rgba(140, 128, 216, 0.24);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
  position: relative;
  z-index: 2;
}

.topbar-btn,
.topbar-placeholder {
  width: 72rpx;
  height: 72rpx;
}

.topbar-btn {
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(140, 128, 216, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--theme-ink);
  font-size: 44rpx;
  line-height: 1;
}

.topbar-title {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.hero-card {
  position: relative;
  z-index: 2;
  padding: 42rpx 34rpx;
  border-radius: 34rpx;
  background: var(--theme-gradient);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow);
}

.hero-eyebrow,
.hero-title,
.hero-subtitle {
  display: block;
}

.hero-eyebrow {
  font-size: 22rpx;
  text-transform: uppercase;
  letter-spacing: 2rpx;
  color: var(--theme-primary-deep);
  font-weight: 700;
}

.hero-title {
  margin-top: 18rpx;
  font-size: 50rpx;
  line-height: 1.24;
  font-weight: 800;
  color: var(--theme-ink);
}

.hero-subtitle {
  margin-top: 18rpx;
  font-size: 25rpx;
  line-height: 1.7;
  color: #6d6582;
}

.option-panel {
  position: relative;
  z-index: 2;
  margin-top: 26rpx;
}

.option-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 30rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.84);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow-soft);
}

.option-icon {
  width: 86rpx;
  height: 86rpx;
  border-radius: 24rpx;
  background: var(--theme-gradient-strong);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 38rpx;
}

.option-copy {
  flex: 1;
}

.opt-title,
.opt-desc {
  display: block;
}

.opt-title {
  font-size: 34rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.opt-desc {
  margin-top: 10rpx;
  font-size: 25rpx;
  line-height: 1.65;
  color: #716a85;
}

.opt-arrow {
  font-size: 42rpx;
  color: var(--theme-muted);
}
</style>
