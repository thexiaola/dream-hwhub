<template>
  <div class="school-manage-page">
    <el-page-header class="page-back" :content="schoolName || '学校管理'" @back="goBack" />

    <el-card class="content-card" v-loading="loading">
      <SchoolManagePanel v-if="allowed" :school-id="schoolId" @changed="onChanged" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { get } from '@/utils/http'
import { useUserStore } from '@/stores/user'
import { SCHOOL_ROLE_ADMIN, type SchoolDetail } from '@/types/school'
import SchoolManagePanel from './SchoolManagePanel.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const schoolId = Number(route.params.id)
const loading = ref(true)
const allowed = ref(false)
const schoolName = ref('')

// 返回：优先回到来源页（管理面板或「我的学校」），无历史时退回「我的学校」
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/school')
  }
}

const load = async () => {
  loading.value = true
  try {
    const result = await get<SchoolDetail>(`/school/${schoolId}`)
    if (result.code !== 200 || !result.data) {
      ElMessage.error(result.message || '学校不存在')
      router.replace('/school')
      return
    }
    schoolName.value = result.data.schoolName
    // 仅平台管理员或该校学校管理员可进入管理页；其余成员退回学校详情
    const isSchoolAdmin = result.data.myRoleCode === SCHOOL_ROLE_ADMIN
    if (!userStore.isOp && !isSchoolAdmin) {
      ElMessage.error('只有该校学校管理员或平台管理员可以管理该学校')
      router.replace(`/school/${schoolId}`)
      return
    }
    allowed.value = true
  } finally {
    loading.value = false
  }
}

// 面板内部已刷新自身统计；此处留作扩展（如需回写标题等）
const onChanged = () => {}

onMounted(load)
</script>

<style scoped>
/* 路由组件直接挂在块级 .main-content 下，用 min-height:100% 撑满内容区
   （flex:1 只在 flex 父容器下生效） */
.school-manage-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
}

.page-back {
  flex-shrink: 0;
}

/* 管理面板纵向撑满剩余高度：内容多时自然增长并滚动 */
.content-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.content-card :deep(.el-card__body) {
  flex: 1;
  min-width: 0;
}

@media (max-width: 768px) {
  .school-manage-page {
    gap: 12px;
  }
}
</style>
