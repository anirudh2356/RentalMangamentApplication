package com.realestate.commissions.dto;

import java.math.BigDecimal;
import java.util.List;

public record CommissionSummaryResponse(
        BigDecimal totalCommission,
        BigDecimal pendingCommission,
        BigDecimal paidCommission,
        boolean paymentTrackingSupported,
        List<CommissionRecordResponse> commissions
) {
}
