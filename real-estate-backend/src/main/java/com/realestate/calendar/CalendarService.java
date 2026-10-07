package com.realestate.calendar;

import com.realestate.calendar.dto.CalendarEventResponse;
import com.realestate.calendar.model.CalendarEventType;
import com.realestate.followup.model.FollowUp;
import com.realestate.followup.repository.FollowUpRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.sitevisit.model.SiteVisit;
import com.realestate.sitevisit.repository.SiteVisitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class CalendarService {

    private final UserRepository userRepository;
    private final SiteVisitRepository siteVisitRepository;
    private final FollowUpRepository followUpRepository;

    public CalendarService(
            UserRepository userRepository,
            SiteVisitRepository siteVisitRepository,
            FollowUpRepository followUpRepository
    ) {
        this.userRepository = userRepository;
        this.siteVisitRepository = siteVisitRepository;
        this.followUpRepository = followUpRepository;
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> events(String email, LocalDate from, LocalDate to) {
        User broker = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required"));
        if (!broker.isEnabled() || broker.getRole() != Role.BROKER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before or equal to to");
        }

        Instant fromInstant = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toExclusive = to == null ? null : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Long brokerId = broker.getId();

        Stream<CalendarEventResponse> visits = siteVisitRepository
                .findCalendarVisits(brokerId, fromInstant, toExclusive)
                .stream()
                .map(this::toSiteVisitEvent);
        Stream<CalendarEventResponse> followUps = followUpRepository
                .findCalendarFollowUps(brokerId, fromInstant, toExclusive)
                .stream()
                .map(this::toFollowUpEvent);

        return Stream.concat(visits, followUps)
                .sorted(Comparator.comparing(CalendarEventResponse::date)
                        .thenComparing(CalendarEventResponse::startTime)
                        .thenComparing(CalendarEventResponse::type))
                .toList();
    }

    private CalendarEventResponse toSiteVisitEvent(SiteVisit visit) {
        var scheduled = visit.getScheduledAt().atOffset(ZoneOffset.UTC);
        return new CalendarEventResponse(
                CalendarEventType.SITE_VISIT.name() + "-" + visit.getId(),
                CalendarEventType.SITE_VISIT,
                visit.getProperty().getTitle() + " visit with " + visit.getLead().getName(),
                scheduled.toLocalDate(),
                scheduled.toLocalTime(),
                null,
                visit.getStatus().name(),
                null,
                visit.getLead().getId(),
                visit.getLead().getName(),
                visit.getProperty().getId(),
                visit.getProperty().getTitle(),
                visit.getId(),
                joinDetails(visit.getProperty().getLocation() + ", " + visit.getProperty().getCity(), visit.getNotes())
        );
    }

    private CalendarEventResponse toFollowUpEvent(FollowUp followUp) {
        var scheduled = followUp.getFollowUpAt().atOffset(ZoneOffset.UTC);
        return new CalendarEventResponse(
                CalendarEventType.FOLLOW_UP.name() + "-" + followUp.getId(),
                CalendarEventType.FOLLOW_UP,
                followUp.getTitle(),
                scheduled.toLocalDate(),
                scheduled.toLocalTime(),
                null,
                followUp.getStatus().name(),
                followUp.getPriority().name(),
                followUp.getLead().getId(),
                followUp.getLead().getName(),
                null,
                null,
                followUp.getId(),
                followUp.getDescription()
        );
    }

    private String joinDetails(String location, String notes) {
        if (notes == null || notes.isBlank()) return location;
        return location + " — " + notes;
    }
}
