import request from '../utils/request.js' 
  
/**  
 * 获取私信列表  
 */  
export async function getChatList() {  
  return request.get('/messages/chats')  
}  
  
/**  
 * 获取赞评通知列表  
 */  
export async function getNotifyList() {  
  return request.get('/messages/notifications')  
}  
  
/**  
 * 标记私信已读  
 * @param {number} chatId - 私信会话ID  
 */  
export async function markChatAsRead(chatId) {  
  return request.put(`/messages/chats/${chatId}/read`)  
}  
  
/**  
 * 标记赞评通知已读  
 * @param {number} notifyId - 通知ID  
 */  
export async function markNotifyAsRead(notifyId) {  
  return request.put(`/messages/notifications/${notifyId}/read`)  
}  
  
/**  
 * 标记所有赞评通知已读  
 */  
export async function markAllNotifyAsRead() {  
  return request.put('/messages/notifications/read-all')  
}  
  
/**  
 * 删除消息  
 * @param {string} type - 消息类型 'chat' | 'notify'  
 * @param {number} id - 消息ID  
 */  
export async function deleteMessage(type, id) {  
  if (type === 'chat') {  
    return request.delete(`/messages/chats/${id}`)  
  } else {  
    return request.delete(`/messages/notifications/${id}`)  
  }  
}  
  
/**  
 * 获取聊天用户信息  
 * @param {number} userId - 用户ID  
 */  
export async function getChatUserInfo(userId) {  
  return request.get(`/messages/users/${userId}`)  
}  
  
/**  
 * 获取聊天记录  
 * @param {number} userId - 对方用户ID  
 * @param {number} page - 页码  
 * @param {number} pageSize - 每页数量  
 */  
export async function getChatMessages(userId, page = 1, pageSize = 20) {  
  return request.get(`/messages/chats/${userId}/messages?page=${page}&pageSize=${pageSize}`)  
}  
  
/**  
 * 发送消息  
 * @param {object} data - 消息数据  
 * @param {number} data.receiverId - 接收者ID  
 * @param {string} data.type - 消息类型 'text' | 'image'  
 * @param {string} data.content - 消息内容  
 */  
export async function sendMessage(data) {  
  return request.post(`/messages/chats/${data.receiverId}/send`, {  
    content: data.content,  
    type: data.type  
  })  
}