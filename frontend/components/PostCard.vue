<template>
  <view class="post-card">
    <view v-if="post.isTop" class="top-badge">置顶</view>

    <view class="post-header">
      <view class="user-info" @click="handleUserClick">
        <image
          class="user-avatar"
          :src="getFullImageUrl(post.userAvatar)"
          mode="aspectFill"
        ></image>

        <view class="user-detail">
          <view class="user-name-row">
            <text class="user-name">{{ post.userName }}</text>
          </view>
          <text class="post-time">{{ post.time }}</text>
        </view>
      </view>
      <text class="post-more" @click="handleMoreClick">...</text>
    </view>

    <view class="post-content" @click="handlePostClick">
      <view class="post-tag" v-if="post.tag">
        <text>{{ post.tag }}</text>
      </view>
      <text class="post-text">{{ post.content }}</text>
    </view>

    <view class="post-images" v-if="post.images && post.images.length > 0">
      <image
        class="image-item"
        v-for="(img, index) in post.images"
        :key="index"
        :src="getFullImageUrl(img)"
        mode="aspectFill"
        @click.stop="handleImageClick(index)"
      >
      </image>
    </view>

    <view class="product-info" v-if="post.product" @click="handleProductClick">
      <text class="product-tag">在售</text>
      <text class="product-price">价格 {{ post.product.price }}</text>
    </view>

    <view class="post-footer">
      <view class="footer-item">
        <text class="icon">◔</text>
        <text>{{ formatNumber(post.views) }}</text>
      </view>
      <view class="footer-item" @click="handleTopClick">
        <text class="icon">⌘</text>
        <text>{{ post.isTop ? '取消置顶' : '置顶' }}</text>
      </view>

      <view class="footer-item" @click="handleLikeClick">
        <text class="icon">{{ isLiked ? '♥' : '♡' }}</text>
        <text>{{ formatNumber(post.likes) }}</text>
      </view>
      <view class="footer-item" @click="handleCommentClick">
        <text class="icon">◌</text>
        <text>{{ formatNumber(post.comments) }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { toAbsoluteUrl } from '@/utils/api'

export default {
  name: 'PostCard',
  props: {
    post: {
      type: Object,
      required: true,
      default: () => ({})
    }
  },
  computed: {
    isLiked() {
      return !!(this.post && this.post.isLiked)
    }
  },
  methods: {
    getFullImageUrl(url) {
      return toAbsoluteUrl(url)
    },

    formatNumber(num) {
      if (!num) return '0'
      const n = parseInt(num)
      if (n >= 10000) {
        return (n / 10000).toFixed(1) + 'w'
      }
      return num
    },

    handleUserClick() { this.$emit('user-click', this.post) },
    handleMoreClick() { this.$emit('more-click', this.post) },
    handlePostClick() { this.$emit('post-click', this.post) },
    handleImageClick(index) { this.$emit('image-click', { post: this.post, imageIndex: index }) },
    handleProductClick() { this.$emit('product-click', this.post) },
    handleTopClick() { this.$emit('top-click', this.post) },
    handleCommentClick() { this.$emit('comment-click', this.post) },
    handleLikeClick() {
      const newStatus = !this.isLiked
      this.$emit('like-click', { post: this.post, isLiked: newStatus })
    }
  }
}
</script>

<style scoped>
.top-badge {
  position: absolute;
  top: 18rpx;
  right: 18rpx;
  background: rgba(140, 128, 216, 0.14);
  border: 1rpx solid rgba(140, 128, 216, 0.22);
  color: var(--theme-primary-deep);
  padding: 8rpx 18rpx;
  font-size: 22rpx;
  border-radius: 999rpx;
  box-shadow: 0 8rpx 16rpx rgba(121, 110, 176, 0.08);
  z-index: 10;
}

.post-card {
  position: relative;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(249, 246, 255, 0.96) 100%);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  border-radius: 28rpx;
  padding: 30rpx;
  margin-bottom: 22rpx;
  box-shadow: var(--theme-shadow-soft);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background-color: #f0eef8;
  margin-right: 20rpx;
  border: 2rpx solid rgba(140, 128, 216, 0.14);
}

.user-detail {
  display: flex;
  flex-direction: column;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.user-name {
  font-size: 28rpx;
  color: var(--theme-ink);
  font-weight: 700;
}

.post-time {
  font-size: 24rpx;
  color: var(--theme-muted);
  margin-top: 5rpx;
}

.post-more {
  font-size: 40rpx;
  color: var(--theme-muted);
  padding: 10rpx;
}

.post-content {
  margin-bottom: 20rpx;
}

.post-tag {
  display: inline-block;
  padding: 8rpx 16rpx;
  background: rgba(140, 128, 216, 0.12);
  color: var(--theme-primary-deep);
  border-radius: 999rpx;
  font-size: 22rpx;
  margin-right: 10rpx;
  margin-bottom: 10rpx;
}

.post-text {
  font-size: 28rpx;
  color: var(--theme-ink);
  line-height: 1.6;
  word-break: break-all;
}

.post-images {
  display: flex;
  gap: 10rpx;
  margin-bottom: 20rpx;
  flex-wrap: wrap;
}

.image-item {
  width: 200rpx;
  height: 200rpx;
  border-radius: 18rpx;
  background-color: #f0eef8;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 15rpx;
  background: rgba(255, 255, 255, 0.84);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  border-radius: 18rpx;
  margin-bottom: 20rpx;
}

.product-tag {
  font-size: 24rpx;
  color: var(--theme-primary-deep);
}

.product-price {
  font-size: 28rpx;
  color: #ce6f8b;
  font-weight: 700;
}

.post-footer {
  display: flex;
  justify-content: space-around;
  padding-top: 20rpx;
  border-top: 1rpx solid rgba(140, 128, 216, 0.12);
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 24rpx;
  color: var(--theme-muted);
  padding: 10rpx 15rpx;
}

.footer-item:active {
  background-color: rgba(140, 128, 216, 0.08);
  border-radius: 999rpx;
}

.footer-item .icon {
  font-size: 28rpx;
}
</style>
