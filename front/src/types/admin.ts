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
  userNo: string
  username: string
  idName?: string
  email: string
  phone?: string
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
  userNo: string
  username: string
  idName: string
  email: string
  phone: string
  password: string
}
