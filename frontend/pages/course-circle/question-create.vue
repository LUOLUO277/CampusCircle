<template>
  <view class="page">
    <view class="nav-card">
      <text class="nav-btn" @click="goBack">&lt;</text>
      <text class="nav-title">发布课程问题</text>
      <text class="nav-btn"></text>
    </view>

    <view class="form-card">
      <text class="label">标题</text>
      <input
        v-model="form.title"
        class="input"
        type="text"
        maxlength="120"
        placeholder="例如：实验环境配置总是报错怎么办"
      />

      <text class="label">问题类型</text>
      <picker :range="types" @change="onTypeChange">
        <view class="input picker">{{ form.questionType || '请选择问题类型' }}</view>
      </picker>

      <text class="label">内容</text>
      <textarea
        v-model="form.content"
        class="textarea"
        maxlength="2000"
        auto-height
        placeholder="补充作业背景、报错信息、卡住的步骤。"
      ></textarea>
    </view>

    <view class="submit-btn" @click="submit">提交问题</view>
  </view>
</template>

<script>
import { createCourseQuestion } from '@/api/course-circle'

export default {
  data() {
    return {
      courseId: null,
      types: ['作业问题', '实验环境', '考试复习', '选课咨询', '项目组队', '其他'],
      form: {
        title: '',
        questionType: '作业问题',
        content: ''
      }
    }
  },
  onLoad(options) {
    this.courseId = Number(options.courseId || 0)
  },
  methods: {
    onTypeChange(event) {
      this.form.questionType = this.types[event.detail.value]
    },
    async submit() {
      if (!this.form.title.trim() || !this.form.content.trim()) {
        uni.showToast({ title: '请填写标题和内容', icon: 'none' })
        return
      }
      try {
        uni.showLoading({ title: '提交中...' })
        const res = await createCourseQuestion(this.courseId, {
          title: this.form.title,
          content: this.form.content,
          questionType: this.form.questionType
        })
        uni.hideLoading()
        uni.showToast({ title: '问题已发布', icon: 'success' })
        setTimeout(() => {
          uni.redirectTo({ url: `/pages/course-circle/question-detail?id=${res.data.id}` })
        }, 400)
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
  background: rgba(255, 255, 255, 0.86);
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
  background: rgba(245, 242, 251, 0.78);
  border-radius: 22rpx;
  padding: 22rpx 24rpx;
  font-size: 26rpx;
  color: var(--theme-ink);
}

.input {
  height: 88rpx;
}

.picker {
  min-height: 88rpx;
  line-height: 44rpx;
  color: var(--theme-ink);
}

.textarea {
  min-height: 280rpx;
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
