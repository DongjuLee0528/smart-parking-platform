package com.smartparking.parkinglot.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartparking.audit.domain.AuditLog;
import com.smartparking.audit.infrastructure.AuditLogRepository;
import com.smartparking.parkinglot.domain.*;
import com.smartparking.parkinglot.dto.request.*;
import com.smartparking.parkinglot.dto.response.*;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import com.smartparking.global.error.ErrorCode;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ParkingLotService {
    private final ParkingLotRepository repository;
    private final AuditLogRepository auditRepository;
    private final JsonMapper auditMapper = JsonMapper.builder().build();

    public ParkingLotService(ParkingLotRepository repository, AuditLogRepository auditRepository) {
        this.repository = repository;
        this.auditRepository = auditRepository;
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
        return ParkingLotDetailResponse.from(repository.findByIdAndOperationStatus(id, OperationStatus.ACTIVE)
            .orElseThrow(() -> new ParkingLotException(ErrorCode.PARKING_LOT_NOT_FOUND, "Parking lot not found")));
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
