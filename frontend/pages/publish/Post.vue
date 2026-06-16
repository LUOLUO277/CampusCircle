<template>
  <view class="publish-page">
    <view class="page-glow page-glow-left"></view>
    <view class="page-glow page-glow-right"></view>

    <view class="nav-header">
      <view class="nav-btn" @click="goBack">‹</view>
      <view class="nav-copy">
        <text class="nav-kicker">Post Studio</text>
        <text class="nav-title">发一条校园内容</text>
      </view>
      <view class="nav-btn ghost" @click="handlePublish" :class="{ disabled: !canPublish }">发布</view>
    </view>

    <view class="intro-card">
      <text class="intro-title">今天想分享什么？</text>
      <text class="intro-desc">先选主题，再把正文、图片和附加信息整理成一条完整内容。</text>
    </view>

    <view class="section-card">
      <view class="section-head" @click="toggleTopicPanel">
        <view>
          <text class="section-title">选择分区</text>
          <text class="section-tip">选一个更适合内容被发现的分类</text>
        </view>
        <text class="section-arrow" :class="{ expanded: showTopicPanel }">⌄</text>
      </view>

      <view v-show="showTopicPanel" class="topic-list">
        <view
          v-for="topic in topicList"
          :key="topic.id"
          class="topic-tag"
          :class="{ active: selectedTopic === topic.id }"
          @click="selectTopic(topic)"
        >
          <text>{{ topic.name }}</text>
        </view>
      </view>

      <view v-if="selectedTopic && !showTopicPanel" class="selected-topic">
        <view class="topic-tag active">
          <text>{{ selectedTopicData.name }}</text>
          <text class="remove-btn" @click.stop="clearTopic">×</text>
        </view>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head static">
        <view>
          <text class="section-title">正文内容</text>
          <text class="section-tip">默认标题栏已去掉，用这里作为唯一主标题区</text>
        </view>
      </view>

      <textarea
        class="content-input"
        v-model="content"
        placeholder="分享你的日常、经验、求助或见闻..."
        :maxlength="2000"
        auto-height
      ></textarea>
      <view class="word-count">{{ content.length }}/2000</view>
    </view>

    <view class="section-card">
      <view class="section-head static">
        <view>
          <text class="section-title">图片素材</text>
          <text class="section-tip">最多 9 张，首图会优先作为内容封面感知</text>
        </view>
      </view>

      <view class="image-list">
        <view
          v-for="(img, index) in imageList"
          :key="index"
          class="image-item"
        >
          <image :src="img" mode="aspectFill" @click="previewImage(index)"></image>
          <view class="delete-btn" @click="deleteImage(index)">×</view>
        </view>

        <view
          v-if="imageList.length < 9"
          class="add-image-btn"
          @click="chooseImage"
        >
          <text class="add-icon">+</text>
        </view>
      </view>
    </view>

    <view v-if="selectedTopic === 1" class="section-card">
      <view class="section-head static">
        <view>
          <text class="section-title">商品信息</text>
          <text class="section-tip">二手交易内容建议补全价格和交易方式</text>
        </view>
      </view>

      <view class="form-item">
        <text class="label">价格</text>
        <view class="input-shell">
          <text class="price-symbol">¥</text>
          <input
            class="price-input"
            type="digit"
            v-model="price"
            placeholder="输入价格"
          />
        </view>
      </view>

      <view class="form-item vertical">
        <text class="label">交易方式</text>
        <view class="trade-options">
          <view
            class="trade-option"
            :class="{ active: tradeMethod === 'face' }"
            @click="tradeMethod = 'face'"
          >
            <text>当面交易</text>
          </view>
          <view
            class="trade-option"
            :class="{ active: tradeMethod === 'delivery' }"
            @click="tradeMethod = 'delivery'"
          >
            <text>邮寄</text>
          </view>
          <view
            class="trade-option"
            :class="{ active: tradeMethod === 'both' }"
            @click="tradeMethod = 'both'"
          >
            <text>都可以</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="selectedTopic === 4" class="section-card">
      <view class="section-head static">
        <view>
          <text class="section-title">投票选项</text>
          <text class="section-tip">至少保留两个有效选项，最多 6 项</text>
        </view>
      </view>

      <view v-for="(option, index) in voteOptions" :key="index" class="vote-option-item">
        <input
          class="vote-input"
          v-model="voteOptions[index]"
          placeholder="请输入投票选项"
        />
        <text
          class="delete-option"
          @click="removeVoteOption(index)"
          v-if="voteOptions.length > 2"
        >×</text>
      </view>
      <view class="add-option-btn" @click="addVoteOption" v-if="voteOptions.length < 6">
        <text>+ 添加选项</text>
      </view>
    </view>

    <view class="section-card compact">
      <view class="setting-item">
        <view>
          <text class="section-title small">匿名发布</text>
          <text class="section-tip">开启后仅隐藏发布者昵称</text>
        </view>
        <view class="switch" :class="{ active: isAnonymous }" @click="toggleAnonymous">
          <view class="switch-dot"></view>
        </view>
      </view>
    </view>

    <view class="bottom-space"></view>

    <view class="bottom-bar">
      <view
        class="publish-btn"
        :class="{ disabled: !canPublish }"
        @click="handlePublish"
      >
        {{ publishing ? '发布中...' : '确认发布' }}
      </view>
    </view>
  </view>
</template>

<script>
import { publishPost } from '../../api/post.js'
import { getCategories } from '@/api/index.js'
import { commonApi } from '@/api/common.js'

export default {
  name: 'PublishPost',
  data() {
    return {
      showTopicPanel: true,
      selectedTopic: null,
      topicList: [],
      content: '',
      imageList: [],
      price: '',
      tradeMethod: 'face',
      voteOptions: ['', ''],
      isAnonymous: false,
      publishing: false
    }
  },
  async mounted() {
    try {
      const res = await getCategories()
      if (res.code === 200) {
        const list = Array.isArray(res.data?.list) ? res.data.list : []
        this.topicList = list
      }
    } catch (error) {
      console.error('获取话题列表失败:', error)
      uni.showToast({ title: '获取话题失败', icon: 'none' })
    }
  },
  computed: {
    selectedTopicData() {
      return this.topicList.find(t => t.id === this.selectedTopic) || {}
    },
    canPublish() {
      const hasContent = this.content.trim().length > 0 || this.imageList.length > 0

      if (this.selectedTopic === 1 && !this.price) {
        return false
      }

      if (this.selectedTopic === 4) {
        const validOptions = this.voteOptions.filter(o => o.trim())
        if (validOptions.length < 2) {
          return false
        }
      }

      return hasContent && !this.publishing
    }
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    toggleTopicPanel() {
      this.showTopicPanel = !this.showTopicPanel
    },
    selectTopic(topic) {
      this.selectedTopic = topic.id
      this.showTopicPanel = false
    },
    clearTopic() {
      this.selectedTopic = null
      this.showTopicPanel = true
    },
    chooseImage() {
      const remainCount = 9 - this.imageList.length

      uni.chooseImage({
        count: remainCount,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.imageList = [...this.imageList, ...res.tempFilePaths]
        },
        fail: () => {
          uni.showToast({ title: '选择图片失败', icon: 'none' })
        }
      })
    },
    previewImage(index) {
      uni.previewImage({
        current: index,
        urls: this.imageList
      })
    },
    deleteImage(index) {
      this.imageList.splice(index, 1)
    },
    addVoteOption() {
      if (this.voteOptions.length < 6) {
        this.voteOptions.push('')
      }
    },
    removeVoteOption(index) {
      if (this.voteOptions.length > 2) {
        this.voteOptions.splice(index, 1)
      }
    },
    toggleAnonymous() {
      this.isAnonymous = !this.isAnonymous
    },
    async uploadImages(tempFiles) {
      const uploadPromises = tempFiles.map(file => commonApi.uploadBatchImages(file))
      const results = await Promise.all(uploadPromises)

      return results.map(res => {
        if (res.code === 200 && res.data) {
          if (res.data.url) return res.data.url
          if (typeof res.data === 'string') return res.data
          if (res.data.list && res.data.list.length > 0) {
            return res.data.list[0].url
          }
        }
        throw new Error('上传响应格式错误: ' + JSON.stringify(res))
      })
    },
    async handlePublish() {
      if (!this.canPublish || this.publishing) return

      this.publishing = true

      try {
        let uploadedImages = []
        if (this.imageList.length > 0) {
          uploadedImages = await this.uploadImages(this.imageList)
        }

        const postData = {
          categoryId: this.selectedTopic,
          topicId: this.selectedTopic,
          topicName: this.selectedTopicData.name,
          content: this.content,
          images: uploadedImages,
          isAnonymous: this.isAnonymous
        }

        if (this.selectedTopic === 1) {
          postData.product = {
            price: parseFloat(this.price) || 0,
            tradeMethod: this.tradeMethod
          }
        }

        if (this.selectedTopic === 4) {
          postData.vote = {
            options: this.voteOptions.filter(o => o.trim())
          }
        }

        const res = await publishPost(postData)

        if (res.code === 200) {
          uni.showToast({ title: '发布成功', icon: 'success' })
          setTimeout(() => {
            uni.navigateBack()
          }, 1200)
        } else {
          uni.showToast({ title: res.message || '发布失败', icon: 'none' })
        }
      } catch (error) {
        console.error('发布失败:', error)
        uni.showToast({ title: '发布失败，请重试', icon: 'none' })
      } finally {
        this.publishing = false
      }
    }
  }
}
</script>

<style scoped>
.publish-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 24rpx 24rpx 168rpx;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.24), transparent 28%),
    linear-gradient(180deg, #faf7f2 0%, #f5f1eb 100%);
}

.page-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(24rpx);
  opacity: 0.42;
  pointer-events: none;
}

.page-glow-left {
  width: 240rpx;
  height: 240rpx;
  top: 140rpx;
  left: -90rpx;
  background: rgba(185, 160, 213, 0.34);
}

.page-glow-right {
  width: 200rpx;
  height: 200rpx;
  top: 320rpx;
  right: -70rpx;
  background: rgba(140, 128, 216, 0.24);
}

.nav-header,
.intro-card,
.section-card,
.bottom-bar {
  position: relative;
  z-index: 2;
}

.nav-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.nav-btn {
  min-width: 84rpx;
  height: 72rpx;
  padding: 0 22rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.8);
  border: 1rpx solid rgba(140, 128, 216, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--theme-ink);
  font-size: 36rpx;
}

.nav-btn.ghost {
  font-size: 24rpx;
  color: var(--theme-primary-deep);
  font-weight: 700;
}

.nav-btn.disabled {
  opacity: 0.45;
}

.nav-copy {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.nav-kicker,
.nav-title {
  display: block;
}

.nav-kicker {
  font-size: 20rpx;
  letter-spacing: 2rpx;
  text-transform: uppercase;
  color: var(--theme-primary-deep);
  font-weight: 700;
}

.nav-title {
  margin-top: 6rpx;
  font-size: 30rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.intro-card {
  padding: 34rpx 32rpx;
  border-radius: 30rpx;
  background: var(--theme-gradient);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow);
}

.intro-title,
.intro-desc,
.section-title,
.section-tip {
  display: block;
}

.intro-title {
  font-size: 42rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.intro-desc {
  margin-top: 12rpx;
  font-size: 25rpx;
  line-height: 1.7;
  color: #6d6582;
}

.section-card {
  margin-top: 22rpx;
  padding: 28rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.86);
  border: 1rpx solid rgba(140, 128, 216, 0.1);
  box-shadow: var(--theme-shadow-soft);
}

.section-card.compact {
  padding: 24rpx 28rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.section-head.static {
  align-items: flex-start;
}

.section-title {
  font-size: 30rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.section-title.small {
  font-size: 28rpx;
}

.section-tip {
  margin-top: 8rpx;
  font-size: 23rpx;
  line-height: 1.6;
  color: var(--theme-muted);
}

.section-arrow {
  font-size: 30rpx;
  color: var(--theme-muted);
  transition: transform 0.3s;
}

.section-arrow.expanded {
  transform: rotate(180deg);
}

.topic-list {
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx;
}

.topic-tag {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: rgba(140, 128, 216, 0.08);
  border: 1rpx solid rgba(140, 128, 216, 0.08);
  border-radius: 999rpx;
  font-size: 26rpx;
  color: var(--theme-muted);
}

.topic-tag.active {
  background: var(--theme-gradient-strong);
  color: #fff;
}

.selected-topic {
  margin-top: 8rpx;
}

.remove-btn {
  margin-left: 12rpx;
  font-size: 28rpx;
}

.content-input {
  width: 100%;
  min-height: 240rpx;
  font-size: 30rpx;
  color: var(--theme-ink);
  line-height: 1.75;
}

.word-count {
  text-align: right;
  font-size: 23rpx;
  color: var(--theme-muted);
  margin-top: 18rpx;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx;
}

.image-item,
.add-image-btn {
  position: relative;
  width: 200rpx;
  height: 200rpx;
  border-radius: 22rpx;
  overflow: hidden;
}

.image-item image {
  width: 100%;
  height: 100%;
}

.delete-btn {
  position: absolute;
  top: 12rpx;
  right: 12rpx;
  width: 42rpx;
  height: 42rpx;
  border-radius: 50%;
  background: rgba(52, 48, 48, 0.68);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
}

.add-image-btn {
  background: rgba(140, 128, 216, 0.08);
  border: 2rpx dashed rgba(140, 128, 216, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-icon {
  font-size: 56rpx;
  color: var(--theme-primary-deep);
}

.form-item {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  gap: 18rpx;
}

.form-item.vertical {
  flex-direction: column;
  align-items: flex-start;
}

.label {
  width: 120rpx;
  font-size: 27rpx;
  color: var(--theme-ink);
  font-weight: 600;
}

.input-shell {
  flex: 1;
  min-height: 84rpx;
  padding: 0 22rpx;
  border-radius: 20rpx;
  background: rgba(140, 128, 216, 0.08);
  display: flex;
  align-items: center;
}

.price-symbol {
  font-size: 30rpx;
  color: #ce6f8b;
  margin-right: 12rpx;
}

.price-input {
  flex: 1;
  height: 84rpx;
  font-size: 30rpx;
  color: var(--theme-ink);
}

.trade-options {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.trade-option {
  padding: 16rpx 24rpx;
  background: rgba(140, 128, 216, 0.08);
  border-radius: 999rpx;
  font-size: 25rpx;
  color: var(--theme-muted);
}

.trade-option.active {
  background: var(--theme-gradient-strong);
  color: #fff;
}

.vote-option-item {
  display: flex;
  align-items: center;
  margin-bottom: 18rpx;
  gap: 16rpx;
}

.vote-input {
  flex: 1;
  height: 82rpx;
  background: rgba(140, 128, 216, 0.08);
  border-radius: 20rpx;
  padding: 0 20rpx;
  font-size: 27rpx;
}

.delete-option {
  width: 52rpx;
  text-align: center;
  font-size: 34rpx;
  color: var(--theme-muted);
}

.add-option-btn {
  padding: 18rpx 0 6rpx;
  color: var(--theme-primary-deep);
  font-size: 26rpx;
  font-weight: 700;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20rpx;
}

.switch {
  width: 100rpx;
  height: 56rpx;
  background-color: rgba(140, 128, 216, 0.18);
  border-radius: 28rpx;
  padding: 6rpx;
  transition: background-color 0.3s;
}

.switch.active {
  background: var(--theme-gradient-strong);
}

.switch-dot {
  width: 44rpx;
  height: 44rpx;
  background-color: #fff;
  border-radius: 50%;
  transition: transform 0.3s;
}

.switch.active .switch-dot {
  transform: translateX(44rpx);
}

.bottom-space {
  height: 20rpx;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 18rpx 24rpx;
  padding-bottom: calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(247, 244, 238, 0.92);
  backdrop-filter: blur(18rpx);
  -webkit-backdrop-filter: blur(18rpx);
}

.publish-btn {
  height: 92rpx;
  background: var(--theme-gradient-strong);
  color: #fff;
  font-size: 30rpx;
  font-weight: 800;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 28rpx rgba(121, 110, 176, 0.24);
}

.publish-btn.disabled {
  opacity: 0.42;
  pointer-events: none;
}
</style>
