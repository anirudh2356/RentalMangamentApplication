package com.realestate.propertymatching;

import com.realestate.propertymatching.dto.PropertyMatchResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/property-matching")
public class PropertyMatchingController {

    private final PropertyMatchingService matchingService;

    public PropertyMatchingController(PropertyMatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping("/lead/{leadId}")
    public List<PropertyMatchResponse> matches(Authentication authentication, @PathVariable Long leadId) {
        return matchingService.findMatches(authentication.getName(), leadId);
    }
}
