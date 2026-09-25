<template>
  <div class="class-member-panel">
    <div class="student-header">
      <el-button @click="showInviteDialog = true" class="toolbar-btn">
        <UserPlus :size="18" />
        邀请学生
      </el-button>
      <el-button @click="generateInviteCode" class="toolbar-btn">
        <Key :size="18" />
        查看邀请码
      </el-button>
      <el-button
        v-if="selectedStudentIds.length > 0"
        type="primary"
        @click="batchSetAssistantAction"
        class="toolbar-btn"
      >
        批量设为课代表 ({{ selectedStudentIds.length }})
      </el-button>
      <el-button
        v-if="selectedStudentIds.length > 0"
        type="danger"
        @click="batchKickStudentsAction"
        class="toolbar-btn"
      >
        批量踢出 ({{ selectedStudentIds.length }})
      </el-button>
      <el-button
        v-if="selectedStudentIds.length > 0"
        @click="clearSelection"
        class="toolbar-btn"
      >
        取消选择
      </el-button>
    </div>

    <div v-if="pendingApprovals.length > 0" class="approval-section">
      <h4 class="approval-title">
        <Bell :size="16" />
        学生邀请审核（{{ pendingApprovals.length }}）
      </h4>
      <div v-for="item in pendingApprovals" :key="item.id" class="approval-item">
        <div class="approval-info">
          <div class="approval-icon">
            <UserPlus :size="15" />
          </div>
          <div class="approval-text">
            <p>
              <strong>{{ item.inviteeUsername }}</strong>
              接受了学生邀请，等待你的确认
            </p>
            <span>{{ formatDate(item.createTime) }}</span>
          </div>
        </div>
        <div class="approval-actions">
          <el-button
            type="primary"
            size="small"
            :loading="approvingId === item.id"
            @click="handleApproval(item, true)"
          >
            同意
          </el-button>
          <el-button
            type="danger"
            plain
            size="small"
            :loading="approvingId === item.id"
            @click="handleApproval(item, false)"
          >
            拒绝
          </el-button>
        </div>
      </div>
    </div>

    <div class="student-list">
      <div v-for="member in members" :key="member.id" class="student-item">
        <div class="student-info">
          <el-checkbox
            v-if="member.role !== '创建者' && member.userId !== ownerId"
            v-model="selectedStudentIds"
            :label="member.userId"
            @change="handleStudentSelect"
          />
          <div class="student-icon">
            <User :size="16" />
          </div>
          <div class="student-details">
            <h4>{{ member.studentName || member.userName }}</h4>
            <p v-if="member.studentNo">{{ member.studentNo }}</p>
          </div>
        </div>
        <div class="student-role">
          <span :class="['role-badge', member.roleCode === 1 ? 'teacher' : 'student']">
            {{ member.role }}
          </span>
        </div>
        <div class="student-actions">
          <button
            v-if="member.roleCode === 0 && myRoleCode === 1"
            class="action-btn"
            @click="setAssistant(member.userId)"
          >
            设为课代表
          </button>
          <button
            v-if="member.role === '课代表' && myRole === '创建者'"
            class="action-btn"
            @click="removeAssistant(member.userId)"
          >
            取消课代表
          </button>
          <button
            v-if="member.role !== '创建者' && member.userId !== ownerId"
            class="action-btn warning"
            @click="transferOwnershipAction(member.userId, member.userName)"
          >
            转让班级
          </button>
          <button
            v-if="member.role !== '创建者' && member.userId !== ownerId"
            class="action-btn danger"
            @click="kickStudent(member.userId)"
          >
            踢出
          </button>
        </div>
      </div>
    </div>
    <div v-if="members.length === 0" class="empty-state">
      <Users :size="32" />
      <p>暂无学生</p>
    </div>

    <!-- 邀请学生 -->
    <el-dialog
      v-model="showInviteDialog"
      title="邀请学生"
      width="400px"
      class="dark-dialog invite-student-dialog"
    >
      <el-form :model="inviteForm" label-width="80px">
        <el-form-item label="用户名/邮箱">
          <el-input v-model="inviteForm.userAccount" placeholder="请输入学生的用户名或邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInviteDialog = false">取消</el-button>
        <el-button type="primary" @click="inviteStudent">邀请</el-button>
      </template>
    </el-dialog>

    <!-- 班级邀请码与邀请设置 -->
    <el-dialog
      v-model="showInviteCodeDialog"
      title="班级邀请码"
      width="520px"
      class="dark-dialog invite-code-dialog"
    >
      <div class="invite-code-content">
        <div class="invite-code-box">
          <Copy :size="16" class="copy-icon-decor" />
          <span class="invite-code">{{ inviteCode }}</span>
        </div>
        <div class="invite-code-toolbar">
          <el-button type="primary" @click="copyInviteCode" class="copy-btn">
            <Copy :size="14" /> 复制邀请码
          </el-button>
          <el-button
            type="danger"
            plain
            @click="resetInviteCode"
            :loading="resettingCode"
            class="reset-btn"
          >
            <RefreshCw :size="14" /> 重置邀请码
          </el-button>
        </div>
        <p class="invite-tip">学生可使用此邀请码直接加入课程</p>
        <p class="invite-warning">重置后旧邀请码立即失效，需重新分享新码</p>
        <el-divider class="invite-divider" />
        <div class="invite-setting-row">
          <div class="invite-setting-text">
            <span class="invite-setting-title">允许学生邀请同学加入</span>
            <span class="invite-setting-desc">
              开启后班级学生可发起邀请，经被邀请人确认和你审核后加入
            </span>
          </div>
          <el-switch
            v-model="allowStudentInvite"
            :loading="savingInviteSetting"
            @change="onAllowStudentInviteChange"
          />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Bell, Copy, Key, RefreshCw, User, UserPlus, Users } from "@lucide/vue";
import { del, get, post, put } from "@/utils/http";
import { confirmDangerousOperation } from "@/composables/useSensitiveVerification";
import { formatDateTime as formatDate } from "@/utils/format";
import type { ClassMemberInfo, TeacherApprovalInfo } from "@/types/class";

const props = defineProps<{
  /** 班级 ID */
  classId: number;
  /** 班级创建者的用户 ID */
  ownerId: number;
  /** 我在该班级的角色名称 */
  myRole: string;
  /** 我在该班级的角色代码：1-拥有班级管理员权限 */
  myRoleCode: number;
  /** 班级名称，用于转让提示 */
  className: string;
  /** 班级是否允许学生邀请同学 */
  allowStudentInvite: boolean;
}>();

const emit = defineEmits<{
  /** 待审核数量变化，用于父页面 tab 徽标 */
  (e: "pending-count-change", count: number): void;
  /** 成员变化（含教师人数），让父页面刷新课程统计 */
  (e: "members-change", teacherCount: number | null): void;
  /** 班级所有权发生转移，父页面需重新加载课程信息 */
  (e: "ownership-transferred"): void;
}>();

const members = ref<ClassMemberInfo[]>([]);
const selectedStudentIds = ref<number[]>([]);
const pendingApprovals = ref<TeacherApprovalInfo[]>([]);
const approvingId = ref<number | null>(null);

const showInviteDialog = ref(false);
const inviteForm = ref({ userAccount: "" });

const showInviteCodeDialog = ref(false);
const inviteCode = ref("");
const resettingCode = ref(false);

const allowStudentInvite = ref(props.allowStudentInvite);
const savingInviteSetting = ref(false);

// 父页面重新加载课程信息后同步该开关
watch(
  () => props.allowStudentInvite,
  (val) => {
    allowStudentInvite.value = val;
  },
);

const loadMembers = async () => {
  const result = await get<{ records: ClassMemberInfo[] }>(
    `/class/${props.classId}/members`,
    { pageSize: 300 },
  );
  if (result.code === 200 && result.data) {
    members.value = result.data.records;
    emit("members-change", result.data.records.length > 0 ? result.data.records[0].teacherCount : 0);
  }
};

const loadPendingApprovals = async () => {
  const result = await get<TeacherApprovalInfo[]>(`/class/${props.classId}/invitations/pending`);
  if (result.code === 200) {
    pendingApprovals.value = result.data || [];
    emit("pending-count-change", pendingApprovals.value.length);
  }
};

const handleApproval = async (item: TeacherApprovalInfo, approved: boolean) => {
  approvingId.value = item.id;
  const result = await put(`/class/invitations/${item.id}/approval`, {
    applicationId: item.id,
    approved,
  });
  approvingId.value = null;
  if (result.code === 200) {
    ElMessage.success(approved ? "已同意，学生已加入班级" : "已拒绝该申请");
    loadPendingApprovals();
    loadMembers();
  } else {
    ElMessage.error(result.message || "操作失败");
  }
};

const handleStudentSelect = () => {
  selectedStudentIds.value = selectedStudentIds.value.filter((id) => {
    const member = members.value.find((m) => m.userId === id);
    return member && member.role !== "创建者" && member.userId !== props.ownerId;
  });
};

const clearSelection = () => {
  selectedStudentIds.value = [];
};

const setAssistant = async (userId: number) => {
  const result = await put(`/class/${props.classId}/assistants/batch`, {
    studentUserIds: [userId],
  });
  if (result.code === 200) {
    ElMessage.success("已设置为课代表");
    loadMembers();
  } else {
    ElMessage.error(result.message);
  }
};

const removeAssistant = async (userId: number) => {
  const result = await del(`/class/${props.classId}/assistants/${userId}`);
  if (result.code === 200) {
    ElMessage.success("已取消课代表");
    loadMembers();
  } else {
    ElMessage.error(result.message);
  }
};

const batchSetAssistantAction = async () => {
  if (selectedStudentIds.value.length === 0) {
    ElMessage.warning("请选择要设为课代表的成员");
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认将 ${selectedStudentIds.value.length} 名成员批量设为课代表？`,
      "批量设为课代表",
      { confirmButtonText: "确认", cancelButtonText: "取消" },
    );
    const result = await put(`/class/${props.classId}/assistants/batch`, {
      studentUserIds: selectedStudentIds.value,
    });
    if (result.code === 200) {
      ElMessage.success(`已将 ${selectedStudentIds.value.length} 名成员设为课代表`);
      selectedStudentIds.value = [];
      loadMembers();
    } else {
      ElMessage.error(result.message);
    }
  } catch {
    // 用户取消
  }
};

const kickStudent = async (userId: number) => {
  // 踢出成员属高危操作：红色警示框 + 身份二次验证
  const headers = await confirmDangerousOperation({
    title: "踢出成员",
    message: "确认将该成员踢出班级？其在本班的作业提交将被清理，此操作不可恢复。",
    confirmText: "确认踢出",
    operationName: "踢出成员",
  });
  if (!headers) return;
  const result = await del(
    `/class/${props.classId}/members/batch`,
    { studentUserIds: [userId] },
    undefined,
    headers,
  );
  if (result.code === 200) {
    ElMessage.success("已踢出");
    loadMembers();
  } else {
    ElMessage.error(result.message);
  }
};

const batchKickStudentsAction = async () => {
  if (selectedStudentIds.value.length === 0) {
    ElMessage.warning("请选择要踢出的学生");
    return;
  }
  // 踢出成员属高危操作：红色警示框 + 身份二次验证
  const headers = await confirmDangerousOperation({
    title: "批量踢出成员",
    message: `确认批量踢出 ${selectedStudentIds.value.length} 名成员？其在本班的作业提交将被清理，此操作不可恢复。`,
    confirmText: "确认踢出",
    operationName: "批量踢出成员",
  });
  if (!headers) return;
  const result = await del(
    `/class/${props.classId}/members/batch`,
    { studentUserIds: selectedStudentIds.value },
    undefined,
    headers,
  );
  if (result.code === 200) {
    ElMessage.success(`已踢出 ${selectedStudentIds.value.length} 名成员`);
    selectedStudentIds.value = [];
    loadMembers();
  } else {
    ElMessage.error(result.message);
  }
};

const transferOwnershipAction = async (userId: number, userName: string) => {
  try {
    await ElMessageBox.confirm(
      `确定将班级"${props.className}"转让给 ${userName} 吗？转让后你将变成该班级的课代表，且无法撤销。`,
      "转让班级",
      {
        confirmButtonText: "确认转让",
        cancelButtonText: "取消",
        type: "warning",
        customClass: "danger-warning-message-box",
      },
    );
  } catch {
    return;
  }
  const result = await put(`/class/${props.classId}/owner`, { newOwnerId: userId });
  if (result.code === 200) {
    ElMessage.success(result.message || "班级所有权转让成功");
    emit("ownership-transferred");
    loadMembers();
  } else {
    ElMessage.error(result.message || "转让失败");
  }
};

const inviteStudent = async () => {
  if (!inviteForm.value.userAccount) {
    ElMessage.warning("请输入学生的用户名或邮箱");
    return;
  }
  const result = await post(`/class/${props.classId}/invitations/teacher`, {
    userAccount: inviteForm.value.userAccount,
  });
  if (result.code === 200) {
    ElMessage.success("邀请已发送");
    showInviteDialog.value = false;
    inviteForm.value.userAccount = "";
  } else {
    ElMessage.error(result.message);
  }
};

const generateInviteCode = async () => {
  const result = await get<string>(`/class/${props.classId}/invite-code`);
  if (result.code === 200) {
    inviteCode.value = result.data!;
    showInviteCodeDialog.value = true;
  } else {
    ElMessage.error(result.message);
  }
};

const copyInviteCode = () => {
  navigator.clipboard.writeText(inviteCode.value);
  ElMessage.success("已复制到剪贴板");
};

const resetInviteCode = async () => {
  try {
    await ElMessageBox.confirm(
      "重置后旧邀请码将立即失效，学生需使用新邀请码加入课程。确定重置？",
      "重置邀请码",
      { type: "warning", confirmButtonText: "重置", cancelButtonText: "取消" },
    );
  } catch {
    return;
  }
  resettingCode.value = true;
  try {
    const result = await post<string>(`/class/${props.classId}/invite-code/reset`);
    if (result.code === 200) {
      inviteCode.value = result.data!;
      ElMessage.success("邀请码已重置，旧码已失效");
    } else {
      ElMessage.error(result.message);
    }
  } catch {
    ElMessage.error("重置失败");
  } finally {
    resettingCode.value = false;
  }
};

const onAllowStudentInviteChange = async (val: boolean | string | number) => {
  savingInviteSetting.value = true;
  const result = await put(`/class/${props.classId}/invite-settings`, {
    allowStudentInvite: Boolean(val),
  });
  savingInviteSetting.value = false;
  if (result.code === 200) {
    ElMessage.success(result.message || "邀请设置已更新");
  } else {
    allowStudentInvite.value = !Boolean(val);
    ElMessage.error(result.message || "邀请设置更新失败");
  }
};

/** 供父页面切换到该页签时刷新 */
const reload = () => {
  loadPendingApprovals();
  loadMembers();
};

defineExpose({ reload });

onMounted(reload);
</script>

<style scoped>
.student-header {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.student-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.student-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 12px;
  padding: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.student-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 180px;
}

.student-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(102, 126, 234, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
}

.student-details h4 {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 2px;
}

.student-details p {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.student-role {
  display: flex;
  align-items: center;
  width: 80px;
  flex-shrink: 0;
}

.student-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.role-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.role-badge.teacher {
  background: rgba(156, 163, 175, 0.2);
  color: #9ca3af;
}

.role-badge.student {
  background: rgba(102, 126, 234, 0.2);
  color: #667eea;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.05);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 8px;
  color: var(--fg);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.action-btn:hover {
  background: rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.3);
}

.action-btn.danger:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.3);
}

.action-btn.warning:hover {
  background: rgba(230, 162, 60, 0.2);
  border-color: rgba(230, 162, 60, 0.3);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

/* ========== 学生邀请审核区块 ========== */
.approval-section {
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 10px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04);
}

.approval-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.92);
}

.approval-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.05);
}

.approval-item + .approval-item {
  margin-top: 8px;
}

.approval-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.approval-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.85), rgba(118, 75, 162, 0.85));
  color: var(--fg-on-accent);
}

.approval-text {
  min-width: 0;
}

.approval-text p {
  margin: 0;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-text p strong {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 1);
}

.approval-text span {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.approval-actions {
  display: flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
}

/* ========== 工具栏按钮深色适配 ========== */
.toolbar-btn {
  display: inline-flex !important;
  align-items: center !important;
  gap: 6px !important;
  padding: 10px 16px !important;
  height: auto !important;
  font-weight: 500 !important;
  font-size: 14px !important;
  border-radius: 10px !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85) !important;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06) !important;
  border: 1.5px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.18) !important;
  transition: all 0.2s ease !important;
}

.toolbar-btn:hover {
  color: #667eea !important;
  border-color: rgba(102, 126, 234, 0.55) !important;
  background: rgba(102, 126, 234, 0.12) !important;
}

.toolbar-btn.el-button--danger {
  color: var(--danger-title) !important;
  background: rgba(239, 68, 68, 0.1) !important;
  border-color: rgba(239, 68, 68, 0.35) !important;
}

.toolbar-btn.el-button--danger:hover {
  color: var(--fg) !important;
  background: rgba(239, 68, 68, 0.28) !important;
  border-color: rgba(239, 68, 68, 0.6) !important;
}

/* ========== 邀请码 ========== */
.invite-code-content {
  text-align: center;
}

.invite-code-box {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  padding: 20px 24px;
  margin: 12px 0 8px;
  background: rgba(102, 126, 234, 0.08);
  border: 1.5px solid rgba(102, 126, 234, 0.28);
  border-radius: 14px;
  min-width: 0;
  width: 100%;
  box-sizing: border-box;
}

.invite-code-box .copy-icon-decor {
  flex-shrink: 0;
  color: rgba(102, 126, 234, 0.6);
}

.invite-code {
  flex: 1 1 auto;
  min-width: 0;
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(90deg, #667eea, #a78bfa);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 1.5px;
  word-break: break-all;
  text-align: left;
  padding: 0 4px;
  user-select: all;
}

.invite-code-toolbar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin: 12px 0 16px;
  flex-wrap: wrap;
}

.copy-btn {
  padding: 10px 22px !important;
  border-radius: 10px !important;
  font-weight: 600 !important;
  font-size: 14px !important;
  color: var(--fg) !important;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  border: none !important;
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.35) !important;
  transition: all 0.2s ease !important;
}

.copy-btn:hover {
  transform: translateY(-1px) !important;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5) !important;
  filter: brightness(1.08) !important;
}

.copy-btn:active {
  transform: translateY(0) !important;
}

.reset-btn {
  padding: 10px 22px !important;
  border-radius: 10px !important;
  font-weight: 600 !important;
  font-size: 14px !important;
  color: var(--danger-strong) !important;
  background: rgba(239, 68, 68, 0.12) !important;
  border: 1.5px solid rgba(239, 68, 68, 0.35) !important;
  transition: all 0.2s ease !important;
}

.reset-btn:hover {
  color: var(--fg) !important;
  background: rgba(239, 68, 68, 0.28) !important;
  border-color: rgba(239, 68, 68, 0.6) !important;
}

.invite-tip {
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
  margin-bottom: 4px;
}

.invite-warning {
  margin-top: 10px;
  font-size: 12px;
  color: rgba(239, 68, 68, 0.7);
}

.invite-divider {
  margin: 16px 0 14px;
}

.invite-setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.invite-setting-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
}

.invite-setting-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.92);
}

.invite-setting-desc {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.55);
  line-height: 1.5;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #667eea;
  border-color: #667eea;
}

@media (max-width: 768px) {
  /* 工具栏按钮改为多行排布：批量操作出现时按钮总量翻倍，单行放不下 */
  .student-header {
    flex-wrap: wrap;
  }

  .student-header .toolbar-btn {
    margin-left: 0;
    justify-content: center;
  }

  .student-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 560px) {
  .invite-code-dialog :deep(.el-dialog) {
    width: 92vw !important;
    min-width: 0 !important;
    margin: 5vh auto !important;
  }

  .invite-code-box {
    flex-wrap: wrap;
    padding: 14px 16px;
    gap: 10px;
  }

  .invite-code-box > .copy-icon-decor {
    display: none;
  }

  .invite-code {
    font-size: 16px;
    letter-spacing: 1px;
    padding: 0;
    text-align: center;
    width: 100%;
  }

  .copy-btn {
    width: 100% !important;
  }
}
</style>
