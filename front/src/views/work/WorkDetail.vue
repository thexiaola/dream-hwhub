<template>
  <div class="work-container">
    <Sidebar />
    
    <div class="work-main">
      <Header />
      
      <div class="work-content">
        <div class="work-header-actions">
          <el-button @click="goBack">返回列表</el-button>
          <router-link :to="`/works/${workId}/edit`" class="el-button el-button--primary">编辑作业</router-link>
          <router-link v-if="canSubmit" :to="`/submit/${workId}`" class="el-button el-button--success">提交作业</router-link>
        </div>
        
        <el-card v-if="work" class="work-detail-card">
          <div class="work-detail-header">
            <h2>{{ work.title }}</h2>
            <div class="work-badges">
              <span v-if="work.isPinned" class="badge pinned">置顶</span>
              <span :class="['badge', statusClass]">{{ statusText }}</span>
            </div>
          </div>
          
          <div class="work-detail-info">
            <div class="info-row">
              <span class="label">发布人</span>
              <span>{{ work.publisherName }}</span>
            </div>
            <div class="info-row">
              <span class="label">班级</span>
              <span>{{ work.className }}</span>
            </div>
            <div class="info-row">
              <span class="label">截止时间</span>
              <span>{{ formatDate(work.deadline) }}</span>
            </div>
            <div class="info-row">
              <span class="label">发布时间</span>
              <span>{{ formatDate(work.publishTime) }}</span>
            </div>
            <div class="info-row">
              <span class="label">总分</span>
              <span>{{ work.totalScore }} 分</span>
            </div>
            <div class="info-row">
              <span class="label">允许逾期提交</span>
              <span>{{ work.allowLateSubmit ? '是' : '否' }}</span>
            </div>
          </div>
          
          <div class="work-detail-section">
            <h3>作业描述</h3>
            <p>{{ work.description }}</p>
          </div>
          
          <div v-if="work.attachments && work.attachments.length > 0" class="work-detail-section">
            <h3>附件</h3>
            <ul class="attachment-list">
              <li v-for="attachment in work.attachments" :key="attachment.id">
                <component :is="componentMap['FileText']" />
                <span>{{ attachment.fileName }}</span>
                <span class="file-size">{{ formatFileSize(attachment.fileSize) }}</span>
              </li>
            </ul>
          </div>
          
          <div class="work-detail-section">
            <h3>提交记录</h3>
            <el-table :data="submissions" border>
              <el-table-column prop="userName" label="学生" />
              <el-table-column prop="submitTime" label="提交时间" :formatter="formatDate" />
              <el-table-column prop="score" label="成绩" />
              <el-table-column prop="isLate" label="是否逾期" :formatter="formatIsLate" />
              <el-table-column label="操作">
                <template #default="scope">
                  <router-link :to="`/submissions/${scope.row.id}`" class="action-btn">查看详情</router-link>
                  <el-button 
                    v-if="canGrade" 
                    type="text" 
                    @click="goToGrade(scope.row.id)" 
                    class="grade-btn"
                  >批改</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from '../../components/Sidebar.vue'
import Header from '../../components/Header.vue'
import { useWorkStore } from '../../stores/work'
import { useSubmissionStore } from '../../stores/submission'
import { FileText } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const workStore = useWorkStore()
const submissionStore = useSubmissionStore()

const componentMap = {
  FileText
}

const workId = computed(() => parseInt(route.params.id))
const work = ref(null)
const submissions = ref([])

const statusText = computed(() => {
  const statusMap = { 0: '未发布', 1: '已发布', 2: '已结束' }
  return statusMap[work.value?.status] || '未知'
})

const statusClass = computed(() => {
  const classMap = { 0: 'draft', 1: 'published', 2: 'ended' }
  return classMap[work.value?.status] || ''
})

const canSubmit = computed(() => {
  return work.value?.status === 1 && new Date(work.value?.deadline) > new Date()
})

const canGrade = computed(() => {
  return work.value?.userRole === '创建者' || work.value?.userRole === '班级助理'
})

onMounted(async () => {
  await loadWorkDetail()
  await loadSubmissions()
})

async function loadWorkDetail() {
  const response = await workStore.getWorkDetail(workId.value)
  if (response.code === 200) {
    work.value = response.data
  }
}

async function loadSubmissions() {
  const response = await submissionStore.getSubmissionListByWork(workId.value)
  if (response.code === 200) {
    submissions.value = response.data.records
  }
}

function formatDate(dateStr) {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatIsLate(row, column) {
  return row.isLate ? '是' : '否'
}

function goBack() {
  router.push('/works')
}

function goToGrade(submissionId) {
  router.push(`/submissions/${submissionId}`)
}
</script>

<style scoped>
.work-container {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.work-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.work-content {
  padding: 24px;
}

.work-header-actions {
  margin-bottom: 20px;
}

.work-detail-card {
  padding: 24px;
}

.work-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.work-detail-header h2 {
  font-size: 24px;
  font-weight: 600;
}

.work-badges {
  display: flex;
  gap: 8px;
}

.badge {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.badge.pinned {
  background: #fff3cd;
  color: #856404;
}

.badge.draft {
  background: #e7f3ff;
  color: #007bff;
}

.badge.published {
  background: #d4edda;
  color: #155724;
}

.badge.ended {
  background: #f8f9fa;
  color: #6c757d;
}

.work-detail-info {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.info-row {
  display: flex;
  flex-direction: column;
}

.info-row .label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.work-detail-section {
  margin-bottom: 24px;
}

.work-detail-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.attachment-list {
  list-style: none;
  padding: 0;
}

.attachment-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  margin-bottom: 8px;
  border-radius: 8px;
}

.file-size {
  margin-left: auto;
  color: #909399;
  font-size: 14px;
}

.action-btn {
  margin-right: 12px;
  color: #667eea;
  font-size: 14px;
}

.grade-btn {
  color: #67c23a;
}
</style>