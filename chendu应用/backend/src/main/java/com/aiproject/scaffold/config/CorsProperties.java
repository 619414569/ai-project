package com.aiproject.scaffold.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins, List<String> allowedOriginPatterns) {
    public CorsProperties {
        allowedOrigins = List.copyOf(allowedOrigins);
        allowedOriginPatterns = (allowedOriginPatterns == null) ? List.of() : List.copyOf(allowedOriginPatterns);
    }
}
