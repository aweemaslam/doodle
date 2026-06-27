 🗓️ Doodle API – High-Performance Distributed Meeting Scheduling Engine
------------------------------
## 📌 Overview
This project is a production-grade Spring Boot microservice that implements a complete, highly available Meeting Scheduling Platform with time slot management, distributed calendars, dynamic time zone translation, Kafka event streaming, Redis caching, and distributed scheduling.
The current implementation intentionally remains a modular monolith to keep operational complexity low while preserving clear service boundaries for future extraction into independent microservices.
The implementation goes beyond a standard CRUD service by incorporating modern backend engineering practices, including microservice-friendly architecture patterns such as:

* 🧾 Time Slot Lifecycle Management (Create → Soft Delete → Modify → Transition)
* 🧠 Instant Concurrency Isolation using Redis + atomic Scripting semantics
* 📬 Transactional Outbox Pattern for reliable asynchronous event publishing
* ⚡ Kafka-based event-driven communication
* 🔐 State transitions (FREE → PENDING_RESERVATION → RESERVED)
* 🔁 Distributed scheduling with ShedLock to support horizontal scaling
* 🐘 PostgreSQL + Liquibase for reliable schema change tracking
* 🚀 Dockerized full-stack infrastructure environment

The system is designed to simulate a real-world scalable meeting orchestration platform capable of handling extreme concurrency.
------------------------------
## 🎯 Implemented User Stories

* ✅ As a calendar owner, I can create individual or continuous bulk availability time slots.
* ✅ As a calendar owner, I can modify, transition, or soft-delete my time slots.
* ✅ As a customer, I can view an aggregated, paginated calendar shifted dynamically into my local time zone.
* ✅ As a customer, I can convert an available free slot into a formalized booking with participants.
* ✅ As a system, high-speed atomic state checks lock slots instantly in memory to prevent double-booking.
* ✅ As a system, a transactional outbox table logs state modifications atomically inside business boundaries.
* ✅ As a system, background worker relays poll and stream event payloads asynchronously over Kafka.

------------------------------

## 🔷 N-Layered Architecture

* Controller → Service → Helper Utility → Repository → Entity

## 🔷 Event-Driven Architecture

* Kafka for asynchronous horizontal communication.
* Transactional Outbox Pattern for reliable message publication without database connection starvation.

## 🔷 Key Design Principles

* SOLID principles and Clean Code practices.
* Java 26 Virtual Threads (spring.threads.virtual.enabled: true) to maximize I/O concurrency.
* Strict separation of concerns (Presentation Enums vs. Core Database State Enums).
* Multi-zone chronology checking to prevent Daylight Saving Time (DST) gap collision vulnerabilities.

------------------------------
## ⚙️ Tech Stack

* Java 26
* Spring Boot 4.1.0
* Spring Data JPA
* PostgreSQL 16
* Redis 7.2 (Operational Fast-Path State Cache)
* Apache Kafka (Confluent Platform 7.5.0)
* ShedLock (JDBC Template Lock Provider)
* Liquibase
* Docker & Docker Compose
* Swagger / OpenAPI (SpringDoc WebMvc)
* Jakarta Bean Validation
* Lombok

------------------------------
## 🚀 Features & Enhancements## 📦 Time Slot & Meeting Management

* Create singular availability slots or bulk-divide a macro timeline into equal segments.
* Universal pageable lookup window aggregation query pipelines.
* Soft deletion support to preserve historical tracking records across related booking tables.

------------------------------
## ⚡ Redis Concurrency Isolation

* Instant, fast-path lock allocation using single-threaded atomic LUA script logic checks.
* Eliminates heavy PostgreSQL row-lock contention under massive concurrent booking spikes.
* Dual-write data alignment guaranteed using automatic compensating Redis rollbacks if database commits fail.

------------------------------
## 📡 Event-Driven Architecture

* Asynchronous background event publication completely decoupled from active database connection threads.
* Implements reliable "at-least-once" messaging mechanics using transaction template chunk processing.

Supported events:

* PENDING_RESERVATION_EVENT
* RESERVED_EVENT

------------------------------
## ⏱️ Scheduler + ShedLock

* Outbox event publisher background cron task running every 20 milliseconds.
* Distributed-safe task orchestration guarded by ShedLock to prevent duplicate message emissions across expanded cluster instances.

------------------------------
## ⚡ Smart Cache Warmup

* Dynamic RedisHydrationCacheWarmer triggers on ApplicationReadyEvent app launch phases.
* Hydrates only active upcoming free slots, keeping memory utilization strictly optimized.

------------------------------
## ❗ Centralized Exception Handling

* Unified diagnostic layout structures returned cleanly via an integrated @RestControllerAdvice controller layer interceptor.
* Guards system privacy by automatically logging raw server crashes internally while returning safe error tokens.

------------------------------
## 🗄️ Database Strategy
The application uses:

* PostgreSQL for immutable persistent storage.
* Redis for transient fast-path state validation.

## Initialization & Indexing Strategy
To handle high-throughput query scans efficiently without causing connection pool latency degradation:

1. Active upcoming slots are batched cleanly into Redis memory caches upon context boot up.
2. Initial booking handshakes check and set states inside Redis memory in microseconds.
3. The database layer utilizes optimized partial and composite indexing structures:
* idx_users_lookup ON users (email, is_active) -> Enables index-only authentication scans.
    * idx_time_slots_schedule ON time_slots (start_time, end_time, is_active) -> Accelerates range queries.
    * idx_outbox_events_polling ON outbox_events (processed, created_at) WHERE processed = FALSE -> Provides instant, zero-overhead partial index sweeps for uncompleted events.

------------------------------
## 🔄 Scheduler
## 📡 Outbox Publisher Job
Polls and relays uncompleted outbox log lines to the Kafka broker cluster asynchronously.

@Scheduled(fixedDelayString = "${app.jobs.outbox-publish-delay-ms:20}")
@SchedulerLock(name = "outboxPublisherJob", lockAtLeastFor = "PT2S", lockAtMostFor = "PT20S")public void publish() {
// Isolates DB transaction chunk reads from Kafka Network I/O
}

------------------------------

## 📘 API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```
![swagger.png](src/main/resources/documentation/swagger.png)
------------------------------
## 📡 API Endpoints##
### REST Endpoints

| Method | Endpoint | Description |
|---------|----------|-------------|
| **POST** | `/api/v1/slots` | Create a single availability slot |
| **POST** | `/api/v1/slots/bulk` | Generate multiple consecutive availability slots |
| **GET** | `/api/v1/slots/{ownerId}` | Retrieve paginated calendar availability |
| **PUT** | `/api/v1/slots/{id}` | Update an existing time slot |
| **PATCH** | `/api/v1/slots/{id}/status` | Update slot status |
| **DELETE** | `/api/v1/slots/{id}` | Soft delete a slot |
| **POST** | `/api/v1/slots/{id}/book` | Book an available slot |

---

### Create a Time Slot

```bash
curl -X POST "http://localhost:8080/api/v1/slots" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "ownerId": "alice.smith@example.com",
    "startTime": "2026-08-29T14:00:00Z",
    "endTime": "2026-08-29T15:00:00Z",
    "timezoneId": "Asia/Karachi"
}'
```

---

### Bulk Create Time Slots

```bash
curl -X POST "http://localhost:8080/api/v1/slots/bulk" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "ownerId": "alice.smith@example.com",
    "startTime": "2026-07-28T17:19:49.565Z",
    "endTime": "2026-07-29T17:19:49.565Z",
    "numberOfSlots": 10,
    "timezoneId": "Asia/Karachi"
}'
```

---

### Retrieve Calendar Availability

With status filter:

```bash
curl -X GET "http://localhost:8080/api/v1/slots/alice.smith@example.com?start=2026-05-28T17:19:49.565Z&end=2026-08-28T17:19:49.565Z&viewingTimeZone=Europe/Berlin&status=RESERVED&page=0&size=20" \
  -H "Accept: application/json"
```

Without status filter:

```bash
curl -X GET "http://localhost:8080/api/v1/slots/alice.smith@example.com?start=2026-05-28T17:19:49.565Z&end=2026-08-28T17:19:49.565Z&viewingTimeZone=Europe/Berlin&page=0&size=20" \
  -H "Accept: application/json"
```

---

### Update a Time Slot

```bash
curl -X PUT "http://localhost:8080/api/v1/slots/2" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "ownerId": "alice.smith@example.com",
    "startTime": "2026-06-28T19:00:00Z",
    "endTime": "2026-06-28T20:00:00Z",
    "timezoneId": "Asia/Karachi"
}'
```

---

### Update Slot Status

Supported values:

- `FREE`
- `RESERVED`
- `NOT_AVAILABLE`

Example:

```bash
curl -X PATCH "http://localhost:8080/api/v1/slots/2/status?status=NOT_AVAILABLE" \
  -H "Accept: application/json"
```

---

### Delete a Time Slot

```bash
curl -X DELETE "http://localhost:8080/api/v1/slots/10" \
  -H "Accept: application/json"
```

---

### Book a Time Slot

```bash
curl -X POST "http://localhost:8080/api/v1/slots/5/book" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "title": "Project Planning Meeting",
    "description": "Sprint Planning Discussion",
    "participants": [
      "john@example.com",
      "jane@example.com"
    ],
    "ownerId": "alice.smith@example.com"
}'
```
------------------------------
## ▶️ Running the Application
## 🐳 Run Full Stack Using Docker Compose

1. Initialize and deploy the entire multi-container stack in detached mode:

``` docker compose up --build -d ```


## Services Included

* postgres-db -> Core persistent storage engine running on port 5432.
* redis-cache -> Fast-path atomic state caching pool running on port 6379.
* zookeeper -> Broker cluster coordination routing node.
* kafka -> High-throughput message stream broker cluster running on port 9092 (internal) and 29092 (host interaction mapping).
* doodle-app -> The main microservice core runtime context running on port 8080.

------------------------------
## 🧪 Testing
The codebase is protected via a thorough testing strategy spanning multiple architectural layers:

* Unit Testing (JUnit 5 + Mockito): Enforces business logic safety, timezone boundary conversions, and strict record layout validations.
* Slice Testing (@WebMvcTest): Asserts input payload validation rules, structural JSON sanitization behaviors, and centralized exception advice mapping filters at the HTTP gate boundary.
* Integration Testing (Testcontainers): Instantiates real, cloud-native Docker container environments for PostgreSQL, Redis, and Kafka to execute multi-threaded race condition validations. Using a CountDownLatch pool, tests fire 100 concurrent booking threads attacking a single slot simultaneously to guarantee our Redis validation logic successfully allows exactly one write while safely returning 409 conflict responses to the remaining 99 requests without transaction leakage.

------------------------------
## 📂 Project Structure
````
com.doodle
├── config              # Global framework beans, OpenApi, and Redis/Kafka serialization mappings
├── controller          # Pure HTTP REST traffic API entry routers
├── dto                 # Immutable Java Record contract transmission models
├── enums               # Segregated presentation filters and persistence states
├── exception           # Unchecked domain exception types and global response handlers
├── kafka
│   ├── consumer        # Transaction-aware cluster message listeners
│   ├── producer        # Deterministic key-routing partition event publishers
│   └── model           # Immutable event envelope record frames
├── model               # Auditable, soft-deletable Hibernate persistence entities
├── repository          # High-performance index-aligned Spring Data query abstractions
└── service
    ├── impl            # Core business domain implementations and state machines
    └── helper          # Common date chronology check utilities and safe lag buffers
````
------------------------------
## ⚖️ Trade-offs & Design Decisions
## ✅ Prioritization Decisions

* Transactional Outbox Over Live In-Flight Emits: We prioritized message delivery safety by using an outbox pattern table log. This ensures event emissions are atomic with database writes, protecting the system from split-brain data sync errors if network connections drop.
* Redis Atomic Pre-Checks Over Relational Block Locks: We prioritized overall throughput under extreme concurrent user booking actions. Executing state lock assignments inside Redis memory first shields the main PostgreSQL tablespace from heavy row-level lock wait loops, avoiding database transaction pool starvation.
* Segregated Presentation Filters (CustomSlotStatus): We purposefully isolated frontend filtering wildcards (like ALL) away from core column enum tables (SlotStatus). This protects database model cleanliness and allows database states to evolve independently without breaking legacy frontend client maps.


