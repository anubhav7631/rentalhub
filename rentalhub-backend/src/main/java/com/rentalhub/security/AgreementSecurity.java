package com.rentalhub.security;

import com.rentalhub.repository.AgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AgreementSecurity — ownership check for Agreement resources.
 * An agreement belongs to a property which belongs to an owner.
 */
@Component("agreementSecurity")
@RequiredArgsConstructor
public class AgreementSecurity {

    private final AgreementRepository agreementRepository;

    @Transactional(readOnly = true)
    public boolean isOwner(Long agreementId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }

        return agreementRepository.findById(agreementId)
            .map(agreement -> {
                Long ownerId = agreement.getProperty().getOwner().getId();
                return ownerId.equals(principal.getId());
            })
            .orElse(false);
    }

    /**
     * isTenantOrOwner — tenant can download THEIR OWN agreement.
     * Owner can access agreements for their properties.
     */
    @Transactional(readOnly = true)
    public boolean isTenantOrOwner(Long agreementId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }

        return agreementRepository.findById(agreementId)
            .map(agreement -> {
                // Check if owner
                boolean isOwner = agreement.getProperty().getOwner()
                    .getId().equals(principal.getId());

                // Check if the tenant linked to this agreement
                boolean isTenant = agreement.getTenant().getTenantUser() != null
                    && agreement.getTenant().getTenantUser()
                       .getId().equals(principal.getId());

                return isOwner || isTenant;
            })
            .orElse(false);
    }
}