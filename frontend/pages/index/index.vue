<template>
  <view class="home-page">
    <view class="page-glow page-glow-left"></view>
    <view class="page-glow page-glow-right"></view>

    <view class="header" :class="{ 'is-fixed': isNavFixed }">
      <view class="brand-mark">
        <view class="brand-icon">◌</view>
        <view class="brand-copy">
          <text class="brand-name">逛校园</text>
          <text class="brand-sub">分享日常，发现同好</text>
        </view>
      </view>
      <view class="header-pill" @click="switchTab('info')">订阅中心</view>
    </view>

    <view v-if="isNavFixed" class="header-placeholder"></view>

    <view class="hero-card">
      <view class="hero-copy">
        <text class="hero-badge">Campus Social</text>
        <text class="hero-title">把校园生活整理成你自己的内容场</text>
        <text class="hero-subtitle">发现校园热点、快速进入常用功能，随时浏览你关注的内容。</text>
      </view>
      <view class="hero-ornament">
        <view class="hero-orb hero-orb-main"></view>
        <view class="hero-orb hero-orb-small"></view>
      </view>

      <view class="search-shell">
        <text class="search-prefix">搜索</text>
        <input
          v-model="keyword"
          class="search-input"
          type="text"
          placeholder="输入帖子、活动、圈子关键词"
          @confirm="search"
        />
        <view class="search-action" @click="search">查找</view>
      </view>
    </view>

    <view class="entry-board">
      <view class="entry-board-head">
        <text class="entry-board-title">常用入口</text>
        <text class="entry-board-meta">{{ categories.length || 0 }} 个分区</text>
      </view>
      <view class="entry-grid">
        <view
          v-for="item in categories.slice(0, 4)"
          :key="item.id"
          class="entry-card"
          @click="switchCategory(item.id)"
        >
          <text class="entry-icon">{{ item.id === 0 ? '◎' : '◈' }}</text>
          <text class="entry-label">{{ item.name }}</text>
        </view>
        <view class="entry-card accent" @click="goPublish">
          <text class="entry-icon">+</text>
          <text class="entry-label">发帖</text>
        </view>
        <view class="entry-card accent" @click="switchTab('profile')">
          <text class="entry-icon">◌</text>
          <text class="entry-label">我的主页</text>
        </view>
      </view>
    </view>

    <view class="action-strip">
      <view class="action-card" @click="handleCheckIn">
        <view class="action-copy">
          <text class="action-title">每日签到</text>
          <text class="action-desc">{{ isCheckedIn ? '今天已完成签到' : '去签到领积分' }}</text>
          <view class="action-go">{{ isCheckedIn ? 'DONE' : 'GO' }}</view>
        </view>
        <view class="action-illus">✦</view>
      </view>

      <view class="action-card alt" @click="goAiAssistant">
        <view class="action-copy">
          <text class="action-title">马上发布</text>
          <text class="action-desc">把灵感整理成一条校园内容</text>
          <view class="action-go dark">ASK AI</view>
        </view>
        <view class="action-illus">AI</view>
      </view>
    </view>

    <HotTopics
      :topics="topics.slice(0, 4)"
      @topic-click="handleTopicClick"
      @more-click="handleTopicsMore"
    />

    <view class="stream-head">
      <view>
        <text class="stream-title">校园动态</text>
        <text class="stream-subtitle">从你关注的内容开始浏览</text>
      </view>
      <view class="stream-chip">{{ posts.length }} 条内容</view>
    </view>

    <CategoryNav
      :categories="categories"
      :current-category="currentCategory"
      :is-fixed="isNavFixed"
      @category-change="switchCategory"
    />

    <view class="post-list">
      <PostCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
        @user-click="handleUserClick"
        @more-click="handlePostMore"
        @post-click="handlePostClick"
        @image-click="handleImageClick"
        @product-click="handleProductClick"
        @comment-click="handleCommentClick"
        @like-click="handleLikeClick"
        @top-click="handleTopClick"
      />
    </view>

    <view class="bottom-space"></view>
    <TabBar :current-tab="currentTab" @tab-change="switchTab" />
  </view>
</template>

<script>
import TabBar from '@/components/TabBar.vue'
import PostCard from '@/components/PostCard.vue'
import HotTopics from '@/components/HotTopics.vue'
import CategoryNav from '@/components/Categorynav.vue'
import { userApi } from '@/api/user.js'
import { setPostTop, likePost } from '@/api/post.js'
import { getHotTopics, getCategories, getPosts, searchPosts } from '@/api/index.js'

export default {
  components: {
    TabBar,
    PostCard,
    HotTopics,
    CategoryNav
  },
  data() {
    return {
      currentTab: 'home',
      currentCategory: 0,
      isNavFixed: false,
      topics: [],
      categories: [],
      posts: [],
      keyword: '',
      isCheckedIn: false,
      page: 1,
      pageSize: 10,
      hasMore: true
    }
  },
  async onLoad() {
    await Promise.all([this.loadTopics(), this.loadCategories(), this.loadPosts()])
  },
  onShow() {
    uni.hideTabBar({ animation: false })
    this.loadCheckInStatus()
    this.loadPosts(true)
  },
  onPageScroll(e) {
    this.isNavFixed = e.scrollTop > 560
  },
  methods: {
    async loadTopics() {
      const res = await getHotTopics()
      if (res.code === 200) this.topics = res.data
    },
    async loadCheckInStatus() {
      try {
        const res = await userApi.getCheckInStatus()
        if (res.code === 200) {
          this.isCheckedIn = !!res.data?.checkedIn
        }
      } catch (error) {
        this.isCheckedIn = false
      }
    },
    async handleCheckIn() {
      try {
        const userRes = await userApi.getUserInfo()
        if (userRes.code !== 200) {
          uni.navigateTo({ url: '/pages/login/index' })
          return
        }
      } catch (error) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }

      if (this.isCheckedIn) {
        uni.showToast({ title: '今天已经签到过了', icon: 'none' })
        return
      }

      try {
        const res = await userApi.checkIn()
        if (res.code === 200) {
          this.isCheckedIn = true
          uni.showToast({ title: '签到成功', icon: 'success' })
        }
      } catch (error) {
        uni.showToast({ title: '签到失败', icon: 'none' })
      }
    },
    async handleTopClick(post) {
      if (post.isTop) {
        const res = await setPostTop(post.id, false)
        if (res.code === 200) {
          uni.showToast({ title: '已取消置顶', icon: 'none' })
          this.loadPosts(true)
        }
        return
      }

      const userRes = await userApi.getUserInfo()
      const user = userRes.data
      if ((user.points || 0) < 30) {
        uni.showToast({ title: '积分不足，需要 30 积分置顶', icon: 'none' })
        return
      }

      const res = await setPostTop(post.id, true)
      if (res.code === 200) {
        uni.showToast({ title: '置顶成功', icon: 'success' })
        this.loadPosts(true)
      }
    },
    async loadCategories() {
      const res = await getCategories()
      if (res.code !== 200) return

      const raw = Array.isArray(res.data?.list) ? res.data.list : []
      const cats = raw.filter(c => c && (c.isActive === undefined || c.isActive))
      if (!cats.some(c => c && c.id === 0)) cats.unshift({ id: 0, name: '全部' })
      this.categories = cats
    },
    async loadPosts(reset = true) {
      if (reset) {
        this.page = 1
        this.posts = []
      }

      const params = {
        page: this.page,
        pageSize: this.pageSize
      }
      if (this.currentCategory && this.currentCategory > 0) {
        params.categoryId = this.currentCategory
      }

      const res = await getPosts(params)
      if (res.code !== 200) return

      const list = Array.isArray(res.data?.list) ? res.data.list : []
      this.posts = reset ? list : [...this.posts, ...list]
      this.hasMore = !!res.data?.hasMore
      if (this.hasMore) this.page += 1

      this.posts.sort((a, b) => {
        const topDiff = (b.isTop ? 1 : 0) - (a.isTop ? 1 : 0)
        if (topDiff !== 0) return topDiff
        const tb = new Date(b.time).getTime() || 0
        const ta = new Date(a.time).getTime() || 0
        return tb - ta
      })
    },
    async switchCategory(id) {
      this.currentCategory = id
      await this.loadPosts(true)
    },
    async search() {
      const kw = (this.keyword || '').trim()
      if (!kw) {
        await this.loadPosts(true)
        return
      }

      const res = await searchPosts(kw)
      if (res.code !== 200) return
      this.posts = res.data?.list || []
      this.hasMore = !!res.data?.hasMore
      this.page = 1
    },
    handleTopicClick(post) {
      uni.navigateTo({ url: `/pages/post/detail?id=${post.id}` })
    },
    handleTopicsMore() {
      uni.navigateTo({ url: '/pages/hot-posts/list' })
    },
    switchTab(tabId) {
      if (tabId === 'publish') {
        uni.navigateTo({ url: '/pages/publish/index' })
        return
      }
      if (tabId === 'info') {
        uni.switchTab({ url: '/pages/info-center/index' })
        return
      }
      if (tabId === 'profile') {
        uni.switchTab({ url: '/pages/profile/index' })
        return
      }
      this.currentTab = tabId
    },
    goPublish() {
      uni.navigateTo({ url: '/pages/publish/index' })
    },
    goAiAssistant() {
      uni.navigateTo({ url: '/pages/ai-assistant/index' })
    },
    handleUserClick(post) {
      if (!post?.userId) return
      uni.navigateTo({ url: `/pages/user/home?id=${post.userId}` })
    },
    handlePostMore() {
      uni.showActionSheet({ itemList: ['收藏', '分享', '举报'] })
    },
    handlePostClick(post) {
      uni.navigateTo({ url: `/pages/post/detail?id=${post.id}` })
    },
    handleImageClick({ post, imageIndex }) {
      if (post.images && post.images.length > 0 && !post.images[0].startsWith('#')) {
        uni.previewImage({ urls: post.images, current: imageIndex })
      }
    },
    handleProductClick(post) {
      uni.navigateTo({ url: `/pages/post/detail?id=${post.id}` })
    },
    handleCommentClick(post) {
      uni.navigateTo({ url: `/pages/post/detail?id=${post.id}&focus=comment` })
    },
    handleLikeClick({ post, isLiked }) {
      ;(async () => {
        try {
          const res = await likePost(post.id, isLiked)
          if (res.code !== 200) return
          const target = this.posts.find(p => p.id === post.id)
          if (!target) return
          target.isLiked = isLiked
          const cur = parseInt(target.likes || 0, 10) || 0
          const next = cur + (isLiked ? 1 : -1)
          target.likes = next < 0 ? 0 : next
        } catch (error) {
          uni.showToast({ title: '点赞失败', icon: 'none' })
        }
      })()
    }
  }
}
</script>

<style scoped>
.home-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.28), transparent 30%),
    radial-gradient(circle at top right, rgba(140, 128, 216, 0.18), transparent 26%),
    linear-gradient(180deg, #fbf8f3 0%, #f7f4ee 35%, #f5f1eb 100%);
  padding: 28rpx 0 160rpx;
}

.page-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(20rpx);
  opacity: 0.45;
  pointer-events: none;
}

.page-glow-left {
  width: 280rpx;
  height: 280rpx;
  top: 40rpx;
  left: -90rpx;
  background: rgba(185, 160, 213, 0.36);
}

.page-glow-right {
  width: 220rpx;
  height: 220rpx;
  top: 150rpx;
  right: -60rpx;
  background: rgba(140, 128, 216, 0.22);
}

.header {
  position: relative;
  z-index: 3;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 30rpx 20rpx;
  padding: 18rpx 20rpx;
  border-radius: 26rpx;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18rpx);
  -webkit-backdrop-filter: blur(18rpx);
  border: 1rpx solid rgba(140, 128, 216, 0.14);
  transition: all 0.3s ease;
}

.header.is-fixed {
  position: fixed;
  top: 18rpx;
  left: 30rpx;
  right: 30rpx;
  box-shadow: var(--theme-shadow-soft);
}

.header-placeholder {
  height: 116rpx;
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.brand-icon {
  width: 74rpx;
  height: 74rpx;
  border-radius: 22rpx;
  background: var(--theme-gradient-strong);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 34rpx;
  box-shadow: 0 10rpx 22rpx rgba(121, 110, 176, 0.2);
}

.brand-copy {
  display: flex;
  flex-direction: column;
}

.brand-name {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.brand-sub {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--theme-muted);
}

.header-pill {
  padding: 14rpx 24rpx;
  border-radius: 999rpx;
  background: rgba(140, 128, 216, 0.1);
  color: var(--theme-primary-deep);
  font-size: 24rpx;
  font-weight: 600;
}

.hero-card {
  position: relative;
  z-index: 2;
  margin: 0 30rpx 24rpx;
  padding: 38rpx 32rpx 30rpx;
  border-radius: 34rpx;
  background: var(--theme-gradient);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow);
  overflow: hidden;
}

.hero-copy {
  position: relative;
  z-index: 2;
  width: 74%;
}

.hero-badge,
.hero-title,
.hero-subtitle {
  display: block;
}

.hero-badge {
  font-size: 22rpx;
  letter-spacing: 2rpx;
  text-transform: uppercase;
  color: var(--theme-primary-deep);
  font-weight: 700;
}

.hero-title {
  margin-top: 18rpx;
  font-size: 54rpx;
  line-height: 1.18;
  font-weight: 800;
  color: var(--theme-ink);
}

.hero-subtitle {
  margin-top: 16rpx;
  font-size: 25rpx;
  line-height: 1.7;
  color: #67607d;
}

.hero-ornament {
  position: absolute;
  right: 24rpx;
  top: 30rpx;
  width: 190rpx;
  height: 190rpx;
}

.hero-orb {
  position: absolute;
  border-radius: 50%;
}

.hero-orb-main {
  width: 156rpx;
  height: 156rpx;
  right: 0;
  top: 0;
  background: radial-gradient(circle at 35% 35%, rgba(255, 255, 255, 0.96), rgba(185, 160, 213, 0.48));
  border: 1rpx solid rgba(255, 255, 255, 0.4);
}

.hero-orb-small {
  width: 84rpx;
  height: 84rpx;
  left: 6rpx;
  bottom: 8rpx;
  background: radial-gradient(circle at 40% 40%, rgba(255, 255, 255, 0.84), rgba(140, 128, 216, 0.34));
}

.search-shell {
  position: relative;
  z-index: 2;
  margin-top: 34rpx;
  min-height: 92rpx;
  padding: 10rpx 12rpx 10rpx 28rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.search-prefix {
  font-size: 24rpx;
  color: var(--theme-muted);
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: var(--theme-ink);
}

.search-action {
  min-width: 132rpx;
  height: 72rpx;
  border-radius: 999rpx;
  background: var(--theme-ink);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 700;
}

.entry-board {
  margin: 0 30rpx 24rpx;
  padding: 28rpx;
  background: rgba(255, 255, 255, 0.76);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  border-radius: 30rpx;
  box-shadow: var(--theme-shadow-soft);
}

.entry-board-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.entry-board-title {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--theme-ink);
}

.entry-board-meta {
  font-size: 22rpx;
  color: var(--theme-muted);
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
}

.entry-card {
  min-height: 118rpx;
  padding: 20rpx 18rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.88);
  border: 1rpx solid rgba(140, 128, 216, 0.1);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.entry-card.accent {
  background: linear-gradient(135deg, rgba(185, 160, 213, 0.18), rgba(140, 128, 216, 0.08));
}

.entry-icon {
  font-size: 34rpx;
  color: var(--theme-primary-deep);
}

.entry-label {
  font-size: 24rpx;
  color: var(--theme-ink);
  font-weight: 600;
}

.action-strip {
  margin: 0 30rpx 24rpx;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.action-card {
  min-height: 176rpx;
  padding: 24rpx 22rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.84);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow-soft);
  display: flex;
  justify-content: space-between;
  gap: 12rpx;
}

.action-card.alt {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(241, 235, 252, 0.9));
}

.action-card.alt .action-title {
  font-size: 0;
}

.action-card.alt .action-title::after {
  content: 'AI 助手';
  font-size: 30rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.action-card.alt .action-desc {
  font-size: 0;
}

.action-card.alt .action-desc::after {
  content: '通知问答与帖子智能搜索';
  font-size: 23rpx;
  line-height: 1.6;
  color: var(--theme-muted);
}

.action-copy {
  flex: 1;
}

.action-title,
.action-desc {
  display: block;
}

.action-title {
  font-size: 30rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.action-desc {
  margin-top: 10rpx;
  font-size: 23rpx;
  line-height: 1.6;
  color: var(--theme-muted);
}

.action-go {
  margin-top: 18rpx;
  width: 92rpx;
  height: 42rpx;
  border-radius: 999rpx;
  background: var(--theme-gradient-strong);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  font-weight: 700;
}

.action-go.dark {
  background: var(--theme-ink);
}

.action-illus {
  width: 68rpx;
  height: 68rpx;
  border-radius: 22rpx;
  background: rgba(140, 128, 216, 0.12);
  color: var(--theme-primary-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
}

.stream-head {
  margin: 12rpx 30rpx 8rpx;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20rpx;
}

.stream-title,
.stream-subtitle {
  display: block;
}

.stream-title {
  font-size: 34rpx;
  font-weight: 800;
  color: var(--theme-ink);
}

.stream-subtitle {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--theme-muted);
}

.stream-chip {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(140, 128, 216, 0.1);
  color: var(--theme-primary-deep);
  font-size: 22rpx;
}

.post-list {
  padding: 18rpx 30rpx 0;
}

.bottom-space {
  height: 20rpx;
}
</style>
