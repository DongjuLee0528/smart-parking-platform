package com.smartparking.savedparkinglocation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "saved_parking_locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedParkingLocation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private UUID vehicleId;
    @Column(name = "parking_space_id", nullable = false, updatable = false)
    private UUID parkingSpaceId;
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "text", updatable = false)
    private String snapshotJson;
    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;
    @Column(name = "released_at")
    private Instant releasedAt;

    public SavedParkingLocation(UUID userId, UUID vehicleId, UUID parkingSpaceId, String snapshotJson) {
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.parkingSpaceId = parkingSpaceId;
        this.snapshotJson = snapshotJson;
        this.savedAt = Instant.now();
    }

    public void release() {
        if (releasedAt == null) {
            releasedAt = Instant.now();
        }
    }
}
