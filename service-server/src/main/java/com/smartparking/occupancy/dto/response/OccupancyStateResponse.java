package com.smartparking.occupancy.dto.response;

import com.smartparking.occupancy.domain.OccupancyState;
import java.time.Instant;
import java.util.UUID;

public record OccupancyStateResponse(
    UUID parkingSpaceId, UUID zoneId, String spaceNumber, boolean active,
    OccupancyState state, Double confidence, Instant observedAt
) {
}
