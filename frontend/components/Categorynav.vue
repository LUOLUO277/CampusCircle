<template>
  <view>
    <!-- 占位元素 -->
    <view class="category-nav-placeholder" v-if="isFixed"></view>
    
    <!-- 分类导航 - 吸顶 + 磨砂效果 -->
    <view class="category-nav-wrapper" :class="{ 'is-fixed': isFixed }">
      <scroll-view class="category-nav" scroll-x show-scrollbar="false">
        <view 
          v-for="(item, index) in categories" 
          :key="index"
          class="category-item"
          :class="{ active: currentCategory === item.id }"
          @click="handleCategoryClick(item.id)"
        >
          {{ item.name }}
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'CategoryNav',
  props: {
    categories: {
      type: Array,
      default: () => []
    },
    currentCategory: {
      type: [Number, String],
      default: 0
    },
    isFixed: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    handleCategoryClick(id) {
      this.$emit('category-change', id);
    }
  }
};
</script>

<style scoped>
/* 分类导航占位元素 */
.category-nav-placeholder {
  height: 288rpx;
}

/* 分类导航 - 吸顶 + 磨砂效果 */
.category-nav-wrapper {
  background-color: transparent;
  border-bottom: 1rpx solid #f0f0f0;
  transition: all 0.3s ease;
  z-index: 99;
 
}

.category-nav-wrapper.is-fixed {
  position: fixed;
  top: 120rpx;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20rpx);
  -webkit-backdrop-filter: blur(20rpx);
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.08);
}

.category-nav {
  padding: 20rpx 30rpx;
  white-space: nowrap;
}

.category-item {
  display: inline-block;
  padding: 10rpx 25rpx;
  margin-right: 20rpx;
  font-size: 28rpx;
  color: #666;
  border-radius: 30rpx;
  transition: all 0.3s;
}

.category-item.active {
  background-color: #e8f5e9;
  color: #4caf50;
  font-weight: bold;
}
</style>