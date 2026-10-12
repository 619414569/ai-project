# 619414569 v1.0 基础数据

本目录记录工作台 v1.0 的初始化数据口径。运行时表结构与幂等初始化代码仍随应用打包，避免部署产物依赖仓库外部文件。

## 初始化用户

| 用户名 | 默认角色 | 默认密码 | 首次改密 |
| --- | --- | --- | --- |
| `admin` | `ADMIN,USER` | `admin123` | 是 |
| `chyong` | `USER` | `admin123` | 是 |
| `liyan` | `USER` | `admin123` | 是 |
| `zhangwei` | `USER` | `admin123` | 是 |
| `wangqi` | `USER` | `admin123` | 是 |

`admin` 可由 `DEMO_ADMIN_USERNAME` 改名，所有默认密码由 `DEMO_ADMIN_PASSWORD` 统一配置。默认凭据只用于本地演示环境。

## 初始化业务数据

| 类型 | 数量 | 说明 |
| --- | ---: | --- |
| 个人应用 | 7 | 覆盖共享、独享、运行和暂停状态 |
| 合作应用 | 3 | 包含创建人及合作人关系 |
| 提交日志 | 4 | 对应仓库初始提交记录 |

## 权威来源

- 表结构：`../619414569文件夹-应用/backend/src/main/resources/db/migration/V2__create_workbench_tables.sql`
- 初始化逻辑：`../619414569文件夹-应用/backend/src/main/java/com/aiproject/scaffold/config/WorkbenchDataInitializer.java`

初始化逻辑按主键判重，可重复启动；正式环境应关闭或替换演示数据初始化，并使用独立的迁移脚本导入业务数据。
