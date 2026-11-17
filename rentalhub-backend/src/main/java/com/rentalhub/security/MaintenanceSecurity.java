package com.rentalhub.security;

import com.rentalhub.entity.MaintenanceRequest;
import com.rentalhub.repository.MaintenanceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component("maintenanceSecurity")
@RequiredArgsConstructor
public class MaintenanceSecurity {

    private final MaintenanceRequestRepository maintenanceRequestRepository;

    @Transactional(readOnly = true)
    public boolean isOwner(Long requestId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return maintenanceRequestRepository.findById(requestId)
            .map(r -> r.getProperty().getOwner().getId().equals(principal.getId()))
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isTenant(Long requestId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return maintenanceRequestRepository.findById(requestId)
            .map(r -> r.getTenant().getTenantUser() != null
                && r.getTenant().getTenantUser().getId().equals(principal.getId()))
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isOwnerOrTenant(Long requestId, Authentication authentication) {
        return isOwner(requestId, authentication) || isTenant(requestId, authentication);
    }
}