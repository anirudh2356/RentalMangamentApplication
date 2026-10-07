package com.realestate.reports.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SiteVisitReport(
        long totalVisits,
        long upcomingVisits,
        long completedVisits,
        long cancelledVisits,
        Map<String, Long> byDate,
        Map<String, Long> byProperty,
        Map<String, Long> byLead,
        List<SiteVisitRow> visits
) {
    public record SiteVisitRow(
            Long id,
            Instant scheduledAt,
            String status,
            Long propertyId,
            String propertyTitle,
            Long leadId,
            String leadName,
            String notes
    ) {
    }
}
