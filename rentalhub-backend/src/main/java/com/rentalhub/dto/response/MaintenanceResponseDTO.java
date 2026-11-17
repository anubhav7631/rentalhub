package com.rentalhub.dto.response;

import com.rentalhub.entity.enums.MaintenanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String category;
    private MaintenanceStatus status;
    private String resolutionNotes;
    private LocalDateTime resolvedAt;

    // Property summary
    private Long propertyId;
    private String propertyName;
    private String propertyAddress;

    // Tenant summary
    private Long tenantId;
    private String tenantName;

    // Assigned to (optional)
    private Long assignedToId;
    private String assignedToName;

    // Images
    private List<String> imagePaths;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}