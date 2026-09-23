<template>
  <div class="group-manage">
    <div class="filter-bar">
      <el-button v-if="canAdd" type="primary" plain @click="openCreate">新建权限组</el-button>
      <span class="hint">权限节点由系统内置定义，权限组用于把一组节点打包授予用户</span>
    </div>

    <el-table v-loading="loading" :data="groups" class="admin-table">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="code" label="标识" min-width="140">
        <template #default="{ row }">
          <code class="node-code">{{ row.code }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180">
        <template #default="{ row }">
          <span class="muted">{{ row.description || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="节点数" width="90">
        <template #default="{ row }">{{ row.nodes?.length ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="用户数" width="90">
        <template #default="{ row }">{{ row.userCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="默认组" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault" type="success" size="small">是</el-tag>
          <span v-else class="muted">否</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canEdit" size="small" text @click="openEdit(row)">编辑</el-button>
          <el-button v-if="canEdit" size="small" text @click="openNodes(row)">配置节点</el-button>
          <el-button v-if="canDelete" size="small" text type="danger" @click="removeGroup(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建 / 编辑权限组 -->
    <el-dialog
      v-model="formDialog.visible"
      :title="formDialog.isCreate ? '新建权限组' : '编辑权限组'"
      width="460px"
      class="dark-dialog"
    >
      <el-form :model="formDialog.form" label-width="90px">
        <el-form-item label="标识">
          <el-input
            v-model="formDialog.form.code"
            :disabled="!formDialog.isCreate"
            placeholder="如 class-admin，仅字母数字下划线连字符"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="formDialog.form.name" placeholder="如 班级管理员" maxlength="64" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formDialog.form.description" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
        <el-form-item label="默认组">
          <el-switch v-model="formDialog.form.isDefault" />
          <span class="muted switch-hint">开启后新注册用户自动加入该组</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="formDialog.submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 配置节点 -->
    <el-dialog v-model="nodesDialog.visible" title="配置权限节点" width="620px" class="dark-dialog">
      <div class="perm-user">
        权限组：<b>{{ nodesDialog.groupName }}</b>
        <span class="muted">（已选 {{ nodesDialog.nodes.length }} 个节点）</span>
      </div>
      <PermissionNodeTree v-model="nodesDialog.nodes" :groups="nodeGroups" />
      <template #footer>
        <el-button @click="nodesDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="nodesDialog.submitting" @click="submitNodes">保存</el-button>
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
import type { PermissionGroup, PermissionNodeGroup } from '@/types/admin'

const userStore = useUserStore()

const canAdd = computed(() => userStore.hasPermission('permission:group:add'))
const canEdit = computed(() => userStore.hasPermission('permission:group:edit'))
const canDelete = computed(() => userStore.hasPermission('permission:group:delete'))

const loading = ref(false)
const groups = ref<PermissionGroup[]>([])
const nodeGroups = ref<PermissionNodeGroup[]>([])

const formDialog = reactive({
  visible: false,
  isCreate: true,
  submitting: false,
  editingId: 0,
  form: { code: '', name: '', description: '', isDefault: false }
})

const nodesDialog = reactive({
  visible: false,
  submitting: false,
  groupId: 0,
  groupName: '',
  nodes: [] as string[]
})

const loadGroups = async () => {
  loading.value = true
  try {
    const result = await get<PermissionGroup[]>('/admin/permissions/groups')
    if (result.code === 200) {
      groups.value = result.data ?? []
    } else {
      ElMessage.error(result.message)
    }
  } finally {
    loading.value = false
  }
}

const loadNodes = async () => {
  const result = await get<PermissionNodeGroup[]>('/admin/permissions/nodes')
  if (result.code === 200) {
    nodeGroups.value = result.data ?? []
  }
}

const emptyGroupForm = () => ({ code: '', name: '', description: '', isDefault: false })

const resetGroupForm = () => {
  formDialog.isCreate = true
  formDialog.editingId = 0
  formDialog.form = emptyGroupForm()
}

const openCreate = () => {
  // 上一次处于编辑状态时切回新建需清空表单；连续新建则保留上次未提交的草稿
  if (formDialog.editingId !== 0) {
    resetGroupForm()
  }
  formDialog.isCreate = true
  formDialog.visible = true
}

const openEdit = (row: PermissionGroup) => {
  // 重新打开同一个权限组时保留未提交的编辑内容
  if (formDialog.editingId !== row.id) {
    formDialog.form = {
      code: row.code,
      name: row.name,
      description: row.description ?? '',
      isDefault: !!row.isDefault
    }
  }
  formDialog.isCreate = false
  formDialog.editingId = row.id
  formDialog.visible = true
}

const submitForm = async () => {
  if (formDialog.isCreate && !formDialog.form.code) {
    ElMessage.warning('请填写权限组标识')
    return
  }
  if (!formDialog.form.name) {
    ElMessage.warning('请填写权限组名称')
    return
  }

  formDialog.submitting = true
  try {
    const payload: Record<string, unknown> = {
      name: formDialog.form.name,
      description: formDialog.form.description,
      isDefault: formDialog.form.isDefault
    }
    if (formDialog.isCreate) {
      payload.code = formDialog.form.code
    }

    const result = formDialog.isCreate
      ? await post<PermissionGroup>('/admin/permissions/groups', payload)
      : await put<PermissionGroup>(`/admin/permissions/groups/${formDialog.editingId}`, payload)

    if (result.code === 200) {
      const created = formDialog.isCreate
      ElMessage.success(created ? '权限组创建成功' : '权限组更新成功')
      formDialog.visible = false
      resetGroupForm()
      loadGroups()
    } else {
      ElMessage.error(result.message)
    }
  } finally {
    formDialog.submitting = false
  }
}

const removeGroup = async (row: PermissionGroup) => {
  try {
    await ElMessageBox.confirm(
      `确认删除权限组「${row.name}」？组内用户将失去该组带来的权限。`,
      '危险操作',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  const result = await del(`/admin/permissions/groups/${row.id}`)
  if (result.code === 200) {
    ElMessage.success('权限组已删除')
    loadGroups()
  } else {
    ElMessage.error(result.message)
  }
}

const openNodes = async (row: PermissionGroup) => {
  await loadNodes()
  // 重新打开同一个权限组时保留未提交的节点选择
  if (nodesDialog.groupId !== row.id) {
    const result = await get<PermissionGroup[]>('/admin/permissions/groups')
    const current = (result.code === 200 ? result.data ?? [] : []).find(item => item.id === row.id)
    nodesDialog.nodes = [...(current?.nodes ?? row.nodes ?? [])]
  }
  nodesDialog.groupId = row.id
  nodesDialog.groupName = row.name
  nodesDialog.visible = true
}

const submitNodes = async () => {
  nodesDialog.submitting = true
  try {
    const result = await put(`/admin/permissions/groups/${nodesDialog.groupId}/nodes`, {
      nodes: nodesDialog.nodes
    })
    if (result.code === 200) {
      ElMessage.success('权限节点已更新')
      nodesDialog.visible = false
      loadGroups()
    } else {
      ElMessage.error(result.message)
    }
  } finally {
    nodesDialog.submitting = false
  }
}

onMounted(() => {
  loadGroups()
  loadNodes()
})
</script>

<style scoped>
.hint {
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
}

.muted {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
  font-size: 13px;
}

.node-code {
  color: #667eea;
  font-size: 13px;
}

.perm-user {
  margin-bottom: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
}

.switch-hint {
  margin-left: 10px;
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
</style>
