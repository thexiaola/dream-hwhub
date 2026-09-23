/** 学校简要信息 */
export interface School {
  id: number
  schoolName: string
  description?: string | null
  /** 加入学校是否免审核：true-填入学工号与姓名后直接加入 */
  allowJoinWithoutApproval: boolean
  memberCount: number
  createTime?: string
}

/** 学校详情（含当前用户在该校的身份与申请状态） */
export interface SchoolDetail extends School {
  adminCount: number
  teacherCount: number
  studentCount: number
  classCount: number
  /** 当前用户是否已是该校成员 */
  member: boolean
  /** 2-学校管理员，1-老师，0-学生，null-非成员 */
  myRoleCode: number | null
  myRole: string | null
  myStaffNo: string | null
  myRealName: string | null
  /** 我的加入申请状态：0-待审核，1-已通过，2-已拒绝，null-无申请 */
  myApplicationStatus: number | null
  /** 待审核的加入申请数量，仅管理员视角返回 */
  pendingApplicationCount?: number | null
  /** 我最新一条加入申请的审核意见，被拒绝时可看到原因 */
  myApplicationComment?: string | null
}

/** 学校成员 */
export interface SchoolMember {
  id: number
  schoolId: number
  userId: number
  username: string
  realName: string
  staffNo: string
  roleCode: number
  role: string
  joinTime: string
}

/** 学校加入申请 */
export interface SchoolJoinApplication {
  id: number
  schoolId: number
  schoolName: string | null
  applicantId: number
  applicantUsername: string | null
  applicantName: string | null
  applicantNo: string | null
  status: number
  reviewerId: number | null
  reviewerName: string | null
  reviewComment: string | null
  createTime: string
  reviewTime: string | null
}

/** 学校成员角色代码：2-学校管理员，1-老师，0-学生 */
export const SCHOOL_ROLE_ADMIN = 2
export const SCHOOL_ROLE_TEACHER = 1
export const SCHOOL_ROLE_STUDENT = 0
