CREATE TABLE IF NOT EXISTS workbench_user (
    username VARCHAR(64) PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL,
    roles VARCHAR(255) NOT NULL,
    password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE workbench_user IS '登录账号表：保存可通过 /api/v1/auth/login 认证的账号、BCrypt 口令散列和授权角色，替代原先仅由配置项构造的演示账号';
COMMENT ON COLUMN workbench_user.username IS '登录名，业务主键且全局唯一，登录请求提交的 username 与此列比对';
COMMENT ON COLUMN workbench_user.password_hash IS 'BCrypt 口令散列，只保存散列不保存明文，登录时由 DaoAuthenticationProvider 比对';
COMMENT ON COLUMN workbench_user.roles IS '账号拥有的角色清单，英文逗号分隔，例如 ADMIN,USER；读取时拆分并映射为 Spring Security 的 ROLE_ 权限';
COMMENT ON COLUMN workbench_user.password_change_required IS '为 true 表示该账号登录后需先修改口令；当前版本未提供改密接口，播种的演示账号显式置为 false';
COMMENT ON COLUMN workbench_user.active IS '为 false 表示账号已停用，加载时映射为 UserDetails 的 disabled 并拒绝登录';
COMMENT ON COLUMN workbench_user.created_at IS '账号首次写入时间，用于审计';
COMMENT ON COLUMN workbench_user.updated_at IS '账号信息最后更新时间，改密时同步刷新';
