/**
 * 班级（课程）相关接口的返回类型
 */

/** 班级信息，对应 /class/mine、/class/{id} 的返回结构 */
export interface CourseInfo {
  id: number
  className: string
  description?: string
  ownerId: number
  ownerName: string
  /** 我在该班级的角色名称：创建者/老师/课代表/学生 */
  userRole: string
  /** 我在该班级的角色代码：1-拥有班级管理员权限，0-普通成员 */
  userRoleCode: number
  memberCount: number
  teacherCount: number
  studentCount: number
  /** 是否允许学生邀请同学加入 */
  allowStudentInvite?: boolean
}

/** 教师视角的作业信息 */
export interface TeacherWorkInfo {
  id: number
  title: string
  description: string
  classId: number
  className: string
  deadline: string
  totalScore: number
  isPinned: boolean
  status: number
  publishTime: string
  submittedCount?: number
  publisherName?: string | null
  publisherStudentName?: string | null
}

/** 班级成员 */
export interface ClassMemberInfo {
  id: number
  userId: number
  userName: string
  studentName: string | null
  studentNo: string | null
  role: string
  /** 1-拥有班级管理员权限，0-普通成员 */
  roleCode: number
  joinTime: string
  teacherCount: number
}

/** 学生邀请的教师审核记录 */
export interface TeacherApprovalInfo {
  id: number
  classId: number
  className: string
  inviteeId: number
  inviteeUsername: string
  status: number
  reviewerId?: number
  reviewerUsername?: string
  reviewTime?: string
  reviewComment?: string
  createTime: string
}
