package com.smartparking.parkingfloor.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartparking.audit.domain.AuditLog;
import com.smartparking.audit.infrastructure.AuditLogRepository;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.parkingfloor.domain.ParkingFloor;
import com.smartparking.parkingfloor.dto.request.SaveParkingFloorRequest;
import com.smartparking.parkingfloor.dto.response.ParkingFloorResponse;
import com.smartparking.parkingfloor.infrastructure.ParkingFloorRepository;
import com.smartparking.parkinglot.application.ParkingLotException;
import com.smartparking.parkinglot.domain.OperationStatus;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ParkingFloorService {
    private final ParkingFloorRepository floors;
    private final ParkingLotRepository lots;
    private final AuditLogRepository audits;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public ParkingFloorService(ParkingFloorRepository floors, ParkingLotRepository lots, AuditLogRepository audits) {
        this.floors = floors;
        this.lots = lots;
        this.audits = audits;
    }

    public List<ParkingFloorResponse> list(UUID lotId, boolean admin) {
        var lot = lots.findById(lotId).orElseThrow(() ->
            new ParkingLotException(ErrorCode.PARKING_LOT_NOT_FOUND, "Parking lot not found"));
        if (!admin && lot.getOperationStatus() != OperationStatus.ACTIVE) {
            throw new ParkingLotException(ErrorCode.PARKING_LOT_NOT_FOUND, "Parking lot not found");
        }
        return floors.findByParkingLotIdOrderByFloorOrderAscNameAsc(lotId).stream()
            .filter(floor -> admin || floor.isActive())
            .map(floor -> ParkingFloorResponse.from(floor, admin)).toList();
    }

    @Transactional
    public ParkingFloorResponse create(UUID actorId, UUID lotId, SaveParkingFloorRequest request) {
        var lot = lots.findById(lotId).orElseThrow(() ->
            new ParkingLotException(ErrorCode.PARKING_LOT_NOT_FOUND, "Parking lot not found"));
        if (request.version() != null || request.active() != null) {
            throw new ParkingLotException(ErrorCode.VALIDATION_FAILED, "Version and active are not allowed on create");
        }
        var name = request.name().strip();
        if (name.isEmpty()) {
            throw new ParkingLotException(ErrorCode.VALIDATION_FAILED, "Floor name is required");
        }
        if (floors.existsByParkingLotIdAndNameAndIdNot(lotId, name, new UUID(0, 0))) {
            throw new ParkingLotException(ErrorCode.VERSION_CONFLICT, "Floor name already exists");
        }
        var floor = floors.saveAndFlush(new ParkingFloor(lot, name, request.floorOrder()));
        var response = ParkingFloorResponse.from(floor, true);
        audits.save(new AuditLog(actorId, "PARKING_FLOOR", "CREATE", floor.getId(), null, json(response)));
        return response;
    }

    @Transactional
    public ParkingFloorResponse update(UUID actorId, UUID floorId, SaveParkingFloorRequest request) {
        var floor = floors.findById(floorId).orElseThrow(() ->
            new ParkingLotException(ErrorCode.PARKING_FLOOR_NOT_FOUND, "Parking floor not found"));
        if (request.version() == null || request.active() == null) {
            throw new ParkingLotException(ErrorCode.VALIDATION_FAILED, "Version and active are required");
        }
        if (!Objects.equals(floor.getVersion(), request.version())) {
            throw new ParkingLotException(ErrorCode.VERSION_CONFLICT, "Parking floor version has changed");
        }
        var name = request.name().strip();
        if (name.isEmpty()) {
            throw new ParkingLotException(ErrorCode.VALIDATION_FAILED, "Floor name is required");
        }
        if (floors.existsByParkingLotIdAndNameAndIdNot(floor.getParkingLot().getId(), name, floorId)) {
            throw new ParkingLotException(ErrorCode.VERSION_CONFLICT, "Floor name already exists");
        }
        var before = json(ParkingFloorResponse.from(floor, true));
        floor.update(name, request.floorOrder(), request.active());
        floors.flush();
        var response = ParkingFloorResponse.from(floor, true);
        audits.save(new AuditLog(actorId, "PARKING_FLOOR", request.active() ? "UPDATE" : "DEACTIVATE",
            floorId, before, json(response)));
        return response;
    }

    private String json(ParkingFloorResponse response) {
        try {
            return mapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize floor audit snapshot", exception);
        }
    }
}
