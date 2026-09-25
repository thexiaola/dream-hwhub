import type { QuestionOption, QuestionType, WorkQuestionVO } from '@/types/work'

/** 选项键序：A~H */
const OPTION_KEYS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']

/** 生成默认选项列表 */
export const defaultOptions = (count = 4): QuestionOption[] =>
  Array.from({ length: count }, (_, i) => ({ key: OPTION_KEYS[i], text: '' }))

/** 追加一个选项（顺序取下一个可用键） */
export const appendOption = (options: QuestionOption[]): QuestionOption[] => {
  const key = OPTION_KEYS[options.length] || String(options.length + 1)
  return [...options, { key, text: '' }]
}

export interface DraftQuestion {
  /** 稳定本地键，用于列表渲染/拖拽 */
  uid: number
  id: number | null
  questionType: QuestionType
  content: string
  options: QuestionOption[]
  /** 单选/判断题：选项键字符串 */
  singleAnswer: string
  /** 多选题：选项键数组 */
  multipleAnswer: string[]
  /** 判断题：true/false */
  judgeAnswer: boolean
  /** 填空题：可接受答案（多行→数组） */
  fillAnswers: string[]
  score: number
  analysis: string
}

let uidSeed = 1
export const nextUid = (): number => uidSeed++

/** 新建一个空白题目草稿 */
export const createDraft = (type: QuestionType = 'single', score = 5): DraftQuestion => ({
  uid: nextUid(),
  id: null,
  questionType: type,
  content: '',
  options: type === 'single' || type === 'multiple' ? defaultOptions() : [],
  singleAnswer: '',
  multipleAnswer: [],
  judgeAnswer: true,
  fillAnswers: [''],
  score,
  analysis: '',
})

/** 教师侧题目 VO → 草稿（编辑已有作业时回填） */
export const draftFromVO = (q: WorkQuestionVO): DraftQuestion => {
  const draft = createDraft(q.questionType, q.score ?? 5)
  draft.id = q.id ?? null
  draft.content = q.content || ''
  draft.options = Array.isArray(q.options) && q.options.length
    ? q.options.map((o) => ({ key: o.key, text: o.text }))
    : draft.options
  draft.analysis = q.analysis || ''

  const ans = q.correctAnswer
  switch (q.questionType) {
    case 'single':
      draft.singleAnswer = typeof ans === 'string' ? ans : ''
      break
    case 'multiple':
      draft.multipleAnswer = Array.isArray(ans) ? ans.map(String) : []
      break
    case 'judge':
      draft.judgeAnswer = ans === true || ans === 'true' || ans === '对' || ans === '√'
      break
    case 'fill':
      draft.fillAnswers = Array.isArray(ans) && ans.length
        ? ans.map(String)
        : typeof ans === 'string' && ans
          ? [ans]
          : ['']
      break
    default:
      break
  }
  return draft
}

/** 草稿 → 提交给后端的题目项 */
export const draftToPayload = (draft: DraftQuestion) => {
  const base = {
    id: draft.id ?? undefined,
    questionType: draft.questionType,
    content: draft.content.trim(),
    score: draft.score,
    analysis: draft.analysis.trim() || undefined,
  }
  switch (draft.questionType) {
    case 'single':
      return {
        ...base,
        options: draft.options.map((o) => ({ key: o.key, text: o.text.trim() })),
        correctAnswer: draft.singleAnswer,
      }
    case 'multiple':
      return {
        ...base,
        options: draft.options.map((o) => ({ key: o.key, text: o.text.trim() })),
        correctAnswer: draft.multipleAnswer,
      }
    case 'judge':
      return { ...base, correctAnswer: draft.judgeAnswer }
    case 'fill':
      return {
        ...base,
        correctAnswer: draft.fillAnswers.map((s) => s.trim()).filter((s) => s !== ''),
      }
    default:
      // 主观题/附加题无参考答案
      return base
  }
}

/** 校验草稿，返回错误信息（null 表示通过） */
export const validateDraft = (draft: DraftQuestion, index: number): string | null => {
  const no = index + 1
  if (!draft.content.trim()) {
    return `第 ${no} 题：题干不能为空`
  }
  if (draft.score == null || draft.score < 0) {
    return `第 ${no} 题：分值不能为负`
  }
  const t = draft.questionType
  if (t === 'single' || t === 'multiple') {
    const filled = draft.options.filter((o) => o.text.trim() !== '')
    if (filled.length < 2) {
      return `第 ${no} 题：选择题至少需要 2 个有效选项`
    }
    if (t === 'single' && !draft.singleAnswer) {
      return `第 ${no} 题：请设置参考答案`
    }
    if (t === 'multiple' && draft.multipleAnswer.length === 0) {
      return `第 ${no} 题：请设置参考答案（至少一项）`
    }
  }
  if (t === 'fill' && draft.fillAnswers.every((s) => s.trim() === '')) {
    return `第 ${no} 题：请至少填写一个可接受答案`
  }
  return null
}
