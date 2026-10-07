package com.realestate.deals.dto;

import com.realestate.deals.model.DealStage;
import com.realestate.deals.model.DealStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DealResponse(
        Long id,
        Long leadId,
        String leadName,
        Long propertyId,
        String propertyTitle,
        String propertyLocation,
        BigDecimal dealValue,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        DealStage stage,
        DealStatus status,
        LocalDate expectedClosingDate,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
