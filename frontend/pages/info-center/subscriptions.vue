<template>
  <view class="page">
    <view class="header-card">
      <text class="header-title">订阅管理</text>
      <text class="header-desc">搜索已登记来源并订阅，第一阶段先使用平台来源库搜索。</text>
    </view>

    <input v-model="keyword" class="search-box" placeholder="搜索公众号、教务、Canvas 来源" @input="handleSearch" />

    <view v-for="source in sources" :key="source.id" class="source-card">
      <view class="source-main">
        <view>
          <text class="source-name">{{ source.name }}</text>
          <text class="source-meta">{{ source.type }} · {{ source.fetchStrategy || '未配置抓取策略' }}</text>
          <text v-if="source.sourceUrl" class="source-url">{{ source.sourceUrl }}</text>
        </view>
        <button class="action-btn" :class="{ active: source.subscribed }" @click="toggleSubscribe(source)">
          {{ source.subscribed ? '取消订阅' : '订阅' }}
        </button>
      </view>
      <input
        v-if="source.subscribed"
        :value="keywordMap[source.id] || ''"
        class="keyword-input"
        placeholder="设置关键词提醒，逗号分隔"
        @blur="saveKeywords(source.id, $event)"
      />
    </view>

    <view class="apply-card">
      <text class="apply-title">新增公众号来源申请</text>
      <text class="apply-desc">第一阶段暂不做全网实时搜公众号。需要新来源时，先由用户提交申请，后台审核录入。</text>
    </view>
  </view>
</template>

<script>
import {
  getInfoSources,
  getInfoSubscriptions,
  searchInfoSources,
  subscribeSource,
  unsubscribeSource,
  updateSubscriptionKeywords
} from '@/api/info-center'

export default {
  data() {
    return {
      keyword: '',
      sources: [],
      keywordMap: {}
    }
  },
  onShow() {
    this.bootstrap()
  },
  methods: {
    async bootstrap() {
      await Promise.all([this.loadSources(), this.loadSubscriptions()])
    },
    async loadSources() {
      const res = this.keyword ? await searchInfoSources(this.keyword) : await getInfoSources()
      if (res.code === 200) {
        this.sources = res.data || []
      }
    },
    async loadSubscriptions() {
      const res = await getInfoSubscriptions()
      if (res.code === 200) {
        const map = {}
        ;(res.data.list || []).forEach(item => {
          map[item.id] = (item.keywords || []).join(', ')
        })
        this.keywordMap = map
      }
    },
    async handleSearch() {
      await this.loadSources()
    },
    async toggleSubscribe(source) {
      if (source.subscribed) {
        await unsubscribeSource(source.id)
        source.subscribed = false
      } else {
        await subscribeSource(source.id)
        source.subscribed = true
      }
    },
    async saveKeywords(sourceId, event) {
      const raw = event.detail.value || ''
      const keywords = raw.split(',').map(item => item.trim()).filter(Boolean)
      await updateSubscriptionKeywords(sourceId, keywords)
      this.keywordMap[sourceId] = raw
      uni.showToast({ title: '关键词已保存', icon: 'none' })
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
.header-card,
.source-card,
.apply-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}
.header-card {
  background: linear-gradient(135deg, #f8f2df 0%, #fff 100%);
}
.header-title,
.apply-title,
.source-name {
  display: block;
  font-weight: 700;
  color: #111827;
}
.header-title {
  font-size: 34rpx;
}
.header-desc,
.apply-desc,
.source-meta,
.source-url {
  display: block;
  color: #64748b;
  font-size: 24rpx;
  line-height: 1.6;
  margin-top: 10rpx;
}
.search-box,
.keyword-input {
  background: #fff;
  border-radius: 22rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}
.source-main {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
}
.action-btn {
  margin: 0;
  width: 160rpx;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 999rpx;
  background: #e2e8f0;
  color: #334155;
  font-size: 24rpx;
}
.action-btn.active {
  background: #1f5f46;
  color: #fff;
}
.action-btn::after {
  border: none;
}
.keyword-input {
  margin-top: 20rpx;
  margin-bottom: 0;
  background: #f8fafc;
}
</style>
