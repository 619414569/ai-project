package com.aiproject.scaffold.demo;

import com.aiproject.scaffold.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/public")
public class PublicController {
    private final String applicationName;

    public PublicController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/ping")
    public ApiResponse<PingResponse> ping() {
        return ApiResponse.success(new PingResponse(applicationName, "UP", Instant.now()));
    }

    public record PingResponse(String application, String status, Instant timestamp) {
    }
}
