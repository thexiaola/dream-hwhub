<template>
  <div class="student-course-detail">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" class="back-btn">
          <ArrowLeft :size="18" />
        </el-button>
        <h2>{{ course?.className }}</h2>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="showInviteDialog = true">
          <UserPlus :size="16" />
          邀请同学
        </el-button>
        <el-button
          v-if="course && course.userRole !== '创建者'"
          type="danger"
          plain
          :loading="leaving"
          @click="leaveClassAction"
        >
          <LogOut :size="16" />
          退出班级
        </el-button>
      </div>
    </div>

    <el-card class="course-info-card">
      <div class="info-section">
        <div class="info-item">
          <User :size="16" />
          <span class="label">授课老师：</span>
          <span class="value">{{ course?.ownerName }}</span>
        </div>
        <div class="info-item">
          <Users :size="16" />
          <span class="label">学生人数：</span>
          <span class="value">{{ course?.studentCount }} 人</span>
        </div>
        <div class="info-item">
          <Calendar :size="16" />
          <span class="label">我的角色：</span>
          <span class="value role-badge student">{{ course?.userRole }}</span>
        </div>
      </div>
      <div v-if="course?.description" class="description-section">
        <h4>课程描述</h4>
        <p>{{ course.description }}</p>
      </div>
    </el-card>

    <el-card class="works-card">
      <template #header>
        <div class="card-header">
          <h3>作业列表</h3>
        </div>
      </template>
      <div class="work-list">
        <div 
          v-for="work in works" 
          :key="work.id" 
          class="work-item"
          @click="goToWork(work.id)"
        >
          <div class="work-header">
            <div class="title-row">
              <h4>{{ work.title }}</h4>
              <span v-if="work.isPinned" class="pin-badge">
                <Star :size="14" />
              </span>
            </div>
            <span :class="['status-tag', getWorkStatus(work)]">
              {{ getWorkStatusText(work) }}
            </span>
          </div>
          <div class="work-info">
            <div class="info-item">
              <Clock :size="14" />
              <span>截止：{{ formatDate(work.deadline) }}</span>
            </div>
            <div class="info-item">
              <FileText :size="14" />
              <span>总分：{{ work.totalScore }}分</span>
            </div>
          </div>
          <div class="submission-status">
            <span v-if="getSubmissionStatus(work) === 'submitted'" class="submitted">
              已提交
            </span>
            <span v-else-if="getSubmissionStatus(work) === 'graded'" class="graded">
              已批改 - {{ work.myScore }}分
            </span>
            <span v-else-if="getSubmissionStatus(work) === 'late'" class="late">
              未提交（已逾期）
            </span>
            <span v-else class="pending">
              未提交
            </span>
          </div>
        </div>
      </div>
      <div v-if="works.length === 0" class="empty-state">
        <FileText :size="32" />
        <p>暂无作业</p>
      </div>
    </el-card>

    <el-dialog
      v-model="showInviteDialog"
      title="邀请同学加入班级"
      width="460px"
      class="dark-dialog student-invite-dialog"
      @close="inviteForm.account = ''"
    >
      <el-form :model="inviteForm" label-width="80px">
        <el-form-item label="同学账号">
          <el-input
            v-model="inviteForm.account"
            placeholder="请输入同学的账号"
            maxlength="64"
          />
        </el-form-item>
      </el-form>
      <p class="invite-flow-tip">
        对方确认接受后，还需老师审核通过才能加入班级
      </p>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showInviteDialog = false">取消</el-button>
          <el-button type="primary" :loading="inviting" @click="inviteClassmate">
            发送邀请
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, post, del } from '@/utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, User, Users, Calendar, Clock, FileText, Star, UserPlus, LogOut } from '@lucide/vue'

interface CourseInfo {
  id: number
  className: string
  description?: string
  ownerId: number
  ownerName: string
  userRole: string
  memberCount: number
  teacherCount: number
  studentCount: number
}

interface WorkInfo {
  id: number
  title: string
  description: string
  classId: number
  className: string
  deadline: string
  totalScore: number
  isPinned: boolean
  status: number
  publishTime: string
  myScore?: number
  mySubmissionStatus?: number
}

interface SubmissionInfo {
  id: number
  workId: number
  status: number
  score?: number
}

const route = useRoute()
const router = useRouter()

const course = ref<CourseInfo | null>(null)
const works = ref<WorkInfo[]>([])
const submissions = ref<SubmissionInfo[]>([])

const showInviteDialog = ref(false)
const inviting = ref(false)
const inviteForm = ref({ account: '' })

const inviteClassmate = async () => {
  if (!inviteForm.value.account.trim()) {
    ElMessage.warning('请输入同学的账号')
    return
  }
  inviting.value = true
  const result = await post(`/class/${route.params.id}/invitations`, {
    userAccount: inviteForm.value.account.trim(),
  })
  inviting.value = false
  if (result.code === 200) {
    ElMessage.success(result.message || '邀请已发送，待对方确认')
    showInviteDialog.value = false
    inviteForm.value.account = ''
  } else {
    ElMessage.error(result.message || '邀请发送失败')
  }
}

const leaving = ref(false)

const leaveClassAction = async () => {
  if (!course.value) return
  try {
    await ElMessageBox.confirm(
      `确定退出班级"${course.value.className}"吗？退出后你在该班级的所有作业提交和附件将被删除，且无法恢复。`,
      '退出班级',
      {
        confirmButtonText: '确认退出',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'danger-warning-message-box',
      },
    )
  } catch {
    return
  }
  leaving.value = true
  const result = await del<void>(`/class/${route.params.id}/members/me`)
  leaving.value = false
  if (result.code === 200) {
    ElMessage.success(result.message || '已退出班级')
    router.push('/student/courses')
  } else {
    ElMessage.error(result.message || '退出失败')
  }
}

const loadCourse = async () => {
  const result = await get<CourseInfo>(`/class/${route.params.id}`)
  if (result.code === 200) {
    course.value = result.data!
    return true
  }
  // 无权访问（非班级成员）或班级不存在时提示并返回课程列表
  ElMessage.error(result.message || '无法访问该课程')
  router.push('/student/courses')
  return false
}

const loadWorks = async () => {
  const result = await get<{ records: WorkInfo[] }>('/works')
  if (result.code === 200) {
    const classId = Number(route.params.id)
    works.value = result.data!.records.filter(work => work.classId === classId)
  }
}

const loadSubmissions = async () => {
  const result = await get<SubmissionInfo[]>('/submissions/student/list')
  if (result.code === 200) {
    submissions.value = result.data!
  }
}

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const getWorkStatus = (work: WorkInfo) => {
  const now = new Date()
  const deadline = new Date(work.deadline)
  if (now > deadline) return 'expired'
  if (work.status === 1) return 'active'
  return 'pending'
}

const getWorkStatusText = (work: WorkInfo) => {
  const status = getWorkStatus(work)
  const map: Record<string, string> = {
    active: '进行中',
    expired: '已截止',
    pending: '未发布'
  }
  return map[status] || status
}

const getSubmissionStatus = (work: WorkInfo) => {
  const submission = submissions.value.find(s => s.workId === work.id)
  if (!submission) {
    const now = new Date()
    const deadline = new Date(work.deadline)
    return now > deadline ? 'late' : 'pending'
  }
  if (submission.status === 2) return 'graded'
  if (submission.status === 1) return 'submitted'
  return 'pending'
}

const goBack = () => {
  router.push('/student/courses')
}

const goToWork = (workId: number) => {
  router.push(`/student/work/${workId}`)
}

onMounted(async () => {
  const loaded = await loadCourse()
  if (loaded) {
    await loadWorks()
    await loadSubmissions()
  }
})
</script>

<style scoped>
.student-course-detail {
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

.course-info-card {
  margin-bottom: 24px;
}

.info-section {
  display: flex;
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

.role-badge.student {
  background: rgba(102, 126, 234, 0.2);
  color: #667eea;
  padding: 2px 8px;
  border-radius: 4px;
}

.description-section h4 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
}

.description-section p {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
  line-height: 1.6;
}

.works-card {
  margin-bottom: 20px;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
}

.work-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.work-item {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s;
  cursor: pointer;
}

.work-item:hover {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.05);
  border-color: rgba(102, 126, 234, 0.3);
}

.work-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-row h4 {
  font-size: 15px;
  font-weight: 600;
}

.pin-badge {
  color: #fbbf24;
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

.work-info {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.work-info .info-item {
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.submission-status {
  display: flex;
  justify-content: flex-end;
}

.submission-status span {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.submitted {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.graded {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.late {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.pending {
  background: rgba(156, 163, 175, 0.2);
  color: #9ca3af;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

.invite-flow-tip {
  margin: 0;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.55);
  line-height: 1.5;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-header .header-right {
    display: flex;
    justify-content: flex-end;
  }

  .page-header .header-right .el-button {
    width: 100%;
    min-height: 42px;
  }
}
</style>
