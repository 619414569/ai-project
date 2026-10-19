# 数据说明

本目录说明 `chendu应用` 当前使用的数据对象、初始化数据和迁移管理约定。

## 数据源

| 项 | 值 |
| --- | --- |
| 环境 | 本机开发环境（PostgreSQL 14.24，Windows 服务 `postgresql-x64-14`） |
| 连接地址 | `jdbc:postgresql://localhost:5432/ai_scaffold` |
| 账号来源 | `backend/src/main/resources/application.yml` 的 `spring.datasource` |
| 迁移工具 | Flyway，脚本目录 `classpath:db/migration` |
| 盘点时间 | 2026-10-19 |

> 本轮所有查询均为只读 `SELECT` / 系统目录查询，未对数据库执行任何写操作。生产环境地址应按 `README.md`「PolarDB-PG 配置」一节通过 `DB_URL` 注入。

## 建库脚本

Flyway 需要先连接到一个已存在的库才能执行迁移，因此建库不放在迁移脚本内，单独提供：

`数据/初始化/00_create_database.sql`

该脚本先查询 `pg_roles` / `pg_database` 判断角色与库是否存在，再由 psql 的 `\gexec`
执行生成的 `CREATE ROLE` / `CREATE DATABASE` 语句，因此可重复执行。库名与角色名均为
`ai_scaffold`，与 `619414569应用` 的连接配置一致。

建库后**必须先启动应用一次**：Flyway 建 `app_setting`、`workbench_user`，Flowable 建
`act_*`、`flw_*` 共 38 张表。空库直接执行 `mvn -B test` 会因 `act_ge_property` 不存在而失败，
见错题本 [BUG-20261019-004](../测试/错题本/2026-10-19_切换空库后构建因Flowable表缺失失败.md)。

用法（Windows 本机示例）：

```powershell
$env:PGPASSWORD = '<postgres 口令>'
psql -U postgres -d postgres -f 数据/初始化/00_create_database.sql
```

角色口令通过 psql 变量注入，默认取与 `.env.example` 相同的本地占位值；非本机环境用
`-v db_password='<口令>'` 覆盖。

脚本内不含建表 DDL：表结构的唯一权威来源是
`backend/src/main/resources/db/migration`，在本脚本重复定义会与迁移产生结构漂移（规则 7.1）。

## 表清单（实测）

查询命令：

```sql
SELECT schemaname || '.' || tablename
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY 1;
```

实测结果：

| 类别 | 数量 | 来源 |
| --- | --- | --- |
| 业务表 | 2 | Flyway `V1__create_application_tables.sql`、`V2__create_workbench_user.sql` |
| Flowable 自动建表 | 38 | 应用启动时由 `flowable.database-schema-update: true` 创建 |
| Flyway 元数据表 | 1 | Flyway 自身维护 |
| **合计** | **41** | |

### 业务表

- `public.app_setting`
- `public.workbench_user`

### Flowable 自动建表（38 张）

`act_*` 共 30 张：

```text
act_evt_log          act_ge_bytearray     act_ge_property      act_hi_actinst
act_hi_attachment    act_hi_comment       act_hi_detail        act_hi_entitylink
act_hi_identitylink  act_hi_procinst      act_hi_taskinst      act_hi_tsk_log
act_hi_varinst       act_procdef_info     act_re_deployment    act_re_model
act_re_procdef       act_ru_actinst       act_ru_deadletter_job
act_ru_entitylink    act_ru_event_subscr  act_ru_execution
act_ru_external_job  act_ru_history_job   act_ru_identitylink
act_ru_job           act_ru_suspended_job act_ru_task
act_ru_timer_job     act_ru_variable
```

`flw_*` 共 8 张：

```text
flw_channel_definition      flw_ev_databasechangelog
flw_ev_databasechangeloglock flw_event_definition
flw_event_deployment         flw_event_resource
flw_ru_batch                 flw_ru_batch_part
```

这些表由 `application.yml` 的 `flowable.database-schema-update: true` 在启动时自动创建，不属于业务迁移范围。按 `README.md`「上线前检查」第 6 条，生产环境应改为受控策略。

## `app_setting` 表

用于存放应用级键值配置项，由 `V1__create_application_tables.sql` 创建。

| 字段 | 类型 | 约束 | 业务用途 |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | 主键 | 配置项代理主键，无业务含义 |
| `setting_key` | `VARCHAR(100)` | `NOT NULL`、`UNIQUE` | 配置项唯一标识，例如 `app.initialized` |
| `setting_value` | `TEXT` | 可空 | 配置项取值，统一存字符串，由读取方解析 |
| `description` | `VARCHAR(255)` | 可空 | 配置项中文说明，说明该配置的业务用途，不用于程序判断 |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL`，默认 `CURRENT_TIMESTAMP` | 配置项首次写入时间 |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL`，默认 `CURRENT_TIMESTAMP` | 配置项最后更新时间 |

## `workbench_user` 表

登录账号表。登录请求提交的 `username` 与此表比对，`password_hash` 为 BCrypt 散列，
由 `DaoAuthenticationProvider` 完成校验。由 `V2__create_workbench_user.sql` 创建。

| 字段 | 类型 | 约束 | 业务用途 |
| --- | --- | --- | --- |
| `username` | `VARCHAR(64)` | 主键 | 登录名，业务主键且全局唯一，登录请求提交的 `username` 与此列比对 |
| `password_hash` | `VARCHAR(100)` | `NOT NULL` | BCrypt 口令散列，只保存散列不保存明文 |
| `roles` | `VARCHAR(255)` | `NOT NULL` | 账号拥有的角色清单，英文逗号分隔，例如 `ADMIN,USER` |
| `password_change_required` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | 为 `true` 表示登录后需先修改口令；当前版本未提供改密接口，播种的演示账号显式置为 `false` |
| `active` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | 为 `false` 表示账号已停用，加载时映射为 `disabled` 并拒绝登录 |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL`，默认 `CURRENT_TIMESTAMP` | 账号首次写入时间 |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL`，默认 `CURRENT_TIMESTAMP` | 账号信息最后更新时间，改密时同步刷新 |

索引：仅主键，无额外索引（当前只按 `username` 查询）。
清理策略：暂不自动清理；演示账号播种由 `APP_DEMO_DATA_ENABLED` 控制。
表结构权威来源：`../backend/src/main/resources/db/migration/V2__create_workbench_user.sql`。

## 初始化数据

`V1` 迁移以 `ON CONFLICT (setting_key) DO NOTHING` 写入一行：

| id | setting_key | setting_value | description |
| --- | --- | --- | --- |
| 1 | `app.initialized` | `true` | Created by the initial Flyway migration |

该行用于标记底座初始化已完成，可被重复执行而不产生重复数据。实测 `app_setting` 当前共 1 行。

## 迁移状态

`flyway_schema_history` 实测记录：

| installed_rank | version | description | success |
| --- | --- | --- | --- |
| 1 | 0 | `<< Flyway Baseline >>` | true |
| 2 | 1 | create application tables | true |
| 3 | 2 | create workbench user | true |

## 演示账号数据

演示账号**存储在数据库 `workbench_user` 表中**，由 `DemoUserInitializer` 在应用启动时
按配置项播种（幂等：只补充缺失账号，不覆盖已存在账号）。口令在运行时经 BCrypt 编码后入库，
明文不落库。

| 用户名 | 角色 | 配置项前缀 | 环境变量前缀 |
| --- | --- | --- | --- |
| `admin` | `ADMIN,USER` | `app.security.demo-user` | `DEMO_ADMIN` |
| `dcy` | `USER` | `app.security.additional-demo-users[0]` | `DEMO_DCY` |

相关配置：

| 配置项 | 环境变量 | 说明 |
| --- | --- | --- |
| `app.security.demo-user.username` | `DEMO_ADMIN_USERNAME` | 主演示账号登录名 |
| `app.security.demo-user.password` | `DEMO_ADMIN_PASSWORD` | 主演示账号口令，启动时编码入库 |
| `app.security.demo-user.roles` | `DEMO_ADMIN_ROLES` | 主演示账号角色 |
| `app.security.additional-demo-users[0].username` | `DEMO_DCY_USERNAME` | 附加演示账号登录名 |
| `app.security.additional-demo-users[0].password` | `DEMO_DCY_PASSWORD` | 附加演示账号口令 |
| `app.security.additional-demo-users[0].roles` | `DEMO_DCY_ROLES` | 附加演示账号角色 |
| `app.demo-data.enabled` | `APP_DEMO_DATA_ENABLED` | 关闭后启动时不再播种演示账号 |

实现位置：

- 账号加载：`backend/src/main/java/com/aiproject/scaffold/user/DatabaseUserDetailsService.java`
- 账号播种：`backend/src/main/java/com/aiproject/scaffold/config/DemoUserInitializer.java`
- 配置定义：`backend/src/main/java/com/aiproject/scaffold/config/SecurityProperties.java`
- 表结构：`backend/src/main/resources/db/migration/V2__create_workbench_user.sql`

播种后账号的 `password_change_required` 为 `false`（当前版本未提供改密接口）。
已存在账号不会被覆盖，修改配置中的口令不会自动刷新库中已有账号的散列。

依据规则 8.7，口令明文不写入文档，实际取值见 `application.yml` 与 `.env.example`。

## 变更约定

- 数据库结构变更只允许新增版本化迁移文件，目录为 `backend/src/main/resources/db/migration`。
- 已在共享环境执行过的迁移文件不得修改。
- 新增业务表需按规则第 7 节说明领域归属、字段含义、约束、索引、审计字段和清理策略，并单独获得确认。
