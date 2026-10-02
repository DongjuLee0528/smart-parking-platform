package com.smartparking.occupancy.infrastructure;

import com.smartparking.occupancy.domain.OccupancyHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OccupancyHistoryRepository extends JpaRepository<OccupancyHistory, UUID> {
}
