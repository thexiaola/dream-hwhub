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

    <!-- 学校管理入口：仅真正的学校管理员可见。
         管理控制台已独立成整页 /school/:id/manage，这里只放一个入口，避免在详情页
         内嵌一大块管理面板。平台管理员请在「管理面板 → 学校管理」中对任意学校进行管理。 -->
    <el-card class="content-card manage-entry-card" v-if="canManage">
      <div class="manage-entry">
        <div class="manage-entry-info">
          <div class="manage-entry-title">
            <Settings :size="18" />
            <span>学校管理</span>
          </div>
          <p class="manage-entry-desc">
            审核加入申请、管理成员与身份、处理班级接管、设置学校私信策略
          </p>
        </div>
        <el-button type="primary" @click="goManage">
          进入管理
          <ArrowRight :size="16" />
        </el-button>
      </div>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Settings } from '@lucide/vue'
import { del, get, post } from '@/utils/http'
import { confirmDangerousOperation } from '@/composables/useSensitiveVerification'
import { SCHOOL_ROLE_ADMIN, type SchoolDetail, type SchoolJoinApplication } from '@/types/school'

const route = useRoute()
const router = useRouter()

const schoolId = Number(route.params.id)
const school = ref<SchoolDetail | null>(null)
const loading = ref(false)

// 仅真正的学校管理员可在个人视角管理本校；
// 平台管理员即便持有 school:update，也统一从「管理面板 → 学校管理」进入，避免个人页出现全局管理界面
const canManage = computed(() => school.value?.myRoleCode === SCHOOL_ROLE_ADMIN)

// 进入独立的学校管理页（整页管理控制台）
const goManage = () => {
  router.push(`/school/${schoolId}/manage`)
}

const loadSchool = async () => {
  loading.value = true
  const result = await get<SchoolDetail>(`/school/${schoolId}`)
  loading.value = false
  if (result.code === 200) {
    school.value = result.data
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

// ===== 退出学校 =====

const leaveSchool = async () => {
  // 退出学校属敏感操作：红色警示框 + 身份二次验证（登录密码或邮箱验证码）
  const headers = await confirmDangerousOperation({
    title: '退出学校',
    message: `确认退出「${school.value?.schoolName ?? ''}」？退出后你将失去该校的成员身份，需要重新申请加入。`,
    confirmText: '确认退出',
    operationName: '退出学校',
  })
  if (!headers) return
  const result = await del(`/school/${schoolId}/membership`, undefined, undefined, headers)
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

.form-tip {
  margin: 0;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.55);
}

/* 学校管理入口卡片：一段说明 + 右侧进入按钮 */
.manage-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.manage-entry-info {
  min-width: 0;
}

.manage-entry-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.92);
}

.manage-entry-title svg {
  color: #667eea;
}

.manage-entry-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

@media (max-width: 768px) {
  /* 手机端按钮另起一行占满宽度 */
  .manage-entry {
    flex-direction: column;
    align-items: stretch;
  }

  .manage-entry .el-button {
    justify-content: center;
    width: 100%;
  }
}
</style>
