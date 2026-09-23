<template>
  <div class="school-detail-page">
    <el-page-header :content="school?.schoolName || '学校详情'" @back="router.back()" />

    <el-card class="content-card" v-loading="loading">
      <div class="overview">
        <div class="overview-main">
          <h2>{{ school?.schoolName }}</h2>
          <p class="description">{{ school?.description || '暂无描述' }}</p>
          <div class="meta-row">
            <span :class="['flag', school?.allowJoinWithoutApproval ? 'auto' : 'manual']">
              {{ school?.allowJoinWithoutApproval ? '免审核加入' : '需管理员审核' }}
            </span>
            <span class="meta-item">成员 {{ school?.memberCount ?? 0 }}</span>
            <span class="meta-item">老师 {{ school?.teacherCount ?? 0 }}</span>
            <span class="meta-item">学生 {{ school?.studentCount ?? 0 }}</span>
            <span class="meta-item">班级 {{ school?.classCount ?? 0 }}</span>
          </div>
        </div>

        <div class="overview-side">
          <template v-if="school?.member">
            <div class="identity-card">
              <div class="identity-line">
                <span class="label">我的身份</span>
                <span class="value">{{ school?.myRole }}</span>
              </div>
              <div class="identity-line">
                <span class="label">姓名</span>
                <span class="value">{{ school?.myRealName || '-' }}</span>
              </div>
              <div class="identity-line">
                <span class="label">学工号</span>
                <span class="value">{{ school?.myStaffNo || '-' }}</span>
              </div>
              <p class="identity-tip">姓名与学工号由学校管理员维护</p>
              <el-button
                v-if="school?.myRoleCode !== SCHOOL_ROLE_ADMIN"
                size="small"
                type="danger"
                plain
                @click="leaveSchool"
              >
                退出学校
              </el-button>
            </div>
          </template>
          <template v-else>
            <div class="identity-card">
              <p class="identity-tip" v-if="school?.myApplicationStatus === 0">
                你的加入申请正在等待学校管理员审核
              </p>
              <p class="identity-tip" v-else-if="school?.myApplicationStatus === 2">
                你的加入申请被拒绝，可修改信息后重新提交
              </p>
              <p
                v-if="school?.myApplicationStatus === 2 && school?.myApplicationComment"
                class="identity-comment"
              >
                拒绝理由：{{ school.myApplicationComment }}
              </p>
              <el-button type="primary" @click="openJoinDialog">加入学校</el-button>
            </div>
          </template>
        </div>
      </div>
    </el-card>

    <el-card class="content-card" v-if="canManage">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane name="applications">
          <template #label>
            加入申请
            <el-badge
              v-if="(school?.pendingApplicationCount ?? 0) > 0"
              :value="school?.pendingApplicationCount"
              class="tab-badge"
            />
          </template>
          <div class="filter-bar">
            <el-radio-group v-model="appFilter" @change="reloadApplications">
              <el-radio-button :value="-1">全部</el-radio-button>
              <el-radio-button :value="0">待审核</el-radio-button>
              <el-radio-button :value="1">已通过</el-radio-button>
              <el-radio-button :value="2">已拒绝</el-radio-button>
            </el-radio-group>
            <el-switch
              v-model="allowJoinWithoutApproval"
              active-text="免审核加入"
              inactive-text="需审核"
              @change="submitJoinApproval"
            />
            <div class="batch-actions">
              <el-switch
                v-model="confirmBeforeApprove"
                size="small"
                active-text="通过前二次确认"
              />
              <el-button
                type="primary"
                size="small"
                :disabled="selectedApplications.length === 0"
                @click="openBatchReview(true)"
              >
                批量通过
              </el-button>
              <el-button
                type="danger"
                size="small"
                :disabled="selectedApplications.length === 0"
                @click="openBatchReview(false)"
              >
                批量拒绝
              </el-button>
            </div>
          </div>

          <el-table
            :data="applications"
            style="width: 100%"
            @selection-change="handleAppSelection"
          >
            <el-table-column type="selection" width="45" :selectable="isPendingRow" />
            <el-table-column prop="applicantUsername" label="账号" min-width="120" />
            <el-table-column prop="applicantName" label="姓名" min-width="100">
              <template #default="{ row }">{{ row.applicantName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="applicantNo" label="学工号" min-width="120">
              <template #default="{ row }">{{ row.applicantNo || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <span :class="['status-tag', statusClass(row.status)]">{{ statusText(row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="申请时间" min-width="150">
              <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="审核人" min-width="100">
              <template #default="{ row }">{{ row.reviewerName || '-' }}</template>
            </el-table-column>
            <el-table-column label="审核时间" min-width="150">
              <template #default="{ row }">
                {{ row.reviewTime ? formatDate(row.reviewTime) : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="审核意见" min-width="140">
              <template #default="{ row }">{{ row.reviewComment || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 0">
                  <el-button
                    size="small"
                    type="primary"
                    text
                    :loading="reviewSubmitting"
                    :disabled="reviewSubmitting"
                    @click="openReview(row, true)"
                  >
                    通过
                  </el-button>
                  <el-button
                    size="small"
                    type="danger"
                    text
                    :disabled="reviewSubmitting"
                    @click="openReview(row, false)"
                  >
                    拒绝
                  </el-button>
                </template>
                <span v-else class="muted">{{ row.reviewComment || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination" v-if="appTotal > 0">
            <el-pagination
              v-model:current-page="appPage"
              :page-size="10"
              :total="appTotal"
              layout="prev, pager, next"
              @current-change="loadApplications"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="成员管理" name="members">
          <div class="filter-bar">
            <el-input
              v-model="memberKeyword"
              placeholder="搜索姓名 / 学工号 / 账号"
              style="width: 260px"
              clearable
              @clear="loadMembers"
              @keyup.enter="loadMembers"
            />
            <el-button type="primary" @click="loadMembers">搜索</el-button>
          </div>

          <el-table :data="members" style="width: 100%">
            <el-table-column prop="username" label="账号" min-width="120" />
            <el-table-column prop="realName" label="姓名" min-width="100" />
            <el-table-column prop="staffNo" label="学工号" min-width="120" />
            <el-table-column label="角色" width="110">
              <template #default="{ row }">
                <span :class="['role-tag', row.roleCode === 2 ? 'admin' : row.roleCode === 1 ? 'teacher' : 'student']">
                  {{ row.role }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="加入时间" min-width="150">
              <template #default="{ row }">{{ formatDate(row.joinTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <template v-if="row.roleCode !== 2">
                  <el-button size="small" text @click="toggleTeacher(row)">
                    {{ row.roleCode === 1 ? '设为学生' : '设为老师' }}
                  </el-button>
                </template>
                <el-button size="small" text @click="openIdentityDialog(row)">改姓名学工号</el-button>
                <el-button
                  v-if="row.roleCode !== 2 && row.userId !== currentUserId"
                  size="small"
                  type="danger"
                  text
                  @click="removeMember(row)"
                >
                  移出学校
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination" v-if="memberTotal > 0">
            <el-pagination
              v-model:current-page="memberPage"
              :page-size="10"
              :total="memberTotal"
              layout="prev, pager, next"
              @current-change="loadMembers"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 加入学校 -->
    <el-dialog v-model="joinDialog.visible" title="填写你的姓名与学工号" width="440px" class="dark-dialog">
      <el-form :model="joinDialog" label-width="80px">
        <el-form-item label="姓名" required>
          <el-input v-model="joinDialog.realName" maxlength="32" placeholder="字母 / 汉字及空格、中点、连字符" />
        </el-form-item>
        <el-form-item label="学工号" required>
          <el-input v-model="joinDialog.staffNo" maxlength="24" placeholder="字母 / 数字 / 下划线 / 连字符" />
        </el-form-item>
      </el-form>
      <p class="form-tip">进入学校后，姓名与学工号不可自行修改，如需变更请联系学校管理员。</p>
      <template #footer>
        <el-button @click="joinDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="joinDialog.submitting" @click="submitJoin">提交</el-button>
      </template>
    </el-dialog>

    <!-- 审核加入申请 -->
    <el-dialog
      v-model="reviewDialog.visible"
      :title="reviewTitle"
      width="440px"
      class="dark-dialog"
    >
      <el-form label-width="80px">
        <el-form-item label="审核意见">
          <el-input v-model="reviewDialog.comment" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialog.visible = false">取消</el-button>
        <el-button :type="reviewDialog.approved ? 'primary' : 'danger'" @click="submitReview">
          确认{{ reviewDialog.approved ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 修改成员姓名与学工号 -->
    <el-dialog v-model="identityDialog.visible" title="修改成员姓名 / 学工号" width="440px" class="dark-dialog">
      <el-form :model="identityDialog" label-width="80px">
        <el-form-item label="姓名" required>
          <el-input v-model="identityDialog.realName" maxlength="32" />
        </el-form-item>
        <el-form-item label="学工号" required>
          <el-input v-model="identityDialog.staffNo" maxlength="24" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="identityDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="identityDialog.submitting" @click="submitIdentity">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { del, get, post, put } from '@/utils/http'
import { useUserStore } from '@/stores/user'
import type { PageResult } from '@/types'
import { SCHOOL_ROLE_ADMIN, type SchoolDetail, type SchoolJoinApplication, type SchoolMember } from '@/types/school'
import { formatDateTime as formatDate } from '@/utils/format'
import { useConfirmBeforeApprove } from '@/composables/useConfirmBeforeApprove'
import { applicationStatusText as statusText, applicationStatusClass as statusClass } from '@/utils/status'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const schoolId = Number(route.params.id)
const school = ref<SchoolDetail | null>(null)
const loading = ref(false)

const currentUserId = computed(() => userStore.userInfo?.id ?? 0)

const canManage = computed(
  () => school.value?.myRoleCode === SCHOOL_ROLE_ADMIN || userStore.hasPermission('school:update')
)

const activeTab = ref<'applications' | 'members'>('applications')
const allowJoinWithoutApproval = ref(false)

// 申请状态文案/样式后缀与 AdminPanel 共用（见 utils/status）

const loadSchool = async () => {
  loading.value = true
  const result = await get<SchoolDetail>(`/school/${schoolId}`)
  loading.value = false
  if (result.code === 200) {
    school.value = result.data
    allowJoinWithoutApproval.value = result.data?.allowJoinWithoutApproval ?? false
    if (canManage.value) {
      loadApplications()
      loadMembers()
    }
  } else {
    ElMessage.error(result.message)
  }
}

// ===== 加入学校 =====

const joinDialog = reactive({
  visible: false,
  submitting: false,
  realName: '',
  staffNo: ''
})

const openJoinDialog = () => {
  joinDialog.visible = true
  joinDialog.submitting = false
}

const submitJoin = async () => {
  if (!joinDialog.realName.trim()) {
    ElMessage.warning('请填写姓名')
    return
  }
  if (!joinDialog.staffNo.trim()) {
    ElMessage.warning('请填写学工号')
    return
  }
  joinDialog.submitting = true
  const result = await post<SchoolJoinApplication>(`/school/${schoolId}/join`, {
    realName: joinDialog.realName.trim(),
    staffNo: joinDialog.staffNo.trim()
  })
  joinDialog.submitting = false
  if (result.code === 200) {
    ElMessage.success(result.data?.status === 1 ? '已加入学校' : '申请已提交，等待学校管理员审核')
    joinDialog.visible = false
    // 提交完成后清空，避免加入其他学校时沿用上一所学校的学工号
    joinDialog.realName = ''
    joinDialog.staffNo = ''
    loadSchool()
  } else {
    ElMessage.error(result.message)
  }
}

// ===== 加入申请审核 =====

const applications = ref<SchoolJoinApplication[]>([])
// 默认只看待审核
const appFilter = ref(0)
const appPage = ref(1)
const appTotal = ref(0)
const selectedApplications = ref<SchoolJoinApplication[]>([])

const handleAppSelection = (rows: SchoolJoinApplication[]) => {
  selectedApplications.value = rows
}

// 只有待审核的申请可以勾选（已处理的不允许再审核）
const isPendingRow = (row: SchoolJoinApplication) => row.status === 0

const reviewDialog = reactive({
  visible: false,
  submitting: false,
  applicationIds: [] as number[],
  approved: false,
  comment: ''
})

// 单条与批量共用同一个弹窗，标题按数量区分
const reviewTitle = computed(() => {
  const batch = reviewDialog.applicationIds.length > 1
  const action = reviewDialog.approved ? '通过' : '拒绝'
  return batch ? `批量${action} ${reviewDialog.applicationIds.length} 条申请` : `${action}申请`
})

// 通过申请前是否需要二次确认：前端偏好，与 AdminPanel 共用（见 useConfirmBeforeApprove）
const confirmBeforeApprove = useConfirmBeforeApprove()

// 审核请求进行中：关闭二次确认时没有弹窗可反馈，靠它禁用行内按钮避免重复提交
const reviewSubmitting = ref(false)

const loadApplications = async () => {
  const params: Record<string, unknown> = { pageNum: appPage.value, pageSize: 10 }
  if (appFilter.value >= 0) {
    params.status = appFilter.value
  }
  const result = await get<PageResult<SchoolJoinApplication>>(`/school/${schoolId}/applications`, params)
  if (result.code === 200) {
    applications.value = result.data!.records
    appTotal.value = result.data!.total
  } else {
    ElMessage.error(result.message)
  }
}

// 切换状态筛选后回到第一页再查询
const reloadApplications = () => {
  appPage.value = 1
  loadApplications()
}

const openReview = (row: SchoolJoinApplication, approved: boolean) => {
  if (approved && !confirmBeforeApprove.value) {
    // 关闭二次确认时，通过操作直接提交
    submitReview({ applicationIds: [row.id], approved: true, comment: '' })
    return
  }
  // 重新打开同一条申请时保留未提交的审核意见
  if (reviewDialog.applicationIds.length !== 1 || reviewDialog.applicationIds[0] !== row.id) {
    reviewDialog.comment = ''
  }
  reviewDialog.visible = true
  reviewDialog.submitting = false
  reviewDialog.applicationIds = [row.id]
  reviewDialog.approved = approved
}

const openBatchReview = (approved: boolean) => {
  if (selectedApplications.value.length === 0) {
    return
  }
  const applicationIds = selectedApplications.value.map(row => row.id)
  if (approved && !confirmBeforeApprove.value) {
    submitReview({ applicationIds, approved: true, comment: '' })
    return
  }
  reviewDialog.visible = true
  reviewDialog.submitting = false
  // 批量审核针对的是另一组申请，不沿用单条审核时未提交的意见
  reviewDialog.comment = ''
  reviewDialog.applicationIds = applicationIds
  reviewDialog.approved = approved
}

interface ReviewTarget {
  applicationIds: number[]
  approved: boolean
  comment: string
}

const submitReview = async (target?: ReviewTarget) => {
  const applicationIds = target?.applicationIds ?? reviewDialog.applicationIds
  const approved = target?.approved ?? reviewDialog.approved
  const comment = target?.comment ?? reviewDialog.comment
  if (applicationIds.length === 0) {
    return
  }

  const isBatch = applicationIds.length > 1
  const url = isBatch
    ? `/school/${schoolId}/applications/batch-approve`
    : `/school/${schoolId}/applications/approve`
  // 通过申请无需说明原因，只有拒绝时才提交意见
  const payload: Record<string, unknown> = { approved }
  if (!approved) {
    payload.comment = comment.trim()
  }
  if (isBatch) {
    payload.applicationIds = applicationIds
  } else {
    payload.applicationId = applicationIds[0]
  }

  reviewSubmitting.value = true
  reviewDialog.submitting = true
  try {
    const result = await put(url, payload)
    if (result.code === 200) {
      ElMessage.success(isBatch ? result.message : approved ? '已通过' : '已拒绝')
      reviewDialog.visible = false
      reviewDialog.comment = ''
      reviewDialog.applicationIds = []
      selectedApplications.value = []
      appPage.value = 1
      loadApplications()
      loadMembers()
      loadSchool()
    } else {
      ElMessage.error(result.message)
    }
  } finally {
    // 网络异常时也要复位，否则行内按钮会一直处于禁用态
    reviewDialog.submitting = false
    reviewSubmitting.value = false
  }
}

const submitJoinApproval = async () => {
  const result = await put(`/school/${schoolId}/join-approval`, {
    allowJoinWithoutApproval: allowJoinWithoutApproval.value
  })
  if (result.code === 200) {
    ElMessage.success('加入学校设置已更新')
  } else {
    ElMessage.error(result.message)
    loadSchool()
  }
}

// ===== 成员管理 =====

const members = ref<SchoolMember[]>([])
const memberKeyword = ref('')
const memberPage = ref(1)
const memberTotal = ref(0)

const loadMembers = async () => {
  const params: Record<string, unknown> = { pageNum: memberPage.value, pageSize: 10 }
  if (memberKeyword.value.trim()) {
    params.keyword = memberKeyword.value.trim()
  }
  const result = await get<PageResult<SchoolMember>>(`/school/${schoolId}/members`, params)
  if (result.code === 200) {
    members.value = result.data!.records
    memberTotal.value = result.data!.total
  } else {
    ElMessage.error(result.message)
  }
}

const toggleTeacher = async (row: SchoolMember) => {
  const targetRole = row.roleCode === 1 ? 0 : 1
  const result = await put(`/school/${schoolId}/members/role`, {
    userId: row.userId,
    role: targetRole
  })
  if (result.code === 200) {
    ElMessage.success(targetRole === 1 ? '已设为老师' : '已设为学生')
    loadMembers()
  } else {
    ElMessage.error(result.message)
  }
}

const identityDialog = reactive({
  visible: false,
  submitting: false,
  userId: 0,
  realName: '',
  staffNo: ''
})

const openIdentityDialog = (row: SchoolMember) => {
  // 重新打开同一位成员时保留未提交的修改
  if (identityDialog.userId !== row.userId) {
    identityDialog.realName = row.realName ?? ''
    identityDialog.staffNo = row.staffNo ?? ''
  }
  identityDialog.visible = true
  identityDialog.submitting = false
  identityDialog.userId = row.userId
}

const submitIdentity = async () => {
  if (!identityDialog.realName.trim()) {
    ElMessage.warning('请填写姓名')
    return
  }
  if (!identityDialog.staffNo.trim()) {
    ElMessage.warning('请填写学工号')
    return
  }
  identityDialog.submitting = true
  const result = await put(`/school/${schoolId}/members/identity`, {
    userId: identityDialog.userId,
    realName: identityDialog.realName.trim(),
    staffNo: identityDialog.staffNo.trim()
  })
  identityDialog.submitting = false
  if (result.code === 200) {
    ElMessage.success('成员姓名与学工号已更新')
    identityDialog.visible = false
    loadMembers()
  } else {
    ElMessage.error(result.message)
  }
}

const handleTabChange = (name: string | number) => {
  if (name === 'applications') {
    loadApplications()
  } else if (name === 'members') {
    loadMembers()
  }
}

/** 将成员移出学校（学校管理员） */
const removeMember = async (row: SchoolMember) => {
  try {
    await ElMessageBox.confirm(
      `确认将「${row.realName || row.username}」移出学校？他将同时被移出该校的所有班级，其在班级内的作业提交也会被清理。`,
      '移出学校',
      { type: 'warning', confirmButtonText: '确认移出', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const result = await del(`/school/${schoolId}/members/${row.userId}`)
  if (result.code === 200) {
    ElMessage.success('成员已移出学校')
    // 若移出的是当前页最后一条，回退一页
    if (members.value.length === 1 && memberPage.value > 1) {
      memberPage.value -= 1
    }
    loadMembers()
    loadSchool()
  } else {
    ElMessage.error(result.message)
  }
}

/** 退出学校 */
const leaveSchool = async () => {
  try {
    await ElMessageBox.confirm(
      `确认退出「${school.value?.schoolName ?? ''}」？退出后你将失去该校的成员身份，需要重新申请加入。`,
      '退出学校',
      { type: 'warning', confirmButtonText: '确认退出', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const result = await del(`/school/${schoolId}/membership`)
  if (result.code === 200) {
    ElMessage.success('已退出学校')
    router.push('/school')
  } else {
    ElMessage.error(result.message)
  }
}

onMounted(loadSchool)
</script>

<style scoped>
.school-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overview {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.overview-main h2 {
  margin: 0 0 8px;
}

.overview-main .description {
  margin: 0 0 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
  font-size: 13px;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.65);
}

/* .flag 加入审核标记样式见全局 style.css（多页共用） */

.identity-card {
  min-width: 220px;
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.12);
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.identity-line {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.identity-line .label {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.55);
}

.identity-tip {
  margin: 4px 0 0;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

/* 被拒绝时展示管理员填写的审核意见 */
.identity-comment {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--danger-strong);
}

/* 「加入申请」页签上的待审核数量角标 */
.tab-badge {
  margin-left: 6px;
}

.tab-badge :deep(.el-badge__content) {
  transform: translateY(-1px);
}

/* .confirm-hint 审核确认提示样式见全局 style.css（多页共用） */

.batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

/* 本页筛选栏嵌在 el-card 内，全局 .filter-bar 的“浮起白色卡片”外观（近白渐变 +
   内白高光）叠在浅灰卡面上过亮。这里改为中性内凹浅底 + 细描边，与卡面同调 */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding: 8px 12px;
  flex-wrap: wrap;
  border-radius: 12px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08);
  box-shadow: none;
}

.status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.status-tag.pending {
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.15);
}

.status-tag.approved {
  color: #67c23a;
  background: rgba(103, 194, 58, 0.15);
}

.status-tag.rejected {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.15);
}

.role-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.role-tag.admin {
  color: #667eea;
  background: rgba(102, 126, 234, 0.15);
}

.role-tag.teacher {
  color: #409eff;
  background: rgba(64, 158, 255, 0.15);
}

.role-tag.student {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08);
}

.muted {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.pagination {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}

.form-tip {
  margin: 0;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.55);
}
</style>
