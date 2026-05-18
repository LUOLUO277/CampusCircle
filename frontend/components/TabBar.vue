<template>
  <view class="tabbar-container">
    <view class="tabbar">
      <view
        v-for="item in tabbarList"
        :key="item.id"
        class="tabbar-item"
        :class="{ active: currentTab === item.id, 'publish-btn': item.id === 'publish' }"
        @click.stop="handleTabClick(item.id)"
      >
        <view v-if="item.id === 'publish'" class="publish-circle">
          <text class="tabbar-icon">{{ item.icon }}</text>
        </view>
        <template v-else>
          <text class="tabbar-icon">{{ item.icon }}</text>
          <text class="tabbar-label">{{ item.label }}</text>
        </template>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'TabBar',
  props: {
    currentTab: {
      type: String,
      default: 'home'
    }
  },
  data() {
    return {
      tabbarList: [
        { id: 'home', label: '首页', icon: '⌂', path: '/pages/index/index' },
        { id: 'info', label: '订阅', icon: '◫', path: '/pages/info-center/index' },
        { id: 'publish', label: '', icon: '+', path: '/pages/publish/index' },
        { id: 'profile', label: '我的', icon: '◌', path: '/pages/profile/index' }
      ]
    }
  },
  methods: {
    handleTabClick(tabId) {
      if (tabId === this.currentTab) return

      const target = this.tabbarList.find(item => item.id === tabId)
      if (!target) return

      if (tabId === 'publish') {
        uni.navigateTo({
          url: target.path
        })
        return
      }

      uni.switchTab({
        url: target.path,
        success: () => {
          this.$emit('tab-change', tabId)
        },
        fail: (err) => {
          console.error('Switch Tab Failed:', err)
        }
      })
    }
  }
}
</script>

<style scoped>
.tabbar-container {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 18rpx 28rpx;
  padding-bottom: calc(18rpx + env(safe-area-inset-bottom));
  z-index: 50;
  pointer-events: none;
}

.tabbar {
  background: rgba(255, 255, 255, 0.82);
  border: 1rpx solid rgba(140, 128, 216, 0.16);
  border-radius: 38rpx;
  height: 124rpx;
  display: flex;
  justify-content: space-around;
  align-items: center;
  backdrop-filter: blur(22rpx);
  -webkit-backdrop-filter: blur(22rpx);
  box-shadow: 0 16rpx 36rpx rgba(121, 110, 176, 0.18);
  pointer-events: auto;
}

.tabbar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.tabbar-item.active .tabbar-icon,
.tabbar-item.active .tabbar-label {
  color: var(--theme-primary-deep);
}

.tabbar-icon {
  font-size: 44rpx;
  color: #8f88a6;
  margin-bottom: 4rpx;
}

.tabbar-label {
  font-size: 20rpx;
  color: #8f88a6;
}

.publish-btn {
  position: relative;
  top: -30rpx;
}

.publish-circle {
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background: var(--theme-gradient-strong);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 6rpx solid rgba(255, 255, 255, 0.94);
  box-shadow: 0 12rpx 28rpx rgba(121, 110, 176, 0.28);
}

.publish-circle .tabbar-icon {
  color: #fff;
  font-size: 50rpx;
  margin-bottom: 0;
}
</style>
