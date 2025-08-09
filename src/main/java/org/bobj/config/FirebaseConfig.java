package org.bobj.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    // docker-compose 환경변수로 전달: FIREBASE_CONFIG_PATH=/config/firebase/serviceAccountKey.json
    @Value("${FIREBASE_CONFIG_PATH:}")
    private String firebaseConfigPath;

    @Bean
    public FirebaseMessaging firebaseMessaging() throws Exception {
        // 1) ENV 경로 우선
        InputStream in;
        if (firebaseConfigPath != null && !firebaseConfigPath.isBlank()) {
            in = new FileInputStream(firebaseConfigPath);
        } else {
            // 2) 없으면 classpath로 폴백 (firebase/serviceAccountKey.json)
            ClassPathResource cpr = new ClassPathResource("firebase/serviceAccountKey.json");
            if (!cpr.exists()) {
                throw new IllegalStateException(
                    "Firebase serviceAccountKey.json not found. " +
                        "Set FIREBASE_CONFIG_PATH or add classpath:firebase/serviceAccountKey.json");
            }
            in = cpr.getInputStream();
        }

        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(in))
            .build();

        // 중복 초기화 방지
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
        return FirebaseMessaging.getInstance();
    }
}
