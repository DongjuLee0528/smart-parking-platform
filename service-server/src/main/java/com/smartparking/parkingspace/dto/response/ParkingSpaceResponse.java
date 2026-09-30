package com.smartparking.parkingspace.dto.response;

import com.smartparking.parkingspace.domain.ParkingSpaceType;
import com.smartparking.parkingspace.dto.NormalizedPoint;
import java.util.List;
import java.util.UUID;

public record ParkingSpaceResponse(
    UUID id, UUID zoneId, String spaceNumber, ParkingSpaceType type, boolean active,
    List<NormalizedPoint> mapPolygon, List<CameraMapping> cameraMappings
) {
    public record CameraMapping(UUID cameraId, int priority, List<NormalizedPoint> imagePolygon) {}
}
