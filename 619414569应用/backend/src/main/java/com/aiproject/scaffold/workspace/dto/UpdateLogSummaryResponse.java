package com.aiproject.scaffold.workspace.dto;

public record UpdateLogSummaryResponse(
        String author,
        String fileName,
        String version,
        String releaseType,
        String releasedAt,
        String summary
) {
}
