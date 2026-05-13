package com.mkoba.mkoba_system.controllers;

import com.mkoba.mkoba_system.entities.Loan;
import com.mkoba.mkoba_system.entities.Savings;
import com.mkoba.mkoba_system.entities.Savings.SavingsType;
import com.mkoba.mkoba_system.entities.User;
import com.mkoba.mkoba_system.repositories.LoanRepository;
import com.mkoba.mkoba_system.repositories.SavingsRepository;
import com.mkoba.mkoba_system.repositories.UserRepository;
import com.mkoba.mkoba_system.services.AzamPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    @Autowired
    private AzamPayService azamPayService;
    
    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private SavingsRepository savingsRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * AzamPay callback endpoint
     */
    @PostMapping("/callback")
    public ResponseEntity<String> paymentCallback(@RequestBody Map<String, Object> callback) {
        try {
            System.out.println("🔔 Received AzamPay callback: " + callback);
            
            // Extract payment details
            String transactionId = (String) callback.get("transactionId");
            String status = (String) callback.get("status");
            String reference = (String) callback.get("reference");
            Double amount = Double.valueOf(callback.get("amount").toString());
            
            // Process based on reference type
            if (reference.startsWith("LOAN-")) {
                processLoanDisbursement(reference, status, amount, transactionId);
            } else if (reference.startsWith("SAVINGS-")) {
                processSavingsContribution(reference, status, amount, transactionId);
            } else if (reference.startsWith("REPAY-")) {
                processLoanRepayment(reference, status, amount, transactionId);
            }
            
            return ResponseEntity.ok("SUCCESS");
            
        } catch (Exception e) {
            System.err.println("❌ Error processing payment callback: " + e.getMessage());
            return ResponseEntity.badRequest().body("ERROR");
        }
    }
    
    /**
     * Initiate loan disbursement
     */
    @PostMapping("/disburse/{loanId}")
    public ResponseEntity<Map<String, Object>> disburseLoan(@PathVariable Long loanId, Principal principal) {
        try {
            Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
            
            // Generate reference
            String reference = "LOAN-" + loanId;
            String description = "Loan disbursement for " + loan.getUser().getFullName();
            
            // Initiate payment
            Map<String, Object> response = azamPayService.initiatePayment(
                loan.getAmount().toString(),
                loan.getUser().getPhoneNumber(),
                reference,
                description
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error disbursing loan: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Process loan repayment
     */
    @PostMapping("/repay/{loanId}")
    public ResponseEntity<Map<String, Object>> repayLoan(@PathVariable Long loanId, 
                                                      @RequestBody Map<String, Object> request) {
        try {
            Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
            
            Double amount = Double.valueOf(request.get("amount").toString());
            String phoneNumber = (String) request.get("phoneNumber");
            
            // Generate reference
            String reference = "REPAY-" + loanId;
            String description = "Loan repayment for " + loan.getUser().getFullName();
            
            // Initiate payment
            Map<String, Object> response = azamPayService.initiatePayment(
                amount.toString(),
                phoneNumber,
                reference,
                description
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error processing loan repayment: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Test endpoint for savings contribution (GET)
     */
    @GetMapping("/contribute")
    public ResponseEntity<Map<String, Object>> testContributeSavings() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "AzamPay API is ready!");
        response.put("status", "success");
        response.put("instructions", "Use POST method to process actual payments");
        response.put("test_data", Map.of(
            "userId", 1,
            "amount", 10000,
            "phoneNumber", "255754123456"
        ));
        return ResponseEntity.ok(response);
    }
    
    /**
     * Process savings contribution
     */
    @PostMapping("/contribute")
    public ResponseEntity<Map<String, Object>> contributeSavings(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Double amount = Double.valueOf(request.get("amount").toString());
            String phoneNumber = (String) request.get("phoneNumber");
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Generate reference
            String reference = "SAVINGS-" + userId + "-" + System.currentTimeMillis();
            String description = "Savings contribution for " + user.getFullName();
            
            // Initiate payment
            Map<String, Object> response = azamPayService.initiatePayment(
                amount.toString(),
                phoneNumber,
                reference,
                description
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error processing savings contribution: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Check payment status
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(@PathVariable String transactionId) {
        try {
            Map<String, Object> status = azamPayService.checkPaymentStatus(transactionId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            System.err.println("❌ Error checking payment status: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Private helper methods
    private void processLoanDisbursement(String reference, String status, Double amount, String transactionId) {
        try {
            Long loanId = Long.valueOf(reference.replace("LOAN-", ""));
            Loan loan = loanRepository.findById(loanId).orElse(null);
            
            if (loan != null && "SUCCESS".equals(status)) {
                loan.setStatus(Loan.LoanStatus.ACTIVE);
                loanRepository.save(loan);
                System.out.println("✅ Loan " + loanId + " disbursed successfully via AzamPay");
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing loan disbursement: " + e.getMessage());
        }
    }
    
    private void processSavingsContribution(String reference, String status, Double amount, String transactionId) {
        try {
            // Extract user ID from reference
            String[] parts = reference.replace("SAVINGS-", "").split("-");
            Long userId = Long.valueOf(parts[0]);
            
            if ("SUCCESS".equals(status)) {
                Savings savings = new Savings();
                savings.setUser(userRepository.findById(userId).orElse(null));
                savings.setAmount(amount);
                savings.setContributionDate(LocalDateTime.now());
                savings.setDescription("AzamPay mobile payment contribution");
                savings.setType(SavingsType.MONTHLY_CONTRIBUTION);
                savings.setCreatedAt(LocalDateTime.now());
                savings.setUpdatedAt(LocalDateTime.now());
                savingsRepository.save(savings);
                System.out.println("✅ AzamPay savings contribution of " + amount + " processed successfully");
                System.out.println("✅ Savings ID: " + savings.getId() + ", User: " + savings.getUser().getFirstName());
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing savings contribution: " + e.getMessage());
        }
    }
    
    private void processLoanRepayment(String reference, String status, Double amount, String transactionId) {
        try {
            Long loanId = Long.valueOf(reference.replace("REPAY-", ""));
            Loan loan = loanRepository.findById(loanId).orElse(null);
            
            if (loan != null && "SUCCESS".equals(status)) {
                loan.setAmountPaid(loan.getAmountPaid() + amount);
                loan.setRemainingBalance(loan.getRemainingBalance() - amount);
                loanRepository.save(loan);
                System.out.println("✅ Loan repayment of " + amount + " processed successfully");
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing loan repayment: " + e.getMessage());
        }
    }
}
