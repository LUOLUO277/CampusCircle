<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="title">信息订阅中心</text>
        <text class="subtitle">统一查看公众号、教务与 Canvas 通知</text>
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
      <view class="toolbar-right">
        <label class="subscribed-switch">
          <switch :checked="!!filters.onlySubscribed" color="#2f855a" @change="onSubscribedToggle" />
          <text>只看已订阅</text>
        </label>
        <view class="view-toggle">
          <button class="toggle-btn" :class="{ active: viewMode === 'cards' }" @click="setViewMode('cards')">卡片视角</button>
          <button class="toggle-btn" :class="{ active: viewMode === 'calendar' }" @click="setViewMode('calendar')">日历视角</button>
        </view>
      </view>
    </view>

    <view v-if="viewMode === 'cards'" class="list">
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

      <view class="day-panel" v-if="selectedDayEvents.length">
        <view class="day-panel-header">
          <text class="day-panel-title">{{ selectedDateLabel }}</text>
          <text class="day-panel-sub">{{ selectedDayEvents.length }} 条</text>
        </view>
        <view v-for="notice in selectedDayEvents" :key="notice.id" class="day-item" @click="goDetail(notice.id)">
          <view class="notice-header">
            <text class="badge">{{ notice.category || '通知' }}</text>
            <text class="source">{{ notice.sourceName }}</text>
          </view>
          <text class="notice-title">{{ notice.title }}</text>
          <view class="meta-row">
            <text class="meta-text">发布时间 {{ formatTime(notice.publishTime) }}</text>
            <text v-if="notice.deadline" class="deadline">截止 {{ formatTime(notice.deadline) }}</text>
          </view>
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
      viewMode: 'cards',
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
      },
      calendarCursor: null,
      selectedDateKey: ''
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
    },
    weekdays() {
      return ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    calendarTitle() {
      const cursor = this.calendarCursor || new Date()
      return `${cursor.getFullYear()}年 ${cursor.getMonth() + 1}月`
    },
    noticeEvents() {
      return (this.notices || [])
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
        const events = (eventsByKey.get(key) || []).slice().sort((a, b) => {
          const ad = !!a.deadline
          const bd = !!b.deadline
          if (ad !== bd) return ad ? -1 : 1
          return `${a.title}`.localeCompare(`${b.title}`)
        })
        days.push({
          key,
          inMonth: d.getMonth() === month,
          isToday: key === todayKey,
          dateNum: d.getDate(),
          events
        })
      }
      return days
    },
    selectedDayEvents() {
      if (!this.selectedDateKey) return []
      return this.calendarDays.find(d => d.key === this.selectedDateKey)?.events || []
    },
    selectedDateLabel() {
      if (!this.selectedDateKey) return ''
      const [y, m, d] = this.selectedDateKey.split('-').map(v => parseInt(v, 10))
      return `${y}年${m}月${d}日`
    }
  },
  onLoad() {
    this.bootstrap()
  },
  methods: {
    async bootstrap() {
      this.viewMode = uni.getStorageSync('infoCenterViewMode') || 'cards'
      const now = new Date()
      this.calendarCursor = new Date(now.getFullYear(), now.getMonth(), 1)
      this.selectedDateKey = this.dateKey(now)
      await Promise.all([this.loadSources(), this.loadNotices()])
    },
    setViewMode(mode) {
      this.viewMode = mode
      uni.setStorageSync('infoCenterViewMode', mode)
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
        pageSize: 40
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
.view-toggle {
  display: flex;
  background: #fff;
  border-radius: 999rpx;
  padding: 8rpx;
  gap: 8rpx;
  box-shadow: 0 12rpx 30rpx rgba(15, 23, 42, 0.06);
}
.toggle-btn {
  margin: 0;
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 20rpx;
  font-size: 24rpx;
  border-radius: 999rpx;
  background: transparent;
  color: #1f2937;
  white-space: nowrap;
}
.toggle-btn::after {
  border: none;
}
.toggle-btn.active {
  background: #1f5f46;
  color: #fff;
}
.toolbar {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
  align-items: center;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 16rpx;
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
  border-radius: 999rpx;
  padding: 14rpx 18rpx;
  font-size: 24rpx;
  color: #334155;
  box-shadow: 0 12rpx 30rpx rgba(15, 23, 42, 0.06);
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

.calendar {
  background: #fff;
  border-radius: 32rpx;
  padding: 20rpx;
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
  background: #eef6ef;
  color: #1f5f46;
  font-size: 24rpx;
}
.cal-btn::after {
  border: none;
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
.cal-icon::after {
  border: none;
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
  padding: 12rpx 12rpx 10rpx 12rpx;
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
.cal-cell.muted .cal-date {
  color: #9ca3af;
}
.cal-cell.muted .cal-event {
  opacity: 0.6;
}
.cal-cell.today {
  background: #f0fdf4;
}
.cal-cell.today .cal-date {
  color: #16a34a;
}
.cal-cell.selected {
  box-shadow: inset 0 0 0 3rpx rgba(47, 133, 90, 0.22);
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
.cal-event-icon::before,
.cal-event-icon::after {
  content: '';
  position: absolute;
  left: 5rpx;
  right: 5rpx;
  height: 2rpx;
  background: #64748b;
  opacity: 0.9;
}
.cal-event-icon::before {
  top: 9rpx;
}
.cal-event-icon::after {
  top: 16rpx;
}
.cal-event-icon.deadline {
  border-color: #ef4444;
}
.cal-event-icon.deadline::before,
.cal-event-icon.deadline::after {
  background: #ef4444;
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
</style>
