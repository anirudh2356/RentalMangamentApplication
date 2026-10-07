package com.realestate.commissions;

import com.realestate.commissions.dto.CommissionRecordResponse;
import com.realestate.commissions.dto.CommissionSummaryResponse;
import com.realestate.deals.model.Deal;
import com.realestate.deals.model.DealStatus;
import com.realestate.deals.repository.DealRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CommissionService {

    private final DealRepository dealRepository;
    private final UserRepository userRepository;

    public CommissionService(DealRepository dealRepository, UserRepository userRepository) {
        this.dealRepository = dealRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CommissionSummaryResponse summary(String email) {
        User broker = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required"));
        if (!broker.isEnabled() || broker.getRole() != Role.BROKER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required");
        }

        List<CommissionRecordResponse> commissions = dealRepository
                .findOwnedByStatus(broker.getId(), DealStatus.WON)
                .stream()
                .map(this::toRecord)
                .toList();
        BigDecimal total = commissions.stream()
                .map(CommissionRecordResponse::commissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // The Deal model has no payment state, so won commissions are earned but unmarked as paid.
        return new CommissionSummaryResponse(
                total,
                total,
                null,
                false,
                commissions
        );
    }

    private CommissionRecordResponse toRecord(Deal deal) {
        return new CommissionRecordResponse(
                deal.getId(),
                deal.getLead().getId(),
                deal.getLead().getName(),
                deal.getProperty().getId(),
                deal.getProperty().getTitle(),
                deal.getProperty().getLocation() + ", " + deal.getProperty().getCity(),
                deal.getDealValue(),
                deal.getCommissionRate(),
                deal.getCommissionAmount(),
                deal.getStatus(),
                deal.getUpdatedAt()
        );
    }
}
