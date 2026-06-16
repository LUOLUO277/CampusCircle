<template>
  <view class="page">
    <view class="hero-card">
      <text class="hero-title">连接个人 Canvas</text>
      <text class="hero-desc">先复用 cookies 同步，必要时再 browser-login。</text>
    </view>

    <view class="section">
      <text class="section-title">绑定信息</text>
      <input v-model="form.baseUrl" class="input" type="text" placeholder="Canvas 地址" placeholder-class="input-placeholder" />
      <input v-model="form.username" class="input" type="text" placeholder="学号 / 统一身份认证用户名" placeholder-class="input-placeholder" />
      <input v-model="form.password" class="input" type="password" password placeholder="密码" placeholder-class="input-placeholder" />
      <input v-model="form.courseIdsText" class="input" type="text" placeholder="课程 ID（可选，逗号分隔）" placeholder-class="input-placeholder" />

      <label class="switch-row">
        <text>同步待办</text>
        <switch :checked="form.includeTodo" color="#1f5f46" @change="onTodoChange" />
      </label>

      <label class="switch-row">
        <text>同步公告页</text>
        <switch :checked="form.includeGlobalAnnouncements" color="#1f5f46" @change="onAnnouncementChange" />
      </label>

      <view class="action-row">
        <button class="primary-btn" @click="submit" :disabled="syncInProgress">保存绑定</button>
        <button class="ghost-btn" @click="syncBySource('canvas')" :disabled="!connected || syncInProgress">同步 Canvas</button>
        <button class="ghost-btn" @click="syncBySource('tongji')" :disabled="!connected || syncInProgress">同步 Tongji 公告</button>
        <button class="ghost-btn" @click="syncBySource('all')" :disabled="!connected || syncInProgress">同步全部</button>
      </view>
      <text v-if="syncInProgress" class="status-line">同步阶段：{{ syncStage }}</text>
    </view>

    <view class="section" v-if="statusVisible">
      <text class="section-title">当前状态</text>
      <text class="status-line">连接状态：{{ connected ? '已连接' : '未连接' }}</text>
      <text class="status-line" v-if="binding.username">账号：{{ binding.username }}</text>
      <text class="status-line" v-if="binding.hasSession">会话：已缓存</text>
      <text class="status-line" v-if="binding.lastSyncStatus">最近同步：{{ binding.lastSyncStatus }}</text>
      <text class="status-line" v-if="binding.lastSyncedAt">同步时间：{{ binding.lastSyncedAt }}</text>
      <text class="status-line" v-if="binding.lastSyncMessage">{{ binding.lastSyncMessage }}</text>

      <view v-if="syncResult" class="sync-result-block">
        <text class="status-line">overallStatus: {{ syncResult.overallStatus }}</text>
        <text class="status-line">canvas: {{ formatSourceStatus(syncResult.canvas) }}</text>
        <text class="status-line">tongji: {{ formatSourceStatus(syncResult.tongjiAnnouncement) }}</text>
        <text v-if="debugPreview" class="status-line">tongji debugItems(前10条): {{ debugPreview }}</text>
      </view>
      <button v-if="connected" class="danger-btn" @click="disconnect" :disabled="syncInProgress">断开连接</button>
    </view>
  </view>
</template>

<script>
import {
  getCanvasBinding,
  saveCanvasBinding,
  syncCanvasBinding,
  browserLoginCanvasBinding,
  disconnectCanvasBinding
} from '@/api/info-center'

export default {
  data() {
    return {
      binding: {},
      connected: false,
      syncResult: null,
      syncInProgress: false,
      syncStage: '',
      form: {
        baseUrl: 'https://canvas.tongji.edu.cn',
        username: '',
        password: '',
        courseIdsText: '',
        includeTodo: true,
        includeGlobalAnnouncements: true
      }
    }
  },
  onShow() {
    this.loadBinding()
  },
  methods: {
    async loadBinding() {
      const res = await getCanvasBinding()
      if (res.code !== 200) return
      this.binding = res.data || {}
      this.connected = !!this.binding.connected
      this.form.baseUrl = this.binding.baseUrl || 'https://canvas.tongji.edu.cn'
      this.form.username = this.binding.username || ''
      this.form.password = ''
      this.form.courseIdsText = (this.binding.courseIds || []).join(', ')
      this.form.includeTodo = this.binding.includeTodo !== false
      this.form.includeGlobalAnnouncements = this.binding.includeGlobalAnnouncements !== false
    },
    onTodoChange(event) {
      this.form.includeTodo = !!event.detail.value
    },
    onAnnouncementChange(event) {
      this.form.includeGlobalAnnouncements = !!event.detail.value
    },
    buildPayload() {
      const courseIds = (this.form.courseIdsText || '')
        .split(',')
        .map(item => Number(item.trim()))
        .filter(item => Number.isFinite(item) && item > 0)
      return {
        baseUrl: this.form.baseUrl.trim(),
        username: this.form.username.trim(),
        password: this.form.password,
        courseIds,
        includeTodo: this.form.includeTodo,
        includeGlobalAnnouncements: this.form.includeGlobalAnnouncements
      }
    },
    async submit() {
      if (!this.form.baseUrl || !this.form.username) {
        uni.showToast({ title: '请填写 Canvas 地址和账号', icon: 'none' })
        return
      }
      if (!this.connected && !this.form.password) {
        uni.showToast({ title: '首次绑定必须填写密码', icon: 'none' })
        return
      }
      await saveCanvasBinding(this.buildPayload())
      uni.showToast({ title: 'Canvas 绑定已保存', icon: 'success' })
      await this.loadBinding()
    },
    async syncBySource(source = 'all') {
      if (this.syncInProgress) {
        uni.showToast({ title: '同步进行中，请稍候', icon: 'none' })
        return
      }
      if (!this.connected) {
        uni.showToast({ title: '请先保存绑定', icon: 'none' })
        return
      }

      this.syncInProgress = true
      const debugRaw = false
      const timer = setInterval(() => {
        if (this.syncStage === '请求中') this.syncStage = '登录状态检查中'
        else if (this.syncStage === '登录状态检查中') this.syncStage = '抓取页面中'
        else if (this.syncStage === '抓取页面中') this.syncStage = '解析与入库中'
      }, 5000)

      this.syncStage = '请求中'
      uni.showLoading({ title: '同步中...' })
      try {
        let res = await syncCanvasBinding({ source, forceRelogin: false, debugRaw })
        this.syncResult = res.data || null
        const needBrowserLogin = this.hasNeedBrowserLogin(this.syncResult, source)
        const shellPage = this.hasTongjiShellPage(this.syncResult, source)

        if (needBrowserLogin || shellPage) {
          uni.hideLoading()
          const modalRes = await uni.showModal({
            title: '需要 browser-login',
            content: needBrowserLogin
              ? '检测到需要登录或验证码。是否先 browser-login，再自动重试一次同步？'
              : 'Tongji 返回壳页。是否先 browser-login，再自动重试一次同步？',
            confirmText: '去登录',
            cancelText: '取消'
          })
          if (modalRes.confirm) {
            this.syncStage = '浏览器登录中'
            uni.showLoading({ title: '浏览器登录中...' })
            await browserLoginCanvasBinding()
            await this.loadBinding()
            this.syncStage = '重试同步中'
            uni.showLoading({ title: '重试同步中...' })
            res = await syncCanvasBinding({ source, forceRelogin: false, debugRaw })
            this.syncResult = res.data || null
          }
        }

        if (debugRaw) {
          const count = ((this.syncResult || {}).tongjiAnnouncement || {}).fetched || 0
          uni.showToast({ title: `调试抓取完成，候选 ${count} 条`, icon: 'none' })
          await this.loadBinding()
          return
        }

        uni.showToast({ title: this.buildSyncToast(this.syncResult), icon: 'none' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/info-center/index' })
        }, 500)
        await this.loadBinding()
      } catch (err) {
        const msg = (err && err.message) || ''
        if (/timeout|timed out|超时/i.test(msg)) {
          uni.showToast({ title: '同步请求超时，后台可能仍在处理，请稍后刷新查看', icon: 'none' })
        } else {
          uni.showToast({ title: msg || '同步失败，请稍后重试', icon: 'none' })
        }
      } finally {
        clearInterval(timer)
        this.syncInProgress = false
        this.syncStage = ''
        uni.hideLoading()
      }
    },
    async disconnect() {
      await disconnectCanvasBinding()
      uni.showToast({ title: '已断开 Canvas', icon: 'none' })
      await this.loadBinding()
    },
    resolveSyncCount(data = {}) {
      if (typeof data.totalFetched === 'number') return data.totalFetched
      if (typeof data.successCount === 'number') return data.successCount
      const canvasCount = Number(data.canvas && data.canvas.successCount) || 0
      const tongjiCount = Number(data.tongjiAnnouncement && data.tongjiAnnouncement.successCount) || 0
      return canvasCount + tongjiCount
    },
    hasNeedBrowserLogin(result = {}, source = 'all') {
      const checkCanvas = source === 'all' || source === 'canvas'
      const checkTongji = source === 'all' || source === 'tongji'
      return (checkCanvas && result.canvas && result.canvas.status === 'NEED_BROWSER_LOGIN') ||
        (checkTongji && result.tongjiAnnouncement && result.tongjiAnnouncement.status === 'NEED_BROWSER_LOGIN')
    },
    hasTongjiShellPage(result = {}, source = 'all') {
      if (!(source === 'all' || source === 'tongji')) return false
      return result.tongjiAnnouncement && result.tongjiAnnouncement.status === 'TONGJI_SHELL_PAGE'
    },
    buildSyncToast(result = {}) {
      const overall = result.overallStatus || 'UNKNOWN'
      const count = this.resolveSyncCount(result)
      const tongjiStatus = result.tongjiAnnouncement && result.tongjiAnnouncement.status
      const canvasStatus = result.canvas && result.canvas.status
      if (tongjiStatus === 'TONGJI_SHELL_PAGE') {
        return `Tongji 壳页未解析（${overall}），入库 ${count} 条`
      }
      if (tongjiStatus === 'NEED_BROWSER_LOGIN' || canvasStatus === 'NEED_BROWSER_LOGIN') {
        return `需要 browser-login（${overall}），入库 ${count} 条`
      }
      return `${overall}，成功入库 ${count} 条`
    },
    formatSourceStatus(sourceResult = {}) {
      const status = sourceResult.status || 'UNKNOWN'
      const fetched = Number(sourceResult.fetched || 0)
      const success = Number(sourceResult.successCount || 0)
      const diag = (sourceResult.diagnostics || [])[0] || ''
      return `${status} | fetched=${fetched} | success=${success}${diag ? ` | ${diag}` : ''}`
    },
    formatDebugItems(result = {}) {
      const items = (((result || {}).tongjiAnnouncement || {}).debugItems || []).slice(0, 10)
      if (!items.length) return ''
      return items
        .map((item, idx) => `${idx + 1}. ${item.title || '<empty>'} | ${item.publishTime || ''} | ${item.originalUrl || ''}`)
        .join(' || ')
    }
  },
  computed: {
    statusVisible() {
      return this.connected || this.binding.lastSyncStatus || this.binding.lastSyncMessage || this.syncResult
    },
    debugPreview() {
      return this.formatDebugItems(this.syncResult)
    }
  }
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f4f7f5; padding: 30rpx; }
.hero-card, .section { background: #fff; border-radius: 28rpx; padding: 28rpx; margin-bottom: 20rpx; }
.hero-card { background: linear-gradient(135deg, #e8f4ec 0%, #fff9ef 100%); }
.hero-title, .section-title { display: block; font-weight: 700; color: #0f172a; }
.hero-title { font-size: 34rpx; }
.hero-desc, .status-line { display: block; margin-top: 12rpx; color: #475569; font-size: 25rpx; line-height: 1.6; }
.input { width: 100%; box-sizing: border-box; background: #f8fafc; color: #0f172a; border: 2rpx solid #dbe4ee; border-radius: 20rpx; padding: 22rpx; margin-top: 18rpx; min-height: 88rpx; }
.input-placeholder { color: #94a3b8; }
.switch-row, .action-row { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; margin-top: 24rpx; }
.action-row { justify-content: flex-start; flex-wrap: wrap; }
.primary-btn, .ghost-btn, .danger-btn { margin: 0; border-radius: 999rpx; font-size: 24rpx; }
.primary-btn { min-width: 180rpx; background: #1f5f46; color: #fff; }
.ghost-btn { min-width: 180rpx; background: #e2e8f0; color: #334155; }
.danger-btn { margin-top: 20rpx; background: #fee2e2; color: #b91c1c; }
.primary-btn::after, .ghost-btn::after, .danger-btn::after { border: none; }
.sync-result-block { margin-top: 18rpx; padding: 16rpx; border-radius: 16rpx; background: #f8fafc; }
</style>
