package com.ntn.fziot.mailtrace.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MtRedisCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private MtRedisCacheService cacheService;

    @BeforeEach
    void setUp() {
        MtRedisCacheProperties properties = new MtRedisCacheProperties();
        properties.setEnvironment("test");
        cacheService = new MtRedisCacheService(redisTemplate, new ObjectMapper(), properties);
    }

    @Test
    void get_shouldDeserializeUsingDeclaredGenericReturnType() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mailtrace:test:role-permission-codes:20"))
                .thenReturn("[\"ticket:read\"]");
        Type returnType = new TypeReference<Set<String>>() {
        }.getType();

        Optional<Object> result = cacheService.get("role-permission-codes", "20", returnType);

        assertEquals(Set.of("ticket:read"), result.orElseThrow());
    }

    @Test
    void put_shouldSerializeValueWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.put("role-permission-codes", "20", Set.of("ticket:read"), 600);

        verify(valueOperations).set(
                "mailtrace:test:role-permission-codes:20",
                "[\"ticket:read\"]",
                Duration.ofSeconds(600)
        );
    }

    @Test
    void get_whenRedisFails_shouldReturnCacheMiss() {
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));

        Optional<Object> result = cacheService.get("role-permission-codes", "20", Set.class);

        assertTrue(result.isEmpty());
    }
}
