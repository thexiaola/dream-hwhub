<template>
  <div class="submission-list-page">
    <div class="page-header">
      <div class="header-left">
        <h2>作业提交</h2>
        <p class="subtitle">查看和批改作业提交</p>
      </div>
    </div>
    <el-card class="content-card">
      <div class="filter-bar">
        <el-select v-model="workFilter" placeholder="选择作业" class="filter-select">
          <el-option label="全部" value="" />
          <el-option v-for="work in workOptions" :key="work.id" :label="work.title" :value="work.id" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="状态筛选" class="filter-select">
          <el-option label="全部" value="" />
          <el-option label="已提交" value="submitted" />
          <el-option label="已批改" value="graded" />
        </el-select>
      </div>
      <el-table :data="filteredSubmissions" class="submission-table">
        <el-table-column prop="workTitle" label="作业名称" />
        <el-table-column prop="submitterName" label="提交人" />
        <el-table-column prop="submitterUserNo" label="学号/工号" />
        <el-table-column prop="submittedAt" label="提交时间" :formatter="formatDate" />
        <el-table-column prop="grade" label="成绩">
          <template #default="scope">
            <span v-if="scope.row.grade !== null">{{ scope.row.grade }}分</span>
            <span v-else class="no-grade">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <span :class="['status-badge', scope.row.status]">
              {{ getStatusText(scope.row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <button class="action-btn" @click="viewSubmission(scope.row.id)">查看</button>
            <button v-if="scope.row.status === 'submitted'" class="action-btn primary" @click="gradeSubmission(scope.row)">批改</button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="filteredSubmissions.length === 0" class="empty-state">
        <FileText :size="48" />
        <p>暂无提交记录</p>
      </div>
    </el-card>
    <el-dialog v-if="showGradeDialog" title="批改作业" @close="closeGradeDialog" class="dark-dialog">
      <div class="grade-form">
        <p class="submission-content">{{ currentSubmission?.content }}</p>
        <div
          class="submission-attachments"
          v-if="currentSubmission?.attachments && currentSubmission.attachments.length > 0"
        >
          <div class="attachments-title">
            <Paperclip :size="14" />
            <span>提交附件 ({{ currentSubmission.attachments.length }})</span>
          </div>
          <div
            v-for="att in currentSubmission.attachments"
            :key="att.id"
            class="attachment-item"
            title="点击查看"
            @click="openAttachmentPreview(att.filePath, att.fileName, true)"
          >
            <File :size="14" />
            <span class="att-name">{{ att.fileName }}</span>
            <span class="att-size" v-if="att.fileSize">{{ formatFileSize(att.fileSize) }}</span>
          </div>
        </div>
        <el-form-item label="成绩">
          <el-input-number
            v-model="grade"
            :min="0"
            :max="1000"
            :step="1"
            placeholder="请输入成绩"
            class="grade-input"
          />
        </el-form-item>
      </div>
      <template #footer>
        <el-button @click="closeGradeDialog">取消</el-button>
        <el-button type="primary" @click="submitGrade">提交成绩</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useSubmissionStore } from '@/stores/submission'
import { useWorkStore } from '@/stores/work'
import { ElMessage } from 'element-plus'
import { FileText, Paperclip, File } from '@lucide/vue'
import { openAttachmentPreview, formatFileSize } from '@/utils/attachment'

const submissionStore = useSubmissionStore()
const workStore = useWorkStore()
const router = useRouter()

const workFilter = ref('')
const statusFilter = ref('')
const workOptions = ref<any[]>([])

const showGradeDialog = ref(false)
const currentSubmission = ref<any>(null)
const grade = ref(0)

const filteredSubmissions = computed(() => {
  return submissionStore.submissions.filter(sub => {
    const matchWork = !workFilter.value || sub.workId === Number(workFilter.value)
    const matchStatus = !statusFilter.value || sub.status === statusFilter.value
    return matchWork && matchStatus
  })
})

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const getStatusText = (status: string) => {
  return status === 'submitted' ? '已提交' : '已批改'
}

const viewSubmission = (id: number) => {
  const sub = submissionStore.submissions.find(s => s.id === id)
  if (sub) {
    router.push(`/teacher/work/${sub.workId}/submissions`)
  }
}

const gradeSubmission = (submission: any) => {
  currentSubmission.value = submission
  grade.value = 0
  showGradeDialog.value = true
}

const closeGradeDialog = () => {
  showGradeDialog.value = false
  currentSubmission.value = null
  grade.value = 0
}

const submitGrade = async () => {
  if (!currentSubmission.value || grade.value <= 0) {
    ElMessage.error('请输入有效的成绩')
    return
  }
  
  const result = await submissionStore.gradeWork(currentSubmission.value.id, grade.value)
  if (result.code === 200) {
    ElMessage.success('批改成功')
    closeGradeDialog()
    await submissionStore.getSubmissions()
  } else {
    ElMessage.error(result.message)
  }
}

const loadData = async () => {
  await workStore.getWorks()
  await submissionStore.getSubmissions()
  workOptions.value = workStore.works
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.submission-list-page {
  padding-bottom: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.subtitle {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.filter-select {
  width: 200px;
}

.submission-table {
  width: 100%;
}

.no-grade {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.submitted {
  background: rgba(234, 179, 8, 0.2);
  color: #eab308;
}

.status-badge.graded {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.action-btn {
  padding: 6px 12px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.05);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 6px;
  color: var(--fg);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
  margin-right: 8px;
}

.action-btn:hover {
  background: rgba(102, 126, 234, 0.2);
}

.action-btn.primary {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
}

.action-btn.primary:hover {
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.empty-state p {
  margin-top: 16px;
}

.grade-form {
  padding: 20px 0;
}

.submission-content {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
  line-height: 1.6;
  padding: 16px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border-radius: 8px;
  margin-bottom: 20px;
}

.submission-attachments {
  margin-bottom: 20px;
}

.attachments-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.75);
  margin-bottom: 8px;
}

.submission-attachments .attachment-item {
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

.submission-attachments .attachment-item:hover {
  background: rgba(102, 126, 234, 0.1);
  border-color: rgba(102, 126, 234, 0.4);
}

.submission-attachments .att-name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
  word-break: break-all;
}

.submission-attachments .att-size {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
  flex-shrink: 0;
}

.filter-select :deep(.el-select__wrapper),
.grade-input :deep(.el-input__wrapper) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06) !important;
  border-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.25) !important;
  box-shadow: none !important;
}

.filter-select :deep(.el-select__wrapper:focus-within),
.grade-input :deep(.el-input__wrapper:focus-within) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2) !important;
}

.filter-select :deep(.el-select__selected-item),
.filter-select :deep(.el-select__placeholder) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95) !important;
}

.grade-input :deep(.el-input__inner) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95) !important;
  background: transparent !important;
}

.grade-input :deep(.el-input-number__decrease),
.grade-input :deep(.el-input-number__increase) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04) !important;
  border-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.15) !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7) !important;
  transition: all 0.2s ease;
}

.grade-input :deep(.el-input-number__decrease:hover),
.grade-input :deep(.el-input-number__increase:hover) {
  background: rgba(102, 126, 234, 0.18) !important;
  color: #667eea !important;
  border-color: rgba(102, 126, 234, 0.35) !important;
}

.grade-input :deep(.el-input-number__decrease:active),
.grade-input :deep(.el-input-number__increase:active) {
  background: rgba(102, 126, 234, 0.28) !important;
}

.grade-input :deep(.is-disabled.el-input-number__decrease),
.grade-input :deep(.is-disabled.el-input-number__increase) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.02) !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.3) !important;
  border-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08) !important;
  cursor: not-allowed;
}

.grade-input :deep(.el-input-number__decrease.is-disabled:hover),
.grade-input :deep(.el-input-number__increase.is-disabled:hover) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.3) !important;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.02) !important;
}

.submission-table :deep(.el-table) {
  background: transparent;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
}

.submission-table :deep(.el-table th.el-table__cell) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04) !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
  border-bottom-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
}

.submission-table :deep(.el-table td.el-table__cell) {
  border-bottom-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06);
}

.submission-table :deep(.el-table tr) {
  background: transparent;
}

.submission-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04) !important;
}

.submission-table :deep(.el-table__empty-text) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.grade-input {
  width: 200px;
}
</style>
