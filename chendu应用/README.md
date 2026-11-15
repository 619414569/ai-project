# AI Project 最小底座

前后端分离的最小可运行骨架：一个连通性接口 + 健康检查 + OpenAPI 文档，外加 Vue 3 控制台。

V2.0 起移除 Spring Security / JWT、Redis、PostgreSQL / Flyway / Flowable、RocketMQ，应用不保存业务数据、不依赖任何外部中间件，也不提供登录。

## 技术基线

| 层级 | 技术 | 用途 |
| --- | --- | --- |
| 后端 | Java 17、Spring Boot 3.3、Maven | REST API |
| 接口文档 | springdoc-openapi | Swagger UI |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Element Plus | 管理控制台 |
| 网关 | Nginx | 静态资源与 API 反向代理 |
| 交付 | Docker、Docker Compose | 独立镜像与整套启动 |

不再包含：Spring Security / JWT / RBAC、Redis 缓存、PostgreSQL、Flyway 迁移、Flowable 工作流、RocketMQ 消息、数据库账号体系。

## 部署拓扑

| 项 | 取值 |
| --- | --- |
| Compose 项目名 | `ai-scaffold-chendu` |
| 网络 | 不新建网络，外部引用 `619414569应用` 的 `ai-scaffold_ai-network` |
| 服务名 | `notebook-backend`、`notebook-frontend`（避开 `619414569应用` 已占用的 `backend`/`frontend` 别名） |
| 中间件依赖 | 无（不需要数据库、缓存、消息队列） |
| 前端入口 | `0.0.0.0:81`（`619414569应用` 已占用 80） |
| 后端端口 | `127.0.0.1:8081`，仅本机排查用，浏览器访问统一走前端 Nginx |

> 服务名必须带 `notebook-` 前缀：同一个用户自定义网络里若出现重名别名，Docker DNS 会返回多个 A 记录，Nginx 只会取其中一个，可能把请求打到 `619414569应用` 的后端。

端口直接写在 `docker-compose.yml` 里，不读取 `.env`；需要改端口时编辑该文件的两处 `ports`。

## 构建与启动

前置条件：同机的 `619414569应用` 已用其 compose 启动过（会创建共享网络 `ai-scaffold_ai-network`）；本应用只做外部引用，不创建网络。

两个镜像都是单阶段构建，只负责运行，编译在本地完成：

| 产物 | 本地命令 | 镜像 |
| --- | --- | --- |
| `backend/target/ai-scaffold-backend.jar` | `cd backend && mvn -B clean package` | `docker build -t notebook-backend:local ./backend` |
| `frontend/dist/` | `cd frontend && npm install && npm run build` | `docker compose build`（或 `docker build -t notebook-frontend:local ./frontend`） |

因此服务器不需要 Java、Maven、Node，只要 Docker；`scripts/deploy.sh` 会先校验 jar 与 `frontend/dist/index.html`，再打后端镜像并 `up -d --build`：

```bash
cd chendu应用/backend && mvn -B clean package && cd ..
cd chendu应用/frontend && npm install && npm run build && cd ..
cd chendu应用 && sh scripts/deploy.sh
```

也可以在本地执行 `make up`，等价于 `backend-jar → backend-image → frontend-dist → compose up -d --build`。

`docker-compose.yml` 中后端固定引用 `notebook-backend:local`，若要改用版本化标签（如 `:2.0.0`），需同时修改 compose 的 `image:` 与构建命令。

访问地址：

- Web 控制台：<http://localhost:81>
- 后端健康检查：<http://127.0.0.1:8081/actuator/health>
- OpenAPI 文档：<http://127.0.0.1:8081/swagger-ui.html>

常用命令：

```bash
docker compose ps
docker compose logs -f notebook-backend notebook-frontend
docker compose config            # 校验编排语法
docker compose down
```

## 独立开发

### 后端

要求 Java 17、Maven 3.9：

```bash
cd backend
mvn spring-boot:run
```

无需任何中间件即可启动。

### 前端

要求 Node.js 20+ 与 npm 10+：

```bash
cd frontend
npm install
npm run dev
```

开发服务器地址为 <http://localhost:5173>，`/api` 默认代理到 `http://localhost:8081`。可通过 `VITE_PROXY_TARGET` 修改代理目标。

## 独立打包与发布

后端镜像是单阶段构建，直接使用宿主机打包好的 jar：

```bash
cd backend
mvn -B clean package                                  # 1. 产出 jar
java -jar target/ai-scaffold-backend.jar              # 2. 本机直接运行

docker build -t notebook-backend:local .    # 3. 用已有 jar 构建镜像（与 compose 引用一致）
docker run --rm -p 127.0.0.1:8081:8080 notebook-backend:local
```

`backend/.dockerignore` 只把 `target/ai-scaffold-backend.jar` 放进构建上下文，`target/` 下的其它产物不会上传。

前端镜像同样是单阶段构建，直接托管本地 `dist`：

```bash
cd frontend
npm install
npm run build                                        # 产出 dist/

docker build -t notebook-frontend:local .  # 只拷贝 dist + nginx 模板
docker run --rm -p 81:80 -e BACKEND_URL=http://host.docker.internal:8081 notebook-frontend:local
```

`frontend/.dockerignore` 排除 `node_modules`、`src` 与日志，构建上下文只含 `dist` 和 `nginx/`。

## 接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/public/ping` | 公共连通性检查 |
| `GET` | `/api/v1/items` | 列出正常条目；`?deleted=true` 返回回收站内容 |
| `GET` | `/api/v1/items/{id}` | 按 ID 查询，不存在返回 `404` |
| `POST` | `/api/v1/items` | 新建条目，`title` 必填（≤100）、`content` 可选（≤2000），返回 `201` |
| `PUT` | `/api/v1/items/{id}` | 更新 `title` 与 `content`；回收站中的条目不允编辑（`409`） |
| `DELETE` | `/api/v1/items/{id}` | 移入回收站（两段式删除的第一步） |
| `POST` | `/api/v1/items/{id}/restore` | 从回收站恢复；不在回收站时返回 `409` |
| `DELETE` | `/api/v1/items/{id}/permanent` | 彻底删除；仅回收站中的条目可删（`409`） |
| `GET` | `/actuator/health` | 健康检查 |
| `GET` | `/swagger-ui.html` | OpenAPI 文档 |

## 页面

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/` | 手账 | 三栏布局（分类 / 列表 / 编辑），数据来自 `/api/v1/items`；支持新建、编辑保存、标题与正文搜索、按今天/本周筛选；每条记录左侧有勾选框，选中后顶部出现「已选 N 篇 / 删除 / 取消」多选栏；删除为两段式：先移入回收站，可恢复或彻底删除 |
| `/status` | 运行概览 | 后端连通性检测与基础能力清单 |

条目的存储实现是 `demo/item/ItemService` 里的 `ConcurrentHashMap`，不落库，进程重启即清空。示例：

```bash
curl -X POST http://localhost:8081/api/v1/items -H 'Content-Type: application/json' -d '{"title":"first","content":"hello"}'
curl http://localhost:8081/api/v1/items
```

> **V2.0 起所有接口均无需鉴权**，任何能访问 `81` 端口的调用方都可直接调用。部署时必须通过安全组、反向代理或内网限制访问范围，不要直接暴露到公网。

## 后端约定

- `ApiResponse<T>`：统一返回 `code`、`message`、`data`、`traceId`、`timestamp`。
- `GlobalExceptionHandler`：统一处理业务异常、参数校验和未知异常。
- `BusinessException` / `ErrorCode`：业务错误的稳定编码。
- `RequestIdFilter`：接收或生成请求追踪 ID，响应头返回 `X-Request-ID`。
- `PageResult<T>`、`AssertUtils`、`DateTimeUtils`、`IdUtils`、`JsonUtils`、`MaskUtils`：无业务依赖的常用工具。
- 无跨域配置：前端通过 Nginx 反代 `/api`，与后端同源；若需要跨域直连，需自行引入 CORS 配置。

## 目录结构

```text
.
├── backend/                  # Spring Boot，可独立打 JAR/镜像
│   ├── src/main/java/        # API 与公共能力
│   ├── src/main/resources/   # 仅 application.yml
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                 # Vue 3，可独立打静态包/镜像
│   ├── src/
│   ├── nginx/
│   ├── Dockerfile
│   └── package.json
├── scripts/deploy.sh         # 部署前置校验与启动脚本
├── 测试/                      # 测试记录与错题本
├── 数据/                      # 数据说明（V2.0 起不再持有数据）
├── 提交日志/                  # 按提交人归档的版本日志
├── docker-compose.yml
├── Makefile
├── .editorconfig
├── .gitignore
└── README.md
```

## 交付文档索引

- [测试资料](./测试/README.md)：测试记录、验收状态与错题本
- [数据说明](./数据/README.md)：V2.0 起无持久化数据的说明
- [提交日志](./提交日志/2026-11-15_V2.0.md)：最新版本的变更记录

## 上线前检查

1. 通过安全组或上层网关限制 `81` 端口的访问范围，禁止无鉴权接口直接暴露公网。
2. 保持前后端同源部署（经 Nginx 反代）；如需跨域直连，先补 CORS 配置并限定来源。
3. 接入日志采集、指标与告警（`/actuator/health` 可直接作为探针）。
4. 如后续重新引入鉴权、持久化或消息队列，必须先补充对应测试与数据库迁移评审。
