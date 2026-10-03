package com.smartparking.vehicle.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateVehicleRequest(
    @NotNull @PositiveOrZero Long version,
    @NotBlank @Size(max = 20) String plateNumber,
    @Size(max = 100) String nickname,
    @NotNull Boolean isPrimary
) {
}
