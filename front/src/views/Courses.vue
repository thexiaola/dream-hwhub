<template>
  <div class="courses-page">
    <!-- 选项卡只作切换器：内容由下方按当前选项卡渲染，便于各自保留原布局 -->
    <el-tabs v-model="activeTab" class="courses-tabs" @tab-change="onTabChange">
      <el-tab-pane label="我听的课" name="student" />
      <el-tab-pane v-if="canEnterTeacherArea" label="我教的课" name="teacher" />
    </el-tabs>

    <!-- 两个面板切换用 v-show 而非 v-if：避免每次切换都把顶部操作栏与内容卡片
         销毁重建（会闪现、且切回时重复请求）。教师面板首次进入后才挂载，保留懒加载。
         切换时只给「当前激活面板的内容卡片」加动画类，顶部操作栏保持静止。 -->
    <MyCourses
      v-show="activeTab === 'student'"
      :class="{ 'content-anim': contentAnim && activeTab === 'student' }"
    />
    <TeachingCourses
      v-if="teacherMounted"
      v-show="activeTab === 'teacher'"
      :class="{ 'content-anim': contentAnim && activeTab === 'teacher' }"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSchoolStore } from '@/stores/school'
import MyCourses from '@/views/student/MyCourses.vue'
import TeachingCourses from '@/views/teacher/TeachingCourses.vue'

type CourseTab = 'student' | 'teacher'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const schoolStore = useSchoolStore()

// 可进入「我教的课」：平台管理员或学校老师（含学校管理员）
const canEnterTeacherArea = computed(() => userStore.isOp || schoolStore.isSchoolTeacher)

const activeTab = ref<CourseTab>('student')
// 「我教的课」面板是否已挂载：首次进入后常驻，之后切换仅显隐、不重新请求
const teacherMounted = ref(false)

// 切换选项卡时，只给「当前激活面板的内容卡片」播放入场动画，顶部操作栏保持静止。
// 面板用 v-show 常驻、不会重挂载，所以用类名触发动画：先移除再于下一帧加回，
// 保证快速连续切换时也能重播；动画结束后复位。
const contentAnim = ref(false)
let contentAnimTimer: number | null = null
const playContentAnim = async () => {
  contentAnim.value = false
  await nextTick()
  contentAnim.value = true
  if (contentAnimTimer !== null) window.clearTimeout(contentAnimTimer)
  contentAnimTimer = window.setTimeout(() => {
    contentAnim.value = false
    contentAnimTimer = null
  }, 460)
}

// 由路由 /courses/:tab 决定当前选项卡；无权限访问教师选项卡时回退到「我听的课」
const syncFromRoute = () => {
  const raw = route.params.tab
  const tab = Array.isArray(raw) ? raw[0] : raw
  const resolved: CourseTab = tab === 'teacher' && canEnterTeacherArea.value ? 'teacher' : 'student'
  if (resolved === 'teacher') teacherMounted.value = true
  activeTab.value = resolved
}

const onTabChange = (name: string | number) => {
  const tab: CourseTab = name === 'teacher' ? 'teacher' : 'student'
  if (tab === 'teacher') teacherMounted.value = true
  // 仅用户主动切换选项卡时播放内容区动画（首次进入不播，避免「假动画」）
  playContentAnim()
  if (route.params.tab !== tab) {
    router.replace({ name: 'Courses', params: { tab } })
  }
}

onMounted(syncFromRoute)
watch(() => route.params.tab, syncFromRoute)
// 学校身份异步就绪后重新解析（如刷新时直接访问 /courses/teacher）
watch(canEnterTeacherArea, syncFromRoute)
</script>

<style scoped>
/* 页面撑满内容区高度：选项卡固定在顶部，下方课程列表卡片纵向延伸铺满 */
.courses-page {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.courses-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.courses-tabs :deep(.el-tabs__item) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
  font-size: 15px;
}

.courses-tabs :deep(.el-tabs__item.is-active) {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
  font-weight: 600;
}

.courses-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--primary-color);
}

.courses-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
}

/* 切换选项卡时内容区入场动画：只作用于下方内容卡片，顶部操作栏保持静止。
   .content-anim 由 Courses.vue 绑到当前激活面板的根节点上（类会落到子组件根元素），
   再用 :deep 命中其内部的内容卡片。 */
.content-anim :deep(.content-card) {
  animation: courses-content-in 0.42s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

@keyframes courses-content-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .content-anim :deep(.content-card) {
    animation: none;
  }
}
</style>
