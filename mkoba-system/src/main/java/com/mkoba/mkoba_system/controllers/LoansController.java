package com.mkoba.mkoba_system.controllers;

import com.mkoba.mkoba_system.entities.Loan;
import com.mkoba.mkoba_system.entities.User;
import com.mkoba.mkoba_system.entities.Group;
import com.mkoba.mkoba_system.entities.Notification;
import com.mkoba.mkoba_system.repositories.LoanRepository;
import com.mkoba.mkoba_system.repositories.UserRepository;
import com.mkoba.mkoba_system.repositories.GroupRepository;
import com.mkoba.mkoba_system.repositories.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import java.security.Principal;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class LoansController {

    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;

    // Loans Management Page
    @GetMapping("/loans")
    public String loansPage(Model model) {
        try {
            // Get all loans with different statuses
            List<Loan> pendingLoans = loanRepository.findByStatus(Loan.LoanStatus.PENDING);
            List<Loan> pendingAccountantLoans = loanRepository.findByStatus(Loan.LoanStatus.PENDING_ACCOUNTANT_CONFIRMATION);
            List<Loan> approvedLoans = loanRepository.findByStatus(Loan.LoanStatus.APPROVED);
            List<Loan> rejectedLoans = loanRepository.findByStatus(Loan.LoanStatus.REJECTED);
            List<Loan> defaultedLoans = loanRepository.findByStatus(Loan.LoanStatus.DEFAULTED);
            List<Loan> allLoans = loanRepository.findAll();
            
            // Calculate additional statistics
            long totalLoans = allLoans.size();
            double totalLoanAmount = allLoans.stream().mapToDouble(Loan::getAmount).sum();
            long activeLoans = allLoans.stream().filter(loan -> Loan.LoanStatus.ACTIVE.equals(loan.getStatus())).count();
            long completedLoans = allLoans.stream().filter(loan -> Loan.LoanStatus.COMPLETED.equals(loan.getStatus())).count();
            
            model.addAttribute("pendingLoans", pendingLoans);
            model.addAttribute("pendingAccountantLoans", pendingAccountantLoans);
            model.addAttribute("approvedLoans", approvedLoans);
            model.addAttribute("rejectedLoans", rejectedLoans);
            model.addAttribute("defaultedLoans", defaultedLoans);
            model.addAttribute("allLoans", allLoans);
            
            // Add statistics for real-time overview
            model.addAttribute("totalLoans", totalLoans);
            model.addAttribute("totalLoanAmount", totalLoanAmount);
            model.addAttribute("activeLoans", activeLoans);
            model.addAttribute("completedLoans", completedLoans);
            
            // Debug: Print statistics to console
            System.out.println("🔍 LOAN STATISTICS:");
            System.out.println("  Total Loans: " + totalLoans);
            System.out.println("  Pending: " + pendingLoans.size());
            System.out.println("  Pending Accountant: " + pendingAccountantLoans.size());
            System.out.println("  Approved: " + approvedLoans.size());
            System.out.println("  Rejected: " + rejectedLoans.size());
            System.out.println("  Defaulted: " + defaultedLoans.size());
            System.out.println("  Active: " + activeLoans);
            System.out.println("  Completed: " + completedLoans);
            System.out.println("  Total Amount: " + totalLoanAmount);
            
            return "admin/loans";
        } catch (Exception e) {
            System.err.println("❌ ERROR loading loans: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading loans: " + e.getMessage());
            return "admin/loans";
        }
    }

    // Approve Loan
    @PostMapping("/loans/approve/{id}")
    public String approveLoan(@PathVariable Long id, 
                           @RequestParam(required = false) Double interestRate,
                           @RequestParam(required = false) Integer repaymentPeriod,
                           @RequestParam(required = false) String approvalReason,
                           RedirectAttributes redirectAttributes) {
        try {
            Optional<Loan> loanOpt = loanRepository.findById(id);
            if (loanOpt.isPresent()) {
                Loan loan = loanOpt.get();
                loan.setStatus(Loan.LoanStatus.PENDING_ACCOUNTANT_CONFIRMATION);
                loan.setApprovalDate(LocalDateTime.now());
                
                // Update interest rate if provided
                if (interestRate != null) {
                    loan.setInterestRate(interestRate);
                }
                
                // Calculate due date based on repayment period
                if (repaymentPeriod != null) {
                    LocalDateTime dueDate = LocalDateTime.now().plusMonths(repaymentPeriod);
                    loan.setDueDate(dueDate);
                }
                
                loanRepository.save(loan);
                
                // Create notification for loan approval (pending accountant confirmation)
                try {
                    String notificationMessage = "Mkopo wako wa TZS " + 
                        String.format("%,.0f", loan.getAmount()) + " umependekezwa kwa ukubaliwa. Inasubiri uthibitisho wa mhasibu.";
                    if (approvalReason != null && !approvalReason.trim().isEmpty()) {
                        notificationMessage += " Maoni: " + approvalReason;
                    }
                    
                    Notification notification = new Notification();
                    notification.setUser(loan.getUser());
                    notification.setMessage(notificationMessage);
                    notification.setType(Notification.NotificationType.LOAN);
                    notification.setCreatedAt(LocalDateTime.now());
                    notification.setIsRead(false);
                    notificationRepository.save(notification);
                    
                    System.out.println("✅ Notification created for loan approval: " + notificationMessage);
                } catch (Exception e) {
                    System.err.println("❌ Error creating approval notification: " + e.getMessage());
                }
                
                redirectAttributes.addFlashAttribute("success", "Mkopo umependezwa kwa ukubaliwa. Inasubiri uthibitisho wa mhasibu!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Mkopo haujapatikana!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Imeshindikana kukubali mkopo: " + e.getMessage());
        }
        return "redirect:/admin/loans";
    }

    // Accountant Confirm Loan
    @PostMapping("/loans/confirm/{id}")
    public String confirmLoan(@PathVariable Long id, 
                           @RequestParam(required = false) String confirmationReason,
                           Principal principal,
                           RedirectAttributes redirectAttributes) {
        try {
            // Check if user has accountant role
            if (principal == null) {
                redirectAttributes.addFlashAttribute("error", "You must be logged in to confirm loans");
                return "redirect:/admin/loans";
            }
            
            // Get current user
            String email = principal.getName();
            User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if user is treasurer or admin
            if (currentUser.getRole() != User.UserRole.TREASURER && 
                currentUser.getRole() != User.UserRole.ADMIN) {
                redirectAttributes.addFlashAttribute("error", "You have no permission to confirm loans. Only treasurers and admins can confirm loans.");
                return "redirect:/admin/loans";
            }
            
            Optional<Loan> loanOpt = loanRepository.findById(id);
            if (loanOpt.isPresent()) {
                Loan loan = loanOpt.get();
                loan.setStatus(Loan.LoanStatus.APPROVED);
                loan.setAccountantConfirmationDate(LocalDateTime.now());
                loan.setAccountantConfirmationReason(confirmationReason);
                
                loanRepository.save(loan);
                
                // Create notification for final approval
                try {
                    String notificationMessage = "Mkopo wako wa TZS " + 
                        String.format("%,.0f", loan.getAmount()) + " umekubaliwa na mhazini!";
                    if (confirmationReason != null && !confirmationReason.trim().isEmpty()) {
                        notificationMessage += " Maoni ya mhazini: " + confirmationReason;
                    }
                    
                    Notification notification = new Notification();
                    notification.setUser(loan.getUser());
                    notification.setMessage(notificationMessage);
                    notification.setType(Notification.NotificationType.LOAN);
                    notification.setCreatedAt(LocalDateTime.now());
                    notification.setIsRead(false);
                    notificationRepository.save(notification);
                    
                    System.out.println("✅ Notification created for treasurer confirmation: " + notificationMessage);
                } catch (Exception e) {
                    System.err.println("❌ Error creating confirmation notification: " + e.getMessage());
                }
                
                redirectAttributes.addFlashAttribute("success", "Mkopo umethibitishwa na mhazini!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Mkopo haujapatikana!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Imeshindikana kuthibitisha mkopo: " + e.getMessage());
        }
        return "redirect:/admin/loans";
    }

    // Reject Loan
    @PostMapping("/loans/reject/{id}")
    public String rejectLoan(@PathVariable Long id, 
                          @RequestParam(required = false) String reason,
                          @RequestParam(required = false) String rejectionType,
                          RedirectAttributes redirectAttributes) {
        try {
            Optional<Loan> loanOpt = loanRepository.findById(id);
            if (loanOpt.isPresent()) {
                Loan loan = loanOpt.get();
                loan.setStatus(Loan.LoanStatus.REJECTED);
                loanRepository.save(loan);
                
                // Create notification for loan rejection
                try {
                    String notificationMessage = "Mkopo wako wa TZS " + 
                        String.format("%,.0f", loan.getAmount()) + " umekataliwa.";
                    if (reason != null && !reason.trim().isEmpty()) {
                        notificationMessage += " Sababu: " + reason;
                    }
                    if (rejectionType != null && !rejectionType.trim().isEmpty()) {
                        notificationMessage += " Aina ya katali: " + rejectionType.replace("_", " ");
                    }
                    
                    Notification notification = new Notification();
                    notification.setUser(loan.getUser());
                    notification.setMessage(notificationMessage);
                    notification.setType(Notification.NotificationType.LOAN);
                    notification.setCreatedAt(LocalDateTime.now());
                    notification.setIsRead(false);
                    notificationRepository.save(notification);
                    
                    System.out.println("✅ Notification created for loan rejection: " + notificationMessage);
                } catch (Exception e) {
                    System.err.println("❌ Error creating rejection notification: " + e.getMessage());
                }
                
                redirectAttributes.addFlashAttribute("success", "Mkopo umekataliwa!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Mkopo haujapatikana!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Imeshindikana kukataa mkopo: " + e.getMessage());
        }
        return "redirect:/admin/loans";
    }

    // Apply Penalty
    @PostMapping("/loans/penalize/{id}")
    public String penalizeLoan(@PathVariable Long id, 
                             @RequestParam Double penaltyAmount,
                             RedirectAttributes redirectAttributes) {
        try {
            Optional<Loan> loanOpt = loanRepository.findById(id);
            if (loanOpt.isPresent()) {
                Loan loan = loanOpt.get();
                
                // Add penalty to total amount
                double newTotalAmount = loan.getTotalAmount() + penaltyAmount;
                loan.setTotalAmount(newTotalAmount);
                loan.setRemainingBalance(newTotalAmount);
                
                // Mark as defaulted if not already
                if (!Loan.LoanStatus.DEFAULTED.equals(loan.getStatus())) {
                    loan.setStatus(Loan.LoanStatus.DEFAULTED);
                }
                
                loanRepository.save(loan);
                redirectAttributes.addFlashAttribute("success", "Penalty imewekwa kwa mafanikio!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Mkopo haujapatikana!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Imeshindikana kuweka penalty: " + e.getMessage());
        }
        return "redirect:/admin/loans";
    }

    // Mark Loan as Completed
    @PostMapping("/loans/complete/{id}")
    public String completeLoan(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Loan> loanOpt = loanRepository.findById(id);
            if (loanOpt.isPresent()) {
                Loan loan = loanOpt.get();
                loan.setStatus(Loan.LoanStatus.COMPLETED);
                loan.setCompletionDate(LocalDateTime.now());
                loan.setAmountPaid(loan.getTotalAmount());
                loan.setRemainingBalance(0.0);
                
                loanRepository.save(loan);
                redirectAttributes.addFlashAttribute("success", "Mkopo umemalizika kwa mafanikio!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Mkopo haujapatikana!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Imeshindikana kumaliza mkopo: " + e.getMessage());
        }
        return "redirect:/admin/loans";
    }

    // Activate Loan
    @PostMapping("/loans/activate/{id}")
    public String activateLoan(@PathVariable Long id, 
                            @RequestParam(required = false) String dueDate,
                            @RequestParam(required = false) Integer repaymentPeriod,
                            RedirectAttributes redirectAttributes) {
        try {
            Optional<Loan> loanOpt = loanRepository.findById(id);
            if (loanOpt.isPresent()) {
                Loan loan = loanOpt.get();
                
                // Set due date if provided
                if (dueDate != null && !dueDate.isEmpty()) {
                    loan.setDueDate(LocalDate.parse(dueDate).atStartOfDay());
                }
                
                // Calculate total amount with interest
                double totalAmount = loan.getAmount() + (loan.getAmount() * (loan.getInterestRate() / 100));
                loan.setTotalAmount(totalAmount);
                loan.setRemainingBalance(totalAmount);
                
                // Change status to ACTIVE
                loan.setStatus(Loan.LoanStatus.ACTIVE);
                
                loanRepository.save(loan);
                redirectAttributes.addFlashAttribute("success", "Mkopo umeshugharishwa kwa mafanikio!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Mkopo haujapatikana!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Imeshindikana kushugharisha mkopo: " + e.getMessage());
        }
        return "redirect:/admin/loans";
    }

    // Reactivate Loan
    @PostMapping("/loans/reactivate/{id}")
    public String reactivateLoan(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Loan> loanOpt = loanRepository.findById(id);
            if (loanOpt.isPresent()) {
                Loan loan = loanOpt.get();
                loan.setStatus(Loan.LoanStatus.ACTIVE);
                loan.setDueDate(LocalDateTime.now().plusMonths(3));
                
                loanRepository.save(loan);
                redirectAttributes.addFlashAttribute("success", "Mkopo umewashwa tena kwa mafanikio!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Mkopo haujapatikana!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Imeshindikana kuwasha mkopo: " + e.getMessage());
        }
        return "redirect:/admin/loans";
    }

    // View Loan Details
    @GetMapping("/loans/view/{id}")
    public String viewLoan(@PathVariable Long id, Model model) {
        try {
            Optional<Loan> loanOpt = loanRepository.findById(id);
            if (loanOpt.isPresent()) {
                model.addAttribute("loan", loanOpt.get());
                return "admin/view-loan";
            } else {
                return "redirect:/admin/loans";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error loading loan details: " + e.getMessage());
            return "redirect:/admin/loans";
        }
    }
}
