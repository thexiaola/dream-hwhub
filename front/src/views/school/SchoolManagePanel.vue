<template>
  <div class="school-manage-panel">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane name="applications">
        <template #label>
          加入申请
          <el-badge v-if="pendingCount > 0" :value="pendingCount" class="tab-badge" />
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
            <el-switch v-model="confirmBeforeApprove" size="small" active-text="通过前二次确认" />
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
          class="admin-table"
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
          <el-table-column label="操作" width="150" fixed="right" align="center">
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

        <el-table class="admin-table" :data="members" style="width: 100%">
          <el-table-column prop="username" label="账号" min-width="120" />
          <el-table-column prop="realName" label="姓名" min-width="100" />
          <el-table-column prop="staffNo" label="学工号" min-width="120" />
          <el-table-column label="角色" width="110">
            <template #default="{ row }">
              <span
                :class="['role-tag', row.roleCode === 2 ? 'admin' : row.roleCode === 1 ? 'teacher' : 'student']"
              >
                {{ row.role }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="加入时间" min-width="150">
            <template #default="{ row }">{{ formatDate(row.joinTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right" align="center">
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

      <el-tab-pane name="takeovers">
        <template #label>
          接管申请
          <el-badge v-if="pendingTakeoverCount > 0" :value="pendingTakeoverCount" class="tab-badge" />
        </template>
        <div class="filter-bar">
          <el-radio-group v-model="takeoverFilter" @change="loadTakeovers">
            <el-radio-button :value="-1">全部</el-radio-button>
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="1">已通过</el-radio-button>
            <el-radio-button :value="2">已拒绝</el-radio-button>
          </el-radio-group>
          <el-switch
            v-model="autoApproveClassTakeover"
            active-text="自动同意接管"
            inactive-text="需管理员审核"
            @change="submitTakeoverSetting"
          />
        </div>

        <p class="takeover-tip">
          当班级创建者失去教师身份时，该班级将暂不可管理且不再接纳新学生，本校老师可申请接管。
          开启「自动同意接管」后，老师申请即立即生效；关闭则需在此审核。
        </p>

        <el-table class="admin-table" :data="takeovers" style="width: 100%">
          <el-table-column prop="className" label="班级" min-width="140" />
          <el-table-column prop="applicantUsername" label="账号" min-width="120">
            <template #default="{ row }">{{ row.applicantUsername || '-' }}</template>
          </el-table-column>
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
          <el-table-column label="操作" width="170" fixed="right" align="center">
            <template #default="{ row }">
              <template v-if="row.status === 0">
                <el-button
                  size="small"
                  type="primary"
                  text
                  :loading="takeoverReviewSubmitting"
                  @click="reviewTakeover(row, true)"
                >
                  同意
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  text
                  :loading="takeoverReviewSubmitting"
                  @click="reviewTakeover(row, false)"
                >
                  拒绝
                </el-button>
              </template>
              <span v-else class="reviewed-note">{{ row.reviewComment || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="私信策略" name="policy">
        <p class="takeover-tip">
          设置本校「向陌生用户发送私信」的条数上限与重置小时数。
          留空表示继承全站默认（每 {{ policyEffective.globalResetHours }} 小时
          {{ policyEffective.globalStrangerLimit }} 条）。
        </p>
        <el-form label-width="110px" class="policy-form">
          <el-form-item label="条数上限">
            <el-input-number
              v-model="policy.strangerLimit"
              :min="0"
              :max="1000"
              placeholder="继承全站"
              controls-position="right"
            />
            <span class="policy-unit">条</span>
          </el-form-item>
          <el-form-item label="重置小时数">
            <el-input-number
              v-model="policy.resetHours"
              :min="1"
              :max="8760"
              placeholder="继承全站"
              controls-position="right"
            />
            <span class="policy-unit">小时</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="policySaving" @click="submitPolicy">保存</el-button>
            <el-button :disabled="policySaving" @click="clearPolicyOverride">沿用全站默认</el-button>
          </el-form-item>
        </el-form>
        <div class="policy-preview">
          <Info :size="15" />
          <span>
            当前生效：每 {{ policyEffective.resetHours }} 小时可向同一陌生用户发送
            <b>{{ policyEffective.strangerLimit }}</b> 条私信
            {{ policyEffective.overridden ? '（本校已自定义）' : '（继承全站默认）' }}。
          </span>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 审核加入申请 -->
    <el-dialog
      v-model="reviewDialog.visible"
      :title="reviewTitle"
      width="440px"
      class="dark-dialog"
      append-to-body
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
    <el-dialog
      v-model="identityDialog.visible"
      title="修改成员姓名 / 学工号"
      width="440px"
      class="dark-dialog"
      append-to-body
    >
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
        <el-button type="primary" :loading="identityDialog.submitting" @click="submitIdentity">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Info } from '@lucide/vue'
import { del, get, put } from '@/utils/http'
import { useUserStore } from '@/stores/user'
import type { PageResult } from '@/types'
import type { SchoolDetail, SchoolJoinApplication, SchoolMember } from '@/types/school'
import type { ClassTakeoverInfo } from '@/types/class'
import type { MessagePolicyInfo } from '@/types/friend'
import { formatDateTime as formatDate } from '@/utils/format'
import { useConfirmBeforeApprove } from '@/composables/useConfirmBeforeApprove'
import { applicationStatusText as statusText, applicationStatusClass as statusClass } from '@/utils/status'

/**
 * 学校管理面板（加入申请 / 成员管理 / 接管申请）
 * 由「我的学校」的学校详情页（真正的学校管理员）与管理面板「学校管理」
 * （平台管理员）共用，避免两处重复实现。组件自带数据加载。
 */
const props = defineProps<{
  /** 目标学校 ID */
  schoolId: number
}>()

const emit = defineEmits<{
  /** 学校数据发生变化（审核通过、移出成员等）时通知父级刷新统计 */
  (e: 'changed'): void
}>()

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id ?? 0)

const activeTab = ref<'applications' | 'members' | 'takeovers' | 'policy'>('applications')

// ===== 学校详情：提供待审核数量与两个开关的初值 =====

const pendingCount = ref(0)
const allowJoinWithoutApproval = ref(false)
const autoApproveClassTakeover = ref(true)

const loadDetail = async () => {
  const result = await get<SchoolDetail>(`/school/${props.schoolId}`)
  if (result.code === 200 && result.data) {
    pendingCount.value = result.data.pendingApplicationCount ?? 0
    allowJoinWithoutApproval.value = result.data.allowJoinWithoutApproval ?? false
    autoApproveClassTakeover.value = result.data.autoApproveClassTakeover !== false
  }
}

// ===== 加入申请 =====

const applications = ref<SchoolJoinApplication[]>([])
// 默认只看待审核
const appFilter = ref(0)
const appPage = ref(1)
const appTotal = ref(0)
const selectedApplications = ref<SchoolJoinApplication[]>([])

// 通过申请前是否需要二次确认：前端偏好，与 AdminPanel 共用（见 useConfirmBeforeApprove）
const confirmBeforeApprove = useConfirmBeforeApprove()

// 审核请求进行中：关闭二次确认时没有弹窗可反馈，靠它禁用行内按钮避免重复提交
const reviewSubmitting = ref(false)

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

const loadApplications = async () => {
  const params: Record<string, unknown> = { pageNum: appPage.value, pageSize: 10 }
  if (appFilter.value >= 0) {
    params.status = appFilter.value
  }
  const result = await get<PageResult<SchoolJoinApplication>>(
    `/school/${props.schoolId}/applications`,
    params
  )
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

const handleAppSelection = (rows: SchoolJoinApplication[]) => {
  selectedApplications.value = rows
}

// 只有待审核的申请可以勾选（已处理的不允许再审核）
const isPendingRow = (row: SchoolJoinApplication) => row.status === 0

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
    ? `/school/${props.schoolId}/applications/batch-approve`
    : `/school/${props.schoolId}/applications/approve`
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
      await loadDetail()
      emit('changed')
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
  const result = await put(`/school/${props.schoolId}/join-approval`, {
    allowJoinWithoutApproval: allowJoinWithoutApproval.value
  })
  if (result.code === 200) {
    ElMessage.success('加入学校设置已更新')
    emit('changed')
  } else {
    ElMessage.error(result.message)
    allowJoinWithoutApproval.value = !allowJoinWithoutApproval.value
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
  const result = await get<PageResult<SchoolMember>>(`/school/${props.schoolId}/members`, params)
  if (result.code === 200) {
    members.value = result.data!.records
    memberTotal.value = result.data!.total
  } else {
    ElMessage.error(result.message)
  }
}

const toggleTeacher = async (row: SchoolMember) => {
  const targetRole = row.roleCode === 1 ? 0 : 1
  const result = await put(`/school/${props.schoolId}/members/role`, {
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
  const result = await put(`/school/${props.schoolId}/members/identity`, {
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
  const result = await del(`/school/${props.schoolId}/members/${row.userId}`)
  if (result.code === 200) {
    ElMessage.success('成员已移出学校')
    // 若移出的是当前页最后一条，回退一页
    if (members.value.length === 1 && memberPage.value > 1) {
      memberPage.value -= 1
    }
    loadMembers()
    await loadDetail()
    emit('changed')
  } else {
    ElMessage.error(result.message)
  }
}

// ===== 班级接管 =====

const takeoverFilter = ref(-1)
const takeovers = ref<ClassTakeoverInfo[]>([])
const takeoverReviewSubmitting = ref(false)

const pendingTakeoverCount = computed(
  () => takeovers.value.filter((t) => t.status === 0).length
)

const loadTakeovers = async () => {
  const params: Record<string, unknown> = {}
  if (takeoverFilter.value >= 0) {
    params.status = takeoverFilter.value
  }
  const result = await get<ClassTakeoverInfo[]>(`/class/takeover/school/${props.schoolId}`, params)
  if (result.code === 200) {
    takeovers.value = result.data ?? []
  } else {
    ElMessage.error(result.message)
  }
}

const submitTakeoverSetting = async () => {
  const result = await put(`/school/${props.schoolId}/class-takeover-approval`, {
    autoApproveClassTakeover: autoApproveClassTakeover.value
  })
  if (result.code === 200) {
    ElMessage.success('班级接管设置已更新')
  } else {
    ElMessage.error(result.message)
    // 回滚开关状态
    autoApproveClassTakeover.value = !autoApproveClassTakeover.value
  }
}

const reviewTakeover = async (row: ClassTakeoverInfo, approved: boolean) => {
  try {
    await ElMessageBox.confirm(
      approved
        ? `同意「${row.applicantUsername}」接管班级「${row.className}」？通过后其将成为该班级创建者。`
        : `拒绝「${row.applicantUsername}」接管班级「${row.className}」的申请？`,
      approved ? '同意接管' : '拒绝接管',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  takeoverReviewSubmitting.value = true
  try {
    const result = await put(`/class/takeover/${row.id}/approve`, { approved, comment: '' })
    if (result.code === 200) {
      ElMessage.success(approved ? '已同意接管' : '已拒绝接管')
      loadTakeovers()
    } else {
      ElMessage.error(result.message)
    }
  } finally {
    takeoverReviewSubmitting.value = false
  }
}

// ===== 私信策略（本校覆盖「向陌生用户发私信」的上限与重置时间）=====

const policySaving = ref(false)
// 输入表单：null 表示继承全站默认
const policy = reactive<{ strangerLimit: number | null; resetHours: number | null }>({
  strangerLimit: null,
  resetHours: null
})
// 生效策略（含全站默认值，供提示与预览）
const policyEffective = reactive({
  strangerLimit: 3,
  resetHours: 24,
  overridden: false,
  globalStrangerLimit: 3,
  globalResetHours: 24
})

const loadPolicy = async () => {
  const result = await get<MessagePolicyInfo>(`/message-policy/school/${props.schoolId}`)
  if (result.code !== 200 || !result.data) {
    return
  }
  const data = result.data
  policyEffective.strangerLimit = data.strangerLimit
  policyEffective.resetHours = data.resetHours
  policyEffective.overridden = data.schoolOverridden
  policyEffective.globalStrangerLimit = data.globalStrangerLimit
  policyEffective.globalResetHours = data.globalResetHours
  // 仅在未覆盖时留空（表示继承）；已覆盖则回填当前值
  policy.strangerLimit = data.schoolOverridden ? data.strangerLimit : null
  policy.resetHours = data.schoolOverridden ? data.resetHours : null
}

const submitPolicy = async () => {
  policySaving.value = true
  const result = await put<MessagePolicyInfo>(`/message-policy/school/${props.schoolId}`, {
    strangerLimit: policy.strangerLimit,
    resetHours: policy.resetHours
  })
  policySaving.value = false
  if (result.code === 200) {
    ElMessage.success(result.message || '已保存')
    await loadPolicy()
  } else {
    ElMessage.error(result.message)
  }
}

/** 清除本校覆盖，恢复继承全站默认 */
const clearPolicyOverride = async () => {
  try {
    await ElMessageBox.confirm(
      '确认清除本校的自定义私信策略？清除后将继承全站默认。',
      '沿用全站默认',
      { type: 'warning', confirmButtonText: '确认清除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  policySaving.value = true
  const result = await put<MessagePolicyInfo>(`/message-policy/school/${props.schoolId}`, {
    strangerLimit: null,
    resetHours: null
  })
  policySaving.value = false
  if (result.code === 200) {
    ElMessage.success('已沿用全站默认')
    await loadPolicy()
  } else {
    ElMessage.error(result.message)
  }
}

// ===== 生命周期与对外接口 =====

const handleTabChange = (name: string | number) => {
  if (name === 'applications') {
    loadApplications()
  } else if (name === 'members') {
    loadMembers()
  } else if (name === 'takeovers') {
    loadTakeovers()
  } else if (name === 'policy') {
    loadPolicy()
  }
}

/** 重新加载学校详情与当前页签数据（供父级在打开/刷新时调用） */
const reload = async () => {
  await loadDetail()
  if (activeTab.value === 'members') {
    loadMembers()
  } else if (activeTab.value === 'takeovers') {
    loadTakeovers()
  } else if (activeTab.value === 'policy') {
    loadPolicy()
  } else {
    loadApplications()
  }
}

onMounted(async () => {
  await loadDetail()
  loadApplications()
  loadMembers()
  loadTakeovers()
})

defineExpose({ reload })
</script>

<style scoped>
/* .admin-table 表格主题化样式见全局 style.css（多页共用） */

.batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

/* 本面板嵌在内容卡片 / 抽屉内，全局 .filter-bar 的“浮起白色卡片”外观（近白渐变 +
   内白高光）叠在浅灰面上过亮。这里改为中性内凹浅底 + 细描边，与所在面色同调 */
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

/* 「加入申请」「接管申请」页签上的待审核数量角标 */
.tab-badge {
  margin-left: 6px;
}

.tab-badge :deep(.el-badge__content) {
  transform: translateY(-1px);
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

/* 班级接管 tab：说明文字与已处理申请的审核结果 */
.takeover-tip {
  margin: 4px 0 12px;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.reviewed-note {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
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
</style>
