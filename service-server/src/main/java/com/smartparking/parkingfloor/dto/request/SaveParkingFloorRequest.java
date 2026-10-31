package com.smartparking.parkingfloor.dto.request;

import jakarta.validation.constraints.*;

public record SaveParkingFloorRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull Integer floorOrder,
    @PositiveOrZero Long version,
    Boolean active
) {}
