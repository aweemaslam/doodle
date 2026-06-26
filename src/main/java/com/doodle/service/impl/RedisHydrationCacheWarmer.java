package com.doodle.service.impl;


import com.doodle.enums.SlotStatus;
import com.doodle.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisHydrationCacheWarmer {

    private final TimeSlotRepository timeSlotRepository;
    private final StringRedisTemplate redisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void hydrateRedisCacheOnStartup() {
        log.info("Starting Redis state hydration from PostgreSQL database...");

        // Fetch only active/future free slots to preserve memory
        timeSlotRepository.findByStatusAndEndTimeAfter(SlotStatus.FREE, Instant.now())
            .forEach(slot -> {
                String redisKey = "slot:state:" + slot.getId();
                // Seed Redis state without overriding if it was modified concurrently
                redisTemplate.opsForValue().setIfAbsent(redisKey, SlotStatus.FREE.name());
            });

        log.info("Redis cache hydration successfully completed.");
    }
}