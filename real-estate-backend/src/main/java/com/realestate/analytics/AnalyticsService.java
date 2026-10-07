package com.realestate.analytics;

import com.realestate.analytics.dto.AnalyticsOverviewResponse;
import com.realestate.deals.model.DealStage;
import com.realestate.deals.model.DealStatus;
import com.realestate.deals.repository.DealRepository;
import com.realestate.leads.model.LeadStatus;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.property.model.PropertyStatus;
import com.realestate.property.repository.PropertyRepository;
import com.realestate.sitevisit.model.SiteVisitStatus;
import com.realestate.sitevisit.repository.SiteVisitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final PropertyRepository propertyRepository;
    private final SiteVisitRepository siteVisitRepository;
    private final DealRepository dealRepository;

    public AnalyticsService(
            UserRepository userRepository,
            LeadRepository leadRepository,
            PropertyRepository propertyRepository,
            SiteVisitRepository siteVisitRepository,
            DealRepository dealRepository
    ) {
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.propertyRepository = propertyRepository;
        this.siteVisitRepository = siteVisitRepository;
        this.dealRepository = dealRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse overview(String email) {
        User broker = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required"));
        if (!broker.isEnabled() || broker.getRole() != Role.BROKER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required");
        }
        Long brokerId = broker.getId();

        Map<String, Long> leadPipeline = counts(LeadStatus.values(),
                status -> leadRepository.countByBrokerIdAndStatus(brokerId, status));
        Map<String, Long> propertyStatusCounts = counts(PropertyStatus.values(),
                status -> propertyRepository.countByBrokerIdAndStatus(brokerId, status));
        Map<String, Long> dealPipeline = counts(DealStage.values(),
                stage -> dealRepository.countByBrokerIdAndStage(brokerId, stage));

        BigDecimal commission = dealRepository.sumCommissionByBrokerIdAndStatus(brokerId, DealStatus.WON);
        long soldRented = propertyStatusCounts.get(PropertyStatus.SOLD.name())
                + propertyStatusCounts.get(PropertyStatus.RENTED.name());

        return new AnalyticsOverviewResponse(
                leadRepository.countByBrokerId(brokerId),
                leadPipeline.get(LeadStatus.NEW.name()),
                leadPipeline.get(LeadStatus.QUALIFIED.name()),
                leadPipeline.get(LeadStatus.WON.name()),
                leadPipeline.get(LeadStatus.LOST.name()),
                leadPipeline,
                propertyRepository.countByBrokerId(brokerId),
                propertyStatusCounts.get(PropertyStatus.ACTIVE.name()),
                soldRented,
                propertyStatusCounts,
                siteVisitRepository.countUpcoming(brokerId, List.of(
                        SiteVisitStatus.SCHEDULED, SiteVisitStatus.RESCHEDULED
                )),
                siteVisitRepository.countByBrokerIdAndStatus(brokerId, SiteVisitStatus.COMPLETED),
                dealRepository.countByBrokerIdAndStatus(brokerId, DealStatus.ACTIVE),
                dealRepository.countByBrokerIdAndStatus(brokerId, DealStatus.WON),
                dealRepository.countByBrokerIdAndStatus(brokerId, DealStatus.LOST),
                dealPipeline,
                dealRepository.sumDealValueByBrokerId(brokerId),
                commission,
                commission,
                false
        );
    }

    private static <E extends Enum<E>> Map<String, Long> counts(
            E[] values,
            EnumCount<E> counter
    ) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (E value : values) {
            result.put(value.name(), counter.count(value));
        }
        return result;
    }

    @FunctionalInterface
    private interface EnumCount<E extends Enum<E>> {
        long count(E value);
    }
}
