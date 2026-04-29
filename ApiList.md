# Work Management API 接口文档

## 项目架构

本项目采用模块化设计，主要包含以下模块：

- **common**: 公共模块（API响应、上下文管理等）
- **support**: 支撑模块（加密、验证、日志、会话管理）
- **config**: 配置层（安全配置、基础设施配置、异常处理）
- **module/user**: 用户模块（登录、注册、用户管理）
- **module/work_management**: 课堂管理模块（班级、作业、提交）

---

## 目录

- [1. 作业管理接口 (WorkController)](#1-作业管理接口-workcontroller)
- [2. 班级管理接口 (ClassController)](#2-班级管理接口-classcontroller)
- [3. 作业提交接口 (WorkSubmissionController)](#3-作业提交接口-worksubmissioncontroller)

---

## 通用说明

### 认证方式

所有接口均需要登录认证，通过 Session 传递用户信息。

### 响应格式

所有接口统一返回 JSON 格式：

**成功响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {}
}
```

**失败响应**:

```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

### 常见错误码

- `200`: 成功
- `400`: 请求参数错误或业务逻辑错误
- `401`: 未登录或登录已过期
- `500`: 服务器内部错误

---

## 1. 作业管理接口 (WorkController)

**基础路径**: `/api/works`

### 1.1 创建作业

**接口地址**: `POST /api/works/create`

**请求头**:

- Content-Type: multipart/form-data
- 需要登录认证（Session）

**请求参数** (multipart/form-data):
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 作业标题，最长 128 字符，不能包含换行符、制表符等特殊字符 |
| description | String | 是 | 作业描述，不能包含制表符等特殊字符 |
| deadline | String | 是 | 截止时间，格式：yyyy-MM-dd'T'HH:mm:ss |
| totalScore | Integer | 是 | 作业总分，默认 100 |
| classId | Integer | 是 | 所属班级 ID |
| publishTime | String | 是 | 发布时间，格式：yyyy-MM-dd'T'HH:mm:ss |
| allowLateSubmit | Boolean | 否 | 是否允许逾期提交，默认 true |
| attachments | File[] | 否 | 附件文件列表（支持多文件上传） |

**请求示例**:

```
POST /api/works/create
Content-Type: multipart/form-data

title: 第一次作业
description: 请完成第一章习题
deadline: 2026-04-15T23:59:59
totalScore: 100
classId: 1
publishTime: 2026-04-09T10:00:00
allowLateSubmit: true
attachments: [file1.pdf, file2.doc]
```

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "title": "作业标题",
    "description": "作业描述",
    "publisherId": 1001,
    "classId": 1,
    "deadline": "2026-04-15T23:59:59",
    "totalScore": 100,
    "publishTime": "2026-04-09T10:00:00",
    "createTime": "2026-04-09T10:00:00",
    "updateTime": "2026-04-09T10:00:00"
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 作业 ID |
| title | String | 作业标题 |
| description | String | 作业描述 |
| publisherId | Integer | 发布人 ID |
| classId | Integer | 所属班级 ID |
| deadline | LocalDateTime | 截止时间 |
| totalScore | Integer | 作业总分 |
| publishTime | LocalDateTime | 发布时间 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

**失败响应**:

```json
{
  "code": 400,
  "message": "作业标题不能为空",
  "data": null
}
```

**可能的错误信息**:

- "作业标题不能为空"
- "作业标题长度不能超过 128 位"
- "作业描述不能为空"
- "截止时间不能为空"
- "作业总分不能为空"
- "所属班级 ID 不能为空"
- "发布时间不能为空"
- "只有班级老师可以发布作业"
- "文件上传失败：xxx"

**注意**:

- **直接上传附件**：通过 `attachments` 参数直接上传文件，无需预先调用文件上传接口
- **文件安全检查**：系统会对上传的文件进行病毒扫描、文件类型白名单验证等安全检查
- **文件存储位置**：`uploads/works/` 目录

---

### 1.2 更新作业

**接口地址**: `PUT /api/works/update`

**请求头**:

- Content-Type: multipart/form-data
- 需要登录认证（Session）

**请求参数** (multipart/form-data):

| 参数                 | 类型      | 必填 | 说明                                  |
| -------------------- | --------- | ---- | ------------------------------------- |
| id                   | Integer   | 是   | 作业 ID                               |
| title                | String    | 是   | 作业标题，最长 128 字符               |
| description          | String    | 是   | 作业描述，最长 1024 字符              |
| deadline             | String    | 是   | 截止时间，格式：yyyy-MM-dd'T'HH:mm:ss |
| totalScore           | Integer   | 是   | 作业总分（无学生提交时可修改）        |
| allowLateSubmit      | Boolean   | 否   | 是否允许逾期提交                      |
| publishTime          | String    | 否   | 发布时间（仅未发布的作业可修改）      |
| attachments          | File[]    | 否   | 新增的附件文件列表                    |
| removedAttachmentIds | Integer[] | 否   | 要删除的附件ID列表                    |

**请求示例**:

```
PUT /api/works/update
Content-Type: multipart/form-data

id: 1
title: 更新后的作业标题
description: 更新后的作业描述
deadline: 2026-04-20T23:59:59
totalScore: 100
attachments: [newfile.pdf]
removedAttachmentIds: [1, 2]
```

**注意**:

- 已发布作业(status=1)不允许修改publishTime，只能修改其他字段
- **当没有学生提交作业时，允许修改totalScore**；有提交记录后禁止修改，保护数据一致性
- **支持附件增量更新**：可以通过`removedAttachmentIds`删除指定附件，通过`attachments`添加新附件
- **不会导致数据丢失**：修改附件不会影响学生的提交记录
- **直接上传附件**：新增附件通过 `attachments` 参数直接上传文件

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "title": "更新后的作业标题",
    "description": "更新后的作业描述",
    "publisherId": 1001,
    "classId": 1,
    "deadline": "2026-04-20T23:59:59",
    "totalScore": 100,
    "publishTime": "2026-04-09T10:00:00",
    "createTime": "2026-04-09T10:00:00",
    "updateTime": "2026-04-09T11:00:00"
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 作业 ID |
| title | String | 作业标题 |
| description | String | 作业描述 |
| publisherId | Integer | 发布人 ID |
| classId | Integer | 所属班级 ID |
| deadline | LocalDateTime | 截止时间 |
| totalScore | Integer | 作业总分 |
| publishTime | LocalDateTime | 发布时间 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

**失败响应**:

```json
{
  "code": 400,
  "message": "作业不存在",
  "data": null
}
```

**可能的错误信息**:

- "作业 ID 不能为空"
- "作业标题不能为空"
- "作业描述不能为空"
- "截止时间不能为空"
- "作业总分不能为空"
- "已发布的作业不能修改发布时间"
- "已有学生提交作业，无法修改总分"
- "作业不存在"
- "只有班级老师可以修改作业"
- "只能修改自己发布的作业"
- "文件上传失败：xxx"

---

### 1.3 删除作业

**接口地址**: `DELETE /api/works/delete`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workId | Integer | 是 | 作业 ID |

**请求示例**: `DELETE /api/works/delete?workId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

**失败响应**:

```json
{
  "code": 400,
  "message": "作业不存在",
  "data": null
}
```

**可能的错误信息**:

- "作业不存在"
- "用户无权限删除此作业"

**注意**:

- **允许删除任何状态的作业**（包括已发布、已结束）
- 只能删除自己发布的作业
- **级联清理机制**：删除作业时会自动清理所有关联数据
  - 删除学生提交的所有附件文件
  - 删除提交附件记录
  - 删除所有提交记录
  - 删除作业本身的附件文件和记录
- **防止资源浪费**：自动释放服务器存储空间，避免孤儿数据

---

### 1.4 查询作业详情

**接口地址**: `GET /api/works/detail`

**请求参数**:

| 参数   | 类型    | 必填 | 说明    |
| ------ | ------- | ---- | ------- |
| workId | Integer | 是   | 作业 ID |

**请求示例**: `GET /api/works/detail?workId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "title": "作业标题",
    "description": "作业描述",
    "publisherId": 1001,
    "classId": 1,
    "deadline": "2026-04-15T23:59:59",
    "totalScore": 100,
    "publishTime": "2026-04-09T10:00:00",
    "createTime": "2026-04-09T10:00:00",
    "updateTime": "2026-04-09T10:00:00"
  }
}
```

**响应字段说明**:

| 字段        | 类型          | 说明        |
| ----------- | ------------- | ----------- |
| id          | Integer       | 作业 ID     |
| title       | String        | 作业标题    |
| description | String        | 作业描述    |
| publisherId | Integer       | 发布人 ID   |
| classId     | Integer       | 所属班级 ID |
| deadline    | LocalDateTime | 截止时间    |
| totalScore  | Integer       | 作业总分    |
| publishTime | LocalDateTime | 发布时间    |
| createTime  | LocalDateTime | 创建时间    |
| updateTime  | LocalDateTime | 更新时间    |

**失败响应**:

```json
{
  "code": 400,
  "message": "作业不存在",
  "data": null
}
```

**可能的错误信息**:

- "作业不存在"

---

### 1.5 查询作业列表（分页）

**接口地址**: `GET /api/works/list`

**请求参数**:

| 参数            | 类型    | 必填 | 说明                                         |
| --------------- | ------- | ---- | -------------------------------------------- |
| publisherUserNo | String  | 否   | 发布人学号/工号筛选                          |
| status          | Integer | 否   | 作业状态筛选（0-未发布，1-已发布，2-已结束） |
| pageNum         | Integer | 否   | 页码，默认1                                  |
| pageSize        | Integer | 否   | 每页大小，默认20，最大300                    |

**请求示例**:

- `GET /api/works/list`
- `GET /api/works/list?publisherUserNo=2021001&status=1`
- `GET /api/works/list?pageNum=1&pageSize=20`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 1,
        "title": "作业标题",
        "description": "作业描述",
        "publisherId": 1001,
        "deadline": "2026-04-15T23:59:59",
        "totalScore": 100,
        "publishTime": "2026-04-09T10:00:00",
        "status": 1,
        "createTime": "2026-04-09T10:00:00",
        "updateTime": "2026-04-09T10:00:00",
        "attachments": [
          {
            "id": 1,
            "fileName": "example.pdf",
            "filePath": "/uploads/works/example.pdf",
            "fileSize": 1024000,
            "fileType": "application/pdf",
            "uploadTime": "2026-04-09T10:00:00"
          }
        ]
      }
    ],
    "total": 15,
    "size": 20,
    "current": 1,
    "pages": 1
  }
}
```

**响应字段说明**:

| 字段    | 类型  | 说明         |
| ------- | ----- | ------------ |
| records | Array | 作业列表数据 |
| total   | Long  | 总记录数     |
| size    | Long  | 每页大小     |
| current | Long  | 当前页码     |
| pages   | Long  | 总页数       |

**records内部字段说明**:

| 字段                     | 类型          | 说明                                 |
| ------------------------ | ------------- | ------------------------------------ |
| id                       | Integer       | 作业 ID                              |
| title                    | String        | 作业标题                             |
| description              | String        | 作业描述                             |
| publisherId              | Integer       | 发布人 ID                            |
| deadline                 | LocalDateTime | 截止时间                             |
| totalScore               | Integer       | 作业总分                             |
| publishTime              | LocalDateTime | 发布时间                             |
| status                   | Integer       | 作业状态(0-未发布,1-已发布,2-已结束) |
| createTime               | LocalDateTime | 创建时间                             |
| updateTime               | LocalDateTime | 更新时间                             |
| attachments              | List          | 附件列表                             |
| attachments[].id         | Integer       | 附件 ID                              |
| attachments[].fileName   | String        | 文件名                               |
| attachments[].filePath   | String        | 文件路径                             |
| attachments[].fileSize   | Long          | 文件大小(字节)                       |
| attachments[].fileType   | String        | 文件类型(MIME)                       |
| attachments[].uploadTime | LocalDateTime | 上传时间                             |

**失败响应**:

```json
{
  "code": 400,
  "message": "查询失败",
  "data": null
}
```

---

## 2. 班级管理接口 (ClassController)

**基础路径**: `/api/class`

### 2.1 提交创建班级申请

**接口地址**: `POST /api/class/create`

**请求头**:

- Content-Type: application/json
- 需要登录认证（Session）

**请求体**:

```json
{
  "className": "计算机科学2024级1班",
  "description": "计算机科学与技术专业2024级1班"
}
```

**字段说明**:

| 字段        | 类型   | 必填 | 说明                                                     |
| ----------- | ------ | ---- | -------------------------------------------------------- |
| className   | String | 是   | 班级名称，最长 64 字符，不能包含换行符、制表符等特殊字符 |
| description | String | 否   | 班级描述，最长 512 字符，不能包含制表符等特殊字符        |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "创建班级的申请已提交，待审核",
  "data": {
    "id": 1,
    "applicantId": 1001,
    "className": "计算机科学2024级1班",
    "description": "计算机科学与技术专业2024级1班",
    "status": 0,
    "createTime": "2026-04-09T10:00:00"
  }
}
```

**响应字段说明**:

| 字段        | 类型          | 说明                                 |
| ----------- | ------------- | ------------------------------------ |
| id          | Integer       | 申请 ID                              |
| applicantId | Integer       | 申请人 ID                            |
| className   | String        | 申请的班级名称                       |
| description | String        | 申请的班级描述                       |
| status      | Integer       | 申请状态(0-待审核,1-已通过,2-已拒绝) |
| createTime  | LocalDateTime | 申请时间                             |

**注意**:

- 创建申请仅管理员可审核
- 审核通过后自动创建班级，申请人成为`创建者`

**失败响应**:

```json
{
  "code": 400,
  "message": "班级名称不能为空",
  "data": null
}
```

**可能的错误信息**:

- "班级名称不能为空"
- "班级名称长度不能超过 64 位"
- "班级名称不能包含特殊字符（换行符、制表符等）"
- "班级描述长度不能超过 512 位"
- "班级描述不能包含特殊字符（制表符等）"

---

### 2.2 提交加入班级申请

**接口地址**: `POST /api/class/join`

**请求头**:

- Content-Type: application/json
- 需要登录认证（Session）

**请求体**:

```json
{
  "classId": 1
}
```

**字段说明**:

| 字段    | 类型    | 必填 | 说明    |
| ------- | ------- | ---- | ------- |
| classId | Integer | 是   | 班级 ID |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "加入班级的申请已提交，待审核",
  "data": {
    "id": 2,
    "classId": 1,
    "applicantId": 1002,
    "status": 0,
    "createTime": "2026-04-09T10:00:00"
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 申请 ID |
| classId | Integer | 申请的班级 ID |
| applicantId | Integer | 申请人 ID |
| status | Integer | 申请状态(0-待审核,1-已通过,2-已拒绝) |
| createTime | LocalDateTime | 申请时间 |

**注意**:

- 加入申请老师和管理员都可审核
- 审核通过后申请人以`学生`身份加入班级

**失败响应**:

```json
{
  "code": 400,
  "message": "班级不存在",
  "data": null
}
```

**可能的错误信息**:

- "班级 ID 不能为空"
- "班级不存在"
- "您已经是该班级成员"
- "已有待审核的加入申请"

---

### 2.3 退出班级

**接口地址**: `POST /api/class/leave`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 是 | 班级 ID |

**请求示例**: `POST /api/class/leave?classId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "已成功退出“计算机科学2024级1班”班级",
  "data": null
}
```

**失败响应**:

```json
{
  "code": 400,
  "message": "您不是该班级成员",
  "data": null
}
```

**可能的错误信息**:

- "您不是该班级成员"
- "班级创建者不能退出班级"

**注意**:

- **级联清理机制**：退出班级时会自动清理该学生在该班级的所有作业提交数据（软删除）
  - 软删除学生提交的所有附件记录（is_deleted = true）
  - 软删除所有提交记录（is_deleted = true）
  - 物理删除学生提交的附件文件
- **数据可恢复**：软删除保留数据完整性，便于审计和恢复

---

### 2.4 解散班级

**接口地址**: `DELETE /api/class/dissolve`

**请求参数**:

| 参数    | 类型    | 必填 | 说明    |
| ------- | ------- | ---- | ------- |
| classId | Integer | 是   | 班级 ID |

**请求示例**: `DELETE /api/class/dissolve?classId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

**失败响应**:

```json
{
  "code": 400,
  "message": "只有班级创建者可以解散班级",
  "data": null
}
```

**可能的错误信息**:

- "班级不存在"
- "只有班级创建者可以解散班级"

**注意**:

- **完整级联删除机制**：解散班级时会永久删除该班级下的所有相关数据（按顺序执行）
  1. 硬删除所有作业提交附件记录（WorkSubmissionAttachment）
  2. 硬删除所有作业提交记录（WorkSubmission）
  3. 硬删除所有作业附件记录（WorkAttachment）
  4. 硬删除所有作业信息记录（WorkInfo）
  5. 硬删除所有班级成员记录（ClassMember）
  6. 删除所有班级邀请记录（ClassInvitation）
  7. 删除所有班级加入申请记录（ClassJoinApplication）
  8. 删除所有班级邀请申请记录（ClassInviteApplication）
  9. 最后删除班级信息（ClassInfo）
- **不可恢复**：此操作不可逆，请谨慎使用

---

### 2.5 获取班级详情

**接口地址**: `GET /api/class/detail`

**请求参数**:

| 参数    | 类型    | 必填 | 说明    |
| ------- | ------- | ---- | ------- |
| classId | Integer | 是   | 班级 ID |

**请求示例**: `GET /api/class/detail?classId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "className": "计算机科学2024级1班",
    "ownerId": 1001,
    "ownerName": "张三",
    "userRole": "创建者",
    "memberCount": 50,
    "teacherCount": 2,
    "studentCount": 48
  }
}
```

**响应字段说明**:

| 字段         | 类型    | 说明                                     |
| ------------ | ------- | ---------------------------------------- |
| id           | Integer | 班级 ID                                  |
| className    | String  | 班级名称                                 |
| ownerId      | Integer | 班级所有者 ID                            |
| ownerName    | String  | 班级所有者姓名                           |
| userRole     | String  | 用户在该班级的角色(创建者/班级助理/学生) |
| memberCount  | Long    | 成员总数                                 |
| teacherCount | Long    | 教师数量                                 |
| studentCount | Long    | 学生数量                                 |

**角色说明**:

- `创建者`: 班级创建者，拥有最高权限（可解散班级、管理班级助理）
- `班级助理`: 由创建者设置，拥有教师权限但不能解散班级或降级其他班级助理
- `学生`: 普通学生

**失败响应**:

```json
{
  "code": 400,
  "message": "班级不存在",
  "data": null
}
```

**可能的错误信息**:

- "班级不存在"
- "您不是该班级成员"

---

### 2.6 获取我加入的班级列表（分页）

**接口地址**: `GET /api/class/mylist`

**请求参数**:

| 参数     | 类型    | 必填 | 说明                      |
| -------- | ------- | ---- | ------------------------- |
| pageNum  | Integer | 否   | 页码，默认1               |
| pageSize | Integer | 否   | 每页大小，默认20，最大300 |

**请求示例**:

- `GET /api/class/mylist`
- `GET /api/class/mylist?pageNum=1&pageSize=10`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 1,
        "className": "计算机科学2024级1班",
        "ownerId": 1001,
        "ownerName": "张三",
        "userRole": "学生",
        "memberCount": 50,
        "teacherCount": 2,
        "studentCount": 48
      },
      {
        "id": 2,
        "className": "软件工程2024级1班",
        "ownerId": 1003,
        "ownerName": "李四",
        "userRole": "班级助理",
        "memberCount": 45,
        "teacherCount": 1,
        "studentCount": 44
      }
    ],
    "total": 2,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

**响应字段说明**:

| 字段    | 类型  | 说明         |
| ------- | ----- | ------------ |
| records | Array | 班级列表数据 |
| total   | Long  | 总记录数     |
| size    | Long  | 每页大小     |
| current | Long  | 当前页码     |
| pages   | Long  | 总页数       |

**records内部字段说明**:

| 字段         | 类型    | 说明                                     |
| ------------ | ------- | ---------------------------------------- |
| id           | Integer | 班级 ID                                  |
| className    | String  | 班级名称                                 |
| ownerId      | Integer | 班级所有者 ID                            |
| ownerName    | String  | 班级所有者姓名                           |
| userRole     | String  | 用户在该班级的角色(创建者/班级助理/学生) |
| memberCount  | Long    | 成员总数                                 |
| teacherCount | Long    | 教师数量                                 |
| studentCount | Long    | 学生数量                                 |

````

**失败响应**:

```json
{
  "code": 401,
  "message": "用户未登录",
  "data": null
}
````

---

### 2.7 获取班级成员列表（分页）

**接口地址**: `GET /api/class/members`

**请求参数**:

| 参数     | 类型    | 必填 | 说明                              |
| -------- | ------- | ---- | --------------------------------- |
| classId  | Integer | 是   | 班级 ID                           |
| pageNum  | Integer | 否   | 页码，默认1，必须大于等于1        |
| pageSize | Integer | 否   | 每页大小，默认20，必须在1-300之间 |

**请求示例**:

- `GET /api/class/members?classId=1`
- `GET /api/class/members?classId=1&pageNum=1&pageSize=20`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 1,
        "userId": 1001,
        "userName": "张三",
        "userNo": "2021001",
        "role": "创建者",
        "joinTime": "2026-04-01T10:00:00"
      },
      {
        "id": 2,
        "userId": 1002,
        "userName": "王五",
        "userNo": "2024001",
        "role": "学生",
        "joinTime": "2026-04-02T10:00:00"
      }
    ],
    "total": 2,
    "size": 20,
    "current": 1,
    "pages": 1
  }
}
```

**响应字段说明**:

| 字段    | 类型  | 说明         |
| ------- | ----- | ------------ |
| records | Array | 成员列表数据 |
| total   | Long  | 总记录数     |
| size    | Long  | 每页大小     |
| current | Long  | 当前页码     |
| pages   | Long  | 总页数       |

**records内部字段说明**:

| 字段     | 类型          | 说明                       |
| -------- | ------------- | -------------------------- |
| id       | Integer       | 成员 ID                    |
| userId   | Integer       | 用户 ID                    |
| userName | String        | 用户姓名                   |
| userNo   | String        | 学号/工号                  |
| role     | String        | 角色(创建者/班级助理/学生) |
| joinTime | LocalDateTime | 加入时间                   |

**角色说明**:

- `创建者`: 班级创建者
- `班级助理`: 班级助理
- `学生`: 普通学生

**失败响应**:

```json
{
  "code": 400,
  "message": "每页大小不能超过300",
  "data": null
}
```

**可能的错误信息**:

- "班级不存在"
- "您不是该班级成员"
- "每页大小不能超过300" - pageSize 超出限制
- "页码必须大于等于1" - pageNum 小于1

---

### 2.8 检查用户是否在指定班级中

**接口地址**: `GET /api/class/checkmember`

**请求参数**:

| 参数    | 类型    | 必填 | 说明    |
| ------- | ------- | ---- | ------- |
| classId | Integer | 是   | 班级 ID |

**请求示例**: `GET /api/class/checkmember?classId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "isMember": true,
    "roleCode": 1,
    "roleName": "创建者"
  }
}
```

**响应字段说明**:

| 字段     | 类型    | 说明                                                                    |
| -------- | ------- | ----------------------------------------------------------------------- |
| isMember | Boolean | 是否是班级成员                                                          |
| roleCode | Integer | 角色代码（用于前端权限判断）：1-创建者，2-班级助理，3-学生，null-非成员 |
| roleName | String  | 角色名称（用于展示）：创建者/班级助理/学生，非成员时为 null             |

**角色代码说明**:

- `1`: 创建者（班级创建者，拥有最高权限）
- `2`: 班级助理（协助管理班级）
- `3`: 学生（普通班级成员）
- `null`: 非班级成员

**管理员特殊权限规则**:

管理员（permission >= 100）具有特殊的跨班级权限：

- **管理员在班级中时**：
  - 可以被踢出班级
  - 可以被任命为班级助理
  - 可以创建作业
  - 可以提交作业（以学生身份）
  - 拥有老师的所有权限

- **管理员不在班级中时**：
  - **受限操作**：不能提交作业（必须先加入班级）
  - **可用权限**：
    - 查看班级信息
    - 审核加入申请和邀请申请
    - 管理班级成员（踢出、任命等）
    - 创建新班级
    - 拥有老师的管理权限

**使用建议**:

- **前端权限判断**: 使用 `roleCode` 进行条件判断，例如 `if (roleCode === 1) { /* 显示创建者专属功能 */ }`
- **界面展示**: 使用 `roleName` 显示给用户，例如 "您的角色：创建者"
- **优势**: `roleCode` 为固定整数值，不受中文文案修改影响，保证前端逻辑稳定性

**非成员响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "isMember": false,
    "roleCode": null,
    "roleName": null
  }
}
```

**失败响应**:

```json
{
  "code": 401,
  "message": "用户未登录",
  "data": null
}
```

---

### 2.9 获取创建班级申请列表（管理员专用）

**接口地址**: `GET /api/class/applications/create/list`

**请求参数**:

| 参数     | 类型    | 必填 | 说明                                     |
| -------- | ------- | ---- | ---------------------------------------- |
| status   | Integer | 否   | 状态筛选（0-待审核，1-已通过，2-已拒绝） |
| pageNum  | Integer | 否   | 页码，默认1                              |
| pageSize | Integer | 否   | 每页大小，默认20，最大300                |

**请求示例**:

- `GET /api/class/applications/create/list`
- `GET /api/class/applications/create/list?status=0`
- `GET /api/class/applications/create/list?pageNum=1&pageSize=20`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "查询创建申请列表成功",
  "data": {
    "records": [
      {
        "id": 1,
        "applicantId": 1001,
        "className": "计算机科学2024级1班",
        "description": "计算机科学与技术专业2024级1班",
        "status": 0,
        "reviewerId": null,
        "reviewTime": null,
        "reviewComment": null,
        "createdClassId": null,
        "createTime": "2026-04-09T10:00:00"
      }
    ],
    "total": 1,
    "size": 20,
    "current": 1,
    "pages": 1
  }
}
```

**响应字段说明**:

| 字段    | 类型  | 说明         |
| ------- | ----- | ------------ |
| records | Array | 申请列表数据 |
| total   | Long  | 总记录数     |
| size    | Long  | 每页大小     |
| current | Long  | 当前页码     |
| pages   | Long  | 总页数       |

**records内部字段说明**:

| 字段           | 类型          | 说明                                 |
| -------------- | ------------- | ------------------------------------ |
| id             | Integer       | 申请 ID                              |
| applicantId    | Integer       | 申请人 ID                            |
| className      | String        | 申请的班级名称                       |
| description    | String        | 申请的班级描述                       |
| status         | Integer       | 申请状态(0-待审核,1-已通过,2-已拒绝) |
| reviewerId     | Integer       | 审核人 ID                            |
| reviewTime     | LocalDateTime | 审核时间                             |
| reviewComment  | String        | 审核意见                             |
| createdClassId | Integer       | 审核通过后创建的班级 ID              |
| createTime     | LocalDateTime | 申请时间                             |

**注意**:

- 仅管理员(permission >= 100)可访问
- 按创建时间倒序排列

---

### 2.10 审核创建班级申请（管理员专用）

**接口地址**: `PUT /api/class/applications/create/approve`

**请求头**:

- Content-Type: application/json
- 需要登录认证（Session）

**请求体**:

```json
{
  "applicationId": 1,
  "approved": true,
  "comment": "同意创建"
}
```

**字段说明**:

| 字段          | 类型    | 必填 | 说明                            |
| ------------- | ------- | ---- | ------------------------------- |
| applicationId | Integer | 是   | 申请 ID                         |
| approved      | Boolean | 是   | 是否通过(true-通过，false-拒绝) |
| comment       | String  | 否   | 审核意见，最长 500 字符         |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

**注意**:

- 仅管理员可审核
- 审核通过后自动创建班级，申请人成为`创建者`

**失败响应**:

```json
{
  "code": 400,
  "message": "该申请已处理",
  "data": null
}
```

**可能的错误信息**:

- "只有管理员可以审核创建申请"
- "申请不存在"
- "该申请已处理"

---

### 2.11 获取加入班级申请列表（老师和管理员专用）

**接口地址**: `GET /api/class/applications/join/list`

**请求参数**:

| 参数     | 类型    | 必填 | 说明                                     |
| -------- | ------- | ---- | ---------------------------------------- |
| classId  | Integer | 否   | 班级 ID 筛选                             |
| status   | Integer | 否   | 状态筛选（0-待审核，1-已通过，2-已拒绝） |
| pageNum  | Integer | 否   | 页码，默认1                              |
| pageSize | Integer | 否   | 每页大小，默认20，最大300                |

**请求示例**:

- `GET /api/class/applications/join/list`
- `GET /api/class/applications/join/list?classId=1&status=0`
- `GET /api/class/applications/join/list?pageNum=1&pageSize=20`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "查询加入申请列表成功",
  "data": {
    "records": [
      {
        "id": 2,
        "classId": 1,
        "applicantId": 1002,
        "status": 0,
        "reviewerId": null,
        "reviewTime": null,
        "reviewComment": null,
        "createTime": "2026-04-09T10:00:00"
      }
    ],
    "total": 1,
    "size": 20,
    "current": 1,
    "pages": 1
  }
}
```

**响应字段说明**:

| 字段    | 类型  | 说明         |
| ------- | ----- | ------------ |
| records | Array | 申请列表数据 |
| total   | Long  | 总记录数     |
| size    | Long  | 每页大小     |
| current | Long  | 当前页码     |
| pages   | Long  | 总页数       |

**records内部字段说明**:

| 字段          | 类型          | 说明                                 |
| ------------- | ------------- | ------------------------------------ |
| id            | Integer       | 申请 ID                              |
| classId       | Integer       | 申请加入的班级 ID                    |
| applicantId   | Integer       | 申请人 ID                            |
| status        | Integer       | 申请状态(0-待审核,1-已通过,2-已拒绝) |
| reviewerId    | Integer       | 审核人 ID                            |
| reviewTime    | LocalDateTime | 审核时间                             |
| reviewComment | String        | 审核意见                             |
| createTime    | LocalDateTime | 申请时间                             |

**注意**:

- **管理员**: `classId` 为空时返回所有班级的申请，提供 `classId` 时只返回指定班级的申请
- **老师/班级助理**: `classId` 为空时返回自己担任老师的所有班级的申请，提供 `classId` 时只返回该班级的申请（需验证是该班老师）
- **学生**: 无权限访问此接口
- 如果没有担任老师的班级，返回空列表（total=0）
- 按创建时间 `createTime` 倒序排列

---

### 2.12 审核加入班级申请（老师和管理员专用）

**接口地址**: `PUT /api/class/applications/join/approve`

**请求头**:

- Content-Type: application/json
- 需要登录认证（Session）

**请求体**:

```json
{
  "applicationId": 2,
  "approved": true,
  "comment": "同意加入"
}
```

**字段说明**:

| 字段          | 类型    | 必填 | 说明                            |
| ------------- | ------- | ---- | ------------------------------- |
| applicationId | Integer | 是   | 申请 ID                         |
| approved      | Boolean | 是   | 是否通过(true-通过，false-拒绝) |
| comment       | String  | 否   | 审核意见，最长 500 字符         |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

**注意**:

- 管理员可审核任何班级的申请
- 老师只能审核自己所在班级的申请
- 审核通过后申请人以`学生`身份加入班级

**失败响应**:

```json
{
  "code": 400,
  "message": "该申请已处理",
  "data": null
}
```

**可能的错误信息**:

- "只有管理员或班级老师可以审核加入申请"
- "申请不存在"
- "该申请已处理"

---

### 2.13 设置学生为班级助理（老师专用）

**接口地址**: `PUT /api/class/set-assistant-teacher`

**请求参数**:

| 参数          | 类型    | 必填 | 说明        |
| ------------- | ------- | ---- | ----------- |
| classId       | Integer | 是   | 班级 ID     |
| studentUserId | Integer | 是   | 学生用户 ID |

**请求示例**: `PUT /api/class/set-assistant-teacher?classId=1&studentUserId=1002`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

**失败响应**:

```json
{
  "code": 400,
  "message": "该用户不是班级学生",
  "data": null
}
```

**可能的错误信息**:

- "您没有权限执行此操作"
- "该用户不是班级学生"
- "用户不存在"

---

### 2.14 将学生踢出班级（老师/班级助理专用）

**接口地址**: `DELETE /api/class/kick-student`

**请求参数**:

| 参数          | 类型    | 必填 | 说明        |
| ------------- | ------- | ---- | ----------- |
| classId       | Integer | 是   | 班级 ID     |
| studentUserId | Integer | 是   | 学生用户 ID |

**请求示例**: `DELETE /api/class/kick-student?classId=1&studentUserId=1002`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

**失败响应**:

```json
{
  "code": 400,
  "message": "该用户不是班级学生",
  "data": null
}
```

**可能的错误信息**:

- "您没有权限执行此操作"
- "该用户不是班级学生"
- "不能移除班级创建者"

**注意**:

- **硬删除成员记录**：踢出学生时会从班级中移除该学生的成员记录
- **级联软清理机制**：踢出学生时会自动软删除该学生在该班级的所有作业提交数据
  - 软删除学生提交的所有附件记录（is_deleted = true）
  - 软删除所有提交记录（is_deleted = true）
  - **保留附件文件**：不物理删除文件，保留完整的作业历史
- **数据可恢复**：作业提交数据和文件都保留，便于审计和恢复

---

### 2.15 取消班级助理权限（降级为学生，仅创建者可用）

**接口地址**: `PUT /api/class/demote-assistant-teacher`

**请求参数**:

| 参数          | 类型    | 必填 | 说明            |
| ------------- | ------- | ---- | --------------- |
| classId       | Integer | 是   | 班级 ID         |
| teacherUserId | Integer | 是   | 班级助理用户 ID |

**请求示例**: `PUT /api/class/demote-assistant-teacher?classId=1&teacherUserId=1002`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

**失败响应**:

```json
{
  "code": 400,
  "message": "只有班级创建者可以执行此操作",
  "data": null
}
```

**可能的错误信息**:

- "只有班级创建者可以执行此操作"
- "该用户不是班级助理"

---

### 2.16 学生邀请用户加入班级（需要用户确认和教师审核）

**接口地址**: `POST /api/class/student/invite`

**请求参数**:

| 参数        | 类型    | 必填 | 说明             |
| ----------- | ------- | ---- | ---------------- |
| classId     | Integer | 是   | 班级 ID          |
| userAccount | String  | 是   | 被邀请用户的账号 |

**请求示例**: `POST /api/class/student/invite?classId=1&userAccount=2024002`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "邀请已发送，待用户确认",
  "data": null
}
```

**失败响应**:

```json
{
  "code": 400,
  "message": "用户不存在",
  "data": null
}
```

**可能的错误信息**:

- "只有班级内的学生才能提交邀请申请"
- "用户不存在"
- "该用户已经在班级中"
- "已有待确认的邀请"

**注意**:

- 学生发起邀请后，需要被邀请用户先确认同意
- 被邀请用户同意后，系统会创建教师审核记录
- 教师或助理审核通过后，被邀请用户才正式加入班级
- 整个流程：**学生发起 → 用户确认 → 教师审核 → 加入班级**
- **重复邀请处理**：如果对同一用户已有待确认的邀请，系统会自动删除旧邀请（包括关联的教师审核记录），然后创建新邀请

---

### 2.17 被邀请用户响应邀请（同意/拒绝）

**接口地址**: `PUT /api/class/respond-user-invitation`

**请求参数**:

| 参数         | 类型    | 必填 | 说明                            |
| ------------ | ------- | ---- | ------------------------------- |
| invitationId | Integer | 是   | 邀请 ID                         |
| accepted     | Boolean | 是   | 是否同意(true-同意，false-拒绝) |

**请求示例**: `PUT /api/class/respond-user-invitation?invitationId=1&accepted=true`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

**注意**:

- 只能响应发给自己的邀请
- 如果同意，系统会创建教师审核记录，等待教师或助理审核
- 如果拒绝，流程结束

**失败响应**:

```json
{
  "code": 400,
  "message": "该邀请已处理",
  "data": null
}
```

**可能的错误信息**:

- “只能响应发给自己的邀请”
- “该邀请已处理”

---

### 2.18 教师或助理审核邀请申请

**接口地址**: `PUT /api/class/approve-teacher-approval`

**请求头**:

- Content-Type: application/json
- 需要登录认证（Session）

**请求体**:

```json
{
  "applicationId": 1,
  "approved": true,
  "comment": "同意加入"
}
```

**字段说明**:

| 字段          | 类型    | 必填 | 说明                              |
| ------------- | ------- | ---- | --------------------------------- |
| applicationId | Integer | 是   | 审核 ID                           |
| approved      | Boolean | 是   | 审核结果（true-通过，false-拒绝） |
| comment       | String  | 否   | 审核意见，最长 256 字符           |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

**失败响应**:

```json
{
  "code": 400,
  "message": "审核记录不存在",
  "data": null
}
```

**可能的错误信息**:

- “审核结果不能为空”
- “审核意见长度不能超过 256 位”
- “审核意见不能包含特殊字符（制表符等）”
- “审核记录不存在”
- “只有班级老师或助理可以审核邀请申请”
- “该申请已处理”

**注意**:

- 只有班级老师或助理可以审核
- 审核通过后，被邀请用户正式以**学生**身份加入班级
- 审核拒绝后，流程结束

---

### 2.19 获取待教师审核的邀请列表（班级老师/助理专用）

**接口地址**: `GET /api/class/teacher-approvals/pending`

**请求参数**:

| 参数    | 类型    | 必填 | 说明    |
| ------- | ------- | ---- | ------- |
| classId | Integer | 是   | 班级 ID |

**请求示例**: `GET /api/class/teacher-approvals/pending?classId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "classId": 1,
      "className": "计算机科学2024级1班",
      "invitationId": 5,
      "inviteeId": 1003,
      "inviteeUsername": "李四",
      "status": 0,
      "reviewerId": null,
      "reviewerUsername": null,
      "reviewTime": null,
      "reviewComment": null,
      "createTime": "2026-04-29T10:00:00"
    }
  ]
}
```

**响应字段说明**:

| 字段             | 类型          | 说明                                     |
| ---------------- | ------------- | ---------------------------------------- |
| id               | Integer       | 审核 ID                                  |
| classId          | Integer       | 班级 ID                                  |
| className        | String        | 班级名称                                 |
| invitationId     | Integer       | 关联的用户邀请 ID                        |
| inviteeId        | Integer       | 被邀请人 ID                              |
| inviteeUsername  | String        | 被邀请人用户名                           |
| status           | Integer       | 教师审核状态(0-待审核,1-已通过,2-已拒绝) |
| reviewerId       | Integer       | 审核人 ID                                |
| reviewerUsername | String        | 审核人用户名                             |
| reviewTime       | LocalDateTime | 审核时间                                 |
| reviewComment    | String        | 审核意见                                 |
| createTime       | LocalDateTime | 创建时间                                 |

**失败响应**:

```json
{
  "code": 400,
  "message": "只有班级老师或助理可以查看待审核邀请",
  "data": null
}
```

**可能的错误信息**:

- “只有班级老师或助理可以查看待审核邀请”

---

### 班级邀请流程说明

系统提供**两种不同的邀请机制**，分别适用于不同场景：

#### 方式一：教师直接邀请（推荐）

**适用场景**: 教师主动邀请学生、助教等加入班级

**流程**: 教师发起 → 发送邀请 → 被邀请人同意 → **直接加入班级**

1. 教师调用 `2.20 教师邀请用户加入班级` 接口
2. 系统创建邀请记录（使用 `class_invitation` 表）并发送给被邀请人
3. 被邀请人调用 `2.21 获取我收到的邀请列表` 查看邀请
4. 被邀请人调用 `2.22 响应邀请` 同意或拒绝
5. **同意后自动以学生身份加入班级**（无需二次审核）

**特点**:

- 单向确认机制：只需被邀请人同意
- 流程简单快速
- 适合教师主动邀请已知人员

---

#### 方式二：学生邀请（需双向确认）

**适用场景**: 班级学生邀请同学加入，需要被邀请人先同意，再由老师审核把关

**流程**: 学生发起 → 用户确认 → 教师审核 → 加入班级

1. 学生调用 `2.16 学生邀请用户加入班级` 接口
2. 系统创建用户邀请记录（使用 `class_user_invitation` 表），状态为待用户确认
3. 被邀请人收到邀请后，调用 `2.17 被邀请用户响应邀请` 同意或拒绝
4. 如果同意，系统创建教师审核记录（使用 `class_teacher_approval` 表），状态为待教师审核
5. 老师或助理调用 `2.19 获取待教师审核的邀请列表` 查看申请
6. 老师或助理调用 `2.18 教师或助理审核邀请申请` 通过或拒绝
7. **审核通过后，被邀请人自动以学生身份加入班级**

**特点**:

- **双向确认机制**：需要被邀请人和教师双方确认
- 流程更加严格，保障各方知情权
- 适合学生邀请同学，避免未经授权的加入

---

### 两种机制的对比

| 特性           | 教师邀请           | 学生邀请                                           |
| -------------- | ------------------ | -------------------------------------------------- |
| **发起者**     | 教师/管理员        | 班级学生                                           |
| **使用的表**   | `class_invitation` | `class_user_invitation` + `class_teacher_approval` |
| **确认次数**   | 1次（被邀请人）    | 2次（被邀请人 + 教师）                             |
| **审核环节**   | 无                 | 有（教师或助理审核）                               |
| **适用场景**   | 教师主动邀请       | 学生邀请同学                                       |
| **流程复杂度** | 简单               | 复杂                                               |
| **安全性**     | 中等               | 高                                                 |

**注意**:

- 所有邀请均需要被邀请人最终确认，保障用户知情权
- 教师邀请和学生邀请是**完全独立的两个流程**，使用不同的数据表和接口
- 被邀请人可以查看邀请详情（班级名称、邀请人等）后再决定

---

### 2.20 教师邀请用户加入班级（需用户同意）

**接口地址**: `POST /api/class/invite-with-approval`

**请求参数**:

| 参数        | 类型    | 必填 | 说明             |
| ----------- | ------- | ---- | ---------------- |
| classId     | Integer | 是   | 班级 ID          |
| userAccount | String  | 是   | 被邀请用户的账号 |

**请求示例**: `POST /api/class/invite-with-approval?classId=1&userAccount=2024001`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "邀请已发送，等待用户响应",
  "data": {
    "id": 1,
    "classId": 1,
    "inviterId": 1001,
    "inviteeUserId": 1002,
    "status": 0,
    "responseTime": null,
    "createTime": "2026-04-09T10:00:00"
  }
}
```

**响应字段说明**:

| 字段          | 类型          | 说明                                 |
| ------------- | ------------- | ------------------------------------ |
| id            | Integer       | 邀请 ID                              |
| classId       | Integer       | 班级 ID                              |
| inviterId     | Integer       | 邀请人 ID(教师)                      |
| inviteeUserId | Integer       | 被邀请人 ID                          |
| status        | Integer       | 邀请状态(0-待处理,1-已同意,2-已拒绝) |
| responseTime  | LocalDateTime | 响应时间                             |
| createTime    | LocalDateTime | 邀请时间                             |

**注意**:

- **只有老师或管理员可以发送邀请**
- **重复邀请会自动替换**：如果对该用户已有待处理邀请，会先删除旧邀请再创建新邀请
- **用户同意后直接加入班级**，无需教师二次审核
- 此接口使用 `class_invitation` 表，与学生邀请的 `class_user_invitation` 表不同

**失败响应**:

```json
{
  "code": 400,
  "message": "已发送过邀请，请等待用户响应",
  "data": null
}
```

**可能的错误信息**:

- "只有老师或管理员可以邀请用户加入班级"
- "用户不存在"
- "该用户已经在班级中"
- "已发送过邀请，请等待用户响应"

---

### 2.21 获取我收到的邀请列表

**接口地址**: `GET /api/class/my-invitations`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | 否 | 状态筛选（0-待处理，1-已同意，2-已拒绝） |

**请求示例**:

- `GET /api/class/my-invitations`
- `GET /api/class/my-invitations?status=0`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "classId": 1,
      "className": "计算机科学2024级1班",
      "inviterId": 1001,
      "inviterName": "张老师",
      "inviteeUserId": 1002,
      "status": 0,
      "responseTime": null,
      "createTime": "2026-04-09T10:00:00"
    }
  ]
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 邀请 ID |
| classId | Integer | 班级 ID |
| className | String | 班级名称 |
| inviterId | Integer | 邀请人 ID |
| inviterName | String | 邀请人姓名 |
| inviteeUserId | Integer | 被邀请人 ID |
| status | Integer | 邀请状态(0-待处理,1-已同意,2-已拒绝) |
| responseTime | LocalDateTime | 响应时间 |
| createTime | LocalDateTime | 邀请时间 |

**注意**:

- 只能查看自己的邀请
- 按创建时间倒序排列
- 包含班级名称和邀请人姓名，方便用户决策

**失败响应**:

```json
{
  "code": 401,
  "message": "用户未登录",
  "data": null
}
```

---

### 2.22 响应邀请（同意/拒绝）

**接口地址**: `PUT /api/class/respond-invitation`

**请求参数**:

| 参数         | 类型    | 必填 | 说明                            |
| ------------ | ------- | ---- | ------------------------------- |
| invitationId | Integer | 是   | 邀请 ID                         |
| accepted     | Boolean | 是   | 是否同意(true-同意，false-拒绝) |

**请求示例**: `PUT /api/class/respond-invitation?invitationId=1&accepted=true`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

**注意**:

- 只能响应发给自己的邀请
- 同意则自动以`学生`身份加入班级
- 拒绝则标记为已拒绝

**失败响应**:

```json
{
  "code": 400,
  "message": "该邀请已处理",
  "data": null
}
```

**可能的错误信息**:

- “只能响应发给自己的邀请”
- “该邀请已处理”

---

### 2.23 生成/刷新班级邀请码（教师专用）

**接口地址**: `POST /api/class/generate-invite-code`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 是 | 班级 ID |

**请求示例**: `POST /api/class/generate-invite-code?classId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "邀请码生成成功",
  "data": "A3F9K2"
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| data | String | 6位随机邀请码（大写字母+数字） |

**注意**:

- 只有老师可以生成邀请码
- 每次调用会生成新的邀请码，旧码失效
- 邀请码有效期直到下次刷新
- 学生可通过`2.23`接口使用邀请码加入班级

**失败响应**:

```json
{
  "code": 400,
  "message": "只有老师可以生成邀请码",
  "data": null
}
```

**可能的错误信息**:

- "用户未登录"
- "班级不存在"
- "只有老师可以生成邀请码"

---

### 2.24 通过邀请码加入班级

**接口地址**: `POST /api/class/join-by-code`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| inviteCode | String | 是 | 6位邀请码 |

**请求示例**: `POST /api/class/join-by-code?inviteCode=A3F9K2`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "classId": 1,
    "applicantId": 1002,
    "status": 1,
    "reviewerId": 1002,
    "reviewTime": "2026-04-09T10:00:00",
    "reviewComment": null,
    "createTime": "2026-04-09T10:00:00"
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 申请 ID（虚拟记录） |
| classId | Integer | 班级 ID |
| applicantId | Integer | 申请人 ID |
| status | Integer | 审核状态(1-已通过) |
| reviewerId | Integer | 审核人 ID（自动审核） |
| reviewTime | LocalDateTime | 审核时间 |
| reviewComment | String | 审核意见 |
| createTime | LocalDateTime | 申请时间 |

**注意**:

- **邀请码加入免审批**：通过正确邀请码加入的用户，直接以学生身份入班，无需老师审核
- `status`固定返回`1`（已通过），表示立即生效
- 邀请码由教师通过`2.23`接口生成
- 不能重复加入同一班级
- 已是班级成员不能再次加入

**失败响应**:

```json
{
  "code": 400,
  "message": "邀请码失效",
  "data": null
}
```

**可能的错误信息**:

- “用户未登录”
- “邀请码不能为空”
- “邀请码失效”
- "您已经是该班级成员"
- "您已有待审核的加入申请"

---

### 2.24 转让班级所有权（仅创建者）

**接口地址**: `PUT /api/class/transfer-ownership`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 是 | 班级 ID |
| newOwnerId | Integer | 是 | 新所有者 ID |

**请求示例**: `PUT /api/class/transfer-ownership?classId=1&newOwnerId=1003`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "班级所有权转让成功",
  "data": null
}
```

**注意**:

- 只有`创建者`可以转让所有权
- 新所有者必须是班级现有成员
- 转让后原`创建者`自动降级为`班级助理`
- 新所有者自动升级为`创建者`并拥有最高权限
- 不能转让给自己

**失败响应**:

```json
{
  "code": 400,
  "message": "只有班级所有者可以转让所有权",
  "data": null
}
```

**可能的错误信息**:

- "用户未登录"
- "班级不存在"
- "只有班级所有者可以转让所有权"
- "新所有者必须是班级成员"
- "不能转让给自己"

---

## 3. 作业提交接口 (WorkSubmissionController)

**基础路径**: `/api/submissions`

### 3.1 提交作业

**接口地址**: `POST /api/submissions/submit`

**请求头**:

- Content-Type: multipart/form-data
- 需要登录认证（Session）

**请求参数** (multipart/form-data):
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workId | Integer | 是 | 作业 ID |
| submissionContent | String | 否 | 提交内容/文本描述 |
| attachments | File[] | 否 | 附件文件列表（支持多文件上传） |

**请求示例**:

```
POST /api/submissions/submit
Content-Type: multipart/form-data

workId: 1
submissionContent: 这是我的作业内容
attachments: [file1.pdf, file2.docx]
```

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "workId": 1,
    "classId": 1,
    "submitterId": 1002,
    "submissionContent": "这是我的作业内容",
    "score": null,
    "comment": null,
    "gradeTime": null,
    "graderId": null,
    "status": 1,
    "isLate": false,
    "createTime": "2026-04-09T10:00:00",
    "updateTime": "2026-04-09T10:00:00"
  }
}
```

**响应字段说明**:

| 字段              | 类型          | 说明                               |
| ----------------- | ------------- | ---------------------------------- |
| id                | Integer       | 提交 ID                            |
| workId            | Integer       | 作业 ID                            |
| classId           | Integer       | 所属班级 ID                        |
| submitterId       | Integer       | 提交人 ID                          |
| submissionContent | String        | 提交内容/文本描述                  |
| score             | BigDecimal    | 提交分数(未批改时为null)           |
| comment           | String        | 批改人评语(未批改时为null)         |
| gradeTime         | LocalDateTime | 批改时间(未批改时为null)           |
| graderId          | Integer       | 批改人 ID(未批改时为null)          |
| status            | Integer       | 提交状态(1-已提交,2-已批改)        |
| isLate            | Boolean       | 是否逾期提交(true-逾期,false-按时) |
| createTime        | LocalDateTime | 创建时间                           |
| updateTime        | LocalDateTime | 更新时间                           |

**失败响应**:

```json
{
  "code": 400,
  "message": "作业不存在",
  "data": null
}
```

**可能的错误信息**:

- "作业 ID 不能为空"
- "作业不存在"
- "只有班级学生可以提交作业"
- "作业未发布或已结束"
- "您已经提交过该作业"
- "作业已截止，不允许逾期提交"
- "文件上传失败：xxx"

**注意**:

- **支持逾期提交**：即使超过截止时间，学生仍然可以提交作业
- 系统会自动标记逾期提交（`isLate: true`），教师可以看到哪些学生是迟交的
- 教师可以根据实际情况决定是否扣减“迟交分”
- **直接上传附件**：通过 `attachments` 参数直接上传文件，无需预先调用文件上传接口
- **文件安全检查**：系统会对上传的文件进行病毒扫描、文件类型白名单验证等安全检查
- **文件存储位置**：`uploads/submissions/` 目录

---

### 3.2 更新提交的作业

**接口地址**: `PUT /api/submissions/update`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionId | Integer | 是 | 提交 ID |
| submissionContent | String | 是 | 更新后的提交内容 |

**请求示例**: `PUT /api/submissions/update?submissionId=1&submissionContent=更新后的作业内容`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "workId": 1,
    "classId": 1,
    "submitterId": 1002,
    "submissionContent": "更新后的作业内容",
    "score": null,
    "comment": null,
    "gradeTime": null,
    "graderId": null,
    "status": 1,
    "createTime": "2026-04-09T10:00:00",
    "updateTime": "2026-04-09T11:00:00"
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 提交 ID |
| workId | Integer | 作业 ID |
| classId | Integer | 所属班级 ID |
| submitterId | Integer | 提交人 ID |
| submissionContent | String | 提交内容/文本描述 |
| score | BigDecimal | 提交分数 |
| comment | String | 批改人评语 |
| gradeTime | LocalDateTime | 批改时间 |
| graderId | Integer | 批改人 ID |
| status | Integer | 提交状态(1-已提交,2-已批改) |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

**失败响应**:

```json
{
  "code": 400,
  "message": "提交记录不存在",
  "data": null
}
```

**可能的错误信息**:

- "提交记录不存在"
- "您无权修改此提交"
- "作业已被批改，无法修改"
- "作业已截止，无法修改"

**注意**:

- 学生只能在作业截止时间之前更新自己的提交
- 已过截止时间的作业不允许更新，即使尚未批改
- 如果作业已被老师批改，也不允许学生修改

---

### 3.3 删除提交的作业

**接口地址**: `DELETE /api/submissions/delete`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionId | Integer | 是 | 提交 ID |

**请求示例**: `DELETE /api/submissions/delete?submissionId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

**失败响应**:

```json
{
  "code": 400,
  "message": "提交记录不存在",
  "data": null
}
```

**可能的错误信息**:

- "提交记录不存在"
- "您无权删除此提交"
- "作业已被批改，无法删除"
- "已过截止时间的作业不能删除"

**注意**:

- 学生在作业截止时间后不能删除自己的提交，防止误操作导致0分
- 教师依然可以删除任何学生的提交

---

### 3.4 查询提交详情

**接口地址**: `GET /api/submissions/detail`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionId | Integer | 是 | 提交 ID |

**请求示例**: `GET /api/submissions/detail?submissionId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "workId": 1,
    "classId": 1,
    "submitterId": 1002,
    "submissionContent": "作业内容",
    "score": 90.5,
    "comment": "完成得很好",
    "gradeTime": "2026-04-10T10:00:00",
    "graderId": 1001,
    "status": 2,
    "createTime": "2026-04-09T10:00:00",
    "updateTime": "2026-04-10T10:00:00"
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 提交 ID |
| workId | Integer | 作业 ID |
| classId | Integer | 所属班级 ID |
| submitterId | Integer | 提交人 ID |
| submissionContent | String | 提交内容/文本描述 |
| score | BigDecimal | 提交分数 |
| comment | String | 批改人评语 |
| gradeTime | LocalDateTime | 批改时间 |
| graderId | Integer | 批改人 ID |
| status | Integer | 提交状态(1-已提交,2-已批改) |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

**失败响应**:

```json
{
  "code": 400,
  "message": "提交记录不存在",
  "data": null
}
```

**可能的错误信息**:

- "提交记录不存在"
- "您无权查看此提交"

---

### 3.5 查询当前用户的提交列表

**接口地址**: `GET /api/submissions/student/list`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workId | Integer | 否 | 作业 ID 筛选（可选） |

**请求示例**:

- `GET /api/submissions/student/list` - 查询当前用户的所有提交
- `GET /api/submissions/student/list?workId=1` - 查询当前用户在指定作业的提交

**功能说明**:

- 查询当前登录用户（学生）的作业提交记录
- 支持按作业 ID 筛选特定作业的提交
- 返回结果按创建时间倒序排列
- 包含完整的提交信息和附件列表

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "workId": 1,
      "workTitle": "第一次作业",
      "submitterId": 1002,
      "submissionContent": "作业内容",
      "score": 90.5,
      "comment": "完成得很好",
      "gradeTime": "2026-04-10T10:00:00",
      "graderId": 1001,
      "status": 2,
      "createTime": "2026-04-09T10:00:00",
      "updateTime": "2026-04-10T10:00:00",
      "attachments": [
        {
          "id": 1,
          "fileName": "homework.pdf",
          "filePath": "/uploads/submissions/homework.pdf",
          "fileSize": 512000,
          "fileType": "application/pdf",
          "uploadTime": "2026-04-09T10:00:00"
        }
      ]
    }
  ]
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 提交 ID |
| workId | Integer | 作业 ID |
| workTitle | String | 作业标题 |
| submitterId | Integer | 提交人 ID |
| submissionContent | String | 提交内容/文本描述 |
| score | BigDecimal | 提交分数 |
| comment | String | 批改人评语 |
| gradeTime | LocalDateTime | 批改时间 |
| graderId | Integer | 批改人 ID |
| status | Integer | 提交状态(1-已提交,2-已批改) |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |
| attachments | List | 附件列表 |
| attachments[].id | Integer | 附件 ID |
| attachments[].fileName | String | 文件名 |
| attachments[].filePath | String | 文件路径 |
| attachments[].fileSize | Long | 文件大小(字节) |
| attachments[].fileType | String | 文件类型(MIME) |
| attachments[].uploadTime | LocalDateTime | 上传时间 |

**失败响应**:

```json
{
  "code": 401,
  "message": "用户未登录",
  "data": null
}
```

---

### 3.6 查询某次作业的所有提交（教师专用，分页）

**接口地址**: `GET /api/submissions/work/list`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workId | Integer | 是 | 作业 ID |
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页大小，默认20，最大300 |

**请求示例**: `GET /api/submissions/work/list?workId=1&pageNum=1&pageSize=20`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 1,
        "workId": 1,
        "workTitle": "第一次作业",
        "submitterId": 1002,
        "submissionContent": "作业内容",
        "score": 90.5,
        "comment": "完成得很好",
        "gradeTime": "2026-04-10T10:00:00",
        "graderId": 1001,
        "status": 2,
        "createTime": "2026-04-09T10:00:00",
        "updateTime": "2026-04-10T10:00:00",
        "attachments": []
      },
      {
        "id": 2,
        "workId": 1,
        "workTitle": "第一次作业",
        "submitterId": 1003,
        "submissionContent": "另一个学生的作业",
        "score": null,
        "comment": null,
        "gradeTime": null,
        "graderId": null,
        "status": 1,
        "createTime": "2026-04-09T11:00:00",
        "updateTime": "2026-04-09T11:00:00",
        "attachments": []
      }
    ],
    "total": 45,
    "size": 20,
    "current": 1,
    "pages": 3
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| records | List | 提交记录列表 |
| total | Long | 总记录数 |
| size | Long | 每页大小 |
| current | Long | 当前页码 |
| pages | Long | 总页数 |

**records 内部字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 提交 ID |
| workId | Integer | 作业 ID |
| workTitle | String | 作业标题 |
| submitterId | Integer | 提交人 ID |
| submissionContent | String | 提交内容/文本描述 |
| score | BigDecimal | 提交分数(未批改时为null) |
| comment | String | 批改人评语(未批改时为null) |
| gradeTime | LocalDateTime | 批改时间(未批改时为null) |
| graderId | Integer | 批改人 ID(未批改时为null) |
| status | Integer | 提交状态(1-已提交,2-已批改) |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |
| attachments | List | 附件列表 |

**失败响应**:

```json
{
  "code": 400,
  "message": "您没有权限查看此作业",
  "data": null
}
```

**可能的错误信息**:

- "您没有权限查看此作业"

---

### 3.7 查询某次作业的已交名单（教师专用）

**接口地址**: `GET /api/submissions/work/submitted`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workId | Integer | 是 | 作业 ID |

**请求示例**: `GET /api/submissions/work/submitted?workId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "workId": 1,
      "workTitle": "第一次作业",
      "submitterId": 1002,
      "submissionContent": "作业内容",
      "score": 90.5,
      "comment": "完成得很好",
      "gradeTime": "2026-04-10T10:00:00",
      "graderId": 1001,
      "status": 2,
      "createTime": "2026-04-09T10:00:00",
      "updateTime": "2026-04-10T10:00:00",
      "attachments": []
    }
  ]
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 提交 ID |
| workId | Integer | 作业 ID |
| workTitle | String | 作业标题 |
| submitterId | Integer | 提交人 ID |
| submissionContent | String | 提交内容/文本描述 |
| score | BigDecimal | 提交分数(未批改时为null) |
| comment | String | 批改人评语(未批改时为null) |
| gradeTime | LocalDateTime | 批改时间(未批改时为null) |
| graderId | Integer | 批改人 ID(未批改时为null) |
| status | Integer | 提交状态(1-已提交,2-已批改) |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |
| attachments | List | 附件列表 |

**注意**:

- 此接口返回已提交学生的完整提交记录
- 按创建时间倒序排列
- 只有班级老师可以调用此接口

**失败响应**:

```json
{
  "code": 400,
  "message": "您没有权限查看此作业",
  "data": null
}
```

**可能的错误信息**:

- "您没有权限查看此作业"
- "作业不存在"

---

### 3.8 查询某次作业的未交名单（教师专用）

**接口地址**: `GET /api/submissions/work/unsubmitted`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workId | Integer | 是 | 作业 ID |

**请求示例**: `GET /api/submissions/work/unsubmitted?workId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1003,
      "username": "张三",
      "userNo": "2024001",
      "email": "zhangsan@example.com"
    },
    {
      "id": 1004,
      "username": "李四",
      "userNo": "2024002",
      "email": "lisi@example.com"
    }
  ]
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 学生 ID |
| username | String | 学生姓名 |
| userNo | String | 学生学号 |
| email | String | 学生邮箱 |

**注意**:

- 此接口直接返回未提交作业的学生列表（User对象）
- **后端使用MyBatisPlus QueryWrapper自动计算**：查询班级所有学生，过滤已提交学生，返回差集
- 只返回`学生`角色的成员，不包括`创建者`和`班级助理`
- 只有班级老师可以调用此接口
- **前端零计算**：直接展示返回数据，无需手动对比或过滤
- **实现位置**：`WorkSubmissionServiceImpl.getUnsubmittedStudents()`

**失败响应**:

```json
{
  "code": 400,
  "message": "您没有权限查看此作业",
  "data": null
}
```

**可能的错误信息**:

- "您没有权限查看此作业"
- "作业不存在"

---

### 3.9 批改作业（教师专用）

**接口地址**: `PUT /api/submissions/grade`

**请求头**:

- Content-Type: application/json
- 需要登录认证（Session）

**请求体**:

```json
{
  "submissionId": 1,
  "score": 90.5,
  "comment": "完成得很好，继续保持！"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
| ------------ | ---------- | ---- | ------------------------------------------ |
| submissionId | Integer | 是 | 提交 ID |
| score | BigDecimal | 是 | 分数，范围 0-100 |
| comment | String | 是 | 批改人评语，不能包含制表符等特殊字符 |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "workId": 1,
    "classId": 1,
    "submitterId": 1002,
    "submissionContent": "作业内容",
    "score": 90.5,
    "comment": "完成得很好，继续保持！",
    "gradeTime": "2026-04-10T10:00:00",
    "graderId": 1001,
    "status": 2,
    "createTime": "2026-04-09T10:00:00",
    "updateTime": "2026-04-10T10:00:00"
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 提交 ID |
| workId | Integer | 作业 ID |
| classId | Integer | 所属班级 ID |
| submitterId | Integer | 提交人 ID |
| submissionContent | String | 提交内容/文本描述 |
| score | BigDecimal | 提交分数 |
| comment | String | 批改人评语 |
| gradeTime | LocalDateTime | 批改时间 |
| graderId | Integer | 批改人 ID |
| status | Integer | 提交状态(1-已提交,2-已批改) |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

**失败响应**:

```json
{
  "code": 400,
  "message": "提交记录不存在",
  "data": null
}
```

**可能的错误信息**:

- "提交 ID 不能为空"
- "分数不能为空"
- "分数不能小于 0"
- "分数超过作业总分"（动态校验：不能超过作业的totalScore，而非固定100分）
- "评语不能为空"
- "评语不能包含特殊字符（制表符等）"
- "提交记录不存在"
- "您没有权限批改此作业"

**注意**:

- 系统支持重新批改已批改的作业，教师可以多次修改分数和评语
- **分数动态校验**：上限为作业的`totalScore`字段值，不再硬编码限制为100分
- 例如：作业总分为150分时，学生可获得0-150分的评分

---

## 附录

### 状态码说明

#### 作业状态 (WorkInfo.status)

- `0`: 未发布
- `1`: 已发布
- `2`: 已结束

#### 申请状态 (ClassApplication.status / ClassInviteApplication.status)

- `0`: 待审核
- `1`: 已通过
- `2`: 已拒绝

#### 提交状态 (WorkSubmission.status)

- `1`: 已提交（学生已提交，待批改）
- `2`: 已批改（教师已完成评分）

**注意**:

- **不存在“未提交”状态（0）**：未交作业的学生在数据库中没有对应的 Submission 记录
- **后端直接计算未交学生**：调用 `GET /api/submissions/work/unsubmitted` 接口即可获取未交学生列表，无需前端手动计算
- **使用MyBatisPlus实现**：通过QueryWrapper查询并过滤，符合项目规范
- **支持逾期提交**：超过deadline后仍然允许提交，系统会标记`isLate: true`
- **逾期标记字段**：`isLate`字段标识是否为逾期提交，教师可以看到哪些学生迟交

#### 角色类型

- `1`: `创建者` - 班级创建者，拥有最高权限（可删除班级、管理班级助理）
- `2`: `班级助理` - 由创建者设置，拥有教师权限但不能删除班级或降级其他班级助理
- `3`: `学生` - 普通班级成员

### 时间格式

所有时间字段均采用 ISO 8601 格式：`yyyy-MM-dd'T'HH:mm:ss`

时区：GMT+8（中国标准时间）

### 注意事项

1. 所有接口均需要进行身份验证，未登录用户将返回 401 错误
2. 部分接口需要特定权限（如教师权限、班级创建者权限等）
3. **作业相关接口使用 multipart/form-data 格式**，直接上传文件，无需预先调用文件上传接口
   - 创建作业：`POST /api/works/create`
   - 更新作业：`PUT /api/works/update`
   - 提交作业：`POST /api/submissions/submit`
4. 日期时间参数需使用 ISO 8601 格式
5. **所有数值型 ID 在请求参数中直接使用 Integer 类型**（不再使用字符串）
