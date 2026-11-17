package com.rentalhub.security;

import com.rentalhub.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("paymentSecurity")
@RequiredArgsConstructor
public class PaymentSecurity {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public boolean isOwner(Long paymentId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }

        return paymentRepository.findById(paymentId)
            .map(payment -> payment.getProperty().getOwner()
                .getId().equals(principal.getId()))
            .orElse(false);
    }

    /**
     * Tenant can view their OWN payment history.
     */
    @Transactional(readOnly = true)
    public boolean isTenantOrOwner(Long paymentId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }

        return paymentRepository.findById(paymentId)
            .map(payment -> {
                boolean isOwner = payment.getProperty().getOwner()
                    .getId().equals(principal.getId());
                boolean isTenant = payment.getTenant().getTenantUser() != null
                    && payment.getTenant().getTenantUser()
                       .getId().equals(principal.getId());
                return isOwner || isTenant;
            })
            .orElse(false);
    }
}