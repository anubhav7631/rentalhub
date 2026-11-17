package com.rentalhub.entity.listener;

import com.rentalhub.entity.AuditLog;
import com.rentalhub.security.SecurityUtils;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class AuditEntityListener {

    private static ApplicationContext context;

    public static void setApplicationContext(ApplicationContext ctx) {
        context = ctx;
    }

    @PostPersist
    public void afterCreate(Object entity) {
        saveLog("CREATE", entity);
    }

    @PostUpdate
    public void afterUpdate(Object entity) {
        saveLog("UPDATE", entity);
    }

    @PostRemove
    public void afterDelete(Object entity) {
        saveLog("DELETE", entity);
    }

    private void saveLog(String action, Object entity) {
        if (context == null) return;

        // Don't audit the audit log itself — infinite loop
        if (entity instanceof AuditLog) return;

        try {
            com.rentalhub.repository.AuditLogRepository repo =
                context.getBean(com.rentalhub.repository.AuditLogRepository.class);

            String entityType = entity.getClass().getSimpleName();
            Long   entityId   = getEntityId(entity);
            Long   userId     = getCurrentUserId();

            AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .performedByUserId(userId)
                .performedAt(LocalDateTime.now())
                .build();

            repo.save(auditLog);

        } catch (Exception e) {
            // Audit failure must NEVER break the main operation
            log.warn("Audit log failed for {} on {}: {}",
                action, entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    private Long getEntityId(Object entity) {
        try {
            var method = entity.getClass().getMethod("getId");
            return (Long) method.invoke(entity);
        } catch (Exception e) {
            return null;
        }
    }

    private Long getCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            // Scheduled jobs run without a security context — return null
            return null;
        }
    }
}