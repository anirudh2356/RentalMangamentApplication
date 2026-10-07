package com.realestate.property;

import com.realestate.property.dto.PropertyRequest;
import com.realestate.property.dto.PropertyResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    public List<PropertyResponse> list(Authentication authentication) {
        return propertyService.list(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse create(
            Authentication authentication,
            @Valid @RequestBody PropertyRequest request
    ) {
        return propertyService.create(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public PropertyResponse get(Authentication authentication, @PathVariable Long id) {
        return propertyService.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public PropertyResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody PropertyRequest request
    ) {
        return propertyService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(Authentication authentication, @PathVariable Long id) {
        propertyService.deactivate(authentication.getName(), id);
    }
}
