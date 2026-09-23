<template>
  <div class="user-manage">
    <!-- 高级检索：可添加多行条件，行间可选「并且 / 或者」（连续并且为一组，组间或者） -->
    <div class="advanced-search">
      <div v-for="(condition, index) in searchConditions" :key="condition.id" class="condition-row">
        <el-radio-group v-if="index > 0" v-model="condition.connector" class="connector-group" size="small">
          <el-radio-button value="and">并且</el-radio-button>
          <el-radio-button value="or">或者</el-radio-button>
        </el-radio-group>
        <span v-else class="condition-placeholder">检索条件</span>

        <el-select v-model="condition.field" class="field-select" placeholder="字段">
          <el-option
            v-for="option in fieldOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-select v-model="condition.matchType" class="match-select">
          <el-option label="模糊" value="contains" />
          <el-option label="精确" value="equals" />
        </el-select>
        <el-input
          v-model="condition.value"
          class="value-input"
          placeholder="请输入内容"
          clearable
          @keyup.enter="search"
        />
        <el-button
          v-if="searchConditions.length > 1"
          text
          type="danger"
          @click="removeCondition(index)"
        >
          删除
        </el-button>
      </div>

      <div class="condition-actions">
        <el-button
          text
          type="primary"
          :disabled="searchConditions.length >= MAX_CONDITIONS"
          @click="addCondition"
        >
          + 添加条件
        </el-button>
        <el-button type="primary" @click="search">检索</el-button>
        <el-button @click="resetSearch">清空</el-button>
        <el-button v-if="canAdd" type="primary" plain @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="users" class="admin-table">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="110" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="权限" min-width="150">
        <template #default="{ row }">
          <span v-if="row.isOp" class="perm-op">平台管理员</span>
          <template v-else-if="row.groupNames?.length">
            <span v-for="name in row.groupNames" :key="name" class="group-name">
              {{ name }}
            </span>
          </template>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <span :class="row.isBanned ? 'status-banned' : 'status-normal'">
            {{ row.isBanned ? '已封禁' : '正常' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canEdit" size="small" text @click="openEdit(row)">编辑</el-button>
          <el-button v-if="canAssign" size="small" text @click="openPermission(row)">权限</el-button>
          <el-button v-if="canBan" size="small" text @click="toggleBan(row)">
            {{ row.isBanned ? '解封' : '封禁' }}
          </el-button>
          <el-button v-if="canSetOp" size="small" text @click="toggleOp(row)">
            {{ row.isOp ? '取消管理员' : '设为管理员' }}
          </el-button>
          <el-button v-if="canDelete" size="small" text type="danger" @click="removeUser(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadUsers"
      />
    </div>

    <!-- 新增 / 编辑用户 -->
    <el-dialog
      v-model="formDialog.visible"
      :title="formDialog.isCreate ? '新增用户' : '编辑用户'"
      width="480px"
      class="dark-dialog"
    >
      <el-form :model="formDialog.form" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="formDialog.form.username" placeholder="3-16 位字母、数字、下划线" maxlength="16" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="formDialog.form.email" maxlength="64" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="formDialog.form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item :label="formDialog.isCreate ? '初始密码' : '重置密码'">
          <el-input
            v-model="formDialog.form.password"
            type="password"
            show-password
            :placeholder="formDialog.isCreate ? '请输入初始密码' : '留空则不修改密码'"
            maxlength="48"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="formDialog.submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配 -->
    <el-dialog v-model="permDialog.visible" title="权限分配" width="640px" class="dark-dialog">
      <div v-if="permDialog.username" class="perm-user">
        用户：<b>{{ permDialog.username }}</b>
        <el-tag v-if="permDialog.isOp" type="danger" size="small" effect="dark">平台管理员</el-tag>
        <span v-if="permDialog.isOp" class="muted">（平台管理员拥有全部权限，无需分配）</span>
      </div>

      <el-tabs v-model="permDialog.tab">
        <el-tab-pane label="权限组" name="groups">
          <el-checkbox-group v-model="permDialog.groupIds" class="group-select">
            <el-checkbox v-for="group in allGroups" :key="group.id" :value="group.id">
              {{ group.name }}
              <code class="node-code">{{ group.code }}</code>
              <span class="muted">（{{ group.nodes.length }} 个节点）</span>
            </el-checkbox>
          </el-checkbox-group>
        </el-tab-pane>
        <el-tab-pane label="直接节点" name="nodes">
          <PermissionNodeTree v-model="permDialog.directNodes" :groups="nodeGroups" />
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="permDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="permDialog.submitting" @click="submitPermission">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { del, get, post, put } from '@/utils/http'
import { useUserStore } from '@/stores/user'
import PermissionNodeTree from './PermissionNodeTree.vue'
import type { AdminUser, AdminUserForm, PermissionGroup, PermissionNodeGroup } from '@/types/admin'

interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

const userStore = useUserStore()

const canAdd = computed(() => userStore.hasPermission('user:add'))
const canEdit = computed(() => userStore.hasPermission('user:edit'))
const canDelete = computed(() => userStore.hasPermission('user:delete'))
const canBan = computed(() => userStore.hasPermission('user:ban'))
const canSetOp = computed(() => userStore.hasPermission('user:setop'))
const canAssign = computed(() =>
  userStore.hasAnyPermission(['permission:group:assign', 'permission:user:assign'])
)

const loading = ref(false)
const users = ref<AdminUser[]>([])
// 高级检索：每一行是一个条件，行间用 connector 指定「并且 / 或者」
interface SearchCondition {
  id: number
  field: string
  matchType: string
  value: string
  connector: string
}

const fieldOptions = [
  { label: '用户名', value: 'username' },
  { label: '邮箱', value: 'email' },
  { label: '学校', value: 'school' },
  { label: '学号', value: 'staffNo' },
  { label: '姓名', value: 'realName' },
  { label: '班级', value: 'className' }
]

// 条件行用自增 id 作为 key，删除中间行时不会复用其它行的输入状态
let conditionSeq = 0
const createCondition = (): SearchCondition => ({
  id: ++conditionSeq,
  field: 'username',
  matchType: 'contains',
  value: '',
  connector: 'and'
})

const searchConditions = ref<SearchCondition[]>([createCondition()])
const page = ref(1)
const pageSize = 10
const total = ref(0)

const allGroups = ref<PermissionGroup[]>([])
const nodeGroups = ref<PermissionNodeGroup[]>([])

const emptyForm = (): AdminUserForm => ({
  username: '',
  email: '',
  phone: '',
  password: ''
})

const formDialog = reactive({
  visible: false,
  isCreate: true,
  submitting: false,
  editingId: 0,
  form: emptyForm()
})

const permDialog = reactive({
  visible: false,
  submitting: false,
  tab: 'groups',
  userId: 0,
  username: '',
  isOp: false,
  groupIds: [] as number[],
  directNodes: [] as string[]
})

const loadUsers = async () => {
  loading.value = true
  try {
    // 只提交填写了内容的条件行，组合规则由后端按 CNKI 语义在数据库中执行
    const conditions = searchConditions.value
      .filter(condition => condition.value.trim())
      .map(condition => ({
        field: condition.field,
        matchType: condition.matchType,
        value: condition.value.trim(),
        connector: condition.connector
      }))
    const result = await post<PageResult<AdminUser>>(
      `/admin/users/search?pageNum=${page.value}&pageSize=${pageSize}`,
      { conditions }
    )
    if (result.code === 200) {
      users.value = result.data!.records
      total.value = result.data!.total
    } else {
      ElMessage.error(result.message)
    }
  } finally {
    loading.value = false
  }
}

// 与后端 @Size(max = 10) 保持一致，避免超限后才在提交时报错
const MAX_CONDITIONS = 10

const addCondition = () => {
  if (searchConditions.value.length >= MAX_CONDITIONS) {
    return
  }
  searchConditions.value.push(createCondition())
}

const removeCondition = (index: number) => {
  searchConditions.value.splice(index, 1)
  // 删除的是首行时，新首行的连接符已无意义，复位为「并且」
  if (index === 0 && searchConditions.value.length > 0) {
    searchConditions.value[0].connector = 'and'
  }
}

const search = () => {
  page.value = 1
  loadUsers()
}

const resetSearch = () => {
  searchConditions.value = [createCondition()]
  search()
}

const loadMeta = async () => {
  const [groupsResult, nodesResult] = await Promise.all([
    get<PermissionGroup[]>('/admin/permissions/groups'),
    get<PermissionNodeGroup[]>('/admin/permissions/nodes')
  ])
  if (groupsResult.code === 200) {
    allGroups.value = groupsResult.data ?? []
  }
  if (nodesResult.code === 200) {
    nodeGroups.value = nodesResult.data ?? []
  }
}

const openCreate = () => {
  // 上一次处于编辑状态时切回新建需清空表单；连续新建则保留上次未提交的草稿
  if (formDialog.editingId !== 0) {
    formDialog.form = emptyForm()
  }
  formDialog.isCreate = true
  formDialog.editingId = 0
  formDialog.visible = true
}

const openEdit = (row: AdminUser) => {
  // 重新打开同一个用户时保留未提交的编辑内容
  if (formDialog.editingId !== row.id) {
    formDialog.form = {
      username: row.username ?? '',
      email: row.email ?? '',
      phone: row.phone ?? '',
      password: ''
    }
  }
  formDialog.isCreate = false
  formDialog.editingId = row.id
  formDialog.visible = true
}

const submitForm = async () => {
  const form = formDialog.form
  if (!form.username || !form.email) {
    ElMessage.warning('请填写用户名和邮箱')
    return
  }
  if (formDialog.isCreate && !form.password) {
    ElMessage.warning('请填写初始密码')
    return
  }

  formDialog.submitting = true
  try {
    const payload: Record<string, unknown> = {
      username: form.username,
      email: form.email,
      phone: form.phone
    }
    if (form.password) {
      payload.password = form.password
    }

    const result = formDialog.isCreate
      ? await post<AdminUser>('/admin/users', payload)
      : await put<AdminUser>(`/admin/users/${formDialog.editingId}`, payload)

    if (result.code === 200) {
      const created = formDialog.isCreate
      ElMessage.success(created ? '用户创建成功' : '用户信息更新成功')
      formDialog.visible = false
      formDialog.isCreate = true
      formDialog.editingId = 0
      formDialog.form = emptyForm()
      loadUsers()
    } else {
      ElMessage.error(result.message)
    }
  } finally {
    formDialog.submitting = false
  }
}

const removeUser = async (row: AdminUser) => {
  try {
    await ElMessageBox.confirm(
      `确认删除用户「${row.username}」？该操作不可恢复。`,
      '危险操作',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  const result = await del(`/admin/users/${row.id}`)
  if (result.code === 200) {
    ElMessage.success('用户已删除')
    loadUsers()
  } else {
    ElMessage.error(result.message)
  }
}

const toggleBan = async (row: AdminUser) => {
  if (row.isBanned) {
    const result = await put<AdminUser>(`/admin/users/${row.id}/ban`, { banned: false })
    if (result.code === 200) {
      ElMessage.success('用户已解封')
      loadUsers()
    } else {
      ElMessage.error(result.message)
    }
    return
  }

  try {
    const { value } = await ElMessageBox.prompt(
      `确认封禁用户「${row.username}」？请填写封禁原因。`,
      '封禁用户',
      { confirmButtonText: '确认封禁', cancelButtonText: '取消', inputPlaceholder: '封禁原因' }
    )
    const result = await put<AdminUser>(`/admin/users/${row.id}/ban`, { banned: true, reason: value })
    if (result.code === 200) {
      ElMessage.success('用户已封禁')
      loadUsers()
    } else {
      ElMessage.error(result.message)
    }
  } catch {
    // 取消
  }
}

const toggleOp = async (row: AdminUser) => {
  const next = !row.isOp
  try {
    await ElMessageBox.confirm(
      next
        ? `确认将「${row.username}」设为平台管理员？平台管理员拥有全部权限。`
        : `确认取消「${row.username}」的平台管理员身份？`,
      '提示',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  const result = await put<AdminUser>(`/admin/users/${row.id}/op`, { isOp: next })
  if (result.code === 200) {
    ElMessage.success(next ? '已设为平台管理员' : '已取消平台管理员身份')
    loadUsers()
  } else {
    ElMessage.error(result.message)
  }
}

const openPermission = async (row: AdminUser) => {
  await loadMeta()
  // 重新打开同一个用户时保留未提交的权限配置
  if (permDialog.userId !== row.id) {
    const result = await get<{
      userId: number
      username: string
      isOp: boolean
      groups: PermissionGroup[]
      directNodes: string[]
    }>(`/admin/users/${row.id}/permissions`)

    if (result.code !== 200) {
      ElMessage.error(result.message)
      return
    }

    permDialog.username = result.data!.username
    permDialog.isOp = result.data!.isOp
    permDialog.groupIds = (result.data!.groups ?? []).map(group => group.id)
    permDialog.directNodes = result.data!.directNodes ?? []
    permDialog.tab = 'groups'
  }

  permDialog.userId = row.id
  permDialog.visible = true
}

const submitPermission = async () => {
  permDialog.submitting = true
  try {
    const groupsResult = await put(`/admin/users/${permDialog.userId}/groups`, {
      groupIds: permDialog.groupIds
    })
    if (groupsResult.code !== 200) {
      ElMessage.error(groupsResult.message)
      return
    }
    const nodesResult = await put(`/admin/users/${permDialog.userId}/nodes`, {
      nodes: permDialog.directNodes
    })
    if (nodesResult.code !== 200) {
      ElMessage.error(nodesResult.message)
      return
    }
    ElMessage.success('权限已更新')
    permDialog.visible = false
    loadUsers()
  } finally {
    permDialog.submitting = false
  }
}

onMounted(() => {
  loadUsers()
  loadMeta()
})
</script>

<style scoped>
/* CNKI 式高级检索：条件行 + 行间「并且 / 或者」 */
.advanced-search {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: fit-content;
  max-width: 100%;
  margin-bottom: 20px;
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--toolbar-bg);
}

.condition-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* 连接符列：首行占位与其余行的单选组保持同宽，使各行的下拉与输入框左右对齐 */
.connector-group,
.condition-placeholder {
  flex: 0 0 120px;
  width: 120px;
}

.condition-placeholder {
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.field-select {
  flex: 0 0 120px;
  width: 120px;
}

.match-select {
  flex: 0 0 100px;
  width: 100px;
}

.value-input {
  flex: 0 0 240px;
  width: 240px;
}

.condition-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 4px;
  padding-left: 130px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.group-name {
  margin-right: 6px;
  color: var(--primary-color);
}

/* 权限列纯文字展示：平台管理员为红、权限组名为主色，不使用标签底色与边框 */
.perm-op {
  color: var(--danger-strong);
}

.muted {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
  font-size: 13px;
}

/* 状态列纯文字展示：正常为绿、封禁为红，不使用标签底色与边框 */
.status-normal {
  color: var(--success-text);
}

.status-banned {
  color: var(--danger-strong);
}

.node-code {
  margin-left: 6px;
  font-size: 12px;
  color: #667eea;
}

.perm-user {
  margin-bottom: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
}

.group-select {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 50vh;
  overflow-y: auto;
}

.admin-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04);
  --el-table-text-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
  --el-table-header-text-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
  --el-table-border-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
}

.admin-table :deep(.el-table__inner-wrapper::before) {
  background-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
}

.admin-table :deep(.el-table__row:hover > td) {
  background-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.05) !important;
}

:deep(.el-checkbox__label) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
}
</style>
