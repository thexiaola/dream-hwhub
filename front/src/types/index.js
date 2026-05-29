export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export interface User {
  id: number
  username: string
  userNo: string
  email: string
  permission: number
  token?: string
  createTime?: string
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

export interface SendCodeRequest {
  email: string
  userNo: string
  username: string
}

export interface ModifyUserInfoRequest {
  username: string
}

export interface ModifyPasswordRequest {
  oldPassword: string
  newPassword: string
}

export interface ModifyEmailRequest {
  oldEmailCode: string
  newEmail: string
  newEmailCode: string
}

export interface RetrievePasswordRequest {
  account: string
  code: string
  newPassword: string
}

export interface WorkInfo {
  id: number
  title: string
  description: string
  publisherId: number
  publisherName: string
  classId: number
  className: string
  deadline: string
  totalScore: number
  allowLateSubmit: boolean
  publishTime: string
  isPinned?: boolean
  status?: number
  createTime: string
  updateTime: string
  attachments?: WorkAttachment[]
}

export interface WorkAttachment {
  id: number
  fileName: string
  filePath: string
  fileSize: number
  fileType: string
  uploadTime: string
}

export interface CreateWorkRequest {
  title: string
  description: string
  deadline: string
  totalScore: number
  classId: number
  publishTime?: string
  allowLateSubmit?: boolean
}

export interface UpdateWorkRequest {
  id: number
  title: string
  description: string
  deadline: string
  totalScore: number
  allowLateSubmit?: boolean
  publishTime?: string
}

export interface PinWorkRequest {
  workId: number
  isPinned: boolean
}

export interface ClassInfo {
  id: number
  className: string
  description?: string
  ownerId: number
  ownerName: string
  userRole: string
  memberCount: number
  teacherCount: number
  studentCount: number
  inviteCode?: string
  approvalStatus?: number
  adminRemark?: string
  createTime?: string
  updateTime?: string
}

export interface ClassMember {
  id: number
  userId: number
  userName: string
  userNo: string
  role: string
  joinTime: string
}

export interface CreateClassRequest {
  className: string
  description?: string
}

export interface JoinClassRequest {
  classId: number
}

export interface UpdateClassRequest {
  classId: number
  className: string
  description?: string
}

export interface ClassApplication {
  id: number
  applicantId: number
  className: string
  description?: string
  status: number
  createTime: string
  classId?: number
  applicantName?: string
}

export interface WorkSubmission {
  id: number
  workId: number
  workTitle: string
  userId: number
  userName: string
  submissionContent?: string
  score?: number
  comment?: string
  submitTime: string
  isLate: boolean
  attachments?: WorkSubmissionAttachment[]
}

export interface WorkSubmissionAttachment {
  id: number
  fileName: string
  filePath: string
  fileSize: number
  fileType: string
  uploadTime: string
}

export interface SubmitWorkRequest {
  workId: number
  submissionContent?: string
}

export interface GradeWorkRequest {
  submissionId: number
  score: number
  comment: string
}

export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface PageRequest {
  pageNum?: number
  pageSize?: number
}