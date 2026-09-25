<template>
  <div class="question-editor">
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <span class="toolbar-title">题目列表</span>
        <span class="toolbar-count">{{ drafts.length }} 题 · 合计 {{ totalScore }} 分</span>
      </div>
      <div class="toolbar-right">
        <span class="add-label">新增</span>
        <el-button
          v-for="t in addTypes"
          :key="t.value"
          size="small"
          plain
          class="add-btn"
          @click="addQuestion(t.value)"
        >
          {{ t.label }}
        </el-button>
      </div>
    </div>

    <div v-if="drafts.length === 0" class="editor-empty">
      <FileQuestion :size="28" />
      <p>暂无题目，点击上方按钮添加</p>
      <p class="editor-empty-tip">不添加题目即为纯文本作业</p>
    </div>

    <div v-else class="question-list">
      <div v-for="(q, index) in drafts" :key="q.uid" class="question-item">
        <div class="question-head">
          <span class="question-no">第 {{ index + 1 }} 题</span>
          <el-select v-model="q.questionType" size="small" class="type-select" @change="onTypeChange(q)">
            <el-option
              v-for="t in allTypes"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
          <span class="auto-badge" :class="isObjectiveType(q.questionType) ? 'auto' : 'manual'">
            {{ isObjectiveType(q.questionType) ? '自动评判' : '手动评分' }}
          </span>
          <span class="score-field">
            <span class="score-label">分值</span>
            <el-input-number v-model="q.score" :min="0" :max="1000" :precision="0" size="small" controls-position="right" />
          </span>
          <div class="head-actions">
            <button class="icon-btn" :disabled="index === 0" title="上移" @click="move(index, -1)">
              <ChevronUp :size="15" />
            </button>
            <button class="icon-btn" :disabled="index === drafts.length - 1" title="下移" @click="move(index, 1)">
              <ChevronDown :size="15" />
            </button>
            <button class="icon-btn danger" title="删除" @click="removeQuestion(index)">
              <Trash2 :size="15" />
            </button>
          </div>
        </div>

        <el-input
          v-model="q.content"
          type="textarea"
          :rows="2"
          maxlength="4096"
          placeholder="请输入题干"
          class="content-input"
        />

        <!-- 选项区（单选/多选） -->
        <div v-if="q.questionType === 'single' || q.questionType === 'multiple'" class="options-block">
          <div v-for="(opt, oi) in q.options" :key="oi" class="option-row">
            <el-radio
              v-if="q.questionType === 'single'"
              v-model="q.singleAnswer"
              :value="opt.key"
              class="option-mark"
            >
              <span class="option-key">{{ opt.key }}</span>
            </el-radio>
            <el-checkbox
              v-else
              v-model="q.multipleAnswer"
              :value="opt.key"
              class="option-mark"
            >
              <span class="option-key">{{ opt.key }}</span>
            </el-checkbox>
            <el-input v-model="opt.text" size="small" placeholder="选项内容" class="option-input" />
            <button
              v-if="q.options.length > 2"
              class="icon-btn danger small"
              title="删除选项"
              @click="removeOption(q, oi)"
            >
              <X :size="13" />
            </button>
          </div>
          <el-button v-if="q.options.length < 8" size="small" text class="add-option-btn" @click="addOption(q)">
            <Plus :size="14" /> 添加选项
          </el-button>
          <p class="hint">勾选/点选左侧标记设置参考答案（{{ q.questionType === 'single' ? '单选' : '多选可多选' }}）</p>
        </div>

        <!-- 判断题 -->
        <div v-else-if="q.questionType === 'judge'" class="judge-block">
          <span class="block-label">参考答案</span>
          <el-radio-group v-model="q.judgeAnswer" size="small">
            <el-radio-button :value="true">正确</el-radio-button>
            <el-radio-button :value="false">错误</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 填空题 -->
        <div v-else-if="q.questionType === 'fill'" class="fill-block">
          <span class="block-label">可接受答案（任一命中即得分）</span>
          <div v-for="(_, ai) in q.fillAnswers" :key="ai" class="fill-row">
            <el-input v-model="q.fillAnswers[ai]" size="small" placeholder="填写一个可接受的答案" class="fill-input" />
            <button
              v-if="q.fillAnswers.length > 1"
              class="icon-btn danger small"
              title="删除"
              @click="removeFill(q, ai)"
            >
              <X :size="13" />
            </button>
          </div>
          <el-button size="small" text class="add-option-btn" @click="q.fillAnswers.push('')">
            <Plus :size="14" /> 添加答案
          </el-button>
        </div>

        <!-- 主观题 / 附加题 -->
        <div v-else class="subjective-block">
          <span class="hint subjective-hint">
            {{ q.questionType === 'extra' ? '附加题属于主观题，仅由老师手动评分' : '主观题由老师手动评分' }}
          </span>
        </div>

        <el-input
          v-model="q.analysis"
          size="small"
          maxlength="2048"
          placeholder="答案解析（选填，批改后学生可见）"
          class="analysis-input"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ChevronUp, ChevronDown, Trash2, X, Plus, FileQuestion } from '@lucide/vue'
import type { QuestionType, WorkQuestionVO } from '@/types/work'
import { isObjectiveType } from '@/types/work'
import {
  appendOption,
  createDraft,
  defaultOptions,
  draftFromVO,
  nextUid,
  type DraftQuestion,
} from './questionDraft'

const props = defineProps<{
  /** 已保存的题目（编辑回填用） */
  modelValue?: WorkQuestionVO[]
}>()

const drafts = ref<DraftQuestion[]>([])

// 外部传入已保存题目时回填（组件挂载或题目更新时）
watch(
  () => props.modelValue,
  (list) => {
    if (Array.isArray(list) && list.length) {
      drafts.value = list.map(draftFromVO)
    }
  },
  { immediate: true },
)

const allTypes: { value: QuestionType; label: string }[] = [
  { value: 'single', label: '单选题' },
  { value: 'multiple', label: '多选题' },
  { value: 'judge', label: '判断题' },
  { value: 'fill', label: '填空题' },
  { value: 'subjective', label: '主观题' },
  { value: 'extra', label: '附加题' },
]

const addTypes: { value: QuestionType; label: string }[] = [
  { value: 'single', label: '单选' },
  { value: 'multiple', label: '多选' },
  { value: 'judge', label: '判断' },
  { value: 'fill', label: '填空' },
  { value: 'subjective', label: '主观' },
  { value: 'extra', label: '附加' },
]

const totalScore = ref(0)
watch(
  drafts,
  () => {
    totalScore.value = drafts.value.reduce((sum, q) => sum + (q.score || 0), 0)
  },
  { deep: true, immediate: true },
)

const emit = defineEmits<{
  /** 题目集合变化时通知父组件（用于校验/提交） */
  (e: 'change', drafts: DraftQuestion[]): void
}>()

watch(
  drafts,
  () => emit('change', drafts.value),
  { deep: true },
)

const addQuestion = (type: QuestionType) => {
  drafts.value.push(createDraft(type))
}

const removeQuestion = (index: number) => {
  drafts.value.splice(index, 1)
}

const move = (index: number, delta: number) => {
  const target = index + delta
  if (target < 0 || target >= drafts.value.length) return
  const [item] = drafts.value.splice(index, 1)
  drafts.value.splice(target, 0, item)
}

const onTypeChange = (q: DraftQuestion) => {
  // 切换题型时重置与题型相关的字段，避免残留脏数据
  if (q.questionType === 'single' || q.questionType === 'multiple') {
    if (!q.options.length) q.options = defaultOptions()
    q.singleAnswer = ''
    q.multipleAnswer = []
  }
}

const addOption = (q: DraftQuestion) => {
  q.options = appendOption(q.options)
}

const removeOption = (q: DraftQuestion, oi: number) => {
  const removed = q.options[oi]
  q.options.splice(oi, 1)
  if (q.singleAnswer === removed.key) q.singleAnswer = ''
  q.multipleAnswer = q.multipleAnswer.filter((k) => k !== removed.key)
}

const removeFill = (q: DraftQuestion, ai: number) => {
  q.fillAnswers.splice(ai, 1)
}

defineExpose({ drafts, nextUid })
</script>

<style scoped>
.question-editor {
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 10px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.02);
  padding: 14px 16px;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.toolbar-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.toolbar-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
}

.toolbar-count {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.add-label {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
  margin-right: 2px;
}

.add-btn {
  height: 28px;
}

.editor-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 24px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.editor-empty p {
  font-size: 13px;
  margin: 0;
}

.editor-empty-tip {
  font-size: 12px !important;
  opacity: 0.8;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.question-item {
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 8px;
  padding: 12px 14px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.question-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.question-no {
  font-size: 13px;
  font-weight: 600;
  color: #667eea;
  white-space: nowrap;
}

.type-select {
  width: 110px;
}

.auto-badge {
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 11px;
}

.auto-badge.auto {
  background: rgba(34, 197, 94, 0.16);
  color: #22c55e;
}

.auto-badge.manual {
  background: rgba(245, 158, 11, 0.16);
  color: #f59e0b;
}

.score-field {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
}

.score-label {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.score-field :deep(.el-input-number) {
  width: 90px;
}

.head-actions {
  display: flex;
  gap: 4px;
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.12);
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04);
  border-radius: 6px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-btn.small {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

.icon-btn:hover:not(:disabled) {
  border-color: rgba(102, 126, 234, 0.5);
  color: #667eea;
}

.icon-btn.danger:hover:not(:disabled) {
  border-color: rgba(239, 68, 68, 0.5);
  color: #ef4444;
}

.icon-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.content-input :deep(.el-textarea__inner) {
  font-size: 14px;
}

.options-block,
.fill-block,
.judge-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-row,
.fill-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-mark {
  flex-shrink: 0;
  margin-right: 0;
}

.option-key {
  font-weight: 600;
}

.option-input,
.fill-input {
  flex: 1;
  min-width: 0;
}

.block-label {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.add-option-btn {
  align-self: flex-start;
  color: #667eea;
}

.hint {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
  margin: 0;
}

.subjective-hint {
  font-size: 13px;
}

.analysis-input :deep(.el-input__wrapper) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
}

@media (max-width: 768px) {
  .type-select {
    width: 96px;
  }

  .score-field {
    margin-left: 0;
  }

  .question-head {
    gap: 8px;
  }
}
</style>
