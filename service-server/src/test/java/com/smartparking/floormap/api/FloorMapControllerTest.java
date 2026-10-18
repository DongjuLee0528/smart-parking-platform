package com.smartparking.floormap.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.domain.CameraStatus;
import com.smartparking.camera.infrastructure.CameraRepository;
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
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        users.saveAndFlush(new User(null, "map-viewer", "map-viewer@example.com",
            "Viewer", Role.USER, UserStatus.ACTIVE));
        when(verifier.verify("viewer-token")).thenReturn("map-viewer");
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

    private ParkingLot lot() {
        return lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("Lot", "Address",
            37.5, 127.0, "", "", List.of(new CreateParkingLotRequest.Floor("B1", -1,
                List.of("A"))))));
    }
}
