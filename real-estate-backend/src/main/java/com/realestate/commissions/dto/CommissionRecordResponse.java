package com.realestate.commissions.dto;

import com.realestate.deals.model.DealStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record CommissionRecordResponse(
        Long dealId,
        Long leadId,
        String leadName,
        Long propertyId,
        String propertyTitle,
        String propertyLocation,
        BigDecimal dealValue,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        DealStatus dealStatus,
        Instant date
) {
}
