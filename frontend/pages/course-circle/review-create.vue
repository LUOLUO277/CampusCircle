<template>
  <view class="page">
    <view class="nav-card">
      <text class="nav-btn" @click="goBack">&lt;</text>
      <text class="nav-title">写课程评价</text>
      <text class="nav-btn"></text>
    </view>

    <view class="form-card">
      <view v-for="item in scoreFields" :key="item.key">
        <text class="label">{{ item.label }}</text>
        <picker :range="scoreOptions" @change="(e) => onScoreChange(item.key, e)">
          <view class="input picker">{{ form[item.key] }}/5</view>
        </picker>
      </view>

      <text class="label">文字评价</text>
      <textarea
        v-model="form.content"
        class="textarea"
        maxlength="2000"
        auto-height
        placeholder="写下真实体验，例如考核方式、老师风格、投入产出比。"
      ></textarea>
      <text class="label">课程标签</text>
      <input
        v-model="form.tags"
        class="input"
        type="text"
        placeholder="例如：项目多,适合保研,期末压力大"
      />
    </view>

    <view class="submit-btn" @click="submit">提交评价</view>
  </view>
</template>

<script>
import { createCourseReview } from '@/api/course-circle'

export default {
  data() {
    return {
      courseId: null,
      scoreOptions: [1, 2, 3, 4, 5],
      scoreFields: [
        { key: 'difficultyRating', label: '课程难度' },
        { key: 'workloadRating', label: '作业量' },
        { key: 'gainRating', label: '收获程度' },
        { key: 'clarityRating', label: '教师讲解清晰度' },
        { key: 'examPressureRating', label: '考核压力' },
        { key: 'recommendRating', label: '推荐指数' }
      ],
      form: {
        difficultyRating: 3,
        workloadRating: 3,
        gainRating: 3,
        clarityRating: 3,
        examPressureRating: 3,
        recommendRating: 3,
        content: '',
        tags: ''
      }
    }
  },
  onLoad(options) {
    this.courseId = Number(options.courseId || 0)
  },
  methods: {
    onScoreChange(key, event) {
      this.form[key] = this.scoreOptions[event.detail.value]
    },
    async submit() {
      try {
        uni.showLoading({ title: '提交中...' })
        await createCourseReview(this.courseId, this.form)
        uni.hideLoading()
        uni.showToast({ title: '评价已提交', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 400)
      } catch (error) {
        uni.hideLoading()
        uni.showToast({ title: error.message || '提交失败', icon: 'none' })
      }
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
.form-card {
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
.label {
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

.form-card {
  margin-top: 22rpx;
  padding: 28rpx;
}

.label {
  margin-top: 18rpx;
  margin-bottom: 12rpx;
  font-size: 26rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.label:first-child {
  margin-top: 0;
}

.input,
.textarea {
  display: block;
  width: 100%;
  box-sizing: border-box;
  padding: 22rpx 24rpx;
  border-radius: 22rpx;
  background: rgba(245, 242, 251, 0.78);
  font-size: 26rpx;
  color: var(--theme-ink);
}

.input {
  height: 88rpx;
}

.picker {
  min-height: 88rpx;
  line-height: 44rpx;
}

.textarea {
  min-height: 220rpx;
}

.submit-btn {
  margin-top: 24rpx;
  height: 92rpx;
  line-height: 92rpx;
  text-align: center;
  border-radius: 999rpx;
  background: var(--theme-ink);
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
}
</style>
