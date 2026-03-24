package com.mysticcard.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = resolveCredentials();
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin initialized successfully");
            }
        } catch (Exception e) {
            log.error("Error initializing Firebase", e);
        }
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        // 1. Env var: FIREBASE_SERVICE_ACCOUNT_JSON (base64-encoded JSON) — for Coolify/Docker
        String base64Json = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
        if (base64Json != null && !base64Json.isBlank()) {
            log.info("Firebase: loading credentials from FIREBASE_SERVICE_ACCOUNT_JSON env var");
            byte[] jsonBytes = Base64.getDecoder().decode(base64Json.trim());
            return GoogleCredentials.fromStream(new ByteArrayInputStream(jsonBytes));
        }

        // 2. Classpath file: serviceAccountKey.json — for local dev
        ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
        if (resource.exists()) {
            log.info("Firebase: loading credentials from serviceAccountKey.json");
            return GoogleCredentials.fromStream(resource.getInputStream());
        }

        // 3. Fallback: GOOGLE_APPLICATION_CREDENTIALS env var (GCP/ADC)
        log.warn("Firebase: falling back to application default credentials");
        return GoogleCredentials.getApplicationDefault();
    }
}
