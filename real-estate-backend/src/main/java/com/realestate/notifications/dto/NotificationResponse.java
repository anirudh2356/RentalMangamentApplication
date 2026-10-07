package com.realestate.notifications.dto;

import com.realestate.notifications.model.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        NotificationType type,
        boolean read,
        String relatedEntityType,
        Long relatedEntityId,
        Instant createdAt
) {
}
