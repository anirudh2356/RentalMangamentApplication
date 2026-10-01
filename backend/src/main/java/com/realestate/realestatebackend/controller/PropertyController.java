package com.realestate.realestatebackend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.realestate.realestatebackend.entity.Property;
import com.realestate.realestatebackend.service.PropertyService;

@RestController
@RequestMapping("/api/properties")
@CrossOrigin(origins = "http://localhost:5173")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    public List<Property> getAllProperties() {
        return propertyService.getAllProperties();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Property> getPropertyById(@PathVariable Long id) {
        Property property = propertyService.getPropertyById(id);

        if (property == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(property);
    }

    @PostMapping
    public Property createProperty(@RequestBody Property property) {
        return propertyService.saveProperty(property);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Property> updateProperty(
            @PathVariable Long id,
            @RequestBody Property property) {

        Property existingProperty = propertyService.getPropertyById(id);

        if (existingProperty == null) {
            return ResponseEntity.notFound().build();
        }

        property.setId(id);
        return ResponseEntity.ok(propertyService.saveProperty(property));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(@PathVariable Long id) {

        Property existingProperty = propertyService.getPropertyById(id);

        if (existingProperty == null) {
            return ResponseEntity.notFound().build();
        }

        propertyService.deleteProperty(id);
        return ResponseEntity.noContent().build();
    }
}