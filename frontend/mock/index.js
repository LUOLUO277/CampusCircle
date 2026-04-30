// mock/data.js

// 模拟延迟，更真实地模拟网络请求
export const delay = (ms = 300) => new Promise(resolve => setTimeout(resolve, ms));

// 话题数据
export const topicsData = [
  { 
    id: 1, 
    title: '感谢好心人请我吃疯4', 
    hot: true, 
    views: '123万',
    
  },
  { 
    id: 2, 
    title: '旅行的意义是什么', 
    hot: true, 
    views: '97万',
    
  },
  { 
    id: 3, 
    title: '今天的单子有点多啊', 
    hot: false, 
    views: '65万',
    
  }
];

// 分类数据
export const categoriesData = [
  { id: 0, name: '全部' },
  { id: 1, name: '闲置' },
  { id: 2, name: '求助' },
  { id: 3, name: '日常生活' },
  { id: 4, name: '投票' },
  { id: 5, name: '吐槽' }
];

// 帖子数据
export const postsData = [
  {
    id: 1,
    userId: 101,
    userAvatar: '👨',
    userName: '黄灯泡绿灯炮',
    userLevel: 'LV.3',
    time: '2024晚',
    tag: '闲置',
    categoryId: 1,
    content: '出一台笔记本自用的可以流畅打朝瓦打cf,换台式了所以不用了,爽快来,980买不了...',
    images: ['#8B7355', '#6B5344', '#5C4033'],
    product: { price: 980 },
    views: '14206',
    comments: '124',
    likes: '234',
	isTop: false
  },
  {
    id: 2,
    userId: 101,
    userAvatar: '👨',
    userName: '黄灯泡绿灯炮',
    userLevel: 'LV.3',
    time: '2024晚',
    tag: '投票',
    categoryId: 4,
    content: '下雨天你最喜欢干什么?',
    images: [],
    views: '14206',
    comments: '124',
    likes: '234',
	isTop: false
  },
  {
    id: 3,
    userId: 102,
    userAvatar: '👩',
    userName: '小红的日常',
    userLevel: 'LV.5',
    time: '1小时前',
    tag: '日常生活',
    categoryId: 3,
    content: '今天天气真好，分享一下校园里的美景～阳光洒在草坪上，感觉整个人都充满了活力！',
    images: ['#90EE90', '#98FB98', '#87CEEB'],
    views: '8520',
    comments: '56',
    likes: '892',
	isTop: false
  },
  {
    id: 4,
    userId: 103,
    userAvatar: '👦',
    userName: '学习小达人',
    userLevel: 'LV.4',
    time: '3小时前',
    tag: '求助',
    categoryId: 2,
    content: '有没有人知道图书馆几点开门啊？明天要去占座准备期末考试',
    images: [],
    views: '3240',
    comments: '45',
    likes: '120',
	isTop: false
  }
];