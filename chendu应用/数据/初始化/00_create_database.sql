-- 建角色与建库脚本（幂等，可重复执行）
--
-- 用途：在本机 PostgreSQL 实例上创建本应用使用的登录角色与数据库，
--       与 619414569应用 的连接配置保持一致（库名 ai_scaffold、角色 ai_scaffold）。
--
-- 用法：
--   psql -U postgres -d postgres -f 数据/初始化/00_create_database.sql
--   指定口令：psql -U postgres -d postgres -v db_password='<口令>' -f 数据/初始化/00_create_database.sql
--
-- 说明：
--   1. PostgreSQL 的 CREATE ROLE / CREATE DATABASE 不支持 IF NOT EXISTS，这里先查
--      pg_roles / pg_database 做存在性判断，再由 psql 的 \gexec 执行生成的语句，实现幂等。
--   2. 脚本只建角色和库，不包含建表 DDL。表结构由 Flyway 迁移
--      V1__create_application_tables.sql 与 V2__create_workbench_user.sql 在应用启动时创建，
--      避免同一结构出现两处定义而产生漂移（规则 7.1）。
--   3. 默认口令是本地开发占位值，与 .env.example 的 POSTGRES_PASSWORD 默认值一致。
--      任何非本机环境都应通过 -v db_password 注入，不要使用默认值。

\if :{?db_password}
\else
\set db_password 'change-me-postgres'
\endif

SELECT 'CREATE ROLE "ai_scaffold" LOGIN PASSWORD ' || quote_literal(:'db_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ai_scaffold')\gexec

SELECT 'CREATE DATABASE "ai_scaffold" OWNER "ai_scaffold"'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'ai_scaffold')\gexec
