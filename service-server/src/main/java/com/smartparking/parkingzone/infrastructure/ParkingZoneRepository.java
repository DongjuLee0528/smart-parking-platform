package com.smartparking.parkingzone.infrastructure;

import com.smartparking.parkingzone.domain.ParkingZone;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingZoneRepository extends JpaRepository<ParkingZone, UUID> {}
