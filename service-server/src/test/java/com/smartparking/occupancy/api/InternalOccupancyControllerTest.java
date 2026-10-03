package com.smartparking.occupancy.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.domain.CameraSpaceMapping;
import com.smartparking.camera.domain.CameraStatus;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.global.security.FirebaseTokenVerifier;
import com.smartparking.global.security.InternalAiAuthenticationFilter;
import com.smartparking.occupancy.domain.OccupancyState;
import com.smartparking.occupancy.dto.request.AiOccupancyResultRequest;
import com.smartparking.occupancy.infrastructure.OccupancyCurrentRepository;
import com.smartparking.occupancy.infrastructure.OccupancyHistoryRepository;
import com.smartparking.parkinglot.domain.ParkingLot;
import com.smartparking.parkinglot.dto.request.CreateParkingLotRequest;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import com.smartparking.parkingspace.domain.ParkingSpace;
import com.smartparking.parkingspace.domain.ParkingSpaceType;
import com.smartparking.parkingspace.infrastructure.ParkingSpaceRepository;
import com.smartparking.service.SmartParkingServiceApplication;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = SmartParkingServiceApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "AI_INTERNAL_TOKEN=test-internal-only")
class InternalOccupancyControllerTest {
    private static final String PATH = "/internal/v1/occupancy-results";
    private static final Instant FIRST = Instant.parse("2026-09-30T04:00:00Z");
    @MockitoBean FirebaseTokenVerifier firebaseTokenVerifier;
    @Autowired WebApplicationContext context;
    @Autowired InternalAiAuthenticationFilter filter;
    @Autowired ObjectMapper mapper;
    @Autowired ParkingLotRepository lots;
    @Autowired CameraRepository cameras;
    @Autowired ParkingSpaceRepository spaces;
    @Autowired OccupancyCurrentRepository current;
    @Autowired OccupancyHistoryRepository history;
    @Autowired EntityManager entityManager;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filter).apply(springSecurity()).build();
    }

    @Test
    @Transactional
    void acceptsMappedResultAndStoresOnlyStateChanges() throws Exception {
        var fixture = fixture();
        var first = result(UUID.randomUUID(), fixture.cameraId(), fixture.spaceId(), 1,
            FIRST, OccupancyState.OCCUPIED, 0.94);
        submit(first, "Bearer test-internal-only").andExpect(status().isNoContent());
        entityManager.clear();
        submit(first, "Bearer test-internal-only").andExpect(status().isNoContent());
        assertThat(current.findById(fixture.spaceId()).orElseThrow().getState()).isEqualTo(OccupancyState.OCCUPIED);
        assertThat(history.findAll()).hasSize(1);

        submit(result(UUID.randomUUID(), fixture.cameraId(), fixture.spaceId(), 1,
            FIRST.plusSeconds(10), OccupancyState.OCCUPIED, 0.97), "Bearer test-internal-only")
            .andExpect(status().isNoContent());
        assertThat(history.findAll()).hasSize(1);

        submit(result(UUID.randomUUID(), fixture.cameraId(), fixture.spaceId(), 1,
            FIRST.plusSeconds(5), OccupancyState.EMPTY, 0.9), "Bearer test-internal-only")
            .andExpect(status().isNoContent());
        assertThat(current.findById(fixture.spaceId()).orElseThrow().getState()).isEqualTo(OccupancyState.OCCUPIED);

        submit(result(UUID.randomUUID(), fixture.cameraId(), fixture.spaceId(), 1,
            FIRST.plusSeconds(20), OccupancyState.EMPTY, 0.92), "Bearer test-internal-only")
            .andExpect(status().isNoContent());
        assertThat(current.findById(fixture.spaceId()).orElseThrow().getState()).isEqualTo(OccupancyState.EMPTY);
        assertThat(history.findAll()).hasSize(2);
    }

    @Test
    @Transactional
    void rejectsMissingAndIncorrectServiceTokens() throws Exception {
        var fixture = fixture();
        var request = result(UUID.randomUUID(), fixture.cameraId(), fixture.spaceId(), 1,
            FIRST, OccupancyState.OCCUPIED, 0.94);
        submit(request, null).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        submit(request, "Bearer wrong").andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.traceId").isString());
        assertThat(current.findAll()).isEmpty();
    }

    @Test
    void rejectsRequestsWhenServiceTokenIsUnconfigured() throws Exception {
        var request = new MockHttpServletRequest("POST", PATH);
        request.addHeader("Authorization", "Bearer any-token");
        var response = new MockHttpServletResponse();
        new InternalAiAuthenticationFilter("", mapper).doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @Transactional
    void rejectsUnknownMappingAndStaleConfiguration() throws Exception {
        var fixture = fixture();
        submit(result(UUID.randomUUID(), fixture.cameraId(), fixture.spaceId(), 0,
            FIRST, OccupancyState.OCCUPIED, 0.94), "Bearer test-internal-only")
            .andExpect(status().isUnprocessableEntity());
        submit(result(UUID.randomUUID(), UUID.randomUUID(), fixture.spaceId(), 1,
            FIRST, OccupancyState.OCCUPIED, 0.94), "Bearer test-internal-only")
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAMERA_NOT_FOUND"));
        var otherCamera = cameras.saveAndFlush(new Camera(spaces.findById(fixture.spaceId()).orElseThrow().getZone(),
            "Unmapped", "CAMERA_UNMAPPED_RTSP"));
        submit(result(UUID.randomUUID(), otherCamera.getId(), fixture.spaceId(), 1,
            FIRST, OccupancyState.OCCUPIED, 0.94), "Bearer test-internal-only")
            .andExpect(status().isUnprocessableEntity());
        submit(result(UUID.randomUUID(), fixture.cameraId(), UUID.randomUUID(), 1,
            FIRST, OccupancyState.OCCUPIED, 0.94), "Bearer test-internal-only")
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PARKING_SPACE_NOT_FOUND"));
        assertThat(current.findAll()).isEmpty();
    }

    @Test
    @Transactional
    void rejectsInvalidConfidence() throws Exception {
        var fixture = fixture();
        submit(result(UUID.randomUUID(), fixture.cameraId(), fixture.spaceId(), 1,
            FIRST, OccupancyState.OCCUPIED, 1.4), "Bearer test-internal-only")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(current.findAll()).isEmpty();
    }

    private ResultActions submit(AiOccupancyResultRequest request, String authorization) throws Exception {
        var builder = post(PATH).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request));
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return mvc.perform(builder);
    }

    private AiOccupancyResultRequest result(UUID eventId, UUID cameraId, UUID spaceId, long version,
                                             Instant capturedAt, OccupancyState state, double confidence) {
        return new AiOccupancyResultRequest(eventId, cameraId, version,
            new AiOccupancyResultRequest.Model("model", "1.0", "checksum"), capturedAt,
            capturedAt.plusMillis(200), CameraStatus.ONLINE,
            List.of(new AiOccupancyResultRequest.Space(spaceId, state, confidence,
                new AiOccupancyResultRequest.Evidence(0.91, 0.63))));
    }

    private Fixture fixture() {
        var lot = lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("Lot", "Address", 37.5, 127.0,
            "", "", List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A"))))));
        var zone = lot.getFloors().get(0).getZones().get(0);
        var camera = cameras.saveAndFlush(new Camera(zone, "Entrance", "CAMERA_ENTRANCE_RTSP"));
        var space = spaces.saveAndFlush(new ParkingSpace(zone, "A-01", ParkingSpaceType.GENERAL,
            "[{\"x\":0.1,\"y\":0.1},{\"x\":0.4,\"y\":0.1},{\"x\":0.4,\"y\":0.4}]", true,
            List.of(new CameraSpaceMapping(camera.getId(), "[]", 1, 1))));
        return new Fixture(camera.getId(), space.getId());
    }

    private record Fixture(UUID cameraId, UUID spaceId) {}
}
