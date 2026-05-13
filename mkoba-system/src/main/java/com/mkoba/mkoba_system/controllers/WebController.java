package com.mkoba.mkoba_system.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {
    
    @GetMapping("/payments/success")
    public String paymentSuccess(
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String mock,
            Model model) {
        
        System.out.println("🎉 Payment Success Page Accessed");
        System.out.println("📊 Transaction ID: " + transactionId);
        System.out.println("💰 Amount: " + amount);
        System.out.println("📱 Status: " + status);
        System.out.println("🔧 Mock Mode: " + mock);
        
        // Handle mock payment simulation
        if ("true".equals(mock) && transactionId != null && transactionId.startsWith("MOCK_TXN_")) {
            System.out.println("🔧 Processing mock payment simulation...");
            
            // Simulate successful payment
            if (status == null) {
                status = "SUCCESS";
            }
            if (amount == null) {
                amount = "1000"; // Default mock amount
            }
            
            System.out.println("✅ Mock payment simulated successfully!");
            
            // Add mock indicator
            model.addAttribute("mockPayment", true);
            model.addAttribute("mockMessage", "This was a test payment using mock AzamPay API");
        }
        
        // Add transaction details to model
        model.addAttribute("transactionId", transactionId);
        model.addAttribute("amount", amount);
        model.addAttribute("status", status);
        
        return "payments/success";
    }
}
