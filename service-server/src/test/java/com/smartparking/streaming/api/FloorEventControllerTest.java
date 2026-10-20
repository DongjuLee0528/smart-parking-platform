package com.smartparking.streaming.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.domain.CameraSpaceMapping;
import com.smartparking.camera.domain.CameraStatus;
import com.smartparking.camera.infrastructure.CameraRepository;
import com.smartparking.audit.infrastructure.AuditLogRepository;
import com.smartparking.global.security.FirebaseTokenVerificationException;
import com.smartparking.global.security.FirebaseTokenVerifier;
import com.smartparking.global.security.Role;
import com.smartparking.occupancy.application.OccupancyService;
import com.smartparking.occupancy.domain.OccupancyState;
import com.smartparking.occupancy.dto.request.AiOccupancyResultRequest;
import com.smartparking.parkinglot.domain.ParkingLot;
import com.smartparking.parkinglot.dto.request.CreateParkingLotRequest;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import com.smartparking.parkingspace.application.ParkingSpaceService;
import com.smartparking.parkingspace.domain.ParkingSpace;
import com.smartparking.parkingspace.domain.ParkingSpaceType;
import com.smartparking.parkingspace.dto.NormalizedPoint;
import com.smartparking.parkingspace.dto.request.SaveParkingSpacesRequest;
import com.smartparking.parkingspace.infrastructure.ParkingSpaceRepository;
import com.smartparking.service.SmartParkingServiceApplication;
import com.smartparking.user.domain.User;
import com.smartparking.user.domain.UserStatus;
import com.smartparking.user.infrastructure.UserRepository;
import com.smartparking.streaming.application.FloorEventStreamService;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = SmartParkingServiceApplication.class)
@ActiveProfiles("test")
class FloorEventControllerTest {
    private static final Instant FIRST = Instant.parse("2026-10-01T00:00:00Z");
    @MockitoBean FirebaseTokenVerifier verifier;
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired ParkingLotRepository lots;
    @Autowired CameraRepository cameras;
    @Autowired ParkingSpaceRepository spaces;
    @Autowired OccupancyService occupancy;
    @Autowired ParkingSpaceService parkingSpaceService;
    @Autowired FloorEventStreamService stream;
    @Autowired AuditLogRepository audits;
    @Autowired TransactionTemplate transactions;
    UUID actorId;
    UUID layoutChangedFloorId;
    MockMvc mvc;

    @AfterEach
    void cleanLayoutChangeAudit() {
        if (layoutChangedFloorId != null) {
            audits.deleteAll(audits.findAll().stream()
                .filter(audit -> audit.getEntityId().equals(layoutChangedFloorId)).toList());
        }
    }

    @BeforeEach
    void setUp() {
        var uid = UUID.randomUUID().toString();
        actorId = users.saveAndFlush(new User(null, uid, uid + "@example.com",
            "Viewer", Role.USER, UserStatus.ACTIVE)).getId();
        when(verifier.verify("viewer-token")).thenReturn(uid);
        when(verifier.verify("invalid")).thenThrow(new FirebaseTokenVerificationException(
            new IllegalArgumentException("Rejected")));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void requiresAuthenticationAndExistingFloor() throws Exception {
        var path = "/api/v1/floors/" + UUID.randomUUID() + "/events";
        mvc.perform(get(path)).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        mvc.perform(get(path).header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PARKING_FLOOR_NOT_FOUND"))
            .andExpect(jsonPath("$.traceId").isString())
            .andExpect(jsonPath("$.details").isMap());
        mvc.perform(get("/api/v1/floors/not-a-uuid/events")
            .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.traceId").isString())
            .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void publishesOnlyCommittedStateChangesToTheWatchedFloorAndReplaysOnReconnect() throws Exception {
        var first = fixture("B1");
        var second = fixture("B2");
        var watched = subscribe(first.floorId(), null);
        var other = subscribe(second.floorId(), null);
        assertThat(watched.getResponse().getContentAsString()).contains("FLOOR_SNAPSHOT_REQUIRED");

        var firstEventId = UUID.randomUUID();
        transactions.executeWithoutResult(status -> {
            occupancy.ingest(result(first, firstEventId, FIRST, OccupancyState.OCCUPIED));
            assertThat(new String(watched.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8))
                .doesNotContain("OCCUPANCY_CHANGED");
        });
        var content = watched.getResponse().getContentAsString();
        assertThat(content).contains("OCCUPANCY_CHANGED", first.spaceId().toString(), "OCCUPIED");
        assertThat(other.getResponse().getContentAsString()).doesNotContain("OCCUPANCY_CHANGED");
        var eventId = content.lines().filter(line -> line.startsWith("id:")).findFirst().orElseThrow().substring(3);

        occupancy.ingest(result(first, firstEventId, FIRST, OccupancyState.OCCUPIED));
        occupancy.ingest(result(first, UUID.randomUUID(), FIRST.plusSeconds(10), OccupancyState.OCCUPIED));
        occupancy.ingest(result(first, UUID.randomUUID(), FIRST.plusSeconds(5), OccupancyState.EMPTY));
        assertThat(watched.getResponse().getContentAsString()).isEqualTo(content);
        transactions.executeWithoutResult(status -> {
            occupancy.ingest(result(first, UUID.randomUUID(), FIRST.plusSeconds(20), OccupancyState.EMPTY));
            status.setRollbackOnly();
        });
        assertThat(watched.getResponse().getContentAsString()).isEqualTo(content);

        occupancy.ingest(result(second, UUID.randomUUID(), FIRST, OccupancyState.EMPTY));
        assertThat(watched.getResponse().getContentAsString()).isEqualTo(content);
        occupancy.ingest(result(first, UUID.randomUUID(), FIRST.plusSeconds(30), OccupancyState.EMPTY));
        assertThat(watched.getResponse().getContentAsString()).contains("EMPTY");

        var replay = subscribe(first.floorId(), eventId);
        assertThat(replay.getResponse().getContentAsString()).contains("OCCUPANCY_CHANGED", "EMPTY")
            .doesNotContain("FLOOR_SNAPSHOT_REQUIRED");
        var unknownCursor = subscribe(first.floorId(), "unknown-event");
        assertThat(unknownCursor.getResponse().getContentAsString()).contains("FLOOR_SNAPSHOT_REQUIRED");
        assertThat(subscribe(second.floorId(), eventId).getResponse().getContentAsString())
            .contains("FLOOR_SNAPSHOT_REQUIRED");
    }

    @Test
    void committedLayoutChangeRequiresSnapshotAndReplaysAfterReconnect() throws Exception {
        var first = fixture("Map B1");
        layoutChangedFloorId = first.floorId();
        var second = fixture("Map B2");
        occupancy.ingest(result(first, UUID.randomUUID(), FIRST, OccupancyState.EMPTY));
        var previousCursor = stream.latestEventId(first.floorId());
        assertThat(previousCursor).isNotBlank();
        mvc.perform(get("/api/v1/floors/{floorId}/map", first.floorId())
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.mapVersion").value(0))
            .andExpect(jsonPath("$.data.eventVersion").value(previousCursor));
        var watched = subscribe(first.floorId(), previousCursor);
        var other = subscribe(second.floorId(), null);
        var zoneId = spaces.findById(first.spaceId()).orElseThrow().getZone().getId();
        var polygon = List.of(new NormalizedPoint(0.1, 0.1), new NormalizedPoint(0.4, 0.1),
            new NormalizedPoint(0.4, 0.4));
        var item = new SaveParkingSpacesRequest.Item(first.spaceId(), zoneId, "A-01",
            ParkingSpaceType.GENERAL, true, polygon, List.of());

        transactions.executeWithoutResult(status -> {
            parkingSpaceService.save(actorId, first.floorId(),
                new SaveParkingSpacesRequest(0L, List.of(item)));
            assertThat(stream.latestEventId(first.floorId())).isEqualTo(previousCursor);
            assertThat(watched.getResponse().getContentAsByteArray()).isEmpty();
        });

        var changed = watched.getResponse().getContentAsString();
        assertThat(changed).contains("FLOOR_SNAPSHOT_REQUIRED", "id:");
        assertThat(other.getResponse().getContentAsString()).doesNotContain("id:");
        var newCursor = stream.latestEventId(first.floorId());
        assertThat(newCursor).isNotEqualTo(previousCursor);
        mvc.perform(get("/api/v1/floors/{floorId}/map", first.floorId())
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.mapVersion").value(1))
            .andExpect(jsonPath("$.data.eventVersion").value(newCursor))
            .andExpect(jsonPath("$.data.spaces[0].mapPolygon[0].x").value(0.1));
        assertThat(subscribe(first.floorId(), previousCursor).getResponse().getContentAsString())
            .contains("FLOOR_SNAPSHOT_REQUIRED", "id:");

        transactions.executeWithoutResult(status -> {
            parkingSpaceService.save(actorId, first.floorId(),
                new SaveParkingSpacesRequest(1L, List.of(item)));
            status.setRollbackOnly();
        });
        assertThat(stream.latestEventId(first.floorId())).isEqualTo(newCursor);
        assertThat(watched.getResponse().getContentAsString()).isEqualTo(changed);
    }

    private MvcResult subscribe(UUID floorId, String lastEventId) throws Exception {
        var request = get("/api/v1/floors/" + floorId + "/events")
            .header("Authorization", "Bearer viewer-token");
        if (lastEventId != null) {
            request.header("Last-Event-ID", lastEventId);
        }
        return mvc.perform(request).andExpect(status().isOk()).andReturn();
    }

    private Fixture fixture(String floorName) {
        var lot = lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("Lot " + floorName,
            "Address", 37.5, 127.0, "", "", List.of(
                new CreateParkingLotRequest.Floor(floorName, -1, List.of("A"))))));
        var zone = lot.getFloors().get(0).getZones().get(0);
        var camera = cameras.saveAndFlush(new Camera(zone, "Camera", "CAMERA_" + floorName));
        var space = spaces.saveAndFlush(new ParkingSpace(zone, "A-01", ParkingSpaceType.GENERAL,
            "[]", true, List.of(new CameraSpaceMapping(camera.getId(), "[]", 1, 0))));
        return new Fixture(lot.getFloors().get(0).getId(), camera.getId(), space.getId());
    }

    private AiOccupancyResultRequest result(Fixture fixture, UUID eventId, Instant capturedAt,
                                            OccupancyState state) {
        return new AiOccupancyResultRequest(eventId, fixture.cameraId(), 0L,
            new AiOccupancyResultRequest.Model("model", "1", "checksum"), capturedAt,
            capturedAt.plusMillis(200), CameraStatus.ONLINE,
            List.of(new AiOccupancyResultRequest.Space(fixture.spaceId(), state, 0.9, null)));
    }

    private record Fixture(UUID floorId, UUID cameraId, UUID spaceId) {}
}
