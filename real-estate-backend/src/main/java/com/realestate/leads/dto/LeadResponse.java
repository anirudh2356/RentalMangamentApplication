package com.realestate.leads.dto;

import com.realestate.leads.model.LeadSource;
import com.realestate.leads.model.LeadStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record LeadResponse(
        Long id,
        String name,
        String email,
        String phone,
        LeadSource source,
        LeadStatus status,
        Long interestedPropertyId,
        String interestedPropertyTitle,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        String preferredLocation,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
