package com.smartparking.camera.dto.response;

import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.domain.CameraStatus;
import java.time.Instant;
import java.util.UUID;

public record CameraResponse(
    UUID id, UUID parkingLotId, UUID floorId, UUID zoneId, String name,
    String streamKeyRef, CameraStatus status, Instant lastFrameAt, Long configVersion
) {
    public static CameraResponse from(Camera camera) {
        var zone = camera.getZone();
        var floor = zone.getFloor();
        return new CameraResponse(camera.getId(), floor.getParkingLot().getId(), floor.getId(),
            zone.getId(), camera.getName(), camera.getStreamKeyRef(), camera.getStatus(),
            camera.getLastFrameAt(), camera.getConfigVersion());
    }
}
