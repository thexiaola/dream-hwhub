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
    <el-dialog v-if="showGradeDialog" title="批改作业" @close="closeGradeDialog">
      <div class="grade-form">
        <p class="submission-content">{{ currentSubmission?.content }}</p>
        <el-form-item label="成绩">
          <el-input v-model.number="grade" type="number" placeholder="请输入成绩" class="grade-input" />
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
import { useSubmissionStore } from '@/stores/submission'
import { useWorkStore } from '@/stores/work'
import { ElMessage } from 'element-plus'
import { FileText } from 'lucide-vue-next'

const submissionStore = useSubmissionStore()
const workStore = useWorkStore()

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
  console.log('View submission:', id)
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
}

.subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
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
  color: rgba(255, 255, 255, 0.4);
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
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: white;
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
  color: rgba(255, 255, 255, 0.4);
}

.empty-state p {
  margin-top: 16px;
}

.grade-form {
  padding: 20px 0;
}

.submission-content {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  margin-bottom: 20px;
}

.grade-input {
  width: 200px;
}
</style>
