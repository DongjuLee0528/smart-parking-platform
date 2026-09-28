package com.smartparking.audit.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;
    @Column(nullable = false, length = 100)
    private String action;
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    @Column(name = "before_json", columnDefinition = "text")
    private String beforeJson;
    @Column(name = "after_json", nullable = false, columnDefinition = "text")
    private String afterJson;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AuditLog(UUID actorId, String action, UUID entityId, String beforeJson, String afterJson) {
        this.actorId = actorId;
        this.action = action;
        this.entityType = "PARKING_LOT";
        this.entityId = entityId;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
        this.createdAt = Instant.now();
    }
}
