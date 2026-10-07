package com.realestate.search;

import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.search.dto.SearchResponse;
import com.realestate.search.dto.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import(SecurityConfig.class)
class SearchControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private SearchService searchService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void authenticatedBrokerSearchUsesPrincipalAndReturnsOnlyPublicSearchFields() throws Exception {
        when(searchService.search("broker@example.local", "RaHuL"))
                .thenReturn(new SearchResponse(List.of(
                        new SearchResult("LEAD", 4L, "Rahul Kumar", "rahul@example.com", "leads")
                )));

        mockMvc.perform(get("/api/search")
                        .param("q", "RaHuL")
                        .param("brokerId", "999")
                        .param("userId", "888")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].type").value("LEAD"))
                .andExpect(jsonPath("$.results[0].title").value("Rahul Kumar"))
                .andExpect(jsonPath("$.results[0].subtitle").value("rahul@example.com"))
                .andExpect(jsonPath("$.results[0].navigationTarget").value("leads"))
                .andExpect(jsonPath("$.results[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.results[0].role").doesNotExist());

        verify(searchService).search("broker@example.local", "RaHuL");
        verify(searchService, never()).search("other@example.local", "RaHuL");
    }

    @Test
    void unauthenticatedAndNonBrokerSearchAreRejected() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "Rahul"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/search")
                        .param("q", "Rahul")
                        .with(user("buyer@example.local").roles("BUYER")))
                .andExpect(status().isForbidden());
        verify(searchService, never()).search("buyer@example.local", "Rahul");
    }

    @Test
    void acceptsMissingOrShortQueryForEmptySearchResponse() throws Exception {
        when(searchService.search("broker@example.local", null)).thenReturn(new SearchResponse(List.of()));
        when(searchService.search("broker@example.local", "r")).thenReturn(new SearchResponse(List.of()));

        mockMvc.perform(get("/api/search").with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isEmpty());
        mockMvc.perform(get("/api/search")
                        .param("q", "r")
                        .with(user("broker@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isEmpty());
    }
}
