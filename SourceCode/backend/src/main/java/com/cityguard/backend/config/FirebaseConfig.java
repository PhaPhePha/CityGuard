package com.cityguard.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cấu hình khởi tạo Firebase Admin SDK.
 * Chỉ khởi tạo khi tệp credential tồn tại để không làm gián đoạn môi trường test/CI chưa có key.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    private static final String FIREBASE_PROJECT_ID = "cityguardapp-1f6c5";
    private static final String DEFAULT_CREDENTIALS_PATH = "firebase-service-account.json";

    @Value("${FIREBASE_CREDENTIALS_PATH:}")
    private String firebaseCredentialsPath;

    @PostConstruct
    public void initFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        String credentialPath = resolveCredentialPath();
        Path path = Paths.get(credentialPath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            log.warn("Không tìm thấy tệp credential Firebase tại '{}'. Bỏ qua khởi tạo FirebaseApp.", credentialPath);
            return;
        }

        try (InputStream serviceAccount = Files.newInputStream(path)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(FIREBASE_PROJECT_ID)
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Đã khởi tạo FirebaseApp thành công cho projectId={}", FIREBASE_PROJECT_ID);
        } catch (IOException e) {
            log.warn("Không thể đọc tệp credential Firebase tại '{}': {}", credentialPath, e.getMessage());
        }
    }

    private String resolveCredentialPath() {
        String googleAppCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (googleAppCreds != null && !googleAppCreds.isBlank()) {
            return googleAppCreds.trim();
        }
        if (firebaseCredentialsPath != null && !firebaseCredentialsPath.isBlank()) {
            return firebaseCredentialsPath.trim();
        }
        String envPath = System.getenv("FIREBASE_CREDENTIALS_PATH");
        if (envPath != null && !envPath.isBlank()) {
            return envPath.trim();
        }
        return DEFAULT_CREDENTIALS_PATH;
    }
}

