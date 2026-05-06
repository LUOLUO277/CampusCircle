<template>
  <view class="page">
    <view class="hero-card">
      <text class="hero-title">连接个人 Canvas</text>
      <text class="hero-desc">课程项目模式：填写你自己的 Canvas 账号和密码，后端临时登录并抓取页面，不需要学校开放接口，也不要求你手动找 token。</text>
    </view>

    <view class="section">
      <text class="section-title">绑定信息</text>
      <input
        v-model="form.baseUrl"
        class="input"
        type="text"
        placeholder="Canvas 地址"
        placeholder-class="input-placeholder"
      />
      <input
        v-model="form.username"
        class="input"
        type="text"
        placeholder="学号 / 统一身份认证用户名"
        placeholder-class="input-placeholder"
      />
      <input
        v-model="form.password"
        class="input"
        type="password"
        password
        placeholder="密码"
        placeholder-class="input-placeholder"
      />
      <input
        v-model="form.courseIdsText"
        class="input"
        type="text"
        placeholder="课程 ID，可选，逗号分隔；留空则自动发现课程"
        placeholder-class="input-placeholder"
      />

      <label class="switch-row">
        <text>同步待办</text>
        <switch :checked="form.includeTodo" color="#1f5f46" @change="onTodoChange" />
      </label>

      <label class="switch-row">
        <text>同步公告页</text>
        <switch :checked="form.includeGlobalAnnouncements" color="#1f5f46" @change="onAnnouncementChange" />
      </label>

      <view class="action-row">
        <button class="primary-btn" @click="submit">保存绑定</button>
        <button class="ghost-btn" @click="syncNow" :disabled="!connected">手动同步</button>
      </view>
    </view>

    <view class="section" v-if="statusVisible">
      <text class="section-title">当前状态</text>
      <text class="status-line">连接状态：{{ connected ? '已连接' : '未连接' }}</text>
      <text class="status-line" v-if="binding.username">账号：{{ binding.username }}</text>
      <text class="status-line" v-if="binding.hasSession">会话：已缓存</text>
      <text class="status-line" v-if="binding.lastSyncStatus">最近同步：{{ binding.lastSyncStatus }}</text>
      <text class="status-line" v-if="binding.lastSyncedAt">同步时间：{{ binding.lastSyncedAt }}</text>
      <text class="status-line" v-if="binding.lastSyncMessage">{{ binding.lastSyncMessage }}</text>
      <button v-if="connected" class="danger-btn" @click="disconnect">断开连接</button>
    </view>

    <view class="section tips">
      <text class="section-title">说明</text>
      <text class="tip-line">1. 这是课程项目演示方案，不是学校正式接口对接。</text>
      <text class="tip-line">2. 当前做法是手动同步优先，后端登录后抓取 Canvas 页面。</text>
      <text class="tip-line">3. 如果学校登录页带验证码或额外验证，这套方式可能失效。</text>
    </view>
  </view>
</template>

<script>
import {
  getCanvasBinding,
  saveCanvasBinding,
  syncCanvasBinding,
  disconnectCanvasBinding
} from '@/api/info-center'

export default {
  data() {
    return {
      binding: {},
      connected: false,
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
  computed: {
    statusVisible() {
      return this.connected || this.binding.lastSyncStatus || this.binding.lastSyncMessage
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
    async syncNow() {
      if (!this.connected) {
        uni.showToast({ title: '请先保存绑定', icon: 'none' })
        return
      }
      uni.showLoading({ title: '同步中...' })
      try {
        const res = await syncCanvasBinding()
        if (res.code === 200) {
          uni.showToast({ title: `同步完成 ${res.data.successCount || 0} 条`, icon: 'none' })
        }
        await this.loadBinding()
      } finally {
        uni.hideLoading()
      }
    },
    async disconnect() {
      await disconnectCanvasBinding()
      uni.showToast({ title: '已断开 Canvas', icon: 'none' })
      await this.loadBinding()
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f4f7f5;
  padding: 30rpx;
}
.hero-card,
.section {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}
.hero-card {
  background: linear-gradient(135deg, #e8f4ec 0%, #fff9ef 100%);
}
.hero-title,
.section-title {
  display: block;
  font-weight: 700;
  color: #0f172a;
}
.hero-title {
  font-size: 34rpx;
}
.hero-desc,
.status-line,
.tip-line {
  display: block;
  margin-top: 12rpx;
  color: #475569;
  font-size: 25rpx;
  line-height: 1.6;
}
.input {
  width: 100%;
  box-sizing: border-box;
  background: #f8fafc;
  color: #0f172a;
  border: 2rpx solid #dbe4ee;
  border-radius: 20rpx;
  padding: 22rpx;
  margin-top: 18rpx;
  min-height: 88rpx;
}
.input-placeholder {
  color: #94a3b8;
}
.switch-row,
.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 24rpx;
}
.action-row {
  justify-content: flex-start;
}
.primary-btn,
.ghost-btn,
.danger-btn {
  margin: 0;
  border-radius: 999rpx;
  font-size: 24rpx;
}
.primary-btn {
  min-width: 180rpx;
  background: #1f5f46;
  color: #fff;
}
.ghost-btn {
  min-width: 180rpx;
  background: #e2e8f0;
  color: #334155;
}
.danger-btn {
  margin-top: 20rpx;
  background: #fee2e2;
  color: #b91c1c;
}
.primary-btn::after,
.ghost-btn::after,
.danger-btn::after {
  border: none;
}
.tips {
  background: #fffdf7;
}
</style>
