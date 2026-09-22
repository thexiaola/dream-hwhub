export interface ApiResponse<T = null> {
  code: number
  message: string
  data: T
}

export interface UserInfo {
  id: number
  account: string
  username: string
  email: string
  phone?: string
  /** 是否为平台管理员（OP），拥有全部权限节点 */
  isOp?: boolean
  /** 生效的权限节点（OP 为全部节点） */
  permissions?: string[]
  role: 'teacher' | 'student'
  registerTime?: string
  lastLoginTime?: string
  isBanned?: boolean
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  emailCode: string
}
