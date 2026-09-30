package com.smartparking.camera.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartparking.audit.domain.AuditLog;
import com.smartparking.audit.infrastructure.AuditLogRepository;
import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.dto.request.CreateCameraRequest;
import com.smartparking.camera.dto.request.UpdateCameraConfigurationRequest;
import com.smartparking.camera.dto.response.CameraPage;
import com.smartparking.camera.dto.response.CameraResponse;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.parkingzone.domain.ParkingZone;
import com.smartparking.parkingzone.infrastructure.ParkingZoneRepository;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CameraService {
    private final CameraRepository repository;
    private final ParkingZoneRepository zones;
    private final AuditLogRepository audits;
    private final JsonMapper auditMapper = JsonMapper.builder().findAndAddModules().build();

    public CameraService(CameraRepository repository, ParkingZoneRepository zones, AuditLogRepository audits) {
        this.repository = repository;
        this.zones = zones;
        this.audits = audits;
    }

    @Transactional
    public CameraResponse create(UUID actorId, CreateCameraRequest request) {
        var camera = repository.saveAndFlush(new Camera(requireZone(request.zoneId()), request.name(), request.streamKeyRef()));
        var response = CameraResponse.from(camera);
        audits.save(new AuditLog(actorId, "CAMERA", "CREATE", camera.getId(), null, json(response)));
        return response;
    }

    @Transactional
    public CameraResponse update(UUID actorId, UUID id, UpdateCameraConfigurationRequest request) {
        var camera = requireCamera(id);
        if (!Objects.equals(camera.getConfigVersion(), request.configVersion())) {
            throw new CameraException(ErrorCode.VERSION_CONFLICT, "Camera configuration version has changed");
        }
        var before = json(CameraResponse.from(camera));
        camera.configure(requireZone(request.zoneId()), request.name(), request.streamKeyRef());
        repository.flush();
        var response = CameraResponse.from(camera);
        audits.save(new AuditLog(actorId, "CAMERA", "UPDATE", id, before, json(response)));
        return response;
    }

    public CameraResponse detail(UUID id) {
        return CameraResponse.from(requireCamera(id));
    }

    public CameraPage list(int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100 || !Set.of("name,asc", "name,desc").contains(sort)) {
            throw new CameraException(ErrorCode.VALIDATION_FAILED,
                "Use page >= 0, size 1..100 and sort name,asc or name,desc");
        }
        var pageable = PageRequest.of(page, size,
            Sort.by(sort.endsWith("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, "name").and(Sort.by("id")));
        var result = repository.findAll(pageable);
        return new CameraPage(result.getContent().stream().map(CameraResponse::from).toList(),
            page, size, result.getTotalElements());
    }

    private Camera requireCamera(UUID id) {
        return repository.findById(id).orElseThrow(() ->
            new CameraException(ErrorCode.CAMERA_NOT_FOUND, "Camera not found"));
    }

    private ParkingZone requireZone(UUID id) {
        return zones.findById(id).orElseThrow(() ->
            new CameraException(ErrorCode.VALIDATION_FAILED, "Parking zone does not exist"));
    }

    private String json(CameraResponse response) {
        try {
            return auditMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize camera audit snapshot", exception);
        }
    }
}
