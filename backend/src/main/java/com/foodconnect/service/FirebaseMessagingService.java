package com.foodconnect.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Service for sending Firebase Cloud Messaging push notifications.
 * Gracefully degrades when Firebase credentials are not configured.
 */
@Service
@Slf4j
public class FirebaseMessagingService {

    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    private boolean firebaseInitialized = false;

    @PostConstruct
    public void init() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("Firebase credentials path not configured. Push notifications will be disabled. " +
                    "Set FIREBASE_CREDENTIALS_PATH to enable FCM.");
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(new FileInputStream(credentialsPath)))
                        .build();
                FirebaseApp.initializeApp(options);
                firebaseInitialized = true;
                log.info("Firebase initialized successfully");
            } else {
                firebaseInitialized = true;
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}. Push notifications will be disabled.", e.getMessage());
        }
    }

    /**
     * Send a push notification to a specific device.
     *
     * @param fcmToken   the device's FCM registration token
     * @param title      notification title
     * @param body       notification body
     * @param donationId associated donation ID (data payload)
     * @param type       notification type (data payload)
     */
    public void sendNotification(String fcmToken, String title, String body,
                                 String donationId, String type) {
        if (!firebaseInitialized) {
            log.debug("Firebase not initialized, skipping push notification to token: {}...",
                    fcmToken != null && fcmToken.length() > 10 ? fcmToken.substring(0, 10) : fcmToken);
            return;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            if (donationId != null) {
                messageBuilder.putData("donationId", donationId);
            }
            if (type != null) {
                messageBuilder.putData("type", type);
            }

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.debug("FCM notification sent successfully: {}", response);
        } catch (Exception e) {
            log.warn("Failed to send FCM notification to token {}...: {}",
                    fcmToken != null && fcmToken.length() > 10 ? fcmToken.substring(0, 10) : fcmToken,
                    e.getMessage());
        }
    }
}
