<template>
  <view class="page">
    <view class="nav-card">
      <text class="nav-btn" @click="goBack">&lt;</text>
      <text class="nav-title">课程主页</text>
      <text class="nav-btn" @click="reload">↻</text>
    </view>

    <view v-if="loading" class="state-card">课程内容加载中...</view>
    <view v-else-if="error" class="state-card error">
      <text>{{ error }}</text>
      <view class="action-pill" @click="reload">重试</view>
    </view>
    <template v-else-if="course.id">
      <view class="hero-card">
        <view class="hero-top">
          <view>
            <text class="course-name">{{ course.name }}</text>
            <text class="course-meta">{{ course.teacherName }} · {{ course.courseCode }}</text>
            <text class="course-meta">{{ course.department }} · {{ course.semester }}</text>
          </view>
          <view class="join-btn" :class="{ joined: course.isJoined }" @click="toggleJoin">
            {{ course.isJoined ? '退出圈子' : '加入圈子' }}
          </view>
        </view>
        <text class="course-desc">{{ course.description }}</text>
        <view class="chip-row">
          <text v-for="tag in course.tags" :key="tag" class="chip">{{ tag }}</text>
        </view>
        <view class="stats-grid">
          <view class="stat-card">
            <text class="stat-num">{{ course.memberCount }}</text>
            <text class="stat-label">圈子人数</text>
          </view>
          <view class="stat-card">
            <text class="stat-num">{{ course.questionCount }}</text>
            <text class="stat-label">问答数</text>
          </view>
          <view class="stat-card">
            <text class="stat-num">{{ course.experienceCount }}</text>
            <text class="stat-label">经验数</text>
          </view>
          <view class="stat-card">
            <text class="stat-num">{{ course.reviewCount }}</text>
            <text class="stat-label">评价数</text>
          </view>
        </view>
      </view>

      <view class="tab-row">
        <view
          v-for="item in tabs"
          :key="item.key"
          class="tab-item"
          :class="{ active: activeTab === item.key }"
          @click="switchTab(item.key)"
        >
          {{ item.label }}
        </view>
      </view>

      <view v-if="activeTab === 'overview'" class="panel">
        <view class="summary-grid">
          <view class="summary-card">
            <text class="summary-label">平均难度</text>
            <text class="summary-value">{{ reviewSummary.averageDifficulty || 0 }}</text>
          </view>
          <view class="summary-card">
            <text class="summary-label">平均作业量</text>
            <text class="summary-value">{{ reviewSummary.averageWorkload || 0 }}</text>
          </view>
          <view class="summary-card">
            <text class="summary-label">平均收获</text>
            <text class="summary-value">{{ reviewSummary.averageGain || 0 }}</text>
          </view>
          <view class="summary-card">
            <text class="summary-label">平均推荐</text>
            <text class="summary-value">{{ reviewSummary.averageRecommend || 0 }}</text>
          </view>
        </view>
        <view class="overview-card">
          <text class="block-title">课程概览</text>
          <text class="overview-text">{{ course.description }}</text>
          <text class="overview-sub">评价人数 {{ reviewSummary.reviewCount || 0 }}</text>
        </view>
      </view>

      <view v-else-if="activeTab === 'questions'" class="panel">
        <view class="toolbar">
          <input v-model="questionQuery.keyword" class="toolbar-input" placeholder="搜索标题或内容" @confirm="reloadQuestions(true)" />
          <picker :range="questionSortOptions" range-key="label" @change="onQuestionSortChange">
            <view class="toolbar-chip">{{ currentQuestionSort.label }}</view>
          </picker>
          <picker :range="questionResolvedOptions" range-key="label" @change="onQuestionResolvedChange">
            <view class="toolbar-chip">{{ currentResolvedLabel }}</view>
          </picker>
        </view>
        <view v-if="questions.length" class="list">
          <view v-for="question in questions" :key="question.id" class="list-card" @click="goQuestionDetail(question.id)">
            <view class="row-between">
              <text class="list-title">{{ question.title }}</text>
              <text class="status-chip" :class="{ resolved: question.resolved }">
                {{ question.resolved ? '已解决' : question.questionType }}
              </text>
            </view>
            <text class="list-content">{{ question.content }}</text>
            <view class="meta-row">
              <text>{{ question.author.name }}</text>
              <text>{{ formatTime(question.createdAt) }}</text>
              <text>{{ question.replyCount }} 条回复</text>
            </view>
          </view>
          <view v-if="questionHasMore" class="more-btn" @click="reloadQuestions()">加载更多问答</view>
        </view>
        <view v-else class="empty-card">这门课还没有问答，发起第一个问题吧。</view>
      </view>

      <view v-else-if="activeTab === 'experiences'" class="panel">
        <view class="toolbar">
          <input v-model="experienceQuery.keyword" class="toolbar-input" placeholder="搜索经验内容或学期" @confirm="reloadExperiences(true)" />
          <picker :range="experienceSortOptions" range-key="label" @change="onExperienceSortChange">
            <view class="toolbar-chip">{{ currentExperienceSort.label }}</view>
          </picker>
        </view>
        <view v-if="experiences.length" class="list">
          <view v-for="item in experiences" :key="item.id" class="list-card">
            <view class="row-between">
              <text class="list-title">{{ item.semester }}</text>
              <text class="status-chip">{{ item.recommendRating }}/5 推荐</text>
            </view>
            <view class="score-row">
              <text>难度 {{ item.difficultyRating }}</text>
              <text>作业 {{ item.workloadRating }}</text>
              <text>收获 {{ item.gainRating }}</text>
              <text>压力 {{ item.examPressureRating }}</text>
            </view>
            <text class="block-title small">学习建议</text>
            <text class="list-content">{{ expandedExperienceIds.includes(item.id) ? item.studyAdvice : item.previewStudyAdvice }}</text>
            <text class="block-title small">避坑建议</text>
            <text class="list-content">{{ expandedExperienceIds.includes(item.id) ? item.pitfallAdvice : item.previewPitfallAdvice }}</text>
            <view class="meta-row">
              <text>{{ item.author.name }}</text>
              <text>{{ formatTime(item.createdAt) }}</text>
            </view>
            <view class="inline-actions">
              <text class="action-link" @click="toggleExperienceExpand(item.id)">
                {{ expandedExperienceIds.includes(item.id) ? '收起' : '展开' }}
              </text>
              <text class="action-link" @click="goExperienceDetail(item.id)">查看详情</text>
            </view>
          </view>
          <view v-if="experienceHasMore" class="more-btn" @click="reloadExperiences()">加载更多经验</view>
        </view>
        <view v-else class="empty-card">还没有课程经验，欢迎补充你的真实体验。</view>
      </view>

      <view v-else class="panel">
        <view class="summary-grid">
          <view class="summary-card">
            <text class="summary-label">讲解清晰度</text>
            <text class="summary-value">{{ reviewSummary.averageClarity || 0 }}</text>
          </view>
          <view class="summary-card">
            <text class="summary-label">考核压力</text>
            <text class="summary-value">{{ reviewSummary.averageExamPressure || 0 }}</text>
          </view>
        </view>
        <view v-if="reviews.length" class="list">
          <view v-for="review in reviews" :key="review.id" class="list-card">
            <view class="row-between">
              <text class="list-title">{{ review.author.name }}</text>
              <text class="status-chip">{{ review.recommendRating }}/5 推荐</text>
            </view>
            <view class="score-row">
              <text>难度 {{ review.difficultyRating }}</text>
              <text>作业 {{ review.workloadRating }}</text>
              <text>收获 {{ review.gainRating }}</text>
              <text>清晰 {{ review.clarityRating }}</text>
            </view>
            <text v-if="review.content" class="list-content">{{ review.content }}</text>
            <view class="chip-row">
              <text v-for="tag in review.tags" :key="tag" class="chip">{{ tag }}</text>
            </view>
            <view class="meta-row">
              <text>{{ formatTime(review.createdAt) }}</text>
            </view>
          </view>
        </view>
        <view v-else class="empty-card">还没有课程评价，先写下你的第一条反馈。</view>
      </view>

      <view class="bottom-actions">
        <view class="action-btn dark" @click="goQuestionCreate">发布问题</view>
        <view class="action-btn" @click="goExperienceCreate">分享经验</view>
        <view class="action-btn" @click="goReviewCreate">写评价</view>
      </view>
    </template>
  </view>
</template>

<script>
import { getToken } from '@/utils/request'
import {
  getCourseDetail,
  getCourseExperiences,
  getCourseQuestions,
  getCourseReviews,
  getCourseReviewSummary,
  joinCourse,
  quitCourse
} from '@/api/course-circle'

export default {
  data() {
    return {
      courseId: null,
      loading: false,
      error: '',
      activeTab: 'overview',
      tabs: [
        { key: 'overview', label: '课程概览' },
        { key: 'questions', label: '课程问答' },
        { key: 'experiences', label: '经验分享' },
        { key: 'reviews', label: '课程评价' }
      ],
      questionSortOptions: [
        { label: '最新', value: 'latest' },
        { label: '未解决优先', value: 'unresolved' },
        { label: '最早', value: 'oldest' }
      ],
      questionResolvedOptions: [
        { label: '全部', value: '' },
        { label: '未解决', value: 'false' },
        { label: '已解决', value: 'true' }
      ],
      experienceSortOptions: [
        { label: '最新', value: 'latest' },
        { label: '推荐优先', value: 'recommend' },
        { label: '收获优先', value: 'gain' },
        { label: '难度优先', value: 'difficulty' }
      ],
      course: {},
      questions: [],
      experiences: [],
      reviews: [],
      reviewSummary: {},
      questionQuery: { keyword: '', resolved: '', sort: 'latest', page: 1, pageSize: 5 },
      experienceQuery: { keyword: '', sort: 'latest', page: 1, pageSize: 5 },
      questionHasMore: false,
      experienceHasMore: false,
      expandedExperienceIds: []
    }
  },
  computed: {
    currentQuestionSort() {
      return this.questionSortOptions.find(item => item.value === this.questionQuery.sort) || this.questionSortOptions[0]
    },
    currentResolvedLabel() {
      return this.questionResolvedOptions.find(item => item.value === this.questionQuery.resolved)?.label || '全部'
    },
    currentExperienceSort() {
      return this.experienceSortOptions.find(item => item.value === this.experienceQuery.sort) || this.experienceSortOptions[0]
    }
  },
  onLoad(options) {
    this.courseId = Number(options.id || 0)
    this.reload()
  },
  onShow() {
    this.reload()
  },
  methods: {
    async reload() {
      if (!this.courseId) return
      this.loading = true
      this.error = ''
      try {
        const [detailRes, summaryRes, reviewsRes] = await Promise.all([
          getCourseDetail(this.courseId),
          getCourseReviewSummary(this.courseId),
          getCourseReviews(this.courseId)
        ])
        this.course = detailRes.data || {}
        this.reviewSummary = summaryRes.data || {}
        this.reviews = reviewsRes.data || []
        await Promise.all([this.reloadQuestions(true), this.reloadExperiences(true)])
      } catch (error) {
        this.error = error.message || '课程详情加载失败'
      } finally {
        this.loading = false
      }
    },
    switchTab(tab) {
      this.activeTab = tab
    },
    async reloadQuestions(reset = false) {
      if (reset) {
        this.questionQuery.page = 1
      }
      const params = {
        keyword: this.questionQuery.keyword || undefined,
        resolved: this.questionQuery.resolved === '' ? undefined : this.questionQuery.resolved === 'true',
        sort: this.questionQuery.sort,
        page: this.questionQuery.page,
        pageSize: this.questionQuery.pageSize
      }
      const res = await getCourseQuestions(this.courseId, params)
      const list = res.data?.list || []
      this.questions = reset ? list : [...this.questions, ...list]
      this.questionHasMore = !!res.data?.hasMore
      if (this.questionHasMore) this.questionQuery.page += 1
    },
    async reloadExperiences(reset = false) {
      if (reset) {
        this.experienceQuery.page = 1
      }
      const params = {
        keyword: this.experienceQuery.keyword || undefined,
        sort: this.experienceQuery.sort,
        page: this.experienceQuery.page,
        pageSize: this.experienceQuery.pageSize
      }
      const res = await getCourseExperiences(this.courseId, params)
      const list = res.data?.list || []
      this.experiences = reset ? list : [...this.experiences, ...list]
      this.experienceHasMore = !!res.data?.hasMore
      if (this.experienceHasMore) this.experienceQuery.page += 1
    },
    onQuestionSortChange(event) {
      this.questionQuery.sort = this.questionSortOptions[event.detail.value].value
      this.reloadQuestions(true)
    },
    onQuestionResolvedChange(event) {
      this.questionQuery.resolved = this.questionResolvedOptions[event.detail.value].value
      this.reloadQuestions(true)
    },
    onExperienceSortChange(event) {
      this.experienceQuery.sort = this.experienceSortOptions[event.detail.value].value
      this.reloadExperiences(true)
    },
    async toggleJoin() {
      if (!getToken()) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      try {
        const res = this.course.isJoined ? await quitCourse(this.courseId) : await joinCourse(this.courseId)
        this.course.isJoined = res.data.isJoined
        this.course.memberCount = res.data.memberCount
        uni.showToast({ title: res.data.isJoined ? '已加入课程圈子' : '已退出课程圈子', icon: 'none' })
      } catch (error) {
        uni.showToast({ title: error.message || '操作失败', icon: 'none' })
      }
    },
    toggleExperienceExpand(id) {
      if (this.expandedExperienceIds.includes(id)) {
        this.expandedExperienceIds = this.expandedExperienceIds.filter(item => item !== id)
      } else {
        this.expandedExperienceIds = [...this.expandedExperienceIds, id]
      }
    },
    goQuestionCreate() {
      if (!this.ensureLogin()) return
      uni.navigateTo({ url: `/pages/course-circle/question-create?courseId=${this.courseId}` })
    },
    goExperienceCreate() {
      if (!this.ensureLogin()) return
      uni.navigateTo({ url: `/pages/course-circle/experience-create?courseId=${this.courseId}` })
    },
    goReviewCreate() {
      if (!this.ensureLogin()) return
      uni.navigateTo({ url: `/pages/course-circle/review-create?courseId=${this.courseId}` })
    },
    goQuestionDetail(questionId) {
      uni.navigateTo({ url: `/pages/course-circle/question-detail?id=${questionId}` })
    },
    goExperienceDetail(experienceId) {
      uni.navigateTo({ url: `/pages/course-circle/experience-detail?id=${experienceId}` })
    },
    ensureLogin() {
      if (getToken()) return true
      uni.navigateTo({ url: '/pages/login/index' })
      return false
    },
    goBack() {
      uni.navigateBack()
    },
    formatTime(value) {
      return `${value || ''}`.replace('T', ' ').slice(0, 16)
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.2), transparent 30%),
    linear-gradient(180deg, #faf7f2 0%, #f5f1eb 100%);
  padding: 24rpx 24rpx 180rpx;
}

.nav-card,
.hero-card,
.tab-row,
.panel,
.list-card,
.summary-card,
.overview-card,
.state-card,
.empty-card {
  background: rgba(255, 255, 255, 0.86);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow-soft);
}

.nav-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 22rpx;
  border-radius: 24rpx;
}

.nav-btn,
.nav-title,
.course-name,
.course-meta,
.course-desc,
.stat-num,
.stat-label,
.summary-label,
.summary-value,
.block-title,
.list-title,
.list-content,
.state-card,
.empty-card {
  display: block;
}

.nav-btn {
  font-size: 32rpx;
  color: var(--theme-ink);
  width: 72rpx;
}

.nav-title {
  font-size: 30rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.hero-card {
  margin-top: 20rpx;
  padding: 30rpx;
  border-radius: 30rpx;
  background: var(--theme-gradient);
}

.hero-top,
.row-between,
.meta-row,
.score-row,
.chip-row,
.stats-grid,
.summary-grid,
.bottom-actions,
.toolbar,
.inline-actions {
  display: flex;
}

.hero-top,
.row-between {
  justify-content: space-between;
  gap: 16rpx;
}

.course-name {
  font-size: 40rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.course-meta {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #6d6582;
}

.join-btn,
.status-chip,
.chip,
.action-pill,
.action-btn,
.toolbar-chip,
.more-btn {
  border-radius: 999rpx;
  font-size: 22rpx;
}

.join-btn {
  padding: 16rpx 24rpx;
  height: fit-content;
  background: var(--theme-ink);
  color: #fff;
}

.join-btn.joined {
  background: rgba(255, 255, 255, 0.82);
  color: var(--theme-primary-deep);
}

.course-desc {
  margin-top: 22rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #655e7b;
}

.chip-row {
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 18rpx;
}

.chip {
  padding: 6rpx 16rpx;
  background: rgba(140, 128, 216, 0.12);
  color: var(--theme-primary-deep);
}

.stats-grid,
.summary-grid {
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 22rpx;
}

.stat-card,
.summary-card {
  flex: 1;
  min-width: 140rpx;
  padding: 20rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.72);
}

.stat-num,
.summary-value {
  font-size: 36rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.stat-label,
.summary-label {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--theme-muted);
}

.tab-row {
  margin-top: 22rpx;
  padding: 8rpx;
  border-radius: 24rpx;
  gap: 8rpx;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 18rpx 10rpx;
  border-radius: 18rpx;
  font-size: 24rpx;
  color: var(--theme-muted);
}

.tab-item.active {
  background: var(--theme-gradient-strong);
  color: #fff;
  font-weight: 700;
}

.panel {
  margin-top: 18rpx;
  padding: 24rpx;
  border-radius: 28rpx;
}

.toolbar {
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 18rpx;
}

.toolbar-input {
  flex: 1;
  min-width: 260rpx;
  height: 78rpx;
  border-radius: 18rpx;
  background: rgba(245, 242, 251, 0.78);
  padding: 0 22rpx;
  font-size: 24rpx;
}

.toolbar-chip {
  padding: 16rpx 22rpx;
  background: rgba(140, 128, 216, 0.1);
  color: var(--theme-primary-deep);
}

.overview-card,
.list-card {
  margin-top: 18rpx;
  padding: 24rpx;
  border-radius: 24rpx;
}

.block-title {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.block-title.small {
  margin-top: 14rpx;
  font-size: 24rpx;
}

.overview-text,
.overview-sub {
  display: block;
  margin-top: 12rpx;
  color: #655e7b;
  font-size: 25rpx;
  line-height: 1.7;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.list-title {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--theme-ink);
  flex: 1;
}

.status-chip {
  padding: 8rpx 16rpx;
  background: rgba(140, 128, 216, 0.1);
  color: var(--theme-primary-deep);
}

.status-chip.resolved {
  background: rgba(82, 196, 26, 0.12);
  color: #2f855a;
}

.list-content {
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #655e7b;
  white-space: pre-wrap;
}

.meta-row,
.score-row,
.inline-actions {
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 14rpx;
  font-size: 22rpx;
  color: var(--theme-muted);
}

.action-link {
  color: var(--theme-primary-deep);
}

.more-btn {
  margin-top: 18rpx;
  align-self: center;
  padding: 16rpx 26rpx;
  background: rgba(140, 128, 216, 0.12);
  color: var(--theme-primary-deep);
  text-align: center;
}

.bottom-actions {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(20rpx + env(safe-area-inset-bottom));
  gap: 12rpx;
}

.action-btn,
.action-pill {
  flex: 1;
  text-align: center;
  padding: 20rpx 16rpx;
  background: rgba(255, 255, 255, 0.92);
  color: var(--theme-primary-deep);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
}

.action-btn.dark {
  background: var(--theme-ink);
  color: #fff;
}

.state-card,
.empty-card {
  margin-top: 22rpx;
  padding: 36rpx 28rpx;
  border-radius: 28rpx;
  text-align: center;
  font-size: 26rpx;
  color: var(--theme-muted);
}

.state-card.error {
  color: #b65473;
}
</style>
