package com.realestate.followup.dto;

import com.realestate.followup.model.FollowUpPriority;
import com.realestate.followup.model.FollowUpStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record FollowUpRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 3000) String description,
        @NotNull Long leadId,
        @NotNull Instant followUpAt,
        @NotNull FollowUpStatus status,
        @NotNull FollowUpPriority priority
) {
    public FollowUpRequest {
        title = title == null ? null : title.trim();
        description = description == null || description.isBlank() ? null : description.trim();
    }
}
