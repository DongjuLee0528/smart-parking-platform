package com.smartparking.savedparkinglocation.infrastructure;

import com.smartparking.savedparkinglocation.domain.SavedParkingLocation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedParkingLocationRepository extends JpaRepository<SavedParkingLocation, UUID> {
    Optional<SavedParkingLocation> findByUserIdAndReleasedAtIsNull(UUID userId);
    Optional<SavedParkingLocation> findByIdAndUserId(UUID id, UUID userId);
}
