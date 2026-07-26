package com.alex.foodfloworders.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyCache {

    private final StringRedisTemplate redis;

    public boolean wasProcessed(String consumer, UUID eventId) {
        String key = "idempotency:" + consumer + ":" + eventId;
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    public void remember(String consumer, UUID eventId) {
        String key = "idempotency:" + consumer + ":" + eventId;
        redis.opsForValue().set(key, "DONE", Duration.ofDays(30));
    }
}