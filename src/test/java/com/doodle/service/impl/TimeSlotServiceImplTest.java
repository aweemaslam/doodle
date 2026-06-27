package com.doodle.service.impl;

import com.doodle.dto.SlotRequest;
import com.doodle.dto.TimeSlotResponse;
import com.doodle.enums.SlotStatus;
import com.doodle.exception.ResourceNotFoundException;
import com.doodle.exception.SlotConflictException;
import com.doodle.model.TimeSlotEntity;
import com.doodle.model.UserEntity;
import com.doodle.repository.TimeSlotRepository;
import com.doodle.repository.UserRepository;
import com.doodle.service.helper.TimeSlotHelperService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceImplTest {

	@Mock
	private TimeSlotHelperService helper;

	@Mock
	private TimeSlotRepository repository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private TimeSlotServiceImpl service;

	// ----------------------------
	// 1. CREATE SLOT SUCCESS
	// ----------------------------
	@Test
	void shouldCreateSlotSuccessfully() {
		SlotRequest request = mock(SlotRequest.class);

		UserEntity user = new UserEntity();
		user.setEmail("test@test.com");

		when(request.ownerId()).thenReturn("test@test.com");
		when(request.startTime()).thenReturn(Instant.now());
		when(request.endTime()).thenReturn(Instant.now().plusSeconds(3600));
		when(request.timezoneId()).thenReturn("UTC");

		when(userRepository.findByEmailAndActiveTrue("test@test.com")).thenReturn(Optional.of(user));

		when(repository.existsOverlappingSlot(anyString(), any(), any()))
				.thenReturn(false);

		TimeSlotEntity saved = new TimeSlotEntity();
		saved.setId(1L);

		when(repository.save(any())).thenReturn(saved);
		when(helper.mapToResponse(any())).thenReturn(mock(TimeSlotResponse.class));

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		TimeSlotResponse response = service.createSlot(request);

		assertNotNull(response);
		verify(repository).save(any());
		verify(redisTemplate).opsForValue();
	}

	// ----------------------------
	// 2. SLOT OVERLAP FAIL
	// ----------------------------
	@Test
	void shouldThrowWhenSlotOverlaps() {
		SlotRequest request = mock(SlotRequest.class);

		when(request.ownerId()).thenReturn("test@test.com");
		when(request.startTime()).thenReturn(Instant.now());
		when(request.endTime()).thenReturn(Instant.now().plusSeconds(3600));
		when(request.timezoneId()).thenReturn("UTC");

		UserEntity user = new UserEntity();
		when(userRepository.findByEmailAndActiveTrue("test@test.com")).thenReturn(Optional.of(user));

		when(repository.existsOverlappingSlot(any(), any(), any()))
				.thenReturn(true);

		assertThrows(SlotConflictException.class,
				() -> service.createSlot(request));
	}

	// ----------------------------
	// 3. USER NOT FOUND
	// ----------------------------
	@Test
	void shouldThrowWhenUserNotFound() {
		SlotRequest request = mock(SlotRequest.class);

		when(request.ownerId()).thenReturn("missing");
		when(userRepository.findByEmailAndActiveTrue("missing")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> service.createSlot(request));
	}

	// ----------------------------
	// 4. MODIFY SLOT SUCCESS
	// ----------------------------
	@Test
	void shouldModifySlotSuccessfully() {
		SlotRequest request = mock(SlotRequest.class);

		TimeSlotEntity slot = new TimeSlotEntity();
		UserEntity owner = new UserEntity();
		owner.setEmail("email@test.com");
		slot.setOwner(owner);
		slot.setStatus(SlotStatus.FREE);

		when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(slot));

		when(repository.existsOverlappingSlotExcludingId(
				anyString(), any(), any(), anyLong()
		)).thenReturn(false);

		when(request.startTime()).thenReturn(Instant.now());
		when(request.endTime()).thenReturn(Instant.now().plusSeconds(3600));
		when(request.timezoneId()).thenReturn("UTC");

		when(repository.save(any())).thenReturn(slot);
		when(helper.mapToResponse(any())).thenReturn(mock(TimeSlotResponse.class));

		TimeSlotResponse response = service.modifySlot(1L, request);

		assertNotNull(response);
		verify(repository).save(any());
	}

	// ----------------------------
	// 5. MODIFY BLOCKED IF NOT FREE
	// ----------------------------
	@Test
	void shouldBlockModifyIfSlotNotFree() {
		UserEntity user = new UserEntity();
		user.setEmail("test@test.com");
		SlotRequest request = mock(SlotRequest.class);

		TimeSlotEntity slot = new TimeSlotEntity();
		slot.setStatus(SlotStatus.RESERVED);
		slot.setOwner(user);
		when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(slot));
		when(repository.existsOverlappingSlotExcludingId(
				any(), any(), any(), anyLong()
		)).thenReturn(true);
		assertThrows(SlotConflictException.class,
				() -> service.modifySlot(1L, request));
	}

	// ----------------------------
	// 6. DELETE SLOT SUCCESS
	// ----------------------------
	@Test
	void shouldDeleteSlotSuccessfully() {
		when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(new TimeSlotEntity()));

		service.deleteSlot(1L);

        assertFalse(repository.existsByIdAndActiveTrue(1L));
	}

	// ----------------------------
	// 7. DELETE SLOT NOT FOUND
	// ----------------------------
	@Test
	void shouldThrowWhenDeletingMissingSlot() {
		when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> service.deleteSlot(1L));
	}

	// ----------------------------
	// 8. CHANGE STATUS SUCCESS
	// ----------------------------
	@Test
	void shouldChangeStatusSuccessfully() {
		TimeSlotEntity slot = new TimeSlotEntity();
		when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(slot));

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		service.changeStatus(1L, SlotStatus.PENDING_RESERVATION);

		assertEquals(SlotStatus.PENDING_RESERVATION, slot.getStatus());
		verify(repository).save(slot);
		verify(redisTemplate).opsForValue();
	}
}