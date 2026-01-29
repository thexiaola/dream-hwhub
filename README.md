# Dream HWHub - 梦学簿

Dream HWHub（梦学簿） 是一个作业管理系统，旨在提供高效的学生作业提交和管理功能。

## 功能特性

- **用户管理**：支持用户注册、登录和权限管理
- **验证码系统**：集成邮箱验证码验证机制
- **数据库集成**：使用 MyBatis-Plus 进行数据库操作
- **安全认证**：集成 Spring Security 提供安全保护
- **邮件服务**：集成邮件发送功能，用于验证码和通知

## 技术栈

- **后端框架**：Spring Boot 4.0.2
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

### 3. 启动应用

```bash
./gradlew bootRun
```

应用将在 `http://localhost:8080` 上运行。

## API 接口

### 用户相关接口

#### 1. 用户登录

- **POST** `/api/users/login`
- 请求体：
  ```json
  {
    "userNo": "学号/工号",
    "email": "邮箱",
    "password": "密码"
  }
  ```

#### 2. 用户注册

- **POST** `/api/users/register`
- 请求体：
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

#### 3. 发送注册验证码

- **POST** `/api/users/getregcode`
- 请求体：
  ```json
  {
    "userNo": "学号/工号",
    "email": "邮箱"
  }
  ```
- 响应：
  ```json
  {
    "code": 200,
    "message": "验证码已发送",
    "data": null
  }
  ```

## 邮件配置

项目集成了邮件服务，用于发送验证码。需要在 `application.properties` 中配置：

```properties
# 邮件配置
spring.mail.host=smtp.qq.com
spring.mail.port=465
spring.mail.username=your-email@qq.com
spring.mail.password=your-app-password
spring.mail.properties.mail.from.nickname=发信人昵称
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.ssl.enable=true
```

## 项目结构

```
src/
├── main/
│   ├── java/top/thexiaola/dreamhwhub/
│   │   ├── common/      # 通用组件
│   │   ├── config/      # 配置类
│   │   ├── controller/  # 控制器层
│   │   ├── domain/      # 实体类
│   │   ├── dto/         # 数据传输对象
│   │   ├── mapper/      # 数据访问层
│   │   ├── service/     # 业务逻辑层
│   │   └── DreamHwhubApplication.java  # 主应用类
│   └── resources/
│       ├── application.properties  # 应用配置
│       ├── logback-spring.xml     # 日志配置
│       └── schema.sql             # 数据库初始化脚本
```

## 数据库初始化

项目使用 `schema.sql` 文件进行数据库初始化，包含用户表等基础表结构。首次运行时，系统会自动创建所需表结构。

## 安全说明

- 密码采用 MD5 加密存储
- 验证码具有时效性（默认30分钟过期）
- 实现了重复注册防护机制

## 许可证

本项目采用 AGPL-3.0 许可证。