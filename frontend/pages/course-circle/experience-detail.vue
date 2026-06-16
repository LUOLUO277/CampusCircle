<template>
  <view class="page">
    <view class="nav-card">
      <text class="nav-btn" @click="goBack">&lt;</text>
      <text class="nav-title">经验详情</text>
      <text class="nav-btn"></text>
    </view>

    <view v-if="loading" class="state-card">经验内容加载中...</view>
    <view v-else-if="error" class="state-card error">{{ error }}</view>
    <view v-else-if="detail.id" class="detail-card">
      <view class="row-between">
        <text class="title">{{ detail.courseName || detail.semester }}</text>
        <text class="badge">{{ detail.recommendRating }}/5 推荐</text>
      </view>
      <text class="meta">{{ detail.semester }} · {{ detail.teacherName }}</text>
      <view class="score-row">
        <text>难度 {{ detail.difficultyRating }}</text>
        <text>作业 {{ detail.workloadRating }}</text>
        <text>收获 {{ detail.gainRating }}</text>
        <text>压力 {{ detail.examPressureRating }}</text>
      </view>
      <text class="block-title">学习建议</text>
      <text class="content">{{ detail.studyAdvice }}</text>
      <text class="block-title">避坑建议</text>
      <text class="content">{{ detail.pitfallAdvice }}</text>
      <text class="block-title">适合人群</text>
      <text class="content">{{ detail.suitableFor }}</text>
      <view class="meta-row">
        <text>{{ detail.author?.name }}</text>
        <text>{{ formatTime(detail.createdAt) }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getCourseExperienceDetail } from '@/api/course-circle'

export default {
  data() {
    return {
      experienceId: null,
      loading: false,
      error: '',
      detail: {}
    }
  },
  onLoad(options) {
    this.experienceId = Number(options.id || 0)
    this.reload()
  },
  methods: {
    async reload() {
      this.loading = true
      this.error = ''
      try {
        const res = await getCourseExperienceDetail(this.experienceId)
        this.detail = res.data || {}
      } catch (error) {
        this.error = error.message || '经验详情加载失败'
      } finally {
        this.loading = false
      }
    },
    formatTime(value) {
      return `${value || ''}`.replace('T', ' ').slice(0, 16)
    },
    goBack() {
      uni.navigateBack()
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.2), transparent 30%),
    linear-gradient(180deg, #faf7f2 0%, #f5f1eb 100%);
}

.nav-card,
.detail-card,
.state-card {
  background: rgba(255, 255, 255, 0.88);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow-soft);
  border-radius: 28rpx;
}

.nav-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18rpx 22rpx;
}

.nav-btn,
.nav-title,
.title,
.meta,
.block-title,
.content {
  display: block;
}

.nav-btn {
  width: 72rpx;
  font-size: 32rpx;
}

.nav-title {
  font-size: 30rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.detail-card {
  margin-top: 22rpx;
  padding: 28rpx;
}

.row-between,
.score-row,
.meta-row {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  flex-wrap: wrap;
}

.title {
  flex: 1;
  font-size: 34rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.badge {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(140, 128, 216, 0.1);
  color: var(--theme-primary-deep);
  font-size: 22rpx;
}

.meta,
.score-row,
.meta-row {
  margin-top: 14rpx;
  font-size: 22rpx;
  color: var(--theme-muted);
}

.block-title {
  margin-top: 22rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.content {
  margin-top: 12rpx;
  font-size: 25rpx;
  line-height: 1.8;
  color: #655e7b;
  white-space: pre-wrap;
}

.state-card {
  margin-top: 22rpx;
  padding: 36rpx 28rpx;
  text-align: center;
  font-size: 26rpx;
  color: var(--theme-muted);
}

.state-card.error {
  color: #b65473;
}
</style>
