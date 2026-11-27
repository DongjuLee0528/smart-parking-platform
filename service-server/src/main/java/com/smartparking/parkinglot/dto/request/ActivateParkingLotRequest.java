package com.smartparking.parkinglot.dto.request;

import jakarta.validation.constraints.NotNull;

public record ActivateParkingLotRequest(@NotNull Long version) {}
