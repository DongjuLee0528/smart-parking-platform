package com.smartparking.vehicle.dto.response;

import java.util.List;

public record VehiclePage(List<VehicleResponse> items, int page, int size, long totalElements) {
}
