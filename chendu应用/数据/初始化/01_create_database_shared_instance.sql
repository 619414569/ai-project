-- 在共享 PostgreSQL 实例上为本应用创建独立角色与数据库（幂等，可重复执行）
--
-- 用途：chendu应用 与 619414569应用 部署在同一台服务器时，复用后者的 postgres 容器，
--       但使用独立库与独立角色，避免 Flyway 记录、workbench_user 等表与 619414569应用 混用。
--
-- 用法（在 619414569应用 目录执行，脚本通过标准输入进入其 postgres 容器）：
--   docker compose exec -T postgres \
--     psql -U ai_scaffold -d postgres \
--          -v db_role='ai_scaffold_chendu' \
--          -v db_name='ai_scaffold_chendu' \
--          -v db_password='<口令>' \
--     -f - < ../chendu应用/数据/初始化/01_create_database_shared_instance.sql
--
-- 说明：
--   1. 本脚本会写共享数据库实例（CREATE ROLE、CREATE DATABASE），
--      执行前必须按规则第 6 节逐项确认目标实例、库名、角色名和影响范围。
--   2. PostgreSQL 的 CREATE ROLE / CREATE DATABASE 不支持 IF NOT EXISTS，
--      这里先查 pg_roles / pg_database 判断存在性，再由 psql 的 \gexec 执行生成的语句。
--   3. 脚本只建角色与库，不含建表 DDL。表结构由 Flyway 迁移与 Flowable 在应用启动时创建，
--      避免同一结构出现两处定义而产生漂移（规则 7.1）。
--   4. 默认口令是占位值，仅供本机演练；共享或生产环境必须用 -v db_password 注入。

\if :{?db_role}
\else
\set db_role 'ai_scaffold_chendu'
\endif

\if :{?db_name}
\else
\set db_name 'ai_scaffold_chendu'
\endif

\if :{?db_password}
\else
\set db_password 'change-me-db'
\endif

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'db_role', :'db_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'db_role')\gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'db_name', :'db_role')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db_name')\gexec
