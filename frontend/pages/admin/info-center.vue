<template>
  <view class="page">
    <view class="section">
      <text class="section-title">来源录入 / 更新</text>
      <input v-model="sourceForm.name" class="input" placeholder="来源名称，例如：同济大学公众号镜像" />
      <input v-model="sourceForm.type" class="input" placeholder="类型：WECHAT / ACADEMIC / CANVAS / MANUAL" />
      <input v-model="sourceForm.sourceKey" class="input" placeholder="唯一 sourceKey，例如 tongji-wechat-rsshub" />
      <input v-model="sourceForm.sourceUrl" class="input" placeholder="来源 URL 或列表页 URL" />
      <input
        v-model="sourceForm.fetchStrategy"
        class="input"
        placeholder="抓取策略：WECHAT_RSS / WECHAT_HTML / ACADEMIC_HTML / CANVAS_API / CANVAS_HTML"
      />
      <input v-model="sourceForm.searchKeywords" class="input" placeholder="搜索关键词，空格或逗号分隔" />
      <textarea
        v-model="sourceForm.fetchConfigJson"
        class="textarea"
        placeholder='fetchConfigJson，例如 {"rssUrl":"https://rsshub.app/wechat/mp/xxx","maxItems":20}'
      />
      <view class="source-form-actions">
        <button class="mini-btn" @click="submitSource">保存来源</button>
        <button class="mini-btn alt" @click="fillExample('wechat')">公众号示例</button>
        <button class="mini-btn alt" @click="fillExample('academic')">教务示例</button>
        <button class="mini-btn alt" @click="fillExample('canvas')">Canvas 示例</button>
      </view>
    </view>

    <view class="section">
      <text class="section-title">来源管理与抓取</text>
      <view v-for="source in sources" :key="source.id" class="source-row">
        <view class="source-info">
          <text class="source-name">{{ source.name }}</text>
          <text class="source-meta">{{ source.type }} / {{ source.status }} / {{ source.lastFetchStatus || '未抓取' }}</text>
        </view>
        <view class="source-actions">
          <button class="mini-btn alt" @click="editSource(source)">编辑</button>
          <button class="mini-btn" @click="fetchSource(source.id)">抓取</button>
          <button class="mini-btn alt" @click="toggleSource(source)">{{ source.status === 'ACTIVE' ? '停用' : '启用' }}</button>
        </view>
      </view>
    </view>

    <view class="section">
      <text class="section-title">人工补录通知</text>
      <picker :range="sources" range-key="name" @change="onSourceSelect">
        <view class="input">{{ manual.sourceName || '选择来源' }}</view>
      </picker>
      <input v-model="manual.title" class="input" placeholder="通知标题" />
      <input v-model="manual.category" class="input" placeholder="分类，例如：通知 / 截止提醒 / 活动" />
      <input v-model="manual.originalUrl" class="input" placeholder="原文链接" />
      <textarea
        v-model="manual.contentSnapshot"
        class="textarea"
        placeholder="正文快照，可写入截止时间、地点、对象、报名链接等关键信息"
      />
      <input v-model="manual.targetAudience" class="input" placeholder="适用对象" />
      <input v-model="manual.location" class="input" placeholder="地点" />
      <input v-model="manual.tagsText" class="input" placeholder="标签，逗号分隔" />
      <button class="submit-btn" @click="submitManualNotice">提交补录</button>
    </view>

    <view class="section" v-if="logs.length">
      <text class="section-title">最近抓取日志</text>
      <view v-for="log in logs" :key="log.id" class="log-item">
        <text class="log-line">{{ log.createdAt }} / {{ log.status }}</text>
        <text class="log-line">成功 {{ log.successCount }} 条，失败 {{ log.failureCount }} 条</text>
      </view>
    </view>
  </view>
</template>

<script>
import {
  getInfoSources,
  triggerSourceFetch,
  getSourceFetchLogs,
  disableInfoSource,
  enableInfoSource,
  createManualNotice,
  createInfoSource,
  getAdminInfoSourceDetail,
  updateInfoSource
} from '@/api/info-center'

export default {
  data() {
    return {
      sources: [],
      logs: [],
      sourceForm: {
        id: null,
        name: '',
        type: 'WECHAT',
        sourceKey: '',
        sourceUrl: '',
        searchKeywords: '',
        fetchStrategy: 'WECHAT_RSS',
        fetchConfigJson: ''
      },
      manual: {
        sourceId: '',
        sourceName: '',
        title: '',
        category: '通知',
        originalUrl: '',
        contentSnapshot: '',
        targetAudience: '',
        location: '',
        tagsText: ''
      }
    }
  },
  onShow() {
    this.loadSources()
  },
  methods: {
    async loadSources() {
      const res = await getInfoSources()
      if (res.code === 200) {
        this.sources = res.data || []
      }
    },
    async submitSource() {
      if (!this.sourceForm.name || !this.sourceForm.type || !this.sourceForm.sourceKey) {
        uni.showToast({ title: '请先填写名称、类型和 sourceKey', icon: 'none' })
        return
      }
      if (this.sourceForm.fetchConfigJson) {
        try {
          JSON.parse(this.sourceForm.fetchConfigJson)
        } catch (error) {
          uni.showToast({ title: 'fetchConfigJson 不是合法 JSON', icon: 'none' })
          return
        }
      }
      const payload = {
        name: this.sourceForm.name,
        type: this.sourceForm.type,
        sourceKey: this.sourceForm.sourceKey,
        sourceUrl: this.sourceForm.sourceUrl,
        searchKeywords: this.sourceForm.searchKeywords,
        fetchStrategy: this.sourceForm.fetchStrategy,
        fetchConfigJson: this.sourceForm.fetchConfigJson,
        status: 'ACTIVE'
      }
      if (this.sourceForm.id) {
        await updateInfoSource(this.sourceForm.id, payload)
      } else {
        await createInfoSource(payload)
      }
      uni.showToast({ title: '来源已保存', icon: 'success' })
      this.resetSourceForm()
      await this.loadSources()
    },
    async editSource(source) {
      const res = await getAdminInfoSourceDetail(source.id)
      const detail = res.code === 200 ? res.data || {} : {}
      this.sourceForm = {
        id: detail.id || source.id,
        name: detail.name || source.name || '',
        type: detail.type || source.type || 'WECHAT',
        sourceKey: detail.sourceKey || source.sourceKey || '',
        sourceUrl: detail.sourceUrl || source.sourceUrl || '',
        searchKeywords: detail.searchKeywords || source.searchKeywords || '',
        fetchStrategy: detail.fetchStrategy || source.fetchStrategy || 'WECHAT_RSS',
        fetchConfigJson: detail.fetchConfigJson || ''
      }
    },
    resetSourceForm() {
      this.sourceForm = {
        id: null,
        name: '',
        type: 'WECHAT',
        sourceKey: '',
        sourceUrl: '',
        searchKeywords: '',
        fetchStrategy: 'WECHAT_RSS',
        fetchConfigJson: ''
      }
    },
    fillExample(kind) {
      if (kind === 'wechat') {
        this.sourceForm.type = 'WECHAT'
        this.sourceForm.fetchStrategy = 'WECHAT_RSS'
        this.sourceForm.fetchConfigJson = JSON.stringify({
          rssUrl: 'https://rsshub.app/wechat/mp/<biz>',
          maxItems: 20,
          category: '活动',
          tags: ['公众号', '校园']
        }, null, 2)
        return
      }
      if (kind === 'academic') {
        this.sourceForm.type = 'ACADEMIC'
        this.sourceForm.fetchStrategy = 'ACADEMIC_HTML'
        this.sourceForm.fetchConfigJson = JSON.stringify({
          listUrl: 'https://example.edu.cn/notice/list',
          baseUrl: 'https://example.edu.cn',
          maxItems: 20,
          category: '教务',
          keyword: '通知 公告',
          tags: ['教务']
        }, null, 2)
        return
      }
      this.sourceForm.type = 'CANVAS'
      this.sourceForm.fetchStrategy = 'CANVAS_API'
      this.sourceForm.fetchConfigJson = JSON.stringify({
        baseUrl: 'https://canvas.example.edu.cn',
        token: 'canvas-access-token',
        announcementCourseIds: [101, 202],
        includeGlobalAnnouncements: true,
        includeTodo: true,
        maxItems: 30,
        category: '课程'
      }, null, 2)
    },
    onSourceSelect(event) {
      const source = this.sources[event.detail.value]
      this.manual.sourceId = source.id
      this.manual.sourceName = source.name
    },
    async fetchSource(id) {
      await triggerSourceFetch(id)
      const logs = await getSourceFetchLogs(id)
      if (logs.code === 200) {
        this.logs = logs.data.list || []
      }
      await this.loadSources()
      uni.showToast({ title: '抓取已执行', icon: 'none' })
    },
    async toggleSource(source) {
      if (source.status === 'ACTIVE') {
        await disableInfoSource(source.id)
      } else {
        await enableInfoSource(source.id)
      }
      await this.loadSources()
    },
    async submitManualNotice() {
      if (!this.manual.sourceId || !this.manual.title || !this.manual.contentSnapshot) {
        uni.showToast({ title: '请先补全来源、标题和正文', icon: 'none' })
        return
      }
      await createManualNotice({
        sourceId: this.manual.sourceId,
        title: this.manual.title,
        category: this.manual.category,
        originalUrl: this.manual.originalUrl,
        contentSnapshot: this.manual.contentSnapshot,
        targetAudience: this.manual.targetAudience,
        location: this.manual.location,
        tags: this.manual.tagsText.split(',').map(item => item.trim()).filter(Boolean)
      })
      uni.showToast({ title: '补录成功', icon: 'success' })
      this.manual.title = ''
      this.manual.contentSnapshot = ''
      this.manual.targetAudience = ''
      this.manual.location = ''
      this.manual.tagsText = ''
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f4f6f5;
  padding: 30rpx;
}
.section {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}
.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 20rpx;
}
.source-row,
.source-actions,
.source-form-actions {
  display: flex;
  gap: 16rpx;
}
.source-form-actions {
  flex-wrap: wrap;
}
.source-row {
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #eef2f7;
}
.source-info {
  flex: 1;
}
.source-name {
  display: block;
  font-size: 28rpx;
  color: #111827;
  font-weight: 600;
}
.source-meta,
.log-line {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #64748b;
}
.mini-btn,
.submit-btn {
  margin: 0;
  border-radius: 999rpx;
  font-size: 24rpx;
}
.mini-btn {
  min-width: 120rpx;
  height: 64rpx;
  line-height: 64rpx;
  background: #1f5f46;
  color: #fff;
}
.mini-btn.alt {
  background: #e2e8f0;
  color: #334155;
}
.mini-btn::after,
.submit-btn::after {
  border: none;
}
.input,
.textarea {
  width: 100%;
  box-sizing: border-box;
  background: #f8fafc;
  border-radius: 20rpx;
  padding: 22rpx;
  margin-bottom: 18rpx;
}
.textarea {
  min-height: 200rpx;
}
.submit-btn {
  background: #1f5f46;
  color: #fff;
}
.log-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid #eef2f7;
}
</style>
