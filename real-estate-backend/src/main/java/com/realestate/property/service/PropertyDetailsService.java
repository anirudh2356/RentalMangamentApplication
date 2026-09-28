package com.realestate.property.service;

import com.realestate.property.dto.PropertyDetailsResponse;
import com.realestate.property.entity.Property;
import com.realestate.property.entity.PropertyAmenity;
import com.realestate.property.entity.PropertyImage;
import com.realestate.property.entity.PropertyStatus;
import com.realestate.property.entity.PropertyTour;
import com.realestate.property.repository.PropertyAmenityRepository;
import com.realestate.property.repository.PropertyImageRepository;
import com.realestate.property.repository.PropertySimilarRepository;
import com.realestate.property.repository.PropertyTourRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PropertyDetailsService {

    private final PropertyImageRepository propertyImageRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;
    private final PropertyTourRepository propertyTourRepository;
    // Also used to look up a property / check it exists
    private final PropertySimilarRepository similarRepository;

    public PropertyDetailsService(PropertyImageRepository propertyImageRepository,
                                  PropertyAmenityRepository propertyAmenityRepository,
                                  PropertyTourRepository propertyTourRepository,
                                  PropertySimilarRepository similarRepository) {
        this.propertyImageRepository = propertyImageRepository;
        this.propertyAmenityRepository = propertyAmenityRepository;
        this.propertyTourRepository = propertyTourRepository;
        this.similarRepository = similarRepository;
    }

    // ---------- Images ----------

    public List<PropertyImage> getImages(Long propertyId) {
        return propertyImageRepository.findByPropertyIdOrderByDisplayOrderAsc(propertyId);
    }

    public Optional<PropertyImage> addImage(Long propertyId, PropertyImage image) {
        if (!similarRepository.existsById(propertyId)) {
            return Optional.empty();
        }
        image.setPropertyId(propertyId);
        return Optional.of(propertyImageRepository.save(image));
    }

    // ---------- Amenities ----------

    public List<PropertyAmenity> getAmenities(Long propertyId) {
        return propertyAmenityRepository.findByPropertyId(propertyId);
    }

    public Optional<PropertyAmenity> addAmenity(Long propertyId, PropertyAmenity amenity) {
        if (!similarRepository.existsById(propertyId)) {
            return Optional.empty();
        }
        amenity.setPropertyId(propertyId);
        return Optional.of(propertyAmenityRepository.save(amenity));
    }

    // ---------- Video + Virtual tour ----------

    public PropertyTour getTour(Long propertyId) {
        return propertyTourRepository.findByPropertyId(propertyId)
                .orElseGet(() -> PropertyTour.builder().propertyId(propertyId).build());
    }

    public Optional<PropertyTour> saveTour(Long propertyId, PropertyTour incoming) {
        if (!similarRepository.existsById(propertyId)) {
            return Optional.empty();
        }
        PropertyTour tour = propertyTourRepository.findByPropertyId(propertyId)
                .orElseGet(() -> PropertyTour.builder().propertyId(propertyId).build());
        tour.setVideoUrl(incoming.getVideoUrl());
        tour.setVirtualTourUrl(incoming.getVirtualTourUrl());
        return Optional.of(propertyTourRepository.save(tour));
    }

    // ---------- Similar properties ----------

    public Optional<List<Property>> getSimilarProperties(Long propertyId) {
        return similarRepository.findById(propertyId)
                .map(p -> similarRepository.findTop4ByPropertyTypeAndStatusAndIdNot(
                        p.getPropertyType(), PropertyStatus.ACTIVE, p.getId()));
    }

    // ---------- Everything for the details page ----------

    public Optional<PropertyDetailsResponse> getFullDetails(Long propertyId) {
        return similarRepository.findById(propertyId)
                .map(p -> new PropertyDetailsResponse(
                        p,
                        getImages(propertyId),
                        getAmenities(propertyId),
                        getTour(propertyId),
                        similarRepository.findTop4ByPropertyTypeAndStatusAndIdNot(
                                p.getPropertyType(), PropertyStatus.ACTIVE, p.getId())));
    }
}