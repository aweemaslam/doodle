package com.doodle.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST API parameter contract enum used for filtering calendar availability views.
 * Intentionally segregated from core domain entities to keep query criteria separate from database state models.
 */
@Schema(description = "Filter token used to query calendar views by a specific availability category.")
public enum CustomSlotStatus {
    /**
     * Filter results to show only open, unbooked time slots.
     */
    FREE,

    /**
     * Filter results to show slots currently locked in an unconfirmed reservation state.
     */
    PENDING_RESERVATION,

    /**
     * Wildcard filter option indicating that all slots should be returned regardless of current state.
     */
    ALL,

    /**
     * Filter results to show slots that have been successfully finalized and booked.
     */
    RESERVED
}