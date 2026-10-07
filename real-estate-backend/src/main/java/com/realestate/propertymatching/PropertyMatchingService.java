package com.realestate.propertymatching;

import com.realestate.leads.model.Lead;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.property.model.Property;
import com.realestate.property.model.PropertyStatus;
import com.realestate.property.repository.PropertyRepository;
import com.realestate.propertymatching.dto.PropertyMatchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PropertyMatchingService {

    private final LeadRepository leadRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public PropertyMatchingService(
            LeadRepository leadRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository
    ) {
        this.leadRepository = leadRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PropertyMatchResponse> findMatches(String email, Long leadId) {
        User broker = userRepository.findByEmailIgnoreCase(email)
                .filter(user -> user.isEnabled() && user.getRole() == Role.BROKER)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Broker access required"
                ));

        Lead lead = leadRepository.findOwnedById(leadId, broker.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));

        Property preference = lead.getInterestedProperty();
        List<Property> candidates = propertyRepository.findByBrokerIdOrderByCreatedAtDesc(broker.getId());
        List<PropertyMatchResponse> matches = new ArrayList<>();

        for (Property property : candidates) {
            if (!property.isAvailable() || property.getStatus() != PropertyStatus.ACTIVE) {
                continue;
            }
            List<String> reasons = new ArrayList<>();
            int criteria = 0;
            int matched = 0;

            if (hasText(lead.getPreferredLocation())) {
                criteria++;
                if (containsLocation(property, lead.getPreferredLocation())) {
                    matched++;
                    reasons.add("Matches preferred location");
                }
            }

            if (lead.getBudgetMin() != null || lead.getBudgetMax() != null) {
                criteria++;
                boolean withinMin = lead.getBudgetMin() == null
                        || property.getPrice().compareTo(lead.getBudgetMin()) >= 0;
                boolean withinMax = lead.getBudgetMax() == null
                        || property.getPrice().compareTo(lead.getBudgetMax()) <= 0;
                if (withinMin && withinMax) {
                    matched++;
                    reasons.add("Within budget");
                }
            }

            if (preference != null && preference.getListingType() != null) {
                criteria++;
                if (property.getListingType() == preference.getListingType()) {
                    matched++;
                    reasons.add("Matches preferred sale/rent type");
                }
            }

            if (preference != null && preference.getBedrooms() != null) {
                criteria++;
                if (property.getBedrooms() >= preference.getBedrooms()) {
                    matched++;
                    reasons.add("Meets preferred bedroom count");
                }
            }

            if (criteria == 0 || matched > 0) {
                int score = criteria == 0 ? 0 : Math.round((float) matched * 100 / criteria);
                matches.add(toResponse(property, score, reasons));
            }
        }

        matches.sort((first, second) -> {
            int scoreOrder = Integer.compare(second.matchScore(), first.matchScore());
            return scoreOrder != 0 ? scoreOrder : first.title().compareToIgnoreCase(second.title());
        });
        return matches;
    }

    private boolean containsLocation(Property property, String preference) {
        String normalized = preference.toLowerCase(Locale.ROOT).trim();
        return contains(property.getCity(), normalized)
                || contains(property.getState(), normalized)
                || contains(property.getLocation(), normalized);
    }

    private boolean contains(String value, String sought) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(sought);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private PropertyMatchResponse toResponse(Property property, int score, List<String> reasons) {
        return new PropertyMatchResponse(
                property.getId(),
                property.getTitle(),
                property.getPropertyType(),
                property.getListingType(),
                property.getPrice(),
                property.getLocation(),
                property.getCity(),
                property.getState(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getArea(),
                property.getStatus(),
                property.getFurnishingStatus(),
                score,
                List.copyOf(reasons)
        );
    }
}
