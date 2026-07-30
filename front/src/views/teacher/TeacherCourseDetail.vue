<template>
  <div class="teacher-course-detail">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" class="back-btn">
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
          <el-button @click="showInviteDialog = true">
            <UserPlus :size="18" />
            邀请学生
          </el-button>
          <el-button @click="generateInviteCode">
            <Key :size="18" />
            生成邀请码
          </el-button>
          <el-button
            v-if="selectedStudentIds.length > 0"
            type="danger"
            @click="batchKickStudentsAction"
          >
            批量踢出 ({{ selectedStudentIds.length }})
          </el-button>
          <el-button
            v-if="selectedStudentIds.length > 0"
            @click="clearSelection"
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
      class="dark-dialog"
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
      class="dark-dialog"
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
      title="邀请码"
      width="400px"
      class="dark-dialog"
    >
      <div class="invite-code-content">
        <p>邀请码已生成：</p>
        <div class="invite-code-box">
          <span class="invite-code">{{ inviteCode }}</span>
          <el-button size="small" @click="copyInviteCode">复制</el-button>
        </div>
        <p class="invite-tip">学生可使用此邀请码直接加入课程</p>
      </div>
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
  X,
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
  file: UploadUserFile,
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
  const result = await post<string>(`/class/${route.params.id}/invite-code`);
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
      "解散课堂后，所有作业、成员、邀请等数据将被永久删除，此操作不可恢复。确认解散？",
      "危险操作",
      {
        confirmButtonText: "确认解散",
        cancelButtonText: "取消",
        type: "error",
      },
    );
    const result = await del(`/class/${route.params.id}`);
    if (result.code === 200) {
      ElMessage.success("课堂已解散");
      router.push("/teacher/courses");
    } else {
      ElMessage.error(result.message);
    }
  } catch {
    // 用户取消
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

.back-btn {
  padding: 8px;
}

.header-left h2 {
  font-size: 24px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
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
  color: rgba(255, 255, 255, 0.6);
}

.info-item svg {
  color: rgba(255, 255, 255, 0.7);
}

.info-item .value {
  color: rgba(255, 255, 255, 0.95);
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
  color: rgba(255, 255, 255, 0.8);
}

.description-section p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
}

.course-tabs {
  margin-bottom: 20px;
}

.course-tabs :deep(.el-tabs__item) {
  color: rgba(255, 255, 255, 0.6);
}

.course-tabs :deep(.el-tabs__item.is-active) {
  color: rgba(255, 255, 255, 0.95);
}

.course-tabs :deep(.el-tabs__item:hover) {
  color: rgba(255, 255, 255, 0.8);
}

.course-tabs :deep(.el-tabs__active-bar) {
  background-color: #667eea;
}

.course-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(255, 255, 255, 0.1);
}

.work-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.work-item {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
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
  color: rgba(255, 255, 255, 0.6);
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
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: white;
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
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
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
  color: rgba(255, 255, 255, 0.5);
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
  color: rgba(255, 255, 255, 0.4);
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
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
}

.invite-code {
  font-size: 18px;
  font-weight: 600;
  color: #667eea;
  padding: 8px 16px;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 8px;
}

.invite-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.course-tabs :deep(.el-checkbox__label) {
  color: rgba(255, 255, 255, 0.8);
}

.course-tabs :deep(.el-checkbox__inner) {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.25);
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
