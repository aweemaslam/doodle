# 🪐 Mini-Doodle Scheduling Engine — Repository Overview and How to Run

This repository is a compact, production-minded implementation of a meeting scheduling system (a "mini Doodle"). It demonstrates architecture patterns and engineering trade-offs used to build a low-latency scheduling platform that can handle large numbers of concurrent reservation attempts while ensuring eventual consistency in persistent storage.

This README provides a lead-level view of the project, including architecture, project structure, APIs, operational instructions (docker-compose), testing, metrics, and guidance for further work.

---

## Quick summary

- Language: Java
- Framework: Spring Boot (4.x)
- Persistence: PostgreSQL (liquibase migrations present)
- In-memory coordination: Redis
- Messaging / Outbox: Apache Kafka (Transactional Outbox pattern)
- Containerized development: Docker + docker-compose
- Tests: JUnit Jupiter, Mockito, Testcontainers (used in integration tests)

This project demonstrates the following capabilities:
- Slot CRUD (create, update, delete, change status)
- Convert free slots into meetings (reservation flow)
- Calendar aggregation API (domain-level calendar view)
- Redis-driven fast path for slot reservation and hydration on startup
- Kafka-based asynchronous outbox relay for durable persistence of events

---

## Table of contents

1. Project overview and design
2. Project structure
3. Key domain models & APIs (examples + payloads)
4. How to run locally (docker-compose)
5. Running tests
6. Observability and metrics (what to add)
7. Design notes & trade-offs
8. Next steps and extension ideas

---

## 1) Project overview and design

This implementation intentionally separates the fast, synchronous request path from the slower persistence path:

- Fast path: HTTP requests arrive and validations happen in Spring controllers/services. For reservation-critical operations the application uses Redis (and optionally Lua scripts) as an atomic, low-latency store to flip slot state and seed an in-memory outbox.
- Async path: A background relay/consumer reads the in-memory outbox (or a Redis stream) and publishes change events to Kafka. Kafka ensures ordered processing per-slot (slotId used as partition key). Consumers read Kafka and apply idempotent changes to PostgreSQL.

This reduces load on relational DB connections under high contention and provides well-defined ordering guarantees via Kafka partitions.

Important domain boundaries:
- Slot (TimeSlot) — represents an available time window belonging to a user (owner). Persisted to DB and seeded to Redis for fast checks.
- Meeting — created from a slot; contains title, description, list of participants, stored in DB.
- Calendar — domain aggregate built on-demand; the repository does not have a separate calendar table.

---

## 2) Project structure

Top-level (relevant files/directories):

- `build.gradle` — Gradle build with test and Testcontainers configuration
- `docker-compose.yml` — local infra (Postgres, Kafka, Redis) for running the service end-to-end
- `src/main/java/com/doodle/` — application code
  - `controller/` — REST controllers (TimeSlotController, MeetingBookingController, CalendarController)
  - `dto/` — request/response DTOs (SlotRequest, BulkSlotRequest, BookingRequest, TimeSlotResponse)
  - `enums/` — domain enums (SlotStatus, OutboxEventType, AggregateType)
  - `model/` — JPA entities (TimeSlotEntity, MeetingEntity, OutboxEventEntity, UserEntity, BaseEntity)
  - `repository/` — Spring Data JPA repositories
  - `service/` — service interfaces and implementations (TimeSlotServiceImpl, MeetingServiceImpl, OutboxEventService, RedisHydrationCacheWarmer)
  - `kafka/` — Kafka producer/consumer glue

- `src/main/resources/db/changelog` — Liquibase changelogs and SQL DDL used to create schema and seed data
- `src/test/java` — unit and integration tests (JUnit, Mockito, Testcontainers initializers)

Files you will likely inspect first:
- `src/main/java/com/doodle/controller/TimeSlotController.java`
- `src/main/java/com/doodle/controller/MeetingBookingController.java`
- `src/main/java/com/doodle/service/impl/TimeSlotServiceImpl.java`
- `src/main/resources/db/changelog/sql/01-create-schema-table.sql`

---

## 3) Key domain models & APIs

Primary REST endpoints (base path `/api/v1`):

- Time Slots
  - POST `/api/v1/slots` — Create a single slot
	- payload: `SlotRequest(ownerId, startTime, endTime, timezoneId)`
  - POST `/api/v1/slots/bulk` — Create multiple slots (bulk generator)
	- payload: `BulkSlotRequest(ownerId, startTime, endTime, numberOfSlots, timezoneId)`
  - PUT `/api/v1/slots/{id}` — Modify an existing slot
	- payload: `SlotRequest` (same shape)
  - PATCH `/api/v1/slots/{id}/status` — Change status (query param `status` e.g. `FREE`, `RESERVED`)
  - DELETE `/api/v1/slots/{id}` — Delete a slot

- Meeting / Booking
  - POST `/api/v1/slots/{id}/book` — Book a slot into a meeting
	- payload: `BookingRequest(title, description, participants (Set<String>), ownerId)`
	- returns `BookingResponse(status, message)`

- Calendar
  - GET `/api/v1/calendars/{ownerId}` — Aggregated calendar view for owner, supports `start`, `end`, `viewingTimeZone` query params

Notes about payloads:
- All times are represented as ISO-8601 Instants in UTC in the DTOs (e.g. `2024-01-01T10:00:00Z`).
- The DTOs are simple Java records where appropriate (see `src/main/java/com/doodle/dto`).

Example curl to create a slot:

```bash
curl -X POST http://localhost:8080/api/v1/slots \
  -H "Content-Type: application/json" \
  -d '{"ownerId":"alice@example.com","startTime":"2026-01-01T10:00:00Z","endTime":"2026-01-01T11:00:00Z","timezoneId":"UTC"}'
```

Example to book a slot:

```bash
curl -X POST http://localhost:8080/api/v1/slots/1/book \
  -H "Content-Type: application/json" \
  -d '{"title":"Team Sync","description":"Discuss Q2","participants":["bob@example.com"],"ownerId":"alice@example.com"}'
```

---

## 4) How to run locally (docker-compose)

Prerequisites:
- Docker & Docker Compose v2
- (Optional) Java 21+ if you want to run the app locally outside containers

Start everything locally (Postgres, Redis, Kafka, and the app using the included Dockerfile):

```bash
docker-compose up --build -d
```

Wait for containers to come up. Common logs to watch:

```bash
docker-compose logs -f app
docker-compose logs -f postgres
docker-compose logs -f kafka
docker-compose logs -f redis
```

Notes:
- Liquibase changelogs live under `src/main/resources/db/changelog`. The Docker image / Spring config runs Liquibase on startup to create the required schema and seed data.
- If you prefer a local JVM run for development (faster iterative code changes), run:

```bash
./gradlew bootRun
```

Environment / application properties used for containers are configured using Spring Boot properties inside `src/main/resources/application.yml` and `src/test/resources/application-test.yml`.

---

## 5) Running tests

Unit & integration test suites are included. To run the full test suite locally (requires Docker for Testcontainers):

```bash
./gradlew test --no-daemon
```

Notes:
- Some integration tests utilize Testcontainers and therefore require Docker running locally.
- Test reports are generated under `build/reports/tests/test/index.html`.

If you want to run a single test class:

```bash
./gradlew test --tests "com.doodle.service.impl.TimeSlotServiceImplTest"
```

---

## 6) Observability & metrics (what to add / where to look)

This repository includes places where metrics and monitoring hooks should be added:

- Instrument the fast-path Redis reservation with Micrometer timers and counters (success/fail, latency histogram).
- Instrument outbox relay throughput and error counters.
- Expose per-endpoint metrics via `/actuator/metrics` (Spring Boot Actuator). Consider adding Prometheus-exporter and Grafana dashboard.
- Add structured logs for important domain events (slot-created, slot-booked, outbox-emitted, outbox-applied) with correlation IDs.

---

## 7) Design notes, decisions & trade-offs

Why Redis + Kafka + Postgres?
- Redis: extremely low-latency atomic operations for contention hotspots (slot booking). It prevents DB connection storms during flash events.
- Kafka: provides durable, ordered event delivery for eventual consistent writes to DB and decouples persistence from the synchronous path.
- Postgres: the source-of-truth for historical data, queries, and analytics.

Trade-offs made in this sample:
- The current code demonstrates an outbox flow but does not include a full, production hardened distributed tracing and retry strategy — those would be next steps.
- Using Redis for primary reservation state requires careful monitoring of memory usage and eviction policies in production.

---

## 8) Next steps and extension ideas

If you want to expand this project to a production-grade service, recommended next steps:

1. Add resiliency patterns: retry with exponential backoff for the outbox relay and Kafka consumer.
2. Add distributed tracing (OpenTelemetry) to correlate HTTP requests → Redis → Kafka → DB.
3. Harden idempotency and upsert semantics for DB writes (use `ON CONFLICT` upserts and unique constraints)
4. Add authentication/authorization (JWT/OAuth2) and multi-tenant isolation if required.
5. Add rate-limiting and queuing fallback to avoid DOS during massive bursts.

---

## 9) Where to look in the codebase (guided tour)

- Controllers: `src/main/java/com/doodle/controller/*`
- Services: `src/main/java/com/doodle/service/impl/*`
- Entities: `src/main/java/com/doodle/model/*`
- Repositories: `src/main/java/com/doodle/repository/*`
- Kafka producer/consumer helpers: `src/main/java/com/doodle/kafka/*`
- Testcontainers initializers: `src/test/java/com/doodle/config/*`

---

If you'd like, I can:

- Clean up any remaining failing tests and convert selected tests to full integration tests using Testcontainers and docker-compose.
- Add a small Postman collection or OpenAPI examples (the app already includes springdoc OpenAPI starter).
- Provide a short design doc with sequence diagrams for the booking flow.

Tell me which of the above you'd like me to do next and I will implement it.


