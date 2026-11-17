package com.rentalhub.security;

import com.rentalhub.entity.Tenant;
import com.rentalhub.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("tenantSecurity")
@RequiredArgsConstructor
@Slf4j
public class TenantSecurity {

    private final TenantRepository tenantRepository;

    /**
     * @Transactional is CRITICAL here.
     *
     * Without it, tenant.getProperty() returns a LAZY proxy.
     * When we then call property.getOwner(), Hibernate tries to
     * initialize that proxy — but there's no active Session/transaction
     * at this point (we're inside Spring Security's filter evaluation,
     * not inside a service method) — so it throws
     * LazyInitializationException, which gets caught silently inside
     * the Optional chain and returns false → 403.
     *
     * Adding @Transactional opens a fresh transaction just for this
     * check, allowing Hibernate to fully load the property and its
     * owner before comparing IDs.
     */
    @Transactional(readOnly = true)
    public boolean isOwnerOfTenant(Long tenantId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            log.warn("TenantSecurity: principal is not UserPrincipal");
            return false;
        }

        return tenantRepository.findById(tenantId)
            .map(tenant -> {
                // Access property and owner INSIDE the transaction
                Long ownerId = tenant.getProperty().getOwner().getId();
                boolean match = ownerId.equals(principal.getId());
                log.debug("TenantSecurity: tenantId={} ownerId={} currentUserId={} match={}",
                    tenantId, ownerId, principal.getId(), match);
                return match;
            })
            .orElseGet(() -> {
                log.warn("TenantSecurity: tenant {} not found", tenantId);
                return false;
            });
    }
}