<template>
  <div class="exam-config">
    <div class="config-row">
      <span class="config-label">考试时长</span>
      <div class="config-control">
        <el-input-number
          :model-value="model.durationMinutes ?? undefined"
          :min="1"
          :max="600"
          :step="5"
          controls-position="right"
          placeholder="不限时"
          class="duration-input"
          @update:model-value="onDurationChange"
        />
        <span class="unit">分钟</span>
        <span class="hint">留空则不单独计时，仅以截止时间为准</span>
      </div>
    </div>

    <div class="config-row">
      <span class="config-label">反作弊</span>
      <div class="config-control">
        <el-switch
          :model-value="model.enabled"
          @update:model-value="(v: string | number | boolean) => update({ enabled: Boolean(v) })"
        />
        <span class="hint">开启后可勾选下列反作弊手段</span>
      </div>
    </div>

    <div v-if="model.enabled" class="anti-cheat-list">
      <label class="anti-item">
        <el-checkbox
          :model-value="model.fontScramble"
          @update:model-value="(v: string | number | boolean) => update({ fontScramble: Boolean(v) })"
        >
          字体映射（防复制搜题）
        </el-checkbox>
        <span class="anti-desc">题干与选项以打乱字体渲染：学生看到的字正常，但复制得到的文本是乱码，无法直接搜题。</span>
      </label>

      <label class="anti-item">
        <el-checkbox
          :model-value="model.forceFullscreen"
          @update:model-value="(v: string | number | boolean) => update({ forceFullscreen: Boolean(v) })"
        >
          强制全屏答题
        </el-checkbox>
        <span class="anti-desc">进入考试后请求全屏；一旦退出全屏即记一次违规。</span>
      </label>

      <label class="anti-item">
        <el-checkbox
          :model-value="model.detectLeave"
          @update:model-value="(v: string | number | boolean) => update({ detectLeave: Boolean(v) })"
        >
          切屏 / 失焦检测
        </el-checkbox>
        <span class="anti-desc">切换到其他标签页或窗口失焦时记一次违规。</span>
      </label>

      <label class="anti-item">
        <el-checkbox
          :model-value="model.noCopy"
          @update:model-value="(v: string | number | boolean) => update({ noCopy: Boolean(v) })"
        >
          禁止复制 / 剪切 / 粘贴
        </el-checkbox>
        <span class="anti-desc">拦截复制、剪切、粘贴与右键菜单，并记一次违规。</span>
      </label>

      <div class="config-row anti-limit">
        <span class="config-label">违规上限</span>
        <div class="config-control">
          <el-input-number
            :model-value="model.maxViolations ?? undefined"
            :min="1"
            :max="99"
            :step="1"
            controls-position="right"
            placeholder="不限制"
            class="limit-input"
            @update:model-value="onMaxViolationsChange"
          />
          <span class="hint">累计达到该次数即自动交卷；留空表示不限制</span>
        </div>
      </div>
    </div>

    <div class="config-row">
      <span class="config-label">题目乱序</span>
      <div class="config-control">
        <el-switch
          :model-value="model.shuffleQuestions"
          @update:model-value="(v: string | number | boolean) => update({ shuffleQuestions: Boolean(v) })"
        />
        <span class="hint">为每位学生随机打乱题目顺序</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ExamConfig } from '@/types/work'

const props = defineProps<{
  modelValue: ExamConfig
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: ExamConfig): void
}>()

/** 当前配置（受控） */
const model = props.modelValue

const update = (patch: Partial<ExamConfig>) => {
  emit('update:modelValue', { ...props.modelValue, ...patch })
}

const onDurationChange = (v: number | undefined) => {
  update({ durationMinutes: v == null ? null : Number(v) })
}

const onMaxViolationsChange = (v: number | undefined) => {
  update({ maxViolations: v == null ? null : Number(v) })
}
</script>

<style scoped>
.exam-config {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.config-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.config-label {
  flex-shrink: 0;
  width: 72px;
  padding-top: 4px;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
}

.config-control {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.duration-input,
.limit-input {
  width: 130px;
}

.unit {
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
}

.hint {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
}

.anti-cheat-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 4px 0 0 84px;
  padding: 12px 14px;
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 8px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
}

.anti-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
}

.anti-desc {
  margin-left: 24px;
  font-size: 12px;
  line-height: 1.5;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
}

.anti-limit {
  margin-top: 2px;
}

.anti-limit .config-label {
  width: 72px;
}

@media (max-width: 768px) {
  .config-row {
    flex-direction: column;
    gap: 6px;
  }

  .config-label {
    width: auto;
    padding-top: 0;
  }

  .anti-cheat-list {
    margin-left: 0;
  }
}
</style>
