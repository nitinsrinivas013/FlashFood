package com.foodconnect.service;

import com.foodconnect.config.AppConfig;
import com.foodconnect.entity.*;
import com.foodconnect.entity.enums.NotificationType;
import com.foodconnect.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages notification creation, persistence, and FCM push dispatch.
 * Notifications are always persisted to the database. FCM push is best-effort.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final DonationRepository donationRepository;
    private final AppConfig appConfig;
    private final FirebaseMessagingService firebaseMessagingService;

    /**
     * Notify nearby NGOs about a new donation.
     */
    @Transactional
    public void notifyNearbyNgos(Long donationId, double lat, double lng) {
        Donation donation = donationRepository.findById(donationId).orElse(null);
        if (donation == null) return;

        List<NgoProfile> nearbyNgos = ngoProfileRepository.findNgosWithinRadius(
                lat, lng, appConfig.getNotificationRadiusKm());

        for (NgoProfile ngo : nearbyNgos) {
            String title = "New Food Available Nearby!";
            String message = String.format("%s has %s %s of %s available for pickup",
                    donation.getRestaurant().getRestaurantName(),
                    donation.getQuantity(), donation.getQuantityUnit(),
                    donation.getFoodName());

            // Persist notification
            Notification notification = Notification.builder()
                    .user(ngo.getUser())
                    .donation(donation)
                    .notificationType(NotificationType.NEW_DONATION_NEARBY)
                    .title(title)
                    .message(message)
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);

            // Send FCM push (best-effort)
            sendPushToUser(ngo.getUser().getId(), title, message, donationId, NotificationType.NEW_DONATION_NEARBY);
        }

        log.info("Notified {} nearby NGOs about donation {}", nearbyNgos.size(), donationId);
    }

    /**
     * Notify restaurant that its donation was accepted.
     */
    @Transactional
    public void notifyDonationAccepted(Donation donation, NgoProfile ngo) {
        String title = "Donation Accepted!";
        String message = String.format("%s has accepted your donation of %s. They will arrive for pickup.",
                ngo.getNgoName(), donation.getFoodName());

        Notification notification = Notification.builder()
                .user(donation.getRestaurant().getUser())
                .donation(donation)
                .notificationType(NotificationType.DONATION_ACCEPTED)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        sendPushToUser(donation.getRestaurant().getUser().getId(), title, message,
                donation.getId(), NotificationType.DONATION_ACCEPTED);
    }

    /**
     * Notify restaurant that donation has been collected.
     */
    @Transactional
    public void notifyDonationCollected(Donation donation, DonationClaim claim) {
        String title = "Donation Collected!";
        String message = String.format("%s has collected your donation of %s.",
                claim.getNgo().getNgoName(), donation.getFoodName());

        Notification notification = Notification.builder()
                .user(donation.getRestaurant().getUser())
                .donation(donation)
                .notificationType(NotificationType.DONATION_COLLECTED)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        sendPushToUser(donation.getRestaurant().getUser().getId(), title, message,
                donation.getId(), NotificationType.DONATION_COLLECTED);
    }

    /**
     * Notify both parties that donation is completed.
     */
    @Transactional
    public void notifyDonationCompleted(Donation donation, DonationClaim claim) {
        String title = "Donation Completed!";

        // Notify restaurant
        String restaurantMsg = String.format("Donation of %s has been completed. Thank you for reducing food waste!",
                donation.getFoodName());
        Notification rNotif = Notification.builder()
                .user(donation.getRestaurant().getUser())
                .donation(donation)
                .notificationType(NotificationType.DONATION_COMPLETED)
                .title(title)
                .message(restaurantMsg)
                .isRead(false)
                .build();
        notificationRepository.save(rNotif);
        sendPushToUser(donation.getRestaurant().getUser().getId(), title, restaurantMsg,
                donation.getId(), NotificationType.DONATION_COMPLETED);

        // Notify NGO
        String ngoMsg = String.format("Pickup of %s is now complete. Great job helping those in need!",
                donation.getFoodName());
        Notification nNotif = Notification.builder()
                .user(claim.getNgo().getUser())
                .donation(donation)
                .notificationType(NotificationType.DONATION_COMPLETED)
                .title(title)
                .message(ngoMsg)
                .isRead(false)
                .build();
        notificationRepository.save(nNotif);
        sendPushToUser(claim.getNgo().getUser().getId(), title, ngoMsg,
                donation.getId(), NotificationType.DONATION_COMPLETED);
    }

    /**
     * Notify assigned NGO that donation was cancelled.
     */
    @Transactional
    public void notifyDonationCancelled(Donation donation) {
        DonationClaim claim = donation.getClaim();
        if (claim == null) return;

        String title = "Donation Cancelled";
        String message = String.format("The donation of %s from %s has been cancelled by the restaurant.",
                donation.getFoodName(), donation.getRestaurant().getRestaurantName());

        Notification notification = Notification.builder()
                .user(claim.getNgo().getUser())
                .donation(donation)
                .notificationType(NotificationType.DONATION_CANCELLED)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        sendPushToUser(claim.getNgo().getUser().getId(), title, message,
                donation.getId(), NotificationType.DONATION_CANCELLED);
    }

    /**
     * Get notifications for a user.
     */
    @Transactional(readOnly = true)
    public List<com.foodconnect.dto.NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> com.foodconnect.dto.NotificationResponse.builder()
                        .id(n.getId())
                        .donationId(n.getDonation() != null ? n.getDonation().getId() : null)
                        .notificationType(n.getNotificationType())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .isRead(n.getIsRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * Mark a notification as read.
     */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new com.foodconnect.exception.ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new com.foodconnect.exception.UnauthorizedActionException("Cannot mark another user's notification");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Send FCM push notification to a user's devices. Best-effort.
     */
    private void sendPushToUser(Long userId, String title, String message,
                                Long donationId, NotificationType type) {
        try {
            List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
            for (DeviceToken token : tokens) {
                firebaseMessagingService.sendNotification(
                        token.getFcmToken(), title, message,
                        donationId != null ? donationId.toString() : null,
                        type.name());
            }
        } catch (Exception e) {
            log.warn("Failed to send push notification to user {}: {}", userId, e.getMessage());
        }
    }
}
