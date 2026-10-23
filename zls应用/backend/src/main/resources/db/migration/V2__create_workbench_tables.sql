CREATE TABLE IF NOT EXISTS workbench_user (
    username VARCHAR(64) PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL,
    roles VARCHAR(255) NOT NULL,
    password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workspace_application (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    description VARCHAR(120) NOT NULL,
    category VARCHAR(32) NOT NULL,
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('personal', 'collaboration')),
    visibility VARCHAR(20) NOT NULL CHECK (visibility IN ('shared', 'exclusive')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('running', 'paused')),
    owner_username VARCHAR(64) NOT NULL REFERENCES workbench_user(username),
    application_url VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workspace_application_owner
    ON workspace_application(owner_username);
CREATE INDEX IF NOT EXISTS idx_workspace_application_visibility
    ON workspace_application(scope, visibility);

CREATE TABLE IF NOT EXISTS workspace_application_collaborator (
    application_id VARCHAR(64) NOT NULL REFERENCES workspace_application(id) ON DELETE CASCADE,
    username VARCHAR(64) NOT NULL REFERENCES workbench_user(username),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (application_id, username)
);

CREATE INDEX IF NOT EXISTS idx_workspace_collaborator_username
    ON workspace_application_collaborator(username);

CREATE TABLE IF NOT EXISTS submission_log (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(64) NOT NULL REFERENCES workbench_user(username),
    committed_at TIMESTAMPTZ NOT NULL,
    commit_hash VARCHAR(64) NOT NULL,
    area VARCHAR(64) NOT NULL,
    summary VARCHAR(500) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_submission_log_user_time
    ON submission_log(username, committed_at DESC);
