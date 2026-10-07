package com.realestate.deals;

import com.realestate.deals.dto.DealResponse;
import com.realestate.deals.model.DealStage;
import com.realestate.deals.model.DealStatus;
import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DealController.class)
@Import({SecurityConfig.class, DealExceptionHandler.class})
class DealControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DealService service;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void brokerCanCreateDealWithCsrf() throws Exception {
        when(service.create(eq("broker@example.local"), any())).thenReturn(response());

        mockMvc.perform(post("/api/deals")
                        .with(user("broker@example.local").roles("BROKER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"leadId":5,"propertyId":8,"dealValue":1000000,"commissionRate":2.5,
                                 "stage":"NEGOTIATION","status":"ACTIVE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.commissionAmount").value(25000.0));

        verify(service).create(eq("broker@example.local"), any());
    }

    @Test
    void dealWriteRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/deals")
                        .with(user("broker@example.local").roles("BROKER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"leadId":5,"propertyId":8,"dealValue":1000000,"commissionRate":2.5,
                                 "stage":"NEGOTIATION","status":"ACTIVE"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonBrokerCannotAccessDealEndpoint() throws Exception {
        mockMvc.perform(post("/api/deals")
                        .with(user("buyer@example.local").roles("BUYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private static DealResponse response() {
        return new DealResponse(
                21L,
                5L,
                "Buyer",
                8L,
                "Apartment",
                "Main Road, Pune",
                new BigDecimal("1000000"),
                new BigDecimal("2.5"),
                new BigDecimal("25000.00"),
                DealStage.NEGOTIATION,
                DealStatus.ACTIVE,
                null,
                null,
                null,
                null
        );
    }
}
