<template>
  <view class="page">
    <view class="nav-card">
      <view>
        <text class="nav-title">课程圈子</text>
        <text class="nav-subtitle">围绕课程沉淀问答、经验与评价</text>
      </view>
      <view class="nav-actions">
        <view class="nav-action" @click="goCreateCourse">新增课程</view>
        <view class="nav-action ghost" @click="reload">刷新</view>
      </view>
    </view>

    <view class="hero-card">
      <text class="hero-badge">Course Circle</text>
      <text class="hero-title">按课程组织信息，查老师、查课程号、查真实经验</text>
      <view class="search-shell">
        <input
          v-model="keyword"
          class="search-input"
          type="text"
          placeholder="搜索课程名、教师名、课程编号"
          @confirm="handleSearch"
        />
        <view class="search-btn" @click="handleSearch">搜索</view>
      </view>
    </view>

    <view v-if="loading" class="state-card">正在加载课程圈子...</view>
    <view v-else-if="error" class="state-card error">
      <text>{{ error }}</text>
      <view class="retry-btn" @click="reload">重试</view>
    </view>
    <template v-else>
      <view class="section">
        <view class="section-head">
          <text class="section-title">我的课程圈子</text>
          <text class="section-meta">{{ myCourses.length }} 门</text>
        </view>
        <scroll-view v-if="myCourses.length" class="my-scroll" scroll-x>
          <view
            v-for="course in myCourses"
            :key="course.id"
            class="my-card"
            @click="goDetail(course.id)"
          >
            <text class="course-name">{{ course.name }}</text>
            <text class="course-teacher">{{ course.teacherName }}</text>
            <text class="course-code">{{ course.courseCode }}</text>
            <view class="chip-row">
              <text v-for="tag in course.tags.slice(0, 2)" :key="tag" class="chip">{{ tag }}</text>
            </view>
          </view>
        </scroll-view>
        <view v-else class="empty-card">还没有加入课程圈子，先去看看热门课程。</view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="section-title">热门课程圈子</text>
          <text class="section-meta">动态排行</text>
        </view>
        <view v-if="hotCourses.length" class="list">
          <view v-for="course in hotCourses" :key="course.id" class="course-card hot" @click="goDetail(course.id)">
            <view class="card-top">
              <view class="rank-badge">TOP {{ course.hotRank }}</view>
              <text class="hot-score">热度 {{ course.hotScore }}</text>
            </view>
            <text class="course-name">{{ course.name }}</text>
            <text class="course-meta">{{ course.teacherName }} · {{ course.department }}</text>
            <text class="course-code">{{ course.courseCode }} · {{ course.semester }}</text>
            <view class="chip-row">
              <text v-for="tag in course.tags" :key="tag" class="chip">{{ tag }}</text>
            </view>
            <view class="stats-row">
              <text>圈子 {{ course.memberCount }}</text>
              <text>问答 {{ course.questionCount }}</text>
              <text>经验 {{ course.experienceCount }}</text>
              <text>评价 {{ course.reviewCount }}</text>
            </view>
          </view>
        </view>
        <view v-else class="empty-card">暂无热门课程数据。</view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="section-title">全部课程</text>
          <text class="section-meta">{{ courses.length }} 门</text>
        </view>
        <view v-if="courses.length" class="list">
          <view v-for="course in courses" :key="course.id" class="course-card" @click="goDetail(course.id)">
            <view class="card-head">
              <view>
                <text class="course-name">{{ course.name }}</text>
                <text class="course-meta">{{ course.teacherName }} · {{ course.department }}</text>
              </view>
              <text class="joined-badge" v-if="course.isJoined">已加入</text>
            </view>
            <text class="course-code">{{ course.courseCode }} · {{ course.semester }}</text>
            <view class="chip-row">
              <text v-for="tag in course.tags" :key="tag" class="chip">{{ tag }}</text>
            </view>
            <view class="stats-row">
              <text>圈子 {{ course.memberCount }}</text>
              <text>问答 {{ course.questionCount }}</text>
              <text>经验 {{ course.experienceCount }}</text>
              <text>评价 {{ course.reviewCount }}</text>
            </view>
          </view>
        </view>
        <view v-else class="empty-card">没有匹配到课程，换个关键词试试。</view>
      </view>
    </template>

    <view class="bottom-space"></view>
    <TabBar current-tab="course" />
  </view>
</template>

<script>
import TabBar from '@/components/TabBar.vue'
import { getCourses, getHotCourses, getMyCourses, searchCourses } from '@/api/course-circle'
import { getToken } from '@/utils/request'

export default {
  components: { TabBar },
  data() {
    return {
      keyword: '',
      loading: false,
      error: '',
      courses: [],
      hotCourses: [],
      myCourses: []
    }
  },
  onLoad() {
    this.reload()
  },
  onShow() {
    uni.hideTabBar({ animation: false })
    this.reload()
  },
  onPullDownRefresh() {
    this.reload().finally(() => uni.stopPullDownRefresh())
  },
  methods: {
    async reload() {
      this.loading = true
      this.error = ''
      try {
        const [allRes, hotRes, myRes] = await Promise.all([
          getCourses(),
          getHotCourses(),
          getMyCourses().catch(() => ({ code: 200, data: [] }))
        ])
        this.courses = allRes.data || []
        this.hotCourses = hotRes.data || []
        this.myCourses = myRes.data || []
      } catch (error) {
        this.error = error.message || '课程圈子加载失败'
      } finally {
        this.loading = false
      }
    },
    async handleSearch() {
      const value = (this.keyword || '').trim()
      if (!value) {
        await this.reload()
        return
      }
      this.loading = true
      this.error = ''
      try {
        const res = await searchCourses(value)
        this.courses = res.data || []
      } catch (error) {
        this.error = error.message || '搜索失败'
      } finally {
        this.loading = false
      }
    },
    goDetail(courseId) {
      uni.navigateTo({ url: `/pages/course-circle/detail?id=${courseId}` })
    },
    goCreateCourse() {
      if (!getToken()) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      uni.navigateTo({ url: '/pages/course-circle/course-create' })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.22), transparent 30%),
    linear-gradient(180deg, #faf7f2 0%, #f5f1eb 100%);
  padding: 28rpx 24rpx 160rpx;
}

.nav-card,
.hero-card,
.course-card,
.state-card,
.empty-card,
.my-card {
  background: rgba(255, 255, 255, 0.84);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow-soft);
}

.nav-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx 28rpx;
  border-radius: 28rpx;
}

.nav-actions {
  display: flex;
  gap: 12rpx;
}

.nav-title,
.nav-subtitle,
.hero-badge,
.hero-title,
.section-title,
.course-name,
.course-meta,
.course-code,
.section-meta,
.state-card,
.empty-card,
.course-teacher {
  display: block;
}

.nav-title {
  font-size: 34rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.nav-subtitle {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--theme-muted);
}

.nav-action,
.search-btn,
.retry-btn,
.joined-badge,
.rank-badge {
  border-radius: 999rpx;
  font-size: 24rpx;
}

.nav-action,
.retry-btn {
  padding: 12rpx 24rpx;
  background: rgba(140, 128, 216, 0.1);
  color: var(--theme-primary-deep);
}

.nav-action.ghost {
  background: rgba(15, 23, 42, 0.06);
  color: var(--theme-ink);
}

.hero-card {
  margin-top: 22rpx;
  padding: 34rpx 30rpx;
  border-radius: 32rpx;
  background: var(--theme-gradient);
}

.hero-badge {
  font-size: 22rpx;
  letter-spacing: 2rpx;
  color: var(--theme-primary-deep);
  font-weight: 700;
  text-transform: uppercase;
}

.hero-title {
  margin-top: 18rpx;
  font-size: 48rpx;
  line-height: 1.28;
  font-weight: 800;
  color: var(--theme-ink);
}

.search-shell {
  margin-top: 28rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 10rpx 12rpx 10rpx 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.92);
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: var(--theme-ink);
}

.search-btn {
  min-width: 132rpx;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  background: var(--theme-ink);
  color: #fff;
  font-weight: 700;
}

.section {
  margin-top: 24rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.section-meta {
  font-size: 22rpx;
  color: var(--theme-muted);
}

.my-scroll {
  white-space: nowrap;
}

.my-card {
  display: inline-flex;
  flex-direction: column;
  width: 280rpx;
  margin-right: 18rpx;
  padding: 24rpx;
  border-radius: 26rpx;
}

.course-name {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.course-teacher,
.course-meta,
.course-code {
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #6d6582;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.course-card {
  padding: 28rpx;
  border-radius: 28rpx;
}

.course-card.hot {
  background: linear-gradient(180deg, rgba(255,255,255,0.95), rgba(248,244,255,0.96));
}

.card-top,
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
}

.rank-badge {
  padding: 8rpx 18rpx;
  background: var(--theme-ink);
  color: #fff;
}

.hot-score {
  font-size: 22rpx;
  color: var(--theme-muted);
}

.joined-badge {
  padding: 8rpx 18rpx;
  height: fit-content;
  background: rgba(140, 128, 216, 0.12);
  color: var(--theme-primary-deep);
}

.chip-row,
.stats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.chip-row {
  margin-top: 16rpx;
}

.chip {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  color: var(--theme-primary-deep);
  background: rgba(140, 128, 216, 0.1);
}

.stats-row {
  margin-top: 18rpx;
  justify-content: space-between;
  font-size: 23rpx;
  color: var(--theme-muted);
}

.state-card,
.empty-card {
  margin-top: 24rpx;
  padding: 36rpx 28rpx;
  border-radius: 28rpx;
  text-align: center;
  color: var(--theme-muted);
  font-size: 26rpx;
}

.state-card.error {
  color: #b65473;
}

.retry-btn {
  margin: 18rpx auto 0;
  width: 160rpx;
}

.bottom-space {
  height: 20rpx;
}
</style>
