package com.realestate.reports.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record LeadReport(
        long totalLeads,
        Map<String, Long> byStatus,
        Map<String, Long> bySource,
        List<LeadRow> leads
) {
    public record LeadRow(
            Long id,
            String name,
            String source,
            String status,
            String interestedProperty,
            Instant createdAt
    ) {
    }
}
