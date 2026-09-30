package com.smartparking.camera.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.smartparking.audit.domain.AuditLog;
import com.smartparking.audit.infrastructure.AuditLogRepository;
import com.smartparking.camera.application.CameraException;
import com.smartparking.camera.application.CameraService;
import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.domain.CameraStatus;
import com.smartparking.camera.dto.request.CreateCameraRequest;
import com.smartparking.camera.dto.request.UpdateCameraConfigurationRequest;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.parkinglot.domain.ParkingLot;
import com.smartparking.parkinglot.dto.request.CreateParkingLotRequest;
import com.smartparking.parkingzone.domain.ParkingZone;
import com.smartparking.parkingzone.infrastructure.ParkingZoneRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class CameraServiceTest {
    private final CameraRepository repository = mock(CameraRepository.class);
    private final ParkingZoneRepository zones = mock(ParkingZoneRepository.class);
    private final AuditLogRepository audits = mock(AuditLogRepository.class);
    private final CameraService service = new CameraService(repository, zones, audits);
    private final UUID actorId = UUID.randomUUID();
    private final UUID zoneId = UUID.randomUUID();
    private final UUID cameraId = UUID.randomUUID();

    private ParkingZone zone() {
        var lot = new ParkingLot(new CreateParkingLotRequest("Lot", "Address", 37.5, 127.0, "", "",
            List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A")))));
        var floor = lot.getFloors().get(0);
        var zone = floor.getZones().get(0);
        ReflectionTestUtils.setField(lot, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(floor, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(zone, "id", zoneId);
        return zone;
    }

    @Test
    void createsOfflineCameraLinkedToZoneAndAuditsActor() {
        when(zones.findById(zoneId)).thenReturn(Optional.of(zone()));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            Camera camera = invocation.getArgument(0);
            ReflectionTestUtils.setField(camera, "id", cameraId);
            ReflectionTestUtils.setField(camera, "configVersion", 0L);
            return camera;
        });
        var response = service.create(actorId, new CreateCameraRequest(zoneId, " Entrance ", "CAMERA_ENTRANCE_RTSP"));
        assertThat(response.id()).isEqualTo(cameraId);
        assertThat(response.zoneId()).isEqualTo(zoneId);
        assertThat(response.status()).isEqualTo(CameraStatus.OFFLINE);
        assertThat(response.lastFrameAt()).isNull();
        assertThat(response.name()).isEqualTo("Entrance");
        var audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(audits).save(audit.capture());
        assertThat(audit.getValue().getActorId()).isEqualTo(actorId);
        assertThat(audit.getValue().getEntityType()).isEqualTo("CAMERA");
        assertThat(audit.getValue().getAfterJson()).contains("CAMERA_ENTRANCE_RTSP");
    }

    @Test
    void updateUsesVersionAndRejectsStaleRequestsWithoutAudit() {
        var camera = new Camera(zone(), "Old", "OLD_RTSP");
        ReflectionTestUtils.setField(camera, "id", cameraId);
        ReflectionTestUtils.setField(camera, "configVersion", 0L);
        when(repository.findById(cameraId)).thenReturn(Optional.of(camera));
        var stale = new UpdateCameraConfigurationRequest(2L, zoneId, "New", "NEW_RTSP");
        assertThatThrownBy(() -> service.update(actorId, cameraId, stale))
            .isInstanceOfSatisfying(CameraException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));
        verifyNoInteractions(zones, audits);

        when(zones.findById(zoneId)).thenReturn(Optional.of(camera.getZone()));
        doAnswer(invocation -> {
            ReflectionTestUtils.setField(camera, "configVersion", 1L);
            return null;
        }).when(repository).flush();
        var response = service.update(actorId, cameraId,
            new UpdateCameraConfigurationRequest(0L, zoneId, "New", "NEW_RTSP"));
        assertThat(response.configVersion()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("New");
        var audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(audits).save(audit.capture());
        assertThat(audit.getValue().getBeforeJson()).contains("Old");
        assertThat(audit.getValue().getAfterJson()).contains("New");
    }

    @Test
    void updateAuditsCameraWithLastFrameTimestamp() {
        var camera = new Camera(zone(), "Old", "OLD_RTSP");
        ReflectionTestUtils.setField(camera, "id", cameraId);
        ReflectionTestUtils.setField(camera, "configVersion", 0L);
        ReflectionTestUtils.setField(camera, "lastFrameAt", Instant.parse("2026-09-29T00:00:00Z"));
        when(repository.findById(cameraId)).thenReturn(Optional.of(camera));
        when(zones.findById(zoneId)).thenReturn(Optional.of(camera.getZone()));

        service.update(actorId, cameraId,
            new UpdateCameraConfigurationRequest(0L, zoneId, "New", "NEW_RTSP"));

        var audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(audits).save(audit.capture());
        assertThat(audit.getValue().getBeforeJson())
            .contains("\"lastFrameAt\":")
            .doesNotContain("\"lastFrameAt\":null");
    }

    @Test
    void rejectsMissingZoneAndCameraAndInvalidPaging() {
        assertThatThrownBy(() -> service.create(actorId,
            new CreateCameraRequest(zoneId, "Entrance", "CAMERA_RTSP")))
            .isInstanceOfSatisfying(CameraException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThatThrownBy(() -> service.detail(cameraId))
            .isInstanceOfSatisfying(CameraException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.CAMERA_NOT_FOUND));
        assertThatThrownBy(() -> service.list(0, 101, "name,asc")).isInstanceOf(CameraException.class);
        verifyNoInteractions(audits);
    }
}
