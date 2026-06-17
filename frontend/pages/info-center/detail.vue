<template>
  <view class="page" v-if="detail">
    <view class="detail-card">
      <view class="top-row">
        <text class="chip">{{ detail.category || '通知' }}</text>
        <text class="source">{{ detail.sourceName || '订阅来源' }}</text>
      </view>

      <text class="title">{{ detail.title }}</text>
      <text v-if="detail.summary" class="summary">{{ detail.summary }}</text>

      <view class="status-panel">
        <view class="status-row">
          <text class="status-label">处理状态</text>
          <text class="status-value" :class="{ completed: detail.completed, expired: detail.expired, due: detail.dueSoon }">
            {{ statusText }}
          </text>
        </view>
        <view class="status-row">
          <text class="status-label">截止信息</text>
          <text class="status-value">
            {{ detail.deadline ? formatTime(detail.deadline) : '该通知暂未识别到截止时间' }}
          </text>
        </view>
        <button
          v-if="canOperate"
          class="complete-btn"
          :class="{ secondary: detail.completed }"
          @click="toggleCompleted"
        >
          {{ detail.completed ? '取消完成' : '标记完成' }}
        </button>
      </view>

      <view class="info-grid">
        <view class="info-item">
          <text class="label">发布时间</text>
          <text class="value">{{ formatTime(detail.publishTime) }}</text>
        </view>
        <view v-if="detail.targetAudience" class="info-item">
          <text class="label">适用对象</text>
          <text class="value">{{ detail.targetAudience }}</text>
        </view>
        <view v-if="detail.location" class="info-item">
          <text class="label">地点</text>
          <text class="value">{{ detail.location }}</text>
        </view>
      </view>

      <view v-if="detail.tags && detail.tags.length" class="tag-row">
        <text v-for="tag in detail.tags" :key="tag" class="tag">{{ tag }}</text>
      </view>

      <text class="content">{{ detail.contentSnapshot || '暂无正文内容。' }}</text>

      <button v-if="detail.originalUrl" class="origin-btn" @click="openUrl(detail.originalUrl)">查看原文</button>
    </view>

    <view class="comment-card">
      <text class="section-title">通知讨论区</text>

      <view class="comment-editor">
        <textarea
          v-model="commentContent"
          class="textarea"
          maxlength="500"
          placeholder="补充说明、提问或提醒其他同学"
        />
        <button class="submit-btn" @click="submitComment">发表评论</button>
      </view>

      <view v-for="comment in comments" :key="comment.id" class="comment-item">
        <view class="comment-head">
          <text class="comment-user">{{ comment.username }}</text>
          <text class="comment-time">{{ formatTime(comment.time) }}</text>
        </view>
        <text class="comment-content">{{ comment.content }}</text>
        <view class="comment-actions">
          <text class="comment-action" @click="likeComment(comment.id)">点赞 {{ comment.likes || 0 }}</text>
          <text class="comment-action" @click="prepareReply(comment)">回复</text>
          <text v-if="comment.isMine" class="comment-action danger" @click="removeComment(comment.id)">删除</text>
        </view>

        <view v-for="reply in comment.replies || []" :key="reply.id" class="reply-item">
          <text class="reply-user">{{ reply.username }}<text v-if="reply.replyTo"> 回复 {{ reply.replyTo }}</text></text>
          <text class="reply-content">{{ reply.content }}</text>
        </view>
      </view>

      <view v-if="!comments.length" class="empty">还没有讨论，先发表第一条评论。</view>
    </view>
  </view>
</template>

<script>
import {
  getInfoNoticeDetail,
  getInfoNoticeComments,
  createInfoNoticeComment,
  deleteInfoComment,
  likeInfoComment,
  completeInfoNotice,
  uncompleteInfoNotice
} from '@/api/info-center'

export default {
  data() {
    return {
      id: '',
      detail: null,
      comments: [],
      commentContent: '',
      replyTarget: null
    }
  },
  computed: {
    canOperate() {
      return !!uni.getStorageSync('token')
    },
    statusText() {
      if (!this.detail) return ''
      if (this.detail.completed) return '已完成'
      if (this.detail.expired) return '已过期'
      if (this.detail.dueSoon) return '即将截止'
      if (this.detail.hasDeadline) return '有 DDL'
      return '未完成'
    }
  },
  onLoad(options) {
    this.id = options.id
    this.bootstrap()
  },
  methods: {
    async bootstrap() {
      await Promise.all([this.loadDetail(), this.loadComments()])
    },
    async loadDetail() {
      const res = await getInfoNoticeDetail(this.id)
      if (res.code === 200) {
        this.detail = res.data
      }
    },
    async loadComments() {
      const res = await getInfoNoticeComments(this.id)
      if (res.code === 200) {
        this.comments = res.data.list || []
      }
    },
    async toggleCompleted() {
      if (!this.canOperate) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      const wasCompleted = !!this.detail.completed
      const action = this.detail.completed ? uncompleteInfoNotice : completeInfoNotice
      await action(this.id)
      await this.loadDetail()
      uni.showToast({
        title: wasCompleted ? '已取消完成' : '已标记完成',
        icon: 'success'
      })
    },
    async submitComment() {
      const content = (this.commentContent || '').trim()
      if (!content) {
        uni.showToast({ title: '请输入评论内容', icon: 'none' })
        return
      }
      await createInfoNoticeComment(this.id, {
        content,
        parentId: this.replyTarget?.parentId || this.replyTarget?.id || null,
        replyToCommentId: this.replyTarget?.id || null
      })
      this.commentContent = ''
      this.replyTarget = null
      await this.loadComments()
      uni.showToast({ title: '评论已发布', icon: 'success' })
    },
    prepareReply(comment) {
      this.replyTarget = comment
      this.commentContent = `@${comment.username} `
    },
    async likeComment(commentId) {
      await likeInfoComment(commentId)
      await this.loadComments()
    },
    async removeComment(commentId) {
      await deleteInfoComment(commentId)
      await this.loadComments()
      uni.showToast({ title: '已删除', icon: 'success' })
    },
    openUrl(url) {
      // #ifdef H5
      window.open(url, '_blank')
      // #endif
      // #ifndef H5
      uni.setClipboardData({ data: url })
      uni.showToast({ title: '链接已复制', icon: 'none' })
      // #endif
    },
    formatTime(value) {
      if (!value) return ''
      return `${value}`.replace('T', ' ').slice(0, 16)
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
  padding: 30rpx;
}

.detail-card,
.comment-card {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 30rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow-soft);
}

.top-row,
.tag-row,
.comment-head,
.comment-actions,
.status-row {
  display: flex;
  gap: 14rpx;
  align-items: center;
}

.top-row {
  margin-bottom: 18rpx;
}

.chip {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(140, 128, 216, 0.12);
  color: var(--theme-primary-deep);
  font-size: 22rpx;
}

.source,
.summary,
.label,
.comment-time,
.status-label {
  color: #64748b;
}

.title {
  display: block;
  font-size: 38rpx;
  line-height: 1.45;
  color: #0f172a;
  font-weight: 700;
}

.summary,
.content,
.comment-content,
.reply-content {
  display: block;
  margin-top: 18rpx;
  line-height: 1.7;
  font-size: 26rpx;
}

.status-panel {
  margin-top: 24rpx;
  padding: 22rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, rgba(243, 236, 252, 0.8), rgba(255, 255, 255, 0.96));
}

.status-row {
  justify-content: space-between;
  padding: 10rpx 0;
}

.status-value {
  color: #1f2937;
  font-size: 24rpx;
}

.status-value.completed {
  color: #64748b;
}

.status-value.expired {
  color: #dc2626;
}

.status-value.due {
  color: #b45309;
}

.complete-btn,
.origin-btn,
.submit-btn {
  margin-top: 24rpx;
  border-radius: 999rpx;
  background: var(--theme-gradient-strong);
  color: #fff;
  font-size: 26rpx;
}

.complete-btn.secondary {
  background: #e2e8f0;
  color: #334155;
}

.complete-btn::after,
.origin-btn::after,
.submit-btn::after {
  border: none;
}

.info-grid {
  margin-top: 24rpx;
}

.info-item {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  padding: 14rpx 0;
  border-bottom: 1rpx solid #f1f5f9;
}

.value {
  color: #1f2937;
  font-size: 24rpx;
  text-align: right;
  flex: 1;
}

.tag-row {
  flex-wrap: wrap;
  margin-top: 20rpx;
}

.tag {
  padding: 6rpx 14rpx;
  background: #f1f5f9;
  border-radius: 999rpx;
  color: #334155;
  font-size: 22rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}

.textarea {
  width: 100%;
  min-height: 180rpx;
  background: #f8fafc;
  border-radius: 24rpx;
  padding: 22rpx;
  box-sizing: border-box;
  margin: 20rpx 0;
}

.comment-item {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #eef2f7;
}

.comment-user,
.reply-user {
  color: #0f172a;
  font-size: 26rpx;
  font-weight: 600;
}

.comment-actions {
  margin-top: 16rpx;
}

.comment-action {
  color: var(--theme-primary-deep);
  font-size: 24rpx;
}

.comment-action.danger {
  color: #dc2626;
}

.reply-item {
  margin-top: 16rpx;
  padding: 18rpx;
  background: #f8fafc;
  border-radius: 18rpx;
}

.empty {
  text-align: center;
  padding: 80rpx 0 20rpx;
  color: #64748b;
  font-size: 26rpx;
}
</style>
