package com.smartparking.parkinglot.infrastructure;

import com.smartparking.parkinglot.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, UUID> {
    Page<ParkingLot> findByOperationStatus(OperationStatus status, Pageable pageable);
    Optional<ParkingLot> findByIdAndOperationStatus(UUID id, OperationStatus status);
}
