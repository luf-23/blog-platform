# Blog Platform

个人博客平台 — Vue 3 全新设计前端 + Spring Boot 后端。

> 本仓库的前端在 2026 年完成了一次完整的 UI/UX 重构：现代化设计语言、深/浅色主题、可折叠侧边栏、统一的布局系统与组件库。后端 API 保持不变。

## 项目结构

```text
blog-platform/
├── frontend/    # Vue 3 + Vite 前端（重新设计）
├── backend/     # Spring Boot 后端
└── README.md
```

## 技术栈

| 层 | 主要技术 |
|----|----------|
| 前端 | Vue 3、Vite 6、Element Plus、Pinia 3、Vue Router 4、@vueuse/core、md-editor-v3 |
| 后端 | Java 21、Spring Boot 3、MyBatis、MySQL、Redis、Aliyun OSS、JWT |

### 前端架构亮点

- **统一布局**：`layouts/AppLayout.vue` 与 `layouts/AuthLayout.vue` 提供主应用壳与登录壳。
- **CSS 变量主题**：`src/styles/theme.css` 定义浅色 / 深色两套语义化色板，配合 `data-theme` 切换。
- **持久化偏好**：主题、侧边栏折叠状态、登录态等通过 `pinia-plugin-persistedstate` 持久化。
- **响应式适配**：通过 `@vueuse/core` 的 `useBreakpoints` 实现桌面 / 平板 / 移动端一致体验。
- **路由懒加载 + 转场**：路由级 `import()` 懒加载，路由切换带淡入动画。
- **干净的组件分层**：`components/common`、`components/article`、`components/chat` 等按域归类。

## 环境要求

- Node.js 18+
- JDK 21
- Maven 3.8+
- MySQL 8+
- Redis（按功能需要）

## 快速开始

### 1. 克隆仓库

```bash
git clone https://github.com/luf-23/blog-platform.git
cd blog-platform
```

### 2. 数据库

执行 `backend/src/main/resources/SQL/init.sql` 初始化 MySQL（建库、表结构、演示数据；演示账号密码均为 `123456`）。

### 3. 后端

```bash
cd backend
cp src/main/resources/application.template src/main/resources/application.properties
# 编辑 application.properties 填入数据库 / Redis / OSS / 邮件配置
mvn spring-boot:run
```

默认 API：`http://localhost:8080`

### 4. 前端

```bash
cd frontend
npm install
npm run dev
```

默认开发地址：`http://localhost:5173`
后端地址在 `frontend/src/utils/request.js` 的 `baseURL` 中配置（默认 `http://localhost:8080/`）。

### 构建

```bash
cd frontend
npm run build      # 产物输出到 dist/
npm run preview    # 本地预览生产构建
```

## 配置说明

- 后端配置：`backend/src/main/resources/application.properties`（已加入 `.gitignore`，请勿提交密钥）
- 配置模板：`backend/src/main/resources/application.template`

## 主要功能

- 用户注册 / 登录 / 邮箱验证码 / 找回密码
- 分类化的文章管理（草稿、待审核、已发布）
- Markdown 编辑器，支持图片粘贴上传到 OSS
- 社区广场与文章评论 / 点赞
- AI 助手：多模型流式对话
- 管理后台：用户管理、文章审核、系统公告
- 深色 / 浅色主题切换
- 响应式设计，适配桌面与移动端
