package com.realestate.notifications;

import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.notifications.dto.NotificationResponse;
import com.realestate.notifications.model.Notification;
import com.realestate.notifications.model.NotificationType;
import com.realestate.notifications.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String email, Boolean unreadOnly) {
        User broker = requireBroker(email);
        List<Notification> rows = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findByBrokerIdAndReadOrderByCreatedAtDesc(broker.getId(), false)
                : notificationRepository.findByBrokerIdOrderByCreatedAtDesc(broker.getId());
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(String email, Long id) {
        User broker = requireBroker(email);
        return toResponse(findOwned(id, broker.getId()));
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return notificationRepository.countByBrokerIdAndReadFalse(requireBroker(email).getId());
    }

    @Transactional
    public NotificationResponse markRead(String email, Long id) {
        User broker = requireBroker(email);
        Notification notification = findOwned(id, broker.getId());
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllRead(String email) {
        User broker = requireBroker(email);
        return notificationRepository.markAllReadByBrokerId(broker.getId());
    }

    @Transactional
    public void delete(String email, Long id) {
        User broker = requireBroker(email);
        notificationRepository.delete(findOwned(id, broker.getId()));
    }

    @Transactional
    public NotificationResponse createForBroker(
            User broker,
            NotificationType type,
            String title,
            String message,
            String relatedEntityType,
            Long relatedEntityId
    ) {
        if (broker == null || broker.getId() == null || broker.getRole() != Role.BROKER || !broker.isEnabled()) {
            throw new IllegalArgumentException("An enabled broker is required to create a notification");
        }
        Notification notification = new Notification();
        notification.setBroker(broker);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRead(false);
        return toResponse(notificationRepository.save(notification));
    }

    private User requireBroker(String email) {
        User broker = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required"));
        if (broker.getRole() != Role.BROKER || !broker.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required");
        }
        return broker;
    }

    private Notification findOwned(Long id, Long brokerId) {
        return notificationRepository.findByIdAndBrokerId(id, brokerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getRelatedEntityType(),
                notification.getRelatedEntityId(),
                notification.getCreatedAt()
        );
    }
}
