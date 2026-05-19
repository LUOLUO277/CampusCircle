<template>
  <view class="page">
    <view class="nav-bar">
      <text class="back-btn" @click="goHome">< 杩斿洖</text>
      <text class="nav-title">淇℃伅璁㈤槄涓績</text>
      <text class="nav-action" @click="goSubscriptions">璁㈤槄绠＄悊</text>
    </view>

    <view class="hero">
      <view>
        <text class="title">淇℃伅璁㈤槄涓績</text>
        <text class="subtitle">缁熶竴鏌ョ湅鍏紬鍙枫€佹暀鍔′笌 Canvas 閫氱煡</text>
      </view>
    </view>

    <view class="toolbar">
      <input
        v-model="filters.keyword"
        class="search-input"
        placeholder="鎼滅储鏍囬銆佹憳瑕併€佸叧閿瘝"
        @confirm="loadNotices"
      />
      <picker class="picker" :range="categories" range-key="label" @change="onCategoryChange">
        <view class="picker-value">{{ currentCategoryLabel }}</view>
      </picker>
    </view>

    <view class="toolbar toolbar-view">
      <view class="view-toggle">
        <button class="toggle-btn" :class="{ active: viewMode === 'cards' }" @click="setViewMode('cards')">鍗＄墖</button>
        <button class="toggle-btn" :class="{ active: viewMode === 'calendar' }" @click="setViewMode('calendar')">鏃ュ巻</button>
      </view>
    </view>

    <view v-if="viewMode === 'cards'" class="timeline-wrap">
      <scroll-view scroll-y class="timeline-scroll" :scroll-into-view="todayGroupAnchorId" :scroll-with-animation="true">
        <view v-if="groupedNotices.length" class="timeline-list">
          <view v-for="group in groupedNotices" :id="group.anchorId" :key="group.key" class="day-group">
            <view class="day-heading">
              <text class="day-title">{{ group.title }}</text>
              <text v-if="group.subTitle" class="day-subtitle">{{ group.subTitle }}</text>
            </view>
            <view class="day-divider"></view>

            <view v-if="group.items.length" class="day-items">
              <view v-for="notice in group.items" :key="notice.id" class="timeline-card" @click="goDetail(notice.id)">
                <view class="notice-header">
                  <text class="badge">{{ notice.category || '閫氱煡' }}</text>
                  <text class="source">{{ notice.sourceName }}</text>
                </view>
                <text class="notice-title">{{ notice.title }}</text>
                <text v-if="notice.summary" class="notice-summary">{{ notice.summary }}</text>
                <view class="meta-row">
                  <text class="meta-text">鍙戝竷鏃堕棿 {{ formatTime(notice.publishTime) }}</text>
                  <text v-if="notice.deadline" class="deadline">鎴 {{ formatTime(notice.deadline) }}</text>
                </view>
                <view v-if="notice.tags && notice.tags.length" class="tag-row">
                  <text v-for="tag in notice.tags" :key="tag" class="tag">{{ tag }}</text>
                </view>
                <view class="source-action-row">
                  <button class="source-btn" @click.stop="openSource(notice)">查看来源</button>
                </view>
              </view>
            </view>

            <view v-else class="timeline-empty">
              <view class="empty-illustration">
                <view class="hill hill-left"></view>
                <view class="hill hill-right"></view>
              </view>
              <text class="timeline-empty-text">灏氭湭杩涜浠讳綍璁″垝</text>
            </view>
          </view>
        </view>

        <view v-else class="empty">鏆傛棤閫氱煡锛屽厛鍘昏闃呮潵婧愭垨瑙﹀彂涓€娆″悓姝ャ€?/view>
      </scroll-view>
    </view>

    <view v-else class="calendar">
      <view class="cal-header">
        <text class="cal-title">{{ calendarTitle }}</text>
        <view class="cal-header-actions">
          <button class="cal-btn" @click="jumpToday">浠婂ぉ</button>
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
          <text class="day-panel-sub">{{ selectedDayEvents.length }} 鏉?/text>
        </view>
        <view v-for="notice in selectedDayEvents" :key="notice.id" class="day-item" @click="goDetail(notice.id)">
          <view class="notice-header">
            <text class="badge">{{ notice.category || '閫氱煡' }}</text>
            <text class="source">{{ notice.sourceName }}</text>
          </view>
          <text class="notice-title">{{ notice.title }}</text>
          <view class="meta-row">
            <text class="meta-text">鍙戝竷鏃堕棿 {{ formatTime(notice.publishTime) }}</text>
            <text v-if="notice.deadline" class="deadline">鎴 {{ formatTime(notice.deadline) }}</text>
          </view>
          <view class="source-action-row">
            <button class="source-btn" @click.stop="openSource(notice)">查看来源</button>
          </view>
        </view>
      </view>

      <view v-if="!notices.length" class="empty">鏆傛棤閫氱煡锛屽厛鍘昏闃呮潵婧愭垨瑙﹀彂涓€娆″悓姝ャ€?/view>
    </view>

    <view class="bottom-space"></view>
    <TabBar :current-tab="'info'" />
  </view>
</template>

<script>
import TabBar from '@/components/TabBar.vue'
import { getInfoNotices } from '@/api/info-center'

export default {
  components: { TabBar },
  data() {
    return {
      hasBootstrapped: false,
      viewMode: 'cards',
      notices: [],
      categories: [
        { value: '', label: '鍏ㄩ儴鍒嗙被' },
        { value: '閫氱煡', label: '閫氱煡' },
        { value: '鎴鎻愰啋', label: '鎴鎻愰啋' },
        { value: '娲诲姩', label: '娲诲姩' },
        { value: '鎶ュ悕', label: '鎶ュ悕' }
      ],
      filters: {
        category: '',
        keyword: ''
      },
      calendarCursor: null,
      selectedDateKey: ''
    }
  },
  computed: {
    currentCategoryLabel() {
      return this.categories.find(item => item.value === this.filters.category)?.label || '鍏ㄩ儴鍒嗙被'
    },
    weekdays() {
      return ['鍛ㄤ竴', '鍛ㄤ簩', '鍛ㄤ笁', '鍛ㄥ洓', '鍛ㄤ簲', '鍛ㄥ叚', '鍛ㄦ棩']
    },
    calendarTitle() {
      const cursor = this.calendarCursor || new Date()
      return `${cursor.getFullYear()}骞?${cursor.getMonth() + 1}鏈坄
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
      return `${y}骞?${m}鏈?${d}鏃
    },
    groupedNotices() {
      const grouped = new Map()
      ;(this.notices || []).forEach(item => {
        const date = this.parseIsoDate(item.deadline || item.publishTime)
        if (!date) return
        const day = this.startOfDay(date)
        const key = this.dateKey(day)
        if (!grouped.has(key)) grouped.set(key, [])
        grouped.get(key).push(item)
      })

      const today = this.startOfDay(new Date())
      const yesterday = this.addDays(today, -1)
      const tomorrow = this.addDays(today, 1)
      const daysWithNotice = Array.from(grouped.keys()).map(key => this.parseIsoDate(`${key} 00:00`)).filter(Boolean)

      const firstNoticeDay = daysWithNotice.length
        ? daysWithNotice.reduce((a, b) => (a.getTime() <= b.getTime() ? a : b))
        : today
      const lastNoticeDay = daysWithNotice.length
        ? daysWithNotice.reduce((a, b) => (a.getTime() >= b.getTime() ? a : b))
        : today

      const start = this.minDate([firstNoticeDay, yesterday])
      const end = this.maxDate([lastNoticeDay, tomorrow])

      const dayEntries = []
      for (let d = new Date(start); d.getTime() <= end.getTime(); d = this.addDays(d, 1)) {
        const key = this.dateKey(d)
        const items = (grouped.get(key) || []).slice().sort((a, b) => {
          const ta = this.timeStampOf(a.deadline || a.publishTime)
          const tb = this.timeStampOf(b.deadline || b.publishTime)
          return ta - tb
        })
        dayEntries.push({
          key,
          date: new Date(d),
          title: this.formatGroupTitle(d),
          subTitle: this.formatGroupSubTitle(d),
          items,
          isToday: key === this.dateKey(today)
        })
      }
      return this.mergeEmptyDayEntries(dayEntries)
    },
    todayGroupAnchorId() {
      const group = (this.groupedNotices || []).find(item => item.isToday)
      return group?.anchorId || ''
    }
  },
  onLoad() {
    this.bootstrap()
  },
  onShow() {
    uni.hideTabBar({ animation: false })
    if (!this.hasBootstrapped) return
    this.loadNotices()
  },
  methods: {
    async bootstrap() {
      this.viewMode = uni.getStorageSync('infoCenterViewMode') || 'cards'
      const now = new Date()
      this.calendarCursor = new Date(now.getFullYear(), now.getMonth(), 1)
      this.selectedDateKey = this.dateKey(now)
      await this.loadNotices()
      this.hasBootstrapped = true
    },
    setViewMode(mode) {
      this.viewMode = mode
      uni.setStorageSync('infoCenterViewMode', mode)
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
    goHome() {
      uni.switchTab({ url: '/pages/index/index' })
    },
    goSubscriptions() {
      uni.navigateTo({ url: '/pages/info-center/subscriptions' })
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/info-center/detail?id=${id}` })
    },
    openSource(notice = {}) {
      const url = this.resolveSourceUrl(notice)
      if (!url) {
        uni.showToast({ title: '暂无来源链接', icon: 'none' })
        return
      }
      // #ifdef H5
      window.open(url, '_blank')
      // #endif
      // #ifndef H5
      uni.setClipboardData({ data: url })
      uni.showToast({ title: '来源链接已复制', icon: 'none' })
      // #endif
    },
    resolveSourceUrl(notice = {}) {
      const originalUrl = `${notice.originalUrl || ''}`.trim()
      if (originalUrl) return originalUrl
      const sourceName = `${notice.sourceName || ''}`
      if (/canvas/i.test(sourceName)) return 'https://canvas.tongji.edu.cn'
      if (/tongji|同济|1系统|1 系统/i.test(sourceName)) return 'https://1.tongji.edu.cn/myAnnouncement'
      return ''
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
    timeStampOf(value) {
      const dt = this.parseIsoDate(value)
      return dt ? dt.getTime() : 0
    },
    startOfDay(date) {
      return new Date(date.getFullYear(), date.getMonth(), date.getDate())
    },
    addDays(date, offset) {
      return new Date(date.getFullYear(), date.getMonth(), date.getDate() + offset)
    },
    minDate(dates = []) {
      return dates.reduce((a, b) => (a.getTime() <= b.getTime() ? a : b))
    },
    maxDate(dates = []) {
      return dates.reduce((a, b) => (a.getTime() >= b.getTime() ? a : b))
    },
    mergeEmptyDayEntries(dayEntries = []) {
      const merged = []
      let index = 0
      while (index < dayEntries.length) {
        const current = dayEntries[index]
        const isEmpty = !current.items.length
        if (!isEmpty || current.isToday) {
          merged.push({ ...current, anchorId: `day-${current.key}` })
          index += 1
          continue
        }
        let end = index
        while (end + 1 < dayEntries.length && !dayEntries[end + 1].items.length && !dayEntries[end + 1].isToday) {
          end += 1
        }
        const span = end - index + 1
        if (span >= 2) {
          const startDay = dayEntries[index].date
          const endDay = dayEntries[end].date
          const startKey = dayEntries[index].key
          const endKey = dayEntries[end].key
          merged.push({
            key: `${startKey}_${endKey}`,
            anchorId: `day-${startKey}`,
            title: this.formatRangeTitle(startDay, endDay),
            subTitle: '',
            items: [],
            isToday: false
          })
          index = end + 1
          continue
        }
        merged.push({ ...current, anchorId: `day-${current.key}` })
        index += 1
      }
      return merged
    },
    dateKey(date) {
      const y = date.getFullYear()
      const m = `${date.getMonth() + 1}`.padStart(2, '0')
      const d = `${date.getDate()}`.padStart(2, '0')
      return `${y}-${m}-${d}`
    },
    formatGroupTitle(date) {
      if (!date) return ''
      const now = new Date()
      const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      const target = new Date(date.getFullYear(), date.getMonth(), date.getDate())
      const diff = Math.round((target.getTime() - today.getTime()) / 86400000)
      if (diff === 0) return '浠婂ぉ'
      if (diff === 1) return '鏄庡ぉ'
      if (diff === -1) return '鏄ㄥぉ'
      const weekMap = ['鏄熸湡鏃?, '鏄熸湡涓€', '鏄熸湡浜?, '鏄熸湡涓?, '鏄熸湡鍥?, '鏄熸湡浜?, '鏄熸湡鍏?]
      return weekMap[target.getDay()]
    },
    formatGroupSubTitle(date) {
      if (!date) return ''
      return `${date.getMonth() + 1}鏈?{date.getDate()}鏃
    },
    formatRangeTitle(startDate, endDate) {
      if (!startDate || !endDate) return ''
      if (startDate.getMonth() === endDate.getMonth()) {
        return `${startDate.getMonth() + 1}鏈?{startDate.getDate()}鏃ヨ嚦${endDate.getDate()}鏃
      }
      return `${startDate.getMonth() + 1}鏈?{startDate.getDate()}鏃ヨ嚦${endDate.getMonth() + 1}鏈?{endDate.getDate()}鏃
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
  white-space: nowrap;
}

.toggle-btn::after {
  border: none;
}

.toggle-btn.active {
  background: var(--theme-gradient-strong);
  color: #fff;
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

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 16rpx;
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

.timeline-wrap {
  height: calc(100vh - 460rpx);
  min-height: 720rpx;
}

.timeline-scroll {
  height: 100%;
}

.timeline-list {
  padding-bottom: 20rpx;
}

.day-group {
  margin-bottom: 24rpx;
}

.day-heading {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6rpx;
}

.day-title {
  font-size: 42rpx;
  color: #1e3a5f;
  font-weight: 700;
}

.day-subtitle {
  font-size: 28rpx;
  color: #475569;
}

.day-divider {
  margin: 12rpx 0 14rpx;
  border-top: 2rpx solid rgba(71, 85, 105, 0.25);
}

.day-items {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.timeline-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24rpx;
  padding: 24rpx;
  border: 1rpx solid rgba(140, 128, 216, 0.1);
  box-shadow: var(--theme-shadow-soft);
}

.timeline-empty {
  border-radius: 24rpx;
  border: 2rpx dashed rgba(148, 163, 184, 0.45);
  min-height: 220rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18rpx;
  background: rgba(248, 250, 252, 0.9);
}

.empty-illustration {
  width: 180rpx;
  height: 90rpx;
  position: relative;
}

.hill {
  position: absolute;
  bottom: 0;
  border-radius: 120rpx 120rpx 0 0;
  border: 4rpx solid #94a3b8;
  border-bottom: none;
}

.hill-left {
  left: 0;
  width: 98rpx;
  height: 56rpx;
}

.hill-right {
  right: 0;
  width: 120rpx;
  height: 74rpx;
}

.timeline-empty-text {
  font-size: 32rpx;
  color: #334155;
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

.cal-cell.muted .cal-date {
  color: #9ca3af;
}

.cal-cell.muted .cal-event {
  opacity: 0.6;
}

.cal-cell.today {
  background: rgba(140, 128, 216, 0.08);
}

.cal-cell.today .cal-date {
  color: var(--theme-primary-deep);
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

.source-action-row {
  margin-top: 12rpx;
  display: flex;
  justify-content: flex-end;
}

.source-btn {
  margin: 0;
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  background: rgba(140, 128, 216, 0.12);
  color: var(--theme-primary-deep);
}

.source-btn::after {
  border: none;
}

.bottom-space {
  height: 20rpx;
}
</style>

