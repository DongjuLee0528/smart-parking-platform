package com.smartparking.occupancy.infrastructure;

import com.smartparking.occupancy.domain.OccupancyCurrent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OccupancyCurrentRepository extends JpaRepository<OccupancyCurrent, UUID> {
    @Query(value = """
        SELECT COALESCE(SUM(CASE WHEN o.state = 'EMPTY' AND c.status <> 'OFFLINE' THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN o.state IN ('EMPTY', 'OCCUPIED') AND c.status <> 'OFFLINE'
                THEN 1 ELSE 0 END), 0),
            COUNT(s.id),
            AVG(CASE WHEN o.state IN ('EMPTY', 'OCCUPIED') AND c.status <> 'OFFLINE'
                THEN o.confidence END)
        FROM parking_spaces s
        JOIN parking_zones z ON z.id = s.zone_id
        LEFT JOIN occupancy_current o ON o.parking_space_id = s.id
        LEFT JOIN cameras c ON c.id = o.source_camera_id
        WHERE z.floor_id = :floorId AND s.active = TRUE
        """, nativeQuery = true)
    List<Object[]> summarizeFloor(@Param("floorId") UUID floorId);

    default long countAvailableByFloorId(UUID floorId) {
        return ((Number) summarizeFloor(floorId).get(0)[0]).longValue();
    }
}
