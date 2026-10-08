package com.smartparking.occupancy.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.global.security.FirebaseTokenVerificationException;
import com.smartparking.global.security.FirebaseTokenVerifier;
import com.smartparking.global.security.Role;
import com.smartparking.occupancy.domain.OccupancyCurrent;
import com.smartparking.occupancy.domain.OccupancyState;
import com.smartparking.occupancy.infrastructure.OccupancyCurrentRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = SmartParkingServiceApplication.class)
@ActiveProfiles("test")
@Transactional
class OccupancyControllerTest {
    @MockitoBean FirebaseTokenVerifier verifier;
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired ParkingLotRepository lots;
    @Autowired ParkingSpaceRepository spaces;
    @Autowired CameraRepository cameras;
    @Autowired OccupancyCurrentRepository current;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        users.saveAndFlush(new User(null, "occupancy-viewer", "occupancy-viewer@example.com",
            "Viewer", Role.USER, UserStatus.ACTIVE));
        when(verifier.verify("viewer-token")).thenReturn("occupancy-viewer");
        when(verifier.verify("invalid")).thenThrow(new FirebaseTokenVerificationException(
            new IllegalArgumentException("Rejected")));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void returnsCompleteLatestFloorStateAndRefreshesAfterNewObservation() throws Exception {
        var lot = lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("Lot", "Address",
            37.5, 127.0, "", "", List.of(
                new CreateParkingLotRequest.Floor("B1", -1, List.of("A", "B")),
                new CreateParkingLotRequest.Floor("B2", -2, List.of("C"))))));
        var floor = lot.getFloors().stream().filter(item -> item.getName().equals("B1")).findFirst().orElseThrow();
        var zoneA = floor.getZones().stream().filter(item -> item.getName().equals("A")).findFirst().orElseThrow();
        var zoneB = floor.getZones().stream().filter(item -> item.getName().equals("B")).findFirst().orElseThrow();
        var zoneC = lot.getFloors().stream().filter(item -> item.getName().equals("B2"))
            .findFirst().orElseThrow().getZones().get(0);
        var observed = spaces.saveAndFlush(new ParkingSpace(zoneA, "A-01", ParkingSpaceType.GENERAL,
            "[]", true, List.of()));
        spaces.saveAndFlush(new ParkingSpace(zoneB, "B-01", ParkingSpaceType.GENERAL,
            "[]", true, List.of()));
        var inactive = spaces.saveAndFlush(new ParkingSpace(zoneB, "B-02", ParkingSpaceType.GENERAL,
            "[]", false, List.of()));
        spaces.saveAndFlush(new ParkingSpace(zoneC, "C-01", ParkingSpaceType.GENERAL,
            "[]", true, List.of()));
        var camera = cameras.saveAndFlush(new Camera(zoneA, "Camera", "CAMERA_TEST_RTSP"));
        var observedAt = Instant.parse("2026-10-01T00:00:00Z");
        current.saveAndFlush(new OccupancyCurrent(observed.getId(), OccupancyState.OCCUPIED, 0.93,
            camera.getId(), observedAt));
        current.saveAndFlush(new OccupancyCurrent(inactive.getId(), OccupancyState.EMPTY, 0.88,
            camera.getId(), observedAt));

        var path = "/api/v1/floors/" + floor.getId() + "/occupancy";
        mvc.perform(get(path).header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].parkingSpaceId").value(observed.getId().toString()))
            .andExpect(jsonPath("$.data[0].state").value("OCCUPIED"))
            .andExpect(jsonPath("$.data[0].confidence").value(0.93))
            .andExpect(jsonPath("$.data[0].observedAt").value("2026-10-01T00:00:00Z"))
            .andExpect(jsonPath("$.data[1].state").value("UNKNOWN"))
            .andExpect(jsonPath("$.data[1].observedAt").value((Object) null))
            .andExpect(jsonPath("$.data[2].active").value(false))
            .andExpect(jsonPath("$.data[2].state").value("UNKNOWN"))
            .andExpect(jsonPath("$.data[2].confidence").value((Object) null));

        current.findById(observed.getId()).orElseThrow().update(OccupancyState.EMPTY, 0.97,
            camera.getId(), observedAt.plusSeconds(5));
        current.flush();
        mvc.perform(get(path).header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].state").value("EMPTY"))
            .andExpect(jsonPath("$.data[0].observedAt").value("2026-10-01T00:00:05Z"));
    }

    @Test
    void requiresAuthenticationAndRejectsUnknownOrMalformedFloor() throws Exception {
        var missing = "/api/v1/floors/" + UUID.randomUUID() + "/occupancy";
        mvc.perform(get(missing)).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        mvc.perform(get(missing).header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get(missing).header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PARKING_FLOOR_NOT_FOUND"))
            .andExpect(jsonPath("$.traceId").isString());
        mvc.perform(get("/api/v1/floors/not-a-uuid/occupancy")
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
