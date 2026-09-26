package com.smartparking.service.config;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartparking.auth.application.AuthService;
import com.smartparking.global.security.CurrentUserPrincipal;
import com.smartparking.global.security.FirebaseAuthenticationFilter;
import com.smartparking.global.security.FirebaseTokenVerifier;
import com.smartparking.global.security.JsonAccessDeniedHandler;
import com.smartparking.global.security.JsonAuthenticationEntryPoint;
import com.smartparking.global.security.Role;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(SecurityConfigTest.TestConfig.class)
class SecurityConfigTest {

    @Autowired
    WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void rejectsUnauthenticatedAdminRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
    }

    @Test
    void rejectsUserFromAdminRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test").header("Authorization", "Bearer user-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.traceId").isString())
            .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void allowsAdminRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }

    @Test
    void allowsUserRequestOutsideAdminApi() throws Exception {
        mockMvc.perform(get("/api/v1/test").header("Authorization", "Bearer user-token"))
            .andExpect(status().isOk());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().findAndAddModules().build();
        }

        @Bean
        JsonAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
            return new JsonAuthenticationEntryPoint(objectMapper);
        }

        @Bean
        JsonAccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
            return new JsonAccessDeniedHandler(objectMapper);
        }

        @Bean
        FirebaseAuthenticationFilter firebaseAuthenticationFilter(ObjectMapper objectMapper) {
            FirebaseTokenVerifier tokenVerifier = token -> token.replace("-token", "-uid");
            AuthService authService = new AuthService(null) {
                @Override
                public CurrentUserPrincipal authenticate(String firebaseUid) {
                    Role role = firebaseUid.equals("admin-uid") ? Role.ADMIN : Role.USER;
                    return new CurrentUserPrincipal(UUID.randomUUID(), firebaseUid, role);
                }
            };
            return new FirebaseAuthenticationFilter(
                tokenVerifier,
                authService,
                authenticationEntryPoint(objectMapper)
            );
        }

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {

        @GetMapping({"/api/v1/test", "/api/v1/admin/test"})
        String test() {
            return "ok";
        }
    }
}
