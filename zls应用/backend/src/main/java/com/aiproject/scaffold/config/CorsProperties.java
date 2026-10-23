package com.aiproject.scaffold.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(@NotEmpty List<String> allowedOriginPatterns) {
    public CorsProperties {
        allowedOriginPatterns = List.copyOf(allowedOriginPatterns);
    }
}
