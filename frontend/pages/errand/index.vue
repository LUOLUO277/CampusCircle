<template>
 <view class="page-container">
     <!-- 自定义头部 (绿色) -->
     <view class="custom-header green-theme">
       <!-- 状态栏占位 (手机时间信号栏) -->
       <view class="status-bar"></view>
       <!-- 标题栏 -->
       <view class="nav-bar">
         <text class="page-title">跑腿圈子</text>
       </view>
     </view>
    
    
    <!-- 内容区域 (需要加 padding-top 避开头部) -->
        <scroll-view scroll-y class="content" @scrolltolower="loadMore">
      <view 
        v-for="item in list" 
        :key="item.id" 
        class="errand-card"
        @click="goToDetail(item.id)"
      >
        <view class="card-header">
          <view class="user-info">
            <view class="avatar">👤</view>
            <text class="name">{{ item.publisher.nickname }}</text>
          </view>
          <view class="bounty-wrapper">
                       <text v-if="item.currency === 1" class="coin-icon">💰</text>
                       <text v-else class="money-icon">¥</text>
                       <text class="bounty-num">{{ item.bounty }}</text>
                    </view>
        </view>
        
        <view class="card-body">
          <text class="desc">{{ item.content }}</text>
          <view class="route">
            <text class="tag pick">取</text> {{ item.pickupAddr }}
          </view>
          <view class="route">
            <text class="tag send">送</text> {{ item.deliveryAddr }}
          </view>
        </view>
        
        <view class="card-footer">
          <text class="time">刚刚发布</text>
          <view class="status-tag" :class="'s-'+item.status">
            {{ formatStatus(item.status) }}
          </view>
        </view>
      </view>
      
      <!-- 空状态 -->
      <view v-if="list.length === 0" class="empty">暂无跑腿订单</view>
    </scroll-view>

    <TabBar current-tab="circle" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TabBar from '@/components/TabBar.vue'
import { errandApi } from '@/api/errand'

const list = ref([])

onShow(() => {
  uni.hideTabBar()
  loadData()
})

const loadData = async () => {
  try {
    const res = await errandApi.getList()
    if (res.code === 200) {
      list.value = res.data.list
    }
  } catch (error) {
    console.error(error)
  }
}

const goToPublish = () => {
  uni.navigateTo({ url: '/pages/errand/publish' })
}

const goToDetail = (id) => {
  uni.navigateTo({ url: `/pages/errand/detail?id=${id}` })
}

const formatStatus = (status) => {
  const map = { 0: '待接单', 1: '进行中', 2: '已完成', 3: '已取消' }
  return map[status]
}

const loadMore = () => {
  // 加载更多逻辑
}
</script>

<style scoped>
.page-container { height: 100vh; display: flex; flex-direction: column; background: #F5F5F5; }
.nav-header { 
  background: #fff; 
  padding: 80rpx 30rpx 20rpx; 
  display: flex; 
  justify-content: center; /* 居中标题 */
  align-items: center; 
  z-index: 10; 
}
/* --- 自定义头部样式 --- */
.custom-header {
  width: 100%;
  position: fixed;
  top: 0;
  z-index: 100;
  padding-bottom: 20rpx;
}

.green-theme {
  /* 你的绿色渐变 */
  background: linear-gradient(135deg, #e8f5e9 0%, #a5d6a7 100%);
  box-shadow: 0 4rpx 10rpx rgba(76, 175, 80, 0.1);
}

.status-bar {
  /* 自动获取系统状态栏高度 */
  height: var(--status-bar-height); 
  width: 100%;
}

.nav-bar {
  height: 88rpx; /* 标准导航栏高度 */
  display: flex;
  justify-content: center; /* 标题居中 */
  align-items: center;
  position: relative;
}

.page-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #1b5e20; /* 深绿色文字 */
}

/* 内容区域下移，给头部留出空间 */
.content {
  flex: 1;
  padding: 20rpx;
  box-sizing: border-box;
  padding-bottom: 160rpx;
  margin-top: calc(var(--status-bar-height) + 108rpx); /* 状态栏+导航栏高度+余量 */
}
.title { font-size: 40rpx; font-weight: 800; }
.plus-btn { background: #E6F7FF; color: #1890FF; padding: 10rpx 24rpx; border-radius: 30rpx; font-size: 26rpx; font-weight: bold; }

.content { flex: 1; padding: 20rpx; box-sizing: border-box; padding-bottom: 160rpx; }

.errand-card { background: #fff; padding: 30rpx; border-radius: 16rpx; margin-bottom: 24rpx; }
.card-header { display: flex; justify-content: space-between; margin-bottom: 20rpx; }
.user-info { display: flex; align-items: center; }
.avatar { width: 50rpx; height: 50rpx; background: #eee; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 16rpx; }
.name { font-size: 26rpx; color: #666; }
.bounty { color: #ff4d4f; font-weight: bold; font-size: 32rpx; }

.desc { font-size: 30rpx; font-weight: bold; margin-bottom: 20rpx; display: block; }
.route { display: flex; align-items: center; font-size: 26rpx; color: #666; margin-bottom: 12rpx; }
.tag { font-size: 20rpx; padding: 2rpx 8rpx; border-radius: 4rpx; margin-right: 12rpx; color: #fff; }
.tag.pick { background: #1890FF; }
.tag.send { background: #52C41A; }

.card-footer { display: flex; justify-content: space-between; margin-top: 20rpx; padding-top: 20rpx; border-top: 1rpx solid #f9f9f9; }
.time { font-size: 24rpx; color: #ccc; }
.status-tag { font-size: 24rpx; font-weight: bold; }
.s-0 { color: #faad14; } /* 待接单 */
.s-1 { color: #1890FF; } /* 进行中 */
.s-2 { color: #52C41A; } /* 已完成 */
/* 新增货币样式 */
.bounty-wrapper { display: flex; align-items: center; }
.coin-icon { font-size: 30rpx; margin-right: 4rpx; }
.money-icon { font-size: 28rpx; color: #ff4d4f; font-weight: bold; margin-right: 2rpx; }
.bounty-num { font-size: 36rpx; color: #ff4d4f; font-weight: bold; }
</style>