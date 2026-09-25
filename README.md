# Dream HWHub - 梦学簿

Dream HWHub（梦学簿）是一个面向学校场景的现代化作业管理系统，采用**前后端分离 + 模块化分层架构**。

- **后端**（`backend/`）：Spring Boot 4 + MyBatis-Plus + MySQL，提供 REST API
- **前端**（`front/`）：Vue 3 + TypeScript + Vite + Element Plus，单页应用

系统覆盖用户认证、学校与班级组织、作业与考试、题目与评分、好友私信、权限后台与敏感操作二次验证等完整能力。

## 项目架构

### 后端模块（`backend/src/main/java/top/thexiaola/dreamhwhub/`）

**公共与支撑**

- `common`：通用能力
  - `api`：统一响应封装（`ApiResponse`）
- `config`：配置层
  - `security`：`AuthInterceptor`（JWT 认证）、`PermissionInterceptor`（权限节点校验）、`CsrfFilter`（CSRF 校验）
  - `DatabaseInitializer`：启动时自动建表 / 同步字段
  - `GlobalExceptionHandler`：全局异常处理
  - `LogSystem` / `MailConfig` / `MybatisPlusConfig` / `MyMetaObjectHandler` / `WebConfig`
- `support`：技术支撑
  - `jwt`：令牌生成与解析
  - `password`：BCrypt 密码加密
  - `security`：敏感操作二次验证（注解、拦截器、校验器、操作注册表、按用户设置）
  - `session`：当前用户上下文（`UserUtils`）
  - `storage`：头像等文件存储
  - `validation`：文件上传校验、XSS 校验
  - `logging`：日志工具
- `enums` / `exception`：业务错误码与业务异常

**业务模块**

- `module/login`：注册、登录、找回密码、资料与头像、改密改邮、账号注销、邮箱验证码、敏感操作验证设置
- `module/school`：学校、学校成员（学工号 / 姓名 / 老师·学生·学校管理员角色）、加入申请
- `module/work_management`：班级、成员与邀请、作业、结构化题目与作答、考试与反作弊、作业提交与批改
- `module/permission`：权限节点注册表、权限组、用户-组 / 用户-节点绑定
- `module/admin`：后台用户管理
- `module/message`：站内信、好友私信、陌生消息配额与消息策略
- `module/file_management`：文件下载

### 前端结构（`front/src/`）

- `views/`：页面（登录注册、课程、学校、个人中心、消息、好友私信、管理面板等）
- `components/`：通用组件（如身份验证弹窗 `SensitiveVerifyDialog`）
- `composables/`：可复用逻辑（如 `useSensitiveVerification`）
- `stores/`：Pinia 状态（`user` / `school` / `message` / `friend`）
- `router/`：路由与登录守卫
- `utils/`：HTTP 封装（自动附加 JWT 与 CSRF Token）、格式化工具

## 功能特性

### 用户与认证
- 注册、登录、邮箱验证码、找回密码
- 个人资料与头像管理、修改密码、换绑邮箱
- **账号注销**：凭登录密码验证所有者身份，自动退出全部班级与学校、软删作业提交并清理关联数据
- JWT 认证 + CSRF 防护；请求鉴权会回库确认账号仍存在且未被封禁

### 学校与班级
- **学校**：创建、加入申请与审核、成员管理（角色：学校管理员 / 老师 / 学生）、学工号与姓名
- **班级**：创建、邀请码加入、加入申请审核、成员管理、课代表（助理老师）、所有权转让
- **25 位邀请码**：大小写字母 + 数字，安全强度显著提升
- **班级接管**：班级创建者失去教师身份时班级冻结，本校其他老师可申请接管（学校级可配置自动同意）
- **独立学校管理页**：学校管理员可从多个入口进入整页 `/school/:id/manage` 进行管理

### 作业、题目与考试
- **作业**：发布、更新、删除、附件、截止时间、分页查询
- **结构化题目**：单选 / 多选 / 判断 / 填空 / 主观 / 附加；客观题提交即自动评判，主观与附加题由老师手动评分，支持参考答案与解析
- **考试**：与作业共用表与评分链路（`work_type` 区分），支持反作弊：
  - 字体映射（打乱字体池 + 服务端打乱文本，防复制搜题）
  - 强制全屏答题、切屏 / 离开页面检测计数、禁止复制、限时
  - 违规达到设定次数后强制交卷
- **批改**：教师可重新批改、打回修改、恢复被打回作业；提交与更新响应不含批改字段，查询时才返回完整信息

### 消息与社交
- 站内信、好友（按学校隔离）、私信
- 未加好友可发私信，陌生消息有默认配额（可被平台 / 校管调整）

### 权限与后台
- LuckPerms 风格权限节点 + 权限组，平台管理员（OP）拥有全部节点
- 管理面板：用户管理、权限组与节点、学校管理、消息策略
- 前端页签 / 按钮按权限显隐，后端强制校验

### 安全
- **敏感操作二次验证**：解散班级、退出 / 解散学校、踢出成员、转让班级、撤回提交，以及平台管理员的高危 / 权限类操作
  - 验证方式由**用户自选**：登录密码与 / 或邮箱验证码（可组合为四种模式）
  - 用户可**逐个操作**关闭 / 开启二次验证；仅展示本人有权限执行的操作
  - 失去操作权限时对应开关自动隐藏并重置为默认（启用验证）
  - 高危操作统一「红色警示框 + 身份验证」成对出现

## 技术栈

### 后端
- **核心框架**：Spring Boot 4.1.1
- **架构模式**：模块化分层（Controller–Service–Mapper）
- **ORM**：MyBatis-Plus 3.5.16（含 `mybatis-plus-jsqlparser`）
- **数据库**：MySQL 9.5.0
- **认证**：JWT（jjwt 0.12.6）
- **密码**：BCrypt（Spring Security Crypto）
- **邮件**：Spring Boot Mail Starter
- **工具库**：Hutool 5.8.44、MapStruct
- **日志**：Logback + 自定义滚动策略
- **构建**：Gradle 9.5.0
- **Java**：JDK 26

### 前端
- **框架**：Vue 3.5 `<script setup>` + TypeScript
- **构建**：Vite 8
- **UI**：Element Plus 2.14
- **状态**：Pinia 3
- **路由**：Vue Router 5
- **图标**：Lucide（`@lucide/vue`）
- **HTTP**：Axios（统一拦截器附加 JWT 与 CSRF Token）

## 开发环境要求

- **Java**：JDK 26 或更高版本
- **数据库**：MySQL 9.5.0 或兼容版本
- **Node.js**：支持 Vite 8 的版本（建议 Node 20+）
- **构建工具**：Gradle 9.5.0 或更高版本（或使用仓库内 `gradlew`）
- **操作系统**：Linux / macOS / Windows（推荐 Linux）

## 快速开始

### 1. 克隆项目

```bash
git clone https://gitee.com/thexiaola/dream-hwhub.git
cd dream-hwhub
```

### 2. 后端配置与启动

#### 2.1 数据库

确保 MySQL 服务正在运行。项目**启动时自动建表并同步字段与索引**，无需手动执行 SQL。如需手动初始化：

```bash
mysql -u root -p < backend/src/main/resources/user_schema.sql
```

修改 `backend/src/main/resources/application.properties` 中的连接信息：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/dream_hwhub?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&allowMultiQueries=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password
```

#### 2.2 邮件服务

邮件配置文件已加入 `.gitignore` 保护，从示例复制后填写：

```bash
cp backend/src/main/resources/mail-config.properties.example backend/src/main/resources/mail-config.properties
```

```properties
spring.mail.host=smtp.qq.com
spring.mail.port=465
spring.mail.username=your-email@qq.com
spring.mail.password=your-app-password
spring.mail.properties.mail.from.nickname=梦学簿系统
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.ssl.enable=true
```

- `spring.mail.password` 为邮箱授权码（非登录密码）

#### 2.3 启动后端

```bash
cd backend
./gradlew bootRun
```

后端将在 `http://localhost:35010` 上运行（端口见 `application.properties` 的 `server.port`）。

### 3. 前端配置与启动

```bash
cd front
npm install
npm run dev      # 开发服务器：http://localhost:5173
```

前端开发服务器已将 `/api` 代理到 `http://localhost:35010`（见 `front/vite.config.ts`）。

生产构建：

```bash
npm run build    # 产物输出到 front/dist
```

## 详细 API 文档

完整的 API 接口文档请参考 [backend/ApiList.md](backend/ApiList.md)。

## 项目结构

```
.
├── backend/                                    # Spring Boot 后端
│   ├── src/main/java/top/thexiaola/dreamhwhub/
│   │   ├── common/                             # 通用（统一响应）
│   │   ├── config/                            # 配置层
│   │   │   ├── security/                       # 认证 / 权限 / CSRF 拦截
│   │   │   ├── DatabaseInitializer.java        # 自动建表与字段同步
│   │   │   ├── GlobalExceptionHandler.java     # 全局异常处理
│   │   │   ├── LogSystem.java                  # 日志系统配置
│   │   │   ├── MailConfig.java                 # 邮件服务配置
│   │   │   ├── MybatisPlusConfig.java          # MyBatis-Plus 配置
│   │   │   ├── MyMetaObjectHandler.java        # 自动填充
│   │   │   └── WebConfig.java                  # Web 配置
│   │   ├── support/                            # 技术支撑
│   │   │   ├── jwt/                            # JWT 工具
│   │   │   ├── password/                       # BCrypt 密码工具
│   │   │   ├── security/                       # 敏感操作二次验证
│   │   │   ├── session/                        # 会话 / 用户上下文
│   │   │   ├── storage/                        # 文件存储
│   │   │   ├── validation/                     # 上传 / XSS 校验
│   │   │   ├── logging/                        # 日志工具
│   │   │   └── mapper/                         # 对象映射（MapStruct）
│   │   ├── module/                             # 业务模块
│   │   │   ├── login/                          # 注册登录、资料、安全设置
│   │   │   ├── school/                         # 学校与成员
│   │   │   ├── work_management/                # 班级、作业、题目、考试
│   │   │   ├── permission/                     # 权限节点与权限组
│   │   │   ├── admin/                          # 后台用户管理
│   │   │   ├── message/                        # 站内信、好友私信
│   │   │   └── file_management/                # 文件下载
│   │   ├── enums/                              # 业务错误码
│   │   ├── exception/                          # 业务异常
│   │   └── DreamHwhubApplication.java          # 主启动类
│   ├── src/main/resources/
│   │   ├── application.properties              # 应用主配置
│   │   ├── logback-spring.xml                  # 日志配置
│   │   ├── mail-config.properties.example      # 邮件配置示例
│   │   ├── exam-font/                          # 考试字体映射池
│   │   ├── user_schema.sql                     # 用户表
│   │   ├── school_schema.sql                   # 学校 / 成员 / 加入申请
│   │   ├── class_schema.sql                    # 班级 / 成员 / 邀请 / 接管
│   │   ├── work_schema.sql                     # 作业 / 题目 / 作答 / 考试
│   │   ├── permission_schema.sql               # 权限组与权限节点
│   │   ├── message_schema.sql                  # 站内信
│   │   ├── friend_schema.sql                   # 好友 / 私信 / 配额 / 策略
│   │   └── sensitive_operation_schema.sql      # 敏感操作验证设置
│   └── ApiList.md                              # 后端 API 文档
├── front/                                      # Vue 3 前端
│   ├── src/
│   │   ├── views/                              # 页面
│   │   ├── components/                         # 组件
│   │   ├── composables/                        # 组合式逻辑
│   │   ├── stores/                             # Pinia 状态
│   │   ├── router/                             # 路由
│   │   ├── types/                              # TS 类型
│   │   └── utils/                              # HTTP / 格式化工具
│   ├── vite.config.ts                          # Vite 配置（含 /api 代理）
│   └── package.json
├── attachments/                                # 上传附件存储
├── LICENSE                                     # 许可证
└── README.md                                   # 项目说明
```

## 数据库管理

### 自动建表（推荐）

项目启动时由 `DatabaseInitializer` 自动创建表并同步字段与索引，无需手动执行脚本。建表脚本按模块拆分在 `backend/src/main/resources/` 下，并在 `application.properties` 的 `spring.sql.init.schema-locations` 中统一登记：

- `user_schema.sql`：`user` 表 —— 用户基本信息（用户名、邮箱、手机号、头像、BCrypt 密码、平台管理员 `is_op`、封禁状态、危险操作验证方式开关），用户名 / 邮箱唯一
- `school_schema.sql`：`school` / `school_member` / `school_join_application` —— 学校、成员（学工号与姓名、角色）与加入申请
- `class_schema.sql`：`class_info` / `class_member` / `class_invitation` / `class_user_invitation` / `class_teacher_approval` / `class_join_application` / `class_takeover_application` —— 班级、成员、邀请、审核与接管
- `work_schema.sql`：`work_info` / `work_attachment` / `work_submission` / `work_submission_attachment` / `work_question` / `work_answer` / `exam_session` / `exam_violation` —— 作业、附件、提交、结构化题目与作答、考试会话与违规记录
- `permission_schema.sql`：`permission_group` / `permission_group_node` / `user_permission_group` / `user_permission_node` —— 权限组、组-节点绑定、用户-组绑定、用户直接节点
- `message_schema.sql`：`site_message` —— 站内信
- `friend_schema.sql`：`user_friend` / `private_message` / `stranger_message_quota` / `message_policy` —— 好友、私信、陌生消息配额与消息策略
- `sensitive_operation_schema.sql`：`user_sensitive_operation` —— 用户级「按操作」的二次验证设置（仅存被关闭的操作）

- 统一使用 UTF-8 字符集，确保中文内容正确存储

## 权限体系与管理员后台

### 权限模型（LuckPerms 风格）

- **权限节点**：由后端内置注册表定义（`PermissionNodes` / `PermissionRegistry`），命名形如 `user:add`、`permission:group:assign`、`class:dissolve`、`school:dissolve`。节点不支持运行时新增，避免无效节点。
- **权限组**：一组权限节点的集合，可标记为「默认组」，新注册用户自动加入。
- **直接授权**：也可直接为单个用户授予权限节点。
- **平台管理员（OP）**：`user.is_op = 1` 的用户拥有全部权限节点，不受权限组限制。

用户最终权限 = OP（全部节点）或 用户直接节点 ∪ 所属权限组节点。

### 越权防护

- 非平台管理员**只能授予自己已拥有的权限节点**：配置权限组节点、授予用户节点、为用户分配权限组时均会校验，避免通过编辑权限组自我提权。
- **平台管理员身份只能由平台管理员授予或取消**，且不能作用于自己。
- **不能修改自己的权限组与平台管理员身份**，也不能删除自己的账号。
- 请求鉴权会回库确认账号仍存在且未被封禁，账号被删除或封禁后原 Token 立即失效。
- 后端由 `PermissionInterceptor` 对 `/api/admin/**` 强制校验权限节点；前端页签 / 按钮显隐仅作体验优化，不作为安全边界。

### 管理员引导

- 平台首次部署（系统内还没有其他用户）时，**第一个注册的用户会自动成为平台管理员（OP）**，随后即可在管理面板中配置用户与权限组。
- 也可以直接指定 OP：

  ```sql
  UPDATE `user` SET `is_op` = 1 WHERE `username` = '你的用户名';
  ```

### 管理面板

管理员可在「管理面板」中按权限节点看到对应页签：

- **用户管理**：新增 / 编辑 / 删除用户、封禁解封、设置或取消平台管理员、为用户分配权限
- **权限组**：创建 / 编辑 / 删除权限组，为权限组勾选权限节点
- **学校管理**：查看、创建、修改、解散学校，指派 / 取消学校管理员
- **班级管理 / 加入申请**：跨班级管理能力由 `class:*` 权限节点控制
- **消息策略**：配置陌生消息配额等

接口清单详见 [backend/ApiList.md](backend/ApiList.md)。

## 安全与认证

### JWT 认证

系统采用基于 Token 的 JWT 认证：

- **登录**：校验账号密码后签发 JWT，前端保存在本地并在后续请求头 `Authorization: Bearer <token>` 中携带
- **校验**：`AuthInterceptor` 解析并校验 Token，回库确认账号状态，将当前用户写入请求属性
- **公开接口**：登录、注册、获取注册验证码、找回密码发送 / 重置等，无需登录
- **统一响应**：未认证请求返回 401 与标准错误格式

### CSRF 防护

- 登录后，写操作（POST / PUT / DELETE / PATCH）需携带 CSRF Token
- 前端 `http.ts` 拦截器基于 Token 自动生成 `X-CSRF-Token`；后端 `CsrfFilter` 在认证之前校验

### 密码安全

- **BCrypt** 哈希存储（`support/password/PasswordUtil`），不可逆，抗彩虹表
- 密码字段仅允许写入、禁止序列化输出，且不进入 `toString`

### 验证码机制

- 邮箱验证码有效期 10 分钟（可配置）
- 发送冷却时间（可配置），防止暴力破解与滥用
- 集成邮件服务，支持多种邮箱提供商

### 敏感操作二次验证

高危操作（解散班级、退出 / 解散学校、踢出成员、转让班级、撤回提交，以及平台管理员的高危 / 权限类操作）在执行前要求再次验证身份：

- **验证方式**：登录密码与 / 或邮箱验证码，由用户在「个人中心 → 危险操作验证」自行开关，组合出四种模式
- **按操作设置**：用户可逐个操作开启 / 关闭二次验证；只展示本人当前有权限执行的操作
- **权限联动**：用户失去某操作权限（如老师资格被取消）时，对应开关自动隐藏并重置为默认（启用验证）
- **统一交互**：所有高危操作统一「红色警示框 + 身份验证」成对出现
- **接口约定**：二次验证失败返回 400（错误码 6100–6105），绝不返回 401，避免前端把「密码输错」误判为登录过期

## 日志系统

项目采用自定义日志滚动策略：

### 日志文件命名

```
logs/log_{日期}_{启动次数}_{文件序号}.log
```

**示例**：`logs/log_20260223_1_1.log`

### 滚动策略

- **时间滚动**：每日自动切换到新日期文件
- **大小滚动**：单文件超过 50MB 时自动滚动
- **启动检测**：应用重启时自动递增启动次数

### 特性

- 异步日志处理，提升性能
- 控制台彩色日志显示
- 完整的操作追踪记录
- 自动清理过期日志文件

## 故障排除

### 常见问题

1. **登录 Token 失效 / 401**
   - 确认已登录且 Token 未过期
   - 账号被删除或封禁后原 Token 会立即失效，需重新登录

2. **CSRF 校验失败（写操作被拒）**
   - 确认请求头携带了 `X-CSRF-Token`
   - 前端通过 `utils/http.ts` 自动生成，直接调接口时需自行附带

3. **权限验证失败**
   - 确认用户已登录且拥有对应权限节点
   - 平台管理员（OP）拥有全部节点，可直接放行

4. **数据库连接失败**
   - 检查 MySQL 服务是否运行
   - 验证 `application.properties` 中的连接配置与账号权限

5. **邮件发送失败**
   - 检查 `mail-config.properties` 是否存在
   - 验证 SMTP 配置与邮箱授权码正确性

6. **端口占用问题**

   ```bash
   # 后端默认端口 35010，前端开发端口 5173
   lsof -i :35010
   kill -9 <PID>
   ```

7. **前端请求 404 / 跨域**
   - 确认后端已在 `35010` 运行
   - 确认 `front/vite.config.ts` 中 `/api` 代理指向正确

8. **启动失败**
   - 检查 Java 版本是否符合要求（JDK 26+）
   - 验证必需的配置文件是否存在
   - 查看详细错误日志

## 许可证

本项目采用 [AGPL-3.0](LICENSE) 许可证。
