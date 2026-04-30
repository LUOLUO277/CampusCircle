<template>

    <view class="container">
      <!-- 通用头部 (白色背景，黑色字) -->
      <view class="custom-header white-theme">
        <view class="status-bar"></view>
        <view class="nav-bar">
          <view class="back-btn" @click="goBack"><text class="back-arrow">←</text></view>
          <text class="page-title">我的发布</text>
        </view>
      </view>
  
      <view class="list-wrapper">
        <view v-for="item in list" :key="item.id" class="post-card" @click="goToDetail(item)">
          <!-- 卡片头部：用户信息 -->
          <view class="card-header">
            <view class="user-info">
              <!-- 直接复用 store 里的当前用户头像，因为是“我的发布” -->
              <image :src="userStore.avatar || '../../static/default-avatar.png'" class="avatar-img" mode="aspectFill"/>
              <view class="meta">
                <text class="user-name">{{ userStore.nickname }}</text>
                <text class="post-time">{{ formatDate(item.createTime) }}</text>
              </view>
            </view>
            <text class="more-icon">···</text>
          </view>
          
          <!-- 卡片内容 -->
          <view class="card-content">
            <text class="text-body">{{ item.content }}</text>
            <!-- 图片网格 -->
            <view v-if="item.images && item.images.length > 0" class="image-grid" :class="'grid-'+item.images.length">
               <image 
                 v-for="(img, idx) in item.images" 
                 :key="idx" 
                 :src="img" 
                 class="post-img" 
                 mode="aspectFill"
                 @click.stop="previewImage(item.images, idx)"
               />
            </view>
          </view>
          
          <!-- 卡片底部：数据 -->
          <view class="card-footer">
            <view class="action-item">
              <text class="icon">👁</text>
              <text class="count">{{ item.stats.views }}</text>
            </view>
            <view class="action-item">
              <text class="icon">💬</text>
              <text class="count">{{ item.stats.comments }}</text>
            </view>
            <view class="action-item">
               <text class="icon">👍</text>
               <text class="count">{{ item.stats.likes }}</text>
            </view>
          </view>
        </view>
        
        <view v-if="list.length === 0" class="empty">暂无发布内容</view>
      </view>
    </view>

</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user' // 引入store拿头像

const userStore = useUserStore()
const list = ref([])

onMounted(async () => {
  try {
    const res = await userApi.getMyPosts()
    if (res.code === 200) list.value = res.data.list
  } catch (e) {}
})

const goBack = () => uni.navigateBack()
const goToDetail = (post) => uni.navigateTo({ url: `/pages/post/detail?id=${post.id}` })
const formatDate = (str) => { if(!str) return ''; const d = new Date(str); return `${d.getMonth()+1}月${d.getDate()}日`; }
const previewImage = (urls, current) => uni.previewImage({ urls, current })

</script>

<style scoped>
.container { min-height: 100vh; background: #F5F5F5; }

/* 头部 */
.custom-header { position: fixed; top: 0; width: 100%; z-index: 100; background: #fff; box-shadow: 0 2rpx 10rpx rgba(0,0,0,0.05); }
.status-bar { height: var(--status-bar-height); }
.nav-bar { height: 88rpx; display: flex; align-items: center; padding: 0 30rpx; }
.back-arrow { font-size: 40rpx; font-weight: bold; padding: 10rpx; margin-left: -10rpx; }
.page-title { flex: 1; text-align: center; font-size: 34rpx; font-weight: bold; margin-right: 40rpx; }

/* 列表容器 */
.list-wrapper { padding: 30rpx; padding-top: calc(var(--status-bar-height) + 118rpx); }

/* 卡片样式 (仿首页) */
.post-card { background: #fff; border-radius: 20rpx; padding: 30rpx; margin-bottom: 24rpx; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.03); }

.card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20rpx; }
.user-info { display: flex; align-items: center; }
.avatar-img { width: 80rpx; height: 80rpx; border-radius: 50%; margin-right: 20rpx; background: #f0f0f0; }
.user-name { font-size: 30rpx; font-weight: bold; color: #333; display: block; }
.post-time { font-size: 24rpx; color: #999; margin-top: 4rpx; display: block; }
.more-icon { color: #ccc; font-weight: bold; letter-spacing: 2rpx; }

.text-body { font-size: 30rpx; color: #333; line-height: 1.6; margin-bottom: 20rpx; display: block; }

.image-grid { display: flex; flex-wrap: wrap; gap: 10rpx; margin-bottom: 20rpx; }
.post-img { width: 200rpx; height: 200rpx; border-radius: 12rpx; background: #f5f5f5; }

.card-footer { display: flex; justify-content: space-between; padding-top: 20rpx; border-top: 1rpx solid #f9f9f9; }
.action-item { display: flex; align-items: center; color: #666; font-size: 26rpx; }
.icon { margin-right: 8rpx; font-size: 30rpx; }
.empty { text-align: center; color: #ccc; margin-top: 100rpx; }
</style>