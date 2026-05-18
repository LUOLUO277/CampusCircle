<template>
  <view class="hot-posts-page">
    <view class="nav-bar">
      <text class="back-btn" @click="goBack">< 返回</text>
      <text class="nav-title">热门帖子</text>
      <view class="right-placeholder"></view>
    </view>

    <scroll-view scroll-y class="scroll-area">
      <view class="posts-container">
        <view
          v-for="post in posts"
          :key="post.id"
          class="post-item"
          @click="goDetail(post)"
        >
          <view class="post-content">
            <text class="topic-tag">#</text>
            <text class="post-text">
              {{ post.title.length > 25 ? post.title.slice(0, 25) + '...' : post.title }}
            </text>
            <text class="post-hot-icon">{{ post.hot ? '🔥' : '' }}</text>
          </view>

          <view class="post-meta">
            <text class="post-views">浏览 {{ post.views }}</text>
          </view>
        </view>
      </view>

      <view style="height: 180rpx"></view>
    </scroll-view>

    <TabBar :current-tab="'home'" @tab-change="switchTab" />
  </view>
</template>

<script>
import { getHotTopics } from '@/api/index.js'
import TabBar from '@/components/TabBar.vue'

export default {
  components: { TabBar },
  data() {
    return {
      posts: []
    }
  },
  async onLoad() {
    await this.loadHotPosts()
  },
  methods: {
    async loadHotPosts() {
      const res = await getHotTopics()
      if (res.code === 200) {
        this.posts = res.data.slice(0, 10)
      }
    },
    goBack() {
      uni.navigateBack()
    },
    goDetail(post) {
      uni.navigateTo({
        url: `/pages/post/detail?id=${post.id}`
      })
    },
    switchTab(tabId) {
      const map = {
        home: '/pages/index/index',
        info: '/pages/info-center/index',
        profile: '/pages/profile/index'
      }

      if (tabId === 'publish') {
        uni.navigateTo({ url: '/pages/publish/index' })
        return
      }

      if (map[tabId]) {
        uni.switchTab({ url: map[tabId] })
      }
    }
  }
}
</script>

<style scoped>
.hot-posts-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.nav-bar {
  height: 110rpx;
  padding: 0 30rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  border-bottom: 1rpx solid #eee;
}

.back-btn {
  font-size: 30rpx;
  color: #333;
}

.nav-title {
  font-size: 34rpx;
  font-weight: bold;
}

.right-placeholder {
  width: 60rpx;
}

.scroll-area {
  height: calc(100vh - 110rpx - 150rpx);
}

.posts-container {
  padding: 20rpx 30rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.post-item {
  padding: 20rpx;
  background-color: #fff;
  border-radius: 15rpx;
  box-shadow: 0 4rpx 15rpx rgba(0, 0, 0, 0.08);
}

.post-item:active {
  background-color: #f0f0f0;
}

.post-content {
  display: flex;
  align-items: center;
  margin-bottom: 15rpx;
}

.topic-tag {
  font-size: 32rpx;
  color: #8bc34a;
  font-weight: bold;
  margin-right: 10rpx;
}

.post-text {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.post-hot-icon {
  font-size: 30rpx;
}

.post-meta {
  margin-top: 10rpx;
  color: #999;
  font-size: 24rpx;
}
</style>
