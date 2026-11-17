package com.rentalhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalhub.dto.request.PropertyRequestDTO;
import com.rentalhub.dto.response.PropertyResponseDTO;
import com.rentalhub.exception.BadRequestException;
import com.rentalhub.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/properties")
@Slf4j
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;
    private final ObjectMapper     objectMapper;

    
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<PropertyResponseDTO> create(
            @RequestPart("property") String propertyJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {

        PropertyRequestDTO request = parseAndValidate(propertyJson);
        log.info("Creating property: {}", request.getPropertyName());

        PropertyResponseDTO created = propertyService.create(request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping
    public ResponseEntity<Page<PropertyResponseDTO>> list(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 12, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(propertyService.list(keyword, pageable));
    }

    
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @propertySecurity.isOwner(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(propertyService.getById(id));
    }

    
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @propertySecurity.isOwner(#id, authentication)")
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<PropertyResponseDTO> update(
            @PathVariable Long id,
            @RequestPart("property") String propertyJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {

        PropertyRequestDTO request = parseAndValidate(propertyJson);
        PropertyResponseDTO updated = propertyService.update(id, request, images);
        return ResponseEntity.ok(updated);
    }

    
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @propertySecurity.isOwner(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        propertyService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/properties/{propertyId}/images/{imageId}
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @propertySecurity.isOwner(#propertyId, authentication)")
    @DeleteMapping("/{propertyId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long propertyId,
            @PathVariable Long imageId) {
        propertyService.deleteImage(propertyId, imageId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/properties/{propertyId}/images/{imageId}/primary
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @propertySecurity.isOwner(#propertyId, authentication)")
    @PatchMapping("/{propertyId}/images/{imageId}/primary")
    public ResponseEntity<Void> setPrimaryImage(
            @PathVariable Long propertyId,
            @PathVariable Long imageId) {
        propertyService.setPrimaryImage(propertyId, imageId);
        return ResponseEntity.ok().build();
    }

    
    private PropertyRequestDTO parseAndValidate(String json) throws IOException {
        PropertyRequestDTO dto = objectMapper.readValue(json, PropertyRequestDTO.class);

        var validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
            throw new BadRequestException(message);
        }

        return dto;
    }
}