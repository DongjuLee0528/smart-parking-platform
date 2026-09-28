package com.smartparking.parkinglot.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreateParkingLotRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 500) String address,
    @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
    @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
    @NotNull @Size(max = 2000) String operatingHours,
    @NotNull @Size(max = 2000) String feeInformation,
    @NotNull @Size(max = 100) List<@NotNull @Valid Floor> floors
) {
    public record Floor(
        @NotBlank @Size(max = 100) String name,
        @NotNull Integer floorOrder,
        @NotNull @Size(max = 100) List<@NotBlank @Size(max = 100) String> zones
    ) {}
}
