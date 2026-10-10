package com.smartparking.streaming.dto.response;

import com.smartparking.occupancy.dto.response.OccupancyStateResponse;
import com.smartparking.streaming.domain.FloorEventType;
import java.util.UUID;

public record FloorEventResponse(FloorEventType type, UUID floorId, OccupancyStateResponse occupancy) {}
