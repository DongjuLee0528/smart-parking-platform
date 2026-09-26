package com.smartparking.user.application;

import com.smartparking.user.domain.User;
import com.smartparking.user.dto.response.UserResponse;
import com.smartparking.user.infrastructure.UserRepository;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BadCredentialsException("Authenticated user no longer exists"));
        return UserResponse.from(user);
    }
}
