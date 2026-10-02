package com.smartparking.occupancy.infrastructure;

import com.smartparking.occupancy.domain.OccupancyCurrent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OccupancyCurrentRepository extends JpaRepository<OccupancyCurrent, UUID> {
}
