# 前端项目说明

## 项目简介

这是一个基于 Vue 3 的作业管理系统前端项目，与后端 Spring Boot 项目配套使用。

## 技术栈

- **框架**: Vue 3 (Composition API)
- **路由**: Vue Router 4
- **状态管理**: Pinia
- **UI 组件库**: Element Plus
- **HTTP 客户端**: Axios
- **图标**: Lucide Vue
- **构建工具**: Vite

## 项目结构

```
front/
├── src/
│   ├── components/     # 公共组件
│   │   ├── Sidebar.vue # 侧边栏
│   │   └── Header.vue  # 顶部导航
│   ├── views/           # 页面视图
│   │   ├── work/        # 作业相关页面
│   │   ├── class/       # 班级相关页面
│   │   └── submission/   # 提交相关页面
│   ├── router/          # 路由配置
│   ├── stores/          # 状态管理
│   ├── types/           # 类型定义
│   ├── utils/           # 工具函数
│   ├── App.vue          # 根组件
│   └── main.js          # 入口文件
├── index.html
├── package.json
└── vite.config.js
```

## 快速开始

### 1. 安装依赖

```bash
cd front
npm install
```

### 2. 配置环境变量

复制环境变量示例文件：

```bash
cp .env.example .env
```

根据实际情况修改 `.env` 文件中的配置。

### 3. 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173

### 4. 构建生产版本

```bash
npm run build
```

构建产物将输出到 `dist/` 目录。

## 功能模块

### 用户认证
- 用户注册
- 用户登录
- 找回密码
- 修改个人信息

### 作业管理
- 创建作业
- 编辑作业
- 删除作业
- 作业列表查询
- 作业置顶

### 班级管理
- 创建班级申请
- 加入班级
- 退出班级
- 班级详情查看
- 班级成员管理
- 编辑班级信息

### 作业提交
- 提交作业
- 查看提交记录
- 批改作业

## API 配置

项目使用 Vite 代理将 `/api` 请求转发到后端服务器。

在 `vite.config.js` 中配置：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

确保后端服务运行在 http://localhost:8080，或者修改代理目标地址。

## 认证机制

项目使用 JWT Token 进行用户认证：

1. 登录成功后，Token 保存在 localStorage
2. 每次请求在 Header 中携带 Token
3. CSRF Token 使用 HMAC-SHA256 算法生成

## 注意事项

1. **跨域配置**: 开发环境使用 Vite 代理解决跨域问题
2. **Token 刷新**: Token 有效期 24 小时，过期后需要重新登录
3. **文件上传**: 支持多文件上传，单个文件最大 50MB
