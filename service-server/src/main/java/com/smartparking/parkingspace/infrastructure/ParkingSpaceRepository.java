package com.smartparking.parkingspace.infrastructure;

import com.smartparking.parkingspace.domain.ParkingSpace;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, UUID> {
    @EntityGraph(attributePaths = {"zone", "cameraMappings"})
    List<ParkingSpace> findByZoneFloorIdOrderBySpaceNumberAsc(UUID floorId);
}
