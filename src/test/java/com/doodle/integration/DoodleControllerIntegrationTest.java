package com.doodle.integration;

import com.doodle.DoodleApplication;
import com.doodle.dto.BookingRequest;
import com.doodle.dto.BulkSlotRequest;
import com.doodle.dto.SlotRequest;
import com.doodle.model.UserEntity;
import com.doodle.repository.TimeSlotRepository;
import com.doodle.repository.UserRepository;
import com.doodle.service.scheduler.OutboxPublisherJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = DoodleApplication.class
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DoodleControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @MockitoBean
    OutboxPublisherJob outboxPublisherJob;

    private Instant start;
    private Instant end;

    @BeforeEach
    void setUp() {
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        start = baseTime.plus(Duration.ofHours(1));
        end = baseTime.plus(Duration.ofHours(2));

        userRepository.deleteAll();
        UserEntity user = new UserEntity();
        user.setEmail("alice.smith@example.com");
        user.setFullName("Alice Smith");
        user.setDefaultTimezone("Europe/Berlin");

        userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        timeSlotRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Order(1)
    void shouldCreateSlotEndToEnd() throws Exception {

        SlotRequest request = new SlotRequest(
                "alice.smith@example.com",
                start,
                end,
                "Asia/Karachi"
        );

        mockMvc.perform(post("/api/v1/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value("alice.smith@example.com"))
                .andExpect(jsonPath("$.status").value("FREE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @Order(2)
    void shouldModifySlotEndToEnd() throws Exception {

        SlotRequest request = new SlotRequest(
                "alice.smith@example.com",
                start,
                end,
                "Asia/Karachi"
        );

        String response = mockMvc.perform(post("/api/v1/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value("alice.smith@example.com"))
                .andExpect(jsonPath("$.status").value("FREE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long slotId = mapper.readTree(response).get("id").asLong();

        request = new SlotRequest(
                "alice.smith@example.com",
                start,
                end,
                "Asia/Karachi"
        );

        mockMvc.perform(put("/api/v1/slots/{id}", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FREE"));
    }

    @Test
    @Order(3)
    void shouldChangeSlotStatusEndToEnd() throws Exception {

        SlotRequest request = new SlotRequest(
                "alice.smith@example.com",
                start,
                end,
                "Asia/Karachi"
        );

        String response = mockMvc.perform(post("/api/v1/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value("alice.smith@example.com"))
                .andExpect(jsonPath("$.status").value("FREE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long slotId = mapper.readTree(response).get("id").asLong();

        mockMvc.perform(
                        patch("/api/v1/slots/{id}/status", slotId)
                                .param("status", "NOT_AVAILABLE"))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(4)
    void shouldFetchCalendarEndToEnd() throws Exception {

        mockMvc.perform(get("/api/v1/slots/alice.smith@example.com")
                        .param("start", start.toString())
                        .param("end", end.toString())
                        .param("viewingTimeZone", "Europe/Berlin")
                        .param("status", "ALL")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(5)
    void shouldBookSlotEndToEnd() throws Exception {

        SlotRequest slotRequest = new SlotRequest(
                "alice.smith@example.com",
                start,
                end,
                "Asia/Karachi"
        );

        String response = mockMvc.perform(post("/api/v1/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(slotRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value("alice.smith@example.com"))
                .andExpect(jsonPath("$.status").value("FREE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long slotId = mapper.readTree(response).get("id").asLong();

        BookingRequest request = new BookingRequest(
                "Architecture Meeting",
                "Planning Session",
                Set.of("john@example.com"),
                "alice.smith@example.com"
        );

        mockMvc.perform(post("/api/v1/slots/{id}/book", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_RESERVATION"));
    }

    @Test
    @Order(6)
    void shouldDeleteSlotEndToEnd() throws Exception {
        SlotRequest slotRequest = new SlotRequest(
                "alice.smith@example.com",
                start,
                end,
                "Asia/Karachi"
        );

        String response = mockMvc.perform(post("/api/v1/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(slotRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value("alice.smith@example.com"))
                .andExpect(jsonPath("$.status").value("FREE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long slotId = mapper.readTree(response).get("id").asLong();
        mockMvc.perform(delete("/api/v1/slots/{id}", slotId))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(7)
    void shouldCreateBulkSlotsEndToEnd() throws Exception {

        BulkSlotRequest request = new BulkSlotRequest(
                "alice.smith@example.com",
                start,
                end,
                5,
                "Europe/Berlin"
        );

        mockMvc.perform(post("/api/v1/slots/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    @Order(8)
    void shouldReturnBadRequestForInvalidInput() throws Exception {

        String invalidJson = """
                {
                  "ownerId":"",
                  "timezoneId":""
                }
                """;

        mockMvc.perform(post("/api/v1/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

}