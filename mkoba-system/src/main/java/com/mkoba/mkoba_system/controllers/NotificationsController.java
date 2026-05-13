package com.mkoba.mkoba_system.controllers;

import com.mkoba.mkoba_system.entities.Notification;
import com.mkoba.mkoba_system.entities.User;
import com.mkoba.mkoba_system.entities.Group;
import com.mkoba.mkoba_system.repositories.NotificationRepository;
import com.mkoba.mkoba_system.repositories.UserRepository;
import com.mkoba.mkoba_system.repositories.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Arrays;

@Controller
@RequestMapping("/admin")
public class NotificationsController {

    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private GroupRepository groupRepository;

    @GetMapping("/notifications")
    public String notificationsPage(
            @RequestParam(value = "tab", defaultValue = "all") String tab,
            Model model) {
        try {
            System.out.println("🔍 DEBUG: Loading notifications page with tab: " + tab);
            
            // Get all notifications from database
            List<Notification> allNotifications = notificationRepository.findAll();
            
            // Filter notifications based on tab
            List<Notification> filteredNotifications;
            
            switch (tab) {
                case "all":
                    filteredNotifications = allNotifications;
                    break;
                case "announcements":
                    filteredNotifications = notificationRepository.findByType(Notification.NotificationType.ANNOUNCEMENT);
                    break;
                case "reminders":
                    filteredNotifications = notificationRepository.findByType(Notification.NotificationType.REMINDER);
                    break;
                case "system":
                    // Show system notifications (including replies from users)
                    filteredNotifications = notificationRepository.findByType(Notification.NotificationType.SYSTEM);
                    break;
                case "loans":
                    filteredNotifications = notificationRepository.findByType(Notification.NotificationType.LOAN);
                    break;
                case "contributions":
                    filteredNotifications = notificationRepository.findByType(Notification.NotificationType.CONTRIBUTION);
                    break;
                case "replies":
                    // Show only replies from users (system notifications with "REPLY:" in title)
                    filteredNotifications = allNotifications.stream()
                        .filter(n -> n.getType() == Notification.NotificationType.SYSTEM && 
                                   n.getTitle() != null && n.getTitle().startsWith("REPLY:"))
                        .collect(java.util.stream.Collectors.toList());
                    break;
                default:
                    filteredNotifications = allNotifications;
                    break;
            }
            
            // Get statistics
            long totalNotifications = allNotifications.size();
            long unreadCount = notificationRepository.countUnreadNotifications();
            
            // Get groups for dropdown
            List<Group> groups = groupRepository.findAll();
            
            // Get users for individual user dropdown
            List<User> users = userRepository.findAll();
            
            // Ensure sample data exists
            ensureSampleData();
            
            model.addAttribute("notifications", filteredNotifications);
            model.addAttribute("allNotifications", allNotifications);
            model.addAttribute("groups", groups);
            model.addAttribute("users", users);
            model.addAttribute("activeTab", tab);
            model.addAttribute("totalNotifications", totalNotifications);
            model.addAttribute("unreadCount", unreadCount);
            model.addAttribute("currentDateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
            
            System.out.println("🔍 DEBUG: Found " + filteredNotifications.size() + " notifications for tab: " + tab);
            System.out.println("🔍 DEBUG: Total notifications: " + totalNotifications + ", Unread: " + unreadCount);
            
            return "admin/notifications";
        } catch (Exception e) {
            System.err.println("❌ ERROR: Error loading notifications: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading notifications: " + e.getMessage());
            return "admin/notifications";
        }
    }

    @PostMapping("/notifications/announcement")
    public String createAnnouncement(
            @RequestParam("title") String title,
            @RequestParam("message") String message,
            @RequestParam("targetGroup") String targetGroup,
            @RequestParam(value = "sendVia", required = false) String[] sendVia,
            RedirectAttributes redirectAttributes) {
        try {
            System.out.println("🔍 DEBUG: Creating announcement");
            System.out.println("  - Title: " + title);
            System.out.println("  - Message: " + message);
            System.out.println("  - Target Group: " + targetGroup);
            if (sendVia != null) {
                System.out.println("  - Send Via: " + String.join(", ", sendVia));
            }
            
            // Create notification entity
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(Notification.NotificationType.ANNOUNCEMENT);
            notification.setPriority(Notification.NotificationPriority.MEDIUM);
            
            // Set recipient based on target group
            if ("all".equals(targetGroup)) {
                notification.setRecipientType("ALL");
                notification.setRecipientId(null);
            } else {
                try {
                    Long groupId = Long.parseLong(targetGroup);
                    notification.setRecipientType("GROUP");
                    notification.setRecipientId(groupId);
                } catch (NumberFormatException e) {
                    notification.setRecipientType("ALL");
                    notification.setRecipientId(null);
                }
            }
            
            // Set delivery methods
            if (sendVia != null && sendVia.length > 0) {
                notification.setSentVia(String.join(",", sendVia));
                notification.setDeliveryStatus("SENT");
            } else {
                notification.setSentVia("APP");
                notification.setDeliveryStatus("SENT");
            }
            
            // Save to database
            notificationRepository.save(notification);
            
            System.out.println("✅ SUCCESS: Announcement saved with ID: " + notification.getId());
            
            redirectAttributes.addFlashAttribute("success", "Announcement sent successfully!");
            return "redirect:/admin/notifications";
        } catch (Exception e) {
            System.err.println("❌ ERROR: Error creating announcement: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error creating announcement: " + e.getMessage());
            return "redirect:/admin/notifications";
        }
    }

    @PostMapping("/notifications/reminders")
    public String saveReminderSettings(
            @RequestParam("contributionReminder") String contributionReminder,
            @RequestParam("loanReminder") String loanReminder,
            @RequestParam(value = "defaulterAlert", required = false) Boolean defaulterAlert,
            RedirectAttributes redirectAttributes) {
        try {
            System.out.println("🔍 DEBUG: Saving reminder settings");
            System.out.println("  - Contribution Reminder: " + contributionReminder);
            System.out.println("  - Loan Reminder: " + loanReminder);
            System.out.println("  - Defaulter Alert: " + (defaulterAlert != null ? defaulterAlert : false));
            
            // Here you would save these settings to database or configuration file
            // For demonstration, we'll create a system notification about the settings change
            Notification settingsNotification = new Notification();
            settingsNotification.setTitle("Reminder Settings Updated");
            settingsNotification.setMessage("Automatic reminder settings have been updated. Contribution: " + contributionReminder + ", Loan: " + loanReminder + ", Defaulter Alert: " + (defaulterAlert != null ? defaulterAlert : false));
            settingsNotification.setType(Notification.NotificationType.SYSTEM);
            settingsNotification.setPriority(Notification.NotificationPriority.LOW);
            settingsNotification.setRecipientType("ALL");
            settingsNotification.setRecipientId(null);
            settingsNotification.setSentVia("APP");
            settingsNotification.setDeliveryStatus("SENT");
            
            notificationRepository.save(settingsNotification);
            
            System.out.println("✅ SUCCESS: Reminder settings saved and notification created");
            
            redirectAttributes.addFlashAttribute("success", "Reminder settings saved successfully!");
            return "redirect:/admin/notifications";
        } catch (Exception e) {
            System.err.println("❌ ERROR: Error saving reminder settings: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error saving reminder settings: " + e.getMessage());
            return "redirect:/admin/notifications";
        }
    }
    
    @PostMapping("/notifications/user")
    public String sendNotificationToUser(
            @RequestParam("userId") Long userId,
            @RequestParam("title") String title,
            @RequestParam("message") String message,
            @RequestParam(value = "sendVia", required = false) String[] sendVia,
            RedirectAttributes redirectAttributes) {
        try {
            System.out.println("🔍 DEBUG: Sending notification to user");
            System.out.println("  - User ID: " + userId);
            System.out.println("  - Title: " + title);
            System.out.println("  - Message: " + message);
            if (sendVia != null) {
                System.out.println("  - Send Via: " + String.join(", ", sendVia));
            }
            
            // Get user from database
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found!");
                return "redirect:/admin/notifications";
            }
            
            // Create notification entity
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(Notification.NotificationType.ANNOUNCEMENT);
            notification.setPriority(Notification.NotificationPriority.MEDIUM);
            notification.setRecipientType("USER");
            notification.setRecipientId(userId);
            notification.setUser(user);
            
            // Set delivery methods
            if (sendVia != null && sendVia.length > 0) {
                notification.setSentVia(String.join(",", sendVia));
                notification.setDeliveryStatus("SENT");
            } else {
                notification.setSentVia("APP");
                notification.setDeliveryStatus("SENT");
            }
            
            // Save to database
            notificationRepository.save(notification);
            
            System.out.println("✅ SUCCESS: User notification saved with ID: " + notification.getId());
            System.out.println("✅ SUCCESS: Sent to user: " + user.getFirstName() + " " + user.getLastName());
            
            redirectAttributes.addFlashAttribute("success", "Notification sent to " + user.getFirstName() + " " + user.getLastName() + " successfully!");
            return "redirect:/admin/notifications";
        } catch (Exception e) {
            System.err.println("❌ ERROR: Error sending notification to user: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error sending notification to user: " + e.getMessage());
            return "redirect:/admin/notifications";
        }
    }
    
    // Add some sample data if database is empty
    private void ensureSampleData() {
        List<Notification> existingNotifications = notificationRepository.findAll();
        if (existingNotifications.isEmpty()) {
            System.out.println("🔍 DEBUG: Creating sample notification data");
            
            LocalDateTime now = LocalDateTime.now();
            
            // Get first user for sample notifications
            List<User> users = userRepository.findAll();
            if (!users.isEmpty()) {
                User sampleUser = users.get(0);
                
                // Create sample notifications
                Notification notification1 = new Notification();
                notification1.setTitle("Contribution Payment Due");
                notification1.setMessage("Amount: 20,000 | Group: Umoja");
                notification1.setType(Notification.NotificationType.CONTRIBUTION);
                notification1.setPriority(Notification.NotificationPriority.HIGH);
                notification1.setRecipientType("USER");
                notification1.setRecipientId(sampleUser.getId());
                notification1.setUser(sampleUser);
                notification1.setSentVia("APP");
                notification1.setDeliveryStatus("SENT");
                notification1.setCreatedAt(now.minusHours(2));
                notification1.setIsRead(false);
                
                Notification notification2 = new Notification();
                notification2.setTitle("Loan Repayment Reminder");
                notification2.setMessage("Amount: 50,000 | Due: 10/02/2026");
                notification2.setType(Notification.NotificationType.LOAN);
                notification2.setPriority(Notification.NotificationPriority.MEDIUM);
                notification2.setRecipientType("USER");
                notification2.setRecipientId(sampleUser.getId());
                notification2.setUser(sampleUser);
                notification2.setSentVia("APP,SMS");
                notification2.setDeliveryStatus("DELIVERED");
                notification2.setCreatedAt(now.minusDays(1));
                notification2.setIsRead(false);
                
                Notification notification3 = new Notification();
                notification3.setTitle("Savings Recorded Successfully");
                notification3.setMessage("Amount: 10,000");
                notification3.setType(Notification.NotificationType.CONTRIBUTION);
                notification3.setPriority(Notification.NotificationPriority.LOW);
                notification3.setRecipientType("USER");
                notification3.setRecipientId(sampleUser.getId());
                notification3.setUser(sampleUser);
                notification3.setSentVia("APP");
                notification3.setDeliveryStatus("SENT");
                notification3.setCreatedAt(now.minusDays(2));
                notification3.setIsRead(true);
                
                Notification notification4 = new Notification();
                notification4.setTitle("Announcement: Group meeting Saturday");
                notification4.setMessage("Meeting at 2:00 PM in the community hall");
                notification4.setType(Notification.NotificationType.ANNOUNCEMENT);
                notification4.setPriority(Notification.NotificationPriority.LOW);
                notification4.setRecipientType("USER");
                notification4.setRecipientId(sampleUser.getId());
                notification4.setUser(sampleUser);
                notification4.setSentVia("APP,SMS,EMAIL");
                notification4.setDeliveryStatus("SENT");
                notification4.setCreatedAt(now.minusDays(3));
                notification4.setIsRead(true);
                
                // Also create some general notifications
                Notification notification5 = new Notification();
                notification5.setTitle("System Maintenance");
                notification5.setMessage("System will be down for maintenance tonight at 11 PM");
                notification5.setType(Notification.NotificationType.SYSTEM);
                notification5.setPriority(Notification.NotificationPriority.LOW);
                notification5.setRecipientType("ALL");
                notification5.setRecipientId(null);
                notification5.setSentVia("APP");
                notification5.setDeliveryStatus("SENT");
                notification5.setCreatedAt(now.minusDays(1));
                notification5.setIsRead(true);
                
                notificationRepository.saveAll(Arrays.asList(notification1, notification2, notification3, notification4, notification5));
                
                System.out.println("✅ SUCCESS: Created 5 sample notifications including user-specific ones");
            }
        }
    }
}
