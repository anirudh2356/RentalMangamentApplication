package com.realestate.notifications;

import com.realestate.leads.LeadService;
import com.realestate.leads.dto.LeadRequest;
import com.realestate.leads.model.Lead;
import com.realestate.leads.model.LeadSource;
import com.realestate.leads.model.LeadStatus;
import com.realestate.leads.repository.LeadRepository;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.notifications.model.NotificationType;
import com.realestate.property.model.Property;
import com.realestate.property.repository.PropertyRepository;
import com.realestate.sitevisit.SiteVisitService;
import com.realestate.sitevisit.dto.SiteVisitRequest;
import com.realestate.sitevisit.model.SiteVisit;
import com.realestate.sitevisit.model.SiteVisitStatus;
import com.realestate.sitevisit.repository.SiteVisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessNotificationIntegrationTest {

    @Mock private NotificationService notificationService;
    @Mock private UserRepository userRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private SiteVisitRepository siteVisitRepository;

    private User broker;
    private Lead lead;
    private Property property;

    @BeforeEach
    void setUp() {
        broker = new User(14L, "Broker");
        broker.setEmail("broker@example.local");
        broker.setRole(Role.BROKER);
        when(userRepository.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));

        lead = new Lead();
        lead.setId(5L);
        lead.setName("Buyer");
        property = new Property();
        property.setId(8L);
        property.setTitle("Apartment");
        property.setLocation("Main Road");
        property.setCity("Pune");
    }

    @Test
    void leadCreationCreatesBrokerOwnedLeadNotification() {
        LeadService leadService = new LeadService(leadRepository, userRepository, propertyRepository, notificationService);
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> {
            Lead saved = invocation.getArgument(0);
            saved.setId(41L);
            return saved;
        });

        leadService.create(
                "broker@example.local",
                new LeadRequest("Buyer", null, null, LeadSource.REFERRAL, LeadStatus.NEW,
                        null, null, null, null, null)
        );

        verify(notificationService).createForBroker(
                broker, NotificationType.LEAD, "New lead added", "A new lead was added: Buyer", "LEAD", 41L
        );
    }

    @Test
    void siteVisitCreationCreatesBrokerOwnedNotification() {
        SiteVisitService siteVisitService = new SiteVisitService(
                siteVisitRepository, userRepository, leadRepository, propertyRepository, notificationService
        );
        when(leadRepository.findOwnedById(5L, 14L)).thenReturn(Optional.of(lead));
        when(propertyRepository.findByIdAndBrokerId(8L, 14L)).thenReturn(Optional.of(property));
        when(siteVisitRepository.save(any(SiteVisit.class))).thenAnswer(invocation -> {
            SiteVisit saved = invocation.getArgument(0);
            saved.setId(52L);
            return saved;
        });

        siteVisitService.create(
                "broker@example.local",
                new SiteVisitRequest(
                        5L,
                        8L,
                        Instant.parse("2026-10-12T09:00:00Z"),
                        SiteVisitStatus.SCHEDULED,
                        null
                )
        );

        verify(notificationService).createForBroker(
                broker,
                NotificationType.SITE_VISIT,
                "Site visit scheduled",
                "A site visit was scheduled for Buyer",
                "SITE_VISIT",
                52L
        );
    }
}
