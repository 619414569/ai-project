package com.aiproject.scaffold.workspace.dto;

public record UpdateLogDocumentResponse(
        String author,
        String fileName,
        String version,
        String releaseType,
        String releasedAt,
        String summary,
        String markdown
) {
}
