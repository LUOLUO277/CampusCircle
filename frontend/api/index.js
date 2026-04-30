// api/index.js
import { delay, topicsData, categoriesData, postsData } from '@/mock/index.js';
import request from '@/utils/request';

/**
 * 获取热门话题
 */
export const getHotTopics = async () => {
  // 改为真实接口：热门帖子
  const res = await request.get('/posts/hot');
  return res;
};

/**
 * 获取分类列表
 */
export const getCategories = async () => {
  // 如果后端未提供分类接口，可临时走 Mock
  // await delay(100);
  // return { code: 200, data: categoriesData, message: 'success' };
  const res = await request.get('/categories');
  return res;
};

/**
 * 获取帖子列表
 * @param {Object} params - { categoryId, page, pageSize }
 */
export const getPosts = async (params = {}) => {
  const res = await request.get('/posts', params);
  return res;
};

/**
 * 点赞/取消点赞
 * @param {Number} postId 
 * @param {Boolean} isLike 
 */
export const toggleLike = async (postId, isLike) => {
  const res = await request.post(`/posts/${postId}/like`, { isLike });
  return res;
};

/**
 * 搜索帖子
 * @param {String} keyword 
 */
export const searchPosts = async (keyword) => {
  // 如果后端未提供搜索接口，可根据列表在前端筛选
  const res = await request.get('/posts', { keyword });
  return res;
};
