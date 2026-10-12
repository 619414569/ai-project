package com.aiproject.scaffold.workspace;

import com.aiproject.scaffold.workspace.dto.SubmissionLogResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class WorkspaceRepository {
    private static final String APPLICATION_COLUMNS = """
            a.id, a.name, a.description, a.category, a.scope, a.visibility, a.status,
            a.owner_username, a.application_url, a.created_at, a.updated_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public WorkspaceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<WorkspaceApplication> findVisibleApplications(String username) {
        List<ApplicationRow> rows = jdbcTemplate.query(
                """
                        SELECT DISTINCT %s
                        FROM workspace_application a
                        LEFT JOIN workspace_application_collaborator c ON c.application_id = a.id
                        WHERE a.owner_username = ?
                           OR (a.scope = 'personal' AND a.visibility = 'shared')
                           OR (a.scope = 'collaboration' AND a.visibility = 'shared' AND c.username = ?)
                        ORDER BY a.updated_at DESC
                        """.formatted(APPLICATION_COLUMNS),
                this::mapApplicationRow,
                username,
                username
        );
        Map<String, List<String>> collaborators = loadCollaborators();
        return rows.stream().map(row -> row.toApplication(collaborators.getOrDefault(row.id(), List.of()))).toList();
    }

    public Optional<WorkspaceApplication> findById(String id) {
        List<ApplicationRow> rows = jdbcTemplate.query(
                "SELECT " + APPLICATION_COLUMNS + " FROM workspace_application a WHERE a.id = ?",
                this::mapApplicationRow,
                id
        );
        if (rows.isEmpty()) return Optional.empty();
        List<String> collaborators = jdbcTemplate.queryForList(
                "SELECT username FROM workspace_application_collaborator WHERE application_id = ? ORDER BY username",
                String.class,
                id
        );
        return Optional.of(rows.get(0).toApplication(collaborators));
    }

    public void insert(WorkspaceApplication application) {
        jdbcTemplate.update(
                """
                        INSERT INTO workspace_application
                            (id, name, description, category, scope, visibility, status, owner_username,
                             application_url, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                application.id(),
                application.name(),
                application.description(),
                application.category(),
                application.scope(),
                application.visibility(),
                application.status(),
                application.owner(),
                application.applicationUrl(),
                application.createdAt(),
                application.updatedAt()
        );
        replaceCollaborators(application.id(), application.collaborators());
    }

    public void update(WorkspaceApplication application) {
        jdbcTemplate.update(
                """
                        UPDATE workspace_application
                        SET visibility = ?, status = ?, application_url = ?, updated_at = ?
                        WHERE id = ?
                        """,
                application.visibility(),
                application.status(),
                application.applicationUrl(),
                application.updatedAt(),
                application.id()
        );
        replaceCollaborators(application.id(), application.collaborators());
    }

    public void delete(String id) {
        jdbcTemplate.update("DELETE FROM workspace_application WHERE id = ?", id);
    }

    public List<SubmissionLogResponse> findSubmissionLogs() {
        return jdbcTemplate.query(
                """
                        SELECT id, username, committed_at, commit_hash, summary, area
                        FROM submission_log
                        ORDER BY committed_at DESC
                        """,
                (resultSet, rowNum) -> new SubmissionLogResponse(
                        resultSet.getString("id"),
                        resultSet.getString("username"),
                        resultSet.getObject("committed_at", OffsetDateTime.class),
                        resultSet.getString("commit_hash"),
                        resultSet.getString("summary"),
                        resultSet.getString("area")
                )
        );
    }

    private void replaceCollaborators(String applicationId, List<String> collaborators) {
        jdbcTemplate.update("DELETE FROM workspace_application_collaborator WHERE application_id = ?", applicationId);
        for (String username : collaborators) {
            jdbcTemplate.update(
                    "INSERT INTO workspace_application_collaborator (application_id, username) VALUES (?, ?)",
                    applicationId,
                    username
            );
        }
    }

    private Map<String, List<String>> loadCollaborators() {
        Map<String, List<String>> result = new HashMap<>();
        jdbcTemplate.query(
                "SELECT application_id, username FROM workspace_application_collaborator ORDER BY username",
                resultSet -> {
                    result.computeIfAbsent(resultSet.getString("application_id"), ignored -> new ArrayList<>())
                            .add(resultSet.getString("username"));
                }
        );
        return result;
    }

    private ApplicationRow mapApplicationRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new ApplicationRow(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getString("category"),
                resultSet.getString("scope"),
                resultSet.getString("visibility"),
                resultSet.getString("status"),
                resultSet.getString("owner_username"),
                resultSet.getString("application_url"),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class)
        );
    }

    private record ApplicationRow(
            String id,
            String name,
            String description,
            String category,
            String scope,
            String visibility,
            String status,
            String owner,
            String applicationUrl,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        WorkspaceApplication toApplication(List<String> collaborators) {
            return new WorkspaceApplication(
                    id, name, description, category, scope, visibility, status, owner,
                    collaborators, applicationUrl, createdAt, updatedAt
            );
        }
    }
}
