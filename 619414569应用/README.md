# AI Project 通用研发脚手架 v1.1

本项目是小组内研发 AI 功能的通用前后端分离底座。它提供一致的工程结构、安全基线、中间件接入方式和容器交付方式，业务模块可以在此基础上独立演进。

## 技术基线

| 层级 | 技术 | 用途 |
| --- | --- | --- |
| 后端 | Java 17、Spring Boot 3.3、Maven | REST API 与业务模块 |
| 安全 | Spring Security、JWT、BCrypt | 无状态登录与角色鉴权 |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Element Plus、Node.js 22 | 管理控制台 |
| 数据库 | PolarDB-PG / PostgreSQL、Flyway | 业务数据与版本化迁移 |
| 缓存 | Redis 7 | 缓存和临时数据 |
| 消息 | RocketMQ 5 | 异步消息 |
| 流程 | Flowable 7 | BPMN 工作流 |
| 网关 | Nginx | 前端静态资源与 API 反向代理 |
| 交付 | Docker、Docker Compose | 独立镜像与整套启动 |

本地 Compose 使用 PostgreSQL 16 作为 PolarDB-PG 的兼容开发环境。生产环境直接替换 JDBC 地址即可。

## Docker 一键部署

前置条件：Linux x86_64 服务器、Docker Engine 24+ 与 Docker Compose v2。Ubuntu、Debian、Rocky Linux、AlmaLinux 和 RHEL 等发行版均可使用；ARM64 服务器需先确认所有基础镜像都提供对应架构。

服务器资源、端口、网络和上线检查见 [`docs/服务器部署准备清单.md`](docs/服务器部署准备清单.md)。

部署目录没有硬编码，可使用 `/opt/ai-workbench`、`/srv/ai-workbench` 或普通用户有写权限的目录。目录名包含中文或空格也可以，但生产环境建议使用 ASCII 路径。部署账号必须对项目目录有写权限，以便生成权限为 `600` 的 `.env`。

Linux 服务器首次部署直接执行：

```bash
sh scripts/deploy.sh
```

脚本会检查 Docker/Compose、首次自动创建 `.env` 和随机基础设施密钥、构建镜像、启动服务，并等待所有容器健康。演示用户初始密码为 `admin123`，首次登录后必须修改。

如果 Redis 的宿主机 `6379` 端口已占用，只修改 `.env` 中的 `REDIS_HOST_PORT`；`REDIS_PORT` 是容器网络或外部 Redis 的实际服务端口，两者相互独立。

rootless Docker 无法绑定低端口或宿主机 `80` 已占用时，可在首次部署时指定其他端口，脚本会将其持久写入 `.env`：

```bash
WEB_PORT=8088 sh scripts/deploy.sh
```

已有安全 `.env` 时，也可以直接执行：

```bash
docker compose up -d --build --remove-orphans
```

首次构建需要下载镜像和 Maven/npm 依赖，完成后访问：

- Web 控制台：<http://localhost>
- 后端健康检查：<http://localhost:8080/actuator/health>
- OpenAPI 文档：<http://localhost:8080/swagger-ui.html>

`.env.example` 中的默认开发账号：

```text
用户名：admin
密码：admin123（首次登录后请立即修改）
```

脚本会拒绝数据库、Redis 和 JWT 使用模板默认值，但按当前产品要求保留演示用户初始密码 `admin123`，并由应用在首次登录后提示修改。正式开放给不受信任网络前，应完成初始密码修改；也可以提前准备 `.env` 自行指定管理员密码。

常用命令：

```bash
docker compose ps
docker compose logs -f backend frontend
docker compose down
docker compose down -v  # 同时清理本地中间件数据，请谨慎使用
```

## 独立开发

### 后端

要求 Java 17、Maven 3.9。先启动基础设施，再运行应用：

```bash
docker compose up -d postgres redis rocketmq-namesrv rocketmq-broker
cd backend
mvn spring-boot:run
```

后端默认读取以下本地地址：

- PostgreSQL：`localhost:5432/ai_scaffold`
- Redis：`localhost:6379`
- RocketMQ NameServer：`localhost:9876`

如果暂时不需要 RocketMQ，可设置 `ROCKETMQ_ENABLED=false`，后端仍可启动，消息示例接口会返回服务不可用。

### 前端

要求 Node.js 22+ 与 npm 10+：

```bash
cd frontend
npm ci
npm run dev
```

开发服务器地址为 <http://localhost:5173>，`/api` 默认代理到 `http://localhost:8080`。可通过 `VITE_PROXY_TARGET` 修改代理目标。

## 独立打包与发布

后端 JAR：

```bash
cd backend
mvn clean package
java -Dfile.encoding=UTF-8 -jar target/ai-scaffold-backend.jar
```

Windows 下从中文目录运行可执行 JAR 时必须保留 UTF-8 参数，避免 Flowable 嵌套 BPMN/XSD 资源路径解析失败。Maven 启动已通过 `backend/.mvn/jvm.config` 固化该参数。

后端镜像：

```bash
docker build -t ai-scaffold-backend:1.1.0 ./backend
docker run --rm --add-host=host.docker.internal:host-gateway -p 8080:8080 --env-file .env -v "$(pwd)/提交日志:/app/update-logs:ro" ai-scaffold-backend:1.1.0
```

单独运行后端容器时，`.env` 中的 `DB_URL`、`REDIS_HOST` 和 `ROCKETMQ_NAME_SERVER` 应指向容器可访问的地址。Linux Docker 20.10+ 通过 `--add-host=host.docker.internal:host-gateway` 提供统一的宿主机入口；若中间件位于其他服务器，应直接填写其内网地址。

前端静态包：

```bash
cd frontend
npm ci
npm run build
```

前端镜像内置 Nginx，运行时用 `BACKEND_URL` 指定后端：

```bash
docker build -t ai-scaffold-frontend:1.1.0 ./frontend
docker run --rm --add-host=host.docker.internal:host-gateway -p 80:80 -e BACKEND_URL=http://host.docker.internal:8080 ai-scaffold-frontend:1.1.0
```

因此，前端和后端可以分别构建、打版本、部署和扩容；根目录 Compose 只是本地与集成环境的便捷编排。

## 错题本与发布门禁

所有缺陷和关键报错统一记录在 [`测试/错题本`](测试/错题本/README.md)。提报 Bug 前必须先检索历史记录；修复必须关联可重复执行的回归用例。

```bash
node 测试/check-error-notebook.mjs
node 测试/check-error-notebook.mjs --release
```

日常 CI 校验记录完整性，`v*` 发布标签额外阻止任何 `open` 问题进入版本。发布负责人仍需逐条核对错题本和关联测试结果。

## PolarDB-PG 配置

PolarDB-PG 兼容 PostgreSQL 协议，后端统一使用 PostgreSQL JDBC 驱动。生产连接示例：

```text
DB_URL=jdbc:postgresql://your-polardb-endpoint:5432/ai_scaffold?sslmode=require&ApplicationName=ai-scaffold
DB_USERNAME=application_user
DB_PASSWORD=replace-with-secret
```

数据库变更只允许新增 Flyway 迁移文件，位置为 `backend/src/main/resources/db/migration`。不要修改已经在共享环境执行过的迁移脚本。

## 安全约定

- 除登录、公共接口、健康检查和 OpenAPI 外，所有接口默认需要 Bearer Token。
- JWT 使用 HS256，`JWT_SECRET` 至少 32 字符；生产建议使用 64 字节随机值并通过密钥服务注入。
- 默认用户来自环境变量，只用于底座演示。接入正式用户体系时替换 `UserDetailsService`，JWT 接口无需改变。
- API 响应携带 `traceId`，响应头同步返回 `X-Request-ID`，便于前后端和日志关联。
- CORS 白名单通过 `CORS_ALLOWED_ORIGINS` 配置，多个来源使用逗号分隔。

## 示例接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/auth/login` | 登录并获取 JWT |
| `GET` | `/api/v1/auth/me` | 当前用户信息 |
| `POST` | `/api/v1/auth/password` | 修改当前用户密码 |
| `GET` | `/api/v1/users` | 查询可选合作成员 |
| `GET/POST` | `/api/v1/workspace/applications` | 查询或创建工作台应用 |
| `PATCH/DELETE` | `/api/v1/workspace/applications/{id}` | 更新或删除应用 |
| `GET` | `/api/v1/workspace/submission-logs` | 查询提交日志 |
| `GET` | `/api/v1/workspace/update-logs` | 按文件名倒序查询版本更新日志 |
| `GET` | `/api/v1/workspace/update-logs/{author}/{fileName}` | 在线读取指定 Markdown 更新日志 |
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

## 工作台功能

- 登录、退出及首次登录修改默认密码。
- 个人应用创建、共享/独享、暂停/恢复、地址配置和删除。
- 按创建人查看其他成员已共享的个人应用。
- 合作应用创建、合作人选择、共享/独享、暂停/恢复、地址配置和删除。
- 按用户查询和筛选代码提交记录。
- 按人员查询版本更新日志，点击 Markdown 文件名可在线预览。
- 应用和权限数据存储于 PolarDB-PG/PostgreSQL，浏览器只保存 JWT。

详细测试步骤和执行结果见 [`测试/工作台功能测试.md`](测试/工作台功能测试.md)。原型和基础数据分别归档于本目录下的 `原型`、`数据` 目录。

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
├── docker/rocketmq/          # RocketMQ Broker 配置
├── 原型/                     # 页面结构与交互原型
├── 测试/                     # 测试用例、执行记录与错题本
├── 数据/                     # 初始化数据口径
├── 提交日志/                 # 按人员归档的版本更新 Markdown 与索引
├── scripts/check-update-logs.mjs # 版本递增、倒序与文件完整性校验
├── docker-compose.yml
└── .env.example
```

## 上线前检查

1. 替换所有默认密码与 `JWT_SECRET`。
2. 将数据库、Redis、RocketMQ 地址改为受管环境地址，并限制网络访问。
3. 将演示用户替换为正式账号与权限数据源。
4. 按环境设置 CORS 白名单和 HTTPS 终止层。
5. 关闭 RocketMQ 自动创建 Topic，在平台侧显式创建并配置权限。
6. 将 Flowable 的 `database-schema-update` 改为受控策略，并在发布流水线中评审数据库变更。
7. 接入日志采集、指标、告警和密钥管理。
