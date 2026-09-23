<template>
  <div class="school-center-page">
    <div class="page-header">
      <div class="header-left">
        <h2>我的学校</h2>
        <p class="subtitle">学工号与姓名随学校确定，加入班级前需先加入对应学校</p>
      </div>
      <div class="header-right">
        <el-button @click="openBrowseDialog">
          <Plus :size="18" />
          加入学校
        </el-button>
      </div>
    </div>

    <el-card class="content-card">
      <div class="school-grid">
        <div
          v-for="school in mySchools"
          :key="school.id"
          class="school-card"
          @click="goToSchool(school.id)"
        >
          <div class="card-header">
            <div class="school-icon">
              <School :size="24" />
            </div>
            <h3>{{ school.schoolName }}</h3>
          </div>
          <p class="description">{{ school.description || '暂无描述' }}</p>
          <div class="identity">
            <div class="identity-item">
              <span class="label">姓名</span>
              <span class="value">{{ school.myRealName || '-' }}</span>
            </div>
            <div class="identity-item">
              <span class="label">学工号</span>
              <span class="value">{{ school.myStaffNo || '-' }}</span>
            </div>
          </div>
          <div class="card-footer">
            <span :class="['role-badge', school.myRoleCode === 2 ? 'admin' : 'member']">
              {{ school.myRole }}
            </span>
            <span class="member-count">{{ school.memberCount }} 名成员</span>
          </div>
        </div>
      </div>
      <div v-if="mySchools.length === 0" class="empty-state">
        <School :size="48" />
        <p>你还没有加入任何学校</p>
        <p class="empty-tip">加入学校后才能加入班级、提交作业</p>
      </div>
    </el-card>

    <!-- 浏览并加入学校 -->
    <el-dialog v-model="browseDialog.visible" title="加入学校" width="560px" class="dark-dialog">
      <div class="browse-filter">
        <el-input
          v-model="browseDialog.keyword"
          placeholder="搜索学校名称"
          clearable
          @clear="loadSchools"
          @keyup.enter="loadSchools"
        />
        <el-button type="primary" @click="loadSchools">搜索</el-button>
      </div>

      <div class="browse-list">
        <div v-for="school in browseDialog.schools" :key="school.id" class="browse-item">
          <div class="browse-info">
            <h4>{{ school.schoolName }}</h4>
            <div class="browse-meta">
              <span :class="['flag', school.allowJoinWithoutApproval ? 'auto' : 'manual']">
                {{ school.allowJoinWithoutApproval ? '免审核加入' : '需管理员审核' }}
              </span>
              <span class="member-count">{{ school.memberCount }} 名成员</span>
            </div>
          </div>
          <el-button size="small" type="primary" plain @click="openJoinDialog(school)">
            加入
          </el-button>
        </div>
        <div v-if="browseDialog.schools.length === 0" class="empty-tip browse-empty">
          没有找到匹配的学校
        </div>
      </div>

      <div class="pagination" v-if="browseDialog.total > 0">
        <el-pagination
          v-model:current-page="browseDialog.page"
          :page-size="8"
          :total="browseDialog.total"
          layout="prev, pager, next"
          @current-change="loadSchools"
        />
      </div>
    </el-dialog>

    <!-- 填写学工号与姓名 -->
    <el-dialog v-model="joinDialog.visible" title="填写你的姓名与学工号" width="440px" class="dark-dialog">
      <p class="dialog-tip">
        申请加入：<b>{{ joinDialog.schoolName }}</b>
      </p>
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
        <el-button type="primary" :loading="joinDialog.submitting" @click="submitJoin">
          提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, School } from '@lucide/vue'
import { get, post } from '@/utils/http'
import { useSchoolStore } from '@/stores/school'
import type { PageResult } from '@/types'
import type { School as SchoolInfo, SchoolDetail, SchoolJoinApplication } from '@/types/school'

const router = useRouter()
const schoolStore = useSchoolStore()

const mySchools = ref<SchoolDetail[]>([])

const loadMySchools = async () => {
  // 以 school store 为准，避免同一接口重复请求
  const loaded = await schoolStore.fetchMySchools(true)
  if (loaded) {
    mySchools.value = schoolStore.mySchools ?? []
  } else {
    ElMessage.error('学校列表加载失败，请稍后重试')
  }
}

const goToSchool = (id: number) => {
  router.push(`/school/${id}`)
}

const browseDialog = reactive({
  visible: false,
  keyword: '',
  page: 1,
  total: 0,
  schools: [] as SchoolInfo[]
})

const loadSchools = async () => {
  const params: Record<string, unknown> = { pageNum: browseDialog.page, pageSize: 8 }
  if (browseDialog.keyword.trim()) {
    params.keyword = browseDialog.keyword.trim()
  }
  const result = await get<PageResult<SchoolInfo>>('/school/list', params)
  if (result.code === 200) {
    browseDialog.schools = result.data!.records
    browseDialog.total = result.data!.total
  } else {
    ElMessage.error(result.message)
  }
}

const openBrowseDialog = () => {
  browseDialog.visible = true
  loadSchools()
}

const joinDialog = reactive({
  visible: false,
  submitting: false,
  schoolId: 0,
  schoolName: '',
  realName: '',
  staffNo: ''
})

const openJoinDialog = (school: SchoolInfo) => {
  joinDialog.visible = true
  joinDialog.submitting = false
  joinDialog.schoolId = school.id
  joinDialog.schoolName = school.schoolName
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
  const result = await post<SchoolJoinApplication>(`/school/${joinDialog.schoolId}/join`, {
    realName: joinDialog.realName.trim(),
    staffNo: joinDialog.staffNo.trim()
  })
  joinDialog.submitting = false
  if (result.code === 200) {
    const joined = result.data?.status === 1
    ElMessage.success(joined ? '已加入学校' : '申请已提交，等待学校管理员审核')
    joinDialog.visible = false
    browseDialog.visible = false
    // 提交完成后清空，避免加入其他学校时沿用上一所学校的学工号
    joinDialog.realName = ''
    joinDialog.staffNo = ''
    loadMySchools()
  } else {
    ElMessage.error(result.message)
  }
}

onMounted(loadMySchools)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.page-header h2 {
  margin: 0;
}

.subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.55);
}

.school-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.school-card {
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.25s;
}

.school-card:hover {
  border-color: rgba(102, 126, 234, 0.5);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.school-icon {
  color: #667eea;
}

.card-header h3 {
  margin: 0;
  font-size: 15px;
}

.description {
  margin: 10px 0;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
  min-height: 18px;
}

.identity {
  display: flex;
  gap: 20px;
  font-size: 13px;
  margin-bottom: 10px;
}

.identity-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.identity-item .label {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.role-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.role-badge.admin {
  color: #667eea;
  background: rgba(102, 126, 234, 0.15);
}

.role-badge.member {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08);
}

.member-count {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.empty-tip {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
}

.browse-filter {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.browse-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 340px;
  overflow-y: auto;
}

.browse-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 8px;
}

.browse-info h4 {
  margin: 0 0 4px;
  font-size: 14px;
}

.browse-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* .flag 加入审核标记样式见全局 style.css（多页共用） */

.browse-empty {
  text-align: center;
  padding: 20px 0;
}

.pagination {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}

.dialog-tip {
  margin: 0 0 12px;
  font-size: 13px;
}

.form-tip {
  margin: 0;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.55);
}

@media (max-width: 768px) {
  /* 手机端页头纵向堆叠，操作按钮另起一行占满宽度 */
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
    margin-bottom: 14px;
  }

  .subtitle {
    font-size: 12px;
  }

  .header-right {
    display: flex;
  }

  .header-right .el-button {
    width: 100%;
    margin-left: 0;
  }

  .school-grid {
    grid-template-columns: 1fr;
  }
}
</style>

