package com.smartparking.service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.smartparking.global.security.FirebaseAdminTokenVerifier;
import com.smartparking.global.security.FirebaseTokenVerifier;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {

    @Bean
    FirebaseApp firebaseApp(
        @Value("${firebase.project-id}") String projectId,
        @Value("${firebase.credentials-path}") String credentialsPath
    ) throws IOException {
        try (FileInputStream credentials = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentials))
                .setProjectId(projectId)
                .build();
            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    FirebaseTokenVerifier firebaseTokenVerifier(FirebaseAuth firebaseAuth) {
        return new FirebaseAdminTokenVerifier(firebaseAuth);
    }
}
