package com.realestate.analytics.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AnalyticsOverviewResponse(
        long totalLeads,
        long newLeads,
        long qualifiedLeads,
        long wonLeads,
        long lostLeads,
        Map<String, Long> leadPipeline,
        long totalProperties,
        long activeProperties,
        long soldRentedProperties,
        Map<String, Long> propertyStatusCounts,
        long upcomingSiteVisits,
        long completedSiteVisits,
        long activeDeals,
        long wonDeals,
        long lostDeals,
        Map<String, Long> dealPipeline,
        BigDecimal totalDealValue,
        BigDecimal totalCommission,
        BigDecimal pendingCommission,
        boolean paymentTrackingSupported
) {
}
