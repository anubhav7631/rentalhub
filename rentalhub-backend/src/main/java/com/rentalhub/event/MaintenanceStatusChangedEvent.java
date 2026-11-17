package com.rentalhub.event;

import com.rentalhub.entity.MaintenanceRequest;
import com.rentalhub.entity.enums.MaintenanceStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MaintenanceStatusChangedEvent extends ApplicationEvent {
    private final MaintenanceRequest request;
    private final MaintenanceStatus  previousStatus;
    private final MaintenanceStatus  newStatus;

    public MaintenanceStatusChangedEvent(
            Object source,
            MaintenanceRequest request,
            MaintenanceStatus previousStatus,
            MaintenanceStatus newStatus) {
        super(source);
        this.request        = request;
        this.previousStatus = previousStatus;
        this.newStatus      = newStatus;
    }
}