package com.smartparking.parkingspace.dto.response;

import java.util.List;
import java.util.UUID;

public record ParkingSpacesResponse(UUID floorId, Long version, List<ParkingSpaceResponse> spaces) {}
