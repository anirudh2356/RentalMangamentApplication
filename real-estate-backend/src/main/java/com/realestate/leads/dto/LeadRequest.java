package com.realestate.leads.dto;

import com.realestate.leads.model.LeadSource;
import com.realestate.leads.model.LeadStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LeadRequest(
        @NotBlank @Size(max = 120) String name,
        @Email @Size(max = 254) String email,
        @Pattern(regexp = "^[+()0-9 .-]{7,30}$") String phone,
        @NotNull LeadSource source,
        @NotNull LeadStatus status,
        Long interestedPropertyId,
        @DecimalMin("0.0") @Digits(integer = 13, fraction = 2) BigDecimal budgetMin,
        @DecimalMin("0.0") @Digits(integer = 13, fraction = 2) BigDecimal budgetMax,
        @Size(max = 255) String preferredLocation,
        @Size(max = 5000) String notes
) {
    public LeadRequest {
        email = blankToNull(email);
        phone = blankToNull(phone);
        preferredLocation = blankToNull(preferredLocation);
        notes = blankToNull(notes);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
