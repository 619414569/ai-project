package com.aiproject.scaffold.workspace;

import com.aiproject.scaffold.common.api.ErrorCode;
import com.aiproject.scaffold.common.exception.BusinessException;
import com.aiproject.scaffold.user.DatabaseUserDetailsService;
import com.aiproject.scaffold.workspace.dto.CreateApplicationRequest;
import com.aiproject.scaffold.workspace.dto.SubmissionLogResponse;
import com.aiproject.scaffold.workspace.dto.UpdateApplicationRequest;
import com.aiproject.scaffold.workspace.dto.WorkspaceApplicationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkspaceService {
    private static final Set<String> CATEGORIES = Set.of("智能问答", "数据分析", "内容生成", "流程自动化", "知识管理");

    private final WorkspaceRepository repository;
    private final DatabaseUserDetailsService userDetailsService;

    public WorkspaceService(WorkspaceRepository repository, DatabaseUserDetailsService userDetailsService) {
        this.repository = repository;
        this.userDetailsService = userDetailsService;
    }

    public List<WorkspaceApplicationResponse> listApplications(String username) {
        return repository.findVisibleApplications(username).stream().map(this::toResponse).toList();
    }

    public List<SubmissionLogResponse> listSubmissionLogs() {
        return repository.findSubmissionLogs();
    }

    @Transactional
    public WorkspaceApplicationResponse createApplication(String username, CreateApplicationRequest request) {
        validateCategory(request.category());
        List<String> collaborators = validateCollaborators(username, request.scope(), request.collaborators());
        OffsetDateTime now = OffsetDateTime.now();
        WorkspaceApplication application = new WorkspaceApplication(
                UUID.randomUUID().toString(),
                request.name().trim(),
                request.description().trim(),
                request.category(),
                request.scope(),
                request.visibility(),
                "running",
                username,
                collaborators,
                normalizeApplicationUrl(request.applicationUrl()),
                now,
                now
        );
        repository.insert(application);
        return toResponse(application);
    }

    @Transactional
    public WorkspaceApplicationResponse updateApplication(
            String username,
            String applicationId,
            UpdateApplicationRequest request
    ) {
        WorkspaceApplication current = requireApplication(applicationId);
        requireOwner(username, current);

        List<String> collaborators = request.collaborators() == null
                ? current.collaborators()
                : validateCollaborators(username, current.scope(), request.collaborators());
        String applicationUrl = request.applicationUrl() == null
                ? current.applicationUrl()
                : normalizeApplicationUrl(request.applicationUrl());
        WorkspaceApplication updated = new WorkspaceApplication(
                current.id(),
                current.name(),
                current.description(),
                current.category(),
                current.scope(),
                request.visibility() == null ? current.visibility() : request.visibility(),
                request.status() == null ? current.status() : request.status(),
                current.owner(),
                collaborators,
                applicationUrl,
                current.createdAt(),
                OffsetDateTime.now()
        );
        repository.update(updated);
        return toResponse(updated);
    }

    @Transactional
    public void deleteApplication(String username, String applicationId) {
        WorkspaceApplication current = requireApplication(applicationId);
        requireOwner(username, current);
        repository.delete(applicationId);
    }

    private WorkspaceApplication requireApplication(String applicationId) {
        return repository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Application was not found"));
    }

    private void requireOwner(String username, WorkspaceApplication application) {
        if (!application.owner().equals(username)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only the application owner can perform this operation");
        }
    }

    private void validateCategory(String category) {
        if (!CATEGORIES.contains(category)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Application category is invalid");
        }
    }

    private List<String> validateCollaborators(String owner, String scope, List<String> requested) {
        if ("personal".equals(scope)) return List.of();
        if (requested.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "A collaboration application requires at least one collaborator");
        }

        Set<String> activeUsers = new LinkedHashSet<>(userDetailsService.listActiveUsernames());
        LinkedHashSet<String> collaborators = new LinkedHashSet<>();
        for (String username : requested) {
            if (!activeUsers.contains(username)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Collaborator does not exist: " + username);
            }
            if (!owner.equals(username)) collaborators.add(username);
        }
        if (collaborators.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "A collaboration application requires at least one collaborator");
        }
        return List.copyOf(collaborators);
    }

    private String normalizeApplicationUrl(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.startsWith("/") && !trimmed.startsWith("//")) return trimmed;
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Application URL must use HTTP or HTTPS");
            }
            return uri.normalize().toString();
        } catch (URISyntaxException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Application URL is invalid");
        }
    }

    private WorkspaceApplicationResponse toResponse(WorkspaceApplication application) {
        return new WorkspaceApplicationResponse(
                application.id(),
                application.name(),
                application.description(),
                application.category(),
                application.scope(),
                application.visibility(),
                application.status(),
                application.owner(),
                application.collaborators(),
                application.applicationUrl(),
                application.updatedAt()
        );
    }
}
