package com.smartparking.floormap.infrastructure;

import com.smartparking.floormap.domain.MapVersion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapVersionRepository extends JpaRepository<MapVersion, UUID> {
    Optional<MapVersion> findFirstByFloorIdOrderByVersionDesc(UUID floorId);
}
