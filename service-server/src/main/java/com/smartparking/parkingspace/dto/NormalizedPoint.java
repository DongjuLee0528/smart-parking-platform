package com.smartparking.parkingspace.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record NormalizedPoint(
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double x,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double y
) {}
