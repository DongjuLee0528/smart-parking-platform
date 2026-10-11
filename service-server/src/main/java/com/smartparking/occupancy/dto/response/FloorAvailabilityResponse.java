package com.smartparking.occupancy.dto.response;

import java.util.UUID;

public record FloorAvailabilityResponse(UUID floorId, long availableSpaces) {
}
