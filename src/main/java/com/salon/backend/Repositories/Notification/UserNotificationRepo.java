package com.salon.backend.Repositories.Notification;

import com.salon.backend.Entities.notifications.UserNotification;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserNotificationRepo extends JpaRepository<UserNotification, Long> {

    Page<UserNotification> findByRecipientId(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    boolean existsByEventKey(String eventKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT notification
            FROM UserNotification notification
            WHERE notification.id = :notificationId
              AND notification.recipient.id = :recipientId
            """)
    Optional<UserNotification> findOwnedByIdForUpdate(
            @Param("notificationId") Long notificationId,
            @Param("recipientId") Long recipientId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE UserNotification notification
            SET notification.readAt = :readAt
            WHERE notification.recipient.id = :recipientId
              AND notification.readAt IS NULL
            """)
    int markAllRead(
            @Param("recipientId") Long recipientId,
            @Param("readAt") LocalDateTime readAt
    );
}
