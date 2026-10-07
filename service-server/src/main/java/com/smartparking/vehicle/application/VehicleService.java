package com.smartparking.vehicle.application;

import com.smartparking.global.error.ErrorCode;
import com.smartparking.savedparkinglocation.infrastructure.SavedParkingLocationRepository;
import com.smartparking.user.domain.User;
import com.smartparking.vehicle.domain.Vehicle;
import com.smartparking.vehicle.dto.request.CreateVehicleRequest;
import com.smartparking.vehicle.dto.request.UpdateVehicleRequest;
import com.smartparking.vehicle.dto.response.VehiclePage;
import com.smartparking.vehicle.dto.response.VehicleResponse;
import com.smartparking.vehicle.infrastructure.VehicleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VehicleService {
    private static final Pattern PLATE = Pattern.compile("^(?:[가-힣]{2})?[0-9]{2,3}[가-힣][0-9]{4}$");
    private final VehicleRepository repository;
    private final SavedParkingLocationRepository savedLocations;
    private final EntityManager entityManager;

    public VehicleService(VehicleRepository repository, SavedParkingLocationRepository savedLocations,
                          EntityManager entityManager) {
        this.repository = repository;
        this.savedLocations = savedLocations;
        this.entityManager = entityManager;
    }

    @Transactional
    public VehicleResponse create(UUID userId, CreateVehicleRequest request) {
        lockUser(userId);
        var plate = plate(request.plateNumber());
        if (repository.existsByUserIdAndPlateNumber(userId, plate)) {
            throw new VehicleException(ErrorCode.VEHICLE_CONFLICT, "Vehicle is already registered");
        }
        if (request.isPrimary()) {
            clearPreviousPrimary(userId, null);
        }
        return VehicleResponse.from(repository.saveAndFlush(new Vehicle(userId, plate, nickname(request.nickname()),
            request.isPrimary())));
    }

    public VehiclePage list(UUID userId, int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100 || !Set.of("createdAt,asc", "createdAt,desc").contains(sort)) {
            throw new VehicleException(ErrorCode.VALIDATION_FAILED, "Invalid vehicle pagination or sort");
        }
        var direction = sort.endsWith("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        var vehicles = repository.findByUserId(userId,
            PageRequest.of(page, size, Sort.by(direction, "createdAt").and(Sort.by("id"))));
        return new VehiclePage(vehicles.getContent().stream().map(VehicleResponse::from).toList(),
            page, size, vehicles.getTotalElements());
    }

    public VehicleResponse detail(UUID userId, UUID vehicleId) {
        return VehicleResponse.from(requireOwned(userId, vehicleId));
    }

    @Transactional
    public VehicleResponse update(UUID userId, UUID vehicleId, UpdateVehicleRequest request) {
        lockUser(userId);
        var vehicle = requireOwned(userId, vehicleId);
        checkVersion(vehicle, request.version());
        var plate = plate(request.plateNumber());
        if (repository.existsByUserIdAndPlateNumberAndIdNot(userId, plate, vehicleId)) {
            throw new VehicleException(ErrorCode.VEHICLE_CONFLICT, "Vehicle is already registered");
        }
        if (request.isPrimary()) {
            clearPreviousPrimary(userId, vehicleId);
        }
        vehicle.update(plate, nickname(request.nickname()), request.isPrimary());
        repository.flush();
        return VehicleResponse.from(vehicle);
    }

    @Transactional
    public void delete(UUID userId, UUID vehicleId, Long version) {
        lockUser(userId);
        var vehicle = requireOwned(userId, vehicleId);
        checkVersion(vehicle, version);
        var releasedAt = Instant.now();
        savedLocations.findAllByUserIdAndVehicleId(userId, vehicleId)
            .forEach(location -> location.detachVehicle(releasedAt));
        repository.delete(vehicle);
        repository.flush();
    }

    private void lockUser(UUID userId) {
        if (entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new VehicleException(ErrorCode.AUTH_TOKEN_INVALID, "Authenticated user no longer exists");
        }
    }

    private Vehicle requireOwned(UUID userId, UUID vehicleId) {
        return repository.findByIdAndUserId(vehicleId, userId).orElseThrow(() ->
            new VehicleException(ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found"));
    }

    private void clearPreviousPrimary(UUID userId, UUID exceptId) {
        repository.findByUserIdAndPrimaryTrue(userId).filter(vehicle -> !vehicle.getId().equals(exceptId))
            .ifPresent(Vehicle::clearPrimary);
    }

    private void checkVersion(Vehicle vehicle, Long version) {
        if (version == null || version < 0) {
            throw new VehicleException(ErrorCode.VALIDATION_FAILED, "Vehicle version is required");
        }
        if (!Objects.equals(vehicle.getVersion(), version)) {
            throw new VehicleException(ErrorCode.VERSION_CONFLICT, "Vehicle version has changed");
        }
    }

    private String plate(String value) {
        var normalized = value.strip().replace("-", "").replace(" ", "");
        if (!PLATE.matcher(normalized).matches()) {
            throw new VehicleException(ErrorCode.VALIDATION_FAILED, "Invalid vehicle plate number");
        }
        return normalized;
    }

    private String nickname(String value) {
        return value == null ? "" : value.strip();
    }
}
