<template>
  <view class="page">
    <view class="nav-card">
      <text class="nav-btn" @click="goBack">&lt;</text>
      <text class="nav-title">课程问答详情</text>
      <text class="nav-btn" @click="reload">↻</text>
    </view>

    <view v-if="loading" class="state-card">问题加载中...</view>
    <view v-else-if="error" class="state-card error">{{ error }}</view>
    <template v-else-if="question.id">
      <view class="question-card">
        <view class="row-between">
          <text class="title">{{ question.title }}</text>
          <text class="status-chip" :class="{ resolved: question.resolved }">
            {{ question.resolved ? '已解决' : question.questionType }}
          </text>
        </view>
        <text class="course-line">{{ question.course.name }} · {{ question.course.teacherName }}</text>
        <text class="content">{{ question.content }}</text>
        <view class="meta-row">
          <text>{{ question.author.name }}</text>
          <text>{{ formatTime(question.createdAt) }}</text>
          <text>{{ question.replyCount }} 条回复</text>
        </view>
        <view
          v-if="canResolve && !question.resolved"
          class="resolve-btn"
          @click="markResolved"
        >
          标记已解决
        </view>
      </view>

      <view class="reply-card">
        <text class="block-title">全部回复</text>
        <view v-if="question.replies.length" class="reply-list">
          <view v-for="reply in question.replies" :key="reply.id" class="reply-item">
            <view class="reply-head">
              <text class="reply-author">{{ reply.author.name }}</text>
              <text class="reply-time">{{ formatTime(reply.createdAt) }}</text>
            </view>
            <text class="reply-content">{{ reply.content }}</text>
          </view>
        </view>
        <view v-else class="empty-card">还没有回复，欢迎补充思路和经验。</view>
      </view>

      <view class="composer">
        <textarea v-model="replyContent" class="textarea" placeholder="写下你的回复、思路或解决办法。" />
        <view class="send-btn" @click="submitReply">发送回复</view>
      </view>
    </template>
  </view>
</template>

<script>
import { getCourseQuestionDetail, createCourseQuestionReply, resolveCourseQuestion } from '@/api/course-circle'

export default {
  data() {
    return {
      questionId: null,
      loading: false,
      error: '',
      question: { replies: [], author: {}, course: {} },
      replyContent: ''
    }
  },
  computed: {
    canResolve() {
      const currentUser = uni.getStorageSync('userInfo') || {}
      return currentUser.id && currentUser.id === this.question.author.id
    }
  },
  onLoad(options) {
    this.questionId = Number(options.id || 0)
    this.reload()
  },
  methods: {
    async reload() {
      this.loading = true
      this.error = ''
      try {
        const res = await getCourseQuestionDetail(this.questionId)
        this.question = res.data || { replies: [], author: {}, course: {} }
      } catch (error) {
        this.error = error.message || '问答详情加载失败'
      } finally {
        this.loading = false
      }
    },
    async submitReply() {
      if (!this.replyContent.trim()) {
        uni.showToast({ title: '请输入回复内容', icon: 'none' })
        return
      }
      try {
        const res = await createCourseQuestionReply(this.questionId, { content: this.replyContent })
        this.question.replies.push(res.data)
        this.question.replyCount += 1
        this.replyContent = ''
        uni.showToast({ title: '回复成功', icon: 'success' })
      } catch (error) {
        uni.showToast({ title: error.message || '回复失败', icon: 'none' })
      }
    },
    async markResolved() {
      try {
        const res = await resolveCourseQuestion(this.questionId)
        this.question = res.data
        uni.showToast({ title: '已标记解决', icon: 'success' })
      } catch (error) {
        uni.showToast({ title: error.message || '操作失败', icon: 'none' })
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
.question-card,
.reply-card,
.composer,
.state-card,
.empty-card {
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
.course-line,
.content,
.block-title,
.reply-author,
.reply-content {
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

.question-card,
.reply-card,
.composer {
  margin-top: 22rpx;
  padding: 28rpx;
}

.row-between,
.meta-row,
.reply-head {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
}

.title {
  flex: 1;
  font-size: 34rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.status-chip,
.resolve-btn,
.send-btn {
  border-radius: 999rpx;
  font-size: 22rpx;
}

.status-chip {
  padding: 8rpx 16rpx;
  height: fit-content;
  background: rgba(140, 128, 216, 0.1);
  color: var(--theme-primary-deep);
}

.status-chip.resolved {
  background: rgba(82, 196, 26, 0.12);
  color: #2f855a;
}

.course-line {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--theme-muted);
}

.content {
  margin-top: 18rpx;
  font-size: 26rpx;
  line-height: 1.8;
  color: #655e7b;
  white-space: pre-wrap;
}

.meta-row {
  margin-top: 18rpx;
  flex-wrap: wrap;
  font-size: 22rpx;
  color: var(--theme-muted);
}

.resolve-btn,
.send-btn {
  margin-top: 20rpx;
  display: inline-block;
  padding: 16rpx 24rpx;
  background: var(--theme-ink);
  color: #fff;
}

.block-title {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.reply-list {
  margin-top: 18rpx;
}

.reply-item {
  padding: 20rpx 0;
  border-bottom: 1rpx solid rgba(140, 128, 216, 0.08);
}

.reply-item:last-child {
  border-bottom: none;
}

.reply-author {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.reply-time {
  font-size: 22rpx;
  color: var(--theme-muted);
}

.reply-content {
  margin-top: 12rpx;
  font-size: 25rpx;
  line-height: 1.7;
  color: #655e7b;
  white-space: pre-wrap;
}

.textarea {
  width: 100%;
  min-height: 220rpx;
  box-sizing: border-box;
  padding: 22rpx 24rpx;
  border-radius: 22rpx;
  background: rgba(245, 242, 251, 0.78);
  font-size: 26rpx;
  color: var(--theme-ink);
}

.state-card,
.empty-card {
  margin-top: 22rpx;
  padding: 36rpx 28rpx;
  text-align: center;
  color: var(--theme-muted);
  font-size: 26rpx;
}

.state-card.error {
  color: #b65473;
}
</style>
