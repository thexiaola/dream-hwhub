<template>
  <div class="teacher-courses-page">
    <div class="page-header">
      <div class="header-left">
        <h2>我教的课</h2>
        <p class="subtitle">管理你作为老师教授的课程</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="showCreateDialog = true">
          <Plus :size="18" />
          创建课程
        </el-button>
      </div>
    </div>

    <el-card class="content-card">
      <div class="course-grid">
        <div 
          v-for="course in teacherCourses" 
          :key="course.id" 
          class="course-card"
          @click="goToCourse(course.id)"
        >
          <div class="card-header">
            <div class="course-icon">
              <Presentation :size="24" />
            </div>
            <h3>{{ course.className }}</h3>
          </div>
          <p class="description">{{ course.description || '暂无描述' }}</p>
          <div class="card-info">
            <div class="info-item">
              <Users :size="14" />
              <span>{{ course.studentCount }} 名学生</span>
            </div>
            <div class="info-item">
              <User :size="14" />
              <span>{{ course.teacherCount }} 位老师</span>
            </div>
          </div>
          <div class="card-footer">
            <span class="role-badge" :class="course.userRole === '创建者' ? 'owner' : 'teacher'">
              {{ course.userRole }}
            </span>
          </div>
        </div>
      </div>
      <div v-if="teacherCourses.length === 0" class="empty-state">
        <Presentation :size="48" />
        <p>暂无课程</p>
        <p class="empty-tip">点击"创建课程"按钮创建你的第一个课程</p>
      </div>
    </el-card>

    <el-dialog v-model="showCreateDialog" title="创建课程" width="500px" class="dark-dialog">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="课程名称">
          <el-input v-model="createForm.className" placeholder="请输入课程名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input 
            v-model="createForm.description" 
            type="textarea" 
            placeholder="请输入课程描述"
            :rows="3"
            maxlength="512"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createCourse">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { get, post } from '@/utils/http'
import { ElMessage } from 'element-plus'
import { Plus, Presentation, Users, User } from '@lucide/vue'

interface CourseInfo {
  id: number
  className: string
  description?: string
  ownerId: number
  ownerName: string
  userRole: string
  memberCount: number
  teacherCount: number
  studentCount: number
}

const router = useRouter()

const teacherCourses = ref<CourseInfo[]>([])
const showCreateDialog = ref(false)
const createForm = ref({
  className: '',
  description: ''
})

const loadTeacherCourses = async () => {
  const result = await get<{ records: CourseInfo[] }>('/class/mine')
  if (result.code === 200) {
    teacherCourses.value = result.data!.records.filter(
      course => course.userRole === '创建者' || course.userRole === '老师'
    )
  }
}

const goToCourse = (id: number) => {
  router.push(`/teacher/course/${id}`)
}

const createCourse = async () => {
  if (!createForm.value.className) {
    ElMessage.warning('请输入课程名称')
    return
  }
  const result = await post('/class/create', createForm.value)
  if (result.code === 200) {
    ElMessage.success('创建申请已提交，等待管理员审核')
    showCreateDialog.value = false
    createForm.value.className = ''
    createForm.value.description = ''
  } else {
    ElMessage.error(result.message)
  }
}

onMounted(() => {
  loadTeacherCourses()
})
</script>

<style scoped>
.teacher-courses-page {
  padding-bottom: 24px;
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
  color: rgba(255, 255, 255, 0.95);
}

.subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.course-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px;
  transition: all 0.3s;
  cursor: pointer;
}

.course-card:hover {
  background: rgba(255, 255, 255, 0.05);
  transform: translateY(-2px);
  border-color: rgba(118, 75, 162, 0.3);
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
  background: rgba(118, 75, 162, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #764ba2;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
}

.description {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
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
  color: rgba(255, 255, 255, 0.6);
}

.info-item svg {
  color: rgba(255, 255, 255, 0.7);
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

.role-badge.owner {
  background: rgba(118, 75, 162, 0.2);
  color: #764ba2;
}

.role-badge.assistant {
  background: rgba(102, 126, 234, 0.2);
  color: #667eea;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: rgba(255, 255, 255, 0.4);
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