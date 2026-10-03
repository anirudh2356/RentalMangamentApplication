package com.realestate.messaging.dto;

import java.time.Instant;

public record MessageDto(
        Long id,
        Long senderId,
        String senderName,
        String content,
        Instant sentAt
) {
}
