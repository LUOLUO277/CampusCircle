<template>
  <view class="hot-posts-card">
    <view class="card-header">
      <text class="card-title">热门话题</text>
      <text class="more-btn" @click="handleMoreClick">查看更多</text>
    </view>

    <view class="posts-container">
      <view
        class="post-item"
        v-for="post in topics"
        :key="post.id"
        @click="handlePostClick(post)"
      >
        <view class="post-content">
          <text class="topic-tag">#</text>
          <text class="post-text">
            {{ post.title.length > 25 ? post.title.slice(0, 25) + '...' : post.title }}
          </text>
          <text class="post-hot-icon">{{ post.hot ? 'HOT' : '' }}</text>
        </view>

        <view class="post-meta">
          <text class="post-views">热度 {{ post.views }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'HotTopics',
  props: {
    topics: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    handlePostClick(post) {
      this.$emit('topic-click', post)
    },
    handleMoreClick() {
      this.$emit('more-click')
    }
  }
}
</script>

<style scoped>
.hot-posts-card {
  margin: 24rpx 30rpx;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(247, 242, 255, 0.96) 100%);
  border: 1rpx solid rgba(140, 128, 216, 0.16);
  border-radius: 28rpx;
  padding: 30rpx;
  box-shadow: var(--theme-shadow-soft);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid rgba(140, 128, 216, 0.14);
}

.card-title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.more-btn {
  font-size: 26rpx;
  color: var(--theme-primary-deep);
}

.topic-tag {
  color: var(--theme-primary);
  font-size: 32rpx;
  font-weight: 700;
  margin-right: 10rpx;
}

.posts-container {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.post-item {
  padding: 22rpx 24rpx;
  background: rgba(255, 255, 255, 0.88);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  border-radius: 20rpx;
  transition: all 0.2s;
}

.post-item:active {
  transform: scale(0.99);
  background: rgba(244, 240, 252, 0.95);
}

.post-content {
  display: flex;
  align-items: center;
  margin-bottom: 15rpx;
}

.post-text {
  font-size: 28rpx;
  color: var(--theme-ink);
  flex: 1;
}

.post-hot-icon {
  font-size: 22rpx;
  color: var(--theme-primary-deep);
  margin-left: 10rpx;
  font-weight: 700;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 10rpx;
}

.post-views {
  font-size: 24rpx;
  color: var(--theme-muted);
}
</style>
