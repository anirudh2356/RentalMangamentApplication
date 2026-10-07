package com.realestate.search;

import com.realestate.documents.model.Document;
import com.realestate.documents.repository.DocumentRepository;
import com.realestate.deals.model.Deal;
import com.realestate.deals.repository.DealRepository;
import com.realestate.followup.model.FollowUp;
import com.realestate.followup.repository.FollowUpRepository;
import com.realestate.leads.model.Lead;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.property.model.Property;
import com.realestate.property.repository.PropertyRepository;
import com.realestate.search.dto.SearchResponse;
import com.realestate.search.dto.SearchResult;
import com.realestate.sitevisit.model.SiteVisit;
import com.realestate.sitevisit.repository.SiteVisitRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private static final int RESULTS_PER_CATEGORY = 5;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final PropertyRepository propertyRepository;
    private final DealRepository dealRepository;
    private final SiteVisitRepository siteVisitRepository;
    private final FollowUpRepository followUpRepository;
    private final DocumentRepository documentRepository;

    public SearchService(
            UserRepository userRepository,
            LeadRepository leadRepository,
            PropertyRepository propertyRepository,
            DealRepository dealRepository,
            SiteVisitRepository siteVisitRepository,
            FollowUpRepository followUpRepository,
            DocumentRepository documentRepository
    ) {
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.propertyRepository = propertyRepository;
        this.dealRepository = dealRepository;
        this.siteVisitRepository = siteVisitRepository;
        this.followUpRepository = followUpRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(String email, String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 2) {
            return new SearchResponse(List.of());
        }

        User broker = userRepository.findByEmailIgnoreCase(email)
                .filter(user -> user.isEnabled() && user.getRole() == Role.BROKER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required"));
        Long brokerId = broker.getId();
        PageRequest limit = PageRequest.of(0, RESULTS_PER_CATEGORY);
        List<SearchResult> results = new ArrayList<>();

        leadRepository.searchOwnedForWorkspace(brokerId, normalizedQuery, limit).stream()
                .map(lead -> result("LEAD", lead.getId(), lead.getName(), lead.getEmail(), "leads"))
                .forEach(results::add);
        propertyRepository.searchOwnedForWorkspace(brokerId, normalizedQuery, limit).stream()
                .map(property -> result(
                        "PROPERTY",
                        property.getId(),
                        property.getTitle(),
                        property.getCity() + ", " + property.getState(),
                        "properties"
                ))
                .forEach(results::add);
        dealRepository.searchOwnedForWorkspace(brokerId, normalizedQuery, limit).stream()
                .map(deal -> result(
                        "DEAL",
                        deal.getId(),
                        deal.getLead().getName() + " - " + deal.getProperty().getTitle(),
                        deal.getProperty().getCity() + ", " + deal.getProperty().getState(),
                        "deals"
                ))
                .forEach(results::add);
        siteVisitRepository.searchOwnedForWorkspace(brokerId, normalizedQuery, limit).stream()
                .map(visit -> result(
                        "SITE_VISIT",
                        visit.getId(),
                        visit.getLead().getName() + " - " + visit.getProperty().getTitle(),
                        visit.getProperty().getCity() + ", " + visit.getProperty().getState(),
                        "site-visits"
                ))
                .forEach(results::add);
        followUpRepository.searchOwnedForWorkspace(brokerId, normalizedQuery, limit).stream()
                .map(followUp -> result(
                        "FOLLOW_UP",
                        followUp.getId(),
                        followUp.getTitle(),
                        followUp.getLead().getName(),
                        "follow-ups"
                ))
                .forEach(results::add);
        documentRepository.searchOwnedForWorkspace(brokerId, normalizedQuery, limit).stream()
                .map(document -> result(
                        "DOCUMENT",
                        document.getId(),
                        document.getOriginalFileName(),
                        document.getDescription(),
                        "documents"
                ))
                .forEach(results::add);
        userRepository.searchTeamMembersForWorkspace(brokerId, normalizedQuery, limit).stream()
                .map(member -> result("TEAM_MEMBER", member.getId(), member.getName(), member.getEmail(), "team"))
                .forEach(results::add);

        return new SearchResponse(List.copyOf(results));
    }

    private static SearchResult result(String type, Long id, String title, String subtitle, String target) {
        return new SearchResult(type, id, title, subtitle, target);
    }
}
