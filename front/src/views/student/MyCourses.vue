<template>
  <div class="student-courses-page">
    <div class="page-header">
      <div class="header-left">
        <h2>我听的课</h2>
        <p class="subtitle">查看你作为学生加入的课程</p>
      </div>
      <div class="header-right">
        <el-badge
          :value="pendingCount"
          :hidden="pendingCount === 0"
          class="invite-badge"
        >
          <el-button @click="openInvitationsDialog">
            <Inbox :size="18" />
            我的邀请
          </el-button>
        </el-badge>
        <el-button @click="showJoinDialog = true">
          <Plus :size="18" />
          加入课程
        </el-button>
      </div>
    </div>

    <el-card class="content-card">
      <div class="course-grid">
        <div
          v-for="course in studentCourses"
          :key="course.id"
          class="course-card"
          @click="goToCourse(course.id)"
        >
          <div class="card-header">
            <div class="course-icon">
              <BookOpen :size="24" />
            </div>
            <h3>{{ course.className }}</h3>
          </div>
          <p class="description">{{ course.description || "暂无描述" }}</p>
          <div class="card-info">
            <div class="info-item">
              <User :size="14" />
              <span>老师：{{ course.ownerName }}</span>
            </div>
            <div class="info-item">
              <Users :size="14" />
              <span>{{ course.studentCount }} 名学生</span>
            </div>
          </div>
          <div class="card-footer">
            <span class="role-badge student">学生</span>
          </div>
        </div>
      </div>
      <div v-if="studentCourses.length === 0" class="empty-state">
        <GraduationCap :size="48" />
        <p>暂无课程</p>
        <p class="empty-tip">点击"加入课程"按钮加入一个课程</p>
      </div>
    </el-card>

    <el-dialog
      v-model="showJoinDialog"
      title="加入课程"
      width="400px"
      class="dark-dialog"
    >
      <el-form :model="joinForm" label-width="80px">
        <el-form-item label="邀请码">
          <el-input
            v-model="joinForm.inviteCode"
            placeholder="请输入25位邀请码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showJoinDialog = false">取消</el-button>
        <el-button type="primary" @click="joinByCode">加入</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showInvitationsDialog"
      title="我的邀请"
      width="520px"
      class="dark-dialog invitations-dialog"
    >
      <div v-if="invitations.length > 0" class="invitation-group">
        <h4 class="invitation-group-title">老师邀请</h4>
        <div class="invitation-list">
          <div
            v-for="item in invitations"
            :key="`t-${item.id}`"
            class="invitation-item"
          >
            <div class="invitation-info">
              <div class="invitation-icon">
                <BookOpen :size="16" />
              </div>
              <div class="invitation-text">
                <p>
                  <strong>{{ item.inviterName }}</strong>
                  邀请你加入「{{ item.className }}」
                </p>
                <span>{{ formatDate(item.createTime) }}</span>
              </div>
            </div>
            <div class="invitation-actions">
              <el-button
                type="primary"
                size="small"
                :loading="respondingId === item.id"
                @click="handleRespond(item, true)"
              >
                接受
              </el-button>
              <el-button
                type="danger"
                plain
                size="small"
                :loading="respondingId === item.id"
                @click="handleRespond(item, false)"
              >
                拒绝
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <div v-if="userInvitations.length > 0" class="invitation-group">
        <h4 class="invitation-group-title">同学邀请</h4>
        <div class="invitation-list">
          <div
            v-for="item in userInvitations"
            :key="`u-${item.id}`"
            class="invitation-item"
          >
            <div class="invitation-info">
              <div class="invitation-icon">
                <UserPlus :size="16" />
              </div>
              <div class="invitation-text">
                <p>
                  <strong>{{ item.inviterName }}</strong>
                  邀请你加入「{{ item.className }}」
                </p>
                <span>{{ formatDate(item.createTime) }}</span>
              </div>
            </div>
            <div class="invitation-actions">
              <el-button
                type="primary"
                size="small"
                :loading="respondingId === item.id"
                @click="handleUserRespond(item, true)"
              >
                接受
              </el-button>
              <el-button
                type="danger"
                plain
                size="small"
                :loading="respondingId === item.id"
                @click="handleUserRespond(item, false)"
              >
                拒绝
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <div
        v-if="invitations.length === 0 && userInvitations.length === 0"
        class="empty-state"
      >
        <Inbox :size="32" />
        <p>暂无待处理的邀请</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { get, post, put } from "@/utils/http";
import { ElMessage } from "element-plus";
import {
  Plus,
  BookOpen,
  User,
  Users,
  GraduationCap,
  Inbox,
  UserPlus,
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

const router = useRouter();

const studentCourses = ref<CourseInfo[]>([]);
const showJoinDialog = ref(false);
const joinForm = ref({
  inviteCode: "",
});

const loadStudentCourses = async () => {
  const result = await get<{ records: CourseInfo[] }>("/class/mine");
  if (result.code === 200) {
    // 学生与助理（协作老师）的课程都在“我听的课”中显示
    studentCourses.value = result.data!.records.filter(
      (course) => course.userRole === "学生" || course.userRole === "老师",
    );
  }
};

const goToCourse = (id: number) => {
  router.push(`/student/course/${id}`);
};

const joinByCode = async () => {
  if (!joinForm.value.inviteCode) {
    ElMessage.warning("请输入邀请码");
    return;
  }
  const result = await post("/class/join-by-code", {
    inviteCode: joinForm.value.inviteCode,
  });
  if (result.code === 200) {
    ElMessage.success("加入成功");
    showJoinDialog.value = false;
    joinForm.value.inviteCode = "";
    loadStudentCourses();
  } else {
    ElMessage.error(result.message);
  }
};

interface InvitationInfo {
  id: number;
  classId: number;
  className: string;
  inviterId: number;
  inviterName: string;
  inviteeUserId: number;
  status: number;
  createTime: string;
}

const showInvitationsDialog = ref(false);
const invitations = ref<InvitationInfo[]>([]);
const userInvitations = ref<InvitationInfo[]>([]);
const respondingId = ref<number | null>(null);
const pendingCount = computed(
  () => invitations.value.length + userInvitations.value.length,
);

const loadMyInvitations = async () => {
  const result = await get<InvitationInfo[]>("/class/my-invitations", {
    status: 0,
  });
  if (result.code === 200) {
    invitations.value = result.data || [];
  }
};

const loadMyUserInvitations = async () => {
  const result = await get<InvitationInfo[]>("/class/my-user-invitations");
  if (result.code === 200) {
    userInvitations.value = result.data || [];
  }
};

const openInvitationsDialog = () => {
  showInvitationsDialog.value = true;
  loadMyInvitations();
  loadMyUserInvitations();
};

const handleRespond = async (item: InvitationInfo, accepted: boolean) => {
  respondingId.value = item.id;
  const result = await put("/class/respond-invitation", {
    invitationId: item.id,
    accepted,
  });
  respondingId.value = null;
  if (result.code === 200) {
    ElMessage.success(accepted ? "已接受，欢迎加入课程" : "已拒绝该邀请");
    loadMyInvitations();
    if (accepted) {
      loadStudentCourses();
    }
  } else {
    ElMessage.error(result.message || "操作失败");
  }
};

const handleUserRespond = async (item: InvitationInfo, accepted: boolean) => {
  respondingId.value = item.id;
  const result = await put(`/class/invitations/${item.id}`, { accepted });
  respondingId.value = null;
  if (result.code === 200) {
    ElMessage.success(accepted ? "已接受，等待老师审核" : "已拒绝该邀请");
    loadMyUserInvitations();
  } else {
    ElMessage.error(result.message || "操作失败");
  }
};

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
};

onMounted(() => {
  loadStudentCourses();
  loadMyInvitations();
  loadMyUserInvitations();
});
</script>

<style scoped>
.student-courses-page {
  padding-bottom: 24px;
}

.invite-badge {
  margin-right: 12px;
}

.invitation-group + .invitation-group {
  margin-top: 16px;
}

.invitation-group-title {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.65);
}

.invitation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.invitation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08);
  border-radius: 10px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04);
}

.invitation-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.invitation-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 50%;
  background: linear-gradient(
    135deg,
    rgba(102, 126, 234, 0.85),
    rgba(118, 75, 162, 0.85)
  );
  color: var(--fg-on-accent);
}

.invitation-text {
  min-width: 0;
}

.invitation-text p {
  margin: 0;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.invitation-text p strong {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 1);
}

.invitation-text span {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.invitation-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .invitation-item {
    flex-direction: column;
    align-items: stretch;
  }

  .invitation-actions {
    justify-content: flex-end;
  }

  .invitation-actions .el-button {
    flex: 1;
    min-height: 42px;
  }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left h2 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.subtitle {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.course-card {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 16px;
  padding: 20px;
  transition: all 0.3s;
  cursor: pointer;
}

.course-card:hover {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.05);
  transform: translateY(-2px);
  border-color: rgba(102, 126, 234, 0.3);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.course-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(102, 126, 234, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.description {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
  margin-bottom: 16px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-info {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.info-item svg {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
}

.card-footer {
  display: flex;
  justify-content: flex-end;
}

.role-badge {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.role-badge.student {
  background: rgba(102, 126, 234, 0.2);
  color: #667eea;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.empty-state p {
  margin-top: 16px;
  font-size: 14px;
}

.empty-tip {
  font-size: 12px;
  margin-top: 8px;
}

@media (max-width: 1200px) {
  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .course-grid {
    grid-template-columns: 1fr;
  }
}
</style>
