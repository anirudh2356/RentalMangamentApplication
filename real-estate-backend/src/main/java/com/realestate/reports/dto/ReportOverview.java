package com.realestate.reports.dto;

import java.math.BigDecimal;

public record ReportOverview(
        long totalLeads,
        long newLeads,
        long qualifiedLeads,
        long activeProperties,
        long upcomingSiteVisits,
        long completedSiteVisits,
        long activeDeals,
        long wonDeals,
        long lostDeals,
        BigDecimal totalDealValue,
        BigDecimal totalCommission,
        BigDecimal pendingCommission
) {
}
