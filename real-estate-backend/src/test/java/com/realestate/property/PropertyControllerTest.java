package com.realestate.property;

import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.property.dto.PropertyResponse;
import com.realestate.property.model.ListingType;
import com.realestate.property.model.PropertyStatus;
import com.realestate.property.model.PropertyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PropertyController.class)
@Import({SecurityConfig.class, PropertyExceptionHandler.class})
class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropertyService propertyService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void brokerCanCreatePropertyUsingAuthenticatedEmail() throws Exception {
        when(propertyService.create(eq("broker@example.local"), any()))
                .thenReturn(response());

        mockMvc.perform(post("/api/properties")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(72))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(propertyService).create(eq("broker@example.local"), any());
    }

    @Test
    void brokerCanRetrieveOwnPropertyList() throws Exception {
        when(propertyService.list("broker@example.local")).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/properties")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(72))
                .andExpect(jsonPath("$[0].title").value("Harbour View"));
    }

    @Test
    void nonBrokerCannotAccessPropertyApi() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .with(user("buyer@example.local").roles("BUYER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidPropertyDataReturnsCleanValidationError() throws Exception {
        mockMvc.perform(post("/api/properties")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","description":"short","propertyType":null,"listingType":"SALE",
                                 "price":0,"area":-1,"bedrooms":-1,"bathrooms":0,"location":"Road",
                                 "city":"","state":"","parkingAvailable":false,"available":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Property validation failed"))
                .andExpect(jsonPath("$.fields.title").exists())
                .andExpect(jsonPath("$.fields.price").exists())
                .andExpect(jsonPath("$.fields.city").exists());
    }

    private static String validRequest() {
        return """
                {"title":"Harbour View","description":"A bright apartment near the waterfront.",
                 "propertyType":"APARTMENT","listingType":"SALE","price":12500000,"area":1100,
                 "bedrooms":2,"bathrooms":2,"location":"Harbour Road","city":"Mumbai",
                 "state":"Maharashtra","pincode":"400001","furnishingStatus":"UNFURNISHED",
                 "parkingAvailable":true,"available":true}
                """;
    }

    private static PropertyResponse response() {
        return new PropertyResponse(
                72L,
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
                PropertyStatus.ACTIVE,
                null,
                null
        );
    }
}
