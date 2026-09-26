package com.smartparking.auth.application;

import com.smartparking.global.security.CurrentUserPrincipal;
import com.smartparking.user.domain.User;
import com.smartparking.user.domain.UserStatus;
import com.smartparking.user.infrastructure.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CurrentUserPrincipal authenticate(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
            .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
            .orElseThrow(() -> new BadCredentialsException("No active internal user is associated with this token"));
        return new CurrentUserPrincipal(user.getId(), user.getFirebaseUid(), user.getRole());
    }
}
