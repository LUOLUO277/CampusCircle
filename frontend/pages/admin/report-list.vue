<template>
  <view class="admin-container">
    <!-- 1. 自定义头部 (深色商务风) -->
    <view class="custom-header admin-theme">
      <!-- 状态栏占位 -->
      <view class="status-bar"></view>
      <!-- 导航栏内容 -->
      <view class="nav-bar">
        <view class="back-btn" @click="goBack">
          <text class="back-arrow">←</text>
        </view>
        <text class="page-title">内容审核工作台</text>
        <view class="right-placeholder"></view>
      </view>
    </view>

    <!-- 2. 列表区域 -->
    <scroll-view scroll-y class="list-wrapper">
      <view v-if="loading" class="loading-state">数据加载中...</view>
      
      <view v-else-if="list.length === 0" class="empty">
        <text>🎉 暂无待审核内容</text>
      </view>

      <view v-else v-for="item in list" :key="item.id" class="report-card">
        
        <!-- 举报基本信息 -->
        <view class="card-header">
          <view class="reason-tag">{{ item.reason }}</view>
          <text class="time">{{ formatDate(item.createTime) }}</text>
        </view>
        
        <!-- 举报详细描述 -->
        <view class="report-desc">
          <text class="label">举报描述：</text>
          <text class="value">{{ item.description || '无详细描述' }}</text>
        </view>
        
        <!-- 被举报的内容快照 (灰色背景区域) -->
        <view class="target-snapshot" v-if="item.targetSnapshot">
          <view class="snapshot-header">
            <text class="target-type" :class="item.targetType">
              {{ item.targetType === 'POST' ? '帖子' : '评论' }}
            </text>
            <text class="author">发布者: {{ item.targetSnapshot.author?.nickname || '未知' }}</text>
          </view>
          <text class="content-preview">{{ item.targetSnapshot.content || '（纯图片或无文本内容）' }}</text>
          <!-- 如果有图片，简单展示第一张 -->
          <view v-if="item.targetSnapshot.images && item.targetSnapshot.images.length" class="img-preview">
             <image :src="item.targetSnapshot.images[0]" mode="aspectFill" class="mini-img"></image>
          </view>
        </view>
        
        <!-- 3. 操作按钮组 -->
        <view class="action-bar">
          <button class="btn reject" @click="handleProcess(item, 'REJECT_REPORT')">
            驳回举报
          </button>
          <view class="danger-zone">
            <button class="btn delete" @click="handleProcess(item, 'DELETE_POST')">
              删内容
            </button>
            <button class="btn ban" @click="handleProcess(item, 'BAN_USER')">
              封号
            </button>
          </view>
        </view>
      </view>
      
      <!-- 底部占位 -->
      <view style="height: 40rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
// 注意：这里导入的是 adminApi
import { adminApi } from '@/api/user' 

const list = ref([])
const loading = ref(false)

onMounted(() => {
  loadData()
})

const goBack = () => {
  uni.navigateBack()
}

const loadData = async () => {
  try {
    loading.value = true
    const res = await adminApi.getReports(0)
    if (res.code === 200) {
      list.value = res.data.list
    } else {
      uni.showToast({ title: res.message || '加载失败', icon: 'none' })
    }
  } catch (e) {
    console.error('加载举报列表失败:', e)
    uni.showToast({ title: '网络请求错误', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 处理举报的核心逻辑
const handleProcess = (item, action) => {
  let actionText = ''
  if (action === 'REJECT_REPORT') actionText = '确定驳回举报？(认为内容无违规)'
  if (action === 'DELETE_POST') actionText = '确定删除该内容？(不可恢复)'
  if (action === 'BAN_USER') actionText = '确定封禁该用户？(慎重)'

  // 弹窗输入处理备注
  uni.showModal({
    title: '审核处理',
    content: actionText,
    editable: true, // 允许输入备注
    placeholderText: '请输入处理理由（选填）',
    success: async (res) => {
      if (res.confirm) {
        const note = res.content || '管理员后台操作'
        try {
          uni.showLoading({ title: '提交中...' })
          await adminApi.processReport(item.id, action, note)
          
          uni.showToast({ title: '处理完成', icon: 'success' })
          
          // 前端直接移除该条目，无需刷新整个列表
          list.value = list.value.filter(i => i.id !== item.id)
        } catch (error) {
          uni.showToast({ title: error.message || '操作失败', icon: 'none' })
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}

const formatDate = (ts) => {
  if (!ts) return '';
  try {
    const d = new Date(ts);
    return `${d.getMonth() + 1}-${d.getDate()} ${d.getHours()}:${d.getMinutes().toString().padStart(2, '0')}`;
  } catch(e) { return ts }
}
</script>

<style scoped>
.admin-container { 
  min-height: 100vh; 
  background: #f0f2f5; 
  /* 避开自定义头部 */
  padding-top: calc(var(--status-bar-height) + 88rpx);
}

/* 头部样式 */
.custom-header { 
  position: fixed; top: 0; left: 0; width: 100%; z-index: 100; 
}
.admin-theme { 
  background: #263238; /* 深色背景 */
  color: #fff;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.15);
}
.status-bar { height: var(--status-bar-height); }
.nav-bar { 
  height: 88rpx; 
  display: flex; 
  align-items: center; 
  justify-content: space-between; /* 左右对齐 */
  padding: 0 30rpx; 
}
.back-btn { padding: 10rpx 20rpx 10rpx 0; }
.back-arrow { font-size: 40rpx; color: #fff; font-weight: bold; }
.page-title { font-size: 34rpx; font-weight: bold; letter-spacing: 2rpx; }
.right-placeholder { width: 40rpx; }

/* 列表容器 */
.list-wrapper { 
  padding: 30rpx; 
  box-sizing: border-box;
  height: calc(100vh - var(--status-bar-height) - 88rpx);
}

/* 举报卡片 */
.report-card { 
  background: #fff; 
  border-radius: 16rpx; 
  padding: 30rpx; 
  margin-bottom: 30rpx; 
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03); 
}

.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx; }
.reason-tag { 
  background: #FFF1F0; 
  color: #F5222D; 
  padding: 6rpx 16rpx; 
  border-radius: 8rpx; 
  font-size: 24rpx; 
  font-weight: bold; 
  border: 1rpx solid #FFA39E; 
}
.time { color: #aaa; font-size: 24rpx; }

.report-desc { margin-bottom: 24rpx; font-size: 28rpx; color: #555; background: #fafafa; padding: 16rpx; border-radius: 8rpx;}
.label { font-weight: bold; color: #333; margin-right: 10rpx;}

/* 快照区域 */
.target-snapshot { 
  background: #F8F9FA; 
  padding: 24rpx; 
  border-radius: 12rpx; 
  margin-bottom: 30rpx; 
  border-left: 8rpx solid #546E7A; /* 引用风格边框 */
}
.snapshot-header { display: flex; justify-content: space-between; margin-bottom: 16rpx; font-size: 24rpx; color: #999; }
.target-type { padding: 2rpx 8rpx; border-radius: 4rpx; color: #fff; font-size: 20rpx; margin-right: 10rpx;}
.target-type.POST { background: #1890FF; }
.target-type.COMMENT { background: #722ED1; }

.content-preview { 
  font-size: 30rpx; 
  color: #262626; 
  line-height: 1.6; 
  font-weight: 500; 
  display: block;
}
.img-preview { margin-top: 10rpx; }
.mini-img { width: 120rpx; height: 120rpx; border-radius: 8rpx; background: #eee; }

/* 按钮组 */
.action-bar { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  border-top: 1rpx solid #f0f0f0; 
  padding-top: 24rpx; 
}
.danger-zone { display: flex; gap: 20rpx; }

.btn { 
  margin: 0; 
  font-size: 26rpx; 
  height: 64rpx; 
  line-height: 64rpx; 
  border-radius: 32rpx; 
  padding: 0 32rpx; 
}
.btn::after { border: none; }

.btn.reject { background: #f5f5f5; color: #666; }
.btn.delete { background: #FFF7E6; color: #FA8C16; }
.btn.ban { background: #FFF1F0; color: #F5222D; }

.empty, .loading-state { text-align: center; margin-top: 100rpx; color: #999; font-size: 28rpx; }
</style>