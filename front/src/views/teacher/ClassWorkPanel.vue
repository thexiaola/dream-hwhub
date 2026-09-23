<template>
  <div class="class-work-panel">
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
            <User :size="14" />
            <span>布置人：{{ publisherLabel(work) }}</span>
          </div>
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
    <div v-if="worksTotal > 0" class="works-pagination">
      <el-pagination
        v-model:current-page="worksPage"
        v-model:page-size="worksPageSize"
        :page-sizes="[10, 20, 50]"
        :total="worksTotal"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="onWorksPageChange"
        @size-change="onWorksSizeChange"
      />
    </div>

    <!-- 发布作业 -->
    <el-dialog
      v-model="showCreateWorkDialog"
      title="发布作业"
      width="600px"
      class="dark-dialog create-work-dialog"
    >
      <el-form :model="workForm" label-width="80px">
        <el-form-item label="作业标题">
          <el-input v-model="workForm.title" placeholder="请输入作业标题" maxlength="128" />
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
            <el-button type="primary" plain size="default" class="upload-trigger-btn">
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
        <el-button type="primary" @click="createWork" :loading="workSubmitting">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox, type UploadUserFile } from "element-plus";
import { Clock, Edit3, Eye, FileText, Paperclip, Star, Trash2, Upload, User, Users } from "@lucide/vue";
import { del, get, patch, postForm } from "@/utils/http";
import { formatDateTime as formatDate } from "@/utils/format";
import type { TeacherWorkInfo } from "@/types/class";

const props = defineProps<{
  /** 班级 ID */
  classId: number;
}>();

const router = useRouter();

const works = ref<TeacherWorkInfo[]>([]);
const worksPage = ref(1);
const worksPageSize = ref(10);
const worksTotal = ref(0);

const showCreateWorkDialog = ref(false);
const workSubmitting = ref(false);

const workForm = ref({
  title: "",
  description: "",
  deadline: "",
  totalScore: 100,
  allowLateSubmit: true,
  classId: props.classId,
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
    classId: props.classId,
  };
  workFormDate.value = "";
  workFormTime.value = "";
  attachmentFiles.value = [];
};

/** 布置人标识：班级成员显示姓名，非成员（如管理员）退回用户名。不展示学号 */
const publisherLabel = (work: TeacherWorkInfo) => {
  return work.publisherStudentName || work.publisherName || "—";
};

const loadWorks = async () => {
  const result = await get<{ records: TeacherWorkInfo[]; total: number }>("/works", {
    classId: props.classId,
    pageNum: worksPage.value,
    pageSize: worksPageSize.value,
  });
  if (result.code === 200 && result.data) {
    works.value = result.data.records || [];
    worksTotal.value = result.data.total || 0;
  }
};

const onWorksPageChange = () => {
  loadWorks();
};

const onWorksSizeChange = () => {
  worksPage.value = 1;
  loadWorks();
};

const getWorkStatus = (work: TeacherWorkInfo) => {
  const now = new Date();
  const deadline = new Date(work.deadline);
  if (now > deadline) return "expired";
  if (work.status === 1) return "active";
  return "pending";
};

const getWorkStatusText = (work: TeacherWorkInfo) => {
  const status = getWorkStatus(work);
  const map: Record<string, string> = {
    active: "进行中",
    expired: "已截止",
    pending: "未发布",
  };
  return map[status] || status;
};

const viewSubmissions = (workId: number) => {
  router.push(`/teacher/work/${workId}/submissions`);
};

const editWork = (workId: number) => {
  router.push(`/teacher/work/${workId}/edit`);
};

const togglePin = async (work: TeacherWorkInfo) => {
  const result = await patch(`/works/${work.id}/pin`, {
    workId: work.id,
    isPinned: !work.isPinned,
  });
  if (result.code === 200) {
    ElMessage.success(work.isPinned ? "已取消置顶" : "已置顶");
    worksPage.value = 1;
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
      // 若删除的是当前页最后一条，回退一页
      if (works.value.length === 1 && worksPage.value > 1) {
        worksPage.value -= 1;
      }
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
      worksPage.value = 1; // 回到第一页以展示最新发布的作业
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

/** 供父页面头部的「发布作业」按钮调用 */
const openCreateWork = () => {
  showCreateWorkDialog.value = true;
};

defineExpose({ openCreateWork, reload: loadWorks });

onMounted(loadWorks);
</script>

<style scoped>
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
