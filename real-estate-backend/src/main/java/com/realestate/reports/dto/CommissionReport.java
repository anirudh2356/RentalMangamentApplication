package com.realestate.reports.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CommissionReport(
        BigDecimal totalCommission,
        BigDecimal pendingCommission,
        Map<String, BigDecimal> byDate,
        List<CommissionRow> commissions
) {
    public record CommissionRow(
            Long dealId,
            String leadName,
            String propertyTitle,
            BigDecimal dealValue,
            BigDecimal commissionRate,
            BigDecimal commissionAmount,
            Instant date
    ) {
    }
}
