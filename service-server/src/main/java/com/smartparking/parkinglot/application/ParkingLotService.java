package com.smartparking.parkinglot.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartparking.audit.domain.AuditLog;
import com.smartparking.audit.infrastructure.AuditLogRepository;
import com.smartparking.camera.domain.CameraStatus;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.parkinglot.domain.*;
import com.smartparking.parkinglot.dto.request.*;
import com.smartparking.parkinglot.dto.response.*;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.occupancy.infrastructure.OccupancyCurrentRepository;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ParkingLotService {
    private final ParkingLotRepository repository;
    private final AuditLogRepository auditRepository;
    private final OccupancyCurrentRepository occupancy;
    private final CameraRepository cameras;
    private final JsonMapper auditMapper = JsonMapper.builder().build();

    public ParkingLotService(ParkingLotRepository repository, AuditLogRepository auditRepository,
                             OccupancyCurrentRepository occupancy, CameraRepository cameras) {
        this.repository = repository;
        this.auditRepository = auditRepository;
        this.occupancy = occupancy;
        this.cameras = cameras;
    }

    @Transactional
    public ParkingLotDetailResponse create(UUID actorId, CreateParkingLotRequest request) {
        var floorNames = new HashSet<String>();
        for (var floor : request.floors()) {
            if (!floorNames.add(floor.name().strip()) ||
                floor.zones().stream().map(String::strip).distinct().count() != floor.zones().size()) {
                throw new ParkingLotException(ErrorCode.VALIDATION_FAILED, "Floor and zone names must be unique");
            }
        }
        var lot = repository.saveAndFlush(new ParkingLot(request));
        var response = ParkingLotDetailResponse.from(lot);
        auditRepository.save(new AuditLog(actorId, "CREATE", lot.getId(), null, json(response)));
        return response;
    }

    @Transactional
    public ParkingLotDetailResponse update(UUID actorId, UUID id, UpdateParkingLotRequest request) {
        var lot = requireLot(id);
        if (!Objects.equals(lot.getVersion(), request.version())) {
            throw new ParkingLotException(ErrorCode.VERSION_CONFLICT, "Parking lot version has changed");
        }
        String before = json(ParkingLotDetailResponse.from(lot));
        lot.update(request);
        repository.flush();
        var response = ParkingLotDetailResponse.from(lot);
        auditRepository.save(new AuditLog(actorId, "UPDATE", lot.getId(), before, json(response)));
        return response;
    }

    public ParkingLotDetailResponse adminDetail(UUID id) {
        return ParkingLotDetailResponse.from(requireLot(id));
    }

    public ParkingLotDetailResponse detail(UUID id) {
        var lot = repository.findByIdAndOperationStatus(id, OperationStatus.ACTIVE)
            .orElseThrow(() -> new ParkingLotException(ErrorCode.PARKING_LOT_NOT_FOUND, "Parking lot not found"));
        var byFloor = new HashMap<UUID, ParkingLotDetailResponse.Availability>();
        for (var floor : lot.getFloors()) {
            var summary = occupancy.summarizeFloor(floor.getId()).get(0);
            byFloor.put(floor.getId(), new ParkingLotDetailResponse.Availability(
                ((Number) summary[0]).longValue(), ((Number) summary[1]).longValue(),
                ((Number) summary[2]).longValue(),
                summary[3] == null ? null : ((Number) summary[3]).doubleValue()));
        }
        return ParkingLotDetailResponse.from(lot, byFloor);
    }

    public ParkingLotPage list(int page, int size, String sort, boolean admin) {
        if (page < 0 || size < 1 || size > 100 || !Set.of("name,asc", "name,desc").contains(sort)) {
            throw new ParkingLotException(ErrorCode.VALIDATION_FAILED,
                "Use page >= 0, size 1..100 and sort name,asc or name,desc");
        }
        var pageable = PageRequest.of(page, size,
            Sort.by(sort.endsWith("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, "name").and(Sort.by("id")));
        var result = admin ? repository.findAll(pageable)
            : repository.findByOperationStatus(OperationStatus.ACTIVE, pageable);
        return new ParkingLotPage(result.getContent().stream().map(lot -> new ParkingLotPage.Item(
            lot.getId(), lot.getName(), lot.getAddress(), lot.getOperatingHours(), lot.getFeeInformation(),
            lot.getOperationStatus(), lot.getVersion())).toList(), page, size, result.getTotalElements());
    }

    public NearbyParkingLotResponse nearby(NearbyParkingLotSearchRequest request) {
        if (!Double.isFinite(request.lat()) || request.lat() < -90 || request.lat() > 90 ||
            !Double.isFinite(request.lng()) || request.lng() < -180 || request.lng() > 180 ||
            !Double.isFinite(request.radius()) || request.radius() <= 0 || request.radius() > 50_000 ||
            request.page() < 0 || request.size() < 1 || request.size() > 100) {
            throw new ParkingLotException(ErrorCode.VALIDATION_FAILED,
                "Use valid coordinates, radius > 0 and <= 50000 meters, page >= 0 and size 1..100");
        }
        var result = repository.findNearby(request.lat(), request.lng(), request.radius(),
            PageRequest.of(request.page(), request.size()));
        var lots = result.getContent();
        Set<UUID> reporting = lots.isEmpty() ? Set.of() : Set.copyOf(cameras.findReportingLotIds(
            lots.stream().map(ParkingLot::getId).toList(), List.of(CameraStatus.ONLINE, CameraStatus.DEGRADED)));
        return new NearbyParkingLotResponse(lots.stream().map(lot -> new NearbyParkingLotResponse.Item(
            lot.getId(), lot.getName(), lot.getAddress(), lot.getLatitude(), lot.getLongitude(),
            distanceMeters(request.lat(), request.lng(), lot.getLatitude(), lot.getLongitude()),
            reporting.contains(lot.getId()))).toList(), request.page(), request.size(), result.getTotalElements());
    }

    private double distanceMeters(double lat, double lng, double targetLat, double targetLng) {
        double latitudeDifference = Math.toRadians(targetLat - lat);
        double longitudeDifference = Math.toRadians(targetLng - lng);
        double a = Math.pow(Math.sin(latitudeDifference / 2), 2) +
            Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(targetLat)) *
                Math.pow(Math.sin(longitudeDifference / 2), 2);
        return 6_371_000 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
    }

    private ParkingLot requireLot(UUID id) {
        return repository.findById(id).orElseThrow(() ->
            new ParkingLotException(ErrorCode.PARKING_LOT_NOT_FOUND, "Parking lot not found"));
    }

    private String json(ParkingLotDetailResponse response) {
        try {
            return auditMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize parking lot audit snapshot", exception);
        }
    }
}
