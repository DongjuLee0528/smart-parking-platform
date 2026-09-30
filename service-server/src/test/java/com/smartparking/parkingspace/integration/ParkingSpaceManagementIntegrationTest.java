package com.smartparking.parkingspace.integration;

import static org.assertj.core.api.Assertions.*;

import com.smartparking.audit.infrastructure.AuditLogRepository;
import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.global.security.FirebaseTokenVerifier;
import com.smartparking.global.security.Role;
import com.smartparking.parkinglot.domain.ParkingLot;
import com.smartparking.parkinglot.dto.request.CreateParkingLotRequest;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import com.smartparking.parkingspace.application.ParkingSpaceException;
import com.smartparking.parkingspace.application.ParkingSpaceService;
import com.smartparking.parkingspace.domain.ParkingSpaceType;
import com.smartparking.parkingspace.dto.NormalizedPoint;
import com.smartparking.parkingspace.dto.request.SaveParkingSpacesRequest;
import com.smartparking.service.SmartParkingServiceApplication;
import com.smartparking.user.domain.User;
import com.smartparking.user.domain.UserStatus;
import com.smartparking.user.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = SmartParkingServiceApplication.class)
@ActiveProfiles("test")
class ParkingSpaceManagementIntegrationTest {
    @MockitoBean FirebaseTokenVerifier firebaseTokenVerifier;
    @Autowired ParkingSpaceService service;
    @Autowired ParkingLotRepository lots;
    @Autowired CameraRepository cameras;
    @Autowired UserRepository users;
    @Autowired AuditLogRepository audits;
    @Autowired EntityManager entityManager;

    private static List<NormalizedPoint> polygon() {
        return List.of(new NormalizedPoint(0.1, 0.1), new NormalizedPoint(0.4, 0.1),
            new NormalizedPoint(0.4, 0.4));
    }

    private static SaveParkingSpacesRequest.Item item(UUID id, UUID zoneId,
                                                        List<SaveParkingSpacesRequest.Mapping> mappings) {
        return new SaveParkingSpacesRequest.Item(id, zoneId, "A-01", ParkingSpaceType.GENERAL,
            true, polygon(), mappings);
    }

    @Test
    @Transactional
    void savesMappedSpaceUpdatesItAndSoftDeactivatesOmissions() {
        var actor = users.saveAndFlush(new User(null, "space-admin-" + UUID.randomUUID(),
            UUID.randomUUID() + "@example.test", "Admin", Role.ADMIN, UserStatus.ACTIVE));
        var lot = lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("Lot", "Address", 37.5, 127.0,
            "", "", List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A"))))));
        var floorId = lot.getFloors().get(0).getId();
        var zoneId = lot.getFloors().get(0).getZones().get(0).getId();
        var camera = cameras.saveAndFlush(new Camera(lot.getFloors().get(0).getZones().get(0),
            "Entrance", "CAMERA_ENTRANCE_RTSP"));
        var backup = cameras.saveAndFlush(new Camera(lot.getFloors().get(0).getZones().get(0),
            "Backup", "CAMERA_BACKUP_RTSP"));
        var mapping = new SaveParkingSpacesRequest.Mapping(camera.getId(), 1, polygon());
        var backupMapping = new SaveParkingSpacesRequest.Mapping(backup.getId(), 2, polygon());

        var created = service.save(actor.getId(), floorId,
            new SaveParkingSpacesRequest(0L, List.of(item(null, zoneId, List.of(mapping, backupMapping)))));
        assertThat(created.version()).isEqualTo(1);
        assertThat(created.spaces()).hasSize(1);
        var id = created.spaces().get(0).id();
        assertThat(created.spaces().get(0).mapPolygon()).isEqualTo(polygon());
        assertThat(created.spaces().get(0).cameraMappings().get(0).cameraId()).isEqualTo(camera.getId());
        entityManager.flush();
        entityManager.clear();

        var reloaded = service.detail(floorId);
        assertThat(reloaded.version()).isEqualTo(1);
        assertThat(reloaded.spaces().get(0).id()).isEqualTo(id);
        assertThat(reloaded.spaces().get(0).cameraMappings()).hasSize(2);
        var updated = service.save(actor.getId(), floorId,
            new SaveParkingSpacesRequest(1L, List.of(item(id, zoneId, List.of()))));
        assertThat(updated.version()).isEqualTo(2);
        assertThat(updated.spaces().get(0).cameraMappings()).isEmpty();
        var deactivated = service.save(actor.getId(), floorId, new SaveParkingSpacesRequest(2L, List.of()));
        assertThat(deactivated.version()).isEqualTo(3);
        assertThat(deactivated.spaces().get(0).active()).isFalse();
        assertThat(audits.findAll().stream().filter(audit -> audit.getEntityType().equals("PARKING_SPACE")))
            .hasSize(3);
    }

    @Test
    @Transactional
    void rejectsStaleVersionAndForeignZone() {
        var first = lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("First", "Address", 37.5, 127.0,
            "", "", List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A"))))));
        var second = lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("Second", "Address", 37.5, 127.0,
            "", "", List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A"))))));
        var floorId = first.getFloors().get(0).getId();
        var otherZoneId = second.getFloors().get(0).getZones().get(0).getId();

        assertThatThrownBy(() -> service.save(UUID.randomUUID(), floorId,
            new SaveParkingSpacesRequest(1L, List.of())))
            .isInstanceOfSatisfying(ParkingSpaceException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));
        assertThatThrownBy(() -> service.save(UUID.randomUUID(), floorId,
            new SaveParkingSpacesRequest(0L, List.of(item(null, otherZoneId, List.of())))))
            .isInstanceOfSatisfying(ParkingSpaceException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    @Transactional
    void rejectsZeroAreaPolygon() {
        var lot = lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("Polygon lot", "Address", 37.5, 127.0,
            "", "", List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A"))))));
        var floorId = lot.getFloors().get(0).getId();
        var zoneId = lot.getFloors().get(0).getZones().get(0).getId();
        var invalid = new SaveParkingSpacesRequest.Item(null, zoneId, "A-01", ParkingSpaceType.GENERAL, true,
            List.of(new NormalizedPoint(0.1, 0.1), new NormalizedPoint(0.2, 0.2),
                new NormalizedPoint(0.3, 0.3)), List.of());

        assertThatThrownBy(() -> service.save(UUID.randomUUID(), floorId,
            new SaveParkingSpacesRequest(0L, List.of(invalid))))
            .isInstanceOfSatisfying(ParkingSpaceException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    @Transactional
    void rejectsCameraOnAnotherFloor() {
        var first = lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("First camera lot", "Address",
            37.5, 127.0, "", "", List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A"))))));
        var second = lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("Second camera lot", "Address",
            37.5, 127.0, "", "", List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A"))))));
        var camera = cameras.saveAndFlush(new Camera(second.getFloors().get(0).getZones().get(0),
            "Other floor", "CAMERA_OTHER_RTSP"));
        var floorId = first.getFloors().get(0).getId();
        var zoneId = first.getFloors().get(0).getZones().get(0).getId();
        var mapping = new SaveParkingSpacesRequest.Mapping(camera.getId(), 1, polygon());

        assertThatThrownBy(() -> service.save(UUID.randomUUID(), floorId,
            new SaveParkingSpacesRequest(0L, List.of(item(null, zoneId, List.of(mapping))))))
            .isInstanceOfSatisfying(ParkingSpaceException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }
}
