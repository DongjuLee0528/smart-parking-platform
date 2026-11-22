package com.smartparking.installation.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItems;

import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.domain.CameraSpaceMapping;
import com.smartparking.camera.domain.CameraStatus;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.floormap.domain.MapVersion;
import com.smartparking.floormap.infrastructure.MapVersionRepository;
import com.smartparking.global.security.FirebaseTokenVerifier;
import com.smartparking.global.security.Role;
import com.smartparking.parkinglot.domain.ParkingLot;
import com.smartparking.parkinglot.dto.request.CreateParkingLotRequest;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import com.smartparking.parkingspace.domain.ParkingSpace;
import com.smartparking.parkingspace.domain.ParkingSpaceType;
import com.smartparking.parkingspace.infrastructure.ParkingSpaceRepository;
import com.smartparking.service.SmartParkingServiceApplication;
import com.smartparking.user.domain.User;
import com.smartparking.user.domain.UserStatus;
import com.smartparking.user.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = SmartParkingServiceApplication.class)
@ActiveProfiles("test")
@Transactional
class InstallationReadinessControllerTest {
    @MockitoBean FirebaseTokenVerifier verifier;
    @Autowired WebApplicationContext context;
    @Autowired ParkingLotRepository lots;
    @Autowired MapVersionRepository drawings;
    @Autowired ParkingSpaceRepository spaces;
    @Autowired CameraRepository cameras;
    @Autowired UserRepository users;
    @Autowired EntityManager entityManager;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        users.saveAndFlush(new User(null, "readiness-admin", "readiness-admin@example.test",
            "Admin", Role.ADMIN, UserStatus.ACTIVE));
        users.saveAndFlush(new User(null, "readiness-viewer", "readiness-viewer@example.test",
            "Viewer", Role.USER, UserStatus.ACTIVE));
        when(verifier.verify("admin-token")).thenReturn("readiness-admin");
        when(verifier.verify("viewer-token")).thenReturn("readiness-viewer");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void reportsMissingDrawingAndSpacesWithoutChangingSetupStatus() throws Exception {
        var lot = lot();
        mvc.perform(get("/api/v1/admin/parking-lots/{lotId}/installation-check", lot.getId())
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.ready").value(false))
            .andExpect(jsonPath("$.data.issues[0].code").value("DRAWING_MISSING"))
            .andExpect(jsonPath("$.data.issues[1].code").value("SPACE_MISSING"));
        entityManager.refresh(lot);
        org.assertj.core.api.Assertions.assertThat(lot.getSetupStatus().name()).isEqualTo("DRAFT");
    }

    @Test
    void requiresReportingCameraAndBothMapAndImageCoordinates() throws Exception {
        var lot = lot();
        var floor = lot.getFloors().get(0);
        var zone = floor.getZones().get(0);
        drawings.saveAndFlush(new MapVersion(floor.getId(), 1, "image/png", 100, 80, new byte[]{1}));
        var camera = cameras.saveAndFlush(new Camera(zone, "Entry", "CAMERA_KEY_REF"));
        var polygon = "[{\"x\":0.1,\"y\":0.1},{\"x\":0.4,\"y\":0.1},{\"x\":0.4,\"y\":0.4}]";
        spaces.saveAndFlush(new ParkingSpace(zone, "A-01", ParkingSpaceType.GENERAL, polygon, true,
            List.of(new CameraSpaceMapping(camera.getId(), polygon, 1, 1))));
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/api/v1/admin/parking-lots/{lotId}/installation-check", lot.getId())
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.ready").value(false))
            .andExpect(jsonPath("$.data.issues[0].code").value("CAMERA_NOT_REPORTING"));

        ReflectionTestUtils.setField(camera, "status", CameraStatus.ONLINE);
        ReflectionTestUtils.setField(camera, "lastFrameAt", Instant.parse("2026-11-21T00:00:00Z"));
        cameras.saveAndFlush(camera);
        entityManager.clear();
        mvc.perform(get("/api/v1/admin/parking-lots/{lotId}/installation-check", lot.getId())
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.ready").value(true))
            .andExpect(jsonPath("$.data.issues.length()").value(0));
    }

    @Test
    void reportsMissingNumberAndBothCoordinateLinks() throws Exception {
        var lot = lot();
        var floor = lot.getFloors().get(0);
        var zone = floor.getZones().get(0);
        drawings.saveAndFlush(new MapVersion(floor.getId(), 1, "image/png", 100, 80, new byte[]{1}));
        var camera = cameras.saveAndFlush(new Camera(zone, "Entry", "CAMERA_KEY_REF"));
        spaces.saveAndFlush(new ParkingSpace(zone, "", ParkingSpaceType.GENERAL, "", true,
            List.of(new CameraSpaceMapping(camera.getId(), "", 1, 1))));
        entityManager.clear();

        mvc.perform(get("/api/v1/admin/parking-lots/{lotId}/installation-check", lot.getId())
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.ready").value(false))
            .andExpect(jsonPath("$.data.issues[*].code", hasItems(
                "SPACE_NUMBER_MISSING", "MAP_POLYGON_MISSING", "IMAGE_POLYGON_MISSING")));
    }

    @Test
    void onlyAdminCanCheckAndUnknownLotUsesStandardError() throws Exception {
        var path = "/api/v1/admin/parking-lots/" + UUID.randomUUID() + "/installation-check";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isForbidden());
        mvc.perform(get(path).header("Authorization", "Bearer admin-token"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PARKING_LOT_NOT_FOUND"))
            .andExpect(jsonPath("$.traceId").isString());
    }

    private ParkingLot lot() {
        return lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("Lot", "Address", 37.5, 127.0,
            "", "", List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A"))))));
    }
}
