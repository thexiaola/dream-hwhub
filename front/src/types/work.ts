/**
 * 作业题目 / 逐题作答相关接口类型
 */

/** 题型：单选/多选/判断/填空/主观/附加 */
export type QuestionType =
  | 'single'
  | 'multiple'
  | 'judge'
  | 'fill'
  | 'subjective'
  | 'extra'

export const QUESTION_TYPE_LABEL: Record<QuestionType, string> = {
  single: '单选题',
  multiple: '多选题',
  judge: '判断题',
  fill: '填空题',
  subjective: '主观题',
  extra: '附加题',
}

/** 客观题（可自动评判） */
export const OBJECTIVE_TYPES: QuestionType[] = ['single', 'multiple', 'judge', 'fill']

export const isObjectiveType = (t: QuestionType): boolean => OBJECTIVE_TYPES.includes(t)

/** 选项 */
export interface QuestionOption {
  key: string
  text: string
}

/** 教师侧题目（含参考答案与解析） */
export interface WorkQuestionVO {
  id: number | null
  workId?: number
  orderNo?: number
  questionType: QuestionType
  questionTypeName?: string
  content: string
  options?: QuestionOption[]
  /** 单选/判断-字符串，多选-字符串数组，填空-可接受答案数组 */
  correctAnswer?: unknown
  score: number
  analysis?: string
  autoGradable?: boolean
}

/** 学生侧题目（不下发参考答案） */
export interface WorkQuestionStudentVO {
  id: number
  orderNo: number
  questionType: QuestionType
  questionTypeName?: string
  content: string
  options?: QuestionOption[]
  score: number
}

/** 提交时携带的单题作答 */
export interface AnswerItem {
  questionId: number
  /** 单选/判断-字符串，多选-字符串数组，填空-字符串，主观/附加-文本 */
  answer: unknown
}

/** 提交详情中的逐题作答明细 */
export interface WorkAnswerVO {
  id: number
  questionId: number
  orderNo?: number
  questionType: QuestionType
  questionTypeName?: string
  content?: string
  answer?: unknown
  fullScore?: number
  score?: number | null
  isCorrect?: boolean | null
  gradingType?: 'auto' | 'manual' | null
  comment?: string | null
  correctAnswer?: unknown
  analysis?: string | null
  autoGradable?: boolean
  gradeTime?: string | null
}

/** 任务类型：作业 / 考试 */
export type WorkType = 'homework' | 'exam'

/** 考试反作弊与限时配置 */
export interface ExamConfig {
  /** 考试时长（分钟）；为空表示仅以截止时间为准 */
  durationMinutes: number | null
  /** 是否开启反作弊（关闭时其余子项一律不生效） */
  enabled: boolean
  /** 字体映射：题干以打乱字体渲染，复制得到乱码 */
  fontScramble: boolean
  /** 强制全屏：离开全屏即记违规 */
  forceFullscreen: boolean
  /** 禁止复制粘贴 */
  noCopy: boolean
  /** 切屏/失焦检测 */
  detectLeave: boolean
  /** 违规次数上限；为空表示不限制 */
  maxViolations: number | null
  /** 题目乱序 */
  shuffleQuestions: boolean
}

/** 违规类型 */
export type ExamViolationType =
  | 'fullscreen_exit'
  | 'visibility_hidden'
  | 'window_blur'
  | 'copy'
  | 'cut'
  | 'paste'

/** 考试会话数据（进入考试返回） */
export interface ExamEnterResponse {
  workId: number
  title: string
  description: string
  totalScore: number
  status: number
  durationMinutes: number | null
  startTime: string
  endTime?: string | null
  remainingSeconds?: number | null
  submitted: boolean
  antiCheatEnabled: boolean
  antiCheatFont: boolean
  antiCheatFullscreen: boolean
  antiCheatNoCopy: boolean
  antiCheatDetectLeave: boolean
  antiCheatMaxViolations: number | null
  violationCount: number
  fontSeed: number | null
  questions: WorkQuestionStudentVO[]
  draftAnswers?: string | null
}

/** 考试违规记录（教师审计用） */
export interface ExamViolationVO {
  id: number
  sessionId: number
  workId: number
  studentId: number
  studentName?: string | null
  studentRealName?: string | null
  studentNo?: string | null
  type: ExamViolationType
  typeName: string
  detail?: string | null
  occurTime: string
}

