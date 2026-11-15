package com.aiproject.scaffold.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI applicationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Scaffold API")
                        .description("Common backend APIs for the AI project team")
                        .version("2.0.0"));
    }
}
