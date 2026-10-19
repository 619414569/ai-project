package com.aiproject.scaffold.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.integration")
public record IntegrationProperties(@Valid @NotNull RocketMq rocketmq) {

    public record RocketMq(boolean enabled, @NotBlank String demoTopic) {
    }
}
