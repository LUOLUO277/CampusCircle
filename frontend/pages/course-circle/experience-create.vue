<template>
  <view class="page">
    <view class="nav-card">
      <text class="nav-btn" @click="goBack">&lt;</text>
      <text class="nav-title">分享课程经验</text>
      <text class="nav-btn"></text>
    </view>

    <view class="form-card">
      <text class="label">学期</text>
      <input v-model="form.semester" class="input" type="text" placeholder="例如：2026春季" />
      <text class="label">任课教师</text>
      <input v-model="form.teacherName" class="input" type="text" placeholder="输入教师姓名" />

      <view v-for="item in scoreFields" :key="item.key">
        <text class="label">{{ item.label }}</text>
        <picker :range="scoreOptions" @change="(e) => onScoreChange(item.key, e)">
          <view class="input picker">{{ form[item.key] }}/5</view>
        </picker>
      </view>

      <text class="label">学习建议</text>
      <textarea
        v-model="form.studyAdvice"
        class="textarea"
        maxlength="2000"
        auto-height
        placeholder="建议从哪里开始学，哪些资料更有效。"
      ></textarea>
      <text class="label">避坑建议</text>
      <textarea
        v-model="form.pitfallAdvice"
        class="textarea"
        maxlength="2000"
        auto-height
        placeholder="哪些环节最容易踩坑，怎么提前规避。"
      ></textarea>
      <text class="label">适合人群</text>
      <textarea
        v-model="form.suitableFor"
        class="textarea"
        maxlength="2000"
        auto-height
        placeholder="更适合哪些背景、目标或时间安排的同学。"
      ></textarea>
    </view>

    <view class="submit-btn" @click="submit">发布经验</view>
  </view>
</template>

<script>
import { createCourseExperience } from '@/api/course-circle'

export default {
  data() {
    return {
      courseId: null,
      scoreOptions: [1, 2, 3, 4, 5],
      scoreFields: [
        { key: 'difficultyRating', label: '课程难度' },
        { key: 'workloadRating', label: '作业强度' },
        { key: 'gainRating', label: '收获程度' },
        { key: 'examPressureRating', label: '考核压力' },
        { key: 'recommendRating', label: '推荐指数' }
      ],
      form: {
        semester: '',
        teacherName: '',
        difficultyRating: 3,
        workloadRating: 3,
        gainRating: 3,
        examPressureRating: 3,
        recommendRating: 3,
        studyAdvice: '',
        pitfallAdvice: '',
        suitableFor: ''
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
      const required = ['semester', 'teacherName', 'studyAdvice', 'pitfallAdvice', 'suitableFor']
      if (required.some(key => !`${this.form[key] || ''}`.trim())) {
        uni.showToast({ title: '请填写完整经验内容', icon: 'none' })
        return
      }
      try {
        uni.showLoading({ title: '发布中...' })
        await createCourseExperience(this.courseId, this.form)
        uni.hideLoading()
        uni.showToast({ title: '经验已发布', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 400)
      } catch (error) {
        uni.hideLoading()
        uni.showToast({ title: error.message || '发布失败', icon: 'none' })
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
