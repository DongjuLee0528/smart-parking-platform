package com.smartparking.user.dto.response;

import com.smartparking.global.security.Role;
import com.smartparking.user.domain.User;
import com.smartparking.user.domain.UserStatus;
import java.util.UUID;

public record UserResponse(UUID id, String email, String displayName, Role role, UserStatus status) {

    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getRole(),
            user.getStatus()
        );
    }
}
