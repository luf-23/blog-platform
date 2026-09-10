# Blog Platform

[![Docker images](https://github.com/luf-23/blog-platform/actions/workflows/docker-publish.yml/badge.svg)](https://github.com/luf-23/blog-platform/actions/workflows/docker-publish.yml)
[![Backend image pulls](https://img.shields.io/docker/pulls/luf23/blog-platform-backend)](https://hub.docker.com/r/luf23/blog-platform-backend)
[![Frontend image pulls](https://img.shields.io/docker/pulls/luf23/blog-platform-frontend)](https://hub.docker.com/r/luf23/blog-platform-frontend)

Blog Platform 是一个前后端分离的个人博客与社区平台，支持文章创作、内容管理、评论互动、文件存储和 AI 助手。

## 功能特性

- 用户注册、登录、邮箱验证码和找回密码
- Markdown 文章编辑、草稿、审核和发布
- 文章分类、二级标签、封面和图片上传
- 社区动态、文章评论、回复和点赞
- AI 助手多模型流式对话
- 管理后台：用户、文章、公告和标签管理
- 深色 / 浅色主题和响应式布局
- Redis 缓存与文章浏览量异步刷新

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite、Element Plus、Pinia、Vue Router、Axios |
| 后端 | Java 21、Spring Boot 3.4、MyBatis、JWT |
| 数据存储 | MySQL 8.4、Redis 7.4 |
| 外部服务 | 阿里云 OSS、AI API、高德地图、SMTP |
| 部署 | Docker、Docker Compose、Nginx、GitHub Actions |

## 快速开始

### Docker 部署

项目已经将前后端镜像发布到 Docker Hub，镜像仓库是公开的。生产部署只需要使用 `deploy/` 目录，不需要在服务器上安装 JDK、Maven 或 Node.js。

详细的服务器上传、配置、启动和更新步骤见 [`deploy/README.md`](deploy/README.md)。本地使用 Docker Compose 启动时：

```bash
cd deploy
cp backend-conf/application.template backend-conf/application.properties
# 编辑 application.properties，填写数据库和第三方服务配置
docker compose pull
docker compose up -d
```

启动后访问 `http://localhost/`。如果 80 端口已被占用，可以设置其他端口：

```bash
FRONTEND_PORT=8088 docker compose up -d
```

### 本地开发

环境要求：Node.js 18+、JDK 21、Maven 3.8+、Docker Engine 和 Docker Compose v2。

先启动 MySQL 和 Redis：

```bash
docker compose up -d mysql redis
```

启动后端：

```bash
cd backend
cp src/main/resources/application.template src/main/resources/application.properties
# 编辑 application.properties
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

启动前端：

```bash
cd frontend
npm ci
npm run dev
```

前端默认运行在 `http://localhost:5173`，开发服务器会将 `/api` 请求代理到后端。如需修改代理地址，可设置 `VITE_API_PROXY_TARGET`。

## 配置

- `backend/src/main/resources/application.template`：本地开发配置模板
- `backend/src/main/resources/application.properties`：本地开发实际配置
- `deploy/backend-conf/application.template`：Docker 部署配置模板
- `deploy/backend-conf/application.properties`：Docker 部署实际配置
- `FRONTEND_PORT`：Docker Compose 发布的前端端口，默认 `80`
- `VITE_API_PROXY_TARGET`：前端开发服务器的后端代理地址

Docker 部署时，后端配置文件会以只读方式挂载到容器中。容器内的数据库和 Redis 地址分别使用 `mysql` 和 `redis`。

## 数据库

首次启动空的 MySQL 数据卷时，Compose 会依次执行：

1. `mysql-init/01_table.sql`：创建数据库和表结构
2. `mysql-init/02_data.sql`：写入开发账号和初始标签

已有数据卷不会重复执行初始化脚本。已有数据库升级评论分页结构时，按需执行 `backend/src/main/resources/SQL/03_comment_root.sql` 一次。

## 构建与测试

后端：

```bash
cd backend
mvn test
mvn -DskipTests package
```

前端：

```bash
cd frontend
npm run test:images
npm run build
npm run preview
```

## CI/CD

`.github/workflows/docker-publish.yml` 会在 `main` 或 `master` 分支提交后构建并推送镜像，也支持手动触发：

```text
luf23/blog-platform-backend:latest
luf23/blog-platform-frontend:latest
```

镜像发布工作流使用 GitHub Actions Secret `DOCKERHUB_TOKEN` 登录 Docker Hub。独立的 `.github/workflows/cd.yml` 会在前后端镜像均发布成功后自动触发，也支持手动运行；它通过 SSH 登录生产服务器并执行：

```bash
cd /home/ubuntu/blog-platform/deploy && docker compose pull && docker compose up -d
```

需要在仓库的 `Settings > Secrets and variables > Actions` 中配置：

| Secret | 内容 |
| --- | --- |
| `SSH_HOST` | Linux 服务器 IP 或域名 |
| `SSH_PORT` | SSH 端口；可不配置，默认使用 `22` |
| `SSH_USER` | SSH 用户名，当前部署目录通常对应 `ubuntu` |
| `SSH_PRIVATE_KEY` | 未设置口令的 SSH 私钥完整内容 |
| `SSH_KNOWN_HOSTS` | 服务器 SSH 主机公钥记录，即 `known_hosts` 格式内容（可多行） |

将与 `SSH_PRIVATE_KEY` 配对的公钥加入服务器登录用户的 `~/.ssh/authorized_keys`。`SSH_KNOWN_HOSTS` 应在可信环境中获取；默认端口可执行 `ssh-keyscan -H SERVER_IP`，自定义端口可执行 `ssh-keyscan -p SSH_PORT -H SERVER_IP`。服务器上的 SSH 用户还需要有权执行 Docker，并能够访问 `/home/ubuntu/blog-platform/deploy`。
