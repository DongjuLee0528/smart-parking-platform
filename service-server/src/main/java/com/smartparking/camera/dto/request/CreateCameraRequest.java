package com.smartparking.camera.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateCameraRequest(
    @NotNull UUID zoneId,
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{0,127}") String streamKeyRef
) {}
