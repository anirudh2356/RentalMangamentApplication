package com.realestate.property.service;

import com.realestate.property.entity.Property;
import com.realestate.property.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    // Get all properties
    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    // Get property by ID
    public Optional<Property> getPropertyById(Long id) {
        return propertyRepository.findById(id);
    }

    // Save property
    public Property saveProperty(Property property) {
        return propertyRepository.save(property);
    }

    // Delete property
    public void deleteProperty(Long id) {
        propertyRepository.deleteById(id);
    }
}