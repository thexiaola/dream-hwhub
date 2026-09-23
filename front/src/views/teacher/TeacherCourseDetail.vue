<template>
  <div v-if="course" class="teacher-course-detail">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" class="page-back-btn" text>
          <ArrowLeft :size="18" />
        </el-button>
        <h2>{{ course?.className }}</h2>
      </div>
      <div class="header-right">
        <el-button
          v-if="course?.canTakeover"
          type="primary"
          :loading="takeoverSubmitting"
          @click="applyTakeoverAction"
        >
          <HandHelping :size="18" />
          申请接管
        </el-button>
        <el-button
          v-else-if="course?.takeoverPending"
          type="info"
          plain
          disabled
        >
          <Clock :size="18" />
          接管申请审核中
        </el-button>
        <el-button
          v-if="!course?.frozen"
          type="primary"
          @click="workPanelRef?.openCreateWork()"
        >
          <Plus :size="18" />
          发布作业
        </el-button>
        <el-button v-if="canDissolve && !course?.frozen" type="danger" @click="dissolveClassAction">
          <Trash2 :size="18" />
          解散课堂
        </el-button>
      </div>
    </div>

    <!-- 班级冻结提示：创建者的教师身份被解除 -->
    <div v-if="course?.frozen" class="frozen-banner">
      <AlertTriangle :size="20" />
      <div class="frozen-text">
        <p class="frozen-title">该班级的老师已失去教师身份，班级暂不可管理</p>
        <p class="frozen-desc">
          期间无法发布/批改作业、邀请同学、修改班级，邀请码也已失效且不再接纳新学生。
          {{ course?.takeoverAutoApprove
            ? '本校老师申请接管后将自动通过。'
            : '本校老师可申请接管，需学校管理员审核。' }}
        </p>
      </div>
    </div>

    <el-card class="course-info-card">
      <div class="info-section">
        <div class="info-item">
          <User :size="16" />
          <span class="label">授课老师：</span>
          <span class="value">{{ course?.ownerName }}</span>
        </div>
        <div class="info-item">
          <Users :size="16" />
          <span class="label">学生人数：</span>
          <span class="value">{{ course?.studentCount }} 人</span>
        </div>
        <div class="info-item" v-if="memberTeacherCount !== null">
          <UserCheck :size="16" />
          <span class="label">教师人数：</span>
          <span class="value">{{ memberTeacherCount }} 人</span>
        </div>
        <div class="info-item">
          <Calendar :size="16" />
          <span class="label">我的角色：</span>
          <span class="value">{{ course?.userRole }}</span>
        </div>
      </div>
      <div v-if="course?.description" class="description-section">
        <h4>课程描述</h4>
        <p>{{ course.description }}</p>
      </div>
    </el-card>

    <el-tabs v-if="!course?.frozen" v-model="activeTab" class="course-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="作业管理" name="works">
        <ClassWorkPanel ref="workPanelRef" :class-id="classId" />
      </el-tab-pane>

      <el-tab-pane name="students">
        <template #label>
          <el-badge
            :value="pendingCount"
            :hidden="pendingCount === 0"
            class="students-tab-badge"
          >
            学生管理
          </el-badge>
        </template>
        <ClassMemberPanel
          ref="memberPanelRef"
          :class-id="classId"
          :owner-id="course?.ownerId ?? 0"
          :my-role="course?.userRole ?? ''"
          :my-role-code="course?.userRoleCode ?? 0"
          :class-name="course?.className ?? ''"
          :allow-student-invite="course?.allowStudentInvite !== false"
          @pending-count-change="pendingCount = $event"
          @members-change="memberTeacherCount = $event"
          @ownership-transferred="reloadCourse"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- ========== 危险操作 Step2：确认文案输入 ========== -->
    <el-dialog
      v-model="showDangerConfirmTextDialog"
      title="二次确认"
      width="520px"
      class="dark-dialog danger-dialog"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
      @close="clearDangerInputs"
    >
      <div class="danger-content">
        <div class="danger-icon">
          <AlertTriangle :size="22" />
        </div>
        <div class="danger-info">
          <p class="danger-title">请完整输入下方确认文案</p>
          <p class="danger-desc">
            解散课堂后，所有作业、成员、邀请等数据将被<b class="danger-strong"
              >永久删除</b
            >，此操作不可恢复。
          </p>
        </div>
      </div>
      <div class="confirm-text-wrap">
        <div class="confirm-text-label">需输入的确认文案：</div>
        <div class="confirm-text-copy">
          <span class="confirm-text-expected">{{ expectedConfirmText }}</span>
          <el-button type="primary" plain size="default" @click="copyExpectedText">
            <Copy :size="14" /> 复制
          </el-button>
        </div>
      </div>
      <el-input
        v-model="dangerConfirmText"
        type="textarea"
        :rows="2"
        placeholder="请输入上方确认文案..."
        class="danger-textarea"
      />
      <template #footer>
        <el-button @click="confirmTextDialogCancel">取消</el-button>
        <el-button
          type="primary"
          :disabled="dangerConfirmText !== expectedConfirmText"
          @click="confirmTextDialogNext"
        >
          下一步
        </el-button>
      </template>
    </el-dialog>

    <!-- ========== 危险操作 Step3：密码校验 ========== -->
    <el-dialog
      v-model="showDangerPasswordDialog"
      title="最终确认"
      width="460px"
      class="dark-dialog danger-dialog"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
      @close="clearDangerInputs"
    >
      <div class="danger-content">
        <div class="danger-icon">
          <ShieldAlert :size="22" />
        </div>
        <div class="danger-info">
          <p class="danger-title">请输入登录密码以继续</p>
          <p class="danger-desc">
            当前操作账号：
            <b class="danger-strong">{{ currentAccountDisplay }}</b>
          </p>
        </div>
      </div>
      <el-input
        v-model="dangerPassword"
        type="password"
        show-password
        placeholder="请输入登录密码"
        @keyup.enter="passwordDialogConfirm"
      />
      <template #footer>
        <el-button @click="passwordDialogCancel">取消</el-button>
        <el-button
          type="danger"
          :disabled="dangerPassword.length === 0"
          :loading="dangerSubmitting"
          @click="passwordDialogConfirm"
        >
          确认解散课堂
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  AlertTriangle,
  ArrowLeft,
  Calendar,
  Clock,
  Copy,
  HandHelping,
  Plus,
  ShieldAlert,
  Trash2,
  User,
  UserCheck,
  Users,
} from "@lucide/vue";
import { del, get, post } from "@/utils/http";
import { useUserStore } from "@/stores/user";
import ClassWorkPanel from "./ClassWorkPanel.vue";
import ClassMemberPanel from "./ClassMemberPanel.vue";
import type { CourseInfo } from "@/types/class";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const classId = Number(route.params.id);

const course = ref<CourseInfo | null>(null);
const activeTab = ref("works");
const pendingCount = ref(0);
const memberTeacherCount = ref<number | null>(null);

const workPanelRef = ref<InstanceType<typeof ClassWorkPanel> | null>(null);
const memberPanelRef = ref<InstanceType<typeof ClassMemberPanel> | null>(null);

const canDissolve = computed(() => {
  if (!course.value || !userStore.userInfo) return false;
  const isOwner = course.value.userRole === "创建者";
  const isAdmin = userStore.hasPermission("class:dissolve");
  return isOwner || isAdmin;
});

const loadCourse = async (): Promise<boolean> => {
  const result = await get<CourseInfo>(`/class/${classId}`);
  if (result.code === 200) {
    const info = result.data!;
    // 管理员可查看任意班级；班级管理员（含接管人）可进入；
    // 班级冻结时允许本校老师进入查看/申请接管（写入操作在后端已全部拦截）
    const isAdmin = userStore.hasPermission("class:view_all");
    const canEnter = isAdmin || info.userRoleCode === 1 || info.frozen === true;
    if (!canEnter) {
      ElMessage.warning("您不是该班级的班级管理员，无权访问教师管理页面");
      router.push("/courses/teacher");
      return false;
    }
    course.value = info;
    return true;
  }
  // 无权访问（非班级成员返回 403）或班级不存在（404）：提示并返回课程列表
  ElMessage.error(result.message || "无法访问该课程");
  router.push("/courses/teacher");
  return false;
};

const reloadCourse = () => {
  loadCourse();
};

const handleTabChange = (tab: string | number) => {
  if (tab === "students") {
    memberPanelRef.value?.reload();
  }
};

const goBack = () => {
  router.push("/courses/teacher");
};

// ========== 危险操作（解散课堂）三步弹窗 ==========
const showDangerConfirmTextDialog = ref(false);
const showDangerPasswordDialog = ref(false);
const dangerConfirmText = ref("");
const dangerPassword = ref("");
const dangerSubmitting = ref(false);

const expectedConfirmText = computed(() => {
  const name = course.value?.className ?? "";
  return `我已确认要删除${name}课堂`;
});

const currentAccountDisplay = computed(() => {
  const u = userStore.userInfo;
  if (!u) return "-";
  return (u.username || u.email || "-") as string;
});

const clearDangerInputs = () => {
  dangerConfirmText.value = "";
  dangerPassword.value = "";
  dangerSubmitting.value = false;
  showDangerConfirmTextDialog.value = false;
  showDangerPasswordDialog.value = false;
};

const copyExpectedText = async () => {
  try {
    await navigator.clipboard.writeText(expectedConfirmText.value);
    ElMessage.success("已复制确认文案");
  } catch {
    ElMessage.warning("复制失败，请手动选中复制");
  }
};

const confirmTextDialogCancel = () => {
  clearDangerInputs();
};

const confirmTextDialogNext = () => {
  if (dangerConfirmText.value !== expectedConfirmText.value) {
    ElMessage.warning("确认文案不匹配");
    return;
  }
  showDangerConfirmTextDialog.value = false;
  showDangerPasswordDialog.value = true;
};

const passwordDialogCancel = () => {
  clearDangerInputs();
};

const passwordDialogConfirm = async () => {
  if (!dangerPassword.value) {
    ElMessage.warning("请输入登录密码");
    return;
  }
  dangerSubmitting.value = true;
  try {
    const params = {
      password: dangerPassword.value,
      confirmText: dangerConfirmText.value,
    };
    // 密码走请求体（DELETE body），避免出现在 URL/访问日志中
    const result = await del(`/class/${classId}`, params);
    if (result.code === 200) {
      ElMessage.success("课堂已解散");
      clearDangerInputs();
      router.push("/courses/teacher");
    } else {
      ElMessage.error(result.message || "解散失败");
    }
  } catch {
    ElMessage.error("解散失败，请重试");
  } finally {
    dangerSubmitting.value = false;
  }
};

const dissolveClassAction = async () => {
  try {
    await ElMessageBox.confirm(
      `解散课堂"${course.value?.className ?? ""}"后，所有作业、成员、邀请等数据将被永久删除，此操作不可恢复。确认解散？`,
      "危险操作",
      {
        confirmButtonText: "确认解散",
        cancelButtonText: "取消",
        type: "warning",
        customClass: "danger-warning-message-box",
      },
    );
    // 进入 Step2：输入确认文案
    showDangerConfirmTextDialog.value = true;
  } catch {
    // 用户取消
  }
};

// ========== 申请接管（班级已冻结时，本校其他老师可接管） ==========
const takeoverSubmitting = ref(false);

const applyTakeoverAction = async () => {
  const auto = course.value?.takeoverAutoApprove;
  try {
    await ElMessageBox.confirm(
      auto
        ? `申请接管「${course.value?.className ?? ""}」后将立即成为该班级创建者（本校已开启自动同意）。确认接管？`
        : `申请接管「${course.value?.className ?? ""}」后将提交学校管理员审核，通过后成为该班级创建者。确认申请？`,
      "申请接管",
      {
        confirmButtonText: auto ? "确认接管" : "提交申请",
        cancelButtonText: "取消",
        type: "warning",
      },
    );
  } catch {
    return;
  }

  takeoverSubmitting.value = true;
  try {
    const result = await post(`/class/${classId}/takeover`);
    if (result.code === 200) {
      ElMessage.success(result.message || "操作成功");
      // 自动同意时所有权已转移，直接进入可管理视图
      const ok = await loadCourse();
      if (ok) memberPanelRef.value?.reload();
    } else {
      ElMessage.error(result.message || "操作失败");
    }
  } catch {
    ElMessage.error("操作失败，请重试");
  } finally {
    takeoverSubmitting.value = false;
  }
};

onMounted(loadCourse);
</script>

<style scoped>
.teacher-course-detail {
  padding-bottom: 24px;
}

/* 班级冻结提示横幅 */
.frozen-banner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid rgba(230, 162, 60, 0.4);
  background: rgba(230, 162, 60, 0.12);
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
}

.frozen-banner svg {
  flex-shrink: 0;
  margin-top: 2px;
  color: #e6a23c;
}

.frozen-text {
  min-width: 0;
}

.frozen-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.frozen-desc {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  font-size: 24px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.course-info-card {
  margin-bottom: 24px;
}

.info-section {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.info-item svg {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
}

.info-item .value {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
  font-weight: 500;
}

.description-section h4 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
}

.description-section p {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
  line-height: 1.6;
}

.course-tabs {
  margin-bottom: 20px;
}

.students-tab-badge {
  line-height: normal;
}

.students-tab-badge :deep(.el-badge__content) {
  z-index: auto;
}

.course-tabs :deep(.el-tabs__item) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.course-tabs :deep(.el-tabs__item.is-active) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.course-tabs :deep(.el-tabs__item:hover) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
}

.course-tabs :deep(.el-tabs__active-bar) {
  background-color: #667eea;
}

.course-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .header-left h2 {
    font-size: 20px;
  }

  .header-right {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .header-right .el-button {
    flex: 1;
    min-width: 0;
  }

  .info-section {
    flex-wrap: wrap;
    gap: 12px 16px;
    margin-bottom: 12px;
  }

  .info-item {
    font-size: 13px;
  }
}
</style>

<!-- 非 scoped 样式：用于控制 teleport 到 body 的 Dialog 移动端样式 -->
<style>
@media (max-width: 768px) {
  .danger-dialog .el-dialog {
    width: calc(100vw - 24px) !important;
    min-width: 0 !important;
    margin: 12px auto !important;
  }
}
</style>
