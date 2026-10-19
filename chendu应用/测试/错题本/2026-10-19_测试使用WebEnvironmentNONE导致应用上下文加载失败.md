# BUG-20261019-003

| 项 | 值 |
| --- | --- |
| 编号 | BUG-20261019-003 |
| 日期 | 2026-10-19 |
| 严重度 | 低（阻塞构建，不影响已运行环境） |
| 状态 | resolved |
| 模块 | `backend` 测试（`src/test/java/com/aiproject/scaffold/user`） |
| 环境 | 本地开发（JDK 17、Spring Boot 3.3.5、PostgreSQL 14、Surefire 3.2.5） |

## 摘要

新增 `DatabaseUserDetailsServiceTest` 后执行 `mvn -B clean package` 构建失败，
该测试类 9 个用例全部因 `Failed to load ApplicationContext` 报 Error。

## 原始错误

```text
[ERROR] Tests run: 9, Failures: 0, Errors: 9, Skipped: 0 -- in com.aiproject.scaffold.user.DatabaseUserDetailsServiceTest
[ERROR] com.aiproject.scaffold.user.DatabaseUserDetailsServiceTest.updatePasswordReplacesHashAndClearsFlag
        IllegalState Failed to load ApplicationContext

Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with
name 'securityFilterChain' defined in class path resource
[com/aiproject/scaffold/security/SecurityConfig.class]: Unsatisfied dependency expressed through
method 'securityFilterChain' parameter 0: No qualifying bean of type
'org.springframework.security.config.annotation.web.builders.HttpSecurity' available:
expected at least 1 bean which qualifies as autowire candidate.
```

## 根因

测试类使用了 `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)`。

`NONE` 表示**不创建任何 Web 应用上下文**，因此 Spring Security 的 Servlet 自动配置
（`SecurityAutoConfiguration`、`HttpSecurityConfiguration`）不会生效，`HttpSecurity` Bean 不存在。

而 `SecurityConfig#securityFilterChain(HttpSecurity, ...)` 的第一个参数正是 `HttpSecurity`，
上下文刷新时依赖无法满足，导致整个应用上下文加载失败，该测试类全部用例报 Error。

**特征**：错误信息指向 `securityFilterChain` 参数 0，而非被测的
`DatabaseUserDetailsService`；且表现为「一个测试类的所有用例同时 Error」。
这个特征容易误导为数据库连接失败或 Flyway 迁移异常。

## 修复

把 `webEnvironment` 改为 `SpringBootTest.WebEnvironment.MOCK`：

```java
@SpringBootTest(
        // 使用 MOCK 而非 NONE：SecurityConfig#securityFilterChain 依赖 HttpSecurity，
        // 只有模拟 Web 环境才会提供该 Bean；MOCK 不占用端口。
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "app.integration.rocketmq.enabled=false",
                "flowable.database-schema-update=false"
        }
)
```

`MOCK` 会创建模拟 Servlet 环境，使 Spring Security 自动配置生效并提供 `HttpSecurity`，
同时**不启动真实服务器、不占用端口**，适合服务层集成测试。

## 可重复回归用例

自动化测试已固化：`backend/src/test/java/com/aiproject/scaffold/user/DatabaseUserDetailsServiceTest.java`
（9 例）。执行方式见 [测试记录](../测试记录/2026-10-19_数据库账号认证改造测试记录.md)。

```powershell
$env:JAVA_HOME = "C:\dev\jdk-17.0.6+10"
mvn -B clean package
```

**期望结果**：`Tests run: 21, Failures: 0, Errors: 0`，`BUILD SUCCESS`。

**修复前的实际结果**：`Tests run: 21, Failures: 0, Errors: 9`，`BUILD FAILURE`。

## 防复发措施

- 本项目的认证配置依赖 Spring Security 的 Servlet 自动配置，集成测试**不要**使用
  `webEnvironment = NONE`；服务层测试统一使用 `MOCK`。
- 新增测试类时，若表现为「整个测试类所有用例同时 Error」且信息指向配置类的 Bean
  依赖缺失，优先检查 `webEnvironment` 与测试切片注解，而不是数据库可用性。

## 修复后验证

| 项 | 结果 |
| --- | --- |
| `mvn -B clean package` | `BUILD SUCCESS`，01:47 min |
| `AuthIntegrationTest` | 通过 10/10 |
| `DatabaseUserDetailsServiceTest` | 通过 9/9 |
| 合计 | 通过 21/21，0 失败 0 错误 |
