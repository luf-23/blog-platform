# Blog Platform 部署目录

`deploy/` 是项目的独立部署目录。将整个目录上传到 Linux 服务器后，服务器直接使用 Docker Hub 中的镜像启动前端、后端、MySQL 和 Redis，不需要克隆源码，也不需要安装 JDK、Maven 或 Node.js。

## 目录结构

```text
deploy/
├── docker-compose.yml
├── backend-conf/
│   ├── application.template       # 配置模板，可提交到 Git
│   └── application.properties     # 服务器实际配置，不提交到 Git
└── mysql-init/
    ├── 01_table.sql                # 初始化表结构
    ├── 02_export_data.sql.raw      # 完整数据备份，文章记录已拆分导入
    ├── 02_import.sh                # 以 binary mode 导入数据备份
    └── 03_data.sql.disabled        # 已禁用的开发种子数据
```

Compose 使用以下镜像：

- `luf23/blog-platform-frontend:latest`
- `luf23/blog-platform-backend:latest`
- `mysql:8.4`
- `redis:7.4-alpine`

## 服务器准备

服务器需要安装 Docker Engine 和 Docker Compose v2，并确保 80 端口可以从外部访问。MySQL、Redis 和后端端口只在 Compose 网络中使用，不需要对公网开放。

## 上传部署目录

在本地项目根目录执行（将用户名和地址替换为服务器实际值）。为避免误上传本地密钥，首次部署请上传以下部署文件；数据备份 `02_export_data.sql.raw` 不纳入版本控制，需要自行准备并上传：

```bash
ssh ubuntu@SERVER_IP "mkdir -p /home/ubuntu/blog-platform"
ssh ubuntu@SERVER_IP "mkdir -p /home/ubuntu/blog-platform/deploy/backend-conf /home/ubuntu/blog-platform/deploy/mysql-init"
scp deploy/docker-compose.yml ubuntu@SERVER_IP:/home/ubuntu/blog-platform/deploy/
scp deploy/backend-conf/application.template ubuntu@SERVER_IP:/home/ubuntu/blog-platform/deploy/backend-conf/
scp deploy/mysql-init/01_table.sql deploy/mysql-init/02_export_data.sql.raw deploy/mysql-init/02_import.sh deploy/mysql-init/03_data.sql.disabled ubuntu@SERVER_IP:/home/ubuntu/blog-platform/deploy/mysql-init/
```

如果本地不存在 `deploy/backend-conf/application.properties`，也可以直接执行 `scp -r deploy ubuntu@SERVER_IP:/home/ubuntu/blog-platform/` 上传整个目录；不要把含有真实密钥的本地配置文件上传到公共位置。

登录服务器并进入目录：

```bash
ssh ubuntu@SERVER_IP
cd /home/ubuntu/blog-platform/deploy
```

服务器最终只需要维护 `/home/ubuntu/blog-platform/deploy`，后续更新镜像时也只在此目录执行 Docker Compose 命令。

## 配置后端密钥

配置文件通过只读 bind mount 注入后端容器，因此敏感信息不会被打包进镜像。首次部署时，在服务器执行：

```bash
cp backend-conf/application.template backend-conf/application.properties
nano backend-conf/application.properties
chmod 644 backend-conf/application.properties
```

至少确认以下配置使用 Compose 服务名，不能改成 `localhost`：

```properties
spring.datasource.url=jdbc:mysql://mysql:3306/blog_platform
spring.datasource.username=blog_user
spring.datasource.password=blog_password
spring.data.redis.host=redis
spring.data.redis.port=6379
server.port=8080
management.server.port=8081
```

然后填写真实的 AI、阿里云 OSS、高德地图和邮件服务配置。后端镜像以非 root 用户运行，配置文件需要对容器用户可读；如果服务器的默认权限不是 `644`，请执行 `chmod 644 backend-conf/application.properties`。`application.properties` 已被 `.gitignore` 忽略，只保存在服务器上，不要提交到代码仓库或发送到 Docker Hub。

生产环境请在首次启动前修改 `docker-compose.yml` 中的 `MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD`，并将 `application.properties` 中的 `spring.datasource.password` 改为同一个强密码。MySQL 用户只会在空数据卷首次初始化时创建；数据卷已经存在时，修改 Compose 环境变量不会自动修改数据库用户密码。

## 首次启动

当前项目镜像仓库是公开的，无需登录 Docker Hub，直接执行：

```bash
docker compose config
docker compose pull
docker compose up -d
docker compose ps
```

如果以后将镜像仓库改为私有，再先执行 `docker login -u luf23`。

看到 `frontend`、`backend`、`mysql` 和 `redis` 状态正常后，可访问 `http://SERVER_IP/`。Compose 会等待 MySQL 和 Redis 健康后再启动后端，前端再依赖后端健康状态启动。

## 更新服务

GitHub Actions 将新镜像推送到 Docker Hub 后，会自动通过 SSH 在服务器执行：

```bash
cd /home/ubuntu/blog-platform/deploy && docker compose pull && docker compose up -d
```

SSH 公钥和 GitHub Actions Secrets 的配置方式见项目根目录 `README.md` 的“CI/CD”章节。

配置文件或配置值变更后，只需重启后端：

```bash
docker compose restart backend
```

## 查看日志和状态

```bash
docker compose ps
docker compose logs --tail=200 backend
docker compose logs --tail=200 frontend
docker compose logs -f backend
```

停止服务但保留数据：

```bash
docker compose down
```

不要使用 `docker compose down -v`，否则会删除 MySQL 和 Redis 数据卷。

## 数据卷和初始化脚本

MySQL 数据保存在 Docker 管理的 `mysql_data` 卷中，Redis 数据保存在 `redis_data` 卷中。MySQL 数据卷第一次创建且为空时，`01_table.sql` 先创建表结构，随后 `02_import.sh` 使用 `--binary-mode` 导入 `02_export_data.sql.raw` 中的完整数据备份。备份中的 9 条 `article` 记录已拆分为独立 `INSERT`，避免单条超长扩展插入解析失败；备份还包含用户、标签和分类等数据，因此不要同时启用 `03_data.sql.disabled`，否则会产生重复数据。后缀为 `.disabled` 和 `.raw` 的文件不会作为普通初始化 SQL 自动执行。

已有数据库再次执行 `up` 不会重复初始化。修改初始化文件后，如果确实需要重新初始化，必须先备份数据，再删除数据卷：

```bash
docker compose down
docker volume ls | grep blog-platform
docker volume rm blog-platform_mysql_data
docker compose up -d
```

删除数据卷会清空数据库，请确认备份和影响范围后再操作。日常发布只需拉取新镜像，不要删除数据卷。

## 常见排查

```bash
docker compose ps
docker compose logs backend
docker compose logs mysql
docker compose logs redis
docker image ls luf23/blog-platform-frontend luf23/blog-platform-backend
```

若后端无法启动，先检查 `backend-conf/application.properties` 是否存在、数据库密码是否与 Compose 中 MySQL 的初始化密码一致，以及服务器是否能够访问 Docker Hub。
