package com.smartparking.parkingspace.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartparking.auth.application.AuthService;
import com.smartparking.global.security.*;
import com.smartparking.global.security.Role;
import com.smartparking.parkingspace.application.ParkingSpaceException;
import com.smartparking.parkingspace.application.ParkingSpaceService;
import com.smartparking.parkingspace.dto.response.ParkingSpacesResponse;
import com.smartparking.service.config.SecurityConfig;
import com.smartparking.global.error.ErrorCode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(AdminParkingSpaceControllerTest.Config.class)
class AdminParkingSpaceControllerTest {
    static final UUID ACTOR = UUID.randomUUID();
    static final UUID FLOOR = UUID.randomUUID();
    static final UUID ZONE = UUID.randomUUID();
    static final String PATH = "/api/v1/admin/floors/" + FLOOR + "/parking-spaces";
    static final String BODY = """
        {"version":0,"spaces":[{"zoneId":"%s","spaceNumber":"A-01","type":"GENERAL",
        "active":true,"mapPolygon":[{"x":0.1,"y":0.1},{"x":0.4,"y":0.1},{"x":0.4,"y":0.4}],
        "cameraMappings":[]}]}
        """.formatted(ZONE);

    @Autowired WebApplicationContext context;
    @Autowired ParkingSpaceService service;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var result = new ParkingSpacesResponse(FLOOR, 1L, List.of());
        when(service.save(eq(ACTOR), eq(FLOOR), any())).thenReturn(result);
        when(service.detail(FLOOR)).thenReturn(result);
    }

    @Test
    void adminCanSaveAndReadFloorSpaces() throws Exception {
        mvc.perform(put(PATH).header("Authorization", "Bearer admin")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1));
        mvc.perform(get(PATH).header("Authorization", "Bearer admin"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.floorId").value(FLOOR.toString()));
        verify(service).save(eq(ACTOR), eq(FLOOR), any());
    }

    @Test
    void rejectsAnonymousAndNonAdmin() throws Exception {
        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        mvc.perform(get(PATH).header("Authorization", "Bearer user"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidNestedCoordinatesAndMalformedBodies() throws Exception {
        for (String invalid : List.of("{}", "{", BODY.replace("\"x\":0.4", "\"x\":1.4"))) {
            mvc.perform(put(PATH).header("Authorization", "Bearer admin")
                    .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.traceId").isString())
                .andExpect(jsonPath("$.details").isMap());
        }
        verifyNoInteractions(service);
    }

    @Test
    void mapsMissingFloorAndVersionConflict() throws Exception {
        when(service.detail(FLOOR)).thenThrow(new ParkingSpaceException(ErrorCode.PARKING_FLOOR_NOT_FOUND, "Missing"));
        mvc.perform(get(PATH).header("Authorization", "Bearer admin"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PARKING_FLOOR_NOT_FOUND"));
        when(service.save(eq(ACTOR), eq(FLOOR), any()))
            .thenThrow(new ParkingSpaceException(ErrorCode.VERSION_CONFLICT, "Changed"));
        mvc.perform(put(PATH).header("Authorization", "Bearer admin")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class, AdminParkingSpaceController.class, ParkingSpaceErrorHandler.class})
    static class Config {
        @Bean ParkingSpaceService service() { return mock(ParkingSpaceService.class); }
        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().findAndAddModules().build(); }
        @Bean JsonAuthenticationEntryPoint entryPoint(ObjectMapper mapper) { return new JsonAuthenticationEntryPoint(mapper); }
        @Bean JsonAccessDeniedHandler denied(ObjectMapper mapper) { return new JsonAccessDeniedHandler(mapper); }
        @Bean FirebaseAuthenticationFilter filter(JsonAuthenticationEntryPoint entryPoint) {
            FirebaseTokenVerifier verifier = token -> {
                if (!Set.of("admin", "user").contains(token)) {
                    throw new FirebaseTokenVerificationException(new IllegalArgumentException("Rejected"));
                }
                return token;
            };
            var auth = new AuthService(null) {
                @Override public CurrentUserPrincipal authenticate(String uid) {
                    return new CurrentUserPrincipal(ACTOR, uid, uid.equals("admin") ? Role.ADMIN : Role.USER);
                }
            };
            return new FirebaseAuthenticationFilter(verifier, auth, entryPoint);
        }
    }
}
