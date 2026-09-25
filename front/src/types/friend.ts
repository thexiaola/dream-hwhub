/** 好友 / 私信 / 私信策略相关类型 */

/** 好友 */
export interface FriendInfo {
  relationId: number
  schoolId: number
  userId: number
  username: string | null
  avatar?: string | null
  realName?: string | null
  staffNo?: string | null
  role?: string | null
  friendTime?: string | null
}

/** 收到的好友申请 */
export interface FriendRequestInfo {
  relationId: number
  schoolId: number
  schoolName?: string | null
  requesterId: number
  requesterUsername: string | null
  requesterRealName?: string | null
  requesterStaffNo?: string | null
  requesterAvatar?: string | null
  createTime: string
}

/** 与我的关系状态 */
export type FriendRelation = 'none' | 'pending_out' | 'pending_in' | 'friend'

/** 可添加的校内用户 */
export interface AddableUserInfo {
  userId: number
  schoolId: number
  username: string | null
  avatar?: string | null
  realName?: string | null
  staffNo?: string | null
  role?: string | null
  relation: FriendRelation
}

/** 私信会话摘要 */
export interface ConversationInfo {
  schoolId: number
  schoolName?: string | null
  peerId: number
  peerUsername: string | null
  peerAvatar?: string | null
  peerRealName?: string | null
  peerStaffNo?: string | null
  peerRole?: string | null
  friend: boolean
  lastContent?: string | null
  lastTime?: string | null
  unreadCount: number
}

/** 单条私信 */
export interface PrivateMessageInfo {
  id: number
  schoolId: number
  senderId: number
  receiverId: number
  content: string
  isRead: boolean
  createTime: string
  mine: boolean
}

/** 私信策略（陌生私信配额） */
export interface MessagePolicyInfo {
  /** 生效的条数上限 */
  strangerLimit: number
  /** 生效的重置小时数 */
  resetHours: number
  /** 学校是否自定义覆盖（全站策略恒为 false） */
  schoolOverridden: boolean
  /** 全站默认条数上限 */
  globalStrangerLimit: number
  /** 全站默认重置小时数 */
  globalResetHours: number
}

/** 关系状态文案 */
export const FRIEND_RELATION_TEXT: Record<FriendRelation, string> = {
  none: '添加好友',
  pending_out: '已申请',
  pending_in: '待我处理',
  friend: '已是好友'
}
