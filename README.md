# Blog-Platform

个人博客平台 — Vue 3 全新设计前端 + Spring Boot 后端。

> 本仓库的前端在 2026 年完成了一次完整的 UI/UX 重构：现代化设计语言、深/浅色主题、可折叠侧边栏、统一的布局系统与组件库。

## 项目结构

```text
blog-platform/
├── frontend/    # Vue 3 + Vite 前端（重新设计）
├── backend/     # Spring Boot 后端
├── backend-conf/ # 服务器上的 Docker 后端配置（不提交）
├── mysql-init/   # MySQL 首次启动初始化脚本
├── docker-compose.yml  # 前后端 + MySQL + Redis 完整编排
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

### Docker 启动整个项目

安装 Docker 和 Docker Compose，在项目根目录执行：

```bash
# 将服务器上的敏感配置写入该文件（不要提交到 Git）
nano backend-conf/application.properties
docker login
docker compose pull
docker compose up -d
```

前后端使用 Docker Hub 用户 `luf23` 下的 `latest` 镜像；服务器不需要项目源码或前后端 Dockerfile。Docker 镜像不包含应用配置文件。启动时 Compose 将服务器上的 `backend-conf/application.properties` 只读挂载到 `/app/config/application.properties`，Spring Boot 会读取该外部配置，因此真实密钥只保留在服务器文件中，不进入镜像。该配置使用容器服务名 `mysql` 和 `redis`。

修改服务器配置后只需重启后端：`docker compose restart backend`，无需重新构建镜像。

启动后访问 `http://localhost`，可通过环境变量 `FRONTEND_PORT` 修改网页端口。前端由 Nginx 托管并将 `/api/` 转发给后端；完整编排只向宿主机发布前端端口。MySQL 和 Redis 健康后启动后端，后端健康后启动前端。

Compose 为四个服务设置了固定容器名：`blog-platform-frontend`、`blog-platform-backend`、`blog-platform-mysql`、`blog-platform-redis`。因此同一台服务器上不能同时运行两套同名项目，也不适合通过 Compose 扩容同一服务。

Compose 只保留 MySQL 镜像初始化所需的数据库、账号、密码和时区环境变量。首次部署前修改 Compose 中的默认密码，并同步修改服务器上的 `backend-conf/application.properties` 中的数据库密码；MySQL 不读取 Spring 配置。已有数据库修改密码需要在数据库内执行，修改初始化环境变量不会更新已有账号。

```bash
docker compose logs -f          # 查看日志
docker compose down            # 停止服务，保留命名卷
```

MySQL 和 Redis 分别使用 Docker 管理的命名卷 `mysql_data`、`redis_data`，首次启动自动创建，无需准备宿主机数据目录。默认实际卷名为 `blog-platform_mysql_data`、`blog-platform_redis_data`。`docker compose down` 会保留数据，`docker compose down -v` 会删除数据卷；部署更新时不要使用 `-v`，并定期备份数据库。

`mysql-init/` 继续只读挂载为初始化目录，空数据库首次启动时依次执行 `01_table.sql` 和 `02_data.sql`。已有数据卷不会重复初始化；升级已有数据库请按下文执行迁移，不要把 `03_comment_root.sql` 放入初始化目录。初始化账号见 SQL 脚本注释，部署前应修改默认密码。

若原先的 `mysql_data/`、`redis_data/` 宿主机目录已有数据，切换前先备份并恢复到命名卷；Docker 不会自动迁移绑定目录的数据。命名卷保存在当前 Docker 主机，迁移服务器时仍需备份和恢复。

更新服务时执行 `docker compose pull && docker compose up -d`，Compose 会拉取最新的 `latest` 镜像。

### 1. 克隆仓库

```bash
git clone https://github.com/luf-23/blog-platform.git
cd blog-platform
```

### 2. 数据库

依次执行 `mysql-init/01_table.sql` 和 `mysql-init/02_data.sql`。`01_table.sql` 会删除并重建 `blog_platform`，`02_data.sql` 写入本地开发账号与初始两级标签。

初始化脚本只提供本地开发账号和平台标签体系；账号信息见脚本注释，部署前必须修改或移除。

已有数据库升级评论分页逻辑：停止后端后，仅执行一次 `backend/src/main/resources/SQL/03_comment_root.sql`，为历史回复回填 `root_comment_id` 并创建分页索引；确认脚本最后的检查没有返回记录，再启动新版后端。迁移包含已软删除的中间评论，保留原有 `parent_id` 直接回复关系。请使用遇错即停的 SQL 执行方式；脚本不是重复执行脚本，新建数据库无需执行。

评论按两层展示：一级评论按文章分页，全部层级的回复按 `root_comment_id` 分页，均以创建时间和评论 ID 倒序排列。接口继续返回 `rootId`，前端用 `parentId` 与回复对象信息显示具体回复关系，无需取得完整父链。删除中间回复不会隐藏其后续回复；删除一级评论会隐藏整组回复，并禁止在该组下继续回复。

### 3. 后端

```bash
cd backend
cp src/main/resources/application.template src/main/resources/application.properties
# 编辑 application.properties 填入数据库 / Redis / OSS / 邮件配置
mvn spring-boot:run
```

默认 API：`http://localhost:8080`

IDEA 的 Project SDK、Maven Runner JRE 请统一使用 JDK 21，并重新加载 Maven 项目以同步 Lombok 注解处理器配置。若运行时报 `NoSuchMethodError`（例如 `OssConfig.getPolicyFile()`），停止旧后端进程，执行 `mvn clean test` 后重新启动，确保加载的是本次构建的类。

### 4. 前端

```bash
cd frontend
npm install
npm run dev
```

默认开发地址：`http://localhost:5173`

浏览器统一通过同源路径 `/api/` 访问后端。开发服务器会由 Vite 将该路径代理到 `http://localhost:8080`；如需修改代理目标，可设置 `VITE_API_PROXY_TARGET`。

### 构建

```bash
cd frontend
npm run build      # 产物输出到 dist/
npm run preview    # 本地预览生产构建
```

生产环境使用 `frontend/nginx.conf`：Nginx 托管 `dist/`，并将 `/api/` 反向代理到 Compose 服务 `backend:8080`。部署到不同主机或容器时，请按实际网络拓扑调整其中的 `proxy_pass`。后端不配置 CORS，必须只通过上述同源代理对浏览器提供服务。

## 配置说明

- 后端配置：`backend/src/main/resources/application.properties`（已加入 `.gitignore`，请勿提交密钥）
- 配置模板：`backend/src/main/resources/application.template`
- Docker 服务器配置：`backend-conf/application.properties`（只读挂载，已加入 `.gitignore`）

## GitHub Actions 发布 Docker 镜像

`.github/workflows/docker-publish.yml` 会在 `main` 或 `master` 分支收到提交后，分别构建 `backend/Dockerfile` 和 `frontend/Dockerfile`，并推送到 Docker Hub。镜像名称为：

```text
luf23/blog-platform-backend
luf23/blog-platform-frontend
```

在 GitHub 仓库的 `Settings → Secrets and variables → Actions` 中添加：

- `DOCKERHUB_TOKEN`：Docker Hub Access Token（需要推送权限）

每次发布会生成 `latest`、分支名和提交 SHA 标签。工作流只构建并推送镜像，不会读取 `backend-conf/`；敏感配置仍只放在部署服务器上。根目录 Compose 已使用对应的 `image`，服务器只需准备 `backend-conf/application.properties` 和 `mysql-init/`，再执行 `docker compose pull && docker compose up -d`。如需修改前端宿主机端口，可额外在 `.env` 中设置 `FRONTEND_PORT`。

## 主要功能

- 用户注册 / 登录 / 邮箱验证码 / 找回密码
- 分类化的文章管理（草稿、待审核、已发布）
- 一级/二级平台标签体系，文章仅选择二级标签
- 分类封面、文章封面与无图回退展示
- Markdown 编辑器，支持图片粘贴上传到 OSS
- 社区广场与文章评论 / 点赞
- AI 助手：多模型流式对话
- 管理后台：用户管理、文章审核、系统公告
- 深色 / 浅色主题切换
- 响应式设计，适配桌面与移动端
