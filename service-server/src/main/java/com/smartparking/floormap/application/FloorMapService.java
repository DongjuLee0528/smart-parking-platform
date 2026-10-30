package com.smartparking.floormap.application;

import com.smartparking.camera.domain.CameraStatus;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.audit.domain.AuditLog;
import com.smartparking.audit.infrastructure.AuditLogRepository;
import com.smartparking.floormap.domain.MapVersion;
import com.smartparking.floormap.dto.response.MapVersionResponse;
import com.smartparking.floormap.dto.response.FloorMapSnapshotResponse;
import com.smartparking.floormap.infrastructure.MapVersionRepository;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.occupancy.domain.OccupancyCurrent;
import com.smartparking.occupancy.domain.OccupancyState;
import com.smartparking.occupancy.infrastructure.OccupancyCurrentRepository;
import com.smartparking.parkingfloor.infrastructure.ParkingFloorRepository;
import com.smartparking.parkingspace.application.ParkingSpaceService;
import com.smartparking.streaming.application.FloorEventStreamService;
import org.springframework.context.ApplicationEventPublisher;
import java.util.UUID;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FloorMapService {
    private final ParkingFloorRepository floors;
    private final ParkingSpaceService spaces;
    private final OccupancyCurrentRepository occupancy;
    private final CameraRepository cameras;
    private final FloorEventStreamService events;
    private final MapVersionRepository drawings;
    private final AuditLogRepository audits;
    private final ApplicationEventPublisher publisher;

    public FloorMapService(ParkingFloorRepository floors, ParkingSpaceService spaces,
                           OccupancyCurrentRepository occupancy, CameraRepository cameras,
                           FloorEventStreamService events, MapVersionRepository drawings,
                           AuditLogRepository audits, ApplicationEventPublisher publisher) {
        this.floors = floors;
        this.spaces = spaces;
        this.occupancy = occupancy;
        this.cameras = cameras;
        this.events = events;
        this.drawings = drawings;
        this.audits = audits;
        this.publisher = publisher;
    }

    @Transactional
    public MapVersionResponse saveDrawing(UUID actorId, UUID floorId, long expectedVersion, byte[] content) {
        var floor = floors.findById(floorId).orElseThrow(() ->
            new FloorMapException(ErrorCode.PARKING_FLOOR_NOT_FOUND, "Parking floor not found"));
        if (expectedVersion < 0 || !Objects.equals(floor.getSpacesVersion(), expectedVersion)) {
            throw new FloorMapException(ErrorCode.VERSION_CONFLICT, "Floor map version has changed");
        }
        var validated = MapDrawingValidator.validate(content);
        if (floors.advanceSpacesVersion(floorId, expectedVersion) != 1) {
            throw new FloorMapException(ErrorCode.VERSION_CONFLICT, "Floor map version has changed");
        }
        var previous = drawings.findFirstByFloorIdOrderByVersionDesc(floorId);
        var drawing = drawings.saveAndFlush(new MapVersion(floorId, expectedVersion + 1,
            validated.mediaType(), validated.width(), validated.height(), content));
        audits.save(new AuditLog(actorId, "FLOOR_MAP", "SAVE", floorId,
            previous.map(old -> "{\"version\":" + old.getVersion() + "}").orElse(null),
            "{\"version\":" + drawing.getVersion() + "}"));
        publisher.publishEvent(new FloorDrawingChangedEvent(floorId));
        return drawingResponse(drawing);
    }

    @Transactional(readOnly = true)
    public MapVersion drawing(UUID floorId) {
        if (floors.findById(floorId).filter(floor -> floor.isActive()).isEmpty()) {
            throw new FloorMapException(ErrorCode.PARKING_FLOOR_NOT_FOUND, "Parking floor not found");
        }
        return drawings.findFirstByFloorIdOrderByVersionDesc(floorId).orElseThrow(() ->
            new FloorMapException(ErrorCode.MAP_DRAWING_NOT_FOUND, "Floor drawing not found"));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public FloorMapSnapshotResponse snapshot(UUID floorId) {
        var eventVersion = events.latestEventId(floorId);
        var floor = floors.findById(floorId).orElseThrow(() ->
            new FloorMapException(ErrorCode.PARKING_FLOOR_NOT_FOUND, "Parking floor not found"));
        if (!floor.isActive()) {
            throw new FloorMapException(ErrorCode.PARKING_FLOOR_NOT_FOUND, "Parking floor not found");
        }
        var layout = spaces.detail(floorId);
        var activeZoneIds = floor.getZones().stream().filter(zone -> zone.isActive())
            .map(zone -> zone.getId()).collect(Collectors.toSet());
        var visibleSpaces = layout.spaces().stream().filter(space -> activeZoneIds.contains(space.zoneId())).toList();
        var spaceIds = visibleSpaces.stream().map(space -> space.id()).toList();
        var observations = occupancy.findAllById(spaceIds).stream().collect(Collectors.toMap(
            OccupancyCurrent::getParkingSpaceId, Function.identity()));
        var cameraIds = observations.values().stream().map(OccupancyCurrent::getSourceCameraId).distinct().toList();
        var cameraStatuses = cameras.findAllById(cameraIds).stream().collect(Collectors.toMap(
            camera -> camera.getId(), camera -> camera.getStatus()));
        var zones = floor.getZones().stream().filter(zone -> zone.isActive()).map(zone ->
            new FloorMapSnapshotResponse.Zone(zone.getId(), zone.getName())).toList();
        var mappedSpaces = visibleSpaces.stream().map(space -> {
            var observation = observations.get(space.id());
            var known = space.active() && observation != null &&
                cameraStatuses.get(observation.getSourceCameraId()) != null &&
                cameraStatuses.get(observation.getSourceCameraId()) != CameraStatus.OFFLINE &&
                observation.getState() != OccupancyState.UNKNOWN;
            return new FloorMapSnapshotResponse.Space(space.id(), space.zoneId(), space.spaceNumber(),
                space.type(), space.active(), space.mapPolygon(),
                known ? observation.getState() : OccupancyState.UNKNOWN,
                known ? observation.getConfidence() : null,
                known ? observation.getObservedAt() : null);
        }).toList();
        return new FloorMapSnapshotResponse(floorId, floor.getParkingLot().getId(), floor.getName(),
            layout.version(), eventVersion,
            drawings.findFirstByFloorIdOrderByVersionDesc(floorId).map(this::drawingResponse).orElse(null),
            zones, mappedSpaces);
    }

    private MapVersionResponse drawingResponse(MapVersion drawing) {
        return new MapVersionResponse(drawing.getVersion(),
            "/api/v1/floors/" + drawing.getFloorId() + "/map/drawing", drawing.getMediaType(),
            drawing.getWidth(), drawing.getHeight());
    }
}
