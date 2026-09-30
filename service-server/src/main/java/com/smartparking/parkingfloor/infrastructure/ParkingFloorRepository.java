package com.smartparking.parkingfloor.infrastructure;

import com.smartparking.parkingfloor.domain.ParkingFloor;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ParkingFloorRepository extends JpaRepository<ParkingFloor, UUID> {
    @Modifying
    @Query("update ParkingFloor floor set floor.spacesVersion = floor.spacesVersion + 1 " +
        "where floor.id = :id and floor.spacesVersion = :version")
    int advanceSpacesVersion(UUID id, Long version);
}
