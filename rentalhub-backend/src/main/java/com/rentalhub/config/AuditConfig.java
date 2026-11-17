package com.rentalhub.config;

import com.rentalhub.entity.listener.AuditEntityListener;  // ← correct import
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AuditConfig {

    private final ApplicationContext applicationContext;

    @Bean
    public AuditEntityListener auditEntityListener() {
        AuditEntityListener listener = new AuditEntityListener();
        AuditEntityListener.setApplicationContext(applicationContext);
        return listener;
    }
}