# 619414569 应用数据

本目录记录工作台当前基础数据口径和可审计的数据维护脚本。

## 当前用户

| 用户名 | 角色 | 初始密码 | 首次改密 |
| --- | --- | --- | --- |
| `dcy` | `USER` | `admin123` | 是 |
| `qhh` | `USER` | `admin123` | 是 |
| `yf` | `USER` | `admin123` | 是 |
| `zls` | `USER` | `admin123` | 是 |
| `xqx` | `USER` | `admin123` | 是 |
| `ytt` | `USER` | `admin123` | 是 |

目标服务器设置 `APP_DEMO_DATA_ENABLED=false`，后端重启时不会补回演示用户、应用或提交日志。

## 当前业务数据

| 类型 | 数量 | 说明 |
| --- | ---: | --- |
| 应用 | 3 | 均为个人应用；新增 `qhh`、`ytt` 时保留了现有数据 |
| 合作人关系 | 0 | 当前没有合作人关系 |
| 提交日志 | 0 | 当前没有数据库提交日志记录 |

以上数量于 `2026-10-15` 从生产 PostgreSQL 只读查询确认。`reset-workbench-users.sql` 是全量重置脚本，执行时仍会清空这些业务数据；本次新增用户未执行该脚本。

## 数据维护

- `reset-workbench-users.sql`：事务化清空用户、应用、合作关系和提交日志，并创建上述六个用户。
- 脚本复用数据库中现有 `admin123` BCrypt 哈希，不存储明文密码或固定哈希。
- 执行前必须先设置 `APP_DEMO_DATA_ENABLED=false` 并确认新版后端已经生效。
- `comment-production-database.sql`：为生产库 `public` schema 的全部业务表、Flowable 表及其字段写入中文用途说明；脚本在事务内校验映射与备注覆盖率，存在遗漏时自动回滚。
- `生产数据库全量导出_2026-10-15.xlsx`：生产数据库表结构、关联关系和数据的只读导出，供部署核查与交接使用。

## 生产库备注基线

`2026-10-15` 已在生产 PostgreSQL 执行 `comment-production-database.sql`，并通过独立只读查询确认：

| 检查项 | 结果 |
| --- | ---: |
| 表备注覆盖 | 44 / 44 |
| 字段备注覆盖 | 646 / 646 |
| 缺少备注的表 | 0 |
| 缺少备注的字段 | 0 |

落库前后业务数据计数保持不变：用户 6、应用 3、合作关系 0、提交日志 0。该脚本依赖 Flowable 已完成运行时建表，因此用于部署后的数据库元数据维护，不作为 Flyway 初始化迁移执行。

## 权威来源

- 表结构：`../backend/src/main/resources/db/migration/V2__create_workbench_tables.sql`
- 可选演示初始化：`../backend/src/main/java/com/aiproject/scaffold/config/WorkbenchDataInitializer.java`
- 运行时开关：`APP_DEMO_DATA_ENABLED`
