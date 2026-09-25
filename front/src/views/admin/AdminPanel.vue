<template>
  <div class="admin-panel">
    <div class="page-header">
      <h2>管理面板</h2>
    </div>

    <div
      class="admin-tabs-host"
      ref="tabsHostRef"
      @pointerdown="onPointerDown"
      @click.capture="onClickCapture"
    >
      <!-- 激活高亮块：可按住拖动，松开后吸附到最近的页签 -->
      <span
        v-show="indicatorVisible"
        class="admin-indicator"
        :class="{ 'is-dragging': indicatorDragging }"
        :style="indicatorStyle"
      />
      <el-tabs v-model="activeTab" class="admin-tabs" @tab-change="handleTabChange">
      <!-- 加入班级申请 -->
      <el-tab-pane v-if="canApproveJoin" label="加入班级申请" name="join">
        <div class="filter-bar">
          <span class="filter-bar__label">
            <SlidersHorizontal :size="14" />
            状态
          </span>
          <SlideSegmented
            v-model="joinFilter"
            :options="statusFilterOptions"
            aria-label="加入班级申请状态"
            @change="loadJoinApplications"
          />
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
          <el-button v-if="canCreateClass" type="primary" plain @click="openCreateClassDialog">
            <Plus :size="16" />
            新建班级
          </el-button>
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
        <UserManage ref="userManageRef" />
      </el-tab-pane>

      <!-- 学校加入申请（平台管理员集中审核） -->
      <el-tab-pane v-if="canViewSchools" label="学校加入申请" name="schoolJoin">
        <div class="filter-bar">
          <span class="filter-bar__label">
            <SlidersHorizontal :size="14" />
            状态
          </span>
          <SlideSegmented
            v-model="schoolJoinFilter"
            :options="statusFilterOptions"
            aria-label="学校加入申请状态"
            @change="reloadSchoolJoinApplications"
          />
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
          class="admin-table"
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
          <el-table-column label="操作" width="150" fixed="right" align="center">
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
        <PermissionGroupManage ref="permissionGroupRef" />
      </el-tab-pane>

      <!-- 学校管理 -->
      <el-tab-pane v-if="canViewSchools" label="学校管理" name="schools" lazy>
        <SchoolManage ref="schoolManageRef" />
      </el-tab-pane>

      <!-- 私信策略（全站默认，仅平台管理员） -->
      <el-tab-pane v-if="isOp" label="私信策略" name="policy" lazy>
        <MessagePolicyManage ref="messagePolicyRef" />
      </el-tab-pane>
      </el-tabs>
    </div>

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

    <!-- 新建班级 -->
    <el-dialog v-model="createClassDialog.visible" title="新建班级" width="460px" class="dark-dialog">
      <el-form :model="createClassDialog.form" label-width="80px">
        <el-form-item label="所属学校" required>
          <el-select
            v-model="createClassDialog.form.schoolId"
            placeholder="请选择班级所属学校"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="school in classSchoolOptions"
              :key="school.id"
              :label="school.schoolName"
              :value="school.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班级名称" required>
          <el-input
            v-model="createClassDialog.form.className"
            placeholder="请输入班级名称"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="班级描述">
          <el-input
            v-model="createClassDialog.form.description"
            type="textarea"
            :rows="3"
            maxlength="512"
            placeholder="选填"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createClassDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="createClassDialog.submitting" @click="submitCreateClass">
          创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, reactive, watch, nextTick, defineAsyncComponent } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { get, post, put, del } from '@/utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FileText, Plus, SlidersHorizontal } from '@lucide/vue'
import { useUserStore } from '@/stores/user'
import { useDraggableIndicator } from '@/composables/useDraggableIndicator'
import { useConfirmBeforeApprove } from '@/composables/useConfirmBeforeApprove'
import { confirmDangerousOperation, requireSensitiveVerification } from '@/composables/useSensitiveVerification'
import SlideSegmented from '@/components/SlideSegmented.vue'
import type { PageResult } from '@/types'
import type { School as SchoolInfo } from '@/types/school'
import { formatDateTime as formatDate } from '@/utils/format'
import { applicationStatusText as getStatusText, applicationStatusClass as getStatusClass } from '@/utils/status'

// 三个子模块各自成 chunk，仅在实际进入对应页签时加载其代码与数据
const UserManage = defineAsyncComponent(() => import('./UserManage.vue'))
const PermissionGroupManage = defineAsyncComponent(() => import('./PermissionGroupManage.vue'))
const SchoolManage = defineAsyncComponent(() => import('./SchoolManage.vue'))
const MessagePolicyManage = defineAsyncComponent(() => import('./MessagePolicyManage.vue'))

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

type AdminTab = 'join' | 'classes' | 'users' | 'groups' | 'schools' | 'schoolJoin' | 'policy'

const ADMIN_TABS: readonly AdminTab[] = ['join', 'classes', 'users', 'groups', 'schools', 'schoolJoin', 'policy']
const isAdminTab = (value: unknown): value is AdminTab =>
  typeof value === 'string' && (ADMIN_TABS as readonly string[]).includes(value)

// 当前页签：由路由 /admin/panel/:tab 驱动，权限就绪后解析
const activeTab = ref<AdminTab>('join')

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 按权限节点控制各页签可见性
const canApproveJoin = computed(() => userStore.hasPermission('class:approve_join'))
const canViewAllClasses = computed(() => userStore.hasPermission('class:view_all'))
// 平台管理员（OP）恒有 class:create，可在任意学校下创建班级
const canCreateClass = computed(() => userStore.hasPermission('class:create'))
const canViewUsers = computed(() => userStore.hasPermission('user:view'))
const canViewPermissions = computed(() => userStore.hasPermission('permission:view'))
const canViewSchools = computed(() => userStore.hasPermission('school:view_all'))
// 审核（含批量）需要 school:update，避免只读角色看得到按钮却点不动
const canUpdateSchool = computed(() => userStore.hasPermission('school:update'))
// 全站私信策略仅平台管理员可设置
const isOp = computed(() => userStore.isOp)

// 通过申请前是否需要二次确认：前端偏好，与 SchoolDetail 共用（见 useConfirmBeforeApprove）
const confirmBeforeApprove = useConfirmBeforeApprove()

// 审核请求进行中：关闭二次确认时没有弹窗可反馈，靠它禁用行内按钮避免重复提交
const reviewSubmitting = ref(false)

const visibleTabs = computed<AdminTab[]>(() => {
  // 顺序与模板中 el-tab-pane 的书写顺序保持一致（决定了「首个可见页签」）
  const tabs: AdminTab[] = []
  if (canApproveJoin.value) tabs.push('join')
  if (canViewAllClasses.value) tabs.push('classes')
  if (canViewUsers.value) tabs.push('users')
  if (canViewSchools.value) tabs.push('schoolJoin')
  if (canViewPermissions.value) tabs.push('groups')
  if (canViewSchools.value) tabs.push('schools')
  if (isOp.value) tabs.push('policy')
  return tabs
})

// 三个子模块的实例引用：用于在每次进入对应页签时触发其数据刷新
// （el-tab-pane lazy 挂载后保持常驻，子组件的 onMounted 只执行一次）
const userManageRef = ref<{ reload: () => void } | null>(null)
const permissionGroupRef = ref<{ reload: () => void } | null>(null)
const schoolManageRef = ref<{ reload: () => void } | null>(null)
const messagePolicyRef = ref<{ reload: () => void } | null>(null)

// 页签对应的数据加载：点击、拖拽、直接访问链接都走这里，避免重复实现
const runTabLoad = (name: AdminTab) => {
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
  } else if (name === 'users') {
    // 每次进入都刷新用户列表与筛选元数据
    userManageRef.value?.reload()
  } else if (name === 'groups') {
    permissionGroupRef.value?.reload()
  } else if (name === 'schools') {
    schoolManageRef.value?.reload()
  } else if (name === 'policy') {
    messagePolicyRef.value?.reload()
  }
}

// 读取路由 /admin/panel/:tab 中的模块名
const tabFromRoute = (): AdminTab | null => {
  const raw = route.params.tab
  const single = Array.isArray(raw) ? raw[0] : raw
  return isAdminTab(single) ? single : null
}

// 权限就绪前（onMounted 解析完成前）不响应路由/权限变化，避免误判
let tabResolved = false

// 根据路由参数与当前权限解析出有效模块；无参数/非法/无权限时回退到首个可见模块
const resolveTab = (): AdminTab => {
  const fromRoute = tabFromRoute()
  if (fromRoute && visibleTabs.value.includes(fromRoute)) return fromRoute
  return visibleTabs.value[0] ?? 'join'
}

// 应用某个模块：更新高亮、必要时规范化 URL、按需加载数据
const applyRoute = async (target: AdminTab, load: boolean) => {
  activeTab.value = target
  if (route.params.tab !== target) {
    await router.replace({ name: 'AdminPanel', params: { tab: target } })
  }
  if (load) runTabLoad(target)
  await nextTick()
  syncToActive()
}

// 点击页签：el-tabs 已通过 v-model 更新 activeTab，这里加载数据并推入历史（可后退）
const handleTabChange = (name: string | number) => {
  if (!isAdminTab(name)) return
  runTabLoad(name)
  if (route.params.tab !== name) {
    router.push({ name: 'AdminPanel', params: { tab: name } })
  }
  nextTick(syncToActive)
}

// 拖拽激活指示器：宿主容器承载指示器，页签几何相对它测量
const tabsHostRef = ref<HTMLElement | null>(null)
let tabsResizeObserver: ResizeObserver | null = null

// 按 DOM 视觉顺序取可拖拽的页签（id 形如 tab-<name>），跳过禁用项
const getTabs = () => {
  const host = tabsHostRef.value
  if (!host) return [] as { key: string; el: HTMLElement }[]
  return Array.from(host.querySelectorAll<HTMLElement>('.el-tabs__nav .el-tabs__item'))
    .filter(el => !el.classList.contains('is-disabled'))
    .map(el => ({ key: el.id.replace(/^tab-/, ''), el }))
}

// 拖拽经过页签时实时切换：更新高亮、加载数据并同步 URL
const activateTab = (name: AdminTab) => {
  if (activeTab.value === name) return
  activeTab.value = name
  runTabLoad(name)
  if (route.params.tab !== name) {
    router.replace({ name: 'AdminPanel', params: { tab: name } })
  }
  nextTick(syncToActive)
}

const {
  dragging: indicatorDragging,
  position: indicatorPosition,
  visible: indicatorVisible,
  syncToActive,
  startDrag,
  onClickCapture
} = useDraggableIndicator({
  container: tabsHostRef,
  getTabs,
  activeKey: () => activeTab.value,
  onCross: key => activateTab(key as AdminTab)
})

// 手机端页签可横向滚动，而自定义指示器位于滚动容器之外，滚动时会与页签错位；
// 故手机端停用拖拽、隐藏自定义指示器，改回 Element Plus 原生下划线激活条（随滚动对齐）
const isNarrowViewport = () => window.matchMedia('(max-width: 768px)').matches

const onPointerDown = (event: PointerEvent) => {
  if (isNarrowViewport()) return
  startDrag(event)
}

// 指示器相对页签上下内缩，避免直角底边压住底部基线
const INDICATOR_INSET_Y = 4

const indicatorStyle = computed(() => {
  const pos = indicatorPosition.value
  if (!pos) return {}
  return {
    transform: `translate(${pos.left}px, ${pos.top + INDICATOR_INSET_Y}px)`,
    width: `${pos.width}px`,
    height: `${pos.height - INDICATOR_INSET_Y * 2}px`
  }
})

// 切换页签或可见页签集合变化（权限就绪）后重新定位
watch(activeTab, () => nextTick(syncToActive))
watch(visibleTabs, () => nextTick(syncToActive))

const joinApplications = ref<ClassJoinApplication[]>([])
const joinFilter = ref(-1)
const joinPage = ref(1)
const joinTotal = ref(0)

// 审核状态筛选：加入班级申请与学校加入申请共用同一组选项
const statusFilterOptions = [
  { label: '全部', value: -1 },
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已拒绝', value: 2 }
]

const reviewDialog = ref({
  visible: false,
  type: '' as 'join',
  applicationId: 0,
  approved: false,
  comment: ''
})

// 申请状态文案/样式后缀与 SchoolDetail 共用（见 utils/status）

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
  // 用户信息（含权限节点）已由路由守卫在进入前加载，这里不再重复请求
  // 依据路由参数与权限解析目标模块并加载其数据（URL 无参数/非法时回退首个可见模块并规范化 URL）
  tabResolved = true
  await applyRoute(resolveTab(), true)

  // 页签渲染完成后再定位指示器，并在容器尺寸变化时重新定位
  await nextTick()
  syncToActive()
  tabsResizeObserver = new ResizeObserver(() => nextTick(syncToActive))
  if (tabsHostRef.value) {
    tabsResizeObserver.observe(tabsHostRef.value)
  }
})

// 直接访问/前进后退切换模块：URL 变化时同步高亮并加载对应数据
watch(
  () => route.params.tab,
  () => {
    if (!tabResolved) return
    const target = resolveTab()
    if (target !== activeTab.value) {
      runTabLoad(target)
    }
    activeTab.value = target
    if (route.params.tab !== target) {
      router.replace({ name: 'AdminPanel', params: { tab: target } })
    }
    nextTick(syncToActive)
  }
)

// 权限就绪后可见页签集合变化（如权限变更）时，若当前模块已不可见则回退
watch(visibleTabs, () => {
  if (!tabResolved) return
  if (!visibleTabs.value.includes(activeTab.value)) {
    applyRoute(resolveTab(), true)
  } else {
    nextTick(syncToActive)
  }
})

onUnmounted(() => {
  tabsResizeObserver?.disconnect()
  tabsResizeObserver = null
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

// 新建班级弹窗（平台管理员可在任意学校下建班）
const createClassDialog = reactive({
  visible: false,
  submitting: false,
  form: {
    schoolId: undefined as number | undefined,
    className: '',
    description: ''
  }
})

const openCreateClassDialog = async () => {
  // 学校下拉选项按需加载，避免未进入过筛选时下拉为空
  if (classSchoolOptions.value.length === 0) {
    await loadClassSchoolOptions()
  }
  // 连续打开时保留上次未提交的草稿；已选学校的情况下不覆盖
  createClassDialog.submitting = false
  createClassDialog.visible = true
}

const submitCreateClass = async () => {
  const { schoolId, className, description } = createClassDialog.form
  if (!schoolId) {
    ElMessage.warning('请选择班级所属学校')
    return
  }
  if (!className.trim()) {
    ElMessage.warning('请输入班级名称')
    return
  }
  createClassDialog.submitting = true
  try {
    const result = await post<{ id: number }>('/class/create', {
      schoolId,
      className: className.trim(),
      description: description.trim()
    })
    if (result.code === 200) {
      ElMessage.success('班级创建成功')
      createClassDialog.visible = false
      createClassDialog.form.className = ''
      createClassDialog.form.description = ''
      // 回到第一页并刷新，确保新班级可见
      classPage.value = 1
      loadClasses()
    } else {
      ElMessage.error(result.message)
    }
  } finally {
    createClassDialog.submitting = false
  }
}

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
  } catch {
    return
  }
  pendingDissolve.value = { classId, className }
  await doDissolveClass()
}

// ========== 危险操作（解散课堂）：确认文案 + 统一身份二次验证 ==========
const dangerSubmitting = ref(false)
const pendingDissolve = ref<{ classId: number; className: string }>({ classId: 0, className: '' })

const clearDangerInputs = () => {
  dangerSubmitting.value = false
  pendingDissolve.value = { classId: 0, className: '' }
}

const doDissolveClass = async () => {
  if (!pendingDissolve.value.classId) {
    ElMessage.warning('请先选择要解散的课堂')
    return
  }
  const classId = pendingDissolve.value.classId
  const confirmText = `我已确认要删除${pendingDissolve.value.className ?? ''}课堂`
  // 解散课堂属敏感操作，需身份二次验证（登录密码或邮箱验证码）
  const headers = await requireSensitiveVerification('解散课堂')
  if (!headers) {
    clearDangerInputs()
    return
  }
  dangerSubmitting.value = true
  try {
    const result = await del(`/class/${classId}`, { confirmText }, undefined, headers)
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
  // 踢出学生属高危操作：红色警示框 + 身份二次验证
  const headers = await confirmDangerousOperation({
    title: '踢出学生',
    message: '确认将该学生踢出班级？其在本班的作业提交将被清理，此操作不可恢复。',
    confirmText: '确认踢出',
    operationName: '踢出学生',
  })
  if (!headers) return
  const result = await del(`/class/${classId}/members/batch`, { studentUserIds: [userId] }, undefined, headers)
  if (result.code === 200) {
    ElMessage.success('已踢出')
    toggleClassDetail(classId)
  } else {
    ElMessage.error(result.message)
  }
}

const batchKickFromAdmin = async (classId: number) => {
  if (selectedAdminKickIds.value.length === 0) {
    ElMessage.warning('请选择要踢出的学生')
    return
  }
  // 踢出学生属高危操作：红色警示框 + 身份二次验证
  const headers = await confirmDangerousOperation({
    title: '批量踢出学生',
    message: `确认批量踢出 ${selectedAdminKickIds.value.length} 名学生？其在本班的作业提交将被清理，此操作不可恢复。`,
    confirmText: '确认踢出',
    operationName: '批量踢出学生',
  })
  if (!headers) return
  const result = await del(`/class/${classId}/members/batch`, { studentUserIds: selectedAdminKickIds.value }, undefined, headers)
  if (result.code === 200) {
    ElMessage.success(`已踢出 ${selectedAdminKickIds.value.length} 名学生`)
    selectedAdminKickIds.value = []
    toggleClassDetail(classId)
  } else {
    ElMessage.error(result.message)
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

/* 承载可拖拽激活块的定位容器 */
.admin-tabs-host {
  position: relative;
}

/* 激活高亮块：按住可拖动，松开吸附到最近页签；文字本身保持不动。
   与顶部导航一致，做成四角全圆的胶囊 + 描边，纵向内缩以避开底部基线 */
.admin-indicator {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
  box-sizing: border-box;
  border: 1px solid rgba(102, 126, 234, 0.4);
  border-radius: 8px;
  background: rgba(102, 126, 234, 0.2);
  pointer-events: none;
  transition:
    transform 0.32s cubic-bezier(0.22, 0.61, 0.36, 1),
    width 0.32s cubic-bezier(0.22, 0.61, 0.36, 1),
    height 0.32s cubic-bezier(0.22, 0.61, 0.36, 1);
}

/* 拖拽中：位移紧跟指针（transform 不设过渡），仅宽高平滑过渡，
   使滑块掠过不同宽度的页签时尺寸变化有动画而非生硬跳变 */
.admin-indicator.is-dragging {
  transition:
    width 0.18s ease,
    height 0.18s ease;
}

/* 用自定义指示器取代 Element Plus 默认的下划线激活条 */
.admin-tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.admin-tabs :deep(.el-tabs__item) {
  position: relative;
  z-index: 2;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

/* EP 默认让首尾页签贴边（首项 padding-left:0、末项 padding-right:0），
   那是为下划线样式对齐准备的；换成圆角高亮块后文字会贴到块边缘，
   这里统一左右内边距，使每个高亮块内的文字都有对称留白 */
.admin-tabs :deep(.el-tabs__item:nth-child(2)),
.admin-tabs :deep(.el-tabs__item:last-child) {
  padding-left: 20px;
  padding-right: 20px;
}

.admin-tabs :deep(.el-tabs__item.is-active) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.admin-tabs :deep(.el-tabs__item:hover) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
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

/* .confirm-hint 审核确认提示样式见全局 style.css（多页共用） */

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

/* 卡片操作栏：独立于信息区的浅色操作条，与上方信息拉开层次 */
.app-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--action-bar-bg);
  border: 1px solid var(--action-bar-border);
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

/* 卡片操作栏：独立于信息区的浅色操作条，与上方信息拉开层次 */
.class-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--action-bar-bg);
  border: 1px solid var(--action-bar-border);
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

  /* 手机端页签改为多行换行（6 个页签横排放不下），消除横向滚动条。
     EP 的下划线激活条在多行下无法定位，故隐藏，改用激活项自身高亮 */
  .admin-tabs {
    overflow-x: visible;
  }

  .admin-tabs :deep(.el-tabs__header) {
    min-width: 0;
    flex-wrap: wrap;
  }

  .admin-tabs :deep(.el-tabs__nav-wrap),
  .admin-tabs :deep(.el-tabs__nav-scroll),
  .admin-tabs :deep(.el-tabs__nav) {
    overflow: visible;
    white-space: normal;
  }

  .admin-tabs :deep(.el-tabs__nav) {
    flex-wrap: wrap;
    width: 100%;
    float: none;
    transform: none !important;
  }

  /* 自定义滑动指示器与 EP 下划线在多行布局下都会错位，一并隐藏；
     换行后也不需要 EP 的左右滚动箭头 */
  .admin-indicator,
  .admin-tabs :deep(.el-tabs__active-bar),
  .admin-tabs :deep(.el-tabs__nav-prev),
  .admin-tabs :deep(.el-tabs__nav-next) {
    display: none;
  }

  /* 激活态改用激活项自身高亮，替代无法跨行的滑动指示器。
     高亮直接画在页签背景上（不用伪元素 + z-index，避免层叠隐患） */
  .admin-tabs :deep(.el-tabs__item.is-active) {
    color: var(--primary-color);
    background: rgba(102, 126, 234, 0.14);
    border-radius: 8px;
  }

  .user-list-card {
    padding: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .admin-indicator,
  .admin-indicator.is-dragging {
    transition: none;
  }
}
</style>
