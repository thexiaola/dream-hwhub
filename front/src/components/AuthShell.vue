<template>
  <div class="auth-page">
    <div class="auth-bg" aria-hidden="true">
      <div class="auth-bg-grid"></div>
      <div class="auth-bg-vignette"></div>
      <div class="auth-bg-shape auth-bg-shape--1"></div>
      <div class="auth-bg-shape auth-bg-shape--2"></div>
      <div class="auth-bg-shape auth-bg-shape--3"></div>
      <div class="auth-bg-shape auth-bg-shape--4"></div>
    </div>

    <div class="auth-toolbar">
      <ThemeToggle />
    </div>

    <div class="auth-card-shell">
      <div class="auth-card-border" aria-hidden="true"></div>
      <div class="auth-card">
        <aside class="auth-brand">
          <div class="auth-brand-mesh" aria-hidden="true"></div>
          <div class="auth-brand-shine" aria-hidden="true"></div>
          <div class="auth-brand-glow" aria-hidden="true"></div>

          <div class="auth-brand-row">
            <span class="auth-logo-wrap auth-logo-wrap--brand">
              <BookOpen :size="24" />
            </span>
            <span class="auth-brand-text">
              <span class="auth-brand-name">作业管理系统</span>
              <span class="auth-brand-sub">dream-hwhub</span>
            </span>
          </div>

          <div class="auth-brand-content">
            <p class="auth-brand-title">欢迎使用<br />作业管理系统</p>
            <p class="auth-brand-desc">课程作业发布、在线提交与批改评分，集中在一个平台完成</p>

            <ul class="auth-features">
              <li v-for="feature in features" :key="feature.text" class="auth-feature">
                <span class="auth-feature-icon">
                  <component :is="feature.icon" :size="14" />
                </span>
                <span>{{ feature.text }}</span>
              </li>
            </ul>

            <div class="auth-illustration" aria-hidden="true">
              <div class="auth-orbit auth-orbit--1"></div>
              <div class="auth-orbit auth-orbit--2"></div>
              <div class="auth-preview">
                <div class="auth-preview-bar">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <div class="auth-preview-body">
                  <div class="auth-preview-sidebar"></div>
                  <div class="auth-preview-main">
                    <div class="auth-preview-chart"></div>
                    <div class="auth-preview-cards">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </aside>

        <section class="auth-form-side">
          <div class="auth-form-divider" aria-hidden="true"></div>
          <div class="auth-form-glass" aria-hidden="true"></div>

          <div class="auth-form-body">
            <div class="auth-mobile-brand">
              <span class="auth-logo-wrap auth-logo-wrap--mobile">
                <BookOpen :size="22" />
              </span>
              <span class="auth-brand-name">作业管理系统</span>
            </div>

            <div class="auth-form-header">
              <span class="auth-form-badge">{{ badge }}</span>
              <h1 class="auth-form-title">{{ title }}</h1>
              <p class="auth-form-subtitle">{{ subtitle }}</p>
            </div>

            <slot />

            <div class="auth-mobile-copyright">{{ copyright }}</div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { BookOpen, UploadCloud, BarChart3 } from '@lucide/vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const props = withDefaults(
  defineProps<{
    /** 表单顶部徽标文案，如「账号密码登录」 */
    badge: string
    /** 表单主标题 */
    title: string
    /** 表单副标题 */
    subtitle: string
    /** 品牌栏与移动端底部显示的版权文案 */
    copyright?: string
  }>(),
  { copyright: '© 2026 作业管理系统' }
)

const features = [
  { icon: BookOpen, text: '课程作业管理' },
  { icon: UploadCloud, text: '在线提交批改' },
  { icon: BarChart3, text: '成绩统计分析' }
]
</script>
