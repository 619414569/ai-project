package com.aiproject.scaffold.workspace.dto;

import java.time.OffsetDateTime;

public record SubmissionLogResponse(
        String id,
        String username,
        OffsetDateTime committedAt,
        String commit,
        String summary,
        String area
) {
}
