package com.smartparking.savedparkinglocation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.occupancy.domain.OccupancyState;
import com.smartparking.occupancy.infrastructure.OccupancyCurrentRepository;
import com.smartparking.parkingspace.infrastructure.ParkingSpaceRepository;
import com.smartparking.savedparkinglocation.domain.SavedParkingLocation;
import com.smartparking.savedparkinglocation.dto.request.SaveParkingLocationRequest;
import com.smartparking.savedparkinglocation.dto.response.SavedParkingLocationResponse;
import com.smartparking.savedparkinglocation.dto.response.SavedParkingLocationResponse.Snapshot;
import com.smartparking.savedparkinglocation.infrastructure.SavedParkingLocationRepository;
import com.smartparking.user.domain.User;
import com.smartparking.vehicle.infrastructure.VehicleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SavedParkingLocationService {
    private final SavedParkingLocationRepository locations;
    private final VehicleRepository vehicles;
    private final ParkingSpaceRepository spaces;
    private final OccupancyCurrentRepository occupancy;
    private final EntityManager entityManager;
    private final ObjectMapper mapper;

    public SavedParkingLocationService(SavedParkingLocationRepository locations, VehicleRepository vehicles,
                                       ParkingSpaceRepository spaces, OccupancyCurrentRepository occupancy,
                                       EntityManager entityManager, ObjectMapper mapper) {
        this.locations = locations;
        this.vehicles = vehicles;
        this.spaces = spaces;
        this.occupancy = occupancy;
        this.entityManager = entityManager;
        this.mapper = mapper;
    }

    @Transactional
    public SavedParkingLocationResponse save(UUID userId, SaveParkingLocationRequest request) {
        lockUser(userId);
        var vehicle = vehicles.findByIdAndUserId(request.vehicleId(), userId).orElseThrow(() ->
            new SavedParkingLocationException(ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found"));
        var space = spaces.findById(request.parkingSpaceId()).orElseThrow(() ->
            new SavedParkingLocationException(ErrorCode.PARKING_SPACE_NOT_FOUND, "Parking space not found"));
        if (!space.isActive()) {
            throw new SavedParkingLocationException(ErrorCode.VALIDATION_FAILED, "Parking space is inactive");
        }
        var active = locations.findByUserIdAndReleasedAtIsNull(userId);
        var state = occupancy.findById(space.getId()).map(current -> current.getState()).orElse(null);
        if (!Boolean.TRUE.equals(request.confirm())) {
            if (active.isPresent()) {
                throw new SavedParkingLocationException(ErrorCode.DUPLICATE_SELECTION_CONFIRM_REQUIRED,
                    "An active parking location already exists; confirm replacement");
            }
            if (state == OccupancyState.EMPTY) {
                throw new SavedParkingLocationException(ErrorCode.EMPTY_SPACE_CONFIRM_REQUIRED,
                    "Selected parking space is empty; confirm saving");
            }
        }
        var zone = space.getZone();
        var floor = zone.getFloor();
        var lot = floor.getParkingLot();
        var snapshot = new Snapshot(lot.getId(), lot.getName(), floor.getId(), floor.getName(),
            zone.getId(), zone.getName(), space.getSpaceNumber(), null, state);
        active.ifPresent(SavedParkingLocation::release);
        var location = locations.saveAndFlush(new SavedParkingLocation(userId, vehicle.getId(),
            space.getId(), json(snapshot)));
        return response(location);
    }

    public SavedParkingLocationResponse active(UUID userId) {
        return locations.findByUserIdAndReleasedAtIsNull(userId).map(this::response).orElse(null);
    }

    @Transactional
    public void release(UUID userId, UUID locationId) {
        lockUser(userId);
        var location = locations.findByIdAndUserId(locationId, userId).orElseThrow(() ->
            new SavedParkingLocationException(ErrorCode.SAVED_PARKING_LOCATION_NOT_FOUND,
                "Saved parking location not found"));
        location.release();
    }

    private void lockUser(UUID userId) {
        if (entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new SavedParkingLocationException(ErrorCode.AUTH_TOKEN_INVALID,
                "Authenticated user no longer exists");
        }
    }

    private SavedParkingLocationResponse response(SavedParkingLocation location) {
        try {
            return new SavedParkingLocationResponse(location.getId(), location.getVehicleId(),
                location.getParkingSpaceId(), mapper.readValue(location.getSnapshotJson(), Snapshot.class),
                location.getSavedAt(), location.getReleasedAt());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read saved parking location snapshot", exception);
        }
    }

    private String json(Snapshot snapshot) {
        try {
            return mapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize parking location snapshot", exception);
        }
    }
}
