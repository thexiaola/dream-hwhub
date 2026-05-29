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
  role: 'teacher' | 'student'
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
  code: string
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
}
