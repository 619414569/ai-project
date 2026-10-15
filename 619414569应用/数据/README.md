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

## 权威来源

- 表结构：`../backend/src/main/resources/db/migration/V2__create_workbench_tables.sql`
- 可选演示初始化：`../backend/src/main/java/com/aiproject/scaffold/config/WorkbenchDataInitializer.java`
- 运行时开关：`APP_DEMO_DATA_ENABLED`
