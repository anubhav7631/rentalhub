package com.rentalhub.security;

import com.rentalhub.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("propertySecurity")
@RequiredArgsConstructor
@Slf4j
public class PropertySecurity {

    private final PropertyRepository propertyRepository;

    @Transactional(readOnly = true)
    public boolean isOwner(Long propertyId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            log.warn("PropertySecurity: principal is not UserPrincipal");
            return false;
        }

        return propertyRepository.findById(propertyId)
            .map(property -> {
                Long ownerId = property.getOwner().getId();
                boolean match = ownerId.equals(principal.getId());
                log.debug("PropertySecurity: propertyId={} ownerId={} currentUserId={} match={}",
                    propertyId, ownerId, principal.getId(), match);
                return match;
            })
            .orElseGet(() -> {
                log.warn("PropertySecurity: property {} not found", propertyId);
                return false;
            });
    }
}