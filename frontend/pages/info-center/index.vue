<template>
  <view class="page">
    <view class="nav-bar">
      <text class="back-btn" @click="goHome">< 返回</text>
      <text class="nav-title">信息订阅中心</text>
      <text class="nav-action" @click="goSchedule">我的日程</text>
    </view>

    <view class="hero">
      <view>
        <text class="title">信息订阅中心</text>
        <text class="subtitle">默认先看近期通知，继续滑动再加载更早内容</text>
      </view>
    </view>

    <view class="toolbar">
      <input
        v-model="filters.keyword"
        class="search-input"
        placeholder="搜索标题、摘要、关键词"
        @confirm="reloadNotices"
      />
      <picker class="picker" :range="sources" range-key="label" @change="onSourceChange">
        <view class="picker-value">{{ currentSourceLabel }}</view>
      </picker>
    </view>

    <view class="toolbar toolbar-view">
      <view class="view-toggle">
        <button class="toggle-btn" :class="{ active: viewMode === 'cards' }" @click="setViewMode('cards')">卡片</button>
        <button class="toggle-btn" :class="{ active: viewMode === 'calendar' }" @click="setViewMode('calendar')">日历</button>
      </view>
      <text class="toolbar-link" @click="goSubscriptions">订阅管理</text>
    </view>

    <scroll-view scroll-x class="filter-scroll">
      <view class="filter-row">
        <view
          v-for="item in filterOptions"
          :key="item.value"
          class="filter-pill"
          :class="{ active: filters.filter === item.value }"
          @click="setFilter(item.value)"
        >
          {{ item.label }}
        </view>
      </view>
    </scroll-view>

    <view v-if="viewMode === 'cards'" class="list">
      <view v-if="initialLoading" class="state-card">Loading...</view>

      <view
        v-for="notice in cardNotices"
        :key="notice.id"
        class="notice-card"
        @click="goDetail(notice.id)"
      >
        <view class="notice-header">
          <text class="badge">{{ notice.category || '通知' }}</text>
          <text class="source">{{ notice.sourceName || '订阅来源' }}</text>
        </view>

        <text class="notice-title">{{ notice.title }}</text>
        <text v-if="notice.summary" class="notice-summary">{{ notice.summary }}</text>

        <view class="status-chip-row">
          <text v-if="notice.completed" class="status-chip muted">已完成</text>
          <text v-else-if="notice.expired" class="status-chip danger">已过期</text>
          <text v-else-if="notice.dueSoon" class="status-chip warn">即将截止</text>
          <text v-else-if="notice.hasDeadline" class="status-chip">有 DDL</text>
        </view>

        <view class="meta-row">
          <text class="meta-text">发布时间 {{ formatTime(notice.publishTime) }}</text>
          <text v-if="notice.deadline" class="deadline">截止 {{ formatTime(notice.deadline) }}</text>
        </view>

        <view v-if="notice.tags && notice.tags.length" class="tag-row">
          <text v-for="tag in notice.tags" :key="tag" class="tag">{{ tag }}</text>
        </view>

        <view v-if="hasToken" class="card-action-row">
          <button
            class="card-action-btn"
            :class="{ secondary: notice.completed }"
            @click.stop="toggleNoticeCompleted(notice)"
          >
            {{ notice.completed ? '取消完成' : '标记完成' }}
          </button>
        </view>
      </view>

      <view v-if="!initialLoading && !cardNotices.length" class="state-card">暂无通知</view>
      <view v-else-if="loadingMore" class="state-card">正在加载更早通知...</view>
      <view v-else-if="!hasMore && cardNotices.length" class="state-card">已经到底了</view>
    </view>

    <view v-else class="calendar">
      <view class="cal-header">
        <text class="cal-title">{{ calendarTitle }}</text>
        <view class="cal-header-actions">
          <button class="cal-btn" @click="jumpToday">今天</button>
          <view class="cal-nav">
            <button class="cal-icon" @click="shiftMonth(-1)">&lt;</button>
            <button class="cal-icon" @click="shiftMonth(1)">&gt;</button>
          </view>
        </view>
      </view>

      <view class="cal-weekdays">
        <text v-for="w in weekdays" :key="w" class="cal-weekday">{{ w }}</text>
      </view>

      <view class="cal-grid">
        <view
          v-for="day in calendarDays"
          :key="day.key"
          class="cal-cell"
          :class="{ muted: !day.inMonth, today: day.isToday, selected: selectedDateKey === day.key }"
          @click="selectDate(day.key)"
        >
          <text class="cal-date">{{ day.dateNum }}</text>
          <view class="cal-events">
            <view
              v-for="evt in day.events.slice(0, 2)"
              :key="evt.id"
              class="cal-event"
              :class="{ deadline: !!evt.deadline }"
              @click.stop="goDetail(evt.id)"
            >
              <view class="cal-event-icon" :class="{ deadline: !!evt.deadline }"></view>
              <text class="cal-event-text">{{ evt.title }}</text>
            </view>
            <text v-if="day.events.length > 2" class="cal-more">+{{ day.events.length - 2 }}</text>
          </view>
        </view>
      </view>

      <view v-if="selectedDayEvents.length" class="day-panel">
        <view class="day-panel-header">
          <text class="day-panel-title">{{ selectedDateLabel }}</text>
          <text class="day-panel-sub">{{ selectedDayEvents.length }} items</text>
        </view>
        <view v-for="notice in selectedDayEvents" :key="notice.id" class="day-item" @click="goDetail(notice.id)">
          <view class="notice-header">
          <text class="badge">{{ notice.category || '通知' }}</text>
          <text class="source">{{ notice.sourceName || '订阅来源' }}</text>
          </view>
          <text class="notice-title">{{ notice.title }}</text>
          <view class="meta-row">
            <text class="meta-text">发布时间 {{ formatTime(notice.publishTime) }}</text>
            <text v-if="notice.deadline" class="deadline">截止 {{ formatTime(notice.deadline) }}</text>
          </view>
        </view>
      </view>

      <view v-if="!visibleNotices.length" class="state-card">当前数据下暂无日历项</view>
    </view>

    <view class="bottom-space"></view>
    <TabBar :current-tab="'info'" />
  </view>
</template>

<script>
import TabBar from '@/components/TabBar.vue'
import { completeInfoNotice, getInfoNotices, uncompleteInfoNotice } from '@/api/info-center'

export default {
  components: { TabBar },
  data() {
    return {
      viewMode: 'cards',
      notices: [],
      page: 1,
      pageSize: 20,
      hasMore: true,
      initialLoading: false,
      loadingMore: false,
      sources: [
        { value: '', label: '全部来源' },
        { value: 'canvas', label: 'Canvas' },
        { value: 'tongji', label: '同济一网通办' }
      ],
      filterOptions: [
        { value: 'all', label: '全部' },
        { value: 'unfinished', label: '未完成' },
        { value: 'completed', label: '已完成' },
        { value: 'hasDeadline', label: '有 DDL' },
        { value: 'next7days', label: '近 7 天' },
        { value: 'expired', label: '已过期' }
      ],
      filters: {
        source: '',
        keyword: '',
        filter: 'all'
      },
      calendarCursor: null,
      selectedDateKey: ''
    }
  },
  computed: {
    hasToken() {
      return !!uni.getStorageSync('token')
    },
    currentSourceLabel() {
      return this.sources.find(item => item.value === this.filters.source)?.label || '全部来源'
    },
    visibleNotices() {
      if (!this.filters.source) return this.notices
      return this.notices.filter(notice => this.matchSource(notice, this.filters.source))
    },
    cardNotices() {
      return [...this.visibleNotices].sort((a, b) => {
        const at = this.parseIsoDate(a.deadline || a.publishTime)?.getTime() || 0
        const bt = this.parseIsoDate(b.deadline || b.publishTime)?.getTime() || 0
        return bt - at
      })
    },
    weekdays() {
      return ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    calendarTitle() {
      const cursor = this.calendarCursor || new Date()
      return `${cursor.getFullYear()}年 ${String(cursor.getMonth() + 1).padStart(2, '0')}月`
    },
    noticeEvents() {
      return this.visibleNotices
        .map(notice => {
          const dt = this.parseIsoDate(notice.deadline || notice.publishTime)
          if (!dt) return null
          return { key: this.dateKey(dt), notice }
        })
        .filter(Boolean)
    },
    calendarDays() {
      const cursor = this.calendarCursor || new Date()
      const year = cursor.getFullYear()
      const month = cursor.getMonth()
      const firstOfMonth = new Date(year, month, 1)
      const jsDay = firstOfMonth.getDay()
      const startOffset = (jsDay + 6) % 7
      const start = new Date(year, month, 1 - startOffset)
      const eventsByKey = new Map()

      for (const evt of this.noticeEvents) {
        const list = eventsByKey.get(evt.key) || []
        list.push(evt.notice)
        eventsByKey.set(evt.key, list)
      }

      const todayKey = this.dateKey(new Date())
      const days = []
      for (let i = 0; i < 42; i++) {
        const d = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i)
        const key = this.dateKey(d)
        days.push({
          key,
          inMonth: d.getMonth() === month,
          isToday: key === todayKey,
          dateNum: d.getDate(),
          events: (eventsByKey.get(key) || []).slice(0, 4)
        })
      }
      return days
    },
    selectedDayEvents() {
      if (!this.selectedDateKey) return []
      return this.calendarDays.find(item => item.key === this.selectedDateKey)?.events || []
    },
    selectedDateLabel() {
      return this.selectedDateKey || ''
    }
  },
  onLoad() {
    this.viewMode = uni.getStorageSync('infoCenterViewMode') || 'cards'
    const now = new Date()
    this.calendarCursor = new Date(now.getFullYear(), now.getMonth(), 1)
    this.selectedDateKey = this.dateKey(now)
    this.reloadNotices()
  },
  onShow() {
    uni.hideTabBar({ animation: false })
  },
  onReachBottom() {
    if (this.viewMode !== 'cards' || this.loadingMore || !this.hasMore) return
    this.loadNotices(false)
  },
  methods: {
    setViewMode(mode) {
      this.viewMode = mode
      uni.setStorageSync('infoCenterViewMode', mode)
    },
    async reloadNotices() {
      this.page = 1
      this.hasMore = true
      this.notices = []
      await this.loadNotices(true)
    },
    async loadNotices(reset = false) {
      if (!this.hasMore && !reset) return
      if (reset) {
        this.initialLoading = true
      } else {
        this.loadingMore = true
      }
      try {
        const res = await getInfoNotices({
          keyword: this.filters.keyword,
          filter: this.filters.filter,
          page: this.page,
          pageSize: this.pageSize
        })
        if (res.code === 200) {
          const list = res.data.list || []
          this.notices = reset ? list : [...this.notices, ...list]
          this.hasMore = !!res.data.hasMore
          if (this.hasMore) this.page += 1
        }
      } finally {
        this.initialLoading = false
        this.loadingMore = false
      }
    },
    onSourceChange(event) {
      this.filters.source = this.sources[event.detail.value].value
    },
    async setFilter(filter) {
      this.filters.filter = filter
      await this.reloadNotices()
    },
    goHome() {
      uni.switchTab({ url: '/pages/index/index' })
    },
    goSubscriptions() {
      uni.navigateTo({ url: '/pages/info-center/subscriptions' })
    },
    goSchedule() {
      uni.navigateTo({ url: '/pages/schedule/index' })
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/info-center/detail?id=${id}` })
    },
    async toggleNoticeCompleted(notice) {
      const wasCompleted = !!notice.completed
      const action = wasCompleted ? uncompleteInfoNotice : completeInfoNotice
      await action(notice.id)
      uni.showToast({
        title: wasCompleted ? '已取消完成' : '已标记完成',
        icon: 'success'
      })
      await this.reloadNotices()
    },
    matchSource(notice = {}, source = '') {
      const sourceName = `${notice.sourceName || ''}`
      if (source === 'canvas') return /canvas/i.test(sourceName)
      if (source === 'tongji') return /tongji|同济|一网通办|1系统|1 系统/i.test(sourceName)
      return true
    },
    formatTime(value) {
      if (!value) return ''
      return `${value}`.replace('T', ' ').slice(0, 16)
    },
    parseIsoDate(value) {
      if (!value) return null
      const normalized = `${value}`.replace('T', ' ')
      const datePart = normalized.slice(0, 10)
      const timePart = normalized.length >= 16 ? normalized.slice(11, 16) : '00:00'
      if (!/^\d{4}-\d{2}-\d{2}$/.test(datePart)) return null
      const [yy, mm, dd] = datePart.split('-').map(v => parseInt(v, 10))
      const [hh, mi] = timePart.split(':').map(v => parseInt(v, 10))
      return new Date(yy, (mm || 1) - 1, dd || 1, hh || 0, mi || 0, 0, 0)
    },
    dateKey(date) {
      const y = date.getFullYear()
      const m = `${date.getMonth() + 1}`.padStart(2, '0')
      const d = `${date.getDate()}`.padStart(2, '0')
      return `${y}-${m}-${d}`
    },
    selectDate(key) {
      this.selectedDateKey = key
    },
    shiftMonth(delta) {
      const cursor = this.calendarCursor || new Date()
      this.calendarCursor = new Date(cursor.getFullYear(), cursor.getMonth() + delta, 1)
    },
    jumpToday() {
      const now = new Date()
      this.calendarCursor = new Date(now.getFullYear(), now.getMonth(), 1)
      this.selectedDateKey = this.dateKey(now)
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
  padding: 24rpx 24rpx 170rpx;
}

.nav-bar {
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10rpx;
}

.back-btn,
.nav-action {
  width: 160rpx;
  font-size: 28rpx;
  color: var(--theme-primary-deep);
}

.nav-action {
  text-align: right;
}

.nav-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}

.hero {
  background: var(--theme-gradient);
  border-radius: 32rpx;
  padding: 36rpx;
  color: var(--theme-ink);
  margin-bottom: 24rpx;
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow);
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
.state-card {
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

.toolbar {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
  align-items: center;
}

.toolbar-view {
  justify-content: flex-end;
}

.view-toggle {
  display: flex;
  background: rgba(255, 255, 255, 0.82);
  border-radius: 999rpx;
  padding: 8rpx;
  gap: 8rpx;
  box-shadow: var(--theme-shadow-soft);
}

.toggle-btn {
  margin: 0;
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 20rpx;
  font-size: 24rpx;
  border-radius: 999rpx;
  background: transparent;
  color: var(--theme-ink);
}

.toggle-btn.active {
  background: var(--theme-gradient-strong);
  color: #fff;
}

.toggle-btn::after,
.card-action-btn::after,
.cal-btn::after,
.cal-icon::after {
  border: none;
}

.toolbar-link {
  font-size: 24rpx;
  color: var(--theme-primary-deep);
}

.search-input,
.picker {
  background: rgba(255, 255, 255, 0.82);
  border-radius: 24rpx;
  min-height: 88rpx;
  box-sizing: border-box;
  border: 1rpx solid rgba(140, 128, 216, 0.1);
}

.search-input {
  flex: 1;
  padding: 0 26rpx;
}

.picker {
  min-width: 220rpx;
  padding: 24rpx 26rpx;
}

.picker-value {
  color: var(--theme-ink);
  font-size: 26rpx;
}

.filter-scroll {
  white-space: nowrap;
  margin-bottom: 18rpx;
}

.filter-row {
  display: flex;
  gap: 14rpx;
}

.filter-pill {
  flex: 0 0 auto;
  padding: 14rpx 22rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.82);
  color: #475569;
  font-size: 24rpx;
  border: 1rpx solid rgba(140, 128, 216, 0.08);
}

.filter-pill.active {
  background: var(--theme-gradient-strong);
  color: #fff;
}

.notice-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid rgba(140, 128, 216, 0.1);
  box-shadow: var(--theme-shadow-soft);
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
  background: rgba(140, 128, 216, 0.1);
  color: var(--theme-primary-deep);
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

.tag-row,
.status-chip-row,
.card-action-row {
  margin-top: 16rpx;
}

.status-chip {
  display: inline-flex;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(140, 128, 216, 0.12);
  color: var(--theme-primary-deep);
  font-size: 22rpx;
}

.status-chip.warn {
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
}

.status-chip.danger {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.status-chip.muted {
  background: #e2e8f0;
  color: #64748b;
}

.card-action-btn {
  margin: 0;
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: rgba(140, 128, 216, 0.12);
  color: var(--theme-primary-deep);
  font-size: 24rpx;
}

.card-action-btn.secondary {
  background: #e2e8f0;
  color: #334155;
}

.tag {
  background: #f1f5f9;
  color: #334155;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
}

.state-card {
  text-align: center;
  color: #64748b;
  padding: 48rpx 30rpx;
  font-size: 28rpx;
}

.calendar {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 32rpx;
  padding: 20rpx;
  border: 1rpx solid rgba(140, 128, 216, 0.1);
  box-shadow: var(--theme-shadow-soft);
}

.cal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.cal-header-actions {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.cal-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
}

.cal-btn {
  margin: 0;
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 18rpx;
  border-radius: 16rpx;
  background: rgba(140, 128, 216, 0.1);
  color: var(--theme-primary-deep);
  font-size: 24rpx;
}

.cal-nav {
  display: flex;
  gap: 8rpx;
}

.cal-icon {
  margin: 0;
  height: 64rpx;
  line-height: 64rpx;
  width: 64rpx;
  padding: 0;
  border-radius: 16rpx;
  background: #f1f5f9;
  color: #0f172a;
  font-size: 24rpx;
}

.cal-weekdays {
  display: flex;
  flex-wrap: nowrap;
  border: 1rpx solid #d1d5db;
  border-bottom: none;
  background: #fff;
}

.cal-weekday {
  width: 14.2857%;
  text-align: center;
  font-size: 24rpx;
  font-weight: 700;
  color: #334155;
  padding: 18rpx 0;
  border-right: 1rpx solid #d1d5db;
  box-sizing: border-box;
}

.cal-weekday:last-child {
  border-right: none;
}

.cal-grid {
  display: flex;
  flex-wrap: wrap;
  border: 1rpx solid #d1d5db;
  border-top: none;
}

.cal-cell {
  width: 14.2857%;
  border-top: 1rpx solid #d1d5db;
  border-right: 1rpx solid #d1d5db;
  min-height: 176rpx;
  padding: 12rpx 12rpx 10rpx;
  box-sizing: border-box;
  background: #fff;
}

.cal-cell:nth-child(7n) {
  border-right: none;
}

.cal-cell.muted {
  background: #fafafa;
  color: #94a3b8;
}

.cal-cell.today {
  background: rgba(140, 128, 216, 0.08);
}

.cal-cell.selected {
  box-shadow: inset 0 0 0 3rpx rgba(140, 128, 216, 0.22);
}

.cal-date {
  font-size: 26rpx;
  font-weight: 600;
  color: #374151;
}

.cal-events {
  margin-top: 8rpx;
}

.cal-event {
  display: flex;
  align-items: center;
  gap: 10rpx;
  border-radius: 10rpx;
  padding: 0 12rpx;
  margin-bottom: 6rpx;
  background: #fff;
  border: 2rpx solid #94a3b8;
  height: 54rpx;
  box-sizing: border-box;
  color: #334155;
}

.cal-event.deadline {
  border-color: #ef4444;
  color: #ef4444;
  background: #fff5f5;
}

.cal-event-icon {
  width: 28rpx;
  height: 28rpx;
  border: 2rpx solid #64748b;
  border-radius: 6rpx;
  position: relative;
  box-sizing: border-box;
  flex: 0 0 auto;
}

.cal-event-icon.deadline {
  border-color: #ef4444;
}

.cal-event-text {
  font-size: 20rpx;
  color: inherit;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cal-more {
  font-size: 20rpx;
  color: #64748b;
}

.day-panel {
  margin-top: 18rpx;
  border-top: 1rpx solid #e5e7eb;
  padding-top: 16rpx;
}

.day-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 10rpx;
}

.day-panel-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
}

.day-panel-sub {
  font-size: 22rpx;
  color: #64748b;
}

.day-item {
  padding: 18rpx 0;
  border-bottom: 1rpx solid #f1f5f9;
}

.day-item:last-child {
  border-bottom: none;
}

.bottom-space {
  height: 20rpx;
}
</style>
