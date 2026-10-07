package com.realestate.deals;

import com.realestate.deals.dto.DealRequest;
import com.realestate.deals.dto.DealResponse;
import com.realestate.deals.model.Deal;
import com.realestate.deals.model.DealStatus;
import com.realestate.deals.repository.DealRepository;
import com.realestate.leads.model.Lead;
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

import java.math.RoundingMode;
import java.util.List;

@Service
public class DealService {

    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final PropertyRepository propertyRepository;
    private final NotificationService notificationService;

    public DealService(
            DealRepository dealRepository,
            UserRepository userRepository,
            LeadRepository leadRepository,
            PropertyRepository propertyRepository,
            NotificationService notificationService
    ) {
        this.dealRepository = dealRepository;
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.propertyRepository = propertyRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<DealResponse> list(String email, String search, com.realestate.deals.model.DealStage stage, DealStatus status) {
        User broker = requireBroker(email);
        String cleanSearch = search == null || search.isBlank() ? null : search.trim();
        return dealRepository.searchOwned(broker.getId(), cleanSearch, stage, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DealResponse create(String email, DealRequest request) {
        User broker = requireBroker(email);
        Deal deal = new Deal();
        deal.setBroker(broker);
        applyRequest(deal, request, broker);
        Deal saved = dealRepository.save(deal);
        notificationService.createForBroker(
                broker,
                NotificationType.DEAL,
                "Deal created",
                "A deal was created for " + saved.getLead().getName(),
                "DEAL",
                saved.getId()
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DealResponse get(String email, Long id) {
        User broker = requireBroker(email);
        return toResponse(findOwnedDeal(id, broker.getId()));
    }

    @Transactional
    public DealResponse update(String email, Long id, DealRequest request) {
        User broker = requireBroker(email);
        Deal deal = findOwnedDeal(id, broker.getId());
        applyRequest(deal, request, broker);
        return toResponse(dealRepository.save(deal));
    }

    @Transactional
    public void delete(String email, Long id) {
        User broker = requireBroker(email);
        dealRepository.delete(findOwnedDeal(id, broker.getId()));
    }

    @Transactional(readOnly = true)
    public long countActive(String email) {
        return dealRepository.countByBrokerIdAndStatus(requireBroker(email).getId(), DealStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public long countWon(String email) {
        return dealRepository.countByBrokerIdAndStatus(requireBroker(email).getId(), DealStatus.WON);
    }

    private User requireBroker(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required"));
        if (!user.isEnabled() || user.getRole() != Role.BROKER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required");
        }
        return user;
    }

    private Deal findOwnedDeal(Long id, Long brokerId) {
        return dealRepository.findOwnedById(id, brokerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deal not found"));
    }

    private void applyRequest(Deal deal, DealRequest request, User broker) {
        Lead lead = leadRepository.findOwnedById(request.leadId(), broker.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));
        Property property = propertyRepository.findByIdAndBrokerId(request.propertyId(), broker.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        deal.setLead(lead);
        deal.setProperty(property);
        deal.setDealValue(request.dealValue());
        deal.setCommissionRate(request.commissionRate());
        deal.setCommissionAmount(request.dealValue()
                .multiply(request.commissionRate())
                .divide(new java.math.BigDecimal("100"), 2, RoundingMode.HALF_UP));
        deal.setStage(request.stage());
        deal.setStatus(request.status());
        deal.setExpectedClosingDate(request.expectedClosingDate());
        deal.setNotes(request.notes());
    }

    private DealResponse toResponse(Deal deal) {
        return new DealResponse(
                deal.getId(),
                deal.getLead().getId(),
                deal.getLead().getName(),
                deal.getProperty().getId(),
                deal.getProperty().getTitle(),
                deal.getProperty().getLocation() + ", " + deal.getProperty().getCity(),
                deal.getDealValue(),
                deal.getCommissionRate(),
                deal.getCommissionAmount(),
                deal.getStage(),
                deal.getStatus(),
                deal.getExpectedClosingDate(),
                deal.getNotes(),
                deal.getCreatedAt(),
                deal.getUpdatedAt()
        );
    }
}
