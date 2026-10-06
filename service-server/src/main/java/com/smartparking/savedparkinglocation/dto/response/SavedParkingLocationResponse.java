package com.smartparking.savedparkinglocation.dto.response;

import com.smartparking.occupancy.domain.OccupancyState;
import java.time.Instant;
import java.util.UUID;

public record SavedParkingLocationResponse(
    UUID id,
    UUID vehicleId,
    UUID parkingSpaceId,
    Snapshot snapshot,
    Instant savedAt,
    Instant releasedAt
) {
    public record Snapshot(
        UUID parkingLotId, String parkingLotName,
        UUID floorId, String floorName,
        UUID zoneId, String zoneName,
        String spaceNumber,
        OccupancyState occupancyState
    ) {
    }
}
