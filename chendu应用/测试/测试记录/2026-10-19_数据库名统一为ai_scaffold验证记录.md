# 2026-10-19 数据库名统一为 ai_scaffold 验证记录

| 项 | 值 |
| --- | --- |
| 日期 | 2026-10-19 |
| 执行人 | `chendu` |
| Git 分支 | `user/chendu` |
| 应用目录 | `chendu应用/` |
| 目标环境 | 本机开发环境 PostgreSQL 14.24（Windows 服务 `postgresql-x64-14`） |
| 变更类型 | 本地建库建表 + 连接配置（无 Java 源码改动） |
| 关联版本 | `V1.1`（尚未提交，位于工作区） |

## 背景

`619414569应用` 的连接配置为库 `ai_scaffold`、角色 `ai_scaffold`。本机实例原先只有
`brilliant-idea`（已建表），没有 `ai_scaffold`。本次在本机创建角色与库 `ai_scaffold`，
并让 `chendu应用` 连接过去，使两边数据库一致。

## 变更范围

| 文件 | 改动后 |
| --- | --- |
| `数据/初始化/00_create_database.sql` | 改写为幂等创建角色 `ai_scaffold` 与库 `ai_scaffold`（属主 `ai_scaffold`），只建角色与库，不含建表 DDL |
| `backend/src/main/resources/application.yml` | `DB_URL` 默认 `jdbc:postgresql://localhost:5432/ai_scaffold`，`DB_USERNAME` 默认 `ai_scaffold`，`DB_PASSWORD` 默认 `change-me-postgres` |
| `docker-compose.yml` | `POSTGRES_DB` 与 backend `DB_URL` 的库名默认值回退为 `ai_scaffold` |
| `.env.example` | `POSTGRES_DB`、`DB_URL` 回退为 `ai_scaffold` |
| `README.md` | 本地地址与 PolarDB-PG 连接示例回退为 `ai_scaffold` |

上一轮曾把库名改为 `brilliant-idea`，本轮按与 `619414569应用` 一致的要求回退为 `ai_scaffold`，
对应的配置、脚本与本文档均已同步修正。

## 执行记录

| # | 检查项 | 方式 | 期望 | 实测 |
| --- | --- | --- | --- | --- |
| 1 | 建角色与建库 | `psql -U postgres -d postgres -f 数据/初始化/00_create_database.sql` | 退出码 0，幂等 | 输出 `CREATE ROLE`、`CREATE DATABASE`，退出码 0 |
| 2 | 角色 | 只读查询 `pg_roles` | `ai_scaffold` 可登录 | `role ai_scaffold canlogin=true` |
| 3 | 库 | 只读查询 `pg_database` | 库存在且属主为 `ai_scaffold` | `db ai_scaffold owner=ai_scaffold` |
| 4 | 业务表 | 只读查询 `pg_tables` | `app_setting`、`workbench_user` | 两者均在（另有 `flyway_schema_history`） |
| 5 | Flowable 表 | 只读查询 `pg_tables` | `act_*`、`flw_*` 已建 | `act=30`、`flw=8`，共 38 |
| 6 | 迁移状态 | 只读查询 `flyway_schema_history` | `v1`、`v2` 成功 | `v1 success=true`、`v2 success=true` |
| 7 | 账号落库 | 只读查询 `workbench_user` | `admin`、`dcy` 在库且为 BCrypt 散列 | `admin\|ADMIN,USER`、`dcy\|USER`，散列前缀 `$2a$`，长度 60 |
| 8 | `dcy` 登录 | `POST /api/v1/auth/login`，Origin `http://localhost:5173` | `200 code=OK` | `200 code=OK`，CORS 回显 `http://localhost:5173` |
| 9 | `admin` 登录 | 同上 | `200 code=OK` | `200 code=OK` |
| 10 | 错误口令 | `dcy` + 错误口令 | `401` | `401 code=UNAUTHORIZED` |
| 11 | 自动化测试 | `mvn -B test` | 21/21 通过 | `Tests run: 21, Failures: 0, Errors: 0`，`BUILD SUCCESS` |

口令明文不写入文档（规则 8.7），实际取值见 `application.yml` 与 `.env.example`。

## 过程中的失败与处理

| 项 | 内容 |
| --- | --- |
| 现象 | 首次 `mvn -B clean package` 在测试阶段失败，`AuthIntegrationTest` 10 例全部报错 |
| 根因 | 集成测试 `flowable.database-schema-update=false`，空库 `ai_scaffold` 尚无 `act_*` 表，Flowable 查询 `act_ge_property` 失败 |
| 处理 | 改用 `-DskipTests` 先产出 JAR，以默认配置启动应用一次完成建表，再执行 `mvn -B test` |
| 记录 | 已记入错题本 [BUG-20261019-004](../错题本/2026-10-19_切换空库后构建因Flowable表缺失失败.md) |

## 未执行项

| 项 | 原因 |
| --- | --- |
| `docker compose config` 与 `docker compose up` 验证 | 本机 Docker 不可用，无法启动 Compose 编排 |
| `619414569应用` 连到同一库的联调 | 该应用未在本机运行；且两应用迁移 `V1`、`V2` 内容不同，共用同一库会导致 Flyway 校验失败，需另议（见版本日志「已知未覆盖项」） |

## 结论

本机已创建角色与库 `ai_scaffold`，表、迁移、账号由应用首次启动建立，`dcy` 与 `admin`
可正常登录，自动化测试 21/21 通过。验收状态保持 `pending`，未提交、未推送。
