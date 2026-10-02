package com.smartparking.occupancy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "occupancy_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OccupancyHistory {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "parking_space_id", nullable = false)
    private UUID parkingSpaceId;
    @Enumerated(EnumType.STRING) @Column(name = "from_state", nullable = false, length = 20)
    private OccupancyState fromState;
    @Enumerated(EnumType.STRING) @Column(name = "to_state", nullable = false, length = 20)
    private OccupancyState toState;
    @Column(nullable = false)
    private double confidence;
    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    public OccupancyHistory(UUID parkingSpaceId, OccupancyState fromState, OccupancyState toState,
                            double confidence, Instant observedAt) {
        this.parkingSpaceId = parkingSpaceId;
        this.fromState = fromState;
        this.toState = toState;
        this.confidence = confidence;
        this.observedAt = observedAt;
    }
}
