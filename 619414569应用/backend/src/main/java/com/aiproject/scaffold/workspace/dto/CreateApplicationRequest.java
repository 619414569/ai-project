package com.aiproject.scaffold.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateApplicationRequest(
        @NotBlank(message = "Application name is required")
        @Size(min = 2, max = 30, message = "Application name must contain 2 to 30 characters")
        String name,
        @NotBlank(message = "Application description is required")
        @Size(max = 120, message = "Application description must not exceed 120 characters")
        String description,
        @NotBlank(message = "Application category is required") String category,
        @NotBlank(message = "Application scope is required")
        @Pattern(regexp = "personal|collaboration", message = "Application scope is invalid") String scope,
        @NotBlank(message = "Application visibility is required")
        @Pattern(regexp = "shared|exclusive", message = "Application visibility is invalid") String visibility,
        List<@Size(min = 1, max = 64) String> collaborators,
        @Size(max = 1000, message = "Application URL must not exceed 1000 characters") String applicationUrl
) {
    public CreateApplicationRequest {
        collaborators = collaborators == null ? List.of() : List.copyOf(collaborators);
    }
}
