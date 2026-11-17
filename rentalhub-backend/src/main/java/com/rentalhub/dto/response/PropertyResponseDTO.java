package com.rentalhub.dto.response;

import com.rentalhub.entity.enums.PropertyStatus;
import com.rentalhub.entity.enums.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyResponseDTO {
    private Long id;
    private String propertyName;
    private PropertyType propertyType;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal rentAmount;
    private BigDecimal depositAmount;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal areaSqft;
    private String description;
    private PropertyStatus status;
    private Long ownerId;
    private String ownerName;

    // Each image as its own DTO (id + path) — needed so the frontend
    // can call DELETE /api/properties/{id}/images/{imageId}
    private List<PropertyImageDTO> images;

    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyImageDTO {
        private Long id;
        private String filePath;
        private Boolean isPrimary;
    }
}