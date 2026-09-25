/**
 * 敏感操作标识常量（与后端 SensitiveOperations 一一对应）
 * <p>
 * 调用危险操作时传入对应 key：若用户为该操作关闭了二次验证，前端将跳过身份验证弹窗，
 * 后端拦截器也会同步跳过校验（双端一致）。
 */
export const SensitiveOperationKeys = {
  CLASS_DISSOLVE: 'class.dissolve',
  CLASS_KICK_MEMBER: 'class.kick_member',
  CLASS_LEAVE: 'class.leave',
  CLASS_TRANSFER: 'class.transfer',
  SCHOOL_LEAVE: 'school.leave',
  SCHOOL_DISSOLVE: 'school.dissolve',
  SCHOOL_ASSIGN_ADMIN: 'school.assign_admin',
  PERMISSION_GROUP_DELETE: 'permission.group.delete',
  PERMISSION_GROUP_SET_NODES: 'permission.group.set_nodes',
  PERMISSION_GROUP_ASSIGN: 'permission.group.assign',
  PERMISSION_USER_ASSIGN: 'permission.user.assign',
  USER_DELETE: 'user.delete',
  USER_BAN: 'user.ban',
  USER_SET_OP: 'user.set_op',
  SUBMISSION_WITHDRAW: 'submission.withdraw',
} as const
