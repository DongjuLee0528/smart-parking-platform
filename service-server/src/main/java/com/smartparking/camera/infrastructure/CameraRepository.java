package com.smartparking.camera.infrastructure;

import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.domain.CameraStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CameraRepository extends JpaRepository<Camera, UUID> {
    @Modifying
    @Query(value = """
        UPDATE cameras SET status = :status, status_observed_at = :observedAt,
            last_frame_at = CASE WHEN :status = 'OFFLINE' THEN last_frame_at ELSE :observedAt END
        WHERE id = :cameraId AND config_version = :configVersion
            AND (status_observed_at IS NULL OR status_observed_at < :observedAt)
        """, nativeQuery = true)
    int updateObservedStatus(@Param("cameraId") UUID cameraId, @Param("configVersion") long configVersion,
                             @Param("status") String status, @Param("observedAt") Instant observedAt);

    @Override
    @EntityGraph(attributePaths = "zone.floor.parkingLot")
    Page<Camera> findAll(Pageable pageable);

    @Query("select distinct c.zone.floor.parkingLot.id from Camera c " +
        "where c.zone.floor.parkingLot.id in :lotIds and c.status in :statuses")
    List<UUID> findReportingLotIds(@Param("lotIds") Collection<UUID> lotIds,
                                   @Param("statuses") Collection<CameraStatus> statuses);
}
