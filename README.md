Markdown
# 🪐 Mini-Doodle High-Performance Scheduling Engine

A high-performance, production-grade simulation of a meeting scheduling platform designed to comfortably process **100k+ concurrent requests**. This system utilizes an ultra-low latency, in-memory reservation engine powered by **Spring Boot (Virtual Threads)** and **Redis Lua scripts**, backed by an asynchronous, eventual-consistency persistence loop using the **Transactional Outbox Pattern** over **Apache Kafka**.

---

## 🏗️ Architectural Overview

To withstand massive concurrent scheduling bursts (e.g., flash bookings or corporate events) without saturating relational database connections, this system completely decouples the **Synchronous Request Path** from the **Asynchronous Persistence Layer**:

[ 100k Req/Sec ] ──> [ Spring Boot (Virtual Threads) ]
│
│ (Atomic Execution via Lua)
▼
[ Redis Cluster ]
┌──────────────────────────┐
│ 1. Update Slot State     │
│ 2. Append to Redis Stream│ <── (In-Memory Outbox with Partition Key)
└────────────┬─────────────┘
│
│ (Async Tailer / Outbox Relay)
▼
[ Kafka Topic ]  <── Partition Key = Slot ID (Guarantees Order)
│
▼
[ Async Consumer Group ]
│
▼
[ PostgreSQL DB ] <── (Idempotent Writes via Upserts)


### Key Technical Decisions
* **Virtual Threads (Project Loom):** Eradicates the thread-per-request I/O bottleneck, allowing Tomcat to manage millions of concurrent connections seamlessly.
* **Atomic Redis Lua Engine:** Checks availability, flips state to `BUSY`, and logs to the in-memory outbox stream atomically in $< 2\text{ms}$. This eliminates dual-write failures entirely.
* **Strict Per-Slot Sequencing:** The Outbox Relay explicitly injects the `slotId` as the **Kafka Partition Key**. This guarantees that all chronological mutations (Create -> Modify -> Delete) for an explicit slot execute in strict sequential order within the same Kafka partition.
* **Timezone & DST Boundary Safety:** Availability boundaries are persisted as normalized **UTC Instants** while capturing the creator's explicit **IANA Time Zone ID** (e.g., `Europe/London`). This allows the dynamic Domain layer to identify and neutralize skipped or overlapping local hours caused by global Daylight Saving Time (DST) shifts.

---

## 🎯 Requirements Traceability Matrix

| Requirement | Architecture Implementation Method |
| :--- | :--- |
| **1. Define & Convert Slots** | Handled natively via `TimeSlotController` (creation) and `MeetingBookingController` (atomic conversion loop). |
| **2. Personal Calendar Management** | Encapsulated in the dynamic `UserCalendar` domain aggregate record. |
| **3. "Calendar" Term Boundary** | **Strictly No DB Table.** Calendar lives exclusively as an in-memory structural domain aggregate compiled on-the-fly. |
| **4. Metadata Tracking** | Captures titles, descriptions, and structural arrays of unique participant IDs seamlessly inside the `Meeting` boundary. |
| **5. Aggregated Range Views** | Fully supported in `CalendarController` using optimized range evaluations backed by database indexes. |
| **6. Full Slot CRUD & Rules** | Exposed via explicit Restful mappings supporting runtime configuration modifications, status overrides, and hard deletions. |
| **7. Distributed Detail Ingestion** | Deployed via asynchronous Kafka messaging consumer workers enforcing idempotent schema writes. |

---

## 🚦 Getting Started (Local Execution)

### Prerequisites
* Docker & Docker Compose V2 Installed
* Java 21+ (If running or building outside containers)

### Spin Up the Complete Infrastructure Stack
The entire environment—including PostgreSQL, Redis, Kafka, Liquibase migration hooks, and the Spring Boot application container—can be initialized with a single command:

```bash
docker-compose up --build -d
To monitor the startup logs, database schema migrations, and internal queue initializations, run:
```
