package com.aiproject.scaffold.workspace;

import java.time.OffsetDateTime;
import java.util.List;

public record WorkspaceApplication(
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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public WorkspaceApplication {
        collaborators = List.copyOf(collaborators);
    }
}
