// api/message.js

const delay = (ms = 300) => new Promise(resolve => setTimeout(resolve, ms));

// 用户信息数据
const usersData = {
  101: {
    id: 101,
    username: '同学c79756',
    avatar: '/static/avatars/avatar1.png',
    isOnline: true
  },
  102: {
    id: 102,
    username: '黄灯泡绿灯炮',
    avatar: '/static/avatars/avatar2.png',
    isOnline: false
  },
  103: {
    id: 103,
    username: '小红的日常',
    avatar: '/static/avatars/avatar3.png',
    isOnline: true
  }
};

// 聊天记录数据 (按用户ID存储)
const chatMessagesData = {
  101: [
    {
      id: 1001,
      senderId: 101,
      receiverId: 1,
      type: 'text',
      content: '你好呀！',
      time: '2025-01-10T09:00:00.000Z',
      status: 'sent'
    },
    {
      id: 1002,
      senderId: 1,
      receiverId: 101,
      type: 'text',
      content: '你好，有什么事吗？',
      time: '2025-01-10T09:01:00.000Z',
      status: 'sent'
    },
    {
      id: 1003,
      senderId: 101,
      receiverId: 1,
      type: 'text',
      content: '想问一下明天的活动几点开始？',
      time: '2025-01-10T09:02:00.000Z',
      status: 'sent'
    },
    {
      id: 1004,
      senderId: 1,
      receiverId: 101,
      type: 'text',
      content: '下午2点，在学校操场集合',
      time: '2025-01-10T09:03:00.000Z',
      status: 'sent'
    },
    {
      id: 1005,
      senderId: 101,
      receiverId: 1,
      type: 'text',
      content: '好的，收到！那需要带什么东西吗？',
      time: '2025-01-10T09:05:00.000Z',
      status: 'sent'
    },
    {
      id: 1006,
      senderId: 1,
      receiverId: 101,
      type: 'text',
      content: '带上水和防晒就行，可能会比较热',
      time: '2025-01-10T09:06:00.000Z',
      status: 'sent'
    },
    {
      id: 1007,
      senderId: 101,
      receiverId: 1,
      type: 'text',
      content: '😊好的，那我们明天见！',
      time: '2025-01-10T09:08:00.000Z',
      status: 'sent'
    }
  ],
  102: [
    {
      id: 2001,
      senderId: 102,
      receiverId: 1,
      type: 'text',
      content: '在吗？',
      time: '2025-01-09T14:00:00.000Z',
      status: 'sent'
    },
    {
      id: 2002,
      senderId: 1,
      receiverId: 102,
      type: 'text',
      content: '在的，什么事？',
      time: '2025-01-09T14:05:00.000Z',
      status: 'sent'
    },
    {
      id: 2003,
      senderId: 102,
      receiverId: 1,
      type: 'text',
      content: '看到你发的那个笔记本，还在吗？',
      time: '2025-01-09T14:06:00.000Z',
      status: 'sent'
    },
    {
      id: 2004,
      senderId: 1,
      receiverId: 102,
      type: 'text',
      content: '还在的，你想要吗？',
      time: '2025-01-09T14:08:00.000Z',
      status: 'sent'
    },
    {
      id: 2005,
      senderId: 102,
      receiverId: 1,
      type: 'text',
      content: '笔记本还在吗？',
      time: '2025-01-10T10:00:00.000Z',
      status: 'sent'
    }
  ],
  103: [
    {
      id: 3001,
      senderId: 1,
      receiverId: 103,
      type: 'text',
      content: '你的校园照片拍得真好看！',
      time: '2025-01-08T16:00:00.000Z',
      status: 'sent'
    },
    {
      id: 3002,
      senderId: 103,
      receiverId: 1,
      type: 'text',
      content: '谢谢你的点赞～',
      time: '2025-01-08T16:30:00.000Z',
      status: 'sent'
    },
    {
      id: 3003,
      senderId: 103,
      receiverId: 1,
      type: 'text',
      content: '那天天气特别好，随手拍的hh',
      time: '2025-01-08T16:31:00.000Z',
      status: 'sent'
    }
  ]
};

// 消息ID计数器
let messageIdCounter = 10000;

// 私信列表数据
let chatListData = [
  {
    id: 1,
    userId: 101,
    avatar: '/static/avatars/avatar1.png',
    username: '同学c79756',
    lastMessage: '好的，那我们明天见！',
    time: '刚刚',
    unread: 2
  },
  {
    id: 2,
    userId: 102,
    avatar: '/static/avatars/avatar2.png',
    username: '黄灯泡绿灯炮',
    lastMessage: '笔记本还在吗？',
    time: '1小时前',
    unread: 0
  },
  {
    id: 3,
    userId: 103,
    avatar: '/static/avatars/avatar3.png',
    username: '小红的日常',
    lastMessage: '谢谢你的点赞～',
    time: '昨天',
    unread: 1
  }
];

// 赞评通知列表数据
// type: 'like' - 点赞, 'comment' - 评论, 'reply' - 回复
let notifyListData = [
  {
    id: 1,
    type: 'like',                    // 点赞类型
    typeText: '赞了你的帖子',
    userId: 201,
    username: '数码爱好者',
    avatar: '/static/avatars/avatar4.png',
    time: '5分钟前',
    postId: 2,                       // 关联的帖子ID
    quote: '出一台笔记本自用的可以流畅打朝瓦打cf...',  // 被赞的帖子内容摘要
    quoteLabel: '我的帖子',
    isRead: false                    // 未读状态
  },
  {
    id: 2,
    type: 'comment',                 // 评论类型
    typeText: '评论了你的帖子',
    userId: 202,
    username: '穷学生一枚',
    avatar: '/static/avatars/avatar5.png',
    time: '30分钟前',
    postId: 2,                       // 关联的帖子ID
    commentId: 2,                    // 评论ID（用于回复）
    commentContent: '800能包邮吗？', // 评论内容
    quote: '出一台笔记本自用的可以流畅打朝瓦打cf...',
    quoteLabel: '我的帖子',
    isRead: false
  },
  {
    id: 3,
    type: 'like',                    // 点赞评论
    typeText: '赞了你的评论',
    userId: 301,
    username: '摄影小白',
    avatar: '/static/logo.png',
    time: '1小时前',
    postId: 3,                       // 关联的帖子ID
    commentId: 1,                    // 被赞的评论ID
    quote: '好美啊！这是哪个校区？', // 被赞的评论内容
    quoteLabel: '我的评论',
    isRead: false
  },
  {
    id: 4,
    type: 'reply',                   // 回复类型
    typeText: '回复了你的评论',
    userId: 102,
    username: '0721高手',
    avatar: '/static/avatars/avatar2.png',
    time: '2小时前',
    postId: 1,                       // 关联的帖子ID
    commentId: 2,                    // 父评论ID
    replyId: 21,                     // 回复ID
    commentContent: '我是男生哇',    // 回复内容
    quote: '可以帮我测800不，酬金100😖',  // 被回复的内容
    quoteLabel: '我的评论',
    isRead: true                     // 已读
  },
  {
    id: 5,
    type: 'comment',
    typeText: '评论了你的帖子',
    userId: 103,
    username: '温婉的网络',
    avatar: '/static/avatars/avatar3.png',
    time: '3小时前',
    postId: 1,
    commentId: 22,
    commentContent: '可以帮我测800不，酬金100😖\n(q:849014041',
    quote: '800m 3分40s但是不稳定可以接d测吗...',
    quoteLabel: '我的帖子',
    isRead: true
  },
  {
    id: 6,
    type: 'like',
    typeText: '赞了你的帖子',
    userId: 104,
    username: '运动达人',
    avatar: '/static/avatars/avatar1.png',
    time: '5小时前',
    postId: 1,
    quote: '800m 3分40s但是不稳定可以接d测吗...',
    quoteLabel: '我的帖子',
    isRead: true
  },
  {
    id: 7,
    type: 'like',
    typeText: '赞了你的帖子',
    userId: 105,
    username: '校园生活家',
    avatar: '/static/avatars/avatar2.png',
    time: '昨天',
    postId: 3,
    quote: '今天天气真好，分享一下校园里的美景～',
    quoteLabel: '我的帖子',
    isRead: true
  },
  {
    id: 8,
    type: 'comment',
    typeText: '评论了你的帖子',
    userId: 301,
    username: '摄影小白',
    avatar: '/static/logo.png',
    time: '昨天',
    postId: 3,
    commentId: 1,
    commentContent: '好美啊！这是哪个校区？',
    quote: '今天天气真好，分享一下校园里的美景～',
    quoteLabel: '我的帖子',
    isRead: true
  }
];

/**
 * 获取私信列表
 */
export async function getChatList() {
  await delay(300);
  
  const unreadCount = chatListData.reduce((sum, item) => sum + item.unread, 0);
  
  return {
    code: 200,
    data: {
      list: chatListData,
      unreadCount
    }
  };
}

/**
 * 获取赞评通知列表
 */
export async function getNotifyList() {
  await delay(300);
  
  // 计算未读数
  const unreadCount = notifyListData.filter(item => !item.isRead).length;
  
  return {
    code: 200,
    data: {
      list: notifyListData,
      unreadCount
    }
  };
}

/**
 * 标记私信已读
 * @param {number} chatId - 私信会话ID
 */
export async function markChatAsRead(chatId) {
  await delay(100);
  
  // 更新本地数据
  const chat = chatListData.find(item => item.id === chatId);
  if (chat) {
    chat.unread = 0;
  }
  
  return {
    code: 200,
    message: '标记成功'
  };
}

/**
 * 标记赞评通知已读
 * @param {number} notifyId - 通知ID
 */
export async function markNotifyAsRead(notifyId) {
  await delay(100);
  
  // 更新本地数据
  const notify = notifyListData.find(item => item.id === notifyId);
  if (notify) {
    notify.isRead = true;
  }
  
  return {
    code: 200,
    message: '标记成功'
  };
}

/**
 * 标记所有赞评通知已读
 */
export async function markAllNotifyAsRead() {
  await delay(200);
  
  // 更新本地数据
  notifyListData.forEach(item => {
    item.isRead = true;
  });
  
  return {
    code: 200,
    message: '标记成功'
  };
}

/**
 * 删除消息
 * @param {string} type - 消息类型 'chat' | 'notify'
 * @param {number} id - 消息ID
 */
export async function deleteMessage(type, id) {
  await delay(200);
  
  if (type === 'chat') {
    chatListData = chatListData.filter(item => item.id !== id);
  } else {
    notifyListData = notifyListData.filter(item => item.id !== id);
  }
  
  return {
    code: 200,
    message: '删除成功'
  };
}

/**
 * 获取聊天用户信息
 * @param {number} userId - 用户ID
 */
export async function getChatUserInfo(userId) {
  await delay(200);
  
  const user = usersData[userId];
  
  if (user) {
    return {
      code: 200,
      data: user
    };
  }
  
  return {
    code: 404,
    message: '用户不存在'
  };
}

/**
 * 获取聊天记录
 * @param {number} userId - 对方用户ID
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 */
export async function getChatMessages(userId, page = 1, pageSize = 20) {
  await delay(300);
  
  const allMessages = chatMessagesData[userId] || [];
  
  // 按时间正序排列
  const sortedMessages = [...allMessages].sort((a, b) => 
    new Date(a.time) - new Date(b.time)
  );
  
  // 分页处理（从最新的开始取）
  const total = sortedMessages.length;
  const start = Math.max(0, total - page * pageSize);
  const end = total - (page - 1) * pageSize;
  const list = sortedMessages.slice(start, end);
  
  return {
    code: 200,
    data: {
      list,
      total,
      hasMore: start > 0
    }
  };
}

/**
 * 发送消息
 * @param {object} data - 消息数据
 * @param {number} data.receiverId - 接收者ID
 * @param {string} data.type - 消息类型 'text' | 'image'
 * @param {string} data.content - 消息内容
 */
export async function sendMessage(data) {
  await delay(500);
  
  // 模拟发送成功
  const newMessage = {
    id: ++messageIdCounter,
    senderId: 1, // 当前用户ID
    receiverId: data.receiverId,
    type: data.type,
    content: data.content,
    time: new Date().toISOString(),
    status: 'sent'
  };
  
  // 保存到聊天记录
  if (!chatMessagesData[data.receiverId]) {
    chatMessagesData[data.receiverId] = [];
  }
  chatMessagesData[data.receiverId].push(newMessage);
  
  // 更新私信列表的最后一条消息
  const chatItem = chatListData.find(item => item.userId === data.receiverId);
  if (chatItem) {
    chatItem.lastMessage = data.type === 'text' ? data.content : '[图片]';
    chatItem.time = '刚刚';
  }
  
  return {
    code: 200,
    data: newMessage,
    message: '发送成功'
  };
}