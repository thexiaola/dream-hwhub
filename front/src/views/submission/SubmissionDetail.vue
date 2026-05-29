<template>
  <div class="submission-container">
    <Sidebar />
    
    <div class="submission-main">
      <Header />
      
      <div class="submission-content">
        <div class="submission-header-actions">
          <el-button @click="goBack">返回列表</el-button>
          <el-button v-if="canGrade" type="primary" @click="showGradeDialog = true">批改作业</el-button>
        </div>
        
        <el-card v-if="submission" class="submission-detail-card">
          <div class="submission-detail-header">
            <h2>{{ submission.workTitle }}</h2>
            <div class="submission-badges">
              <span v-if="submission.score !== null" class="badge scored">
                已批改: {{ submission.score }} 分
              </span>
              <span v-else class="badge pending">待批改</span>
              <span v-if="submission.isLate" class="badge late">逾期提交</span>
            </div>
          </div>
          
          <div class="submission-detail-info">
            <div class="info-row">
              <span class="label">提交人</span>
              <span>{{ submission.userName }}</span>
            </div>
            <div class="info-row">
              <span class="label">提交时间</span>
              <span>{{ formatDate(submission.submitTime) }}</span>
            </div>
            <div class="info-row" v-if="submission.score !== null">
              <span class="label">批改时间</span>
              <span>{{ formatDate(submission.gradeTime) }}</span>
            </div>
            <div class="info-row" v-if="submission.comment">
              <span class="label">教师评语</span>
              <span>{{ submission.comment }}</span>
            </div>
          </div>
          
          <div v-if="submission.submissionContent" class="submission-detail-section">
            <h3>提交内容</h3>
            <div class="content-box">{{ submission.submissionContent }}</div>
          </div>
          
          <div v-if="submission.attachments && submission.attachments.length > 0" class="submission-detail-section">
            <h3>附件</h3>
            <ul class="attachment-list">
              <li v-for="attachment in submission.attachments" :key="attachment.id">
                <component :is="componentMap['FileText']" />
                <span>{{ attachment.fileName }}</span>
                <span class="file-size">{{ formatFileSize(attachment.fileSize) }}</span>
              </li>
            </ul>
          </div>
        </el-card>
        
        <el-dialog v-model="showGradeDialog" title="批改作业" width="500px">
          <el-form :model="gradeForm">
            <el-form-item label="成绩" required>
              <el-input-number 
                v-model="gradeForm.score" 
                :min="0" 
                :max="work?.totalScore || 100"
              />
              <span style="margin-left: 8px;">/ {{ work?.totalScore || 100 }} 分</span>
            </el-form-item>
            <el-form-item label="评语" required>
              <el-input 
                v-model="gradeForm.comment" 
                type="textarea" 
                :rows="4"
                placeholder="请输入评语"
              />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showGradeDialog = false">取消</el-button>
            <el-button type="primary" @click="handleGrade">提交</el-button>
          </template>
        </el-dialog>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from '../../components/Sidebar.vue'
import Header from '../../components/Header.vue'
import { useSubmissionStore } from '../../stores/submission'
import { useWorkStore } from '../../stores/work'
import { ElMessage } from 'element-plus'
import { FileText } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const submissionStore = useSubmissionStore()
const workStore = useWorkStore()

const componentMap = {
  FileText
}

const submissionId = computed(() => parseInt(route.params.id))
const submission = ref(null)
const work = ref(null)
const showGradeDialog = ref(false)
const canGrade = computed(() => {
  return submission.value?.userRole === '创建者' || submission.value?.userRole === '班级助理'
})

const gradeForm = reactive({
  score: 0,
  comment: ''
})

onMounted(async () => {
  await loadSubmissionDetail()
})

async function loadSubmissionDetail() {
  const response = await submissionStore.getSubmissionDetail(submissionId.value)
  if (response.code === 200) {
    submission.value = response.data
    
    const workResponse = await workStore.getWorkDetail(response.data.workId)
    if (workResponse.code === 200) {
      work.value = workResponse.data
    }
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function handleGrade() {
  if (!gradeForm.comment) {
    ElMessage.error('请输入评语')
    return
  }
  
  const response = await submissionStore.gradeWork({
    submissionId: submissionId.value,
    score: gradeForm.score,
    comment: gradeForm.comment
  })
  
  if (response.code === 200) {
    ElMessage.success('批改成功')
    showGradeDialog.value = false
    await loadSubmissionDetail()
  } else {
    ElMessage.error(response.message)
  }
}

function goBack() {
  router.push('/submissions')
}
</script>

<style scoped>
.submission-container {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.submission-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.submission-content {
  padding: 24px;
}

.submission-header-actions {
  margin-bottom: 20px;
}

.submission-detail-card {
  padding: 24px;
}

.submission-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.submission-detail-header h2 {
  font-size: 24px;
  font-weight: 600;
}

.submission-badges {
  display: flex;
  gap: 8px;
}

.badge {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 14px;
}

.badge.scored {
  background: #d4edda;
  color: #155724;
}

.badge.pending {
  background: #fff3cd;
  color: #856404;
}

.badge.late {
  background: #f8d7da;
  color: #721c24;
}

.submission-detail-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
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

.submission-detail-section {
  margin-bottom: 24px;
}

.submission-detail-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.content-box {
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  white-space: pre-wrap;
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
</style>