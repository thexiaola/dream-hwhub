# Dream HWHub - 梦学簿

Dream HWHub（梦学簿）是一个基于 Spring Boot 的现代化作业管理系统，采用模块化架构设计，提供安全可靠的用户认证和作业管理功能。

## 功能特性

- **用户认证系统**：完整的用户注册、登录流程，采用Session会话管理
- **邮箱验证码**：集成邮件服务，提供安全的验证码验证机制
- **权限管理**：基于权限级别的访问控制，通过拦截器实现API权限验证
- **会话管理**：智能Session管理，支持用户状态跟踪和并发登录控制
- **模块化架构**：清晰的业务模块划分，遵循分层设计理念
- **数据库集成**：使用 MyBatis-Plus ORM框架，支持自动建表
- **配置管理**：敏感信息外部化配置，支持多环境部署
- **日志系统**：自定义滚动策略，支持完整的操作追踪

## 技术栈

- **核心框架**：Spring Boot 4.0.3
- **架构模式**：模块化分层架构（Controller-Service-Mapper）
- **ORM框架**：MyBatis-Plus 3.5.15
- **数据库**：MySQL 9.5.0
- **认证机制**：Session 会话管理
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
mysql -u root -p < src/main/resources/user_schema.sql
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
  "message": "操作成功",
  "data": null
}
```

**响应字段说明：**
- `code`: 状态码（200为成功，400为参数错误，401为未授权，403为权限不足，500服务器错误）
- `message`: 响应消息
- `data`: 响应数据（无数据时为null）

### 认证机制
系统采用Session会话管理，用户登录/注册成功后会建立会话，后续请求需携带Session Cookie：

```http
Cookie: JSESSIONID=YOUR_SESSION_ID
```

或者通过浏览器自动管理Cookie进行认证。

### 核心API接口

#### 用户认证接口

##### 1. 用户登录
- **方法**: `POST /api/users/login`
- **请求体**:
  ```json
  {
    "account": "学号/用户名/邮箱",
    "password": "密码"
  }
  ```
- **成功响应**:
  ```json
  {
    "code": 200,
    "message": "登录成功！",
    "data": {
      "id": 1,
      "userNo": "2158514781",
      "username": "张三",
      "idName": null,
      "email": "zhangsan@example.com",
      "phone": null,
      "permission": 1,
      "isBanned": false,
      "registerTime": "2026-03-21T17:58:34",
      "lastLoginTime": "2026-03-21T17:58:34"
    }
  }
  ```

##### 2. 用户注册
- **方法**: `POST /api/users/register`
- **说明**: 注册成功后自动登录并建立Session会话
- **请求体**:
  ```json
  {
    "userNo": "学号/工号",
    "username": "用户名",
    "email": "邮箱",
    "password": "密码",
    "emailCode": "邮箱验证码"
  }
  ```
- **成功响应**:
  ```json
  {
    "code": 200,
    "message": "注册成功并已自动登录！",
    "data": {
      "id": 1,
      "userNo": "2158514781",
      "username": "张三",
      "idName": null,
      "email": "zhangsan@example.com",
      "phone": null,
      "permission": 1,
      "isBanned": false,
      "registerTime": "2026-03-21T17:58:34",
      "lastLoginTime": "2026-03-21T17:58:34"
    }
  }
  ```

##### 3. 发送注册验证码
- **方法**: `POST /api/users/getregcode`
- **请求体**:
  ```json
  {
    "userNo": "学号/工号",
    "username": "用户名",
    "email": "邮箱"
  }
  ```
- **成功响应**:
  ```json
  {
    "code": 200,
    "message": "验证码发送成功！",
    "cooldown": 60
  }
  ```
- **冷却时间响应** (400):
  ```json
  {
    "code": 400,
    "message": "验证码已发送，请在 57 秒后再次尝试",
    "cooldown": 57
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
    "emailCode": "123456"
  }'
```

**响应特点：** 注册成功后系统自动建立Session会话，服务器通过Set-Cookie头自动设置会话标识，浏览器会自动在后续请求中携带Cookie进行认证。

### 用户登录示例

```bash
# 登录请求（服务器会自动设置Cookie）
curl -X POST "http://localhost:8080/api/users/login" \
  -H "Content-Type: application/json" \
  -d '{
    "account": "STU2024001",
    "password": "123456"
  }' \
  -c cookies.txt -D headers.txt

# 查看服务器返回的Set-Cookie头
cat headers.txt
```

### 带认证的API调用

```bash
# 使用保存的Cookie进行认证（推荐方式）
curl -X GET "http://localhost:8080/api/protected/resource" \
  -b cookies.txt

# 或者手动传递Cookie头
curl -X GET "http://localhost:8080/api/protected/resource" \
  -H "Cookie: JSESSIONID=$(grep JSESSIONID cookies.txt | cut -d'=' -f2 | cut -d';' -f1)"
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
│   │   │   │   ├── DatabaseInitializer.java     # 数据库初始化配置
│   │   │   │   ├── LogSystem.java               # 自定义日志滚动策略
│   │   │   │   ├── MailConfig.java              # 邮件配置类
│   │   │   │   └── WebConfig.java               # Web配置类
│   │   │   ├── interceptor/                     # 拦截器
│   │   │   │   └── AuthInterceptor.java         # 权限认证拦截器
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
│   │   │   │           ├── RegisterUserService.java
│   │   │   │           └── impl/                # 服务实现
│   │   │   │               ├── EmailServiceImpl.java
│   │   │   │               ├── LoginUserServiceImpl.java
│   │   │   │               └── RegisterUserServiceImpl.java
│   │   │   ├── util/                            # 工具类
│   │   │   │   ├── AESEncryptionUtil.java       # AES加密工具
│   │   │   │   ├── LogUtil.java                 # 日志工具类
│   │   │   │   ├── SessionManager.java          # Session管理工具
│   │   │   │   └── UserUtils.java               # 用户工具类
│   │   │   └── DreamHwhubApplication.java       # 主应用启动类
│   │   └── resources/                           # 资源文件
│   │       ├── application.properties           # 应用主配置
│   │       ├── logback-spring.xml               # 日志配置
│   │       ├── mail-config.properties.example   # 邮件配置示例
│   │       ├── password-key.properties.example  # 密钥配置示例
│   │       └── user_schema.sql                       # 数据库初始化脚本
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
mysql -u root -p < src/main/resources/user_schema.sql
```

### 数据库表结构
- `user` 表：存储用户基本信息
  - 核心字段：学号、用户名、邮箱、加密密码、权限级别
  - 唯一约束：学号、用户名、邮箱均唯一
- 支持 UTF-8 字符集，确保中文内容正确存储
- 简洁高效的表结构设计

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

### Session认证机制
系统采用传统的Session会话管理进行用户身份认证：

- **会话存储**: 基于HttpSession的服务器端存储
- **状态管理**: SessionManager统一管理用户会话状态
- **权限验证**: AuthInterceptor拦截器实现访问控制
- **安全性**: 支持会话超时检测和并发登录控制

#### Session管理特性
- **会话跟踪**: 自动跟踪用户登录状态
- **并发控制**: 防止同一账号多地同时登录
- **会话清理**: 自动清理过期会话
- **内存优化**: 高效的会话存储和回收机制

#### 拦截器配置
- **保护路径**: `/api/**` 下的所有接口
- **公开接口**: 注册、登录、验证码发送、检查接口等
- **统一响应**: 未认证请求返回 401 状态码和标准错误格式

### 密码安全
- **AES-256-GCM加密**: 使用行业标准的对称加密算法
- **Base64编码**: 便于数据库存储的字符串格式
- **字段长度**: VARCHAR(255)合理分配存储空间

### 验证码机制
- 邮箱验证码有效期 10 分钟（可配置）
- 发送冷却时间 60 秒（可配置）
- 防止暴力破解和重复注册
- 集成邮件服务，支持多种邮箱提供商

### 安全特性

- **密码加密**: AES-256-GCM 算法加密 + Base64 编码存储（VARCHAR(255)）
- **会话管理**: 基于 Session 的用户状态管理，支持并发登录控制
- **权限拦截**: 自定义拦截器实现 API 访问权限验证
- **验证码保护**: 10 分钟有效期，60 秒冷却时间，防止滥用
- **重复注册防护**: 数据库唯一约束（学号、用户名、邮箱）+ 业务层验证
- **权限控制**: 基于权限级别的访问控制（默认权限级别 1）

### 会话管理API

系统提供以下Session相关的工具方法：

```java
// 获取当前登录用户
User currentUser = UserUtils.getCurrentUser();

// 检查用户是否已登录
boolean isLoggedIn = UserUtils.isLoggedIn();

// 获取当前用户ID
Integer userId = UserUtils.getCurrentUserId();

// 获取客户端IP地址
String clientIp = UserUtils.getClientIpAddress();
```

## 故障排除

### 常见问题

1. **Session丢失问题**
   - 检查浏览器Cookie设置是否启用
   - 确认服务器Session超时配置
   - 验证负载均衡环境下Session共享配置

2. **权限验证失败**
   - 确认用户已成功登录
   - 检查AuthInterceptor配置的拦截路径
   - 验证Session中用户信息完整性

3. **并发登录异常**
   - 查看SessionManager的日志记录
   - 检查用户会话状态一致性
   - 确认Session清理机制正常工作

4. **数据库连接失败**
   - 检查MySQL服务是否运行
   - 验证数据库连接配置
   - 确认数据库用户权限

5. **邮件发送失败**
   - 检查邮件配置文件是否存在
   - 验证SMTP服务器配置
   - 确认邮箱授权码正确性

6. **端口占用问题**
   ```bash
   # 查找占用8080端口的进程
   lsof -i :8080
   # 强制终止进程
   kill -9 <PID>
   ```

7. **启动失败**
   - 检查Java版本是否符合要求
   - 验证所有必需的配置文件是否存在
   - 查看详细错误日志

## 许可证

本项目采用 [AGPL-3.0](LICENSE) 许可证。