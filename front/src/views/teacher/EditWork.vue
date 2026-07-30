<template>
  <div class="create-work-page">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" class="page-back-btn" text>
          <ArrowLeft :size="18" />
        </el-button>
        <div>
          <h2>编辑作业</h2>
          <p class="subtitle">修改作业信息</p>
        </div>
      </div>
      <div class="header-right">
        <el-button @click="goBack" class="page-cancel-btn">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="loading">保存</el-button>
      </div>
    </div>
    <el-card class="content-card dark-dialog" v-loading="pageLoading">
      <el-form :model="form" :rules="rules" ref="formRef" class="create-form" label-width="100px">
        <el-form-item label="作业标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入作业标题"
            class="form-input"
          />
        </el-form-item>
        <el-form-item label="作业描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入作业描述"
            :autosize="{ minRows: 10, maxRows: 24 }"
            resize="vertical"
            class="form-input"
          />
        </el-form-item>
        <el-form-item label="满分" prop="totalScore">
          <el-input-number
            v-model="form.totalScore"
            :min="1"
            :max="1000"
            :step="1"
            placeholder="请输入满分"
            class="score-input"
          />
        </el-form-item>
        <el-form-item label="截止时间" prop="deadline">
          <div class="deadline-split-wrap">
            <el-date-picker
              v-model="formDate"
              type="date"
              placeholder="选择日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              popper-class="dark-picker"
            />
            <el-time-picker
              v-model="formTime"
              placeholder="选择时间"
              format="HH:mm:ss"
              value-format="HH:mm:ss"
              popper-class="dark-picker"
            />
          </div>
        </el-form-item>
        <el-form-item label="作业附件">
          <el-upload
            v-model:file-list="attachmentFiles"
            :auto-upload="false"
            multiple
            :on-exceed="handleAttachmentExceed"
            :on-remove="handleAttachmentRemove"
            :on-change="handleAttachmentChange"
          >
            <el-button type="primary" plain size="default" class="upload-trigger-btn">
              <Upload :size="16" />
              <span>选择文件</span>
            </el-button>
            <template #tip>
              <div class="upload-tip">
                <Paperclip :size="12" />
                <span>支持多文件上传，单个文件不超过 50MB</span>
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="允许逾期提交">
          <el-switch v-model="form.allowLateSubmit" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { UploadUserFile } from 'element-plus'
import { Paperclip, Upload, ArrowLeft } from '@lucide/vue'
import { get, putForm } from '@/utils/http'

const router = useRouter()
const route = useRoute()

const pageLoading = ref(false)
const workClassId = ref<number | null>(null)
const loading = ref(false)
const removedAttachmentIds = ref<number[]>([])
const existingAttachmentMap = new Map<number, number>()

const form = ref({
  title: '',
  description: '',
  totalScore: 100,
  deadline: '',
  allowLateSubmit: true
})

const formDate = ref('')
const formTime = ref('')

const buildDeadline = (): string => {
  if (formDate.value && formTime.value) {
    return `${formDate.value}T${formTime.value}`
  }
  if (formDate.value) {
    return `${formDate.value}T23:59:59`
  }
  return ''
}

watch([formDate, formTime], () => {
  form.value.deadline = buildDeadline()
}, { immediate: false })

const attachmentFiles = ref<UploadUserFile[]>([])
const MAX_ATTACHMENT_SIZE = 50 * 1024 * 1024

const handleAttachmentExceed = () => {
  ElMessage.warning('单次最多上传 20 个文件')
}

const handleAttachmentRemove = (file: UploadUserFile, _uploadFiles: UploadUserFile[]) => {
  if (file.uid !== undefined && existingAttachmentMap.has(file.uid)) {
    removedAttachmentIds.value.push(existingAttachmentMap.get(file.uid)!)
    existingAttachmentMap.delete(file.uid)
  }
}

const handleAttachmentChange = (file: UploadUserFile, uploadFiles: UploadUserFile[]) => {
  if (file.size && file.size > MAX_ATTACHMENT_SIZE) {
    ElMessage.warning(`文件「${file.name}」超过 50MB，已自动跳过`)
    const idx = attachmentFiles.value.findIndex(f => f.uid === file.uid)
    if (idx > -1) attachmentFiles.value.splice(idx, 1)
    return
  }
  attachmentFiles.value = uploadFiles.filter(f => !f.size || f.size <= MAX_ATTACHMENT_SIZE)
}

const rules = {
  title: [
    { required: true, message: '请输入作业标题', trigger: 'blur' }
  ],
  totalScore: [
    { required: true, message: '请输入满分', trigger: 'blur' },
    { type: 'number', min: 1, max: 1000, message: '分数范围1-1000', trigger: 'blur' }
  ],
  deadline: [
    { required: true, message: '请选择截止时间', trigger: 'change' }
  ]
}

const goBack = () => {
  if (workClassId.value) {
    router.push(`/teacher/course/${workClassId.value}`)
    return
  }
  router.push('/teacher/courses')
}

const loadWork = async () => {
  const workId = route.params.id
  pageLoading.value = true
  try {
    const result = await get<any>(`/works/${workId}`)
    if (result.code === 200 && result.data) {
      const data = result.data
      workClassId.value = data.classId ?? null
      form.value.title = data.title || ''
      form.value.description = data.description || ''
      form.value.totalScore = data.totalScore || 100
      form.value.allowLateSubmit = data.allowLateSubmit ?? true
      if (data.deadline) {
        const dt = data.deadline.replace(' ', 'T')
        const [d, t] = dt.split('T')
        formDate.value = d
        formTime.value = t || '23:59:59'
      }
      if (Array.isArray(data.attachments)) {
        attachmentFiles.value = data.attachments.map((att: any) => {
          const uid = Date.now() + att.id
          existingAttachmentMap.set(uid, att.id)
          return {
            uid,
            name: att.fileName,
            size: att.fileSize,
            status: 'success'
          } as UploadUserFile
        })
      }
    } else {
      ElMessage.error(result.message || '加载作业失败')
      goBack()
    }
  } catch {
    ElMessage.error('加载作业失败')
    goBack()
  } finally {
    pageLoading.value = false
  }
}

const submitForm = async () => {
  const deadline = buildDeadline()
  if (!deadline) {
    ElMessage.warning('请选择截止时间')
    return
  }
  loading.value = true
  try {
    const formData = new FormData()
    formData.append('id', String(route.params.id))
    formData.append('title', form.value.title)
    formData.append('description', form.value.description)
    formData.append('deadline', deadline)
    formData.append('totalScore', String(form.value.totalScore))
    formData.append('allowLateSubmit', String(form.value.allowLateSubmit))
    if (removedAttachmentIds.value.length > 0) {
      removedAttachmentIds.value.forEach(id => {
        formData.append('removedAttachmentIds', String(id))
      })
    }
    const newFiles = attachmentFiles.value.filter(f => f.raw).map(f => f.raw as File)
    if (newFiles.length > 0) {
      newFiles.forEach(file => {
        formData.append('attachments', file)
      })
    }
    const result = await putForm(`/works/${route.params.id}`, formData)
    if (result.code === 200) {
      ElMessage.success('保存成功')
      goBack()
    } else {
      ElMessage.error(result.message)
    }
  } catch {
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadWork()
})
</script>

<style scoped>
.create-work-page {
  padding-bottom: 24px;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
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

.header-left h2 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
  color: rgba(255, 255, 255, 0.95);
}

.subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.header-right {
  display: flex;
  gap: 12px;
}

.content-card {
  width: 100%;
  box-sizing: border-box;
}

.create-form {
  padding: 28px 40px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-input {
  width: 100%;
}

.create-form :deep(.el-textarea__inner) {
  min-height: 280px !important;
  line-height: 1.7;
}

.create-form :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.8);
}

.el-form-item {
  margin-bottom: 20px;
}

.score-input {
  width: 220px;
}

.score-input :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12) inset !important;
}

.score-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.22) inset !important;
}

.score-input :deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 1px var(--primary-color) inset !important;
}

.score-input :deep(.el-input-number__decrease),
.score-input :deep(.el-input-number__increase) {
  background: rgba(255, 255, 255, 0.04) !important;
  border-color: rgba(255, 255, 255, 0.15) !important;
  color: rgba(255, 255, 255, 0.7) !important;
  transition: all 0.2s ease;
}

.score-input :deep(.el-input-number__decrease:hover),
.score-input :deep(.el-input-number__increase:hover) {
  background: rgba(102, 126, 234, 0.18) !important;
  color: #667eea !important;
  border-color: rgba(102, 126, 234, 0.35) !important;
}

.score-input :deep(.el-input-number__decrease:active),
.score-input :deep(.el-input-number__increase:active) {
  background: rgba(102, 126, 234, 0.28) !important;
}

.score-input :deep(.is-disabled.el-input-number__decrease),
.score-input :deep(.is-disabled.el-input-number__increase) {
  background: rgba(255, 255, 255, 0.02) !important;
  color: rgba(255, 255, 255, 0.3) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
  cursor: not-allowed;
}

.score-input :deep(.el-input-number__decrease.is-disabled:hover),
.score-input :deep(.el-input-number__increase.is-disabled:hover) {
  color: rgba(255, 255, 255, 0.3) !important;
  background: rgba(255, 255, 255, 0.02) !important;
}

.create-form :deep(.el-upload .el-upload--text) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px dashed rgba(255, 255, 255, 0.2) !important;
  color: rgba(255, 255, 255, 0.7) !important;
  transition: all 0.2s ease;
}

.create-form :deep(.el-upload .el-upload--text:hover) {
  border-color: #667eea !important;
  color: #667eea !important;
  background: rgba(102, 126, 234, 0.08) !important;
}

.create-form :deep(.el-upload-list__item) {
  background: rgba(255, 255, 255, 0.04) !important;
  color: rgba(255, 255, 255, 0.85) !important;
  border-radius: 6px;
  padding: 6px 10px;
  margin-bottom: 6px;
}

.create-form :deep(.el-upload-list__item-name) {
  color: rgba(255, 255, 255, 0.85) !important;
}

.create-form :deep(.el-upload-list__item-name:hover) {
  color: #667eea !important;
}

.create-form :deep(.el-upload-list__item-delete) {
  color: rgba(255, 255, 255, 0.5) !important;
}

.create-form :deep(.el-upload-list__item-delete:hover) {
  color: #ef4444 !important;
}

.upload-trigger-btn {
  display: inline-flex !important;
  align-items: center !important;
  gap: 6px !important;
}

.upload-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 6px;
}

.deadline-split-wrap {
  display: inline-flex !important;
  align-items: center !important;
  gap: 8px !important;
  flex-wrap: nowrap !important;
  width: auto !important;
}

.deadline-split-wrap :deep(.el-date-editor),
.deadline-split-wrap :deep(.el-time-editor) {
  flex: 0 0 auto !important;
  min-width: 0 !important;
  width: 220px !important;
}

.deadline-split-wrap :deep(.el-input__wrapper) {
  width: 100% !important;
  min-width: 0 !important;
}

.deadline-split-wrap :deep(.el-time-editor .el-input__inner),
.deadline-split-wrap :deep(.el-time-editor .el-input__wrapper) {
  color: rgba(255, 255, 255, 0.95) !important;
}
</style>
