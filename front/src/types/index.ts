export interface ApiResponse<T = null> {
  code: number
  message: string
  data: T
}

export interface UserInfo {
  id: number
  account: string
  username: string
  userNo: string
  email: string
  idName?: string
  phone?: string
  permission: number
  role: 'teacher' | 'student'
  registerTime?: string
  lastLoginTime?: string
  isBanned?: boolean
}

export interface RegisterRequest {
  username: string
  userNo: string
  email: string
  password: string
  emailCode: string
}
