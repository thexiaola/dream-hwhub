export interface PermissionNode {
  node: string
  name: string
  description: string
}

export interface PermissionNodeGroup {
  key: string
  name: string
  nodes: PermissionNode[]
}

export interface PermissionGroup {
  id: number
  code: string
  name: string
  description?: string
  isDefault: boolean
  nodes: string[]
  userCount: number
  createTime?: string
}

export interface AdminUser {
  id: number
  username: string
  email: string
  phone?: string
  /** 头像文件相对路径，空表示未设置 */
  avatar?: string | null
  isOp: boolean
  isBanned: boolean
  banReason?: string | null
  groupNames: string[]
  permissions: string[]
  registerTime?: string
  lastLoginTime?: string
}

export interface UserPermissionDetail {
  userId: number
  username: string
  isOp: boolean
  groups: PermissionGroup[]
  directNodes: string[]
  permissions: string[]
}

export interface AdminUserForm {
  username: string
  email: string
  phone: string
  password: string
}
