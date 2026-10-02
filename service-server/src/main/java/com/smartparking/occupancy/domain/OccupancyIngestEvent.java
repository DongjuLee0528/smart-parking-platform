package com.smartparking.occupancy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "occupancy_ingest_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OccupancyIngestEvent {
    @Id @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "camera_id", nullable = false)
    private UUID cameraId;
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    public OccupancyIngestEvent(UUID eventId, UUID cameraId, Instant capturedAt) {
        this.eventId = eventId;
        this.cameraId = cameraId;
        this.capturedAt = capturedAt;
    }
}
