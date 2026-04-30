<template>
  <view class="hot-posts-card">
    <!-- 标题 -->
    <view class="card-header">
      <text class="card-title">🔥 热门帖子</text>
      <text class="more-btn" @click="handleMoreClick">查看更多 ›</text>
    </view>

    <!-- 热门帖子列表 -->
    <view class="posts-container">
      <view
        class="post-item"
        v-for="post in topics"
        :key="post.id"
        @click="handlePostClick(post)"
      >
        <!-- 内容区 -->
        <view class="post-content">
		  <text class="topic-tag">#</text>
          <text class="post-text">
            {{ post.title.length > 25 ? post.title.slice(0, 25) + '...' : post.title }}

          </text>
          <text class="post-hot-icon">{{ post.hot ? '🔥' : '' }}</text>
        </view>

        <!-- 底部元信息 -->
        <view class="post-meta">
          <text class="post-views">点赞 {{ post.views }}</text>
          
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: "HotTopics",
  props: {
    topics: {
      type: Array,
      default: () => [],
    },
  },
  methods: {
    // 点击热门帖子 → 通知父组件
    handlePostClick(post) {
      this.$emit("topic-click", post); // 首页已经在监听这个事件
    },
    // 查看更多
    handleMoreClick() {
      this.$emit("more-click");
    },
  },
};
</script>

<style scoped>
.hot-posts-card {
  margin: 20rpx 30rpx;
  background-color: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 15rpx rgba(0, 0, 0, 0.08);
}

/* 标题区 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25rpx;
  padding-bottom: 20rpx;
  border-bottom: 2rpx solid #f0f0f0;
}

.card-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.more-btn {
  font-size: 26rpx;
  color: #999;
}
.topic-tag { color: #8bc34a; font-size: 32rpx; font-weight: bold; margin-right: 10rpx; }

/* 内容列表 */
.posts-container {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.post-item {
  padding: 20rpx;
  background-color: #fafafa;
  border-radius: 15rpx;
  transition: all 0.2s;
}

.post-item:active {
  background-color: #f0f0f0;
}

/* 内容文本 */
.post-content {
  display: flex;
  align-items: center;
  margin-bottom: 15rpx;
}

.post-text {
  font-size: 28rpx;
  color: #333;
  flex: 1;
}

.post-hot-icon {
  font-size: 30rpx;
  margin-left: 10rpx;
}

/* 底部数据 */
.post-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 10rpx;
}

.post-views,
.post-likes {
  font-size: 24rpx;
  color: #999;
}
</style>
