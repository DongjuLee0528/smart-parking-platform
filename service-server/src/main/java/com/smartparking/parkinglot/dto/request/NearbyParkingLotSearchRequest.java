package com.smartparking.parkinglot.dto.request;

public record NearbyParkingLotSearchRequest(double lat, double lng, double radius, int page, int size) {
}
