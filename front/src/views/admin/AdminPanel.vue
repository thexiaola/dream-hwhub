<template>
  <div class="admin-panel">
    <div class="page-header">
      <h2>管理面板</h2>
    </div>

    <el-tabs v-model="activeTab" class="admin-tabs">
      <!-- 创建班级申请 -->
      <el-tab-pane label="创建班级申请" name="create">
        <div class="filter-bar">
          <el-radio-group v-model="createFilter" @change="loadCreateApplications">
            <el-radio-button :value="-1">全部</el-radio-button>
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="1">已通过</el-radio-button>
            <el-radio-button :value="2">已拒绝</el-radio-button>
          </el-radio-group>
        </div>

        <div class="application-list">
          <div v-for="app in createApplications" :key="app.id" class="application-card">
            <div class="app-header">
              <div class="app-title">
                <h4>{{ app.className }}</h4>
                <span :class="['status-tag', getStatusClass(app.status)]">
                  {{ getStatusText(app.status) }}
                </span>
              </div>
              <span class="app-time">申请时间：{{ formatDate(app.createTime) }}</span>
            </div>
            <div class="app-body">
              <div class="app-info">
                <span class="label">申请人ID：</span>
                <span class="value">{{ app.applicantId }}</span>
              </div>
              <div class="app-info" v-if="app.description">
                <span class="label">班级描述：</span>
                <span class="value">{{ app.description }}</span>
              </div>
              <div class="app-info" v-if="app.reviewComment">
                <span class="label">审核意见：</span>
                <span class="value">{{ app.reviewComment }}</span>
              </div>
              <div class="app-info" v-if="app.createdClassId">
                <span class="label">已创建班级ID：</span>
                <span class="value">{{ app.createdClassId }}</span>
              </div>
            </div>
            <div class="app-actions" v-if="app.status === 0">
              <el-button type="primary" size="small" @click="openReviewDialog('create', app.id, true)">
                通过
              </el-button>
              <el-button type="danger" size="small" @click="openReviewDialog('create', app.id, false)">
                拒绝
              </el-button>
            </div>
          </div>
          <div v-if="createApplications.length === 0" class="empty-state">
            <FileText :size="32" />
            <p>暂无创建班级申请</p>
          </div>
        </div>

        <div class="pagination" v-if="createTotal > 0">
          <el-pagination
            v-model:current-page="createPage"
            :page-size="10"
            :total="createTotal"
            layout="prev, pager, next"
            @current-change="loadCreateApplications"
          />
        </div>
      </el-tab-pane>

      <!-- 加入班级申请 -->
      <el-tab-pane label="加入班级申请" name="join">
        <div class="filter-bar">
          <el-radio-group v-model="joinFilter" @change="loadJoinApplications">
            <el-radio-button :value="-1">全部</el-radio-button>
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="1">已通过</el-radio-button>
            <el-radio-button :value="2">已拒绝</el-radio-button>
          </el-radio-group>
        </div>

        <div class="application-list">
          <div v-for="app in joinApplications" :key="app.id" class="application-card">
            <div class="app-header">
              <div class="app-title">
                <h4>申请加入班级 #{{ app.classId }}</h4>
                <span :class="['status-tag', getStatusClass(app.status)]">
                  {{ getStatusText(app.status) }}
                </span>
              </div>
              <span class="app-time">申请时间：{{ formatDate(app.createTime) }}</span>
            </div>
            <div class="app-body">
              <div class="app-info">
                <span class="label">申请人ID：</span>
                <span class="value">{{ app.applicantId }}</span>
              </div>
              <div class="app-info">
                <span class="label">班级ID：</span>
                <span class="value">{{ app.classId }}</span>
              </div>
              <div class="app-info" v-if="app.reviewComment">
                <span class="label">审核意见：</span>
                <span class="value">{{ app.reviewComment }}</span>
              </div>
            </div>
            <div class="app-actions" v-if="app.status === 0">
              <el-button type="primary" size="small" @click="openReviewDialog('join', app.id, true)">
                通过
              </el-button>
              <el-button type="danger" size="small" @click="openReviewDialog('join', app.id, false)">
                拒绝
              </el-button>
            </div>
          </div>
          <div v-if="joinApplications.length === 0" class="empty-state">
            <FileText :size="32" />
            <p>暂无加入班级申请</p>
          </div>
        </div>

        <div class="pagination" v-if="joinTotal > 0">
          <el-pagination
            v-model:current-page="joinPage"
            :page-size="10"
            :total="joinTotal"
            layout="prev, pager, next"
            @current-change="loadJoinApplications"
          />
        </div>
      </el-tab-pane>

      <!-- 班级管理 -->
      <el-tab-pane label="班级管理" name="classes">
        <div class="filter-bar">
          <el-input
            v-model="classSearchKeyword"
            placeholder="搜索班级名称"
            style="width: 240px"
            clearable
            @clear="loadClasses"
            @keyup.enter="loadClasses"
          />
          <el-button type="primary" @click="loadClasses">搜索</el-button>
        </div>

        <div class="class-list">
          <div v-for="cls in classList" :key="cls.id" class="application-card">
            <div class="app-header">
              <div class="app-title">
                <h4>{{ cls.className }}</h4>
              </div>
              <span class="app-time">创建时间：{{ formatDate(cls.createTime) }}</span>
            </div>
            <div class="app-body">
              <div class="app-info">
                <span class="label">班级ID：</span>
                <span class="value">{{ cls.id }}</span>
              </div>
              <div class="app-info">
                <span class="label">创建者ID：</span>
                <span class="value">{{ cls.ownerId }}</span>
              </div>
              <div class="app-info" v-if="cls.description">
                <span class="label">描述：</span>
                <span class="value">{{ cls.description }}</span>
              </div>
            </div>
            <div class="class-actions">
              <el-button
                type="danger"
                size="small"
                @click="dissolveClass(cls.id, cls.className)"
              >
                解散课堂
              </el-button>
              <el-button
                size="small"
                @click="toggleClassDetail(cls.id)"
              >
                {{ expandedClassId === cls.id ? '收起成员' : '查看成员' }}
              </el-button>
            </div>
            <div v-if="expandedClassId === cls.id" class="class-members">
              <div class="members-header">
                <span>班级成员 ({{ classMembers.length }})</span>
                <el-button
                  v-if="selectedAdminKickIds.length > 0"
                  type="danger"
                  size="small"
                  @click="batchKickFromAdmin(cls.id)"
                >
                  批量踢出 ({{ selectedAdminKickIds.length }})
                </el-button>
              </div>
              <div v-if="classMembers.length === 0" class="empty-tip">暂无成员</div>
              <div v-for="member in classMembers" :key="member.id" class="member-row">
                <el-checkbox
                  v-if="member.role === '学生'"
                  v-model="selectedAdminKickIds"
                  :label="member.userId"
                />
                <span class="member-name">{{ member.userName }}</span>
                <span class="member-no">{{ member.userNo }}</span>
                <span :class="['member-role', member.role === '老师' ? 'teacher' : 'student']">
                  {{ member.role }}
                </span>
                <el-button
                  v-if="member.role === '学生'"
                  type="danger"
                  size="small"
                  text
                  @click="kickStudentFromAdmin(cls.id, member.userId)"
                >
                  踢出
                </el-button>
              </div>
            </div>
          </div>
          <div v-if="classList.length === 0" class="empty-state">
            <FileText :size="32" />
            <p>暂无班级</p>
          </div>
        </div>

        <div class="pagination" v-if="classTotal > 0">
          <el-pagination
            v-model:current-page="classPage"
            :page-size="10"
            :total="classTotal"
            layout="prev, pager, next"
            @current-change="loadClasses"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 审核对话框 -->
    <el-dialog v-model="reviewDialog.visible" :title="reviewDialog.approved ? '通过申请' : '拒绝申请'" width="450px" class="dark-dialog">
      <el-form label-width="80px">
        <el-form-item label="审核意见">
          <el-input
            v-model="reviewDialog.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入审核意见（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialog.visible = false">取消</el-button>
        <el-button :type="reviewDialog.approved ? 'primary' : 'danger'" @click="submitReview">
          确认{{ reviewDialog.approved ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ========== 危险操作 Step2：密码校验 ========== -->
    <el-dialog
      v-model="showDangerPasswordDialog"
      title="最终确认"
      width="460px"
      class="dark-dialog danger-dialog"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
      @close="clearDangerInputs"
    >
      <div class="danger-content">
        <div class="danger-icon">
          <ShieldAlert :size="22" />
        </div>
        <div class="danger-info">
          <p class="danger-title">请输入登录密码以继续</p>
          <p class="danger-desc">
            当前操作账号：
            <b class="danger-strong">{{ currentAccountDisplay }}</b>
          </p>
        </div>
      </div>
      <el-input
        v-model="dangerPassword"
        type="password"
        show-password
        placeholder="请输入登录密码"
        @keyup.enter="passwordDialogConfirm"
      />
      <template #footer>
        <el-button @click="passwordDialogCancel">取消</el-button>
        <el-button
          type="danger"
          :disabled="dangerPassword.length === 0"
          :loading="dangerSubmitting"
          @click="passwordDialogConfirm"
        >
          确认解散课堂
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { get, put, del } from '@/utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FileText, ShieldAlert } from '@lucide/vue'
import { useUserStore } from '@/stores/user'

interface ClassCreateApplication {
  id: number
  applicantId: number
  className: string
  description: string
  status: number
  reviewerId: number | null
  reviewTime: string | null
  reviewComment: string | null
  createdClassId: number | null
  createTime: string
}

interface ClassJoinApplication {
  id: number
  classId: number
  applicantId: number
  status: number
  reviewerId: number | null
  reviewTime: string | null
  reviewComment: string | null
  createTime: string
}

interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

const activeTab = ref<'create' | 'join' | 'classes'>('create')

const userStore = useUserStore()

const createApplications = ref<ClassCreateApplication[]>([])
const createFilter = ref(-1)
const createPage = ref(1)
const createTotal = ref(0)

const joinApplications = ref<ClassJoinApplication[]>([])
const joinFilter = ref(-1)
const joinPage = ref(1)
const joinTotal = ref(0)

const reviewDialog = ref({
  visible: false,
  type: '' as 'create' | 'join',
  applicationId: 0,
  approved: false,
  comment: ''
})

const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
  return map[status] || '未知'
}

const getStatusClass = (status: number) => {
  const map: Record<number, string> = { 0: 'pending', 1: 'approved', 2: 'rejected' }
  return map[status] || 'pending'
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return '-'
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const loadCreateApplications = async () => {
  const params: Record<string, unknown> = {
    pageNum: createPage.value,
    pageSize: 10
  }
  if (createFilter.value >= 0) {
    params.status = createFilter.value
  }
  const result = await get<PageResult<ClassCreateApplication>>('/class/applications/create/list', params)
  if (result.code === 200) {
    createApplications.value = result.data!.records
    createTotal.value = result.data!.total
  }
}

const loadJoinApplications = async () => {
  const params: Record<string, unknown> = {
    pageNum: joinPage.value,
    pageSize: 10
  }
  if (joinFilter.value >= 0) {
    params.status = joinFilter.value
  }
  const result = await get<PageResult<ClassJoinApplication>>('/class/applications/join/list', params)
  if (result.code === 200) {
    joinApplications.value = result.data!.records
    joinTotal.value = result.data!.total
  }
}

const openReviewDialog = (type: 'create' | 'join', applicationId: number, approved: boolean) => {
  reviewDialog.value = {
    visible: true,
    type,
    applicationId,
    approved,
    comment: ''
  }
}

const submitReview = async () => {
  const { type, applicationId, approved, comment } = reviewDialog.value
  const url = type === 'create'
    ? '/class/applications/create/approve'
    : '/class/applications/join/approve'
  const result = await put(url, { applicationId, approved, comment })
  if (result.code === 200) {
    ElMessage.success(approved ? '已通过' : '已拒绝')
    reviewDialog.value.visible = false
    if (type === 'create') {
      loadCreateApplications()
    } else {
      loadJoinApplications()
    }
  } else {
    ElMessage.error(result.message)
  }
}

onMounted(() => {
  loadCreateApplications()
  loadJoinApplications()
  loadClasses()
})

interface ClassInfoSimple {
  id: number
  className: string
  ownerId: number
  description: string
  createTime: string
}

interface ClassMemberInfo {
  id: number
  userId: number
  userName: string
  userNo: string
  role: string
}

const classList = ref<ClassInfoSimple[]>([])
const classTotal = ref(0)
const classPage = ref(1)
const classSearchKeyword = ref('')
const expandedClassId = ref<number | null>(null)
const classMembers = ref<ClassMemberInfo[]>([])
const selectedAdminKickIds = ref<number[]>([])

const loadClasses = async () => {
  const params: Record<string, unknown> = {
    pageNum: classPage.value,
    pageSize: 10
  }
  if (classSearchKeyword.value) {
    params.keyword = classSearchKeyword.value
  }
  const result = await get<PageResult<ClassInfoSimple>>('/class/mine', params)
  if (result.code === 200) {
    classList.value = result.data!.records
    classTotal.value = result.data!.total
  }
}

const toggleClassDetail = async (classId: number) => {
  if (expandedClassId.value === classId) {
    expandedClassId.value = null
    classMembers.value = []
    return
  }
  expandedClassId.value = classId
  selectedAdminKickIds.value = []
  const result = await get<{ records: ClassMemberInfo[] }>(`/class/${classId}/members`)
  if (result.code === 200) {
    classMembers.value = result.data!.records
  }
}

const dissolveClass = async (classId: number, className: string) => {
  try {
    await ElMessageBox.confirm(
      `解散课堂"${className}"后，所有数据将被永久删除，此操作不可恢复。确认解散？`,
      '危险操作',
      { confirmButtonText: '确认解散', cancelButtonText: '取消', type: 'warning', customClass: 'danger-warning-message-box' }
    )
    pendingDissolve.value.classId = classId
    pendingDissolve.value.className = className
    showDangerPasswordDialog.value = true
  } catch {
    clearDangerInputs()
  }
}

// ========== 危险操作（解散课堂）两步弹窗状态 ==========
const showDangerPasswordDialog = ref(false)
const dangerPassword = ref('')
const dangerSubmitting = ref(false)
const pendingDissolve = ref<{ classId: number; className: string }>({ classId: 0, className: '' })

const currentAccountDisplay = computed(() => {
  const u = userStore.userInfo
  if (!u) return '-'
  return (u.userNo || u.username || u.email || '-') as string
})

const clearDangerInputs = () => {
  dangerPassword.value = ''
  dangerSubmitting.value = false
  showDangerPasswordDialog.value = false
  pendingDissolve.value = { classId: 0, className: '' }
}

const passwordDialogCancel = () => {
  clearDangerInputs()
}

const passwordDialogConfirm = async () => {
  if (!dangerPassword.value) {
    ElMessage.warning('请输入登录密码')
    return
  }
  if (!pendingDissolve.value.classId) {
    ElMessage.warning('请先选择要解散的课堂')
    return
  }
  dangerSubmitting.value = true
  const classId = pendingDissolve.value.classId
  const confirmText = `我已确认要删除${pendingDissolve.value.className ?? ''}课堂`
  try {
    const params = {
      password: dangerPassword.value,
      confirmText,
    }
    const result = await del(`/class/${classId}`, undefined, params)
    if (result.code === 200) {
      ElMessage.success('课堂已解散')
      if (expandedClassId.value === classId) {
        expandedClassId.value = null
        classMembers.value = []
      }
      clearDangerInputs()
      loadClasses()
    } else {
      ElMessage.error(result.message || '解散失败')
    }
  } catch {
    ElMessage.error('解散失败，请重试')
  } finally {
    dangerSubmitting.value = false
  }
}

const kickStudentFromAdmin = async (classId: number, userId: number) => {
  try {
    await ElMessageBox.confirm('确认踢出此学生？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    const result = await del(`/class/${classId}/members/${userId}`)
    if (result.code === 200) {
      ElMessage.success('已踢出')
      toggleClassDetail(classId)
    } else {
      ElMessage.error(result.message)
    }
  } catch {
    // 用户取消
  }
}

const batchKickFromAdmin = async (classId: number) => {
  if (selectedAdminKickIds.value.length === 0) {
    ElMessage.warning('请选择要踢出的学生')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认批量踢出 ${selectedAdminKickIds.value.length} 名学生？`,
      '提示',
      { confirmButtonText: '确认', cancelButtonText: '取消' }
    )
    const result = await del(`/class/${classId}/members/batch`, selectedAdminKickIds.value)
    if (result.code === 200) {
      ElMessage.success(`已踢出 ${selectedAdminKickIds.value.length} 名学生`)
      selectedAdminKickIds.value = []
      toggleClassDetail(classId)
    } else {
      ElMessage.error(result.message)
    }
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.admin-panel {
  padding-bottom: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: rgba(var(--rgb-fg), 0.95);
}

.filter-bar {
  margin-bottom: 20px;
}

.admin-tabs :deep(.el-tabs__item) {
  color: rgba(var(--rgb-fg), 0.6);
}

.admin-tabs :deep(.el-tabs__item.is-active) {
  color: rgba(var(--rgb-fg), 0.95);
}

.admin-tabs :deep(.el-tabs__item:hover) {
  color: rgba(var(--rgb-fg), 0.8);
}

.admin-tabs :deep(.el-tabs__active-bar) {
  background-color: #667eea;
}

.admin-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(var(--rgb-fg), 0.1);
}

.application-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.application-card {
  background: rgba(var(--rgb-fg), 0.03);
  border: 1px solid rgba(var(--rgb-fg), 0.1);
  border-radius: 12px;
  padding: 16px 20px;
  transition: border-color 0.3s;
}

.application-card:hover {
  border-color: rgba(102, 126, 234, 0.3);
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.app-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-title h4 {
  font-size: 16px;
  font-weight: 600;
  color: rgba(var(--rgb-fg), 0.95);
}

.app-time {
  font-size: 13px;
  color: rgba(var(--rgb-fg), 0.4);
}

.status-tag {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.pending {
  background: rgba(251, 191, 36, 0.2);
  color: #fbbf24;
}

.status-tag.approved {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.status-tag.rejected {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.app-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.app-info {
  font-size: 14px;
}

.app-info .label {
  color: rgba(var(--rgb-fg), 0.5);
}

.app-info .value {
  color: rgba(var(--rgb-fg), 0.9);
}

.app-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px;
  color: rgba(var(--rgb-fg), 0.4);
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.class-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.class-members {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(var(--rgb-fg), 0.1);
}

.members-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  color: rgba(var(--rgb-fg), 0.7);
}

.member-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: rgba(var(--rgb-fg), 0.03);
  border-radius: 8px;
  margin-bottom: 8px;
}

.member-name {
  font-size: 14px;
  font-weight: 500;
  color: rgba(var(--rgb-fg), 0.9);
  min-width: 100px;
}

.member-no {
  font-size: 12px;
  color: rgba(var(--rgb-fg), 0.5);
}

.member-role {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.member-role.teacher {
  background: rgba(102, 126, 234, 0.2);
  color: #667eea;
}

.member-role.student {
  background: rgba(156, 163, 175, 0.2);
  color: #9ca3af;
}

.empty-tip {
  font-size: 13px;
  color: rgba(var(--rgb-fg), 0.4);
  text-align: center;
  padding: 16px;
}

.filter-bar :deep(.el-input__wrapper) {
  background: rgba(var(--rgb-fg), 0.06) !important;
  border-color: rgba(var(--rgb-fg), 0.25) !important;
}

.filter-bar :deep(.el-input__wrapper:focus-within) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2) !important;
}

.filter-bar :deep(.el-input__inner) {
  color: rgba(var(--rgb-fg), 0.95) !important;
}

.filter-bar :deep(.el-input__inner::placeholder) {
  color: rgba(var(--rgb-fg), 0.4);
}

/* el-radio-button 深色主题适配
   注意：Element Plus 新版本 .el-radio-button__inner 的外边框用的是
   outline: var(--el-border)（即 1px solid var(--el-border-color)），
   不是 border！所以必须覆盖 outline / --el-border-color，覆盖 border-color 无效。 */
.filter-bar :deep(.el-radio-group) {
  --el-border-color: rgba(var(--rgb-fg), 0.2);
}

.filter-bar :deep(.el-radio-button__inner) {
  background: rgba(var(--rgb-fg), 0.05) !important;
  outline: 1px solid rgba(var(--rgb-fg), 0.2) !important;
  outline-offset: -1px;
  color: rgba(var(--rgb-fg), 0.7) !important;
  box-shadow: -1px 0 0 0 rgba(var(--rgb-fg), 0.2) inset !important;
  transition: background-color 0.2s ease, color 0.2s ease, outline-color 0.2s ease;
}

.filter-bar :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-top-left-radius: 8px !important;
  border-bottom-left-radius: 8px !important;
  box-shadow: none !important;
}

.filter-bar :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-top-right-radius: 8px !important;
  border-bottom-right-radius: 8px !important;
}

.filter-bar :deep(.el-radio-button:not(.is-active):hover .el-radio-button__inner) {
  background: rgba(var(--rgb-fg), 0.09) !important;
  color: rgba(var(--rgb-fg), 0.9) !important;
  outline-color: rgba(var(--rgb-fg), 0.32) !important;
  box-shadow: -1px 0 0 0 rgba(var(--rgb-fg), 0.32) inset !important;
}

.filter-bar :deep(.el-radio-button:first-child:not(.is-active):hover .el-radio-button__inner) {
  box-shadow: none !important;
}

.filter-bar :deep(.el-radio-button.is-active .el-radio-button__inner),
.filter-bar :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.85), rgba(118, 75, 162, 0.85)) !important;
  outline: 1px solid rgba(102, 126, 234, 0.8) !important;
  outline-offset: -1px;
  color: var(--fg-on-accent) !important;
  box-shadow: -1px 0 0 0 rgba(102, 126, 234, 0.8) inset !important;
  font-weight: 600;
}

.filter-bar :deep(.el-radio-button.is-active:first-child .el-radio-button__inner),
.filter-bar :deep(.el-radio-button:first-child .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  box-shadow: none !important;
}

.filter-bar :deep(.el-radio-button.is-disabled .el-radio-button__inner) {
  background: rgba(var(--rgb-fg), 0.02) !important;
  outline: 1px solid rgba(var(--rgb-fg), 0.08) !important;
  outline-offset: -1px;
  color: rgba(var(--rgb-fg), 0.25) !important;
  cursor: not-allowed;
}

.admin-tabs :deep(.el-pagination .el-pagination__total),
.admin-tabs :deep(.el-pagination button:disabled),
.admin-tabs :deep(.el-pagination .btn-prev),
.admin-tabs :deep(.el-pagination .btn-next),
.admin-tabs :deep(.el-pagination .el-pager li) {
  color: rgba(var(--rgb-fg), 0.6);
  background: transparent;
}

.admin-tabs :deep(.el-pagination .el-pager li.is-active) {
  color: #667eea;
}

.admin-tabs :deep(.el-checkbox__label) {
  color: rgba(var(--rgb-fg), 0.8);
}

.admin-tabs :deep(.el-checkbox__inner) {
  background: rgba(var(--rgb-fg), 0.06);
  border-color: rgba(var(--rgb-fg), 0.25);
}

.admin-tabs :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #667eea;
  border-color: #667eea;
}

/* 手机端适配 */
@media (max-width: 768px) {
  .page-header h2 {
    font-size: 20px;
  }

  .filter-bar {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .filter-bar .el-radio-group {
    gap: 4px;
    min-width: max-content;
  }

  .filter-bar .el-radio-button__inner {
    padding: 8px 14px !important;
    font-size: 12px !important;
  }

  .application-list {
    gap: 12px;
  }

  .application-card {
    padding: 14px 16px;
  }

  .app-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .app-actions {
    flex-wrap: wrap;
  }

  .app-actions .el-button {
    flex: 1;
    min-width: 0;
  }

  .admin-tabs {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .admin-tabs :deep(.el-tabs__header) {
    min-width: max-content;
  }

  .user-list-card {
    padding: 12px;
  }
}
</style>
