package com.smartparking.savedparkinglocation.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SaveParkingLocationRequest(
    @NotNull UUID vehicleId,
    @NotNull UUID parkingSpaceId,
    Boolean confirm
) {
}
