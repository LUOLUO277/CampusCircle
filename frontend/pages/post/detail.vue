<template>
  <view class="post-detail-page">
    <!-- 顶部导航 -->
    <view class="nav-header">
      <text class="back-btn" @click="goBack">＜</text>
      <text class="nav-title">帖子详情</text>
      <text class="nav-more" @click="showPostMenu">⋮</text>
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="loading-tip">加载中...</view>

    <!-- 滚动内容区 -->
    <scroll-view v-else class="scroll-content" scroll-y>
      <!-- 帖子主体 -->
      <view class="post-card">
        <!-- 用户信息 -->
        <view class="post-header">
          <view class="user-info" @click="goToUser">
            <!-- ✅ 修复：使用 getFullImageUrl 处理头像 -->
            <image 
              class="avatar" 
              :src="getFullImageUrl(post.userAvatar)" 
              mode="aspectFill"
            ></image>
            <view class="user-meta">
              <view class="user-name-row">
                <text class="username">{{ post.userName }}</text>
              </view>
              <text class="post-time">{{ post.time }}</text>
            </view>
          </view>
          <text class="more-btn" @click="showPostMenu">⋯</text>
        </view>

        <!-- 帖子内容 -->
        <view class="post-content">
          <text class="content-text">{{ post.content }}</text>
        </view>

        <!-- 帖子图片 -->
        <view v-if="post.images && post.images.length > 0" class="post-images">
          <image 
            v-for="(img, index) in post.images" 
            :key="index"
            class="post-image"
            :src="getFullImageUrl(img)" 
            mode="aspectFill"
            @click="previewImage(index)"
          ></image>
        </view>

        <!-- 投票 -->
        <view v-if="post.vote && post.vote.options && post.vote.options.length" class="vote-card">
          <view class="vote-header">
            <text class="vote-title">投票</text>
            <text class="vote-subtitle">{{ post.vote.totalVotes || 0 }} 人参与</text>
          </view>
          <view class="vote-options">
            <view
              v-for="opt in post.vote.options"
              :key="opt.id"
              class="vote-option"
              :class="{ selected: post.vote.myOptionId === opt.id }"
              @click="handleVote(opt)"
            >
              <view class="vote-option-row">
                <text class="vote-option-text">{{ opt.text }}</text>
                <text class="vote-option-count">{{ opt.count || 0 }}票</text>
              </view>
              <view class="vote-bar">
                <view class="vote-bar-fill" :style="{ width: (opt.percent || 0) + '%' }"></view>
              </view>
            </view>
          </view>
          <text v-if="post.vote.myOptionId" class="vote-hint">已投票</text>
          <text v-else class="vote-hint">点击选项投票</text>
        </view>

        <!-- 操作栏 -->
        <view class="action-bar">
          <view class="action-item" @click="handleCollect">
            <text class="action-icon" :class="{ active: isCollected }">{{ isCollected ? '★' : '☆' }}</text>
            <text class="action-text">{{ post.collectCount > 0 ? post.collectCount : '收藏' }}</text>
          </view>
          
          <view class="action-item" @click="handleLike">
            <text class="action-icon heart-icon" :class="{ active: isLiked }">
              {{ isLiked ? '❤️' : '🤍' }}
            </text>
            <text class="action-text">{{ post.likes > 0 ? post.likes : '赞' }}</text>
          </view>
        </view>
      </view>

      <!-- 评论区 -->
      <view class="comment-section">
        <!-- 评论头部 -->
        <view class="comment-header">
          <text class="comment-title">全部评论·{{ comments.length }}</text>
          <view class="sort-btn">
            <text>常规</text>
            <text class="sort-arrow">﹀</text>
          </view>
        </view>

        <!-- 评论列表 -->
        <view class="comment-list">
          <view 
            v-for="comment in comments" 
            :key="comment.id" 
            class="comment-item"
          >
            <!-- 主评论 -->
            <view class="comment-main">
              <image 
                class="comment-avatar" 
                :src="getFullImageUrl(comment.avatar)" 
                mode="aspectFill"
                @click="goToUserProfile(comment.userId)"
              ></image>
              <view class="comment-body">
                <view class="comment-user">
                  <text class="comment-username">{{ comment.username }}</text>
                  <text v-if="comment.isAuthor" class="author-tag">作者</text>
                </view>
                <text class="comment-content" @click="replyToComment(comment)">
                  {{ comment.content }}
                </text>
                <view class="comment-footer">
                  <text class="comment-time">{{ comment.time }}</text>
                  <view class="comment-actions">
                    <view class="comment-action" @click="handleLikeComment(comment)">
                      <text class="comment-like-icon" :class="{ liked: comment.isLiked }">
                        {{ comment.isLiked ? '❤️' : '🤍' }}
                      </text>
                      <text class="comment-like-text">
                        {{ comment.likes > 0 ? comment.likes : '点赞' }}
                      </text>
                    </view>
                    <text class="comment-more" @click="showCommentMenu(comment)">⋯</text>
                  </view>
                </view>

                <!-- 子评论/回复 -->
                <view v-if="comment.replies && comment.replies.length > 0" class="reply-list">
                  <view 
                    v-for="reply in comment.replies" 
                    :key="reply.id" 
                    class="reply-item"
                  >
                    <image 
                      class="reply-avatar" 
                      :src="getFullImageUrl(reply.avatar)" 
                      mode="aspectFill"
                      @click="goToUserProfile(reply.userId)"
                    ></image>
                    <view class="reply-body">
                      <view class="reply-user">
                        <text class="reply-username">{{ reply.username }}</text>
                        <text v-if="reply.isAuthor" class="author-tag">作者</text>
                        <text v-if="reply.isOP" class="op-tag">层主</text>
                        <text class="reply-time">{{ reply.time }}</text>
                        <text class="reply-like" @click="handleLikeReply(reply)">
                          <text class="reply-like-icon" :class="{ liked: reply.isLiked }">
                            {{ reply.isLiked ? '❤️' : '🤍' }}
                          </text>
                          <text v-if="reply.likes > 0">{{ reply.likes }}</text>
                        </text>
                        <text class="reply-more" @click="showReplyMenu(reply, comment)">⋯</text>
                      </view>
                      <text class="reply-content" @click="replyToReply(reply, comment)">
                        <text v-if="reply.replyTo" class="reply-to">@{{ reply.replyTo }}：</text>
                        {{ reply.content }}
                      </text>
                    </view>
                  </view>
                </view>
              </view>
            </view>
          </view>

          <!-- 空评论 -->
          <view v-if="comments.length === 0" class="empty-comment">
            暂无评论，快来抢沙发吧~
          </view>
        </view>
      </view>

      <!-- 底部占位 -->
      <view class="bottom-space"></view>
    </scroll-view>

    <!-- 底部评论输入栏 -->
    <view class="comment-input-bar">
      <text class="home-icon" @click="goHome">🏠</text>
      <view class="input-wrapper">
        <input 
          class="comment-input" 
          v-model="commentText"
          :placeholder="replyPlaceholder"
          :adjust-position="true"
          @focus="handleInputFocus"
          @blur="handleInputBlur"
        />
        <text class="emoji-btn" @click="toggleEmojiPanel">😊</text>
      </view>
      <text class="image-btn">🖼</text>
      <view class="send-btn" :class="{ active: commentText.trim() }" @click="handleSendComment">
        发送
      </view>
    </view>
	<!-- 表情面板 -->
	<view v-if="showEmojiPanel" class="emoji-panel">
	  <scroll-view class="emoji-content" scroll-y>
	    <view class="emoji-grid">
	      <text 
	        v-for="(emoji, index) in emojiList" 
	        :key="index"
	        class="emoji-item"
	        @click="insertEmoji(emoji)"
	      >
	        {{ emoji }}
	      </text>
	    </view>
	  </scroll-view>
	</view>

    <!-- 取消回复按钮 -->
    <view v-if="replyTarget" class="reply-bar">
      <text class="reply-hint">回复 @{{ replyTarget.username }}</text>
      <text class="cancel-reply" @click="cancelReply">✕</text>
    </view>
  </view>
</template>

<script>
import { getPostDetail, getPostComments, likePost, collectPost, addComment, likeComment, reportPost, reportComment, votePost } from '../../api/post.js';
import { getBackendOrigin } from '@/utils/api'

// ✅ 必须配置：请将此处改为你的电脑 IP (手机调试) 或 localhost (电脑调试)
const BASE_URL = getBackendOrigin();

export default {
  data() {
    return {
      postId: null,
      loading: true,
      isLiked: false,
      isCollected: false,
      commentText: '',
	  showEmojiPanel: false,
	  emojiList: ['😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃', '😉', '😊', '😇', '🥰', '😍', '🤩', '😘', '😗', '😚', '😙', '🥲', '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔', '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '👍', '👎', '👊', '✊', '🤝', '👏', '🙌', '🎉', '🎊', '🎈', '🎁', '🎀', '🎂', '🍰'],
      
      // 回复相关
      replyTarget: null, 
      
      // 帖子数据
      post: {},
      
      // 评论数据
      comments: []
    };
  },
  
  computed: {
    replyPlaceholder() {
      if (this.replyTarget) {
        return `回复 @${this.replyTarget.username}`;
      }
      return '说点什么吧';
    }
  },
  
  onLoad(options) {
    if (options.id) {
      this.postId = Number(options.id);
      this.initData();
    }
  },
  
  methods: {
    // ✅ 新增：图片路径处理方法
    getFullImageUrl(url) {
      if (!url) return '/static/logo.png'; // 默认图
      if (url.startsWith('http')) return url;
      // 处理相对路径
      return BASE_URL + (url.startsWith('/') ? url : '/' + url);
    },

    // 初始化数据
    async initData() {
      this.loading = true;
      try {
        await Promise.all([
          this.fetchPostDetail(),
          this.fetchComments()
        ]);
      } catch (error) {
        console.error('加载失败:', error);
        uni.showToast({ title: '加载失败', icon: 'none' });
      } finally {
        this.loading = false;
      }
    },
    
    // 获取帖子详情
    async fetchPostDetail() {
      const res = await getPostDetail(this.postId);
      if (res.code === 200) {
        this.post = res.data;
        this.isLiked = !!res.data.isLiked;
        this.isCollected = !!res.data.isCollected;
      } else {
        uni.showToast({ title: res.message, icon: 'none' });
      }
    },

    async handleVote(option) {
      if (!option || !option.id) return
      if (this.post.vote && this.post.vote.myOptionId) {
        uni.showToast({ title: '已投票', icon: 'none' })
        return
      }
      try {
        uni.showLoading({ title: '投票中...' })
        const res = await votePost(this.postId, option.id)
        if (res.code === 200) {
          this.post = { ...this.post, vote: res.data }
          uni.showToast({ title: '投票成功', icon: 'none' })
        }
      } catch (e) {
        uni.showToast({ title: e.message || '投票失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
    
    // 获取评论列表
    async fetchComments() {
      const res = await getPostComments(this.postId);
      if (res.code === 200) {
        this.comments = res.data.list;
      }
    },
    
    // 显示帖子菜单
    showPostMenu() {
      uni.showActionSheet({
        itemList: ['举报', '复制链接', '分享'],
        success: (res) => {
          if (res.tapIndex === 0) this.reportPost();
          else if (res.tapIndex === 1) this.copyLink();
          else if (res.tapIndex === 2) this.sharePost();
        }
      });
    },
    
    // 举报帖子
    reportPost() {
      uni.showActionSheet({
        itemList: ['垃圾广告', '违法违规', '低俗色情', '涉嫌侵权', '人身攻击', '其他'],
        success: async (res) => {
          const reasons = ['垃圾广告', '违法违规', '低俗色情', '涉嫌侵权', '人身攻击', '其他'];
          const reason = reasons[res.tapIndex];
          try {
            const result = await reportPost(this.postId, reason);
            if (result.code === 200) {
              uni.showToast({ title: '举报成功', icon: 'none' });
            }
          } catch (error) {
            uni.showToast({ title: '举报失败', icon: 'none' });
          }
        }
      });
    },
    
    // 复制链接
    copyLink() {
      uni.setClipboardData({
        data: `https://example.com/post/${this.postId}`,
        success: () => {
          uni.showToast({ title: '链接已复制', icon: 'none' });
        }
      });
    },
    
    sharePost() {
      uni.showToast({ title: '分享功能开发中', icon: 'none' });
    },
    
    // 显示评论菜单
    showCommentMenu(comment) {
      const items = comment.isMine ? ['删除', '举报'] : ['回复', '举报'];
      uni.showActionSheet({
        itemList: items,
        success: (res) => {
          const action = items[res.tapIndex];
          if (action === '删除') this.deleteComment(comment);
          else if (action === '举报') this.reportComment(comment);
          else if (action === '回复') this.replyToComment(comment);
        }
      });
    },
    
    // 显示回复菜单
    showReplyMenu(reply, parentComment) {
      const items = reply.isMine ? ['删除', '举报'] : ['回复', '举报'];
      uni.showActionSheet({
        itemList: items,
        success: (res) => {
          const action = items[res.tapIndex];
          if (action === '删除') this.deleteReply(reply, parentComment);
          else if (action === '举报') this.reportComment(reply);
          else if (action === '回复') this.replyToReply(reply, parentComment);
        }
      });
    },
    
    // 举报评论
    reportComment(comment) {
      uni.showActionSheet({
        itemList: ['垃圾广告', '违法违规', '低俗色情', '人身攻击', '其他'],
        success: async (res) => {
          const reasons = ['垃圾广告', '违法违规', '低俗色情', '人身攻击', '其他'];
          const reason = reasons[res.tapIndex];
          try {
            const result = await reportComment(comment.id, reason);
            if (result.code === 200) {
              uni.showToast({ title: '举报成功', icon: 'none' });
            }
          } catch (error) {
            uni.showToast({ title: '举报失败', icon: 'none' });
          }
        }
      });
    },
	// 切换表情面板
	toggleEmojiPanel() {
	  this.showEmojiPanel = !this.showEmojiPanel;
	},
	insertEmoji(emoji) {
	  this.commentText += emoji;
	  // 可选：插入后关闭面板
	  // this.showEmojiPanel = false;
	},
    
    // 回复一级评论
    replyToComment(comment) {
      this.replyTarget = {
        type: 'comment',
        id: comment.id,
        userId: comment.userId,
        username: comment.username,
        parentId: comment.id
      };
    },
    
    // 回复二级评论
    replyToReply(reply, parentComment) {
      this.replyTarget = {
        type: 'reply',
        id: reply.id,
        userId: reply.userId,
        username: reply.username,
        parentId: parentComment.id,
        replyToId: reply.id
      };
    },
    
    cancelReply() {
      this.replyTarget = null;
    },
    
    handleInputFocus() { 
		console.log('Focus');
		this.showEmojiPanel = false;
	 },
    handleInputBlur() {},
    
    goBack() { uni.navigateBack(); },
    goHome() { uni.reLaunch({ url: '/pages/index/index' }); },
    
    // ✅ 修复：跳转到帖子作者主页
    goToUser() {  
      const userId = this.post.userId || (this.post.user && this.post.user.id);
      console.log('查看帖子作者主页:', userId);
      if (userId) {
        uni.navigateTo({ url: `/pages/user/home?id=${userId}` });  
      }
    }, 
      
    // ✅ 修复：跳转到评论者主页 (方法之间加了逗号)
    goToUserProfile(userId) {  
      console.log('查看评论者主页:', userId); 
      if (userId) {
        uni.navigateTo({ url: `/pages/user/home?id=${userId}` });  
      }
    },

    // ✅ 修复：预览图片时也使用处理过的路径
    previewImage(index) {
      // 将所有图片路径处理为绝对路径
      const urls = this.post.images.map(img => this.getFullImageUrl(img));
      uni.previewImage({
        current: index,
        urls: urls
      });
    },
    
    // 点赞帖子
    async handleLike() {
      const newStatus = !this.isLiked;
      try {
        const res = await likePost(this.postId, newStatus);
        if (res.code === 200) {
          this.isLiked = newStatus;
          const delta = newStatus ? 1 : -1;
          const next = (this.post.likes || 0) + delta;
          this.post.likes = Math.max(0, next);
          if (newStatus) uni.showToast({ title: '❤️', icon: 'none', duration: 500 });
        }
      } catch (error) {
        uni.showToast({ title: '操作失败', icon: 'none' });
      }
    },
    
    // 收藏帖子
    async handleCollect() {
      const newStatus = !this.isCollected;
      try {
        const res = await collectPost(this.postId, newStatus);
        if (res.code === 200) {
          this.isCollected = newStatus;
          const delta = newStatus ? 1 : -1;
          const next = (this.post.collectCount || 0) + delta;
          this.post.collectCount = Math.max(0, next);
          uni.showToast({ title: newStatus ? '收藏成功' : '取消收藏', icon: 'none' });
        }
      } catch (error) {
        uni.showToast({ title: '操作失败', icon: 'none' });
      }
    },
    
    // 点赞评论
    async handleLikeComment(comment) {
      const newStatus = !comment.isLiked;
      try {
        const res = await likeComment(comment.id, newStatus);
        if (res.code === 200) {
          comment.isLiked = newStatus;
          comment.likes = (comment.likes || 0) + (newStatus ? 1 : -1);
          comment.likes = Math.max(0, comment.likes);
          if (newStatus) uni.showToast({ title: '❤️', icon: 'none', duration: 500 });
        }
      } catch (error) {
        console.error(error);
      }
    },
    
    // 点赞回复
    async handleLikeReply(reply) {
      const newStatus = !reply.isLiked;
      try {
        const res = await likeComment(reply.id, newStatus);
        if (res.code === 200) {
          reply.isLiked = newStatus;
          reply.likes = (reply.likes || 0) + (newStatus ? 1 : -1);
          reply.likes = Math.max(0, reply.likes);
          if (newStatus) uni.showToast({ title: '❤️', icon: 'none', duration: 500 });
        }
      } catch (error) {
        console.error(error);
      }
    },
    
    // 发送评论
    async handleSendComment() {
      if (!this.commentText.trim()) return;
      
      try {
        const commentData = {
          postId: this.postId,
          content: this.commentText
        };
        
        if (this.replyTarget) {
          commentData.parentId = this.replyTarget.parentId;
          commentData.replyToId = this.replyTarget.id;
        }
        
        const res = await addComment(commentData);
        if (res.code === 200) {
          const newComment = res.data;
          
          if (this.replyTarget) {
            // 添加到回复列表
            const parentComment = this.comments.find(c => c.id === this.replyTarget.parentId);
            if (parentComment) {
              if (!parentComment.replies) parentComment.replies = [];
              parentComment.replies.push(newComment);
            }
          } else {
            // 添加到主评论列表
            this.comments.unshift(newComment);
          }
          
          this.commentText = '';
          this.replyTarget = null;
          uni.showToast({ title: '评论成功', icon: 'none' });
        }
      } catch (error) {
        uni.showToast({ title: '评论失败', icon: 'none' });
      }
    },
    
    deleteComment(comment) {
      uni.showModal({
        title: '提示', content: '确定要删除这条评论吗？',
        success: (res) => {
          if (res.confirm) {
            const index = this.comments.findIndex(c => c.id === comment.id);
            if (index > -1) {
              this.comments.splice(index, 1);
              uni.showToast({ title: '删除成功', icon: 'none' });
            }
          }
        }
      });
    },
    
    deleteReply(reply, parentComment) {
      uni.showModal({
        title: '提示', content: '确定要删除这条回复吗？',
        success: (res) => {
          if (res.confirm) {
            const index = parentComment.replies.findIndex(r => r.id === reply.id);
            if (index > -1) {
              parentComment.replies.splice(index, 1);
              uni.showToast({ title: '删除成功', icon: 'none' });
            }
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.post-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 顶部导航 */
.nav-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  background: linear-gradient(135deg, #e8f5e9 0%, #f1f8e9 100%);
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn { font-size: 36rpx; color: #333; padding: 10rpx 20rpx; }
.nav-title { font-size: 32rpx; font-weight: bold; color: #333; }
.nav-more { font-size: 36rpx; color: #333; padding: 10rpx 20rpx; }

/* 加载提示 */
.loading-tip { text-align: center; padding: 100rpx 0; color: #999; font-size: 28rpx; }
.scroll-content { flex: 1; height: 0; }

/* 帖子卡片 */
.post-card { background-color: #fff; padding: 30rpx; margin-bottom: 20rpx; }
.post-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20rpx; }
.user-info { display: flex; align-items: center; }
.avatar { width: 90rpx; height: 90rpx; border-radius: 50%; margin-right: 20rpx; background-color: #e8f5e9; }
.user-meta { display: flex; flex-direction: column; }
.user-name-row { display: flex; align-items: center; margin-bottom: 6rpx; }
.username { font-size: 30rpx; font-weight: bold; color: #333; margin-right: 10rpx; }
.post-time { font-size: 24rpx; color: #999; }
.more-btn { font-size: 32rpx; color: #999; padding: 10rpx; }
.post-content { margin-bottom: 20rpx; }
.content-text { font-size: 30rpx; color: #333; line-height: 1.6; white-space: pre-wrap; }
.post-images { display: flex; flex-wrap: wrap; gap: 10rpx; margin-bottom: 20rpx; }
.post-image { width: 220rpx; height: 220rpx; border-radius: 10rpx; }

/* 投票 */
.vote-card { margin-bottom: 20rpx; padding: 22rpx; border-radius: 16rpx; background: #f8fafc; border: 1rpx solid #eef2f7; }
.vote-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.vote-title { font-size: 28rpx; font-weight: 700; color: #0f172a; }
.vote-subtitle { font-size: 24rpx; color: #64748b; }
.vote-options { display: flex; flex-direction: column; gap: 14rpx; }
.vote-option { padding: 16rpx; border-radius: 14rpx; background: #fff; border: 1rpx solid #e2e8f0; }
.vote-option.selected { border-color: #22c55e; background: #f0fdf4; }
.vote-option-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10rpx; }
.vote-option-text { font-size: 26rpx; color: #0f172a; }
.vote-option-count { font-size: 24rpx; color: #475569; }
.vote-bar { height: 10rpx; border-radius: 999rpx; background: #e2e8f0; overflow: hidden; }
.vote-bar-fill { height: 100%; border-radius: 999rpx; background: linear-gradient(90deg, #22c55e, #16a34a); }
.vote-hint { margin-top: 12rpx; display: block; font-size: 24rpx; color: #64748b; }

/* 操作栏 */
.action-bar { display: flex; justify-content: space-around; padding-top: 20rpx; border-top: 1rpx solid #eee; }
.action-item { display: flex; align-items: center; padding: 15rpx 25rpx; }
.action-icon { font-size: 32rpx; margin-right: 8rpx; color: #666; transition: transform 0.2s; }
.action-icon.active { color: #4CAF50; }
.heart-icon { font-size: 36rpx; }
.heart-icon.active { animation: heartBeat 0.3s ease; color: #ff4081; }
@keyframes heartBeat { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.3); } }
.action-text { font-size: 26rpx; color: #666; }

/* 评论区 */
.comment-section { background-color: #fff; padding: 30rpx; }
.comment-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30rpx; }
.comment-title { font-size: 30rpx; font-weight: bold; color: #333; }
.sort-btn { display: flex; align-items: center; font-size: 26rpx; color: #666; }
.sort-arrow { margin-left: 5rpx; }
.comment-item { margin-bottom: 30rpx; }
.comment-main { display: flex; }
.comment-avatar { width: 80rpx; height: 80rpx; border-radius: 50%; margin-right: 20rpx; background-color: #e8f5e9; flex-shrink: 0; }
.comment-body { flex: 1; min-width: 0; }
.comment-user { display: flex; align-items: center; margin-bottom: 10rpx; flex-wrap: wrap; }
.comment-username { font-size: 28rpx; font-weight: bold; color: #333; margin-right: 10rpx; }
.author-tag { font-size: 20rpx; color: #4CAF50; background-color: #e8f5e9; padding: 2rpx 10rpx; border-radius: 6rpx; margin-right: 10rpx; }
.op-tag { font-size: 20rpx; color: #ff9800; background-color: #fff3e0; padding: 2rpx 10rpx; border-radius: 6rpx; margin-right: 10rpx; }
.comment-content { font-size: 28rpx; color: #333; line-height: 1.5; margin-bottom: 15rpx; white-space: pre-wrap; }
.comment-footer { display: flex; justify-content: space-between; align-items: center; }
.comment-time { font-size: 24rpx; color: #999; }
.comment-actions { display: flex; align-items: center; }
.comment-action { display: flex; align-items: center; font-size: 24rpx; color: #666; margin-right: 20rpx; }
.comment-like-icon { font-size: 28rpx; margin-right: 5rpx; transition: transform 0.2s; }
.comment-like-icon.liked { animation: heartBeat 0.3s ease; color: #ff4081; }
.comment-like-text { font-size: 24rpx; color: #666; }
.comment-more { font-size: 28rpx; color: #999; }

/* 回复列表 */
.reply-list { background-color: #f8f8f8; border-radius: 10rpx; padding: 20rpx; margin-top: 15rpx; }
.reply-item { display: flex; margin-bottom: 20rpx; }
.reply-item:last-child { margin-bottom: 0; }
.reply-avatar { width: 60rpx; height: 60rpx; border-radius: 50%; margin-right: 15rpx; background-color: #e8f5e9; flex-shrink: 0; }
.reply-body { flex: 1; min-width: 0; }
.reply-user { display: flex; align-items: center; flex-wrap: wrap; margin-bottom: 8rpx; }
.reply-username { font-size: 26rpx; font-weight: bold; color: #333; margin-right: 10rpx; }
.reply-time { font-size: 22rpx; color: #999; margin-left: auto; }
.reply-like { display: flex; align-items: center; font-size: 22rpx; color: #666; margin-left: 15rpx; }
.reply-like-icon { font-size: 24rpx; margin-right: 3rpx; transition: transform 0.2s; }
.reply-like-icon.liked { animation: heartBeat 0.3s ease; color: #ff4081; }
.reply-more { font-size: 26rpx; color: #999; margin-left: 15rpx; }
.reply-content { font-size: 26rpx; color: #333; line-height: 1.5; white-space: pre-wrap; }
.reply-to { color: #4CAF50; }

.empty-comment { text-align: center; padding: 60rpx 0; color: #999; font-size: 28rpx; }
.bottom-space { height: 120rpx; }

/* 底部评论输入栏 */
.comment-input-bar { position: fixed; bottom: 0; left: 0; right: 0; display: flex; align-items: center; padding: 15rpx 20rpx; padding-bottom: calc(15rpx + env(safe-area-inset-bottom)); background-color: #fff; border-top: 1rpx solid #eee; z-index: 100; }
.home-icon { font-size: 40rpx; margin-right: 15rpx; }
.input-wrapper { flex: 1; display: flex; align-items: center; background-color: #f5f5f5; border-radius: 35rpx; padding: 15rpx 25rpx; margin-right: 15rpx; }
.comment-input { flex: 1; font-size: 28rpx; background: transparent; }
.emoji-btn { font-size: 36rpx; margin-left: 10rpx; }
.image-btn { font-size: 40rpx; margin-right: 15rpx; }
.send-btn { padding: 15rpx 30rpx; background-color: #ccc; color: #fff; font-size: 28rpx; border-radius: 35rpx; }
.send-btn.active { background-color: #4CAF50; }

.reply-bar { position: fixed; bottom: 120rpx; left: 0; right: 0; display: flex; justify-content: space-between; align-items: center; padding: 15rpx 30rpx; background-color: #fff3e0; border-top: 1rpx solid #ffe0b2; z-index: 99; }
.reply-hint { font-size: 26rpx; color: #ff6f00; }
.cancel-reply { font-size: 32rpx; color: #ff6f00; padding: 5rpx 15rpx; }
/* 表情面板 */
.emoji-panel {
  position: fixed;
  bottom: 120rpx;
  left: 0;
  right: 0;
  height: 400rpx;
  background-color: #fff;
  border-top: 1rpx solid #eee;
  z-index: 99;
}

.emoji-content {
  height: 100%;
  padding: 20rpx;
}

.emoji-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.emoji-item {
  font-size: 50rpx;
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
</style>
