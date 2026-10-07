package com.realestate.reports.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DealReport(
        long totalDeals,
        long activeDeals,
        long wonDeals,
        long lostDeals,
        BigDecimal totalDealValue,
        BigDecimal averageDealValue,
        Map<String, Long> byStage,
        Map<String, Long> byStatus,
        Map<String, Long> byDate,
        List<DealRow> deals
) {
    public record DealRow(
            Long id,
            String leadName,
            String propertyTitle,
            BigDecimal dealValue,
            String stage,
            String status,
            Instant createdAt
    ) {
    }
}
