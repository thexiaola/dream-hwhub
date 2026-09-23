/**
 * 申请审核状态（0-待审核，1-已通过，2-已拒绝）的展示映射。
 *
 * 「加入班级申请」与「加入学校申请」共用同一套状态码与文案/样式后缀，
 * 集中在此避免各页面各写一份、新增状态时漏改。
 */

/** 状态 → 文案 */
const STATUS_TEXT: Record<number, string> = {
  0: '待审核',
  1: '已通过',
  2: '已拒绝',
}

/** 状态 → 样式后缀（配 .status-tag / .status-text 使用） */
const STATUS_CLASS: Record<number, string> = {
  0: 'pending',
  1: 'approved',
  2: 'rejected',
}

/** 未知状态返回「未知」，样式回退为 pending */
export const applicationStatusText = (status: number): string => STATUS_TEXT[status] ?? '未知'

export const applicationStatusClass = (status: number): string => STATUS_CLASS[status] ?? 'pending'
