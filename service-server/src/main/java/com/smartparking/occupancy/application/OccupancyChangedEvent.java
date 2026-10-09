package com.smartparking.occupancy.application;

import com.smartparking.occupancy.dto.response.OccupancyStateResponse;
import java.util.UUID;

public record OccupancyChangedEvent(UUID floorId, OccupancyStateResponse occupancy) {}
