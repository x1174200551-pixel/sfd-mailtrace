package com.ntn.fziot.mailtrace.infrastructure.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MtRedisCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MtRedisCacheProperties properties;

    public Optional<Object> get(String cacheName, String businessKey, Type returnType) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        try {
            String value = redisTemplate.opsForValue().get(buildKey(cacheName, businessKey));
            if (value == null) {
                return Optional.empty();
            }
            JavaType javaType = objectMapper.getTypeFactory().constructType(returnType);
            return Optional.ofNullable(objectMapper.readValue(value, javaType));
        } catch (Exception exception) {
            log.warn("读取 Redis 缓存失败，已回退数据库查询，cacheName={}", cacheName, exception);
            return Optional.empty();
        }
    }

    public void put(String cacheName, String businessKey, Object value, long ttlSeconds) {
        if (!properties.isEnabled() || value == null || ttlSeconds <= 0) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(
                    buildKey(cacheName, businessKey),
                    json,
                    Duration.ofSeconds(ttlSeconds)
            );
        } catch (Exception exception) {
            log.warn("写入 Redis 缓存失败，业务结果不受影响，cacheName={}", cacheName, exception);
        }
    }

    public void delete(String cacheName, String businessKey) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            redisTemplate.delete(buildKey(cacheName, businessKey));
        } catch (Exception exception) {
            log.warn("删除 Redis 缓存失败，数据库写入不受影响，cacheName={}", cacheName, exception);
        }
    }

    String buildKey(String cacheName, String businessKey) {
        return String.join(":",
                valueOrDefault(properties.getKeyPrefix(), "mailtrace"),
                valueOrDefault(properties.getEnvironment(), "default"),
                valueOrDefault(cacheName, "unknown"),
                valueOrDefault(businessKey, "unknown"));
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
