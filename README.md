# Blog Platform

个人博客平台，包含 Vue 3 前端与 Spring Boot 后端。

## 项目结构

```text
blog-platform/
├── frontend/    # Vue 3 + Vite 前端
├── backend/     # Spring Boot 后端
└── README.md
```

## 技术栈

| 部分 | 技术 |
|------|------|
| 前端 | Vue 3、Vite、Element Plus、Pinia、Vue Router |
| 后端 | Java 21、Spring Boot 3、MyBatis、MySQL、Redis |

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

执行 `backend/src/main/resources/SQL/Table.sql` 初始化 MySQL（默认库名 `blog_platform`）。

### 3. 后端

```bash
cd backend
cp src/main/resources/application.template src/main/resources/application.properties
# 编辑 application.properties，填写数据库等配置
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

本地 API 地址在 `frontend/src/utils/request.js` 的 `baseURL` 中配置，一般为 `http://localhost:8080/`。

## IntelliJ IDEA

**File → Open** 选择 `backend` 目录，Maven 导入完成后运行主类 `com.blogplatform.backend.BackendApplication`。

## 配置说明

- 后端配置：`backend/src/main/resources/application.properties`（已加入 `.gitignore`，请勿提交密钥）
- 配置模板：`backend/src/main/resources/application.template`

