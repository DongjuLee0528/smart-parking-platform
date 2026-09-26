package com.smartparking.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.smartparking.auth.application.AuthService;
import com.smartparking.global.security.CurrentUserPrincipal;
import com.smartparking.global.security.Role;
import com.smartparking.user.domain.User;
import com.smartparking.user.domain.UserStatus;
import com.smartparking.user.infrastructure.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthService authService = new AuthService(userRepository);

    @Test
    void associatesVerifiedFirebaseUidWithActiveInternalUser() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "firebase-uid", "user@example.com", "User", Role.USER, UserStatus.ACTIVE);
        when(userRepository.findByFirebaseUid("firebase-uid")).thenReturn(Optional.of(user));

        CurrentUserPrincipal principal = authService.authenticate("firebase-uid");

        assertThat(principal).isEqualTo(new CurrentUserPrincipal(userId, "firebase-uid", Role.USER));
    }

    @Test
    void rejectsUidWithoutActiveInternalUser() {
        User disabled = new User(
            UUID.randomUUID(),
            "disabled-uid",
            "disabled@example.com",
            "Disabled",
            Role.USER,
            UserStatus.DISABLED
        );
        when(userRepository.findByFirebaseUid("missing-uid")).thenReturn(Optional.empty());
        when(userRepository.findByFirebaseUid("disabled-uid")).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> authService.authenticate("missing-uid"))
            .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> authService.authenticate("disabled-uid"))
            .isInstanceOf(BadCredentialsException.class);
    }
}
