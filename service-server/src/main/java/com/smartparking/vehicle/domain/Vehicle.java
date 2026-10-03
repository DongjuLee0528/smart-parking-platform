package com.smartparking.vehicle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vehicles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vehicle {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(name = "plate_number", nullable = false, length = 20)
    private String plateNumber;
    @Column(nullable = false, length = 100)
    private String nickname;
    @Column(name = "is_primary", nullable = false)
    private boolean primary;
    @Version @Column(nullable = false)
    private Long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Vehicle(UUID userId, String plateNumber, String nickname, boolean primary) {
        this.userId = userId;
        this.plateNumber = plateNumber;
        this.nickname = nickname;
        this.primary = primary;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void update(String plateNumber, String nickname, boolean primary) {
        this.plateNumber = plateNumber;
        this.nickname = nickname;
        this.primary = primary;
        this.updatedAt = Instant.now();
    }

    public void clearPrimary() {
        this.primary = false;
        this.updatedAt = Instant.now();
    }
}
