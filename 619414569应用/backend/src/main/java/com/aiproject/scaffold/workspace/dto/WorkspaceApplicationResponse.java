package com.aiproject.scaffold.workspace.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record WorkspaceApplicationResponse(
        String id,
        String name,
        String description,
        String category,
        String scope,
        String visibility,
        String status,
        String owner,
        List<String> collaborators,
        String applicationUrl,
        OffsetDateTime updatedAt
) {
    public WorkspaceApplicationResponse {
        collaborators = List.copyOf(collaborators);
    }
}
