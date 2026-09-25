<template>
  <div class="school-center-page">
    <div class="page-header">
      <div class="header-left">
        <h2>我的学校</h2>
        <p class="subtitle">学工号与姓名随学校确定，加入班级前需先加入对应学校</p>
      </div>
      <div class="header-right">
        <el-input
          v-model="searchKeyword"
          class="school-search"
          placeholder="搜索学校名称"
          clearable
          @clear="applySearch"
          @keyup.enter="applySearch"
        >
          <template #prefix>
            <Search :size="16" />
          </template>
        </el-input>
        <el-button type="primary" @click="applySearch">搜索</el-button>
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
          :class="{ 'is-admin': school.myRoleCode === SCHOOL_ROLE_ADMIN }"
          @click="goToSchool(school.id)"
        >
          <!-- 我是该校管理员的醒目标注：右上角角标 -->
          <span
            v-if="school.myRoleCode === SCHOOL_ROLE_ADMIN"
            class="admin-ribbon"
          >
            <ShieldCheck :size="13" />
            我是管理员
          </span>
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
            <span :class="['role-badge', school.myRoleCode === SCHOOL_ROLE_ADMIN ? 'admin' : 'member']">
              {{ school.myRole }}
            </span>
            <span class="member-count">{{ school.memberCount }} 名成员</span>
          </div>
          <!-- 仅该学校的学校管理员可见管理入口；其余成员只看到基础信息 -->
          <el-button
            v-if="school.myRoleCode === SCHOOL_ROLE_ADMIN"
            class="card-manage-btn"
            size="small"
            type="primary"
            plain
            @click.stop="openManageDialog(school)"
          >
            <Settings :size="14" />
            管理学校
          </el-button>
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
import { Plus, School, Search, Settings, ShieldCheck } from '@lucide/vue'
import { get, post } from '@/utils/http'
import { useSchoolStore } from '@/stores/school'
import type { PageResult } from '@/types'
import { SCHOOL_ROLE_ADMIN, type School as SchoolInfo, type SchoolDetail, type SchoolJoinApplication } from '@/types/school'

const router = useRouter()
const schoolStore = useSchoolStore()

const mySchools = ref<SchoolDetail[]>([])
// 搜索关键字：提交给后端做数据库筛选（前端不再二次过滤）
const searchKeyword = ref('')

// 学校管理：跳转到独立管理页（仅该学校的学校管理员可见入口）。
// 原先在右侧抽屉里呈现管理控制台过于拥挤，改为整页 /school/:id/manage。
const openManageDialog = (school: SchoolDetail) => {
  router.push(`/school/${school.id}/manage`)
}

const loadMySchools = async () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    // 无关键字：用全局学校列表（含缓存），避免同一接口重复请求
    const loaded = await schoolStore.fetchMySchools(true)
    if (loaded) {
      mySchools.value = schoolStore.mySchools ?? []
    } else {
      ElMessage.error('学校列表加载失败，请稍后重试')
    }
    return
  }
  // 有关键字：直接向后端查询，由数据库按学校名称筛选后返回；
  // 不写入全局 store，避免污染顶部「选择学校」等依赖完整列表的地方
  const result = await get<SchoolDetail[]>('/school/mine', { keyword })
  if (result.code === 200) {
    mySchools.value = result.data ?? []
  } else {
    ElMessage.error(result.message)
  }
}

// 点击搜索 / 回车 / 清空：带上当前关键字重新查询
const applySearch = () => {
  loadMySchools()
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
/* 页面撑满内容区高度：页头固定，下方卡片纵向延伸铺满。
   本页是路由组件，直接挂在块级的 .main-content 下，故用 min-height:100%
   （父级高度确定）而非 flex:1（那只在 flex 父容器下生效）。 */
.school-center-page {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

/* 内容卡片纵向撑满剩余高度：内容少时铺满一屏，内容多时随内容增长并滚动。
   卡片底边与页面底部的间距由 .main-content 的内边距提供。 */
.content-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.content-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

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

/* 页头右侧：搜索框 + 加入学校按钮，同一行右对齐 */
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.school-search {
  width: 220px;
}

/* 搜索框内的放大镜前缀图标 */
.school-search :deep(.el-input__prefix) {
  display: flex;
  align-items: center;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
}

.school-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.school-card {
  position: relative;
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

/* 我是该校管理员：描边加粗高亮 + 略深底色，与普通成员学校一眼区分 */
.school-card.is-admin {
  border-color: rgba(102, 126, 234, 0.6);
  background: rgba(102, 126, 234, 0.06);
}

.school-card.is-admin:hover {
  border-color: rgba(102, 126, 234, 0.8);
}

/* 右上角「我是管理员」角标 */
.admin-ribbon {
  position: absolute;
  top: 0;
  right: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px 3px 12px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.5;
  color: #fff;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-top-right-radius: 10px;
  border-bottom-left-radius: 10px;
}

/* 管理员卡片标题为角标让位，避免遮挡学校名 */
.school-card.is-admin .card-header {
  padding-right: 96px;
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

/* 学校管理员专属的管理入口：整行按钮，与上方信息区拉开层次 */
.card-manage-btn {
  width: 100%;
  margin-top: 12px;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
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
    flex-wrap: wrap;
    gap: 8px;
    width: 100%;
  }

  /* 手机端搜索框独占一行，两个按钮平分剩余宽度 */
  .school-search {
    width: 100%;
  }

  .header-right .el-button {
    flex: 1;
    min-width: 0;
    margin-left: 0;
  }

  .school-grid {
    grid-template-columns: 1fr;
  }
}
</style>

