package com.smartparking.floormap.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.domain.CameraStatus;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.audit.infrastructure.AuditLogRepository;
import com.smartparking.floormap.infrastructure.MapVersionRepository;
import com.smartparking.global.security.FirebaseTokenVerificationException;
import com.smartparking.global.security.FirebaseTokenVerifier;
import com.smartparking.global.security.Role;
import com.smartparking.occupancy.domain.OccupancyCurrent;
import com.smartparking.occupancy.domain.OccupancyState;
import com.smartparking.occupancy.application.OccupancyChangedEvent;
import com.smartparking.occupancy.dto.response.OccupancyStateResponse;
import com.smartparking.occupancy.infrastructure.OccupancyCurrentRepository;
import com.smartparking.parkingfloor.infrastructure.ParkingFloorRepository;
import com.smartparking.parkinglot.domain.ParkingLot;
import com.smartparking.parkinglot.dto.request.CreateParkingLotRequest;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import com.smartparking.parkingspace.domain.ParkingSpace;
import com.smartparking.parkingspace.domain.ParkingSpaceType;
import com.smartparking.parkingspace.infrastructure.ParkingSpaceRepository;
import com.smartparking.service.SmartParkingServiceApplication;
import com.smartparking.streaming.application.FloorEventStreamService;
import com.smartparking.user.domain.User;
import com.smartparking.user.domain.UserStatus;
import com.smartparking.user.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = SmartParkingServiceApplication.class)
@ActiveProfiles("test")
@Transactional
class FloorMapControllerTest {
    @MockitoBean FirebaseTokenVerifier verifier;
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired ParkingLotRepository lots;
    @Autowired ParkingFloorRepository floors;
    @Autowired ParkingSpaceRepository spaces;
    @Autowired CameraRepository cameras;
    @Autowired OccupancyCurrentRepository occupancy;
    @Autowired EntityManager entityManager;
    @Autowired FloorEventStreamService events;
    @Autowired MapVersionRepository drawings;
    @Autowired AuditLogRepository audits;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        users.saveAndFlush(new User(null, "map-viewer", "map-viewer@example.com",
            "Viewer", Role.USER, UserStatus.ACTIVE));
        when(verifier.verify("viewer-token")).thenReturn("map-viewer");
        users.saveAndFlush(new User(null, "map-admin", "map-admin@example.com",
            "Admin", Role.ADMIN, UserStatus.ACTIVE));
        when(verifier.verify("admin-token")).thenReturn("map-admin");
        when(verifier.verify("invalid")).thenThrow(new FirebaseTokenVerificationException(
            new IllegalArgumentException("Rejected")));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void returnsVersionedLayoutAndCurrentStatesWithoutCameraSecrets() throws Exception {
        var lot = lot();
        var floor = lot.getFloors().get(0);
        var zone = floor.getZones().get(0);
        var polygon = "[{\"x\":0.1,\"y\":0.1},{\"x\":0.4,\"y\":0.1},{\"x\":0.4,\"y\":0.4}]";
        var occupied = spaces.saveAndFlush(new ParkingSpace(zone, "A-01", ParkingSpaceType.GENERAL,
            polygon, true, List.of()));
        var offline = spaces.saveAndFlush(new ParkingSpace(zone, "A-02", ParkingSpaceType.GENERAL,
            polygon, true, List.of()));
        spaces.saveAndFlush(new ParkingSpace(zone, "A-03", ParkingSpaceType.GENERAL,
            polygon, false, List.of()));
        var onlineCamera = new Camera(zone, "Online", "SECRET_STREAM_KEY_REF");
        ReflectionTestUtils.setField(onlineCamera, "status", CameraStatus.ONLINE);
        cameras.saveAndFlush(onlineCamera);
        var offlineCamera = cameras.saveAndFlush(new Camera(zone, "Offline", "OTHER_SECRET_KEY_REF"));
        var observedAt = Instant.parse("2026-10-01T00:00:00Z");
        occupancy.saveAndFlush(new OccupancyCurrent(occupied.getId(), OccupancyState.OCCUPIED,
            0.91, onlineCamera.getId(), observedAt));
        occupancy.saveAndFlush(new OccupancyCurrent(offline.getId(), OccupancyState.EMPTY,
            0.92, offlineCamera.getId(), observedAt));
        floors.advanceSpacesVersion(floor.getId(), 0L);
        entityManager.refresh(floor);

        mvc.perform(get("/api/v1/floors/{floorId}/map", floor.getId())
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.floorId").value(floor.getId().toString()))
            .andExpect(jsonPath("$.data.parkingLotId").value(lot.getId().toString()))
            .andExpect(jsonPath("$.data.mapVersion").value(1))
            .andExpect(jsonPath("$.data.eventVersion").value((Object) null))
            .andExpect(jsonPath("$.data.zones[0].name").value("A"))
            .andExpect(jsonPath("$.data.spaces.length()").value(3))
            .andExpect(jsonPath("$.data.spaces[0].mapPolygon[0].x").value(0.1))
            .andExpect(jsonPath("$.data.spaces[0].state").value("OCCUPIED"))
            .andExpect(jsonPath("$.data.spaces[0].confidence").value(0.91))
            .andExpect(jsonPath("$.data.spaces[1].state").value("UNKNOWN"))
            .andExpect(jsonPath("$.data.spaces[1].confidence").value((Object) null))
            .andExpect(jsonPath("$.data.spaces[2].active").value(false))
            .andExpect(jsonPath("$.data.spaces[2].state").value("UNKNOWN"))
            .andExpect(jsonPath("$.data.spaces[0].cameraMappings").doesNotExist())
            .andExpect(jsonPath("$.data.spaces[0].streamKeyRef").doesNotExist());
    }

    @Test
    void emptyFloorHasExplicitEmptyLayout() throws Exception {
        var floor = lot().getFloors().get(0);
        mvc.perform(get("/api/v1/floors/{floorId}/map", floor.getId())
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.mapVersion").value(0))
            .andExpect(jsonPath("$.data.zones.length()").value(1))
            .andExpect(jsonPath("$.data.spaces.length()").value(0));
    }

    @Test
    void snapshotExposesLatestEventReplayCursor() throws Exception {
        var floor = lot().getFloors().get(0);
        events.publish(new OccupancyChangedEvent(floor.getId(), new OccupancyStateResponse(
            UUID.randomUUID(), floor.getZones().get(0).getId(), "A-01", true,
            OccupancyState.EMPTY, 0.9, Instant.parse("2026-10-01T00:00:00Z"))));
        var cursor = events.latestEventId(floor.getId());
        mvc.perform(get("/api/v1/floors/{floorId}/map", floor.getId())
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.eventVersion").value(cursor));
    }

    @Test
    void requiresValidTokenAndKnownFloor() throws Exception {
        var path = "/api/v1/floors/" + UUID.randomUUID() + "/map";
        mvc.perform(get(path)).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        mvc.perform(get(path).header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PARKING_FLOOR_NOT_FOUND"))
            .andExpect(jsonPath("$.traceId").isString());
        mvc.perform(get("/api/v1/floors/not-a-uuid/map")
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void adminCanRegisterAndReplaceImmutableDrawing() throws Exception {
        var floorId = lot().getFloors().get(0).getId();
        var svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"80\"><rect width=\"100\" height=\"80\" fill=\"#fff\"/></svg>";
        var file = new MockMultipartFile("file", "plan.svg", "image/svg+xml", svg.getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/v1/admin/floors/{floorId}/map", floorId).file(file)
                .param("expectedVersion", "0").header("Authorization", "Bearer admin-token")
                .with(request -> { request.setMethod("PUT"); return request; }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.version").value(1))
            .andExpect(jsonPath("$.data.mediaType").value("image/svg+xml"));
        entityManager.clear();
        mvc.perform(get("/api/v1/floors/{floorId}/map", floorId)
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.mapVersion").value(1))
            .andExpect(jsonPath("$.data.drawing.width").value(100));
        mvc.perform(get("/api/v1/floors/{floorId}/map/drawing", floorId)
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(content().bytes(svg.getBytes(StandardCharsets.UTF_8)));
        mvc.perform(multipart("/api/v1/admin/floors/{floorId}/map", floorId).file(file)
                .param("expectedVersion", "1").header("Authorization", "Bearer admin-token")
                .with(request -> { request.setMethod("PUT"); return request; }))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(2));
        org.assertj.core.api.Assertions.assertThat(drawings.findAll()).extracting("version").containsExactlyInAnyOrder(1L, 2L);
        org.assertj.core.api.Assertions.assertThat(audits.findAll()).anySatisfy(audit ->
            org.assertj.core.api.Assertions.assertThat(audit.getEntityType()).isEqualTo("FLOOR_MAP"));
    }

    @Test
    void uploadRejectsMissingAuthWrongRoleUnsafeSvgAndStaleVersion() throws Exception {
        var floorId = lot().getFloors().get(0).getId();
        var svg = new MockMultipartFile("file", "bad.svg", "image/svg+xml",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8));
        var request = multipart("/api/v1/admin/floors/{floorId}/map", floorId).file(svg)
            .param("expectedVersion", "0").with(value -> { value.setMethod("PUT"); return value; });
        mvc.perform(request).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/v1/admin/floors/{floorId}/map", floorId).file(svg)
                .param("expectedVersion", "0").header("Authorization", "Bearer viewer-token")
                .with(value -> { value.setMethod("PUT"); return value; }))
            .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/admin/floors/{floorId}/map", floorId).file(svg)
                .param("expectedVersion", "0").header("Authorization", "Bearer admin-token")
                .with(value -> { value.setMethod("PUT"); return value; }))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        var safe = new MockMultipartFile("file", "safe.svg", "image/svg+xml",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"/>"
                .getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/v1/admin/floors/{floorId}/map", floorId).file(safe)
                .param("expectedVersion", "1").header("Authorization", "Bearer admin-token")
                .with(value -> { value.setMethod("PUT"); return value; }))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
        mvc.perform(get("/api/v1/floors/{floorId}/map/drawing", floorId)
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MAP_DRAWING_NOT_FOUND"));
        org.assertj.core.api.Assertions.assertThat(drawings.count()).isZero();
    }

    private ParkingLot lot() {
        return lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("Lot", "Address",
            37.5, 127.0, "", "", List.of(new CreateParkingLotRequest.Floor("B1", -1,
                List.of("A"))))));
    }
}
