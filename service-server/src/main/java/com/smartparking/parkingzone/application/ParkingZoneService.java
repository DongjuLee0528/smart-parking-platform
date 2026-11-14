package com.smartparking.parkingzone.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartparking.audit.domain.AuditLog;
import com.smartparking.audit.infrastructure.AuditLogRepository;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.parkingfloor.infrastructure.ParkingFloorRepository;
import com.smartparking.parkinglot.application.ParkingLotException;
import com.smartparking.parkingzone.domain.ParkingZone;
import com.smartparking.parkingzone.dto.request.SaveParkingZoneRequest;
import com.smartparking.parkingzone.dto.response.ParkingZoneResponse;
import com.smartparking.parkingzone.infrastructure.ParkingZoneRepository;
import com.smartparking.parkingspace.application.ParkingSpacesChangedEvent;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ParkingZoneService {
    private final ParkingZoneRepository zones;
    private final ParkingFloorRepository floors;
    private final AuditLogRepository audits;
    private final ApplicationEventPublisher events;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public ParkingZoneService(ParkingZoneRepository zones, ParkingFloorRepository floors,
                              AuditLogRepository audits, ApplicationEventPublisher events) {
        this.zones = zones;
        this.floors = floors;
        this.audits = audits;
        this.events = events;
    }

    @Transactional
    public ParkingZoneResponse create(UUID actorId, UUID floorId, SaveParkingZoneRequest request) {
        var floor = floors.findById(floorId).orElseThrow(() ->
            new ParkingLotException(ErrorCode.PARKING_FLOOR_NOT_FOUND, "Parking floor not found"));
        if (request.version() != null || request.active() != null) {
            throw new ParkingLotException(ErrorCode.VALIDATION_FAILED, "Version and active are not allowed on create");
        }
        var name = request.name().strip();
        if (name.isEmpty()) {
            throw new ParkingLotException(ErrorCode.VALIDATION_FAILED, "Zone name is required");
        }
        if (zones.existsByFloorIdAndNameAndIdNot(floorId, name, new UUID(0, 0))) {
            throw new ParkingLotException(ErrorCode.VERSION_CONFLICT, "Zone name already exists");
        }
        var zone = zones.saveAndFlush(new ParkingZone(floor, name));
        var response = ParkingZoneResponse.from(zone);
        audits.save(new AuditLog(actorId, "PARKING_ZONE", "CREATE", zone.getId(), null, json(response)));
        return response;
    }

    @Transactional
    public ParkingZoneResponse update(UUID actorId, UUID zoneId, SaveParkingZoneRequest request) {
        var zone = zones.findById(zoneId).orElseThrow(() ->
            new ParkingLotException(ErrorCode.PARKING_ZONE_NOT_FOUND, "Parking zone not found"));
        if (request.version() == null || request.active() == null) {
            throw new ParkingLotException(ErrorCode.VALIDATION_FAILED, "Version and active are required");
        }
        if (!Objects.equals(zone.getVersion(), request.version())) {
            throw new ParkingLotException(ErrorCode.VERSION_CONFLICT, "Parking zone version has changed");
        }
        var name = request.name().strip();
        if (name.isEmpty()) {
            throw new ParkingLotException(ErrorCode.VALIDATION_FAILED, "Zone name is required");
        }
        if (zones.existsByFloorIdAndNameAndIdNot(zone.getFloor().getId(), name, zoneId)) {
            throw new ParkingLotException(ErrorCode.VERSION_CONFLICT, "Zone name already exists");
        }
        var before = json(ParkingZoneResponse.from(zone));
        boolean wasActive = zone.isActive();
        zone.update(name, request.active());
        zones.flush();
        var response = ParkingZoneResponse.from(zone);
        String action = wasActive == request.active() ? "UPDATE" : request.active() ? "REACTIVATE" : "DEACTIVATE";
        audits.save(new AuditLog(actorId, "PARKING_ZONE", action,
            zoneId, before, json(response)));
        events.publishEvent(new ParkingSpacesChangedEvent(zone.getFloor().getId()));
        return response;
    }

    private String json(ParkingZoneResponse response) {
        try {
            return mapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize zone audit snapshot", exception);
        }
    }
}
