package com.rentalhub.event;

import com.rentalhub.entity.Agreement;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AgreementExpiryEvent extends ApplicationEvent {
    private final Agreement agreement;
    private final long daysUntilExpiry;

    public AgreementExpiryEvent(Object source, Agreement agreement, long daysUntilExpiry) {
        super(source);
        this.agreement       = agreement;
        this.daysUntilExpiry = daysUntilExpiry;
    }
}