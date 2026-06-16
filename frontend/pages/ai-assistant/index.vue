<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">AI 助手</text>
      <text class="hero-subtitle">校园通知问答与帖子智能搜索</text>
    </view>

    <view class="tabs">
      <view class="tab" :class="{ active: tab === 'notice' }" @click="tab = 'notice'">通知问答</view>
      <view class="tab" :class="{ active: tab === 'post' }" @click="tab = 'post'">帖子搜索</view>
    </view>

    <view class="panel">
      <input
        v-model="question"
        class="input"
        :placeholder="tab === 'notice' ? '请输入你想查询的校园通知问题' : '请输入你想搜索的帖子问题'"
      />
      <button class="btn" :disabled="loading" @click="runQuery">{{ loading ? '查询中...' : '开始查询' }}</button>
    </view>

    <view v-if="answer" class="answer">{{ answer }}</view>

    <view v-if="tab === 'notice'">
      <view v-for="item in noticeItems" :key="item.id" class="card">
        <text class="title">{{ item.title }}</text>
        <text class="meta">发布时间：{{ item.publishTime }}</text>
        <text class="meta">分类：{{ item.category || '其他' }}</text>
        <text class="text">摘要：{{ item.summary }}</text>
        <text class="text">匹配原因：{{ item.reason }}</text>
      </view>
      <view v-if="!loading && queried && noticeItems.length === 0" class="empty">未找到相关通知</view>
    </view>

    <view v-else>
      <view v-for="item in postItems" :key="item.id" class="card" @click="goPostDetail(item.id)">
        <text class="title">{{ item.title }}</text>
        <text class="meta">作者：{{ item.authorName }}</text>
        <text class="meta">发布时间：{{ item.createdAt }}</text>
        <text class="meta">帖子ID：{{ item.id }}</text>
        <text class="text">摘要：{{ item.summary }}</text>
        <text class="text">匹配原因：{{ item.reason }}</text>
      </view>
      <view v-if="!loading && queried && postItems.length === 0" class="empty">未找到相关帖子</view>
    </view>
  </view>
</template>

<script>
import { queryAiNotices, searchAiPosts } from '@/api/ai'

export default {
  data() {
    return {
      tab: 'notice',
      question: '',
      answer: '',
      noticeItems: [],
      postItems: [],
      loading: false,
      queried: false
    }
  },
  methods: {
    async runQuery() {
      if (!this.question.trim()) {
        uni.showToast({ title: '请输入问题', icon: 'none' })
        return
      }
      this.loading = true
      this.queried = false
      try {
        if (this.tab === 'notice') {
          const res = await queryAiNotices(this.question.trim())
          this.answer = res?.data?.answer || ''
          this.noticeItems = res?.data?.matchedNotices || []
        } else {
          const res = await searchAiPosts(this.question.trim())
          this.answer = res?.data?.answer || ''
          this.postItems = res?.data?.matchedPosts || []
        }
      } finally {
        this.loading = false
        this.queried = true
      }
    },
    goPostDetail(id) {
      uni.navigateTo({ url: `/pages/post/detail?id=${id}` })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.18), transparent 26%),
    linear-gradient(180deg, #faf7f2 0%, #f5f1eb 100%);
  padding: 24rpx;
}

.hero {
  background: var(--theme-gradient);
  border-radius: 30rpx;
  padding: 30rpx;
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow-soft);
  margin-bottom: 20rpx;
}

.hero-title {
  display: block;
  font-size: 38rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.hero-subtitle {
  display: block;
  margin-top: 10rpx;
  color: var(--theme-muted);
  font-size: 24rpx;
}

.tabs {
  display: flex;
  gap: 14rpx;
  margin-bottom: 18rpx;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 14rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.85);
  color: var(--theme-ink);
  border: 1rpx solid rgba(140, 128, 216, 0.1);
}

.tab.active {
  background: var(--theme-gradient-strong);
  color: #fff;
  border-color: transparent;
}

.panel {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20rpx;
  padding: 20rpx;
  border: 1rpx solid rgba(140, 128, 216, 0.1);
  margin-bottom: 18rpx;
}

.input {
  height: 84rpx;
  border: 1rpx solid rgba(140, 128, 216, 0.2);
  border-radius: 14rpx;
  padding: 0 18rpx;
  background: #fff;
  margin-bottom: 14rpx;
}

.btn {
  background: var(--theme-ink);
  color: #fff;
  border-radius: 14rpx;
  font-size: 26rpx;
}

.btn::after {
  border: none;
}

.answer {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 16rpx;
  padding: 16rpx;
  border: 1rpx solid rgba(140, 128, 216, 0.1);
  color: var(--theme-ink);
  margin-bottom: 16rpx;
}

.card {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 16rpx;
  padding: 18rpx;
  margin-bottom: 14rpx;
  border: 1rpx solid rgba(140, 128, 216, 0.1);
}

.title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.meta {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: var(--theme-muted);
}

.text {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #2f3241;
}

.empty {
  text-align: center;
  color: var(--theme-muted);
  padding: 80rpx 0;
}
</style>
