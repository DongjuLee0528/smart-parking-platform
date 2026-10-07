package com.smartparking.savedparkinglocation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.smartparking.savedparkinglocation.infrastructure.SavedParkingLocationRepository;
import com.smartparking.service.SmartParkingServiceApplication;
import com.smartparking.user.domain.User;
import com.smartparking.user.domain.UserStatus;
import com.smartparking.user.infrastructure.UserRepository;
import com.smartparking.vehicle.domain.Vehicle;
import com.smartparking.vehicle.infrastructure.VehicleRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = SmartParkingServiceApplication.class)
@ActiveProfiles("test")
@Transactional
class SavedParkingLocationControllerTest {
    private static final String PATH = "/api/v1/saved-parking-locations";
    @MockitoBean FirebaseTokenVerifier verifier;
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired VehicleRepository vehicles;
    @Autowired ParkingLotRepository lots;
    @Autowired ParkingSpaceRepository spaces;
    @Autowired CameraRepository cameras;
    @Autowired OccupancyCurrentRepository occupancy;
    @Autowired SavedParkingLocationRepository locations;
    MockMvc mvc;
    UUID vehicleA;
    UUID secondVehicleA;
    UUID vehicleB;
    UUID spaceId;

    @BeforeEach
    void setUp() {
        var userA = users.saveAndFlush(new User(null, "location-user-a", "location-a@example.com",
            "User A", Role.USER, UserStatus.ACTIVE));
        var userB = users.saveAndFlush(new User(null, "location-user-b", "location-b@example.com",
            "User B", Role.USER, UserStatus.ACTIVE));
        vehicleA = vehicles.saveAndFlush(new Vehicle(userA.getId(), "12가3456", "Mine", true)).getId();
        secondVehicleA = vehicles.saveAndFlush(new Vehicle(userA.getId(), "56다7890", "Also mine", false)).getId();
        vehicleB = vehicles.saveAndFlush(new Vehicle(userB.getId(), "34나5678", "Other", true)).getId();
        var lot = lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("Original Lot", "Address",
            37.5, 127.0, "", "", List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A"))))));
        var zone = lot.getFloors().get(0).getZones().get(0);
        spaceId = spaces.saveAndFlush(new ParkingSpace(zone, "A-01", ParkingSpaceType.GENERAL,
            "[]", true, List.of())).getId();
        when(verifier.verify("token-a")).thenReturn("location-user-a");
        when(verifier.verify("token-b")).thenReturn("location-user-b");
        when(verifier.verify("invalid")).thenThrow(new FirebaseTokenVerificationException(
            new IllegalArgumentException("Rejected")));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void savesSnapshotReplacesActiveAndReleasesIdempotently() throws Exception {
        mvc.perform(get(PATH + "/active").header("Authorization", "Bearer token-a"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").value((Object) null));
        var first = save("token-a", vehicleA, spaceId, false)
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.snapshot.parkingLotName")
                .value("Original Lot")).andExpect(jsonPath("$.data.snapshot.floorName").value("B1"))
            .andReturn();
        var firstId = data(first.getResponse().getContentAsString()).get("id").asText();
        save("token-a", vehicleA, spaceId, false).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_SELECTION_CONFIRM_REQUIRED"));
        var second = save("token-a", secondVehicleA, spaceId, true).andExpect(status().isCreated()).andReturn();
        var secondId = data(second.getResponse().getContentAsString()).get("id").asText();
        assertThat(locations.findById(UUID.fromString(firstId)).orElseThrow().getReleasedAt()).isNotNull();
        mvc.perform(get(PATH + "/active").header("Authorization", "Bearer token-a"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(secondId))
            .andExpect(jsonPath("$.data.snapshot.plateNumber").value((Object) null));
        mvc.perform(delete(PATH + "/{id}", secondId).header("Authorization", "Bearer token-a"))
            .andExpect(status().isNoContent());
        var releasedAt = locations.findById(UUID.fromString(secondId)).orElseThrow().getReleasedAt();
        mvc.perform(delete(PATH + "/{id}", secondId).header("Authorization", "Bearer token-a"))
            .andExpect(status().isNoContent());
        assertThat(locations.findById(UUID.fromString(secondId)).orElseThrow().getReleasedAt())
            .isEqualTo(releasedAt);
        mvc.perform(get(PATH + "/active").header("Authorization", "Bearer token-a"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").value((Object) null));
    }

    @Test
    void requiresConfirmationWhenCurrentOccupancySaysEmpty() throws Exception {
        var zone = spaces.findById(spaceId).orElseThrow().getZone();
        var camera = cameras.saveAndFlush(new Camera(zone, "Camera", "CAMERA_TEST_RTSP"));
        occupancy.saveAndFlush(new OccupancyCurrent(spaceId, OccupancyState.EMPTY, 0.95,
            camera.getId(), Instant.parse("2026-10-03T00:00:00Z")));
        save("token-a", vehicleA, spaceId, false).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("EMPTY_SPACE_CONFIRM_REQUIRED"));
        assertThat(locations.findAll()).isEmpty();
        save("token-a", vehicleA, spaceId, true).andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.snapshot.occupancyState").value("EMPTY"));
    }

    @Test
    void enforcesOwnershipAndRejectsInvalidTargets() throws Exception {
        save("token-a", vehicleB, spaceId, true).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
        save("token-a", vehicleA, UUID.randomUUID(), true).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PARKING_SPACE_NOT_FOUND"));
        var inactive = spaces.saveAndFlush(new ParkingSpace(spaces.findById(spaceId).orElseThrow().getZone(),
            "A-02", ParkingSpaceType.GENERAL, "[]", false, List.of()));
        save("token-a", vehicleA, inactive.getId(), true).andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        var created = save("token-a", vehicleA, spaceId, false).andExpect(status().isCreated()).andReturn();
        var id = data(created.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(get(PATH + "/active").header("Authorization", "Bearer token-b"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").value((Object) null));
        mvc.perform(delete(PATH + "/{id}", id).header("Authorization", "Bearer token-b"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SAVED_PARKING_LOCATION_NOT_FOUND"));
        mvc.perform(post(PATH).header("Authorization", "Bearer token-a")
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":true}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(delete(PATH + "/not-a-uuid").header("Authorization", "Bearer token-a"))
            .andExpect(status().isUnprocessableEntity());
        assertThat(locations.findById(UUID.fromString(id)).orElseThrow().getReleasedAt()).isNull();
    }

    @Test
    void requiresValidFirebaseToken() throws Exception {
        mvc.perform(get(PATH + "/active")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        mvc.perform(get(PATH + "/active").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void deletingVehicleReleasesActiveLocationAndPreservesLocationHistoryWithoutVehicleData() throws Exception {
        var first = save("token-a", vehicleA, spaceId, false).andExpect(status().isCreated()).andReturn();
        var firstId = UUID.fromString(data(first.getResponse().getContentAsString()).get("id").asText());
        var second = save("token-a", secondVehicleA, spaceId, true)
            .andExpect(status().isCreated()).andReturn();
        var secondId = UUID.fromString(data(second.getResponse().getContentAsString()).get("id").asText());
        var firstReleasedAt = locations.findById(firstId).orElseThrow().getReleasedAt();

        mvc.perform(delete("/api/v1/vehicles/{id}?version=0", vehicleA)
                .header("Authorization", "Bearer token-a"))
            .andExpect(status().isNoContent());
        var former = locations.findById(firstId).orElseThrow();
        assertThat(former.getVehicleId()).isNull();
        assertThat(former.getReleasedAt()).isEqualTo(firstReleasedAt);
        assertThat(former.getSnapshotJson()).contains("Original Lot").doesNotContain("12가3456");
        mvc.perform(get(PATH + "/active").header("Authorization", "Bearer token-a"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.vehicleId")
                .value(secondVehicleA.toString()));

        mvc.perform(delete("/api/v1/vehicles/{id}?version=0", secondVehicleA)
                .header("Authorization", "Bearer token-a"))
            .andExpect(status().isNoContent());
        var latest = locations.findById(secondId).orElseThrow();
        assertThat(latest.getVehicleId()).isNull();
        assertThat(latest.getReleasedAt()).isNotNull();
        assertThat(latest.getSnapshotJson()).contains("Original Lot").doesNotContain("56다7890");
        mvc.perform(get(PATH + "/active").header("Authorization", "Bearer token-a"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").value((Object) null));
        assertThat(vehicles.findById(vehicleB)).isPresent();
    }

    private org.springframework.test.web.servlet.ResultActions save(String token, UUID vehicleId,
                                                                     UUID parkingSpaceId, boolean confirm)
        throws Exception {
        return mvc.perform(post(PATH).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(
                new com.smartparking.savedparkinglocation.dto.request.SaveParkingLocationRequest(
                    vehicleId, parkingSpaceId, confirm))));
    }

    private JsonNode data(String response) throws Exception {
        return mapper.readTree(response).get("data");
    }
}
