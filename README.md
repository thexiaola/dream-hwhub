# Dream HWHub - 梦学簿

Dream HWHub（梦学簿） 是一个作业管理系统，旨在提供高效的学生作业提交和管理功能。

## 功能特性

- **模块化架构**：采用清晰的业务模块划分，便于维护和扩展
- **用户管理**：支持用户注册、登录和权限管理
- **验证码系统**：集成邮箱验证码验证机制
- **邀请码系统**：支持生成和验证邀请码（权限等级>50）
- **数据库集成**：使用 MyBatis-Plus 进行数据库操作
- **安全认证**：集成 Spring Security 提供安全保护
- **邮件服务**：集成邮件发送功能，用于验证码和通知
- **配置安全管理**：敏感配置信息外部化管理，支持开发环境隔离

## 技术栈

- **后端框架**：Spring Boot 4.0.2
- **架构模式**：模块化分层架构
- **ORM 框架**：MyBatis-Plus 3.5.15
- **数据库**：MySQL
- **邮件服务**：Spring Boot Mail Starter
- **安全框架**：Spring Security
- **构建工具**：Gradle

## 开发环境

- Java 25
- MySQL 9.5.0
- Gradle 9.3.1

## 快速开始

### 1. 克隆项目

```bash
git clone https://gitee.com/thexiaola/dream-hwhub.git
cd dream-hwhub
```

### 2. 配置数据库

确保 MySQL 服务正在运行，然后修改 `src/main/resources/application.properties` 文件中的数据库连接信息：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/dream_hwhub?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&allowMultiQueries=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
```

### 3. 配置邮件服务

项目采用安全的配置管理方式：

#### 3.1 获取示例配置
```bash
# 复制示例配置文件
cp src/main/resources/mail-config.properties src/main/resources/mail-config.properties.example
```

#### 3.2 配置邮件参数
编辑 `mail-config.properties.example` 文件，填入真实的邮件配置：

```properties
# 邮件服务器配置
spring.mail.host=smtp.qq.com
spring.mail.port=465
spring.mail.username=your-email@qq.com
spring.mail.password=your-app-password
spring.mail.properties.mail.from.nickname=发信人昵称
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.ssl.enable=true
```

#### 3.3 配置文件说明
- `mail-config.properties.example` - Git跟踪的示例配置文件（不含敏感信息）
- `mail-config.properties` - 本地使用的实际配置文件（Git忽略）

### 4. 启动应用

```bash
./gradlew bootRun
```

应用将在 `http://localhost:8080` 上运行。

## API 接口

### 响应格式规范

所有API接口都遵循统一的响应格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

**字段说明：**
- `code`: 状态码（200成功，400参数错误，401未授权，403权限不足，500服务器错误）
- `message`: 响应消息
- `data`: 响应数据（无数据时为null）

### 用户相关接口

#### 1. 用户登录

- **POST** `/api/users/login`
- **请求体：**
  ```json
  {
    "userNo": "学号/工号",
    "email": "邮箱",
    "password": "密码"
  }
  ```
- **响应示例：**
  ```json
  {
    "code": 200,
    "message": "登录成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "user": {
        "id": 1,
        "userNo": "STU001",
        "username": "张三",
        "email": "zhangsan@example.com",
        "permission": 10
      }
    }
  }
  ```

#### 2. 用户注册

- **POST** `/api/users/register`
- **请求体：**
  ```json
  {
    "userNo": "学号/工号",
    "username": "用户名",
    "email": "邮箱",
    "password": "密码",
    "verificationCode": "验证码",
    "invitationCode": "邀请码(可选)"
  }
  ```
- **响应示例：**
  ```json
  {
    "code": 200,
    "message": "注册成功",
    "data": {
      "id": 1,
      "userNo": "STU001",
      "username": "张三",
      "email": "zhangsan@example.com",
      "permission": 10
    }
  }
  ```

#### 3. 发送注册验证码

- **POST** `/api/users/getregcode`
- **请求体：**
  ```json
  {
    "userNo": "学号/工号",
    "email": "邮箱"
  }
  ```
- **响应：**
  ```json
  {
    "code": 200,
    "message": "验证码已发送",
    "data": null
  }
  ```

### 管理员用户管理接口（权限等级>50）

#### 4. 创建用户
**POST** `/api/admin/users`

**请求头:**
- `Authorization: Bearer {admin_token}`

**请求体:**
```json
{
  "userNo": "学号/工号",
  "username": "用户名",
  "email": "邮箱",
  "password": "密码",
  "permission": 10
}
```

**权限要求:** 权限等级 > 50

**示例请求:**
```bash
curl -X POST "http://localhost:8080/api/admin/users" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -d '{
    "userNo": "STU001",
    "username": "张三",
    "email": "zhangsan@example.com",
    "password": "123456",
    "permission": 10
  }'
```

**响应示例:**
```json
{
  "code": 200,
  "message": "用户创建成功",
  "data": {
    "id": 1,
    "userNo": "STU001",
    "username": "张三",
    "email": "zhangsan@example.com",
    "permission": 10
  }
}
```

#### 5. 更新用户
**PUT** `/api/admin/users/{id}`

**路径参数:**
- `id`: 用户ID

**请求头:**
- `Authorization: Bearer {admin_token}`

**请求体:**
```json
{
  "userNo": "学号/工号",
  "username": "用户名",
  "email": "邮箱",
  "password": "新密码(可选)",
  "permission": 20
}
```

**权限要求:** 权限等级 > 50

**示例请求:**
```bash
curl -X PUT "http://localhost:8080/api/admin/users/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -d '{
    "username": "李四",
    "permission": 20
  }'
```

**响应示例:**
```json
{
  "code": 200,
  "message": "用户更新成功",
  "data": {
    "id": 1,
    "userNo": "STU001",
    "username": "李四",
    "email": "zhangsan@example.com",
    "permission": 20
  }
}
```

#### 6. 删除用户
**DELETE** `/api/admin/users/{id}`

**路径参数:**
- `id`: 用户ID

**请求头:**
- `Authorization: Bearer {admin_token}`

**权限要求:** 权限等级 > 50

**示例请求:**
```bash
curl -X DELETE "http://localhost:8080/api/admin/users/1" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

**响应示例:**
```json
{
  "code": 200,
  "message": "用户删除成功",
  "data": null
}
```

#### 7. 查询用户列表（分页）
**POST** `/api/admin/users/list`

**请求头:**
- `Authorization: Bearer {admin_token}`

**请求体:**
```json
{
  "page": 1,
  "size": 30,
  "keyword": "搜索关键词",
  "permission": 10
}
```

**参数说明:**
- `page`: 页码（默认1）
- `size`: 每页大小（默认30，最大100）
- `keyword`: 搜索关键词（可搜索学号、用户名、邮箱）
- `permission`: 权限筛选

**权限要求:** 权限等级 > 50

**示例请求:**
```bash
curl -X POST "http://localhost:8080/api/admin/users/list" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -d '{
    "page": 1,
    "size": 30,
    "keyword": "张三"
  }'
```

**响应示例:**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "id": 1,
        "userNo": "STU001",
        "username": "张三",
        "email": "zhangsan@example.com",
        "permission": 10
      }
    ],
    "total": 1,
    "page": 1,
    "size": 30,
    "pages": 1
  }
}
```

#### 8. 获取用户详情
**GET** `/api/admin/users/{id}`

**路径参数:**
- `id`: 用户ID

**请求头:**
- `Authorization: Bearer {admin_token}`

**权限要求:** 权限等级 > 50

**示例请求:**
```bash
curl -X GET "http://localhost:8080/api/admin/users/1" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

**响应示例:**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 1,
    "userNo": "STU001",
    "username": "张三",
    "email": "zhangsan@example.com",
    "permission": 10,
    "createdTime": "2024-01-01T10:00:00",
    "updatedTime": "2024-01-01T10:00:00"
  }
}
```

### 管理员功能重要说明

- **用户ID管理**: 用户ID由数据库自增管理，创建用户时不能显式指定
- **权限控制**: 所有管理员API都需要权限等级大于50
- **分页机制**: 默认每页30条记录，支持最大每页100条记录
- **数据验证**: 学号和邮箱具有唯一性约束，自动检查重复
- **安全措施**: 密码在存储前会进行MD5加密，所有操作都有详细日志记录

### 邀请码相关接口（仅限管理员，权限等级>50）

#### 9. 批量生成邀请码
**POST** `/api/invitations/generate`

**请求头:**
- `Authorization: Bearer {admin_token}`

**请求参数:**
- `count` (Integer, 必填): 生成数量（1-100）
- `expireDays` (Integer, 可选, 默认30): 有效期天数

**权限要求:** 权限等级 > 50

**示例请求:**
```bash
curl -X POST "http://localhost:8080/api/invitations/generate?count=5&expireDays=30" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

**响应示例:**
```json
{
  "code": 200,
  "message": "邀请码生成成功",
  "data": [
    "A1B2C3D4",
    "E5F6G7H8",
    "I9J0K1L2",
    "M3N4O5P6",
    "Q7R8S9T0"
  ]
}
```

#### 10. 查看所有邀请码
**GET** `/api/invitations/all`

**请求头:**
- `Authorization: Bearer {admin_token}`

**权限要求:** 权限等级 > 50

**示例请求:**
```bash
curl -X GET "http://localhost:8080/api/invitations/all" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

**响应示例:**
```json
{
  "code": 200,
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "code": "A1B2C3D4",
      "creatorId": 1,
      "usedCount": 0,
      "maxUsage": 1,
      "createdTime": "2024-01-01T10:00:00",
      "expireTime": "2024-01-31T10:00:00",
      "isActive": true
    }
  ]
}
```

#### 11. 删除邀请码
**DELETE** `/api/invitations/{code}`

**路径参数:**
- `code` (String, 必填): 要删除的邀请码

**请求头:**
- `Authorization: Bearer {admin_token}`

**权限要求:** 权限等级 > 50

**示例请求:**
```bash
curl -X DELETE "http://localhost:8080/api/invitations/A1B2C3D4" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

**响应示例:**
```json
{
  "code": 200,
  "message": "邀请码删除成功",
  "data": null
}
```

#### 12. 验证邀请码（公开接口）
**GET** `/api/invitations/validate/{code}`

**路径参数:**
- `code`: 要验证的邀请码

**权限要求:** 无需登录

**示例请求:**
```bash
curl -X GET "http://localhost:8080/api/invitations/validate/A1B2C3D4"
```

**响应示例:**
```json
{
  "code": 200,
  "message": "邀请码有效",
  "data": {
    "code": "A1B2C3D4",
    "isValid": true
  }
}
```

### 邀请码系统重要说明

- **使用限制**: 每个邀请码只能使用一次
- **批量生成**: 管理员可以批量生成邀请码（最多100个）
- **统一管理**: 管理员可以查看和删除所有邀请码
- **自动验证**: 注册时使用邀请码，系统会自动验证并标记为已使用

**注册接口保持不变:**
**POST** `/api/users/register`

注册时在请求体中包含 `invitationCode` 字段即可。

## 错误码说明

| 错误码 | 说明 | 场景 |
|--------|------|------|
| 200 | 成功 | 操作成功执行 |
| 400 | 参数错误 | 请求参数格式不正确或缺失 |
| 401 | 未授权 | Token无效或已过期 |
| 403 | 权限不足 | 权限等级不够 |
| 404 | 资源不存在 | 请求的资源未找到 |
| 500 | 服务器错误 | 系统内部错误 |

## 使用示例

### 完整的用户注册流程

1. **发送验证码**
```bash
curl -X POST "http://localhost:8080/api/users/getregcode" \
  -H "Content-Type: application/json" \
  -d '{
    "userNo": "STU2024001",
    "email": "student@example.com"
  }'
```

2. **用户注册**
```bash
curl -X POST "http://localhost:8080/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "userNo": "STU2024001",
    "username": "学生甲",
    "email": "student@example.com",
    "password": "123456",
    "verificationCode": "123456"
  }'
```

### 管理员操作示例

1. **管理员登录获取Token**
```bash
curl -X POST "http://localhost:8080/api/users/login" \
  -H "Content-Type: application/json" \
  -d '{
    "userNo": "ADMIN001",
    "email": "admin@example.com",
    "password": "admin123"
  }'
```

2. **使用Token创建用户**
```bash
curl -X POST "http://localhost:8080/api/admin/users" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "userNo": "STU2024002",
    "username": "学生乙",
    "email": "student2@example.com",
    "password": "123456",
    "permission": 10
  }'
```

### 邀请码管理示例

1. **生成邀请码**
```bash
curl -X POST "http://localhost:8080/api/invitations/generate?count=3&expireDays=7" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

2. **验证邀请码**
```bash
curl -X GET "http://localhost:8080/api/invitations/validate/ABC123DE"
```

3. **查看所有邀请码**
```bash
curl -X GET "http://localhost:8080/api/invitations/all" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

项目集成了邮件服务，用于发送验证码。配置文件采用安全管理模式：

### 配置文件结构
```
src/main/resources/
├── mail-config.properties            # 本地配置文件（Git跟踪）
├── mail-config.properties.example    # 示例配置文件（Git忽略）
└── application.properties            # 主配置文件
```

### 配置项说明
```properties
# SMTP服务器配置
spring.mail.host=smtp.qq.com        # 邮件服务器地址
spring.mail.port=465                # 端口号
spring.mail.username=xxx@qq.com     # 发件人邮箱
spring.mail.password=xxxxxx         # 邮箱授权码
spring.mail.properties.mail.from.nickname=发信人昵称  # 发件人显示名称
```

## 项目结构

```
src/
├── main/
│   ├── java/top/thexiaola/dreamhwhub/
│   │   ├── shared/         # 共享组件
│   │   ├── config/         # 全局配置类
│   │   ├── dto/            # 全局数据传输对象
│   │   ├── util/           # 工具类
│   │   ├── module/         # 业务模块
│   │   │   ├── login/      # 登录认证模块
│   │   │   │   ├── controller/
│   │   │   │   ├── domain/
│   │   │   │   ├── dto/
│   │   │   │   ├── mapper/
│   │   │   │   └── service/
│   │   │   └── adminuser/  # 管理员用户管理模块
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       └── service/
│   │   └── DreamHwhubApplication.java  # 主应用类
│   └── resources/
│       ├── application.properties      # 应用配置
│       ├── mail-config.properties      # 邮件配置
│       ├── logback-spring.xml         # 日志配置
│       └── schema.sql                 # 数据库初始化脚本
```

## 数据库初始化

项目支持自动数据库表创建功能：

### 自动创建（推荐）
项目启动时会自动检查并创建所需的数据库表，无需手动执行SQL脚本。

### 手动创建
如果需要手动初始化，可以执行以下脚本：

```bash
# 初始化所有表结构
mysql -u root -p < src/main/resources/schema.sql

# 或者只初始化邀请码表
mysql -u root -p < init_invitation_table.sql
```

### 表结构说明
- `user` 表：存储用户基本信息
- `invitation_code` 表：存储邀请码信息

所有表创建都具有防重复机制，可安全重复执行。

## 安全说明

- 密码采用 MD5 加密存储
- 验证码具有时效性（默认30分钟过期）
- 实现了重复注册防护机制
- 敏感配置信息外部化管理
- 邮件配置文件Git忽略保护

## 许可证

本项目采用 AGPL-3.0 许可证。