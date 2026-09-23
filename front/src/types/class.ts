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
  /** 班级所属学校 */
  schoolId?: number | null
  schoolName?: string | null
  memberCount: number
  teacherCount: number
  studentCount: number
  /** 是否允许学生邀请同学加入 */
  allowStudentInvite?: boolean
  /** 班级是否已冻结：创建者的教师身份被解除 */
  frozen?: boolean
  /** 创建者是否仍具备教师身份 */
  ownerActive?: boolean
  /** 当前用户是否可申请接管该班级（已冻结且本人是该校老师） */
  canTakeover?: boolean
  /** 当前用户是否已提交待审核的接管申请 */
  takeoverPending?: boolean
  /** 所属学校的班级接管是否自动同意 */
  takeoverAutoApprove?: boolean
}

/** 班级接管申请 */
export interface ClassTakeoverInfo {
  id: number
  classId: number
  className: string
  applicantId: number
  applicantUsername?: string
  applicantName?: string | null
  applicantNo?: string | null
  /** 0-待审核，1-已通过，2-已拒绝 */
  status: number
  reviewerId?: number | null
  reviewerUsername?: string | null
  reviewTime?: string | null
  reviewComment?: string | null
  createTime: string
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
