package com.rentalhub.event;

import com.rentalhub.entity.Payment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;


@Getter
public class PaymentPaidEvent extends ApplicationEvent {
    private final Payment payment;

    public PaymentPaidEvent(Object source, Payment payment) {
        super(source);
        this.payment = payment;
    }
}