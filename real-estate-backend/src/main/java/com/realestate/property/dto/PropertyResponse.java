package com.realestate.property.dto;

import com.realestate.property.model.FurnishingStatus;
import com.realestate.property.model.ListingType;
import com.realestate.property.model.PropertyStatus;
import com.realestate.property.model.PropertyType;

import java.math.BigDecimal;
import java.time.Instant;

public record PropertyResponse(
        Long id,
        String title,
        String description,
        PropertyType propertyType,
        ListingType listingType,
        BigDecimal price,
        BigDecimal area,
        Integer bedrooms,
        Integer bathrooms,
        String location,
        String city,
        String state,
        String pincode,
        FurnishingStatus furnishingStatus,
        boolean parkingAvailable,
        boolean available,
        PropertyStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
