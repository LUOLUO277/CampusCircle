<template>  
  <view class="chat-page">  
    <!-- 顶部导航 -->  
    <view class="nav-header">  
      <text class="back-btn" @click="goBack">＜</text>  
      <view class="user-info">  
        <text class="username">{{ chatUser.username }}</text>  
        <text v-if="chatUser.isOnline" class="online-status">在线</text>  
      </view>  
      <text class="more-btn" @click="showChatMenu">⋮</text>  
    </view>  
  
<!-- 聊天消息列表 -->
<scroll-view
  class="message-list"
  scroll-y
  :scroll-top="scrollTop"
  :scroll-with-animation="true"
  @scrolltoupper="loadMoreMessages"
>
  <!-- 加载更多 -->
  <view v-if="hasMore" class="load-more" @click="loadMoreMessages">
    <text>{{ loading ? '加载中...' : '点击加载更多' }}</text>
  </view>

  <!-- 消息列表 -->
  <view
    v-for="(msg, index) in messageList"
    :key="msg.id"
    class="message-item"
  >
    <!-- 时间分割线 -->
    <view v-if="showTimeHeader(index)" class="time-divider">
      <text>{{ formatTime(msg.time) }}</text>
    </view>

    <!-- 对方消息 -->
    <view v-if="msg.senderId !== currentUserId" class="message-row other">
      <image
        class="avatar"
        :src="getFullImageUrl(chatUser.avatar)"
        mode="aspectFill"
      />
      <view class="message-bubble other">
        <text v-if="msg.type === 'text'">{{ msg.content }}</text>
        <image
          v-else
          class="message-image"
          :src="getFullImageUrl(msg.content)"
          @click="previewImage(getFullImageUrl(msg.content))"
        />
      </view>
    </view>

<!-- 自己消息 -->
<view v-else class="message-row self">
  <view class="message-bubble self">
    <view v-if="msg.status === 'sending'">⏳</view>
    <view v-else-if="msg.status === 'failed'" @click="resendMessage(msg)">⚠️</view>

    <text v-if="msg.type === 'text'">{{ msg.content }}</text>
    <image
      v-else
      class="message-image"
      :src="getFullImageUrl(msg.content)"
      @click="previewImage(getFullImageUrl(msg.content))"
    />
  </view>

  <!-- 改为安全写法 -->
  <image
    class="avatar"
    :src="currentUserAvatar ? getFullImageUrl(currentUserAvatar) : '/static/avatars/default.png'"
    mode="aspectFill"
  />
</view>
  </view>

  <view class="list-bottom"></view>
</scroll-view>
  
    <!-- 底部输入栏 -->  
    <view class="input-bar">  
      <view class="input-wrapper">  
        <input   
          class="message-input"   
          v-model="inputText"  
          placeholder="发送消息..."  
          :adjust-position="true"  
          confirm-type="send"  
          @confirm="sendTextMessage"  
          @focus="handleInputFocus"  
        />  
      </view>  
      <text class="emoji-btn" @click="toggleEmojiPanel">😊</text>  
      <text class="image-btn" @click="chooseImage">🖼</text>  
      <view   
        class="send-btn"   
        :class="{ active: inputText.trim() }"   
        @click="sendTextMessage"  
      >  
        发送  
      </view>  
    </view>  
  
    <!-- 表情面板 -->  
    <view v-if="showEmoji" class="emoji-panel">  
      <view class="emoji-grid">  
        <text   
          v-for="emoji in emojiList"   
          :key="emoji"   
          class="emoji-item"  
          @click="insertEmoji(emoji)"  
        >{{ emoji }}</text>  
      </view>  
    </view>  
  </view>  
</template>  
  
<script>  
import { getChatMessages, sendMessage, getChatUserInfo } from '../../api/message.js';  
import { useUserStore } from '@/stores/user';  
  
export default {  
  data() {  
    return {  
      userId: null,           // 对方用户ID  
      chatUser: {},           // 对方用户信息  
      currentUserId: null,    // 当前登录用户ID（动态获取）  
      currentUserAvatar: '/static/avatars/default.png',  // 当前用户头像  
        
      messageList: [],        // 消息列表  
      inputText: '',          // 输入框内容  
      scrollTop: 0,           // 滚动位置  
        
      loading: false,         // 加载状态  
      hasMore: true,          // 是否有更多消息  
      page: 1,                // 当前页码  
        
      showEmoji: false,       // 是否显示表情面板  
      emojiList: [  
        '😀', '😁', '😂', '🤣', '😃', '😄', '😅', '😆',  
        '😉', '😊', '😋', '😎', '😍', '😘', '🥰', '😗',  
        '😙', '😚', '🙂', '🤗', '🤩', '🤔', '🤨', '😐',  
        '😑', '😶', '🙄', '😏', '😣', '😥', '😮', '🤐',  
        '😯', '😪', '😫', '🥱', '😴', '😌', '😛', '😜',  
        '😝', '🤤', '😒', '😓', '😔', '😕', '🙃', '🤑',  
        '👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙',  
        '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '💔'  
      ]  
    };  
  },  
    
onLoad(options) {
  const userStore = useUserStore()
  this.currentUserId = userStore.userInfo?.id

  // 动态获取当前用户头像
  if (userStore.userInfo) {
    this.currentUserAvatar = userStore.userInfo.avatar || '/static/avatars/default.png'
    console.log('self avatar URL:', this.getFullImageUrl(this.currentUserAvatar))
  } else {
    // 监听 userInfo 更新
    userStore.$subscribe((mutation, state) => {
      if (state.userInfo) {
        this.currentUserAvatar = state.userInfo.avatar || '/static/avatars/default.png'
        console.log('self avatar URL updated:', this.getFullImageUrl(this.currentUserAvatar))
      }
    })
  }

  if (options.userId) {
    this.userId = Number(options.userId)
    this.initChat()
  }
},
    
  onUnload() {  
    // 页面卸载时可以做一些清理工作  
  },  
    
  methods: {  
    // 初始化聊天  
    async initChat() {  
      await this.fetchUserInfo();  
      await this.fetchMessages();  
      this.scrollToBottom();  
    },  
      
    // 获取对方用户信息  
    async fetchUserInfo() {  
      try {  
        const res = await getChatUserInfo(this.userId);  
        if (res.code === 200) {  
          this.chatUser = res.data;  
        }  
      } catch (error) {  
        console.error('获取用户信息失败:', error);  
      }  
    },  
        // 添加图片URL处理方法  
  getFullImageUrl(url) {
    if (!url) return '/static/avatars/default.png'
    if (url.startsWith('http')) return url
    return (url.startsWith('/') ? url : '/' + url)
  },  
    // 获取聊天记录  
    async fetchMessages() {  
      if (this.loading) return;  
        
      this.loading = true;  
      try {  
        const res = await getChatMessages(this.userId, this.page);  
        if (res.code === 200) {  
          let messages = res.data.list;  
            
          // 按时间排序（升序，最新的在下方）  
          messages.sort((a, b) => new Date(a.time) - new Date(b.time));  
            
          if (this.page === 1) {  
            this.messageList = messages;  
          } else {  
            // 加载更多时，将新消息插入到列表前面  
            this.messageList = [...messages, ...this.messageList];  
          }  
          this.hasMore = res.data.hasMore;  
        }  
      } catch (error) {  
        console.error('获取消息失败:', error);  
      } finally {  
        this.loading = false;  
      }  
    },  
      
    // 加载更多消息  
    async loadMoreMessages() {  
      if (this.loading || !this.hasMore) return;  
      this.page++;  
      await this.fetchMessages();  
    },  
      
    // 发送文本消息  
    async sendTextMessage() {  
      const content = this.inputText.trim();  
      if (!content) return;  
        
      // 创建临时消息  
      const tempMsg = {  
        id: Date.now(),  
        senderId: this.currentUserId,  
        receiverId: this.userId,  
        type: 'text',  
        content: content,  
        time: new Date().toISOString(),  
        status: 'sending'  
      };  
        
      // 添加到消息列表  
      this.messageList.push(tempMsg);  
      this.inputText = '';  
      this.showEmoji = false;  
      this.scrollToBottom();  
        
      // 发送消息到服务器  
      try {  
        const res = await sendMessage({  
          receiverId: this.userId,  
          type: 'text',  
          content: content  
        });  
          
        if (res.code === 200) {  
          // 更新消息状态为已发送  
          const index = this.messageList.findIndex(m => m.id === tempMsg.id);  
          if (index > -1) {  
            this.messageList[index].status = 'sent';  
            this.messageList[index].id = res.data.id;  
          }  
        } else {  
          // 发送失败  
          const index = this.messageList.findIndex(m => m.id === tempMsg.id);  
          if (index > -1) {  
            this.messageList[index].status = 'failed';  
          }  
        }  
      } catch (error) {  
        // 发送失败  
        const index = this.messageList.findIndex(m => m.id === tempMsg.id);  
        if (index > -1) {  
          this.messageList[index].status = 'failed';  
        }  
      }  
    },  
      
    // 重发消息  
    async resendMessage(msg) {  
      uni.showModal({  
        title: '提示',  
        content: '是否重新发送该消息？',  
        success: async (res) => {  
          if (res.confirm) {  
            msg.status = 'sending';  
              
            try {  
              const result = await sendMessage({  
                receiverId: this.userId,  
                type: msg.type,  
                content: msg.content  
              });  
                
              if (result.code === 200) {  
                msg.status = 'sent';  
                msg.id = result.data.id;  
              } else {  
                msg.status = 'failed';  
              }  
            } catch (error) {  
              msg.status = 'failed';  
            }  
          }  
        }  
      });  
    },  
      
    // 选择图片  
    chooseImage() {  
      uni.chooseImage({  
        count: 1,  
        sizeType: ['compressed'],  
        sourceType: ['album', 'camera'],  
        success: async (res) => {  
          const tempFilePath = res.tempFilePaths[0];  
            
          // 创建临时消息  
          const tempMsg = {  
            id: Date.now(),  
            senderId: this.currentUserId,  
            receiverId: this.userId,  
            type: 'image',  
            content: tempFilePath,  
            time: new Date().toISOString(),  
            status: 'sending'  
          };  
            
          this.messageList.push(tempMsg);  
          this.scrollToBottom();  
            
          // 模拟上传  
          setTimeout(() => {  
            const index = this.messageList.findIndex(m => m.id === tempMsg.id);  
            if (index > -1) {  
              this.messageList[index].status = 'sent';  
            }  
          }, 1500);  
        }  
      });  
    },  
      
    // 预览图片  
    previewImage(url) {  
      const imageUrls = this.messageList  
        .filter(m => m.type === 'image')  
        .map(m => m.content);  
        
      uni.previewImage({  
        current: url,  
        urls: imageUrls  
      });  
    },  
      
    // 插入表情  
    insertEmoji(emoji) {  
      this.inputText += emoji;  
    },  
      
    // 切换表情面板  
    toggleEmojiPanel() {  
      this.showEmoji = !this.showEmoji;  
    },  
      
    // 输入框获得焦点  
    handleInputFocus() {  
      this.showEmoji = false;  
      this.scrollToBottom();  
    },  
      
    // 滚动到底部  
    scrollToBottom() {  
      this.$nextTick(() => {  
        this.scrollTop = 999999;  
      });  
    },  
      
    // 是否显示时间头部  
    showTimeHeader(index) {  
      if (index === 0) return true;  
        
      const current = new Date(this.messageList[index].time);  
      const prev = new Date(this.messageList[index - 1].time);  
        
      // 间隔超过5分钟显示时间  
      return (current - prev) > 5 * 60 * 1000;  
    },  
      
    // 格式化时间  
    formatTime(timeStr) {  
      const date = new Date(timeStr);  
      const now = new Date();  
      const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());  
      const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000);  
        
      const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;  
        
      if (date >= today) {  
        return time;  
      } else if (date >= yesterday) {  
        return `昨天 ${time}`;  
      } else {  
        return `${date.getMonth() + 1}月${date.getDate()}日 ${time}`;  
      }  
    },  
      
    // 显示聊天菜单  
    showChatMenu() {  
      uni.showActionSheet({  
        itemList: ['查看资料', '清空聊天记录', '举报'],  
        success: (res) => {  
          if (res.tapIndex === 0) {  
            this.viewProfile();  
          } else if (res.tapIndex === 1) {  
            this.clearMessages();  
          } else if (res.tapIndex === 2) {  
            this.reportUser();  
          }  
        }  
      });  
    },  
      
    // 查看资料  
    viewProfile() {  
      uni.navigateTo({  
        url: `/pages/user/home?id=${this.userId}`  
      });  
    },  
      
    // 清空聊天记录  
    clearMessages() {  
      uni.showModal({  
        title: '提示',  
        content: '确定要清空聊天记录吗？',  
        success: (res) => {  
          if (res.confirm) {  
            this.messageList = [];  
            uni.showToast({ title: '已清空', icon: 'none' });  
          }  
        }  
      });  
    },  
      
    // 举报用户  
    reportUser() {  
      uni.showActionSheet({  
        itemList: ['垃圾广告', '骚扰信息', '欺诈行为', '其他'],  
        success: (res) => {  
          uni.showToast({  
            title: '举报成功',  
            icon: 'none'  
          });  
        }  
      });  
    },  
      
    // 返回  
    goBack() {  
      uni.navigateBack();  
    }  
  }  
};  
</script>  
  
<style scoped>  
.chat-page {  
  display: flex;  
  flex-direction: column;  
  height: 100vh;  
  background-color: #f5f5f5;  
}  
  
/* 顶部导航 */  
.nav-header {  
  display: flex;  
  align-items: center;  
  justify-content: space-between;  
  padding: 20rpx 30rpx;  
  background-color: #fff;  
  border-bottom: 1rpx solid #eee;  
  flex-shrink: 0;  
}  
  
.back-btn {  
  font-size: 36rpx;  
  color: #333;  
  padding: 10rpx;  
}  
  
.user-info {  
  display: flex;  
  flex-direction: column;  
  align-items: center;  
}  
  
.username {  
  font-size: 32rpx;  
  font-weight: bold;  
  color: #333;  
}  
  
.online-status {  
  font-size: 22rpx;  
  color: #4CAF50;  
  margin-top: 4rpx;  
}  
  
.more-btn {  
  font-size: 36rpx;  
  color: #666;  
  padding: 10rpx;  
}  
  
/* 消息列表 */  
.message-list {  
  flex: 1;  
  padding: 20rpx 30rpx;  
  overflow-y: auto;  
}  
  
.load-more {  
  text-align: center;  
  padding: 20rpx;  
  color: #999;  
  font-size: 24rpx;  
}  
  
/* 时间分割线 */  
.time-divider {  
  text-align: center;  
  padding: 20rpx 0;  
}  
  
.time-divider text {  
  font-size: 22rpx;  
  color: #999;  
  background-color: #e8e8e8;  
  padding: 6rpx 20rpx;  
  border-radius: 20rpx;  
}  
  
/* 消息行 */  
.message-row {  
  display: flex;  
  align-items: flex-start;  
  margin-bottom: 30rpx;  
}  
  
.message-row.other {  
  justify-content: flex-start;  
}  
  
.message-row.self {  
  justify-content: flex-end;  
}  
  
.avatar {  
  width: 80rpx;  
  height: 80rpx;  
  border-radius: 50%;  
  background-color: #e8f5e9;  
  flex-shrink: 0;  
}  
  
/* 消息气泡 */  
.message-bubble {  
  max-width: 65%;  
  padding: 20rpx 28rpx;  
  border-radius: 20rpx;  
  position: relative;  
  display: flex;  
  align-items: center;  
}  
  
.message-bubble.other {  
  background-color: #fff;  
  margin-left: 20rpx;  
  border-top-left-radius: 6rpx;  
}  
  
.message-bubble.self {  
  background-color: #95EC69;  
  margin-right: 20rpx;  
  border-top-right-radius: 6rpx;  
}  
  
.message-text {  
  font-size: 30rpx;  
  color: #333;  
  line-height: 1.5;  
  word-break: break-all;  
}  
  
.message-image {  
  max-width: 100%;  
  min-width: 200rpx;  
  border-radius: 10rpx;  
}  
  
/* 发送状态图标 */  
.sending-icon,  
.failed-icon {  
  font-size: 28rpx;  
  margin-right: 10rpx;  
}  
  
.failed-icon {  
  color: #ff4d4f;  
}  
  
.list-bottom {  
  height: 20rpx;  
}  
  
/* 底部输入栏 */  
.input-bar {  
  display: flex;  
  align-items: center;  
  padding: 20rpx 30rpx;  
  background-color: #fff;  
  border-top: 1rpx solid #eee;  
  flex-shrink: 0;  
}  
  
.input-wrapper {  
  flex: 1;  
  background-color: #f5f5f5;  
  border-radius: 36rpx;  
  padding: 0 30rpx;  
}  
  
.message-input {  
  height: 72rpx;  
  font-size: 28rpx;  
}  
  
.emoji-btn,  
.image-btn {  
  font-size: 48rpx;  
  margin-left: 20rpx;  
}  
  
.send-btn {  
  margin-left: 20rpx;  
  padding: 16rpx 32rpx;  
  background-color: #e0e0e0;  
  color: #999;  
  font-size: 28rpx;  
  border-radius: 36rpx;  
}  
  
.send-btn.active {  
  background-color: #4CAF50;  
  color: #fff;  
}  
  
/* 表情面板 */  
.emoji-panel {  
  background-color: #fff;  
  border-top: 1rpx solid #eee;  
  padding: 20rpx;  
  flex-shrink: 0;  
}  
  
.emoji-grid {  
  display: flex;  
  flex-wrap: wrap;  
}  
  
.emoji-item {  
  width: 12.5%;  
  text-align: center;  
  font-size: 48rpx;  
  padding: 15rpx 0;  
}  
</style>