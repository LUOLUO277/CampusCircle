<template>
  <view>
    <view class="category-nav-placeholder" v-if="isFixed"></view>

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
      this.$emit('category-change', id)
    }
  }
}
</script>

<style scoped>
.category-nav-placeholder {
  height: 134rpx;
}

.category-nav-wrapper {
  background-color: transparent;
  transition: all 0.3s ease;
  z-index: 99;
}

.category-nav-wrapper.is-fixed {
  position: fixed;
  top: 114rpx;
  left: 26rpx;
  right: 26rpx;
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(140, 128, 216, 0.14);
  border-radius: 26rpx;
  backdrop-filter: blur(22rpx);
  -webkit-backdrop-filter: blur(22rpx);
  box-shadow: var(--theme-shadow-soft);
}

.category-nav {
  padding: 18rpx 24rpx;
  white-space: nowrap;
}

.category-item {
  display: inline-block;
  padding: 14rpx 24rpx;
  margin-right: 16rpx;
  font-size: 26rpx;
  color: var(--theme-muted);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  transition: all 0.3s;
}

.category-item.active {
  background: var(--theme-gradient-strong);
  color: #fff;
  font-weight: 700;
  box-shadow: 0 10rpx 22rpx rgba(121, 110, 176, 0.18);
}
</style>
