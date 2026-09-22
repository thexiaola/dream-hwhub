<template>
  <div class="user-manage">
    <div class="filter-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名 / 邮箱"
        style="width: 260px"
        clearable
        @clear="search"
        @keyup.enter="search"
      />
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button v-if="canAdd" type="primary" plain @click="openCreate">新增用户</el-button>
    </div>

    <el-table v-loading="loading" :data="users" class="admin-table">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="110" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="权限" min-width="150">
        <template #default="{ row }">
          <el-tag v-if="row.isOp" type="danger" size="small" effect="dark">平台管理员</el-tag>
          <template v-else-if="row.groupNames?.length">
            <el-tag v-for="name in row.groupNames" :key="name" size="small" class="group-tag">
              {{ name }}
            </el-tag>
          </template>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.isBanned ? 'danger' : 'success'" size="small">
            {{ row.isBanned ? '已封禁' : '正常' }}
          </el-tag>
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
const keyword = ref('')
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
    const params: Record<string, unknown> = { pageNum: page.value, pageSize }
    if (keyword.value) {
      params.keyword = keyword.value
    }
    const result = await get<PageResult<AdminUser>>('/admin/users', params)
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

const search = () => {
  page.value = 1
  loadUsers()
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
  formDialog.isCreate = true
  formDialog.editingId = 0
  formDialog.form = emptyForm()
  formDialog.visible = true
}

const openEdit = (row: AdminUser) => {
  formDialog.isCreate = false
  formDialog.editingId = row.id
  formDialog.form = {
    username: row.username ?? '',
    email: row.email ?? '',
    phone: row.phone ?? '',
    password: ''
  }
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
      ElMessage.success(formDialog.isCreate ? '用户创建成功' : '用户信息更新成功')
      formDialog.visible = false
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

  permDialog.userId = row.id
  permDialog.username = result.data!.username
  permDialog.isOp = result.data!.isOp
  permDialog.groupIds = (result.data!.groups ?? []).map(group => group.id)
  permDialog.directNodes = result.data!.directNodes ?? []
  permDialog.tab = 'groups'
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
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.group-tag {
  margin-right: 6px;
}

.muted {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
  font-size: 13px;
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
