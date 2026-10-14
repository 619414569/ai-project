# 619414569 应用数据

本目录记录工作台当前基础数据口径和可审计的数据维护脚本。

## 当前用户

| 用户名 | 角色 | 初始密码 | 首次改密 |
| --- | --- | --- | --- |
| `dcy` | `USER` | `admin123` | 是 |
| `yf` | `USER` | `admin123` | 是 |
| `zls` | `USER` | `admin123` | 是 |
| `xqx` | `USER` | `admin123` | 是 |

目标服务器设置 `APP_DEMO_DATA_ENABLED=false`，后端重启时不会补回演示用户、应用或提交日志。

## 当前业务数据

| 类型 | 数量 | 说明 |
| --- | ---: | --- |
| 应用 | 0 | 个人应用和合作应用均已清空 |
| 合作人关系 | 0 | 随应用数据一并清空 |
| 提交日志 | 0 | 因外键依赖旧用户，清理用户时一并清空 |

## 数据维护

- `reset-workbench-users.sql`：事务化清空用户、应用、合作关系和提交日志，并创建上述四个用户。
- 脚本复用数据库中现有 `admin123` BCrypt 哈希，不存储明文密码或固定哈希。
- 执行前必须先设置 `APP_DEMO_DATA_ENABLED=false` 并确认新版后端已经生效。

## 权威来源

- 表结构：`../backend/src/main/resources/db/migration/V2__create_workbench_tables.sql`
- 可选演示初始化：`../backend/src/main/java/com/aiproject/scaffold/config/WorkbenchDataInitializer.java`
- 运行时开关：`APP_DEMO_DATA_ENABLED`
