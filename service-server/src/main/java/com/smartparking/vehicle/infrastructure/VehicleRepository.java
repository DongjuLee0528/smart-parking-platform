package com.smartparking.vehicle.infrastructure;

import com.smartparking.vehicle.domain.Vehicle;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    Page<Vehicle> findByUserId(UUID userId, Pageable pageable);
    Optional<Vehicle> findByIdAndUserId(UUID id, UUID userId);
    Optional<Vehicle> findByUserIdAndPrimaryTrue(UUID userId);
    boolean existsByUserIdAndPlateNumber(UUID userId, String plateNumber);
    boolean existsByUserIdAndPlateNumberAndIdNot(UUID userId, String plateNumber, UUID id);
}
