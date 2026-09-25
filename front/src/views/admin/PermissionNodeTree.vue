<template>
  <div class="permission-node-tree">
    <div v-for="group in groups" :key="group.key" class="node-group">
      <div class="group-header">
        <el-checkbox
          :model-value="isGroupChecked(group)"
          :indeterminate="isGroupIndeterminate(group)"
          @change="(val: boolean | string | number) => toggleGroup(group, Boolean(val))"
        >
          <span class="group-name">{{ group.name }}</span>
          <span class="group-count">{{ selectedCount(group) }}/{{ group.nodes.length }}</span>
        </el-checkbox>
      </div>
      <div class="group-nodes">
        <el-checkbox
          v-for="item in group.nodes"
          :key="item.node"
          :model-value="modelValue.includes(item.node)"
          @change="(val: boolean | string | number) => toggleNode(item.node, Boolean(val))"
        >
          <span class="node-name">{{ item.name }}</span>
          <code class="node-code">{{ item.node }}</code>
          <span class="node-desc">{{ item.description }}</span>
        </el-checkbox>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PermissionNodeGroup } from '@/types/admin'

const props = defineProps<{
  groups: PermissionNodeGroup[]
}>()

const modelValue = defineModel<string[]>({ default: () => [] })

const isGroupChecked = (group: PermissionNodeGroup) =>
  group.nodes.length > 0 && group.nodes.every(item => modelValue.value.includes(item.node))

const isGroupIndeterminate = (group: PermissionNodeGroup) => {
  const count = selectedCount(group)
  return count > 0 && count < group.nodes.length
}

const selectedCount = (group: PermissionNodeGroup) =>
  group.nodes.filter(item => modelValue.value.includes(item.node)).length

const toggleNode = (node: string, checked: boolean) => {
  const next = new Set(modelValue.value)
  if (checked) {
    next.add(node)
  } else {
    next.delete(node)
  }
  modelValue.value = Array.from(next)
}

const toggleGroup = (group: PermissionNodeGroup, checked: boolean) => {
  const next = new Set(modelValue.value)
  for (const item of group.nodes) {
    if (checked) {
      next.add(item.node)
    } else {
      next.delete(item.node)
    }
  }
  modelValue.value = Array.from(next)
}
</script>

<style scoped>
.permission-node-tree {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 50vh;
  overflow-y: auto;
}

.node-group {
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.12);
  border-radius: 10px;
  padding: 12px 16px;
}

.group-header {
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08);
  margin-bottom: 8px;
}

.group-name {
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.group-count {
  margin-left: 8px;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
}

.group-nodes {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.node-name {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
}

.node-code {
  margin-left: 8px;
  font-size: 12px;
  color: #667eea;
}

.node-desc {
  margin-left: 8px;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
}

.permission-node-tree :deep(.el-checkbox__label) {
  display: inline-flex;
  align-items: center;
  /* 窄屏（弹窗全宽后）下节点说明可换行，避免长文案撑出容器 */
  flex-wrap: wrap;
  row-gap: 2px;
}
</style>
