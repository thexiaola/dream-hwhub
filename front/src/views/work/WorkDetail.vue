<template>
  <div class="work-detail-page">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" class="back-btn">
          <ArrowLeft :size="18" />
        </el-button>
        <h2>{{ work?.title }}</h2>
      </div>
      <div class="header-right">
        <el-button v-if="work?.isPinned" @click="togglePin(false)">取消置顶</el-button>
        <el-button v-else @click="togglePin(true)">置顶作业</el-button>
        <el-button type="primary" @click="goToSubmit">提交作业</el-button>
      </div>
    </div>
    <div v-if="work" class="work-detail">
      <el-card class="detail-card">
        <div class="detail-section">
          <h3>基本信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">班级</span>
              <span class="value">{{ work.className }}</span>
            </div>
            <div class="info-item">
              <span class="label">满分</span>
              <span class="value">{{ work.score }}分</span>
            </div>
            <div class="info-item">
              <span class="label">截止时间</span>
              <span class="value">{{ formatDate(work.deadline) }}</span>
            </div>
            <div class="info-item">
              <span class="label">状态</span>
              <span :class="['status-badge', work.status]">{{ getStatusText(work.status) }}</span>
            </div>
          </div>
        </div>
        <div class="detail-section">
          <h3>作业描述</h3>
          <p class="description">{{ work.description }}</p>
        </div>
      </el-card>
      <el-card class="submit-section">
        <template #header>
          <h3>作业提交</h3>
        </template>
        <div v-if="submission" class="submission-info">
          <div class="submission-header">
            <span class="submitter">{{ submission.submitterName }}</span>
            <span :class="['status-badge', submission.status]">{{ getSubmissionStatus(submission.status) }}</span>
          </div>
          <p class="submission-content">{{ submission.content }}</p>
          <div class="submission-meta">
            <span>提交时间：{{ formatDate(submission.submittedAt) }}</span>
            <span v-if="submission.gradedAt">批改时间：{{ formatDate(submission.gradedAt) }}</span>
            <span v-if="submission.grade !== null">成绩：{{ submission.grade }}分</span>
          </div>
        </div>
        <div v-else class="no-submission">
          <FileText :size="48" />
          <p>暂无提交记录</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useWorkStore } from '@/stores/work'
import { useSubmissionStore } from '@/stores/submission'
import { ElMessage } from 'element-plus'
import { ArrowLeft, FileText } from '@lucide/vue'

const route = useRoute()
const router = useRouter()
const workStore = useWorkStore()
const submissionStore = useSubmissionStore()

const work = ref<any>(null)
const submission = ref<any>(null)

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending: '未截止',
    expired: '已过期',
    graded: '已批改'
  }
  return map[status] || status
}

const getSubmissionStatus = (status: string) => {
  return status === 'submitted' ? '已提交' : '已批改'
}

const goBack = () => {
  router.push('/work')
}

const goToSubmit = () => {
  router.push(`/submission/create?workId=${route.params.id}`)
}

const togglePin = async (isPinned: boolean) => {
  const result = await workStore.pinWork(Number(route.params.id), isPinned)
  if (result.code === 200) {
    ElMessage.success(isPinned ? '置顶成功' : '取消置顶成功')
    work.value.isPinned = isPinned
  } else {
    ElMessage.error(result.message)
  }
}

const loadData = async () => {
  const workId = Number(route.params.id)
  work.value = await workStore.getWorkById(workId)
  await submissionStore.getSubmissions(workId)
  submission.value = submissionStore.submissions[0]
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.work-detail-page {
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
}

.header-right {
  display: flex;
  gap: 12px;
}

.detail-card, .submit-section {
  margin-bottom: 20px;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
}

.info-item .label {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.info-item .value {
  font-size: 14px;
  font-weight: 500;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.pending {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.status-badge.expired {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.status-badge.graded, .status-badge.submitted {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.description {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
}

.submission-info {
  padding: 16px;
}

.submission-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.submitter {
  font-weight: 600;
}

.submission-content {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  margin-bottom: 12px;
}

.submission-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.no-submission {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: rgba(255, 255, 255, 0.4);
}

.no-submission p {
  margin-top: 16px;
}
</style>
