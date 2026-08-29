<template>
  <div class="create-work-page">
    <div class="page-header">
      <div class="header-left">
        <h2>创建作业</h2>
        <p class="subtitle">填写作业信息</p>
      </div>
      <div class="header-right">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="loading">提交</el-button>
      </div>
    </div>
    <el-card class="content-card">
      <el-form :model="form" :rules="rules" ref="formRef" class="create-form">
        <el-form-item label="作业标题" prop="title">
          <el-input 
            v-model="form.title" 
            placeholder="请输入作业标题"
            class="form-input"
          />
        </el-form-item>
        <el-form-item label="作业描述" prop="description">
          <el-textarea 
            v-model="form.description" 
            placeholder="请输入作业描述"
            :rows="4"
            class="form-input"
          />
        </el-form-item>
        <el-form-item label="所属班级" prop="classId">
          <el-select v-model="form.classId" placeholder="请选择班级" class="form-input">
            <el-option v-for="cls in classOptions" :key="cls.id" :label="cls.className" :value="cls.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="满分" prop="score">
          <el-input-number
            v-model="form.score"
            :min="1"
            :max="1000"
            :step="1"
            placeholder="请输入满分"
            class="form-input score-input"
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
              class="form-input"
              popper-class="dark-picker"
            />
            <el-time-picker
              v-model="formTime"
              placeholder="选择时间"
              format="HH:mm:ss"
              value-format="HH:mm:ss"
              class="form-input"
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
        <el-form-item label="置顶作业">
          <el-switch v-model="form.isPinned" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkStore } from '@/stores/work'
import { useClassStore } from '@/stores/class'
import { ElMessage } from 'element-plus'
import type { UploadUserFile } from 'element-plus'
import { Paperclip, Upload } from '@lucide/vue'

const router = useRouter()
const workStore = useWorkStore()
const classStore = useClassStore()

const form = ref({
  title: '',
  description: '',
  classId: null,
  score: 100,
  deadline: '',
  isPinned: false
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

const handleAttachmentRemove = (_file: UploadUserFile, uploadFiles: UploadUserFile[]) => {
  attachmentFiles.value = uploadFiles
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

const loading = ref(false)
const classOptions = ref<any[]>([])

const rules = {
  title: [
    { required: true, message: '请输入作业标题', trigger: 'blur' }
  ],
  classId: [
    { required: true, message: '请选择班级', trigger: 'change' }
  ],
  score: [
    { required: true, message: '请输入满分', trigger: 'blur' },
    { type: 'number', min: 1, max: 1000, message: '分数范围1-1000', trigger: 'blur' }
  ],
  deadline: [
    { required: true, message: '请选择截止时间', trigger: 'change' }
  ]
}

const goBack = () => {
  router.push('/work')
}

const submitForm = async () => {
  loading.value = true
  
  try {
    const files = attachmentFiles.value
      .filter(f => f.raw)
      .map(f => f.raw as File)
    
    const result = await workStore.createWork({
      title: form.value.title,
      description: form.value.description,
      classId: form.value.classId,
      totalScore: form.value.score,
      deadline: form.value.deadline,
      isPinned: form.value.isPinned
    }, files)
    
    if (result.code === 200) {
      ElMessage.success('创建成功')
      router.push('/work')
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('创建失败')
  } finally {
    loading.value = false
  }
}

const loadData = async () => {
  await classStore.getClasses()
  classOptions.value = classStore.classes
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.create-work-page {
  padding-bottom: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left h2 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.subtitle {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.header-right {
  display: flex;
  gap: 12px;
}

.content-card {
  max-width: 600px;
}

.create-form {
  padding: 20px 0;
}

.form-input {
  width: 100%;
}

.create-form :deep(.el-input__wrapper),
.create-form :deep(.el-select__wrapper),
.create-form :deep(.el-date-editor) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06) !important;
  border-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.25) !important;
}

.create-form :deep(.el-input__wrapper:focus-within),
.create-form :deep(.el-select__wrapper:focus-within),
.create-form :deep(.el-date-editor:focus-within) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2) !important;
}

.create-form :deep(.el-input__inner),
.create-form :deep(.el-select__selected-item),
.create-form :deep(.el-date-editor .el-input__inner) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95) !important;
  background: transparent !important;
}

.create-form :deep(.el-input__inner::placeholder),
.create-form :deep(.el-select__placeholder) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.create-form :deep(.el-textarea__inner) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06) !important;
  border-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.25) !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95) !important;
}

.create-form :deep(.el-textarea__inner::placeholder) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.create-form :deep(.el-textarea__inner:focus) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2) !important;
}

.create-form :deep(.el-form-item__label) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
}

.el-form-item {
  margin-bottom: 20px;
}

.score-input {
  width: 100%;
}

.score-input :deep(.el-input__wrapper) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06) !important;
  border-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.25) !important;
  box-shadow: none !important;
}

.score-input :deep(.el-input__wrapper:focus-within) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2) !important;
}

.score-input :deep(.el-input__inner) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95) !important;
  background: transparent !important;
}

.score-input :deep(.el-input-number__decrease),
.score-input :deep(.el-input-number__increase) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04) !important;
  border-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.15) !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7) !important;
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
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.02) !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.3) !important;
  border-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08) !important;
  cursor: not-allowed;
}

.score-input :deep(.el-input-number__decrease.is-disabled:hover),
.score-input :deep(.el-input-number__increase.is-disabled:hover) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.3) !important;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.02) !important;
}

.create-form :deep(.el-upload .el-upload--text) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06) !important;
  border: 1px dashed rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.2) !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7) !important;
  transition: all 0.2s ease;
}

.create-form :deep(.el-upload .el-upload--text:hover) {
  border-color: #667eea !important;
  color: #667eea !important;
  background: rgba(102, 126, 234, 0.08) !important;
}

.create-form :deep(.el-upload-list__item) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04) !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85) !important;
  border-radius: 6px;
  padding: 6px 10px;
  margin-bottom: 6px;
}

.create-form :deep(.el-upload-list__item-name) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85) !important;
}

.create-form :deep(.el-upload-list__item-name:hover) {
  color: #667eea !important;
}

.create-form :deep(.el-upload-list__item-delete) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5) !important;
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
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
  margin-top: 6px;
}

.deadline-split-wrap {
  display: inline-flex !important;
  align-items: center !important;
  gap: 8px !important;
  flex-wrap: nowrap !important;
  width: 100% !important;
}

.deadline-split-wrap :deep(.el-date-editor),
.deadline-split-wrap :deep(.el-time-editor) {
  flex: 1 1 0 !important;
  min-width: 0 !important;
  width: 100% !important;
}

.deadline-split-wrap :deep(.el-input__wrapper) {
  width: 100% !important;
  min-width: 0 !important;
}

.deadline-split-wrap :deep(.el-time-editor .el-input__inner),
.deadline-split-wrap :deep(.el-time-editor .el-input__wrapper) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95) !important;
}

/* 手机端适配 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .content-card {
    max-width: 100%;
  }

  .create-form {
    padding: 16px 0;
  }

  .deadline-split-wrap {
    flex-direction: column !important;
    gap: 10px !important;
  }

  .deadline-split-wrap :deep(.el-date-editor),
  .deadline-split-wrap :deep(.el-time-editor) {
    flex: none !important;
    width: 100% !important;
  }
}
</style>
