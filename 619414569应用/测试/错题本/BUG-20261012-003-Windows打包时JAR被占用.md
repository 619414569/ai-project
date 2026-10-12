---
id: BUG-20261012-003
date: 2026-10-12
severity: P1
status: resolved
summary: Windows 上运行中的后端 JAR 导致 clean package 失败
regression: stop old backend process before mvn clean package
---

# Windows 打包时 JAR 被占用

## 原始报错

```text
Failed to execute goal org.apache.maven.plugins:maven-clean-plugin:3.3.2:clean
Failed to delete backend/target/ai-scaffold-backend.jar
```

## 根因

Windows 会锁定正在运行的可执行 JAR。旧后端仍从 `target/ai-scaffold-backend.jar` 运行时，Maven clean 无法删除并重新生成该文件。

## 修复与防复发校验

- 干净打包前先停止从 `target` 目录运行的旧后端进程。
- 发布检查确认后端端口对应进程已停止，再执行 `mvn -B -ntp clean package`。
- 打包成功后核对新 JAR 的修改时间，并使用新产物启动验收环境。
