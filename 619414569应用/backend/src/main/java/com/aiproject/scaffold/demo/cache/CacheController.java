package com.aiproject.scaffold.demo.cache;

import com.aiproject.scaffold.common.api.ApiResponse;
import com.aiproject.scaffold.common.api.ErrorCode;
import com.aiproject.scaffold.common.exception.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Validated
@RestController
@RequestMapping("/api/v1/demo/cache")
public class CacheController {
    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @PutMapping("/{key}")
    public ApiResponse<CacheValue> put(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9._-]{1,64}") String key,
            @Valid @RequestBody CachePutRequest request
    ) {
        cacheService.put(key, request.value(), Duration.ofSeconds(request.ttlSeconds()));
        return ApiResponse.success(new CacheValue(key, request.value()));
    }

    @GetMapping("/{key}")
    public ApiResponse<CacheValue> get(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9._-]{1,64}") String key
    ) {
        String value = cacheService.get(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Cache key does not exist"));
        return ApiResponse.success(new CacheValue(key, value));
    }

    @DeleteMapping("/{key}")
    public ApiResponse<DeleteResult> delete(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9._-]{1,64}") String key
    ) {
        return ApiResponse.success(new DeleteResult(key, cacheService.delete(key)));
    }

    public record CachePutRequest(
            @NotBlank(message = "Cache value is required") String value,
            @NotNull @Min(1) @Max(86400) Long ttlSeconds
    ) {
    }

    public record CacheValue(String key, String value) {
    }

    public record DeleteResult(String key, boolean deleted) {
    }
}
