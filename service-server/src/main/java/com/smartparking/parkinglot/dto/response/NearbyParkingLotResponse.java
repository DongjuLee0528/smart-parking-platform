package com.smartparking.parkinglot.dto.response;

import java.util.List;
import java.util.UUID;

public record NearbyParkingLotResponse(List<Item> items, int page, int size, long totalElements) {
    public record Item(UUID id, String name, String address, double latitude, double longitude,
                       double distanceMeters, boolean realtimeAvailable) {}
}
