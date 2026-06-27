-- liquibase formatted sql

-- changeset aweem:create-users-table splitStatements:false runOnChange:false
-- comment: Table for storing user details.
-- rollback DROP TABLE users;
CREATE TABLE users
(
    email            VARCHAR(50) NOT NULL,
    full_name        VARCHAR(255) NOT NULL,
    default_timezone VARCHAR(64)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ,
    is_active        BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT pk_users PRIMARY KEY (email)
);
CREATE INDEX idx_users_lookup ON users (email, is_active);

-- changeset aweem:create-time-slots-table splitStatements:false runOnChange:false
-- comment: Table tracking bookable time segments linked to an owner.
-- rollback DROP TABLE time_slots;
CREATE TABLE time_slots
(
    time_slot_id BIGSERIAL    NOT NULL,
    owner_id      VARCHAR(50) NOT NULL,
    start_time   TIMESTAMPTZ  NOT NULL,
    end_time     TIMESTAMPTZ  NOT NULL,
    timezone_id  VARCHAR(64)  NOT NULL,
    status       VARCHAR(32)  NOT NULL DEFAULT 'FREE',
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ,
    is_active    BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT pk_time_slots PRIMARY KEY (time_slot_id),
    CONSTRAINT fk_time_slots_owner FOREIGN KEY (owner_id) REFERENCES users (email)
);

CREATE INDEX idx_time_slots_owner ON time_slots (owner_id);
CREATE INDEX idx_time_slots_schedule ON time_slots (start_time, end_time, is_active);

-- changeset aweem:create-bookings-table splitStatements:false runOnChange:false
-- comment: Table maintaining booking metadata mapped to a unique time slot.
-- rollback DROP TABLE bookings;
CREATE TABLE bookings
(
    booking_id   BIGSERIAL    NOT NULL,
    time_slot_id BIGINT       NOT NULL,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ,
    is_active    BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT pk_bookings PRIMARY KEY (booking_id),
    CONSTRAINT fk_bookings_time_slot FOREIGN KEY (time_slot_id) REFERENCES time_slots (time_slot_id),
    CONSTRAINT uq_bookings_time_slot UNIQUE (time_slot_id)
);

-- changeset aweem:create-booking-participants-table splitStatements:false runOnChange:false
-- comment: Element collection table mapping many participants to a single booking context.
-- rollback DROP TABLE booking_participants;
CREATE TABLE booking_participants
(
    booking_id     BIGINT       NOT NULL,
    participant_id VARCHAR(255) NOT NULL,
    CONSTRAINT pk_booking_participants PRIMARY KEY (booking_id, participant_id),
    CONSTRAINT fk_booking_participants_booking FOREIGN KEY (booking_id) REFERENCES bookings (booking_id) ON DELETE CASCADE
);

CREATE INDEX idx_booking_participants_lookup ON booking_participants (booking_id);

-- changeset aweem:create-outbox-events-table splitStatements:false runOnChange:false
-- comment: Outbox pattern transaction table capturing events to distribute asynchronously.
-- rollback DROP TABLE outbox_events;
CREATE TABLE outbox_events
(
    outbox_event_id UUID         NOT NULL,
    aggregate_id    VARCHAR(255) NOT NULL,
    aggregate_type  VARCHAR(64)  NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    payload         JSONB        NOT NULL,
    processed       BOOLEAN      NOT NULL,
    retry_count     INTEGER      NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ,
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT pk_outbox_events PRIMARY KEY (outbox_event_id)
);

CREATE INDEX idx_outbox_events_aggregate ON outbox_events (aggregate_id);
CREATE INDEX idx_outbox_events_polling ON outbox_events (processed, created_at) WHERE processed = FALSE;