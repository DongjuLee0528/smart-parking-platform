package com.smartparking.occupancy.application;

import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.occupancy.domain.OccupancyCurrent;
import com.smartparking.occupancy.domain.OccupancyHistory;
import com.smartparking.occupancy.domain.OccupancyIngestEvent;
import com.smartparking.occupancy.domain.OccupancyState;
import com.smartparking.occupancy.dto.request.AiOccupancyResultRequest;
import com.smartparking.occupancy.dto.response.OccupancyStateResponse;
import com.smartparking.occupancy.infrastructure.OccupancyCurrentRepository;
import com.smartparking.occupancy.infrastructure.OccupancyHistoryRepository;
import com.smartparking.parkingfloor.infrastructure.ParkingFloorRepository;
import com.smartparking.parkingspace.domain.ParkingSpace;
import com.smartparking.parkingspace.infrastructure.ParkingSpaceRepository;
import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OccupancyService {
    private final CameraRepository cameras;
    private final ParkingFloorRepository floors;
    private final ParkingSpaceRepository spaces;
    private final OccupancyCurrentRepository current;
    private final OccupancyHistoryRepository history;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher events;

    public OccupancyService(CameraRepository cameras, ParkingFloorRepository floors, ParkingSpaceRepository spaces,
                            OccupancyCurrentRepository current, OccupancyHistoryRepository history,
                            EntityManager entityManager, ApplicationEventPublisher events) {
        this.cameras = cameras;
        this.floors = floors;
        this.spaces = spaces;
        this.current = current;
        this.history = history;
        this.entityManager = entityManager;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<OccupancyStateResponse> floorSnapshot(UUID floorId) {
        if (!floors.existsById(floorId)) {
            throw new OccupancyException(ErrorCode.PARKING_FLOOR_NOT_FOUND, "Parking floor not found");
        }
        var floorSpaces = spaces.findByZoneFloorIdOrderBySpaceNumberAsc(floorId);
        var activeIds = floorSpaces.stream().filter(ParkingSpace::isActive).map(ParkingSpace::getId).toList();
        var latest = current.findAllById(activeIds).stream().collect(Collectors.toMap(
            OccupancyCurrent::getParkingSpaceId, Function.identity()));
        return floorSpaces.stream()
            .sorted(Comparator.comparing((ParkingSpace space) -> space.getZone().getName())
                .thenComparing(ParkingSpace::getSpaceNumber).thenComparing(ParkingSpace::getId))
            .map(space -> {
                var observation = latest.get(space.getId());
                return new OccupancyStateResponse(space.getId(), space.getZone().getId(),
                    space.getSpaceNumber(), space.isActive(),
                    observation == null ? OccupancyState.UNKNOWN : observation.getState(),
                    observation == null ? null : observation.getConfidence(),
                    observation == null ? null : observation.getObservedAt());
            }).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void ingest(AiOccupancyResultRequest request) {
        if (request.capturedAt().isAfter(request.processedAt())) {
            throw new OccupancyException(ErrorCode.VALIDATION_FAILED, "Captured time is after processed time");
        }
        cameras.findById(request.cameraId()).orElseThrow(() ->
            new OccupancyException(ErrorCode.CAMERA_NOT_FOUND, "Camera not found"));
        var existingEvent = entityManager.find(OccupancyIngestEvent.class, request.eventId());
        if (existingEvent != null) {
            if (!existingEvent.getCameraId().equals(request.cameraId())) {
                throw new OccupancyException(ErrorCode.VERSION_CONFLICT, "Event ID belongs to another camera");
            }
            return;
        }
        var seenSpaces = new HashSet<UUID>();
        for (var result : request.spaces().stream()
            .sorted(Comparator.comparing(AiOccupancyResultRequest.Space::parkingSpaceId)).toList()) {
            if (!seenSpaces.add(result.parkingSpaceId()) || !Double.isFinite(result.confidence())) {
                throw new OccupancyException(ErrorCode.VALIDATION_FAILED, "Invalid or duplicate space result");
            }
            var space = spaces.lockById(result.parkingSpaceId()).orElseThrow(() ->
                new OccupancyException(ErrorCode.PARKING_SPACE_NOT_FOUND, "Parking space not found"));
            if (!space.isActive() || space.getCameraMappings().stream().noneMatch(mapping ->
                mapping.getCameraId().equals(request.cameraId()) &&
                    mapping.getConfigVersion() == request.configVersion())) {
                throw new OccupancyException(ErrorCode.VALIDATION_FAILED,
                    "Space is inactive, unmapped, or uses another configuration version");
            }
            var previous = current.findById(result.parkingSpaceId()).orElse(null);
            if (previous != null && !request.capturedAt().isAfter(previous.getObservedAt())) {
                continue;
            }
            var priorState = previous == null ? OccupancyState.UNKNOWN : previous.getState();
            if (previous == null) {
                current.save(new OccupancyCurrent(space.getId(), result.state(), result.confidence(),
                    request.cameraId(), request.capturedAt()));
            } else {
                previous.update(result.state(), result.confidence(), request.cameraId(), request.capturedAt());
            }
            if (priorState != result.state()) {
                history.save(new OccupancyHistory(space.getId(), priorState, result.state(),
                    result.confidence(), request.capturedAt()));
                events.publishEvent(new OccupancyChangedEvent(space.getZone().getFloor().getId(),
                    new OccupancyStateResponse(space.getId(), space.getZone().getId(),
                        space.getSpaceNumber(), space.isActive(), result.state(), result.confidence(),
                        request.capturedAt())));
            }
        }
        entityManager.persist(new OccupancyIngestEvent(request.eventId(), request.cameraId(), request.capturedAt()));
        entityManager.flush();
    }
}
