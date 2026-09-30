package com.smartparking.camera.infrastructure;

import com.smartparking.camera.domain.Camera;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CameraRepository extends JpaRepository<Camera, UUID> {
    @Override
    @EntityGraph(attributePaths = "zone.floor.parkingLot")
    Page<Camera> findAll(Pageable pageable);
}
