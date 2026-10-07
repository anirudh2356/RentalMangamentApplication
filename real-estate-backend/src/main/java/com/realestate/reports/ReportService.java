package com.realestate.reports;

import com.realestate.commissions.dto.CommissionSummaryResponse;
import com.realestate.commissions.CommissionService;
import com.realestate.deals.model.Deal;
import com.realestate.deals.model.DealStage;
import com.realestate.deals.model.DealStatus;
import com.realestate.deals.repository.DealRepository;
import com.realestate.leads.model.Lead;
import com.realestate.leads.model.LeadSource;
import com.realestate.leads.model.LeadStatus;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.property.model.ListingType;
import com.realestate.property.model.Property;
import com.realestate.property.model.PropertyStatus;
import com.realestate.property.model.PropertyType;
import com.realestate.property.repository.PropertyRepository;
import com.realestate.reports.dto.CommissionReport;
import com.realestate.reports.dto.DealReport;
import com.realestate.reports.dto.LeadReport;
import com.realestate.reports.dto.PropertyReport;
import com.realestate.reports.dto.ReportOverview;
import com.realestate.reports.dto.SiteVisitReport;
import com.realestate.sitevisit.model.SiteVisit;
import com.realestate.sitevisit.model.SiteVisitStatus;
import com.realestate.sitevisit.repository.SiteVisitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final PropertyRepository propertyRepository;
    private final SiteVisitRepository siteVisitRepository;
    private final DealRepository dealRepository;
    private final CommissionService commissionService;

    public ReportService(
            UserRepository userRepository,
            LeadRepository leadRepository,
            PropertyRepository propertyRepository,
            SiteVisitRepository siteVisitRepository,
            DealRepository dealRepository,
            CommissionService commissionService
    ) {
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.propertyRepository = propertyRepository;
        this.siteVisitRepository = siteVisitRepository;
        this.dealRepository = dealRepository;
        this.commissionService = commissionService;
    }

    @Transactional(readOnly = true)
    public ReportOverview overview(String email, LocalDate from, LocalDate to) {
        User broker = requireBroker(email);
        List<Lead> leads = leads(broker.getId(), from, to, null);
        List<Property> properties = properties(broker.getId(), from, to, null);
        List<SiteVisit> visits = visits(broker.getId(), from, to, null);
        List<Deal> deals = deals(broker.getId(), from, to, null);
        BigDecimal totalDealValue = sum(deals, Deal::getDealValue);
        BigDecimal totalCommission = sumWonCommission(deals);
        long upcoming = visits.stream()
                .filter(v -> v.getScheduledAt().isAfter(Instant.now()))
                .filter(v -> v.getStatus() == SiteVisitStatus.SCHEDULED
                        || v.getStatus() == SiteVisitStatus.RESCHEDULED)
                .count();

        return new ReportOverview(
                leads.size(),
                count(leads, l -> l.getStatus() == LeadStatus.NEW),
                count(leads, l -> l.getStatus() == LeadStatus.QUALIFIED),
                count(properties, p -> p.getStatus() == PropertyStatus.ACTIVE),
                upcoming,
                count(visits, v -> v.getStatus() == SiteVisitStatus.COMPLETED),
                count(deals, d -> d.getStatus() == DealStatus.ACTIVE),
                count(deals, d -> d.getStatus() == DealStatus.WON),
                count(deals, d -> d.getStatus() == DealStatus.LOST),
                totalDealValue,
                totalCommission,
                totalCommission
        );
    }

    @Transactional(readOnly = true)
    public LeadReport leads(String email, LocalDate from, LocalDate to, String status) {
        User broker = requireBroker(email);
        LeadStatus selectedStatus = parseStatus(status, LeadStatus.class);
        List<Lead> records = leads(broker.getId(), from, to, selectedStatus);
        Map<String, Long> byStatus = enumCounts(LeadStatus.values(), records, Lead::getStatus);
        Map<String, Long> bySource = enumCounts(LeadSource.values(), records, Lead::getSource);
        List<LeadReport.LeadRow> rows = records.stream()
                .map(lead -> new LeadReport.LeadRow(
                        lead.getId(),
                        lead.getName(),
                        lead.getSource().name(),
                        lead.getStatus().name(),
                        lead.getInterestedProperty() == null ? null : lead.getInterestedProperty().getTitle(),
                        lead.getCreatedAt()
                ))
                .toList();
        return new LeadReport(rows.size(), byStatus, bySource, rows);
    }

    @Transactional(readOnly = true)
    public PropertyReport properties(String email, LocalDate from, LocalDate to, String status) {
        User broker = requireBroker(email);
        PropertyStatus selectedStatus = parseStatus(status, PropertyStatus.class);
        List<Property> records = properties(broker.getId(), from, to, selectedStatus);
        BigDecimal totalValue = sum(records, Property::getPrice);
        BigDecimal average = records.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : totalValue.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
        return new PropertyReport(
                records.size(),
                count(records, property -> property.getStatus() == PropertyStatus.ACTIVE),
                count(records, property -> property.getStatus() == PropertyStatus.INACTIVE),
                count(records, property -> property.getStatus() == PropertyStatus.SOLD),
                count(records, property -> property.getStatus() == PropertyStatus.RENTED),
                count(records, Property::isAvailable),
                totalValue,
                average,
                enumCounts(PropertyType.values(), records, Property::getPropertyType),
                enumCounts(ListingType.values(), records, Property::getListingType),
                enumCounts(PropertyStatus.values(), records, Property::getStatus),
                records.stream().map(property -> new PropertyReport.PropertyRow(
                        property.getId(),
                        property.getTitle(),
                        property.getPropertyType().name(),
                        property.getListingType().name(),
                        property.getStatus().name(),
                        property.isAvailable(),
                        property.getPrice(),
                        property.getCity(),
                        property.getCreatedAt()
                )).toList()
        );
    }

    @Transactional(readOnly = true)
    public SiteVisitReport siteVisits(String email, LocalDate from, LocalDate to, String status) {
        User broker = requireBroker(email);
        SiteVisitStatus selectedStatus = parseStatus(status, SiteVisitStatus.class);
        List<SiteVisit> records = visits(broker.getId(), from, to, selectedStatus);
        List<SiteVisitReport.SiteVisitRow> rows = records.stream()
                .map(visit -> new SiteVisitReport.SiteVisitRow(
                        visit.getId(),
                        visit.getScheduledAt(),
                        visit.getStatus().name(),
                        visit.getProperty().getId(),
                        visit.getProperty().getTitle(),
                        visit.getLead().getId(),
                        visit.getLead().getName(),
                        visit.getNotes()
                ))
                .toList();
        return new SiteVisitReport(
                rows.size(),
                count(records, visit -> visit.getScheduledAt().isAfter(Instant.now())
                        && (visit.getStatus() == SiteVisitStatus.SCHEDULED
                        || visit.getStatus() == SiteVisitStatus.RESCHEDULED)),
                count(records, visit -> visit.getStatus() == SiteVisitStatus.COMPLETED),
                count(records, visit -> visit.getStatus() == SiteVisitStatus.CANCELLED),
                counts(records, visit -> visit.getScheduledAt().atZone(ZoneOffset.UTC).toLocalDate().toString()),
                counts(records, visit -> visit.getProperty().getTitle()),
                counts(records, visit -> visit.getLead().getName()),
                rows
        );
    }

    @Transactional(readOnly = true)
    public DealReport deals(String email, LocalDate from, LocalDate to, String status) {
        User broker = requireBroker(email);
        DealStatus selectedStatus = parseStatus(status, DealStatus.class);
        List<Deal> records = deals(broker.getId(), from, to, selectedStatus);
        BigDecimal total = sum(records, Deal::getDealValue);
        BigDecimal average = records.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
        return new DealReport(
                records.size(),
                count(records, deal -> deal.getStatus() == DealStatus.ACTIVE),
                count(records, deal -> deal.getStatus() == DealStatus.WON),
                count(records, deal -> deal.getStatus() == DealStatus.LOST),
                total,
                average,
                enumCounts(DealStage.values(), records, Deal::getStage),
                enumCounts(DealStatus.values(), records, Deal::getStatus),
                counts(records, deal -> deal.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().toString()),
                records.stream().map(deal -> new DealReport.DealRow(
                        deal.getId(),
                        deal.getLead().getName(),
                        deal.getProperty().getTitle(),
                        deal.getDealValue(),
                        deal.getStage().name(),
                        deal.getStatus().name(),
                        deal.getCreatedAt()
                )).toList()
        );
    }

    @Transactional(readOnly = true)
    public CommissionReport commissions(String email, LocalDate from, LocalDate to) {
        User broker = requireBroker(email);
        CommissionSummaryResponse summary = commissionService.summary(email);
        Instant fromInstant = start(from);
        Instant toExclusive = endExclusive(to);
        List<CommissionReport.CommissionRow> rows = summary.commissions().stream()
                .filter(record -> inRange(record.date(), fromInstant, toExclusive))
                .map(record -> new CommissionReport.CommissionRow(
                        record.dealId(),
                        record.leadName(),
                        record.propertyTitle(),
                        record.dealValue(),
                        record.commissionRate(),
                        record.commissionAmount(),
                        record.date()
                ))
                .toList();
        BigDecimal total = rows.stream().map(CommissionReport.CommissionRow::commissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> byDate = new LinkedHashMap<>();
        rows.forEach(row -> {
            String date = row.date().atZone(ZoneOffset.UTC).toLocalDate().toString();
            byDate.merge(date, row.commissionAmount(), BigDecimal::add);
        });
        return new CommissionReport(total, total, byDate, rows);
    }

    private List<Lead> leads(Long brokerId, LocalDate from, LocalDate to, LeadStatus status) {
        return leadRepository.searchOwned(brokerId, null, status, null, null).stream()
                .filter(lead -> inRange(lead.getCreatedAt(), start(from), endExclusive(to)))
                .toList();
    }

    private List<Property> properties(Long brokerId, LocalDate from, LocalDate to, PropertyStatus status) {
        return propertyRepository.findByBrokerIdOrderByCreatedAtDesc(brokerId).stream()
                .filter(property -> status == null || property.getStatus() == status)
                .filter(property -> inRange(property.getCreatedAt(), start(from), endExclusive(to)))
                .toList();
    }

    private List<SiteVisit> visits(Long brokerId, LocalDate from, LocalDate to, SiteVisitStatus status) {
        return siteVisitRepository.findOwnedVisits(brokerId, status).stream()
                .filter(visit -> inRange(visit.getScheduledAt(), start(from), endExclusive(to)))
                .toList();
    }

    private List<Deal> deals(Long brokerId, LocalDate from, LocalDate to, DealStatus status) {
        return dealRepository.searchOwned(brokerId, null, null, status).stream()
                .filter(deal -> inRange(deal.getCreatedAt(), start(from), endExclusive(to)))
                .toList();
    }

    private User requireBroker(String email) {
        User broker = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required"));
        if (!broker.isEnabled() || broker.getRole() != Role.BROKER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required");
        }
        return broker;
    }

    private static Instant start(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant endExclusive(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static boolean inRange(Instant value, Instant from, Instant toExclusive) {
        return value != null
                && (from == null || !value.isBefore(from))
                && (toExclusive == null || value.isBefore(toExclusive));
    }

    private static <E extends Enum<E>> E parseStatus(String value, Class<E> type) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value");
        }
    }

    private static <T> long count(List<T> values, java.util.function.Predicate<T> predicate) {
        return values.stream().filter(predicate).count();
    }

    private static <T> BigDecimal sum(List<T> values, Function<T, BigDecimal> amount) {
        return values.stream().map(amount).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumWonCommission(List<Deal> deals) {
        return deals.stream()
                .filter(deal -> deal.getStatus() == DealStatus.WON)
                .map(Deal::getCommissionAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static <T> Map<String, Long> counts(List<T> values, Function<T, String> key) {
        Map<String, Long> result = new LinkedHashMap<>();
        values.forEach(value -> result.merge(key.apply(value), 1L, Long::sum));
        return result;
    }

    private static <T, E extends Enum<E>> Map<String, Long> enumCounts(
            E[] possibleValues,
            List<T> records,
            Function<T, E> extractor
    ) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (E value : possibleValues) {
            result.put(value.name(), records.stream().filter(record -> extractor.apply(record) == value).count());
        }
        return result;
    }
}
