export interface ApiResponse<T = null> {
  code: number
  message: string
  data: T
}

/** 分页查询结果信封（各列表接口通用） */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export interface UserInfo {
  id: number
  account: string
  username: string
  email: string
  phone?: string
  /** 头像文件相对路径，空表示未设置 */
  avatar?: string | null
  /** 是否为平台管理员（OP），拥有全部权限节点 */
  isOp?: boolean
  /** 生效的权限节点（OP 为全部节点） */
  permissions?: string[]
  /** 危险操作是否启用「密码验证」 */
  verifyByPassword?: boolean
  /** 危险操作是否启用「邮箱验证码验证」 */
  verifyByEmailCode?: boolean
  /** 已被该用户关闭二次验证的敏感操作标识（如 class.dissolve）；未列出者默认需要验证 */
  disabledVerificationOperations?: string[]
  role: 'teacher' | 'student'
  registerTime?: string
  lastLoginTime?: string
  isBanned?: boolean
}

/** 危险操作安全验证设置 */
export interface SecurityVerificationSettings {
  verifyByPassword: boolean
  verifyByEmailCode: boolean
}

/** 单个敏感操作的验证设置（危险操作验证页签） */
export interface SensitiveOperationSetting {
  /** 操作标识，如 class.dissolve */
  key: string
  /** 操作名称，如 解散班级 */
  name: string
  /** 操作说明 */
  description: string
  /** 是否要求二次验证（默认 true） */
  enabled: boolean
  /** 当前用户是否可用该操作（不可用则前端不展示其开关） */
  available: boolean
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  emailCode: string
}
