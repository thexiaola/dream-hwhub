<template>
  <div class="submissions-page">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" class="page-back-btn">
          <ArrowLeft :size="18" />
        </el-button>
        <div>
          <h2>查看提交</h2>
          <p class="subtitle" v-if="work">{{ work.title }}</p>
        </div>
      </div>
      <div class="header-right">
        <el-button
          type="primary"
          :loading="downloadingZip"
          @click="batchDownload"
        >
          <Download :size="16" />
          打包下载附件
        </el-button>
        <el-button @click="goBack" class="page-cancel-btn">返回</el-button>
      </div>
    </div>

    <el-card class="work-info-card dark-dialog" v-loading="loading">
      <div class="info-grid" v-if="work">
        <div class="info-item">
          <FileText :size="16" />
          <span class="label">作业标题：</span>
          <span class="value">{{ work.title }}</span>
        </div>
        <div class="info-item">
          <Clock :size="16" />
          <span class="label">截止时间：</span>
          <span class="value">{{ formatDate(work.deadline) }}</span>
        </div>
        <div class="info-item">
          <Award :size="16" />
          <span class="label">满分：</span>
          <span class="value">{{ work.totalScore }} 分</span>
        </div>
        <div class="info-item">
          <UserCheck :size="16" />
          <span class="label">已交：</span>
          <span class="value">{{ submittedCount }} 人</span>
        </div>
        <div class="info-item">
          <UserX :size="16" />
          <span class="label">未交：</span>
          <span class="value">{{ unsubmittedCount }} 人</span>
        </div>
      </div>

      <div class="work-description" v-if="work && work.description">
        <div class="content-label">
          <FileText :size="14" />
          <span>作业内容</span>
        </div>
        <p class="description-text">{{ work.description }}</p>
      </div>

      <div
        class="work-attachments"
        v-if="work && work.attachments && work.attachments.length > 0"
      >
        <div class="content-label">
          <Paperclip :size="14" />
          <span>作业附件 ({{ work.attachments.length }})</span>
        </div>
        <div class="attachment-list">
          <div
            v-for="att in work.attachments"
            :key="att.id"
            class="attachment-item"
            @click="downloadAttachment(att)"
          >
            <File :size="14" />
            <span class="att-name">{{ att.fileName }}</span>
            <span class="att-size" v-if="att.fileSize">({{ formatFileSize(att.fileSize) }})</span>
          </div>
        </div>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" class="submissions-tabs" @tab-change="handleTabChange">
      <el-tab-pane :label="`已交 (${submittedCount})`" name="submitted">
        <div class="submission-list" v-loading="loading">
          <div v-for="sub in submissions" :key="sub.id" class="submission-item">
            <div class="submission-header">
              <div class="student-info">
                <div class="student-avatar">
                  <User :size="18" />
                </div>
                <div class="student-details">
                  <h4>{{ sub.submitterUsername || sub.submitterName }}</h4>
                  <p class="student-email">{{ sub.submitterEmail }}</p>
                  <p class="student-meta">
                    <template v-if="sub.submitterIdName">{{ sub.submitterIdName }} · </template>{{ sub.submitterUserNo }}
                  </p>
                </div>
              </div>
              <div class="submission-meta">
                <span
                  class="status-tag"
                  :class="sub.status === 2 ? 'graded' : sub.status === 3 ? 'returned' : 'pending'"
                >
                  {{ sub.status === 2 ? "已批改" : sub.status === 3 ? "已打回" : "待批改" }}
                </span>
                <span v-if="sub.isLate" class="late-tag">逾期提交</span>
              </div>
            </div>

            <div class="submission-content" v-if="sub.submissionContent">
              <div class="content-label">
                <MessageSquare :size="14" />
                <span>提交内容</span>
              </div>
              <p class="content-text">{{ sub.submissionContent }}</p>
            </div>

            <div class="submission-attachments" v-if="sub.attachments && sub.attachments.length > 0">
              <div class="content-label">
                <Paperclip :size="14" />
                <span>附件 ({{ sub.attachments.length }})</span>
              </div>
              <div class="attachment-list">
                <div
                  v-for="att in sub.attachments"
                  :key="att.id"
                  class="attachment-item"
                  @click="downloadAttachment(att)"
                >
                  <File :size="14" />
                  <span class="att-name">{{ att.fileName }}</span>
                  <span class="att-size" v-if="att.fileSize">({{ formatFileSize(att.fileSize) }})</span>
                </div>
              </div>
            </div>

            <div class="submission-grade" v-if="sub.status === 2 || sub.status === 3">
              <div class="grade-row">
                <Award :size="16" />
                <span class="label">分数：</span>
                <span class="score">{{ sub.score }} / {{ work?.totalScore }}</span>
              </div>
              <div class="grade-row" v-if="sub.comment">
                <MessageSquare :size="16" />
                <span class="label">评语：</span>
                <span class="comment">{{ sub.comment }}</span>
              </div>
              <div class="grade-row" v-if="sub.graderName">
                <User :size="16" />
                <span class="label">批改人：</span>
                <span>{{ sub.graderName }}</span>
              </div>
            </div>

            <div class="submission-footer">
              <Clock :size="12" />
              <span>提交时间：{{ formatDate(sub.createTime) }}</span>
              <el-button
                type="primary"
                size="small"
                class="grade-btn"
                @click="openGradeDialog(sub)"
              >
                {{ sub.status === 2 ? "重新批改" : "批改" }}
              </el-button>
            </div>
          </div>
        </div>
        <div v-if="!loading && submissions.length === 0" class="empty-state">
          <FileText :size="32" />
          <p>暂无提交</p>
        </div>
        <div class="pagination-wrap" v-if="total > pageSize">
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="loadSubmissions"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane :label="`未交 (${unsubmittedCount})`" name="unsubmitted">
        <div class="unsubmitted-list" v-loading="unsubmittedLoading">
          <div v-for="stu in unsubmittedStudents" :key="stu.id" class="unsubmitted-item">
            <div class="student-avatar">
              <User :size="16" />
            </div>
            <div class="student-details">
              <span class="name">{{ stu.username }}</span>
              <span class="email">{{ stu.email }}</span>
              <span class="user-no">
                <template v-if="stu.idName">{{ stu.idName }} · </template>{{ stu.userNo }}
              </span>
            </div>
          </div>
        </div>
        <div v-if="!unsubmittedLoading && unsubmittedStudents.length === 0" class="empty-state">
          <CheckCircle :size="32" />
          <p>全部已提交</p>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="gradeDialogVisible"
      title="批改作业"
      width="480px"
      class="dark-dialog"
      :close-on-click-modal="false"
    >
      <div v-if="gradingSubmission" class="grade-form-wrap">
        <p class="grade-student">
          {{ gradingSubmission.submitterName }}（{{ gradingSubmission.submitterUserNo }}）的提交
        </p>
        <el-form label-width="90px">
          <el-form-item label="分数">
            <el-input-number
              v-model="gradeForm.score"
              :min="0"
              :max="work?.totalScore ?? 100"
              :precision="1"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="评语">
            <el-input
              v-model="gradeForm.comment"
              type="textarea"
              :rows="3"
              maxlength="512"
              show-word-limit
              placeholder="请输入评语（必填）"
            />
          </el-form-item>
          <el-form-item label="打回重做">
            <el-switch v-model="gradeForm.isReturned" />
            <span class="return-tip">开启后学生可重新提交该作业</span>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="gradeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="grading" @click="submitGrade">
          {{ gradeForm.isReturned ? "确认打回" : "确认批改" }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { get, put } from "@/utils/http";
import instance from "@/utils/http";
import { openAttachmentPreview } from "@/utils/attachment";
import { ElMessage } from "element-plus";
import {
  ArrowLeft,
  FileText,
  Clock,
  Award,
  User,
  UserCheck,
  UserX,
  Paperclip,
  File,
  MessageSquare,
  CheckCircle,
  Download,
} from "@lucide/vue";

interface WorkInfo {
  id: number;
  title: string;
  description: string;
  deadline: string;
  totalScore: number;
  attachments?: AttachmentInfo[];
}

interface AttachmentInfo {
  id: number;
  fileName: string;
  filePath: string;
  fileSize: number;
  fileType: string;
  uploadTime: string;
}

interface SubmissionInfo {
  id: number;
  workId: number;
  submitterId: number;
  submitterName: string;
  submitterUsername?: string;
  submitterEmail?: string;
  submitterIdName?: string;
  submitterUserNo: string;
  submissionContent: string;
  score: number | null;
  comment: string | null;
  gradeTime: string | null;
  graderName: string | null;
  status: number;
  isLate: boolean;
  createTime: string;
  attachments: AttachmentInfo[];
}

interface UnsubmittedStudent {
  id: number;
  username: string;
  email?: string;
  idName?: string;
  userNo: string;
}

const route = useRoute();
const router = useRouter();

const workClassId = ref<number | null>(null);
const work = ref<WorkInfo | null>(null);
const submissions = ref<SubmissionInfo[]>([]);
const unsubmittedStudents = ref<UnsubmittedStudent[]>([]);
const loading = ref(false);
const unsubmittedLoading = ref(false);
const activeTab = ref("submitted");

const pageNum = ref(1);
const pageSize = 10;
const total = ref(0);
const submittedCount = ref(0);
const unsubmittedCount = ref(0);

const goBack = () => {
  if (workClassId.value) {
    router.push(`/teacher/course/${workClassId.value}`);
    return;
  }
  router.push("/teacher/courses");
};

const formatDate = (dateStr: string | null): string => {
  if (!dateStr) return "未知";
  const d = new Date(dateStr.replace(" ", "T"));
  if (isNaN(d.getTime())) return dateStr;
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const h = String(d.getHours()).padStart(2, "0");
  const min = String(d.getMinutes()).padStart(2, "0");
  return `${y}-${m}-${day} ${h}:${min}`;
};

const formatFileSize = (bytes: number): string => {
  if (!bytes) return "";
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / (1024 * 1024)).toFixed(2) + " MB";
};

const loadWork = async () => {
  const workId = route.params.id;
  try {
    const result = await get<WorkInfo & { classId?: number }>(`/works/${workId}`);
    if (result.code === 200 && result.data) {
      work.value = result.data;
      workClassId.value = result.data.classId ?? null;
    }
  } catch {
    ElMessage.error("加载作业信息失败");
  }
};

const loadSubmissions = async () => {
  const workId = route.params.id;
  loading.value = true;
  try {
    const result = await get<{ records: SubmissionInfo[]; total: number }>(
      `/submissions/work/list`,
      { workId: Number(workId), pageNum: pageNum.value, pageSize }
    );
    if (result.code === 200 && result.data) {
      submissions.value = result.data.records || [];
      total.value = result.data.total || 0;
      submittedCount.value = result.data.total || 0;
    }
  } catch {
    ElMessage.error("加载提交列表失败");
  } finally {
    loading.value = false;
  }
};

const loadUnsubmitted = async () => {
  const workId = route.params.id;
  unsubmittedLoading.value = true;
  try {
    const result = await get<UnsubmittedStudent[]>(
      `/submissions/work/unsubmitted`,
      { workId: Number(workId) }
    );
    if (result.code === 200 && result.data) {
      unsubmittedStudents.value = result.data;
      unsubmittedCount.value = result.data.length;
    }
  } catch {
    ElMessage.error("加载未交名单失败");
  } finally {
    unsubmittedLoading.value = false;
  }
};

const handleTabChange = (tab: string) => {
  if (tab === "unsubmitted" && unsubmittedStudents.value.length === 0) {
    loadUnsubmitted();
  }
};

const gradeDialogVisible = ref(false);
const grading = ref(false);
const gradingSubmission = ref<SubmissionInfo | null>(null);
const gradeForm = ref({ score: 0, comment: "", isReturned: false });

const openGradeDialog = (sub: SubmissionInfo) => {
  gradingSubmission.value = sub;
  gradeForm.value = {
    score: sub.score ?? 0,
    comment: sub.comment ?? "",
    isReturned: false,
  };
  gradeDialogVisible.value = true;
};

const submitGrade = async () => {
  if (!gradingSubmission.value) return;
  if (!gradeForm.value.comment.trim()) {
    ElMessage.warning("评语不能为空");
    return;
  }
  grading.value = true;
  const result = await put("/submissions/grade", {
    submissionId: gradingSubmission.value.id,
    score: gradeForm.value.score,
    comment: gradeForm.value.comment.trim(),
    isReturned: gradeForm.value.isReturned,
  });
  grading.value = false;
  if (result.code === 200) {
    ElMessage.success(gradeForm.value.isReturned ? "已打回，学生可重新提交" : "批改成功");
    gradeDialogVisible.value = false;
    loadSubmissions();
  } else {
    ElMessage.error(result.message || "批改失败");
  }
};

const downloadAttachment = (att: AttachmentInfo) => {
  openAttachmentPreview(att.filePath, att.fileName, true)
};

const downloadingZip = ref(false);

const batchDownload = async () => {
  if (!work.value) return;
  downloadingZip.value = true;
  try {
    const response = await instance.post(
      "/submissions/batch-download",
      { workId: Number(route.params.id) },
      { responseType: "blob" },
    );
    if (response.status === 200) {
      const blob = response.data as Blob;
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `${work.value.title}-提交附件.zip`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } else {
      ElMessage.error("打包下载失败，可能暂无符合条件的提交附件");
    }
  } catch {
    ElMessage.error("打包下载失败，请稍后重试");
  } finally {
    downloadingZip.value = false;
  }
};

onMounted(async () => {
  await loadWork();
  await loadSubmissions();
});
</script>

<style scoped>
.submissions-page {
  padding-bottom: 24px;
  width: 100%;
  box-sizing: border-box;
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
  margin-bottom: 4px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.subtitle {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.header-right {
  display: flex;
  gap: 12px;
}

.work-info-card {
  margin-bottom: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.info-item .label {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.info-item .value {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
  font-weight: 500;
}

.submissions-tabs {
  margin-top: 8px;
}

.submission-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.submission-item {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08);
  border-radius: 10px;
  padding: 18px 20px;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.submission-item:hover {
  border-color: rgba(102, 126, 234, 0.4);
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06);
}

.submission-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.student-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.student-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--fg-on-accent);
  flex-shrink: 0;
}

.student-info h4 {
  font-size: 15px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
  margin-bottom: 2px;
}

.student-info p {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.submission-header .student-details .student-email {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.65);
}

.submission-header .student-details .student-meta {
  margin-top: 2px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.42);
}

.submission-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-tag {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.graded {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.status-tag.pending {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}

.status-tag.returned {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.late-tag {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.submission-content {
  margin-top: 12px;
  padding: 12px 14px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border-radius: 8px;
  border-left: 3px solid rgba(102, 126, 234, 0.5);
}

.content-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
  margin-bottom: 6px;
}

.content-text {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.work-attachments {
  margin-top: 16px;
}

.work-description {
  margin-top: 16px;
}

.description-text {
  font-size: 14px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
  line-height: 1.7;
  padding: 12px 14px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-word;
}

.submission-attachments {
  margin-top: 12px;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 6px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08);
  border-radius: 6px;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
  cursor: pointer;
  transition: all 0.2s ease;
}

.attachment-item:hover {
  background: rgba(102, 126, 234, 0.1);
  border-color: rgba(102, 126, 234, 0.3);
  color: #667eea;
}

.att-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.att-size {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.submission-grade {
  margin-top: 12px;
  padding: 12px 14px;
  background: rgba(102, 126, 234, 0.06);
  border-radius: 8px;
  border: 1px solid rgba(102, 126, 234, 0.15);
}

.grade-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 6px;
}

.grade-row:last-child {
  margin-bottom: 0;
}

.grade-row .label {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
  flex-shrink: 0;
}

.grade-row .score {
  color: #667eea;
  font-weight: 600;
}

.grade-row .comment {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
}

.submission-footer {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.submission-footer .grade-btn {
  margin-left: auto;
}

.grade-form-wrap .grade-student {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.85);
}

.grade-form-wrap .return-tip {
  margin-left: 10px;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.unsubmitted-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}

.unsubmitted-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08);
  border-radius: 8px;
}

.unsubmitted-item .student-details {
  display: flex;
  flex-direction: column;
}

.unsubmitted-item .name {
  font-size: 14px;
  font-weight: 500;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
}

.unsubmitted-item .user-no {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
}

.unsubmitted-item .email {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.65);
}

/* ================================================
   el-tabs 深色主题适配
   ================================================ */

.submissions-tabs :deep(.el-tabs__item) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.65) !important;
}

.submissions-tabs :deep(.el-tabs__item.is-active) {
  color: var(--fg) !important;
  font-weight: 600 !important;
}

.submissions-tabs :deep(.el-tabs__item:hover) {
  color: #667eea !important;
}

.submissions-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.12) !important;
}

.submissions-tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #667eea, #764ba2) !important;
  height: 2px !important;
}

/* 空状态字色 */
.submissions-page :deep(.el-empty__description p) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4) !important;
}

/* 分页深色适配 */
.pagination-wrap :deep(.el-pagination .el-pager li) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06) !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.75) !important;
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08) !important;
}

.pagination-wrap :deep(.el-pagination .el-pager li:hover) {
  color: #667eea !important;
  border-color: rgba(102, 126, 234, 0.4) !important;
}

.pagination-wrap :deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  color: var(--fg-on-accent) !important;
  border-color: transparent !important;
}

.pagination-wrap :deep(.el-pagination button) {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06) !important;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.75) !important;
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08) !important;
}

.pagination-wrap :deep(.el-pagination button:hover:not(:disabled)) {
  color: #667eea !important;
  border-color: rgba(102, 126, 234, 0.4) !important;
}

.pagination-wrap :deep(.el-pagination button:disabled) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.25) !important;
  cursor: not-allowed !important;
}

/* loading 深色 */
.submissions-page :deep(.el-loading-spinner .circular) {
  stroke: #667eea !important;
}
</style>
