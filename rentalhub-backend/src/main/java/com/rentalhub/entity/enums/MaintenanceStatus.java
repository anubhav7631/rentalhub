package com.rentalhub.entity.enums;

/**
 * MaintenanceStatus — the workflow state of a maintenance request.
 *
 * State machine (Phase 13):
 *   OPEN → IN_PROGRESS → COMPLETED
 *   OPEN → REJECTED
 *
 * Only the owner can move status forward. The tenant creates (OPEN)
 * and can view the status, but cannot change it.
 */
public enum MaintenanceStatus {
    OPEN,
    IN_PROGRESS,
    COMPLETED,
    REJECTED
}