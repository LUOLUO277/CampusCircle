<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view>
          <text class="hero-title">AI 校园助手</text>
          <text class="hero-subtitle">
            可以结合引用来源，回答通知、帖子和课程圈相关内容。
          </text>
        </view>
        <view class="hero-actions">
          <view class="hero-btn" @click="startNewSession">新建对话</view>
          <view class="hero-btn primary" @click="historyVisible = true">历史记录</view>
        </view>
      </view>

      <view class="session-bar">
        <text class="session-label">{{ activeSessionTitle || '新对话' }}</text>
        <view class="session-mini-actions">
          <text class="mini-link" @click="handleClearSession">清空</text>
          <text class="mini-link todo" @click="showRegenerateTodo">重新生成</text>
        </view>
      </view>
    </view>

    <view v-if="!isLoggedIn" class="login-state">
      <text class="state-title">登录后才可以保存 AI 对话记录。</text>
      <text class="state-subtitle">请先登录，再开始提问并查看历史会话。</text>
      <view class="login-btn" @click="goLogin">去登录</view>
    </view>

    <template v-else>
      <scroll-view
        scroll-y
        class="chat-scroll"
        :scroll-into-view="scrollIntoView"
        :show-scrollbar="false"
      >
        <view class="chat-inner">
          <view v-if="bootLoading" class="state-card">正在加载聊天记录...</view>

          <template v-else-if="messages.length">
            <view
              v-for="message in messages"
              :id="`message-${message.localId || message.id}`"
              :key="message.localId || message.id"
              class="message-row"
              :class="message.role === 'USER' ? 'user-row' : 'assistant-row'"
            >
              <view v-if="message.role === 'USER'" class="user-bubble">
                <text class="message-text">{{ message.content }}</text>
              </view>

              <view v-else class="assistant-card">
                <view class="assistant-head">
                  <text class="assistant-badge">助手</text>
                  <text v-if="message.loading" class="assistant-stage">{{ message.stage }}</text>
                  <view v-else class="assistant-tools">
                    <text class="tool-link" @click="copyAnswer(message.content)">复制</text>
                  </view>
                </view>

                <view v-if="message.loading" class="loading-wrap">
                  <view class="dot-group">
                    <text class="dot" />
                    <text class="dot" />
                    <text class="dot" />
                  </view>
                </view>

                <rich-text
                  v-else
                  class="assistant-markdown"
                  :nodes="renderMarkdown(message.content)"
                />

                <view v-if="!message.loading && message.sources?.length" class="source-wrap">
                  <text class="source-title">引用来源</text>
                  <view
                    v-for="source in message.sources"
                    :key="`${source.sourceType}-${source.sourceId}-${source.id || ''}`"
                    class="source-card"
                    @click="openSource(source)"
                  >
                    <view class="source-head">
                      <text class="source-type">{{ sourceTypeLabel(source.sourceType) }}</text>
                      <text class="source-time">{{ formatTime(source.publishedAt) }}</text>
                    </view>
                    <text class="source-name">{{ source.title }}</text>
                    <text class="source-summary">{{ source.summary || '点击查看详情' }}</text>
                  </view>
                </view>
              </view>
            </view>
          </template>

          <view v-else class="empty-state">
            <text class="empty-title">可以直接提问校园相关内容。</text>
            <text class="empty-copy">AI 回复支持更清晰的列表、摘要和引用来源展示。</text>
          </view>

          <view v-if="pageError" class="state-card error">{{ pageError }}</view>
          <view id="message-anchor" />
        </view>
      </scroll-view>

      <view class="composer-shell">
        <view class="composer">
          <textarea
            v-model="inputValue"
            class="composer-input"
            auto-height
            maxlength="-1"
            placeholder="输入你的问题，比如：最近有哪些重要通知？"
            :disabled="sending"
            @confirm="submitQuestion"
          />
          <view class="send-btn" :class="{ disabled: sending }" @click="submitQuestion">
            发送
          </view>
        </view>
      </view>

      <view v-if="historyVisible" class="history-mask" @click="historyVisible = false">
        <view class="history-panel" @click.stop>
          <view class="history-head">
            <text class="history-title">历史记录</text>
            <text class="history-close" @click="historyVisible = false">关闭</text>
          </view>

          <view class="history-body">
            <view v-if="sessionLoading" class="history-state">正在加载会话...</view>
            <view v-else-if="!sessions.length" class="history-state">暂时还没有保存的会话。</view>
            <view
              v-for="session in sessions"
              :key="session.id"
              class="history-item"
              :class="{ active: Number(session.id) === Number(activeSessionId) }"
              @click="openSession(session.id)"
            >
              <view class="history-copy">
                <text class="history-item-title">{{ session.title || '未命名对话' }}</text>
                <text class="history-item-preview">{{ session.preview || '暂无消息' }}</text>
                <text class="history-item-time">{{ formatTime(session.updatedAt) }}</text>
              </view>
              <text class="history-delete" @click.stop="removeSession(session.id)">删除</text>
            </view>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { ask, deleteSession, getSessionDetail, getSessions } from '@/api/ai-chat'

const userStore = useUserStore()

const inputValue = ref('')
const sending = ref(false)
const bootLoading = ref(false)
const sessionLoading = ref(false)
const historyVisible = ref(false)
const pageError = ref('')
const messages = ref([])
const sessions = ref([])
const activeSessionId = ref(null)
const activeSessionTitle = ref('新对话')
const scrollIntoView = ref('')

const isLoggedIn = computed(() => userStore.isLoggedIn)

onLoad((options) => {
  if (options?.sessionId) {
    activeSessionId.value = Number(options.sessionId)
  }
  bootstrap()
})

onShow(() => {
  if (isLoggedIn.value) {
    loadSessions()
  }
})

const bootstrap = async () => {
  if (!isLoggedIn.value) return
  bootLoading.value = true
  pageError.value = ''
  try {
    await loadSessions()
    if (activeSessionId.value) {
      await openSession(activeSessionId.value, false)
    }
  } catch (error) {
    pageError.value = error.message || 'AI 对话初始化失败。'
  } finally {
    bootLoading.value = false
  }
}

const loadSessions = async () => {
  if (!isLoggedIn.value) return
  sessionLoading.value = true
  try {
    const res = await getSessions()
    sessions.value = Array.isArray(res.data) ? res.data : []
  } finally {
    sessionLoading.value = false
  }
}

const openSession = async (sessionId, closeHistory = true) => {
  if (!sessionId) return
  const res = await getSessionDetail(sessionId)
  const data = res.data || {}
  activeSessionId.value = data.id
  activeSessionTitle.value = data.title || '新对话'
  messages.value = (data.messages || []).map((item) => ({
    ...item,
    sources: Array.isArray(item.sources) ? item.sources : []
  }))
  if (closeHistory) {
    historyVisible.value = false
  }
  await scrollToBottom()
}

const startNewSession = () => {
  activeSessionId.value = null
  activeSessionTitle.value = '新对话'
  messages.value = []
  pageError.value = ''
  historyVisible.value = false
  inputValue.value = ''
}

const handleClearSession = () => {
  if (!activeSessionId.value) {
    startNewSession()
    return
  }
  uni.showModal({
    title: '清空当前对话',
    content: '这会删除当前 AI 会话及其消息，是否继续？',
    success: async (res) => {
      if (!res.confirm) return
      await removeSession(activeSessionId.value, true)
    }
  })
}

const removeSession = async (sessionId, silent = false) => {
  await deleteSession(sessionId)
  sessions.value = sessions.value.filter((item) => Number(item.id) !== Number(sessionId))
  if (Number(activeSessionId.value) === Number(sessionId)) {
    startNewSession()
  }
  if (!silent) {
    uni.showToast({ title: '会话已删除', icon: 'none' })
  }
}

const submitQuestion = async () => {
  const question = (inputValue.value || '').trim()
  if (!question || sending.value || !isLoggedIn.value) return

  sending.value = true
  pageError.value = ''
  const userMessage = {
    localId: `u-${Date.now()}`,
    role: 'USER',
    content: question,
    createdAt: new Date().toISOString(),
    sources: []
  }
  const loadingMessage = {
    localId: `a-${Date.now()}`,
    role: 'ASSISTANT',
    content: '',
    loading: true,
    stage: '正在检索校园内容...'
  }

  messages.value = [...messages.value, userMessage, loadingMessage]
  inputValue.value = ''
  await scrollToBottom()

  try {
    const res = await ask({ sessionId: activeSessionId.value, question })
    const data = res.data || {}
    activeSessionId.value = data.sessionId
    activeSessionTitle.value = data.sessionTitle || activeSessionTitle.value || '新对话'
    messages.value = [
      ...messages.value.slice(0, -1),
      {
        id: data.messageId,
        role: 'ASSISTANT',
        content: data.answer || '暂时没有检索到足够的平台信息。',
        createdAt: new Date().toISOString(),
        sources: Array.isArray(data.sources) ? data.sources : []
      }
    ]
    await loadSessions()
  } catch (error) {
    messages.value = [
      ...messages.value.slice(0, -1),
      {
        localId: `e-${Date.now()}`,
        role: 'ASSISTANT',
        content: error.message || 'AI 回复失败，请稍后重试。',
        createdAt: new Date().toISOString(),
        sources: []
      }
    ]
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

const scrollToBottom = async () => {
  await nextTick()
  scrollIntoView.value = ''
  await nextTick()
  scrollIntoView.value = 'message-anchor'
}

const copyAnswer = (content) => {
  uni.setClipboardData({
    data: content || '',
    success: () => uni.showToast({ title: '已复制', icon: 'none' })
  })
}

const openSource = (source) => {
  if (!source?.route) {
    uni.showToast({ title: '来源页面暂时无法打开', icon: 'none' })
    return
  }
  uni.navigateTo({ url: source.route })
}

const showRegenerateTodo = () => {
  uni.showToast({ title: '重新生成功能暂未接入', icon: 'none' })
}

const sourceTypeLabel = (sourceType) => {
  const mapping = {
    NOTICE: '通知',
    POST: '帖子',
    COURSE: '课程',
    COURSE_QUESTION: '课程问答',
    COURSE_EXPERIENCE: '课程经验'
  }
  return mapping[sourceType] || sourceType || '来源'
}

const formatTime = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').replace(/\+\d{2}:\d{2}$/, '')
}

const goLogin = () => {
  uni.navigateTo({ url: '/pages/login/index' })
}

const escapeHtml = (value = '') =>
  String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

const applyInlineMarkdown = (value = '') => {
  let output = escapeHtml(value)
  output = output.replace(
    /\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g,
    '<a href="$2" style="color:#6f5bd3;text-decoration:none;">$1</a>'
  )
  output = output.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  output = output.replace(/\*([^*]+)\*/g, '<em>$1</em>')
  output = output.replace(
    /`([^`]+)`/g,
    '<code style="background:#f4efff;color:#5b4aa8;padding:2px 8px;border-radius:8px;font-family:monospace;">$1</code>'
  )
  return output
}

const renderMarkdown = (content = '') => {
  const normalized = String(content || '').replace(/\r\n/g, '\n').trim()
  if (!normalized) {
    return '<div style="color:#6f7487;line-height:1.8;">暂无回复内容。</div>'
  }

  const blocks = normalized.split(/\n{2,}/)
  const html = blocks
    .map((block) => {
      if (/^```/.test(block) && /```$/.test(block)) {
        const code = block.replace(/^```[^\n]*\n?/, '').replace(/\n?```$/, '')
        return `<pre style="margin:0 0 18px;padding:18px 20px;background:#221f2e;color:#f7f3ff;border-radius:18px;overflow:auto;white-space:pre-wrap;line-height:1.7;"><code>${escapeHtml(code)}</code></pre>`
      }

      if (/^>\s?/m.test(block)) {
        const quote = block
          .split('\n')
          .map((line) => line.replace(/^>\s?/, ''))
          .join('<br/>')
        return `<blockquote style="margin:0 0 18px;padding:4px 0 4px 18px;border-left:6px solid #cdbdf6;color:#5a6074;">${applyInlineMarkdown(quote)}</blockquote>`
      }

      if (/^#{1,6}\s/.test(block)) {
        const line = block.split('\n')[0]
        const level = Math.min((line.match(/^#+/) || ['#'])[0].length, 6)
        const text = line.replace(/^#{1,6}\s*/, '')
        const sizeMap = { 1: 34, 2: 32, 3: 30, 4: 28, 5: 26, 6: 24 }
        return `<div style="margin:0 0 16px;font-size:${sizeMap[level]}rpx;font-weight:700;color:#2e3242;line-height:1.5;">${applyInlineMarkdown(text)}</div>`
      }

      if (/^[-*]\s+/m.test(block)) {
        const items = block
          .split('\n')
          .filter((line) => /^[-*]\s+/.test(line))
          .map(
            (line) =>
              `<li style="margin:0 0 10px 0;">${applyInlineMarkdown(line.replace(/^[-*]\s+/, ''))}</li>`
          )
          .join('')
        return `<ul style="margin:0 0 18px;padding-left:28px;color:#2e3242;line-height:1.8;">${items}</ul>`
      }

      if (/^\d+\.\s+/m.test(block)) {
        const items = block
          .split('\n')
          .filter((line) => /^\d+\.\s+/.test(line))
          .map(
            (line) =>
              `<li style="margin:0 0 10px 0;">${applyInlineMarkdown(line.replace(/^\d+\.\s+/, ''))}</li>`
          )
          .join('')
        return `<ol style="margin:0 0 18px;padding-left:34px;color:#2e3242;line-height:1.8;">${items}</ol>`
      }

      return `<div style="margin:0 0 18px;color:#2e3242;line-height:1.9;font-size:27rpx;">${applyInlineMarkdown(
        block
      ).replace(/\n/g, '<br/>')}</div>`
    })
    .join('')

  return `<div>${html}</div>`
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.18), transparent 28%),
    linear-gradient(180deg, #faf7f2 0%, #f3eff8 100%);
  padding: 24rpx 24rpx 260rpx;
}

.hero {
  background: var(--theme-gradient);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow-soft);
  border-radius: 32rpx;
  padding: 28rpx;
}

.hero-top,
.hero-actions,
.session-bar,
.session-mini-actions,
.assistant-head,
.source-head,
.history-head,
.history-item,
.history-copy,
.composer,
.dot-group {
  display: flex;
}

.hero-top,
.session-bar,
.history-head,
.history-item,
.assistant-head,
.source-head,
.composer {
  justify-content: space-between;
  align-items: center;
}

.hero-title {
  display: block;
  font-size: 40rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.hero-subtitle {
  display: block;
  margin-top: 10rpx;
  color: var(--theme-muted);
  font-size: 24rpx;
  line-height: 1.6;
}

.hero-actions {
  gap: 14rpx;
  margin-left: 20rpx;
}

.hero-btn,
.login-btn,
.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  font-size: 24rpx;
}

.hero-btn {
  min-width: 144rpx;
  height: 64rpx;
  padding: 0 24rpx;
  color: var(--theme-ink);
  background: rgba(255, 255, 255, 0.74);
}

.hero-btn.primary,
.login-btn,
.send-btn {
  background: var(--theme-ink);
  color: #fff;
}

.session-bar {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid rgba(140, 128, 216, 0.1);
}

.session-label {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.session-mini-actions {
  gap: 18rpx;
}

.mini-link,
.tool-link,
.history-close,
.history-delete {
  font-size: 22rpx;
  color: var(--theme-primary-deep);
}

.mini-link.todo {
  color: #9c86cf;
}

.chat-scroll {
  height: calc(100vh - 500rpx);
  margin-top: 20rpx;
}

.chat-inner {
  padding-bottom: 24rpx;
}

.message-row {
  display: flex;
  margin-bottom: 24rpx;
}

.user-row {
  justify-content: flex-end;
}

.assistant-row {
  justify-content: flex-start;
}

.user-bubble {
  max-width: 76%;
  background: var(--theme-gradient-strong);
  color: #fff;
  border-radius: 28rpx 28rpx 12rpx 28rpx;
  padding: 22rpx 24rpx;
  box-shadow: 0 12rpx 26rpx rgba(126, 111, 189, 0.18);
}

.assistant-card,
.state-card,
.empty-state,
.login-state,
.history-panel {
  background: rgba(255, 255, 255, 0.92);
  border: 1rpx solid rgba(140, 128, 216, 0.1);
  box-shadow: var(--theme-shadow-soft);
}

.assistant-card {
  width: 100%;
  border-radius: 28rpx;
  padding: 24rpx;
}

.assistant-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 54rpx;
  height: 54rpx;
  border-radius: 18rpx;
  background: rgba(140, 128, 216, 0.14);
  color: var(--theme-primary-deep);
  font-size: 24rpx;
  font-weight: 700;
}

.assistant-stage {
  margin-left: 16rpx;
  color: var(--theme-muted);
  font-size: 22rpx;
}

.assistant-tools {
  margin-left: auto;
}

.message-text {
  display: block;
  margin-top: 20rpx;
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: 27rpx;
}

.assistant-markdown {
  display: block;
  margin-top: 20rpx;
}

.loading-wrap {
  margin-top: 18rpx;
}

.dot-group {
  gap: 10rpx;
}

.dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: rgba(140, 128, 216, 0.54);
  animation: blink 1.2s infinite ease-in-out;
}

.dot:nth-child(2) {
  animation-delay: 0.15s;
}

.dot:nth-child(3) {
  animation-delay: 0.3s;
}

.source-wrap {
  margin-top: 20rpx;
}

.source-title {
  display: block;
  margin-bottom: 14rpx;
  font-size: 24rpx;
  color: var(--theme-muted);
}

.source-card {
  padding: 20rpx;
  border-radius: 22rpx;
  background: rgba(246, 242, 253, 0.72);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  margin-bottom: 14rpx;
}

.source-type {
  color: var(--theme-primary-deep);
  font-size: 22rpx;
  font-weight: 700;
}

.source-time {
  color: var(--theme-muted);
  font-size: 22rpx;
}

.source-name {
  display: block;
  margin-top: 10rpx;
  color: var(--theme-ink);
  font-size: 28rpx;
  font-weight: 700;
}

.source-summary {
  display: block;
  margin-top: 10rpx;
  color: #555d72;
  font-size: 24rpx;
  line-height: 1.6;
}

.state-card,
.empty-state,
.login-state {
  border-radius: 28rpx;
  padding: 34rpx;
  margin-top: 20rpx;
}

.state-card,
.history-state {
  color: var(--theme-muted);
  font-size: 24rpx;
  text-align: center;
}

.state-card.error {
  color: #b65473;
}

.empty-title,
.state-title {
  display: block;
  color: var(--theme-ink);
  font-size: 30rpx;
  font-weight: 800;
}

.empty-copy,
.state-subtitle {
  display: block;
  margin-top: 12rpx;
  color: var(--theme-muted);
  font-size: 24rpx;
  line-height: 1.7;
}

.login-btn {
  width: 180rpx;
  height: 72rpx;
  margin-top: 24rpx;
}

.composer-shell {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 24rpx;
  z-index: 20;
}

.composer {
  gap: 18rpx;
  padding: 18rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.94);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: 0 16rpx 38rpx rgba(120, 107, 174, 0.12);
}

.composer-input {
  flex: 1;
  max-height: 240rpx;
  padding: 14rpx 8rpx;
  font-size: 27rpx;
  color: var(--theme-ink);
}

.send-btn {
  width: 120rpx;
  min-height: 96rpx;
}

.send-btn.disabled {
  opacity: 0.5;
}

.history-mask {
  position: fixed;
  inset: 0;
  z-index: 40;
  background: rgba(35, 30, 52, 0.16);
}

.history-panel {
  position: absolute;
  top: 0;
  right: 0;
  width: 620rpx;
  height: 100vh;
  padding: 28rpx 24rpx;
  border-radius: 30rpx 0 0 30rpx;
}

.history-title {
  font-size: 30rpx;
  color: var(--theme-ink);
  font-weight: 800;
}

.history-body {
  margin-top: 22rpx;
  height: calc(100vh - 120rpx);
  overflow: auto;
}

.history-item {
  gap: 18rpx;
  padding: 22rpx 18rpx;
  border-radius: 22rpx;
  background: rgba(247, 244, 252, 0.78);
  margin-bottom: 14rpx;
}

.history-item.active {
  border: 1rpx solid rgba(140, 128, 216, 0.24);
  background: rgba(239, 233, 249, 0.92);
}

.history-copy {
  flex: 1;
  flex-direction: column;
}

.history-item-title {
  color: var(--theme-ink);
  font-size: 27rpx;
  font-weight: 700;
}

.history-item-preview,
.history-item-time {
  display: block;
  margin-top: 8rpx;
  color: var(--theme-muted);
  font-size: 22rpx;
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-4rpx);
  }
}
</style>
