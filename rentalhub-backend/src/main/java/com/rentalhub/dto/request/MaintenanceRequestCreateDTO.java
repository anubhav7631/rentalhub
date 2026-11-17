package com.rentalhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequestCreateDTO {

    @NotNull(message = "Property ID is required")
    private Long propertyId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be under 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    // e.g. "PLUMBING", "ELECTRICAL", "PAINTING", "AC", "OTHER"
    private String category;
}