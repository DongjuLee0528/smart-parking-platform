package com.smartparking.parkinglot.api;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartparking.auth.application.AuthService;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.global.security.*;
import com.smartparking.global.security.Role;
import com.smartparking.parkinglot.application.*;
import com.smartparking.parkinglot.domain.*;
import com.smartparking.parkinglot.dto.response.*;
import com.smartparking.service.config.SecurityConfig;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringJUnitWebConfig(ParkingLotControllerTest.Config.class)
class ParkingLotControllerTest {
    static final UUID ACTOR = UUID.randomUUID();
    static final UUID LOT = UUID.randomUUID();
    static final String BODY = """
        {"name":"Manual lot","address":"Sample address","latitude":37.5,"longitude":127.0,
         "operatingHours":"Always","feeInformation":"Free",
         "floors":[{"name":"B1","floorOrder":-1,"zones":["A"]}]}
        """;
    @Autowired WebApplicationContext context;
    @Autowired ParkingLotService service;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var response = new ParkingLotDetailResponse(LOT, "Manual lot", "Sample address", 37.5, 127.0,
            "Always", "Free", OperationStatus.INACTIVE, SetupStatus.DRAFT, 0L, List.of());
        when(service.create(eq(ACTOR), any())).thenReturn(response);
        when(service.update(eq(ACTOR), eq(LOT), any())).thenReturn(response);
        when(service.adminDetail(LOT)).thenReturn(response);
        when(service.list(anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(new ParkingLotPage(List.of(), 0, 20, 0));
    }

    @Test
    void adminCreatesAndUpdatesUsingVerifiedActor() throws Exception {
        mvc.perform(post("/api/v1/admin/parking-lots").header("Authorization", "Bearer admin")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/admin/parking-lots/" + LOT))
            .andExpect(jsonPath("$.data.id").value(LOT.toString()))
            .andExpect(jsonPath("$.data.operationStatus").value("INACTIVE"));
        mvc.perform(patch("/api/v1/admin/parking-lots/" + LOT).header("Authorization", "Bearer admin")
                .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("\"name\":", "\"version\":0,\"name\":")))
            .andExpect(status().isOk());
        verify(service).create(eq(ACTOR), any());
        verify(service).update(eq(ACTOR), eq(LOT), any());
    }

    @Test
    void rejectsAnonymousAndNormalUserWritesBeforeServiceCall() throws Exception {
        mvc.perform(post("/api/v1/admin/parking-lots").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        mvc.perform(patch("/api/v1/admin/parking-lots/" + LOT).header("Authorization", "Bearer user")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(service);
    }

    @Test
    void validatesRequiredFieldsCoordinatesNestedFloorsAndMalformedInput() throws Exception {
        for (String invalid : List.of("{}", "{", BODY.replace("37.5", "91.0"),
                BODY.replace("\"B1\"", "\"\""), BODY.replace("[\"A\"]", "[null]"),
                BODY.replace("[{\"name\":\"B1\",\"floorOrder\":-1,\"zones\":[\"A\"]}]", "[null]"))) {
            mvc.perform(post("/api/v1/admin/parking-lots").header("Authorization", "Bearer admin")
                    .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.traceId").isString()).andExpect(jsonPath("$.details").isMap());
        }
        mvc.perform(get("/api/v1/parking-lots/not-a-uuid").header("Authorization", "Bearer user"))
            .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(service);
    }

    @Test
    void mapsNotFoundAndVersionConflictToStandardJson() throws Exception {
        when(service.detail(LOT)).thenThrow(new ParkingLotException(ErrorCode.PARKING_LOT_NOT_FOUND, "Not found"));
        mvc.perform(get("/api/v1/parking-lots/" + LOT).header("Authorization", "Bearer user"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PARKING_LOT_NOT_FOUND"));
        when(service.update(eq(ACTOR), eq(LOT), any()))
            .thenThrow(new ParkingLotException(ErrorCode.VERSION_CONFLICT, "Changed"));
        mvc.perform(patch("/api/v1/admin/parking-lots/" + LOT).header("Authorization", "Bearer admin")
                .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("\"name\":", "\"version\":0,\"name\":")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void authenticatedUserListsWhileAdminCanInspectDraft() throws Exception {
        mvc.perform(get("/api/v1/parking-lots").header("Authorization", "Bearer user"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isArray());
        verify(service).list(0, 20, "name,asc", false);
        mvc.perform(get("/api/v1/admin/parking-lots/" + LOT).header("Authorization", "Bearer admin"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.setupStatus").value("DRAFT"));
    }

    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class, AdminParkingLotController.class, ParkingLotController.class, ParkingLotErrorHandler.class})
    static class Config {
        @Bean ParkingLotService service() { return mock(ParkingLotService.class); }
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
