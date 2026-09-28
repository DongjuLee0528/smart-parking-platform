package com.smartparking.parkinglot.dto.response;

import com.smartparking.parkinglot.domain.*;
import java.util.*;

public record ParkingLotDetailResponse(
    UUID id, String name, String address, double latitude, double longitude,
    String operatingHours, String feeInformation, OperationStatus operationStatus,
    SetupStatus setupStatus, Long version, List<Floor> floors
) {
    public record Floor(UUID id, String name, int floorOrder, List<Zone> zones) {}
    public record Zone(UUID id, String name) {}

    public static ParkingLotDetailResponse from(ParkingLot lot) {
        return new ParkingLotDetailResponse(lot.getId(), lot.getName(), lot.getAddress(),
            lot.getLatitude(), lot.getLongitude(), lot.getOperatingHours(),
            lot.getFeeInformation(), lot.getOperationStatus(), lot.getSetupStatus(), lot.getVersion(),
            lot.getFloors().stream().map(floor -> new Floor(floor.getId(), floor.getName(),
                floor.getFloorOrder(), floor.getZones().stream()
                    .map(zone -> new Zone(zone.getId(), zone.getName())).toList())).toList());
    }
}
