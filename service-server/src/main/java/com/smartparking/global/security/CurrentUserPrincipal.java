package com.smartparking.global.security;

import java.util.UUID;

public record CurrentUserPrincipal(UUID userId, String firebaseUid, Role role) {
}
