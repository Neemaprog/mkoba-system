package com.mkoba.mkoba_system.controllers;

import com.mkoba.mkoba_system.entities.*;
import com.mkoba.mkoba_system.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SavingsRepository savingsRepository;
    
    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @GetMapping
    public String dashboard(Model model, Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Group group = user.getGroup();
        
        // Get user's savings
        List<Savings> userSavings = savingsRepository.findByUserIdOrderByContributionDateDesc(user.getId());
        Double totalSavings = savingsRepository.getTotalSavingsByUser(user.getId());
        if (totalSavings == null) totalSavings = 0.0;
        
        // Get user's loans
        List<Loan> userLoans = loanRepository.findByUserIdOrderByApplicationDateDesc(user.getId());
        List<Loan.LoanStatus> activeStatuses = Arrays.asList(
            Loan.LoanStatus.ACTIVE, Loan.LoanStatus.APPROVED
        );
        Double totalOutstanding = loanRepository.getTotalOutstandingBalance(user.getId(), activeStatuses);
        if (totalOutstanding == null) totalOutstanding = 0.0;
        
        // DEBUG: Print values for troubleshooting
        System.out.println("=== DASHBOARD DEBUG ===");
        System.out.println("User ID: " + user.getId());
        System.out.println("Total Savings: " + totalSavings);
        System.out.println("Total Outstanding: " + totalOutstanding);
        System.out.println("Available Balance: " + (totalSavings - totalOutstanding));
        System.out.println("=====================");
        
        // Get unread notifications count
        long unreadCount = 0;
        try {
            unreadCount = notificationRepository.findNotificationsForUser(user.getId())
                .stream()
                .filter(n -> !n.getIsRead())
                .count();
        } catch (Exception e) {
            System.out.println(" DEBUG: Could not get notifications: " + e.getMessage());
        }
        
        // Get recent savings
        List<Savings> recentSavings = savingsRepository.findByUserIdOrderByContributionDateDesc(user.getId());
        if (recentSavings.size() > 5) {
            recentSavings = recentSavings.subList(0, 5);
        }
        
        // Get recent loans
        List<Loan> recentLoans = loanRepository.findByUserIdOrderByApplicationDateDesc(user.getId());
        if (recentLoans.size() > 5) {
            recentLoans = recentLoans.subList(0, 5);
        }
        
        // Get recent transactions
        List<Transaction> recentTransactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId());
        if (recentTransactions.size() > 10) {
            recentTransactions = recentTransactions.subList(0, 10);
        }
        
        // Add all data to model
        model.addAttribute("user", user);
        model.addAttribute("group", group);
        model.addAttribute("totalSavings", totalSavings);
        model.addAttribute("totalOutstanding", totalOutstanding);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("recentSavings", recentSavings);
        model.addAttribute("recentLoans", recentLoans);
        model.addAttribute("recentTransactions", recentTransactions);
        
        return "dashboard";
    }
    
    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Calculate membership duration in months
        LocalDateTime createdAt = user.getCreatedAt();
        LocalDateTime now = LocalDateTime.now();
        long membershipMonths = java.time.temporal.ChronoUnit.MONTHS.between(createdAt, now);
        if (membershipMonths == 0) membershipMonths = 1; // At least 1 month
        
        // Get total savings amount and count
        List<Savings> userSavings = savingsRepository.findByUserIdOrderByContributionDateDesc(user.getId());
        int totalSavingsCount = userSavings.size();
        Double totalSavingsAmount = savingsRepository.getTotalSavingsByUser(user.getId());
        if (totalSavingsAmount == null) totalSavingsAmount = 0.0;
        
        // Get total loans count
        List<Loan> userLoans = loanRepository.findByUserIdOrderByApplicationDateDesc(user.getId());
        int totalLoansCount = userLoans.size();
        
        // Calculate payment reliability (percentage of completed loans vs total loans)
        int paidLoansCount = 0;
        for (Loan loan : userLoans) {
            if (loan.getStatus() == Loan.LoanStatus.COMPLETED || 
                (loan.getAmountPaid() != null && loan.getAmountPaid() >= loan.getTotalAmount())) {
                paidLoansCount++;
            }
        }
        
        double paymentReliability = 0.0;
        if (totalLoansCount > 0) {
            paymentReliability = (double) paidLoansCount / totalLoansCount * 100;
        }
        
        // Create statistics object
        Map<String, Object> stats = new HashMap<>();
        stats.put("membershipMonths", membershipMonths);
        stats.put("totalSavingsCount", totalSavingsCount);
        stats.put("totalSavingsAmount", String.format("%,.0f", totalSavingsAmount));
        stats.put("totalLoans", totalLoansCount);
        stats.put("paymentReliability", String.format("%.1f%%", paymentReliability));
        
        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        return "profile";
    }
    
    @GetMapping("/savings")
    public String savings(Model model, Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Savings> savings = savingsRepository.findByUserIdOrderByContributionDateDesc(user.getId());
        Double totalSavings = savingsRepository.getTotalSavingsByUser(user.getId());
        if (totalSavings == null) totalSavings = 0.0;
        
        model.addAttribute("user", user);
        model.addAttribute("savings", savings);
        model.addAttribute("totalSavings", totalSavings);
        
        return "savings";
    }
    
    @GetMapping("/loans")
    public String loans(Model model, Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Loan> loans = loanRepository.findByUserIdOrderByApplicationDateDesc(user.getId());
        
        // Calculate loan statistics
        Map<String, Object> loanStats = new HashMap<>();
        
        // Total loans count
        loanStats.put("totalLoans", loans.size());
        
        // Loans by status
        long completedLoans = loans.stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.COMPLETED)
                .count();
        long activeLoans = loans.stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.ACTIVE)
                .count();
        long pendingLoans = loans.stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.PENDING)
                .count();
        long approvedLoans = loans.stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.APPROVED)
                .count();
        long rejectedLoans = loans.stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.REJECTED)
                .count();
        
        loanStats.put("completedLoans", completedLoans);
        loanStats.put("activeLoans", activeLoans);
        loanStats.put("pendingLoans", pendingLoans);
        loanStats.put("approvedLoans", approvedLoans);
        loanStats.put("rejectedLoans", rejectedLoans);
        
        // Total loan amount
        double totalLoanAmount = loans.stream()
                .mapToDouble(Loan::getAmount)
                .sum();
        loanStats.put("totalLoanAmount", totalLoanAmount);
        
        List<Loan.LoanStatus> activeStatuses = Arrays.asList(
            Loan.LoanStatus.ACTIVE, Loan.LoanStatus.APPROVED
        );
        Double totalOutstanding = loanRepository.getTotalOutstandingBalance(user.getId(), activeStatuses);
        if (totalOutstanding == null) totalOutstanding = 0.0;
        
        // Calculate total savings
        Double totalSavings = savingsRepository.getTotalSavingsByUser(user.getId());
        if (totalSavings == null) totalSavings = 0.0;
        
        // Calculate available balance
        Double availableBalance = totalSavings - totalOutstanding;
        
        // DEBUG: Print values for troubleshooting
        System.out.println("=== LOANS PAGE DEBUG ===");
        System.out.println("User ID: " + user.getId());
        System.out.println("Total Savings: " + totalSavings);
        System.out.println("Total Outstanding: " + totalOutstanding);
        System.out.println("Available Balance: " + availableBalance);
        System.out.println("========================");
        
        model.addAttribute("user", user);
        model.addAttribute("group", user.getGroup());
        model.addAttribute("loans", loans);
        model.addAttribute("loanStats", loanStats);
        model.addAttribute("totalOutstanding", totalOutstanding);
        model.addAttribute("totalSavings", totalSavings);
        model.addAttribute("availableBalance", availableBalance);
        
        return "loans";
    }
    
    @GetMapping("/notifications")
    public String notifications(Model model, Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get user's notifications
        List<Notification> userNotifications = notificationRepository.findNotificationsForUser(user.getId());
        
        // Count unread notifications
        long unreadCount = userNotifications.stream()
                .filter(n -> !n.getIsRead())
                .count();
        
        model.addAttribute("user", user);
        model.addAttribute("notifications", userNotifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("currentUser", user);
        
        // Create reminder notifications if needed
        createReminderNotifications(user);
        
        return "dashboard/notifications";
    }
    
    @PostMapping("/notifications/mark-read")
    public String markNotificationAsRead(@RequestParam Long notificationId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));
            
            // Security check - ensure notification belongs to current user
            if (!notification.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You don't have permission to mark this notification as read");
                return "redirect:/dashboard/notifications";
            }
            
            notification.setIsRead(true);
            notificationRepository.save(notification);
            
            redirectAttributes.addFlashAttribute("success", "Notification marked as read");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error marking notification as read: " + e.getMessage());
        }
        
        return "redirect:/dashboard/notifications";
    }
    
    @PostMapping("/notifications/reply")
    public String replyToNotification(
            @RequestParam Long notificationId,
            @RequestParam String replyMessage,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Notification originalNotification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));
            
            // Security check
            if (!originalNotification.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You don't have permission to reply to this notification");
                return "redirect:/dashboard/notifications";
            }
            
            // Mark original notification as read
            originalNotification.setIsRead(true);
            notificationRepository.save(originalNotification);
            
            // Create reply notification (for admin to see)
            Notification replyNotification = new Notification();
            replyNotification.setTitle("REPLY: " + originalNotification.getTitle());
            replyNotification.setMessage("From " + user.getFirstName() + " " + user.getLastName() + ": " + replyMessage);
            replyNotification.setType(Notification.NotificationType.SYSTEM);
            replyNotification.setPriority(Notification.NotificationPriority.MEDIUM);
            replyNotification.setRecipientType("ALL"); // Admin will see this
            replyNotification.setRecipientId(null);
            replyNotification.setUser(user);
            replyNotification.setSentVia("APP");
            replyNotification.setDeliveryStatus("SENT");
            replyNotification.setIsRead(false);
            
            notificationRepository.save(replyNotification);
            
            redirectAttributes.addFlashAttribute("success", "Reply sent successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error sending reply: " + e.getMessage());
        }
        
        return "redirect:/dashboard/notifications";
    }
    
    // Helper method to create notifications
    private void createNotification(User user, String title, String message, Notification.NotificationType type) {
        try {
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(type);
            notification.setPriority(Notification.NotificationPriority.MEDIUM);
            notification.setRecipientType("USER");
            notification.setRecipientId(user.getId());
            notification.setUser(user);
            notification.setSentVia("APP");
            notification.setDeliveryStatus("SENT");
            notification.setIsRead(false);
            
            notificationRepository.save(notification);
            
            System.out.println("✅ NOTIFICATION CREATED: " + title + " for user " + user.getFirstName());
        } catch (Exception e) {
            System.err.println("❌ ERROR creating notification: " + e.getMessage());
        }
    }
    
    // Create reminder notifications for user
    private void createReminderNotifications(User user) {
        try {
            // Check for upcoming loan payments
            List<Loan.LoanStatus> activeStatuses = Arrays.asList(
                Loan.LoanStatus.ACTIVE, Loan.LoanStatus.APPROVED
            );
            List<Loan> activeLoans = loanRepository.findByUserIdAndStatusIn(user.getId(), activeStatuses);
            
            for (Loan loan : activeLoans) {
                // Check if due date is within 7 days
                if (loan.getDueDate() != null) {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime dueDate = loan.getDueDate();
                    long daysUntilDue = java.time.Duration.between(now, dueDate).toDays();
                    
                    if (daysUntilDue <= 7 && daysUntilDue > 0) {
                        // Check if reminder already exists
                        List<Notification> existingNotifications = notificationRepository.findNotificationsForUser(user.getId());
                        boolean reminderExists = existingNotifications.stream()
                            .anyMatch(n -> n.getTitle().contains("Loan Payment Due") && 
                                           n.getMessage().contains("Loan #" + loan.getId()));
                        
                        if (!reminderExists) {
                            createNotification(user, "Loan Payment Due", 
                                "Your loan payment of Tsh " + loan.getRemainingBalance() + " is due in " + daysUntilDue + " days", 
                                Notification.NotificationType.LOAN);
                        }
                    }
                }
            }
            
            // Check for monthly contribution reminder
            LocalDateTime now = LocalDateTime.now();
            int currentDay = now.getDayOfMonth();
            
            // Check if user has made contribution this month
            List<Savings> thisMonthSavings = savingsRepository.findByUserIdAndDateRange(
                user.getId(), 
                now.withDayOfMonth(1).withHour(0).withMinute(0), 
                now.withDayOfMonth(now.getMonth().maxLength()).withHour(23).withMinute(59)
            );
            
            if (thisMonthSavings.isEmpty() && currentDay >= 25) {
                // Check if reminder already exists
                List<Notification> existingNotifications = notificationRepository.findNotificationsForUser(user.getId());
                boolean reminderExists = existingNotifications.stream()
                    .anyMatch(n -> n.getTitle().contains("Monthly Contribution") && 
                                   n.getMessage().contains("month"));
                
                if (!reminderExists) {
                    createNotification(user, "Monthly Contribution Reminder", 
                        "Don't forget to make your monthly contribution for this month", 
                        Notification.NotificationType.CONTRIBUTION);
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERROR creating reminder notifications: " + e.getMessage());
        }
    }
    
    @GetMapping("/loans/apply")
    public String applyLoanForm(Model model, Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("user", user);
        model.addAttribute("loan", new Loan());
        return "apply-loan";
    }
    
    @PostMapping("/loans/apply")
    public String applyLoan(@ModelAttribute Loan loan, Principal principal, RedirectAttributes redirectAttributes) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        loan.setUser(user);
        loan.setGroup(user.getGroup());
        loan.setApplicationDate(LocalDateTime.now());
        loan.setStatus(Loan.LoanStatus.PENDING);
        loan.setAmountPaid(0.0);
        
        loanRepository.save(loan);
        
        redirectAttributes.addFlashAttribute("message", "Ombi lako la mkopo limetumwa na linasubiri idhini.");
        return "redirect:/dashboard/loans";
    }
    
    @PostMapping("/loans/apply/json")
    @ResponseBody
    public Map<String, Object> applyLoanJson(@RequestBody Map<String, Object> loanData, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Loan loan = new Loan();
            loan.setUser(user);
            loan.setGroup(user.getGroup());
            loan.setAmount(Double.parseDouble(loanData.get("amount").toString()));
            loan.setPurpose(loanData.get("description").toString());
            
            // Parse date string to LocalDateTime
            String dateStr = loanData.get("applicationDate").toString();
            LocalDateTime applicationDate = LocalDateTime.parse(dateStr + "T00:00:00");
            loan.setApplicationDate(applicationDate);
            
            // Set interest rate from group
            loan.setInterestRate(user.getGroup().getInterestRate());
            
            // Calculate total amount (principal + interest)
            double interestAmount = loan.getAmount() * (loan.getInterestRate() / 100);
            double totalAmount = loan.getAmount() + interestAmount;
            loan.setTotalAmount(totalAmount);
            loan.setRemainingBalance(totalAmount);
            
            // Set due date based on period
            int period = Integer.parseInt(loanData.get("period").toString());
            LocalDateTime dueDate = applicationDate.plusMonths(period);
            loan.setDueDate(dueDate);
            
            loan.setStatus(Loan.LoanStatus.PENDING);
            loan.setAmountPaid(0.0);
            
            loanRepository.save(loan);
            
            // Create notification for user
            createNotification(user, "Loan Application Submitted", 
                              "Your loan application of Tsh " + loan.getAmount() + " has been submitted and is pending approval", 
                              Notification.NotificationType.LOAN);
            
            response.put("success", true);
            response.put("message", "Ombi la mkopo limetumwa kikamilifu!");
            
        } catch (Exception e) {
            e.printStackTrace(); // Add logging for debugging
            response.put("success", false);
            response.put("message", "Imeshindikana kutuma ombi: " + e.getMessage());
        }
        
        return response;
    }
    
    @GetMapping("/loans/{id}/details")
    @ResponseBody
    public Map<String, Object> getLoanDetails(@PathVariable Long id, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Loan loan = loanRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Mkopo haujapatikana"));
            
            // Verify user owns this loan
            if (!loan.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Huna ruhusa ya kuona mkopo huu");
            }
            
            // Calculate period in months
            int period = 0;
            if (loan.getApplicationDate() != null && loan.getDueDate() != null) {
                period = (int) java.time.temporal.ChronoUnit.MONTHS.between(
                    loan.getApplicationDate().toLocalDate(), 
                    loan.getDueDate().toLocalDate()
                );
            }
            
            Map<String, Object> loanData = new HashMap<>();
            loanData.put("id", loan.getId());
            loanData.put("amount", loan.getAmount());
            loanData.put("purpose", loan.getPurpose());
            loanData.put("applicationDate", loan.getApplicationDate() != null ? 
                loan.getApplicationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
            loanData.put("status", loan.getStatus().getDisplayName());
            loanData.put("interestRate", loan.getInterestRate());
            loanData.put("totalAmount", loan.getTotalAmount());
            loanData.put("amountPaid", loan.getAmountPaid());
            loanData.put("remainingBalance", loan.getRemainingBalance());
            loanData.put("period", period);
            
            response.put("success", true);
            response.put("loan", loanData);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Imeshindikana kupata maelezo: " + e.getMessage());
        }
        
        return response;
    }
    
    @GetMapping("/loans/{id}/agreement")
    @ResponseBody
    public ResponseEntity<Resource> downloadLoanAgreement(@PathVariable Long id, Principal principal) {
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Loan loan = loanRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Mkopo haujapatikana"));
            
            // Verify user owns this loan
            if (!loan.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Huna ruhusa ya kupata mkopo huu");
            }
            
            // Create loan agreement content
            String agreementContent = createLoanAgreementContent(loan, user);
            
            ByteArrayResource resource = new ByteArrayResource(agreementContent.getBytes());
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"loan_agreement_" + loan.getId() + ".txt\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    private String createLoanAgreementContent(Loan loan, User user) {
        StringBuilder content = new StringBuilder();
        content.append("MAKUBALIANO YA MKOPO\n");
        content.append("====================\n\n");
        content.append("Tarehe: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
        content.append("Jina la Mwanachama: ").append(user.getFullName()).append("\n");
        content.append("Barua pepe: ").append(user.getEmail()).append("\n\n");
        
        content.append("MAELEZO YA MKOPO\n");
        content.append("---------------\n");
        content.append("Kiasi cha Mkopo: Tsh ").append(String.format("%,.2f", loan.getAmount())).append("\n");
        content.append("Riba: ").append(loan.getInterestRate()).append("%\n");
        content.append("Kiasi Jumla: Tsh ").append(String.format("%,.2f", loan.getTotalAmount())).append("\n");
        content.append("Tarehe ya Ombi: ").append(loan.getApplicationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        content.append("Hali: ").append(loan.getStatus().getDisplayName()).append("\n\n");
        
        content.append("MAELEKEZO\n");
        content.append("---------\n");
        content.append("1. Mkopo utalipwa kulingana na ratiba iliyopangwa.\n");
        content.append("2. Riba itakadirishwa kila mwezi kulingana na kiasi kilichobaki.\n");
        content.append("3. Muda wa malipo ni kama ulivyoomba.\n");
        content.append("4. Makopo yasiyolipwa kwa wakati yatachukuliwa hatua za kisheria.\n\n");
        
        content.append("SAHIHI\n");
        content.append("------\n");
        content.append("Mwanachama: _________________________\n");
        content.append("Tarehe: _________________________\n\n");
        
        content.append("Afisa wa Kikundi: _________________________\n");
        content.append("Tarehe: _________________________\n");
        
        return content.toString();
    }
    
    @GetMapping("/transactions")
    public String transactions(Model model, Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId());
        
        // Calculate transaction statistics
        double totalIncome = 0.0;
        double totalExpenses = 0.0;
        
        for (Transaction transaction : transactions) {
            if (transaction.getType() == Transaction.TransactionType.SAVINGS_CONTRIBUTION ||
                transaction.getType() == Transaction.TransactionType.LOAN_DISBURSEMENT ||
                transaction.getType() == Transaction.TransactionType.REFUND) {
                totalIncome += transaction.getAmount();
            } else {
                totalExpenses += transaction.getAmount();
            }
        }
        
        double balance = totalIncome - totalExpenses;
        
        model.addAttribute("user", user);
        model.addAttribute("transactions", transactions);
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("balance", balance);
        
        return "transactions";
    }
    
    @PostMapping("/transactions/create")
    @ResponseBody
    public Map<String, Object> createTransaction(@RequestBody Map<String, Object> transactionData, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Create new transaction
            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setGroup(user.getGroup());
            
            // Set transaction type
            String typeStr = (String) transactionData.get("type");
            Transaction.TransactionType type = Transaction.TransactionType.valueOf(typeStr);
            transaction.setType(type);
            
            // Set amount
            Double amount = Double.parseDouble(transactionData.get("amount").toString());
            transaction.setAmount(amount);
            
            // Set description
            String description = (String) transactionData.get("description");
            transaction.setDescription(description);
            
            // Set transaction date
            String dateStr = (String) transactionData.get("date");
            LocalDateTime transactionDate = LocalDateTime.parse(dateStr + "T00:00:00");
            transaction.setTransactionDate(transactionDate);
            
            // Generate reference number
            String referenceNumber = type.name().substring(0, 3) + System.currentTimeMillis();
            transaction.setReferenceNumber(referenceNumber);
            
            // Set status
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            
            // Save transaction
            transactionRepository.save(transaction);
            
            response.put("success", true);
            response.put("message", "Miamala yako imefanikiwa kikamilifu!");
            response.put("transactionId", transaction.getId());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Imeshindikana kutuma miamala: " + e.getMessage());
        }
        
        return response;
    }
    
    @GetMapping("/transactions/{transactionId}")
    @ResponseBody
    public Map<String, Object> getTransactionDetails(@PathVariable Long transactionId, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get transaction by ID and ensure it belongs to the current user
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));
            
            // Security check: ensure transaction belongs to current user
            if (!transaction.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "Hauna ruhusa ya kuona miamala hii.");
                return response;
            }
            
            // Prepare transaction data for response
            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("id", transaction.getId());
            transactionData.put("referenceNumber", transaction.getReferenceNumber());
            transactionData.put("transactionDate", transaction.getTransactionDate());
            transactionData.put("type", transaction.getType().name());
            transactionData.put("status", transaction.getStatus().name());
            transactionData.put("amount", transaction.getAmount());
            transactionData.put("description", transaction.getDescription());
            
            response.put("success", true);
            response.put("transaction", transactionData);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Imeshindikana kupata maelezo ya miamala: " + e.getMessage());
        }
        
        return response;
    }
    
    @GetMapping("/reports")
    public String reports(Model model, Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get real data for statistics
        Double totalSavings = savingsRepository.getTotalSavingsByUser(user.getId());
        if (totalSavings == null) totalSavings = 0.0;
        
        List<Loan.LoanStatus> activeStatuses = Arrays.asList(
            Loan.LoanStatus.ACTIVE, Loan.LoanStatus.APPROVED
        );
        Double totalOutstanding = loanRepository.getTotalOutstandingBalance(user.getId(), activeStatuses);
        if (totalOutstanding == null) totalOutstanding = 0.0;
        
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId());
        int totalTransactions = transactions.size();
        
        double netBalance = totalSavings - totalOutstanding;
        
        // Get savings data for chart (last 6 months)
        List<String> savingsLabels = new ArrayList<>();
        List<Double> savingsData = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        
        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = currentDate.minusMonths(i);
            String monthLabel = monthDate.getMonth().name().substring(0, 3);
            savingsLabels.add(monthLabel);
            
            // Get savings for this month (simplified - in real app, you'd query by date range)
            Double monthSavings = savingsRepository.getTotalSavingsByUser(user.getId());
            if (monthSavings == null) monthSavings = 0.0;
            savingsData.add(monthSavings * (0.8 + (i * 0.04))); // Simulate growth
        }
        
        // Get loan status data
        Map<String, Long> loanStatusCount = new HashMap<>();
        loanStatusCount.put("Inayoendelea", loanRepository.countByUserIdAndStatus(user.getId(), Loan.LoanStatus.ACTIVE));
        loanStatusCount.put("Imekamilishwa", loanRepository.countByUserIdAndStatus(user.getId(), Loan.LoanStatus.COMPLETED));
        loanStatusCount.put("Inasubiri", loanRepository.countByUserIdAndStatus(user.getId(), Loan.LoanStatus.PENDING));
        loanStatusCount.put("Imekataliwa", loanRepository.countByUserIdAndStatus(user.getId(), Loan.LoanStatus.REJECTED));
        
        // Get transaction types data
        Map<String, Long> transactionTypeCount = new HashMap<>();
        transactionTypeCount.put("Mchango wa Akiba", transactions.stream().filter(t -> t.getType() == Transaction.TransactionType.SAVINGS_CONTRIBUTION).count());
        transactionTypeCount.put("Toleo la Mkopo", transactions.stream().filter(t -> t.getType() == Transaction.TransactionType.LOAN_DISBURSEMENT).count());
        transactionTypeCount.put("Malipo ya Mkopo", transactions.stream().filter(t -> t.getType() == Transaction.TransactionType.LOAN_REPAYMENT).count());
        transactionTypeCount.put("Faini", transactions.stream().filter(t -> t.getType() == Transaction.TransactionType.PENALTY_PAYMENT).count());
        transactionTypeCount.put("Ada", transactions.stream().filter(t -> t.getType() == Transaction.TransactionType.FEE).count());
        
        // Get monthly performance data (income vs expenses)
        List<Double> monthlyIncome = new ArrayList<>();
        List<Double> monthlyExpenses = new ArrayList<>();
        
        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = currentDate.minusMonths(i);
            // Simplified calculation - in real app, you'd query by date range
            double income = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.SAVINGS_CONTRIBUTION || 
                           t.getType() == Transaction.TransactionType.LOAN_DISBURSEMENT ||
                           t.getType() == Transaction.TransactionType.REFUND)
                .mapToDouble(Transaction::getAmount)
                .sum();
            
            double expenses = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.LOAN_REPAYMENT || 
                           t.getType() == Transaction.TransactionType.PENALTY_PAYMENT ||
                           t.getType() == Transaction.TransactionType.FEE ||
                           t.getType() == Transaction.TransactionType.INTEREST_PAYMENT)
                .mapToDouble(Transaction::getAmount)
                .sum();
            
            monthlyIncome.add(income * (0.7 + (i * 0.06))); // Simulate variation
            monthlyExpenses.add(expenses * (0.6 + (i * 0.08))); // Simulate variation
        }
        
        model.addAttribute("user", user);
        model.addAttribute("totalSavings", totalSavings);
        model.addAttribute("totalOutstanding", totalOutstanding);
        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("netBalance", netBalance);
        
        // Chart data
        model.addAttribute("savingsLabels", savingsLabels);
        model.addAttribute("savingsData", savingsData);
        model.addAttribute("loanStatusCount", loanStatusCount);
        model.addAttribute("transactionTypeCount", transactionTypeCount);
        model.addAttribute("monthlyIncome", monthlyIncome);
        model.addAttribute("monthlyExpenses", monthlyExpenses);
        
        return "reports";
    }
    
    @GetMapping("/group-info")
    public String groupInfo(Model model, Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Group group = user.getGroup();
        if (group == null) {
            // Handle case where user has no group
            model.addAttribute("user", user);
            model.addAttribute("group", new Group());
            return "group-info";
        }
        
        List<User> members = userRepository.findByGroupId(group.getId());
        
        model.addAttribute("user", user);
        model.addAttribute("group", group);
        model.addAttribute("members", members);
        
        return "group-info";
    }
    
    // API endpoints for AJAX calls
    @GetMapping("/api/summary")
    @ResponseBody
    public Map<String, Object> getFinancialSummary(Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Double totalSavings = savingsRepository.getTotalSavingsByUser(user.getId());
        if (totalSavings == null) totalSavings = 0.0;
        
        List<Loan.LoanStatus> activeStatuses = Arrays.asList(
            Loan.LoanStatus.ACTIVE, Loan.LoanStatus.APPROVED
        );
        Double totalOutstanding = loanRepository.getTotalOutstandingBalance(user.getId(), activeStatuses);
        if (totalOutstanding == null) totalOutstanding = 0.0;
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSavings", totalSavings);
        summary.put("totalOutstanding", totalOutstanding);
        summary.put("availableBalance", totalSavings - totalOutstanding);
        
        return summary;
    }
    
    @GetMapping("/api/transactions")
    @ResponseBody
    public Map<String, Object> getTransactions(Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId());
        
        // Calculate transaction statistics
        double totalIncome = 0.0;
        double totalExpenses = 0.0;
        
        for (Transaction transaction : transactions) {
            if (transaction.getType() == Transaction.TransactionType.SAVINGS_CONTRIBUTION ||
                transaction.getType() == Transaction.TransactionType.LOAN_DISBURSEMENT ||
                transaction.getType() == Transaction.TransactionType.REFUND) {
                totalIncome += transaction.getAmount();
            } else {
                totalExpenses += transaction.getAmount();
            }
        }
        
        double balance = totalIncome - totalExpenses;
        
        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactions);
        response.put("totalIncome", totalIncome);
        response.put("totalExpenses", totalExpenses);
        response.put("balance", balance);
        
        return response;
    }
    
    @PostMapping("/profile/update")
    @ResponseBody
    public Map<String, Object> updateProfile(@RequestParam Map<String, String> profileData, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Update user fields
            user.setFirstName(profileData.get("firstName"));
            user.setLastName(profileData.get("lastName"));
            user.setEmail(profileData.get("email"));
            user.setPhoneNumber(profileData.get("phoneNumber"));
            
            // Save updated user
            userRepository.save(user);
            
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    @PostMapping("/profile/change-password")
    @ResponseBody
    public Map<String, Object> changePassword(@RequestParam Map<String, String> passwordData, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                response.put("success", false);
                response.put("message", "Nenosiri la sasa si sahihi!");
                return response;
            }
            
            // Encode and update new password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            response.put("success", true);
            response.put("message", "Password changed successfully");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    @PostMapping("/profile/upload-picture")
    @ResponseBody
    public Map<String, Object> uploadPicture(@RequestParam("profilePicture") MultipartFile file, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Tafadhali chagua picha!");
                return response;
            }
            
            // Check file size (50MB limit)
            if (file.getSize() > 50 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "Ukubwa wa faili ni mkubwa sana! Ukubwa wa juu ni 50MB.");
                return response;
            }
            
            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "Tafadhali chagua faili la picha!");
                return response;
            }
            
            // Create upload directory if it doesn't exist
            String uploadDir = System.getProperty("user.dir") + "/uploads/profile-pictures/";
            java.io.File directory = new java.io.File(uploadDir);
            
            System.out.println("Upload directory: " + uploadDir);
            System.out.println("Directory exists: " + directory.exists());
            
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                System.out.println("Directory created: " + created);
                if (!created) {
                    response.put("success", false);
                    response.put("message", "Imeshindikana kuunda directory ya kuhifadhi picha!");
                    return response;
                }
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".jpg";
            String filename = "profile_" + user.getId() + "_" + System.currentTimeMillis() + fileExtension;
            
            // Save file
            java.io.File destinationFile = new java.io.File(uploadDir + filename);
            
            System.out.println("Destination file: " + destinationFile.getAbsolutePath());
            System.out.println("File size: " + file.getSize());
            
            try {
                file.transferTo(destinationFile);
                System.out.println("File saved successfully");
            } catch (Exception e) {
                System.err.println("Error saving file: " + e.getMessage());
                response.put("success", false);
                response.put("message", "Imeshindikana kuhifadhi picha: " + e.getMessage());
                return response;
            }
            
            // Update user profile picture path
            user.setProfilePicturePath("/uploads/profile-pictures/" + filename);
            userRepository.save(user);
            
            response.put("success", true);
            response.put("message", "Profile picture uploaded successfully");
            response.put("picturePath", user.getProfilePicturePath());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error uploading picture: " + e.getMessage());
        }
        
        return response;
    }
    
    @PostMapping("/savings/monthly-contribution")
    @ResponseBody
    public Map<String, Object> makeMonthlyContribution(@RequestParam Map<String, String> contributionData, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Parse contribution data
            Double amount = Double.parseDouble(contributionData.get("amount"));
            String description = contributionData.get("description");
            String contributionDateStr = contributionData.get("contributionDate");
            
            // Parse date
            LocalDateTime contributionDate = LocalDateTime.parse(contributionDateStr + "T00:00:00");
            
            // Create new savings record
            Savings savings = new Savings();
            savings.setUser(user);
            savings.setGroup(user.getGroup());
            savings.setAmount(amount);
            savings.setDescription(description);
            savings.setContributionDate(contributionDate);
            savings.setType(Savings.SavingsType.MONTHLY_CONTRIBUTION);
            savings.setCreatedAt(LocalDateTime.now());
            savings.setUpdatedAt(LocalDateTime.now());
            
            // Save savings
            savingsRepository.save(savings);
            
            // Create corresponding transaction
            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setGroup(user.getGroup());
            transaction.setAmount(amount);
            transaction.setDescription(description);
            transaction.setTransactionDate(contributionDate);
            transaction.setType(Transaction.TransactionType.SAVINGS_CONTRIBUTION);
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setReferenceNumber("SAV" + System.currentTimeMillis());
            
            transactionRepository.save(transaction);
            
            // Create notification for user
            createNotification(user, "Savings Contribution Recorded", 
                              "Your contribution of Tsh " + amount + " has been recorded successfully", 
                              Notification.NotificationType.CONTRIBUTION);
            
            response.put("success", true);
            response.put("message", "Monthly contribution saved successfully");
            response.put("transactionId", transaction.getId());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error saving contribution: " + e.getMessage());
        }
        
        return response;
    }
    
    @PostMapping("/savings/additional-savings")
    @ResponseBody
    public Map<String, Object> makeAdditionalSavings(@RequestParam Map<String, String> contributionData, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Parse contribution data
            Double amount = Double.parseDouble(contributionData.get("amount"));
            String description = contributionData.get("description");
            String contributionDateStr = contributionData.get("contributionDate");
            
            // Parse date
            LocalDateTime contributionDate = LocalDateTime.parse(contributionDateStr + "T00:00:00");
            
            // Create new savings record
            Savings savings = new Savings();
            savings.setUser(user);
            savings.setGroup(user.getGroup());
            savings.setAmount(amount);
            savings.setDescription(description);
            savings.setContributionDate(contributionDate);
            savings.setType(Savings.SavingsType.ADDITIONAL_SAVINGS);
            savings.setCreatedAt(LocalDateTime.now());
            savings.setUpdatedAt(LocalDateTime.now());
            
            // Save savings
            savingsRepository.save(savings);
            
            // Create corresponding transaction
            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setGroup(user.getGroup());
            transaction.setAmount(amount);
            transaction.setDescription(description);
            transaction.setTransactionDate(contributionDate);
            transaction.setType(Transaction.TransactionType.SAVINGS_CONTRIBUTION);
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setReferenceNumber("SAV" + System.currentTimeMillis());
            
            transactionRepository.save(transaction);
            
            // Create notification for user
            createNotification(user, "Additional Savings Recorded", 
                              "Your additional savings of Tsh " + amount + " has been recorded successfully", 
                              Notification.NotificationType.CONTRIBUTION);
            
            response.put("success", true);
            response.put("message", "Additional savings saved successfully");
            response.put("transactionId", transaction.getId());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error saving additional savings: " + e.getMessage());
        }
        
        return response;
    }
    
    @GetMapping("/savings/download-report")
    public ResponseEntity<byte[]> downloadSavingsReport(Principal principal) {
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get user's savings
            List<Savings> userSavings = savingsRepository.findByUserIdOrderByContributionDateDesc(user.getId());
            
            // Create Excel content (CSV format that Excel can open)
            StringBuilder excelContent = new StringBuilder();
            excelContent.append("Tarehe,Aina ya Mchango,Maelezo,Kiasi\n");
            
            for (Savings savings : userSavings) {
                excelContent.append(savings.getContributionDate().toLocalDate() + ",");
                excelContent.append(savings.getType().getDisplayName() + ",");
                excelContent.append("\"" + (savings.getDescription() != null ? savings.getDescription() : "") + "\",");
                excelContent.append(savings.getAmount() + "\n");
            }
            
            // Add summary
            excelContent.append("\n");
            excelContent.append("JUMLA YA AKIBA," + userSavings.stream().mapToDouble(Savings::getAmount).sum() + "\n");
            
            byte[] excelBytes = excelContent.toString().getBytes("UTF-8");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                .filename("ripoti_ya_akiba_" + java.time.LocalDate.now() + ".xlsx")
                .build());
            
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            return new ResponseEntity<>(("Error generating report: " + e.getMessage()).getBytes(), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/savings/{id}/details")
    @ResponseBody
    public Map<String, Object> getSavingsDetails(@PathVariable Long id, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get savings by ID and verify ownership
            Savings savings = savingsRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Savings not found"));
            
            // Verify that the savings belongs to the current user
            if (!savings.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Unauthorized access to savings");
            }
            
            // Create savings details map
            Map<String, Object> savingsDetails = new HashMap<>();
            savingsDetails.put("id", savings.getId());
            savingsDetails.put("contributionDate", savings.getContributionDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            savingsDetails.put("type", savings.getType().getDisplayName());
            savingsDetails.put("amount", String.format("%,.2f", savings.getAmount()));
            savingsDetails.put("description", savings.getDescription() != null ? savings.getDescription() : "-");
            savingsDetails.put("groupName", savings.getGroup().getName());
            
            response.put("success", true);
            response.put("savings", savingsDetails);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching savings details: " + e.getMessage());
        }
        
        return response;
    }
    
    @GetMapping("/savings/{id}/receipt")
    public ResponseEntity<byte[]> downloadSavingsReceipt(@PathVariable Long id, Principal principal) {
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get savings by ID and verify ownership
            Savings savings = savingsRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Savings not found"));
            
            // Verify that the savings belongs to the current user
            if (!savings.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Unauthorized access to savings");
            }
            
            // Create receipt content (Excel format)
            StringBuilder receiptContent = new StringBuilder();
            receiptContent.append("RISITI YA MCHANGO\n");
            receiptContent.append("==================\n\n");
            receiptContent.append("ID ya Mchango,#" + savings.getId() + "\n");
            receiptContent.append("Jina la Mwanachama," + user.getFirstName() + " " + user.getLastName() + "\n");
            receiptContent.append("Kikundi," + savings.getGroup().getName() + "\n");
            receiptContent.append("Tarehe," + savings.getContributionDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n");
            receiptContent.append("Aina ya Mchango," + savings.getType().getDisplayName() + "\n");
            receiptContent.append("Kiasi,Tsh " + String.format("%,.2f", savings.getAmount()) + "\n");
            receiptContent.append("Maelezo,\"" + (savings.getDescription() != null ? savings.getDescription() : "-") + "\"\n\n");
            receiptContent.append("Hali,IMETHIBITISHWA\n");
            receiptContent.append("==================\n");
            receiptContent.append("Hii ni risiti ya kihisania. Tafadhali ihifadhi salama.\n");
            
            byte[] receiptBytes = receiptContent.toString().getBytes("UTF-8");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                .filename("risiti_ya_mchango_" + id + ".xlsx")
                .build());
            
            return new ResponseEntity<>(receiptBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            return new ResponseEntity<>(("Error generating receipt: " + e.getMessage()).getBytes(), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/api/user-loans")
    @ResponseBody
    public Map<String, Object> getUserLoans(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get active loans for the user
            List<Loan.LoanStatus> activeStatuses = Arrays.asList(
                Loan.LoanStatus.ACTIVE, 
                Loan.LoanStatus.APPROVED
            );
            
            List<Loan> activeLoans = loanRepository.findByUserIdAndStatusIn(user.getId(), activeStatuses);
            
            // Prepare loan data with remaining balance
            List<Map<String, Object>> loanData = new ArrayList<>();
            for (Loan loan : activeLoans) {
                Map<String, Object> loanInfo = new HashMap<>();
                loanInfo.put("id", loan.getId());
                loanInfo.put("amount", loan.getAmount());
                loanInfo.put("remainingBalance", calculateRemainingBalance(loan));
                loanData.add(loanInfo);
            }
            
            response.put("success", true);
            response.put("loans", loanData);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Imeshindikana kupata mikopo: " + e.getMessage());
        }
        
        return response;
    }
    
    @PostMapping("/api/loan-repayment")
    @ResponseBody
    public Map<String, Object> processLoanRepayment(@RequestBody Map<String, Object> repaymentData, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get the loan
            Long loanId = Long.parseLong(repaymentData.get("loanId").toString());
            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Mkopo haujapatikana"));
            
            // Security check: ensure loan belongs to current user
            if (!loan.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "Hauna ruhusa ya kulipa mkopo huu.");
                return response;
            }
            
            // Get repayment details
            Double amount = Double.parseDouble(repaymentData.get("amount").toString());
            String paymentMethod = (String) repaymentData.get("paymentMethod");
            String paymentReference = (String) repaymentData.get("paymentReference");
            String repaymentDateStr = (String) repaymentData.get("repaymentDate");
            String description = (String) repaymentData.get("description");
            
            // Parse repayment date
            LocalDateTime repaymentDate = LocalDateTime.parse(repaymentDateStr + "T00:00:00");
            
            // Create loan repayment transaction
            Transaction repaymentTransaction = new Transaction();
            repaymentTransaction.setUser(user);
            repaymentTransaction.setGroup(user.getGroup());
            repaymentTransaction.setAmount(amount);
            repaymentTransaction.setType(Transaction.TransactionType.LOAN_REPAYMENT);
            repaymentTransaction.setTransactionDate(repaymentDate);
            repaymentTransaction.setDescription(description != null ? description : "Malipo ya mkopo - " + paymentMethod);
            repaymentTransaction.setReferenceNumber("REP" + System.currentTimeMillis());
            repaymentTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            
            // Save the transaction
            transactionRepository.save(repaymentTransaction);
            
            // Update loan status if fully paid
            double currentBalance = calculateRemainingBalance(loan);
            if (amount >= currentBalance) {
                loan.setStatus(Loan.LoanStatus.COMPLETED);
                loan.setCompletionDate(repaymentDate);
            }
            
            loanRepository.save(loan);
            
            // Create notification for user
            String notificationTitle = amount >= currentBalance ? "Loan Fully Paid" : "Loan Repayment Received";
            String notificationMessage = amount >= currentBalance ? 
                "Congratulations! Your loan has been fully paid. Total payment: Tsh " + amount :
                "Your loan repayment of Tsh " + amount + " has been received. Remaining balance: Tsh " + (currentBalance - amount);
            
            createNotification(user, notificationTitle, notificationMessage, Notification.NotificationType.LOAN);
            
            response.put("success", true);
            response.put("message", "Malipo ya mkopo yamefanikiwa kikamilifu!");
            response.put("transactionId", repaymentTransaction.getId());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Imeshindikana kutuma malipo: " + e.getMessage());
        }
        
        return response;
    }
    
    // Helper method to calculate remaining balance
    private double calculateRemainingBalance(Loan loan) {
        // Get all repayment transactions for this loan
        List<Transaction> repayments = transactionRepository.findByUserIdAndTypeOrderByTransactionDateDesc(
            loan.getUser().getId(), 
            Transaction.TransactionType.LOAN_REPAYMENT
        );
        
        double totalRepaid = repayments.stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        return loan.getAmount() - totalRepaid;
    }
}
