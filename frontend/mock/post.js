// mock/post.js

export const delay = (ms = 300) => new Promise(resolve => setTimeout(resolve, ms));

// 帖子详情数据
export const postsDetailData = {
  1: {
    id: 1,
    userId: 101,
    userAvatar: '/static/avatars/avatar1.png',
    userName: '同学c79756',
    userLevel: 'v4',
    time: '14天前',
    content: '800m\n3分40s但是不稳定可以接d测吗哈哈哈不知道算什么水平',
    images: [],
    shares: 1,
    collects: 0,
    likes: 1,
	isTop: false
  },
  2: {
    id: 2,
    userId: 102,
    userAvatar: '/static/avatars/avatar2.png',
    userName: '黄灯泡绿灯炮',
    userLevel: 'v3',
    time: '2024晚',
    content: '出一台笔记本自用的可以流畅打朝瓦打cf,换台式了所以不用了,爽快来,980买不了吃亏买不了上当！',
    images: ['/static/images/laptop1.png', '/static/images/laptop2.png'],
    shares: 5,
    collects: 12,
    likes: 234,
    product: { price: 980 },
	isTop: false
  },
  3: {
    id: 3,
    userId: 103,
    userAvatar: '/static/avatars/avatar3.png',
    userName: '小红的日常',
    userLevel: 'v5',
    time: '1小时前',
    content: '今天天气真好，分享一下校园里的美景～阳光洒在草坪上，感觉整个人都充满了活力！',
    images: ['/static/images/campus1.png', '/static/images/campus2.png', '/static/images/campus3.png'],
    shares: 8,
    collects: 25,
    likes: 892,
	isTop: false
  }
};

// 评论数据
export const commentsData = {
  1: [
    {
      id: 1,
      userId: 101,
      avatar: '/static/avatars/avatar1.png',
      username: '同学c79756',
     
      isAuthor: true,
      content: '立定跳远无沙坑可 > 2m',
      time: '14天前',
      likes: 0,
      replies: []
    },
    {
      id: 2,
      userId: 102,
      avatar: '/static/avatars/avatar2.png',
      username: '0721高手',
      
      isAuthor: false,
      content: '请问待测基本是什么流程啊，老师会不会验脸啊，我也想干这一行',
      time: '14天前',
      likes: 0,
      replies: [
        {
          id: 21,
          userId: 101,
          avatar: '/static/avatars/avatar1.png',
          username: '同学c79756',
          isAuthor: true,
          isOP: false,
          time: '14天前',
          content: '好像不会，但是我在犹豫因为太冷了',
          replyTo: null
        },
        {
          id: 22,
          userId: 103,
          avatar: '/static/avatars/avatar3.png',
          username: '温婉的网络',
          isAuthor: false,
          isOP: false,
          time: '14天前',
          content: '可以帮我测800不，酬金100😖\n(q:849014041',
          replyTo: null
        },
        {
          id: 23,
          userId: 102,
          avatar: '/static/avatars/avatar2.png',
          username: '0721高手',
          isAuthor: false,
          isOP: true,
          time: '14天前',
          content: '我是男生哇',
          replyTo: '蓝色果冻水母'
        }
      ]
    }
  ],
  2: [
    {
      id: 1,
      userId: 201,
      avatar: '/static/avatars/avatar4.png',
      username: '数码爱好者',
      
      isAuthor: false,
      content: '什么配置啊？能说一下吗',
      time: '1天前',
      likes: 5,
      replies: [
        {
          id: 11,
          userId: 102,
          avatar: '/static/avatars/avatar2.png',
          username: '黄灯泡绿灯炮',
          isAuthor: true,
          isOP: false,
          time: '1天前',
          content: 'i5-10400 + GTX1650 + 16G内存 + 512G固态',
          replyTo: null
        }
      ]
    },
    {
      id: 2,
      userId: 202,
      avatar: '/static/avatars/avatar5.png',
      username: '穷学生一枚',
      
      isAuthor: false,
      content: '800能包邮吗？',
      time: '12小时前',
      likes: 2,
      replies: []
    }
  ],
  3: [
    {
      id: 1,
      userId: 301,
      avatar: '/static/logo.png',
      username: '摄影小白',
      
      isAuthor: false,
      content: '好美啊！这是哪个校区？',
      time: '30分钟前',
      likes: 3,
      replies: []
    }
  ]
};

import { postsData } from './index.js';  // ⭐ 引入首页用的帖子列表

export async function setTop(postId, isTop = true) {
  await delay(200);

  // 首页数据
  const post = postsData.find(p => p.id === postId);
  if (post) post.isTop = isTop;

  // 详情数据
  if (postsDetailData[postId]) {
    postsDetailData[postId].isTop = isTop;
  }

  return { code: 200, message: isTop ? '置顶成功' : '取消置顶成功' };
}
