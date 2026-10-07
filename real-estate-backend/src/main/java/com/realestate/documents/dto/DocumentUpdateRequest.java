package com.realestate.documents.dto;

import com.realestate.documents.model.DocumentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocumentUpdateRequest(
        @NotNull DocumentType documentType,
        @Size(max = 2000) String description,
        Long propertyId,
        Long leadId,
        Long dealId
) {
}
