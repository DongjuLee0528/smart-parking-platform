package com.smartparking.savedparkinglocation.infrastructure;

import com.smartparking.savedparkinglocation.domain.SavedParkingLocation;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SavedParkingLocationRepository extends JpaRepository<SavedParkingLocation, UUID> {
    Optional<SavedParkingLocation> findByUserIdAndReleasedAtIsNull(UUID userId);
    Optional<SavedParkingLocation> findByIdAndUserId(UUID id, UUID userId);

    @Modifying(flushAutomatically = true)
    @Query("update SavedParkingLocation location set location.vehicleId = null, " +
        "location.releasedAt = coalesce(location.releasedAt, :releasedAt) " +
        "where location.userId = :userId and location.vehicleId = :vehicleId")
    int detachVehicle(UUID userId, UUID vehicleId, Instant releasedAt);
}
