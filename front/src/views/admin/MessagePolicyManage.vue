<template>
  <div class="policy-manage">
    <p class="policy-tip">
      设置全站默认的「向陌生用户发送私信」策略：同一用户每
      <b>重置小时数</b> 内，最多可向 <b>同一位陌生用户</b> 发送
      <b>条数上限</b> 条私信。到达重置时间后计数清零，可继续发送。
      学校可在其管理面板中独立覆盖本策略。
    </p>

    <el-form v-loading="loading" label-width="140px" class="policy-form">
      <el-form-item label="条数上限">
        <el-input-number v-model="form.strangerLimit" :min="0" :max="1000" :step="1" />
        <span class="unit">条</span>
      </el-form-item>
      <el-form-item label="重置小时数">
        <el-input-number v-model="form.resetHours" :min="1" :max="8760" :step="1" />
        <span class="unit">小时</span>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
        <el-button :disabled="saving" @click="load">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="policy-preview">
      <Info :size="16" />
      <span>
        当前生效：每 {{ form.resetHours }} 小时可向同一陌生用户发送
        <b>{{ form.strangerLimit }}</b> 条私信。
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Info } from '@lucide/vue'
import { get, put } from '@/utils/http'
import type { MessagePolicyInfo } from '@/types/friend'

const loading = ref(false)
const saving = ref(false)
const form = reactive({ strangerLimit: 3, resetHours: 24 })

const load = async () => {
  loading.value = true
  const result = await get<MessagePolicyInfo>('/message-policy')
  loading.value = false
  if (result.code === 200 && result.data) {
    form.strangerLimit = result.data.strangerLimit
    form.resetHours = result.data.resetHours
  } else {
    ElMessage.error(result.message)
  }
}

const submit = async () => {
  saving.value = true
  const result = await put<MessagePolicyInfo>('/message-policy', {
    strangerLimit: form.strangerLimit,
    resetHours: form.resetHours
  })
  saving.value = false
  if (result.code === 200) {
    ElMessage.success(result.message || '已保存')
    if (result.data) {
      form.strangerLimit = result.data.strangerLimit
      form.resetHours = result.data.resetHours
    }
  } else {
    ElMessage.error(result.message)
  }
}

onMounted(load)

defineExpose({ reload: load })
</script>

<style scoped>
.policy-tip {
  margin: 0 0 18px;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.75);
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08);
}

.policy-form {
  max-width: 460px;
}

.unit {
  margin-left: 8px;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.policy-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(102, 126, 234, 0.25);
}
</style>
