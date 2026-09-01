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

export interface LoginRequest {
  account: string
  password: string
}

export interface RegisterRequest {
  username: string
  userNo: string
  email: string
  password: string
  emailCode: string
}

export interface WorkInfo {
  id: number
  title: string
  description: string
  classId: number
  className: string
  score: number
  deadline: string
  status: 'pending' | 'graded' | 'expired'
  isPinned: boolean
  createdAt: string
  updatedAt: string
  attachments?: WorkAttachment[]
}

export interface WorkAttachment {
  id: number
  fileName: string
  filePath: string
  fileSize?: number
  fileType?: string
}

export interface ClassInfo {
  id: number
  className: string
  description: string
  teacherId: number
  teacherName: string
  createdAt: string
}

export interface WorkSubmission {
  id: number
  workId: number
  workTitle: string
  submitterId: number
  submitterName: string
  submitterUserNo: string
  content: string
  grade: number | null
  graderName: string | null
  gradedAt: string | null
  submittedAt: string
  status: 'submitted' | 'graded'
  attachments?: WorkAttachment[]
}
