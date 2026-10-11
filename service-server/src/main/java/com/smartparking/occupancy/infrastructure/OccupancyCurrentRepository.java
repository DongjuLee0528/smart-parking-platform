package com.smartparking.occupancy.infrastructure;

import com.smartparking.occupancy.domain.OccupancyCurrent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OccupancyCurrentRepository extends JpaRepository<OccupancyCurrent, UUID> {
    @Query(value = """
        SELECT COUNT(*) FROM occupancy_current o
        JOIN parking_spaces s ON s.id = o.parking_space_id
        JOIN parking_zones z ON z.id = s.zone_id
        JOIN cameras c ON c.id = o.source_camera_id
        WHERE z.floor_id = :floorId AND s.active = TRUE
            AND o.state = 'EMPTY' AND c.status <> 'OFFLINE'
        """, nativeQuery = true)
    long countAvailableByFloorId(@Param("floorId") UUID floorId);
}
