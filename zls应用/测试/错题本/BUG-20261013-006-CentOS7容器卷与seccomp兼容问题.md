---
id: BUG-20261013-006
date: 2026-10-13
severity: P0
status: resolved
summary: CentOS 7 旧内核下 PostgreSQL 被默认 seccomp 拒绝写入且 RocketMQ 命名卷属主不正确
regression: Compose seccomp compatibility and RocketMQ volume initialization check, server health and external HTTP acceptance
---

# CentOS 7 容器卷与 seccomp 兼容问题

## 原始问题

- `postgres:16-alpine` 初始化数据目录时反复报错：`could not write to file "postmaster.pid": Operation not permitted`，容器持续重启。
- RocketMQ 的日志和存储命名卷首次创建为 `root:root`，镜像内的 `rocketmq` 用户无法创建目录，Broker 随后以 `NullPointerException` 退出。
- PostgreSQL、RocketMQ Broker 未健康，导致后端和前端都停留在 `Created`，外部端口 `1888` 无法访问。

## 根因

- 目标服务器使用 CentOS 7 和 `3.10` 内核。Docker 默认 seccomp 配置会以 `EPERM` 拒绝当前 PostgreSQL 镜像使用的部分系统调用；相同镜像在 `seccomp=unconfined` 的隔离诊断中可正常初始化并启动。
- Docker 命名卷默认由 root 创建，而 RocketMQ 5.3.1 镜像以非 root 用户运行，Compose 未在首次启动前初始化卷属主。

## 修复

- 仅对 PostgreSQL 服务设置 `security_opt: seccomp=unconfined`，兼容 CentOS 7 旧内核；生产环境升级到受支持的新内核后可复核并移除此兼容项。
- 新增一次性 `rocketmq-volume-init` 服务，以 root 身份把 Nameserver 日志、Broker 日志和存储卷归属调整为 `rocketmq:rocketmq`。
- RocketMQ Nameserver 等待卷初始化成功后再启动，Broker 继续等待 Nameserver 健康后启动。

## 防复发校验

- Compose 静态检查必须确认 PostgreSQL 含 CentOS 7 seccomp 兼容项、RocketMQ 卷初始化服务存在并被 Nameserver 依赖。
- 在目标服务器执行 Compose 集成测试，要求 PostgreSQL、Redis、RocketMQ、后端和前端全部健康。
- 服务器本地和外部网络分别访问 `/healthz` 与首页，均成功后才能通过部署验收。
