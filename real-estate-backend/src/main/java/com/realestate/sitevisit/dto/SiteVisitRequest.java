package com.realestate.sitevisit.dto;

import com.realestate.sitevisit.model.SiteVisitStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record SiteVisitRequest(
        @NotNull Long leadId,
        @NotNull Long propertyId,
        @NotNull Instant scheduledAt,
        @NotNull SiteVisitStatus status,
        @Size(max = 3000) String notes
) {
    public SiteVisitRequest {
        notes = notes == null || notes.isBlank() ? null : notes.trim();
    }
}
