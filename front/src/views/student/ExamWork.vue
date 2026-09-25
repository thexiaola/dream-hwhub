<template>
  <div class="exam-page" :class="{ 'is-fullscreen': isFullscreen }">
    <!-- 进入前说明 -->
    <div v-if="!entered" class="exam-gate" v-loading="entering">
      <div class="gate-card">
        <h2>{{ exam?.title || '考试' }}</h2>
        <p v-if="exam?.description" class="gate-desc">{{ exam.description }}</p>
        <div class="gate-meta">
          <span>总分：{{ exam?.totalScore ?? '—' }}</span>
          <span v-if="exam?.durationMinutes">时长：{{ exam.durationMinutes }} 分钟</span>
          <span v-else>不限时</span>
          <span>题目：{{ exam?.questions?.length ?? 0 }} 题</span>
        </div>
        <div v-if="antiCheatActive" class="gate-anti">
          <div class="gate-anti-title"><ShieldAlert :size="16" /> 本场考试已开启反作弊</div>
          <ul>
            <li v-if="exam?.antiCheatFont">字体映射：复制题干得到的文字是乱码</li>
            <li v-if="exam?.antiCheatFullscreen">强制全屏：离开全屏会被记为违规</li>
            <li v-if="exam?.antiCheatDetectLeave">切屏检测：切换标签页/窗口会被记为违规</li>
            <li v-if="exam?.antiCheatNoCopy">禁止复制粘贴</li>
            <li v-if="exam?.antiCheatMaxViolations">
              违规达到 {{ exam.antiCheatMaxViolations }} 次将自动交卷
            </li>
          </ul>
        </div>
        <el-button type="primary" size="large" @click="startExam" :loading="entering">
          开始答题
        </el-button>
      </div>
    </div>

    <!-- 答题界面 -->
    <div v-else class="exam-body">
      <div class="exam-topbar">
        <div class="topbar-left">
          <span class="exam-title">{{ exam?.title }}</span>
        </div>
        <div class="topbar-right">
          <span v-if="remainingText" class="timer" :class="{ urgent: remainingSeconds <= 60 }">
            <Timer :size="16" /> {{ remainingText }}
          </span>
          <span class="violation" :class="{ warn: (exam?.violationCount ?? 0) > 0 }">
            <ShieldAlert :size="16" /> 违规 {{ exam?.violationCount ?? 0 }}
            <template v-if="exam?.antiCheatMaxViolations">/ {{ exam.antiCheatMaxViolations }}</template>
          </span>
          <el-button type="primary" :loading="submitting" @click="confirmSubmit">交卷</el-button>
        </div>
      </div>

      <div class="exam-questions" :class="{ 'font-scrambled': exam?.antiCheatFont }">
        <div v-for="(q, qi) in exam?.questions || []" :key="q.id" class="exam-question">
          <div class="eq-head">
            <span class="eq-no">第 {{ qi + 1 }} 题</span>
            <span class="eq-type">{{ q.questionTypeName || typeLabel(q.questionType) }}</span>
            <span class="eq-score">（{{ q.score }} 分）</span>
          </div>
          <p class="eq-content">{{ q.content }}</p>

          <el-radio-group
            v-if="q.questionType === 'single'"
            :model-value="strAnswer(q.id)"
            class="eq-options"
            @update:model-value="(v: string | number | boolean | undefined) => setStrAnswer(q.id, v)"
          >
            <el-radio v-for="opt in q.options" :key="opt.key" :value="opt.key" class="eq-option">
              <span class="opt-key">{{ opt.key }}.</span> {{ opt.text }}
            </el-radio>
          </el-radio-group>

          <el-checkbox-group
            v-else-if="q.questionType === 'multiple'"
            :model-value="arrAnswer(q.id)"
            class="eq-options"
            @update:model-value="(v: (string | number)[]) => setArrAnswer(q.id, v)"
          >
            <el-checkbox v-for="opt in q.options" :key="opt.key" :value="opt.key" class="eq-option">
              <span class="opt-key">{{ opt.key }}.</span> {{ opt.text }}
            </el-checkbox>
          </el-checkbox-group>

          <el-radio-group
            v-else-if="q.questionType === 'judge'"
            :model-value="strAnswer(q.id)"
            class="eq-options"
            @update:model-value="(v: string | number | boolean | undefined) => setStrAnswer(q.id, v)"
          >
            <el-radio value="true" class="eq-option">正确</el-radio>
            <el-radio value="false" class="eq-option">错误</el-radio>
          </el-radio-group>

          <el-input
            v-else-if="q.questionType === 'fill'"
            :model-value="strAnswer(q.id)"
            placeholder="请输入答案"
            class="eq-input"
            @update:model-value="(v: string) => setStrAnswer(q.id, v)"
          />

          <el-input
            v-else
            :model-value="strAnswer(q.id)"
            type="textarea"
            :rows="4"
            maxlength="4096"
            show-word-limit
            placeholder="请输入作答内容"
            class="eq-input"
            @update:model-value="(v: string) => setStrAnswer(q.id, v)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShieldAlert, Timer } from '@lucide/vue'
import { post, postForm, put } from '@/utils/http'
import { QUESTION_TYPE_LABEL, type AnswerItem, type ExamEnterResponse } from '@/types/work'

const route = useRoute()
const router = useRouter()
const workId = Number(route.params.id)

const exam = ref<ExamEnterResponse | null>(null)
const entering = ref(false)
const entered = ref(false)
const submitting = ref(false)
const isFullscreen = ref(false)

/** questionId -> 作答值 */
const answerMap = ref<Record<number, string | string[]>>({})
const remainingSeconds = ref(0)

const antiCheatActive = computed(() => Boolean(exam.value?.antiCheatEnabled))
const remainingText = computed(() => {
  if (!exam.value || exam.value.remainingSeconds == null) return ''
  const s = Math.max(0, remainingSeconds.value)
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
})

const typeLabel = (t: string) => QUESTION_TYPE_LABEL[t as keyof typeof QUESTION_TYPE_LABEL] || t

const strAnswer = (qid: number): string => {
  const v = answerMap.value[qid]
  return typeof v === 'string' ? v : ''
}
const arrAnswer = (qid: number): string[] => {
  const v = answerMap.value[qid]
  return Array.isArray(v) ? v : []
}
const setStrAnswer = (qid: number, v: string | number | boolean | undefined) => {
  answerMap.value[qid] = v == null ? '' : String(v)
  scheduleDraftSave()
}
const setArrAnswer = (qid: number, v: (string | number)[]) => {
  answerMap.value[qid] = (v || []).map(String)
  scheduleDraftSave()
}

const initAnswerMap = () => {
  const map: Record<number, string | string[]> = {}
  for (const q of exam.value?.questions || []) {
    map[q.id] = q.questionType === 'multiple' ? [] : ''
  }
  answerMap.value = map
}

/** 回填草稿作答 */
const applyDraft = () => {
  const draft = exam.value?.draftAnswers
  if (!draft) return
  try {
    const arr = JSON.parse(draft) as { questionId: number; answer: unknown }[]
    for (const item of arr) {
      const q = (exam.value?.questions || []).find((x) => x.id === item.questionId)
      if (!q) continue
      if (q.questionType === 'multiple') {
        answerMap.value[q.id] = Array.isArray(item.answer) ? (item.answer as unknown[]).map(String) : []
      } else if (q.questionType === 'judge') {
        const v = item.answer
        answerMap.value[q.id] = v === true || v === 'true' ? 'true' : v === false || v === 'false' ? 'false' : ''
      } else {
        answerMap.value[q.id] = item.answer != null ? String(item.answer) : ''
      }
    }
  } catch {
    /* 草稿损坏时忽略 */
  }
}

// ===== 进入考试 =====
const loadExam = async () => {
  entering.value = true
  try {
    const result = await post<ExamEnterResponse>(`/exams/${workId}/enter`)
    if (result.code === 200 && result.data) {
      exam.value = result.data
      if (result.data.submitted) {
        ElMessage.info('你已交卷，正在查看成绩')
        router.replace(`/student/work/${workId}`)
        return
      }
      initAnswerMap()
      applyDraft()
      remainingSeconds.value = result.data.remainingSeconds ?? 0
    } else {
      ElMessage.error(result.message || '加载考试失败')
      router.replace(`/student/work/${workId}`)
    }
  } finally {
    entering.value = false
  }
}

const startExam = async () => {
  // 字体映射：先把打乱字体注册为 @font-face，再进入答题
  loadAntiCheatFont()
  // 强制全屏：必须在用户手势中请求
  if (exam.value?.antiCheatFullscreen) {
    try {
      await document.documentElement.requestFullscreen()
      isFullscreen.value = true
    } catch {
      ElMessage.warning('未能进入全屏，请允许全屏后继续；离开全屏会被记为违规')
    }
  }
  entered.value = true
  bindAntiCheat()
  startTimer()
}

/**
 * 加载反作弊打乱字体：把 /api/exams/font/{seed}.woff2 注册为 @font-face 家族
 * ExamAntiCheatFont。字体仅把显示码点映射到真实字形，本身不含明文。
 */
const fontStyleEl = ref<HTMLStyleElement | null>(null)
const loadAntiCheatFont = () => {
  const seed = exam.value?.fontSeed
  if (!exam.value?.antiCheatFont || seed == null) return
  // 字体文件需携带 JWT 才能访问，用 fetch 取回后转 blob URL 注入 @font-face
  void fetchFontAndInject(seed)
}

const fetchFontAndInject = async (seed: number) => {
  try {
    const instance = (await import('@/utils/http')).default
    const res = await instance.get(`/exams/font/${seed}.woff2`, { responseType: 'blob' })
    const url = URL.createObjectURL(res.data as Blob)
    const style = document.createElement('style')
    style.textContent = `@font-face { font-family: 'ExamAntiCheatFont'; src: url('${url}') format('woff2'); font-display: block; }`
    document.head.appendChild(style)
    fontStyleEl.value = style
  } catch {
    ElMessage.warning('反作弊字体加载失败，将使用系统字体显示')
  }
}

// ===== 限时 =====
let timerId: number | null = null
let autoSubmitted = false

const startTimer = () => {
  if (exam.value?.remainingSeconds == null) return
  timerId = window.setInterval(() => {
    remainingSeconds.value -= 1
    if (remainingSeconds.value <= 0) {
      remainingSeconds.value = 0
      stopTimer()
      if (!autoSubmitted) {
        autoSubmitted = true
        ElMessage.warning('考试时间已到，正在自动交卷')
        doSubmit()
      }
    }
  }, 1000)
}
const stopTimer = () => {
  if (timerId !== null) {
    clearInterval(timerId)
    timerId = null
  }
}

// ===== 反作弊 =====
const reportViolation = async (type: string, detail?: string) => {
  if (!entered.value || submitting.value) return
  try {
    const res = await post<boolean>(`/exams/${workId}/violations`, { type, detail })
    if (res.code === 200) {
      if (exam.value) {
        exam.value.violationCount = (exam.value.violationCount ?? 0) + 1
      }
      if (res.data === true && !autoSubmitted) {
        autoSubmitted = true
        ElMessage.error('违规次数已达上限，正在自动交卷')
        doSubmit()
      }
    }
  } catch {
    /* 违规上报失败不阻断作答 */
  }
}

const onFullscreenChange = () => {
  const fs = Boolean(document.fullscreenElement)
  isFullscreen.value = fs
  if (!fs && exam.value?.antiCheatFullscreen && entered.value && !submitting.value) {
    reportLeaveViolation('fullscreen_exit', '退出全屏')
    // 提示并请求重新进入全屏
    ElMessageBox.alert('已检测到你退出了全屏，本次已记为违规。点击确定重新进入全屏。', '反作弊提醒', {
      confirmButtonText: '重新进入全屏',
      showClose: false,
    })
      .then(() => reenterFullscreen())
      .catch(() => reenterFullscreen())
  }
}

const reenterFullscreen = () => {
  document.documentElement.requestFullscreen?.().catch(() => {
    /* 用户拒绝则保持退出全屏状态，后续离开仍会记违规 */
  })
}

// 「离开」类违规的合并窗口：一次真实的离开动作（切标签页/切窗口/退出全屏）会同时触发
// visibilitychange 与 blur 等多个事件，若各自计数会把 1 次离开算成多次，
// 导致违规上限被过早触发。这里用时间窗合并，保证同一动作只计 1 次。
const LEAVE_MERGE_WINDOW_MS = 2000
let lastLeaveAt = 0
const reportLeaveViolation = (type: string, detail?: string) => {
  const now = Date.now()
  if (now - lastLeaveAt < LEAVE_MERGE_WINDOW_MS) {
    return
  }
  lastLeaveAt = now
  reportViolation(type, detail)
}

const onVisibilityChange = () => {
  if (document.hidden && exam.value?.antiCheatDetectLeave && entered.value && !submitting.value) {
    reportLeaveViolation('visibility_hidden', '切换到其他标签页')
  }
}

const onWindowBlur = () => {
  // 仅在切屏检测开启、且未在全屏态（全屏退出由 fullscreenchange 单独记录）时记录失焦
  if (exam.value?.antiCheatDetectLeave && entered.value && !submitting.value && !document.fullscreenElement) {
    reportLeaveViolation('window_blur', '窗口失焦')
  }
}

const onCopyOrCut = (e: Event) => {
  if (!exam.value?.antiCheatNoCopy || !entered.value) return
  e.preventDefault()
  const type = e.type === 'cut' ? 'cut' : 'copy'
  reportViolation(type, '复制/剪切操作')
}
const onPaste = (e: Event) => {
  if (!exam.value?.antiCheatNoCopy || !entered.value) return
  e.preventDefault()
  reportViolation('paste', '粘贴操作')
}
const onContextMenu = (e: Event) => {
  if (!exam.value?.antiCheatNoCopy || !entered.value) return
  e.preventDefault()
}

const bindAntiCheat = () => {
  document.addEventListener('fullscreenchange', onFullscreenChange)
  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('blur', onWindowBlur)
  document.addEventListener('copy', onCopyOrCut)
  document.addEventListener('cut', onCopyOrCut)
  document.addEventListener('paste', onPaste)
  document.addEventListener('contextmenu', onContextMenu)
}

const unbindAntiCheat = () => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  window.removeEventListener('blur', onWindowBlur)
  document.removeEventListener('copy', onCopyOrCut)
  document.removeEventListener('cut', onCopyOrCut)
  document.removeEventListener('paste', onPaste)
  document.removeEventListener('contextmenu', onContextMenu)
}

// ===== 草稿自动保存 =====
let draftTimer: number | null = null
const scheduleDraftSave = () => {
  if (!entered.value) return
  if (draftTimer !== null) clearTimeout(draftTimer)
  draftTimer = window.setTimeout(() => {
    void saveDraft()
  }, 1500)
}

/** 草稿以 JSON 字符串作为字段提交，后端原样保存 */
const saveDraft = async () => {
  try {
    await put(`/exams/${workId}/draft`, { draft: JSON.stringify(buildAnswers()) })
  } catch {
    /* 忽略草稿保存失败 */
  }
}

// ===== 交卷 =====
const buildAnswers = (): AnswerItem[] => {
  return (exam.value?.questions || []).map((q) => {
    const raw = answerMap.value[q.id]
    let answer: unknown = raw
    if (q.questionType === 'judge') {
      answer = raw === 'true' ? true : raw === 'false' ? false : null
    }
    return { questionId: q.id, answer }
  })
}

const confirmSubmit = async () => {
  try {
    await ElMessageBox.confirm('确认交卷？交卷后不能再修改答案。', '交卷确认', {
      confirmButtonText: '确认交卷',
      cancelButtonText: '继续作答',
      type: 'warning',
    })
  } catch {
    return
  }
  doSubmit()
}

const doSubmit = async () => {
  if (submitting.value) return
  submitting.value = true
  stopTimer()
  try {
    const formData = new FormData()
    formData.append('workId', String(workId))
    formData.append('answers', JSON.stringify(buildAnswers()))
    const result = await postForm(`/submissions`, formData)
    if (result.code === 200) {
      exitFullscreen()
      ElMessage.success('交卷成功')
      router.push(`/student/work/${workId}`)
    } else {
      ElMessage.error(result.message || '交卷失败')
      submitting.value = false
    }
  } catch {
    ElMessage.error('交卷失败，请重试')
    submitting.value = false
  }
}

const exitFullscreen = () => {
  if (document.fullscreenElement) {
    document.exitFullscreen?.().catch(() => {
      /* ignore */
    })
  }
}

onMounted(loadExam)

onBeforeUnmount(() => {
  stopTimer()
  unbindAntiCheat()
  if (draftTimer !== null) clearTimeout(draftTimer)
  if (fontStyleEl.value) {
    document.head.removeChild(fontStyleEl.value)
    fontStyleEl.value = null
  }
  exitFullscreen()
})
</script>

<style scoped>
.exam-page {
  padding-bottom: 32px;
}

/* 进入前说明卡片 */
.exam-gate {
  display: flex;
  justify-content: center;
  padding: 48px 16px;
}

.gate-card {
  width: 100%;
  max-width: 560px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 16px;
  padding: 28px 32px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.gate-card h2 {
  margin: 0;
  font-size: 22px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.gate-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
  white-space: pre-wrap;
}

.gate-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.gate-anti {
  border: 1px solid rgba(245, 158, 11, 0.3);
  background: rgba(245, 158, 11, 0.08);
  border-radius: 10px;
  padding: 12px 16px;
}

.gate-anti-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #f59e0b;
  margin-bottom: 8px;
}

.gate-anti ul {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  line-height: 1.8;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.75);
}

/* 答题界面 */
.exam-topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 18px;
  margin-bottom: 16px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 10px;
  backdrop-filter: blur(8px);
}

.exam-title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.timer,
.violation {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-variant-numeric: tabular-nums;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
}

.timer.urgent {
  color: #ef4444;
  font-weight: 600;
}

.violation.warn {
  color: #f59e0b;
  font-weight: 600;
}

.exam-questions {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.exam-question {
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 10px;
  padding: 16px 18px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.eq-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.eq-no {
  font-size: 13px;
  font-weight: 600;
  color: #667eea;
}

.eq-type {
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 10px;
  background: rgba(102, 126, 234, 0.14);
  color: #667eea;
}

.eq-score {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.eq-content {
  font-size: 15px;
  line-height: 1.7;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.92);
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}

.eq-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
}

.eq-option {
  margin-right: 0;
  height: auto;
  white-space: normal;
}

.eq-option :deep(.el-radio__label),
.eq-option :deep(.el-checkbox__label) {
  white-space: normal;
  line-height: 1.5;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
}

.opt-key {
  font-weight: 600;
}

.eq-input {
  width: 100%;
}

/* 字体映射：仅作用于题干与选项文本，控件本身仍用系统字体，保证可读 */
.font-scrambled .eq-content,
.font-scrambled .eq-option :deep(.el-radio__label),
.font-scrambled .eq-option :deep(.el-checkbox__label) {
  font-family: 'ExamAntiCheatFont', system-ui, sans-serif;
}

@media (max-width: 768px) {
  .exam-topbar {
    flex-wrap: wrap;
  }

  .topbar-right {
    gap: 10px;
  }
}
</style>
