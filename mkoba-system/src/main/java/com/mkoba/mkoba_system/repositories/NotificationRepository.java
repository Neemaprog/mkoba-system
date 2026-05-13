package com.mkoba.mkoba_system.repositories;

import com.mkoba.mkoba_system.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Find notifications by type
    List<Notification> findByType(Notification.NotificationType type);
    
    // Find notifications by read status
    List<Notification> findByIsRead(Boolean isRead);
    
    // Find notifications by recipient type
    List<Notification> findByRecipientType(String recipientType);
    
    // Find notifications by recipient
    List<Notification> findByRecipientTypeAndRecipientId(String recipientType, Long recipientId);
    
    // Find recent notifications (last 30 days)
    @Query("SELECT n FROM Notification n WHERE n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("since") LocalDateTime since);
    
    // Find notifications by date range
    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :start AND :end ORDER BY n.createdAt DESC")
    List<Notification> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // Count unread notifications
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isRead = false")
    Long countUnreadNotifications();
    
    // Find notifications by type and date range
    @Query("SELECT n FROM Notification n WHERE n.type = :type AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findByTypeAndSince(@Param("type") Notification.NotificationType type, @Param("since") LocalDateTime since);
    
    // Find notifications for specific user
    @Query("SELECT n FROM Notification n WHERE (n.recipientType = 'ALL' OR (n.recipientType = 'USER' AND n.recipientId = :userId)) ORDER BY n.createdAt DESC")
    List<Notification> findNotificationsForUser(@Param("userId") Long userId);
    
    // Find notifications for specific group
    @Query("SELECT n FROM Notification n WHERE (n.recipientType = 'ALL' OR (n.recipientType = 'GROUP' AND n.recipientId = :groupId)) ORDER BY n.createdAt DESC")
    List<Notification> findNotificationsForGroup(@Param("groupId") Long groupId);
}
