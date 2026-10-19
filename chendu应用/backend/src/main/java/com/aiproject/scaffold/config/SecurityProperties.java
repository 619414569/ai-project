package com.aiproject.scaffold.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull DemoUser demoUser,
        List<@Valid DemoUser> additionalDemoUsers
) {
    public SecurityProperties {
        additionalDemoUsers = (additionalDemoUsers == null) ? List.of() : List.copyOf(additionalDemoUsers);
    }

    public record Jwt(
            @NotBlank String issuer,
            @NotBlank @Size(min = 32) String secret,
            @NotNull Duration accessTokenTtl
    ) {
    }

    public record DemoUser(
            @NotBlank String username,
            @NotBlank String password,
            @NotEmpty List<String> roles
    ) {
        public DemoUser {
            roles = List.copyOf(roles);
        }
    }
}
