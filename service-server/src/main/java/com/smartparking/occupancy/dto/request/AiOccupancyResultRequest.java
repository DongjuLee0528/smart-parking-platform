package com.smartparking.occupancy.dto.request;

import com.smartparking.camera.domain.CameraStatus;
import com.smartparking.occupancy.domain.OccupancyState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiOccupancyResultRequest(
    @NotNull UUID eventId,
    @NotNull UUID cameraId,
    @NotNull @Min(0) Long configVersion,
    @NotNull @Valid Model model,
    @NotNull Instant capturedAt,
    @NotNull Instant processedAt,
    @NotNull CameraStatus cameraStatus,
    @NotEmpty @Size(max = 500) List<@NotNull @Valid Space> spaces
) {
    public record Model(@NotBlank @Size(max = 100) String name,
                        @NotBlank @Size(max = 100) String version,
                        @NotBlank @Size(max = 128) String sha256) {}

    public record Space(@NotNull UUID parkingSpaceId, @NotNull OccupancyState state,
                        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double confidence,
                        @Valid Evidence evidence) {}

    public record Evidence(@DecimalMin("0.0") @DecimalMax("1.0") Double vehicleConfidence,
                           @DecimalMin("0.0") @DecimalMax("1.0") Double overlapRatio) {}
}
