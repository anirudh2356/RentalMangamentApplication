package com.realestate.notifications;

import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.notifications.model.Notification;
import com.realestate.notifications.model.NotificationType;
import com.realestate.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private NotificationService notificationService;

    private User broker;
    private Notification notification;

    @BeforeEach
    void setUp() {
        broker = new User(14L, "Broker");
        broker.setEmail("broker@example.local");
        broker.setRole(Role.BROKER);

        notification = notification(81L, broker, false);
    }

    @Test
    void brokerReadsOnlyOwnNotificationsAndUnreadCount() {
        stubBroker();
        when(notificationRepository.findByBrokerIdOrderByCreatedAtDesc(14L)).thenReturn(List.of(notification));
        when(notificationRepository.findByBrokerIdAndReadOrderByCreatedAtDesc(14L, false))
                .thenReturn(List.of(notification));
        when(notificationRepository.countByBrokerIdAndReadFalse(14L)).thenReturn(1L);

        assertEquals(1, notificationService.list("broker@example.local", null).size());
        assertEquals(1, notificationService.list("broker@example.local", true).size());
        assertEquals(1L, notificationService.unreadCount("broker@example.local"));
        verify(notificationRepository).findByBrokerIdOrderByCreatedAtDesc(14L);
        verify(notificationRepository).findByBrokerIdAndReadOrderByCreatedAtDesc(14L, false);
        verify(notificationRepository).countByBrokerIdAndReadFalse(14L);
    }

    @Test
    void brokerCannotRetrieveOrChangeAnotherBrokersNotification() {
        stubBroker();
        when(notificationRepository.findByIdAndBrokerId(99L, 14L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                ResponseStatusException.class,
                () -> notificationService.get("broker@example.local", 99L)
        ).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                ResponseStatusException.class,
                () -> notificationService.markRead("broker@example.local", 99L)
        ).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                ResponseStatusException.class,
                () -> notificationService.delete("broker@example.local", 99L)
        ).getStatusCode());

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    void markingReadAndReadAllAreBrokerScoped() {
        stubBroker();
        when(notificationRepository.findByIdAndBrokerId(81L, 14L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationRepository.markAllReadByBrokerId(14L)).thenReturn(2);

        var marked = notificationService.markRead("broker@example.local", 81L);
        int affected = notificationService.markAllRead("broker@example.local");

        assertEquals(true, marked.read());
        assertEquals(2, affected);
        verify(notificationRepository).findByIdAndBrokerId(81L, 14L);
        verify(notificationRepository).markAllReadByBrokerId(14L);
    }

    @Test
    void deletingOwnNotificationDeletesOnlyOwnedRecord() {
        stubBroker();
        when(notificationRepository.findByIdAndBrokerId(81L, 14L)).thenReturn(Optional.of(notification));
        notificationService.delete("broker@example.local", 81L);
        verify(notificationRepository).delete(notification);
    }

    @Test
    void notificationCreationAssignsEnabledBrokerAndRelatedEntity() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(82L);
            saved.setCreatedAt(Instant.parse("2026-10-07T10:00:00Z"));
            return saved;
        });

        var created = notificationService.createForBroker(
                broker, NotificationType.LEAD, "New lead", "Added buyer", "LEAD", 23L
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(broker, captor.getValue().getBroker());
        assertEquals(false, captor.getValue().isRead());
        assertEquals("LEAD", captor.getValue().getRelatedEntityType());
        assertEquals(23L, captor.getValue().getRelatedEntityId());
        assertEquals(82L, created.id());
        assertEquals(NotificationType.LEAD, created.type());
    }

    @Test
    void nonBrokerCannotAccessNotificationService() {
        User buyer = new User(15L, "Buyer");
        buyer.setEmail("buyer@example.local");
        buyer.setRole(Role.BUYER);
        when(userRepository.findByEmailIgnoreCase("buyer@example.local")).thenReturn(Optional.of(buyer));

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(
                ResponseStatusException.class,
                () -> notificationService.unreadCount("buyer@example.local")
        ).getStatusCode());
    }

    private static Notification notification(Long id, User owner, boolean read) {
        Notification item = new Notification();
        item.setId(id);
        item.setBroker(owner);
        item.setTitle("New lead");
        item.setMessage("A lead was added.");
        item.setType(NotificationType.LEAD);
        item.setRead(read);
        item.setCreatedAt(Instant.parse("2026-10-07T09:00:00Z"));
        return item;
    }

    private void stubBroker() {
        when(userRepository.findByEmailIgnoreCase("broker@example.local")).thenReturn(Optional.of(broker));
    }
}
