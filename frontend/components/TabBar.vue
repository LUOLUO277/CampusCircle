<template>
  <view class="tabbar-container">
    <view class="tabbar">
      <view 
        class="tabbar-item" 
        v-for="(item, index) in tabbarList" 
        :key="index"
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
        { id: 'home', label: '首页', icon: '🏠', path: '/pages/index/index' },
        { id: 'circle', label: '跑腿', icon: '👥', path: '/pages/errand/index' }, // 对应 pages.json 里的 errand
        { id: 'publish', label: '', icon: '➕', path: '/pages/publish/index' },
        { id: 'message', label: '消息', icon: '💬', path: '/pages/message/index' },
        { id: 'profile', label: '我的', icon: '👤', path: '/pages/profile/index' }
      ]
    };
  },
  methods: {
    handleTabClick(tabId) {
      // 1. 如果点击的是当前页，不做操作
      if (tabId === this.currentTab) return;

      const target = this.tabbarList.find(item => item.id === tabId);
      
      if (tabId === 'publish') {
        // 发布按钮通常是 navigateTo (普通跳转) 或者打开弹窗，而不是 switchTab
        uni.navigateTo({
          url: target.path
        });
      } else {
        // 其他四个页面是 Tab 页，必须用 switchTab
        uni.switchTab({
          url: target.path,
          success: () => {
             // 只有 switchTab 成功后才触发事件（可选）
             this.$emit('tab-change', tabId);
          },
          fail: (err) => {
            console.error('Switch Tab Failed:', err);
          }
        });
      }
    }
  }
};
</script>

<style scoped>
/* 保持你原有的样式不变 */
.tabbar-container {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  z-index: 50; /* 提高层级 *降一下,要让举报浮在前面*/
  pointer-events: none;
}

.tabbar {
  background: #ffffff;
  border-radius: 50rpx;
  height: 120rpx;
  display: flex;
  justify-content: space-around;
  align-items: center;
  box-shadow: 0 -4rpx 30rpx rgba(0, 0, 0, 0.1);
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
  color: #52C41A;
}

.tabbar-icon {
  font-size: 44rpx;
  color: #666;
  margin-bottom: 4rpx;
}

.tabbar-label {
  font-size: 20rpx;
  color: #666;
}

.publish-btn {
  position: relative;
  top: -30rpx;
}

.publish-circle {
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #95DE64 0%, #52C41A 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 20rpx rgba(82, 196, 26, 0.4);
}

.publish-circle .tabbar-icon {
  color: #fff;
  font-size: 50rpx;
  margin-bottom: 0;
}
</style>