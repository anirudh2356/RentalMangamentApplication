package com.realestate.property.controller;

import com.realestate.property.dto.PropertyDetailsResponse;
import com.realestate.property.entity.Property;
import com.realestate.property.entity.PropertyAmenity;
import com.realestate.property.entity.PropertyImage;
import com.realestate.property.entity.PropertyTour;
import com.realestate.property.service.PropertyDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class PropertyDetailsController {

    private final PropertyDetailsService detailsService;

    public PropertyDetailsController(PropertyDetailsService detailsService) {
        this.detailsService = detailsService;
    }

    // ---------- Images ----------

    @GetMapping("/{id}/media")
    public ResponseEntity<List<PropertyImage>> getMedia(@PathVariable Long id) {
        return ResponseEntity.ok(detailsService.getImages(id));
    }

    @PostMapping("/{id}/media")
    public ResponseEntity<PropertyImage> addMedia(@PathVariable Long id,
                                                  @RequestBody PropertyImage image) {
        return detailsService.addImage(id, image)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---------- Amenities ----------

    @GetMapping("/{id}/amenities")
    public ResponseEntity<List<PropertyAmenity>> getAmenities(@PathVariable Long id) {
        return ResponseEntity.ok(detailsService.getAmenities(id));
    }

    @PostMapping("/{id}/amenities")
    public ResponseEntity<PropertyAmenity> addAmenity(@PathVariable Long id,
                                                      @RequestBody PropertyAmenity amenity) {
        return detailsService.addAmenity(id, amenity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---------- Video + Virtual tour ----------

    @GetMapping("/{id}/virtual-tour")
    public ResponseEntity<PropertyTour> getTour(@PathVariable Long id) {
        return ResponseEntity.ok(detailsService.getTour(id));
    }

    @PutMapping("/{id}/virtual-tour")
    public ResponseEntity<PropertyTour> saveTour(@PathVariable Long id,
                                                 @RequestBody PropertyTour tour) {
        return detailsService.saveTour(id, tour)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---------- Similar properties ----------

    // GET /api/properties/{id}/similar
    @GetMapping("/{id}/similar")
    public ResponseEntity<List<Property>> getSimilar(@PathVariable Long id) {
        return detailsService.getSimilarProperties(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

        // ---------- Everything in one call ----------

    // GET /api/properties/{id}/details
    @GetMapping("/{id}/details")
    public ResponseEntity<PropertyDetailsResponse> getDetails(@PathVariable Long id) {
        return detailsService.getFullDetails(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}