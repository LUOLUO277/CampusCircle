<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="title">{{ text.pageTitle }}</text>
        <text class="subtitle">{{ text.pageSubtitle }}</text>
      </view>
      <button class="create-btn" @click="goCreate">{{ text.createButton }}</button>
    </view>

    <scroll-view scroll-x class="filter-scroll">
      <view class="filter-row">
        <view
          v-for="item in filterOptions"
          :key="item.value"
          class="filter-pill"
          :class="{ active: filter === item.value }"
          @click="setFilter(item.value)"
        >
          {{ item.label }}
        </view>
      </view>
    </scroll-view>

    <view v-if="loading" class="state">{{ text.loading }}</view>
    <view v-else-if="!items.length" class="state">{{ text.empty }}</view>

    <view v-for="item in items" :key="item.id" class="card" @click="goEdit(item.id)">
      <view class="card-top">
        <text class="type">{{ typeLabel(item.type) }}</text>
        <text class="status" :class="{ completed: item.completed, expired: isExpired(item) }">
          {{ statusText(item) }}
        </text>
      </view>
      <text class="card-title">{{ item.title }}</text>
      <text v-if="item.description" class="card-desc">{{ item.description }}</text>
      <text class="card-deadline">{{ text.deadlinePrefix }} {{ formatTime(item.deadlineAt) }}</text>
      <view class="card-actions">
        <button class="action-btn" :class="{ secondary: item.completed }" @click.stop="toggleComplete(item)">
          {{ item.completed ? text.undoComplete : text.markComplete }}
        </button>
        <button class="action-btn danger" @click.stop="removeItem(item.id)">{{ text.deleteButton }}</button>
      </view>
    </view>
  </view>
</template>

<script>
import {
  completeScheduleItem,
  deleteScheduleItem,
  getScheduleItems,
  uncompleteScheduleItem
} from '@/api/info-center'

const TEXT = {
  pageTitle: '\u6211\u7684\u65e5\u7a0b',
  pageSubtitle: '\u628a\u901a\u77e5\u4e4b\u5916\u7684\u5f85\u529e\u4e5f\u7eb3\u5165\u8fd1 7 \u5929\u63d0\u9192',
  createButton: '\u65b0\u589e\u65e5\u7a0b',
  loading: '\u52a0\u8f7d\u4e2d...',
  empty: '\u5f53\u524d\u7b5b\u9009\u4e0b\u6682\u65e0\u65e5\u7a0b',
  deadlinePrefix: '\u622a\u6b62',
  markComplete: '\u6807\u8bb0\u5b8c\u6210',
  undoComplete: '\u53d6\u6d88\u5b8c\u6210',
  deleteButton: '\u5220\u9664',
  cancelCompleteSuccess: '\u5df2\u53d6\u6d88\u5b8c\u6210',
  completeSuccess: '\u5df2\u6807\u8bb0\u5b8c\u6210',
  deleteSuccess: '\u5df2\u5220\u9664',
  statusCompleted: '\u5df2\u5b8c\u6210',
  statusExpired: '\u5df2\u8fc7\u671f',
  statusUnfinished: '\u672a\u5b8c\u6210'
}

const FILTER_OPTIONS = [
  { value: 'unfinished', label: '\u672a\u5b8c\u6210' },
  { value: 'completed', label: '\u5df2\u5b8c\u6210' },
  { value: 'next7days', label: '\u8fd1 7 \u5929' },
  { value: 'expired', label: '\u5df2\u8fc7\u671f' },
  { value: 'all', label: '\u5168\u90e8' }
]

const TYPE_LABEL_MAP = {
  ASSIGNMENT: '\u4f5c\u4e1a',
  EXAM: '\u8003\u8bd5',
  SIGNUP: '\u62a5\u540d',
  MEETING: '\u4f1a\u8bae',
  PROJECT: '\u9879\u76ee',
  OTHER: '\u5176\u4ed6'
}

export default {
  data() {
    return {
      text: TEXT,
      filterOptions: FILTER_OPTIONS,
      loading: false,
      filter: 'unfinished',
      items: []
    }
  },
  onShow() {
    this.loadItems()
  },
  methods: {
    async loadItems() {
      this.loading = true
      try {
        const res = await getScheduleItems({ filter: this.filter })
        if (res.code === 200) {
          this.items = res.data.list || []
        }
      } finally {
        this.loading = false
      }
    },
    async setFilter(filter) {
      this.filter = filter
      await this.loadItems()
    },
    goCreate() {
      uni.navigateTo({ url: '/pages/schedule/form' })
    },
    goEdit(id) {
      uni.navigateTo({ url: `/pages/schedule/form?id=${id}` })
    },
    async toggleComplete(item) {
      const action = item.completed ? uncompleteScheduleItem : completeScheduleItem
      await action(item.id)
      uni.showToast({
        title: item.completed ? this.text.cancelCompleteSuccess : this.text.completeSuccess,
        icon: 'success'
      })
      await this.loadItems()
    },
    async removeItem(id) {
      await deleteScheduleItem(id)
      uni.showToast({ title: this.text.deleteSuccess, icon: 'success' })
      await this.loadItems()
    },
    formatTime(value) {
      return `${value || ''}`.replace('T', ' ').slice(0, 16)
    },
    typeLabel(type) {
      return TYPE_LABEL_MAP[type] || TYPE_LABEL_MAP.OTHER
    },
    isExpired(item) {
      return !item.completed && item.deadlineAt && new Date(item.deadlineAt).getTime() < Date.now()
    },
    statusText(item) {
      if (item.completed) return this.text.statusCompleted
      if (this.isExpired(item)) return this.text.statusExpired
      return this.text.statusUnfinished
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 30rpx;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.18), transparent 26%),
    linear-gradient(180deg, #faf7f2 0%, #f5f1eb 100%);
}

.hero,
.card-top,
.card-actions,
.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.hero {
  padding: 28rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.88);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow-soft);
}

.title,
.subtitle,
.card-title,
.card-desc,
.card-deadline {
  display: block;
}

.title {
  font-size: 34rpx;
  font-weight: 800;
  color: #0f172a;
}

.subtitle {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #64748b;
}

.create-btn,
.action-btn {
  margin: 0;
  border-radius: 999rpx;
  background: var(--theme-gradient-strong);
  color: #fff;
  font-size: 24rpx;
}

.action-btn.secondary {
  background: #e2e8f0;
  color: #334155;
}

.action-btn.danger {
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
}

.create-btn::after,
.action-btn::after {
  border: none;
}

.filter-scroll {
  margin: 20rpx 0;
  white-space: nowrap;
}

.filter-row {
  justify-content: flex-start;
}

.filter-pill {
  flex: 0 0 auto;
  padding: 14rpx 22rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.82);
  color: #475569;
  font-size: 24rpx;
}

.filter-pill.active {
  background: var(--theme-gradient-strong);
  color: #fff;
}

.state {
  padding: 80rpx 0;
  text-align: center;
  color: #64748b;
  font-size: 26rpx;
}

.card {
  margin-top: 18rpx;
  padding: 24rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.92);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow-soft);
}

.type,
.status {
  font-size: 22rpx;
  color: var(--theme-primary-deep);
}

.status.completed {
  color: #64748b;
}

.status.expired {
  color: #dc2626;
}

.card-title {
  margin-top: 12rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
}

.card-desc,
.card-deadline {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #64748b;
  line-height: 1.6;
}

.card-actions {
  justify-content: flex-end;
  margin-top: 18rpx;
}
</style>
