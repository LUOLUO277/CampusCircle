// api/post.js
import request from '@/utils/request';

/**
 * 获取帖子详情
 * @param {Number} postId 
 */
export const getPostDetail = async (postId) => {
  const res = await request.get(`/posts/${postId}`);
  return res;
};

/**
 * 获取帖子评论列表
 * @param {Number} postId 
 */
export const getPostComments = async (postId) => {
  const res = await request.get(`/posts/${postId}/comments`);
  return res;
};

/**
 * 点赞帖子
 * @param {Number} postId 
 * @param {Boolean} isLike 
 */
export const likePost = async (postId, isLike) => {
  if (isLike === true) {
    return await request.post(`/posts/${postId}/like`);
  } else {
    return await request.del(`/posts/${postId}/like`);
  }
};

/**
 * 收藏帖子
 * @param {Number} postId 
 * @param {Boolean} isCollect 
 */
export const collectPost = async (postId, isCollect) => {
  if (isCollect) {
    return await request.post(`/posts/${postId}/collect`);
  }
  return await request.del(`/posts/${postId}/collect`);
};

/**
 * 发表评论
 * @param {Object} data - { postId, content, replyTo }
 */
export const addComment = async (data) => {
  const payload = {
    content: data.content,
    parentId: data.parentId || null,
    replyToId: data.replyToId || null
  };
  const res = await request.post(`/posts/${data.postId}/comments`, payload);
  return res;
};

/**
 * 点赞评论
 * @param {Number} commentId 
 * @param {Boolean} isLike 
 */
export const likeComment = async (commentId, isLike) => {
  const flag = isLike === true ? 'true' : 'false';
  return await request.post(`/comments/${commentId}/like?isLike=${flag}`);
};
// 举报帖子
export const reportPost = async (postId, reason) => {
  const res = await request.post(`/reports/post?postId=${postId}`, { reason });
  return res;
};
// 暂无评论举报后端接口，如需可保留 Mock 或后续补充
export const reportComment = async (commentId, reason) => {
  return { code: 200, message: '暂未开通评论举报' };
};

// 发布帖子
export const publishPost = async (data) => {
  const payload = {
    categoryId: data.categoryId || null,
    isAnonymous: data.isAnonymous || false,
    content: data.content,
    images: data.images || [],
    product: data.product || null,
    topicId: data.topicId || null,
    topicName: data.topicName || null
  };
  const res = await request.post('/posts', payload);
  return res;
};

/**
 * 置顶帖子
 * @param {Number} postId
 */
export const setPostTop = async (postId, isTop = true) => {
  if (isTop) {
    return await request.post(`/posts/${postId}/top`, { isTop: true });
  } else {
    return await request.del(`/posts/${postId}/top`);
  }
};
