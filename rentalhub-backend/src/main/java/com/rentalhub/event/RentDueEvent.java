package com.rentalhub.event;

import com.rentalhub.entity.Payment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * RentDueEvent — published by the @Scheduled job when a payment
 * is approaching its due date (3 days before).
 */
@Getter
public class RentDueEvent extends ApplicationEvent {
    private final Payment payment;
    private final long daysUntilDue;

    public RentDueEvent(Object source, Payment payment, long daysUntilDue) {
        super(source);
        this.payment      = payment;
        this.daysUntilDue = daysUntilDue;
    }
}