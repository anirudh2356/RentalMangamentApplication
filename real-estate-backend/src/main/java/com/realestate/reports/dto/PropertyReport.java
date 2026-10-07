package com.realestate.reports.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PropertyReport(
        long totalProperties,
        long activeProperties,
        long inactiveProperties,
        long soldProperties,
        long rentedProperties,
        long availableProperties,
        BigDecimal totalPropertyValue,
        BigDecimal averagePropertyValue,
        Map<String, Long> byType,
        Map<String, Long> byListingType,
        Map<String, Long> byStatus,
        List<PropertyRow> properties
) {
    public record PropertyRow(
            Long id,
            String title,
            String propertyType,
            String listingType,
            String status,
            boolean available,
            BigDecimal price,
            String city,
            Instant createdAt
    ) {
    }
}
