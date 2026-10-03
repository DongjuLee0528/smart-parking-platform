package com.smartparking.vehicle.dto.response;

import com.smartparking.vehicle.domain.Vehicle;
import java.time.Instant;
import java.util.UUID;

public record VehicleResponse(
    UUID id, String plateNumber, String nickname, boolean isPrimary, long version,
    Instant createdAt, Instant updatedAt
) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getPlateNumber(), vehicle.getNickname(),
            vehicle.isPrimary(), vehicle.getVersion(), vehicle.getCreatedAt(), vehicle.getUpdatedAt());
    }
}
