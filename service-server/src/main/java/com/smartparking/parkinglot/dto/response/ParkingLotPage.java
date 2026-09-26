package com.smartparking.parkinglot.dto.response;

import com.smartparking.parkinglot.domain.OperationStatus;
import java.util.*;

public record ParkingLotPage(List<Item> items, int page, int size, long totalElements) {
    public record Item(UUID id, String name, String address, String operatingHours,
                       String feeInformation, OperationStatus operationStatus, Long version) {}
}
