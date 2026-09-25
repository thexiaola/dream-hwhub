# Dream HWHub 前端（front）

梦学簿作业管理系统的前端单页应用，基于 **Vue 3 + TypeScript + Vite + Element Plus**。

配合后端 Spring Boot 服务使用（后端默认运行在 `http://localhost:35010`，本前端开发服务器运行在 `http://localhost:5173` 并将 `/api` 代理到后端）。

## 技术栈

| 类别 | 技术 |
| ---- | ---- |
| 框架 | Vue 3.5（`<script setup>` 单文件组件）+ TypeScript |
| 构建 | Vite 8（`@vitejs/plugin-vue`） |
| UI 组件库 | Element Plus 2.14（中文语言包 `zh-cn`） |
| 状态管理 | Pinia 3 |
| 路由 | Vue Router 5（`createWebHistory`） |
| 图标 | Lucide（`@lucide/vue`） |
| HTTP | Axios 1.16（统一拦截器） |
| 类型检查 | vue-tsc 3 |

## 快速开始

```bash
cd front
npm install

# 开发（热更新），默认 http://localhost:5173
npm run dev

# 生产构建，产物输出到 dist/
npm run build

# 本地预览生产构建
npm run preview
```

### 与后端的联调

开发服务器已在 `vite.config.ts` 中配置代理：

```ts
server: {
  port: 5173,
  proxy: {
    "/api": { target: "http://localhost:35010", changeOrigin: true, secure: false }
  }
}
```

请确保后端已在 `35010` 端口运行，否则接口会返回 404 / 连接失败。若后端端口不同，修改上述 `target` 即可。

## 目录结构

```
front/
├── src/
│   ├── main.ts                 # 应用入口：挂载 Pinia / Router / Element Plus，预置主题
│   ├── App.vue                 # 根组件（含全局身份验证弹窗）
│   ├── style.css               # 全局样式与主题令牌（亮/暗）
│   ├── styles/
│   │   └── auth.css            # 登录/注册等认证页样式
│   ├── router/
│   │   └── index.ts            # 路由表与登录/权限守卫
│   ├── stores/                 # Pinia 状态
│   │   ├── user.ts             # 当前用户、登录态、权限节点判断
│   │   ├── school.ts           # 我的学校与当前学校
│   │   ├── message.ts          # 站内信未读等
│   │   └── friend.ts           # 好友与私信未读等
│   ├── composables/            # 组合式逻辑
│   │   ├── useSensitiveVerification.ts   # 危险操作「红色警示框 + 身份验证」统一入口
│   │   ├── useTheme.ts                    # 亮/暗主题切换（data-theme）
│   │   ├── useConfirmBeforeApprove.ts
│   │   └── useDraggableIndicator.ts
│   ├── components/             # 通用组件
│   │   ├── Layout.vue          # 主框架（页头 / 侧边导航 / 学校切换）
│   │   ├── AuthShell.vue       # 认证页外壳
│   │   ├── SensitiveVerifyDialog.vue # 危险操作身份验证弹窗
│   │   ├── UserAvatar.vue
│   │   ├── SlideSegmented.vue
│   │   ├── ThemeToggle.vue
│   │   └── work/               # 作业/题目/考试编辑相关组件
│   ├── views/                  # 页面（按角色分包）
│   │   ├── Login.vue / Register.vue / Retrieve.vue   # 认证
│   │   ├── Courses.vue         # 「课程」入口（我听的课 / 我教的课）
│   │   ├── Profile.vue         # 个人中心（资料、改密、危险操作验证）
│   │   ├── student/            # 学生端：课程详情、作业、考试作答
│   │   ├── teacher/            # 教师端：班级详情、成员、作业编辑与批改
│   │   ├── school/             # 学校：中心、详情、管理页
│   │   ├── message/            # 站内信、好友、私信
│   │   └── admin/              # 管理面板：用户、权限组、学校、消息策略
│   ├── constants/
│   │   └── sensitiveOperations.ts # 敏感操作标识（与后端一一对应）
│   ├── types/                  # TypeScript 类型（按域拆分）
│   └── utils/
│       ├── http.ts             # Axios 封装：自动附加 JWT 与 CSRF Token
│       ├── format.ts           # 日期 / 文件大小格式化
│       ├── attachment.ts       # 附件 URL 与缓存
│       └── status.ts           # 状态码 → 文案映射
├── vite.config.ts              # Vite 配置（别名 @、开发端口、/api 代理）
├── tsconfig*.json              # TS 配置（strict，noUnusedLocals / noUnusedParameters）
└── package.json
```

## 路由与访问守卫

| 路径 | 页面 | 说明 |
| ---- | ---- | ---- |
| `/login`、`/register`、`/retrieve` | 认证 | 公开 |
| `/courses/:tab?` | 课程 | `student`（我听的课）/ `teacher`（我教的课） |
| `/student/course/:id`、`/student/work/:id` | 学生课程详情 / 作业 | 需登录 |
| `/student/exam/:id` | 考试作答 | 与作业详情分离，承载反作弊（强制全屏 / 切屏检测 / 字体映射） |
| `/teacher/course/:id` | 班级详情 | 需成员/管理员/本校老师；允许冻结班级的原创建者查看 |
| `/teacher/work/:id/edit`、`/teacher/work/:id/submissions` | 作业编辑 / 批改 | 需学校老师或平台管理员 |
| `/school`、`/school/:id` | 学校中心 / 详情 | 需登录 |
| `/school/:id/manage` | 学校管理独立页 | 仅平台管理员或该校学校管理员 |
| `/profile` | 个人中心 | 需登录 |
| `/messages`、`/friends`、`/private-messages` | 站内信 / 好友 / 私信 | 按学校隔离 |
| `/admin/panel/:tab?` | 管理面板 | 需管理权限（`requiresAdmin`） |

**守卫规则**（`router/index.ts`）：

- `meta.requiresAuth`：未登录跳登录页；无用户信息时拉取一次，失败则清理登录态
- `meta.requiresAdmin`：非管理用户跳回课程页
- `meta.requiresSchoolTeacher`：进入前强制刷新学校身份（`fetchMySchools(true)`），学校老师或平台管理员放行——避免身份被收回后用旧缓存进入
- 旧链接兼容：`/student/courses`、`/teacher/courses` 重定向到合并后的 `/courses/:tab`

## HTTP 与鉴权

`src/utils/http.ts` 封装了 Axios 实例，统一处理：

- **JWT**：从 `localStorage` 读取 `token`，自动附加 `Authorization: Bearer <token>`
- **CSRF**：对写操作（POST / PUT / DELETE / PATCH）基于 JWT 用 HMAC-SHA256 生成 `X-CSRF-Token`
- **会话过期**：仅在携带凭证的请求收到 401 时才视为登录过期——清除 Token 并派发 `auth-expired` 事件跳转登录页；未携带凭证的公开接口（登录 / 找回密码 / 注册）收到 401 按业务失败原样返回

导出方法：`get` / `post` / `put` / `del` / `patch` / `postForm` / `putForm`，均支持可选 `headers`（危险操作传入身份验证请求头）。

## 危险操作与身份二次验证

`composables/useSensitiveVerification.ts` 提供全局单例的验证流程：

- `confirmDangerousOperation({ title, message, confirmText?, operationName?, operationKey? })`：**高危操作统一入口**，先弹红色警示框（不透明深红、禁用点遮罩关闭），再按需弹出身份验证弹窗，返回请求头或 `null`（用户取消）
- `requireSensitiveVerification(opName, operationKey?)`：仅做身份验证（用于已有自定义确认弹窗的流程）
- **按操作跳过**：若用户已在个人中心关闭某操作的二次验证（`operationKey` 命中 `userInfo.disabledVerificationOperations`），则跳过验证弹窗直接放行；传数组表示复合操作，须全部关闭才跳过

`operationKey` 取值见 `constants/sensitiveOperations.ts`，与后端 `SensitiveOperations` 一一对应。

`components/SensitiveVerifyDialog.vue` 全局挂载于 `App.vue`；验证方式（登录密码 / 邮箱验证码）依据 `userInfo` 的 `verifyByPassword`、`verifyByEmailCode` 决定可选项。

## 主题（亮 / 暗）

- 通过 `html[data-theme="light|dark"]` 切换，主题令牌集中定义在 `src/style.css`
- `composables/useTheme.ts` 管理切换：写入 `data-theme`、持久化到 `localStorage`（key: `theme`），并在挂载前预置以避免首屏闪烁
- 组件内**避免硬编码颜色**，应使用全局主题令牌，确保亮暗主题一致

## 约定与注意事项

- 路由页面按需异步加载（`() => import(...)`），管理面板各模块进一步分包
- `tsconfig` 开启 `noUnusedLocals` / `noUnusedParameters`，未使用的变量或导入会导致 `npm run build` 失败
- 前端权限（页签 / 按钮显隐）仅作体验优化，**安全边界在后端**
- 列表页需要筛选 / 分页 / 排序时，参数下推到后端查询，不在前端拉全量再过滤
- 手机端与电脑端均需支持：窄屏下页头纵向堆叠、表单纵向堆叠、表格操作列防溢出

## 环境变量

前端**不强制需要** `.env` 文件；接口地址通过 Vite 开发代理（`/api`）转发。若需自定义，可在 `front/` 下创建 `.env`（已被 `.gitignore` 忽略）。

## 相关文档

- 项目总览：[../README.md](../README.md)
- 后端 API 文档：[../backend/ApiList.md](../backend/ApiList.md)
