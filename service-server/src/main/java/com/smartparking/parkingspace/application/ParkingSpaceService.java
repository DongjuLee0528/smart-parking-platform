package com.smartparking.parkingspace.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.audit.domain.AuditLog;
import com.smartparking.audit.infrastructure.AuditLogRepository;
import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.domain.CameraSpaceMapping;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.parkingfloor.domain.ParkingFloor;
import com.smartparking.parkingfloor.infrastructure.ParkingFloorRepository;
import com.smartparking.parkingspace.domain.ParkingSpace;
import com.smartparking.parkingspace.dto.NormalizedPoint;
import com.smartparking.parkingspace.dto.request.SaveParkingSpacesRequest;
import com.smartparking.parkingspace.dto.response.ParkingSpaceResponse;
import com.smartparking.parkingspace.dto.response.ParkingSpacesResponse;
import com.smartparking.parkingspace.infrastructure.ParkingSpaceRepository;
import com.smartparking.parkingzone.domain.ParkingZone;
import com.smartparking.parkingzone.infrastructure.ParkingZoneRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ParkingSpaceService {
    private final ParkingFloorRepository floors;
    private final ParkingZoneRepository zones;
    private final CameraRepository cameras;
    private final ParkingSpaceRepository spaces;
    private final AuditLogRepository audits;
    private final EntityManager entityManager;
    private final ObjectMapper mapper;

    public ParkingSpaceService(ParkingFloorRepository floors, ParkingZoneRepository zones,
                               CameraRepository cameras, ParkingSpaceRepository spaces,
                               AuditLogRepository audits, EntityManager entityManager, ObjectMapper mapper) {
        this.floors = floors;
        this.zones = zones;
        this.cameras = cameras;
        this.spaces = spaces;
        this.audits = audits;
        this.entityManager = entityManager;
        this.mapper = mapper;
    }

    @Transactional
    public ParkingSpacesResponse save(UUID actorId, UUID floorId, SaveParkingSpacesRequest request) {
        var floor = requireFloor(floorId);
        if (!Objects.equals(floor.getSpacesVersion(), request.version())) {
            throw new ParkingSpaceException(ErrorCode.VERSION_CONFLICT, "Parking spaces version has changed");
        }
        var current = spaces.findByZoneFloorIdOrderBySpaceNumberAsc(floorId);
        var before = summary(request.version(), current);
        var existing = new HashMap<UUID, ParkingSpace>();
        current.forEach(space -> existing.put(space.getId(), space));
        var seenIds = new HashSet<UUID>();
        var numbers = new HashSet<String>();
        var zoneCache = new HashMap<UUID, ParkingZone>();
        var cameraCache = new HashMap<UUID, Camera>();
        var saved = new ArrayList<ParkingSpace>();
        for (var item : request.spaces()) {
            var zone = zoneCache.computeIfAbsent(item.zoneId(), id -> requireZone(id, floorId));
            var key = item.zoneId() + ":" + item.spaceNumber().strip().toLowerCase(java.util.Locale.ROOT);
            if (!numbers.add(key)) {
                throw new ParkingSpaceException(ErrorCode.VALIDATION_FAILED, "Duplicate space number in zone");
            }
            var mappings = new ArrayList<CameraSpaceMapping>();
            var mappedCameras = new HashSet<UUID>();
            var priorities = new HashSet<Integer>();
            for (var mapping : item.cameraMappings()) {
                if (!mappedCameras.add(mapping.cameraId()) || !priorities.add(mapping.priority())) {
                    throw new ParkingSpaceException(ErrorCode.VALIDATION_FAILED, "Duplicate camera or priority");
                }
                var camera = cameraCache.computeIfAbsent(mapping.cameraId(), this::requireCamera);
                if (!camera.getZone().getFloor().getId().equals(floorId)) {
                    throw new ParkingSpaceException(ErrorCode.VALIDATION_FAILED, "Camera belongs to another floor");
                }
                mappings.add(new CameraSpaceMapping(camera.getId(), polygon(mapping.imagePolygon()),
                    mapping.priority(), request.version() + 1));
            }
            ParkingSpace space;
            if (item.id() == null) {
                space = new ParkingSpace(zone, item.spaceNumber(), item.type(),
                    polygon(item.mapPolygon()), item.active(), mappings);
            } else {
                space = existing.get(item.id());
                if (space == null || !seenIds.add(item.id())) {
                    throw new ParkingSpaceException(ErrorCode.PARKING_SPACE_NOT_FOUND,
                        "Parking space not found on floor or listed twice");
                }
                space.configure(zone, item.spaceNumber(), item.type(), polygon(item.mapPolygon()),
                    item.active(), mappings);
            }
            saved.add(space);
        }
        current.stream().filter(space -> !seenIds.contains(space.getId())).forEach(ParkingSpace::deactivate);
        if (floors.advanceSpacesVersion(floorId, request.version()) != 1) {
            throw new ParkingSpaceException(ErrorCode.VERSION_CONFLICT, "Parking spaces version has changed");
        }
        spaces.saveAllAndFlush(saved);
        entityManager.refresh(floor);
        var latest = spaces.findByZoneFloorIdOrderBySpaceNumberAsc(floorId);
        var result = response(floorId, floor.getSpacesVersion(), latest);
        audits.save(new AuditLog(actorId, "PARKING_SPACE", "SAVE", floorId, before,
            summary(result.version(), latest)));
        return result;
    }

    public ParkingSpacesResponse detail(UUID floorId) {
        var floor = requireFloor(floorId);
        return response(floorId, floor.getSpacesVersion(), spaces.findByZoneFloorIdOrderBySpaceNumberAsc(floorId));
    }

    private ParkingFloor requireFloor(UUID id) {
        return floors.findById(id).orElseThrow(() ->
            new ParkingSpaceException(ErrorCode.PARKING_FLOOR_NOT_FOUND, "Parking floor not found"));
    }

    private ParkingZone requireZone(UUID id, UUID floorId) {
        var zone = zones.findById(id).orElseThrow(() ->
            new ParkingSpaceException(ErrorCode.VALIDATION_FAILED, "Parking zone does not exist"));
        if (!zone.getFloor().getId().equals(floorId)) {
            throw new ParkingSpaceException(ErrorCode.VALIDATION_FAILED, "Parking zone belongs to another floor");
        }
        return zone;
    }

    private Camera requireCamera(UUID id) {
        return cameras.findById(id).orElseThrow(() ->
            new ParkingSpaceException(ErrorCode.VALIDATION_FAILED, "Camera does not exist"));
    }

    private ParkingSpacesResponse response(UUID floorId, Long version, List<ParkingSpace> spaces) {
        return new ParkingSpacesResponse(floorId, version, spaces.stream().map(space ->
            new ParkingSpaceResponse(space.getId(), space.getZone().getId(), space.getSpaceNumber(),
                space.getType(), space.isActive(), points(space.getMapPolygon()),
                space.getCameraMappings().stream().map(mapping -> new ParkingSpaceResponse.CameraMapping(
                    mapping.getCameraId(), mapping.getPriority(), points(mapping.getImagePolygon()))).toList()))
            .toList());
    }

    private String polygon(List<NormalizedPoint> points) {
        double area = 0;
        if (points.size() < 3) {
            throw new ParkingSpaceException(ErrorCode.VALIDATION_FAILED, "Polygon needs at least three points");
        }
        for (int index = 0; index < points.size(); index++) {
            var point = points.get(index);
            var next = points.get((index + 1) % points.size());
            if (point == null || next == null || point.x() == null || point.y() == null ||
                next.x() == null || next.y() == null || !Double.isFinite(point.x()) ||
                !Double.isFinite(point.y()) || point.x() < 0 || point.x() > 1 || point.y() < 0 || point.y() > 1) {
                throw new ParkingSpaceException(ErrorCode.VALIDATION_FAILED, "Polygon coordinates must be between 0 and 1");
            }
            area += point.x() * next.y() - next.x() * point.y();
        }
        if (Math.abs(area) <= 1e-12) {
            throw new ParkingSpaceException(ErrorCode.VALIDATION_FAILED, "Polygon must have an area");
        }
        return json(points);
    }

    private List<NormalizedPoint> points(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<NormalizedPoint>>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read parking space polygon", exception);
        }
    }

    private String summary(Long version, List<ParkingSpace> spaces) {
        return json(Map.of("version", version, "spaceIds", spaces.stream().map(ParkingSpace::getId).toList()));
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize parking space data", exception);
        }
    }
}
