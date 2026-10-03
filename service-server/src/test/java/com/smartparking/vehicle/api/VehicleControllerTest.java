package com.smartparking.vehicle.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.global.security.FirebaseTokenVerificationException;
import com.smartparking.global.security.FirebaseTokenVerifier;
import com.smartparking.global.security.Role;
import com.smartparking.service.SmartParkingServiceApplication;
import com.smartparking.user.domain.User;
import com.smartparking.user.domain.UserStatus;
import com.smartparking.user.infrastructure.UserRepository;
import com.smartparking.vehicle.infrastructure.VehicleRepository;
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
class VehicleControllerTest {
    @MockitoBean FirebaseTokenVerifier verifier;
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired VehicleRepository vehicles;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        users.saveAndFlush(new User(null, "vehicle-user-a", "vehicle-a@example.com",
            "User A", Role.USER, UserStatus.ACTIVE));
        users.saveAndFlush(new User(null, "vehicle-user-b", "vehicle-b@example.com",
            "User B", Role.USER, UserStatus.ACTIVE));
        when(verifier.verify("token-a")).thenReturn("vehicle-user-a");
        when(verifier.verify("token-b")).thenReturn("vehicle-user-b");
        when(verifier.verify("invalid")).thenThrow(new FirebaseTokenVerificationException(
            new IllegalArgumentException("Rejected")));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void managesOnlyOwnersVehiclesAndSwitchesPrimary() throws Exception {
        var first = create("token-a", "12가 3456", "Home", true);
        var second = create("token-a", "123나-4567", "Work", true);
        assertThat(first.get("plateNumber").asText()).isEqualTo("12가3456");
        assertThat(second.get("isPrimary").asBoolean()).isTrue();
        mvc.perform(get("/api/v1/vehicles/{id}", first.get("id").asText())
                .header("Authorization", "Bearer token-a"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.isPrimary").value(false));
        mvc.perform(get("/api/v1/vehicles?page=0&size=1&sort=createdAt,asc")
                .header("Authorization", "Bearer token-a"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.items.length()").value(1));

        var id = second.get("id").asText();
        mvc.perform(patch("/api/v1/vehicles/{id}", id).header("Authorization", "Bearer token-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"plateNumber\":\"123나4567\",\"nickname\":\"Office\",\"isPrimary\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value("Office"))
            .andExpect(jsonPath("$.data.version").value(1));
        mvc.perform(delete("/api/v1/vehicles/{id}?version=1", id)
                .header("Authorization", "Bearer token-a"))
            .andExpect(status().isNoContent());
        assertThat(vehicles.findById(UUID.fromString(id))).isEmpty();
    }

    @Test
    void concealsOtherUsersVehicles() throws Exception {
        var id = create("token-a", "12가3456", "Mine", false).get("id").asText();
        mvc.perform(get("/api/v1/vehicles").header("Authorization", "Bearer token-b"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/v1/vehicles/{id}", id).header("Authorization", "Bearer token-b"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
        mvc.perform(patch("/api/v1/vehicles/{id}", id).header("Authorization", "Bearer token-b")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"plateNumber\":\"12가3456\",\"isPrimary\":true}"))
            .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/vehicles/{id}?version=0", id)
                .header("Authorization", "Bearer token-b"))
            .andExpect(status().isNotFound());
        assertThat(vehicles.findById(UUID.fromString(id))).isPresent();
        create("token-b", "12가3456", "Also mine", false);
        mvc.perform(get("/api/v1/vehicles").header("Authorization", "Bearer token-b"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void rejectsDuplicateInvalidAndStaleVehicleChanges() throws Exception {
        var id = create("token-a", "12가3456", "", false).get("id").asText();
        mvc.perform(post("/api/v1/vehicles").header("Authorization", "Bearer token-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"plateNumber\":\"12가-3456\",\"isPrimary\":false}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VEHICLE_CONFLICT"));
        mvc.perform(post("/api/v1/vehicles").header("Authorization", "Bearer token-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"plateNumber\":\"not-a-plate\",\"isPrimary\":false}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(patch("/api/v1/vehicles/{id}", id).header("Authorization", "Bearer token-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":7,\"plateNumber\":\"12가3456\",\"isPrimary\":true}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
        mvc.perform(delete("/api/v1/vehicles/{id}", id).header("Authorization", "Bearer token-a"))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/vehicles?size=101").header("Authorization", "Bearer token-a"))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/vehicles/not-a-uuid").header("Authorization", "Bearer token-a"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejectsMissingAndInvalidFirebaseTokens() throws Exception {
        mvc.perform(get("/api/v1/vehicles"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        mvc.perform(get("/api/v1/vehicles").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.traceId").isString());
    }

    private JsonNode create(String token, String plate, String nickname, boolean primary) throws Exception {
        var body = mapper.writeValueAsString(new com.smartparking.vehicle.dto.request.CreateVehicleRequest(
            plate, nickname, primary));
        var result = mvc.perform(post("/api/v1/vehicles").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("data");
    }
}
