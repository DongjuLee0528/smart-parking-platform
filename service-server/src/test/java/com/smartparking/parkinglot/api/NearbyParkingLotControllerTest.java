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
import com.smartparking.parkinglot.domain.OperationStatus;
import com.smartparking.parkinglot.domain.ParkingLot;
import com.smartparking.parkinglot.dto.request.CreateParkingLotRequest;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import com.smartparking.service.SmartParkingServiceApplication;
import com.smartparking.user.domain.User;
import com.smartparking.user.domain.UserStatus;
import com.smartparking.user.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
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
class NearbyParkingLotControllerTest {
    @MockitoBean FirebaseTokenVerifier verifier;
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired ParkingLotRepository lots;
    @Autowired CameraRepository cameras;
    @Autowired EntityManager entityManager;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        users.saveAndFlush(new User(null, "nearby-viewer", "nearby-viewer@example.com",
            "Viewer", Role.USER, UserStatus.ACTIVE));
        when(verifier.verify("viewer-token")).thenReturn("nearby-viewer");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void filtersAndPagesActiveLotsByDistanceAndReportsCameraAvailability() throws Exception {
        var near = lot("Near", 37.5005, true);
        var farther = lot("Farther", 37.501, true);
        lot("Outside", 37.51, true);
        lot("Inactive", 37.5001, false);
        var zone = near.getFloors().get(0).getZones().get(0);
        var camera = cameras.saveAndFlush(new Camera(zone, "Camera", "CAMERA_RTSP"));
        cameras.saveAndFlush(new Camera(farther.getFloors().get(0).getZones().get(0),
            "Offline camera", "OFFLINE_RTSP"));
        cameras.updateObservedStatus(camera.getId(), camera.getConfigVersion(), "ONLINE",
            Instant.parse("2026-10-01T00:00:00Z"));
        entityManager.clear();

        mvc.perform(get("/api/v1/parking-lots/nearby?lat=37.5&lng=127&radius=150&size=1")
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(near.getId().toString()))
            .andExpect(jsonPath("$.data.items[0].distanceMeters").isNumber())
            .andExpect(jsonPath("$.data.items[0].realtimeAvailable").value(true));
        mvc.perform(get("/api/v1/parking-lots/nearby?lat=37.5&lng=127&radius=150&size=1&page=1")
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.items[0].id").value(farther.getId().toString()))
            .andExpect(jsonPath("$.data.items[0].realtimeAvailable").value(false));
    }

    @Test
    void rejectsAnonymousAndInvalidSearchInput() throws Exception {
        mvc.perform(get("/api/v1/parking-lots/nearby?lat=37.5&lng=127&radius=150"))
            .andExpect(status().isUnauthorized());
        for (String query : List.of("lat=91&lng=127&radius=150", "lat=37.5&lng=181&radius=150",
                "lat=37.5&lng=127&radius=0", "lat=37.5&lng=127&radius=50001",
                "lat=37.5&lng=127&radius=150&size=101", "lat=37.5&lng=127&radius=150&page=-1",
                "lat=NaN&lng=127&radius=150", "lat=abc&lng=127&radius=150",
                "lng=127&radius=150")) {
            mvc.perform(get("/api/v1/parking-lots/nearby?" + query)
                    .header("Authorization", "Bearer viewer-token"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.traceId").isString())
                .andExpect(jsonPath("$.details").isMap());
        }
    }

    private ParkingLot lot(String name, double latitude, boolean active) {
        var lot = lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest(name, "Address",
            latitude, 127.0, "Always", "Free", List.of(
                new CreateParkingLotRequest.Floor("B1", -1, List.of("A"))))));
        if (active) {
            ReflectionTestUtils.setField(lot, "operationStatus", OperationStatus.ACTIVE);
            lots.flush();
        }
        return lot;
    }
}
