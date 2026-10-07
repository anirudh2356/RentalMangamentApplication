package com.realestate.propertymatching.dto;

import com.realestate.property.model.FurnishingStatus;
import com.realestate.property.model.ListingType;
import com.realestate.property.model.PropertyStatus;
import com.realestate.property.model.PropertyType;

import java.math.BigDecimal;
import java.util.List;

public record PropertyMatchResponse(
        Long id,
        String title,
        PropertyType propertyType,
        ListingType listingType,
        BigDecimal price,
        String location,
        String city,
        String state,
        Integer bedrooms,
        Integer bathrooms,
        BigDecimal area,
        PropertyStatus status,
        FurnishingStatus furnishingStatus,
        int matchScore,
        List<String> matchReasons
) {
}
