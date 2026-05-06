# Dream HWHub - 梦学簿

Dream HWHub（梦学簿）是一个基于 Spring Boot 的现代化作业管理系统，采用**模块化分层架构**设计，提供安全可靠的用户认证和作业管理功能。

## 🏗️ 项目架构

本项目采用清晰的模块化设计，主要包含以下模块：

### 核心模块
- **common**: 公共模块
  - `api`: API统一响应封装
  - `context`: 用户上下文管理
  
- **support**: 支撑模块（技术工具）
  - `encryption`: AES加密工具
  - `validation`: 文件上传验证
  - `logging`: 日志工具
  - `session`: 会话管理和用户工具
  
- **config**: 配置层
  - `security`: 安全配置（拦截器）
  - `infrastructure`: 基础设施配置（数据库、邮件、日志）
  - `exception`: 全局异常处理

### 业务模块
- **module/login**: 用户认证模块
  - 用户注册、登录、密码找回
  - 邮箱验证码服务
  - 用户信息管理
  
- **module/work_management**: 课堂管理模块
  - 班级管理（创建、加入、邀请码、所有权转让）
  - 作业管理（发布、更新、删除、附件）
  - 作业提交（提交、批改、统计、分页查询）

## 功能特性

- **用户认证系统**：完整的用户注册、登录流程，采用Session会话管理
- **邮箱验证码**：集成邮件服务，提供安全的验证码验证机制
- **权限管理**：基于权限级别的访问控制，通过拦截器实现API权限验证
- **会话管理**：智能Session管理，支持用户状态跟踪和并发登录控制
- **模块化架构**：清晰的业务模块划分，遵循分层设计理念
- **数据库集成**：使用 MyBatis-Plus ORM框架，支持自动建表
- **配置管理**：敏感信息外部化配置，支持多环境部署
- **日志系统**：自定义滚动策略，支持完整的操作追踪
- **班级管理功能**：
  - 创建者（OWNER）：班级创建者，拥有最高权限
  - 班级助理（ASSISTANT）：由创建者设置，协助管理班级
  - 学生（STUDENT）：普通班级成员
  - 支持邀请码加入、所有权转让、成员管理等功能
  - 学生可邀请其他用户加入班级（需老师审核），邀请记录存储被邀请人ID和姓名
  - **25位邀请码**：采用大小写字母+数字组合，安全性提升10^35倍
- **分页查询**：所有列表接口支持MyBatisPlus分页，提升大数据量性能
- **完整API文档**：详细的接口说明请参考 [ApiList.md](ApiList.md)
- **优化的响应结构**：提交作业和更新作业的响应不包含批改字段，查询时才返回完整信息
- **灵活的批改机制**：教师可以重新批改、打回学生修改、或恢复被打回的作业

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

## 详细 API 文档

完整的 API 接口文档请参考 [ApiList.md](ApiList.md)。

## 项目结构

```
.
├── src/
│   ├── main/
│   │   ├── java/top/thexiaola/dreamhwhub/
│   │   │   ├── common/                          # 公共模块
│   │   │   │   ├── api/                         # API统一响应
│   │   │   │   │   └── ApiResponse.java
│   │   │   │   └── context/                     # 上下文管理
│   │   │   │       └── UserContext.java
│   │   │   ├── config/                          # 配置层
│   │   │   │   ├── security/                    # 安全配置
│   │   │   │   │   └── AuthInterceptor.java
│   │   │   │   ├── DatabaseInitializer.java     # 数据库初始化
│   │   │   │   ├── GlobalExceptionHandler.java  # 全局异常处理
│   │   │   │   ├── LogSystem.java               # 日志系统配置
│   │   │   │   ├── MailConfig.java              # 邮件服务配置
│   │   │   │   └── WebConfig.java               # Web配置
│   │   │   ├── support/                         # 支撑模块（技术工具）
│   │   │   │   ├── encryption/                  # 加密工具
│   │   │   │   │   └── AESEncryptionUtil.java
│   │   │   │   ├── validation/                  # 验证工具
│   │   │   │   │   └── FileUploadValidator.java
│   │   │   │   ├── logging/                     # 日志工具
│   │   │   │   │   └── LogUtil.java
│   │   │   │   └── session/                     # 会话管理
│   │   │   │       ├── SessionManager.java
│   │   │   │       └── UserUtils.java
│   │   │   ├── module/                          # 业务模块
│   │   │   │   ├── login/                       # 用户认证模块
│   │   │   │   │   ├── controller/              # 控制器层
│   │   │   │   │   │   ├── LoginUserController.java
│   │   │   │   │   │   ├── ModifyUserController.java
│   │   │   │   │   │   ├── RegisterController.java
│   │   │   │   │   │   └── RetrieveUserController.java
│   │   │   │   │   ├── dto/                     # 数据传输对象
│   │   │   │   │   │   ├── EmailCodeRequest.java
│   │   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   │   ├── ModifyEmailRequest.java
│   │   │   │   │   │   ├── ModifyPasswordRequest.java
│   │   │   │   │   │   ├── ModifyUserInfoRequest.java
│   │   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   │   ├── RetrievePasswordCodeRequest.java
│   │   │   │   │   │   ├── RetrievePasswordModifyRequest.java
│   │   │   │   │   │   ├── SendModifyCodeRequest.java
│   │   │   │   │   │   └── UserResponse.java
│   │   │   │   │   ├── entity/                  # 实体类
│   │   │   │   │   │   └── User.java
│   │   │   │   │   ├── mapper/                  # 数据访问层
│   │   │   │   │   │   └── UserMapper.java
│   │   │   │   │   └── service/                 # 服务层
│   │   │   │   │       ├── EmailService.java
│   │   │   │   │       ├── LoginUserService.java
│   │   │   │   │       ├── ModifyUserService.java
│   │   │   │   │       ├── RegisterUserService.java
│   │   │   │   │       └── impl/                # 服务实现
│   │   │   │   │           ├── EmailServiceImpl.java
│   │   │   │   │           ├── LoginUserServiceImpl.java
│   │   │   │   │           ├── ModifyUserServiceImpl.java
│   │   │   │   │           └── RegisterUserServiceImpl.java
│   │   │   │   └── work_management/             # 课堂管理模块
│   │   │   │       ├── controller/              # 控制器层
│   │   │   │       │   ├── ClassController.java
│   │   │   │       │   ├── WorkController.java
│   │   │   │       │   └── WorkSubmissionController.java
│   │   │   │       ├── dto/                     # 请求DTO
│   │   │   │       │   ├── ApproveJoinClassRequest.java
│   │   │   │       │   ├── CreateClassRequest.java
│   │   │   │       │   ├── CreateWorkRequest.java
│   │   │   │       │   ├── GradeWorkRequest.java
│   │   │   │       │   ├── JoinClassRequest.java
│   │   │   │       │   ├── RespondInvitationRequest.java
│   │   │   │       │   ├── SubmitWorkRequest.java
│   │   │   │       │   └── UpdateWorkRequest.java
│   │   │   │       ├── entity/                  # 实体类
│   │   │   │       │   ├── ClassCreateApplication.java
│   │   │   │       │   ├── ClassInfo.java
│   │   │   │       │   ├── ClassInvitation.java
│   │   │   │       │   ├── ClassInviteApplication.java
│   │   │   │       │   ├── ClassJoinApplication.java
│   │   │   │       │   ├── ClassMember.java
│   │   │   │       │   ├── WorkAttachment.java
│   │   │   │       │   ├── WorkInfo.java
│   │   │   │       │   ├── WorkSubmission.java
│   │   │   │       │   └── WorkSubmissionAttachment.java
│   │   │   │       ├── mapper/                  # 数据访问层
│   │   │   │       │   ├── ClassCreateApplicationMapper.java
│   │   │   │       │   ├── ClassInfoMapper.java
│   │   │   │       │   ├── ClassInvitationMapper.java
│   │   │   │       │   ├── ClassInviteApplicationMapper.java
│   │   │   │       │   ├── ClassJoinApplicationMapper.java
│   │   │   │       │   ├── ClassMemberMapper.java
│   │   │   │       │   ├── WorkAttachmentMapper.java
│   │   │   │       │   ├── WorkMapper.java
│   │   │   │       │   ├── WorkSubmissionAttachmentMapper.java
│   │   │   │       │   └── WorkSubmissionMapper.java
│   │   │   │       ├── service/                 # 服务层
│   │   │   │       │   ├── ClassService.java
│   │   │   │       │   ├── WorkService.java
│   │   │   │       │   ├── WorkSubmissionService.java
│   │   │   │       │   └── impl/                # 服务实现
│   │   │   │       │       ├── ClassServiceImpl.java
│   │   │   │       │       ├── WorkServiceImpl.java
│   │   │   │       │       └── WorkSubmissionServiceImpl.java
│   │   │   │       └── vo/                      # 响应VO
│   │   │   │           ├── ClassDetailResponse.java
│   │   │   │           ├── ClassMemberResponse.java
│   │   │   │           ├── CreateClassApplicationResponse.java
│   │   │   │           ├── InvitationResponse.java
│   │   │   │           ├── JoinClassApplicationResponse.java
│   │   │   │           ├── MemberCheckResponse.java
│   │   │   │           ├── WorkResponse.java
│   │   │   │           └── WorkSubmissionResponse.java
│   │   │   ├── enums/                           # 枚举类
│   │   │   │   └── BusinessErrorCode.java
│   │   │   ├── exception/                       # 异常类
│   │   │   │   └── BusinessException.java
│   │   │   └── DreamHwhubApplication.java       # 主应用启动类
│   │   └── resources/                           # 资源文件
│   │       ├── application.properties           # 应用主配置
│   │       ├── logback-spring.xml               # 日志配置
│   │       ├── mail-config.properties.example   # 邮件配置示例
│   │       ├── password-key.properties.example  # 密钥配置示例
│   │       ├── user_schema.sql                  # 数据库初始化脚本
│   │       └── work_management.sql              # 课堂管理表结构
│   └── test/                                    # 测试代码
│       └── java/top/thexiaola/dreamhwhub/
│           └── DreamHwhubApplicationTests.java  # 应用测试类
├── build.gradle                                 # Gradle构建配置
├── settings.gradle                              # Gradle设置
├── gradlew                                      # Gradle包装器(Linux)
├── gradlew.bat                                  # Gradle包装器(Windows)
├── .gitignore                                   # Git忽略配置
├── LICENSE                                      # 许可证文件
├── README.md                                    # 项目说明文档
└── ApiList.md                                   # API接口文档
```

## 邮件服务配置

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

系统提供以下Session相关的工具方法（位于 `support.session` 包）：

```java
import top.thexiaola.dreamhwhub.support.session.UserUtils;

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