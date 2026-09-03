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
import { ElMessage, ElMessageBox } from 'element-plus'
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
  X,
  XCircle,
} from '@lucide/vue'

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
  attachments?: AttachmentInfo[]
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

const MAX_ATTACHMENT_SIZE = 50 * 1024 * 1024

const isExpired = computed(() => {
  if (!work.value) return false
  return new Date() > new Date(work.value.deadline)
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
  try {
    await ElMessageBox.confirm(
      '撤回后本次提交内容与附件将被删除，需要重新提交。确认撤回？',
      '撤回提交',
      {
        confirmButtonText: '确认撤回',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'danger-warning-message-box',
      },
    )
  } catch {
    return
  }
  withdrawing.value = true
  const result = await del<void>(`/submissions/${mySubmission.value.id}`)
  withdrawing.value = false
  if (result.code === 200) {
    ElMessage.success('已撤回提交')
    if (editing.value) cancelEditing()
    await loadSubmission()
  } else {
    ElMessage.error(result.message || '撤回失败')
  }
}

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const formatSize = (size?: number) => {
  if (!size) return ''
  if (size < 1024) return `${size}B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)}KB`
  return `${(size / (1024 * 1024)).toFixed(1)}MB`
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
  editing.value = true
}

const cancelEditing = () => {
  editing.value = false
  content.value = ''
  newFiles.value = []
  existingAttachments.value = []
  removedIds.value = []
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
  if (!content.value.trim() && !hasNewFile && !hasKeptFile) {
    ElMessage.warning('请填写提交内容或上传附件')
    return
  }
  submitting.value = true
  try {
    let result
    if (editing.value && mySubmission.value) {
      const formData = new FormData()
      formData.append('submissionContent', content.value)
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
    router.push('/student/courses')
  }
}

onMounted(async () => {
  await loadWork()
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
