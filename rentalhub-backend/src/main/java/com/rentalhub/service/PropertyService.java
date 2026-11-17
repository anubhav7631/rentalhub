package com.rentalhub.service;

import com.rentalhub.dto.request.PropertyRequestDTO;
import com.rentalhub.dto.response.PropertyResponseDTO;
import com.rentalhub.entity.Property;
import com.rentalhub.entity.PropertyImage;
import com.rentalhub.entity.User;
import com.rentalhub.entity.enums.PropertyStatus;
import com.rentalhub.exception.BadRequestException;
import com.rentalhub.exception.ResourceNotFoundException;
import com.rentalhub.repository.PropertyRepository;
import com.rentalhub.repository.UserRepository;
import com.rentalhub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PropertyService — business logic for the Property module.
 *
 * Key responsibilities:
 *   - CRUD operations scoped to the current owner (or all, for ADMIN)
 *   - Image upload orchestration (delegates I/O to FileStorageService)
 *   - Entity ↔ DTO mapping
 *   - Enforcing the "owner can only see/edit their own properties" rule
 *     at the service layer (in ADDITION to @PreAuthorize on the controller)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository     userRepository;
    private final FileStorageService fileStorageService;

   
    @Transactional
    public PropertyResponseDTO create(PropertyRequestDTO request, List<MultipartFile> images) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        User owner = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        Property property = Property.builder()
            .propertyName(request.getPropertyName())
            .propertyType(request.getPropertyType())
            .addressLine1(request.getAddressLine1())
            .addressLine2(request.getAddressLine2())
            .city(request.getCity())
            .state(request.getState())
            .pincode(request.getPincode())
            .rentAmount(request.getRentAmount())
            .depositAmount(request.getDepositAmount())
            .bedrooms(request.getBedrooms() != null ? request.getBedrooms() : 1)
            .bathrooms(request.getBathrooms() != null ? request.getBathrooms() : 1)
            .areaSqft(request.getAreaSqft())
            .description(request.getDescription())
            .status(PropertyStatus.AVAILABLE)
            .owner(owner)
            .build();

        Property saved = propertyRepository.save(property);

        // Process images AFTER the property has an ID (needed for the subdirectory path)
        if (images != null && !images.isEmpty()) {
            attachImages(saved, images);
            saved = propertyRepository.save(saved);
        }

        log.info("Property created: id={}, owner={}", saved.getId(), owner.getEmail());
        return toResponseDTO(saved);
    }

    
    @Transactional
    public PropertyResponseDTO update(Long id, PropertyRequestDTO request, List<MultipartFile> newImages) {
        Property property = propertyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property", id));

        property.setPropertyName(request.getPropertyName());
        property.setPropertyType(request.getPropertyType());
        property.setAddressLine1(request.getAddressLine1());
        property.setAddressLine2(request.getAddressLine2());
        property.setCity(request.getCity());
        property.setState(request.getState());
        property.setPincode(request.getPincode());
        property.setRentAmount(request.getRentAmount());
        property.setDepositAmount(request.getDepositAmount());
        if (request.getBedrooms() != null)  property.setBedrooms(request.getBedrooms());
        if (request.getBathrooms() != null) property.setBathrooms(request.getBathrooms());
        property.setAreaSqft(request.getAreaSqft());
        property.setDescription(request.getDescription());

        if (newImages != null && !newImages.isEmpty()) {
            attachImages(property, newImages);
        }

        Property saved = propertyRepository.save(property);
        log.info("Property updated: id={}", saved.getId());
        return toResponseDTO(saved);
    }

    /**
     * GET single property by id.
     * @Transactional(readOnly = true): required because toResponseDTO
     * accesses property.getImages() (a LAZY collection) — must happen
     * inside an open transaction/session.
     */
    @Transactional(readOnly = true)
    public PropertyResponseDTO getById(Long id) {
        Property property = propertyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property", id));
        return toResponseDTO(property);
    }

    /**
     * LIST properties — behavior depends on caller's role:
     *   ADMIN  → sees ALL properties (any owner)
     *   OWNER  → sees only THEIR properties
     *
     * keyword: optional search term (matches property name, city, address)
     *
     * This method demonstrates ROLE-AWARE SERVICE LOGIC — the same
     * endpoint behaves differently based on who's calling, without
     * needing two separate endpoints.
     */
    @Transactional(readOnly = true)
    public Page<PropertyResponseDTO> list(String keyword, Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasAuthority("ROLE_ADMIN");

        Page<Property> properties;
        if (isAdmin) {
            properties = (keyword == null || keyword.isBlank())
                ? propertyRepository.findAll(pageable)
                : propertyRepository.searchAll(keyword, pageable);
        } else {
            properties = (keyword == null || keyword.isBlank())
                ? propertyRepository.findByOwnerId(currentUserId, pageable)
                : propertyRepository.searchByOwner(currentUserId, keyword, pageable);
        }

        return properties.map(this::toResponseDTO);
    }

    
    @Transactional
    public void softDelete(Long id) {
        Property property = propertyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property", id));

        if (property.getStatus() == PropertyStatus.OCCUPIED) {
            throw new BadRequestException(
                "Cannot delete a property with an active tenant. " +
                "Please end the tenancy first."
            );
        }

        property.setStatus(PropertyStatus.INACTIVE);
        propertyRepository.save(property);
        log.info("Property soft-deleted (set INACTIVE): id={}", id);
    }

    /**
     * DELETE a single image from a property.
     * Removes both the DB record (via orphanRemoval) and the file on disk.
     */
    @Transactional
    public void deleteImage(Long propertyId, Long imageId) {
        Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));

        PropertyImage image = property.getImages().stream()
            .filter(img -> img.getId().equals(imageId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Image", imageId));

        // Remove from the collection — orphanRemoval=true (Phase 5) deletes the DB row
        property.getImages().remove(image);
        propertyRepository.save(property);

        // Remove the physical file
        fileStorageService.delete(image.getFilePath());

        log.info("Deleted image {} from property {}", imageId, propertyId);
    }

    
    @Transactional
    public void setPrimaryImage(Long propertyId, Long imageId) {
        Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));

        boolean found = false;
        for (PropertyImage img : property.getImages()) {
            boolean isThisOne = img.getId().equals(imageId);
            img.setIsPrimary(isThisOne);
            if (isThisOne) found = true;
        }

        if (!found) {
            throw new ResourceNotFoundException("Image", imageId);
        }

        propertyRepository.save(property);
    }

    // ── Private helpers ──────────────────────────────────────────

    
    private void attachImages(Property property, List<MultipartFile> images) {
        boolean hasPrimaryAlready = property.getImages().stream()
            .anyMatch(PropertyImage::getIsPrimary);

        String subDir = "properties/" + property.getId();

        for (int i = 0; i < images.size(); i++) {
            MultipartFile file = images.get(i);
            if (file.isEmpty()) continue;

            String storedPath = fileStorageService.store(file, subDir);

            PropertyImage image = PropertyImage.builder()
                .filePath(storedPath)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .isPrimary(!hasPrimaryAlready && i == 0) // first image of a fresh property
                .build();

            property.addImage(image); // sets both sides of the relationship (Phase 5)
        }
    }

    
    private PropertyResponseDTO toResponseDTO(Property property) {
        List<PropertyResponseDTO.PropertyImageDTO> imageDTOs = property.getImages().stream()
            .map(img -> PropertyResponseDTO.PropertyImageDTO.builder()
                .id(img.getId())
                .filePath(img.getFilePath())
                .isPrimary(img.getIsPrimary())
                .build())
            .collect(Collectors.toList());

        return PropertyResponseDTO.builder()
            .id(property.getId())
            .propertyName(property.getPropertyName())
            .propertyType(property.getPropertyType())
            .addressLine1(property.getAddressLine1())
            .addressLine2(property.getAddressLine2())
            .city(property.getCity())
            .state(property.getState())
            .pincode(property.getPincode())
            .rentAmount(property.getRentAmount())
            .depositAmount(property.getDepositAmount())
            .bedrooms(property.getBedrooms())
            .bathrooms(property.getBathrooms())
            .areaSqft(property.getAreaSqft())
            .description(property.getDescription())
            .status(property.getStatus())
            .ownerId(property.getOwner().getId())
            .ownerName(property.getOwner().getFullName())
            .images(imageDTOs)
            .createdAt(property.getCreatedAt())
            .build();
    }
}