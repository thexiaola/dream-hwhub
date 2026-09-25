/** 站内信相关接口的返回类型 */

/** 站内信，对应 /messages 分页返回的记录 */
export interface SiteMessageInfo {
  id: number
  /** 所属学校 ID（站内信按学校隔离） */
  schoolId: number
  /** 所属学校名称 */
  schoolName?: string | null
  /** 消息类型，如 work_published */
  type: string
  /** 消息标题 */
  title: string
  /** 消息内容 */
  content?: string | null
  /** 关联班级 */
  classId?: number | null
  className?: string | null
  /** 关联作业 */
  workId?: number | null
  isRead: boolean
  readTime?: string | null
  createTime: string
}

/** 某学校的未读站内信数量 */
export interface UnreadCount {
  schoolId: number
  count: number
}

/** 站内信类型常量：与后端 SiteMessageType 对应 */
export const MESSAGE_TYPE_WORK_PUBLISHED = 'work_published'

/** 站内信类型筛选项 */
export const MESSAGE_TYPE_OPTIONS: { label: string; value: string }[] = [
  { label: '全部类型', value: '' },
  { label: '新作业', value: MESSAGE_TYPE_WORK_PUBLISHED }
]

/** 已读状态筛选项 */
export const MESSAGE_READ_OPTIONS: { label: string; value: '' | 'unread' | 'read' }[] = [
  { label: '全部', value: '' },
  { label: '未读', value: 'unread' },
  { label: '已读', value: 'read' }
]
