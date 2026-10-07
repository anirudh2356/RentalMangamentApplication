package com.realestate.documents.dto;

import com.realestate.documents.model.DocumentType;

import java.time.Instant;

public record DocumentResponse(
        Long id,
        String originalFileName,
        String contentType,
        long fileSize,
        DocumentType documentType,
        String description,
        Long propertyId,
        String propertyTitle,
        Long leadId,
        String leadName,
        Long dealId,
        String dealSummary,
        Instant uploadedAt,
        Instant updatedAt
) {
}
