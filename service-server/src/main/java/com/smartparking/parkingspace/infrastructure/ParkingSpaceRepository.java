package com.smartparking.parkingspace.infrastructure;

import com.smartparking.parkingspace.domain.ParkingSpace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, UUID> {
    @EntityGraph(attributePaths = {"zone", "cameraMappings"})
    List<ParkingSpace> findByZoneFloorIdOrderBySpaceNumberAsc(UUID floorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select space from ParkingSpace space where space.id = :id")
    Optional<ParkingSpace> lockById(UUID id);
}
