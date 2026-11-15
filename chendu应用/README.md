# AI Project 通用研发脚手架

本项目是小组内研发 AI 功能的通用前后端分离底座。它提供一致的工程结构、安全基线、中间件接入方式和容器交付方式，业务模块可以在此基础上独立演进。

## 技术基线

| 层级 | 技术 | 用途 |
| --- | --- | --- |
| 后端 | Java 17、Spring Boot 3.3、Maven | REST API 与业务模块 |
| 安全 | Spring Security、JWT、BCrypt | 无状态登录与角色鉴权 |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Element Plus | 管理控制台 |
| 数据库 | PolarDB-PG / PostgreSQL、Flyway | 业务数据与版本化迁移 |
| 缓存 | Redis 7 | 缓存和临时数据 |
| 消息 | RocketMQ 5 | 异步消息 |
| 流程 | Flowable 7 | BPMN 工作流 |
| 网关 | Nginx | 前端静态资源与 API 反向代理 |
| 交付 | Docker、Docker Compose | 独立镜像与整套启动 |

本地 Compose 不再启动 PostgreSQL / Redis / RocketMQ 容器，统一复用同机 `619414569应用` 的 compose 已启动的中间件；本应用只编排前端与后端服务。生产环境替换 JDBC 地址与共享实例地址即可。

## 同机部署拓扑

本应用与 `619414569应用` 部署在同一台服务器，不启动 PostgreSQL、Redis、RocketMQ 容器：

| 项 | 取值 |
| --- | --- |
| Compose 项目名 | `ai-scaffold-chendu`（与 `619414569应用` 的 `ai-scaffold` 隔离） |
| 共享网络 | `ai-scaffold_ai-network`，由 `619414569应用` 的 compose 创建后外部引用 |
| PostgreSQL | 共享实例内独立库 `ai_scaffold_chendu`、独立角色 `ai_scaffold_chendu` |
| Redis | `redis:6379`，口令必须与 `619414569应用` 的 `REDIS_PASSWORD` 一致 |
| RocketMQ | `rocketmq-namesrv:9876`，生产者/消费者组独立，避免争抢队列 |
| 前端入口 | `0.0.0.0:81`（`619414569应用` 已占用 80） |
| 后端端口 | `127.0.0.1:8081`，仅本机排查用，浏览器访问统一走前端 Nginx |

## 启动步骤

前置条件：Docker Engine 24+、Docker Compose v2，且同机 `619414569应用` 已按自身文档完成部署。

以下步骤在仓库根目录执行。

1. 确认共享中间件已运行：

```bash
cd 619414569应用
docker compose up -d postgres redis rocketmq-namesrv rocketmq-broker
docker network ls | grep ai-scaffold
```

2. 首次部署时在共享实例中创建独立库（属于写库操作，执行前需按规则第 6 节取得确认）：

```bash
cd 619414569应用
docker compose exec -T postgres psql -U ai_scaffold -d postgres \
  -v db_password='<库口令>' -f - < ../chendu应用/数据/初始化/01_create_database_shared_instance.sql
```

3. 配置并启动本应用：

```bash
cd chendu应用
cp .env.example .env
# 修改 DB_PASSWORD（与上一步一致）、REDIS_PASSWORD（与 619414569应用 一致）、JWT_SECRET
sh scripts/deploy.sh
```

`scripts/deploy.sh` 会校验共享网络是否存在、`.env` 是否仍含占位口令，然后构建并启动容器。首次启动由 Flyway 与 Flowable 建表，见 [数据说明](./数据/README.md)。

首次构建需要下载镜像和 Maven/npm 依赖，完成后访问：

- Web 控制台：<http://localhost:81>
- 后端健康检查：<http://127.0.0.1:8081/actuator/health>
- OpenAPI 文档：<http://127.0.0.1:8081/swagger-ui.html>

默认开发账号：

```text
用户名：admin
密码：Admin@123456
```

默认值仅用于本地启动。共享或生产环境必须先替换 `.env` 中的数据库、Redis、JWT 和管理员密码。

常用命令：

```bash
docker compose ps
docker compose logs -f backend frontend
docker compose config            # 校验变量与语法
docker compose down              # 只停本应用，不影响 619414569应用 的中间件
```

> 本应用已不再声明独立数据卷，不要用 `docker compose down -v` 清理；中间件数据卷属于 `619414569应用`。

## 独立开发

### 后端

要求 Java 17、Maven 3.9。先启动同机共享中间件，再运行应用：

```bash
cd 619414569应用
docker compose up -d postgres redis rocketmq-namesrv rocketmq-broker
cd ../chendu应用/backend
mvn spring-boot:run
```

后端在宿主机直连时读取以下地址（中间件端口由 `619414569应用` 暴露到回环地址）：

- PostgreSQL：`localhost:5432/ai_scaffold_chendu`
- Redis：`localhost:6379`，口令与 `619414569应用` 一致
- RocketMQ NameServer：`localhost:9876`

如果暂时不需要 RocketMQ，可设置 `ROCKETMQ_ENABLED=false`，后端仍可启动，消息示例接口会返回服务不可用。

### 前端

要求 Node.js 20+ 与 npm 10+：

```bash
cd frontend
npm install
npm run dev
```

开发服务器地址为 <http://localhost:5173>，`/api` 默认代理到 `http://localhost:8081`。可通过 `VITE_PROXY_TARGET` 修改代理目标。

## 独立打包与发布

后端 JAR：

```bash
cd backend
mvn clean package
java -jar target/ai-scaffold-backend.jar
```

后端镜像：

```bash
docker build -t ai-scaffold-chendu-backend:1.1.0 ./backend
docker run --rm -p 127.0.0.1:8081:8080 --env-file .env ai-scaffold-chendu-backend:1.1.0
```

单独运行后端容器时，`.env` 中的 `DB_URL`、`REDIS_HOST` 和 `ROCKETMQ_NAME_SERVER` 应指向容器可访问的地址（Compose 内的 `postgres`、`redis`、`rocketmq-namesrv` 服务名只在共享网络中有效）；示例使用 Docker Desktop 提供的 `host.docker.internal`。

前端静态包：

```bash
cd frontend
npm install
npm run build
```

前端镜像内置 Nginx，运行时用 `BACKEND_URL` 指定后端：

```bash
docker build -t ai-scaffold-chendu-frontend:1.0.0 ./frontend
docker run --rm -p 81:80 -e BACKEND_URL=http://host.docker.internal:8081 ai-scaffold-chendu-frontend:1.0.0
```

因此，前端和后端可以分别构建、打版本、部署和扩容；根目录 Compose 只是本地与集成环境的便捷编排。

## PolarDB-PG 配置

PolarDB-PG 兼容 PostgreSQL 协议，后端统一使用 PostgreSQL JDBC 驱动。生产连接示例：

```text
DB_URL=jdbc:postgresql://your-polardb-endpoint:5432/ai_scaffold_chendu?sslmode=require&ApplicationName=ai-scaffold-chendu
DB_USERNAME=ai_scaffold_chendu
DB_PASSWORD=replace-with-secret
```

数据库变更只允许新增 Flyway 迁移文件，位置为 `backend/src/main/resources/db/migration`。不要修改已经在共享环境执行过的迁移脚本。

## 安全约定

- 除登录、公共接口、健康检查和 OpenAPI 外，所有接口默认需要 Bearer Token。
- JWT 使用 HS256，`JWT_SECRET` 至少 32 字符；生产建议使用 64 字节随机值并通过密钥服务注入。
- 默认用户来自环境变量，只用于底座演示。接入正式用户体系时替换 `UserDetailsService`，JWT 接口无需改变。
- API 响应携带 `traceId`，响应头同步返回 `X-Request-ID`，便于前后端和日志关联。
- CORS 白名单通过 `CORS_ALLOWED_ORIGINS` 配置，多个来源使用逗号分隔；`CORS_ALLOWED_ORIGIN_PATTERNS` 支持通配符，用于 Vite 开发服务器绑定 `0.0.0.0` 后通过 `127.0.0.1` 或局域网地址访问的场景。
- 默认值仅覆盖回环地址与 RFC1918 私有网段，公网部署时必须按环境显式覆盖 `CORS_ALLOWED_ORIGINS` 与 `CORS_ALLOWED_ORIGIN_PATTERNS`。

## 示例接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/auth/login` | 登录并获取 JWT |
| `GET` | `/api/v1/auth/me` | 当前用户信息 |
| `GET` | `/api/v1/public/ping` | 公共连通性检查 |
| `PUT/GET/DELETE` | `/api/v1/demo/cache/{key}` | Redis 缓存示例 |
| `POST` | `/api/v1/demo/messages` | RocketMQ 发布示例 |
| `POST` | `/api/v1/demo/workflows/approval` | 启动 Flowable 审批流程 |
| `GET` | `/api/v1/demo/workflows/tasks` | 查询审批人待办 |
| `POST` | `/api/v1/demo/workflows/tasks/{id}/complete` | 完成审批任务 |

上述受保护接口可直接在前端“组件联调”页面操作。

## 通用后端约定

- `ApiResponse<T>`：统一返回 `code`、`message`、`data`、`traceId`、`timestamp`。
- `GlobalExceptionHandler`：统一处理业务异常、参数校验、鉴权异常和未知异常。
- `BusinessException` / `ErrorCode`：业务错误的稳定编码。
- `RequestIdFilter`：接收或生成请求追踪 ID。
- `PageResult<T>`：分页返回模型。
- `AssertUtils`、`DateTimeUtils`、`IdUtils`、`JsonUtils`、`MaskUtils`：无业务依赖的常用工具。

新增业务建议按 `领域/接口/应用服务/基础设施` 分包，不要把业务逻辑继续堆入 `demo` 包；`demo` 仅用于证明各组件已接通。

## 目录结构

```text
.
├── backend/                  # Spring Boot，可独立打 JAR/镜像
│   ├── src/main/java/        # API、安全、公共能力和中间件示例
│   ├── src/main/resources/   # 配置、Flyway、BPMN
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                 # Vue 3，可独立打静态包/镜像
│   ├── src/
│   ├── nginx/
│   ├── Dockerfile
│   └── package.json
├── docker/rocketmq/          # 本地独立 Broker 配置（同机复用中间件时不再挂载）
├── scripts/deploy.sh         # 同机部署前置校验与启动脚本
├── 测试/                      # 测试记录与错题本
│   ├── 测试记录/
│   └── 错题本/
├── 数据/                      # 数据对象与初始化数据说明
├── 提交日志/                  # 按提交人归档的版本日志
├── docker-compose.yml
├── Makefile
├── .editorconfig
├── .gitignore
├── .env.example
└── README.md
```

## 交付文档索引

- [测试资料](./测试/README.md)：测试记录、验收状态与错题本
- [数据说明](./数据/README.md)：数据对象、初始化数据与迁移约定
- [提交日志](./提交日志/2026-11-15_V1.2.md)：最新版本的变更记录

## 上线前检查

1. 替换所有默认密码与 `JWT_SECRET`。
2. 复用同机中间件时，只通过 `ai-scaffold_ai-network` 内网访问 PostgreSQL、Redis、RocketMQ，不额外暴露中间件端口；`DB_URL` 指向独立库 `ai_scaffold_chendu`，`REDIS_PASSWORD` 与共享实例一致。
3. 将演示用户替换为正式账号与权限数据源。
4. 按环境设置 CORS 白名单和 HTTPS 终止层。
5. 关闭 RocketMQ 自动创建 Topic，在平台侧显式创建并配置权限。
6. 将 Flowable 的 `database-schema-update` 改为受控策略，并在发布流水线中评审数据库变更。
7. 接入日志采集、指标、告警和密钥管理。
