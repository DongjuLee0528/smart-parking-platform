package com.smartparking.installation.application;

import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.domain.CameraStatus;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.floormap.infrastructure.MapVersionRepository;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.installation.dto.response.InstallationReadinessResponse;
import com.smartparking.parkinglot.application.ParkingLotException;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import com.smartparking.parkingspace.infrastructure.ParkingSpaceRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class InstallationReadinessService {
    private final ParkingLotRepository lots;
    private final MapVersionRepository drawings;
    private final ParkingSpaceRepository spaces;
    private final CameraRepository cameras;

    public InstallationReadinessService(ParkingLotRepository lots, MapVersionRepository drawings,
                                        ParkingSpaceRepository spaces, CameraRepository cameras) {
        this.lots = lots;
        this.drawings = drawings;
        this.spaces = spaces;
        this.cameras = cameras;
    }

    public InstallationReadinessResponse check(UUID lotId) {
        var lot = lots.findById(lotId).orElseThrow(() ->
            new ParkingLotException(ErrorCode.PARKING_LOT_NOT_FOUND, "Parking lot not found"));
        var issues = new ArrayList<InstallationReadinessResponse.Issue>();
        var activeFloors = lot.getFloors().stream().filter(floor -> floor.isActive()).toList();
        if (activeFloors.isEmpty()) issue(issues, "FLOOR_MISSING", null, null);

        for (var floor : activeFloors) {
            var floorId = floor.getId();
            if (!drawings.existsByFloorId(floorId)) issue(issues, "DRAWING_MISSING", floorId, null);
            var activeZones = new HashSet<UUID>();
            floor.getZones().stream().filter(zone -> zone.isActive())
                .forEach(zone -> activeZones.add(zone.getId()));
            if (activeZones.isEmpty()) issue(issues, "ZONE_MISSING", floorId, null);

            var floorSpaces = spaces.findByZoneFloorIdOrderBySpaceNumberAsc(floorId).stream()
                .filter(space -> space.isActive()).toList();
            if (floorSpaces.stream().noneMatch(space -> activeZones.contains(space.getZone().getId()))) {
                issue(issues, "SPACE_MISSING", floorId, null);
            }
            var cameraIds = floorSpaces.stream().flatMap(space -> space.getCameraMappings().stream())
                .map(mapping -> mapping.getCameraId()).distinct().toList();
            var byCameraId = new HashMap<UUID, Camera>();
            cameras.findAllById(cameraIds).forEach(camera -> byCameraId.put(camera.getId(), camera));
            for (var space : floorSpaces) {
                var spaceId = space.getId();
                if (!activeZones.contains(space.getZone().getId())) {
                    issue(issues, "SPACE_ZONE_INACTIVE", floorId, spaceId);
                }
                if (space.getSpaceNumber() == null || space.getSpaceNumber().isBlank()) {
                    issue(issues, "SPACE_NUMBER_MISSING", floorId, spaceId);
                }
                if (space.getType() == null) issue(issues, "SPACE_TYPE_MISSING", floorId, spaceId);
                if (space.getMapPolygon() == null || space.getMapPolygon().isBlank()) {
                    issue(issues, "MAP_POLYGON_MISSING", floorId, spaceId);
                }
                if (space.getCameraMappings().isEmpty()) {
                    issue(issues, "CAMERA_MAPPING_MISSING", floorId, spaceId);
                }
                for (var mapping : space.getCameraMappings()) {
                    var camera = byCameraId.get(mapping.getCameraId());
                    if (camera == null || !camera.getZone().getFloor().getId().equals(floorId)
                        || !camera.getZone().isActive()) {
                        issue(issues, "CAMERA_INVALID", floorId, spaceId);
                    } else if (camera.getStatus() == CameraStatus.OFFLINE || camera.getLastFrameAt() == null) {
                        issue(issues, "CAMERA_NOT_REPORTING", floorId, spaceId);
                    }
                    if (mapping.getImagePolygon() == null || mapping.getImagePolygon().isBlank()) {
                        issue(issues, "IMAGE_POLYGON_MISSING", floorId, spaceId);
                    }
                }
            }
        }
        return new InstallationReadinessResponse(lotId, issues.isEmpty(), issues);
    }

    private void issue(ArrayList<InstallationReadinessResponse.Issue> issues,
                       String code, UUID floorId, UUID spaceId) {
        issues.add(new InstallationReadinessResponse.Issue(code, floorId, spaceId));
    }
}
