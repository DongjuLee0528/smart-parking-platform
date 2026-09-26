package com.smartparking.global.security;

public interface FirebaseTokenVerifier {
    String verify(String idToken);
}
