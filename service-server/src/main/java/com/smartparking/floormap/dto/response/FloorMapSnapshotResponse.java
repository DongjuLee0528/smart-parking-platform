package com.smartparking.floormap.dto.response;

import com.smartparking.occupancy.domain.OccupancyState;
import com.smartparking.parkingspace.domain.ParkingSpaceType;
import com.smartparking.parkingspace.dto.NormalizedPoint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FloorMapSnapshotResponse(
    UUID floorId, UUID parkingLotId, String floorName, long mapVersion, String eventVersion,
    MapVersionResponse drawing,
    List<Zone> zones, List<Space> spaces
) {
    public record Zone(UUID id, String name) {}

    public record Space(
        UUID id, UUID zoneId, String spaceNumber, ParkingSpaceType type, boolean active,
        List<NormalizedPoint> mapPolygon, OccupancyState state, Double confidence, Instant observedAt
    ) {}
}
