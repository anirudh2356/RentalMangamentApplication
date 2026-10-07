package com.realestate.property.dto;

import com.realestate.property.model.FurnishingStatus;
import com.realestate.property.model.ListingType;
import com.realestate.property.model.PropertyStatus;
import com.realestate.property.model.PropertyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PropertyRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(min = 10, max = 5000) String description,
        @NotNull PropertyType propertyType,
        @NotNull ListingType listingType,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal price,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal area,
        @NotNull @Min(0) Integer bedrooms,
        @NotNull @Min(0) Integer bathrooms,
        @NotBlank @Size(max = 255) String location,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String state,
        @Size(max = 20) String pincode,
        FurnishingStatus furnishingStatus,
        boolean parkingAvailable,
        boolean available,
        PropertyStatus status
) {
}
