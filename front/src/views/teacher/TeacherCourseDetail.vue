<template>
  <div class="teacher-course-detail">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" class="page-back-btn" text>
          <ArrowLeft :size="18" />
        </el-button>
        <h2>{{ course?.className }}</h2>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="showCreateWorkDialog = true">
          <Plus :size="18" />
          发布作业
        </el-button>
        <el-button
          v-if="canDissolve"
          type="danger"
          @click="dissolveClassAction"
        >
          <Trash2 :size="18" />
          解散课堂
        </el-button>
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

    <el-tabs v-model="activeTab" class="course-tabs">
      <el-tab-pane label="作业管理" name="works">
        <div class="work-list">
          <div v-for="work in works" :key="work.id" class="work-item">
            <div class="work-header">
              <div class="title-row">
                <h4>{{ work.title }}</h4>
                <span v-if="work.isPinned" class="pin-badge">
                  <Star :size="14" />
                </span>
              </div>
              <span :class="['status-tag', getWorkStatus(work)]">
                {{ getWorkStatusText(work) }}
              </span>
            </div>
            <div class="work-info">
              <div class="info-item">
                <Clock :size="14" />
                <span>截止：{{ formatDate(work.deadline) }}</span>
              </div>
              <div class="info-item">
                <FileText :size="14" />
                <span>总分：{{ work.totalScore }}分</span>
              </div>
              <div class="info-item">
                <Users :size="14" />
                <span>已交：{{ work.submittedCount || 0 }}人</span>
              </div>
            </div>
            <div class="work-actions">
              <button class="action-btn" @click="viewSubmissions(work.id)">
                <Eye :size="14" />
                查看提交
              </button>
              <button class="action-btn" @click="editWork(work.id)">
                <Edit3 :size="14" />
                编辑
              </button>
              <button class="action-btn" @click="togglePin(work)">
                <Star :size="14" />
                {{ work.isPinned ? "取消置顶" : "置顶" }}
              </button>
              <button class="action-btn danger" @click="deleteWork(work.id)">
                <Trash2 :size="14" />
                删除
              </button>
            </div>
          </div>
        </div>
        <div v-if="works.length === 0" class="empty-state">
          <FileText :size="32" />
          <p>暂无作业</p>
          <p class="empty-tip">点击"发布作业"按钮创建作业</p>
        </div>
      </el-tab-pane>

      <el-tab-pane label="学生管理" name="students">
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
        <div class="student-list">
          <div v-for="member in members" :key="member.id" class="student-item">
            <div class="student-info">
              <el-checkbox
                v-if="member.role === '学生'"
                v-model="selectedStudentIds"
                :label="member.userId"
                @change="handleStudentSelect"
              />
              <div class="student-icon">
                <User :size="16" />
              </div>
              <div class="student-details">
                <h4>{{ member.userName }}</h4>
                <p>{{ member.userNo }}</p>
              </div>
            </div>
            <div class="student-role">
              <span
                :class="[
                  'role-badge',
                  member.role === '创建者' || member.role === '老师'
                    ? 'teacher'
                    : 'student',
                ]"
              >
                {{ member.role }}
              </span>
            </div>
            <div class="student-actions">
              <button
                v-if="
                  member.role === '学生' &&
                  (course?.userRole === '创建者' || course?.userRole === '老师')
                "
                class="action-btn"
                @click="setAssistant(member.userId)"
              >
                设为老师
              </button>
              <button
                v-if="member.role === '老师' && course?.userRole === '创建者'"
                class="action-btn"
                @click="removeAssistant(member.userId)"
              >
                取消老师
              </button>
              <button
                v-if="member.role === '学生'"
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
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="showCreateWorkDialog"
      title="发布作业"
      width="600px"
      class="dark-dialog create-work-dialog"
      @close="resetCreateWorkForm"
    >
      <el-form :model="workForm" label-width="80px">
        <el-form-item label="作业标题">
          <el-input
            v-model="workForm.title"
            placeholder="请输入作业标题"
            maxlength="128"
          />
        </el-form-item>
        <el-form-item label="作业描述">
          <el-input
            v-model="workForm.description"
            type="textarea"
            placeholder="请输入作业描述"
            :rows="4"
          />
        </el-form-item>
        <el-form-item label="截止时间">
          <div class="deadline-split-wrap">
            <el-date-picker
              v-model="workFormDate"
              type="date"
              placeholder="选择日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              popper-class="dark-picker"
            />
            <el-time-picker
              v-model="workFormTime"
              placeholder="选择时间"
              format="HH:mm:ss"
              value-format="HH:mm:ss"
              popper-class="dark-picker"
            />
          </div>
        </el-form-item>
        <el-form-item label="作业总分">
          <el-input-number
            v-model="workForm.totalScore"
            :min="1"
            :max="1000"
            :step="1"
            class="total-score-input"
          />
        </el-form-item>
        <el-form-item label="作业附件">
          <el-upload
            v-model:file-list="attachmentFiles"
            :auto-upload="false"
            multiple
            :on-exceed="handleAttachmentExceed"
            :on-remove="handleAttachmentRemove"
            :on-change="handleAttachmentChange"
          >
            <el-button
              type="primary"
              plain
              size="default"
              class="upload-trigger-btn"
            >
              <Upload :size="16" />
              <span>选择文件</span>
            </el-button>
            <template #tip>
              <div class="upload-tip">
                <Paperclip :size="12" />
                <span>支持多文件上传，单个文件不超过 50MB</span>
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="允许逾期">
          <el-switch v-model="workForm.allowLateSubmit" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateWorkDialog = false">取消</el-button>
        <el-button type="primary" @click="createWork" :loading="workSubmitting"
          >发布</el-button
        >
      </template>
    </el-dialog>

    <el-dialog
      v-model="showInviteDialog"
      title="邀请学生"
      width="400px"
      class="dark-dialog invite-student-dialog"
    >
      <el-form :model="inviteForm" label-width="80px">
        <el-form-item label="学生账号">
          <el-input
            v-model="inviteForm.userAccount"
            placeholder="请输入学生学号/工号或邮箱"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInviteDialog = false">取消</el-button>
        <el-button type="primary" @click="inviteStudent">邀请</el-button>
      </template>
    </el-dialog>

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
      </div>
    </el-dialog>

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
          <el-button
            type="primary"
            plain
            size="default"
            @click="copyExpectedText"
          >
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
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { UploadUserFile } from "element-plus";
import { get, post, postForm, put, del, patch } from "@/utils/http";
import { ElMessage, ElMessageBox } from "element-plus";
import { useUserStore } from "@/stores/user";
import {
  ArrowLeft,
  User,
  Users,
  Calendar,
  Clock,
  FileText,
  Star,
  Plus,
  Eye,
  Edit3,
  Trash2,
  UserPlus,
  UserCheck,
  Key,
  Paperclip,
  Upload,
  Copy,
  RefreshCw,
  AlertTriangle,
  ShieldAlert,
} from "@lucide/vue";

interface CourseInfo {
  id: number;
  className: string;
  description?: string;
  ownerId: number;
  ownerName: string;
  userRole: string;
  memberCount: number;
  teacherCount: number;
  studentCount: number;
}

interface WorkInfo {
  id: number;
  title: string;
  description: string;
  classId: number;
  className: string;
  deadline: string;
  totalScore: number;
  isPinned: boolean;
  status: number;
  publishTime: string;
  submittedCount?: number;
}

interface MemberInfo {
  id: number;
  userId: number;
  userName: string;
  userNo: string;
  role: string;
  joinTime: string;
  teacherCount: number;
}

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const course = ref<CourseInfo | null>(null);
const works = ref<WorkInfo[]>([]);
const members = ref<MemberInfo[]>([]);
const memberTeacherCount = ref<number | null>(null);
const activeTab = ref("works");
const selectedStudentIds = ref<number[]>([]);

const canDissolve = computed(() => {
  if (!course.value || !userStore.userInfo) return false;
  const isOwner = course.value.userRole === "创建者";
  const isAdmin = userStore.userInfo.permission >= 100;
  return isOwner || isAdmin;
});

const showCreateWorkDialog = ref(false);
const workSubmitting = ref(false);
const showInviteDialog = ref(false);
const showInviteCodeDialog = ref(false);
const inviteCode = ref("");

const workForm = ref({
  title: "",
  description: "",
  deadline: "",
  totalScore: 100,
  allowLateSubmit: true,
  classId: Number(route.params.id),
});

const workFormDate = ref("");
const workFormTime = ref("");

const buildDeadline = (): string => {
  if (workFormDate.value && workFormTime.value) {
    return `${workFormDate.value}T${workFormTime.value}`;
  }
  if (workFormDate.value) {
    return `${workFormDate.value}T23:59:59`;
  }
  return "";
};

const attachmentFiles = ref<UploadUserFile[]>([]);

const MAX_ATTACHMENT_SIZE = 50 * 1024 * 1024;

const handleAttachmentExceed = () => {
  ElMessage.warning("单次最多上传 20 个文件");
};

const handleAttachmentRemove = (
  _file: UploadUserFile,
  uploadFiles: UploadUserFile[],
) => {
  attachmentFiles.value = uploadFiles;
};

const handleAttachmentChange = (
  file: UploadUserFile,
  uploadFiles: UploadUserFile[],
) => {
  if (file.size && file.size > MAX_ATTACHMENT_SIZE) {
    ElMessage.warning(`文件「${file.name}」超过 50MB，已自动跳过`);
    const idx = attachmentFiles.value.findIndex((f) => f.uid === file.uid);
    if (idx > -1) attachmentFiles.value.splice(idx, 1);
    return;
  }
  attachmentFiles.value = uploadFiles.filter(
    (f) => !f.size || f.size <= MAX_ATTACHMENT_SIZE,
  );
};

const resetCreateWorkForm = () => {
  workForm.value = {
    title: "",
    description: "",
    deadline: "",
    totalScore: 100,
    allowLateSubmit: true,
    classId: Number(route.params.id),
  };
  workFormDate.value = "";
  workFormTime.value = "";
  attachmentFiles.value = [];
};

const inviteForm = ref({
  userAccount: "",
});

const loadCourse = async () => {
  const result = await get<CourseInfo>(`/class/${route.params.id}`);
  if (result.code === 200) {
    course.value = result.data!;
  }
};

const loadWorks = async () => {
  const result = await get<{ records: WorkInfo[] }>("/works");
  if (result.code === 200) {
    const classId = Number(route.params.id);
    works.value = result.data!.records.filter(
      (work) => work.classId === classId,
    );
  }
};

const loadMembers = async () => {
  const result = await get<{ records: MemberInfo[] }>(
    `/class/${route.params.id}/members`,
  );
  if (result.code === 200) {
    members.value = result.data!.records;
    if (result.data!.records.length > 0) {
      memberTeacherCount.value = result.data!.records[0].teacherCount;
    } else {
      memberTeacherCount.value = 0;
    }
  }
};

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
};

const getWorkStatus = (work: WorkInfo) => {
  const now = new Date();
  const deadline = new Date(work.deadline);
  if (now > deadline) return "expired";
  if (work.status === 1) return "active";
  return "pending";
};

const getWorkStatusText = (work: WorkInfo) => {
  const status = getWorkStatus(work);
  const map: Record<string, string> = {
    active: "进行中",
    expired: "已截止",
    pending: "未发布",
  };
  return map[status] || status;
};

const goBack = () => {
  router.push("/teacher/courses");
};

const viewSubmissions = (workId: number) => {
  router.push(`/teacher/work/${workId}/submissions`);
};

const editWork = (workId: number) => {
  router.push(`/teacher/work/${workId}/edit`);
};

const togglePin = async (work: WorkInfo) => {
  const result = await patch(`/works/${work.id}/pin`, {
    workId: work.id,
    isPinned: !work.isPinned,
  });
  if (result.code === 200) {
    ElMessage.success(work.isPinned ? "已取消置顶" : "已置顶");
    loadWorks();
  } else {
    ElMessage.error(result.message);
  }
};

const deleteWork = async (workId: number) => {
  try {
    await ElMessageBox.confirm("确认删除此作业？", "提示", {
      confirmButtonText: "确认",
      cancelButtonText: "取消",
    });
    const result = await del(`/works/${workId}`);
    if (result.code === 200) {
      ElMessage.success("删除成功");
      loadWorks();
    } else {
      ElMessage.error(result.message);
    }
  } catch {
    // 用户取消
  }
};

const createWork = async () => {
  if (!workForm.value.title) {
    ElMessage.warning("请输入作业标题");
    return;
  }
  if (!workForm.value.description) {
    ElMessage.warning("请输入作业描述");
    return;
  }
  const deadline = buildDeadline();
  if (!deadline) {
    ElMessage.warning("请选择截止时间");
    return;
  }
  workSubmitting.value = true;
  try {
    workForm.value.deadline = deadline;
    const formData = new FormData();
    formData.append("title", workForm.value.title);
    formData.append("description", workForm.value.description);
    formData.append("deadline", workForm.value.deadline);
    formData.append("totalScore", String(workForm.value.totalScore));
    formData.append("allowLateSubmit", String(workForm.value.allowLateSubmit));
    formData.append("classId", String(workForm.value.classId));
    if (attachmentFiles.value && attachmentFiles.value.length > 0) {
      for (const fileItem of attachmentFiles.value) {
        if (fileItem.raw) {
          formData.append("attachments", fileItem.raw);
        }
      }
    }
    const result = await postForm("/works", formData);
    if (result.code === 200) {
      ElMessage.success("作业发布成功");
      showCreateWorkDialog.value = false;
      resetCreateWorkForm();
      loadWorks();
    } else {
      ElMessage.error(result.message);
    }
  } catch {
    ElMessage.error("发布失败，请重试");
  } finally {
    workSubmitting.value = false;
  }
};

const generateInviteCode = async () => {
  const result = await get<string>(`/class/${route.params.id}/invite-code`);
  if (result.code === 200) {
    inviteCode.value = result.data!;
    showInviteCodeDialog.value = true;
  } else {
    ElMessage.error(result.message);
  }
};

const resettingCode = ref(false);

// ========== 危险操作（解散课堂）三步弹窗状态 ==========
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
  return (u.userNo || u.username || u.email || "-") as string;
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
    const result = await del(`/class/${route.params.id}`, undefined, params);
    if (result.code === 200) {
      ElMessage.success("课堂已解散");
      clearDangerInputs();
      router.push("/teacher/courses");
    } else {
      ElMessage.error(result.message || "解散失败");
    }
  } catch (e) {
    ElMessage.error("解散失败，请重试");
  } finally {
    dangerSubmitting.value = false;
  }
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
    const result = await post<string>(
      `/class/${route.params.id}/invite-code/reset`,
    );
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

const copyInviteCode = () => {
  navigator.clipboard.writeText(inviteCode.value);
  ElMessage.success("已复制到剪贴板");
};

const inviteStudent = async () => {
  if (!inviteForm.value.userAccount) {
    ElMessage.warning("请输入学生账号");
    return;
  }
  const result = await post(`/class/${route.params.id}/invitations/teacher`, {
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

const setAssistant = async (userId: number) => {
  const result = await put(`/class/${route.params.id}/assistants`, {
    studentUserId: userId,
  });
  if (result.code === 200) {
    ElMessage.success("已设置为班级助理");
    loadMembers();
  } else {
    ElMessage.error(result.message);
  }
};

const removeAssistant = async (userId: number) => {
  const result = await del(`/class/${route.params.id}/assistants/${userId}`);
  if (result.code === 200) {
    ElMessage.success("已取消班级助理");
    loadMembers();
  } else {
    ElMessage.error(result.message);
  }
};

const kickStudent = async (userId: number) => {
  try {
    await ElMessageBox.confirm("确认踢出此学生？", "提示", {
      confirmButtonText: "确认",
      cancelButtonText: "取消",
    });
    const result = await del(`/class/${route.params.id}/members/${userId}`);
    if (result.code === 200) {
      ElMessage.success("已踢出");
      loadMembers();
    } else {
      ElMessage.error(result.message);
    }
  } catch {
    // 用户取消
  }
};

const handleStudentSelect = () => {
  selectedStudentIds.value = selectedStudentIds.value.filter((id) => {
    const member = members.value.find((m) => m.userId === id);
    return member && member.role === "学生";
  });
};

const clearSelection = () => {
  selectedStudentIds.value = [];
};

const batchKickStudentsAction = async () => {
  if (selectedStudentIds.value.length === 0) {
    ElMessage.warning("请选择要踢出的学生");
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认批量踢出 ${selectedStudentIds.value.length} 名学生？`,
      "提示",
      { confirmButtonText: "确认", cancelButtonText: "取消" },
    );
    const result = await del(
      `/class/${route.params.id}/members/batch`,
      selectedStudentIds.value,
    );
    if (result.code === 200) {
      ElMessage.success(`已踢出 ${selectedStudentIds.value.length} 名学生`);
      selectedStudentIds.value = [];
      loadMembers();
    } else {
      ElMessage.error(result.message);
    }
  } catch {
    // 用户取消
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
    // 进入Step2
    showDangerConfirmTextDialog.value = true;
  } catch {
    // 用户取消 Step1
    clearDangerInputs();
  }
};

onMounted(async () => {
  await loadCourse();
  await loadWorks();
  await loadMembers();
});
</script>

<style scoped>
.teacher-course-detail {
  padding-bottom: 24px;
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

.role-badge.teacher {
  background: rgba(156, 163, 175, 0.2);
  color: #9ca3af;
  padding: 2px 8px;
  border-radius: 4px;
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

.work-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.work-item {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 12px;
  padding: 16px;
}

.work-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-row h4 {
  font-size: 15px;
  font-weight: 600;
}

.pin-badge {
  color: #fbbf24;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.active {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.status-tag.expired {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.status-tag.pending {
  background: rgba(156, 163, 175, 0.2);
  color: #9ca3af;
}

.work-info {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.work-info .info-item {
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.work-actions {
  display: flex;
  gap: 10px;
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
}

.student-info {
  display: flex;
  align-items: center;
  gap: 12px;
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
}

.student-actions {
  display: flex;
  gap: 10px;
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

.empty-tip {
  font-size: 12px;
  margin-top: 8px;
}

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
  color: var(--fg);
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

/* 复制邀请码按钮：白底紫字，高对比度 */
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

/* 重置邀请码按钮：淡红色底色，红边红字 */
.reset-btn {
  padding: 10px 22px !important;
  border-radius: 10px !important;
  font-weight: 600 !important;
  font-size: 14px !important;
  color: #fca5a5 !important;
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

/* ========== 学生管理工具栏按钮深色适配 ========== */
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

/* danger 类型按钮走红色路线，覆盖上面紫色 */
.toolbar-btn.el-button--danger {
  color: #fecaca !important;
  background: rgba(239, 68, 68, 0.1) !important;
  border-color: rgba(239, 68, 68, 0.35) !important;
}

.toolbar-btn.el-button--danger:hover {
  color: var(--fg) !important;
  background: rgba(239, 68, 68, 0.28) !important;
  border-color: rgba(239, 68, 68, 0.6) !important;
}

/* 手机端适配 */
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

/* 手机端适配（768px 以下） */
@media (max-width: 768px) {
  /* 页面头部：纵向堆叠 */
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

  /* 课程信息卡片：信息项换行 */
  .info-section {
    flex-wrap: wrap;
    gap: 12px 16px;
    margin-bottom: 12px;
  }

  .info-item {
    font-size: 13px;
  }

  /* 作业列表项 */
  .work-item {
    padding: 14px;
  }

  .work-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .title-row h4 {
    font-size: 14px;
  }

  .work-info {
    flex-wrap: wrap;
    gap: 8px 14px;
  }

  .work-info .info-item {
    font-size: 12px;
  }

  /* 作业操作按钮：2 列网格 */
  .work-actions {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .action-btn {
    justify-content: center;
    padding: 8px 10px;
    font-size: 13px;
  }

  /* 邀请学生弹窗：全宽 */
  .invite-dialog :deep(.el-dialog),
  .invite-student-dialog :deep(.el-dialog) {
    width: calc(100vw - 24px) !important;
    min-width: 0 !important;
    margin: 12px auto !important;
  }
}

.course-tabs :deep(.el-checkbox__label) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.8);
}

.course-tabs :deep(.el-checkbox__inner) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06);
  border-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.25);
}

.course-tabs :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #667eea;
  border-color: #667eea;
}

.deadline-split-wrap {
  display: inline-flex !important;
  align-items: center !important;
  gap: 8px !important;
  flex-wrap: nowrap !important;
  width: 100% !important;
}

.deadline-split-wrap :deep(.el-date-editor),
.deadline-split-wrap :deep(.el-time-editor) {
  flex: 1 1 0 !important;
  min-width: 0 !important;
  width: 100% !important;
}

.deadline-split-wrap :deep(.el-input__wrapper) {
  width: 100% !important;
  min-width: 0 !important;
}
</style>

<!-- 非 scoped 样式：用于控制 teleport 到 body 的 Dialog 移动端样式 -->
<style>
@media (max-width: 768px) {
  .create-work-dialog .el-dialog {
    width: calc(100vw - 24px) !important;
    min-width: 0 !important;
    margin: 12px auto !important;
  }

  .create-work-dialog .el-dialog__body {
    padding: 16px 14px !important;
  }

  .create-work-dialog .el-form-item {
    display: flex !important;
    flex-direction: column !important;
    align-items: stretch !important;
    margin-bottom: 18px !important;
  }

  .create-work-dialog .el-form-item__label {
    float: none !important;
    display: block !important;
    width: auto !important;
    height: auto !important;
    line-height: 1.5 !important;
    padding: 0 0 6px 0 !important;
    text-align: left !important;
    color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85) !important;
    font-size: 14px !important;
  }

  .create-work-dialog .el-form-item__content {
    display: block !important;
    margin-left: 0 !important;
    line-height: normal !important;
  }

  .create-work-dialog .el-input__wrapper,
  .create-work-dialog .el-textarea__inner,
  .create-work-dialog .el-input-number,
  .create-work-dialog .el-date-editor,
  .create-work-dialog .el-time-editor {
    width: 100% !important;
  }

  .create-work-dialog .el-input__wrapper {
    min-height: 40px;
  }

  .create-work-dialog .el-textarea__inner {
    min-height: 96px;
    font-size: 15px;
  }

  .create-work-dialog .el-input-number {
    width: 100% !important;
  }

  .create-work-dialog .deadline-split-wrap {
    display: flex !important;
    flex-direction: column !important;
    align-items: stretch !important;
    gap: 10px !important;
    width: 100% !important;
  }

  .create-work-dialog .deadline-split-wrap .el-date-editor,
  .create-work-dialog .deadline-split-wrap .el-time-editor {
    flex: none !important;
    width: 100% !important;
    margin-right: 0 !important;
  }

  .create-work-dialog .total-score-input {
    width: 100%;
  }

  .create-work-dialog .el-upload {
    width: 100%;
  }

  .create-work-dialog .upload-trigger-btn {
    width: 100%;
    min-height: 40px;
    justify-content: center;
  }

  .create-work-dialog .el-upload-list {
    margin-top: 10px;
  }

  .create-work-dialog .el-dialog__footer {
    display: flex;
    gap: 10px;
    padding: 12px 14px 16px;
  }

  .create-work-dialog .el-dialog__footer .el-button {
    flex: 1;
    min-height: 42px;
    font-size: 15px;
  }
}
</style>
