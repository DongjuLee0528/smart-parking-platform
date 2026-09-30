package com.smartparking.parkingspace.dto.request;

import com.smartparking.parkingspace.domain.ParkingSpaceType;
import com.smartparking.parkingspace.dto.NormalizedPoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SaveParkingSpacesRequest(
    @NotNull @Min(0) Long version,
    @NotNull @Size(max = 500) List<@NotNull @Valid Item> spaces
) {
    public record Item(
        UUID id,
        @NotNull UUID zoneId,
        @NotBlank @Size(max = 40) String spaceNumber,
        @NotNull ParkingSpaceType type,
        @NotNull Boolean active,
        @NotNull @Size(min = 3, max = 64) List<@NotNull @Valid NormalizedPoint> mapPolygon,
        @NotNull @Size(max = 8) List<@NotNull @Valid Mapping> cameraMappings
    ) {}

    public record Mapping(
        @NotNull UUID cameraId,
        @Positive int priority,
        @NotNull @Size(min = 3, max = 64) List<@NotNull @Valid NormalizedPoint> imagePolygon
    ) {}
}
