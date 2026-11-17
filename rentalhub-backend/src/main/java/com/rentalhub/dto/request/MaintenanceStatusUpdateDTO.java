package com.rentalhub.dto.request;

import com.rentalhub.entity.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MaintenanceStatusUpdateDTO — owner updates the request status.
 *
 * ONLY the owner can call the status-update endpoint.
 * Valid transitions are enforced in the service layer, not here.
 * This DTO just carries the desired new status + optional notes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private MaintenanceStatus status;

    // Resolution notes — optional except when COMPLETED or REJECTED
    private String resolutionNotes;

    // Who to assign the work to (optional — can be left null)
    private Long assignedToUserId;
}