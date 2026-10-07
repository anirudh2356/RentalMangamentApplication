package com.realestate.followup;

import com.realestate.followup.dto.FollowUpRequest;
import com.realestate.followup.dto.FollowUpResponse;
import com.realestate.followup.model.FollowUp;
import com.realestate.followup.model.FollowUpPriority;
import com.realestate.followup.model.FollowUpStatus;
import com.realestate.followup.repository.FollowUpRepository;
import com.realestate.leads.model.Lead;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.notifications.NotificationService;
import com.realestate.notifications.model.NotificationType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final NotificationService notificationService;

    public FollowUpService(
            FollowUpRepository followUpRepository,
            UserRepository userRepository,
            LeadRepository leadRepository,
            NotificationService notificationService
    ) {
        this.followUpRepository = followUpRepository;
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<FollowUpResponse> list(
            String email,
            FollowUpStatus status,
            FollowUpPriority priority,
            Long leadId,
            Instant fromDate,
            Instant toDate,
            String search
    ) {
        User broker = requireBroker(email);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before or equal to to");
        }
        return followUpRepository.findOwned(
                        broker.getId(), status, priority, leadId, fromDate, toDate, blankToNull(search)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FollowUpResponse create(String email, FollowUpRequest request) {
        User broker = requireBroker(email);
        FollowUp followUp = new FollowUp();
        followUp.setBroker(broker);
        applyRequest(followUp, request, broker);
        FollowUp saved = followUpRepository.save(followUp);
        notificationService.createForBroker(
                broker,
                NotificationType.FOLLOW_UP,
                "Follow-up scheduled",
                "A follow-up was scheduled for " + saved.getLead().getName(),
                "FOLLOW_UP",
                saved.getId()
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public FollowUpResponse get(String email, Long id) {
        User broker = requireBroker(email);
        return toResponse(findOwned(id, broker.getId()));
    }

    @Transactional
    public FollowUpResponse update(String email, Long id, FollowUpRequest request) {
        User broker = requireBroker(email);
        FollowUp followUp = findOwned(id, broker.getId());
        applyRequest(followUp, request, broker);
        return toResponse(followUpRepository.save(followUp));
    }

    @Transactional
    public void delete(String email, Long id) {
        User broker = requireBroker(email);
        followUpRepository.delete(findOwned(id, broker.getId()));
    }

    private User requireBroker(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required"));
        if (!user.isEnabled() || user.getRole() != Role.BROKER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required");
        }
        return user;
    }

    private FollowUp findOwned(Long id, Long brokerId) {
        return followUpRepository.findOwnedById(id, brokerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Follow-up not found"));
    }

    private void applyRequest(FollowUp followUp, FollowUpRequest request, User broker) {
        Lead lead = leadRepository.findOwnedById(request.leadId(), broker.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));
        followUp.setTitle(request.title());
        followUp.setDescription(request.description());
        followUp.setLead(lead);
        followUp.setFollowUpAt(request.followUpAt());
        followUp.setStatus(request.status());
        followUp.setPriority(request.priority());
    }

    private FollowUpResponse toResponse(FollowUp followUp) {
        return new FollowUpResponse(
                followUp.getId(),
                followUp.getTitle(),
                followUp.getDescription(),
                followUp.getLead().getId(),
                followUp.getLead().getName(),
                followUp.getFollowUpAt(),
                followUp.getStatus(),
                followUp.getPriority(),
                followUp.getCreatedAt(),
                followUp.getUpdatedAt()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
