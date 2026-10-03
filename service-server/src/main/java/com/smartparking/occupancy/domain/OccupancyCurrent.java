package com.smartparking.occupancy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "occupancy_current")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OccupancyCurrent {
    @Id @Column(name = "parking_space_id")
    private UUID parkingSpaceId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private OccupancyState state;
    @Column(nullable = false)
    private double confidence;
    @Column(name = "source_camera_id", nullable = false)
    private UUID sourceCameraId;
    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;
    @Version @Column(nullable = false)
    private Long version;

    public OccupancyCurrent(UUID parkingSpaceId, OccupancyState state, double confidence,
                            UUID sourceCameraId, Instant observedAt) {
        this.parkingSpaceId = parkingSpaceId;
        update(state, confidence, sourceCameraId, observedAt);
    }

    public void update(OccupancyState state, double confidence, UUID sourceCameraId, Instant observedAt) {
        this.state = state;
        this.confidence = confidence;
        this.sourceCameraId = sourceCameraId;
        this.observedAt = observedAt;
    }
}
