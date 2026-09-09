package com.smartparking.audit.domain;

import java.time.Instant;

public abstract class AuditableEntity {
    protected Instant createdAt;
    protected Instant updatedAt;
}
