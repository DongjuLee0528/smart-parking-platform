package com.smartparking.parkingfloor.dto.response;

import com.smartparking.parkingfloor.domain.ParkingFloor;
import com.smartparking.parkingzone.dto.response.ParkingZoneResponse;
import java.util.List;
import java.util.UUID;

public record ParkingFloorResponse(UUID id, String name, int floorOrder, boolean active, Long version,
                                   List<ParkingZoneResponse> zones) {
    public static ParkingFloorResponse from(ParkingFloor floor, boolean includeInactive) {
        return new ParkingFloorResponse(floor.getId(), floor.getName(), floor.getFloorOrder(), floor.isActive(),
            floor.getVersion(), floor.getZones().stream().filter(zone -> includeInactive || zone.isActive())
                .map(ParkingZoneResponse::from).toList());
    }
}
