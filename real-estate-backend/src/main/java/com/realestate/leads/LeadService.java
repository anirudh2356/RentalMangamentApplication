package com.realestate.leads;

import com.realestate.leads.dto.LeadRequest;
import com.realestate.leads.dto.LeadResponse;
import com.realestate.leads.model.Lead;
import com.realestate.leads.model.LeadSource;
import com.realestate.leads.model.LeadStatus;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.notifications.NotificationService;
import com.realestate.notifications.model.NotificationType;
import com.realestate.property.model.Property;
import com.realestate.property.repository.PropertyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final NotificationService notificationService;

    public LeadService(
            LeadRepository leadRepository,
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            NotificationService notificationService
    ) {
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<LeadResponse> list(
            String email,
            String search,
            LeadStatus status,
            LeadSource source,
            Long interestedPropertyId
    ) {
        User broker = requireBroker(email);
        return leadRepository.searchOwned(
                        broker.getId(),
                        blankToNull(search),
                        status,
                        source,
                        interestedPropertyId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LeadResponse create(String email, LeadRequest request) {
        User broker = requireBroker(email);
        Lead lead = new Lead();
        lead.setBroker(broker);
        applyRequest(lead, request, broker);
        Lead saved = leadRepository.save(lead);
        notificationService.createForBroker(
                broker,
                NotificationType.LEAD,
                "New lead added",
                "A new lead was added: " + saved.getName(),
                "LEAD",
                saved.getId()
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public LeadResponse get(String email, Long id) {
        User broker = requireBroker(email);
        return toResponse(findOwnedLead(id, broker.getId()));
    }

    @Transactional
    public LeadResponse update(String email, Long id, LeadRequest request) {
        User broker = requireBroker(email);
        Lead lead = findOwnedLead(id, broker.getId());
        applyRequest(lead, request, broker);
        return toResponse(leadRepository.save(lead));
    }

    @Transactional
    public void delete(String email, Long id) {
        User broker = requireBroker(email);
        Lead lead = findOwnedLead(id, broker.getId());
        leadRepository.delete(lead);
    }

    @Transactional(readOnly = true)
    public long count(String email) {
        return leadRepository.countByBrokerId(requireBroker(email).getId());
    }

    @Transactional(readOnly = true)
    public long countNew(String email) {
        return leadRepository.countByBrokerIdAndStatus(
                requireBroker(email).getId(),
                LeadStatus.NEW
        );
    }

    private User requireBroker(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required"));
        if (user.getRole() != Role.BROKER || !user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required");
        }
        return user;
    }

    private Lead findOwnedLead(Long id, Long brokerId) {
        return leadRepository.findOwnedById(id, brokerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));
    }

    private void applyRequest(Lead lead, LeadRequest request, User broker) {
        lead.setName(request.name().trim());
        lead.setEmail(blankToNull(request.email()));
        lead.setPhone(blankToNull(request.phone()));
        lead.setSource(request.source());
        lead.setStatus(request.status());
        lead.setInterestedProperty(findOwnedProperty(request.interestedPropertyId(), broker.getId()));
        lead.setBudgetMin(request.budgetMin());
        lead.setBudgetMax(request.budgetMax());
        lead.setPreferredLocation(blankToNull(request.preferredLocation()));
        lead.setNotes(blankToNull(request.notes()));
    }

    private Property findOwnedProperty(Long propertyId, Long brokerId) {
        if (propertyId == null) {
            return null;
        }
        return propertyRepository.findByIdAndBrokerId(propertyId, brokerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Interested property not found"
                ));
    }

    private LeadResponse toResponse(Lead lead) {
        Property property = lead.getInterestedProperty();
        return new LeadResponse(
                lead.getId(),
                lead.getName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getSource(),
                lead.getStatus(),
                property == null ? null : property.getId(),
                property == null ? null : property.getTitle(),
                lead.getBudgetMin(),
                lead.getBudgetMax(),
                lead.getPreferredLocation(),
                lead.getNotes(),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
