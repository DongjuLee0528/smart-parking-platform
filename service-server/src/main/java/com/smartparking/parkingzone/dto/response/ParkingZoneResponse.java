package com.smartparking.parkingzone.dto.response;

import com.smartparking.parkingzone.domain.ParkingZone;
import java.util.UUID;

public record ParkingZoneResponse(UUID id, UUID floorId, String name, boolean active, Long version) {
    public static ParkingZoneResponse from(ParkingZone zone) {
        return new ParkingZoneResponse(zone.getId(), zone.getFloor().getId(), zone.getName(), zone.isActive(),
            zone.getVersion());
    }
}
