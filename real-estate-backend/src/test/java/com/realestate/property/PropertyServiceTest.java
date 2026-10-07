package com.realestate.property;

import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.property.dto.PropertyRequest;
import com.realestate.property.model.ListingType;
import com.realestate.property.model.Property;
import com.realestate.property.model.PropertyStatus;
import com.realestate.property.model.PropertyType;
import com.realestate.property.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyServiceTest {

    private final PropertyRepository propertyRepository = org.mockito.Mockito.mock(PropertyRepository.class);
    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private PropertyService propertyService;
    private User broker;
    private User anotherBroker;

    @BeforeEach
    void setUp() {
        propertyService = new PropertyService(propertyRepository, userRepository);
        broker = user(17L, "broker@example.local", Role.BROKER);
        anotherBroker = user(28L, "other@example.local", Role.BROKER);
        when(userRepository.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));
        when(userRepository.findByEmailIgnoreCase("other@example.local")).thenReturn(Optional.of(anotherBroker));
    }

    @Test
    void createAssociatesPropertyWithAuthenticatedBrokerAndUsesOwnListing() {
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> {
            Property saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        when(propertyRepository.findByBrokerIdOrderByCreatedAtDesc(17L)).thenReturn(List.of(property(100L, broker)));

        var created = propertyService.create("broker@example.local", request());
        var listed = propertyService.list("broker@example.local");

        assertEquals(100L, created.id());
        assertEquals(PropertyStatus.ACTIVE, created.status());
        assertEquals("Harbour View", listed.getFirst().title());
        verify(propertyRepository).findByBrokerIdOrderByCreatedAtDesc(17L);
    }

    @Test
    void brokerCanUpdateOwnProperty() {
        Property owned = property(100L, broker);
        when(propertyRepository.findByIdAndBrokerId(100L, 17L)).thenReturn(Optional.of(owned));
        when(propertyRepository.save(owned)).thenReturn(owned);

        var updated = propertyService.update("broker@example.local", 100L, request());

        assertEquals("Harbour View", updated.title());
        verify(propertyRepository).save(owned);
    }

    @Test
    void brokerCannotUpdateOrDeleteAnotherBrokersProperty() {
        when(propertyRepository.findByIdAndBrokerId(200L, 17L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                ResponseStatusException.class,
                () -> propertyService.update("broker@example.local", 200L, request())
        ).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                ResponseStatusException.class,
                () -> propertyService.deactivate("broker@example.local", 200L)
        ).getStatusCode());
        verify(propertyRepository, never()).save(any(Property.class));
    }

    @Test
    void deleteDeactivatesOnlyTheOwnedProperty() {
        Property owned = property(100L, broker);
        when(propertyRepository.findByIdAndBrokerId(100L, 17L)).thenReturn(Optional.of(owned));

        propertyService.deactivate("broker@example.local", 100L);

        assertEquals(PropertyStatus.INACTIVE, owned.getStatus());
        assertEquals(false, owned.isAvailable());
        verify(propertyRepository).save(owned);
    }

    @Test
    void nonBrokerCannotUsePropertyService() {
        User buyer = user(3L, "buyer@example.local", Role.BUYER);
        when(userRepository.findByEmailIgnoreCase("buyer@example.local")).thenReturn(Optional.of(buyer));

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(
                ResponseStatusException.class,
                () -> propertyService.list("buyer@example.local")
        ).getStatusCode());
    }

    private static User user(Long id, String email, Role role) {
        User user = new User(id, email);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private static Property property(Long id, User broker) {
        Property property = new Property();
        property.setId(id);
        property.setTitle("Harbour View");
        property.setDescription("A bright apartment near the waterfront.");
        property.setPropertyType(PropertyType.APARTMENT);
        property.setListingType(ListingType.SALE);
        property.setPrice(new BigDecimal("12500000"));
        property.setArea(new BigDecimal("1100"));
        property.setBedrooms(2);
        property.setBathrooms(2);
        property.setLocation("Harbour Road");
        property.setCity("Mumbai");
        property.setState("Maharashtra");
        property.setAvailable(true);
        property.setStatus(PropertyStatus.ACTIVE);
        property.setBroker(broker);
        return property;
    }

    private static PropertyRequest request() {
        return new PropertyRequest(
                "Harbour View",
                "A bright apartment near the waterfront.",
                PropertyType.APARTMENT,
                ListingType.SALE,
                new BigDecimal("12500000"),
                new BigDecimal("1100"),
                2,
                2,
                "Harbour Road",
                "Mumbai",
                "Maharashtra",
                "400001",
                null,
                true,
                true,
                null
        );
    }
}
