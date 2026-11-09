package com.example.kodyjobdam.notification.repository;

import com.example.kodyjobdam.notification.entity.Notification;
import com.example.kodyjobdam.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByReceiver_IdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    long countByReceiver_IdAndReadFalse(Long receiverId);

    Optional<Notification> findByIdAndReceiver_Id(Long id, Long receiverId);

    boolean existsByReceiver_IdAndTypeAndTargetId(Long receiverId, NotificationType type, Long targetId);

    /** 주어진 사람 중 이 대상의 알림을 이미 받은 사람. 유니크 키 (receiver_id, type, target_id)를 탄다. */
    @Query("""
            select n.receiver.id from Notification n
            where n.receiver.id in :receiverIds and n.type = :type and n.targetId = :targetId
            """)
    Set<Long> findReceiverIdsAlreadyNotified(@Param("receiverIds") Collection<Long> receiverIds,
                                             @Param("type") NotificationType type,
                                             @Param("targetId") Long targetId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true where n.receiver.id = :receiverId and n.read = false")
    int markAllAsRead(@Param("receiverId") Long receiverId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from Notification n
            where n.expiresAt <= :now
            """)
    int deleteExpiredNotifications(@Param("now") java.time.LocalDateTime now);
}
