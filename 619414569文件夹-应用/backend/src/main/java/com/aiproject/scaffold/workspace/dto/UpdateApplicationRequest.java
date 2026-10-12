package com.aiproject.scaffold.workspace.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateApplicationRequest(
        @Pattern(regexp = "shared|exclusive", message = "Application visibility is invalid") String visibility,
        @Pattern(regexp = "running|paused", message = "Application status is invalid") String status,
        List<@Size(min = 1, max = 64) String> collaborators,
        @Size(max = 1000, message = "Application URL must not exceed 1000 characters") String applicationUrl
) {
    public UpdateApplicationRequest {
        collaborators = collaborators == null ? null : List.copyOf(collaborators);
    }
}
