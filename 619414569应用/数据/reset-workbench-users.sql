\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE retained_password_hash ON COMMIT DROP AS
SELECT password_hash
FROM workbench_user
ORDER BY username
LIMIT 1;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM retained_password_hash) THEN
        RAISE EXCEPTION 'No existing password hash is available for the reset';
    END IF;
END
$$;

-- submission_log must be cleared because it references workbench_user.
TRUNCATE TABLE
    workspace_application_collaborator,
    workspace_application,
    submission_log,
    workbench_user;

INSERT INTO workbench_user (
    username,
    password_hash,
    roles,
    password_change_required,
    active
)
SELECT
    requested_user.username,
    retained_password_hash.password_hash,
    'USER',
    TRUE,
    TRUE
FROM (VALUES
    ('dcy'),
    ('qhh'),
    ('yf'),
    ('zls'),
    ('xqx'),
    ('ytt')
) AS requested_user(username)
CROSS JOIN retained_password_hash;

COMMIT;

SELECT username, roles, password_change_required, active
FROM workbench_user
ORDER BY username;

SELECT
    (SELECT COUNT(*) FROM workspace_application) AS application_count,
    (SELECT COUNT(*) FROM workspace_application_collaborator) AS collaborator_count,
    (SELECT COUNT(*) FROM submission_log) AS submission_log_count;
