package com.realestate.messaging.dto;

import java.time.Instant;

public record ConversationSummaryDto(
        Long id,
        Long otherUserId,
        String otherUserName,
        String latestMessage,
        Instant latestMessageAt,
        long unreadCount
) {
}
