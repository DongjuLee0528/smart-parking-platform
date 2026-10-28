package com.smartparking.parkingfloor.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.audit.infrastructure.AuditLogRepository;
import com.smartparking.global.security.FirebaseTokenVerifier;
import com.smartparking.global.security.Role;
import com.smartparking.parkinglot.domain.OperationStatus;
import com.smartparking.parkinglot.domain.ParkingLot;
import com.smartparking.parkinglot.dto.request.CreateParkingLotRequest;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import com.smartparking.parkingspace.domain.ParkingSpace;
import com.smartparking.parkingspace.domain.ParkingSpaceType;
import com.smartparking.parkingspace.infrastructure.ParkingSpaceRepository;
import com.smartparking.parkingzone.infrastructure.ParkingZoneRepository;
import com.smartparking.service.SmartParkingServiceApplication;
import com.smartparking.user.domain.User;
import com.smartparking.user.domain.UserStatus;
import com.smartparking.user.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class ParkingFloorManagementTest {
    @MockitoBean FirebaseTokenVerifier verifier;
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired ParkingLotRepository lots;
    @Autowired ParkingZoneRepository zones;
    @Autowired ParkingSpaceRepository spaces;
    @Autowired AuditLogRepository audits;
    @Autowired EntityManager entityManager;
    @Autowired ObjectMapper mapper;
    MockMvc mvc;
    UUID lotId;

    @BeforeEach
    void setUp() {
        users.saveAndFlush(new User(null, "floor-admin", "floor-admin@example.com",
            "Admin", Role.ADMIN, UserStatus.ACTIVE));
        users.saveAndFlush(new User(null, "floor-viewer", "floor-viewer@example.com",
            "Viewer", Role.USER, UserStatus.ACTIVE));
        when(verifier.verify("admin-token")).thenReturn("floor-admin");
        when(verifier.verify("viewer-token")).thenReturn("floor-viewer");
        var lot = lots.saveAndFlush(new ParkingLot(new CreateParkingLotRequest("Lot", "Address",
            37.5, 127.0, "Always", "Free", List.of())));
        ReflectionTestUtils.setField(lot, "operationStatus", OperationStatus.ACTIVE);
        lots.flush();
        lotId = lot.getId();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void createsUpdatesAndDeactivatesWithoutDeletingChildren() throws Exception {
        var floorId = createFloor();
        var zoneId = UUID.fromString(mapper.readTree(mvc.perform(post("/api/v1/admin/floors/{id}/zones", floorId)
                .header("Authorization", "Bearer admin-token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
            .path("data").path("id").asText());
        var zone = zones.findById(zoneId).orElseThrow();
        spaces.saveAndFlush(new ParkingSpace(zone, "A-01", ParkingSpaceType.GENERAL, "[]", true, List.of()));

        mvc.perform(patch("/api/v1/admin/zones/{id}", zoneId)
                .header("Authorization", "Bearer admin-token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"C\",\"version\":0,\"active\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("C"))
            .andExpect(jsonPath("$.data.version").value(1));

        mvc.perform(patch("/api/v1/admin/zones/{id}", zoneId)
                .header("Authorization", "Bearer admin-token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"C\",\"version\":1,\"active\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(false))
            .andExpect(jsonPath("$.data.version").value(2));
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/api/v1/parking-lots/{id}/floors", lotId)
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].zones.length()").value(0));
        mvc.perform(get("/api/v1/parking-lots/{id}", lotId)
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.availability.totalSpaces").value(0));
        mvc.perform(get("/api/v1/floors/{id}/map", floorId)
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.spaces.length()").value(0));

        mvc.perform(patch("/api/v1/admin/floors/{id}", floorId)
                .header("Authorization", "Bearer admin-token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"B2\",\"floorOrder\":-2,\"version\":0,\"active\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("B2"))
            .andExpect(jsonPath("$.data.floorOrder").value(-2));
        mvc.perform(patch("/api/v1/admin/floors/{id}", floorId)
                .header("Authorization", "Bearer admin-token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"B2\",\"floorOrder\":-2,\"version\":1,\"active\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(false));
        mvc.perform(get("/api/v1/parking-lots/{id}/floors", lotId)
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/api/v1/admin/parking-lots/{id}/floors", lotId)
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].zones[0].active").value(false));
        mvc.perform(get("/api/v1/floors/{id}/map", floorId)
                .header("Authorization", "Bearer viewer-token"))
            .andExpect(status().isNotFound());
        assertEquals(6, audits.findAll().stream().filter(a ->
            a.getEntityType().equals("PARKING_FLOOR") || a.getEntityType().equals("PARKING_ZONE")).count());
        assertEquals(1, spaces.count());
    }

    @Test
    void rejectsUnauthorizedInvalidDuplicateAndStaleChanges() throws Exception {
        var path = "/api/v1/admin/parking-lots/" + lotId + "/floors";
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"B1\",\"floorOrder\":-1}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post(path).header("Authorization", "Bearer viewer-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"B1\",\"floorOrder\":-1}"))
            .andExpect(status().isForbidden());
        mvc.perform(post(path).header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  \",\"floorOrder\":-1}"))
            .andExpect(status().isUnprocessableEntity());
        var floorId = createFloor();
        mvc.perform(post(path).header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"B1\",\"floorOrder\":-2}"))
            .andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/admin/floors/{id}", floorId)
                .header("Authorization", "Bearer admin-token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"B1\",\"floorOrder\":-1,\"version\":8,\"active\":false}"))
            .andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/admin/zones/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer admin-token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"version\":0,\"active\":false}"))
            .andExpect(status().isNotFound());
    }

    private UUID createFloor() throws Exception {
        var body = mvc.perform(post("/api/v1/admin/parking-lots/{id}/floors", lotId)
                .header("Authorization", "Bearer admin-token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"B1\",\"floorOrder\":-1}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.version").value(0))
            .andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(body).path("data").path("id").asText());
    }
}
