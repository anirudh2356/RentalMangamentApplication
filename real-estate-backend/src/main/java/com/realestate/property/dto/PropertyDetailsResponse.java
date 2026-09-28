package com.realestate.property.dto;

import com.realestate.property.entity.Property;
import com.realestate.property.entity.PropertyAmenity;
import com.realestate.property.entity.PropertyImage;
import com.realestate.property.entity.PropertyTour;

import java.util.List;

public record PropertyDetailsResponse(
        Property property,
        List<PropertyImage> images,
        List<PropertyAmenity> amenities,
        PropertyTour tour,
        List<Property> similar
) {}