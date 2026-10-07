package com.realestate.followup.dto;

import com.realestate.followup.model.FollowUpPriority;
import com.realestate.followup.model.FollowUpStatus;

import java.time.Instant;

public record FollowUpResponse(
        Long id,
        String title,
        String description,
        Long leadId,
        String leadName,
        Instant followUpAt,
        FollowUpStatus status,
        FollowUpPriority priority,
        Instant createdAt,
        Instant updatedAt
) {
}
