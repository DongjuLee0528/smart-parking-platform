package com.smartparking.camera.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartparking.auth.application.AuthService;
import com.smartparking.camera.application.CameraException;
import com.smartparking.camera.application.CameraService;
import com.smartparking.camera.domain.CameraStatus;
import com.smartparking.camera.dto.response.CameraPage;
import com.smartparking.camera.dto.response.CameraResponse;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.global.security.*;
import com.smartparking.global.security.Role;
import com.smartparking.service.config.SecurityConfig;
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

@SpringJUnitWebConfig(AdminCameraControllerTest.Config.class)
class AdminCameraControllerTest {
    static final UUID ACTOR = UUID.randomUUID();
    static final UUID CAMERA = UUID.randomUUID();
    static final UUID ZONE = UUID.randomUUID();
    static final String BODY = """
        {"zoneId":"%s","name":"Entrance","streamKeyRef":"CAMERA_ENTRANCE_RTSP"}
        """.formatted(ZONE);
    @Autowired WebApplicationContext context;
    @Autowired CameraService service;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var response = new CameraResponse(CAMERA, UUID.randomUUID(), UUID.randomUUID(), ZONE,
            "Entrance", "CAMERA_ENTRANCE_RTSP", CameraStatus.OFFLINE, null, 0L);
        when(service.create(eq(ACTOR), any())).thenReturn(response);
        when(service.update(eq(ACTOR), eq(CAMERA), any())).thenReturn(response);
        when(service.detail(CAMERA)).thenReturn(response);
        when(service.list(anyInt(), anyInt(), anyString()))
            .thenReturn(new CameraPage(List.of(response), 0, 20, 1));
    }

    @Test
    void adminCreatesUpdatesAndReadsCamera() throws Exception {
        mvc.perform(post("/api/v1/admin/cameras").header("Authorization", "Bearer admin")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/admin/cameras/" + CAMERA))
            .andExpect(jsonPath("$.data.status").value("OFFLINE"))
            .andExpect(jsonPath("$.data.zoneId").value(ZONE.toString()));
        mvc.perform(patch("/api/v1/admin/cameras/" + CAMERA).header("Authorization", "Bearer admin")
                .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("{", "{\"configVersion\":0,")))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/cameras/" + CAMERA).header("Authorization", "Bearer admin"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/cameras").header("Authorization", "Bearer admin"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        verify(service).create(eq(ACTOR), any());
        verify(service).update(eq(ACTOR), eq(CAMERA), any());
    }

    @Test
    void rejectsAnonymousAndNonAdminAccess() throws Exception {
        mvc.perform(post("/api/v1/admin/cameras").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
        mvc.perform(get("/api/v1/admin/cameras").header("Authorization", "Bearer user"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMalformedBodiesAndRawRtspUrls() throws Exception {
        for (String invalid : List.of("{}", "{", BODY.replace("CAMERA_ENTRANCE_RTSP", "rtsp://camera.example/stream"))) {
            mvc.perform(post("/api/v1/admin/cameras").header("Authorization", "Bearer admin")
                    .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.traceId").isString())
                .andExpect(jsonPath("$.details").isMap());
        }
        mvc.perform(get("/api/v1/admin/cameras/not-a-uuid").header("Authorization", "Bearer admin"))
            .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(service);
    }

    @Test
    void mapsMissingCameraVersionConflictAndInvalidPaging() throws Exception {
        when(service.detail(CAMERA)).thenThrow(new CameraException(ErrorCode.CAMERA_NOT_FOUND, "Camera not found"));
        mvc.perform(get("/api/v1/admin/cameras/" + CAMERA).header("Authorization", "Bearer admin"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAMERA_NOT_FOUND"));
        when(service.update(eq(ACTOR), eq(CAMERA), any()))
            .thenThrow(new CameraException(ErrorCode.VERSION_CONFLICT, "Changed"));
        mvc.perform(patch("/api/v1/admin/cameras/" + CAMERA).header("Authorization", "Bearer admin")
                .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("{", "{\"configVersion\":0,")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
        when(service.list(eq(0), eq(101), anyString()))
            .thenThrow(new CameraException(ErrorCode.VALIDATION_FAILED, "Invalid page"));
        mvc.perform(get("/api/v1/admin/cameras?size=101").header("Authorization", "Bearer admin"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class, AdminCameraController.class, CameraErrorHandler.class})
    static class Config {
        @Bean CameraService service() { return mock(CameraService.class); }
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
