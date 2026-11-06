package com.foodconnect.mapper;

import com.foodconnect.dto.NotificationResponse;
import com.foodconnect.entity.Notification;
import org.springframework.stereotype.Component;

/**
 * Maps Notification entities to NotificationResponse DTOs.
 */
@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationResponse.builder()
                .id(notification.getId())
                .donationId(notification.getDonation() != null ? notification.getDonation().getId() : null)
                .notificationType(notification.getNotificationType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
