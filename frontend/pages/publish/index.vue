<template>
  <view class="publish-page">
    <view class="close-btn" @click="goBack">×</view>

    <view class="header">
      <text class="title">发布校园内容</text>
      <text class="subtitle">当前版本仅保留帖子发布，跑腿与私聊相关功能已下线。</text>
    </view>

    <view class="options-container">
      <view class="option-card post" @click="handlePublishPost">
        <view class="icon">📝</view>
        <text class="opt-title">发帖子</text>
        <text class="opt-desc">分享生活、二手信息、经验和校园动态</text>
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
  background: #ffffff;
  padding: 60rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
}

.close-btn {
  position: absolute;
  top: 80rpx;
  right: 60rpx;
  font-size: 60rpx;
  color: #333;
  line-height: 1;
}

.header {
  margin-bottom: 60rpx;
}

.title {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
  color: #333;
}

.subtitle {
  display: block;
  margin-top: 18rpx;
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}

.options-container {
  display: flex;
  flex-direction: column;
  gap: 40rpx;
}

.option-card {
  padding: 40rpx;
  border-radius: 24rpx;
  display: flex;
  flex-direction: column;
  transition: all 0.2s;
}

.option-card:active {
  transform: scale(0.98);
}

.option-card.post {
  background: #e8f5e9;
}

.icon {
  font-size: 60rpx;
  margin-bottom: 20rpx;
}

.opt-title {
  font-size: 36rpx;
  font-weight: bold;
  margin-bottom: 10rpx;
  color: #333;
}

.opt-desc {
  font-size: 26rpx;
  color: #666;
}
</style>
