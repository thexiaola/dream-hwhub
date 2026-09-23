<template>
  <div class="admin-panel">
    <div class="page-header">
      <h2>管理面板</h2>
    </div>

    <el-tabs v-model="activeTab" class="admin-tabs" @tab-change="handleTabChange">
      <!-- 加入班级申请 -->
      <el-tab-pane v-if="canApproveJoin" label="加入班级申请" name="join">
        <div class="filter-bar">
          <span class="filter-bar__label">
            <SlidersHorizontal :size="14" />
            状态
          </span>
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
                <span class="label">申请人：</span>
                <span class="value">{{ app.applicantName || '未填写姓名' }}</span>
              </div>
              <div class="app-info">
                <span class="label">学号：</span>
                <span class="value">{{ app.applicantNo || '未填写学号' }}</span>
              </div>
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
              <el-button type="primary" size="small" @click="openReviewDialog(app.id, true)">
                通过
              </el-button>
              <el-button type="danger" size="small" @click="openReviewDialog(app.id, false)">
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
      <el-tab-pane v-if="canViewAllClasses" label="班级管理" name="classes">
        <div class="filter-bar">
          <el-select
            v-model="classSchoolId"
            placeholder="全部学校"
            style="width: 200px"
            clearable
            @change="reloadClasses"
          >
            <el-option
              v-for="school in classSchoolOptions"
              :key="school.id"
              :label="school.schoolName"
              :value="school.id"
            />
          </el-select>
          <el-input
            v-model="classSearchKeyword"
            placeholder="搜索班级名称"
            style="width: 240px"
            clearable
            @clear="reloadClasses"
            @keyup.enter="reloadClasses"
          />
          <el-button type="primary" @click="reloadClasses">搜索</el-button>
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
                type="primary"
                size="small"
                plain
                @click="goManageClass(cls.id)"
              >
                进入管理
              </el-button>
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
                  v-if="member.roleCode === 0"
                  v-model="selectedAdminKickIds"
                  :label="member.userId"
                />
                <span class="member-name">{{ member.studentName || member.userName }}</span>
                <span v-if="member.studentNo" class="member-no">{{ member.studentNo }}</span>
                <span
                  :class="[
                    'member-role',
                    member.role === '课代表'
                      ? 'assistant'
                      : member.roleCode === 1
                        ? 'teacher'
                        : 'student',
                  ]"
                >
                  {{ member.role }}
                </span>
                <el-button
                  v-if="member.roleCode === 0"
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

      <!-- 用户管理 -->
      <el-tab-pane v-if="canViewUsers" label="用户管理" name="users" lazy>
        <UserManage />
      </el-tab-pane>

      <!-- 学校加入申请（平台管理员集中审核） -->
      <el-tab-pane v-if="canViewSchools" label="学校加入申请" name="schoolJoin">
        <div class="filter-bar">
          <span class="filter-bar__label">
            <SlidersHorizontal :size="14" />
            状态
          </span>
          <el-radio-group v-model="schoolJoinFilter" @change="reloadSchoolJoinApplications">
            <el-radio-button :value="-1">全部</el-radio-button>
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="1">已通过</el-radio-button>
            <el-radio-button :value="2">已拒绝</el-radio-button>
          </el-radio-group>
          <el-select
            v-model="schoolJoinSchoolId"
            placeholder="全部学校"
            style="width: 200px"
            clearable
            @change="reloadSchoolJoinApplications"
          >
            <el-option
              v-for="school in classSchoolOptions"
              :key="school.id"
              :label="school.schoolName"
              :value="school.id"
            />
          </el-select>
          <div v-if="canUpdateSchool" class="batch-actions">
            <el-switch
              v-model="confirmBeforeApprove"
              size="small"
              active-text="通过前二次确认"
            />
            <el-button
              type="primary"
              size="small"
              :disabled="selectedSchoolApps.length === 0"
              @click="openSchoolJoinReview(true)"
            >
              批量通过
            </el-button>
            <el-button
              type="danger"
              size="small"
              :disabled="selectedSchoolApps.length === 0"
              @click="openSchoolJoinReview(false)"
            >
              批量拒绝
            </el-button>
          </div>
        </div>

        <el-table
          :data="schoolJoinApplications"
          v-loading="schoolJoinLoading"
          style="width: 100%"
          @selection-change="handleSchoolAppSelection"
        >
          <el-table-column v-if="canUpdateSchool" type="selection" width="45" :selectable="isPendingSchoolApp" />
          <el-table-column label="学校" min-width="150">
            <template #default="{ row }">{{ row.schoolName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="applicantUsername" label="申请账号" min-width="120" />
          <el-table-column prop="applicantName" label="姓名" min-width="100" />
          <el-table-column prop="applicantNo" label="学工号" min-width="110" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <span :class="['status-text', getStatusClass(row.status)]">
                {{ getStatusText(row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" min-width="160">
            <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="审核人" min-width="110">
            <template #default="{ row }">{{ row.reviewerName || '-' }}</template>
          </el-table-column>
          <el-table-column label="审核时间" min-width="160">
            <template #default="{ row }">{{ row.reviewTime ? formatDate(row.reviewTime) : '-' }}</template>
          </el-table-column>
          <el-table-column label="审核意见" min-width="140">
            <template #default="{ row }">{{ row.reviewComment || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 0 && canUpdateSchool">
                <el-button
                  type="primary"
                  size="small"
                  text
                  :loading="reviewSubmitting"
                  :disabled="reviewSubmitting"
                  @click="openSchoolJoinReview(true, row.id)"
                >
                  通过
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  text
                  :disabled="reviewSubmitting"
                  @click="openSchoolJoinReview(false, row.id)"
                >
                  拒绝
                </el-button>
              </template>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            :current-page="schoolJoinPage"
            :page-size="10"
            :total="schoolJoinTotal"
            layout="prev, pager, next"
            @current-change="handleSchoolJoinPageChange"
          />
        </div>

        <!-- 审核加入学校申请 -->
        <el-dialog v-model="schoolJoinDialog.visible" :title="schoolJoinTitle" width="420px" class="dark-dialog">
          <!-- 通过无需说明原因，直接展示确认提示；只有拒绝才填意见 -->
          <p v-if="schoolJoinDialog.approved" class="confirm-hint">
            确认通过{{ schoolJoinDialog.applicationIds.length > 1 ? `这 ${schoolJoinDialog.applicationIds.length} 条` : '这条' }}申请？
            通过后申请人将直接加入学校。
          </p>
          <el-form v-else label-width="70px">
            <el-form-item label="拒绝原因">
              <el-input
                v-model="schoolJoinDialog.comment"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="选填，建议说明原因"
              />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="schoolJoinDialog.visible = false">取消</el-button>
            <el-button
              :type="schoolJoinDialog.approved ? 'primary' : 'danger'"
              :loading="schoolJoinDialog.submitting"
              @click="submitSchoolJoinReview"
            >
              确认{{ schoolJoinDialog.approved ? '通过' : '拒绝' }}
            </el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 权限组 -->
      <el-tab-pane v-if="canViewPermissions" label="权限组" name="groups" lazy>
        <PermissionGroupManage />
      </el-tab-pane>

      <!-- 学校管理 -->
      <el-tab-pane v-if="canViewSchools" label="学校管理" name="schools" lazy>
        <SchoolManage />
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
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { get, put, del } from '@/utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FileText, ShieldAlert, SlidersHorizontal } from '@lucide/vue'
import { useUserStore } from '@/stores/user'
import UserManage from './UserManage.vue'
import PermissionGroupManage from './PermissionGroupManage.vue'
import SchoolManage from './SchoolManage.vue'
import type { School as SchoolInfo } from '@/types/school'
import { formatDateTime as formatDate } from '@/utils/format'

interface ClassJoinApplication {
  id: number
  classId: number
  applicantId: number
  applicantName: string | null
  applicantNo: string | null
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

type AdminTab = 'join' | 'classes' | 'users' | 'groups' | 'schools' | 'schoolJoin'

const activeTab = ref<AdminTab>('join')

// 按权限节点控制各页签可见性
const canApproveJoin = computed(() => userStore.hasPermission('class:approve_join'))
const canViewAllClasses = computed(() => userStore.hasPermission('class:view_all'))
const canViewUsers = computed(() => userStore.hasPermission('user:view'))
const canViewPermissions = computed(() => userStore.hasPermission('permission:view'))
const canViewSchools = computed(() => userStore.hasPermission('school:view_all'))
// 审核（含批量）需要 school:update，避免只读角色看得到按钮却点不动
const canUpdateSchool = computed(() => userStore.hasPermission('school:update'))

// 通过申请前是否需要二次确认：前端偏好，存本地
const CONFIRM_APPROVE_KEY = 'schoolReviewConfirmBeforeApprove'

// 隐私模式下 localStorage 可能不可写，失败时回退到默认值
const readConfirmBeforeApprove = (): boolean => {
  try {
    return localStorage.getItem(CONFIRM_APPROVE_KEY) !== '0'
  } catch {
    return true
  }
}

const confirmBeforeApprove = ref(readConfirmBeforeApprove())

// 审核请求进行中：关闭二次确认时没有弹窗可反馈，靠它禁用行内按钮避免重复提交
const reviewSubmitting = ref(false)

watch(confirmBeforeApprove, (value: boolean) => {
  try {
    localStorage.setItem(CONFIRM_APPROVE_KEY, value ? '1' : '0')
  } catch {
    // 存储不可用时仅本次会话生效
  }
})

const visibleTabs = computed<AdminTab[]>(() => {
  const tabs: AdminTab[] = []
  if (canApproveJoin.value) tabs.push('join')
  if (canViewAllClasses.value) tabs.push('classes')
  if (canViewUsers.value) tabs.push('users')
  if (canViewPermissions.value) tabs.push('groups')
  if (canViewSchools.value) tabs.push('schools')
  if (canViewSchools.value) tabs.push('schoolJoin')
  return tabs
})

const handleTabChange = (name: string | number) => {
  if (name === 'join') {
    loadJoinApplications()
  } else if (name === 'classes') {
    // 学校筛选选项按需加载，避免切换到班级管理时下拉为空
    if (classSchoolOptions.value.length === 0) {
      loadClassSchoolOptions()
    }
    loadClasses()
  } else if (name === 'schoolJoin') {
    if (classSchoolOptions.value.length === 0) {
      loadClassSchoolOptions()
    }
    loadSchoolJoinApplications()
  }
}

const router = useRouter()

const userStore = useUserStore()

const joinApplications = ref<ClassJoinApplication[]>([])
const joinFilter = ref(-1)
const joinPage = ref(1)
const joinTotal = ref(0)

const reviewDialog = ref({
  visible: false,
  type: '' as 'join',
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

const openReviewDialog = (applicationId: number, approved: boolean) => {
  // 重新打开同一条申请时保留未提交的审核意见
  const comment = reviewDialog.value.applicationId === applicationId ? reviewDialog.value.comment : ''
  reviewDialog.value = {
    visible: true,
    type: 'join',
    applicationId,
    approved,
    comment
  }
}

const submitReview = async () => {
  const { applicationId, approved, comment } = reviewDialog.value
  const result = await put('/class/applications/join/approve', { applicationId, approved, comment })
  if (result.code === 200) {
    ElMessage.success(approved ? '已通过' : '已拒绝')
    reviewDialog.value.visible = false
    reviewDialog.value.comment = ''
    loadJoinApplications()
  } else {
    ElMessage.error(result.message)
  }
}

onMounted(async () => {
  // 强制刷新一次用户信息，确保权限节点为最新（权限变更后无需重新登录）
  await userStore.getUserInfo(true)
  // 默认定位到当前用户有权限查看的第一个页签
  if (!visibleTabs.value.includes(activeTab.value)) {
    activeTab.value = visibleTabs.value[0] ?? 'join'
  }
  if (activeTab.value === 'join') {
    loadJoinApplications()
  } else if (activeTab.value === 'classes') {
    loadClassSchoolOptions()
    loadClasses()
  } else if (activeTab.value === 'schoolJoin') {
    loadClassSchoolOptions()
    loadSchoolJoinApplications()
  }
})

interface ClassInfoSimple {
  id: number
  className: string
  ownerId: number
  description: string
  createTime: string
}

interface SchoolJoinApplicationRow {
  id: number
  schoolId: number
  schoolName: string | null
  applicantId: number
  applicantUsername: string | null
  applicantName: string | null
  applicantNo: string | null
  status: number
  reviewerName: string | null
  reviewTime: string | null
  reviewComment: string | null
  createTime: string
}

// ===== 学校加入申请（平台管理员集中审核）=====
const schoolJoinApplications = ref<SchoolJoinApplicationRow[]>([])
const schoolJoinFilter = ref(0)
const schoolJoinSchoolId = ref<number | undefined>(undefined)
const schoolJoinPage = ref(1)
const schoolJoinTotal = ref(0)
const schoolJoinLoading = ref(false)
const selectedSchoolApps = ref<SchoolJoinApplicationRow[]>([])

const schoolJoinDialog = reactive({
  visible: false,
  submitting: false,
  applicationIds: [] as number[],
  approved: false,
  comment: ''
})

const schoolJoinTitle = computed(() => {
  const batch = schoolJoinDialog.applicationIds.length > 1
  const action = schoolJoinDialog.approved ? '通过' : '拒绝'
  return batch ? `批量${action} ${schoolJoinDialog.applicationIds.length} 条申请` : `${action}加入学校申请`
})

const handleSchoolAppSelection = (rows: SchoolJoinApplicationRow[]) => {
  selectedSchoolApps.value = rows
}

// 只有待审核的申请可以勾选
const isPendingSchoolApp = (row: SchoolJoinApplicationRow) => row.status === 0

const loadSchoolJoinApplications = async () => {
  schoolJoinLoading.value = true
  try {
    const params: Record<string, unknown> = { pageNum: schoolJoinPage.value, pageSize: 10 }
    if (schoolJoinFilter.value >= 0) {
      params.status = schoolJoinFilter.value
    }
    if (schoolJoinSchoolId.value) {
      params.schoolId = schoolJoinSchoolId.value
    }
    const result = await get<PageResult<SchoolJoinApplicationRow>>('/admin/schools/applications', params)
    if (result.code === 200) {
      schoolJoinApplications.value = result.data!.records
      schoolJoinTotal.value = result.data!.total
    } else {
      ElMessage.error(result.message)
    }
  } finally {
    schoolJoinLoading.value = false
  }
}

// 筛选条件变化后回到第一页再查询
const reloadSchoolJoinApplications = () => {
  schoolJoinPage.value = 1
  loadSchoolJoinApplications()
}

const handleSchoolJoinPageChange = (current: number) => {
  schoolJoinPage.value = current
  loadSchoolJoinApplications()
}

const openSchoolJoinReview = (approved: boolean, applicationId?: number) => {
  let applicationIds: number[]
  if (applicationId === undefined) {
    if (selectedSchoolApps.value.length === 0) {
      return
    }
    applicationIds = selectedSchoolApps.value.map(row => row.id)
    schoolJoinDialog.comment = ''
  } else {
    // 重新打开同一条申请时保留未提交的审核意见
    if (schoolJoinDialog.applicationIds.length !== 1 || schoolJoinDialog.applicationIds[0] !== applicationId) {
      schoolJoinDialog.comment = ''
    }
    applicationIds = [applicationId]
  }

  // 关闭二次确认时，通过操作直接提交
  if (approved && !confirmBeforeApprove.value) {
    submitSchoolJoinReview({ applicationIds, approved: true, comment: '' })
    return
  }

  schoolJoinDialog.visible = true
  schoolJoinDialog.submitting = false
  schoolJoinDialog.applicationIds = applicationIds
  schoolJoinDialog.approved = approved
}

interface SchoolJoinReviewTarget {
  applicationIds: number[]
  approved: boolean
  comment: string
}

const submitSchoolJoinReview = async (target?: SchoolJoinReviewTarget) => {
  const applicationIds = target?.applicationIds ?? schoolJoinDialog.applicationIds
  const approved = target?.approved ?? schoolJoinDialog.approved
  const comment = target?.comment ?? schoolJoinDialog.comment
  if (applicationIds.length === 0) {
    return
  }

  const isBatch = applicationIds.length > 1
  // 通过申请无需说明原因，只有拒绝时才提交意见
  const payload: Record<string, unknown> = { applicationIds, approved }
  if (!approved) {
    payload.comment = comment.trim()
  }

  reviewSubmitting.value = true
  schoolJoinDialog.submitting = true
  try {
    // 单条与批量都走同一接口，服务端按各自学校校验权限
    const result = await put<{ handled: number; skipped: number }>(
      '/admin/schools/applications/batch-approve',
      payload
    )
    if (result.code === 200) {
      // 已被他人处理的申请不会真正变更，按服务端返回的处理条数提示
      if (!result.data?.handled) {
        ElMessage.warning('所选申请均已被处理，未做变更')
      } else {
        ElMessage.success(isBatch ? result.message : approved ? '已通过' : '已拒绝')
      }
      schoolJoinDialog.visible = false
      schoolJoinDialog.comment = ''
      schoolJoinDialog.applicationIds = []
      selectedSchoolApps.value = []
      reloadSchoolJoinApplications()
    } else {
      ElMessage.error(result.message)
    }
  } finally {
    // 网络异常时也要复位，否则行内按钮会一直处于禁用态
    schoolJoinDialog.submitting = false
    reviewSubmitting.value = false
  }
}

interface ClassMemberInfo {
  id: number
  userId: number
  userName: string
  studentName: string | null
  studentNo: string | null
  role: string
  /** 1-拥有班级管理员权限，0-普通成员 */
  roleCode: number
}

const classList = ref<ClassInfoSimple[]>([])
const classTotal = ref(0)
const classPage = ref(1)
const classSchoolId = ref<number | undefined>(undefined)
const classSchoolOptions = ref<SchoolInfo[]>([])
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
  if (classSchoolId.value) {
    params.schoolId = classSchoolId.value
  }
  const result = await get<PageResult<ClassInfoSimple>>('/class/manage', params)
  if (result.code === 200) {
    classList.value = result.data!.records
    classTotal.value = result.data!.total
  }
}

// 筛选条件变化后回到第一页再查询
const reloadClasses = () => {
  classPage.value = 1
  loadClasses()
}

const loadClassSchoolOptions = async () => {
  const result = await get<PageResult<SchoolInfo>>('/school/list', { pageNum: 1, pageSize: 200 })
  if (result.code === 200) {
    classSchoolOptions.value = result.data!.records
  }
}

const goManageClass = (classId: number) => {
  router.push(`/teacher/course/${classId}`)
}

const toggleClassDetail = async (classId: number) => {
  if (expandedClassId.value === classId) {
    expandedClassId.value = null
    classMembers.value = []
    return
  }
  expandedClassId.value = classId
  selectedAdminKickIds.value = []
  const result = await get<{ records: ClassMemberInfo[] }>(`/class/${classId}/members`, { pageSize: 300 })
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
  return (u.username || u.email || '-') as string
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
    // 密码走请求体（DELETE body），避免出现在 URL/访问日志中
    const result = await del(`/class/${classId}`, params)
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
    const result = await del(`/class/${classId}/members/batch`, [userId])
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
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.admin-tabs :deep(.el-tabs__item) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.admin-tabs :deep(.el-tabs__item.is-active) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.admin-tabs :deep(.el-tabs__item:hover) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
}

.admin-tabs :deep(.el-tabs__active-bar) {
  background-color: #667eea;
}

.admin-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
}

.application-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.application-card {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
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
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.app-time {
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

/* 通过申请前的确认提示 */
.confirm-hint {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
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
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.app-info .value {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
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
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
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
  border-top: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
}

.members-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
}

.member-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border-radius: 8px;
  margin-bottom: 8px;
}

.member-name {
  font-size: 14px;
  font-weight: 500;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
  min-width: 100px;
}

.member-no {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
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

.member-role.assistant {
  background: rgba(64, 158, 255, 0.2);
  color: #409eff;
}

.member-role.student {
  background: rgba(156, 163, 175, 0.2);
  color: #9ca3af;
}

.empty-tip {
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
  text-align: center;
  padding: 16px;
}

/* el-radio-button 做成凹槽轨道内的分段控件：
   边界由外层 .el-radio-group 的 background 提供，按钮自身不再描边，
   避免 Element Plus 默认 outline 在浮起卡片上形成灰色硬线。 */
.filter-bar :deep(.el-radio-button__inner) {
  background: transparent !important;
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
  border-radius: 8px !important;
  padding: 7px 16px !important;
  font-size: 13px !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.62) !important;
  transition: background-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.filter-bar :deep(.el-radio-button:not(.is-active) .el-radio-button__inner:hover) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06) !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9) !important;
}

/* 键盘聚焦时保留可见的焦点指示 */
.filter-bar :deep(.el-radio-button__original-radio:focus-visible + .el-radio-button__inner) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.55) !important;
}

.filter-bar :deep(.el-radio-button.is-active .el-radio-button__inner),
.filter-bar :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  color: var(--fg-on-accent) !important;
  font-weight: 600;
  box-shadow: 0 2px 8px -2px rgba(102, 126, 234, 0.65) !important;
}

.filter-bar :deep(.el-radio-button.is-disabled .el-radio-button__inner) {
  background: transparent !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.25) !important;
  cursor: not-allowed;
}

.admin-tabs :deep(.el-pagination .el-pagination__total),
.admin-tabs :deep(.el-pagination button:disabled),
.admin-tabs :deep(.el-pagination .btn-prev),
.admin-tabs :deep(.el-pagination .btn-next),
.admin-tabs :deep(.el-pagination .el-pager li) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
  background: transparent;
}

.admin-tabs :deep(.el-pagination .el-pager li.is-active) {
  color: #667eea;
}

.admin-tabs :deep(.el-checkbox__label) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
}

.admin-tabs :deep(.el-checkbox__inner) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06);
  border-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.25);
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

  .filter-bar :deep(.el-radio-group) {
    min-width: max-content;
  }

  .filter-bar :deep(.el-radio-button__inner) {
    padding: 6px 12px !important;
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
