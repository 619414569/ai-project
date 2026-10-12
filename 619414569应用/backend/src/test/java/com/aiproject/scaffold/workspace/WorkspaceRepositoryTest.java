package com.aiproject.scaffold.workspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:workspace;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@Import(WorkspaceRepository.class)
@Sql(scripts = "/workspace-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class WorkspaceRepositoryTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkspaceRepository repository;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO workbench_user (username, password_hash, roles) VALUES ('admin', 'hash', 'ADMIN')");
        jdbcTemplate.update("INSERT INTO workbench_user (username, password_hash, roles) VALUES ('chyong', 'hash', 'USER')");
        insertApplication("admin-exclusive", "personal", "exclusive", "admin");
        insertApplication("admin-shared", "personal", "shared", "admin");
        insertApplication("chyong-exclusive", "personal", "exclusive", "chyong");
        insertApplication("admin-collaboration", "collaboration", "shared", "admin");
        jdbcTemplate.update(
                "INSERT INTO workspace_application_collaborator (application_id, username) VALUES ('admin-collaboration', 'chyong')"
        );
    }

    @Test
    void ownerSeesOwnApplicationsAndSharedPersonalApplications() {
        List<String> ids = repository.findVisibleApplications("admin").stream().map(WorkspaceApplication::id).toList();

        assertEquals(3, ids.size());
        assertTrue(ids.containsAll(List.of("admin-exclusive", "admin-shared", "admin-collaboration")));
    }

    @Test
    void collaboratorCannotSeeOtherUsersExclusivePersonalApplication() {
        List<String> ids = repository.findVisibleApplications("chyong").stream().map(WorkspaceApplication::id).toList();

        assertEquals(3, ids.size());
        assertTrue(ids.containsAll(List.of("chyong-exclusive", "admin-shared", "admin-collaboration")));
    }

    private void insertApplication(String id, String scope, String visibility, String owner) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO workspace_application
                            (id, name, description, category, scope, visibility, status, owner_username,
                             created_at, updated_at)
                        VALUES (?, ?, ?, '智能问答', ?, ?, 'running', ?, ?, ?)
                        """,
                id,
                id,
                "description",
                scope,
                visibility,
                owner,
                now,
                now
        );
    }
}
