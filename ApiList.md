# Work Management API 接口文档

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
  "message": "success",
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

- Content-Type: application/json
- 需要登录认证（Session）

**请求体**:

```json
{
  "title": "作业标题",
  "description": "作业描述",
  "deadline": "2026-04-15T23:59:59",
  "totalScore": 100,
  "classId": "1",
  "publishTime": "2026-04-09T10:00:00",
  "attachmentPaths": ["/path/to/file1.pdf", "/path/to/file2.doc"]
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 作业标题，最长 128 字符，不能包含换行符、制表符等特殊字符 |
| description | String | 是 | 作业描述，不能包含制表符等特殊字符 |
| deadline | LocalDateTime | 是 | 截止时间，格式：yyyy-MM-dd'T'HH:mm:ss |
| totalScore | Integer | 是 | 作业总分，默认 100 |
| classId | String | 是 | 所属班级 ID，必须是数字字符串 |
| publishTime | LocalDateTime | 是 | 发布时间，格式：yyyy-MM-dd'T'HH:mm:ss |
| attachmentPaths | List<String> | 否 | 附件文件路径列表 |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
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
- "作业标题不能包含特殊字符（换行符、制表符等）"
- "作业描述不能为空"
- "作业描述不能包含特殊字符（制表符等）"
- "截止时间不能为空"
- "作业总分不能为空"
- "所属班级 ID 不能为空"
- "班级 ID 必须是数字"
- "发布时间不能为空"
- "用户无权限在此班级发布作业"
- "班级不存在"

---

### 1.2 更新作业

**接口地址**: `PUT /api/works/update`

**请求头**:

- Content-Type: application/json
- 需要登录认证（Session）

**请求体**:

```json
{
  "id": "1",
  "title": "更新后的作业标题",
  "description": "更新后的作业描述",
  "deadline": "2026-04-20T23:59:59",
  "totalScore": 100,
  "attachmentPaths": ["/path/to/newfile.pdf"]
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | String | 是 | 作业 ID，必须是数字字符串 |
| title | String | 是 | 作业标题，最长 128 字符 |
| description | String | 是 | 作业描述，最长 1024 字符 |
| deadline | LocalDateTime | 是 | 截止时间 |
| totalScore | Integer | 是 | 作业总分 |
| publishTime | LocalDateTime | 否 | 发布时间（仅未发布的作业可修改） |
| attachmentPaths | List<String> | 否 | 附件文件路径列表 |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
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
- "作业 ID 必须是数字"
- "作业标题不能为空"
- "作业描述不能为空"
- "截止时间不能为空"
- "作业总分不能为空"
- "已发布的作业不能修改发布时间"
- "作业不存在"
- "用户无权限修改此作业"

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
  "message": "success",
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

---

### 1.4 查询作业详情

**接口地址**: `GET /api/works/detail`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workId | Integer | 是 | 作业 ID |

**请求示例**: `GET /api/works/detail?workId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
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
  "message": "作业不存在",
  "data": null
}
```

**可能的错误信息**:

- "作业不存在"

---

### 1.5 查询作业列表

**接口地址**: `GET /api/works/list`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| publisherUserNo | String | 否 | 发布人学号/工号筛选 |
| status | Integer | 否 | 作业状态筛选（0-未发布，1-已发布，2-已结束） |

**请求示例**:

- `GET /api/works/list`
- `GET /api/works/list?publisherUserNo=2021001&status=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
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
  ]
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 作业 ID |
| title | String | 作业标题 |
| description | String | 作业描述 |
| publisherId | Integer | 发布人 ID |
| deadline | LocalDateTime | 截止时间 |
| totalScore | Integer | 作业总分 |
| publishTime | LocalDateTime | 发布时间 |
| status | Integer | 作业状态(0-未发布,1-已发布,2-已结束) |
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
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| className | String | 是 | 班级名称，最长 64 字符，不能包含换行符、制表符等特殊字符 |
| description | String | 否 | 班级描述，最长 512 字符，不能包含制表符等特殊字符 |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "创建班级的申请已提交，待审核",
  "data": {
    "id": 1,
    "type": null,
    "classId": null,
    "applicantId": 1001,
    "className": "计算机科学2024级1班",
    "description": "计算机科学与技术专业2024级1班",
    "status": 0,
    "createTime": "2026-04-09T10:00:00"
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 申请 ID |
| type | Integer | 申请类型(已废弃，固定为null) |
| classId | Integer | 班级 ID(审核通过后才会有值) |
| applicantId | Integer | 申请人 ID |
| className | String | 班级名称 |
| description | String | 班级描述 |
| status | Integer | 申请状态(0-待审核,1-已通过,2-已拒绝) |
| createTime | LocalDateTime | 申请时间 |

**注意**: 
- 创建申请仅管理员可审核
- 审核通过后自动创建班级，申请人成为班级创建者(OWNER)

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
  "classId": "1"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | String | 是 | 班级 ID，必须是数字字符串 |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "加入班级的申请已提交，待审核",
  "data": {
    "id": 2,
    "type": null,
    "classId": 1,
    "applicantId": 1002,
    "className": null,
    "description": null,
    "status": 0,
    "createTime": "2026-04-09T10:00:00"
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 申请 ID |
| type | Integer | 申请类型(已废弃，固定为null) |
| classId | Integer | 班级 ID |
| applicantId | Integer | 申请人 ID |
| className | String | 班级名称(固定为null) |
| description | String | 班级描述(固定为null) |
| status | Integer | 申请状态(0-待审核,1-已通过,2-已拒绝) |
| createTime | LocalDateTime | 申请时间 |

**注意**: 
- 加入申请老师和管理员都可审核
- 审核通过后申请人以STUDENT身份加入班级

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
- "班级 ID 必须是数字"
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
  "message": "success",
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

---

### 2.4 删除班级

**接口地址**: `DELETE /api/class/delete`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 是 | 班级 ID |

**请求示例**: `DELETE /api/class/delete?classId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应**:

```json
{
  "code": 400,
  "message": "只有班级创建者可以删除班级",
  "data": null
}
```

**可能的错误信息**:

- "班级不存在"
- "只有班级创建者可以删除班级"

---

### 2.5 获取班级详情

**接口地址**: `GET /api/class/detail`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 是 | 班级 ID |

**请求示例**: `GET /api/class/detail?classId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "className": "计算机科学2024级1班",
    "ownerId": 1001,
    "ownerName": "张三",
    "userRole": "TEACHER",
    "memberCount": 50,
    "teacherCount": 2,
    "studentCount": 48
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 班级 ID |
| className | String | 班级名称 |
| ownerId | Integer | 班级所有者 ID |
| ownerName | String | 班级所有者姓名 |
| userRole | String | 用户在该班级的角色(OWNER/ASSISTANT/STUDENT) |
| memberCount | Long | 成员总数 |
| teacherCount | Long | 教师数量 |
| studentCount | Long | 学生数量 |

**角色说明**:
- `OWNER`: 班级创建者，拥有最高权限（可删除班级、管理助理老师）
- `ASSISTANT`: 助理老师，由创建者设置，拥有教师权限但不能删除班级或降级其他助理老师
- `STUDENT`: 普通学生

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

### 2.6 获取我加入的班级列表

**接口地址**: `GET /api/class/mylist`

**请求参数**: 无

**请求示例**: `GET /api/class/mylist`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "className": "计算机科学2024级1班",
      "ownerId": 1001,
      "ownerName": "张三",
      "userRole": "STUDENT",
      "memberCount": 50,
      "teacherCount": 2,
      "studentCount": 48
    },
    {
      "id": 2,
      "className": "软件工程2024级1班",
      "ownerId": 1003,
      "ownerName": "李四",
      "userRole": "TEACHER",
      "memberCount": 45,
      "teacherCount": 1,
      "studentCount": 44
    }
  ]
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

### 2.7 获取班级成员列表

**接口地址**: `GET /api/class/members`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 是 | 班级 ID |

**请求示例**: `GET /api/class/members?classId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "userId": 1001,
      "userName": "张三",
      "userNo": "2021001",
      "role": "TEACHER",
      "joinTime": "2026-04-01T10:00:00"
    },
    {
      "id": 2,
      "userId": 1002,
      "userName": "王五",
      "userNo": "2024001",
      "role": "STUDENT",
      "joinTime": "2026-04-02T10:00:00"
    }
  ]
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 成员 ID |
| userId | Integer | 用户 ID |
| userName | String | 用户姓名 |
| userNo | String | 学号/工号 |
| role | String | 角色(OWNER/ASSISTANT/STUDENT) |
| joinTime | LocalDateTime | 加入时间 |

**角色说明**:
- `OWNER`: 班级创建者
- `ASSISTANT`: 助理老师
- `STUDENT`: 普通学生

**失败响应**:

```json
{
  "code": 400,
  "message": "您不是该班级成员",
  "data": null
}
```

**可能的错误信息**:

- "班级不存在"
- "您不是该班级成员"

---

### 2.8 检查用户是否在指定班级中

**接口地址**: `GET /api/class/checkmember`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 是 | 班级 ID |

**请求示例**: `GET /api/class/checkmember?classId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "isMember": true,
    "role": "TEACHER"
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| isMember | Boolean | 是否是班级成员 |
| role | String | 角色(OWNER/ASSISTANT/STUDENT)，非成员时为 null |

**角色说明**:
- `OWNER`: 班级创建者
- `ASSISTANT`: 助理老师
- `STUDENT`: 普通学生

**非成员响应**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "isMember": false,
    "role": null
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
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | 否 | 状态筛选（0-待审核，1-已通过，2-已拒绝） |

**请求示例**:

- `GET /api/class/applications/create/list`
- `GET /api/class/applications/create/list?status=0`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "查询创建申请列表成功",
  "data": [
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
  ]
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 申请 ID |
| applicantId | Integer | 申请人 ID |
| className | String | 申请的班级名称 |
| description | String | 申请的班级描述 |
| status | Integer | 申请状态(0-待审核,1-已通过,2-已拒绝) |
| reviewerId | Integer | 审核人 ID |
| reviewTime | LocalDateTime | 审核时间 |
| reviewComment | String | 审核意见 |
| createdClassId | Integer | 审核通过后创建的班级 ID |
| createTime | LocalDateTime | 申请时间 |

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
  "memberId": "1",
  "approved": true,
  "comment": "同意创建"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| memberId | String | 是 | 申请 ID，必须是数字字符串 |
| approved | Boolean | 是 | 是否通过(true-通过，false-拒绝) |
| comment | String | 否 | 审核意见，最长 500 字符 |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**注意**: 
- 仅管理员可审核
- 审核通过后自动创建班级，申请人成为OWNER

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
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 否 | 班级 ID 筛选 |
| status | Integer | 否 | 状态筛选（0-待审核，1-已通过，2-已拒绝） |

**请求示例**:

- `GET /api/class/applications/join/list`
- `GET /api/class/applications/join/list?classId=1&status=0`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "查询加入申请列表成功",
  "data": [
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
  ]
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 申请 ID |
| classId | Integer | 申请加入的班级 ID |
| applicantId | Integer | 申请人 ID |
| status | Integer | 申请状态(0-待审核,1-已通过,2-已拒绝) |
| reviewerId | Integer | 审核人 ID |
| reviewTime | LocalDateTime | 审核时间 |
| reviewComment | String | 审核意见 |
| createTime | LocalDateTime | 申请时间 |

**注意**: 
- 管理员可查看所有班级的申请
- 老师只能查看自己所在班级的申请
- 按创建时间倒序排列

---

### 2.12 审核加入班级申请（老师和管理员专用）

**接口地址**: `PUT /api/class/applications/join/approve`

**请求头**:

- Content-Type: application/json
- 需要登录认证（Session）

**请求体**:

```json
{
  "memberId": "2",
  "approved": true,
  "comment": "同意加入"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| memberId | String | 是 | 申请 ID，必须是数字字符串 |
| approved | Boolean | 是 | 是否通过(true-通过，false-拒绝) |
| comment | String | 否 | 审核意见，最长 500 字符 |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**注意**: 
- 管理员可审核任何班级的申请
- 老师只能审核自己所在班级的申请
- 审核通过后申请人以STUDENT身份加入班级

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

### 2.13 设置学生为助理老师（老师专用）

**接口地址**: `PUT /api/class/set-assistant-teacher`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 是 | 班级 ID |
| studentUserId | Integer | 是 | 学生用户 ID |

**请求示例**: `PUT /api/class/set-assistant-teacher?classId=1&studentUserId=1002`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
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

### 2.14 将学生踢出班级（老师/助理老师专用）

**接口地址**: `DELETE /api/class/remove-student`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 是 | 班级 ID |
| studentUserId | Integer | 是 | 学生用户 ID |

**请求示例**: `DELETE /api/class/remove-student?classId=1&studentUserId=1002`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
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

---

### 2.15 取消助理老师权限（降级为学生，仅创建者可用）

**接口地址**: `PUT /api/class/demote-assistant-teacher`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 是 | 班级 ID |
| teacherUserId | Integer | 是 | 助理老师用户 ID |

**请求示例**: `PUT /api/class/demote-assistant-teacher?classId=1&teacherUserId=1002`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
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
- "该用户不是助理老师"

---

### 2.16 学生邀请用户加入班级（需要审核）

**接口地址**: `POST /api/class/student/invite`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 是 | 班级 ID |
| userAccount | String | 是 | 被邀请用户的账号 |

**请求示例**: `POST /api/class/student/invite?classId=1&userAccount=2024002`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "classId": 1,
    "inviterId": 1002,
    "inviteeAccount": "2024002",
    "status": 0,
    "reviewerId": null,
    "reviewTime": null,
    "reviewComment": null,
    "createTime": "2026-04-09T10:00:00"
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 邀请申请 ID |
| classId | Integer | 班级 ID |
| inviterId | Integer | 邀请人 ID(学生) |
| inviteeAccount | String | 被邀请人账号 |
| status | Integer | 审核状态(0-待审核,1-已通过,2-已拒绝) |
| reviewerId | Integer | 审核人 ID |
| reviewTime | LocalDateTime | 审核时间 |
| reviewComment | String | 审核意见 |
| createTime | LocalDateTime | 邀请时间 |

**失败响应**:

```json
{
  "code": 400,
  "message": "用户不存在",
  "data": null
}
```

**可能的错误信息**:

- "您不是该班级成员"
- "用户不存在"
- "用户已经是该班级成员"
- "已有待审核的邀请申请"

**注意**: 
- 学生发起邀请后，需要老师审核通过
- 审核通过后，被邀请人会收到邀请通知
- **被邀请人需要调用“同意邀请”接口（2.21）才能加入班级**
- 整个流程：学生发起 → 老师审核 → 发送邀请 → 被邀请人同意

---

### 2.17 审核邀请申请（老师/管理员专用）

**接口地址**: `PUT /api/class/invite/approve`

**请求头**:

- Content-Type: application/json
- 需要登录认证（Session）

**请求体**:

```json
{
  "applicationId": "1",
  "approved": true,
  "comment": "同意加入"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| applicationId | String | 是 | 邀请申请 ID，必须是数字字符串 |
| approved | Boolean | 是 | 审核结果（true-通过，false-拒绝） |
| comment | String | 否 | 审核意见，最长 256 字符 |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应**:

```json
{
  "code": 400,
  "message": "邀请申请不存在",
  "data": null
}
```

**可能的错误信息**:

- "邀请申请 ID 不能为空"
- "邀请申请 ID 必须是数字"
- "审核结果不能为空"
- "审核意见长度不能超过 256 位"
- "审核意见不能包含特殊字符（制表符等）"
- "邀请申请不存在"
- "您没有权限审核此申请"
- "该申请已审核"

**注意**: 
- 审核通过后，系统会创建一条邀请记录发送给被邀请人
- **被邀请人需要调用“同意邀请”接口（2.21）才能加入班级**
- 邀请有效期为7天，过期后需重新发起邀请

---

### 2.18 获取待审核的邀请申请列表（班级老师专用）

**接口地址**: `GET /api/class/invite/applications/pending`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 是 | 班级 ID |

**请求示例**: `GET /api/class/invite/applications/pending?classId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "classId": 1,
      "inviterId": 1002,
      "inviteeAccount": "2024002",
      "status": 0,
      "reviewerId": null,
      "reviewTime": null,
      "reviewComment": null,
      "createTime": "2026-04-09T10:00:00"
    }
  ]
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 邀请申请 ID |
| classId | Integer | 班级 ID |
| inviterId | Integer | 邀请人 ID(学生) |
| inviteeAccount | String | 被邀请人账号 |
| status | Integer | 审核状态(0-待审核,1-已通过,2-已拒绝) |
| reviewerId | Integer | 审核人 ID |
| reviewTime | LocalDateTime | 审核时间 |
| reviewComment | String | 审核意见 |
| createTime | LocalDateTime | 邀请时间 |

**失败响应**:

```json
{
  "code": 400,
  "message": "您没有权限查看此班级的邀请申请",
  "data": null
}
```

**可能的错误信息**:

- "您没有权限查看此班级的邀请申请"

---

### 班级邀请流程说明

系统提供两种邀请用户加入班级的方式，均需被邀请人最终确认同意：

#### 方式一：教师直接邀请（推荐）

**流程**: 教师发起 → 发送邀请 → 被邀请人同意 → 加入班级

1. 教师调用 `2.19 教师邀请用户加入班级` 接口
2. 系统创建邀请记录并发送给被邀请人
3. 被邀请人调用 `2.20 获取我收到的邀请列表` 查看邀请
4. 被邀请人调用 `2.21 响应邀请` 同意或拒绝
5. 同意后自动以STUDENT身份加入班级

**适用场景**: 教师主动邀请学生、助教等加入班级

#### 方式二：学生邀请（需审核）

**流程**: 学生发起 → 老师审核 → 发送邀请 → 被邀请人同意 → 加入班级

1. 学生调用 `2.16 学生邀请用户加入班级` 接口
2. 系统创建邀请申请，状态为待审核
3. 老师调用 `2.18 获取待审核的邀请申请列表` 查看申请
4. 老师调用 `2.17 审核邀请申请` 通过或拒绝
5. 审核通过后，系统创建邀请记录发送给被邀请人
6. 被邀请人调用 `2.20 获取我收到的邀请列表` 查看邀请
7. 被邀请人调用 `2.21 响应邀请` 同意或拒绝
8. 同意后自动以STUDENT身份加入班级

**适用场景**: 班级学生邀请同学加入，需要老师审核把关

**注意**:
- 所有邀请均需要被邀请人最终确认，保障用户知情权
- 邀请有效期为7天，过期后需重新发起
- 被邀请人可以查看邀请详情（班级名称、邀请人等）后再决定

---

### 2.19 教师邀请用户加入班级（需用户同意）

**接口地址**: `POST /api/class/invite-with-approval`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classId | Integer | 是 | 班级 ID |
| userAccount | String | 是 | 被邀请用户的账号 |

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
    "expireTime": "2026-04-16T10:00:00",
    "responseTime": null,
    "responseComment": null,
    "createTime": "2026-04-09T10:00:00"
  }
}
```

**响应字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 邀请 ID |
| classId | Integer | 班级 ID |
| inviterId | Integer | 邀请人 ID(教师) |
| inviteeUserId | Integer | 被邀请人 ID |
| status | Integer | 邀请状态(0-待处理,1-已同意,2-已拒绝,3-已过期) |
| expireTime | LocalDateTime | 过期时间(7天后) |
| responseTime | LocalDateTime | 响应时间 |
| responseComment | String | 用户回复说明 |
| createTime | LocalDateTime | 邀请时间 |

**注意**: 
- 只有老师或管理员可以发送邀请
- 邀请有效期为7天
- 防止重复邀请同一用户
- 用户需要主动同意才能加入班级

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

### 2.20 获取我收到的邀请列表

**接口地址**: `GET /api/class/my-invitations`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | 否 | 状态筛选（0-待处理，1-已同意，2-已拒绝，3-已过期） |

**请求示例**:

- `GET /api/class/my-invitations`
- `GET /api/class/my-invitations?status=0`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "classId": 1,
      "className": "计算机科学2024级1班",
      "inviterId": 1001,
      "inviterName": "张老师",
      "inviteeUserId": 1002,
      "status": 0,
      "expireTime": "2026-04-16T10:00:00",
      "responseTime": null,
      "responseComment": null,
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
| status | Integer | 邀请状态(0-待处理,1-已同意,2-已拒绝,3-已过期) |
| expireTime | LocalDateTime | 过期时间 |
| responseTime | LocalDateTime | 响应时间 |
| responseComment | String | 用户回复说明 |
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

### 2.21 响应邀请（同意/拒绝）

**接口地址**: `PUT /api/class/respond-invitation`

**请求头**:

- Content-Type: application/json
- 需要登录认证（Session）

**请求体**:

```json
{
  "invitationId": "1",
  "accepted": true,
  "comment": "很高兴加入这个班级"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| invitationId | String | 是 | 邀请 ID，必须是数字字符串 |
| accepted | Boolean | 是 | 是否同意(true-同意，false-拒绝) |
| comment | String | 否 | 回复说明，最长 500 字符 |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**注意**: 
- 只能响应发给自己的邀请
- 同意则自动以STUDENT身份加入班级
- 拒绝则标记为已拒绝
- 已过期的邀请无法响应

**失败响应**:

```json
{
  "code": 400,
  "message": "邀请已过期",
  "data": null
}
```

**可能的错误信息**:

- "只能响应发给自己的邀请"
- "该邀请已处理"
- "邀请已过期"

---

## 3. 作业提交接口 (WorkSubmissionController)

**基础路径**: `/api/submissions`

### 3.1 提交作业

**接口地址**: `POST /api/submissions/submit`

**请求头**:

- Content-Type: application/json
- 需要登录认证（Session）

**请求体**:

```json
{
  "workId": "1",
  "submissionContent": "这是我的作业内容",
  "attachmentPaths": [
    "/uploads/submissions/file1.pdf",
    "/uploads/submissions/file2.docx"
  ]
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workId | String | 是 | 作业 ID，必须是数字字符串 |
| submissionContent | String | 否 | 提交内容/文本描述，不能包含制表符等特殊字符 |
| attachmentPaths | List<String> | 否 | 附件文件路径列表 |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "workId": 1,
    "classId": 1,
    "submitterId": 1002,
    "submissionContent": "这是我的作业内容",
    "score": null,
    "comment": null,
    "submitTime": "2026-04-09T10:00:00",
    "gradeTime": null,
    "graderId": null,
    "status": 1,
    "createTime": "2026-04-09T10:00:00",
    "updateTime": "2026-04-09T10:00:00"
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
| score | BigDecimal | 提交分数(未批改时为null) |
| comment | String | 批改人评语(未批改时为null) |
| submitTime | LocalDateTime | 提交时间 |
| gradeTime | LocalDateTime | 批改时间(未批改时为null) |
| graderId | Integer | 批改人 ID(未批改时为null) |
| status | Integer | 提交状态(1-已提交,2-已批改) |
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
- "作业 ID 必须是数字"
- "提交内容不能包含特殊字符（制表符等）"
- "作业不存在"
- "您不是该班级的成员"
- "您已经提交过此作业"
- "作业已截止，无法提交"

**注意**: 
- 学生必须在作业截止时间之前提交
- 已过截止时间的作业不允许提交

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
  "message": "success",
  "data": {
    "id": 1,
    "workId": 1,
    "classId": 1,
    "submitterId": 1002,
    "submissionContent": "更新后的作业内容",
    "score": null,
    "comment": null,
    "submitTime": "2026-04-09T10:00:00",
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
| submitTime | LocalDateTime | 提交时间 |
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
  "message": "success",
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
  "message": "success",
  "data": {
    "id": 1,
    "workId": 1,
    "classId": 1,
    "submitterId": 1002,
    "submissionContent": "作业内容",
    "score": 90.5,
    "comment": "完成得很好",
    "submitTime": "2026-04-09T10:00:00",
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
| submitTime | LocalDateTime | 提交时间 |
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

### 3.5 查询学生的提交列表

**接口地址**: `GET /api/submissions/student/list`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workId | Integer | 否 | 作业 ID 筛选 |

**请求示例**:

- `GET /api/submissions/student/list`
- `GET /api/submissions/student/list?workId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "workId": 1,
      "workTitle": "第一次作业",
      "submitterId": 1002,
      "submissionContent": "作业内容",
      "score": 90.5,
      "comment": "完成得很好",
      "submitTime": "2026-04-09T10:00:00",
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
| submitTime | LocalDateTime | 提交时间 |
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

### 3.6 查询某次作业的所有提交（教师专用）

**接口地址**: `GET /api/submissions/work/list`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workId | Integer | 是 | 作业 ID |

**请求示例**: `GET /api/submissions/work/list?workId=1`

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "workId": 1,
      "workTitle": "第一次作业",
      "submitterId": 1002,
      "submissionContent": "作业内容",
      "score": 90.5,
      "comment": "完成得很好",
      "submitTime": "2026-04-09T10:00:00",
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
      "submitTime": "2026-04-09T11:00:00",
      "gradeTime": null,
      "graderId": null,
      "status": 1,
      "createTime": "2026-04-09T11:00:00",
      "updateTime": "2026-04-09T11:00:00",
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
| submitTime | LocalDateTime | 提交时间 |
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
  "message": "success",
  "data": [
    {
      "id": 1,
      "workId": 1,
      "workTitle": "第一次作业",
      "submitterId": 1002,
      "submissionContent": "作业内容",
      "score": 90.5,
      "comment": "完成得很好",
      "submitTime": "2026-04-09T10:00:00",
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
| submitTime | LocalDateTime | 提交时间 |
| gradeTime | LocalDateTime | 批改时间(未批改时为null) |
| graderId | Integer | 批改人 ID(未批改时为null) |
| status | Integer | 提交状态(1-已提交,2-已批改) |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |
| attachments | List | 附件列表 |

**注意**: 
- 此接口返回已提交学生的完整提交记录
- 按提交时间倒序排列
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
  "message": "success",
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
- **后端使用MyBatisPlus查询并计算**，自动过滤已提交学生
- 只返回STUDENT角色的学生，不包括OWNER和ASSISTANT
- 只有班级老师可以调用此接口
- **前端零计算**：直接展示返回数据，无需任何处理

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
  "submissionId": "1",
  "score": 90.5,
  "comment": "完成得很好，继续保持！"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionId | String | 是 | 提交 ID，必须是数字字符串 |
| score | BigDecimal | 是 | 分数，范围 0-100 |
| comment | String | 是 | 批改人评语，不能包含制表符等特殊字符 |

**成功响应 (200)**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "workId": 1,
    "classId": 1,
    "submitterId": 1002,
    "submissionContent": "作业内容",
    "score": 90.5,
    "comment": "完成得很好，继续保持！",
    "submitTime": "2026-04-09T10:00:00",
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
| submitTime | LocalDateTime | 提交时间 |
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
- "提交 ID 必须是数字"
- "分数不能为空"
- "分数不能小于 0"
- "分数超过作业总分"
- "评语不能为空"
- "评语不能包含特殊字符（制表符等）"
- "提交记录不存在"
- "您没有权限批改此作业"

**注意**: 
- 系统支持重新批改已批改的作业，教师可以修改分数和评语
- 分数上限动态校验，不能超过作业的 `totalScore`

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

- `1`: 已提交
- `2`: 已批改

**注意**: 
- 不存在“未提交”状态（0），未交作业的学生在数据库中没有对应的 Submission 记录
- **后端直接计算未交学生**：调用 `GET /api/submissions/work/unsubmitted` 接口即可获取未交学生列表，无需前端手动计算
- **使用MyBatisPlus实现**：通过QueryWrapper查询并过滤，符合项目规范

#### 角色类型

- `OWNER`: 班级创建者，拥有最高权限（可删除班级、管理助理老师）
- `ASSISTANT`: 助理老师，由创建者设置，拥有教师权限但不能删除班级或降级其他助理老师
- `STUDENT`: 普通学生

### 时间格式

所有时间字段均采用 ISO 8601 格式：`yyyy-MM-dd'T'HH:mm:ss`

时区：GMT+8（中国标准时间）

### 注意事项

1. 所有接口均需要进行身份验证，未登录用户将返回 401 错误
2. 部分接口需要特定权限（如教师权限、班级创建者权限等）
3. 文件上传功能需要先调用文件上传接口获取文件路径，再将路径传入相应接口
4. 日期时间参数需使用 ISO 8601 格式
5. 所有数值型 ID 在请求体中均以字符串形式传递，在请求参数中以整数形式传递
