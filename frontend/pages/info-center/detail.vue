<template>
  <view class="page" v-if="detail">
    <view class="detail-card">
      <view class="top-row">
        <text class="chip">{{ detail.category || '通知' }}</text>
        <text class="source">{{ detail.sourceName }}</text>
      </view>
      <text class="title">{{ detail.title }}</text>
      <text v-if="detail.summary" class="summary">{{ detail.summary }}</text>

      <view class="info-grid">
        <view class="info-item"><text class="label">发布时间</text><text class="value">{{ formatTime(detail.publishTime) }}</text></view>
        <view v-if="detail.deadline" class="info-item"><text class="label">截止时间</text><text class="value">{{ formatTime(detail.deadline) }}</text></view>
        <view v-if="detail.targetAudience" class="info-item"><text class="label">适用对象</text><text class="value">{{ detail.targetAudience }}</text></view>
        <view v-if="detail.location" class="info-item"><text class="label">地点</text><text class="value">{{ detail.location }}</text></view>
      </view>

      <view v-if="detail.tags && detail.tags.length" class="tag-row">
        <text v-for="tag in detail.tags" :key="tag" class="tag">{{ tag }}</text>
      </view>

      <text class="content">{{ detail.contentSnapshot || '暂无正文快照' }}</text>

      <button v-if="detail.originalUrl" class="origin-btn" @click="openUrl(detail.originalUrl)">查看原文</button>
    </view>

    <view class="comment-card">
      <text class="section-title">通知讨论区</text>

      <view class="comment-editor">
        <textarea v-model="commentContent" class="textarea" maxlength="500" placeholder="写下你的讨论、补充或提醒" />
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

      <view v-if="!comments.length" class="empty">还没有讨论，先发第一条。</view>
    </view>
  </view>
</template>

<script>
import {
  getInfoNoticeDetail,
  getInfoNoticeComments,
  createInfoNoticeComment,
  deleteInfoComment,
  likeInfoComment
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
      return value.replace('T', ' ').slice(0, 16)
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7f5;
  padding: 30rpx;
}
.detail-card,
.comment-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
}
.top-row,
.tag-row,
.comment-head,
.comment-actions {
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
  background: #edf7ee;
  color: #1f5f46;
  font-size: 22rpx;
}
.source,
.summary,
.label,
.comment-time {
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
.origin-btn,
.submit-btn {
  margin-top: 24rpx;
  border-radius: 999rpx;
  background: #1f5f46;
  color: #fff;
  font-size: 26rpx;
}
.origin-btn::after,
.submit-btn::after {
  border: none;
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
  color: #2f855a;
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
