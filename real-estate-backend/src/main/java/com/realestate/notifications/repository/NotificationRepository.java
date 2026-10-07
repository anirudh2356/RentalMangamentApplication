package com.realestate.notifications.repository;

import com.realestate.notifications.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByBrokerIdOrderByCreatedAtDesc(Long brokerId);

    List<Notification> findByBrokerIdAndReadOrderByCreatedAtDesc(Long brokerId, boolean read);

    Optional<Notification> findByIdAndBrokerId(Long id, Long brokerId);

    long countByBrokerIdAndReadFalse(Long brokerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.read = true where n.broker.id = :brokerId and n.read = false")
    int markAllReadByBrokerId(@Param("brokerId") Long brokerId);
}
