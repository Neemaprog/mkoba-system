package com.mkoba.mkoba_system.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;
    
    @Column(nullable = false)
    private Double amount;
    
    @Column
    private Double interestRate;
    
    @Column(nullable = false)
    private Double totalAmount;
    
    @Column(nullable = false)
    private Double amountPaid;
    
    @Column(nullable = false)
    private Double remainingBalance;
    
    @Column(nullable = false)
    private LocalDateTime applicationDate;
    
    private LocalDateTime approvalDate;
    
    private LocalDateTime accountantConfirmationDate;
    
    private String accountantConfirmationReason;
    
    private LocalDateTime dueDate;
    
    private LocalDateTime completionDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;
    
    @Column
    private String purpose;
    
    @Column
    private String guarantor;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = LoanStatus.PENDING;
        }
        calculateTotals();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateTotals();
    }
    
    private void calculateTotals() {
        if (amount != null && interestRate != null) {
            totalAmount = amount + (amount * (interestRate / 100));
        }
        if (totalAmount != null && amountPaid != null) {
            remainingBalance = totalAmount - amountPaid;
        }
    }
    
    public enum LoanStatus {
        PENDING("Inasubiri"),
        PENDING_ACCOUNTANT_CONFIRMATION("Inasubiri Uthibitisho wa Mhasibu"),
        APPROVED("Imekubaliwa"),
        REJECTED("Imekataliwa"),
        ACTIVE("Inaendelea"),
        COMPLETED("Imemalizika"),
        DEFAULTED("Imeshindwa Kulipwa");
        
        private final String displayName;
        
        LoanStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
