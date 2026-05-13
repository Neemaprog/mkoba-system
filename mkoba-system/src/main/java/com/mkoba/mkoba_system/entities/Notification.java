package com.mkoba.mkoba_system.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "recipient_type")
    private String recipientType; // "ALL", "GROUP", "USER"
    
    @Column(name = "recipient_id")
    private Long recipientId; // Group ID or User ID
    
    @Column(name = "sent_via")
    private String sentVia; // "APP", "SMS", "EMAIL"
    
    @Column(name = "delivery_status")
    private String deliveryStatus; // "SENT", "DELIVERED", "PENDING", "FAILED"
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    // Constructors
    public Notification() {}
    
    public Notification(String title, String message, NotificationType type, NotificationPriority priority, String recipientType, Long recipientId) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.priority = priority;
        this.recipientType = recipientType;
        this.recipientId = recipientId;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }
    
    // Enums
    public enum NotificationType {
        ANNOUNCEMENT, REMINDER, SYSTEM, LOAN, CONTRIBUTION
    }
    
    public enum NotificationPriority {
        HIGH, MEDIUM, LOW
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    
    public NotificationPriority getPriority() { return priority; }
    public void setPriority(NotificationPriority priority) { this.priority = priority; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    
    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }
    
    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }
    
    public String getSentVia() { return sentVia; }
    public void setSentVia(String sentVia) { this.sentVia = sentVia; }
    
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    // Helper method to get icon based on type
    public String getIcon() {
        switch (type) {
            case ANNOUNCEMENT: return "fas fa-bullhorn";
            case REMINDER: return "fas fa-clock";
            case SYSTEM: return "fas fa-cogs";
            case LOAN: return "fas fa-hand-holding-usd";
            case CONTRIBUTION: return "fas fa-piggy-bank";
            default: return "fas fa-bell";
        }
    }
    
    // Helper method to get recipient display name
    public String getRecipientDisplayName() {
        if ("ALL".equals(recipientType)) {
            return "All Users";
        } else if ("GROUP".equals(recipientType) && user != null && user.getGroup() != null) {
            return user.getGroup().getName();
        } else if ("USER".equals(recipientType) && user != null) {
            return user.getFirstName() + " " + user.getLastName();
        }
        return "Unknown";
    }
}
