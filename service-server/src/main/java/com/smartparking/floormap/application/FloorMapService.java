package com.smartparking.floormap.application;

import com.smartparking.camera.domain.CameraStatus;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.floormap.dto.response.FloorMapSnapshotResponse;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.occupancy.domain.OccupancyCurrent;
import com.smartparking.occupancy.domain.OccupancyState;
import com.smartparking.occupancy.infrastructure.OccupancyCurrentRepository;
import com.smartparking.parkingfloor.infrastructure.ParkingFloorRepository;
import com.smartparking.parkingspace.application.ParkingSpaceService;
import com.smartparking.streaming.application.FloorEventStreamService;
import java.util.UUID;
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

    public FloorMapService(ParkingFloorRepository floors, ParkingSpaceService spaces,
                           OccupancyCurrentRepository occupancy, CameraRepository cameras,
                           FloorEventStreamService events) {
        this.floors = floors;
        this.spaces = spaces;
        this.occupancy = occupancy;
        this.cameras = cameras;
        this.events = events;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public FloorMapSnapshotResponse snapshot(UUID floorId) {
        var eventVersion = events.latestEventId(floorId);
        var floor = floors.findById(floorId).orElseThrow(() ->
            new FloorMapException(ErrorCode.PARKING_FLOOR_NOT_FOUND, "Parking floor not found"));
        var layout = spaces.detail(floorId);
        var spaceIds = layout.spaces().stream().map(space -> space.id()).toList();
        var observations = occupancy.findAllById(spaceIds).stream().collect(Collectors.toMap(
            OccupancyCurrent::getParkingSpaceId, Function.identity()));
        var cameraIds = observations.values().stream().map(OccupancyCurrent::getSourceCameraId).distinct().toList();
        var cameraStatuses = cameras.findAllById(cameraIds).stream().collect(Collectors.toMap(
            camera -> camera.getId(), camera -> camera.getStatus()));
        var zones = floor.getZones().stream().map(zone ->
            new FloorMapSnapshotResponse.Zone(zone.getId(), zone.getName())).toList();
        var mappedSpaces = layout.spaces().stream().map(space -> {
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
            layout.version(), eventVersion, zones, mappedSpaces);
    }
}
