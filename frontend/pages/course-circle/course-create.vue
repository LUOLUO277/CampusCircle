<template>
  <view class="page">
    <view class="nav-card">
      <text class="nav-btn" @click="goBack">&lt;</text>
      <text class="nav-title">新增课程</text>
      <text class="nav-btn"></text>
    </view>

    <view class="form-card">
      <text class="label">课程名称</text>
      <input v-model="form.name" class="input" type="text" placeholder="例如：机器学习导论" />
      <text class="label">任课教师</text>
      <input v-model="form.teacherName" class="input" type="text" placeholder="输入任课教师姓名" />
      <text class="label">课程编号</text>
      <input v-model="form.courseCode" class="input" type="text" placeholder="例如：AI3201" />
      <text class="label">开课学院</text>
      <input v-model="form.department" class="input" type="text" placeholder="例如：人工智能学院" />
      <text class="label">学期</text>
      <input v-model="form.semester" class="input" type="text" placeholder="例如：2026春季" />
      <text class="label">课程标签</text>
      <input v-model="form.tags" class="input" type="text" placeholder="例如：项目驱动,实验课,保研向" />
      <text class="label">课程简介</text>
      <textarea v-model="form.description" class="textarea" maxlength="2000" auto-height placeholder="简要介绍课程内容、节奏和适合人群。"></textarea>
    </view>

    <view class="submit-btn" @click="submit">提交课程</view>
  </view>
</template>

<script>
import { createCourse } from '@/api/course-circle'

export default {
  data() {
    return {
      form: {
        name: '',
        teacherName: '',
        courseCode: '',
        department: '',
        semester: '',
        tags: '',
        description: ''
      }
    }
  },
  methods: {
    async submit() {
      const required = ['name', 'teacherName', 'courseCode', 'department', 'semester', 'description']
      if (required.some(key => !`${this.form[key] || ''}`.trim())) {
        uni.showToast({ title: '请填写完整课程信息', icon: 'none' })
        return
      }
      try {
        uni.showLoading({ title: '提交中...' })
        const res = await createCourse(this.form)
        uni.hideLoading()
        uni.showToast({ title: '课程已创建', icon: 'success' })
        setTimeout(() => {
          uni.redirectTo({ url: `/pages/course-circle/detail?id=${res.data.id}` })
        }, 400)
      } catch (error) {
        uni.hideLoading()
        uni.showToast({ title: error.message || '创建失败', icon: 'none' })
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
