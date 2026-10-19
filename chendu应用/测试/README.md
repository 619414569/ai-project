# chendu 测试资料

本目录集中存放 `chendu应用` 的测试记录与错题本。

- [验收状态](./验收状态.md)
- [错题本](./错题本/README.md)
- 测试记录：
  - [2026-10-19 数据库名统一为 ai_scaffold 验证记录](./测试记录/2026-10-19_数据库名统一为ai_scaffold验证记录.md)
  - [2026-10-19 数据库账号认证改造测试记录](./测试记录/2026-10-19_数据库账号认证改造测试记录.md)
  - [2026-10-19 登录与跨域回归测试记录](./测试记录/2026-10-19_登录与跨域回归测试记录.md)

## 执行方式

自动化测试位于 `backend/src/test/`，从 `chendu应用/backend` 目录执行：

```powershell
$env:JAVA_HOME = "C:\dev\jdk-17.0.6+10"
mvn -B test
mvn -B clean package
```

> 注意：后端进程运行期间会占用 `target/ai-scaffold-backend.jar`，`mvn clean` 会失败。构建前需先停止后端进程，构建完成后重启。

> 注意：登录已改为数据库校验，集成测试（`AuthIntegrationTest`、`DatabaseUserDetailsServiceTest`）会执行 Flyway 迁移并读写 `workbench_user` 表，需要本机 PostgreSQL 可连通。

前端类型检查与生产构建，从 `chendu应用/frontend` 目录执行：

```bash
npm run typecheck   # vue-tsc --noEmit
npm run build       # vue-tsc --noEmit && vite build
```

## 测试记录索引

| 日期 | 记录 | 覆盖范围 | 结果 |
| --- | --- | --- | --- |
| 2026-10-19 | [数据库名统一为 ai_scaffold 验证记录](./测试记录/2026-10-19_数据库名统一为ai_scaffold验证记录.md) | 本机建角色与库 `ai_scaffold`、建库脚本、表与账号落库、登录回归 | 自动化测试 21/21 通过；11 项检查全部符合期望（Compose 启动因 Docker 不可用未执行） |
| 2026-10-19 | [数据库账号认证改造测试记录](./测试记录/2026-10-19_数据库账号认证改造测试记录.md) | 登录改为 `workbench_user` 表校验、演示账号播种、跨域边界 | 自动化测试 21/21 通过；端到端 12/12 组合符合期望 |
| 2026-10-19 | [登录与跨域回归测试记录](./测试记录/2026-10-19_登录与跨域回归测试记录.md) | BUG-20261019-001、BUG-20261019-002 回归，登录主流程端到端 | 自动化测试 9/9 通过；端到端 6/6 组合符合期望 |
