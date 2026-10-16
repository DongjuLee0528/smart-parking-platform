package com.smartparking.parkinglot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartparking.parkinglot.domain.*;
import java.util.*;

public record ParkingLotDetailResponse(
    UUID id, String name, String address, double latitude, double longitude,
    String operatingHours, String feeInformation, OperationStatus operationStatus,
    SetupStatus setupStatus, Long version, List<Floor> floors,
    @JsonInclude(JsonInclude.Include.NON_NULL) Availability availability
) {
    public ParkingLotDetailResponse(UUID id, String name, String address, double latitude, double longitude,
                                    String operatingHours, String feeInformation, OperationStatus operationStatus,
                                    SetupStatus setupStatus, Long version, List<Floor> floors) {
        this(id, name, address, latitude, longitude, operatingHours, feeInformation,
            operationStatus, setupStatus, version, floors, null);
    }

    public record Floor(UUID id, String name, int floorOrder, List<Zone> zones,
                        @JsonInclude(JsonInclude.Include.NON_NULL) Availability availability) {}
    public record Zone(UUID id, String name) {}
    public record Availability(long availableSpaces, long knownSpaces, long totalSpaces, Double confidence) {}

    public static ParkingLotDetailResponse from(ParkingLot lot) {
        return from(lot, null);
    }

    public static ParkingLotDetailResponse from(ParkingLot lot, Map<UUID, Availability> byFloor) {
        var floors = lot.getFloors().stream().map(floor -> new Floor(floor.getId(), floor.getName(),
            floor.getFloorOrder(), floor.getZones().stream()
                .map(zone -> new Zone(zone.getId(), zone.getName())).toList(),
            byFloor == null ? null : Objects.requireNonNull(byFloor.get(floor.getId())))).toList();
        Availability availability = null;
        if (byFloor != null) {
            long available = floors.stream().mapToLong(floor -> floor.availability().availableSpaces()).sum();
            long known = floors.stream().mapToLong(floor -> floor.availability().knownSpaces()).sum();
            long total = floors.stream().mapToLong(floor -> floor.availability().totalSpaces()).sum();
            double weightedConfidence = floors.stream().filter(floor -> floor.availability().knownSpaces() > 0)
                .mapToDouble(floor -> floor.availability().confidence() * floor.availability().knownSpaces()).sum();
            availability = new Availability(available, known, total,
                known == 0 ? null : weightedConfidence / known);
        }
        return new ParkingLotDetailResponse(lot.getId(), lot.getName(), lot.getAddress(),
            lot.getLatitude(), lot.getLongitude(), lot.getOperatingHours(),
            lot.getFeeInformation(), lot.getOperationStatus(), lot.getSetupStatus(), lot.getVersion(),
            floors, availability);
    }
}
