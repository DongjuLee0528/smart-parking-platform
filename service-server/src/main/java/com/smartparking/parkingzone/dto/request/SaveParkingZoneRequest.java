package com.smartparking.parkingzone.dto.request;

import jakarta.validation.constraints.*;

public record SaveParkingZoneRequest(
    @NotBlank @Size(max = 100) String name,
    @PositiveOrZero Long version,
    Boolean active
) {}
