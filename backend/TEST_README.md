# API 单元测试说明

## 测试框架和结构

本项目已创建完整的 API 单元测试框架，使用以下技术栈：
- **JUnit 5**: 测试框架
- **Mockito**: Mock 框架
- **SpringBootTest**: Spring 集成测试
- **MockMvc**: HTTP 请求模拟
- **H2 Database**: 内存数据库（测试环境）

## 已创建的测试文件

### 1. 登录模块测试
- `LoginUserControllerTest.java` - 用户登录/登出测试
  - 正常登录场景
  - 密码错误场景
  - 账号被封禁场景
  - 参数校验（空账号、空密码、密码过长）
  - 登出功能测试

- `RegisterControllerTest.java` - 用户注册测试
  - 正常注册场景
  - 学号已存在场景
  - 参数校验（空学号、邮箱格式错误、密码过短）
  - 发送注册验证码测试

- `ModifyUserControllerTest.java` - 用户信息修改测试
  - 修改用户信息
  - 修改邮箱
  - 修改密码
  - 发送换绑验证码
  - 异常场景测试

- `RetrieveUserControllerTest.java` - 密码找回测试
  - 发送找回密码验证码
  - 重置密码
  - 账号不存在场景
  - 验证码错误场景
  - 参数校验测试

### 2. 班级管理模块测试
- `ClassControllerTest.java` - 班级管理测试
  - 创建班级申请
  - 获取班级详情
  - 退出班级
  - 解散班级
  - 更新班级信息
  - 获取我的班级列表
  - 生成邀请码
  - 通过邀请码加入班级
  - 转让班级所有权
  - 参数校验测试

### 3. 作业管理模块测试
- `WorkControllerTest.java` - 作业管理测试
  - 创建作业（含文件上传）
  - 查询作业详情
  - 删除作业
  - 查询作业列表
  - 置顶/取消置顶作业
  - 更新作业
  - 作业不存在场景

- `WorkSubmissionControllerTest.java` - 作业提交测试
  - 提交作业（含文件上传）
  - 查询提交详情
  - 删除提交
  - 查询学生提交列表
  - 查询作业的提交列表（分页）
  - 查询已交/未交名单
  - 批改作业
  - 更新提交
  - 参数校验（缺少 workId、分数超出范围）

## 测试特点

### 1. 完整的测试覆盖
- ✅ 正常业务场景测试（正向用例）
- ✅ 边界值测试（空值、超长、最小值、最大值）
- ✅ 异常/错误数据测试（非法参数、业务规则不满足）
- ✅ 每个接口至少 3 条以上用例

### 2. 测试风格
- ✅ 使用 MockMvc 模拟 HTTP 请求
- ✅ Service 层全部 Mock，不依赖数据库
- ✅ 断言响应状态码、响应内容、返回数据格式
- ✅ 使用 @DisplayName 提供清晰的测试描述

### 3. 代码规范
- ✅ 方法名见名知意（testXxx_Success, testXxx_Failure 等）
- ✅ 添加清晰注释（中文注释说明测试目的）
- ✅ 符合阿里巴巴编码规范
- ✅ 统一的测试结构（准备数据 -> Mock 行为 -> 执行测试 -> 验证结果）

## 运行测试

### 运行所有测试
```bash
./gradlew test
```

### 运行特定测试类
```bash
./gradlew test --tests "*LoginUserControllerTest*"
./gradlew test --tests "*RegisterControllerTest*"
./gradlew test --tests "*ClassControllerTest*"
```

### 运行特定测试方法
```bash
./gradlew test --tests "*.LoginUserControllerTest.testLogin_Success"
```

### 生成测试报告
```bash
./gradlew test
# 报告位置：build/reports/tests/test/index.html
```

## 测试配置

### 测试环境配置
- 配置文件：`src/test/resources/application-test.properties`
- 使用 H2 内存数据库
- 禁用邮件发送
- 降低日志级别

### 依赖配置
已在 `build.gradle` 中添加：
```groovy
testImplementation 'com.h2database:h2'
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'com.fasterxml.jackson.core:jackson-databind'
```

## 测试示例

### 正常场景测试
```java
@Test
@DisplayName("测试正常登录 - 成功")
void testLogin_Success() throws Exception {
    // 准备测试数据
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setAccount("testuser");
    loginRequest.setPassword("password123");

    // Mock 行为
    Mockito.when(loginUserService.login(...)).thenReturn(mockUser);
    
    // 执行测试并验证
    mockMvc.perform(post("/api/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}
```

### 异常场景测试
```java
@Test
@DisplayName("测试登录失败 - 密码错误")
void testLogin_InvalidCredentials() throws Exception {
    // Mock 抛出业务异常
    Mockito.when(loginUserService.login(...))
            .thenThrow(new BusinessException(BusinessErrorCode.INVALID_CREDENTIALS));

    // 验证返回 401 状态码
    mockMvc.perform(post("/api/users/login")...)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
}
```

### 参数校验测试
```java
@Test
@DisplayName("测试登录 - 账号为空")
void testLogin_AccountEmpty() throws Exception {
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setAccount("");  // 空账号
    
    // 验证被参数校验拦截，返回 400
    mockMvc.perform(post("/api/users/login")...)
            .andExpect(status().isBadRequest());
}
```

## 注意事项

### Spring Boot 4.x 兼容性
由于项目使用 Spring Boot 4.0.6，测试依赖可能需要根据实际版本调整。如果遇到编译问题：

1. 确认 `spring-boot-starter-test` 包含所有必需的依赖
2. 检查 `AutoConfigureMockMvc` 和 `MockBean` 的包路径是否正确
3. 必要时显式添加缺失的依赖

### 测试隔离
- 每个测试方法独立运行，互不影响
- 使用 @MockBean 确保 Service 层完全 Mock
- 不依赖真实数据库，使用 H2 内存数据库

### 文件上传测试
对于包含文件上传的接口（如创建作业、提交作业），使用 `MockMultipartFile`：
```java
MockMultipartFile file = new MockMultipartFile(
    "attachments",
    "test.pdf",
    "application/pdf",
    "test content".getBytes()
);

mockMvc.perform(multipart("/api/works/")
        .file(file)
        .param("title", "测试作业")
        .contentType(MediaType.MULTIPART_FORM_DATA))
```

## 测试统计

- **测试文件数量**: 7 个 Controller 测试类
- **测试方法总数**: 约 60+ 个测试用例
- **覆盖率目标**: 
  - Controller 层: 100%
  - 主要业务场景: 全覆盖
  - 边界情况: 全面覆盖

## 后续优化建议

1. **集成测试**: 添加端到端集成测试，验证完整业务流程
2. **性能测试**: 添加压力测试和性能基准测试
3. **安全测试**: 添加 CSRF、XSS、SQL 注入等安全测试
4. **契约测试**: 使用 Spring Cloud Contract 进行 API 契约测试
5. **持续集成**: 在 CI/CD 流水线中自动运行测试

## 常见问题

### Q: 测试编译失败怎么办？
A: 检查 `build.gradle` 中的测试依赖是否完整，特别是：
- spring-boot-starter-test
- jackson-databind
- h2 (如果使用内存数据库)

### Q: 如何调试失败的测试？
A: 
1. 查看测试报告：`build/reports/tests/test/index.html`
2. 使用 IDE 的调试功能运行单个测试
3. 检查 Mock 配置是否正确
4. 验证请求参数和预期响应

### Q: 测试运行太慢怎么办？
A:
1. 使用 @MockBean 避免加载不必要的 Bean
2. 使用 H2 内存数据库替代 MySQL
3. 并行运行测试：`./gradlew test --parallel`
4. 只运行相关的测试类而非全部测试

---

**创建时间**: 2026-05-21  
**测试框架版本**: JUnit 5 + Mockito + Spring Boot Test  
**维护者**: AI Assistant
