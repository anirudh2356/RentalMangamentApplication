package com.realestate.sitevisit;

import com.realestate.leads.model.Lead;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.notifications.NotificationService;
import com.realestate.notifications.model.NotificationType;
import com.realestate.property.model.Property;
import com.realestate.property.repository.PropertyRepository;
import com.realestate.sitevisit.dto.SiteVisitRequest;
import com.realestate.sitevisit.dto.SiteVisitResponse;
import com.realestate.sitevisit.model.SiteVisit;
import com.realestate.sitevisit.model.SiteVisitStatus;
import com.realestate.sitevisit.repository.SiteVisitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SiteVisitService {

    private final SiteVisitRepository siteVisitRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final PropertyRepository propertyRepository;
    private final NotificationService notificationService;

    public SiteVisitService(
            SiteVisitRepository siteVisitRepository,
            UserRepository userRepository,
            LeadRepository leadRepository,
            PropertyRepository propertyRepository,
            NotificationService notificationService
    ) {
        this.siteVisitRepository = siteVisitRepository;
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.propertyRepository = propertyRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<SiteVisitResponse> list(String email, SiteVisitStatus status) {
        User broker = requireBroker(email);
        return siteVisitRepository.findOwnedVisits(broker.getId(), status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SiteVisitResponse> upcoming(String email) {
        User broker = requireBroker(email);
        return siteVisitRepository.findUpcoming(
                        broker.getId(),
                        List.of(SiteVisitStatus.SCHEDULED, SiteVisitStatus.RESCHEDULED)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SiteVisitResponse create(String email, SiteVisitRequest request) {
        User broker = requireBroker(email);
        SiteVisit visit = new SiteVisit();
        visit.setBroker(broker);
        applyRequest(visit, request, broker);
        SiteVisit saved = siteVisitRepository.save(visit);
        notificationService.createForBroker(
                broker,
                NotificationType.SITE_VISIT,
                "Site visit scheduled",
                "A site visit was scheduled for " + saved.getLead().getName(),
                "SITE_VISIT",
                saved.getId()
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SiteVisitResponse get(String email, Long id) {
        User broker = requireBroker(email);
        return toResponse(findOwnedVisit(id, broker.getId()));
    }

    @Transactional
    public SiteVisitResponse update(String email, Long id, SiteVisitRequest request) {
        User broker = requireBroker(email);
        SiteVisit visit = findOwnedVisit(id, broker.getId());
        applyRequest(visit, request, broker);
        return toResponse(siteVisitRepository.save(visit));
    }

    @Transactional
    public void delete(String email, Long id) {
        User broker = requireBroker(email);
        siteVisitRepository.delete(findOwnedVisit(id, broker.getId()));
    }

    private User requireBroker(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required"));
        if (user.getRole() != Role.BROKER || !user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required");
        }
        return user;
    }

    private SiteVisit findOwnedVisit(Long id, Long brokerId) {
        return siteVisitRepository.findOwnedById(id, brokerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site visit not found"));
    }

    private void applyRequest(SiteVisit visit, SiteVisitRequest request, User broker) {
        Lead lead = leadRepository.findOwnedById(request.leadId(), broker.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));
        Property property = propertyRepository.findByIdAndBrokerId(request.propertyId(), broker.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        visit.setLead(lead);
        visit.setProperty(property);
        visit.setScheduledAt(request.scheduledAt());
        visit.setStatus(request.status());
        visit.setNotes(request.notes());
    }

    private SiteVisitResponse toResponse(SiteVisit visit) {
        return new SiteVisitResponse(
                visit.getId(),
                visit.getLead().getId(),
                visit.getLead().getName(),
                visit.getProperty().getId(),
                visit.getProperty().getTitle(),
                visit.getProperty().getLocation() + ", " + visit.getProperty().getCity(),
                visit.getScheduledAt(),
                visit.getStatus(),
                visit.getNotes(),
                visit.getCreatedAt(),
                visit.getUpdatedAt()
        );
    }
}
