<template>
  <div class="submit-work-page">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" class="back-btn">
          <ArrowLeft :size="18" />
        </el-button>
        <h2>提交作业</h2>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="submitWork" :loading="loading">提交</el-button>
      </div>
    </div>
    <el-card class="content-card">
      <div v-if="work" class="work-info">
        <h3>{{ work.title }}</h3>
        <p>{{ work.description }}</p>
        <div
          class="work-attachments"
          v-if="work.attachments && work.attachments.length > 0"
        >
          <div class="attachments-title">作业附件</div>
          <div
            v-for="att in work.attachments"
            :key="att.id"
            class="attachment-item"
            title="点击查看"
            @click="previewAttachment(att)"
          >
            <Paperclip :size="14" />
            <span class="att-name">{{ att.fileName }}</span>
            <span class="att-size" v-if="att.fileSize">
              {{ formatFileSize(att.fileSize) }}
            </span>
            <Eye :size="13" class="att-eye" />
          </div>
        </div>
        <div class="work-meta">
          <span>截止时间：{{ formatDate(work.deadline) }}</span>
          <span>满分：{{ work.score }}分</span>
        </div>
      </div>
      <el-form :model="form" class="submit-form">
        <el-form-item label="作业内容">
          <el-textarea 
            v-model="form.content" 
            placeholder="请输入作业内容"
            :rows="8"
            class="form-input"
          />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useWorkStore } from '@/stores/work'
import { useSubmissionStore } from '@/stores/submission'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Paperclip, Eye } from '@lucide/vue'
import { openAttachmentPreview, formatFileSize } from '@/utils/attachment'

const route = useRoute()
const router = useRouter()
const workStore = useWorkStore()
const submissionStore = useSubmissionStore()

const work = ref<any>(null)
const form = ref({
  content: ''
})
const loading = ref(false)

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const goBack = () => {
  const workId = route.query.workId
  if (workId) {
    router.push(`/work/${workId}`)
  } else {
    router.push('/submission')
  }
}

const previewAttachment = (att: { filePath: string; fileName: string }) => {
  openAttachmentPreview(att.filePath, att.fileName, true)
}

const submitWork = async () => {
  if (!form.value.content.trim()) {
    ElMessage.error('请输入作业内容')
    return
  }
  
  loading.value = true
  
  try {
    const workId = Number(route.query.workId) || 0
    const result = await submissionStore.submitWork(workId, form.value.content)
    
    if (result.code === 200) {
      ElMessage.success('提交成功')
      goBack()
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('提交失败')
  } finally {
    loading.value = false
  }
}

const loadData = async () => {
  const workId = Number(route.query.workId)
  if (workId) {
    work.value = await workStore.getWorkById(workId)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.submit-work-page {
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

.content-card {
  max-width: 800px;
}

.work-info {
  padding: 20px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border-radius: 12px;
  margin-bottom: 24px;
}

.work-info h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.work-info p {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
  line-height: 1.6;
  margin-bottom: 12px;
}

.work-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.work-attachments {
  margin: 4px 0 12px;
}

.attachments-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.75);
  margin-bottom: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 6px;
}

.attachment-item:hover {
  background: rgba(102, 126, 234, 0.1);
  border-color: rgba(102, 126, 234, 0.4);
}

.attachment-item .att-name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
  word-break: break-all;
}

.attachment-item .att-size {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
  flex-shrink: 0;
}

.attachment-item .att-eye {
  flex-shrink: 0;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.35);
}

.attachment-item:hover .att-eye {
  color: #667eea;
}

.submit-form {
  padding: 20px 0;
}

.submit-form :deep(.el-textarea__inner) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06) !important;
  border-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.25) !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95) !important;
}

.submit-form :deep(.el-textarea__inner::placeholder) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.submit-form :deep(.el-textarea__inner:focus) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2) !important;
}

.submit-form :deep(.el-form-item__label) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
}

.form-input {
  width: 100%;
}
</style>
