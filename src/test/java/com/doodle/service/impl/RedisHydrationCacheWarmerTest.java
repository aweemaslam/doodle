package com.doodle.service.impl;

import com.doodle.enums.SlotStatus;
import com.doodle.model.TimeSlotEntity;
import com.doodle.repository.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisHydrationCacheWarmerTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisHydrationCacheWarmer warmer;

    // ----------------------------
    // 1. HYDRATE REDIS WITH FREE SLOTS
    // ----------------------------
    @Test
    void shouldHydrateCacheWithFreeSlots() {

        TimeSlotEntity slot1 = new TimeSlotEntity();
        slot1.setId(1L);
        slot1.setStatus(SlotStatus.FREE);

        TimeSlotEntity slot2 = new TimeSlotEntity();
        slot2.setId(2L);
        slot2.setStatus(SlotStatus.FREE);

        when(timeSlotRepository.findByStatusAndEndTimeAfter(
                eq(SlotStatus.FREE),
                any(Instant.class)
        )).thenReturn(List.of(slot1, slot2));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        warmer.hydrateRedisCacheOnStartup();

        verify(valueOperations, times(2)).setIfAbsent(anyString(), eq("FREE"));
    }

    // ----------------------------
    // 2. EMPTY LIST SAFE (FIXED)
    // ----------------------------
    @Test
    void shouldHandleEmptySlotList() {

        when(timeSlotRepository.findByStatusAndEndTimeAfter(
                eq(SlotStatus.FREE),
                any(Instant.class)
        )).thenReturn(List.of());

        warmer.hydrateRedisCacheOnStartup();

        verify(valueOperations, never()).setIfAbsent(anyString(), anyString());
    }

    // ----------------------------
    // 3. REDIS KEY FORMAT
    // ----------------------------
    @Test
    void shouldUseCorrectRedisKeyFormat() {

        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setId(123L);
        slot.setStatus(SlotStatus.FREE);

        when(timeSlotRepository.findByStatusAndEndTimeAfter(
                eq(SlotStatus.FREE),
                any(Instant.class)
        )).thenReturn(List.of(slot));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        warmer.hydrateRedisCacheOnStartup();

        verify(valueOperations, times(1)).setIfAbsent("slot:state:123", "FREE");
    }

    // ----------------------------
    // 4. ONLY FREE SLOTS HYDRATED
    // ----------------------------
    @Test
    void shouldOnlyHydrateFreeSlots() {

        when(timeSlotRepository.findByStatusAndEndTimeAfter(
                eq(SlotStatus.FREE),
                any(Instant.class)
        )).thenReturn(List.of());

        //when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        warmer.hydrateRedisCacheOnStartup();

        verify(timeSlotRepository, never()).findByStatusAndEndTimeAfter(
                eq(SlotStatus.RESERVED),
                any(Instant.class)
        );
    }

    // ----------------------------
    // 5. HANDLE REDIS EXCEPTION
    // ----------------------------
    @Test
    void shouldHandleRedisExceptionGracefully() {

        TimeSlotEntity slot = new TimeSlotEntity();
        slot.setId(1L);
        slot.setStatus(SlotStatus.FREE);

        when(timeSlotRepository.findByStatusAndEndTimeAfter(
                eq(SlotStatus.FREE),
                any(Instant.class)
        )).thenReturn(List.of(slot));

        when(redisTemplate.opsForValue())
                .thenThrow(new RuntimeException("Redis connection failed"));

        assertThrows(RuntimeException.class,
                () -> warmer.hydrateRedisCacheOnStartup());
    }
}