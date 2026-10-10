package com.aiproject.scaffold.demo.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class CacheService {
    private static final String KEY_PREFIX = "ai-scaffold:demo:";
    private final StringRedisTemplate redisTemplate;

    public CacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void put(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(redisKey(key), value, ttl);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(redisKey(key)));
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(redisKey(key)));
    }

    private String redisKey(String key) {
        return KEY_PREFIX + key;
    }
}
