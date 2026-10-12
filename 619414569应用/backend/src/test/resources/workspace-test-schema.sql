DROP TABLE IF EXISTS workspace_application_collaborator;
DROP TABLE IF EXISTS submission_log;
DROP TABLE IF EXISTS workspace_application;
DROP TABLE IF EXISTS workbench_user;

CREATE TABLE workbench_user (
    username VARCHAR(64) PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL,
    roles VARCHAR(255) NOT NULL,
    password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workspace_application (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    description VARCHAR(120) NOT NULL,
    category VARCHAR(32) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    owner_username VARCHAR(64) NOT NULL REFERENCES workbench_user(username),
    application_url VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workspace_application_collaborator (
    application_id VARCHAR(64) NOT NULL REFERENCES workspace_application(id) ON DELETE CASCADE,
    username VARCHAR(64) NOT NULL REFERENCES workbench_user(username),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (application_id, username)
);

CREATE TABLE submission_log (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(64) NOT NULL REFERENCES workbench_user(username),
    committed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    commit_hash VARCHAR(64) NOT NULL,
    area VARCHAR(64) NOT NULL,
    summary VARCHAR(500) NOT NULL
);
