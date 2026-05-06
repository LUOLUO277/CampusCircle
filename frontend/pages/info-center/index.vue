<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="title">信息订阅中心</text>
        <text class="subtitle">统一查看公众号、教务和 Canvas 通知</text>
      </view>
      <view class="hero-actions">
        <button class="ghost-btn" @click="goSubscriptions">管理订阅</button>
      </view>
    </view>

    <view class="toolbar">
      <input v-model="filters.keyword" class="search-input" placeholder="搜索标题、摘要、关键词" @confirm="loadNotices" />
      <picker class="picker" :range="categories" range-key="label" @change="onCategoryChange">
        <view class="picker-value">{{ currentCategoryLabel }}</view>
      </picker>
    </view>

    <view class="toolbar">
      <picker class="picker source-picker" :range="sourceOptions" range-key="name" @change="onSourceChange">
        <view class="picker-value">{{ currentSourceLabel }}</view>
      </picker>
      <label class="subscribed-switch">
        <switch :checked="!!filters.onlySubscribed" color="#2f855a" @change="onSubscribedToggle" />
        <text>只看已订阅</text>
      </label>
    </view>

    <view class="list">
      <view v-for="notice in notices" :key="notice.id" class="notice-card" @click="goDetail(notice.id)">
        <view class="notice-header">
          <text class="badge">{{ notice.category || '通知' }}</text>
          <text class="source">{{ notice.sourceName }}</text>
        </view>
        <text class="notice-title">{{ notice.title }}</text>
        <text v-if="notice.summary" class="notice-summary">{{ notice.summary }}</text>
        <view class="meta-row">
          <text class="meta-text">发布时间 {{ formatTime(notice.publishTime) }}</text>
          <text v-if="notice.deadline" class="deadline">截止 {{ formatTime(notice.deadline) }}</text>
        </view>
        <view v-if="notice.tags && notice.tags.length" class="tag-row">
          <text v-for="tag in notice.tags" :key="tag" class="tag">{{ tag }}</text>
        </view>
      </view>

      <view v-if="!notices.length" class="empty">暂无通知，先去订阅来源或触发后台抓取。</view>
    </view>
  </view>
</template>

<script>
import { getInfoNotices, getInfoSources } from '@/api/info-center'

export default {
  data() {
    return {
      notices: [],
      sources: [],
      categories: [
        { value: '', label: '全部分类' },
        { value: '通知', label: '通知' },
        { value: '截止提醒', label: '截止提醒' },
        { value: '活动', label: '活动' },
        { value: '报名', label: '报名' }
      ],
      filters: {
        category: '',
        sourceId: '',
        keyword: '',
        onlySubscribed: false
      }
    }
  },
  computed: {
    sourceOptions() {
      return [{ id: '', name: '全部来源' }, ...this.sources]
    },
    currentCategoryLabel() {
      return this.categories.find(item => item.value === this.filters.category)?.label || '全部分类'
    },
    currentSourceLabel() {
      return this.sourceOptions.find(item => `${item.id}` === `${this.filters.sourceId}`)?.name || '全部来源'
    }
  },
  onLoad() {
    this.bootstrap()
  },
  methods: {
    async bootstrap() {
      await Promise.all([this.loadSources(), this.loadNotices()])
    },
    async loadSources() {
      const res = await getInfoSources()
      if (res.code === 200) {
        this.sources = res.data || []
      }
    },
    async loadNotices() {
      const res = await getInfoNotices({
        ...this.filters,
        page: 1,
        pageSize: 20
      })
      if (res.code === 200) {
        this.notices = res.data.list || []
      }
    },
    onCategoryChange(event) {
      this.filters.category = this.categories[event.detail.value].value
      this.loadNotices()
    },
    onSourceChange(event) {
      this.filters.sourceId = this.sourceOptions[event.detail.value].id
      this.loadNotices()
    },
    onSubscribedToggle(event) {
      this.filters.onlySubscribed = !!event.detail.value
      this.loadNotices()
    },
    goSubscriptions() {
      uni.navigateTo({ url: '/pages/info-center/subscriptions' })
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/info-center/detail?id=${id}` })
    },
    formatTime(value) {
      if (!value) return ''
      return value.replace('T', ' ').slice(0, 16)
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef6ef 0%, #f7f7f7 28%, #f7f7f7 100%);
  padding: 30rpx;
}
.hero {
  background: linear-gradient(135deg, #1f5f46 0%, #2f855a 60%, #b7d7b0 100%);
  border-radius: 32rpx;
  padding: 36rpx;
  color: #fff;
  margin-bottom: 24rpx;
}
.title,
.subtitle,
.notice-title,
.notice-summary,
.source,
.meta-text,
.badge,
.deadline,
.tag,
.empty {
  display: block;
}
.title {
  font-size: 40rpx;
  font-weight: 700;
}
.subtitle {
  margin-top: 12rpx;
  font-size: 24rpx;
  opacity: 0.9;
}
.hero-actions {
  margin-top: 24rpx;
}
.ghost-btn {
  margin: 0;
  width: 220rpx;
  height: 72rpx;
  line-height: 72rpx;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  border: 1rpx solid rgba(255, 255, 255, 0.35);
  border-radius: 999rpx;
  font-size: 26rpx;
}
.ghost-btn::after {
  border: none;
}
.toolbar {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
  align-items: center;
}
.search-input,
.picker {
  background: #fff;
  border-radius: 24rpx;
  min-height: 88rpx;
  box-sizing: border-box;
}
.search-input {
  flex: 1;
  padding: 0 26rpx;
}
.picker {
  min-width: 220rpx;
  padding: 24rpx 26rpx;
}
.source-picker {
  flex: 1;
}
.picker-value {
  color: #1f2937;
  font-size: 26rpx;
}
.subscribed-switch {
  display: flex;
  align-items: center;
  gap: 12rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 16rpx 20rpx;
  font-size: 24rpx;
  color: #334155;
}
.notice-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 12rpx 30rpx rgba(15, 23, 42, 0.05);
}
.notice-header,
.meta-row,
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  align-items: center;
}
.notice-header {
  margin-bottom: 16rpx;
}
.badge {
  background: #edf7ee;
  color: #1f5f46;
  border-radius: 999rpx;
  padding: 6rpx 18rpx;
  font-size: 22rpx;
}
.source,
.meta-text {
  font-size: 22rpx;
  color: #64748b;
}
.notice-title {
  font-size: 32rpx;
  line-height: 1.5;
  color: #0f172a;
  font-weight: 600;
}
.notice-summary {
  margin-top: 14rpx;
  color: #475569;
  font-size: 25rpx;
  line-height: 1.7;
}
.meta-row {
  margin-top: 18rpx;
  justify-content: space-between;
}
.deadline {
  color: #b45309;
  font-size: 22rpx;
}
.tag-row {
  margin-top: 18rpx;
}
.tag {
  background: #f1f5f9;
  color: #334155;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
}
.empty {
  text-align: center;
  color: #64748b;
  padding: 120rpx 30rpx;
  font-size: 28rpx;
}
</style>
