<template>
  <div class="student-work-page">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" class="back-btn">
          <ArrowLeft :size="18" />
        </el-button>
        <h2>{{ work?.title || '作业详情' }}</h2>
        <span v-if="work" :class="['status-tag', workStatus]">{{ workStatusText }}</span>
      </div>
    </div>

    <el-card class="work-info-card">
      <div class="info-section">
        <div class="info-item">
          <User :size="16" />
          <span class="label">布置人：</span>
          <span class="value">{{ publisherLabel }}</span>
        </div>
        <div class="info-item">
          <FileText :size="16" />
          <span class="label">总分：</span>
          <span class="value">{{ work?.totalScore }}分</span>
        </div>
        <div class="info-item">
          <Clock :size="16" />
          <span class="label">截止时间：</span>
          <span :class="['value', { overdue: isExpired }]">{{ work ? formatDate(work.deadline) : '' }}</span>
        </div>
        <div class="info-item">
          <CalendarClock :size="16" />
          <span class="label">逾期提交：</span>
          <span class="value">{{ work?.allowLateSubmit ? '允许' : '不允许' }}</span>
        </div>
      </div>
      <div v-if="work?.description" class="description-section">
        <h4>作业要求</h4>
        <p>{{ work.description }}</p>
      </div>
      <div v-if="work?.attachments?.length" class="attachments-section">
        <h4>作业附件</h4>
        <div class="attachment-list">
          <div v-for="att in work.attachments" :key="att.id" class="attachment-card">
            <Paperclip :size="14" class="att-icon" />
            <span class="att-name" :title="att.fileName">{{ att.fileName }}</span>
            <span class="att-size">{{ formatSize(att.fileSize) }}</span>
            <el-button size="small" type="primary" plain class="att-download" @click="downloadFile(att)">
              <Download :size="14" />
              <span>下载</span>
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="submission-card">
      <template #header>
        <div class="card-header">
          <h3>我的提交</h3>
          <div class="header-actions">
            <el-button
              v-if="mySubmission && mySubmission.status !== 2 && canEdit"
              type="danger"
              plain
              :loading="withdrawing"
              @click="withdrawSubmission"
            >
              <Undo2 :size="16" />
              <span>撤回提交</span>
            </el-button>
            <el-button
              v-if="mySubmission && mySubmission.status !== 2 && !editing && canEdit"
              type="primary"
              plain
              @click="startEditing"
            >
              <PenLine :size="16" />
              <span>修改提交</span>
            </el-button>
          </div>
        </div>
      </template>

      <!-- 已批改 / 已打回：均展示分数与批语；打回时额外提示可重新提交 -->
      <div v-if="mySubmission && (mySubmission.status === 2 || mySubmission.status === 3)" class="graded-section">
        <el-alert
          v-if="mySubmission.status === 3"
          type="warning"
          :closable="false"
          show-icon
          title="作业已被打回，请根据下方教师评语修改后重新提交"
          class="edit-alert return-alert"
        />
        <div class="score-row">
          <Award :size="20" />
          <span class="score-value">{{ mySubmission.score ?? '—' }}</span>
          <span class="score-total">/ {{ work?.totalScore }}分</span>
          <span v-if="mySubmission.isLate" class="late-badge">逾期提交</span>
        </div>
        <div class="graded-meta">
          <span>批改人：{{ mySubmission.graderName || '—' }}</span>
          <span>批改时间：{{ mySubmission.gradeTime ? formatDate(mySubmission.gradeTime) : '—' }}</span>
        </div>
        <div v-if="mySubmission.comment" class="comment-section">
          <h4>{{ mySubmission.status === 3 ? '教师评语（打回原因）' : '教师评语' }}</h4>
          <p>{{ mySubmission.comment }}</p>
        </div>
        <div v-if="mySubmission.answers?.length" class="questions-result-block">
          <h4>逐题作答与得分</h4>
          <div
            v-for="(a, ai) in mySubmission.answers"
            :key="a.questionId"
            class="result-question-item"
            :class="correctnessClass(a)"
          >
            <div class="rq-head">
              <span class="rq-no">第 {{ ai + 1 }} 题</span>
              <span class="rq-type">{{ a.questionTypeName || typeLabel(a.questionType) }}</span>
              <span class="rq-score">
                <template v-if="a.score !== null && a.score !== undefined">
                  {{ a.score }} / {{ a.fullScore }} 分
                </template>
                <template v-else>待评分（满分 {{ a.fullScore }}）</template>
              </span>
              <span v-if="a.isCorrect === true" class="rq-flag correct">正确</span>
              <span v-else-if="a.isCorrect === false" class="rq-flag wrong">错误</span>
            </div>
            <p class="rq-content">{{ a.content }}</p>
            <div class="rq-answer">
              <span class="rq-label">我的作答：</span>
              <span class="rq-value">{{ formatAnswer(a.answer, a.questionType) || '未作答' }}</span>
            </div>
            <div v-if="showCorrect(a)" class="rq-correct">
              <span class="rq-label">参考答案：</span>
              <span class="rq-value correct-text">{{ formatAnswer(a.correctAnswer, a.questionType) }}</span>
            </div>
            <div v-if="a.analysis" class="rq-analysis">
              <span class="rq-label">解析：</span>
              <span class="rq-value">{{ a.analysis }}</span>
            </div>
            <div v-if="a.comment" class="rq-teacher-comment">
              <span class="rq-label">本题评语：</span>
              <span class="rq-value">{{ a.comment }}</span>
            </div>
          </div>
        </div>
        <div v-if="mySubmission.submissionContent" class="content-section">
          <h4>提交内容</h4>
          <p class="content-text">{{ mySubmission.submissionContent }}</p>
        </div>
        <div v-if="mySubmission.attachments?.length" class="attachments-section">
          <h4>提交附件</h4>
          <div class="attachment-list">
            <div v-for="att in mySubmission.attachments" :key="att.id" class="attachment-card">
              <Paperclip :size="14" class="att-icon" />
              <span class="att-name" :title="att.fileName">{{ att.fileName }}</span>
              <span class="att-size">{{ formatSize(att.fileSize) }}</span>
              <el-button size="small" type="primary" plain class="att-download" @click="downloadFile(att)">
                <Download :size="14" />
                <span>下载</span>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 只读展示已提交未批改（已打回会走上面的分数/评语分支） -->
      <div v-else-if="mySubmission && !editing" class="readonly-section">
        <div class="submitted-meta">
          <span class="submitted-badge">
            <CheckCircle2 :size="14" />
            已提交
          </span>
          <span v-if="mySubmission.isLate" class="late-badge">逾期提交</span>
          <span class="meta-time">提交时间：{{ mySubmission.createTime ? formatDate(mySubmission.createTime) : '—' }}</span>
        </div>
        <div v-if="mySubmission.submissionContent" class="content-section">
          <h4>提交内容</h4>
          <p class="content-text">{{ mySubmission.submissionContent }}</p>
        </div>
        <div v-if="mySubmission.attachments?.length" class="attachments-section">
          <h4>提交附件</h4>
          <div class="attachment-list">
            <div v-for="att in mySubmission.attachments" :key="att.id" class="attachment-card">
              <Paperclip :size="14" class="att-icon" />
              <span class="att-name" :title="att.fileName">{{ att.fileName }}</span>
              <span class="att-size">{{ formatSize(att.fileSize) }}</span>
              <el-button size="small" type="primary" plain class="att-download" @click="downloadFile(att)">
                <Download :size="14" />
                <span>下载</span>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 编辑/新增提交表单 -->
      <div v-else class="edit-section">
        <el-alert
          v-if="editing"
          :type="mySubmission && mySubmission.status === 3 ? 'warning' : 'info'"
          :closable="false"
          show-icon
          :title="mySubmission && mySubmission.status === 3
            ? '作业已被打回，请按教师评语修改后重新提交，保存后将覆盖原提交内容'
            : '正在修改已提交的作业，保存后将覆盖原提交内容'"
          class="edit-alert"
        />
        <el-alert
          v-else-if="isExpired && work?.allowLateSubmit"
          type="warning"
          :closable="false"
          show-icon
          title="已超过截止时间，当前为逾期提交"
          class="edit-alert"
        />
        <el-form label-position="top" class="submission-form">
          <div v-if="questions.length" class="questions-answer-block">
            <h4 class="questions-title">题目作答</h4>
            <div
              v-for="(q, qi) in questions"
              :key="q.id"
              class="answer-question-item"
            >
              <div class="aq-head">
                <span class="aq-no">第 {{ qi + 1 }} 题</span>
                <span class="aq-type">{{ q.questionTypeName || typeLabel(q.questionType) }}</span>
                <span class="aq-score">（{{ q.score }} 分）</span>
              </div>
              <p class="aq-content">{{ q.content }}</p>

              <!-- 单选 -->
              <el-radio-group
                v-if="q.questionType === 'single'"
                :model-value="strAnswer(q.id)"
                class="aq-options"
                @update:model-value="(v: string | number | boolean | undefined) => setStrAnswer(q.id, v)"
              >
                <el-radio
                  v-for="opt in q.options"
                  :key="opt.key"
                  :value="opt.key"
                  class="aq-option"
                >
                  <span class="opt-key">{{ opt.key }}.</span> {{ opt.text }}
                </el-radio>
              </el-radio-group>

              <!-- 多选 -->
              <el-checkbox-group
                v-else-if="q.questionType === 'multiple'"
                :model-value="arrAnswer(q.id)"
                class="aq-options"
                @update:model-value="(v: (string | number)[]) => setArrAnswer(q.id, v)"
              >
                <el-checkbox
                  v-for="opt in q.options"
                  :key="opt.key"
                  :value="opt.key"
                  class="aq-option"
                >
                  <span class="opt-key">{{ opt.key }}.</span> {{ opt.text }}
                </el-checkbox>
              </el-checkbox-group>

              <!-- 判断题 -->
              <el-radio-group
                v-else-if="q.questionType === 'judge'"
                :model-value="strAnswer(q.id)"
                class="aq-options"
                @update:model-value="(v: string | number | boolean | undefined) => setStrAnswer(q.id, v)"
              >
                <el-radio value="true" class="aq-option">正确</el-radio>
                <el-radio value="false" class="aq-option">错误</el-radio>
              </el-radio-group>

              <!-- 填空题 -->
              <el-input
                v-else-if="q.questionType === 'fill'"
                :model-value="strAnswer(q.id)"
                placeholder="请输入答案"
                class="aq-input"
                @update:model-value="(v: string) => setStrAnswer(q.id, v)"
              />

              <!-- 主观题 / 附加题 -->
              <el-input
                v-else
                :model-value="strAnswer(q.id)"
                type="textarea"
                :rows="4"
                maxlength="4096"
                show-word-limit
                placeholder="请输入作答内容"
                class="aq-input"
                @update:model-value="(v: string) => setStrAnswer(q.id, v)"
              />
            </div>
          </div>

          <el-form-item label="提交内容">
            <el-input
              v-model="content"
              type="textarea"
              :rows="8"
              maxlength="5000"
              show-word-limit
              placeholder="请输入作业内容或说明（选填）"
            />
          </el-form-item>
          <el-form-item label="新增附件">
            <el-upload
              v-model:file-list="newFiles"
              class="new-upload"
              :auto-upload="false"
              multiple
              :limit="20"
              :on-exceed="handleExceed"
              :on-change="handleFileChange"
            >
              <el-button type="primary" plain class="upload-trigger-btn">
                <Upload :size="16" />
                <span>选择文件</span>
              </el-button>
              <template #tip>
                <div class="upload-tip">
                  <Paperclip :size="12" />
                  <span>支持多文件上传，单个文件不超过 50MB</span>
                </div>
              </template>
              <template #file="{ file }">
                <div class="attachment-card new-file-item">
                  <Paperclip :size="14" class="att-icon" />
                  <span class="att-name" :title="file.name">{{ file.name }}</span>
                  <span class="att-size">{{ formatSize(file.size) }}</span>
                  <el-button
                    link
                    type="danger"
                    class="att-action"
                    @click="removeNewFile(file)"
                  >
                    <X :size="14" />
                    <span>移除</span>
                  </el-button>
                </div>
              </template>
            </el-upload>
          </el-form-item>
          <el-form-item v-if="editing && existingAttachments.length" label="已上传附件">
            <div class="existing-attachment-list">
              <div
                v-for="att in existingAttachments"
                :key="att.id"
                :class="['attachment-card', 'existing-attachment-item', { removed: removedIds.includes(att.id) }]"
              >
                <Paperclip :size="14" class="att-icon" />
                <span class="att-name" :title="att.fileName">{{ att.fileName }}</span>
                <span class="att-size">{{ formatSize(att.fileSize) }}</span>
                <el-button
                  v-if="!removedIds.includes(att.id)"
                  link
                  type="danger"
                  class="att-action"
                  @click="markRemove(att.id)"
                >
                  <X :size="14" />
                  <span>移除</span>
                </el-button>
                <el-button v-else link type="primary" class="att-action" @click="unmarkRemove(att.id)">
                  <RotateCcw :size="14" />
                  <span>恢复</span>
                </el-button>
              </div>
            </div>
          </el-form-item>
        </el-form>
        <div class="form-actions">
          <el-button @click="cancelEditing">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ editing ? '更新提交' : '提交作业' }}
          </el-button>
        </div>
      </div>

      <!-- 空状态：截止且不允许逾期 -->
      <div v-if="!mySubmission && !editing && !canEdit" class="locked-state">
        <XCircle :size="32" />
        <p>作业已截止，且教师未开启逾期提交</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, postForm, putForm, del } from '@/utils/http'
import instance from '@/utils/http'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadUserFile } from 'element-plus'
import {
  ArrowLeft,
  Award,
  CalendarClock,
  CheckCircle2,
  Clock,
  Download,
  FileText,
  Paperclip,
  PenLine,
  RotateCcw,
  Undo2,
  Upload,
  User,
  X,
  XCircle,
} from '@lucide/vue'
import { formatDateTime as formatDate, formatFileSize as formatSize } from '@/utils/format'
import { confirmDangerousOperation } from '@/composables/useSensitiveVerification'
import { SensitiveOperationKeys } from '@/constants/sensitiveOperations'
import type { AnswerItem, WorkAnswerVO, WorkQuestionStudentVO } from '@/types/work'
import { QUESTION_TYPE_LABEL } from '@/types/work'

interface AttachmentInfo {
  id: number
  fileName: string
  filePath: string
  fileSize?: number
  fileType?: string
  uploadTime?: string
}

interface WorkDetail {
  id: number
  title: string
  description: string
  classId: number
  className: string
  deadline: string
  totalScore: number
  allowLateSubmit: boolean
  status: number
  isPinned: boolean
  hasQuestions?: boolean
  attachments?: AttachmentInfo[]
  publisherName?: string | null
  publisherStudentName?: string | null
}

interface MySubmission {
  id: number
  workId: number
  workTitle?: string
  submissionContent: string
  score?: number | null
  comment?: string | null
  graderName?: string | null
  gradeTime?: string | null
  status: number
  isLate: boolean
  hasQuestions?: boolean
  answers?: WorkAnswerVO[]
  createTime: string
  updateTime?: string
  attachments?: AttachmentInfo[]
}

const route = useRoute()
const router = useRouter()

const work = ref<WorkDetail | null>(null)
const mySubmission = ref<MySubmission | null>(null)

const editing = ref(false)
const submitting = ref(false)
const content = ref('')
const newFiles = ref<UploadUserFile[]>([])
const existingAttachments = ref<AttachmentInfo[]>([])
const removedIds = ref<number[]>([])

/** 题目列表（学生侧，不含参考答案） */
const questions = ref<WorkQuestionStudentVO[]>([])
/** 逐题作答：questionId -> 作答值（单选/判断-字符串，多选-字符串数组，填空/主观-字符串） */
const answerMap = ref<Record<number, string | string[]>>({})

/** 取字符串型作答案（单选/判断/填空/主观） */
const strAnswer = (qid: number): string => {
  const v = answerMap.value[qid]
  return typeof v === 'string' ? v : ''
}
/** 取数组型作答（多选） */
const arrAnswer = (qid: number): string[] => {
  const v = answerMap.value[qid]
  return Array.isArray(v) ? v : []
}
const setStrAnswer = (qid: number, v: string | number | boolean | undefined) => {
  answerMap.value[qid] = v == null ? '' : String(v)
}
const setArrAnswer = (qid: number, v: (string | number)[]) => {
  answerMap.value[qid] = (v || []).map(String)
}

const typeLabel = (t: string) => QUESTION_TYPE_LABEL[t as keyof typeof QUESTION_TYPE_LABEL] || t

/** 展示答案文本（多选数组用「、」连接） */
const formatAnswer = (val: unknown, type: string): string => {
  if (val === null || val === undefined || val === '') return ''
  if (type === 'judge') {
    const s = String(val).toLowerCase()
    if (s === 'true' || s === '1' || s === '对') return '正确'
    if (s === 'false' || s === '0' || s === '错') return '错误'
    return String(val)
  }
  if (Array.isArray(val)) return val.map(String).join('、')
  return String(val)
}

/** 是否展示参考答案（教师侧或学生提交被批改后由后端下发的 correctAnswer 决定） */
const showCorrect = (a: WorkAnswerVO): boolean =>
  a.correctAnswer !== null && a.correctAnswer !== undefined && a.correctAnswer !== ''

const correctnessClass = (a: WorkAnswerVO): string => {
  if (a.isCorrect === true) return 'is-correct'
  if (a.isCorrect === false) return 'is-wrong'
  return ''
}

const MAX_ATTACHMENT_SIZE = 50 * 1024 * 1024

const isExpired = computed(() => {
  if (!work.value) return false
  return new Date() > new Date(work.value.deadline)
})

/** 布置人标识：班级成员显示班级内姓名，非成员（如管理员）退回用户名。不展示学号 */
const publisherLabel = computed(() => {
  const w = work.value
  if (!w) {
    return '—'
  }
  return w.publisherStudentName || w.publisherName || '—'
})

const workStatus = computed(() => {
  if (!work.value) return 'pending'
  if (isExpired.value) return 'expired'
  if (work.value.status === 1) return 'active'
  return 'pending'
})

const workStatusText = computed(() => {
  const map: Record<string, string> = {
    active: '进行中',
    expired: '已截止',
    pending: '未发布',
  }
  return map[workStatus.value] || workStatus.value
})

const canEdit = computed(() => {
  if (!work.value) return false
  if (!isExpired.value) return true
  return !!work.value.allowLateSubmit
})

const loadWork = async () => {
  const result = await get<WorkDetail>(`/works/${route.params.id}`)
  if (result.code === 200) {
    work.value = result.data
  } else {
    ElMessage.error(result.message)
  }
}

/** 加载题目（学生侧，不含参考答案）；纯文本作业返回空数组 */
const loadQuestions = async () => {
  const result = await get<WorkQuestionStudentVO[]>(`/works/${route.params.id}/questions`)
  if (result.code === 200) {
    questions.value = result.data || []
    initAnswerMap()
  }
}

/** 依据题目初始化作答映射（多选为数组，其余为字符串） */
const initAnswerMap = () => {
  const map: Record<number, string | string[]> = {}
  for (const q of questions.value) {
    if (q.questionType === 'multiple') {
      map[q.id] = []
    } else if (q.questionType === 'judge') {
      map[q.id] = ''
    } else {
      map[q.id] = ''
    }
  }
  answerMap.value = map
}

/** 将已有作答回填到作答映射（修改提交场景） */
const applySubmissionAnswers = (sub: MySubmission | null) => {
  if (!sub || !Array.isArray(sub.answers) || !questions.value.length) return
  const map: Record<number, string | string[]> = {}
  for (const q of questions.value) {
    const saved = sub.answers.find((a) => a.questionId === q.id)
    if (q.questionType === 'multiple') {
      map[q.id] = Array.isArray(saved?.answer) ? (saved!.answer as unknown[]).map(String) : []
    } else if (q.questionType === 'judge') {
      const v = saved?.answer
      map[q.id] = v === true || v === 'true' ? 'true' : v === false || v === 'false' ? 'false' : ''
    } else {
      map[q.id] = saved?.answer != null ? String(saved.answer) : ''
    }
  }
  answerMap.value = map
}

/** 组装逐题作答为提交载荷 */
const buildAnswers = (): AnswerItem[] => {
  return questions.value.map((q) => {
    const raw = answerMap.value[q.id]
    let answer: unknown = raw
    if (q.questionType === 'judge') {
      answer = raw === 'true' ? true : raw === 'false' ? false : null
    }
    return { questionId: q.id, answer }
  })
}

/** 校验题目作答（仅校验客观题是否作答，主观题允许空） */
const validateAnswers = (): boolean => {
  for (let i = 0; i < questions.value.length; i++) {
    const q = questions.value[i]
    const raw = answerMap.value[q.id]
    const empty =
      raw === null ||
      raw === undefined ||
      raw === '' ||
      (Array.isArray(raw) && raw.length === 0)
    if (q.questionType === 'judge' && empty) {
      ElMessage.warning(`第 ${i + 1} 题：请选择答案`)
      return false
    }
    if (q.questionType !== 'subjective' && q.questionType !== 'extra' && empty) {
      ElMessage.warning(`第 ${i + 1} 题：请完成作答`)
      return false
    }
  }
  return true
}

const loadSubmission = async () => {
  const result = await get<MySubmission[]>('/submissions/student/list', {
    workId: Number(route.params.id),
  })
  if (result.code === 200) {
    mySubmission.value = result.data?.[0] ?? null
  }
}

const withdrawing = ref(false)

const withdrawSubmission = async () => {
  if (!mySubmission.value) return
  // 撤回提交属高危操作：红色警示框 + 身份二次验证（用户可为该操作单独关闭验证）
  const headers = await confirmDangerousOperation({
    title: '撤回提交',
    message: '撤回后本次提交内容与附件将被删除，需要重新提交。确认撤回？',
    confirmText: '确认撤回',
    operationName: '撤回提交',
    operationKey: SensitiveOperationKeys.SUBMISSION_WITHDRAW,
  })
  if (!headers) return
  withdrawing.value = true
  const result = await del<void>(`/submissions/${mySubmission.value.id}`, undefined, undefined, headers)
  withdrawing.value = false
  if (result.code === 200) {
    ElMessage.success('已撤回提交')
    if (editing.value) cancelEditing()
    await loadSubmission()
  } else {
    ElMessage.error(result.message || '撤回失败')
  }
}

const handleExceed = () => {
  ElMessage.warning('单次最多上传 20 个文件')
}

const handleFileChange = (file: UploadFile) => {
  if (file.size && file.size > MAX_ATTACHMENT_SIZE) {
    ElMessage.warning(`文件「${file.name}」超过 50MB，已自动跳过`)
    const idx = newFiles.value.findIndex((f) => f.uid === file.uid)
    if (idx > -1) newFiles.value.splice(idx, 1)
  }
}

const removeNewFile = (file: UploadFile) => {
  newFiles.value = newFiles.value.filter((f) => f.uid !== file.uid)
}

const markRemove = (id: number) => {
  removedIds.value.push(id)
}

const unmarkRemove = (id: number) => {
  removedIds.value = removedIds.value.filter((v) => v !== id)
}

const startEditing = () => {
  if (!mySubmission.value) return
  content.value = mySubmission.value.submissionContent || ''
  existingAttachments.value = [...(mySubmission.value.attachments || [])]
  removedIds.value = []
  newFiles.value = []
  // 回填原逐题作答
  applySubmissionAnswers(mySubmission.value)
  editing.value = true
}

const cancelEditing = () => {
  editing.value = false
  content.value = ''
  newFiles.value = []
  existingAttachments.value = []
  removedIds.value = []
  // 作答复位到题目初始状态
  initAnswerMap()
}

const buildAttachments = (formData: FormData) => {
  for (const fileItem of newFiles.value) {
    if (fileItem.raw) {
      formData.append('attachments', fileItem.raw)
    }
  }
}

const handleSubmit = async () => {
  const hasNewFile = newFiles.value.some((f) => f.raw)
  const hasKeptFile = editing.value && existingAttachments.value.length > removedIds.value.length
  const hasQuestions = questions.value.length > 0
  if (!hasQuestions && !content.value.trim() && !hasNewFile && !hasKeptFile) {
    ElMessage.warning('请填写提交内容或上传附件')
    return
  }
  // 含题目的作业：校验逐题作答
  if (hasQuestions && !validateAnswers()) {
    return
  }
  submitting.value = true
  try {
    let result
    if (editing.value && mySubmission.value) {
      const formData = new FormData()
      formData.append('submissionContent', content.value)
      if (hasQuestions) {
        formData.append('answers', JSON.stringify(buildAnswers()))
      }
      buildAttachments(formData)
      for (const id of removedIds.value) {
        formData.append('removedAttachmentIds', String(id))
      }
      result = await putForm(`/submissions/${mySubmission.value.id}`, formData)
    } else {
      const formData = new FormData()
      formData.append('workId', String(route.params.id))
      if (content.value.trim()) {
        formData.append('submissionContent', content.value)
      }
      if (hasQuestions) {
        formData.append('answers', JSON.stringify(buildAnswers()))
      }
      buildAttachments(formData)
      result = await postForm('/submissions', formData)
    }
    if (result.code === 200) {
      ElMessage.success(result.message || (editing.value ? '更新成功' : '提交成功'))
      cancelEditing()
      await loadSubmission()
    } else {
      ElMessage.error(result.message)
    }
  } catch {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

const downloadFile = async (att: AttachmentInfo) => {
  try {
    const response = await instance.get('/files/download', {
      params: { path: att.filePath, fileName: att.fileName },
      responseType: 'blob',
    })
    const url = URL.createObjectURL(response.data as Blob)
    const link = document.createElement('a')
    link.href = url
    link.download = att.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('附件下载失败，请重试')
  }
}

const goBack = () => {
  if (work.value?.classId) {
    router.push(`/student/course/${work.value.classId}`)
  } else {
    router.push('/courses/student')
  }
}

onMounted(async () => {
  await loadWork()
  await loadQuestions()
  await loadSubmission()
})
</script>

<style scoped>
.student-work-page {
  padding-bottom: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  padding: 8px;
}

.header-left h2 {
  font-size: 24px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.status-tag {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.active {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.status-tag.expired {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.status-tag.pending {
  background: rgba(156, 163, 175, 0.2);
  color: #9ca3af;
}

.work-info-card {
  margin-bottom: 20px;
}

.info-section {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.info-item svg {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
}

.info-item .value {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
  font-weight: 500;
}

.info-item .value.overdue {
  color: #ef4444;
}

.description-section h4,
.attachments-section h4,
.content-section h4,
.comment-section h4 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
}

.description-section p {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
  line-height: 1.6;
  white-space: pre-wrap;
}

.attachments-section {
  margin-top: 16px;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.att-name {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
  font-weight: 500;
  word-break: break-all;
}

.att-size {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
  font-size: 12px;
  white-space: nowrap;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
}

.submitted-meta,
.graded-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
  margin-bottom: 16px;
}

.submitted-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.late-badge {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.score-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
  color: #fbbf24;
}

.score-row svg {
  align-self: center;
}

.score-value {
  font-size: 32px;
  font-weight: 700;
}

.score-total {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.score-row .late-badge {
  align-self: center;
  margin-left: 12px;
}

.content-text,
.comment-section p {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
  line-height: 1.6;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 8px;
  padding: 12px 16px;
  white-space: pre-wrap;
  word-break: break-all;
}

.comment-section {
  margin-top: 16px;
}

.edit-alert {
  margin-bottom: 16px;
}

.upload-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
  margin-top: 8px;
}

.existing-attachment-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.existing-attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
}

/* ===== 题目作答区 ===== */
.questions-answer-block {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 8px;
}

.questions-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
  margin: 0;
}

.answer-question-item {
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 10px;
  padding: 14px 16px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.aq-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.aq-no {
  font-size: 13px;
  font-weight: 600;
  color: #667eea;
}

.aq-type {
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 10px;
  background: rgba(102, 126, 234, 0.14);
  color: #667eea;
}

.aq-score {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.aq-content {
  font-size: 14px;
  line-height: 1.6;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}

.aq-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
}

.aq-option {
  margin-right: 0;
  height: auto;
  white-space: normal;
}

.aq-option :deep(.el-radio__label),
.aq-option :deep(.el-checkbox__label) {
  white-space: normal;
  line-height: 1.5;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
}

.opt-key {
  font-weight: 600;
}

.aq-input {
  width: 100%;
}

/* ===== 逐题成绩展示 ===== */
.questions-result-block {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.questions-result-block h4 {
  font-size: 14px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
  margin: 0;
}

.result-question-item {
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-left: 3px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.2);
  border-radius: 8px;
  padding: 12px 14px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-question-item.is-correct {
  border-left-color: #22c55e;
}

.result-question-item.is-wrong {
  border-left-color: #ef4444;
}

.rq-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.rq-no {
  font-size: 13px;
  font-weight: 600;
  color: #667eea;
}

.rq-type {
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 10px;
  background: rgba(102, 126, 234, 0.14);
  color: #667eea;
}

.rq-score {
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.75);
  font-weight: 500;
}

.rq-flag {
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 10px;
  margin-left: auto;
}

.rq-flag.correct {
  background: rgba(34, 197, 94, 0.16);
  color: #22c55e;
}

.rq-flag.wrong {
  background: rgba(239, 68, 68, 0.16);
  color: #ef4444;
}

.rq-content {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}

.rq-answer,
.rq-correct,
.rq-analysis,
.rq-teacher-comment {
  font-size: 13px;
  line-height: 1.6;
}

.rq-label {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.rq-value {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
  word-break: break-word;
}

.rq-value.correct-text {
  color: #22c55e;
  font-weight: 500;
}

/* 统一的附件条目卡片（已上传附件与新选文件共用） */
.attachment-card {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 8px;
  padding: 8px 12px;
  transition: border-color 0.2s, background 0.2s;
}

.attachment-card:hover {
  border-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.22);
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06);
}

.attachment-card .att-icon {
  flex-shrink: 0;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
}

.attachment-card .att-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  word-break: keep-all;
  font-weight: 500;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
}

.attachment-card .att-size {
  flex-shrink: 0;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.attachment-card .att-action {
  flex-shrink: 0;
  margin-left: 0;
}

.attachment-card .att-download {
  flex-shrink: 0;
}

.existing-attachment-item.removed {
  opacity: 0.45;
  text-decoration: line-through;
}

/* Element Plus 上传组件：新选文件也按卡片展示，去掉默认列表观感 */
:deep(.new-upload) {
  width: 100%;
}

:deep(.new-upload .el-upload-list) {
  margin-top: 8px;
  padding: 0;
}

:deep(.new-upload .el-upload-list__item) {
  margin-bottom: 8px;
  background: transparent;
}

:deep(.new-upload .el-upload-list__item:hover) {
  background: transparent;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

/* 统一“取消 / 提交作业”按钮高度，避免任何全局样式继承导致的不齐 */
.form-actions .el-button {
  height: 32px;
  line-height: 1;
  box-sizing: border-box;
}

.locked-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.locked-state p {
  margin-top: 12px;
  font-size: 14px;
}

@media (max-width: 768px) {
  .info-section {
    flex-direction: column;
    gap: 12px;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions .el-button {
    width: 100%;
  }

  .upload-trigger-btn {
    width: 100%;
    min-height: 42px;
  }
}
</style>
