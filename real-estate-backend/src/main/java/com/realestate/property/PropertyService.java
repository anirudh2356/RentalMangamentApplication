package com.realestate.property;

import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.property.dto.PropertyRequest;
import com.realestate.property.dto.PropertyResponse;
import com.realestate.property.model.Property;
import com.realestate.property.model.PropertyStatus;
import com.realestate.property.repository.PropertyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public PropertyService(PropertyRepository propertyRepository, UserRepository userRepository) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> list(String email) {
        User broker = requireBroker(email);
        return propertyRepository.findByBrokerIdOrderByCreatedAtDesc(broker.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PropertyResponse create(String email, PropertyRequest request) {
        User broker = requireBroker(email);
        Property property = new Property();
        applyRequest(property, request);
        property.setBroker(broker);
        property.setStatus(request.status() == null ? PropertyStatus.ACTIVE : request.status());
        return toResponse(propertyRepository.save(property));
    }

    @Transactional(readOnly = true)
    public PropertyResponse get(String email, Long id) {
        User broker = requireBroker(email);
        return toResponse(findOwnedProperty(id, broker.getId()));
    }

    @Transactional
    public PropertyResponse update(String email, Long id, PropertyRequest request) {
        User broker = requireBroker(email);
        Property property = findOwnedProperty(id, broker.getId());
        applyRequest(property, request);
        if (request.status() != null) {
            property.setStatus(request.status());
        }
        return toResponse(propertyRepository.save(property));
    }

    @Transactional
    public void deactivate(String email, Long id) {
        User broker = requireBroker(email);
        Property property = findOwnedProperty(id, broker.getId());
        property.setAvailable(false);
        property.setStatus(PropertyStatus.INACTIVE);
        propertyRepository.save(property);
    }

    private User requireBroker(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required"));
        if (user.getRole() != Role.BROKER || !user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required");
        }
        return user;
    }

    private Property findOwnedProperty(Long id, Long brokerId) {
        return propertyRepository.findByIdAndBrokerId(id, brokerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
    }

    private void applyRequest(Property property, PropertyRequest request) {
        property.setTitle(request.title().trim());
        property.setDescription(request.description().trim());
        property.setPropertyType(request.propertyType());
        property.setListingType(request.listingType());
        property.setPrice(request.price());
        property.setArea(request.area());
        property.setBedrooms(request.bedrooms());
        property.setBathrooms(request.bathrooms());
        property.setLocation(request.location().trim());
        property.setCity(request.city().trim());
        property.setState(request.state().trim());
        property.setPincode(request.pincode() == null ? null : request.pincode().trim());
        property.setFurnishingStatus(request.furnishingStatus());
        property.setParkingAvailable(request.parkingAvailable());
        property.setAvailable(request.available());
    }

    private PropertyResponse toResponse(Property property) {
        return new PropertyResponse(
                property.getId(),
                property.getTitle(),
                property.getDescription(),
                property.getPropertyType(),
                property.getListingType(),
                property.getPrice(),
                property.getArea(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getLocation(),
                property.getCity(),
                property.getState(),
                property.getPincode(),
                property.getFurnishingStatus(),
                property.isParkingAvailable(),
                property.isAvailable(),
                property.getStatus(),
                property.getCreatedAt(),
                property.getUpdatedAt()
        );
    }
}
