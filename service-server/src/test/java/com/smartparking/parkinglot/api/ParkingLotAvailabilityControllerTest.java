package com.smartparking.parkinglot.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.global.security.FirebaseTokenVerifier;
import com.smartparking.global.security.Role;
import com.smartparking.occupancy.domain.OccupancyCurrent;
import com.smartparking.occupancy.domain.OccupancyState;
import com.smartparking.occupancy.infrastructure.OccupancyCurrentRepository;
import com.smartparking.parkinglot.domain.OperationStatus;
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
class ParkingLotAvailabilityControllerTest {
    @MockitoBean FirebaseTokenVerifier verifier;
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired ParkingLotRepository lots;
    @Autowired ParkingSpaceRepository spaces;
    @Autowired CameraRepository cameras;
    @Autowired OccupancyCurrentRepository current;
    @Autowired EntityManager entityManager;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        users.saveAndFlush(new User(null, "lot-viewer", "lot-viewer@example.com",
            "Viewer", Role.USER, UserStatus.ACTIVE));
        when(verifier.verify("viewer-token")).thenReturn("lot-viewer");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void returnsKnownAvailabilityAndConfidenceForLotAndEachFloor() throws Exception {
        var lot = lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("Lot", "Address",
            37.5, 127.0, "Always", "Free", List.of(
                new CreateParkingLotRequest.Floor("B1", -1, List.of("A")),
                new CreateParkingLotRequest.Floor("B2", -2, List.of("B")),
                new CreateParkingLotRequest.Floor("B3", -3, List.of("C"))))));
        ReflectionTestUtils.setField(lot, "operationStatus", OperationStatus.ACTIVE);
        lots.flush();
        var zone1 = lot.getFloors().stream().filter(floor -> floor.getName().equals("B1"))
            .findFirst().orElseThrow().getZones().get(0);
        var zone2 = lot.getFloors().stream().filter(floor -> floor.getName().equals("B2"))
            .findFirst().orElseThrow().getZones().get(0);
        var online = cameras.saveAndFlush(new Camera(zone1, "Online", "ONLINE_RTSP"));
        var degraded = cameras.saveAndFlush(new Camera(zone1, "Degraded", "DEGRADED_RTSP"));
        var offline = cameras.saveAndFlush(new Camera(zone2, "Offline", "OFFLINE_RTSP"));
        var observedAt = Instant.parse("2026-10-01T00:00:00Z");
        cameras.updateObservedStatus(online.getId(), online.getConfigVersion(), "ONLINE", observedAt);
        cameras.updateObservedStatus(degraded.getId(), degraded.getConfigVersion(), "DEGRADED", observedAt);
        var empty = space(zone1, "A-01", true);
        var occupied = space(zone1, "A-02", true);
        var unknown = space(zone1, "A-03", true);
        var inactive = space(zone1, "A-04", false);
        var offlineEmpty = space(zone2, "B-01", true);
        space(zone2, "B-02", true);
        observe(empty, OccupancyState.EMPTY, 0.9, online, observedAt);
        observe(occupied, OccupancyState.OCCUPIED, 0.7, degraded, observedAt);
        observe(unknown, OccupancyState.UNKNOWN, 0.2, online, observedAt);
        observe(inactive, OccupancyState.EMPTY, 0.99, online, observedAt);
        observe(offlineEmpty, OccupancyState.EMPTY, 0.8, offline, observedAt);

        entityManager.clear();
        var path = "/api/v1/parking-lots/" + lot.getId();
        mvc.perform(get(path).header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Lot"))
            .andExpect(jsonPath("$.data.availability.availableSpaces").value(1))
            .andExpect(jsonPath("$.data.availability.knownSpaces").value(2))
            .andExpect(jsonPath("$.data.availability.totalSpaces").value(5))
            .andExpect(jsonPath("$.data.availability.confidence").value(0.8))
            .andExpect(jsonPath("$.data.floors[0].name").value("B3"))
            .andExpect(jsonPath("$.data.floors[0].availability.availableSpaces").value(0))
            .andExpect(jsonPath("$.data.floors[0].availability.knownSpaces").value(0))
            .andExpect(jsonPath("$.data.floors[0].availability.totalSpaces").value(0))
            .andExpect(jsonPath("$.data.floors[0].availability.confidence").value((Object) null))
            .andExpect(jsonPath("$.data.floors[1].name").value("B2"))
            .andExpect(jsonPath("$.data.floors[1].availability.totalSpaces").value(2))
            .andExpect(jsonPath("$.data.floors[2].name").value("B1"))
            .andExpect(jsonPath("$.data.floors[2].availability.availableSpaces").value(1))
            .andExpect(jsonPath("$.data.floors[2].availability.knownSpaces").value(2))
            .andExpect(jsonPath("$.data.floors[2].availability.totalSpaces").value(3));
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/parking-lots/" + UUID.randomUUID())
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isNotFound());
    }

    private ParkingSpace space(com.smartparking.parkingzone.domain.ParkingZone zone,
                               String number, boolean active) {
        return spaces.saveAndFlush(new ParkingSpace(zone, number, ParkingSpaceType.GENERAL,
            "[]", active, List.of()));
    }

    private void observe(ParkingSpace space, OccupancyState state, double confidence,
                         Camera camera, Instant observedAt) {
        current.saveAndFlush(new OccupancyCurrent(space.getId(), state, confidence,
            camera.getId(), observedAt));
    }
}
