# Dream HWHub API 接口文档

梦学簿作业管理系统后端 REST API 文档。后端基于 Spring Boot 4 + MyBatis-Plus，统一前缀 `/api`。

## 项目架构

后端采用模块化分层设计（Controller–Service–Mapper），主要包含：

- **common**：公共模块（统一响应封装等）
- **support**：技术支撑（JWT、密码、敏感操作验证、会话、文件存储、上传校验、日志、对象映射）
- **config**：配置层（认证 / 权限 / CSRF 拦截器、数据库初始化、异常处理、邮件、日志）
- **module/login**：用户认证与账号（注册、登录、找回密码、资料与头像、安全验证设置）
- **module/school**：学校、学校成员与加入申请
- **module/work_management**：班级、作业、结构化题目、考试、作业提交
- **module/permission**：权限节点、权限组与用户授权
- **module/admin**：后台用户管理
- **module/message**：站内信、好友私信、消息策略
- **module/file_management**：文件下载

---

## 目录

- [通用说明](#通用说明)
- [1. 用户认证与账号（LoginUser / Register / Retrieve）](#1-用户认证与账号loginuserregisterretrieve)
- [2. 资料与安全验证设置（ModifyUser / SensitiveVerification / SensitiveOperationSettings）](#2-资料与安全验证设置modifyusersensitiveverificationsensitiveoperationsettings)
- [3. 学校（SchoolController）](#3-学校schoolcontroller)
- [4. 班级（ClassController）](#4-班级classcontroller)
- [5. 作业与题目（WorkController / WorkQuestionController）](#5-作业与题目workcontrollerworkquestioncontroller)
- [6. 作业提交（WorkSubmissionController）](#6-作业提交worksubmissioncontroller)
- [7. 考试（ExamController）](#7-考试examcontroller)
- [8. 消息（SiteMessage / PrivateMessage / Friend / MessagePolicy）](#8-消息sitemessageprivatemessagefriendmessagepolicy)
- [9. 管理员后台（AdminUser / AdminPermission / AdminSchool）](#9-管理员后台adminuseradminpermissionadminschool)
- [10. 文件（FileController）](#10-文件filecontroller)
- [附录](#附录)

---

## 通用说明

### 认证方式

除公开接口外，所有接口需登录认证，采用 **JWT Token**。

**请求头要求**：

- `Authorization`: `Bearer <jwt_token>` — 所有需认证接口必需
- `X-CSRF-Token`: `<csrf_token>` — 写操作（POST / PUT / DELETE / PATCH）必需

**获取 Token 流程**：

1. 调用 `POST /api/users/login` 获取 JWT Token
2. 基于 JWT Token 生成 CSRF Token（HMAC-SHA256 + Base64URL 去填充）
3. 后续请求在 Header 中携带两个 Token

**CSRF Token 生成示例**（前端）：

```javascript
// 使用 HMAC-SHA256 对 JWT 计算签名，Base64URL 编码并去除填充
async function generateCsrfToken(jwtToken) {
  const key = await crypto.subtle.importKey(
    "raw", new TextEncoder().encode(JWT_SECRET),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign"]
  );
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(jwtToken));
  return btoa(String.fromCharCode(...new Uint8Array(sig)))
    .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
```

**公开接口**（无需登录）：

- `POST /api/users/register`、`POST /api/users/getregcode`
- `POST /api/users/login`
- `POST /api/users/retrieve/sendcode`、`PUT /api/users/retrieve/resetpassword`

### 敏感操作二次验证

以下操作在执行前需进行身份二次验证（凭据通过请求头携带）。**是否有该操作权限、以及是否开启验证由用户设置决定**；若用户已关闭某操作的验证，则无需携带凭据。

**验证请求头**（二选一，取决于用户设置）：

- 登录密码：`X-Verify-Method: password` + `X-Verify-Password: <登录密码>`
- 邮箱验证码：`X-Verify-Method: email_code` + `X-Verify-Code: <验证码>`

**受保护的操作**：

| 操作 | 接口 |
| ---- | ---- |
| 解散班级 | `DELETE /api/class/{classId}` |
| 踢出班级成员 | `DELETE /api/class/{classId}/members/batch` |
| 退出班级 | `DELETE /api/class/{classId}/members/me` |
| 转让班级 | `PUT /api/class/{classId}/owner` |
| 退出学校 | `DELETE /api/school/{schoolId}/membership` |
| 解散学校 | `DELETE /api/admin/schools/{schoolId}` |
| 指派学校管理员 | `PUT /api/admin/schools/{schoolId}/admin` |
| 删除权限组 | `DELETE /api/admin/permissions/groups/{groupId}` |
| 设置权限组节点 | `PUT /api/admin/permissions/groups/{groupId}/nodes` |
| 分配权限组 | `PUT /api/admin/users/{userId}/groups` |
| 分配权限节点 | `PUT /api/admin/users/{userId}/nodes` |
| 删除用户 | `DELETE /api/admin/users/{userId}` |
| 封禁/解封用户 | `PUT /api/admin/users/{userId}/ban` |
| 设置平台管理员身份 | `PUT /api/admin/users/{userId}/op` |
| 撤回提交 | `DELETE /api/submissions/{submissionId}` |

**约定**：二次验证失败一律返回 **HTTP 400**（错误码 6100–6105），**绝不返回 401**，避免前端把「凭据输错」误判为登录过期。

### 响应格式

所有接口统一返回 JSON：

**成功响应**：

```json
{ "code": 200, "message": "成功", "data": {} }
```

**失败响应**：

```json
{ "code": 400, "message": "错误信息", "data": null }
```

> 说明：业务错误码通过 `code` 字段表达；HTTP 状态码通常为 200（业务失败）或 400/401/403/500（鉴权与异常）。以 `code` 为准判断业务结果。

### 分页参数

多数列表接口支持 `/api/works` 与 `/api/submissions` 使用 `PageRequest`：

| 字段 | 类型 | 默认 | 说明 |
| ---- | ---- | ---- | ---- |
| pageNum | Integer | 1 | 页码（从 1 开始） |
| pageSize | Integer | 20 | 每页条数 |
| keyword | String | - | 关键字（部分接口支持） |

分页响应结构：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "records": [],
    "total": 100,
    "size": 20,
    "current": 1
  }
}
```

### 完整错误码

| code | 含义 |
| ---- | ---- |
| 0 | 操作成功 |
| 1001 / 1002 / 1003 | 验证码无效 / 已过期 / 不存在 |
| 2002 / 2003 / 2004 | 用户名已被占用 / 邮箱已被占用 / 注册失败 |
| 3001 / 3002 / 3003 / 3004 | 用户不存在 / 账号或密码错误 / 用户已被封禁 / 用户未登录 |
| 3005 | 账号注销受阻（仍持有需先解除的身份） |
| 4001 / 4003 / 4004 | 邮件发送失败 / 邮件服务器未配置 / 邮件发送失败 |
| 5000 / 5001 | 系统错误 / 数据库操作失败 |
| 6003 / 6004 / 6005 | 新邮箱不能与原邮箱相同 / 原密码错误 / 新密码不能与原密码相同 |
| 6100 | 该操作需要验证身份，请提供登录密码或邮箱验证码 |
| 6101 | 身份验证失败，请检查登录密码或邮箱验证码 |
| 6102 | 不支持的身份验证方式 |
| 6103 | 该验证方式已被关闭，请使用其他方式 |
| 6104 | 安全验证设置不合法 |
| 6105 | 该操作对你的账号不可用，无法配置其验证开关 |
| 7001 / 7002 / 7003 | 作业不存在 / 作业状态错误 / 已经提交过该作业 |
| 7004 / 7005 / 7006 | 提交记录不存在 / 作业已被批改不能修改 / 分数超过作业总分 |
| 7007 / 7008 / 7009 | 题目不存在 / 题型不合法 / 作答不完整或与题目不匹配 |
| 7010 / 7011 / 7012 / 7013 / 7014 / 7015 | 考试不存在 / 考试尚未开始 / 考试已结束 / 考试时间已到 / 考试状态不允许该操作 / 违规类型不合法 |
| 8001 / 8002 / 8003 / 8004 / 8005 | 文件上传失败 / 不允许的文件类型 / 文件大小超过限制 / 文件可能包含病毒 / 非法的文件路径 |
| 8501 / 8502 / 8503 / 8504 / 8505 | 班级不存在 / 班级已解散 / 你已经在该班级中 / 你不是该班级的成员 / 创建者不能退出班级 |
| 8506 / 8507 / 8508 / 8509 | 该学号在班级中已被占用 / 班级已冻结（老师失去身份）/ 该班级当前无需接管 / 你已提交过接管申请 |
| 8601 / 8602 / 8603 / 8604 / 8605 | 学校不存在 / 学校名称已被占用 / 你已经在该学校中 / 你不是该学校的成员 / 该学工号在该学校已被占用 |
| 8606 / 8607 | 学校下仍存在班级 / 只有学校老师才能创建班级 |
| 8701 / 8702 / 8703 / 8704 / 8705 | 只能添加同校好友 / 不能添加自己 / 你们已经是好友 / 已存在待处理的好友申请 / 好友关系不存在 |
| 8706 / 8707 / 8708 | 只能给同校用户发私信 / 陌生用户私信条数已达上限 / 不能给自己发送私信 |
| 9001 / 9002 / 9003 / 9004 / 9005 | 权限不足 / 缺少必要参数 / 参数错误 / 已有待处理的申请 / 已经是班级成员 |

---

## 1. 用户认证与账号（LoginUser / Register / Retrieve）

**基础路径**：`/api/users`

### 1.1 用户注册

**接口地址**：`POST /api/users/register`（公开）

**请求体**：

```json
{
  "username": "张三",
  "email": "zhangsan@example.com",
  "emailCode": "123456",
  "password": "Password@123"
}
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| username | String | 是 | 用户名，最长 64 字符 |
| email | String | 是 | 邮箱地址 |
| emailCode | String | 是 | 邮箱验证码 |
| password | String | 是 | 密码，8–32 位，需包含大小写字母和数字 |

**成功响应 (200)**：

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 1001,
    "username": "张三",
    "email": "zhangsan@example.com",
    "isOp": false,
    "permissions": [],
    "verifyByPassword": true,
    "verifyByEmailCode": false,
    "disabledVerificationOperations": []
  }
}
```

**可能的错误信息**：用户名不能为空 / 邮箱格式不正确 / 密码不符合要求 / 验证码错误或已过期 / 用户名已被占用 / 邮箱已被占用。

---

### 1.2 发送注册验证码

**接口地址**：`POST /api/users/getregcode`（公开）

**请求体**：

```json
{ "username": "张三", "email": "zhangsan@example.com" }
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| username | String | 是 | 用户名 |
| email | String | 是 | 邮箱地址 |

**成功响应**：`{ "code": 200, "message": "验证码已发送", "data": null }`

**注意**：验证码为 6 位数字，有效期 10 分钟；同一邮箱 60 秒内仅可发送一次（冷却时间可配置）。

---

### 1.3 用户登录

**接口地址**：`POST /api/users/login`（公开）

**请求体**：

```json
{ "account": "zhangsan@example.com", "password": "Password@123" }
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| account | String | 是 | 用户名或邮箱 |
| password | String | 是 | 密码 |

**成功响应 (200)**：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "id": 1001,
    "username": "张三",
    "email": "zhangsan@example.com",
    "phone": "13800000000",
    "avatar": null,
    "isOp": false,
    "permissions": ["class:update"],
    "verifyByPassword": true,
    "verifyByEmailCode": false,
    "disabledVerificationOperations": [],
    "isBanned": false,
    "registerTime": "2026-05-07T10:00:00",
    "lastLoginTime": "2026-05-08T09:00:00",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**失败**：账号或密码错误返回 HTTP 400 + `code: 3002`；账号被封禁返回 HTTP 403 + `code: 3003`（含封禁原因）。

> `permissions` 为该用户生效的权限节点（OP 返回全部节点）；`disabledVerificationOperations` 为已关闭二次验证的操作标识数组。

---

### 1.4 用户登出

**接口地址**：`POST /api/users/logout`

**成功响应**：`{ "code": 200, "message": "登出成功", "data": null }`

---

### 1.5 注销账号（不可逆）

**接口地址**：`DELETE /api/users/account`

凭登录密码验证账号所有者身份；成功后自动退出全部班级与学校、软删除其作业提交，并清理好友 / 私信 / 站内信 / 权限等关联数据。

**请求体**：

```json
{ "password": "Password@123" }
```

**注意**：平台管理员（OP）、学校管理员、班级创建者不可注销（返回 `code: 3005`），需先解除相应身份。

---

### 1.6 获取当前登录用户信息

**接口地址**：`GET /api/users/info`

**成功响应**：同登录响应的 `data`，但不含 `token`。

---

### 1.7 发送找回密码验证码

**接口地址**：`POST /api/users/retrieve/sendcode`（公开）

**请求体**：`{ "account": "zhangsan@example.com" }`（用户名或邮箱）

---

### 1.8 重置密码

**接口地址**：`PUT /api/users/retrieve/resetpassword`（公开）

**请求体**：

```json
{ "account": "zhangsan@example.com", "code": "123456", "newPassword": "NewPass@123" }
```

---

## 2. 资料与安全验证设置（ModifyUser / SensitiveVerification / SensitiveOperationSettings）

**基础路径**：`/api/users/modify`、`/api/users/sensitive-verification`、`/api/users/sensitive-operations`

### 2.1 修改用户信息

**接口地址**：`PUT /api/users/modify/info`

**请求体**：`{ "username": "李四", "phone": "13900000000" }`（手机号留空表示删除）

---

### 2.2 上传头像 / 清除头像

- `POST /api/users/modify/avatar`：`multipart/form-data`，字段 `file`
- `DELETE /api/users/modify/avatar`

响应为更新后的用户信息（`UserResponse`）。

---

### 2.3 修改邮箱（换绑）

**接口地址**：`PUT /api/users/modify/email`

**请求体**：

```json
{ "beforeCode": "111111", "newEmail": "new@example.com", "afterCode": "222222" }
```

| 字段 | 说明 |
| ---- | ---- |
| beforeCode | 原邮箱验证码 |
| newEmail | 新邮箱 |
| afterCode | 新邮箱验证码 |

---

### 2.4 发送换绑验证码

- `POST /api/users/modify/getmodifycode/before`：向**原邮箱**发送（无请求体）
- `POST /api/users/modify/getmodifycode/after`：向**新邮箱**发送，请求体 `{ "newEmail": "new@example.com" }`

响应：`data` 为冷却秒数（Integer）。

---

### 2.5 修改密码

**接口地址**：`PUT /api/users/modify/password`

**请求体**：`{ "oldPassword": "OldPass@1", "newPassword": "NewPass@2" }`

---

### 2.6 查询危险操作安全验证设置

**接口地址**：`GET /api/users/modify/security-verification`

**成功响应**：

```json
{ "code": 200, "message": "成功", "data": { "verifyByPassword": true, "verifyByEmailCode": false } }
```

---

### 2.7 更新危险操作安全验证设置

**接口地址**：`PUT /api/users/modify/security-verification`

变更本身是危险操作：**每个被改动的开关都必须用该方式自身的凭据验证身份**——改动密码验证需携带登录密码；改动邮箱验证码验证需携带邮箱验证码；两者都改动则两者都需提供。

**请求体**：

```json
{
  "verifyByPassword": true,
  "verifyByEmailCode": true,
  "password": "Password@123",
  "emailCode": "123456"
}
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| verifyByPassword | Boolean | 是 | 目标：是否启用密码验证 |
| verifyByEmailCode | Boolean | 是 | 目标：是否启用邮箱验证码验证 |
| password | String | 条件 | 改动了「密码验证」开关时必填 |
| emailCode | String | 条件 | 改动了「邮箱验证码验证」开关时必填 |

**四种组合**：都关 → 不验证；仅邮箱 → 只邮箱验证码；仅密码 → 只密码；都开 → 任选其一。

---

### 2.8 发送敏感操作身份验证码

**接口地址**：`POST /api/users/sensitive-verification/code`

向当前用户绑定邮箱发送验证码（供危险操作二次验证使用）。响应 `data` 为冷却秒数。

---

### 2.9 查询可配置的敏感操作

**接口地址**：`GET /api/users/sensitive-operations`

返回**仅当前用户可用**（有权限执行）的操作列表。

**成功响应**：

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    { "key": "class.dissolve", "name": "解散班级", "description": "解散你创建或有权管理的班级，数据不可恢复", "enabled": true, "available": true },
    { "key": "class.kick_member", "name": "踢出班级成员", "description": "将成员移出你管理的班级并清理其作业提交", "enabled": true, "available": true }
  ]
}
```

| 字段 | 说明 |
| ---- | ---- |
| key | 操作标识（稳定，与后端 `SensitiveOperations` 对应） |
| name / description | 名称与说明 |
| enabled | 是否需要二次验证（默认 true） |
| available | 当前用户是否可用（不可用则不展示开关） |

**全部操作标识**：`class.dissolve`、`class.kick_member`、`class.leave`、`class.transfer`、`school.leave`、`school.dissolve`、`school.assign_admin`、`permission.group.delete`、`permission.group.set_nodes`、`permission.group.assign`、`permission.user.assign`、`user.delete`、`user.ban`、`user.set_op`、`submission.withdraw`。

---

### 2.10 更新敏感操作验证开关

**接口地址**：`PUT /api/users/sensitive-operations`

逐项设置哪些操作需要二次验证（仅提交改动的项）。后端会校验每个操作对当前用户是否可用，不可用或未登记返回 `code: 6105`。

**请求体**：

```json
{ "settings": [ { "key": "class.dissolve", "enabled": false } ] }
```

**响应**：更新后的完整操作列表（同 2.9）。

---

## 3. 学校（SchoolController）

**基础路径**：`/api/school`

### 3.1 查询学校列表（分页）

**接口地址**：`GET /api/school/list`

| 参数 | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| keyword | String | 否 | 学校名称关键字 |
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 10 |

响应：`Page<SchoolVO>`。`SchoolVO` 字段：`id`、`schoolName`、`description`、`allowJoinWithoutApproval`、`autoApproveClassTakeover`、`memberCount`、`createTime`。

---

### 3.2 查询我加入的学校

**接口地址**：`GET /api/school/mine`

| 参数 | 类型 | 说明 |
| ---- | ---- | ---- |
| minRoleCode | Integer | 角色下限（如 1 表示只要老师及以上） |
| keyword | String | 学校名称关键字 |

响应：`List<SchoolDetailResponse>`。

`SchoolDetailResponse` 字段：`id`、`schoolName`、`description`、`allowJoinWithoutApproval`、`autoApproveClassTakeover`、`memberCount`、`adminCount`、`teacherCount`、`studentCount`、`classCount`、`createTime`、`member`、`myRoleCode`、`myRole`、`myStaffNo`、`myRealName`、`myApplicationStatus`、`pendingApplicationCount`、`myApplicationComment`。

---

### 3.3 查询学校详情

**接口地址**：`GET /api/school/{schoolId}` → `SchoolDetailResponse`

---

### 3.4 申请加入学校

**接口地址**：`POST /api/school/{schoolId}/join`

**请求体**：`{ "realName": "张三", "staffNo": "2024001" }`

响应：`SchoolJoinApplicationResponse`（含 `status`，若学校免审核则直接为已加入）。

---

### 3.5 查询我的加入申请

**接口地址**：`GET /api/school/{schoolId}/my-application` → `SchoolJoinApplicationResponse`

---

### 3.6 查询加入申请列表

**接口地址**：`GET /api/school/{schoolId}/applications`（学校管理员）

| 参数 | 类型 | 说明 |
| ---- | ---- | ---- |
| status | Integer | 申请状态筛选 |
| pageNum / pageSize | Integer | 分页 |

响应：`Page<SchoolJoinApplicationResponse>`（字段：`id`、`schoolId`、`schoolName`、`applicantId`、`applicantUsername`、`applicantName`、`applicantNo`、`status`、`reviewerId`、`reviewerName`、`reviewComment`、`createTime`、`reviewTime`）。

---

### 3.7 审核加入申请

**接口地址**：`PUT /api/school/{schoolId}/applications/approve`

**请求体**：

```json
{ "applicationId": 1, "approved": true, "comment": "欢迎" }
```

---

### 3.8 批量审核加入申请

**接口地址**：`PUT /api/school/{schoolId}/applications/batch-approve`

**请求体**：

```json
{ "applicationIds": [1, 2], "approved": true, "comment": "" }
```

响应：`BatchReviewResult`（`handled`、`skipped`）。

---

### 3.9 查询学校成员

**接口地址**：`GET /api/school/{schoolId}/members`

| 参数 | 类型 | 说明 |
| ---- | ---- | ---- |
| keyword | String | 姓名 / 学工号关键字 |
| pageNum / pageSize | Integer | 分页 |

响应：`Page<SchoolMemberResponse>`（字段：`id`、`schoolId`、`userId`、`username`、`realName`、`staffNo`、`roleCode`、`role`、`joinTime`）。

---

### 3.10 设置成员角色

**接口地址**：`PUT /api/school/{schoolId}/members/role`（学校管理员）

**请求体**：`{ "userId": 1002, "role": 1 }`（1-老师，0-学生；学校管理员由平台管理员指派）。

> 老师降级为学生时，会级联把其在本校各班级中的角色同步为普通成员。

---

### 3.11 修改成员身份信息

**接口地址**：`PUT /api/school/{schoolId}/members/identity`

**请求体**：`{ "userId": 1002, "realName": "张三", "staffNo": "2024002" }`

---

### 3.12 移出成员

**接口地址**：`DELETE /api/school/{schoolId}/members/{userId}`（学校管理员）

---

### 3.13 退出学校 ⚠️ 需二次验证

**接口地址**：`DELETE /api/school/{schoolId}/membership`

操作标识：`school.leave`。学校管理员需先被取消管理员身份后才能退出。

---

### 3.14 设置加入审核

**接口地址**：`PUT /api/school/{schoolId}/join-approval`

**请求体**：`{ "allowJoinWithoutApproval": true }`

---

### 3.15 设置班级接管自动同意

**接口地址**：`PUT /api/school/{schoolId}/class-takeover-approval`

**请求体**：`{ "autoApproveClassTakeover": true }`

---

## 4. 班级（ClassController）

**基础路径**：`/api/class`

### 4.1 创建班级

**接口地址**：`POST /api/class/create`

**请求体**：

```json
{ "schoolId": 1, "className": "高一(1)班", "description": "说明" }
```

响应：`ClassDetailResponse`。创建者自动成为班级管理员（老师）。仅学校老师可创建（否则 `code: 8607`）。

---

### 4.2 提交加入班级申请

**接口地址**：`POST /api/class/{classId}/applications/join`

响应：`JoinClassApplicationResponse`。

---

### 4.3 退出班级 ⚠️ 需二次验证

**接口地址**：`DELETE /api/class/{classId}/members/me`

操作标识：`class.leave`。创建者不能退出（需先转让或解散，否则 `code: 8505`）。

---

### 4.4 解散班级 ⚠️ 需二次验证

**接口地址**：`DELETE /api/class/{classId}`

操作标识：`class.dissolve`。创建者或拥有 `class:dissolve` 权限者可执行。

**请求体**（确认文案，必须精确匹配「我已确认要删除{className}课堂」）：

```json
{ "confirmText": "我已确认要删除高一(1)班课堂" }
```

---

### 4.5 更新班级信息

**接口地址**：`PUT /api/class/{classId}`

**请求体**：`{ "classId": 1, "className": "高一(2)班", "description": "新说明" }`

---

### 4.6 获取班级详情

**接口地址**：`GET /api/class/{classId}` → `ClassDetailResponse`

字段：`id`、`className`、`schoolId`、`schoolName`、`ownerId`、`ownerName`、`userRole`、`userRoleCode`、`memberCount`、`teacherCount`、`studentCount`、`description`、`allowStudentInvite`、`createTime`、`frozen`、`ownerActive`、`canTakeover`、`takeoverPending`、`takeoverAutoApprove`。

---

### 4.7 获取我加入的班级列表（分页）

**接口地址**：`GET /api/class/mine`

| 参数 | 类型 | 说明 |
| ---- | ---- | ---- |
| pageNum / pageSize | Integer | 分页 |
| schoolId | Integer | 按学校筛选 |
| roleCode | Integer | 按班级角色筛选 |
| excludeOwner | Boolean | 是否排除我创建的班级 |

---

### 4.8 管理员获取可管理班级列表（分页）

**接口地址**：`GET /api/class/manage`（需班级管理权限）

| 参数 | 类型 | 说明 |
| ---- | ---- | ---- |
| pageNum / pageSize | Integer | 分页 |
| schoolId | Integer | 按学校筛选 |

---

### 4.9 获取班级成员列表（分页）

**接口地址**：`GET /api/class/{classId}/members`

响应：`Page<ClassMemberResponse>`（字段：`id`、`userId`、`userName`、`studentName`、`studentNo`、`role`、`roleCode`、`joinTime`）。

---

### 4.10 检查用户是否在指定班级中

**接口地址**：`GET /api/class/{classId}/membership` → `MemberCheckResponse`（`isMember`、`roleCode`、`roleName`）

---

### 4.11 获取加入班级申请列表

**接口地址**：`GET /api/class/applications/join/list`

| 参数 | 类型 | 说明 |
| ---- | ---- | ---- |
| classId | Integer | 按班级筛选 |
| status | Integer | 状态筛选 |
| pageNum / pageSize | Integer | 分页 |

---

### 4.12 审核加入班级申请

**接口地址**：`PUT /api/class/applications/join/approve`

**请求体**：`{ "applicationId": 1, "approved": true, "comment": "欢迎" }`

---

### 4.13 批量设置课代表

**接口地址**：`PUT /api/class/{classId}/assistants/batch`

**请求体**：`{ "studentUserIds": [1002, 1003] }`

---

### 4.14 批量踢出学生 ⚠️ 需二次验证

**接口地址**：`DELETE /api/class/{classId}/members/batch`

操作标识：`class.kick_member`。

**请求体**：

```json
{ "studentUserIds": [1002, 1003] }
```

---

### 4.15 取消课代表权限

**接口地址**：`DELETE /api/class/{classId}/assistants/{teacherUserId}`（仅创建者或平台管理员）

---

### 4.16 设置学生邀请设置

**接口地址**：`PUT /api/class/{classId}/invite-settings`

**请求体**：`{ "allowStudentInvite": true }`

---

### 4.17 邀请用户加入班级

**接口地址**：`POST /api/class/{classId}/invitations`

**请求体**：`{ "userAccount": "lisi" }`

---

### 4.18 响应邀请

**接口地址**：`PUT /api/class/invitations/{invitationId}` → `{ "invitationId": 1, "accepted": true }`

---

### 4.19 审核教师邀请

**接口地址**：`PUT /api/class/invitations/{applicationId}/approval`

**请求体**：`{ "applicationId": 1, "approved": true, "comment": "" }`

---

### 4.20 获取待审核邀请列表

**接口地址**：`GET /api/class/{classId}/invitations/pending` → `List<TeacherApprovalResponse>`

---

### 4.21 教师邀请用户（需用户同意）

**接口地址**：`POST /api/class/{classId}/invitations/teacher`

**请求体**：`{ "userAccount": "lisi" }` → `InvitationResponse`

---

### 4.22 获取我收到的邀请列表

**接口地址**：`GET /api/class/my-invitations?status=<可选>`

### 4.23 获取我收到的用户邀请列表

**接口地址**：`GET /api/class/my-user-invitations`

### 4.24 响应邀请

**接口地址**：`PUT /api/class/respond-invitation`

**请求体**：`{ "invitationId": 1, "accepted": true }`

---

### 4.25 获取班级邀请码

**接口地址**：`GET /api/class/{classId}/invite-code` → `data` 为 25 位邀请码字符串

### 4.26 重置班级邀请码

**接口地址**：`POST /api/class/{classId}/invite-code/reset` → 新邀请码

---

### 4.27 通过邀请码加入班级

**接口地址**：`POST /api/class/join-by-code`

**请求体**：`{ "inviteCode": "Ab3...", "schoolId": 1 }`

---

### 4.28 转让班级所有权 ⚠️ 需二次验证

**接口地址**：`PUT /api/class/{classId}/owner`

操作标识：`class.transfer`。仅创建者可执行。

**请求体**：`{ "newOwnerId": 1002 }`

---

### 4.29 申请接管班级

**接口地址**：`POST /api/class/{classId}/takeover` → `ClassTakeoverResponse`

### 4.30 查询我的接管申请

**接口地址**：`GET /api/class/{classId}/takeover/mine`

### 4.31 我学校可接管的班级

**接口地址**：`GET /api/class/takeover/available` → `List<ClassTakeoverResponse>`

### 4.32 学校内班级接管申请列表

**接口地址**：`GET /api/class/takeover/school/{schoolId}?status=<可选>`

### 4.33 审核接管申请

**接口地址**：`PUT /api/class/takeover/{applicationId}/approve`

**请求体**：`{ "approved": true, "comment": "" }`

---

## 5. 作业与题目（WorkController / WorkQuestionController）

**基础路径**：`/api/works`

### 5.1 创建作业

**接口地址**：`POST /api/works`（`multipart/form-data`）

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| title | String | 是 | 标题 |
| description | String | 否 | 说明 |
| deadline | DateTime | 否 | 截止时间 |
| totalScore | Integer | 否 | 总分，默认 100 |
| allowLateSubmit | Boolean | 否 | 是否允许迟交，默认 true |
| classId | Integer | 是 | 所属班级 |
| attachments | File[] | 否 | 附件 |
| questionsJson | String | 否 | 结构化题目 JSON（见下） |
| examConfigJson | String | 否 | 考试配置 JSON（设置后即为考试） |

**`questionsJson` 题目项结构**：

```json
[
  {
    "questionType": "single",
    "content": "1+1=?",
    "options": [{ "key": "A", "text": "1" }, { "key": "B", "text": "2" }],
    "correctAnswer": "B",
    "score": 10,
    "analysis": "算术"
  }
]
```

题型 `questionType`：`single`（单选）、`multiple`（多选）、`judge`（判断）、`fill`（填空）、`subjective`（主观）、`extra`（附加）。

**`examConfigJson` 考试配置**：

```json
{
  "enabled": true,
  "durationMinutes": 60,
  "fontScramble": true,
  "forceFullscreen": true,
  "noCopy": true,
  "detectLeave": true,
  "maxViolations": 3,
  "shuffleQuestions": false
}
```

响应：`WorkResponse`。

---

### 5.2 更新作业

**接口地址**：`PUT /api/works/{workId}`（`multipart/form-data`）

参数同创建，另加：

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| removedAttachmentIds | Integer[] | 需删除的附件 ID |
| publishTime | DateTime | 发布时间 |
| questionsProvided | Boolean | 是否提交了题目字段（用于区分「未改动」与「清空」） |
| examConfigProvided | Boolean | 是否提交了考试配置 |

---

### 5.3 删除作业

**接口地址**：`DELETE /api/works/{workId}`

---

### 5.4 查询作业详情

**接口地址**：`GET /api/works/{workId}` → `WorkResponse`

`WorkResponse` 字段：`id`、`title`、`description`、`publisherId`、`publisherName`、`publisherStudentName`、`classId`、`className`、`deadline`、`totalScore`、`publishTime`、`status`、`isOverdue`、`isPinned`、`createTime`、`updateTime`、`attachments`、`submittedCount`、`hasQuestions`、`workType`、`examDurationMinutes`、`antiCheatEnabled`、`antiCheatFont`、`antiCheatFullscreen`、`antiCheatNoCopy`。

---

### 5.5 查询作业列表（分页）

**接口地址**：`GET /api/works`

| 参数 | 类型 | 说明 |
| ---- | ---- | ---- |
| status | Integer | 状态筛选 |
| classId | Integer | 按班级筛选 |
| pageNum / pageSize | Integer | 分页 |

---

### 5.6 置顶 / 取消置顶

**接口地址**：`PATCH /api/works/{workId}/pin`

**请求体**：`{ "workId": 1, "isPinned": true }`

---

### 5.7 获取作业题目

**接口地址**：`GET /api/works/{workId}/questions`

- 教师或已批改后：返回含参考答案与解析的完整题目（`WorkQuestionVO`）
- 学生答题中：返回不含答案的题目（`WorkQuestionStudentVO`）

---

## 6. 作业提交（WorkSubmissionController）

**基础路径**：`/api/submissions`

### 6.1 提交作业

**接口地址**：`POST /api/submissions`（`multipart/form-data`）

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| workId | Integer | 是 | 作业 ID |
| submissionContent | String | 否 | 文本内容 |
| attachments | File[] | 否 | 附件 |
| answers | String | 否 | 结构化作答 JSON：`[{"questionId":1,"answer":"B"}]` |

响应：`WorkSubmissionSubmitResponse`。

---

### 6.2 更新提交的作业

**接口地址**：`PUT /api/submissions/{submissionId}`（`multipart/form-data`）

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| submissionContent | String | 文本内容 |
| attachments | File[] | 新增附件 |
| removedAttachmentIds | Integer[] | 需删除的附件 ID |
| answers | String | 结构化作答 JSON |

---

### 6.3 撤回提交 ⚠️ 需二次验证

**接口地址**：`DELETE /api/submissions/{submissionId}`

操作标识：`submission.withdraw`。学生只能删除自己的提交，且不能删除已过截止时间的；教师可删除任意提交。

---

### 6.4 查询提交详情

**接口地址**：`GET /api/submissions/{submissionId}` → `WorkSubmissionResponse`

字段含：`id`、`workId`、`workTitle`、`submitterId`、`submissionContent`、`score`、`comment`、`gradeTime`、`graderId`、`graderName`、`submitterName`、`submitterStudentName`、`submitterStudentNo`、`status`、`isLate`、`attachments`、`hasQuestions`、`answers`。

---

### 6.5 查询当前用户的提交列表

**接口地址**：`GET /api/submissions/student/list?workId=<可选>` → `List<WorkSubmissionResponse>`

---

### 6.6 查询某次作业的所有提交（教师，分页）

**接口地址**：`GET /api/submissions/work/list?workId={id}&pageNum&pageSize`

### 6.7 查询已交名单（教师）

**接口地址**：`GET /api/submissions/work/submitted?workId={id}`

### 6.8 查询未交名单（教师）

**接口地址**：`GET /api/submissions/work/unsubmitted?workId={id}` → `List<UnsubmittedStudentResponse>`

---

### 6.9 批改作业（教师）

**接口地址**：`PUT /api/submissions/grade`

**请求体**：

```json
{ "submissionId": 1, "score": 85, "comment": "不错", "isReturned": false }
```

| 字段 | 说明 |
| ---- | ---- |
| score | 总分 |
| comment | 评语 |
| isReturned | 是否打回重做（true 则要求学生修改） |

---

### 6.10 按题批改（教师）

**接口地址**：`PUT /api/submissions/grade-answers`

**请求体**：

```json
{
  "submissionId": 1,
  "items": [ { "questionId": 1, "score": 8, "comment": "步骤不全" } ]
}
```

---

### 6.11 批量下载作业附件（教师）

**接口地址**：`POST /api/submissions/batch-download`（返回 `application/zip`）

**请求体**：

```json
{
  "workId": 1,
  "fileNameFormat": "{username}-{userNo}_{originalFileName}",
  "gradedOnly": false,
  "lateOnly": false
}
```

---

## 7. 考试（ExamController）

**基础路径**：`/api/exams`

考试与作业共用数据表，仅 `work_type` 区分；反作弊配置在创建 / 更新作业时通过 `examConfigJson` 设置。

### 7.1 进入考试

**接口地址**：`POST /api/exams/{workId}/enter`

首次进入会创建考试会话。响应 `ExamEnterResponse`：

字段含 `workId`、`title`、`description`、`totalScore`、`status`、`durationMinutes`、`startTime`、`endTime`、`remainingSeconds`、`submitted`、`antiCheatEnabled`、`antiCheatFont`、`antiCheatFullscreen`、`antiCheatNoCopy`、`antiCheatDetectLeave`、`antiCheatMaxViolations`、`violationCount`、`fontSeed`、`questions`、`draftAnswers`。

---

### 7.2 上报违规

**接口地址**：`POST /api/exams/{workId}/violations`

**请求体**：

```json
{ "type": "visibility_hidden", "detail": "切出页面" }
```

合法 `type`：`fullscreen_exit`（退出全屏）、`visibility_hidden`（切出页面）、`window_blur`（窗口失焦）、`copy`、`cut`、`paste`。

响应 `data` 为 Boolean，表示是否因违规达上限而需强制交卷。

---

### 7.3 保存作答草稿

**接口地址**：`PUT /api/exams/{workId}/draft`

**请求体**：`{ "draft": "<作答草稿 JSON 字符串>" }`

---

### 7.4 查询违规记录（教师）

**接口地址**：`GET /api/exams/{workId}/violations` → `List<ExamViolationVO>`

字段：`id`、`sessionId`、`workId`、`studentId`、`studentName`、`studentRealName`、`studentNo`、`type`、`typeName`、`detail`、`occurTime`。

---

### 7.5 下载考试字体

**接口地址**：`GET /api/exams/font/{seed}.woff2`

用于字体映射反作弊：前端按 `fontSeed` 下载打乱后的字体，防止复制搜题。

---

## 8. 消息（SiteMessage / PrivateMessage / Friend / MessagePolicy）

### 8.1 站内信

**基础路径**：`/api/messages`

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/messages` | 我的站内信（分页）。参数：`pageNum`、`pageSize`、`schoolId`、`type`、`isRead`、`keyword` |
| GET | `/api/messages/unread-count` | 按学校统计未读数 |
| PUT | `/api/messages/{messageId}/read` | 标记单条已读 |
| PUT | `/api/messages/read-all?schoolId=<可选>` | 全部已读 |

`SiteMessageResponse`：`id`、`schoolId`、`schoolName`、`type`、`title`、`content`、`classId`、`className`、`workId`、`isRead`、`readTime`、`createTime`。

---

### 8.2 私信

**基础路径**：`/api/private-messages`

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/private-messages/conversations?schoolId={id}` | 会话列表 |
| GET | `/api/private-messages?schoolId&peerId&pageNum&pageSize` | 与某人的消息（分页） |
| POST | `/api/private-messages` | 发送私信 |
| PUT | `/api/private-messages/read?schoolId&peerId` | 标记会话已读 |
| GET | `/api/private-messages/unread-count` | 按学校统计未读数 |

**发送请求体**：`{ "schoolId": 1, "receiverId": 1002, "content": "你好" }`

`ConversationInfo`：`schoolId`、`schoolName`、`peerId`、`peerUsername`、`peerAvatar`、`peerRealName`、`peerStaffNo`、`peerRole`、`friend`、`lastContent`、`lastTime`、`unreadCount`。

`PrivateMessageInfo`：`id`、`schoolId`、`senderId`、`receiverId`、`content`、`isRead`、`createTime`、`mine`。

---

### 8.3 好友

**基础路径**：`/api/friends`

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/friends?schoolId={id}` | 好友列表 |
| GET | `/api/friends/requests?schoolId={id}` | 待处理好友申请 |
| GET | `/api/friends/search?schoolId&keyword` | 可添加用户搜索 |
| POST | `/api/friends/requests` | 发起好友申请 |
| PUT | `/api/friends/requests` | 响应好友申请 |
| DELETE | `/api/friends/{relationId}` | 删除好友 |

**发起申请**：`{ "schoolId": 1, "targetUserId": 1002 }`
**响应申请**：`{ "relationId": 1, "accepted": true }`

> 好友按学校隔离，只能添加同一学校内的用户。

---

### 8.4 消息策略

**基础路径**：`/api/message-policy`

| 方法 | 路径 | 权限 | 说明 |
| ---- | ---- | ---- | ---- |
| GET | `/api/message-policy` | 平台管理员 | 查询全局策略 |
| PUT | `/api/message-policy` | 平台管理员 | 更新全局策略 |
| GET | `/api/message-policy/school/{schoolId}` | 学校管理员 | 查询学校覆盖策略 |
| PUT | `/api/message-policy/school/{schoolId}` | 学校管理员 | 更新学校覆盖策略 |

**请求体**：`{ "strangerLimit": 3, "resetHours": 24 }`（陌生用户私信条数与重置周期）

`MessagePolicyInfo`：`strangerLimit`、`resetHours`、`schoolOverridden`、`globalStrangerLimit`、`globalResetHours`。

---

## 9. 管理员后台（AdminUser / AdminPermission / AdminSchool）

> 所有 `/api/admin/**` 接口由 `PermissionInterceptor` 强制校验权限节点；平台管理员（OP）拥有全部节点。

### 9.1 用户管理

**基础路径**：`/api/admin/users`

| 方法 | 路径 | 权限节点 | 说明 |
| ---- | ---- | ---- | ---- |
| POST | `/api/admin/users/search` | `user:view` | 检索用户（分页） |
| POST | `/api/admin/users` | `user:add` | 新增用户 |
| PUT | `/api/admin/users/{userId}` | `user:edit` | 编辑用户 |
| DELETE | `/api/admin/users/{userId}` | `user:delete` | 删除用户 ⚠️ 二次验证 |
| POST | `/api/admin/users/{userId}/avatar` | `user:edit` | 上传用户头像（multipart） |
| DELETE | `/api/admin/users/{userId}/avatar` | `user:edit` | 清除用户头像 |
| PUT | `/api/admin/users/{userId}/ban` | `user:ban` | 封禁 / 解封 ⚠️ 二次验证 |
| PUT | `/api/admin/users/{userId}/op` | `user:setop` | 设置 / 取消平台管理员 ⚠️ 二次验证 |
| GET | `/api/admin/users/{userId}/permissions` | `user:view` 或 `permission:view` | 查询用户权限明细 |
| PUT | `/api/admin/users/{userId}/groups` | `permission:group:assign` | 分配权限组 ⚠️ 二次验证 |
| PUT | `/api/admin/users/{userId}/nodes` | `permission:user:assign` | 分配权限节点 ⚠️ 二次验证 |

**检索用户请求体**：

```json
{
  "conditions": [
    { "field": "username", "matchType": "contains", "value": "张", "connector": "and" }
  ]
}
```

`matchType`：`contains` / `equals` / `startsWith` 等；`connector`：`and` / `or`。

**新增 / 编辑用户请求体**：`{ "username": "张三", "email": "a@b.com", "phone": "138...", "password": "Pass@123" }`

**封禁请求体**：`{ "banned": true, "reason": "违规" }`
**设置 OP 请求体**：`{ "isOp": true }`
**分配权限组**：`{ "groupIds": [1, 2] }`
**分配权限节点**：`{ "nodes": ["class:dissolve"] }`

`AdminUserVO`：`id`、`username`、`email`、`phone`、`avatar`、`isOp`、`isBanned`、`banReason`、`groupNames`、`permissions`、`registerTime`、`lastLoginTime`。

`UserPermissionDetailVO`：`userId`、`username`、`isOp`、`groups`、`directNodes`、`permissions`。

---

### 9.2 权限组管理

**基础路径**：`/api/admin/permissions`

| 方法 | 路径 | 权限节点 | 说明 |
| ---- | ---- | ---- | ---- |
| GET | `/api/admin/permissions/nodes` | `permission:view` | 权限节点分组树 |
| GET | `/api/admin/permissions/groups` | `permission:view` | 权限组列表 |
| POST | `/api/admin/permissions/groups` | `permission:group:add` | 创建权限组 |
| PUT | `/api/admin/permissions/groups/{groupId}` | `permission:group:edit` | 编辑权限组 |
| DELETE | `/api/admin/permissions/groups/{groupId}` | `permission:group:delete` | 删除权限组 ⚠️ 二次验证 |
| PUT | `/api/admin/permissions/groups/{groupId}/nodes` | `permission:group:edit` | 配置权限组节点 ⚠️ 二次验证 |

**创建 / 编辑权限组请求体**：`{ "code": "class-admin", "name": "班级管理员", "description": "", "isDefault": false }`

**配置节点请求体**：`{ "nodes": ["class:update", "class:member:kick"] }`

`PermissionGroupVO`：`id`、`code`、`name`、`description`、`isDefault`、`nodes`、`userCount`、`createTime`。

**权限节点清单**（按分组）：

| 分组 | 节点 |
| ---- | ---- |
| 用户管理 | `user:view`、`user:add`、`user:edit`、`user:delete`、`user:ban`、`user:setop` |
| 权限管理 | `permission:view`、`permission:group:add`、`permission:group:edit`、`permission:group:delete`、`permission:group:assign`、`permission:user:assign` |
| 班级管理 | `class:view_all`、`class:create`、`class:update`、`class:dissolve`、`class:member:kick`、`class:approve_join`、`class:teacher:add` |
| 学校管理 | `school:view_all`、`school:create`、`school:update`、`school:dissolve`、`school:admin:assign` |

---

### 9.3 学校管理

**基础路径**：`/api/admin/schools`

| 方法 | 路径 | 权限节点 | 说明 |
| ---- | ---- | ---- | ---- |
| GET | `/api/admin/schools` | `school:view_all` | 学校列表（分页） |
| GET | `/api/admin/schools/applications` | `school:view_all` | 全平台加入申请（分页） |
| PUT | `/api/admin/schools/applications/batch-approve` | `school:update` | 批量审核加入申请 |
| GET | `/api/admin/schools/{schoolId}` | `school:view_all` | 学校详情 |
| POST | `/api/admin/schools` | `school:create` | 创建学校 |
| PUT | `/api/admin/schools/{schoolId}` | `school:update` | 修改学校 |
| DELETE | `/api/admin/schools/{schoolId}` | `school:dissolve` | 解散学校 ⚠️ 二次验证 |
| PUT | `/api/admin/schools/{schoolId}/admin` | `school:admin:assign` | 指派 / 取消学校管理员 ⚠️ 二次验证 |

**创建学校请求体**：`{ "schoolName": "第一中学", "description": "", "allowJoinWithoutApproval": false }`
**修改学校请求体**：`{ "schoolName": "第一中学", "description": "新说明" }`
**指派学校管理员请求体**：`{ "userAccount": "lisi", "assigned": true, "staffNo": "T001", "realName": "李四" }`
**批量审核请求体**：`{ "applicationIds": [1, 2], "approved": true, "comment": "" }`

---

## 10. 文件（FileController）

**基础路径**：`/api/files`

### 10.1 下载文件

**接口地址**：`GET /api/files/download`

| 参数 | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| path | String | 是 | 文件相对路径 |
| fileName | String | 否 | 下载时的文件名 |
| inline | Boolean | 否 | 是否内联展示（默认 false，即作为附件下载） |

响应：文件二进制流（`Resource`）。

---

## 附录

### 作业状态（WorkInfo.status）

| 值 | 含义 |
| ---- | ---- |
| 0 | 草稿 |
| 1 | 已发布 |
| 2 | 已截止 / 已结束 |

### 提交状态（WorkSubmission.status）

| 值 | 含义 |
| ---- | ---- |
| 1 | 已提交 |
| 2 | 已批改 |
| 3 | 已打回（需重新提交） |

### 申请状态（学校 / 班级加入申请）

| 值 | 含义 |
| ---- | ---- |
| 0 | 待审核 |
| 1 | 已通过 |
| 2 | 已拒绝 |

### 学校成员角色

| 值 | 含义 |
| ---- | ---- |
| 0 | 学生 |
| 1 | 老师 |
| 2 | 学校管理员 |

### 班级成员角色（ClassMember.role）

| 值 | 含义 |
| ---- | ---- |
| 0 | 学生（学校老师获得的班级管理权限显示为「课代表」） |
| 1 | 老师 / 课代表（班级管理员） |

### 时间格式

- 日期时间：`yyyy-MM-ddTHH:mm:ss`（ISO-8601，时区 GMT+8），如 `2026-05-07T10:00:00`
- 仅日期：`yyyy-MM-dd`

### 注意事项

1. 所有写操作（POST / PUT / DELETE / PATCH）需携带 `X-CSRF-Token`。
2. 二次验证失败返回 **400**（错误码 6100–6105），不会返回 401。
3. 平台管理员（OP）拥有全部权限节点，`PermissionInterceptor` 对其直接放行。
4. 请求鉴权会回库确认账号仍存在且未被封禁，账号被删除或封禁后原 Token 立即失效。
5. 前端页签 / 按钮显隐仅作体验优化，不作为安全边界。
