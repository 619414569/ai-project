package com.aiproject.scaffold.security;

import com.aiproject.scaffold.config.CorsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SecurityConfigTest {

    @Test
    void allowsLoopbackDevelopmentOriginsOnDynamicPorts() {
        CorsConfiguration configuration = corsConfiguration(List.of(
                "http://localhost:[*]",
                "http://127.0.0.1:[*]"
        ));

        assertEquals("http://127.0.0.1:5174", configuration.checkOrigin("http://127.0.0.1:5174"));
        assertEquals("http://localhost:5173", configuration.checkOrigin("http://localhost:5173"));
        assertNull(configuration.checkOrigin("https://example.com"));
    }

    private CorsConfiguration corsConfiguration(List<String> allowedOriginPatterns) {
        SecurityConfig securityConfig = new SecurityConfig();
        CorsConfigurationSource source = securityConfig.corsConfigurationSource(
                new CorsProperties(allowedOriginPatterns)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        CorsConfiguration configuration = source.getCorsConfiguration(request);
        assertNotNull(configuration);
        return configuration;
    }
}
