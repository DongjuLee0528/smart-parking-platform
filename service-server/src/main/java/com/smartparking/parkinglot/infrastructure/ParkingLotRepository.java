package com.smartparking.parkinglot.infrastructure;

import com.smartparking.parkinglot.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, UUID> {
    String DISTANCE_METERS = "6371000 * 2 * ASIN(SQRT(LEAST(1, " +
        "POWER(SIN(RADIANS(p.latitude - :lat) / 2), 2) + " +
        "COS(RADIANS(:lat)) * COS(RADIANS(p.latitude)) * " +
        "POWER(SIN(RADIANS(p.longitude - :lng) / 2), 2))))";

    Page<ParkingLot> findByOperationStatus(OperationStatus status, Pageable pageable);
    Optional<ParkingLot> findByIdAndOperationStatus(UUID id, OperationStatus status);

    @Query(value = "SELECT p.* FROM parking_lots p WHERE p.operation_status = 'ACTIVE' AND " +
        DISTANCE_METERS + " <= :radius ORDER BY " + DISTANCE_METERS + ", p.id",
        countQuery = "SELECT COUNT(*) FROM parking_lots p WHERE p.operation_status = 'ACTIVE' AND " +
            DISTANCE_METERS + " <= :radius", nativeQuery = true)
    Page<ParkingLot> findNearby(@Param("lat") double lat, @Param("lng") double lng,
                                @Param("radius") double radius, Pageable pageable);
}
