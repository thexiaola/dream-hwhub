# Dream HWHub - 梦学簿

Dream HWHub（梦学簿）是一个基于 Spring Boot 的现代化作业管理系统，采用模块化架构设计，提供安全可靠的用户认证和作业管理功能。

## 功能特性

- **用户认证系统**：完整的用户注册、登录流程，支持JWT令牌认证
- **邮箱验证码**：集成邮件服务，提供安全的验证码验证机制
- **权限管理**：基于权限级别的访问控制
- **模块化架构**：清晰的业务模块划分，遵循分层设计理念
- **数据库集成**：使用 MyBatis-Plus ORM框架，支持自动建表
- **配置管理**：敏感信息外部化配置，支持多环境部署
- **日志系统**：自定义滚动策略，支持完整的操作追踪

## 技术栈

- **核心框架**：Spring Boot 4.0.3
- **架构模式**：模块化分层架构（Controller-Service-Mapper）
- **ORM框架**：MyBatis-Plus 3.5.15
- **数据库**：MySQL 9.5.0
- **认证机制**：JWT (JSON Web Token)
- **邮件服务**：Spring Boot Mail Starter
- **日志系统**：Logback + 自定义滚动策略
- **构建工具**：Gradle 9.3.0
- **Java版本**：Java 25

## 开发环境要求

- **Java**: JDK 25 或更高版本
- **数据库**: MySQL 9.5.0 或兼容版本
- **构建工具**: Gradle 9.3.0 或更高版本
- **操作系统**: Linux/macOS/Windows (推荐Linux)

## 快速开始

### 1. 克隆项目

```bash
git clone https://gitee.com/thexiaola/dream-hwhub.git
cd dream-hwhub
```

### 2. 数据库配置

确保 MySQL 服务正在运行，项目支持两种数据库配置方式：

#### 方式一：自动建表（推荐）
项目启动时会自动创建所需的数据库表，无需手动执行SQL脚本。

#### 方式二：手动建表
如需手动初始化数据库，可以执行：

```bash
mysql -u root -p < src/main/resources/schema.sql
```

#### 数据库连接配置
修改 `src/main/resources/application.properties` 文件中的数据库连接信息：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/dream_hwhub?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&allowMultiQueries=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3. 邮件服务配置

项目采用安全的配置管理方式，邮件配置文件已加入 `.gitignore` 保护：

#### 3.1 配置文件准备
```bash
# 从示例文件复制配置模板
cp src/main/resources/mail-config.properties.example src/main/resources/mail-config.properties
```

#### 3.2 填写邮件配置
编辑 `src/main/resources/mail-config.properties` 文件，填入真实的邮件配置：

```properties
# 邮件服务器配置
spring.mail.host=smtp.qq.com
spring.mail.port=465
spring.mail.username=your-email@qq.com
spring.mail.password=your-app-password
spring.mail.properties.mail.from.nickname=梦学簿系统
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.ssl.enable=true
```

#### 3.3 配置说明
- `spring.mail.host`: 邮件服务器SMTP地址
- `spring.mail.port`: SMTP端口（QQ邮箱建议使用465）
- `spring.mail.username`: 发件人邮箱账号
- `spring.mail.password`: 邮箱授权码（非登录密码）
- 配置文件 `mail-config.properties` 已被 `.gitignore` 忽略，确保敏感信息安全

### 4. 启动应用

```bash
./gradlew bootRun
```

应用将在 `http://localhost:8080` 上运行。

## API 接口文档

### 统一响应格式

所有API接口遵循统一的JSON响应格式：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

**响应字段说明：**
- `code`: 状态码（200成功，400参数错误，401未授权，403权限不足，500服务器错误）
- `msg`: 响应消息
- `data`: 响应数据（无数据时为null）

### 认证机制
系统采用JWT令牌认证，用户登录/注册成功后会获得token，后续请求需在Header中携带：

```http
Authorization: Bearer YOUR_JWT_TOKEN
```

### 核心API接口

#### 用户认证接口

##### 1. 用户登录
- **方法**: `POST /api/users/login`
- **请求体**:
  ```json
  {
    "userNo": "学号/工号",
    "email": "邮箱",
    "password": "密码"
  }
  ```
- **成功响应**:
  ```json
  {
    "code": 200,
    "msg": "登录成功！",
    "data": {
      "user": {
        "id": 1,
        "userNo": "STU001",
        "username": "张三",
        "email": "zhangsan@example.com",
        "permission": 10
      },
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "expiresIn": 43200,
      "isLoggedIn": true
    }
  }
  ```

##### 2. 用户注册
- **方法**: `POST /api/users/register`
- **说明**: 注册成功后自动登录并返回JWT token
- **请求体**:
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
- **成功响应**:
  ```json
  {
    "code": 200,
    "msg": "注册成功并已自动登录！",
    "data": {
      "user": {
        "id": 1,
        "userNo": "STU001",
        "username": "张三",
        "email": "zhangsan@example.com",
        "permission": 1
      },
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "expiresIn": 43200,
      "isLoggedIn": true
    }
  }
  ```

##### 3. 发送注册验证码
- **方法**: `POST /api/users/getregcode`
- **请求体**:
  ```json
  {
    "userNo": "学号/工号",
    "email": "邮箱"
  }
  ```
- **成功响应**:
  ```json
  {
    "code": 200,
    "msg": "验证码发送成功！",
    "data": null
  }
  ```

#### 用户信息检查接口

##### 4. 检查学号是否可用
- **方法**: `GET /api/users/check/userno?userNo={学号}`
- **成功响应**:
  ```json
  {
    "code": 200,
    "message": "检查成功",
    "data": {
      "exists": false
    }
  }
  ```

##### 5. 检查用户名是否可用
- **方法**: `GET /api/users/check/username?username={用户名}`
- **成功响应**:
  ```json
  {
    "code": 200,
    "message": "检查成功",
    "data": {
      "exists": false
    }
  }
  ```

##### 6. 检查邮箱是否可用
- **方法**: `GET /api/users/check/email?email={邮箱}`
- **成功响应**:
  ```json
  {
    "code": 200,
    "message": "检查成功",
    "data": {
      "exists": false
    }
  }
  ```





## 错误码参考

### HTTP状态码

| 状态码 | 说明    | 使用场景         |
|-----|-------|--------------|
| 200 | 成功    | 操作成功执行       |
| 400 | 参数错误  | 请求参数格式不正确或缺失 |
| 401 | 未授权   | Token无效或已过期  |
| 403 | 权限不足  | 权限等级不够       |
| 404 | 资源不存在 | 请求的资源未找到     |
| 409 | 冲突    | 数据冲突（如重复注册）  |
| 500 | 服务器错误 | 系统内部错误       |

### 业务错误码

| 错误码  | 说明     | 使用场景       |
|------|--------|------------|
| 1001 | 验证码错误  | 验证码不正确或已过期 |
| 1003 | 用户已存在  | 学号或邮箱已被注册  |
| 1004 | 权限不足   | 操作需要更高权限   |
| 1005 | 数据验证失败 | 输入数据不符合要求  |

## 使用示例

### 完整用户注册流程

1. **发送验证码**
```bash
curl -X POST "http://localhost:8080/api/users/getregcode" \
  -H "Content-Type: application/json" \
  -d '{
    "userNo": "STU2024001",
    "email": "student@example.com"
  }'
```

2. **用户注册（自动登录）**
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

**响应特点：** 注册成功后系统自动为用户生成JWT token，前端可直接使用返回的token进行后续API调用。

### 用户登录示例

```bash
curl -X POST "http://localhost:8080/api/users/login" \
  -H "Content-Type: application/json" \
  -d '{
    "userNo": "STU2024001",
    "email": "student@example.com",
    "password": "123456"
  }'
```

### 带认证的API调用

```bash
curl -X GET "http://localhost:8080/api/protected/resource" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```



项目集成了邮件服务，用于发送验证码。配置文件采用安全管理模式：

### 配置文件结构
```
src/main/resources/
├── mail-config.properties            # 本地配置文件（Git跟踪）
├── mail-config.properties.example    # 示例配置文件（Git忽略）
├── application.properties            # 主配置文件
└── logback-spring.xml               # 日志配置文件
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
.
├── src/
│   ├── main/
│   │   ├── java/top/thexiaola/dreamhwhub/
│   │   │   ├── config/                          # 全局配置类
│   │   │   │   ├── LogSystem.java               # 自定义日志滚动策略
│   │   │   │   ├── MailConfig.java              # 邮件配置类
│   │   │   │   └── WebConfig.java               # Web配置类
│   │   │   ├── module/                          # 业务模块
│   │   │   │   └── login/                       # 用户认证模块
│   │   │   │       ├── controller/              # 控制器层
│   │   │   │       │   ├── LoginUserController.java
│   │   │   │       │   └── RegisterController.java
│   │   │   │       ├── domain/                  # 实体类
│   │   │   │       │   └── User.java
│   │   │   │       ├── dto/                     # 数据传输对象
│   │   │   │       │   ├── EmailCodeRequest.java
│   │   │   │       │   ├── LoginRequest.java
│   │   │   │       │   ├── RegisterRequest.java
│   │   │   │       │   ├── ServiceResult.java
│   │   │   │       │   └── UserResponse.java
│   │   │   │       ├── enums/                   # 枚举类
│   │   │   │       │   └── BusinessErrorCode.java
│   │   │   │       ├── mapper/                  # 数据访问层
│   │   │   │       │   └── UserMapper.java
│   │   │   │       └── service/                 # 服务层
│   │   │   │           ├── EmailService.java
│   │   │   │           ├── LoginUserService.java
│   │   │   │           └── impl/                # 服务实现
│   │   │   │               ├── EmailServiceImpl.java
│   │   │   │               └── LoginUserServiceImpl.java
│   │   │   ├── util/                            # 工具类
│   │   │   │   ├── AESEncryptionUtil.java       # AES加密工具
│   │   │   │   ├── JwtUtil.java                 # JWT工具类
│   │   │   │   └── LogUtil.java                 # 日志工具类
│   │   │   └── DreamHwhubApplication.java       # 主应用启动类
│   │   └── resources/                           # 资源文件
│   │       ├── application.properties           # 应用主配置
│   │       ├── logback-spring.xml               # 日志配置
│   │       ├── mail-config.properties.example   # 邮件配置示例
│   │       ├── password-key.properties.example  # 密钥配置示例
│   │       └── schema.sql                       # 数据库初始化脚本
│   └── test/                                    # 测试代码
│       └── java/top/thexiaola/dreamhwhub/
│           └── DreamHwhubApplicationTests.java  # 应用测试类
├── build.gradle                                 # Gradle构建配置
├── settings.gradle                              # Gradle设置
├── gradlew                                      # Gradle包装器(Linux)
├── gradlew.bat                                  # Gradle包装器(Windows)
├── .gitignore                                   # Git忽略配置
├── .gitattributes                               # Git属性配置
└── LICENSE                                      # 许可证文件
```

## 数据库管理

### 自动建表（推荐）
项目启动时会自动创建所需的数据库表，无需手动执行SQL脚本。

### 手动建表
如需手动初始化数据库结构：

```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 数据库表结构
- `user` 表：存储用户基本信息（学号、用户名、邮箱、加密密码、权限级别）
- 支持UTF-8字符集，确保中文内容正确存储
- 包含唯一索引约束，防止重复注册

## 日志系统

项目采用自定义日志滚动策略，支持智能化日志管理：

### 日志文件命名
```
logs/log_{日期}_{启动次数}_{文件序号}.log
```

**示例**：`logs/log_20260223_1_1.log`

### 滚动策略
- **时间滚动**：每日自动切换到新日期文件
- **大小滚动**：单文件超过50MB时自动滚动
- **启动检测**：应用重启时自动递增启动次数

### 特性
- 支持异步日志处理，提升性能
- 控制台彩色日志显示
- 完整的操作追踪记录
- 自动清理过期日志文件

## 认证与安全

### JWT认证机制
系统采用JWT (JSON Web Token) 进行用户身份认证：

- **算法**: HS512 (HMAC SHA-512)
- **默认有效期**: 12小时 (43200秒)
- **包含信息**: 用户ID、用户名
- **安全性**: 支持token过期检测和验证

### 密码安全
- 防止明文存储，确保数据安全

### 验证码机制
- 邮箱验证码有效期30分钟
- 防止暴力破解和重复注册
- 集成邮件服务，支持多种邮箱提供商

### 安全特性

- **验证码保护**: 30分钟有效期，防止滥用
- **重复注册防护**: 数据库唯一约束 + 业务层验证
- **权限控制**: 基于权限级别的访问控制

## 许可证

本项目采用 [AGPL-3.0](LICENSE) 许可证。