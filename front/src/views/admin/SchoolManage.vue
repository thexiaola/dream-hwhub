<template>
  <div class="school-manage">
    <div class="filter-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索学校名称"
        style="width: 240px"
        clearable
        @clear="loadSchools"
        @keyup.enter="loadSchools"
      />
      <el-button type="primary" @click="loadSchools">搜索</el-button>
      <el-button v-if="canCreate" type="primary" plain @click="openCreateDialog">
        <Plus :size="16" />
        新建学校
      </el-button>
    </div>

    <div class="school-list">
      <div v-for="school in schools" :key="school.id" class="school-card">
        <div class="card-main">
          <div class="card-head">
            <h4>{{ school.schoolName }}</h4>
            <span :class="['flag', school.allowJoinWithoutApproval ? 'auto' : 'manual']">
              {{ school.allowJoinWithoutApproval ? '免审核加入' : '需管理员审核' }}
            </span>
          </div>
          <div class="card-meta">
            <span class="meta-item">
              <span class="label">学校ID：</span>{{ school.id }}
            </span>
            <span class="meta-item">
              <span class="label">成员数：</span>{{ school.memberCount }}
            </span>
            <span class="meta-item">
              <span class="label">创建时间：</span>{{ formatDate(school.createTime) }}
            </span>
            <span v-if="school.description" class="meta-item meta-desc">
              <span class="label">描述：</span>{{ school.description }}
            </span>
          </div>
        </div>

        <div class="card-actions">
          <el-button v-if="canAssign" size="small" type="primary" plain @click="openAdminDialog(school)">
            指派学校管理员
          </el-button>
          <el-button v-if="canUpdate" size="small" @click="openEditDialog(school)">编辑</el-button>
          <el-button v-if="canDissolve" size="small" type="danger" @click="dissolve(school)">
            解散学校
          </el-button>
        </div>
      </div>

      <div v-if="schools.length === 0" class="empty-state">
        <School :size="32" />
        <p>暂无学校</p>
      </div>
    </div>

    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="10"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadSchools"
      />
    </div>

    <!-- 新建 / 编辑学校 -->
    <el-dialog
      v-model="formDialog.visible"
      :title="formDialog.id ? '编辑学校' : '新建学校'"
      width="460px"
      class="dark-dialog"
    >
      <el-form :model="formDialog" label-width="110px">
        <el-form-item label="学校名称" required>
          <el-input v-model="formDialog.schoolName" maxlength="100" placeholder="请输入学校名称" />
        </el-form-item>
        <el-form-item label="学校描述">
          <el-input
            v-model="formDialog.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            placeholder="选填"
          />
        </el-form-item>
        <el-form-item v-if="!formDialog.id" label="加入审核">
          <el-switch
            v-model="formDialog.allowJoinWithoutApproval"
            active-text="免审核"
            inactive-text="需审核"
          />
          <p class="form-tip">免审核时，用户填入学工号与姓名即可直接加入学校</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="formDialog.submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 指派 / 取消学校管理员 -->
    <el-dialog v-model="adminDialog.visible" title="设置学校管理员" width="480px" class="dark-dialog">
      <p class="dialog-tip">
        目标学校：<b>{{ adminDialog.schoolName }}</b>
      </p>
      <el-form :model="adminDialog" label-width="110px">
        <el-form-item label="用户账号" required>
          <el-input v-model="adminDialog.userAccount" placeholder="用户名或邮箱" />
        </el-form-item>
        <el-form-item label="操作">
          <el-radio-group v-model="adminDialog.assigned">
            <el-radio-button :value="true">指派为管理员</el-radio-button>
            <el-radio-button :value="false">取消管理员</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <template v-if="adminDialog.assigned">
          <el-form-item label="学工号">
            <el-input v-model="adminDialog.staffNo" maxlength="24" placeholder="目标用户尚未加入学校时必填" />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="adminDialog.realName" maxlength="32" placeholder="目标用户尚未加入学校时必填" />
          </el-form-item>
          <p class="form-tip">目标用户已是学校成员时，可留空以上两项；取消管理员会将其降为老师。</p>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="adminDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="adminDialog.submitting" @click="submitAdmin">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, School } from '@lucide/vue'
import { del, get, post, put } from '@/utils/http'
import { useUserStore } from '@/stores/user'
import type { PageResult, School as SchoolInfo, SchoolDetail } from '@/types/school'
import { formatDateOnly as formatDate } from '@/utils/format'

const userStore = useUserStore()

const canCreate = computed(() => userStore.hasPermission('school:create'))
const canUpdate = computed(() => userStore.hasPermission('school:update'))
const canDissolve = computed(() => userStore.hasPermission('school:dissolve'))
const canAssign = computed(() => userStore.hasPermission('school:admin:assign'))

const schools = ref<SchoolInfo[]>([])
const keyword = ref('')
const page = ref(1)
const total = ref(0)

const loadSchools = async () => {
  const params: Record<string, unknown> = { pageNum: page.value, pageSize: 10 }
  if (keyword.value.trim()) {
    params.keyword = keyword.value.trim()
  }
  const result = await get<PageResult<SchoolInfo>>('/admin/schools', params)
  if (result.code === 200) {
    schools.value = result.data!.records
    total.value = result.data!.total
  } else {
    ElMessage.error(result.message)
  }
}

const formDialog = reactive({
  visible: false,
  submitting: false,
  id: 0,
  schoolName: '',
  description: '',
  allowJoinWithoutApproval: false
})

const resetFormDialog = () => {
  formDialog.id = 0
  formDialog.schoolName = ''
  formDialog.description = ''
  formDialog.allowJoinWithoutApproval = false
  formDialog.submitting = false
}

const openCreateDialog = () => {
  // 上一次处于编辑状态时切回新建需清空表单；连续新建则保留上次未提交的草稿
  if (formDialog.id !== 0) {
    formDialog.schoolName = ''
    formDialog.description = ''
    formDialog.allowJoinWithoutApproval = false
  }
  formDialog.id = 0
  formDialog.submitting = false
  formDialog.visible = true
}

const openEditDialog = (school: SchoolInfo) => {
  // 重新打开同一所学校时保留未提交的编辑内容
  if (formDialog.id !== school.id) {
    formDialog.schoolName = school.schoolName
    formDialog.description = school.description ?? ''
    formDialog.allowJoinWithoutApproval = false
  }
  formDialog.id = school.id
  formDialog.submitting = false
  formDialog.visible = true
}

const submitForm = async () => {
  if (!formDialog.schoolName.trim()) {
    ElMessage.warning('请输入学校名称')
    return
  }
  formDialog.submitting = true
  const payload: Record<string, unknown> = {
    schoolName: formDialog.schoolName.trim(),
    description: formDialog.description.trim()
  }
  let result
  if (formDialog.id) {
    result = await put<SchoolDetail>(`/admin/schools/${formDialog.id}`, payload)
  } else {
    payload.allowJoinWithoutApproval = formDialog.allowJoinWithoutApproval
    result = await post<SchoolDetail>('/admin/schools', payload)
  }
  formDialog.submitting = false
  if (result.code === 200) {
    const updated = !!formDialog.id
    ElMessage.success(updated ? '学校信息已更新' : '学校创建成功')
    formDialog.visible = false
    resetFormDialog()
    loadSchools()
  } else {
    ElMessage.error(result.message)
  }
}

const dissolve = async (school: SchoolInfo) => {
  try {
    await ElMessageBox.confirm(
      `解散后「${school.schoolName}」的成员关系与加入申请都会被清除，确定继续吗？`,
      '解散学校',
      { type: 'warning', confirmButtonText: '确定解散', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const result = await del(`/admin/schools/${school.id}`)
  if (result.code === 200) {
    ElMessage.success('学校已解散')
    loadSchools()
  } else {
    ElMessage.error(result.message)
  }
}

const adminDialog = reactive({
  visible: false,
  submitting: false,
  schoolId: 0,
  schoolName: '',
  userAccount: '',
  assigned: true,
  staffNo: '',
  realName: ''
})

const openAdminDialog = (school: SchoolInfo) => {
  adminDialog.visible = true
  adminDialog.submitting = false
  adminDialog.schoolId = school.id
  adminDialog.schoolName = school.schoolName
  adminDialog.assigned = true
}

const submitAdmin = async () => {
  if (!adminDialog.userAccount.trim()) {
    ElMessage.warning('请输入用户账号')
    return
  }
  adminDialog.submitting = true
  const result = await put(`/admin/schools/${adminDialog.schoolId}/admin`, {
    userAccount: adminDialog.userAccount.trim(),
    assigned: adminDialog.assigned,
    staffNo: adminDialog.staffNo.trim(),
    realName: adminDialog.realName.trim()
  })
  adminDialog.submitting = false
  if (result.code === 200) {
    ElMessage.success(adminDialog.assigned ? '已指派为学校管理员' : '已取消学校管理员身份')
    adminDialog.visible = false
    // 提交完成后清空，避免给其他学校指派管理员时沿用上一个账号
    adminDialog.userAccount = ''
    adminDialog.staffNo = ''
    adminDialog.realName = ''
    loadSchools()
  } else {
    ElMessage.error(result.message)
  }
}

onMounted(loadSchools)
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.school-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 学校卡片：左侧信息 + 右侧操作，单行紧凑排列，学校较多时更省纵向空间 */
.school-card {
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 10px;
  padding: 12px 16px;
  background: var(--bg-elevated);
}

.card-main {
  flex: 1;
  min-width: 0;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.card-head h4 {
  margin: 0;
  font-size: 15px;
}

.card-meta {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
}

.card-meta .label {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.meta-desc {
  min-width: 0;
  overflow-wrap: anywhere;
}

.flag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.flag.auto {
  color: #67c23a;
  background: rgba(103, 194, 58, 0.15);
}

.flag.manual {
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.15);
}

/* 操作区：独立成一块高亮面板，与左侧信息区拉开层次 */
.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding: 8px 10px;
  border-radius: 10px;
  background: var(--action-bar-bg);
  border: 1px solid var(--action-bar-border);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.dialog-tip {
  margin: 0 0 12px;
  font-size: 13px;
}

.form-tip {
  margin: 4px 0 0;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.55);
}

/* 窄屏下改为纵向堆叠，避免操作按钮被挤压 */
@media (max-width: 768px) {
  .school-card {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .card-actions {
    flex-wrap: wrap;
  }

  .card-actions .el-button {
    flex: 1;
    min-width: 0;
    margin-left: 0;
  }
}
</style>
