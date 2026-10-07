package com.realestate.sitevisit.dto;

import com.realestate.sitevisit.model.SiteVisitStatus;

import java.time.Instant;

public record SiteVisitResponse(
        Long id,
        Long leadId,
        String leadName,
        Long propertyId,
        String propertyTitle,
        String propertyLocation,
        Instant scheduledAt,
        SiteVisitStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
