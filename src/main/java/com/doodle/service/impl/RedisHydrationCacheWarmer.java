package com.doodle.service.impl;

import com.doodle.enums.SlotStatus;
import com.doodle.model.TimeSlotEntity;
import com.doodle.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * High-performance Cache Warmer engine.
 * Synchronizes core operational database slot states into the Redis fast-path ring on startup.
 */
@Component
@ConditionalOnProperty(prefix = "app.redis", name = "hydrate-on-startup", havingValue = "true", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class RedisHydrationCacheWarmer {

    private final TimeSlotRepository timeSlotRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_KEY_PREFIX = "slot:state:";

    @EventListener(ApplicationReadyEvent.class)
    public void hydrateRedisCacheOnStartup() {
        log.info("Starting batch Redis state hydration from PostgreSQL master database...");

        // Fetch only active, future free slots to keep memory utilization optimized
        List<TimeSlotEntity> pendingHydrationSlots = timeSlotRepository
                .findByStatusAndEndTimeAfterAndActiveTrue(SlotStatus.FREE, Instant.now());

        if (pendingHydrationSlots.isEmpty()) {
            log.info("No pending active time slots require cache allocation. Hydration skipped.");
            return;
        }

        // Map collection into a localized Key-Value Map structure
        Map<String, String> bulkCacheMap = pendingHydrationSlots.stream()
                .collect(Collectors.toMap(
                        slot -> REDIS_KEY_PREFIX + slot.getId(),
                        slot -> SlotStatus.FREE.name(),
                        (existing, replacement) -> existing
                ));

        // Execute a single, atomic operation
        Boolean batchSuccess = redisTemplate.opsForValue().multiSetIfAbsent(bulkCacheMap);

        log.info("Redis cache hydration completed. Successfully seeded batch block state: {}", batchSuccess);
    }
}