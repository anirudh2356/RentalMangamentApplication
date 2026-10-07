package com.realestate.deals.dto;

import com.realestate.deals.model.DealStage;
import com.realestate.deals.model.DealStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DealRequest(
        @NotNull Long leadId,
        @NotNull Long propertyId,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 2) BigDecimal dealValue,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") @Digits(integer = 3, fraction = 2) BigDecimal commissionRate,
        @NotNull DealStage stage,
        @NotNull DealStatus status,
        LocalDate expectedClosingDate,
        @Size(max = 5000) String notes
) {
    public DealRequest {
        notes = notes == null || notes.isBlank() ? null : notes.trim();
    }
}
