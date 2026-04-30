<template>
  <view class="message-page">
    <!-- 顶部标题 -->
    <view class="header">
      <text class="title">消息</text>
    </view>

    <!-- Tab 切换 -->
    <view class="tab-bar">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'chat' }"
        @click="switchTab('chat')"
      >
        <text>私信</text>
        <view v-if="unreadChat > 0" class="badge">{{ unreadChat }}</view>
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'notification' }"
        @click="switchTab('notification')"
      >
        <text>赞评</text>
        <view v-if="unreadNotify > 0" class="badge">{{ unreadNotify }}</view>
      </view>
    </view>

    <!-- 私信列表 -->
    <view v-if="currentTab === 'chat'" class="message-list">
      <view 
        v-for="item in chatList" 
        :key="item.id" 
        class="message-card"
        @click="goToChat(item)"
      >
        <image class="avatar" :src="item.avatar" mode="aspectFill"></image>
        <view class="message-info">
          <text class="username">{{ item.username }}</text>
          <text class="content">{{ item.lastMessage }}</text>
        </view>
        <!-- 右侧：时间在上，未读在下 -->
        <view class="message-right">
          <text class="time">{{ item.time }}</text>
          <view v-if="item.unread > 0" class="unread-dot">{{ item.unread }}</view>
        </view>
      </view>
      
      <view v-if="chatList.length === 0" class="empty-tip">
        暂无私信消息
      </view>
    </view>

    <!-- 赞评列表 -->
    <view v-if="currentTab === 'notification'" class="message-list">
      <view 
        v-for="item in notifyList" 
        :key="item.id" 
        class="notify-card"
        :class="{ unread: !item.isRead }"
        @click="goToPostDetail(item)"
      >
        <!-- 未读小红点 -->
        <view v-if="!item.isRead" class="notify-unread-dot"></view>
        
        <image class="avatar" :src="item.avatar" mode="aspectFill"></image>
        <view class="notify-info">
          <view class="info-header">
            <text class="username">{{ item.username }}</text>
            <text class="notify-type">{{ item.typeText }} {{ item.time }}</text>
          </view>
          
          <!-- 评论内容（如果是评论类型） -->
          <text v-if="(item.type === 'comment' || item.type === 'reply') && item.commentContent" class="comment-text">
            {{ item.commentContent }}
          </text>
          
          <!-- 引用的帖子/评论内容 -->
          <view v-if="item.quote" class="quote-content">
            <text class="quote-label">{{ item.quoteLabel || '原文' }}：</text>
            <text>{{ item.quote }}</text>
          </view>
          
          <!-- 只有评论类型才显示回复按钮 -->
          <view 
            v-if="item.type === 'comment' || item.type === 'reply'" 
            class="reply-btn" 
            @click.stop="handleReply(item)"
          >
            回复
          </view>
        </view>
        
        <!-- 右侧指示箭头 -->
        <text class="arrow-icon">›</text>
      </view>
      
      <view v-if="notifyList.length === 0" class="empty-tip">
        暂无赞评消息
      </view>
    </view>

    <!-- 底部占位 -->
    <view class="bottom-space"></view>

    <!-- 底部导航栏 -->
    <TabBar 
      :current-tab="'message'"
      @tab-change="handleTabChange"
    />
  </view>
</template>

<script>
import TabBar from '@/components/TabBar.vue';
import { getChatList, getNotifyList, markChatAsRead, markNotifyAsRead } from '../../api/message.js';

export default {
  components: {
    TabBar
  },
  data() {
    return {
      currentTab: 'chat',
      chatList: [],
      notifyList: [],
      unreadChat: 0,
      unreadNotify: 0
    };
  },
  
  onLoad() {
    this.initData();
  },
  
  onShow() {
    // 每次显示页面时刷新数据
    this.fetchChatList();
    this.fetchNotifyList();
  },
  
  methods: {
    async initData() {
      await Promise.all([
        this.fetchChatList(),
        this.fetchNotifyList()
      ]);
    },
    
    async fetchChatList() {  
      try {  
        const res = await getChatList();  
        if (res.code === 200) {  
          // 按用户ID去重，保留每个用户的最新对话  
          const uniqueChats = new Map()  
          res.data.list.forEach(chat => {  
            if (!uniqueChats.has(chat.userId)) {  
              uniqueChats.set(chat.userId, chat)  
            }  
          })  
          this.chatList = Array.from(uniqueChats.values())  
          this.unreadChat = res.data.unreadCount;  
        }  
      } catch (error) {  
        console.error('获取私信列表失败:', error);  
      }  
    },
    
    async fetchNotifyList() {
      try {
        const res = await getNotifyList();
        if (res.code === 200) {
          this.notifyList = res.data.list;
          this.unreadNotify = res.data.unreadCount;
        }
      } catch (error) {
        console.error('获取赞评列表失败:', error);
      }
    },
    
    // 更新未读数
    updateUnreadCounts() {
      this.unreadChat = this.chatList.reduce((sum, item) => sum + (item.unread || 0), 0);
      this.unreadNotify = this.notifyList.filter(item => !item.isRead).length;
    },
    
    switchTab(tab) {
      this.currentTab = tab;
    },
    
    // 进入聊天并标记已读
    async goToChat(item) {
      // 立即更新本地状态
      if (item.unread > 0) {
        item.unread = 0;
        this.updateUnreadCounts();
        
        // 调用API标记已读
        try {
          await markChatAsRead(item.id);
        } catch (error) {
          console.error('标记已读失败:', error);
        }
      }
      
      console.log('进入聊天:', item.username);
      // 跳转到聊天页面
      uni.navigateTo({ url: `/pages/chat/detail?userId=${item.userId}` });
    },
    
    // 跳转到帖子详情并标记已读
    async goToPostDetail(item) {
      // 立即更新本地状态
      if (!item.isRead) {
        item.isRead = true;
        this.updateUnreadCounts();
        
        // 调用API标记已读
        try {
          await markNotifyAsRead(item.id);
        } catch (error) {
          console.error('标记已读失败:', error);
        }
      }
      
      if (item.postId) {
        uni.navigateTo({ 
          url: `/pages/post/detail?id=${item.postId}` 
        });
      }
    },
    
    // 回复评论并标记已读
    async handleReply(item) {
      // 立即更新本地状态
      if (!item.isRead) {
        item.isRead = true;
        this.updateUnreadCounts();
        
        // 调用API标记已读
        try {
          await markNotifyAsRead(item.id);
        } catch (error) {
          console.error('标记已读失败:', error);
        }
      }
      
      // 跳转到帖子详情页，并传递需要回复的评论信息
      if (item.postId) {
        const params = {
          id: item.postId
        };
        
        // 如果是回复评论，传递评论ID
        if (item.commentId) {
          params.replyCommentId = item.commentId;
          params.replyUsername = item.username;
        }
        
        const queryString = Object.keys(params)
          .map(key => `${key}=${encodeURIComponent(params[key])}`)
          .join('&');
        
        uni.navigateTo({ 
          url: `/pages/post/detail?${queryString}` 
        });
      }
    },
    
    handleTabChange(tabId) {
      if (tabId !== 'message') {
        console.log('切换到:', tabId);
      }
    }
  }
};
</script>

<style scoped>
.message-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 150rpx;
}

/* 顶部标题 */
.header {
  padding: 30rpx;
  background: linear-gradient(135deg, #e8f5e9 0%, #f1f8e9 100%);
}

.title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

/* Tab 切换栏 */
.tab-bar {
  display: flex;
  background-color: transparent;
  padding: 20rpx 30rpx;
  border-bottom: 1rpx solid #eee;
}

.tab-item {
  position: relative;
  margin-right: 60rpx;
  padding: 10rpx 0;
  font-size: 30rpx;
  color: #666;
}

.tab-item.active {
  color: #4CAF50;
  font-weight: bold;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40rpx;
  height: 6rpx;
  background-color: #4CAF50;
  border-radius: 3rpx;
}

.badge {
  position: absolute;
  top: -10rpx;
  right: -30rpx;
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 8rpx;
  background-color: #ff4d4f;
  color: #fff;
  font-size: 20rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 消息列表 */
.message-list {
  padding: 20rpx 30rpx;
}

/* 私信卡片 */
.message-card {
  display: flex;
  align-items: center;
  padding: 30rpx;
  background-color: #fff;
  border-radius: 20rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  background-color: #e8f5e9;
  flex-shrink: 0;
}

.message-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.username {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 10rpx;
}

.message-info .content {
  font-size: 26rpx;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 右侧区域：时间在上，未读在下 */
.message-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  height: 100rpx;
  margin-left: 20rpx;
  flex-shrink: 0;
}

.message-right .time {
  font-size: 24rpx;
  color: #999;
}

.unread-dot {
  min-width: 36rpx;
  height: 36rpx;
  padding: 0 10rpx;
  background-color: #ff4d4f;
  color: #fff;
  font-size: 22rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 赞评卡片 */
.notify-card {
  display: flex;
  padding: 30rpx;
  padding-left: 40rpx;
  background-color: #fff;
  border-radius: 20rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
  position: relative;
}

/* 未读状态的卡片 */
.notify-card.unread {
  background-color: #fafffe;
}

/* 赞评未读小红点 */
.notify-unread-dot {
  position: absolute;
  top: 50%;
  left: 15rpx;
  transform: translateY(-50%);
  width: 16rpx;
  height: 16rpx;
  background-color: #ff4d4f;
  border-radius: 50%;
}

.notify-info {
  flex: 1;
  overflow: hidden;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}

.notify-type {
  font-size: 24rpx;
  color: #999;
}

/* 评论内容 */
.comment-text {
  font-size: 28rpx;
  color: #333;
  line-height: 1.5;
  margin-bottom: 15rpx;
  display: block;
}

/* 引用内容 */
.quote-content {
  margin-top: 10rpx;
  padding: 20rpx;
  background-color: #f5f5f5;
  border-radius: 10rpx;
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.quote-label {
  color: #999;
  font-size: 24rpx;
}

/* 回复按钮 */
.reply-btn {
  display: inline-block;
  margin-top: 15rpx;
  padding: 10rpx 30rpx;
  background-color: #e8f5e9;
  color: #4CAF50;
  font-size: 24rpx;
  border-radius: 30rpx;
}

/* 右侧箭头 */
.arrow-icon {
  color: #ccc;
  font-size: 32rpx;
  margin-left: 10rpx;
  align-self: center;
}

/* 空状态 */
.empty-tip {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 28rpx;
}

.bottom-space {
  height: 20rpx;
}
</style>