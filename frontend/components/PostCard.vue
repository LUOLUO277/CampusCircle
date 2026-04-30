<template>
  <view class="post-card">
    <view v-if="post.isTop" class="top-badge">馃搶缃《</view>

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
      <text class="product-tag">馃洅 鍦ㄥ敭</text>
      <text class="product-price">浠锋牸: 楼{{ post.product.price }}</text>
    </view>

    <view class="post-footer">
      <view class="footer-item">
        <text class="icon">馃憗</text>
        <text>{{ formatNumber(post.views) }}</text>
      </view>
      <view class="footer-item" @click="handleTopClick">
        <text class="icon">馃搶</text>
        <text>{{ post.isTop ? '鍙栨秷缃《' : '缃《' }}</text>
      </view>

      <view class="footer-item" @click="handleLikeClick">
        <text class="icon">{{ isLiked ? '鉂わ笍' : '馃憤' }}</text>
        <text>{{ formatNumber(post.likes) }}</text>
      </view>
      <view class="footer-item" @click="handleCommentClick">
        <text class="icon">馃挰</text>
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
        return (n / 10000).toFixed(1) + '涓?'
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
  top: 10rpx;
  right: 10rpx;
  background-color: #ff9800;
  color: #fff;
  padding: 6rpx 16rpx;
  font-size: 22rpx;
  border-radius: 8rpx;
  box-shadow: 0 2rpx 6rpx rgba(0,0,0,0.15);
  z-index: 10;
}

.post-card {
  position: relative;
  background-color: #fff;
  border-radius: 15rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
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
  background-color: #f0f0f0;
  margin-right: 20rpx;
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
  color: #333;
  font-weight: bold;
}

.post-time {
  font-size: 24rpx;
  color: #999;
  margin-top: 5rpx;
}

.post-more {
  font-size: 40rpx;
  color: #999;
  padding: 10rpx;
}

.post-content {
  margin-bottom: 20rpx;
}

.post-tag {
  display: inline-block;
  padding: 4rpx 12rpx;
  background-color: #e8f5e9;
  color: #4caf50;
  border-radius: 8rpx;
  font-size: 22rpx;
  margin-right: 10rpx;
  margin-bottom: 10rpx;
}

.post-text {
  font-size: 28rpx;
  color: #333;
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
  border-radius: 10rpx;
  background-color: #f0f0f0;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 15rpx;
  background-color: #fffef0;
  border-radius: 10rpx;
  margin-bottom: 20rpx;
}

.product-tag {
  font-size: 24rpx;
  color: #8bc34a;
}

.product-price {
  font-size: 28rpx;
  color: #f44336;
  font-weight: bold;
}

.post-footer {
  display: flex;
  justify-content: space-around;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 24rpx;
  color: #666;
  padding: 10rpx 15rpx;
}

.footer-item:active {
  background-color: #f5f5f5;
  border-radius: 10rpx;
}

.footer-item .icon {
  font-size: 28rpx;
}
</style>
