package com.smartparking.user.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartparking.auth.application.AuthService;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartparking.global.security.FirebaseAuthenticationFilter;
import com.smartparking.global.security.FirebaseTokenVerificationException;
import com.smartparking.global.security.FirebaseTokenVerifier;
import com.smartparking.global.security.CurrentUserPrincipal;
import com.smartparking.global.security.JsonAuthenticationEntryPoint;
import com.smartparking.global.security.Role;
import com.smartparking.user.application.UserService;
import com.smartparking.user.domain.User;
import com.smartparking.user.domain.UserStatus;
import com.smartparking.user.dto.response.UserResponse;
import com.smartparking.user.infrastructure.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class UserControllerTest {

    private final FirebaseTokenVerifier tokenVerifier = mock(FirebaseTokenVerifier.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = mock(UserService.class);
    private final MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(new UserController(userService))
        .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer container,
                NativeWebRequest request,
                org.springframework.web.bind.support.WebDataBinderFactory binderFactory
            ) {
                return ((Authentication) request.getUserPrincipal()).getPrincipal();
            }
        })
        .addFilters(new FirebaseAuthenticationFilter(
            tokenVerifier,
            new AuthService(userRepository),
            new JsonAuthenticationEntryPoint(JsonMapper.builder().findAndAddModules().build())
        ))
        .build();

    private final UUID userId = UUID.randomUUID();

    UserControllerTest() {
        User user = new User(userId, "verified-uid", "user@example.com", "Parking User", Role.USER, UserStatus.ACTIVE);
        when(tokenVerifier.verify("valid-token")).thenReturn("verified-uid");
        when(userRepository.findByFirebaseUid("verified-uid")).thenReturn(Optional.of(user));
        when(userService.getCurrentUser(any(UUID.class))).thenReturn(UserResponse.from(user));
    }

    @Test
    void returnsCurrentInternalUserForValidFirebaseToken() throws Exception {
        var principal = new CurrentUserPrincipal(userId, "verified-uid", Role.USER);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            principal.role().authorities()
        );

        mockMvc.perform(get("/api/v1/me")
                .principal(authentication)
                .header("Authorization", "Bearer valid-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(userId.toString()))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andExpect(jsonPath("$.data.displayName").value("Parking User"))
            .andExpect(jsonPath("$.data.role").value("USER"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(userRepository).findByFirebaseUid("verified-uid");
        verify(userService).getCurrentUser(userId);
    }

    @Test
    void rejectsMissingToken() throws Exception {
        assertUnauthorized(null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic abc", "Bearer", "Bearer ", "Bearer token with spaces"})
    void rejectsMalformedToken(String header) throws Exception {
        assertUnauthorized(header);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-token", "expired-token"})
    void rejectsInvalidOrExpiredFirebaseToken(String token) throws Exception {
        when(tokenVerifier.verify(token)).thenThrow(new FirebaseTokenVerificationException(new RuntimeException("rejected")));

        assertUnauthorized("Bearer " + token);
    }

    private void assertUnauthorized(String authorization) throws Exception {
        var request = get("/api/v1/me");
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        mockMvc.perform(request)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"))
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.traceId").isString())
            .andExpect(jsonPath("$.details").isMap());
    }
}
